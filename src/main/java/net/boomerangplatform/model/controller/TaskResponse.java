package net.boomerangplatform.model.controller;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskResponse extends Response {

  private Map<String, String> results = new HashMap<>();

  public TaskResponse() {
    // Do nothing
  }

  public TaskResponse(String code, String desc, Map<String, String> results) {
    super(code, desc);
    this.results = results;
  }

  public Map<String, String> getResults() {
    return results;
  }

  public void setResults(Map<String, String> results) {
    this.results = results;
  }
}
