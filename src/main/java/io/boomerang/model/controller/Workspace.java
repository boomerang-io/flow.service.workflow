package io.boomerang.model.controller;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties
public class Workspace {

  private String name;

  private String id;

  @JsonProperty("labels")
  private Map<String, String> labels = new HashMap<>();
  
  private String size;
  
  /*
   * Leave blank for clusters default to be used
   */
  private String className;
  
  /*
   * Supported values: 
   * - ReadOnlyMany
   * - ReadWriteMany
   * - ReadWriteOnce
   */
  private String accessMode;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Map<String, String> getLabels() {
    return labels;
  }

  public void setLabels(Map<String, String> labels) {
    this.labels = labels;
  }

  public void setLabel(String name, String value) {
    this.labels.put(name, value);
  }

  public String getSize() {
    return size;
  }

  public void setSize(String size) {
    this.size = size;
  }

  public String getClassName() {
    return className;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public String getAccessMode() {
    return accessMode;
  }

  public void setAccessMode(String accessMode) {
    this.accessMode = accessMode;
  }
}
