package io.boomerang.v4.model.ref;

import io.boomerang.v4.model.enums.ref.ParamType;

public class ParamSpec {
  
  private String name;
  private ParamType type;
  private String description;
  private Object defaultValue;
  
  @Override
  public String toString() {
    return "ParamSpec [name=" + name + ", type=" + type + ", description=" + description
        + ", defaultValue=" + defaultValue + "]";
  }

  public String getName() {
    return name;
  }

  public ParamType getType() {
    return type;
  }

  public void setType(ParamType type) {
    this.type = type;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Object getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(Object defaultValue) {
    this.defaultValue = defaultValue;
  }

  public void setName(String name) {
    this.name = name;
  }
}
