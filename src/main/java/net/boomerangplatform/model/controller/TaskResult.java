package net.boomerangplatform.model.controller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/*
 * Partially replicates Tekton TaskResult but ensures that the SDK Model is not exposed
 * as the controllers model
 * 
 * Reference:
 * - io.fabric8.tekton.pipeline.v1beta1.TaskResult;
 */
@JsonIgnoreProperties
public class TaskResult {

  @JsonProperty("description")
  private String description;
  
  @JsonProperty("name")
  private String name;

  /**
   * No args constructor for use in serialization
   * 
   */
  public TaskResult() {
  }

  /**
   * 
   * @param name
   * @param description
   */
  public TaskResult(String description, String name) {
      super();
      this.description = description;
      this.name = name;
  }

  @JsonProperty("description")
  public String getDescription() {
      return description;
  }

  @JsonProperty("description")
  public void setDescription(String description) {
      this.description = description;
  }

  @JsonProperty("name")
  public String getName() {
      return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
      this.name = name;
  }
  
}
