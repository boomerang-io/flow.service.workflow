package io.boomerang.v4.data.entity;

import java.util.HashMap;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/*
 * Entity for Relationships
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Document(collection = "#{@mongoConfiguration.fullCollectionName('relationships')}")
public class RelationshipEntity {

  @Id
  private String id;
  private String relationship; //TODO convert to enum - belongs-to, initiated-by, 
  private String fromType; //TODO convert to enum
  private String fromRef;
  private String toType; //TODO convert to enum
  private String toRef;
  private Map<String, Object> data = new HashMap<>();
   
  @Override
  public String toString() {
    return "RelationshipEntity [id=" + id + ", relationship=" + relationship + ", fromType="
        + fromType + ", fromRef=" + fromRef + ", toType=" + toType + ", toRef=" + toRef + "]";
  }
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }
  public String getRelationship() {
    return relationship;
  }
  public void setRelationship(String relationship) {
    this.relationship = relationship;
  }
  public String getFromType() {
    return fromType;
  }
  public void setFromType(String fromType) {
    this.fromType = fromType;
  }
  public String getFromRef() {
    return fromRef;
  }
  public void setFromRef(String fromRef) {
    this.fromRef = fromRef;
  }
  public String getToType() {
    return toType;
  }
  public void setToType(String toType) {
    this.toType = toType;
  }
  public String getToRef() {
    return toRef;
  }
  public void setToRef(String toRef) {
    this.toRef = toRef;
  }
  public Map<String, Object> getData() {
    return data;
  }
  public void setData(Map<String, Object> data) {
    this.data = data;
  }
}
