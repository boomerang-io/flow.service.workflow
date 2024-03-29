package io.boomerang.model;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.BeanUtils;
import org.springframework.data.annotation.Id;
import io.boomerang.data.entity.UserEntity;
import io.boomerang.model.enums.UserType;

/*
 * The external model for a User
 */
public class UserRequest {
  
  @Id
  private String id;
  private String email;
  private String name;
  private String displayName;
  private UserType type;
  private UserStatus status;
  private Map<String, String> labels = new HashMap<>();

  public UserRequest() {
    
  }
  
  public UserRequest(UserEntity entity) {
    BeanUtils.copyProperties(entity, this);
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }


  public UserType getType() {
    return type;
  }

  public void setType(UserType type) {
    this.type = type;
  }

  public UserStatus getStatus() {
    return status;
  }

  public void setStatus(UserStatus status) {
    this.status = status;
  }

  public Map<String, String> getLabels() {
    return labels;
  }

  public void setLabels(Map<String, String> labels) {
    this.labels = labels;
  }
}
