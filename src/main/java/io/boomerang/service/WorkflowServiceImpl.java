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
import io.boomerang.data.model.CurrentQuotas;
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
import io.boomerang.model.CanvasEdge;
import io.boomerang.model.CanvasEdgeData;
import io.boomerang.model.CanvasNode;
import io.boomerang.model.CanvasNodeData;
import io.boomerang.model.CanvasNodePosition;
import io.boomerang.model.WorkflowCanvas;
import io.boomerang.model.enums.RelationshipLabel;
import io.boomerang.model.enums.RelationshipType;
import io.boomerang.model.enums.TriggerEnum;
import io.boomerang.model.enums.ref.TaskType;
import io.boomerang.model.ref.ChangeLogVersion;
import io.boomerang.model.ref.ParamLayers;
import io.boomerang.model.ref.RunParam;
import io.boomerang.model.ref.Trigger;
import io.boomerang.model.ref.Workflow;
import io.boomerang.model.ref.WorkflowCount;
import io.boomerang.model.ref.WorkflowRun;
import io.boomerang.model.ref.WorkflowSubmitRequest;
import io.boomerang.model.ref.WorkflowTask;
import io.boomerang.model.ref.WorkflowTaskDependency;
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
  
  private static final String TASK_REF_SEPERATOR = "/";
  public static final String TEAMS_SETTINGS_KEY = "teams";
  public static final String FEATURES_SETTINGS_KEY = "features";
  public static final String FEATURES_TEAM_QUOTA = "teamQuotas";
  public static final String QUOTA_MAX_WORKFLOW_DURATION = "max.workflow.duration";
  public static final String QUOTA_MAX_WORKFLOW_STORAGE = "max.workflow.storage";
  public static final String QUOTA_MAX_WORKFLOWRUN_STORAGE = "max.workflowrun.storage";
  public static final String TASK_SETTINGS_KEY = "task";

  @Autowired
  private EngineClient engineClient;

  @Autowired
  private RelationshipServiceImpl relationshipServiceImpl;

  @Autowired
  private ScheduleServiceImpl scheduleService;

  @Autowired
  private ParameterManager parameterManager;

  @Autowired
  private SettingsService settingsService;

  @Autowired
  private ActionService actionService;

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
  public Workflow get(String team, String workflowId, Optional<Integer> version,
      boolean withTasks) {
    if (workflowId == null || workflowId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
    Workflow workflow = internalGet(team, workflowId, version, withTasks);

    // Filter out sensitive values
    DataAdapterUtil.filterParamSpecValueByFieldType(workflow.getConfig(), workflow.getParams(),
        FieldType.PASSWORD.value());
    
    return workflow;
  }
  
  /*
   * This method is used by the compose methods but ensures the password values are not yet filtered.
   */
  private Workflow internalGet(String team, String workflowId, Optional<Integer> version,
      boolean withTasks) {
    if (relationshipServiceImpl.hasTeamRelationship(Optional.of(RelationshipType.WORKFLOW),
        Optional.of(workflowId), RelationshipLabel.BELONGSTO, team, false)) {
      Workflow workflow = engineClient.getWorkflow(workflowId, version, withTasks);
      
      //Convert Workflow TaskRefs to Slugs
      convertTaskRefsToSlugs(team, workflow);
      return workflow;
    }
    throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
  }

  /*
   * Query for Workflows.
   */
  @Override
  public WorkflowResponsePage query(String queryTeam, Optional<Integer> queryLimit, Optional<Integer> queryPage,
      Optional<Direction> querySort, Optional<List<String>> queryLabels,
      Optional<List<String>> queryStatus, Optional<List<String>> queryWorkflows) {

    // Get Refs that request has access to
    List<String> refs = relationshipServiceImpl.getFilteredRefs(Optional.of(RelationshipType.WORKFLOW), queryWorkflows, RelationshipLabel.BELONGSTO, RelationshipType.TEAM, queryTeam, false);
    LOGGER.debug("Workflow Refs: {}", refs.toString());
    if (refs == null || refs.size() == 0) {
      return new WorkflowResponsePage();
    }

    WorkflowResponsePage response = engineClient.queryWorkflows(queryLimit, queryPage, querySort,
        queryLabels, queryStatus, Optional.of(refs));

    // Filter out sensitive values
    if (!response.getContent().isEmpty()) {
      response.getContent().forEach(w -> {
        if (w.getConfig() != null) {
          DataAdapterUtil.filterParamSpecValueByFieldType(w.getConfig(), w.getParams(),
              FieldType.PASSWORD.value());
        }
        //Convert Workflow TaskRefs to Slugs
        convertTaskRefsToSlugs(queryTeam, w);
      });
    }

    return response;
  }

  /*
   * Retrieve the statistics for a specific period of time and filters
   */
  @Override
  public WorkflowCount count(String queryTeam, Optional<Long> from, Optional<Long> to,
      Optional<List<String>> queryLabels, Optional<List<String>> queryWorkflows) {
    // Get Refs that request has access to
    List<String> refs = relationshipServiceImpl.getFilteredRefs(Optional.of(RelationshipType.WORKFLOW), queryWorkflows, RelationshipLabel.BELONGSTO, RelationshipType.TEAM, queryTeam, false);
    LOGGER.debug("Workflow Refs: {}", refs.toString());

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
//  @Audit(scope = PermissionScope.WORKFLOW)
  public Workflow create(String team, Workflow request) {
    // Check creation quotas
    canCreateWithQuotas(team);
    
    // Default Triggers
    validateTriggerDefaults(request);

    // Default Workspaces
    setUpWorkspaceDefaults(request);

    // Convert TaskRefs to IDs
    convertTaskRefsToIds(team, request);

    Workflow workflow = engineClient.createWorkflow(request);

    // Create Relationship
    relationshipServiceImpl.createNodeWithTeamConnection(RelationshipType.WORKFLOW, workflow.getId(), "", team, Optional.empty());

    // Filter out sensitive values
    DataAdapterUtil.filterParamSpecValueByFieldType(workflow.getConfig(), workflow.getParams(),
        FieldType.PASSWORD.value());

    //Convert Workflow TaskRefs to Slugs
    convertTaskRefsToSlugs(team, workflow);
    
    return workflow;
  }

  private void setUpWorkspaceDefaults(Workflow request) {
    boolean quotasEnabled = settingsService.getSettingConfig(FEATURES_SETTINGS_KEY, FEATURES_TEAM_QUOTA).getBooleanValue();
    if (request.getWorkspaces() != null && !request.getWorkspaces().isEmpty()) {
      //Workflow Storage
      for (WorkflowWorkspace ws : request.getWorkspaces()) {
        if (ws.getType().equals("workflow")) {
          String maxStorageSizeQuota = this.settingsService
              .getSettingConfig(TEAMS_SETTINGS_KEY, QUOTA_MAX_WORKFLOW_STORAGE).getValue().replace("Gi", "");
          ws.setName("workflow");
          ws.setOptional(false);
          WorkflowWorkspaceSpec workflowWorkspaceSpec = new WorkflowWorkspaceSpec();
          if (ws.getSpec() != null) {
            workflowWorkspaceSpec = (WorkflowWorkspaceSpec) ws.getSpec();
          }
          if (workflowWorkspaceSpec.getSize() == null) {
            workflowWorkspaceSpec.setSize(maxStorageSizeQuota);
          } else if (quotasEnabled && (Integer.valueOf(workflowWorkspaceSpec.getSize()) > Integer.valueOf(maxStorageSizeQuota))) {
            throw new BoomerangException(BoomerangError.QUOTA_EXCEEDED, "Workspace Size Limit", workflowWorkspaceSpec.getSize(), maxStorageSizeQuota);
          }
          ws.setSpec(workflowWorkspaceSpec);
        } else if (ws.getType().equals("workflowrun")) {
          String maxStorageSizeQuota = this.settingsService
              .getSettingConfig(TEAMS_SETTINGS_KEY, QUOTA_MAX_WORKFLOWRUN_STORAGE).getValue().replace("Gi", "");
          ws.setName("workflowrun");
          ws.setOptional(false);
          WorkflowWorkspaceSpec workflowWorkspaceSpec = new WorkflowWorkspaceSpec();
          if (ws.getSpec() != null) {
            workflowWorkspaceSpec = (WorkflowWorkspaceSpec) ws.getSpec();
          }
          if (workflowWorkspaceSpec.getSize() == null) {
            workflowWorkspaceSpec.setSize(maxStorageSizeQuota);
          } else if (quotasEnabled && (Integer.valueOf(workflowWorkspaceSpec.getSize()) > Integer.valueOf(maxStorageSizeQuota))) {
            throw new BoomerangException(BoomerangError.QUOTA_EXCEEDED, "Workspace Size Limit", workflowWorkspaceSpec.getSize(), maxStorageSizeQuota);
          }
          ws.setSpec(workflowWorkspaceSpec);
        }
      }
    }
  }

  /*
   * Apply allows you to create a new version or override an existing Workflow as well as create new
   * Workflow with supplied ID
   */
  @Override
  public Workflow apply(String team, Workflow workflow, boolean replace) {
    if (workflow != null && workflow.getId() != null && !workflow.getId().isBlank()) {
      if (relationshipServiceImpl.doesSlugOrRefExistForTypeInTeam(RelationshipType.WORKFLOW, workflow.getId(), team)) {
        updateScheduleTriggers(workflow,
            this.get(team, workflow.getId(), Optional.empty(), false).getTriggers());

        // Default Triggers
        validateTriggerDefaults(workflow);

        // Convert TaskRefs to IDs
        convertTaskRefsToIds(team, workflow);
        
        Workflow appliedWorkflow = engineClient.applyWorkflow(workflow, replace);
        
        // Filter out sensitive values
        DataAdapterUtil.filterParamSpecValueByFieldType(appliedWorkflow.getConfig(),
            appliedWorkflow.getParams(), FieldType.PASSWORD.value());

        //Convert Workflow TaskRefs to Slugs
        convertTaskRefsToSlugs(team, appliedWorkflow);
        
        return appliedWorkflow;
      }
    } 
    if (workflow != null) {
      workflow.setId(null);
      return this.create(team, workflow);
    }
    throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
  }

  /*
   * Submit Workflow to Run
   */
  @Override
  public WorkflowRun submit(String team, String workflowId, WorkflowSubmitRequest request, boolean start) {
    if (workflowId == null || workflowId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
    if (relationshipServiceImpl.hasTeamRelationship(Optional.of(RelationshipType.WORKFLOW),
        Optional.of(workflowId), RelationshipLabel.BELONGSTO, team, false)) {
      return this.internalSubmit(team, workflowId, request, start);
    } else {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
  }
  
  /*
   * Submit WorkflowRun Internally by Team
   * 
   * Used by TriggerService
   */
  protected void internalSubmitForTeam(String team, WorkflowSubmitRequest request,
      boolean start) {
    List<String> wfRefs =
        relationshipServiceImpl.getFilteredRefs(Optional.of(RelationshipType.WORKFLOW),
            Optional.empty(), RelationshipLabel.BELONGSTO,
            RelationshipType.TEAM, team, true);
    
    wfRefs.forEach(r -> {
      this.internalSubmit(team, r, request, start);}
    );
  }
  
  /*
   * Submit WorkflowRun Internally
   * 
   * Caution: bypasses the authN and authZ and Relationship checks
   */
  public WorkflowRun internalSubmit(String team, String workflowId, WorkflowSubmitRequest request,
      boolean start) {
      // Check if Workflow exists and is active. Then check triggers are enabled.
      // Presumed workflow exists as relationship was valid to get to this point.
      Workflow workflow = engineClient.getWorkflow(workflowId, Optional.empty(), false);
      //Check Triggers - Throws Exception - Check first, as if trigger not enabled, no point in checking quotas
      canRunWithTrigger(workflow.getTriggers(), request.getTrigger(), request.getParams());
      //Check Quotas - Throws Exception
      canRunWithQuotas(team, workflowId, Optional.of(request.getWorkspaces()));
      // Set Workflow & Task Debug
      if (Objects.isNull(request.getDebug())) {
        boolean enableDebug = false;
        String setting =
            this.settingsService.getSettingConfig("task", "debug").getValue();
        if (setting != null) {
          enableDebug = Boolean.parseBoolean(setting);
        }
        request.setDebug(Boolean.valueOf(enableDebug));
        LOGGER.info("Setting debug = " + enableDebug);
      }
      // Set Workflow Timeout
      Long timeout = teamService.getWorkflowMaxDurationForTeam(team).longValue();
      if (!Objects.isNull(request.getTimeout()) && request.getTimeout() < timeout) {
        timeout = request.getTimeout();
      }
      request.setTimeout(Long.valueOf(timeout));
      //These annotations are processed by the DAGUtility in the Engine
      Map<String, Object> executionAnnotations = new HashMap<>();
      executionAnnotations.put("boomerang.io/task-deletion", this.settingsService.getSettingConfig(TASK_SETTINGS_KEY, "deletion.policy").getValue());
      executionAnnotations.put("boomerang.io/task-default-image", this.settingsService.getSettingConfig(TASK_SETTINGS_KEY, "default.image").getValue());
      executionAnnotations.put("boomerang.io/task-timeout", this.settingsService.getSettingConfig(TASK_SETTINGS_KEY, "default.timeout").getValue());
      
      //Add Context, Global, and Team parameters to the WorkflowRun request
      ParamLayers paramLayers = parameterManager.buildParamLayers(team, workflow);
      executionAnnotations.put("boomerang.io/global-params", paramLayers.getGlobalParams());
      executionAnnotations.put("boomerang.io/context-params", paramLayers.getContextParams());
      executionAnnotations.put("boomerang.io/team-params", paramLayers.getTeamParams());
      
      //Add Contextual Information such as team-name. Used by Engine and the AcquireTaskLock and other tasks to add a hidden prefix.
      executionAnnotations.put("boomerang.io/team-name", team);
      request.getAnnotations().putAll(executionAnnotations);
      
      // TODO: figure out the storing of initiated by. Is that just a relationship?
      WorkflowRun wfRun = engineClient.submitWorkflow(workflowId, request, start);

      // Creates relationship with owning team
      relationshipServiceImpl.createNodeWithTeamConnection(RelationshipType.WORKFLOWRUN, wfRun.getId(), "", team, Optional.empty());
      return wfRun;
  }

  /*
   * Retrieve a workflows changelog from all versions
   */

  @Override
  public ResponseEntity<List<ChangeLogVersion>> changelog(String team, String workflowId) {
    if (workflowId == null || workflowId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }

    if (relationshipServiceImpl.hasTeamRelationship(Optional.of(RelationshipType.WORKFLOW),
        Optional.of(workflowId), RelationshipLabel.BELONGSTO, team, false)) {
      return ResponseEntity.ok(engineClient.getWorkflowChangeLog(workflowId));
    } else {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
  }

  /*
   * Delete the Workflows, WorkflowRuns, and TaskRuns by calling Engine.
   * 
   * Engine takes care of deleting Triggers & Workspaces
   * 
   * We have to delete the Actions, Schedules, Tokens, and Relationships
   */
  @Override
  public void delete(String team, String workflowId) {
    if (workflowId == null || workflowId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }

    if (relationshipServiceImpl.hasTeamRelationship(Optional.of(RelationshipType.WORKFLOW),
        Optional.of(workflowId), RelationshipLabel.BELONGSTO, team, false)) {
      // Deletes the Workflow and associated WorkflowRuns, and TaskRuns
      engineClient.deleteWorkflow(workflowId);
      scheduleService.deleteAllForWorkflow(workflowId);
      tokenService.deleteAllForPrincipal(workflowId);
      actionService.deleteAllByWorkflow(workflowId);
      relationshipServiceImpl.removeNodeByRefOrSlug(RelationshipType.WORKFLOW, workflowId);
    } else {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
  }

  /*
   * Export the Workflow as JSON
   */
  @Override
  public ResponseEntity<InputStreamResource> export(String team, String workflowId) {
    final Workflow workflow = this.get(team, workflowId, Optional.empty(), true);

    HttpHeaders headers = new HttpHeaders();
    headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
    headers.add("Pragma", "no-cache");
    headers.add("Expires", "0");
    headers.add("Content-Disposition", "attachment; filename=\"any_name.json\"");

    try {

      ObjectMapper mapper = new ObjectMapper();

      byte[] buf = mapper.writeValueAsBytes(workflow);

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
   * 
   * Relationship checks are handled in the Get and Create methods
   */
  @Override
  public Workflow duplicate(String team, String workflowId) {
    final Workflow response = this.get(team, workflowId, Optional.empty(), true);
    Workflow workflow = response;
    workflow.setId(null);
    workflow.setName(workflow.getName() + " (duplicate)");
    return this.create(team, workflow);
  }

  /*
   * Retrieves Workflow with Tasks and converts / composes it to the appropriate model.
   * 
   * Relationship check handled in Get
   * 
   * TODO: add a type to handle canvas or Tekton YAML etc etc
   */
  @Override
  public WorkflowCanvas composeGet(String team, String workflowId, Optional<Integer> version) {
    if (workflowId == null || workflowId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }

    final Workflow response = this.internalGet(team, workflowId, Optional.empty(), true);
    return convertWorkflowToCanvas(response);
  }

  /*
   * Retrieves Workflow with Tasks and converts / composes it to the appropriate model.
   * 
   * Relationship check handled in Apply
   * 
   * TODO: add a type to handle canvas or Tekton YAML etc etc
   */
  @Override
  public WorkflowCanvas composeApply(String team, WorkflowCanvas canvas, boolean replace) {
    if (canvas == null) {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }

    Workflow workflow = convertCanvasToWorkflow(canvas);
    Workflow response = this.apply(team, workflow, replace);
    return convertWorkflowToCanvas(response);
  }

  /*
   * Forms the param layers (keys only)
   * 
   * Used by the UI to provide helpful prompts on available params
   * 
   * Relationship check handled in Get
   */
  @Override
  public List<String> getAvailableParameters(String team, String workflowId) {
    if (workflowId == null || workflowId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
    
    final Workflow workflow = this.get(team, workflowId, Optional.empty(), true);
    List<String> paramKeys =
        parameterManager.buildParamKeys(team, workflow);
    workflow.getTasks().forEach(t -> {
      if (t.getResults() != null && !t.getResults().isEmpty()) {
        t.getResults().forEach(r -> {
          String key = "tasks." + t.getName() + ".results." + r.getName();
          paramKeys.add(key);
        });
      }
    });
    return paramKeys;
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
   */
  private void canCreateWithQuotas(String team) {
    if (settingsService.getSettingConfig(FEATURES_SETTINGS_KEY, FEATURES_TEAM_QUOTA).getBooleanValue()) {
      CurrentQuotas quotas = teamService.getCurrentQuotas(team);
      LOGGER.debug("Quotas: {}", quotas.toString());
      if (quotas.getCurrentWorkflowCount() > quotas.getMaxWorkflowCount()) {
        throw new BoomerangException(BoomerangError.QUOTA_EXCEEDED, "Number of Workflows", quotas.getCurrentWorkflowCount(), quotas.getMaxWorkflowCount());
      }
    }
  }

  /*
   * Check if the Team Quotas allow a Workflow to run
   */
  private void canRunWithQuotas(String team, String workflowId, Optional<List<WorkflowWorkspace>> workspaces) {
    if (settingsService.getSettingConfig(FEATURES_SETTINGS_KEY, FEATURES_TEAM_QUOTA).getBooleanValue()) {
      CurrentQuotas quotas = teamService.getCurrentQuotas(team);
      LOGGER.debug("Quotas: {}", quotas.toString());
      if (quotas.getCurrentConcurrentRuns() > quotas.getMaxConcurrentRuns()) {
        throw new BoomerangException(BoomerangError.QUOTA_EXCEEDED, "Concurrent runs (executions)", quotas.getCurrentConcurrentRuns(), quotas.getMaxConcurrentRuns());
      } else if (quotas.getCurrentRuns() > quotas.getMaxWorkflowRunMonthly()) {
        throw new BoomerangException(BoomerangError.QUOTA_EXCEEDED, "Number of runs (executions)", quotas.getCurrentRuns(), quotas.getMaxWorkflowRunMonthly());
      } else if (workspaces.isPresent() && !workspaces.get().isEmpty() && workspaces.get().size() > 0) {
        workspaces.get().forEach(ws -> {
          if (ws.getType().equals("workflow") && ws.getSpec()!= null) {
            try {
              Field sizeField = ws.getSpec().getClass().getDeclaredField("size");
              String size = (String) sizeField.get(ws.getSpec());
              if (Integer.valueOf(size) > quotas.getMaxWorkflowStorage()) {
                throw new BoomerangException(BoomerangError.QUOTA_EXCEEDED, "Requested Workspace size", size, quotas.getMaxWorkflowStorage());
              }
            } catch (NoSuchFieldException | IllegalAccessException ex) {
              //Do nothing
            }
          } else if (ws.getType().equals("workflowrun") && ws.getSpec()!= null) {
            try {
              Field sizeField = ws.getSpec().getClass().getDeclaredField("size");
              String size = (String) sizeField.get(ws.getSpec());
              if (Integer.valueOf(size) > quotas.getMaxWorkflowRunStorage()) {
                throw new BoomerangException(BoomerangError.QUOTA_EXCEEDED, "Requested Workspace size", size, quotas.getMaxWorkflowRunStorage());
              }
            } catch (NoSuchFieldException | IllegalAccessException ex) {
              //Do nothing
            }
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
  protected void canRunWithTrigger(WorkflowTrigger triggers, TriggerEnum runTrigger, List<RunParam> params) {
    // Check no further if trigger not provided
    if (!Objects.isNull(runTrigger)) {
      if (!Objects.isNull(triggers)) {
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
    List<WorkflowTask> wfTasks = workflow.getTasks();
    WorkflowCanvas wfCanvas = new WorkflowCanvas(workflow);
    List<CanvasNode> nodes = new ArrayList<>();
    List<CanvasEdge> edges = new ArrayList<>();

    Map<String, TaskType> taskNamesToType =
        wfTasks.stream().collect(Collectors.toMap(WorkflowTask::getName, WorkflowTask::getType));
    Map<String, String> taskNameToNodeId = new HashMap<>();
   
    //Make config the source of truth on the canvas
    wfCanvas.setConfig(ParameterUtil.paramSpecToAbstractParamV2(workflow.getParams(), workflow.getConfig()));

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
      nodeData.setTaskRef(task.getTaskRef());
      nodeData.setTaskVersion(task.getTaskVersion());
      nodeData.setUpgradesAvailable(task.getUpgradesAvailable());
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
    
    //Make params the source of truth on the Workflow
    //First merge the config so that password defaultValues are preserved
    workflow.setConfig(ParameterUtil.mergeAbstractParms(workflow.getConfig(), canvas.getConfig()));
    //Next merge params using the merged config
    workflow.setParams(ParameterUtil.abstractParamsToParamSpecsV2(workflow.getConfig(), workflow.getParams()));
    
    List<CanvasNode> nodes = canvas.getNodes();
    List<CanvasEdge> edges = canvas.getEdges();

    Map<String, String> nodeIdToTaskName =
        nodes.stream().collect(Collectors.toMap(n -> n.getId(), n -> n.getData().getName()));

    nodes.forEach(node -> {
      WorkflowTask task = new WorkflowTask();
      task.setName(node.getData().getName());
      task.setType(node.getType());
      Map<String, Number> position = new HashMap<>();
      position.put("x", node.getPosition().getX());
      position.put("y", node.getPosition().getY());
      task.getAnnotations().put("boomerang.io/position", position);
      task.setParams(node.getData().getParams());
      task.setResults(node.getData().getResults());
      task.setTaskRef(node.getData().getTaskRef());
      task.setTaskVersion(node.getData().getTaskVersion());

      List<WorkflowTaskDependency> dependencies = new LinkedList<>();
      edges.stream().filter(e -> e.getTarget().equals(node.getId())).forEach(e -> {
        WorkflowTaskDependency dep = new WorkflowTaskDependency();
        dep.setTaskRef(nodeIdToTaskName.get(e.getSource()));
        dep.setDecisionCondition(e.getData().getDecisionCondition());
        dep.setExecutionCondition(e.getData().getExecutionCondition());
        dependencies.add(dep);
      });
      task.setDependencies(dependencies);
      workflow.getTasks().add(task);
    });
    LOGGER.debug("Converted Workflow: " + workflow.toString());
    return workflow;
  }

  /*
   * Helper methods to from TaskRef to TaskSlug and vice versa
   * 
   * Duplicated in WorkflowRunService.impl
   */
  private void convertTaskRefsToSlugs(String team, Workflow workflow) {
    workflow.getTasks().forEach(t -> {
      if (!t.getName().equals("start") && !t.getName().equals("end")) {
        String ref = "";
        if (relationshipServiceImpl.doesSlugOrRefExistForType(RelationshipType.GLOBALTASK, t.getTaskRef())) {
          ref = relationshipServiceImpl.getSlugFromRef(RelationshipType.GLOBALTASK, t.getTaskRef());
        } else {
          ref = team + TASK_REF_SEPERATOR + relationshipServiceImpl.getSlugFromRef(RelationshipType.TASK, t.getTaskRef());
        }
        if (ref.isBlank()) {
          throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_TASK_REF, t.getName(), t.getTaskRef());
        }
        t.setTaskRef(ref);
      }
    });
  }

  private void convertTaskRefsToIds(String team, Workflow workflow) {
    workflow.getTasks().forEach(t -> {
      if (!t.getName().equals("start") && !t.getName().equals("end")) {
        String ref = "";
        if (!t.getTaskRef().contains(TASK_REF_SEPERATOR)) {
          ref = relationshipServiceImpl.getRefFromSlug(RelationshipType.GLOBALTASK, t.getTaskRef());
        } else {
          ref = relationshipServiceImpl.getRefFromSlug(RelationshipType.TASK, t.getTaskRef().split(TASK_REF_SEPERATOR)[1]);
        }
        if (ref.isBlank()) {
          throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_TASK_REF, t.getName(), t.getTaskRef());
        }
        t.setTaskRef(ref);
      }
    });
  }
}
