package io.boomerang.security.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum TeamRoleEnum {
  OWNER("owner"), EDITOR("editor"), READER("reader");

  private String role;

  TeamRoleEnum(String role) {
    this.role = role;
  }

  @JsonValue
  public String getRole() {
    return role;
  }
}
