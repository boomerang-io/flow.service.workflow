package io.boomerang.model.ref;

public class Trigger {

  private Boolean enable = Boolean.FALSE;

  public Trigger() {
  } 
  
  public Trigger(Boolean enable) {
    this.enable = enable;
  }

  public Boolean getEnable() {
    return enable;
  }

  public void setEnable(Boolean enable) {
    this.enable = enable;
  }
}
