package net.boomerangplatform.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Response {

  @JsonProperty("code")
  private String code;

  @JsonProperty("message")
  private String message;

  public Response() {}

  public Response(String code, String desc) {
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
