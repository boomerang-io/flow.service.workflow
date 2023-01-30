package io.boomerang.v4.model.ref;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties
public class WorkflowWorkspace {

  private String name;
  
  private String description;

  private String type;

  private boolean optional = false;
  
  private Object spec;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public boolean isOptional() {
    return optional;
  }

  public void setOptional(boolean optional) {
    this.optional = optional;
  }

  public Object getSpec() {
    return spec;
  }

  public void setSpec(Object spec) {
    this.spec = spec;
  }
}
