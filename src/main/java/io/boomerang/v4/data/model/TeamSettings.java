package io.boomerang.v4.data.model;

import java.util.LinkedList;
import java.util.List;

public class TeamSettings {

  private List<TeamParameter> parameters = new LinkedList<>();

  public List<TeamParameter> getParameters() {
    return parameters;
  }

  public void setParameters(List<TeamParameter> parameters) {
    this.parameters = parameters;
  }
}
