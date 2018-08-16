package com.example;

import static com.example.schema.tables.Users.USERS;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.RenderNameStyle;
import org.jooq.conf.Settings;
import org.jooq.conf.StatementType;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.lambda.Unchecked;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

public class Main {
  private static AtomicInteger errorCount = new AtomicInteger();

  private static DataSource initDatabase() {
    JdbcDataSource dataSource = new JdbcDataSource();
    dataSource.setURL("jdbc:h2:/tmp/h2db");
    dataSource.setUser("sa");

    Flyway flyway = new Flyway();
    flyway.setDataSource(dataSource);
    flyway.setLocations("classpath:migrations");
    flyway.setLocations("filesystem:src/main/resources/migrations");
    flyway.migrate();

    Runtime.getRuntime().addShutdownHook(new Thread(flyway::clean));

    return dataSource;
  }

  private static DSLContext setupContext(DataSource dataSource) {
    Configuration configuration = new DefaultConfiguration()
        .set(dataSource)
        .set(SQLDialect.POSTGRES)
        .set(new Settings()
            // Uncommenting this will stop Jooq from producing inconsistencies.
            // .withCacheRecordMappers(false)
            .withExecuteWithOptimisticLocking(true)
            .withExecuteLogging(true)
            .withRenderNameStyle(RenderNameStyle.AS_IS)
            .withStatementType(StatementType.PREPARED_STATEMENT));
    return DSL.using(configuration);
  }

  private static Callable<User> queryForUser(DSLContext context, String username) {
    return () -> {
      User user = context.select()
          .from(USERS)
          .where(USERS.USERNAME.eq(username))
          .fetchOneInto(User.class);
      if (!user.getUsername().equals(username)) {
        System.out.println("Inconsistency found: expecting username to be" + username + ". Got " + user);
        errorCount.incrementAndGet();
      }
      return user;
    };
  }

  public static void main(String[] args) throws InterruptedException, IOException {
    DSLContext context = setupContext(initDatabase());

    context.insertInto(USERS)
        .values("FirstUser", "first@example.com")
        .values("SecondUser", "second@example.com")
        .execute();

    ExecutorService executor = Executors.newWorkStealingPool();
    List<Future<User>> futures = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      futures.add(executor.submit(queryForUser(context, "FirstUser")));
      futures.add(executor.submit(queryForUser(context, "SecondUser")));
    }

    futures.forEach(Unchecked.consumer(Future::get));

    executor.awaitTermination(10, TimeUnit.SECONDS);
    System.out.println("Total number of errors: " + errorCount);
    executor.shutdown();
  }
}
