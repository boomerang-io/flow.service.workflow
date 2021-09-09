package io.boomerang.mongo.model;

import java.util.List;
import io.boomerang.model.teams.ApproverUser;

public class ApproverGroup {

  private String id;
  private String name;
  private List<ApproverUser> approvers;
  
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }
  public List<ApproverUser> getApprovers() {
    return approvers;
  }
  public void setApprovers(List<ApproverUser> approvers) {
    this.approvers = approvers;
  }

  
}
