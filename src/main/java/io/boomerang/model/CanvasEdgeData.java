package io.boomerang.model;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.boomerang.model.enums.ref.ExecutionCondition;

public class CanvasEdgeData {
  
  String decisionCondition;
  
  ExecutionCondition executionCondition;
  
  @JsonIgnore
  private Map<String, Object> additionalProperties = new HashMap<>();
  
  public String getDecisionCondition() {
    return decisionCondition;
  }

  public void setDecisionCondition(String decisionCondition) {
    this.decisionCondition = decisionCondition;
  }

  public ExecutionCondition getExecutionCondition() {
    return executionCondition;
  }

  public void setExecutionCondition(ExecutionCondition executionCondition) {
    this.executionCondition = executionCondition;
  }

  @JsonAnyGetter
  public Map<String, Object> getAdditionalProperties() {
    return this.additionalProperties;
  }

  @JsonAnySetter
  public void setAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
  }
}
