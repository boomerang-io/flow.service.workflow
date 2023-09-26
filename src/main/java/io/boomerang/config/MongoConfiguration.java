package io.boomerang.config;

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
    workflowCollectionPrefix = workflowCollectionPrefix.endsWith("_") ? workflowCollectionPrefix : workflowCollectionPrefix + "_";
    return workflowCollectionPrefix + collectionName;
  }
  
  public String collectionPrefix() {
    return this.workflowCollectionPrefix;
  }
  
}



