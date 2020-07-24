
package net.boomerangplatform.security.model;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"error", "code", "message"})
public class Error {

  @JsonIgnore
  private final Map<String, Object> additionalProperties = new HashMap<>();
  @JsonProperty("code")
  private Integer code;
  @JsonProperty("error")
  private Errors errorDesc = null;

  @JsonProperty("message")
  private String message;

  @JsonAnyGetter
  public Map<String, Object> getAdditionalProperties() {
    return this.additionalProperties;
  }

  @JsonProperty("code")
  public Integer getCode() {
    return code;
  }

  @JsonProperty("error")
  public Errors getError() {
    return errorDesc;
  }

  @JsonProperty("message")
  public String getMessage() {
    return message;
  }

  @JsonAnySetter
  public void setAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
  }

  @JsonProperty("code")
  public void setCode(Integer code) {
    this.code = code;
  }

  @JsonProperty("error")
  public void setError(Errors error) {
    this.errorDesc = error;
  }

  @JsonProperty("message")
  public void setMessage(String message) {
    this.message = message;
  }

}
