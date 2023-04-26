package io.boomerang.model;

import java.util.List;
import io.boomerang.v4.data.entity.TeamEntity;
import io.boomerang.v4.data.entity.UserEntity;

public class FlowUserProfile extends UserEntity {

  private List<TeamEntity> userTeams;

  public List<TeamEntity> getUserTeams() {
    return userTeams;
  }

  public void setUserTeams(List<TeamEntity> userTeams) {
    this.userTeams = userTeams;
  }

}
