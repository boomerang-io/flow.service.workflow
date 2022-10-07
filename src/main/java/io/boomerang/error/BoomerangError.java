package io.boomerang.error;

import org.springframework.http.HttpStatus;

public enum BoomerangError {

  /** Add reusable error list here. */
  // @formatter:off
  TEAM_NAME_ALREADY_EXISTS(100, "TEAM_NAME_ALREADY_EXISTS", HttpStatus.BAD_REQUEST),
  TOO_MANY_REQUESTS(429, "TOO_MANY_REQUESTS", HttpStatus.TOO_MANY_REQUESTS),
  IMPORT_WORKFLOW_FAILED(400, "IMPORT_WORKFLOW_FAILED", HttpStatus.BAD_REQUEST),
  WORKFLOW_TRIGGER_DISABLED(403, "WORKFLOW_TRIGGER_DISABLED", HttpStatus.FORBIDDEN),
  WORKFLOW_TOKEN_INVALID(401, "WORKFLOW_TOKEN_INVALID", HttpStatus.UNAUTHORIZED),
  WORKFLOW_TEAM_INACTIVE(403, "WORKFLOW_TEAM_INACTIVE", HttpStatus.FORBIDDEN);
  // @formatter:on

  private final int code;

  private final String description;

  private final HttpStatus httpStatus;

  public int getCode() {
    return code;
  }

  public String getDescription() {
    return description;
  }

  public HttpStatus getHttpStatus() {
    return httpStatus;
  }

  private BoomerangError(int code, String description, HttpStatus httpStatus) {
    this.code = code;
    this.description = description;
    this.httpStatus = httpStatus;
  }
}
