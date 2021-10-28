package io.boomerang.model;

public class CronValidationResponse {

  boolean vaild;
  String cron;
  String message;

  public boolean isVaild() {
    return vaild;
  }

  public void setVaild(boolean vaild) {
    this.vaild = vaild;
  }

  public String getCron() {
    return cron;
  }

  public void setCron(String cron) {
    this.cron = cron;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

}
