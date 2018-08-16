package com.example;

import org.flywaydb.core.Flyway;
import org.jooq.codegen.GenerationTool;
import org.jooq.meta.jaxb.Configuration;
import org.jooq.meta.jaxb.Database;
import org.jooq.meta.jaxb.Generate;
import org.jooq.meta.jaxb.Generator;
import org.jooq.meta.jaxb.Jdbc;
import org.jooq.meta.jaxb.Target;

/**
 * This class is used to generate the schema classes under the <code>com.example.schema</code> package.
 */
public class SchemaGen {
  public static void main(String[] args) throws Exception {
    String jdbcUrl = "jdbc:h2:/tmp/h2db";
    String dbUser = "sa";
    Jdbc jdbc = new Jdbc()
        .withDriver("org.h2.Driver")
        .withUrl(jdbcUrl)
        .withUser(dbUser);

    // Specify which tables to include (here we just include everything and filter out unwanted files in the build rule
    // which ends up being simpler).
    Database database = new Database()
        .withName("org.jooq.meta.h2.H2Database")
        .withIncludes(".*")
        .withInputSchema("PUBLIC")
        .withOutputSchemaToDefault(true);

    // Configure the output package in which these classes should reside.
    Target target = new Target()
        .withPackageName("com.example.schema")
        .withDirectory("/tmp/dbgen");

    Generator generator = new Generator()
        .withDatabase(database)
        .withTarget(target)
        .withGenerate(new Generate().withVarargSetters(false));

    Configuration conf = new Configuration()
        .withJdbc(jdbc)
        .withGenerator(generator);

    Flyway flyway = new Flyway();
    flyway.setDataSource(jdbcUrl, dbUser, "");
    flyway.setLocations("classpath:migrations");
    flyway.migrate();

    GenerationTool.generate(conf);

    flyway.clean();
  }
}
