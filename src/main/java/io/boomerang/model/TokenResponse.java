package io.boomerang.model;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import io.boomerang.security.model.TokenAccessScope;
import io.boomerang.security.model.TokenType;

public class TokenResponse {  
  
  private String id;
  private List<TokenAccessScope> scopes = new LinkedList<>();
  private TokenType type;
  private Date creationDate;
  private Date expirationDate;
  private boolean valid;
  private String author;
  
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
}
