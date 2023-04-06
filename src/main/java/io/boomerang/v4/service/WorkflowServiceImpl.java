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
import io.boomerang.v4.client.EngineClient;
import io.boomerang.v4.client.WorkflowResponsePage;
import io.boomerang.v4.model.CanvasEdge;
import io.boomerang.v4.model.CanvasEdgeData;
import io.boomerang.v4.model.CanvasNode;
import io.boomerang.v4.model.CanvasNodeData;
import io.boomerang.v4.model.CanvasNodePosition;
import io.boomerang.v4.model.WorkflowCanvas;
import io.boomerang.v4.model.enums.RelationshipRefType;
import io.boomerang.v4.model.ref.Task;
import io.boomerang.v4.model.ref.Workflow;

/*
 * This service replicates the required calls for Engine WorkflowV1 APIs
 * 
 * It will
 * - Check authorization using Relationships
 * - Determines if to add or remove elements
 * - Forward call onto Engine (if applicable)
 * - Converts response as needed for UI
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
  public ResponseEntity<Workflow> get(String workflowId, Optional<Integer> version, boolean withTasks) {
    if (workflowId == null || workflowId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }

    List<String> workflowRefs = relationshipService.getFilteredRefs(RelationshipRefType.WORKFLOW, Optional.empty(), Optional.empty(), Optional.empty());
    if (!workflowRefs.isEmpty() && workflowRefs.contains(workflowId)) {
      Workflow workflow = engineClient.getWorkflow(workflowId, version, withTasks);
      return ResponseEntity.ok(workflow);
    } else {
      //TODO: do we want to return invalid ref or unauthorized
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
  }

  /*
   * Query for Workflows. Pass query onto EngineClient
   * 
   * No need to validate params as they are either defaulted or optional
   */
  @Override
  public WorkflowResponsePage query(int page, int limit, Sort sort, Optional<List<String>> queryLabels,
      Optional<List<String>> queryStatus, Optional<List<String>> queryTeams) {
    List<String> workflowRefs = relationshipService.getFilteredRefs(RelationshipRefType.WORKFLOW, Optional.empty(), queryTeams, Optional.empty());
    LOGGER.debug("Query Ids: ", workflowRefs);
    
    return engineClient.queryWorkflows(page, limit, sort, queryLabels, queryStatus, Optional.of(workflowRefs));
  }
  
  /*
   * Create Workflow. Pass query onto EngineClient
   * 
   * No need to validate params as they are either defaulted or optional
   */
  @Override
  public ResponseEntity<Workflow> create(Workflow workflow) {
    // TODO: Add in the verification / handling of Workspaces for the UI
    // CHeck the loader for v4 migration of Workspaces to know the object needed
    // if (workflow.getStorage() == null) {
    // workflow.setStorage(new Storage());
    // }
    // if (workflow.getStorage().getActivity() == null) {
    // workflow.getStorage().setActivity(new ActivityStorage());
    // }
    Workflow createdWorkflow = engineClient.createWorkflow(workflow);
    // TODO: move this to an Async Aspect or CloudEvent handler so that it stores the relationship
    // regardless of REST thrown error.
    relationshipService.createRelationshipRef(RelationshipRefType.WORKFLOW,
        createdWorkflow.getId());
    return ResponseEntity.ok(createdWorkflow);
  }

  /*
   * Apply allows you to create a new version or override an existing Workflow as well as create new
   * Workflow with supplied ID
   */
  @Override
  public ResponseEntity<Workflow> apply(Workflow workflow, boolean replace) {   
    String workflowId = workflow.getId();
    List<String> workflowRefsList = relationshipService.getFilteredRefs(RelationshipRefType.WORKFLOW, Optional.empty(), Optional.empty(), Optional.empty());
    if (workflowId == null || workflowId.isBlank() || (!workflowRefsList.isEmpty() && workflowRefsList.contains(workflowId))) {
      Workflow appliedWorkflow = engineClient.applyWorkflow(workflow, replace);
    //TODO: move this to an Async Aspect or CloudEvent handler so that it stores the relationship regardless of REST thrown error.
      relationshipService.createRelationshipRef(RelationshipRefType.WORKFLOW, appliedWorkflow.getId());
      return ResponseEntity.ok(appliedWorkflow);
    } else if (workflowId != null && !workflowId.isBlank() && !relationshipService.doesRelationshipExist(RelationshipRefType.WORKFLOW, workflowId)) {
      return this.create(workflow);
    } else {
      //TODO: do we want to return invalid ref or unauthorized
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
  }

  @Override
  public ResponseEntity<Void> enable(String workflowId) {
    if (workflowId == null || workflowId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }

    List<String> workflowRefs = relationshipService.getFilteredRefs(RelationshipRefType.WORKFLOW, Optional.empty(), Optional.empty(), Optional.empty());
    if (!workflowRefs.isEmpty() && workflowRefs.contains(workflowId)) {
      engineClient.enableWorkflow(workflowId);
      return ResponseEntity.noContent().build();
    } else {
      //TODO: do we want to return invalid ref or unauthorized
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
  }

  @Override
  public ResponseEntity<Void> disable(String workflowId) {
    if (workflowId == null || workflowId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }

    List<String> workflowRefs = relationshipService.getFilteredRefs(RelationshipRefType.WORKFLOW, Optional.empty(), Optional.empty(), Optional.empty());
    if (!workflowRefs.isEmpty() && workflowRefs.contains(workflowId)) {
      engineClient.disableWorkflow(workflowId);
      return ResponseEntity.noContent().build();
    } else {
      //TODO: do we want to return invalid ref or unauthorized
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
  }

  @Override
  public ResponseEntity<Void> delete(String workflowId) {
    if (workflowId == null || workflowId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }

    List<String> workflowRefs = relationshipService.getFilteredRefs(RelationshipRefType.WORKFLOW, Optional.empty(), Optional.empty(), Optional.empty());
    if (!workflowRefs.isEmpty() && workflowRefs.contains(workflowId)) {
      engineClient.deleteWorkflow(workflowId);
      
      //TODO: delete all triggers
      //TODO: delete all tokens
      return ResponseEntity.noContent().build();
    } else {
      //TODO: do we want to return invalid ref or unauthorized
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

    List<String> workflowRefs = relationshipService.getFilteredRefs(RelationshipRefType.WORKFLOW, Optional.empty(), Optional.empty(), Optional.empty());
    if (!workflowRefs.isEmpty() && workflowRefs.contains(workflowId)) {
      Workflow workflow = engineClient.getWorkflow(workflowId, version, true);
      return ResponseEntity.ok(convertToCanvasModel(workflow));
    } else {
      //TODO: do we want to return invalid ref or unauthorized
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
      node.setId(task.getName()); //TODO does the ID need to just be random
      node.setType(task.getType());
      if (task.getAnnotations().containsKey("io.boomerang/position")) {
        Map<String, Number> position = (Map<String, Number>) task.getAnnotations().get("io.boomerang/position");
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
