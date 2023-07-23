package io.boomerang.v4.model;

import java.util.List;

public class TeamMemberRequest {
  
  private List<UserSummary> users;

  public List<UserSummary> getUsers() {
    return users;
  }

  public void setUsers(List<UserSummary> users) {
    this.users = users;
  }
}
