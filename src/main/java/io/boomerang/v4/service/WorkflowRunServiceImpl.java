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
import io.boomerang.v4.data.entity.RelationshipEntity;
import io.boomerang.v4.data.model.CurrentQuotas;
import io.boomerang.v4.model.enums.RelationshipRef;
import io.boomerang.v4.model.enums.RelationshipType;
import io.boomerang.v4.model.ref.WorkflowRun;
import io.boomerang.v4.model.ref.WorkflowRunInsight;
import io.boomerang.v4.model.ref.WorkflowRunRequest;
import io.boomerang.v4.model.ref.WorkflowRunSubmitRequest;

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
  
  @Autowired
  private TeamService teamService;
  
  @Autowired
  private SettingsService settingsService;
  
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

    List<String> workflowRunRefs = relationshipService.getFilteredRefs(Optional.of(RelationshipRef.WORKFLOWRUN), Optional.of(List.of(workflowRunId)), Optional.of(RelationshipType.BELONGSTO), Optional.empty(), Optional.empty());
    if (!workflowRunRefs.isEmpty()) {
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
      Optional<List<String>> queryStatus, Optional<List<String>> queryPhase, Optional<List<String>> queryTeams, Optional<List<String>> queryRefs) {
    // Get Refs that request has access to
    List<String> workflowRunRefs = relationshipService.getFilteredRefs(Optional.of(RelationshipRef.WORKFLOWRUN),
        queryRefs, Optional.of(RelationshipType.BELONGSTO), Optional.of(RelationshipRef.TEAM), queryTeams);
    
    if (!workflowRunRefs.isEmpty()) {
      return engineClient.queryWorkflowRuns(page, limit, sort, queryLabels, queryStatus, queryPhase, Optional.of(workflowRunRefs));
    } else {
      //TODO: do we want to return invalid ref or unauthorized
      throw new BoomerangException(BoomerangError.WORKFLOW_RUN_INVALID_REF);
    }
  }
  
  /*
   * Retrieve the insights / statistics for a specific period of time and filters
   */
  @Override
  public WorkflowRunInsight insight(Optional<Long> from, Optional<Long> to,  Optional<List<String>> queryLabels, Optional<List<String>> queryTeams) {
    List<String> workflowRunRefs = relationshipService.getFilteredRefs(Optional.of(RelationshipRef.WORKFLOWRUN), Optional.empty(), Optional.of(RelationshipType.BELONGSTO), Optional.empty(), Optional.empty());
    LOGGER.debug("Query Ids: ", workflowRunRefs);
    
    return engineClient.insightWorkflowRuns(queryLabels, Optional.of(workflowRunRefs), from, to);
  }

  /*
   * Submit WorkflowRun
   */
  @Override
  public ResponseEntity<WorkflowRun> submit(WorkflowRunSubmitRequest request, boolean start) {
    if (request != null && request.getWorkflowRef() == null || request.getWorkflowRef().isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
    List<String> workflowRefs = relationshipService.getFilteredRefs(Optional.of(RelationshipRef.WORKFLOW),
        Optional.of(List.of(request.getWorkflowRef())), Optional.of(RelationshipType.BELONGSTO), Optional.empty(), Optional.empty());
    // Check if Workflow can run (Quotas & Triggers)
    // TODO: check triggers allow submission
    if (!workflowRefs.isEmpty() && canRunWithQuotas(request.getWorkflowRef())) {
      // Set Workflow & Task Debug
      if (request.getDebug() == null) {
        boolean enableDebug = false;
        String setting =
            this.settingsService.getSetting("controller", "enable.debug").getValue();
        if (setting != null) {
          enableDebug = Boolean.parseBoolean(setting);
        }
        request.setDebug(Boolean.valueOf(enableDebug));
      }
      // Set Workflow Timeout
      if (request.getTimeout() == null) {
        String setting =
            this.settingsService.getSetting("controller", "task.timeout.configuration").getValue();
        if (setting != null) {
          request.setTimeout(Long.valueOf(setting));
        }
      }
      
      WorkflowRun wfRun = engineClient.submitWorkflowRun(request, start);
       // TODO: FUTURE - Creates the relationship with the Workflow
//       relationshipService.addRelationshipRef(RelationshipRef.WORKFLOWRUN, wfRun.getId(), RelationshipType.EXECUTIONOF, RelationshipRef.WORKFLOW, Optional.of(workflowId));
      
      // Creates the owning relationship with the team that owns the Workflow
      Optional<RelationshipEntity> relEntity = relationshipService.getRelationship(RelationshipRef.WORKFLOW, wfRun.getWorkflowRef(), RelationshipType.BELONGSTO);
      relationshipService.addRelationshipRef(RelationshipRef.WORKFLOWRUN, wfRun.getId(), relEntity.get().getTo(), Optional.of(relEntity.get().getToRef()));
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
   */
  @Override
  public ResponseEntity<WorkflowRun> start(String workflowRunId,
      Optional<WorkflowRunRequest> optRunRequest) {
    if (workflowRunId == null || workflowRunId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_RUN_INVALID_REF);
    }
    List<String> workflowRunRefs = relationshipService.getFilteredRefs(Optional.of(RelationshipRef.WORKFLOWRUN), Optional.of(List.of(workflowRunId)), Optional.of(RelationshipType.BELONGSTO), Optional.empty(), Optional.empty());
    if (!workflowRunRefs.isEmpty()) {
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
    List<String> workflowRunRefs = relationshipService.getFilteredRefs(Optional.of(RelationshipRef.WORKFLOWRUN), Optional.of(List.of(workflowRunId)), Optional.of(RelationshipType.BELONGSTO), Optional.empty(), Optional.empty());
    if (!workflowRunRefs.isEmpty()) {
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
    List<String> workflowRunRefs = relationshipService.getFilteredRefs(Optional.of(RelationshipRef.WORKFLOWRUN), Optional.of(List.of(workflowRunId)), Optional.of(RelationshipType.BELONGSTO), Optional.empty(), Optional.empty());
    if (!workflowRunRefs.isEmpty()) {
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
    List<String> workflowRunRefs = relationshipService.getFilteredRefs(Optional.of(RelationshipRef.WORKFLOWRUN), Optional.of(List.of(workflowRunId)), Optional.of(RelationshipType.BELONGSTO), Optional.empty(), Optional.empty());
    if (!workflowRunRefs.isEmpty()) {
      WorkflowRun wfRun = engineClient.retryWorkflowRun(workflowRunId);
      return ResponseEntity.ok(wfRun);
    } else {
      //TODO: do we want to return invalid ref or unauthorized
      throw new BoomerangException(BoomerangError.WORKFLOW_RUN_INVALID_REF);
    }
  }

  /*
   * Check if the quotas allow a Workflow to run
   */
  private boolean canRunWithQuotas(String workflowId) {
    if (!settingsService.getSetting("features", "workflowQuotas").getBooleanValue()) {
      return true;
    }

    Optional<RelationshipEntity> relEntities = relationshipService.getRelationship(RelationshipRef.WORKFLOWRUN,
        workflowId, RelationshipType.BELONGSTO);
    if (relEntities.isPresent() && relEntities.get().getTo().equals(RelationshipRef.TEAM)) {
      CurrentQuotas quotas = teamService.getQuotas(relEntities.get().getToRef()).getBody();
      if (quotas.getCurrentConcurrentWorkflows() <= quotas.getMaxConcurrentWorkflows()
          || quotas.getCurrentWorkflowExecutionMonthly() <= quotas
              .getMaxWorkflowExecutionMonthly()) {
        return true;
      }
    }
    return false;
  }
}
