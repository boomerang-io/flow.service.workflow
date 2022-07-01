package io.boomerang.mongo.entity;

import java.util.List;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.boomerang.mongo.model.KeyValuePair;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Document(collection = "#{@mongoConfiguration.fullCollectionName('extensions')}")
public class ExtensionEntity {
  @Id
  private String id;

  private String type;
  
  private Map<String, Object> data;

  private List<KeyValuePair> labels;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Map<String, Object> getData() {
    return data;
  }

  public void setData(Map<String, Object> data) {
    this.data = data;
  }

  public List<KeyValuePair> getLabels() {
    return labels;
  }

  public void setLabels(List<KeyValuePair> labels) {
    this.labels = labels;
  }
}
