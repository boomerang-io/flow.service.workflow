package io.boomerang.security.model;

import io.boomerang.mongo.model.TokenScope;

public class TeamToken extends Token {
  
  private String teamId;
  
  public TeamToken(String teamId) {
    super();
    this.setScope(TokenScope.team);
    this.teamId = teamId;
  }
  
  public String getTeamId() {
    return teamId;
  }
  public void setTeamId(String teamId) {
    this.teamId = teamId;
  }

}
