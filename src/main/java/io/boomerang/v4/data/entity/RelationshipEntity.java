package io.boomerang.v4.data.entity;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.boomerang.v4.model.enums.RelationshipRef;
import io.boomerang.v4.model.enums.RelationshipType;

/*
 * Entity for Relationships
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Document(collection = "#{@mongoConfiguration.fullCollectionName('relationships')}")
public class RelationshipEntity {

  @Id
  private String id;
  private Date creationDate = new Date();
  private RelationshipType type; 
  private RelationshipRef from;
  private String fromRef;
  private RelationshipRef to;
  private String toRef;
  private Map<String, Object> data = new HashMap<>();
   
  @Override
  public String toString() {
    return "RelationshipEntity [id=" + id + ", relationship=" + type + ", fromType="
        + from + ", fromRef=" + fromRef + ", toType=" + to + ", toRef=" + toRef + "]";
  }
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }
  public RelationshipType getRelationship() {
    return type;
  }
  public void setRelationship(RelationshipType relationship) {
    this.type = relationship;
  }
  public RelationshipRef getFromType() {
    return from;
  }
  public void setFromType(RelationshipRef fromType) {
    this.from = fromType;
  }
  public String getFromRef() {
    return fromRef;
  }
  public void setFromRef(String fromRef) {
    this.fromRef = fromRef;
  }
  public RelationshipRef getToType() {
    return to;
  }
  public void setToType(RelationshipRef toType) {
    this.to = toType;
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
  public Date getCreationDate() {
    return creationDate;
  }
  public void setCreationDate(Date creationDate) {
    this.creationDate = creationDate;
  }
}
