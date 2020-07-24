package net.boomerangplatform.iam.model;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"execution_id", "message_id", "process_name", "activity_status", "datetime",
    "resource", "activity_name", "order"})
public class IAMRequest {

  @JsonProperty("execution_id")
  private String executionId;
  @JsonProperty("message_id")
  private String messageId;
  @JsonProperty("process_name")
  private String processName;
  @JsonProperty("activity_status")
  private IAMStatus activityStatus;
  @JsonProperty("datetime")
  private Date datetime;
  @JsonProperty("resource")
  private String resource;
  @JsonProperty("activity_name")
  private String activityName;
  @JsonProperty("order")
  private Integer order;
  @JsonIgnore
  private Map<String, Object> additionalProperties = new HashMap<>();

  @JsonProperty("execution_id")
  public String getExecutionId() {
    return executionId;
  }

  @JsonProperty("execution_id")
  public void setExecutionId(String executionId) {
    this.executionId = executionId;
  }

  @JsonProperty("message_id")
  public String getMessageId() {
    return messageId;
  }

  @JsonProperty("message_id")
  public void setMessageId(String messageId) {
    this.messageId = messageId;
  }

  @JsonProperty("process_name")
  public String getProcessName() {
    return processName;
  }

  @JsonProperty("process_name")
  public void setProcessName(String processName) {
    this.processName = processName;
  }

  @JsonProperty("activity_status")
  public IAMStatus getActivityStatus() {
    return activityStatus;
  }

  @JsonProperty("activity_status")
  public void setActivityStatus(IAMStatus activityStatus) {
    this.activityStatus = activityStatus;
  }

  @JsonProperty("datetime")
  public Date getDatetime() {
    return datetime;
  }

  @JsonProperty("datetime")
  public void setDatetime(Date datetime) {
    this.datetime = datetime;
  }

  @JsonProperty("resource")
  public String getResource() {
    return resource;
  }

  @JsonProperty("resource")
  public void setResource(String resource) {
    this.resource = resource;
  }

  @JsonProperty("activity_name")
  public String getActivityName() {
    return activityName;
  }

  @JsonProperty("activity_name")
  public void setActivityName(String activityName) {
    this.activityName = activityName;
  }

  @JsonProperty("order")
  public Integer getOrder() {
    return order;
  }

  @JsonProperty("order")
  public void setOrder(Integer order) {
    this.order = order;
  }

  @JsonAnyGetter
  public Map<String, Object> getAdditionalProperties() {
    return this.additionalProperties;
  }

  @JsonAnySetter
  public void setAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
  }

}
