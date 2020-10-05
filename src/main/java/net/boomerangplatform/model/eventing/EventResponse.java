package net.boomerangplatform.model.eventing;

public class EventResponse {
      
      private String activityId;
      
      private Integer statusCode;
      
      private String statusMessage;

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
