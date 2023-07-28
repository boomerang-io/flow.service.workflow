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
    return "RelationshipEntity [id=" + id + ", type=" + type + ", from="
        + from + ", fromRef=" + fromRef + ", to=" + to + ", toRef=" + toRef + ", date=" + creationDate + ", data=" + data + "]";
  }
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }
  public RelationshipType getType() {
    return type;
  }
  public void setType(RelationshipType type) {
    this.type = type;
  }
  public RelationshipRef getFrom() {
    return from;
  }
  public void setFrom(RelationshipRef from) {
    this.from = from;
  }
  public String getFromRef() {
    return fromRef;
  }
  public void setFromRef(String fromRef) {
    this.fromRef = fromRef;
  }
  public RelationshipRef getTo() {
    return to;
  }
  public void setTo(RelationshipRef to) {
    this.to = to;
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
