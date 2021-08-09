package io.boomerang.model;

public class CreateTeamTokenRequest extends CreateTokenRequest {
  private String teamId;

  public String getTeamId() {
    return teamId;
  }

  public void setTeamId(String teamId) {
    this.teamId = teamId;
  }
  

}
