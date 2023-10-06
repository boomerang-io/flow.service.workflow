package io.boomerang.model.ref;

public class TriggerEvent extends Trigger {

  private String type;
  private String subject;
  
  public String getType() {
    return type;
  }
  public void setType(String type) {
    this.type = type;
  }
  public String getSubject() {
    return subject;
  }
  public void setSubject(String subject) {
    this.subject = subject;
  }
}
