package net.boomerangplatform.iam.service;

import java.util.Date;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import net.boomerangplatform.iam.model.IAMRequest;
import net.boomerangplatform.iam.model.IAMResponse;
import net.boomerangplatform.iam.model.IAMStatus;

@Service
public class IAMClientImpl implements IAMClient {

  @Autowired
  @Qualifier("internalRestTemplate")
  private RestTemplate restTemplate;

  @Value("${acc.service.custombot.event}")
  private String customBotUrl;

  private final Logger logger = LogManager.getLogger(getClass());

  @Override
  public void publishEvent(String executionId, String messageId, String activityName,
      IAMStatus status) {
    IAMRequest request = new IAMRequest();
    request.setActivityName(activityName);
    request.setActivityStatus(status);
    request.setDatetime(new Date());
    request.setResource("Action Listener");
    request.setExecutionId(executionId);
    request.setMessageId(messageId);

    try {
      IAMResponse response = restTemplate.postForObject(customBotUrl, request, IAMResponse.class);
      if (response != null && response.getSuccess()) {
        logger.info("Published event to IAM successfully.");
      } else {
        logger.info("Failed to publish event to IAM.");
      }

    } catch (RestClientResponseException responseException) {
      logger.error("Failed to publish event to IAM");
      logger.error(responseException);
    }
  }

}
