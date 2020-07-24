package net.boomerangplatform.model;

import java.util.Date;

public class RevisionResponse {

  private Date date;
  private String reason;
  private String revisionId;

  private String userId;
  private String userName;
  private long version;
  private String workflowId;

  public Date getDate() {
    return date;
  }

  public String getRevisionId() {
    return revisionId;
  }

  public String getUserId() {
    return userId;
  }

  public String getUserName() {
    return userName;
  }

  public long getVersion() {
    return version;
  }

  public String getWorkflowId() {
    return workflowId;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  public void setRevisionId(String revisionId) {
    this.revisionId = revisionId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public void setVersion(long version) {
    this.version = version;
  }

  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }
}
