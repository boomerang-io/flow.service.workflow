package net.boomerangplatform.model.controller;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskResponse extends Response {

  private Map<String, String> output = new HashMap<>();

  public TaskResponse() {
    // Do nothing
  }

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
