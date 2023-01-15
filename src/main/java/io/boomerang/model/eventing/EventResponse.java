package io.boomerang.model.eventing;

public class EventResponse {

  private String activityId;

  private Integer statusCode;

  private String statusMessage;

  public EventResponse() {
  }

  public EventResponse(String activityId, Integer statusCode, String statusMessage) {
    this.activityId = activityId;
    this.statusCode = statusCode;
    this.statusMessage = statusMessage;
  }

  public String getActivityId() {
    return activityId;
  }

  public void setActivityId(String activityId) {
    this.activityId = activityId;
  }

  public Integer getStatusCode() {
    return statusCode;
  }

  public void setStatusCode(Integer statusCode) {
    this.statusCode = statusCode;
  }

  public String getStatusMessage() {
    return statusMessage;
  }

  public void setStatusMessage(String statusMessage) {
    this.statusMessage = statusMessage;
  }
}
