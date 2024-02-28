package io.boomerang.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
import io.boomerang.client.WorkflowResponsePage;
import io.boomerang.data.entity.RelationshipEntity;
import io.boomerang.data.model.CurrentQuotas;
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
import io.boomerang.model.CanvasEdge;
import io.boomerang.model.CanvasEdgeData;
import io.boomerang.model.CanvasNode;
import io.boomerang.model.CanvasNodeData;
import io.boomerang.model.CanvasNodePosition;
import io.boomerang.model.WorkflowCanvas;
import io.boomerang.model.enums.RelationshipRef;
import io.boomerang.model.enums.RelationshipType;
import io.boomerang.model.enums.TriggerEnum;
import io.boomerang.model.enums.ref.TaskType;
import io.boomerang.model.enums.ref.WorkflowStatus;
import io.boomerang.model.ref.ChangeLogVersion;
import io.boomerang.model.ref.ParamLayers;
import io.boomerang.model.ref.RunParam;
import io.boomerang.model.ref.Task;
import io.boomerang.model.ref.TaskDependency;
import io.boomerang.model.ref.Trigger;
import io.boomerang.model.ref.Workflow;
import io.boomerang.model.ref.WorkflowCount;
import io.boomerang.model.ref.WorkflowRun;
import io.boomerang.model.ref.WorkflowSubmitRequest;
import io.boomerang.model.ref.WorkflowTrigger;
import io.boomerang.model.ref.WorkflowWorkspace;
import io.boomerang.model.ref.WorkflowWorkspaceSpec;
import io.boomerang.security.service.TokenServiceImpl;
import io.boomerang.util.DataAdapterUtil;
import io.boomerang.util.DataAdapterUtil.FieldType;
import io.boomerang.util.ParameterUtil;

/*
 * This service replicates the required calls for Engine WorkflowV1 APIs
 * 
 * It will - Check authorization using Relationships - Determines if to add or remove elements -
 * Forward call onto Engine (if applicable) - Converts response as needed for UI
 * 
 * TODO: migrate Triggers to an alternative workflow_triggers collection and use Relationships to
 * adjust
 */
@Service
public class WorkflowServiceImpl implements WorkflowService {

  private static final Logger LOGGER = LogManager.getLogger();

  @Autowired
  private EngineClient engineClient;

  @Autowired
  private RelationshipService relationshipService;

  @Autowired
  private ScheduleService scheduleService;

  @Autowired
  private ParameterManager parameterManager;

  @Autowired
  private SettingsService settingsService;

  @Autowired
  private TokenServiceImpl tokenService;
  
  @Autowired
  private TeamServiceImpl teamService;

  /*
   * Get Worklfow
   * 
   * No need to validate params as they are either defaulted or optional
   */
  @Override
  public ResponseEntity<Workflow> get(String workflowId, Optional<Integer> version,
      boolean withTasks) {
    if (workflowId == null || workflowId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }

    List<String> workflowRefs = relationshipService.getFilteredFromRefs(
        Optional.of(RelationshipRef.WORKFLOW), Optional.of(List.of(workflowId)),
        Optional.of(RelationshipType.BELONGSTO), Optional.empty(), Optional.empty());
    if (!workflowRefs.isEmpty()) {
      Workflow workflow = engineClient.getWorkflow(workflowId, version, withTasks);

      if (WorkflowStatus.deleted.equals(workflow.getStatus())) {
        ResponseEntity.notFound();
      }
      // Filter out sensitive values
      DataAdapterUtil.filterParamSpecValueByFieldType(workflow.getConfig(), workflow.getParams(),
          FieldType.PASSWORD.value());
      return ResponseEntity.ok(workflow);
    }
    throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
  }

