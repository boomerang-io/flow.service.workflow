package io.boomerang.data.entity;

import java.util.LinkedList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/*
 * Entity for Relationships
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RelationshipEntityV2Aggregate extends RelationshipEntityV2 {

  private List<RelationshipEntityV2> children = new LinkedList<>();

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
  
}
