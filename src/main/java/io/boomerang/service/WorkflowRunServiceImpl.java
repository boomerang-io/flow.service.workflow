package io.boomerang.service;

import java.lang.reflect.Field;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import io.boomerang.client.EngineClient;
import io.boomerang.client.WorkflowRunResponsePage;
import io.boomerang.data.entity.RelationshipEntity;
import io.boomerang.data.model.CurrentQuotas;
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
import io.boomerang.model.enums.RelationshipRef;
import io.boomerang.model.enums.RelationshipType;
import io.boomerang.model.enums.TriggerEnum;
import io.boomerang.model.ref.ParamLayers;
import io.boomerang.model.ref.RunParam;
import io.boomerang.model.ref.Trigger;
import io.boomerang.model.ref.Workflow;
import io.boomerang.model.ref.WorkflowRun;
import io.boomerang.model.ref.WorkflowRunCount;
import io.boomerang.model.ref.WorkflowRunInsight;
import io.boomerang.model.ref.WorkflowRunRequest;
import io.boomerang.model.ref.WorkflowRunSubmitRequest;
import io.boomerang.model.ref.WorkflowTrigger;
import io.boomerang.model.ref.WorkflowWorkspace;
import io.boomerang.util.ParameterUtil;

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
      //Check Triggers - Throws Exception - Check first, as if trigger not enabled, no point in checking quotas
      canRunWithTrigger(request.getWorkflowRef(), request.getTrigger(), request.getParams());
      //Check Quotas - Throws Exception
      canRunWithQuotas(teamRelationship.get().getToRef(), request.getWorkflowRef(), Optional.of(request.getWorkspaces()));
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
   * Submit WorkflowRun Internally by Team
   * 
   * Used by TriggerService
   */
  public void internalSubmitForTeam(WorkflowRunSubmitRequest request,
      boolean start, String teamRef) {
    List<String> wfRefs =
        relationshipService.getFilteredFromRefs(Optional.of(RelationshipRef.WORKFLOW),
            Optional.empty(), Optional.of(RelationshipType.BELONGSTO),
            Optional.of(RelationshipRef.WORKFLOW), Optional.of(List.of(teamRef)));
    
    wfRefs.forEach(r -> {
      request.setWorkflowRef(r);
      this.internalSubmit(request, start);}
    );
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
   * 
   * TODO: add additional checks for not exceeding Workspace size for any Workspace that is saved on the Workflow
   */
  private void canRunWithQuotas(String teamId, String workflowId, Optional<List<WorkflowWorkspace>> workspaces) {
    if (settingsService.getSettingConfig("features", "workflowQuotas").getBooleanValue()) {
      CurrentQuotas quotas = teamService.getQuotas(teamId);
      if (quotas.getCurrentConcurrentWorkflows() > quotas.getMaxConcurrentRuns()) {
        throw new BoomerangException(BoomerangError.QUOTA_EXCEEDED, "Concurrent Workflows", quotas.getCurrentConcurrentWorkflows(), quotas.getMaxConcurrentRuns());
      } else if (quotas.getCurrentRuns() > quotas.getMaxWorkflowRunMonthly()) {
        throw new BoomerangException(BoomerangError.QUOTA_EXCEEDED, "Number of Runs (executions)", quotas.getCurrentRuns(), quotas.getMaxWorkflowRunMonthly());
      } else if (workspaces.isPresent() && workspaces.get().size() > 0) {
        workspaces.get().forEach(ws -> {
          try {
            Field sizeField = ws.getSpec().getClass().getDeclaredField("size");
            String size = (String) sizeField.get(ws.getSpec());
            if (Integer.valueOf(size) > quotas.getMaxWorkflowStorage()) {
              throw new BoomerangException(BoomerangError.QUOTA_EXCEEDED, "Requested Workspace size", size, quotas.getMaxWorkflowStorage());
            }
          } catch (NoSuchFieldException | IllegalAccessException ex) {
            //Do nothing
          }
        });
      }
    }
  }

  /*
   * Checks if the Workflow can be executed based on an active workflow and enabled triggers.
   * 
   * @param workflowId the Workflows unique ID
   * 
   * @param Trigger an optional Trigger object
   */
  protected void canRunWithTrigger(String workflowId, TriggerEnum runTrigger, List<RunParam> params) {
    // Check no further if trigger not provided
    if (!Objects.isNull(runTrigger)) {
      // Check if Workflow exists and is active. Then check triggers are enabled.
      Workflow workflow = engineClient.getWorkflow(workflowId, Optional.empty(), false);
      if (!Objects.isNull(workflow)) {
        WorkflowTrigger triggers = workflow.getTriggers();
        if (TriggerEnum.manual.equals(runTrigger) && triggers.getManual().getEnabled()) {
          return;
        } else if (TriggerEnum.schedule.equals(runTrigger)
            && triggers.getSchedule().getEnabled()) {
          return;
        } else if (TriggerEnum.webhook.equals(runTrigger)
            && triggers.getWebhook().getEnabled()) {
          return;
        } else if (TriggerEnum.event.equals(runTrigger)
            && triggers.getEvent().getEnabled()) {
            Trigger trigger = triggers.getEvent();
            validateTriggerConditions(ParameterUtil.getValue(params, "event"), trigger);
            return;
        } else if (TriggerEnum.github.equals(runTrigger) && triggers.getWebhook().getEnabled()) {
          Trigger trigger = triggers.getWebhook();
          validateTriggerConditions(ParameterUtil.getValue(params, "payload"), trigger);
          return;
        }
        throw new BoomerangException(BoomerangError.WORKFLOWRUN_TRIGGER_DISABLED);
      }
    }
  }

  /*
   * Implements the logic checks for each WorkflowTriggerCondition operation type
   */
  private void validateTriggerConditions(Object data,
      Trigger trigger) {
    if (!trigger.getConditions().isEmpty()) {
      //Convert Object to JsonNode and configure for JsonPath
      ObjectMapper mapper = new ObjectMapper();
      JsonNode jData = mapper.valueToTree(data);
      Configuration jsonConfig =
          Configuration.builder().mappingProvider(new JacksonMappingProvider())
              .jsonProvider(new JacksonJsonNodeJsonProvider())
              .options(Option.DEFAULT_PATH_LEAF_TO_NULL).build();
      DocumentContext jsonContext = JsonPath.using(jsonConfig).parse(jData);
      
      //Determine all conditions match
      trigger.getConditions().forEach(con -> {
        Boolean canRun = Boolean.TRUE;
        String field = jsonContext.read(con.getField());
        switch (con.getOperation()) {
          case matches -> {
            canRun = field.matches(con.getValue());
          }
          case equals -> {
            canRun = field.equals(con.getValue());
          }
          case in -> {
            canRun = con.getValues().contains(field);
          }
        }
        if (!canRun) {
          throw new BoomerangException(BoomerangError.WORKFLOWRUN_TRIGGER_DISABLED);
        }
      });
    }
  }
}
