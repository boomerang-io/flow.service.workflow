package io.boomerang.security.model;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class CreateTokenResponse {

//  @JsonIgnore
  private String id;
  private TokenScope type;
  private String value;
  private Date expirationDate;
  
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }
  public TokenScope getType() {
    return type;
  }
  public void setType(TokenScope type) {
    this.type = type;
  }
  public String getValue() {
    return value;
  }
  public void setValue(String value) {
    this.value = value;
  }
  public Date getExpirationDate() {
    return expirationDate;
  }
  public void setExpirationDate(Date expirationDate) {
    this.expirationDate = expirationDate;
  }
}
