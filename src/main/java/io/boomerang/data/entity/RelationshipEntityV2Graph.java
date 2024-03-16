package io.boomerang.data.entity;

import java.util.LinkedList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/*
 * Entity for Relationships
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RelationshipEntityV2Graph extends RelationshipEntityV2 {

  private List<RelationshipEntityV2> children = new LinkedList<>();
  
  private List<RelationshipEntityV2> teams = new LinkedList<>();
  
  private List<RelationshipEntityV2> members = new LinkedList<>();

  @Override
  public String toString() {
    return super.toString() + "RelationshipEntityV2Aggregate [children=" + children + "]";
  }

  public List<RelationshipEntityV2> getChildren() {
    return children;
  }

  public void setChildren(List<RelationshipEntityV2> children) {
    this.children = children;
  }

  public List<RelationshipEntityV2> getTeams() {
    return teams;
  }

  public void setTeams(List<RelationshipEntityV2> teams) {
    this.teams = teams;
  }

  public List<RelationshipEntityV2> getMembers() {
    return members;
  }

  public void setMembers(List<RelationshipEntityV2> members) {
    this.members = members;
  }
  
}
