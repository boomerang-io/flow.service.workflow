package net.boomerangplatform.service.refactor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import net.boomerangplatform.Application;
import net.boomerangplatform.mongo.model.internal.InternalTaskRequest;
import net.boomerangplatform.mongo.model.internal.InternalTaskResponse;

@Service
public class TaskClientImpl implements TaskClient {

  @Value("${flow.starttask.url}")
  public String startTaskUrl;

  @Value("${flow.endtask.url}")
  public String endTaskUrl;
  
  private static final Logger LOGGER = LogManager.getLogger(TaskClientImpl.class);

  @Autowired
  @Qualifier("selfRestTemplate")
  public RestTemplate restTemplate;

  @Override
  @Async("flowAsyncExecutor")
  public void startTask(InternalTaskRequest taskRequest) {
    LOGGER.debug("Submitting start task: {}", taskRequest.getActivityId());
    
    try {
      restTemplate.postForLocation(startTaskUrl, taskRequest);
    } catch (RestClientException ex) {

    }
  }

  @Override
  @Async("flowAsyncExecutor")
  public void endTask(InternalTaskResponse taskResponse) {
    LOGGER.debug("Submitting end task: {}", taskResponse.getActivityId());
    
    try {
      restTemplate.postForLocation(endTaskUrl, taskResponse);
    } catch (RestClientException ex) {

    }
  }
}
