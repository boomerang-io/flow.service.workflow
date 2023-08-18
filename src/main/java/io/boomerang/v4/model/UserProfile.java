package io.boomerang.v4.model;

import java.util.List;
import org.springframework.beans.BeanUtils;
import io.boomerang.v4.data.entity.UserEntity;

/*
 * Utilised by the Profile endpoint
 * 
 * Same as User but with Teams & permissions
 */
public class UserProfile extends UserEntity {
  
  List<TeamSummary> teams;
  
  List<String> permissions;

  public UserProfile() {
    
  }
  
  public UserProfile(UserEntity entity) {
    BeanUtils.copyProperties(entity, this);
  }

  public List<TeamSummary> getTeams() {
    return teams;
  }

  public void setTeams(List<TeamSummary> teams) {
    this.teams = teams;
  }

  public List<String> getPermissions() {
    return permissions;
  }

  public void setPermissions(List<String> permissions) {
    this.permissions = permissions;
  }
}
