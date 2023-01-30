package io.boomerang.error;

import org.springframework.http.HttpStatus;

public enum BoomerangError {
  
  /** 
   * Add reusable error list here. Map to messages.properties
   * 
   * @param code the internal codes documented for end users and systems to integrate
   * @param reason Key to look up in messages.properties
   * @param HttpStatus status to respond as
   * 
   * Last updated 2023/01/30 - keep this date updated as this is used across the services.
   * 
   * */
  TEAM_NAME_ALREADY_EXISTS(100, "TEAM_NAME_ALREADY_EXISTS", HttpStatus.BAD_REQUEST),
  TOO_MANY_REQUESTS(429, "TOO_MANY_REQUESTS", HttpStatus.TOO_MANY_REQUESTS),
  PERMISSION_DENIED(403, "PERMISSION_DENIED", HttpStatus.UNAUTHORIZED),
  IMPORT_WORKFLOW_FAILED(400, "IMPORT_WORKFLOW_FAILED", HttpStatus.BAD_REQUEST),
  WORKFLOW_TRIGGER_DISABLED(429, "WORKFLOW_TRIGGER_DISABLED", HttpStatus.UNAUTHORIZED),
  WORKFLOW_TEAM_INACTIVE(429, "WORKFLOW_TEAM_INACTIVE", HttpStatus.UNAUTHORIZED),
  QUERY_INVALID_FILTERS(1001, "QUERY_INVALID_FILTERS", HttpStatus.BAD_REQUEST),
  WORKFLOW_INVALID_REF(1101, "WORKFLOW_INVALID_REFERENCE", HttpStatus.BAD_REQUEST),
  WORKFLOW_NON_UNIQUE_TASK_NAME(1102, "WORKFLOW_NON_UNIQUE_TASK_NAME", HttpStatus.BAD_REQUEST),
  WORKFLOW_RUN_INVALID_REF(1201, "WORKFLOW_RUN_INVALID_REFERENCE", HttpStatus.BAD_REQUEST),
  WORKFLOW_RUN_INVALID_REQ(1202, "WORKFLOW_RUN_INVALID_REQUIREMENT", HttpStatus.BAD_REQUEST),
  WORKFLOW_RUN_INVALID_DEPENDENCY(1203, "WORKFLOW_RUN_INVALID_DEPENDENCY", HttpStatus.BAD_REQUEST),
  WORKFLOW_REVISION_NOT_FOUND(1301, "WORKFLOW_REVISION_NOT_FOUND", HttpStatus.CONFLICT),
  TASK_RUN_INVALID_REF(1401, "TASK_RUN_INVALID_REFERENCE", HttpStatus.BAD_REQUEST),
  TASK_RUN_INVALID_REQ(1402, "TASK_RUN_INVALID_REQUIREMENT", HttpStatus.BAD_REQUEST),
  TASK_RUN_INVALID_END_STATUS(1403, "TASK_RUN_INVALID_END_STATUS", HttpStatus.BAD_REQUEST),
  TASK_TEMPLATE_INVALID_REF(1501, "TASK_TEMPLATE_INVALID_REFERENCE", HttpStatus.BAD_REQUEST),
  TASK_TEMPLATE_ALREADY_EXISTS(1502, "TASK_TEMPLATE_ALREADY_EXISTS", HttpStatus.BAD_REQUEST),
  TASK_TEMPLATE_INVALID_NAME(1503, "TASK_TEMPLATE_INVALID_NAME", HttpStatus.BAD_REQUEST),
  TASK_TEMPLATE_INVALID_SCOPE_CHANGE(1504, "TASK_TEMPLATE_INVALID_SCOPE_CHANGE", HttpStatus.BAD_REQUEST);
  
  private final int code;
  private final String reason;
  private final HttpStatus status;

  public int getCode() {
    return code;
  }

  public String getReason() {
    return reason;
  }

  public HttpStatus getStatus() {
    return status;
  }

  private BoomerangError(int code, String reason, HttpStatus status) {
    this.code = code;
    this.reason = reason;
    this.status = status;
  }
}
