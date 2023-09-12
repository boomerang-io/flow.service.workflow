package io.boomerang.data.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.boomerang.model.AbstractParam;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Document(collection = "#{@mongoConfiguration.fullCollectionName('global_params')}")
public class GlobalParamEntity extends AbstractParam {

  @Id
  String id;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }
}
