
package net.boomerangplatform.mongo.model.next;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.boomerangplatform.mongo.model.CoreProperty;
import net.boomerangplatform.mongo.model.TaskType;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"taskId", "type", "label", "templateId", "dependencies", "properties",
    "metadata"})
public class DAGTask {

  @JsonProperty("taskId")
  private String taskId;
  @JsonProperty("type")
  private TaskType type;
  @JsonProperty("label")
  private String label;
  @JsonProperty("templateId")
  private String templateId;

  @JsonProperty("templateVersion")
  private Integer templateVersion;

  public Integer getTemplateVersion() {
    return templateVersion;
  }

  public void setTemplateVersion(Integer templateVersion) {
    this.templateVersion = templateVersion;
  }

  private String decisionValue;

  @JsonProperty("dependencies")
  private List<Dependency> dependencies = null;
  @JsonProperty("properties")

  private List<CoreProperty> properties;

  public List<CoreProperty> getProperties() {
    return properties;
  }

  public void setProperties(List<CoreProperty> properties) {
    this.properties = properties;
  }

  @JsonProperty("metadata")
  private Map<String, Object> metadata;

  @JsonIgnore
  private Map<String, Object> additionalProperties = new HashMap<>();

  @JsonProperty("taskId")
  public String getTaskId() {
    return taskId;
  }

  @JsonProperty("taskId")
  public void setId(String taskId) {
    this.taskId = taskId;
  }

  @JsonProperty("type")
  public TaskType getType() {
    return type;
  }

  @JsonProperty("type")
  public void setType(TaskType type) {
    this.type = type;
  }

  @JsonProperty("label")
  public String getLabel() {
    return label;
  }

  @JsonProperty("label")
  public void setLabel(String label) {
    this.label = label;
  }

  @JsonProperty("templateId")
  public String getTemplateId() {
    return templateId;
  }

  @JsonProperty("templateId")
  public void setTemplateId(String templateId) {
    this.templateId = templateId;
  }

  @JsonProperty("dependencies")
  public List<Dependency> getDependencies() {
    return dependencies;
  }

  @JsonProperty("dependencies")
  public void setDependencies(List<Dependency> dependencies) {
    this.dependencies = dependencies;
  }


  @JsonProperty("metadata")
  public Map<String, Object> getMetadata() {
    return metadata;
  }

  @JsonProperty("metadata")
  public void setMetadata(Map<String, Object> metadata) {
    this.metadata = metadata;
  }

  @JsonAnyGetter
  public Map<String, Object> getAdditionalProperties() {
    return this.additionalProperties;
  }

  @JsonAnySetter
  public void setAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
  }

  public String getDecisionValue() {
    return decisionValue;
  }

  public void setDecisionValue(String decisionValue) {
    this.decisionValue = decisionValue;
  }

}
