
package net.boomerangplatform.model.projectstormv5;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"path", "script", "taskName", "duration"})
public class Inputs {

  @JsonProperty("path")
  private String path;
  @JsonProperty("script")
  private String script;
  @JsonProperty("taskName")
  private String taskName;
  @JsonProperty("duration")
  private String duration;
  @JsonIgnore
  private Map<String, Object> additionalProperties = new HashMap<>();

  @JsonProperty("path")
  public String getPath() {
    return path;
  }

  @JsonProperty("path")
  public void setPath(String path) {
    this.path = path;
  }

  @JsonProperty("script")
  public String getScript() {
    return script;
  }

  @JsonProperty("script")
  public void setScript(String script) {
    this.script = script;
  }

  @JsonProperty("taskName")
  public String getTaskName() {
    return taskName;
  }

  @JsonProperty("taskName")
  public void setTaskName(String taskName) {
    this.taskName = taskName;
  }

  @JsonProperty("duration")
  public String getDuration() {
    return duration;
  }

  @JsonProperty("duration")
  public void setDuration(String duration) {
    this.duration = duration;
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
