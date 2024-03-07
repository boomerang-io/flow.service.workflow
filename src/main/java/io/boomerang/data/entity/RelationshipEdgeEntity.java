package io.boomerang.data.entity;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.boomerang.model.enums.RelationshipLabel;

/*
 * Entity for Relationships
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Document(collection = "#{@mongoConfiguration.fullCollectionName('relationship_edges')}")
public class RelationshipEdgeEntity {

  @Id
  private String id;
  private Date creationDate = new Date();
  private ObjectId from;
  private RelationshipLabel label; 
  private ObjectId to;
  private Map<String, String> data = new HashMap<>();

  public RelationshipEdgeEntity() {
    // TODO Auto-generated constructor stub
  }

  public RelationshipEdgeEntity(String from, RelationshipLabel label,
      String to, Optional<Map<String, String>> data) {
    this.from = new ObjectId(from);
    this.label = label;
    this.to = new ObjectId(to);
    if (data.isPresent()) {
      this.setData(data.get());
    }
  }
  
  @Override
  public String toString() {
    return "RelationshipEdgeEntity [id=" + id + ", creationDate=" + creationDate + ", from=" + from
        + ", label=" + label + ", to=" + to + ", data=" + data + "]";
  }
  
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }
  public Date getCreationDate() {
    return creationDate;
  }
  public void setCreationDate(Date creationDate) {
    this.creationDate = creationDate;
  }
  public String getFrom() {
    return from.toString();
  }
  public void setFrom(String from) {
    this.from = new ObjectId(from);
  }
  public RelationshipLabel getLabel() {
    return label;
  }
  public void setLabel(RelationshipLabel label) {
    this.label = label;
  }
  public String getTo() {
    return to.toString();
  }
  public void setTo(String to) {
    this.to = new ObjectId(to);
  }

  public Map<String, String> getData() {
    return data;
  }

  public void setData(Map<String, String> data) {
    this.data = data;
  }
}
