package io.boomerang.mongo.model;

public class ErrorResponse {
  private String code;

  private String message;

  public ErrorResponse() {
    // Do nothing
  }

  public ErrorResponse(String code, String desc) {
    super();
    this.code = code;
    this.message = desc;
  }

  public String getMessage() {
    return this.message;
  }

  public String getCode() {
    return this.code;
  }

  public void setMessage(String desc) {
    this.message = desc;
  }

  public void setCode(String code) {
    this.code = code;
  }

}
