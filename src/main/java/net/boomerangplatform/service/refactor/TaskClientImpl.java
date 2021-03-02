package net.boomerangplatform.service.refactor;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import net.boomerangplatform.model.FlowWebhookResponse;
import net.boomerangplatform.model.RequestFlowExecution;
import net.boomerangplatform.mongo.model.internal.InternalTaskRequest;
import net.boomerangplatform.mongo.model.internal.InternalTaskResponse;

@Service
public class TaskClientImpl implements TaskClient {

  @Value("${flow.starttask.url}")
  public String startTaskUrl;

  @Value("${flow.endtask.url}")
  public String endTaskUrl;
  
  @Value("${flow.webhook.url}")
  public String webhookUrl;

  @Autowired
  @Qualifier("selfRestTemplate")
  public RestTemplate restTemplate;
  
  private static final Logger LOGGER = LogManager.getLogger();


  @Override
  @Async
  public void startTask(InternalTaskRequest taskRequest) {
    try {
      restTemplate.postForLocation(startTaskUrl, taskRequest);
    } catch (RestClientException ex) {

    } 
  }

  @Override
  @Async
  public void endTask(InternalTaskResponse taskResponse) {
    try {
      restTemplate.postForLocation(endTaskUrl, taskResponse);
    } catch (RestClientException ex) {

    }
  }

  @Override
  public String submitWebhookEvent(RequestFlowExecution request) {
    LOGGER.info("Creating a request to execute workflow");
    try {
      FlowWebhookResponse response =
          restTemplate.postForObject(webhookUrl, request, FlowWebhookResponse.class);
      if (response != null) {
        return response.getActivityId();
      }
    } catch (RestClientException ex) {
      LOGGER.error(ExceptionUtils.getStackTrace(ex));
    }
    return null;
  }
}
