package io.boomerang.v4.model.ref;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.boomerang.v4.model.enums.ref.ParamType;

public class RunParam {
  
  private String name;
  private Object value;
  @JsonIgnore
  private ParamType type;
  
  protected RunParam() {
  }

  public RunParam(String name, Object value) {
    this.name = name;
    this.value = value;
  }

  public RunParam(String name, Object value, ParamType type) {
    this.name = name;
    this.type = type;
    this.value = value;
  }

  @Override
  public String toString() {
    return "RunParam [name=" + name + ", type=" + type + ", value=" + value + "]";
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Object getValue() {
    return value;
  }

  public void setValue(Object value) {
    this.value = value;
  }

  public ParamType getType() {
    return type;
  }

  public void setType(ParamType type) {
    this.type = type;
  }
}
