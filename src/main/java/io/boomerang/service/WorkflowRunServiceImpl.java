package io.boomerang.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import io.boomerang.client.EngineClient;
import io.boomerang.client.WorkflowRunResponsePage;
import io.boomerang.data.entity.RelationshipEntity;
import io.boomerang.data.model.CurrentQuotas;
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
import io.boomerang.model.enums.RelationshipRef;
import io.boomerang.model.enums.RelationshipType;
import io.boomerang.model.enums.TriggerEnum;
import io.boomerang.model.enums.ref.WorkflowStatus;
import io.boomerang.model.ref.ParamLayers;
import io.boomerang.model.ref.Workflow;
import io.boomerang.model.ref.WorkflowRun;
import io.boomerang.model.ref.WorkflowRunCount;
import io.boomerang.model.ref.WorkflowRunInsight;
import io.boomerang.model.ref.WorkflowRunRequest;
import io.boomerang.model.ref.WorkflowRunSubmitRequest;
import io.boomerang.model.ref.WorkflowTrigger;

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
  private TeamServiceImpl teamService;
  
  @Autowired
  private SettingsService settingsService;
  
  @Autowired
  private ParameterManager parameterManager;
  
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

    List<String> workflowRunRefs = relationshipService.getFilteredFromRefs(Optional.of(RelationshipRef.WORKFLOWRUN), Optional.of(List.of(workflowRunId)), Optional.of(RelationshipType.BELONGSTO), Optional.of(RelationshipRef.TEAM), Optional.empty());
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
        Optional.of(RelationshipRef.WORKFLOWRUN), queryWorkflowRuns,
        Optional.of(RelationshipType.BELONGSTO), Optional.of(RelationshipRef.TEAM), queryTeams);
    
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
    List<String> workflowRunRefs = relationshipService.getFilteredFromRefs(Optional.of(RelationshipRef.WORKFLOWRUN), Optional.empty(), Optional.of(RelationshipType.BELONGSTO), Optional.of(RelationshipRef.TEAM), queryTeams);
    LOGGER.debug("Query Ids: ", workflowRunRefs);
    
    return engineClient.insightWorkflowRuns(queryLabels, Optional.of(workflowRunRefs), queryWorkflows, from, to);
  }
  
  /*
   * Retrieve the insights / statistics for a specific period of time and filters
   */
  @Override
  public WorkflowRunCount count(Optional<Long> from, Optional<Long> to,  Optional<List<String>> queryLabels, Optional<List<String>> queryTeams, Optional<List<String>> queryWorkflows) {
    List<String> refs = relationshipService.getFilteredFromRefs(Optional.of(RelationshipRef.WORKFLOW), queryWorkflows, Optional.of(RelationshipType.BELONGSTO), Optional.of(RelationshipRef.TEAM), queryTeams);
    LOGGER.debug("Query Ids: ", refs);
    
    return engineClient.countWorkflowRuns(queryLabels, Optional.of(refs), from, to);
  }

  /*
   * Submit WorkflowRun
   */
  @Override
  public ResponseEntity<WorkflowRun> submit(WorkflowRunSubmitRequest request, boolean start) {
    if (request != null && request.getWorkflowRef() == null || request.getWorkflowRef().isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
    List<String> workflowRefs = relationshipService.getFilteredFromRefs(Optional.of(RelationshipRef.WORKFLOW),
        Optional.of(List.of(request.getWorkflowRef())), Optional.of(RelationshipType.BELONGSTO), Optional.empty(), Optional.empty());
    // Check if Workflow can run (Quotas & Triggers)
    // TODO: check triggers allow submission
    if (!workflowRefs.isEmpty()) {
      return this.internalSubmit(request, start);
    } else {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
  }
  
  /*
   * Submit WorkflowRun Internally
   * 
   * Caution: bypasses the authN and authZ and Relationship checks
   * 
   * Used by ExecuteScheduleJob
   */
  public ResponseEntity<WorkflowRun> internalSubmit(WorkflowRunSubmitRequest request,
      boolean start) {
    Optional<RelationshipEntity> teamRelationship = relationshipService.getRelationship(
        RelationshipRef.WORKFLOW, request.getWorkflowRef(), RelationshipType.BELONGSTO);
    if (teamRelationship.isPresent()) {
      //Check Quotas - Throws Exception
      canRunWithQuotas(teamRelationship.get().getToRef(), request.getWorkflowRef());
      //Check Triggers - Throws Exception
      canRunWithTrigger(request.getWorkflowRef(), Optional.of(request.getTrigger()));
      // Set Workflow & Task Debug
      if (!Objects.isNull(request.getDebug())) {
        boolean enableDebug = false;
        String setting =
            this.settingsService.getSettingConfig("task", "debug").getValue();
        if (setting != null) {
          enableDebug = Boolean.parseBoolean(setting);
        }
        request.setDebug(Boolean.valueOf(enableDebug));
      }
      // Set Workflow Timeout
      if (!Objects.isNull(request.getTimeout())) {
        String setting = this.settingsService
            .getSettingConfig("task", "default.timeout").getValue();
        if (setting != null) {
          request.setTimeout(Long.valueOf(setting));
        }
      }
      //These annotations are processed by the DAGUtility in the Engine
      Map<String, Object> executionAnnotations = new HashMap<>();
      executionAnnotations.put("boomerang.io/task-deletion", this.settingsService.getSettingConfig("task", "deletion.policy").getValue());
      executionAnnotations.put("boomerang.io/task-default-image", this.settingsService.getSettingConfig("task", "default.image").getValue());
      
      //Add Context, Global, and Team parameters to the WorkflowRun request
      ParamLayers paramLayers = parameterManager.buildParamLayers(teamRelationship.get().getToRef(), request.getWorkflowRef());
      executionAnnotations.put("boomerang.io/global-params", paramLayers.getGlobalParams());
      executionAnnotations.put("boomerang.io/context-params", paramLayers.getContextParams());
      executionAnnotations.put("boomerang.io/team-params", paramLayers.getTeamParams());
      
      //Add Contextual Information such as team-name. Used by Engine and the AcquireTaskLock and other tasks to add a hidden prefix.
      executionAnnotations.put("boomerang.io/team-name", teamRelationship.get().getTo());
      request.getAnnotations().putAll(executionAnnotations);
      
      // TODO: figure out the storing of initiated by. Is that just a relationship?
      WorkflowRun wfRun = engineClient.submitWorkflowRun(request, start);
      // TODO: FUTURE - Creates the relationship with the Workflow
      // relationshipService.addRelationshipRef(RelationshipRef.WORKFLOWRUN, wfRun.getId(),
      // RelationshipType.EXECUTIONOF, RelationshipRef.WORKFLOW, Optional.of(workflowId));

      // Creates the owning relationship with the team that owns the Workflow
      relationshipService.addRelationshipRef(RelationshipRef.WORKFLOWRUN, wfRun.getId(), RelationshipType.BELONGSTO,
          teamRelationship.get().getTo(), Optional.of(teamRelationship.get().getToRef()), Optional.empty());
      return ResponseEntity.ok(wfRun);
    } else {
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
      throw new BoomerangException(BoomerangError.WORKFLOWRUN_INVALID_REF);
    }
    List<String> workflowRunRefs = relationshipService.getFilteredFromRefs(Optional.of(RelationshipRef.WORKFLOWRUN), Optional.of(List.of(workflowRunId)), Optional.of(RelationshipType.BELONGSTO), Optional.empty(), Optional.empty());
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
    List<String> workflowRunRefs = relationshipService.getFilteredFromRefs(Optional.of(RelationshipRef.WORKFLOWRUN), Optional.of(List.of(workflowRunId)), Optional.of(RelationshipType.BELONGSTO), Optional.empty(), Optional.empty());
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
    List<String> workflowRunRefs = relationshipService.getFilteredFromRefs(Optional.of(RelationshipRef.WORKFLOWRUN), Optional.of(List.of(workflowRunId)), Optional.of(RelationshipType.BELONGSTO), Optional.empty(), Optional.empty());
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
    List<String> workflowRunRefs = relationshipService.getFilteredFromRefs(Optional.of(RelationshipRef.WORKFLOWRUN), Optional.of(List.of(workflowRunId)), Optional.of(RelationshipType.BELONGSTO), Optional.empty(), Optional.empty());
    if (!workflowRunRefs.isEmpty()) {
      WorkflowRun wfRun = engineClient.retryWorkflowRun(workflowRunId);
      return ResponseEntity.ok(wfRun);
    } else {
      throw new BoomerangException(BoomerangError.WORKFLOWRUN_INVALID_REF);
    }
  }

  /*
   * Check if the Team Quotas allow a Workflow to run
   */
  private void canRunWithQuotas(String teamId, String workflowId) {
    if (settingsService.getSettingConfig("features", "workflowQuotas").getBooleanValue()) {
      CurrentQuotas quotas = teamService.getQuotas(teamId);
      if (quotas.getCurrentConcurrentWorkflows() > quotas.getMaxConcurrentWorkflows()) {
        throw new BoomerangException(BoomerangError.QUOTA_EXCEEDED, "Concurrent Workflows", quotas.getCurrentConcurrentWorkflows(), quotas.getMaxConcurrentWorkflows());
      } else if (quotas.getCurrentRunTotalDuration() > quotas.getMaxWorkflowExecutionMonthly()) {
        throw new BoomerangException(BoomerangError.QUOTA_EXCEEDED, "Concurrent Workflows", quotas.getCurrentRunTotalDuration(), quotas.getMaxWorkflowExecutionMonthly());
      }
    }
  }

  /*
   * Checks if the Workflow can be executed based on an active workflow and enabled triggers.
   * 
   * If trigger is Manual or Schedule then a deeper check is used to check if those triggers are
   * enabled.
   * 
   * @param workflowId the Workflows unique ID
   * 
   * @param Trigger an optional Trigger object
   */
  protected void canRunWithTrigger(String workflowId, Optional<String> trigger) {
    // Check no further if trigger not provided
    if (trigger.isPresent() && !trigger.get().isEmpty()) {
      // Check if Workflow exists and is active. Then check triggers are enabled.
      //TODO determine a less expensive way to get the Workflow Triggers
      Workflow workflow = engineClient.getWorkflow(workflowId, Optional.empty(), false);
      if (!Objects.isNull(workflow)) {
        WorkflowTrigger triggers = workflow.getTriggers();
        if (TriggerEnum.manual.toString().equals(trigger.get()) && triggers.getManual() != null && triggers.getManual().getEnable()) {
          return;
        } else if (TriggerEnum.scheduler.toString().equals(trigger.get())
            && triggers.getScheduler() != null && triggers.getScheduler().getEnable()) {
          return;
        } else if (TriggerEnum.webhook.toString().equals(trigger.get())
            && triggers.getWebhook() != null && triggers.getWebhook().getEnable()) {
          return;
        } else if (TriggerEnum.custom.toString().equals(trigger.get())
            && triggers.getCustom() != null && triggers.getCustom().getEnable()) {
          return;
        } else {
          throw new BoomerangException(BoomerangError.WORKFLOWRUN_TRIGGER_DISABLED);
        }
      }
    }
  }
}
