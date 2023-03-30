package io.boomerang.v4.model.ref;

import java.util.Date;

public class ChangeLog {

  private String userId;
  private String reason;
  private Date date;
  private String userName;

  public ChangeLog() {
  }

  public ChangeLog(String reason) {
    super();
    this.reason = reason;
    this.date = new Date();
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

}
