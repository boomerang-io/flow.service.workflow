package io.boomerang.mongo.model;

import java.util.List;

public class ApproverGroup {

  private String id;
  private String name;
  private List<String> members;
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
  public List<String> getMembers() {
    return members;
  }
  public void setMembers(List<String> members) {
    this.members = members;
  }
  
}
