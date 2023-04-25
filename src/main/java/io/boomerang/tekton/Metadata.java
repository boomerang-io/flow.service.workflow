package io.boomerang.tekton;

import java.util.HashMap;
import java.util.Map;

public class Metadata {
  private String name;
  private Map<String, String> labels = new HashMap<String, String>();
  private Map<String, Object> annotations = new HashMap<String, Object>();

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Map<String, String> getLabels() {
    return labels;
  }

  public void setLabels(Map<String, String> labels) {
    this.labels = labels;
  }

  public Map<String, Object> getAnnotations() {
    return annotations;
  }

  public void setAnnotations(Map<String, Object> annotations) {
    this.annotations = annotations;
  }
}
