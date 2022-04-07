package io.boomerang.model.teams;

import java.util.List;

public class CreateApproverGroupRequest {
  
  private String groupName;
  
  private List<ApproverUser> approvers;

  public String getGroupName() {
    return groupName;
  }

  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }

  public List<ApproverUser> getApprovers() {
    return approvers;
  }

  public void setApprovers(List<ApproverUser> approvers) {
    this.approvers = approvers;
  }

}
