package io.boomerang.model.controller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/*
 * Partially replicates Tekton TaskRunResult but ensures that the SDK Model is not exposed
 * as the controllers model
 * 
 * Reference:
 * - io.fabric8.tekton.pipeline.v1beta1.TaskRunResult;
 */
@JsonIgnoreProperties
public class TaskResponseResult {
  
  @JsonProperty("name")
  private String name;

  @JsonProperty("value")
  private String value;

  /**
   * No args constructor for use in serialization
   * 
   */
  public TaskResponseResult() {
  }

  /**
   * 
   * @param name
   * @param description
   */
  public TaskResponseResult(String name, String value) {
      super();
      this.name = name;
      this.value = value;
  }

  @JsonProperty("name")
  public String getName() {
      return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
      this.name = name;
  }

  @JsonProperty("value")
  public String getValue() {
      return value;
  }

  @JsonProperty("value")
  public void setValue(String value) {
      this.value = value;
  }
  
}
