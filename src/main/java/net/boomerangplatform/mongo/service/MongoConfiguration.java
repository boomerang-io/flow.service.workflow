package net.boomerangplatform.mongo.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MongoConfiguration {

  @Value("${flow.mongo.collection.prefix}")
  private String workflowCollectionPrefix;
  
  private static final Logger LOGGER = LogManager.getLogger();
  
  public String fullCollectionName(String collectionName) {
    
    LOGGER.info("Looking up collection name: " + collectionName);
    
    if (workflowCollectionPrefix == null || workflowCollectionPrefix.isBlank()) {
      LOGGER.info("Detected blank collection name, returning no prefix");
      return "" + collectionName;
    }
    String newCollectionName = workflowCollectionPrefix + "_" + collectionName;
    
    LOGGER.info("Returning: " + newCollectionName);
    
    return newCollectionName;
  }
}



