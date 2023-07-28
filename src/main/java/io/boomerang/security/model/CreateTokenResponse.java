package io.boomerang.security.model;

import java.util.Date;

public class CreateTokenResponse {

//  @JsonIgnore
  private String id;
  private AuthType type;
  private String token;
  private Date expirationDate;
  
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }
  public AuthType getType() {
    return type;
  }
  public void setType(AuthType type) {
    this.type = type;
  }
  public String getToken() {
    return token;
  }
  public void setToken(String token) {
    this.token = token;
  }
  public Date getExpirationDate() {
    return expirationDate;
  }
  public void setExpirationDate(Date expirationDate) {
    this.expirationDate = expirationDate;
  }
}
