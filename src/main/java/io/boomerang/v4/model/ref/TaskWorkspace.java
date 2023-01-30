package io.boomerang.v4.model.ref;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties
public class TaskWorkspace {

  private String name;

  private String type;

  private boolean optional = false;
  
  private String mountPath;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public boolean isOptional() {
    return optional;
  }

  public void setOptional(boolean optional) {
    this.optional = optional;
  }

  public String getMountPath() {
    return mountPath;
  }

  public void setMountPath(String mountPath) {
    this.mountPath = mountPath;
  }
}
