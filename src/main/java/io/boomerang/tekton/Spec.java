package io.boomerang.tekton;

import java.time.Duration;
import java.util.List;
import io.boomerang.model.Result;
import io.boomerang.v4.model.ref.ResultSpec;

public class Spec {

  private String description;
  private List<Param> params;
  private List<Step> steps;
  private Duration timeout;
  private List<ResultSpec> results;
  
  public List<Step> getSteps() {
    return steps;
  }

  public void setSteps(List<Step> steps) {
    this.steps = steps;
  }

  public List<Param> getParams() {
    return params;
  }

  public void setParams(List<Param> params) {
    this.params = params;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public List<ResultSpec> getResults() {
    return results;
  }

  public void setResults(List<ResultSpec> results) {
    this.results = results;
  }

  public Duration getTimeout() {
    return timeout;
  }

  public void setTimeout(Duration timeout) {
    this.timeout = timeout;
  }
}
