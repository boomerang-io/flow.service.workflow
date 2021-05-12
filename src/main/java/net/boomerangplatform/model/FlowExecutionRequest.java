package net.boomerangplatform.model;

import java.util.Map;

public class FlowExecutionRequest {

  private boolean applyQuotas;
  
  private Map<String, String> properties;

  public Map<String, String> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

  public boolean isApplyQuotas() {
    return applyQuotas;
  }

  public void setApplyQuotas(boolean applyQuotas) {
    this.applyQuotas = applyQuotas;
  }

}
