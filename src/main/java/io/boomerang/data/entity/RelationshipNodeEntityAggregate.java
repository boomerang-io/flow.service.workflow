package io.boomerang.data.entity;

import java.util.LinkedList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/*
 * Entity for Relationships
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RelationshipNodeEntityAggregate extends RelationshipNodeEntity {

  private List<RelationshipEdgeEntity> paths = new LinkedList<>();

  private List<RelationshipNodeEntity> children = new LinkedList<>();

  @Override
  public String toString() {
    return super.toString() + "RelationshipNodeEntity [paths=" + paths + ", children=" + children + "]";
  }

  public List<RelationshipNodeEntity> getChildren() {
    return children;
  }

  public void setChildren(List<RelationshipNodeEntity> children) {
    this.children = children;
  }

  public List<RelationshipEdgeEntity> getPaths() {
    return paths;
  }

  public void setPaths(List<RelationshipEdgeEntity> paths) {
    this.paths = paths;
  }
  
}
