package net.boomerangplatform.model.tekton;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Param{
    private String name;
    private String type;
    private String description;
    
    @JsonProperty("default")
    private String defaultString;
    
    public String getDefaultString() {
      return defaultString;
    }
    public void setDefaultString(String defaultString) {
      this.defaultString = defaultString;
    }
    public String getName() {
      return name;
    }
    public void setName(String name) {
      this.name = name;
    }
    public String getType() {
      return type;
    }
    public void setType(String type) {
      this.type = type;
    }
    public String getDescription() {
      return description;
    }
    public void setDescription(String description) {
      this.description = description;
    }
}
