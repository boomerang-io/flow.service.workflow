package io.boomerang.v4.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
import io.boomerang.model.controller.TaskResult;
import io.boomerang.mongo.entity.FlowTaskTemplateEntity;
import io.boomerang.mongo.entity.WorkflowEntity;
import io.boomerang.mongo.model.Dag;
import io.boomerang.mongo.model.Revision;
import io.boomerang.mongo.model.WorkflowScope;
import io.boomerang.mongo.model.next.DAGTask;
import io.boomerang.util.DataAdapterUtil;
import io.boomerang.util.DataAdapterUtil.FieldType;
import io.boomerang.v4.client.EngineClient;
import io.boomerang.v4.client.WorkflowResponsePage;
import io.boomerang.v4.data.entity.RelationshipEntity;
import io.boomerang.v4.model.CanvasEdge;
import io.boomerang.v4.model.CanvasEdgeData;
import io.boomerang.v4.model.CanvasNode;
import io.boomerang.v4.model.CanvasNodeData;
import io.boomerang.v4.model.CanvasNodePosition;
import io.boomerang.v4.model.WorkflowCanvas;
import io.boomerang.v4.model.enums.RelationshipRef;
import io.boomerang.v4.model.enums.RelationshipType;
import io.boomerang.v4.model.enums.TriggerEnum;
import io.boomerang.v4.model.enums.ref.TaskType;
import io.boomerang.v4.model.ref.ParamSpec;
import io.boomerang.v4.model.ref.Task;
import io.boomerang.v4.model.ref.Trigger;
import io.boomerang.v4.model.ref.TriggerEvent;
import io.boomerang.v4.model.ref.TriggerScheduler;
import io.boomerang.v4.model.ref.Workflow;
import io.boomerang.v4.model.ref.WorkflowStatus;
import io.boomerang.v4.model.ref.WorkflowTrigger;

/*
 * This service replicates the required calls for Engine WorkflowV1 APIs
 * 
 * It will - Check authorization using Relationships - Determines if to add or remove elements -
 * Forward call onto Engine (if applicable) - Converts response as needed for UI
 * 
 * TODO: migrate Triggers to an alternative workflow_triggers collection and use Relationships to adjust
 */
@Service
public class WorkflowServiceImpl implements WorkflowService {

  private static final Logger LOGGER = LogManager.getLogger();

  @Autowired
  private EngineClient engineClient;

  @Autowired
  private RelationshipService relationshipService;
  
  @Autowired
  private WorkflowScheduleService workflowScheduleService;

  @Autowired
  private SettingsService settingsService;

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

    List<String> workflowRefs = relationshipService.getFilteredRefs(Optional.of(RelationshipRef.WORKFLOW),
        Optional.of(List.of(workflowId)), Optional.of(RelationshipType.BELONGSTO), Optional.empty(), Optional.empty());
    if (!workflowRefs.isEmpty()) {
      Workflow workflow = engineClient.getWorkflow(workflowId, version, withTasks);
      
      // Filter out sensitive values
      DataAdapterUtil.filterParamSpecValueByFieldType(workflow.getConfig(), workflow.getParams(), FieldType.PASSWORD.value());
      return ResponseEntity.ok(workflow);
    } else {
      // TODO: do we want to return invalid ref or unauthorized
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
  }

  /*
   * Query for Workflows.
   */
  @Override
  public WorkflowResponsePage query(int page, int limit, Sort sort,
      Optional<List<String>> queryLabels, Optional<List<String>> queryStatus,
      Optional<List<String>> queryTeams,
      Optional<List<String>> queryWorkflows) {
    
    // Get Refs that request has access to
    List<String> workflowRefs = relationshipService.getFilteredRefs(Optional.of(RelationshipRef.WORKFLOW),
        queryWorkflows, Optional.of(RelationshipType.BELONGSTO), Optional.ofNullable(RelationshipRef.TEAM),
        queryTeams);
    LOGGER.debug("Query Ids: ", workflowRefs);

    WorkflowResponsePage response = engineClient.queryWorkflows(page, limit, sort, queryLabels, queryStatus,
        Optional.of(workflowRefs));

    // Filter out sensitive values
    if (!response.getContent().isEmpty()) {
      response.getContent().forEach(w -> 
      DataAdapterUtil.filterParamSpecValueByFieldType(w.getConfig(), w.getParams(), FieldType.PASSWORD.value()));
    }
    
    return response;
  }

