package net.boomerangplatform.mongo.model;

public class FlowProperty extends AbstractConfigurationProperty {
  
  public String jsonPath;

  public String getJsonPath() {
    return jsonPath;
  }

  public void setJsonPath(String jsonPath) {
    this.jsonPath = jsonPath;
  }
  
}
