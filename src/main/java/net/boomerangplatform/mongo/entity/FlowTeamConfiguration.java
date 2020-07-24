package net.boomerangplatform.mongo.entity;

import net.boomerangplatform.mongo.model.AbstractConfigurationProperty;

public class FlowTeamConfiguration extends AbstractConfigurationProperty {

  private String id;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

}
