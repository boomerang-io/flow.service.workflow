package io.boomerang.v4.model;

public class CreateTeamRequest {

  private String name;
  private String externalRef;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getExternalRef() {
    return externalRef;
  }

  public void setExternalRef(String externalRef) {
    this.externalRef = externalRef;
  }
}
