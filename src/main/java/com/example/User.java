package com.example;

import java.beans.ConstructorProperties;

public class User {

  private final String username;
  private final String email;

  @ConstructorProperties({"username", "email"})
  public User(String username, String email) {
    this.username = username;
    this.email = email;
  }

  public String getUsername() {
    return username;
  }

  public String getEmail() {
    return email;
  }

  @Override
  public String toString() {
    return "User{username=" + username + ", email=" + email + "}";
  }
}
