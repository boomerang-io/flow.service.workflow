package io.boomerang.model;

public class CronValidationResponse {

  boolean vaild;
  String cron;

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


}
