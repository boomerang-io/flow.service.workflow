package net.boomerangplatform.model.profile;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class Response {

  @JsonProperty("rc")
  private String rc;

  public void setMessage(String message) {
    this.message = message;
  }

  @JsonProperty("message")
  private String message;

  public Response() {

  }

  public Response(String rc, String message) {
    this.rc = rc;
    this.message = message;
  }

  public String getRc() {
    return this.rc;
  }

  public void setRc(String rc) {
    this.rc = rc;
  }

  public String getMessage() {
    return this.message;
  }

  public String toJSONString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{");

    sb.append("\"rc\"");
    sb.append(":");
    sb.append("\"" + this.rc + "\"");
    sb.append(",");

    sb.append("\"message\"");
    sb.append(":");
    sb.append("\"" + this.message + "\"");

    sb.append("}");

    return sb.toString();
  }
}
