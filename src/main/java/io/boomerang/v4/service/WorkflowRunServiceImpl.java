package io.boomerang.v4.service;

import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
import io.boomerang.v4.client.EngineClient;
import io.boomerang.v4.client.WorkflowRunResponsePage;
import io.boomerang.v4.model.enums.RelationshipRefType;
import io.boomerang.v4.model.ref.WorkflowRun;
import io.boomerang.v4.model.ref.WorkflowRunInsight;
import io.boomerang.v4.model.ref.WorkflowRunRequest;

/*
 * This service replicates the required calls for Engine WorkflowRunV1 APIs
 * 
 * It will
 * - Check authorization using Relationships
 * - Forward call onto Engine
 */
@Service
public class WorkflowRunServiceImpl implements WorkflowRunService {

  private static final Logger LOGGER = LogManager.getLogger();

  @Autowired
  private EngineClient engineClient;

  @Autowired
  private RelationshipService relationshipService;
  
  /*
   * Get Workflow Run
   * 
   * No need to validate params as they are either defaulted or optional
   */
  @Override
  public ResponseEntity<WorkflowRun> get(String workflowRunId, boolean withTasks) {
    if (workflowRunId == null || workflowRunId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_RUN_INVALID_REF);
    }

    List<String> workflowRunRefs = relationshipService.getFilteredRefs(RelationshipRefType.WORKFLOWRUN, Optional.empty(), Optional.empty(), Optional.empty());
    if (!workflowRunRefs.isEmpty() && workflowRunRefs.contains(workflowRunId)) {
      WorkflowRun wfRun = engineClient.getWorkflowRun(workflowRunId, withTasks);
      return ResponseEntity.ok(wfRun);
    } else {
      //TODO: do we want to return invalid ref or unauthorized
      throw new BoomerangException(BoomerangError.WORKFLOW_RUN_INVALID_REF);
    }
  }

  @Override
  /*
   * Query for WorkflowRun
   * 
   * No need to validate params as they are either defaulted or optional
   */
  public WorkflowRunResponsePage query(int page, int limit, Sort sort, Optional<List<String>> queryLabels,
      Optional<List<String>> queryStatus, Optional<List<String>> queryPhase, Optional<List<String>> queryTeams) {
    List<String> workflowRunRefs = relationshipService.getFilteredRefs(RelationshipRefType.WORKFLOWRUN, Optional.empty(), queryTeams, Optional.empty());
    LOGGER.debug("Query Ids: ", workflowRunRefs);
    
    return engineClient.queryWorkflowRuns(page, limit, sort, queryLabels, queryStatus, queryPhase, Optional.of(workflowRunRefs));
  }
  
  @Override
  public WorkflowRunInsight insight(Optional<Long> from, Optional<Long> to,  Optional<List<String>> queryLabels, Optional<List<String>> queryTeams) {
    List<String> workflowRunRefs = relationshipService.getFilteredRefs(RelationshipRefType.WORKFLOWRUN, Optional.empty(), queryTeams, Optional.empty());
    LOGGER.debug("Query Ids: ", workflowRunRefs);
    
    return engineClient.insightWorkflowRuns(queryLabels, Optional.of(workflowRunRefs), from, to);
  }

  /*
   * Submit WorkflowRun
   * 
   * No need to validate params as they are either defaulted or optional
   */
  @Override
  public ResponseEntity<WorkflowRun> submit(String workflowId, Optional<Integer> version, boolean start,
      Optional<WorkflowRunRequest> optRunRequest) {
    if (workflowId == null || workflowId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
    List<String> workflowRefsList = relationshipService.getFilteredRefs(RelationshipRefType.WORKFLOW, Optional.empty(), Optional.empty(), Optional.empty());
    if (!workflowRefsList.isEmpty() && workflowRefsList.contains(workflowId)) {
      WorkflowRun wfRun = engineClient.submitWorkflowRun(workflowId, version, start, optRunRequest);
      //TODO: move this to an Async Aspect or CloudEvent handler so that it stores the relationship regardless of REST thrown error.
      relationshipService.createRelationshipRef(RelationshipRefType.WORKFLOWRUN, wfRun.getId());
      return ResponseEntity.ok(wfRun);
    } else {
      //TODO: do we want to return invalid ref or unauthorized
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
  }
  
  /*
   * Start WorkflowRun
   * 
   * TODO: do we expose this one?
   * 
   * No need to validate params as they are either defaulted or optional
   */
  @Override
  public ResponseEntity<WorkflowRun> start(String workflowRunId,
      Optional<WorkflowRunRequest> optRunRequest) {
    if (workflowRunId == null || workflowRunId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_RUN_INVALID_REF);
    }
    List<String> workflowRunRefsList = relationshipService.getFilteredRefs(RelationshipRefType.WORKFLOWRUN, Optional.empty(), Optional.empty(), Optional.empty());
    if (!workflowRunRefsList.isEmpty() && workflowRunRefsList.contains(workflowRunId)) {
      WorkflowRun wfRun = engineClient.startWorkflowRun(workflowRunId, optRunRequest);
      return ResponseEntity.ok(wfRun);
    } else {
      //TODO: do we want to return invalid ref or unauthorized
      throw new BoomerangException(BoomerangError.WORKFLOW_RUN_INVALID_REF);
    }
  }

  /*
   * Finalize WorkflowRun
   * 
   * TODO: do we expose this one?
   */
  @Override
  public ResponseEntity<WorkflowRun> finalize(String workflowRunId) {
    if (workflowRunId == null || workflowRunId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_RUN_INVALID_REF);
    }
    List<String> workflowRunRefsList = relationshipService.getFilteredRefs(RelationshipRefType.WORKFLOWRUN, Optional.empty(), Optional.empty(), Optional.empty());
    if (!workflowRunRefsList.isEmpty() && workflowRunRefsList.contains(workflowRunId)) {
      WorkflowRun wfRun = engineClient.finalizeWorkflowRun(workflowRunId);
      return ResponseEntity.ok(wfRun);
    } else {
      //TODO: do we want to return invalid ref or unauthorized
      throw new BoomerangException(BoomerangError.WORKFLOW_RUN_INVALID_REF);
    }
  }

  /*
   * Cancel WorkflowRun
   */
  @Override
  public ResponseEntity<WorkflowRun> cancel(String workflowRunId) {
    if (workflowRunId == null || workflowRunId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_RUN_INVALID_REF);
    }
    List<String> workflowRunRefsList = relationshipService.getFilteredRefs(RelationshipRefType.WORKFLOWRUN, Optional.empty(), Optional.empty(), Optional.empty());
    if (!workflowRunRefsList.isEmpty() && workflowRunRefsList.contains(workflowRunId)) {
      WorkflowRun wfRun = engineClient.cancelWorkflowRun(workflowRunId);
      return ResponseEntity.ok(wfRun);
    } else {
      //TODO: do we want to return invalid ref or unauthorized
      throw new BoomerangException(BoomerangError.WORKFLOW_RUN_INVALID_REF);
    }
  }

  /*
   * Retry WorkflowRun
   */
  @Override
  public ResponseEntity<WorkflowRun> retry(String workflowRunId) {
    if (workflowRunId == null || workflowRunId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_RUN_INVALID_REF);
    }
    List<String> workflowRunRefsList = relationshipService.getFilteredRefs(RelationshipRefType.WORKFLOWRUN, Optional.empty(), Optional.empty(), Optional.empty());
    if (!workflowRunRefsList.isEmpty() && workflowRunRefsList.contains(workflowRunId)) {
      WorkflowRun wfRun = engineClient.retryWorkflowRun(workflowRunId);
      return ResponseEntity.ok(wfRun);
    } else {
      //TODO: do we want to return invalid ref or unauthorized
      throw new BoomerangException(BoomerangError.WORKFLOW_RUN_INVALID_REF);
    }
  }
}