  /*
   * Query for Workflows.
   */
  @Override
  public WorkflowResponsePage query(Optional<Integer> queryLimit, Optional<Integer> queryPage,
      Optional<Direction> querySort, Optional<List<String>> queryLabels,
      Optional<List<String>> queryStatus, Optional<List<String>> queryTeams,
      Optional<List<String>> queryWorkflows) {

    // Get Refs that request has access to
    List<String> workflowRefs =
        relationshipService.getFilteredFromRefs(Optional.of(RelationshipRef.WORKFLOW),
            queryWorkflows, Optional.of(RelationshipType.BELONGSTO),
            Optional.ofNullable(RelationshipRef.TEAM), queryTeams);
    if (workflowRefs.isEmpty()) {
      return new WorkflowResponsePage();
    }

    // Filter out deleted Workflows
    if (queryStatus.isPresent() && queryStatus.get().contains("deleted")) {
      queryStatus.get().remove("deleted");
    }

    WorkflowResponsePage response = engineClient.queryWorkflows(queryLimit, queryPage, querySort,
        queryLabels, queryStatus, Optional.of(workflowRefs));

    // Filter out sensitive values
    if (!response.getContent().isEmpty()) {
      response.getContent().forEach(w -> {
        if (w.getConfig() != null) {
          DataAdapterUtil.filterParamSpecValueByFieldType(w.getConfig(), w.getParams(),
              FieldType.PASSWORD.value());
        }
      });
    }

    return response;
  }

  /*
   * Retrieve the statistics for a specific period of time and filters
   */
  @Override
  public WorkflowCount count(Optional<Long> from, Optional<Long> to,
      Optional<List<String>> queryLabels, Optional<List<String>> queryTeams,
      Optional<List<String>> queryWorkflows) {
    List<String> refs = relationshipService.getFilteredFromRefs(
        Optional.of(RelationshipRef.WORKFLOW), queryWorkflows,
        Optional.of(RelationshipType.BELONGSTO), Optional.of(RelationshipRef.TEAM), queryTeams);
    LOGGER.debug("Query Ids: ", refs);

    //Handle no Workflows for Team. Otherwise the engine will return all workflows due to no filter
    if (refs.size() > 0) {
      return engineClient.countWorkflows(queryLabels, Optional.of(refs), from, to);
    }
    return new WorkflowCount();
  }

  /*
   * Create Workflow. Pass query onto EngineClient
   * 
   * No need to validate params as they are either defaulted or optional
   */
  @Override
  public ResponseEntity<Workflow> create(Workflow request, String team) {
    // Default Triggers
    validateTriggerDefaults(request);

    // Default Workspaces
    setUpWorkspaceDefaults(request);

    // TODO: add a check that they are prefixed with the current team scope OR are a valid Global
    // TaskTemplate

    Workflow workflow = engineClient.createWorkflow(request);
    // Create BELONGSTO relationship for mapping Workflow to Owner
    relationshipService.addRelationshipRef(RelationshipRef.WORKFLOW, workflow.getId(),
        RelationshipType.BELONGSTO, RelationshipRef.TEAM, Optional.of(team), Optional.empty());

    // Filter out sensitive values
    DataAdapterUtil.filterParamSpecValueByFieldType(workflow.getConfig(), workflow.getParams(),
        FieldType.PASSWORD.value());
    return ResponseEntity.ok(workflow);
  }

