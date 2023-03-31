package io.boomerang.model;

import java.util.Date;
public class CreateTokenRequest {
  private String ref;
  private Date expiryDate;
  private String description;

  public String getRef() {
    return ref;
  }
  public void setRef(String ref) {
    this.ref = ref;
  }
  public Date getExpiryDate() {
    return expiryDate;
  }
  public void setExpiryDate(Date expiryDate) {
    this.expiryDate = expiryDate;
  }
  public String getDescription() {
    return description;
  }
  public void setDescription(String description) {
    this.description = description;
  }
}
