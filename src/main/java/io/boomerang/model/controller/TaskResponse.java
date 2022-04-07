package io.boomerang.model.controller;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskResponse extends Response {

  private List<TaskResponseResult> results = new ArrayList<>();

  public TaskResponse() {
    // Do nothing
  }

  public TaskResponse(String code, String desc, List<TaskResponseResult> results) {
    super(code, desc);
    this.results = results;
  }

  public List<TaskResponseResult> getResults() {
    return results;
  }

  public void setResults(List<TaskResponseResult> results) {
    this.results = results;
  }
}
