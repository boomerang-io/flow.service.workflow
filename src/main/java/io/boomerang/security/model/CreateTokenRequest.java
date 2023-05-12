package io.boomerang.security.model;

import java.util.Date;
import java.util.List;

public class CreateTokenRequest {

  private TokenScope type;
  private String name;
  private String principal;
  private String description;
  private Date expirationDate;
  private List<TokenPermission> scopes;
  
  public TokenScope getType() {
    return type;
  }
  public void setType(TokenScope type) {
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
  public List<TokenPermission> getPermissions() {
    return scopes;
  }
  public void setPermissions(List<TokenPermission> permissions) {
    this.scopes = permissions;
  }
}
