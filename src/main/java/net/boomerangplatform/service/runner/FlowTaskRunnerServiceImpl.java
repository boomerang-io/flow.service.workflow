package net.boomerangplatform.service.runner;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgrapht.Graph;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm.SingleSourcePaths;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import net.boomerangplatform.exceptions.RunWorkflowException;
import net.boomerangplatform.iam.model.IAMStatus;
import net.boomerangplatform.iam.service.IAMClient;
import net.boomerangplatform.model.Task;
import net.boomerangplatform.model.TaskResult;
import net.boomerangplatform.mongo.entity.FlowTaskExecutionEntity;
import net.boomerangplatform.mongo.entity.FlowWorkflowActivityEntity;
import net.boomerangplatform.mongo.entity.FlowWorkflowEntity;
import net.boomerangplatform.mongo.model.CoreProperty;
import net.boomerangplatform.mongo.model.FlowProperty;
import net.boomerangplatform.mongo.model.FlowTaskStatus;
import net.boomerangplatform.mongo.model.TaskType;
import net.boomerangplatform.mongo.model.WorkflowExecutionCondition;
import net.boomerangplatform.mongo.model.next.Dependency;
import net.boomerangplatform.mongo.service.FlowWorkflowActivityService;
import net.boomerangplatform.mongo.service.FlowWorkflowActivityTaskService;
import net.boomerangplatform.mongo.service.FlowWorkflowService;
import net.boomerangplatform.service.crud.FlowActivityService;
import net.boomerangplatform.service.runner.misc.CreateTaskLifecycleService;
import net.boomerangplatform.service.runner.misc.CustomTaskLifecycleService;
import net.boomerangplatform.service.runner.misc.DecisionLifecycleService;
import net.boomerangplatform.service.runner.misc.WorkflowLifecycleService;

@Service
@SuppressWarnings("rawtypes")
public class FlowTaskRunnerServiceImpl implements FlowTaskRunnerService {

  private static final Logger LOGGER = LogManager.getLogger(FlowTaskRunnerServiceImpl.class);

  @Autowired
  private FlowWorkflowActivityService activityService;

  @Autowired
  private FlowActivityService flowActivityService;

  @Autowired
  private FlowWorkflowService flowWorkflowService;

  @Autowired
  private IAMClient iamClient;

  @Autowired
  private WorkflowLifecycleService workflowLifecycleService;

  @Autowired
  private CreateTaskLifecycleService taskLifecycleService;

  @Autowired
  private CustomTaskLifecycleService customTaskLifecycleService;

  @Autowired
  private DecisionLifecycleService decisionLifecycleService;

  @Autowired
  private FlowWorkflowActivityTaskService taskService;

  @Override
  public CompletableFuture<TaskResult> runTasks(Graph<String, DefaultEdge> graph,
      List<Task> tasksToRun, String activityId, String start, String end) {

    final FlowWorkflowActivityEntity activityEntity =
        this.flowActivityService.findWorkflowActivity(activityId);
    String workflowId = activityEntity.getWorkflowId();
    FlowWorkflowEntity workflowEntity = this.flowWorkflowService.getWorkflow(workflowId);
    boolean enableStorage = workflowEntity.isEnablePersistentStorage();

    final String workflowName = workflowEntity.getName();
    activityEntity.setStatus(FlowTaskStatus.inProgress);
    activityEntity.setCreationDate(new Date());

    activityService.saveWorkflowActivity(activityEntity);

    final Map<String, String> executionProperties =
        buildExecutionProperties(activityEntity, workflowEntity);
    workflowLifecycleService.createFlow(activityEntity.getWorkflowId(), workflowName, activityId,
        enableStorage, executionProperties);

    Map<String, CompletableFuture<TaskResult>> taskFutureMap = new HashMap<>();

    CompletableFuture<TaskResult> future = initializeWorkflow();
    final DijkstraShortestPath<String, DefaultEdge> dijkstraAlg = new DijkstraShortestPath<>(graph);
    TopologicalOrderIterator<String, DefaultEdge> orderIterator =
        new TopologicalOrderIterator<>(graph);

    while (orderIterator.hasNext()) {

      final String taskId = orderIterator.next();

      List<CompletableFuture> dependantTasksList =
          createDependancyList(graph, taskFutureMap, taskId);

      CompletableFuture[] dependantTasks = new CompletableFuture[dependantTasksList.size()];

      CompletableFuture<Void> workTask = createWorkTask(tasksToRun, activityId, taskId,
          dependantTasksList.toArray(dependantTasks));

      future = workTask
          .thenApplyAsync(x -> createDecisionTask(new WorkflowGraph(graph, start, end, dijkstraAlg),
              tasksToRun, activityId, workflowName, executionProperties, taskId));
      taskFutureMap.put(taskId, future);

    }

    return future.thenApplyAsync(
        x -> terminateWorkflow(activityId, activityEntity, workflowEntity, workflowName));
  }

