package io.boomerang.v4.model;

import org.springframework.beans.BeanUtils;
import io.boomerang.v4.data.entity.UserEntity;

/*
 * The external model for a User
 */
public class User extends UserEntity {

  public User() {
    
  }
  
  public User(UserEntity entity) {
    BeanUtils.copyProperties(entity, this);
  }
  
}
