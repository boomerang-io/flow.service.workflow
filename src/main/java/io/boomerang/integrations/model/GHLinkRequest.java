package io.boomerang.integrations.model;

public class GHLinkRequest {

  private Integer installationId;

  private String team;

  public Integer getInstallationId() {
    return installationId;
  }

  public void setInstallationId(Integer installationId) {
    this.installationId = installationId;
  }

  public String getTeam() {
    return team;
  }

  public void setTeam(String team) {
    this.team = team;
  }
}
