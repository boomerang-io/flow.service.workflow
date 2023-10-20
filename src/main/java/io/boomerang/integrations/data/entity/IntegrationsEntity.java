package io.boomerang.integrations.data.entity;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.boomerang.integrations.model.enums.IntegrationStatus;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Document(collection = "#{@mongoConfiguration.fullCollectionName('integrations')}")
public class IntegrationsEntity {
  @Id
  private String id;
  
  private String type;
  
  private String ref;
  
  private Object data;
  
  private IntegrationStatus status = IntegrationStatus.unlinked;

  private  Map<String, String> labels = new HashMap<>();

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

  public String getRef() {
    return ref;
  }

  public void setRef(String ref) {
    this.ref = ref;
  }

  public Object getData() {
    return data;
  }

  public void setData(Object data) {
    this.data = data;
  }

  public IntegrationStatus getStatus() {
    return status;
  }

  public void setStatus(IntegrationStatus status) {
    this.status = status;
  }

  public Map<String, String> getLabels() {
    return labels;
  }

  public void setLabels(Map<String, String> labels) {
    this.labels = labels;
  }
}
