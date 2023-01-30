package io.boomerang.v4.model.ref;

public class RunResult {
  
  private String name;
  private String description;
  private Object value;
  
  public RunResult() {
  }
  
  public RunResult(String name, Object value) {
    this.name = name;
    this.value = value;
  }
  
  public RunResult(String name, String description, Object value) {
    this.name = name;
    this.description = description;
    this.value = value;
  }
  
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
  public Object getValue() {
    return value;
  }
  public void setValue(Object value) {
    this.value = value;
  } 

}
