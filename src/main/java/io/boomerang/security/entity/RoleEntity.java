package io.boomerang.security.entity;

import java.util.LinkedList;
import java.util.List;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.boomerang.security.model.AuthType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Document(collection = "#{@mongoConfiguration.fullCollectionName('roles')}")
public class RoleEntity {
  
//  @JsonIgnore
  private String id;
  private AuthType type;
  private String name;
  private String description;
  private List<String> permissions = new LinkedList<>();
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }
  public AuthType getType() {
    return type;
  }
  public void setType(AuthType type) {
    this.type = type;
  }
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }
  public String getDescription() {
    return description;
  }
  public void setDescription(String description) {
    this.description = description;
  }
  public List<String> getPermissions() {
    return permissions;
  }
  public void setPermissions(List<String> permissions) {
    this.permissions = permissions;
  }
}