  private void setUpWorkspaceDefaults(Workflow request) {
    if (request.getWorkspaces() != null && !request.getWorkspaces().isEmpty()) {
      String maxSizeQuota = this.settingsService
          .getSettingConfig("teams", "max.team.workflow.storage").getValue().replace("Gi", "");
      Integer totalSize = 0;
      if (request.getWorkspaces().stream().anyMatch(ws -> ws.getType().equals("workflow"))) {
        WorkflowWorkspace workflowWorkspace = request.getWorkspaces().stream()
            .filter(ws -> ws.getType().equals("workflow")).findFirst().get();
        Integer index = request.getWorkspaces().indexOf(workflowWorkspace);
        workflowWorkspace.setName("workflow");
        workflowWorkspace.setOptional(false);
        WorkflowWorkspaceSpec workflowWorkspaceSpec = new WorkflowWorkspaceSpec();
        if (workflowWorkspace.getSpec() != null) {
          workflowWorkspaceSpec = (WorkflowWorkspaceSpec) workflowWorkspace.getSpec();
        }
        if (workflowWorkspaceSpec.getSize() == null) {
          workflowWorkspaceSpec.setSize(this.settingsService
              .getSettingConfig("workflow", "storage.size").getValue().replace("Gi", ""));
        }
        workflowWorkspace.setSpec(workflowWorkspaceSpec);
        totalSize += Integer.valueOf(workflowWorkspaceSpec.getSize());
        request.getWorkspaces().set(index, workflowWorkspace);
      }
      if (request.getWorkspaces().stream().anyMatch(ws -> ws.getType().equals("workflowrun"))) {
        WorkflowWorkspace workflowWorkspace = request.getWorkspaces().stream()
            .filter(ws -> ws.getType().equals("workflow")).findFirst().get();
        Integer index = request.getWorkspaces().indexOf(workflowWorkspace);
        workflowWorkspace.setName("workflowrun");
        workflowWorkspace.setOptional(false);
        WorkflowWorkspaceSpec workflowWorkspaceSpec = new WorkflowWorkspaceSpec();
        if (workflowWorkspace.getSpec() != null) {
          workflowWorkspaceSpec = (WorkflowWorkspaceSpec) workflowWorkspace.getSpec();
        }
        if (workflowWorkspaceSpec.getSize() == null) {
          workflowWorkspaceSpec.setSize(this.settingsService
              .getSettingConfig("workflowrun", "storage.size").getValue().replace("Gi", ""));
        }
        workflowWorkspace.setSpec(workflowWorkspaceSpec);
        totalSize += Integer.valueOf(workflowWorkspaceSpec.getSize());
        request.getWorkspaces().set(index, workflowWorkspace);
      }
      if (totalSize > Integer.valueOf(maxSizeQuota)) {
        throw new BoomerangException(BoomerangError.QUOTA_EXCEEDED, "Workspace Size Limit", totalSize, maxSizeQuota);
      }
    }
  }

  /*
   * Apply allows you to create a new version or override an existing Workflow as well as create new
   * Workflow with supplied ID
   */
  @Override
  public ResponseEntity<Workflow> apply(Workflow workflow, boolean replace, Optional<String> team) {
    if (workflow != null && workflow.getId() != null && !workflow.getId().isBlank()) {
      List<String> workflowRefs =
          relationshipService.getFilteredFromRefs(Optional.of(RelationshipRef.WORKFLOW),
              Optional.of(List.of(workflow.getId())), Optional.of(RelationshipType.BELONGSTO),
              Optional.of(RelationshipRef.TEAM), team.isPresent() ? Optional.of(List.of(team.get())): Optional.empty());
      if (!workflowRefs.isEmpty()) {
        updateScheduleTriggers(workflow,
            this.get(workflow.getId(), Optional.empty(), false).getBody().getTriggers());
        validateTriggerDefaults(workflow);
        Workflow appliedWorkflow = engineClient.applyWorkflow(workflow, replace);
        // Filter out sensitive values
        DataAdapterUtil.filterParamSpecValueByFieldType(appliedWorkflow.getConfig(),
            appliedWorkflow.getParams(), FieldType.PASSWORD.value());
        return ResponseEntity.ok(appliedWorkflow);
      }
    } 
    if (workflow != null && team.isPresent()) {
      workflow.setId(null);
      return this.create(workflow, team.get());
    }
    throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
  }

