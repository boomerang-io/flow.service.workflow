package io.boomerang.data.entity;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.boomerang.model.enums.RelationshipNodeType;
import io.boomerang.model.enums.RelationshipLabel;

/*
 * Entity for Relationships
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Document(collection = "#{@mongoConfiguration.fullCollectionName('relationships')}")
public class RelationshipEntity {

  @Id
  private String id;
  private Date creationDate = new Date();
  private RelationshipNodeType from;
  private String fromRef;
  private RelationshipLabel type; 
  private RelationshipNodeType to;
  private String toRef;
  private Map<String, Object> data = new HashMap<>();
   
  @Override
  public String toString() {
    return "RelationshipEntity [id=" + id + ", from="
        + from + ", fromRef=" + fromRef + ", label=" + type + ", to=" + to + ", toRef=" + toRef + ", date=" + creationDate + ", data=" + data + "]";
  }
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }
  public RelationshipLabel getLabel() {
    return type;
  }
  public void setLabel(RelationshipLabel type) {
    this.type = type;
  }
  public RelationshipNodeType getFrom() {
    return from;
  }
  public void setFrom(RelationshipNodeType from) {
    this.from = from;
  }
  public String getFromRef() {
    return fromRef;
  }
  public void setFromRef(String fromRef) {
    this.fromRef = fromRef;
  }
  public RelationshipNodeType getTo() {
    return to;
  }
  public void setTo(RelationshipNodeType to) {
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
