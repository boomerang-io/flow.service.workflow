package io.boomerang.data.entity;

import java.util.LinkedList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/*
 * Entity for Relationships
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RelationshipEntityGraph extends RelationshipEntity {

  private List<RelationshipEntity> children = new LinkedList<>();
  
  private List<RelationshipEntity> teams = new LinkedList<>();
  
  private List<RelationshipEntity> members = new LinkedList<>();

  @Override
  public String toString() {
    return super.toString() + "RelationshipEntityV2Aggregate [children=" + children + "]";
  }

  public List<RelationshipEntity> getChildren() {
    return children;
  }

  public void setChildren(List<RelationshipEntity> children) {
    this.children = children;
  }

  public List<RelationshipEntity> getTeams() {
    return teams;
  }

  public void setTeams(List<RelationshipEntity> teams) {
    this.teams = teams;
  }

  public List<RelationshipEntity> getMembers() {
    return members;
  }

  public void setMembers(List<RelationshipEntity> members) {
    this.members = members;
  }
  
}
