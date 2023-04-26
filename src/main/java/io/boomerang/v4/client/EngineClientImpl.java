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
import io.boomerang.v4.model.ref.TaskRun;
import io.boomerang.v4.model.ref.TaskRunEndRequest;
import io.boomerang.v4.model.ref.TaskTemplate;
import io.boomerang.v4.model.ref.Workflow;
import io.boomerang.v4.model.ref.WorkflowRun;
import io.boomerang.v4.model.ref.WorkflowRunInsight;
import io.boomerang.v4.model.ref.WorkflowRunRequest;
import io.boomerang.v4.model.ref.WorkflowRunSubmitRequest;

@Service
@Primary
public class EngineClientImpl implements EngineClient {

  private static final Logger LOGGER = LogManager.getLogger(EngineClientImpl.class);

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

  @Value("${flow.engine.taskrun.get.url}")
  public String getTaskRunURL;

  @Value("${flow.engine.taskrun.end.url}")
  public String endTaskRunURL;

  @Value("${flow.engine.tasktemplate.get.url}")
  public String getTaskTemplateURL;

  @Value("${flow.engine.tasktemplate.query.url}")
  public String queryTaskTemplateURL;

  @Value("${flow.engine.tasktemplate.create.url}")
  public String createTaskTemplateURL;

  @Value("${flow.engine.tasktemplate.apply.url}")
  public String applyTaskTemplateURL;

  @Value("${flow.engine.tasktemplate.enable.url}")
  public String enableTaskTemplateURL;

  @Value("${flow.engine.tasktemplate.disable.url}")
  public String disableTaskTemplateURL;

  @Autowired
  @Qualifier("internalRestTemplate")
  public RestTemplate restTemplate;

