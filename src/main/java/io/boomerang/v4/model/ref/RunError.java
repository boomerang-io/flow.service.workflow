package io.boomerang.v4.model.ref;

public class RunError {
  private String code;

  private String message;

  public RunError() {
    // Do nothing
  }

  public RunError(String code, String desc) {
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