  private TaskResult terminateWorkflow(String activityId,
      final FlowWorkflowActivityEntity activityEntity, FlowWorkflowEntity entity,
      final String workflowName) {

    workflowLifecycleService.terminateFlow(activityEntity.getWorkflowId(), workflowName,
        activityId);

    FlowWorkflowActivityEntity finalActivity =
        this.flowActivityService.findWorkflowActivity(activityId);
    if (finalActivity.getStatus() != FlowTaskStatus.failure) {
      finalActivity.setStatus(FlowTaskStatus.completed);
    }

    final Date finishDate = new Date();
    final long duration = finishDate.getTime() - finalActivity.getCreationDate().getTime();
    finalActivity.setDuration(duration);
    activityService.saveWorkflowActivity(finalActivity);
    TaskResult taskResult = new TaskResult();
    taskResult.setStatus(FlowTaskStatus.completed);
    publishActivity(entity, activityEntity, IAMStatus.COMPLETED);
    return taskResult;
  }

  private List<CompletableFuture> createDependancyList(Graph<String, DefaultEdge> graph,
      Map<String, CompletableFuture<TaskResult>> futures, final String vert) {
    List<CompletableFuture> dependnacyList = new LinkedList<>();
    Iterator<DefaultEdge> itr = graph.edgesOf(vert).iterator();
    while (itr.hasNext()) {
      DefaultEdge e = itr.next();
      String source = graph.getEdgeSource(e);
      String destination = graph.getEdgeTarget(e);
      if (vert.equals(destination)) {
        dependnacyList.add(futures.get(source));

      }
    }
    return dependnacyList;
  }

  private TaskResult createDecisionTask(WorkflowGraph workflowGraph, List<Task> tasksToRun, // NOSONAR
      String activityId, final String workflowName, final Map<String, String> executionProperties,
      final String vert) {

    final FlowWorkflowActivityEntity activity =
        this.flowActivityService.findWorkflowActivity(activityId);

    if (vert.equals(workflowGraph.getEnd())) {
      final SingleSourcePaths<String, DefaultEdge> pathToEnd =
          workflowGraph.getDijkstraAlg().getPaths(workflowGraph.getStart());
      final boolean canReachEnd = (pathToEnd.getPath(workflowGraph.getEnd()) != null);

      if (!canReachEnd) {
        activity.setStatus(FlowTaskStatus.failure);
        this.activityService.saveWorkflowActivity(activity);
      }

    } else if (!(vert.equals(workflowGraph.getStart()) || vert.equals(workflowGraph.getEnd()))) {
      final SingleSourcePaths<String, DefaultEdge> pathToEnd =
          workflowGraph.getDijkstraAlg().getPaths(vert);
      final SingleSourcePaths<String, DefaultEdge> pathFromStart =
          workflowGraph.getDijkstraAlg().getPaths(workflowGraph.getStart());
      final boolean canReachEnd = (pathToEnd.getPath(workflowGraph.getEnd()) != null);
      final boolean startToVertex = (pathFromStart.getPath(vert) != null);

      Task task =
          tasksToRun.stream().filter(t -> t.getTaskId().equals(vert)).findFirst().orElse(null);
      if (task != null && canReachEnd && startToVertex) {
        if (task.getTaskType() == TaskType.decision) {
          decisionLifecycleService.processDecision(workflowGraph.getGraph(), tasksToRun, activityId,
              executionProperties, vert, task);
          return decisionLifecycleService.submitDecision(task, activityId);
        } else {
          TaskResult result = null;
          if (task.getTaskType() == TaskType.template) {
            result = taskLifecycleService.submitTask(task, activityId, workflowName);
          } else if (task.getTaskType() == TaskType.customtask) {
            result =
                this.customTaskLifecycleService.submitCustomTask(task, activityId, workflowName);
          }
          processResult(result, workflowGraph.getGraph(), tasksToRun, vert);
          return result;
        }
      } else {
        return skipTask(task, activityId);
      }
    }

    return null;
  }