  /* 
   * **************************************
   * WorkflowRun endpoints
   * **************************************
   */
  @Override
  public WorkflowRun getWorkflowRun(String workflowRunId, boolean withTasks) {
    try {
      String url = getWorkflowRunURL.replace("{workflowRunId}", workflowRunId);
      Map<String, String> requestParams = new HashMap<>();
      requestParams.put("withTasks", Boolean.toString(withTasks));

      String encodedURL =
          requestParams.keySet().stream().map(key -> key + "=" + requestParams.get(key)).collect(
              Collectors.joining("&", url + "?", ""));
      
      LOGGER.info("URL: " + encodedURL);

      ResponseEntity<WorkflowRun> response = restTemplate.getForEntity(encodedURL, WorkflowRun.class);
      
      LOGGER.info("Status Response: " + response.getStatusCode());
      LOGGER.info("Content Response: " + response.getBody().toString());
      
      return response.getBody();
    } catch (RestClientException ex) {
      LOGGER.error(ex.toString());
      throw new BoomerangException(ex, HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getClass().getSimpleName(), "Exception in communicating with internal services.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public WorkflowRunResponsePage queryWorkflowRuns(int page, int limit, Sort sort, Optional<Long> fromDate, Optional<Long> toDate, Optional<List<String>> queryLabels,
      Optional<List<String>> queryStatus, Optional<List<String>> queryPhase, Optional<List<String>> queryWorkflowRuns, Optional<List<String>> queryWorkflows) {
    try {

      Map<String, String> requestParams = new HashMap<>();
      requestParams.put("page", Integer.toString(page));
      requestParams.put("limit", Integer.toString(limit));

      StringBuilder sb = new StringBuilder();
      sort.forEach(s -> sb.append(s.getProperty()).append(",").append(s.getDirection()));
      requestParams.put("sort", sb.toString());
      if (fromDate.isPresent()) {
        requestParams.put("fromDate", fromDate.get().toString());
      }
      if (toDate.isPresent()) {
        requestParams.put("toDate", toDate.get().toString());
      }
      if (queryLabels.isPresent()) {
        requestParams.put("labels", queryLabels.get().toString());
      }
      if (queryStatus.isPresent()) {
        requestParams.put("status", queryStatus.get().toString());
      }
      if (queryPhase.isPresent()) {
        requestParams.put("phase", queryPhase.get().toString());
      }
      if (queryWorkflowRuns.isPresent() && !queryWorkflowRuns.get().isEmpty()) {
        requestParams.put("workflowruns", queryWorkflowRuns.get().toString());
      }
      if (queryWorkflows.isPresent() && !queryWorkflows.get().isEmpty()) {
        requestParams.put("workflows", queryWorkflows.get().toString());
      }

      String encodedURL =
          requestParams.keySet().stream().map(key -> key + "=" + requestParams.get(key)).collect(
              Collectors.joining("&", queryWorkflowRunURL + "?", ""));
      
      LOGGER.info("Query URL: " + encodedURL);
//      final HttpHeaders headers = new HttpHeaders();
//      headers.setContentType(MediaType.APPLICATION_JSON);
//      HttpEntity<String> entity = new HttpEntity<String>("{}", headers);
//      ResponseEntity<Page<WorkflowRunEntity>> response =
//          restTemplate.exchange(encodedURL, HttpMethod.GET, null, Page.class);

      ResponseEntity<WorkflowRunResponsePage> response = restTemplate.getForEntity(encodedURL, WorkflowRunResponsePage.class);
      
      LOGGER.info("Status Response: " + response.getStatusCode());
      LOGGER.info("Content Response: " + response.getBody().getContent().toString());
      
      return response.getBody();
    } catch (RestClientException ex) {
      LOGGER.error(ex.toString());
      throw new BoomerangException(ex, HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getClass().getSimpleName(), "Exception in communicating with internal services.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public WorkflowRunInsight insightWorkflowRuns(Optional<List<String>> queryLabels,
      Optional<List<String>> queryWorkflowRuns, Optional<List<String>> queryWorkflows, Optional<Long> fromDate, Optional<Long> toDate) {
    try {
      Map<String, String> requestParams = new HashMap<>();
      if (queryLabels.isPresent()) {
        requestParams.put("labels", queryLabels.get().toString());
      }
      if (queryWorkflowRuns.isPresent() && !queryWorkflowRuns.get().isEmpty()) {
        requestParams.put("workflowruns", queryWorkflowRuns.get().toString());
      }
      if (queryWorkflows.isPresent() && !queryWorkflows.get().isEmpty()) {
        requestParams.put("workflows", queryWorkflows.get().toString());
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
      
      LOGGER.info("Query URL: " + encodedURL);

      ResponseEntity<WorkflowRunInsight> response = restTemplate.getForEntity(encodedURL, WorkflowRunInsight.class);
      
      LOGGER.info("Status Response: " + response.getStatusCode());
      LOGGER.info("Content Response: " + response.getBody().toString());
      
      return response.getBody();
    } catch (RestClientException ex) {
      LOGGER.error(ex.toString());
      throw new BoomerangException(ex, HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getClass().getSimpleName(), "Exception in communicating with internal services.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public WorkflowRun submitWorkflowRun(WorkflowRunSubmitRequest request, boolean start) {
    try {
      String url = submitWorkflowRunURL;
      Map<String, String> requestParams = new HashMap<>();
      requestParams.put("start", Boolean.toString(start));
      String encodedURL =
          requestParams.keySet().stream().map(key -> key + "=" + requestParams.get(key)).collect(
              Collectors.joining("&", url + "?", ""));
      
      LOGGER.info("URL: " + encodedURL);

      ResponseEntity<WorkflowRun> response = restTemplate.postForEntity(encodedURL, request, WorkflowRun.class);
      
      LOGGER.info("Status Response: " + response.getStatusCode());
      LOGGER.info("Content Response: " + response.getBody().toString());
      
      return response.getBody();
    } catch (RestClientException ex) {
      LOGGER.error(ex.toString());
      throw new BoomerangException(ex, HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getClass().getSimpleName(), "Exception in communicating with internal services.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public WorkflowRun startWorkflowRun(String workflowRunId, Optional<WorkflowRunRequest> request) {
    try {
      String url = startWorkflowRunURL.replace("{workflowRunId}", workflowRunId);
      
      LOGGER.info("URL: " + url);

      ResponseEntity<WorkflowRun> response = restTemplate.postForEntity(url, request, WorkflowRun.class);
      
      LOGGER.info("Status Response: " + response.getStatusCode());
      LOGGER.info("Content Response: " + response.getBody().toString());
      
      return response.getBody();
    } catch (RestClientException ex) {
      LOGGER.error(ex.toString());
      throw new BoomerangException(ex, HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getClass().getSimpleName(), "Exception in communicating with internal services.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public WorkflowRun finalizeWorkflowRun(String workflowRunId) {
    try {
      String url = finalizeWorkflowRunURL.replace("{workflowRunId}", workflowRunId);
      
      LOGGER.info("URL: " + url);
    final HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
  HttpEntity<String> entity = new HttpEntity<String>("{}", headers);
    ResponseEntity<WorkflowRun> response =
        restTemplate.exchange(url, HttpMethod.PUT, entity, WorkflowRun.class);
      
      LOGGER.info("Status Response: " + response.getStatusCode());
      LOGGER.info("Content Response: " + response.getBody().toString());
      
      return response.getBody();
    } catch (RestClientException ex) {
      LOGGER.error(ex.toString());
      throw new BoomerangException(ex, HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getClass().getSimpleName(), "Exception in communicating with internal services.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public WorkflowRun cancelWorkflowRun(String workflowRunId) {
    try {
      String url = cancelWorkflowRunURL.replace("{workflowRunId}", workflowRunId);
      
      LOGGER.info("URL: " + url);
    ResponseEntity<WorkflowRun> response =
        restTemplate.exchange(url, HttpMethod.DELETE, null, WorkflowRun.class);
      
      LOGGER.info("Status Response: " + response.getStatusCode());
      LOGGER.info("Content Response: " + response.getBody().toString());
      
      return response.getBody();
    } catch (RestClientException ex) {
      LOGGER.error(ex.toString());
      throw new BoomerangException(ex, HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getClass().getSimpleName(), "Exception in communicating with internal services.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public WorkflowRun retryWorkflowRun(String workflowRunId) {
    try {
      String url = retryWorkflowRunURL.replace("{workflowRunId}", workflowRunId);
      
      LOGGER.info("URL: " + url);
    final HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
  HttpEntity<String> entity = new HttpEntity<String>("{}", headers);
    ResponseEntity<WorkflowRun> response =
        restTemplate.exchange(url, HttpMethod.PUT, entity, WorkflowRun.class);
      
      LOGGER.info("Status Response: " + response.getStatusCode());
      LOGGER.info("Content Response: " + response.getBody().toString());
      
      return response.getBody();
    } catch (RestClientException ex) {
      LOGGER.error(ex.toString());
      throw new BoomerangException(ex, HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getClass().getSimpleName(), "Exception in communicating with internal services.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /* 
   * **************************************
   * Workflow endpoints
   * **************************************
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
      
      LOGGER.info("URL: " + encodedURL);

      ResponseEntity<Workflow> response = restTemplate.getForEntity(encodedURL, Workflow.class);
      
      LOGGER.info("Status Response: " + response.getStatusCode());
      LOGGER.info("Content Response: " + response.getBody().toString());
      
      return response.getBody();
    } catch (RestClientException ex) {
      LOGGER.error(ex.toString());
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
      
      LOGGER.info("Query URL: " + encodedURL);

      ResponseEntity<WorkflowResponsePage> response = restTemplate.getForEntity(encodedURL, WorkflowResponsePage.class);
      
      LOGGER.info("Status Response: " + response.getStatusCode());
      LOGGER.info("Content Response: " + response.getBody().getContent().toString());
      
      return response.getBody();
    } catch (RestClientException ex) {
      LOGGER.error(ex.toString());
      throw new BoomerangException(ex, HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getClass().getSimpleName(), "Exception in communicating with internal services.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public Workflow createWorkflow(Workflow workflow) {
    try {
      LOGGER.info("URL: " + createWorkflowURL);

      ResponseEntity<Workflow> response =
          restTemplate.postForEntity(createWorkflowURL, workflow, Workflow.class);

      LOGGER.info("Status Response: " + response.getStatusCode());
      LOGGER.info("Content Response: " + response.getBody().toString());

      return response.getBody();
    } catch (RestClientException ex) {
      LOGGER.error(ex.toString());
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
      
      LOGGER.info("URL: " + encodedURL);

      final HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      HttpEntity<Workflow> entity = new HttpEntity<Workflow>(workflow, headers);
      ResponseEntity<Workflow> response =
          restTemplate.exchange(encodedURL, HttpMethod.PUT, entity, Workflow.class);

      LOGGER.info("Status Response: " + response.getStatusCode());
      LOGGER.info("Content Response: " + response.getBody().toString());

      return response.getBody();
    } catch (RestClientException ex) {
      LOGGER.error(ex.toString());
      throw new BoomerangException(ex, HttpStatus.INTERNAL_SERVER_ERROR.value(),
          ex.getClass().getSimpleName(), "Exception in communicating with internal services.",
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public void enableWorkflow(String workflowId) {
    try {
      String url = enableWorkflowURL.replace("{workflowId}", workflowId);
      
      LOGGER.info("URL: " + url);
      ResponseEntity<Void> response =
        restTemplate.exchange(url, HttpMethod.PUT, null, Void.class);

      LOGGER.info("Status Response: " + response.getStatusCode());
      
      if (!HttpStatus.NO_CONTENT.equals(response.getStatusCode())) {
        throw new RestClientException("Unable to enable Workflow");
      }
    } catch (RestClientException ex) {
      LOGGER.error(ex.toString());
      throw new BoomerangException(ex, HttpStatus.INTERNAL_SERVER_ERROR.value(),
          ex.getClass().getSimpleName(), "Exception in communicating with internal services.",
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public void disableWorkflow(String workflowId) {
    try {
      String url = disableWorkflowURL.replace("{workflowId}", workflowId);
      
      LOGGER.info("URL: " + url);
      ResponseEntity<Void> response =
        restTemplate.exchange(url, HttpMethod.PUT, null, Void.class);

      LOGGER.info("Status Response: " + response.getStatusCode());
      
      if (!HttpStatus.NO_CONTENT.equals(response.getStatusCode())) {
        throw new RestClientException("Unable to disable Workflow");
      }
    } catch (RestClientException ex) {
      LOGGER.error(ex.toString());
      throw new BoomerangException(ex, HttpStatus.INTERNAL_SERVER_ERROR.value(),
          ex.getClass().getSimpleName(), "Exception in communicating with internal services.",
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public void deleteWorkflow(String workflowId) {
    try {
      String url = deleteWorkflowURL.replace("{workflowId}", workflowId);
      
      LOGGER.info("URL: " + url);
      ResponseEntity<Void> response =
        restTemplate.exchange(url, HttpMethod.DELETE, null, Void.class);

      LOGGER.info("Status Response: " + response.getStatusCode());
      
      if (!HttpStatus.NO_CONTENT.equals(response.getStatusCode())) {
        throw new RestClientException("Unable to delete Workflow");
      }
    } catch (RestClientException ex) {
      LOGGER.error(ex.toString());
      throw new BoomerangException(ex, HttpStatus.INTERNAL_SERVER_ERROR.value(),
          ex.getClass().getSimpleName(), "Exception in communicating with internal services.",
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /* 
   * **************************************
   * TaskRun endpoints
   * **************************************
   */
  @Override
  public TaskRun getTaskRun(String taskRunId) {
    try {
      String url = getTaskRunURL.replace("{taskRunId}", taskRunId);
      LOGGER.info("URL: " + url);

      ResponseEntity<TaskRun> response = restTemplate.getForEntity(url, TaskRun.class);
      
      LOGGER.info("Status Response: " + response.getStatusCode());
      LOGGER.info("Content Response: " + response.getBody().toString());
      
      return response.getBody();
    } catch (RestClientException ex) {
      LOGGER.error(ex.toString());
      throw new BoomerangException(ex, HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getClass().getSimpleName(), "Exception in communicating with internal services.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public TaskRun endTaskRun(String taskRunId, TaskRunEndRequest request) {
    try {
      String url = endTaskRunURL.replace("{taskRunId}", taskRunId);
      
      LOGGER.info("URL: " + url);

      final HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      HttpEntity<TaskRunEndRequest> entity = new HttpEntity<TaskRunEndRequest>(request, headers);
      ResponseEntity<TaskRun> response =
          restTemplate.exchange(url, HttpMethod.PUT, entity, TaskRun.class);

      LOGGER.info("Status Response: " + response.getStatusCode());
      LOGGER.info("Content Response: " + response.getBody().toString());

      return response.getBody();
    } catch (RestClientException ex) {
      LOGGER.error(ex.toString());
      throw new BoomerangException(ex, HttpStatus.INTERNAL_SERVER_ERROR.value(),
          ex.getClass().getSimpleName(), "Exception in communicating with internal services.",
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /* 
   * **************************************
   * TaskTemplate endpoints
   * **************************************
   */
  @Override
  public TaskTemplate getTaskTemplate(String name, Optional<Integer> version) {
    try {
      String url = getTaskTemplateURL.replace("{name}", name);
      Map<String, String> requestParams = new HashMap<>();
      if (version.isPresent()) {
        requestParams.put("version", version.toString());
      }

      String encodedURL =
          requestParams.keySet().stream().map(key -> key + "=" + requestParams.get(key)).collect(
              Collectors.joining("&", url + "?", ""));
      
      LOGGER.info("URL: " + encodedURL);

      ResponseEntity<TaskTemplate> response = restTemplate.getForEntity(url, TaskTemplate.class);
      
      LOGGER.info("Status Response: " + response.getStatusCode());
      LOGGER.info("Content Response: " + response.getBody().toString());
      
      return response.getBody();
    } catch (RestClientException ex) {
      LOGGER.error(ex.toString());
      throw new BoomerangException(ex, HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getClass().getSimpleName(), "Exception in communicating with internal services.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
  
  @Override
  public TaskTemplateResponsePage queryTaskTemplates(int page, int limit, Sort sort, Optional<List<String>> queryLabels,
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
              Collectors.joining("&", queryTaskTemplateURL + "?", ""));
      
      LOGGER.info("Query URL: " + encodedURL);

      ResponseEntity<TaskTemplateResponsePage> response = restTemplate.getForEntity(encodedURL, TaskTemplateResponsePage.class);
      
      LOGGER.info("Status Response: " + response.getStatusCode());
      LOGGER.info("Content Response: " + response.getBody().getContent().toString());
      
      return response.getBody();
    } catch (RestClientException ex) {
      LOGGER.error(ex.toString());
      throw new BoomerangException(ex, HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getClass().getSimpleName(), "Exception in communicating with internal services.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public TaskTemplate createTaskTemplate(TaskTemplate taskTemplate) {
    try {
      LOGGER.info("URL: " + createTaskTemplateURL);

      ResponseEntity<TaskTemplate> response =
          restTemplate.postForEntity(createTaskTemplateURL, taskTemplate, TaskTemplate.class);

      LOGGER.info("Status Response: " + response.getStatusCode());
      LOGGER.info("Content Response: " + response.getBody().toString());

      return response.getBody();
    } catch (RestClientException ex) {
      LOGGER.error(ex.toString());
      throw new BoomerangException(ex, HttpStatus.INTERNAL_SERVER_ERROR.value(),
          ex.getClass().getSimpleName(), "Exception in communicating with internal services.",
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public TaskTemplate applyTaskTemplate(TaskTemplate taskTemplate, boolean replace) {
    try {
      String url = applyTaskTemplateURL;
      Map<String, String> requestParams = new HashMap<>();
      requestParams.put("replace", Boolean.toString(replace));

      String encodedURL =
          requestParams.keySet().stream().map(key -> key + "=" + requestParams.get(key)).collect(
              Collectors.joining("&", url + "?", ""));
      
      LOGGER.info("URL: " + encodedURL);

      final HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      HttpEntity<TaskTemplate> entity = new HttpEntity<TaskTemplate>(taskTemplate, headers);
      ResponseEntity<TaskTemplate> response =
          restTemplate.exchange(encodedURL, HttpMethod.PUT, entity, TaskTemplate.class);

      LOGGER.info("Status Response: " + response.getStatusCode());
      LOGGER.info("Content Response: " + response.getBody().toString());

      return response.getBody();
    } catch (RestClientException ex) {
      LOGGER.error(ex.toString());
      throw new BoomerangException(ex, HttpStatus.INTERNAL_SERVER_ERROR.value(),
          ex.getClass().getSimpleName(), "Exception in communicating with internal services.",
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public void enableTaskTemplate(String name) {
    try {
      String url = enableTaskTemplateURL.replace("{name}", name);
      
      LOGGER.info("URL: " + url);
      ResponseEntity<Void> response =
        restTemplate.exchange(url, HttpMethod.PUT, null, Void.class);

      LOGGER.info("Status Response: " + response.getStatusCode());
      
      if (!HttpStatus.NO_CONTENT.equals(response.getStatusCode())) {
        throw new RestClientException("Unable to enable TaskTemplate");
      }
    } catch (RestClientException ex) {
      LOGGER.error(ex.toString());
      throw new BoomerangException(ex, HttpStatus.INTERNAL_SERVER_ERROR.value(),
          ex.getClass().getSimpleName(), "Exception in communicating with internal services.",
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public void disableTaskTemplate(String name) {
    try {
      String url = disableTaskTemplateURL.replace("{name}", name);
      
      LOGGER.info("URL: " + url);
      ResponseEntity<Void> response =
        restTemplate.exchange(url, HttpMethod.PUT, null, Void.class);

      LOGGER.info("Status Response: " + response.getStatusCode());
      
      if (!HttpStatus.NO_CONTENT.equals(response.getStatusCode())) {
        throw new RestClientException("Unable to disable TaskTemplate");
      }
    } catch (RestClientException ex) {
      LOGGER.error(ex.toString());
      throw new BoomerangException(ex, HttpStatus.INTERNAL_SERVER_ERROR.value(),
          ex.getClass().getSimpleName(), "Exception in communicating with internal services.",
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

}
