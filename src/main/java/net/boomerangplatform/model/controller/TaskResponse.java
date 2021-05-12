package net.boomerangplatform.model.controller;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.fabric8.tekton.pipeline.v1beta1.TaskRunResult;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskResponse extends Response {

  private List<TaskRunResult> results = new ArrayList<>();

  public TaskResponse() {
    // Do nothing
  }

  public TaskResponse(String code, String desc, List<TaskRunResult> results) {
    super(code, desc);
    this.results = results;
  }

  public List<TaskRunResult> getResults() {
    return results;
  }

  public void setResults(List<TaskRunResult> results) {
    this.results = results;
  }
}