package io.boomerang.mongo.model;

import io.boomerang.v4.model.AbstractParam;

public class WorkflowProperty extends AbstractParam {
  
  private String jsonPath;

  public String getJsonPath() {
    return jsonPath;
  }

  public void setJsonPath(String jsonPath) {
    this.jsonPath = jsonPath;
  }
  
}
