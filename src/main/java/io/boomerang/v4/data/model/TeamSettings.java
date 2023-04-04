package io.boomerang.v4.data.model;

import java.util.LinkedList;
import java.util.List;

public class TeamSettings {

  private List<TeamAbstractConfiguration> parameters = new LinkedList<>();

  public List<TeamAbstractConfiguration> getParameters() {
    return parameters;
  }

  public void setParameters(List<TeamAbstractConfiguration> parameters) {
    this.parameters = parameters;
  }
}
