package io.boomerang.security.model;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class TokenResponse {  
  
  private String id;
  private TokenType type;
  private String description;
  private Date creationDate;
  private Date expirationDate;
  private boolean valid;
  private String author;
  private List<TokenAccessScope> scopes = new LinkedList<>();

  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }
  public List<TokenAccessScope> getScopes() {
    return scopes;
  }
  public void setScopes(List<TokenAccessScope> scopes) {
    this.scopes = scopes;
  }
  public TokenType getType() {
    return type;
  }
  public void setType(TokenType type) {
    this.type = type;
  }
  public Date getCreationDate() {
    return creationDate;
  }
  public void setCreationDate(Date creationDate) {
    this.creationDate = creationDate;
  }
  public Date getExpirationDate() {
    return expirationDate;
  }
  public void setExpirationDate(Date expirationDate) {
    this.expirationDate = expirationDate;
  }
  public boolean isValid() {
    return valid;
  }
  public void setValid(boolean valid) {
    this.valid = valid;
  }
  public String getAuthor() {
    return author;
  }
  public void setAuthor(String author) {
    this.author = author;
  }
  public String getDescription() {
    return description;
  }
  public void setDescription(String description) {
    this.description = description;
  }
}
