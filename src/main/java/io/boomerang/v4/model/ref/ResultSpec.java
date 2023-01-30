package io.boomerang.v4.model.ref;

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
public class ResultSpec {

  @JsonProperty("description")
  private String description;
  
  @JsonProperty("name")
  private String name;

  /**
   * No args constructor for use in serialization
   * 
   */
  public ResultSpec() {
  }

  /**
   * 
   * @param name
   * @param description
   */
  public ResultSpec(String description, String name) {
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
