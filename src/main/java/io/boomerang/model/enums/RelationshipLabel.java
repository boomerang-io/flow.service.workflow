package io.boomerang.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/*
 * Relationship Type
 * 
 * Ref: 
 * - https://learn.microsoft.com/en-us/azure/cosmos-db/gremlin/modeling#relationship-labels
 * - https://tinkerpop.apache.org/docs/3.6.2/recipes/
 */
public enum RelationshipLabel {
  BELONGSTO("Belongs To"), INTEGRATIONFOR("Integration For"), MEMBEROF("Member Of"), EXECUTIONOF("Execution Of"), CREATED("Created");//, INITIATEDBY("Initiated By");

  private String name;

  RelationshipLabel(String name) {
    this.name = name;
  }

  @JsonValue
  public String toString() {
    return name;
  }
}
