package net.boomerangplatform.model.profile;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LeavePlatformResponse {

  private String requestId;
  private String requesterMessage;
  private Date requestCreationDate;
  private String status;
  private String message;

  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  public String getRequesterMessage() {
    return requesterMessage;
  }

  public void setRequesterMessage(String requesterMessage) {
    this.requesterMessage = requesterMessage;
  }

  public Date getRequestCreationDate() {
    return requestCreationDate;
  }

  public void setRequestCreationDate(Date requestCreationDate) {
    this.requestCreationDate = requestCreationDate;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
