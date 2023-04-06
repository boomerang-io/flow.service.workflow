package io.boomerang.v4.model;

import java.util.List;

public class ApproverGroupCreateRequest {
  
  private String name;
  
  private List<String> approvers;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<String> getApprovers() {
    return approvers;
  }

  public void setApprovers(List<String> approvers) {
    this.approvers = approvers;
  }

}
