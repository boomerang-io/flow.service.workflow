package io.boomerang.service;

import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import io.boomerang.audit.AuditRepository;
import io.boomerang.client.EngineClient;
import io.boomerang.client.WorkflowRunResponsePage;
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
import io.boomerang.model.enums.RelationshipLabel;
import io.boomerang.model.enums.RelationshipType;
import io.boomerang.model.ref.Workflow;
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
  private RelationshipServiceImpl relationshipServiceImpl;
  
  /*
   * Get Workflow Run
   * 
   * No need to validate params as they are either defaulted or optional
   */
  @Override
  public ResponseEntity<WorkflowRun> get(String team, String workflowRunId, boolean withTasks) {
    if (workflowRunId == null || workflowRunId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOWRUN_INVALID_REF);
    }

    if (relationshipServiceImpl.hasTeamRelationship(Optional.of(RelationshipType.WORKFLOWRUN),
        Optional.of(workflowRunId), RelationshipLabel.BELONGSTO, team, false)) {
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
  public WorkflowRunResponsePage query(String queryTeam, 
      Optional<Long> fromDate, Optional<Long> toDate, 
      Optional<Integer> queryLimit, Optional<Integer> queryPage, Optional<Direction> queryOrder, Optional<List<String>> queryLabels,
      Optional<List<String>> queryStatus, Optional<List<String>> queryPhase, Optional<List<String>> queryWorkflowRuns,
      Optional<List<String>> queryWorkflows, Optional<List<String>> queryTriggers) {

    // Get Refs that request has access to
//    List<String> wfRunRefs = relationshipServiceImpl.getFilteredRefs(Optional.of(RelationshipType.WORKFLOWRUN), queryWorkflowRuns, RelationshipLabel.BELONGSTO, RelationshipType.TEAM, queryTeam, false);
//    LOGGER.debug("WorkflowRun Refs: {}", wfRunRefs.toString());
//    if (wfRunRefs == null || wfRunRefs.size() == 0) {
//      return new WorkflowRunResponsePage();
//    }
    
    // Check the queryWorkflows
//    if (queryWorkflows.isPresent()) {
      List<String> wfRefs = relationshipServiceImpl.getFilteredRefs(Optional.of(RelationshipType.WORKFLOW), queryWorkflows, RelationshipLabel.BELONGSTO, RelationshipType.TEAM, queryTeam, false);
      LOGGER.debug("Workflow Refs: {}", wfRefs.toString());
//      queryWorkflows.get().clear();
//      queryWorkflows.get().addAll(wfRefs);
//    }

//    if (!wfRunRefs.isEmpty()) {
//    if (!wfRunRefs.isEmpty()) {
//      return engineClient.queryWorkflowRuns(fromDate,
//          toDate, queryLimit, queryPage, queryOrder, queryLabels, queryStatus, queryPhase, Optional.of(wfRunRefs),
//          queryWorkflows, queryTriggers);
//    } else {
//      throw new BoomerangException(BoomerangError.WORKFLOWRUN_INVALID_REF);
//    } 
    if (!wfRefs.isEmpty()) {
//      LOGGER.debug("triggers: {}", queryTriggers.get().toString());
      return engineClient.queryWorkflowRuns(fromDate,
          toDate, queryLimit, queryPage, queryOrder, queryLabels, queryStatus, queryPhase, Optional.empty(),
          Optional.of(wfRefs), queryTriggers);
    } else {
      throw new BoomerangException(BoomerangError.WORKFLOWRUN_INVALID_REF);
    }
  }
  
  /*
   * Retrieve the insights / statistics for a specific period of time and filters
   */
  @Override
  public WorkflowRunInsight insight(String queryTeam, Optional<Long> from, Optional<Long> to,  Optional<List<String>> queryLabels, Optional<List<String>> queryWorkflows) {
    // Get Refs that request has access to
//    List<String> wfRunRefs = relationshipServiceImpl.getFilteredRefs(Optional.of(RelationshipType.WORKFLOWRUN), Optional.empty(), RelationshipLabel.BELONGSTO, RelationshipType.TEAM, queryTeam, false);
//    LOGGER.debug("WorkflowRun Refs: {}", wfRunRefs.toString());
//    if (wfRunRefs == null || wfRunRefs.size() == 0) {
//      return new WorkflowRunInsight();
//    }
    
    // Check the queryWorkflows
      List<String> wfRefs = relationshipServiceImpl.getFilteredRefs(Optional.of(RelationshipType.WORKFLOW), queryWorkflows, RelationshipLabel.BELONGSTO, RelationshipType.TEAM, queryTeam, false);
      LOGGER.debug("Workflow Refs: {}", wfRefs.toString());
      
    return engineClient.insightWorkflowRuns(queryLabels, Optional.empty(), Optional.of(wfRefs), from, to);
  }
  
  /*
   * Retrieve the insights / statistics for a specific period of time and filters
   */
  @Override
  public WorkflowRunCount count(String queryTeam, Optional<Long> from, Optional<Long> to,  Optional<List<String>> queryLabels, Optional<List<String>> queryWorkflows) {
    List<String> wfRefs = relationshipServiceImpl.getFilteredRefs(Optional.of(RelationshipType.WORKFLOW), queryWorkflows, RelationshipLabel.BELONGSTO, RelationshipType.TEAM, queryTeam, false);
    LOGGER.debug("Workflow Refs: {}", wfRefs.toString());
    
    return engineClient.countWorkflowRuns(queryLabels, Optional.of(wfRefs), from, to);
  }
  
  /*
   * Start WorkflowRun
   * 
   * TODO: do we expose this one?
   */
  @Override
  public ResponseEntity<WorkflowRun> start(String team, String workflowRunId,
      Optional<WorkflowRunRequest> optRunRequest) {
    if (workflowRunId == null || workflowRunId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOWRUN_INVALID_REF);
    }
    if (relationshipServiceImpl.hasTeamRelationship(Optional.of(RelationshipType.WORKFLOWRUN),
        Optional.of(workflowRunId), RelationshipLabel.BELONGSTO, team, false)) {
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
  public ResponseEntity<WorkflowRun> finalize(String team, String workflowRunId) {
    if (workflowRunId == null || workflowRunId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOWRUN_INVALID_REF);
    }
    if (relationshipServiceImpl.hasTeamRelationship(Optional.of(RelationshipType.WORKFLOWRUN),
        Optional.of(workflowRunId), RelationshipLabel.BELONGSTO, team, false)) {
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
  public ResponseEntity<WorkflowRun> cancel(String team, String workflowRunId) {
    if (workflowRunId == null || workflowRunId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOWRUN_INVALID_REF);
    }
    if (relationshipServiceImpl.hasTeamRelationship(Optional.of(RelationshipType.WORKFLOWRUN),
        Optional.of(workflowRunId), RelationshipLabel.BELONGSTO, team, false)) {
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
  public ResponseEntity<WorkflowRun> retry(String team, String workflowRunId) {
    if (workflowRunId == null || workflowRunId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOWRUN_INVALID_REF);
    }
    if (relationshipServiceImpl.hasTeamRelationship(Optional.of(RelationshipType.WORKFLOWRUN),
        Optional.of(workflowRunId), RelationshipLabel.BELONGSTO, team, false)) {
      WorkflowRun wfRun = engineClient.retryWorkflowRun(workflowRunId);

      // Creates relationship with owning team
      relationshipServiceImpl.createNodeWithTeamConnection(RelationshipType.WORKFLOWRUN, wfRun.getId(), "", team, Optional.empty());
      return ResponseEntity.ok(wfRun);
    } else {
      throw new BoomerangException(BoomerangError.WORKFLOWRUN_INVALID_REF);
    }
  }
}
