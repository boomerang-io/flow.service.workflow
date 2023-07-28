package io.boomerang.security.model;

import java.util.LinkedList;
import java.util.List;
import org.springframework.beans.BeanUtils;
import io.boomerang.security.entity.RoleEntity;

public class Role {
  
//  @JsonIgnore
  private String id;
  private AuthType type;
  private String name;
  private String description;
  private List<String> permissions = new LinkedList<>();
  
  public Role() {

  }

  public Role(RoleEntity entity) {
    BeanUtils.copyProperties(entity, this);
  }
  
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
