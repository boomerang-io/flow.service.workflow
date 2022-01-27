package io.boomerang.security.model;

import io.boomerang.mongo.model.TokenScope;

public class TeamToken extends Token {
  
  public TeamToken() {
    this.setScope(TokenScope.team);
  }
  
  private String teamId;
  public String getTeamId() {
    return teamId;
  }
  public void setTeamId(String teamId) {
    this.teamId = teamId;
  }

}
