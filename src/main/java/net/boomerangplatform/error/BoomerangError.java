package net.boomerangplatform.error;

import org.springframework.http.HttpStatus;

public enum BoomerangError {
  
  /** Add reusable error list here. */
  TEAM_NAME_ALREADY_EXISTS(100, "TEAM_NAME_ALREADY_EXISTS", HttpStatus.BAD_REQUEST),
  TOO_MANY_REQUESTS(429, "TOO_MANY_REQUESTS", HttpStatus.TOO_MANY_REQUESTS),
  WORKLOAD_MANUAL_DISABLED(429, "WORKLOAD_MANUAL_DISABLED", HttpStatus.UNAUTHORIZED);
  
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
