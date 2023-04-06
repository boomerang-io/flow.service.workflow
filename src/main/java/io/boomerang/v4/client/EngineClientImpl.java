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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import io.boomerang.error.BoomerangException;
import io.boomerang.v4.model.ref.Workflow;
import io.boomerang.v4.model.ref.WorkflowRun;
import io.boomerang.v4.model.ref.WorkflowRunInsight;
import io.boomerang.v4.model.ref.WorkflowRunRequest;

@Service
@Primary
public class EngineClientImpl implements EngineClient {

  private static final Logger logger = LogManager.getLogger(EngineClientImpl.class);

  @Value("${flow.engine.workflowrun.query.url}")
  public String queryWorkflowRunURL;
  
  @Value("${flow.engine.workflowrun.insight.url}")
  public String insightWorkflowRunURL;

  @Value("${flow.engine.workflowrun.get.url}")
  public String getWorkflowRunURL;

  @Value("${flow.engine.workflowrun.submit.url}")
  public String submitWorkflowRunURL;

  @Value("${flow.engine.workflowrun.start.url}")
  public String startWorkflowRunURL;

  @Value("${flow.engine.workflowrun.finalize.url}")
  public String finalizeWorkflowRunURL;

  @Value("${flow.engine.workflowrun.cancel.url}")
  public String cancelWorkflowRunURL;

  @Value("${flow.engine.workflowrun.retry.url}")
  public String retryWorkflowRunURL;

  @Value("${flow.engine.workflow.get.url}")
  public String getWorkflowURL;

  @Value("${flow.engine.workflow.query.url}")
  public String queryWorkflowURL;

  @Value("${flow.engine.workflow.create.url}")
  public String createWorkflowURL;

  @Value("${flow.engine.workflow.apply.url}")
  public String applyWorkflowURL;

  @Value("${flow.engine.workflow.enable.url}")
  public String enableWorkflowURL;

  @Value("${flow.engine.workflow.disable.url}")
  public String disableWorkflowURL;

  @Value("${flow.engine.workflow.delete.url}")
  public String deleteWorkflowURL;

