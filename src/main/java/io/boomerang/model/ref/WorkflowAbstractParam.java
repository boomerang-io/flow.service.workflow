package io.boomerang.model.ref;

import io.boomerang.model.AbstractParam;

//TODO: figure out if we need this - can we just use the name jsonPath on the param.
public class WorkflowAbstractParam extends AbstractParam {
  
  private String jsonPath;

  public String getJsonPath() {
    return jsonPath;
  }

  public void setJsonPath(String jsonPath) {
    this.jsonPath = jsonPath;
  }
  
}
