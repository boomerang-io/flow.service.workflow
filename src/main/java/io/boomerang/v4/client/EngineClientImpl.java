package io.boomerang.v4.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import io.boomerang.error.BoomerangException;
import io.boomerang.v4.model.ref.WorkflowRun;

@Service
@Primary
public class EngineClientImpl implements EngineClient {

  private static final Logger logger = LogManager.getLogger(EngineClientImpl.class);

  @Value("${flow.engine.workflowrun.query.url}")
  public String queryWorkflowRunURL;

  @Value("${flow.engine.workflowrun.get.url}")
  public String getWorkflowRunURL;

  @Autowired
  @Qualifier("internalRestTemplate")
  public RestTemplate restTemplate;

  @Override
  public WorkflowRunResponsePage queryWorkflowRuns(int page, int limit, Sort sort, Optional<List<String>> queryLabels,
      Optional<List<String>> queryStatus, Optional<List<String>> queryPhase, Optional<List<String>> queryIds) {
    try {

      Map<String, String> requestParams = new HashMap<>();
      requestParams.put("page", Integer.toString(page));
      requestParams.put("limit", Integer.toString(limit));

      StringBuilder sb = new StringBuilder();
      sort.forEach(s -> sb.append(s.getProperty()).append(",").append(s.getDirection()));
      requestParams.put("sort", sb.toString());
      if (queryLabels.isPresent()) {
        requestParams.put("labels", queryLabels.get().toString());
      }
      if (queryStatus.isPresent()) {
        requestParams.put("status", queryStatus.get().toString());
      }
      if (queryPhase.isPresent()) {
        requestParams.put("phase", queryPhase.get().toString());
      }
      if (queryIds.isPresent() && !queryIds.get().isEmpty()) {
        requestParams.put("ids", queryIds.get().toString());
      }

      String encodedURL =
          requestParams.keySet().stream().map(key -> key + "=" + requestParams.get(key)).collect(
              Collectors.joining("&", queryWorkflowRunURL + "?", ""));
      
      logger.info("Query URL: " + encodedURL);
//      final HttpHeaders headers = new HttpHeaders();
//      headers.setContentType(MediaType.APPLICATION_JSON);
//      HttpEntity<String> entity = new HttpEntity<String>("{}", headers);
//      ResponseEntity<Page<WorkflowRunEntity>> response =
//          restTemplate.exchange(encodedURL, HttpMethod.GET, null, Page.class);

      ResponseEntity<WorkflowRunResponsePage> response = restTemplate.getForEntity(encodedURL, WorkflowRunResponsePage.class);
      
      logger.info("Status Response: " + response.getStatusCode());
      logger.info("Content Response: " + response.getBody().getContent().toString());
      
      return response.getBody();
    } catch (RestClientException ex) {
      logger.error(ex.toString());
      throw new BoomerangException(ex, HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getClass().getSimpleName(), "Exception in communicating with internal services.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public WorkflowRun getWorkflowRun(String workflowRunId, boolean withTasks) {
    try {
      String url = getWorkflowRunURL.replace("{workflowRunId}", workflowRunId);
      Map<String, String> requestParams = new HashMap<>();
      requestParams.put("withTasks", Boolean.toString(withTasks));

      String encodedURL =
          requestParams.keySet().stream().map(key -> key + "=" + requestParams.get(key)).collect(
              Collectors.joining("&", url + "?", ""));
      
      logger.info("URL: " + encodedURL);

      ResponseEntity<WorkflowRun> response = restTemplate.getForEntity(encodedURL, WorkflowRun.class);
      
      logger.info("Status Response: " + response.getStatusCode());
      logger.info("Content Response: " + response.getBody().toString());
      
      return response.getBody();
    } catch (RestClientException ex) {
      logger.error(ex.toString());
      throw new BoomerangException(ex, HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getClass().getSimpleName(), "Exception in communicating with internal services.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
