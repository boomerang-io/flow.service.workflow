package io.boomerang.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;


public class CronValidationResponse {

  private boolean vaild;
  private String cron;
  @JsonInclude(Include.NON_NULL)
  private String message;

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