  @Autowired
  @Qualifier("internalRestTemplate")
  public RestTemplate restTemplate;

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
  public WorkflowRunInsight insightWorkflowRuns(Optional<List<String>> queryLabels,
      Optional<List<String>> queryIds, Optional<Long> fromDate, Optional<Long> toDate) {
    try {
      Map<String, String> requestParams = new HashMap<>();
      if (queryLabels.isPresent()) {
        requestParams.put("labels", queryLabels.get().toString());
      }
      if (queryIds.isPresent() && !queryIds.get().isEmpty()) {
        requestParams.put("ids", queryIds.get().toString());
      }
      if (fromDate.isPresent()) {
        requestParams.put("fromDate", fromDate.get().toString());
      }
      if (toDate.isPresent()) {
        requestParams.put("toDate", toDate.get().toString());
      }

      String encodedURL =
          requestParams.keySet().stream().map(key -> key + "=" + requestParams.get(key)).collect(
              Collectors.joining("&", insightWorkflowRunURL + "?", ""));
      
      logger.info("Query URL: " + encodedURL);

      ResponseEntity<WorkflowRunInsight> response = restTemplate.getForEntity(encodedURL, WorkflowRunInsight.class);
      
      logger.info("Status Response: " + response.getStatusCode());
      logger.info("Content Response: " + response.getBody().toString());
      
      return response.getBody();
    } catch (RestClientException ex) {
      logger.error(ex.toString());
      throw new BoomerangException(ex, HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getClass().getSimpleName(), "Exception in communicating with internal services.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public WorkflowRun submitWorkflowRun(String workflowId, Optional<Integer> version, boolean start, Optional<WorkflowRunRequest> request) {
    try {
      String url = submitWorkflowRunURL.replace("{workflowId}", workflowId);
      Map<String, String> requestParams = new HashMap<>();
      if (version.isPresent()) {
        requestParams.put("version", Integer.toString(version.get()));
      }
      requestParams.put("start", Boolean.toString(start));
      String encodedURL =
          requestParams.keySet().stream().map(key -> key + "=" + requestParams.get(key)).collect(
              Collectors.joining("&", url + "?", ""));
      
      logger.info("URL: " + encodedURL);

      ResponseEntity<WorkflowRun> response = restTemplate.postForEntity(encodedURL, request, WorkflowRun.class);
      
      logger.info("Status Response: " + response.getStatusCode());
      logger.info("Content Response: " + response.getBody().toString());
      
      return response.getBody();
    } catch (RestClientException ex) {
      logger.error(ex.toString());
      throw new BoomerangException(ex, HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getClass().getSimpleName(), "Exception in communicating with internal services.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public WorkflowRun startWorkflowRun(String workflowRunId, Optional<WorkflowRunRequest> request) {
    try {
      String url = startWorkflowRunURL.replace("{workflowRunId}", workflowRunId);
      
      logger.info("URL: " + url);

      ResponseEntity<WorkflowRun> response = restTemplate.postForEntity(url, request, WorkflowRun.class);
      
      logger.info("Status Response: " + response.getStatusCode());
      logger.info("Content Response: " + response.getBody().toString());
      
      return response.getBody();
    } catch (RestClientException ex) {
      logger.error(ex.toString());
      throw new BoomerangException(ex, HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getClass().getSimpleName(), "Exception in communicating with internal services.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public WorkflowRun finalizeWorkflowRun(String workflowRunId) {
    try {
      String url = finalizeWorkflowRunURL.replace("{workflowRunId}", workflowRunId);
      
      logger.info("URL: " + url);
    final HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
  HttpEntity<String> entity = new HttpEntity<String>("{}", headers);
    ResponseEntity<WorkflowRun> response =
        restTemplate.exchange(url, HttpMethod.PUT, entity, WorkflowRun.class);
      
      logger.info("Status Response: " + response.getStatusCode());
      logger.info("Content Response: " + response.getBody().toString());
      
      return response.getBody();
    } catch (RestClientException ex) {
      logger.error(ex.toString());
      throw new BoomerangException(ex, HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getClass().getSimpleName(), "Exception in communicating with internal services.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public WorkflowRun cancelWorkflowRun(String workflowRunId) {
    try {
      String url = cancelWorkflowRunURL.replace("{workflowRunId}", workflowRunId);
      
      logger.info("URL: " + url);
    ResponseEntity<WorkflowRun> response =
        restTemplate.exchange(url, HttpMethod.DELETE, null, WorkflowRun.class);
      
      logger.info("Status Response: " + response.getStatusCode());
      logger.info("Content Response: " + response.getBody().toString());
      
      return response.getBody();
    } catch (RestClientException ex) {
      logger.error(ex.toString());
      throw new BoomerangException(ex, HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getClass().getSimpleName(), "Exception in communicating with internal services.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public WorkflowRun retryWorkflowRun(String workflowRunId) {
    try {
      String url = retryWorkflowRunURL.replace("{workflowRunId}", workflowRunId);
      
      logger.info("URL: " + url);
    final HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
  HttpEntity<String> entity = new HttpEntity<String>("{}", headers);
    ResponseEntity<WorkflowRun> response =
        restTemplate.exchange(url, HttpMethod.PUT, entity, WorkflowRun.class);
      
      logger.info("Status Response: " + response.getStatusCode());
      logger.info("Content Response: " + response.getBody().toString());
      
      return response.getBody();
    } catch (RestClientException ex) {
      logger.error(ex.toString());
      throw new BoomerangException(ex, HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getClass().getSimpleName(), "Exception in communicating with internal services.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /*
   * Start Workflow based client Endpoints
   */
  @Override
  public Workflow getWorkflow(String workflowId, Optional<Integer> version, boolean withTasks) {
    try {
      String url = getWorkflowURL.replace("{workflowId}", workflowId);
      Map<String, String> requestParams = new HashMap<>();
      if (version.isPresent()) {
        requestParams.put("version", version.toString());
      }
      requestParams.put("withTasks", Boolean.toString(withTasks));

      String encodedURL =
          requestParams.keySet().stream().map(key -> key + "=" + requestParams.get(key)).collect(
              Collectors.joining("&", url + "?", ""));
      
      logger.info("URL: " + encodedURL);

      ResponseEntity<Workflow> response = restTemplate.getForEntity(encodedURL, Workflow.class);
      
      logger.info("Status Response: " + response.getStatusCode());
      logger.info("Content Response: " + response.getBody().toString());
      
      return response.getBody();
    } catch (RestClientException ex) {
      logger.error(ex.toString());
      throw new BoomerangException(ex, HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getClass().getSimpleName(), "Exception in communicating with internal services.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
  

  @Override
  public WorkflowResponsePage queryWorkflows(int page, int limit, Sort sort, Optional<List<String>> queryLabels,
      Optional<List<String>> queryStatus, Optional<List<String>> queryIds) {
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
      if (queryIds.isPresent() && !queryIds.get().isEmpty()) {
        requestParams.put("ids", queryIds.get().toString());
      }

      String encodedURL =
          requestParams.keySet().stream().map(key -> key + "=" + requestParams.get(key)).collect(
              Collectors.joining("&", queryWorkflowURL + "?", ""));
      
      logger.info("Query URL: " + encodedURL);

      ResponseEntity<WorkflowResponsePage> response = restTemplate.getForEntity(encodedURL, WorkflowResponsePage.class);
      
      logger.info("Status Response: " + response.getStatusCode());
      logger.info("Content Response: " + response.getBody().getContent().toString());
      
      return response.getBody();
    } catch (RestClientException ex) {
      logger.error(ex.toString());
      throw new BoomerangException(ex, HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getClass().getSimpleName(), "Exception in communicating with internal services.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public Workflow createWorkflow(Workflow workflow) {
    try {
      logger.info("URL: " + createWorkflowURL);

      ResponseEntity<Workflow> response =
          restTemplate.postForEntity(createWorkflowURL, workflow, Workflow.class);

      logger.info("Status Response: " + response.getStatusCode());
      logger.info("Content Response: " + response.getBody().toString());

      return response.getBody();
    } catch (RestClientException ex) {
      logger.error(ex.toString());
      throw new BoomerangException(ex, HttpStatus.INTERNAL_SERVER_ERROR.value(),
          ex.getClass().getSimpleName(), "Exception in communicating with internal services.",
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public Workflow applyWorkflow(Workflow workflow, boolean replace) {
    try {
      String url = applyWorkflowURL;
      Map<String, String> requestParams = new HashMap<>();
      requestParams.put("replace", Boolean.toString(replace));

      String encodedURL =
          requestParams.keySet().stream().map(key -> key + "=" + requestParams.get(key)).collect(
              Collectors.joining("&", url + "?", ""));
      
      logger.info("URL: " + encodedURL);

      final HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      HttpEntity<Workflow> entity = new HttpEntity<Workflow>(workflow, headers);
      ResponseEntity<Workflow> response =
          restTemplate.exchange(encodedURL, HttpMethod.PUT, entity, Workflow.class);

      logger.info("Status Response: " + response.getStatusCode());
      logger.info("Content Response: " + response.getBody().toString());

      return response.getBody();
    } catch (RestClientException ex) {
      logger.error(ex.toString());
      throw new BoomerangException(ex, HttpStatus.INTERNAL_SERVER_ERROR.value(),
          ex.getClass().getSimpleName(), "Exception in communicating with internal services.",
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public void enableWorkflow(String workflowId) {
    try {
      String url = enableWorkflowURL.replace("{workflowId}", workflowId);
      
      logger.info("URL: " + url);
      ResponseEntity<Void> response =
        restTemplate.exchange(url, HttpMethod.PUT, null, Void.class);

      logger.info("Status Response: " + response.getStatusCode());
      
      if (!HttpStatus.NO_CONTENT.equals(response.getStatusCode())) {
        throw new RestClientException("Unable to enable Workflow");
      }
    } catch (RestClientException ex) {
      logger.error(ex.toString());
      throw new BoomerangException(ex, HttpStatus.INTERNAL_SERVER_ERROR.value(),
          ex.getClass().getSimpleName(), "Exception in communicating with internal services.",
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public void disableWorkflow(String workflowId) {
    try {
      String url = disableWorkflowURL.replace("{workflowId}", workflowId);
      
      logger.info("URL: " + url);
      ResponseEntity<Void> response =
        restTemplate.exchange(url, HttpMethod.PUT, null, Void.class);

      logger.info("Status Response: " + response.getStatusCode());
      
      if (!HttpStatus.NO_CONTENT.equals(response.getStatusCode())) {
        throw new RestClientException("Unable to disable Workflow");
      }
    } catch (RestClientException ex) {
      logger.error(ex.toString());
      throw new BoomerangException(ex, HttpStatus.INTERNAL_SERVER_ERROR.value(),
          ex.getClass().getSimpleName(), "Exception in communicating with internal services.",
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public void deleteWorkflow(String workflowId) {
    try {
      String url = deleteWorkflowURL.replace("{workflowId}", workflowId);
      
      logger.info("URL: " + url);
      ResponseEntity<Void> response =
        restTemplate.exchange(url, HttpMethod.DELETE, null, Void.class);

      logger.info("Status Response: " + response.getStatusCode());
      
      if (!HttpStatus.NO_CONTENT.equals(response.getStatusCode())) {
        throw new RestClientException("Unable to delete Workflow");
      }
    } catch (RestClientException ex) {
      logger.error(ex.toString());
      throw new BoomerangException(ex, HttpStatus.INTERNAL_SERVER_ERROR.value(),
          ex.getClass().getSimpleName(), "Exception in communicating with internal services.",
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
