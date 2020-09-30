package net.boomerangplatform.mongo.model;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class Audit {

  private String approverId;
  private String approverEmail;
  private String approverName;

  public String getApproverEmail() {
    return approverEmail;
  }

  public void setApproverEmail(String approverEmail) {
    this.approverEmail = approverEmail;
  }

  public String getApproverName() {
    return approverName;
  }

  public void setApproverName(String approverName) {
    this.approverName = approverName;
  }

  private Date actionDate;

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
}
