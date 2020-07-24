package net.boomerangplatform.mongo.model.next;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.boomerangplatform.mongo.model.WorkflowExecutionCondition;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"taskId", "executionCondition"})
public class Dependency {

  private boolean conditionalExecution;

  @JsonProperty("taskId")
  private String taskId;

  private String switchCondition;

  @JsonProperty("executionCondition")
  private WorkflowExecutionCondition executionCondition;

  @JsonIgnore
  private Map<String, Object> additionalProperties = new HashMap<>();

  @JsonProperty("metadata")
  private Map<String, Object> metadata;

  public Map<String, Object> getMetadata() {
    return metadata;
  }

  public void setMetadata(Map<String, Object> metadata) {
    this.metadata = metadata;
  }

  @JsonProperty("taskId")
  public String getTaskId() {
    return taskId;
  }

  @JsonProperty("taskId")
  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }

  @JsonProperty("executionCondition")
  public WorkflowExecutionCondition getExecutionCondition() {
    return executionCondition;
  }

  @JsonProperty("executionCondition")
  public void setExecutionCondition(WorkflowExecutionCondition executionCondition) {
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

  public String getSwitchCondition() {
    return switchCondition;
  }

  public void setSwitchCondition(String switchCondition) {
    this.switchCondition = switchCondition;
  }

  public boolean isConditionalExecution() {
    return conditionalExecution;
  }

  public void setConditionalExecution(boolean conditionalExecution) {
    this.conditionalExecution = conditionalExecution;
  }

}
