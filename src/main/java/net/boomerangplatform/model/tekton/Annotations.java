package net.boomerangplatform.model.tekton; 
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonPropertyOrder; 

public class Annotations{
  
  private Map<String, Object> unknownFields = new HashMap<>();

  @JsonAnyGetter
  @JsonPropertyOrder(alphabetic=true)
  public Map<String, Object> otherFields() {
      return unknownFields;
  }

  @JsonAnySetter
  public void setOtherField(String name, Object value) {
      unknownFields.put(name, value);
  }
}
