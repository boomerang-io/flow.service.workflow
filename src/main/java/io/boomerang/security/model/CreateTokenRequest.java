package io.boomerang.security.model;

import java.util.Date;
import java.util.List;

public class CreateTokenRequest {

  private AuthType type;
  private String name;
  private String principal;
  private String description;
  private Date expirationDate;
  private List<String> permissions;
  private List<String> teams;
  
  public AuthType getType() {
    return type;
  }
  public void setType(AuthType type) {
    this.type = type;
  }
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }
  public String getDescription() {
    return description;
  }
  public String getPrincipal() {
    return principal;
  }
  public void setPrincipal(String principal) {
    this.principal = principal;
  }
  public void setDescription(String description) {
    this.description = description;
  }
  public Date getExpirationDate() {
    return expirationDate;
  }
  public void setExpirationDate(Date expirationDate) {
    this.expirationDate = expirationDate;
  }
  public List<String> getPermissions() {
    return permissions;
  }
  public void setPermissions(List<String> permissions) {
    this.permissions = permissions;
  }
  public List<String> getTeams() {
    return teams;
  }
  public void setTeams(List<String> teams) {
    this.teams = teams;
  }
}
