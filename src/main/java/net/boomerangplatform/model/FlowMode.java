package net.boomerangplatform.model;

public enum FlowMode {

  IBMSERVICESESSENTIALS("ibm-services-essentials"), STANDALONE("standalone"), EMBEDDED("embedded");

  private String mode;

  FlowMode(String mode) {
    this.mode = mode;
  }

  public String getMode() {
    return mode;
  }

  public void setMode(String mode) {
    this.mode = mode;
  }

}
