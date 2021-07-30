package net.boomerangplatform.model;

import java.util.Date;
public class CreateTokenRequest {
  
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
  private Date expiryDate;
  private String description;
}