  /*
   * Create Workflow. Pass query onto EngineClient
   * 
   * No need to validate params as they are either defaulted or optional
   */
  @Override
  public ResponseEntity<Workflow> create(Workflow request,
      Optional<String> team) {
    //Default triggers
    setupTriggerDefaults(request);

    // TODO: Add in the verification / handling of Workspaces for the UI
    // CHeck the loader for v4 migration of Workspaces to know the object needed
    // if (workflow.getStorage() == null) {
    // workflow.setStorage(new Storage());
    // }
    // if (workflow.getStorage().getActivity() == null) {
    // workflow.getStorage().setActivity(new ActivityStorage());
    //
    //    setupWorkspaceDefaults();
    Workflow workflow = engineClient.createWorkflow(request);
    if (team.isPresent()) {
    // Create BELONGSTO relationship for mapping Workflow to Owner
      relationshipService.addRelationshipRef(RelationshipRef.WORKFLOW, workflow.getId(), RelationshipRef.TEAM,
          team);
    } else {
      // Creates a relationship based on current used Security Scope
      relationshipService.addRelationshipRefForCurrentScope(RelationshipRef.WORKFLOW,
          workflow.getId());
    }

    // Filter out sensitive values
    DataAdapterUtil.filterParamSpecValueByFieldType(workflow.getConfig(), workflow.getParams(), FieldType.PASSWORD.value());
    return ResponseEntity.ok(workflow);
  }

  /*
   * Apply allows you to create a new version or override an existing Workflow as well as create new
   * Workflow with supplied ID
   */
  @Override
  public ResponseEntity<Workflow> apply(Workflow workflow, boolean replace) {
    String workflowId = workflow.getId();
    List<String> workflowRefs = relationshipService.getFilteredRefs(Optional.of(RelationshipRef.WORKFLOW),
        Optional.of(List.of(workflowId)), Optional.of(RelationshipType.BELONGSTO), Optional.of(RelationshipRef.TEAM), Optional.empty());

    if (workflowId != null && !workflowId.isBlank() && !workflowRefs.isEmpty()) {
      updateScheduleTriggers(workflow, this.get(workflowId, Optional.empty(), false).getBody().getTriggers());
      setupTriggerDefaults(workflow);
      Workflow appliedWorkflow = engineClient.applyWorkflow(workflow, replace);
      // Filter out sensitive values
      DataAdapterUtil.filterParamSpecValueByFieldType(appliedWorkflow.getConfig(), appliedWorkflow.getParams(), FieldType.PASSWORD.value());
      return ResponseEntity.ok(appliedWorkflow);
    } else {
      workflow.setId(null);
      return this.create(workflow, Optional.empty());
    }
  }

