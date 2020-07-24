package net.boomerangplatform.mongo.model;

public class Webhook {

  private Boolean enable;
  private String token;

  public Boolean getEnable() {
    return enable;
  }

  public void setEnable(Boolean enable) {
    this.enable = enable;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }


}
