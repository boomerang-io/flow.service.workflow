package io.boomerang.v4.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/*
 * Relationship Type
 * 
 * Ref: 
 * - https://learn.microsoft.com/en-us/azure/cosmos-db/gremlin/modeling#relationship-labels
 * - https://tinkerpop.apache.org/docs/3.6.2/recipes/
 */
public enum RelationshipType {
  BELONGSTO("Belongs To"), MEMBEROF("Member Of"), EXECUTIONOF("Execution Of"), AUTHORIZES("Authorizes"), CREATED("Created");//, INITIATEDBY("Initiated By");

  private String name;

  RelationshipType(String name) {
    this.name = name;
  }

  @JsonValue
  public String toString() {
    return name;
  }
}
