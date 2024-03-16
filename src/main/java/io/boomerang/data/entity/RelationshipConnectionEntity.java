package io.boomerang.data.entity;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.bson.types.ObjectId;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.boomerang.model.enums.RelationshipLabel;

/*
 * Entity for Relationship Connections
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RelationshipConnectionEntity {

  private Date creationDate = new Date();
  private RelationshipLabel label;
//  @DocumentReference(lazy=true)
//  private RelationshipEntityV2 to;
//  @Field(targetType = FieldType.OBJECT_ID)
  private ObjectId to;
  private Map<String, String> data = new HashMap<>();
  
  public RelationshipConnectionEntity() {
    // TODO Auto-generated constructor stub
  }

  public RelationshipConnectionEntity(RelationshipLabel label, String to,
      Optional<Map<String, String>> data) {
    this.label = label;
    this.to = new ObjectId(to);
    if (data.isPresent()) {
      this.data = data.get();      
    }
  }

  @Override
  public String toString() {
    return "RelationshipConnectionEntity [creationDate=" + creationDate + ", label=" + label
        + ", to=" + to + ", data=" + data + "]";
  }

  public Date getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(Date creationDate) {
    this.creationDate = creationDate;
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