  private void processResult(TaskResult result, Graph<String, DefaultEdge> graph,
      List<Task> tasksToRun, String currentVert) {

    LOGGER.info("Processing result");

    List<String> matchedNodes = new LinkedList<>();

    Set<DefaultEdge> outgoingEdges = graph.outgoingEdgesOf(currentVert);
    FlowTaskStatus value = result.getStatus();

    for (DefaultEdge edge : outgoingEdges) {

      String destination = graph.getEdgeTarget(edge);

      Task destTask = tasksToRun.stream().filter(t -> t.getTaskId().equals(destination)).findFirst()
          .orElse(null);
      if (destTask != null) {

        LOGGER.info("Outgoing task: {}", destTask.getTaskName());
        determineNodeMatching(currentVert, matchedNodes, value, destTask);
      }
    }

    Iterator<DefaultEdge> itrerator = graph.edgesOf(currentVert).iterator();
    while (itrerator.hasNext()) {
      DefaultEdge e = itrerator.next();
      String destination = graph.getEdgeTarget(e);
      String source = graph.getEdgeSource(e);

      if (source.equals(currentVert)
          && matchedNodes.stream().noneMatch(str -> str.trim().equals(destination))) {
        Task destTask = tasksToRun.stream().filter(t -> t.getTaskId().equals(destination))
            .findFirst().orElse(null);


        if (destTask != null) {
          LOGGER.info("Removing tag: {}", destTask.getTaskName());
        } else {
          LOGGER.error("Shouldn't be null");
        }

        graph.removeEdge(e);
      }
    }
  }

  private void determineNodeMatching(final String currentVert, List<String> matchedNodes,
      FlowTaskStatus status, Task destTask) {
    Optional<Dependency> optionalDependency = destTask.getDetailedDepednacies().stream()
        .filter(d -> d.getTaskId().equals(currentVert)).findAny();
    if (optionalDependency.isPresent()) {
      Dependency dependency = optionalDependency.get();
      WorkflowExecutionCondition condition = dependency.getExecutionCondition();
      String node = destTask.getTaskId();
      if (condition != null
          && (status == FlowTaskStatus.failure && condition == WorkflowExecutionCondition.failure)
          || (status == FlowTaskStatus.completed && condition == WorkflowExecutionCondition.success)
          || (condition == WorkflowExecutionCondition.always)) {
        matchedNodes.add(node);
      }
    }
  }

  private CompletableFuture<Void> createWorkTask(List<Task> tasksToRun, String activityId,
      final String vert, CompletableFuture[] array) {
    return CompletableFuture.allOf(array).whenComplete((v, th) -> {
      FlowWorkflowActivityEntity activity =
          this.flowActivityService.findWorkflowActivity(activityId);
      if (activity.getStatus() == FlowTaskStatus.failure) {
        TaskResult taskResult = new TaskResult();
        taskResult.setNode(vert);
        taskResult.setStatus(FlowTaskStatus.failure);
        return;
      }

      boolean shouldContinueWorkflow = true;

      shouldContinueWorkflow =
          checkForWorkflowFailConditions(tasksToRun, vert, array, shouldContinueWorkflow);



    });
  }

