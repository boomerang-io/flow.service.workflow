package net.boomerangplatform.model;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskResponse extends Response {

  @JsonProperty("output")
  private Map<String, String> output = new HashMap<>();

  public TaskResponse() {}

  public TaskResponse(String code, String desc, Map<String, String> output) {
    super(code, desc);
    this.output = output;
  }

  public Map<String, String> getOutput() {
    return output;
  }

  public void setOutput(Map<String, String> output) {
    this.output = output;
  }
}