  /*
   * Submit Workflow to Run
   */
  @Override
  public WorkflowRun submit(String workflowId, WorkflowSubmitRequest request, boolean start) {
    if (workflowId == null || workflowId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
    List<String> workflowRefs = relationshipService.getFilteredFromRefs(Optional.of(RelationshipRef.WORKFLOW),
        Optional.of(List.of(workflowId)), Optional.of(RelationshipType.BELONGSTO), Optional.empty(), Optional.empty());
    // Check if Workflow can run (Quotas & Triggers)
    // TODO: check triggers allow submission
    if (!workflowRefs.isEmpty()) {
      return this.internalSubmit(workflowId, request, start);
    } else {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
  }
  
  /*
   * Submit WorkflowRun Internally by Team
   * 
   * Used by TriggerService
   */
  public void internalSubmitForTeam(WorkflowSubmitRequest request,
      boolean start, String teamRef) {
    List<String> wfRefs =
        relationshipService.getFilteredFromRefs(Optional.of(RelationshipRef.WORKFLOW),
            Optional.empty(), Optional.of(RelationshipType.BELONGSTO),
            Optional.of(RelationshipRef.WORKFLOW), Optional.of(List.of(teamRef)));
    
    wfRefs.forEach(r -> {
      this.internalSubmit(r, request, start);}
    );
  }
  
  /*
   * Submit WorkflowRun Internally
   * 
   * Caution: bypasses the authN and authZ and Relationship checks
   */
  public WorkflowRun internalSubmit(String workflowId, WorkflowSubmitRequest request,
      boolean start) {
    Optional<RelationshipEntity> teamRelationship = relationshipService.getRelationship(
        RelationshipRef.WORKFLOW, workflowId, RelationshipType.BELONGSTO);
    if (teamRelationship.isPresent()) {
      //Check Triggers - Throws Exception - Check first, as if trigger not enabled, no point in checking quotas
      canRunWithTrigger(workflowId, request.getTrigger(), request.getParams());
      //Check Quotas - Throws Exception
      canRunWithQuotas(teamRelationship.get().getToRef(), workflowId, Optional.of(request.getWorkspaces()));
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
      ParamLayers paramLayers = parameterManager.buildParamLayers(teamRelationship.get().getToRef(), workflowId);
      executionAnnotations.put("boomerang.io/global-params", paramLayers.getGlobalParams());
      executionAnnotations.put("boomerang.io/context-params", paramLayers.getContextParams());
      executionAnnotations.put("boomerang.io/team-params", paramLayers.getTeamParams());
      
      //Add Contextual Information such as team-name. Used by Engine and the AcquireTaskLock and other tasks to add a hidden prefix.
      executionAnnotations.put("boomerang.io/team-name", teamRelationship.get().getTo());
      request.getAnnotations().putAll(executionAnnotations);
      
      // TODO: figure out the storing of initiated by. Is that just a relationship?
      WorkflowRun wfRun = engineClient.submitWorkflow(workflowId, request, start);
      // TODO: FUTURE - Creates the relationship with the Workflow
      // relationshipService.addRelationshipRef(RelationshipRef.WORKFLOWRUN, wfRun.getId(),
      // RelationshipType.EXECUTIONOF, RelationshipRef.WORKFLOW, Optional.of(workflowId));

      // Creates the owning relationship with the team that owns the Workflow
      relationshipService.addRelationshipRef(RelationshipRef.WORKFLOWRUN, wfRun.getId(), RelationshipType.BELONGSTO,
          teamRelationship.get().getTo(), Optional.of(teamRelationship.get().getToRef()), Optional.empty());
      return wfRun;
    } else {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
  }

  /*
   * Retrieve a workflows changelog from all versions
   */

  @Override
  public ResponseEntity<List<ChangeLogVersion>> changelog(String workflowId) {
    if (workflowId == null || workflowId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }

    List<String> workflowRefs = relationshipService.getFilteredFromRefs(
        Optional.of(RelationshipRef.WORKFLOW), Optional.of(List.of(workflowId)),
        Optional.of(RelationshipType.BELONGSTO), Optional.empty(), Optional.empty());
    if (!workflowRefs.isEmpty()) {
      return ResponseEntity.ok(engineClient.getWorkflowChangeLog(workflowId));
    } else {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
  }

  /*
   * Set Workflow to Deleted Status
   * 
   * The Workflow is kept around so as to ensure that we can display the WorkflowRun in the Activity
   * screen.
   * 
   * Engine takes care of deleting Triggers & Workspaces
   */
  @Override
  public ResponseEntity<Void> delete(String workflowId) {
    if (workflowId == null || workflowId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }

    List<String> workflowRefs =
        relationshipService.getFilteredFromRefs(Optional.of(RelationshipRef.WORKFLOW),
            Optional.of(List.of(workflowId)), Optional.of(RelationshipType.BELONGSTO),
            Optional.of(RelationshipRef.TEAM), Optional.empty());
    if (!workflowRefs.isEmpty()) {
      engineClient.deleteWorkflow(workflowId);
      // Delete all Schedules
      try {
        scheduleService.deleteAllForWorkflow(workflowId);
      } catch (SchedulerException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

      // Delete all tokens
      tokenService.deleteAllForPrincipal(workflowId);

      return ResponseEntity.noContent().build();
    } else {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
  }

  /*
   * Export the Workflow as JSON
   */
  @Override
  public ResponseEntity<InputStreamResource> export(String workflowId) {
    final ResponseEntity<Workflow> workflow = this.get(workflowId, Optional.empty(), true);

    HttpHeaders headers = new HttpHeaders();
    headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
    headers.add("Pragma", "no-cache");
    headers.add("Expires", "0");

    headers.add("Content-Disposition", "attachment; filename=\"any_name.json\"");

    try {

      ObjectMapper mapper = new ObjectMapper();

      byte[] buf = mapper.writeValueAsBytes(workflow.getBody());

      return ResponseEntity.ok().contentLength(buf.length)
          .contentType(MediaType.parseMediaType("application/octet-stream"))
          .body(new InputStreamResource(new ByteArrayInputStream(buf)));
    } catch (IOException e) {

      LOGGER.error(e);
    }
    return null;
  }

  /*
   * Duplicate the Workflow and adjust name
   */
  @Override
  public ResponseEntity<Workflow> duplicate(String workflowId) {
    final ResponseEntity<Workflow> response = this.get(workflowId, Optional.empty(), true);
    Workflow workflow = response.getBody();
    workflow.setId(null);
    workflow.setName(workflow.getName() + " (duplicate)");
    Optional<RelationshipEntity> relEntity = relationshipService
        .getRelationship(RelationshipRef.WORKFLOW, workflowId, RelationshipType.BELONGSTO);
    return this.create(workflow, relEntity.get().getToRef());
  }

  /*
   * Retrieves Workflow with Tasks and converts / composes it to the appropriate model.
   * 
   * TODO: add a type to handle canvas or Tekton YAML etc etc
   */
  @Override
  public ResponseEntity<WorkflowCanvas> composeGet(String workflowId, Optional<Integer> version) {
    if (workflowId == null || workflowId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }

    List<String> workflowRefs =
        relationshipService.getFilteredFromRefs(Optional.of(RelationshipRef.WORKFLOW),
            Optional.of(List.of(workflowId)), Optional.of(RelationshipType.BELONGSTO),
            Optional.of(RelationshipRef.TEAM), Optional.empty());
    if (!workflowRefs.isEmpty()) {
      Workflow workflow = engineClient.getWorkflow(workflowId, version, true);
      return ResponseEntity.ok(convertWorkflowToCanvas(workflow));
    } else {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
  }

  /*
   * Retrieves Workflow with Tasks and converts / composes it to the appropriate model.
   * 
   * TODO: add a type to handle canvas or Tekton YAML etc etc
   */
  @Override
  public ResponseEntity<WorkflowCanvas> composeApply(WorkflowCanvas canvas, boolean replace,
      Optional<String> team) {
    if (canvas == null) {
      // TODO - better error
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }

    Workflow workflow = convertCanvasToWorkflow(canvas);
    ResponseEntity<Workflow> response = this.apply(workflow, replace, team);
    return ResponseEntity.ok(convertWorkflowToCanvas(response.getBody()));
  }

  @Override
  public List<String> getAvailableParameters(String workflowId) {
    if (workflowId == null || workflowId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
    List<String> teamRefs =
        relationshipService.getFilteredToRefs(Optional.of(RelationshipRef.WORKFLOW),
            Optional.of(List.of(workflowId)), Optional.of(RelationshipType.BELONGSTO),
            Optional.of(RelationshipRef.TEAM), Optional.empty());
    if (!teamRefs.isEmpty()) {
      Workflow workflow = engineClient.getWorkflow(workflowId, Optional.empty(), true);
      List<String> paramKeys =
          parameterManager.buildParamKeys(teamRefs.get(0), workflowId, workflow.getParams());
      workflow.getTasks().forEach(t -> {
        if (t.getResults() != null && !t.getResults().isEmpty()) {
          t.getResults().forEach(r -> {
            String key = "tasks." + t.getName() + ".results." + r.getName();
            paramKeys.add(key);
          });
        }
      });
      return paramKeys;
    } else {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
  }

  /*
   * Sets up the Triggers
   */
  private void validateTriggerDefaults(Workflow workflow) {
    if (Objects.isNull(workflow.getTriggers())) {
      //Manual trigger will be set to Enable = true.
      workflow.setTriggers(new WorkflowTrigger());
    }
    LOGGER.debug("Triggers: " + workflow.getTriggers());
    // Default to enabled for Workflows
    if (Objects.isNull(workflow.getTriggers().getManual())) {
      workflow.getTriggers().setManual(new Trigger(Boolean.TRUE));
    }
    if (Objects.isNull(workflow.getTriggers().getSchedule())) {
      workflow.getTriggers().setSchedule(new Trigger(Boolean.FALSE));
    }
    if (Objects.isNull(workflow.getTriggers().getWebhook())) {
      workflow.getTriggers().setWebhook(new Trigger(Boolean.FALSE));
    }
    if (Objects.isNull(workflow.getTriggers().getEvent())) {
      workflow.getTriggers().setEvent(new Trigger(Boolean.FALSE));
    }
    if (Objects.isNull(workflow.getTriggers().getGithub())) {
      workflow.getTriggers().setGithub(new Trigger(Boolean.FALSE));
    }
  }

  /*
   * Determine if Schedules need to be disabled based on triggers
   */
  private void updateScheduleTriggers(final Workflow request, WorkflowTrigger currentTriggers) {
    if (!Objects.isNull(request.getTriggers()) && !Objects.isNull(request.getTriggers().getSchedule()) && !Objects.isNull(currentTriggers) && !Objects.isNull(currentTriggers.getSchedule())) {
      boolean currentSchedulerEnabled = currentTriggers.getSchedule().getEnabled();
      boolean requestSchedulerEnabled = request.getTriggers().getSchedule().getEnabled();
      if (currentSchedulerEnabled != false && requestSchedulerEnabled == false) {
        scheduleService.disableAllTriggerSchedules(request.getId());
      } else if (currentSchedulerEnabled == false && requestSchedulerEnabled == true) {
        scheduleService.enableAllTriggerSchedules(request.getId());
      }
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

  /*
   * Converts from Workflow to Workflow Canvas
   */
  protected WorkflowCanvas convertWorkflowToCanvas(Workflow workflow) {
    List<Task> wfTasks = workflow.getTasks();
    WorkflowCanvas wfCanvas = new WorkflowCanvas(workflow);
    List<CanvasNode> nodes = new ArrayList<>();
    List<CanvasEdge> edges = new ArrayList<>();

    Map<String, TaskType> taskNamesToType =
        wfTasks.stream().collect(Collectors.toMap(Task::getName, Task::getType));
    Map<String, String> taskNameToNodeId = new HashMap<>();

    // Create Nodes
    wfTasks.forEach(task -> {
      CanvasNode node = new CanvasNode();
      node.setType(task.getType());
      if (task.getAnnotations().containsKey("boomerang.io/position")) {
        Map<String, Number> position =
            (Map<String, Number>) task.getAnnotations().get("boomerang.io/position");
        CanvasNodePosition nodePosition = new CanvasNodePosition();
        nodePosition.setX(position.get("x"));
        nodePosition.setY(position.get("y"));
        LOGGER.info("Node Position:" + nodePosition.toString());
        node.setPosition(nodePosition);
      }
      CanvasNodeData nodeData = new CanvasNodeData();
      nodeData.setName(task.getName());
      nodeData.setParams(task.getParams());
      nodeData.setResults(task.getResults());
      nodeData.setTemplateRef(task.getTemplateRef());
      nodeData.setTemplateVersion(task.getTemplateVersion());
      // TODO figure out template upgrades
      nodeData.setTemplateUpgradesAvailable(task.getTemplateUpgradesAvailable());
      node.setData(nodeData);
      nodes.add(node);
      taskNameToNodeId.put(task.getName(), node.getId());
    });
    wfCanvas.setNodes(nodes);

    // Creates Edges - depends on nodes as the IDs for each node are used in the edge mapping
    wfTasks.forEach(task -> {
      task.getDependencies().forEach(dep -> {
        CanvasEdge edge = new CanvasEdge();
        edge.setTarget(taskNameToNodeId.get(task.getName()));
        edge.setSource(taskNameToNodeId.get(dep.getTaskRef()));
        edge.setType(taskNamesToType.get(dep.getTaskRef()) != null
            ? taskNamesToType.get(dep.getTaskRef()).toString()
            : "");
        CanvasEdgeData edgeData = new CanvasEdgeData();
        edgeData.setExecutionCondition(dep.getExecutionCondition());
        edgeData.setDecisionCondition(dep.getDecisionCondition());
        edge.setData(edgeData);
        edges.add(edge);
      });
    });

    wfCanvas.setEdges(edges);

    return wfCanvas;
  }

  /*
   * Converts from Canvas Workflow to Workflow
   */
  protected Workflow convertCanvasToWorkflow(WorkflowCanvas canvas) {
    LOGGER.debug("Workflow Canvas: " + canvas.toString());
    Workflow workflow = new Workflow(canvas);
    LOGGER.debug("Converted Workfloed: " + workflow.toString());
    List<CanvasNode> nodes = canvas.getNodes();
    List<CanvasEdge> edges = canvas.getEdges();

    Map<String, String> nodeIdToTaskName =
        nodes.stream().collect(Collectors.toMap(n -> n.getId(), n -> n.getData().getName()));

    nodes.forEach(node -> {
      Task task = new Task();
      task.setName(node.getData().getName());
      task.setType(node.getType());
      Map<String, Number> position = new HashMap<>();
      position.put("x", node.getPosition().getX());
      position.put("y", node.getPosition().getY());
      task.getAnnotations().put("boomerang.io/position", position);
      task.setParams(node.getData().getParams());
      task.setResults(node.getData().getResults());
      task.setTemplateRef(node.getData().getTemplateRef());
      task.setTemplateVersion(node.getData().getTemplateVersion());

      List<TaskDependency> dependencies = new LinkedList<>();
      edges.stream().filter(e -> e.getTarget().equals(node.getId())).forEach(e -> {
        TaskDependency dep = new TaskDependency();
        dep.setTaskRef(nodeIdToTaskName.get(e.getSource()));
        dep.setDecisionCondition(e.getData().getDecisionCondition());
        dep.setExecutionCondition(e.getData().getExecutionCondition());
        dependencies.add(dep);
      });
      task.setDependencies(dependencies);
      workflow.getTasks().add(task);
    });

    return workflow;
  }
}