  private boolean checkForWorkflowFailConditions(List<Task> tasksToRun, final String vert,
      CompletableFuture[] array, boolean shouldContinueWorkflow) {
    for (CompletableFuture previousFuture : array) {
      Object futureResult;
      try {
        futureResult = previousFuture.get();
        if (futureResult instanceof TaskResult) {
          TaskResult taskResult = (TaskResult) futureResult;
          Task nextNode =
              tasksToRun.stream().filter(t -> t.getTaskId().equals(vert)).findFirst().orElse(null);

          if (nextNode != null) {
            Dependency dependency = nextNode.getDetailedDepednacies().stream()
                .filter(t -> t.getTaskId().equals(taskResult.getNode())).findFirst().orElse(null);
            shouldContinueWorkflow =
                checkConditions(shouldContinueWorkflow, taskResult, dependency);
          }
        }
      } catch (InterruptedException | ExecutionException e) {
        LOGGER.error(e.getCause());
        Thread.currentThread().interrupt();
        throw new RunWorkflowException();
      }
    }
    return shouldContinueWorkflow;
  }

  private boolean checkConditions(boolean shouldContinueWorkflow, TaskResult taskResult,
      Dependency dependency) {

    if (dependency != null) {
      WorkflowExecutionCondition condition = dependency.getExecutionCondition();
      if ((condition == WorkflowExecutionCondition.failure
          && taskResult.getStatus() == FlowTaskStatus.completed)
          || (condition == WorkflowExecutionCondition.success
              && taskResult.getStatus() == FlowTaskStatus.failure)) {
        shouldContinueWorkflow = false;
      }
    }
    return shouldContinueWorkflow;
  }

  private CompletableFuture<TaskResult> initializeWorkflow() {
    return CompletableFuture.supplyAsync(() -> {
      TaskResult taskResult = new TaskResult();
      taskResult.setStatus(FlowTaskStatus.completed);
      return taskResult;
    });
  }

  private Map<String, String> buildExecutionProperties(
      final FlowWorkflowActivityEntity activityEntity, FlowWorkflowEntity entity) {
    List<FlowProperty> defaultProperties = entity.getProperties();
    Map<String, String> tempExecutionProperties = new HashMap<>();
    Map<String, String> activityProperties = new HashMap<>();
    List<CoreProperty> propertyList = activityEntity.getProperties();
    if (propertyList != null) {
      for (CoreProperty p : propertyList) {
        activityProperties.put(p.getKey(), p.getValue());
      }
    }
    if (defaultProperties != null) {
      tempExecutionProperties = buildExecutionProperties(defaultProperties, activityProperties);
    }
    return tempExecutionProperties;
  }

  private void publishActivity(FlowWorkflowEntity workflow, FlowWorkflowActivityEntity activity,
      IAMStatus status) {
    if (workflow.isEnableACCIntegration()) {
      CoreProperty executionProperty = activity.getProperties().stream()
          .filter(prop -> "execution_id".equals(prop.getKey())).findAny().orElse(null);
      if (executionProperty != null) {
        String executionId = executionProperty.getValue();
        String messageId = activity.getId();
        String activityName = workflow.getName();

        this.iamClient.publishEvent(executionId, messageId, activityName, status);
      }
    }
  }

  private TaskResult skipTask(Task task, String activityId) {

    if (task == null) {
      return new TaskResult();
    }
    FlowTaskExecutionEntity taskExecution =
        taskService.findByTaskIdAndActiityId(task.getTaskId(), activityId);

    if (taskExecution != null) {
      taskExecution.setFlowTaskStatus(FlowTaskStatus.skipped);
      taskService.save(taskExecution);
    }

    TaskResult taskResult = new TaskResult();
    taskResult.setNode(task.getTaskId());
    taskResult.setStatus(FlowTaskStatus.skipped);

    return taskResult;
  }

  private Map<String, String> buildExecutionProperties(List<FlowProperty> defaultProperties,
      Map<String, String> properties) {
    Map<String, String> executionProperties = new HashMap<>();

    for (FlowProperty flowProperty : defaultProperties) {
      executionProperties.put(flowProperty.getKey(), flowProperty.getDefaultValue());
    }

    if (properties == null) {
      return executionProperties;
    }

    for (String keyProperty : properties.keySet()) {

      String key = keyProperty;
      String value = properties.get(key);
      executionProperties.put(key, value);

    }

    return executionProperties;
  }

}
