package io.boomerang.v4.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
import io.boomerang.mongo.model.TaskType;
import io.boomerang.util.DataAdapterUtil;
import io.boomerang.util.DataAdapterUtil.FieldType;
import io.boomerang.v4.client.EngineClient;
import io.boomerang.v4.client.WorkflowResponsePage;
import io.boomerang.v4.model.CanvasEdge;
import io.boomerang.v4.model.CanvasEdgeData;
import io.boomerang.v4.model.CanvasNode;
import io.boomerang.v4.model.CanvasNodeData;
import io.boomerang.v4.model.CanvasNodePosition;
import io.boomerang.v4.model.WorkflowCanvas;
import io.boomerang.v4.model.enums.RelationshipRef;
import io.boomerang.v4.model.enums.RelationshipType;
import io.boomerang.v4.model.ref.Task;
import io.boomerang.v4.model.ref.Workflow;

/*
 * This service replicates the required calls for Engine WorkflowV1 APIs
 * 
 * It will - Check authorization using Relationships - Determines if to add or remove elements -
 * Forward call onto Engine (if applicable) - Converts response as needed for UI
 */
@Service
public class WorkflowServiceImpl implements WorkflowService {

  private static final Logger LOGGER = LogManager.getLogger();

  @Autowired
  private EngineClient engineClient;

  @Autowired
  private RelationshipService relationshipService;

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
      DataAdapterUtil.filterParamSpecValueByFieldType(workflow.getConfig(), workflow.getParams(), FieldType.PASSWORD.value());
      
      //TODO: add triggers
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
      Optional<String> owner) {
    // TODO: Add in the verification / handling of Workspaces for the UI
    // CHeck the loader for v4 migration of Workspaces to know the object needed
    // if (workflow.getStorage() == null) {
    // workflow.setStorage(new Storage());
    // }
    // if (workflow.getStorage().getActivity() == null) {
    // workflow.getStorage().setActivity(new ActivityStorage());
    // }
    
    //TODO default Triggers
    Workflow workflow = engineClient.createWorkflow(request);
    if (owner.isPresent()) {
    // Create BELONGSTO relationship for mapping Workflow to Owner
      relationshipService.addRelationshipRef(RelationshipRef.WORKFLOW, workflow.getId(), RelationshipRef.TEAM,
          owner);
    } else {
      // Creates a relationship based on current used Security Scope
      relationshipService.addRelationshipRefForCurrentScope(RelationshipRef.WORKFLOW,
          workflow.getId());
    }
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
      Workflow appliedWorkflow = engineClient.applyWorkflow(workflow, replace);
      return ResponseEntity.ok(appliedWorkflow);
    } else {
      workflow.setId(null);
      return this.create(workflow, Optional.empty());
    }
  }

  @Override
  public ResponseEntity<Void> enable(String workflowId) {
    if (workflowId == null || workflowId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }

    List<String> workflowRefs = relationshipService.getFilteredRefs(Optional.of(RelationshipRef.WORKFLOW),
        Optional.of(List.of(workflowId)), Optional.of(RelationshipType.BELONGSTO),  Optional.of(RelationshipRef.TEAM), Optional.empty());
    if (!workflowRefs.isEmpty()) {
      engineClient.enableWorkflow(workflowId);
      return ResponseEntity.noContent().build();
    } else {
      // TODO: do we want to return invalid ref or unauthorized
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
  }

  @Override
  public ResponseEntity<Void> disable(String workflowId) {
    if (workflowId == null || workflowId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }

    List<String> workflowRefs = relationshipService.getFilteredRefs(Optional.of(RelationshipRef.WORKFLOW),
        Optional.of(List.of(workflowId)), Optional.of(RelationshipType.BELONGSTO), Optional.of(RelationshipRef.TEAM), Optional.empty());
    if (!workflowRefs.isEmpty()) {
      engineClient.disableWorkflow(workflowId);
      return ResponseEntity.noContent().build();
    } else {
      // TODO: do we want to return invalid ref or unauthorized
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
  }

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
