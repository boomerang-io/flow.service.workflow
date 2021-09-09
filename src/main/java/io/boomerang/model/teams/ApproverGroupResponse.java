package io.boomerang.model.teams;

import java.util.List;

public class ApproverGroupResponse {
  

  public String getGroupId() {
    return groupId;
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  public String getGroupName() {
    return groupName;
  }

  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }

  public String getTeamId() {
    return teamId;
  }

  public void setTeamId(String teamId) {
    this.teamId = teamId;
  }

  public String getTeamName() {
    return teamName;
  }

  public void setTeamName(String teamName) {
    this.teamName = teamName;
  }

  public List<ApproverUser> getApprovers() {
    return approvers;
  }

  public void setApprovers(List<ApproverUser> approvers) {
    this.approvers = approvers;
  }

  private String groupId;
  private String groupName;
  private String teamId;
  private String teamName;
  
  private List<ApproverUser> approvers;

}
