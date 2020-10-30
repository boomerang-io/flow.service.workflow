package net.boomerangplatform.mongo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MongoConfiguration {

  @Value("${workflow.mongo.collection.prefix}")
  private String workflowCollectionPrefix;
  
  public String prefix(String collectionName) {
    if (workflowCollectionPrefix == null || workflowCollectionPrefix.isBlank()) {
      return "" + collectionName;
    }
    return workflowCollectionPrefix + "_" + collectionName;
  }
}



