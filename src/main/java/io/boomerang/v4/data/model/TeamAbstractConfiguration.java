package io.boomerang.v4.data.model;

import io.boomerang.mongo.model.AbstractConfigurationProperty;

public class TeamAbstractConfiguration extends AbstractConfigurationProperty {

  private String id;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

}
