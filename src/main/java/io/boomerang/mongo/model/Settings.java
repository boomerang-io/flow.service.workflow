package io.boomerang.mongo.model;

import java.util.List;
import io.boomerang.mongo.entity.FlowTeamConfiguration;

public class Settings {

  private List<FlowTeamConfiguration> properties;

  public List<FlowTeamConfiguration> getProperties() {
    return properties;
  }

  public void setProperties(List<FlowTeamConfiguration> properties) {
    this.properties = properties;
  }
}
