package io.boomerang.v4.model;

import org.springframework.beans.BeanUtils;
import org.springframework.data.annotation.Id;
import io.boomerang.security.model.TeamRoleEnum;
import io.boomerang.v4.data.entity.UserEntity;

public class UserSummary {
   
  @Id
  private String id;
  private String email;
  private String name;
  private TeamRoleEnum role;
   
  public UserSummary() {
  }
  
  public UserSummary(UserEntity entity) {
    BeanUtils.copyProperties(entity, this);
  }
  
  public UserSummary(UserEntity entity, TeamRoleEnum role) {
    BeanUtils.copyProperties(entity, this);
    this.role = role;
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

  public TeamRoleEnum getRole() {
    return role;
  }

  public void setRole(TeamRoleEnum role) {
    this.role = role;
  }
}
