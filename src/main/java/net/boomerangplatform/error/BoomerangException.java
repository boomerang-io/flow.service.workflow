package net.boomerangplatform.error;

import org.springframework.http.HttpStatus;

public class BoomerangException extends RuntimeException {

  private static final long serialVersionUID = 1L;
  
  private final int code;
  private final String description;
  private final HttpStatus httpStatus;
  
  public BoomerangException(int code, String description, HttpStatus httpStatus) {
    super();
    this.code = code;
    this.description = description;
    this.httpStatus = httpStatus;
  }

  public BoomerangException(BoomerangError error) {
    super();
    this.code = error.getCode();
    this.description = error.getDescription();
    this.httpStatus = error.getHttpStatus();
  }
  
  public int getCode() {
    return code;
  }

  public String getDescription() {
    return description;
  }

  public HttpStatus getHttpStatus() {
    return httpStatus;
  }

}
