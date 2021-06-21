package net.boomerangplatform.model.controller;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties
public class Workspace {

  private String name;

  private String id;

  @JsonProperty("storage")
  private Storage storage;

  @JsonProperty("labels")
  private Map<String, String> labels = new HashMap<>();

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getId() {
    return id;
  }

  public void id(String id) {
    this.id = id;
  }

  public Storage getStorage() {
    return storage;
  }

  public void setStorage(Storage storage) {
    this.storage = storage;
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
}
