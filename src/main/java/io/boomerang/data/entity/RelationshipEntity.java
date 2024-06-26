package io.boomerang.data.entity;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.boomerang.model.enums.RelationshipType;

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
  private String ref;
  private String slug;
  private Map<String, String> data = new HashMap<>();
  private List<RelationshipConnectionEntity> connections = new LinkedList<>();
  
  public RelationshipEntity() {
    // TODO Auto-generated constructor stub
  }

  public RelationshipEntity(RelationshipType type, String ref, String slug,
      Optional<Map<String, String>> data) {
    this.type = type;
    this.ref = ref;
    this.slug = slug;
    if (data.isPresent()) {
      this.data = data.get();      
    }
  }

  public RelationshipEntity(RelationshipType type, String ref, String slug,
      Optional<Map<String, String>> data, RelationshipConnectionEntity connection) {
    this.type = type;
    this.ref = ref;
    this.slug = slug;
    if (data.isPresent()) {
      this.data = data.get();      
    }
    this.connections.add(connection);
  }

  @Override
  public String toString() {
    return "RelationshipEntity [id=" + id + ", creationDate=" + creationDate + ", type=" + type
        + ", ref=" + ref + ", slug=" + slug + ", data=" + data + "]";
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
  public RelationshipType getType() {
    return type;
  }
  public void setType(RelationshipType type) {
    this.type = type;
  }
  public String getRef() {
    return ref;
  }
  public void setRef(String ref) {
    this.ref = ref;
  }
  public String getSlug() {
    return slug;
  }

  public void setSlug(String slug) {
    this.slug = slug;
  }

  public Map<String, String> getData() {
    return data;
  }
  public void setData(Map<String, String> data) {
    this.data = data;
  }

  public List<RelationshipConnectionEntity> getConnections() {
    return connections;
  }

  public void setConnections(List<RelationshipConnectionEntity> connections) {
    this.connections = connections;
  }
 
  
}
