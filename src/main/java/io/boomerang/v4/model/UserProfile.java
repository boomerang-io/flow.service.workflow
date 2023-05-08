package io.boomerang.v4.model;

import java.util.List;
import org.springframework.beans.BeanUtils;
import io.boomerang.v4.data.entity.UserEntity;

/*
 * Utilised by the Profile endpoint
 * 
 * Same as User but with Teams
 */
public class UserProfile extends UserEntity {
  
  List<Team> teams;

  public UserProfile() {
    
  }
  
  public UserProfile(UserEntity entity) {
    BeanUtils.copyProperties(entity, this);
  }

  public List<Team> getTeams() {
    return teams;
  }

  public void setTeams(List<Team> teams) {
    this.teams = teams;
  }
}
