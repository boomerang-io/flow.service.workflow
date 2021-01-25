package net.boomerangplatform.service.refactor;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import com.github.alturkovic.lock.exception.LockNotAvailableException;
import net.boomerangplatform.model.Task;
import net.boomerangplatform.mongo.entity.TaskExecutionEntity;
import net.boomerangplatform.mongo.service.MongoConfiguration;
import net.boomerangplatform.service.PropertyManager;
import net.boomerangplatform.util.FlowMongoLock;

@Service
public class LockManagerImpl implements LockManager {

  @Autowired
  private MongoTemplate mongoTemplate;

  @Autowired
  private MongoConfiguration mongoConfiguration;

  @Autowired
  private PropertyManager propertyManager;
  
  private static final Logger LOGGER = LogManager.getLogger(TaskServiceImpl.class);

  @Override
  public void acquireLock(Task taskExecution) {

    LOGGER.info("Entering Acquire Lock");
    
   
    String activityId = taskExecution.getTaskActivityId();
    String workflowId = taskExecution.getWorkflowId();

    long timeout = 60000;
    String key = null;
    
    // TODO: if (taskExecution != null && taskExecution.getOutputs() != null) {
    if (taskExecution != null) {
      
      Map<String, String> properties = null;
      
      if (properties.get("timeout") != null) {
        String timeoutStr = properties.get("timeout");
        if (!timeoutStr.isBlank() && NumberUtils.isCreatable(timeoutStr)) {
          timeout = Long.valueOf(timeoutStr);
        }
      } else if (properties.get("key") != null) {
        key = properties.get("key");
        ControllerRequestProperties propertiesList =
            propertyManager.buildRequestPropertyLayering(null, activityId, workflowId);
        key = propertyManager.replaceValueWithProperty(key, activityId, propertiesList);
      }
    }

    if (key != null) {
      LOGGER.info("Lock key: " + key);
      
      String storeId = taskExecution.getWorkflowId();
      final List<String> keys = new LinkedList<>();
      keys.add(storeId);
      String text = null;
      ControllerRequestProperties properties =
          propertyManager.buildRequestPropertyLayering(null, activityId, workflowId);
      final String textValue =
          propertyManager.replaceValueWithProperty(text, activityId, properties);
      Supplier<String> supplier = () -> textValue;

      String storeID = mongoConfiguration.fullCollectionName("tasks_locks");
      FlowMongoLock mongoLock = new FlowMongoLock(supplier, this.mongoTemplate);
      final String token = mongoLock.acquire(keys, storeID, timeout);

      if (StringUtils.isEmpty(token)) {
        /** TODO: What to do here. */
        throw new LockNotAvailableException(
            String.format("Lock not available for keys: %s in store %s", keys, storeId));
      }

      RetryTemplate retryTemplate = getRetryTemplate();
      retryTemplate.execute(ctx -> {
        final boolean lockExists = mongoLock.exists(storeID, token);
        if (lockExists) {
          throw new LockNotAvailableException(
              String.format("Lock hasn't been released yet for: %s in store %s", keys, storeId));
        }
        return lockExists;
      });

    } else {
      LOGGER.info("No Acquire Lock Key Found!");
    }
  }

  private RetryTemplate getRetryTemplate() {
    RetryTemplate retryTemplate = new RetryTemplate();
    FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
    fixedBackOffPolicy.setBackOffPeriod(10000l);
    retryTemplate.setBackOffPolicy(fixedBackOffPolicy);
    SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
    retryPolicy.setMaxAttempts(Integer.MAX_VALUE);
    retryTemplate.setRetryPolicy(retryPolicy);
    return retryTemplate;
  }

  @Override
  public void releaseLock(Task taskExecution) {
    
    LOGGER.info("Entering Release Lock");
    
    String activityId = taskExecution.getTaskActivityId();
    String workflowId = taskExecution.getWorkflowId();
    String storeID = mongoConfiguration.fullCollectionName("tasks_locks");
    String key = null;
    
    // TODO:  if (taskExecution != null && taskExecution.getOutputs() != null) {
    
    if (taskExecution != null) {
      Map<String, String> properties = null;
      if (properties.get("key") != null) {
        key = properties.get("key");
        ControllerRequestProperties propertiesList =
            propertyManager.buildRequestPropertyLayering(null, activityId, workflowId);
        key = propertyManager.replaceValueWithProperty(key, activityId, propertiesList);
      }
    }

    if (key != null) {
      String storeId = taskExecution.getWorkflowId();
      final List<String> keys = new LinkedList<>();
      keys.add(storeId);
      String text = null;
      ControllerRequestProperties properties =
          propertyManager.buildRequestPropertyLayering(null, activityId, workflowId);
      final String textValue = propertyManager.replaceValueWithProperty(text, activityId, properties);
      Supplier<String> supplier = () -> textValue;
      String token = null;
      FlowMongoLock mongoLock = new FlowMongoLock(supplier, this.mongoTemplate);
      mongoLock.release(null, storeID, token);
    }
  }
}
