package io.boomerang.model;

import java.util.List;
import java.util.Map;
import io.boomerang.mongo.model.KeyValuePair;

public class FlowExecutionRequest {

  private List<KeyValuePair> labels;
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

  public List<KeyValuePair> getLabels() {
    return labels;
  }

  public void setLabels(List<KeyValuePair> labels) {
    this.labels = labels;
  }

}
