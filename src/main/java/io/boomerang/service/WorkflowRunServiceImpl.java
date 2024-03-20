package io.boomerang.service;

import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import io.boomerang.client.EngineClient;
import io.boomerang.client.WorkflowRunResponsePage;
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
import io.boomerang.model.enums.RelationshipType;
import io.boomerang.model.enums.RelationshipLabel;
import io.boomerang.model.ref.WorkflowRun;
import io.boomerang.model.ref.WorkflowRunCount;
import io.boomerang.model.ref.WorkflowRunInsight;
import io.boomerang.model.ref.WorkflowRunRequest;

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
      throw new BoomerangException(BoomerangError.WORKFLOWRUN_INVALID_REF);
    }

    List<String> workflowRunRefs = relationshipService.getFilteredFromRefs(Optional.of(RelationshipType.WORKFLOWRUN), Optional.of(List.of(workflowRunId)), Optional.of(RelationshipLabel.BELONGSTO), Optional.of(RelationshipType.TEAM), Optional.empty());
    if (!workflowRunRefs.isEmpty()) {
      WorkflowRun wfRun = engineClient.getWorkflowRun(workflowRunId, withTasks);
      return ResponseEntity.ok(wfRun);
    } else {
      throw new BoomerangException(BoomerangError.WORKFLOWRUN_INVALID_REF);
    }
  }

  @Override
  /*
   * Query for WorkflowRun
   * 
   * No need to validate params as they are either defaulted or optional
   */
  public WorkflowRunResponsePage query(
      Optional<Long> fromDate, Optional<Long> toDate, 
      Optional<Integer> queryLimit, Optional<Integer> queryPage, Optional<Direction> queryOrder, Optional<List<String>> queryLabels,
      Optional<List<String>> queryStatus, Optional<List<String>> queryPhase,
      Optional<List<String>> queryTeams, Optional<List<String>> queryWorkflowRuns,
      Optional<List<String>> queryWorkflows, Optional<List<String>> queryTriggers) {
    // Get Refs that request has access to
    List<String> workflowRunRefs = relationshipService.getFilteredFromRefs(
        Optional.of(RelationshipType.WORKFLOWRUN), queryWorkflowRuns,
        Optional.of(RelationshipLabel.BELONGSTO), Optional.of(RelationshipType.TEAM), queryTeams);
    
    LOGGER.debug("Refs: " + workflowRunRefs);

    if (!workflowRunRefs.isEmpty()) {
      return engineClient.queryWorkflowRuns(fromDate,
          toDate, queryLimit, queryPage, queryOrder, queryLabels, queryStatus, queryPhase, Optional.of(workflowRunRefs),
          queryWorkflows, queryTriggers);
    } else {
      throw new BoomerangException(BoomerangError.WORKFLOWRUN_INVALID_REF);
    }
  }
  
  /*
   * Retrieve the insights / statistics for a specific period of time and filters
   */
  @Override
  public WorkflowRunInsight insight(Optional<Long> from, Optional<Long> to,  Optional<List<String>> queryLabels, Optional<List<String>> queryWorkflows, Optional<List<String>> queryTeams) {
    List<String> workflowRunRefs = relationshipService.getFilteredFromRefs(Optional.of(RelationshipType.WORKFLOWRUN), Optional.empty(), Optional.of(RelationshipLabel.BELONGSTO), Optional.of(RelationshipType.TEAM), queryTeams);
    LOGGER.debug("Query Ids: ", workflowRunRefs);
    
    return engineClient.insightWorkflowRuns(queryLabels, Optional.of(workflowRunRefs), queryWorkflows, from, to);
  }
  
  /*
   * Retrieve the insights / statistics for a specific period of time and filters
   */
  @Override
  public WorkflowRunCount count(Optional<Long> from, Optional<Long> to,  Optional<List<String>> queryLabels, Optional<List<String>> queryTeams, Optional<List<String>> queryWorkflows) {
    List<String> refs = relationshipService.getFilteredFromRefs(Optional.of(RelationshipType.WORKFLOW), queryWorkflows, Optional.of(RelationshipLabel.BELONGSTO), Optional.of(RelationshipType.TEAM), queryTeams);
    LOGGER.debug("Query Ids: ", refs);
    
    return engineClient.countWorkflowRuns(queryLabels, Optional.of(refs), from, to);
  }
  
  /*
   * Start WorkflowRun
   * 
   * TODO: do we expose this one?
   */
  @Override
  public ResponseEntity<WorkflowRun> start(String workflowRunId,
      Optional<WorkflowRunRequest> optRunRequest) {
    if (workflowRunId == null || workflowRunId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOWRUN_INVALID_REF);
    }
    List<String> workflowRunRefs = relationshipService.getFilteredFromRefs(Optional.of(RelationshipType.WORKFLOWRUN), Optional.of(List.of(workflowRunId)), Optional.of(RelationshipLabel.BELONGSTO), Optional.empty(), Optional.empty());
    if (!workflowRunRefs.isEmpty()) {
      WorkflowRun wfRun = engineClient.startWorkflowRun(workflowRunId, optRunRequest);
      return ResponseEntity.ok(wfRun);
    } else {
      throw new BoomerangException(BoomerangError.WORKFLOWRUN_INVALID_REF);
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
      throw new BoomerangException(BoomerangError.WORKFLOWRUN_INVALID_REF);
    }
    List<String> workflowRunRefs = relationshipService.getFilteredFromRefs(Optional.of(RelationshipType.WORKFLOWRUN), Optional.of(List.of(workflowRunId)), Optional.of(RelationshipLabel.BELONGSTO), Optional.empty(), Optional.empty());
    if (!workflowRunRefs.isEmpty()) {
      WorkflowRun wfRun = engineClient.finalizeWorkflowRun(workflowRunId);
      return ResponseEntity.ok(wfRun);
    } else {
      throw new BoomerangException(BoomerangError.WORKFLOWRUN_INVALID_REF);
    }
  }

  /*
   * Cancel WorkflowRun
   */
  @Override
  public ResponseEntity<WorkflowRun> cancel(String workflowRunId) {
    if (workflowRunId == null || workflowRunId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOWRUN_INVALID_REF);
    }
    List<String> workflowRunRefs = relationshipService.getFilteredFromRefs(Optional.of(RelationshipType.WORKFLOWRUN), Optional.of(List.of(workflowRunId)), Optional.of(RelationshipLabel.BELONGSTO), Optional.empty(), Optional.empty());
    if (!workflowRunRefs.isEmpty()) {
      WorkflowRun wfRun = engineClient.cancelWorkflowRun(workflowRunId);
      return ResponseEntity.ok(wfRun);
    } else {
      //TODO: do we want to return invalid ref or unauthorized
      throw new BoomerangException(BoomerangError.WORKFLOWRUN_INVALID_REF);
    }
  }

  /*
   * Retry WorkflowRun
   */
  @Override
  public ResponseEntity<WorkflowRun> retry(String workflowRunId) {
    if (workflowRunId == null || workflowRunId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOWRUN_INVALID_REF);
    }
    List<String> workflowRunRefs = relationshipService.getFilteredFromRefs(Optional.of(RelationshipType.WORKFLOWRUN), Optional.of(List.of(workflowRunId)), Optional.of(RelationshipLabel.BELONGSTO), Optional.empty(), Optional.empty());
    if (!workflowRunRefs.isEmpty()) {
      WorkflowRun wfRun = engineClient.retryWorkflowRun(workflowRunId);
      return ResponseEntity.ok(wfRun);
    } else {
      throw new BoomerangException(BoomerangError.WORKFLOWRUN_INVALID_REF);
    }
  }
}
