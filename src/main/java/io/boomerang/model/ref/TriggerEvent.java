package io.boomerang.model.ref;

public class TriggerEvent extends Trigger {

  private String token;
  private String topic;

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public String getTopic() {
    return topic;
  }

  public void setTopic(String topic) {
    this.topic = topic;
  }
}
