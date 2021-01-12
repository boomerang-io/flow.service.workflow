package net.boomerangplatform.mongo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MongoConfiguration {

  @Value("${flow.mongo.collection.prefix}")
  private String workflowCollectionPrefix;
  
  public String fullCollectionName(String collectionName) {
    
    if (workflowCollectionPrefix == null || workflowCollectionPrefix.isBlank()) {
      return "" + collectionName;
    }
    String newCollectionName = workflowCollectionPrefix + "_" + collectionName;
    
    return newCollectionName;
  }
  
  public String collectionPrefix() {
    return this.workflowCollectionPrefix;
  }
  
}



