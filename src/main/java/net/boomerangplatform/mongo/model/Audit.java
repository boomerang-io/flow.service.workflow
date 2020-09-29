package net.boomerangplatform.mongo.model;

import java.util.Date;

public class Audit {
  
  private String approverId;
  private Date actionDate;
  private Boolean result;
  public String getApproverId() {
    return approverId;
  }
  public void setApproverId(String approverId) {
    this.approverId = approverId;
  }
  public Date getActionDate() {
    return actionDate;
  }
  public void setActionDate(Date actionDate) {
    this.actionDate = actionDate;
  }
  public Boolean getResult() {
    return result;
  }
  public void setResult(Boolean result) {
    this.result = result;
  }

}
