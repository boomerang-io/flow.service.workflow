package io.boomerang.model.tekton;

public class Metadata {
  private String name;
  private Labels labels;
  private Annotations annotations;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Labels getLabels() {
    return labels;
  }

  public void setLabels(Labels labels) {
    this.labels = labels;
  }

  public Annotations getAnnotations() {
    return annotations;
  }

  public void setAnnotations(Annotations annotations) {
    this.annotations = annotations;
  }
}