  /*
   * Set Workflow to Active Status
   */
  @Override
  public ResponseEntity<Void> enable(String workflowId) {
    if (workflowId == null || workflowId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }

    List<String> workflowRefs = relationshipService.getFilteredRefs(Optional.of(RelationshipRef.WORKFLOW),
        Optional.of(List.of(workflowId)), Optional.of(RelationshipType.BELONGSTO),  Optional.of(RelationshipRef.TEAM), Optional.empty());
    if (!workflowRefs.isEmpty()) {
      engineClient.enableWorkflow(workflowId);
      // Enable schedules
      return ResponseEntity.noContent().build();
    } else {
      // TODO: do we want to return invalid ref or unauthorized
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
  }

  /*
   * Set Workflow to Inactive Status
   */
  @Override
  public ResponseEntity<Void> disable(String workflowId) {
    if (workflowId == null || workflowId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }

    List<String> workflowRefs = relationshipService.getFilteredRefs(Optional.of(RelationshipRef.WORKFLOW),
        Optional.of(List.of(workflowId)), Optional.of(RelationshipType.BELONGSTO), Optional.of(RelationshipRef.TEAM), Optional.empty());
    if (!workflowRefs.isEmpty()) {
      engineClient.disableWorkflow(workflowId);
      //TODO: disable all schedules
      return ResponseEntity.noContent().build();
    } else {
      // TODO: do we want to return invalid ref or unauthorized
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
  }

  /*
   * Set Workflow to Deleted Status
   * 
   * The Workflow is kept around so as to ensure that we can display the WorkflowRun in the Activity screen. 
   */
  @Override
  public ResponseEntity<Void> delete(String workflowId) {
    if (workflowId == null || workflowId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }

    List<String> workflowRefs = relationshipService.getFilteredRefs(Optional.of(RelationshipRef.WORKFLOW),
        Optional.of(List.of(workflowId)), Optional.of(RelationshipType.BELONGSTO), Optional.of(RelationshipRef.TEAM), Optional.empty());
    if (!workflowRefs.isEmpty()) {
      engineClient.deleteWorkflow(workflowId);

      // TODO: delete all triggers
      // TODO: delete all schedules
      // TODO: delete all tokens
//      if (entity.getTriggers() != null) {
//        Triggers trigger = entity.getTriggers();
//        if (trigger != null) {
//          TriggerScheduler scheduler = trigger.getScheduler();
//          if (scheduler != null && scheduler.getEnable()) {
//            try {
//              workflowScheduleService.deleteAllSchedules(workflowId);
//            } catch (SchedulerException e) {
//              logger.info("Unable to remove job. ");
//              logger.error(e);
//            }
//          }
//        }
//      }
      return ResponseEntity.noContent().build();
    } else {
      // TODO: do we want to return invalid ref or unauthorized
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
   * Export the Workflow as JSON
   */
  @Override
  public ResponseEntity<Workflow> duplicate(String workflowId) {
    final ResponseEntity<Workflow> response = this.get(workflowId, Optional.empty(), true);
    Workflow workflow = response.getBody();
    workflow.setId(null);
    workflow.setName(workflow.getName() + " (duplicate)");
    
    Optional<RelationshipEntity> relEntity = relationshipService.getRelationship(RelationshipRef.WORKFLOW, workflowId, RelationshipType.BELONGSTO);
    return this.create(workflow, Optional.of(relEntity.get().getToRef()));
  }

  /*
   * Retrieves Workflow with Tasks and converts / composes it to the appropriate model.
   * 
   * TODO: add a type to handle canvas or Tekton YAML etc etc
   */
  @Override
  public ResponseEntity<WorkflowCanvas> compose(String workflowId, Optional<Integer> version) {
    if (workflowId == null || workflowId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }

    List<String> workflowRefs = relationshipService.getFilteredRefs(Optional.of(RelationshipRef.WORKFLOW),
        Optional.of(List.of(workflowId)), Optional.of(RelationshipType.BELONGSTO),  Optional.of(RelationshipRef.TEAM), Optional.empty());
    if (!workflowRefs.isEmpty()) {
      Workflow workflow = engineClient.getWorkflow(workflowId, version, true);
      return ResponseEntity.ok(convertToCanvasModel(workflow));
    } else {
      // TODO: do we want to return invalid ref or unauthorized
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
  }  
  @Override
  public List<String> getAvailableParameters(String workflowId) {
    final ResponseEntity<Workflow> response = this.get(workflowId, Optional.empty(), true);
    Workflow workflow = response.getBody();
    List<ParamSpec> parameters = workflow.getParams() != null ? workflow.getParams() : new ArrayList<>();

    if (settingsService.getConfiguration("features", "globalParameters").getBooleanValue()) {
      Map<String, String> globalProperties = new HashMap<>();
      propertyManager.buildGlobalProperties(globalProperties);

      for (Map.Entry<String, String> globalProperty : globalProperties.entrySet()) {
        parameters.add("global.params." + globalProperty.getKey());
        parameters.add("params." + globalProperty.getKey());
      }
    }

    if (flowSettingsService.getConfiguration("features", "teamParameters").getBooleanValue()
        && workflow.getScope() != null && WorkflowScope.team.equals(workflow.getScope())) {
      Map<String, String> teamProperties = new HashMap<>();
      propertyManager.buildTeamProperties(teamProperties, workflow.getId());

      for (Map.Entry<String, String> teamProperty : teamProperties.entrySet()) {
        parameters.add("team.params." + teamProperty.getKey());
        parameters.add("params." + teamProperty.getKey());
      }
    }

    Map<String, String> workflowProperties = new HashMap<>();
    propertyManager.buildWorkflowProperties(workflowProperties, null, workflow.getId());
    for (Map.Entry<String, String> workflowProperty : workflowProperties.entrySet()) {
      parameters.add("workflow.params." + workflowProperty.getKey());
      parameters.add("params." + workflowProperty.getKey());
    }

    Map<String, String> systemProperties = new HashMap<>();
    propertyManager.buildSystemProperties(null, null, workflow.getId(), systemProperties);
    for (Map.Entry<String, String> systemProperty : systemProperties.entrySet()) {
      parameters.add("system.params." + systemProperty.getKey());
      parameters.add("params." + systemProperty.getKey());
    }


    if (revision != null) {
      Dag dag = revision.getDag();
      List<DAGTask> dagkTasks = dag.getTasks();
      if (dagkTasks != null) {
        for (DAGTask task : dagkTasks) {
          String taskName = task.getLabel();
          if (task.getTemplateId() != null) {
            String templateId = task.getTemplateId();
            FlowTaskTemplateEntity taskTemplate = templateService.getTaskTemplateWithId(templateId);

            if (taskTemplate != null) {
              if ("templateTask".equals(taskTemplate.getNodetype())) {
                int revisionCount = taskTemplate.getRevisions().size();
                Revision latestRevision = taskTemplate.getRevisions().stream()
                    .filter(x -> x.getVersion() == revisionCount).findFirst().orElse(null);
                if (latestRevision != null) {
                  List<TaskResult> results = latestRevision.getResults();
                  if (results != null) {
                    for (TaskResult result : results) {
                      String key = "tasks." + taskName + ".results." + result.getName();
                      parameters.add(key);
                    }
                  }
                }
              } else {
                List<TaskResult> results = task.getResults();
                if (results != null) {
                  for (TaskResult result : results) {
                    String key = "tasks." + taskName + ".results." + result.getName();
                    parameters.add(key);
                  }
                }
              }
            }
          }
        }
      }

    }

    return parameters;
  }
  
  /*
   * Checks if the Workflow can be executed based on an active workflow and enabled triggers.
   * 
   * If trigger is Manual or Schedule then a deeper check is used to check if those triggers are
   * enabled.
   * 
   * @param workflowId the Workflows unique ID
   * @param Trigger an optional Trigger object
   * @return Boolean whether the workflow can execute or not
   * 
   * TODO: move to WorkflowRun service
   */
  protected boolean canRunWithTrigger(String workflowId, Optional<String> trigger) {
    // Check no further if trigger not provided
    if (!trigger.isPresent()) {
      return true;
    }

    // Check if Workflow exists and is active. Then check triggers are enabled.
    ResponseEntity<Workflow> workflowResponse = this.get(workflowId, Optional.empty(), false);
    if (workflowResponse.getBody() != null) {
      Workflow workflow = workflowResponse.getBody();
      if (workflow.getStatus().equals(WorkflowStatus.active)) {
        WorkflowTrigger triggers = workflow.getTriggers();
        if (TriggerEnum.manual.toString().equals(trigger.get()) && triggers.getManual() != null) {
          return triggers.getManual().getEnable();
        } else if (TriggerEnum.scheduler.toString().equals(trigger.get())
            && triggers.getScheduler() != null) {
          return triggers.getScheduler().getEnable();
        } else if (TriggerEnum.webhook.toString().equals(trigger.get())
            && triggers.getWebhook() != null) {
          return triggers.getWebhook().getEnable();
        } else if (TriggerEnum.custom.toString().equals(trigger.get())
            && triggers.getCustom() != null) {
          return triggers.getCustom().getEnable();
        } else {
          return false;
        }
      }
    }
    return false;
  }

  /*
   * Sets up the Triggers as per v3 design
   */
  private void setupTriggerDefaults(final Workflow workflow) {

    if (workflow.getTokens() == null) {
      workflow.setTokens(new LinkedList<>());
    }

    if (workflow.getTriggers() == null) {
      workflow.setTriggers(new WorkflowTrigger());
      workflow.getTriggers().getManual().setEnable(true);
    }
    
    // Default to enabled for Workflows
    if (workflow.getTriggers().getManual() == null) {
      Trigger manual = new Trigger();
      manual.setEnable(Boolean.TRUE);
      workflow.getTriggers().setManual(manual);
    }

    if (workflow.getTriggers().getScheduler() == null) {
      TriggerScheduler schedule = new TriggerScheduler();
      workflow.getTriggers().setScheduler(schedule);
    }

    if (workflow.getTriggers().getCustom() == null) {
      TriggerEvent custom = new TriggerEvent();
      workflow.getTriggers().setCustom(custom);
    }

    if (workflow.getTriggers().getWebhook() == null) {
      TriggerEvent webhook = new TriggerEvent();
      workflow.getTriggers().setWebhook(webhook);
    }
  }

  /*
   * Update Triggers
   */
  private void updateScheduleTriggers(final Workflow updatedWorkflow, WorkflowTrigger currentTriggers) {
    if (currentTriggers == null) {
      currentTriggers = new WorkflowTrigger();
    }
    if (updatedWorkflow.getTriggers() != null) {
      boolean currentSchedulerEnabled = false;
      if (currentTriggers.getScheduler() != null) {
        currentSchedulerEnabled = currentTriggers.getScheduler().getEnable();
      }
      boolean updatedSchedulerEnabled = updatedWorkflow.getTriggers() != null ? updatedWorkflow.getTriggers().getScheduler().getEnable() : false;
      if (updatedSchedulerEnabled == false) {
        workflowScheduleService.disableAllTriggerSchedules(updatedWorkflow.getId());
      } else if (currentSchedulerEnabled == false && updatedSchedulerEnabled == true) {
        workflowScheduleService.enableAllTriggerSchedules(updatedWorkflow.getId());
      }
    }
  }

  /*
   * Converts from standard Workflow to Canvas Model
   * 
   * TODO: move this code to a private method or a Convertor class
   */
  private WorkflowCanvas convertToCanvasModel(Workflow workflow) {
    List<Task> wfTasks = workflow.getTasks();
    WorkflowCanvas wfCanvas = new WorkflowCanvas();
    List<CanvasNode> nodes = new ArrayList<>();
    List<CanvasEdge> edges = new ArrayList<>();

    wfTasks.forEach(task -> {
      CanvasNode node = new CanvasNode();
      node.setId(task.getName()); // TODO does the ID need to just be random
      node.setType(task.getType());
      if (task.getAnnotations().containsKey("io.boomerang/position")) {
        Map<String, Number> position =
            (Map<String, Number>) task.getAnnotations().get("io.boomerang/position");
        CanvasNodePosition nodePosition = new CanvasNodePosition();
        nodePosition.setX(position.get("x"));
        nodePosition.setY(position.get("y"));
        LOGGER.info("Node Position:" + nodePosition.toString());
        node.setPosition(nodePosition);
      }
      CanvasNodeData nodeData = new CanvasNodeData();
      nodeData.setLabel(task.getName());
      nodeData.setParams(task.getParams());
      node.setData(nodeData);
      nodes.add(node);

      task.getDependencies().forEach(dep -> {
        CanvasEdge edge = new CanvasEdge();
        edge.setTarget(task.getName());
        edge.setSource(dep.getTaskRef());
        edge.setType(TaskType.decision.equals(task.getType()) ? "decision" : "let me know");
        CanvasEdgeData edgeData = new CanvasEdgeData();
        edgeData.setExecutionCondition(dep.getExecutionCondition());
        edgeData.setDecisionCondition(dep.getDecisionCondition());
        edges.add(edge);
      });
    });

    wfCanvas.setNodes(nodes);
    wfCanvas.setEdges(edges);

    return wfCanvas;
  }
}
