package io.boomerang.security.model;

import io.boomerang.mongo.model.TokenScope;

public class TeamToken extends Token {
  
  private String teamId;
  
  public TeamToken() {
    this.setScope(TokenScope.team);
  }
  
  public TeamToken(String teamId) {
    super();
    this.teamId = teamId;
    this.setScope(TokenScope.team);
  }

  public String getTeamId() {
    return teamId;
  }
  public void setTeamId(String teamId) {
    this.teamId = teamId;
  }
}
