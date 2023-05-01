package io.boomerang.security.model;

import java.util.Date;
import java.util.List;

public class CreateTokenRequest {

  private TokenType type;
  private String name;
  private String owner;
  private String description;
  private Date expirationDate;
  private List<TokenScope> scopes;
  
  public TokenType getType() {
    return type;
  }
  public void setType(TokenType type) {
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
  public String getOwner() {
    return owner;
  }
  public void setOwner(String owner) {
    this.owner = owner;
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
  public List<TokenScope> getScopes() {
    return scopes;
  }
  public void setScopes(List<TokenScope> scopes) {
    this.scopes = scopes;
  }
}
