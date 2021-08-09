package io.boomerang.security.model;

import io.boomerang.mongo.model.TokenScope;

public class UserToken extends Token {

  private String email;
  private String firstName;
  private String lastName;

  public UserToken(String email, String firstName, String lastName) {
    super();
    this.email = email;
    this.firstName = firstName;
    this.lastName = lastName;
    this.setScope(TokenScope.user);
  }

  public String getEmail() {
    return email;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }
}
