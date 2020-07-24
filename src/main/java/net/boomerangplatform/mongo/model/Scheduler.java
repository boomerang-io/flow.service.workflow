package net.boomerangplatform.mongo.model;

public class Scheduler {

  private Boolean enable;
  private String schedule;
  private String timezone;
  private Boolean advancedCron;

  public Boolean getEnable() {
    return enable;
  }

  public void setEnable(Boolean enable) {
    this.enable = enable;
  }

  public String getSchedule() {
    return schedule;
  }

  public void setSchedule(String schedule) {
    this.schedule = schedule;
  }

  public String getTimezone() {
    return timezone;
  }

  public void setTimezone(String timezone) {
    this.timezone = timezone;
  }

  public Boolean getAdvancedCron() {
    return advancedCron;
  }

  public void setAdvancedCron(Boolean advancedCron) {
    this.advancedCron = advancedCron;
  }

}
