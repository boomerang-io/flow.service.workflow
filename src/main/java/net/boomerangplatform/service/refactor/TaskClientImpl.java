package net.boomerangplatform.service.refactor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import net.boomerangplatform.mongo.model.internal.InternalTaskRequest;
import net.boomerangplatform.mongo.model.internal.InternalTaskResponse;

@Service
public class TaskClientImpl implements TaskClient {

  @Value("${flow.starttask.url}")
  public String startTaskUrl;

  @Value("${flow.endtask.url}")
  public String endTaskUrl;

  @Autowired
  @Qualifier("selfRestTemplate")
  public RestTemplate restTemplate;

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
}
