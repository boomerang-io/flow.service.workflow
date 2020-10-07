package net.boomerangplatform.service.refactor;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.jgrapht.Graph;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm.SingleSourcePaths;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import net.boomerangplatform.exceptions.InvalidWorkflowRuntimeException;
import net.boomerangplatform.model.Task;
import net.boomerangplatform.model.TaskResult;
import net.boomerangplatform.mongo.entity.ActivityEntity;
import net.boomerangplatform.mongo.entity.FlowTaskTemplateEntity;
import net.boomerangplatform.mongo.entity.RevisionEntity;
import net.boomerangplatform.mongo.entity.TaskExecutionEntity;
import net.boomerangplatform.mongo.entity.WorkflowEntity;
import net.boomerangplatform.mongo.model.CoreProperty;
import net.boomerangplatform.mongo.model.Dag;
import net.boomerangplatform.mongo.model.FlowProperty;
import net.boomerangplatform.mongo.model.Revision;
import net.boomerangplatform.mongo.model.TaskStatus;
import net.boomerangplatform.mongo.model.TaskType;
import net.boomerangplatform.mongo.model.WorkflowExecutionCondition;
import net.boomerangplatform.mongo.model.next.DAGTask;
import net.boomerangplatform.mongo.model.next.Dependency;
import net.boomerangplatform.mongo.service.ActivityTaskService;
import net.boomerangplatform.mongo.service.FlowTaskTemplateService;
import net.boomerangplatform.mongo.service.FlowWorkflowActivityService;
import net.boomerangplatform.mongo.service.FlowWorkflowService;
import net.boomerangplatform.mongo.service.RevisionService;
import net.boomerangplatform.service.runner.misc.DecisionLifecycleService;
import net.boomerangplatform.util.GraphProcessor;

@Service
public class DAGUtility {

  @Autowired
  private RevisionService workflowVersionService;

  @Autowired
  private ActivityTaskService taskActivityService;

  @Autowired
  private FlowTaskTemplateService templateService;

  @Autowired
  private FlowWorkflowService flowWorkflowService;

  @Autowired
  private DecisionLifecycleService decisionService;

  @Autowired
  private FlowWorkflowActivityService activityService;

  
  public boolean validateWorkflow(ActivityEntity workflowActivity) {
    
    RevisionEntity revision =
        workflowVersionService.getWorkflowlWithId(workflowActivity.getWorkflowRevisionid());
    List<Task> tasks = this.createTaskList(revision, workflowActivity);
   
    final Task start = tasks.stream().filter(tsk -> TaskType.start.equals(tsk.getTaskType()))
        .findAny().orElse(null);
    
    final Task end = tasks.stream().filter(tsk -> TaskType.end.equals(tsk.getTaskType()))
        .findAny().orElse(null);

    Graph<String, DefaultEdge> graph = this.createGraph(tasks, workflowActivity);
    DijkstraShortestPath<String, DefaultEdge> dijkstraAlg = new DijkstraShortestPath<>(graph);
    final SingleSourcePaths<String, DefaultEdge> pathFromStart =
        dijkstraAlg.getPaths(start.getTaskId());
    final boolean startToVertex = (pathFromStart.getPath(end.getTaskId()) != null);
    return startToVertex;
  }
  
  public boolean canCompleteTask(ActivityEntity workflowActivity, String taskId) {
    RevisionEntity revision =
        workflowVersionService.getWorkflowlWithId(workflowActivity.getWorkflowRevisionid());
    List<Task> tasks = this.createTaskList(revision, workflowActivity);
    final Task start = tasks.stream().filter(tsk -> TaskType.start.equals(tsk.getTaskType()))
        .findAny().orElse(null);
    final Task current =
        tasks.stream().filter(tsk -> taskId.equals(tsk.getTaskId())).findAny().orElse(null);
    Graph<String, DefaultEdge> graph = this.createGraph(tasks, workflowActivity);
    DijkstraShortestPath<String, DefaultEdge> dijkstraAlg = new DijkstraShortestPath<>(graph);
    final SingleSourcePaths<String, DefaultEdge> pathFromStart =
        dijkstraAlg.getPaths(start.getTaskId());
    final boolean startToVertex = (pathFromStart.getPath(current.getTaskId()) != null);
    return startToVertex;
  }

  private Graph<String, DefaultEdge> createGraph(List<Task> tasks, ActivityEntity activity) {
    WorkflowEntity workflow = this.flowWorkflowService.getWorkflow(activity.getWorkflowId());
    final Map<String, String> executionProperties = buildExecutionProperties(activity, workflow);

    Graph<String, DefaultEdge> graph = createGraph(tasks);
    TopologicalOrderIterator<String, DefaultEdge> orderIterator =
        new TopologicalOrderIterator<>(graph);
    while (orderIterator.hasNext()) {

      final String taskId = orderIterator.next();
      Task currentTask = this.getTaskByid(tasks, taskId);
      if (TaskType.start != currentTask.getTaskType() && TaskType.end != currentTask.getTaskType()) {
        if (currentTask.getTaskActivityId() == null) {
          continue;
        }
        
        TaskExecutionEntity taskExecution =
            taskActivityService.findById(currentTask.getTaskActivityId());
        if (taskExecution == null) {
          System.out.println("What to do");
        }
        TaskStatus flowTaskStatus = taskExecution.getFlowTaskStatus();
        if (flowTaskStatus == TaskStatus.completed || flowTaskStatus == TaskStatus.failure) {
          if (currentTask.getTaskType() == TaskType.decision) {
            decisionService.processDecision(graph, tasks, activity.getId(), executionProperties,
                currentTask.getTaskId(), currentTask);
          } else {
            TaskResult result = new TaskResult();
            result.setStatus(flowTaskStatus);
            this.updateTaskInGraph(result, graph, tasks, taskId);
          }
        }
      }
    }

    return graph;
  }

  public Map<String, String> buildExecutionProperties(final ActivityEntity activityEntity,
      WorkflowEntity entity) {
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

  private void updateTaskInGraph(TaskResult result, Graph<String, DefaultEdge> graph,
      List<Task> tasksToRun, String currentVert) {
    List<String> matchedNodes = new LinkedList<>();
    Set<DefaultEdge> outgoingEdges = graph.outgoingEdgesOf(currentVert);
    TaskStatus value = result.getStatus();

    for (DefaultEdge edge : outgoingEdges) {
      String destination = graph.getEdgeTarget(edge);
      Task destTask = tasksToRun.stream().filter(t -> t.getTaskId().equals(destination)).findFirst()
          .orElse(null);
      if (destTask != null) {
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
        graph.removeEdge(e);
      }
    }
  }

  private void determineNodeMatching(final String currentVert, List<String> matchedNodes,
      TaskStatus status, Task destTask) {
    Optional<Dependency> optionalDependency = destTask.getDetailedDepednacies().stream()
        .filter(d -> d.getTaskId().equals(currentVert)).findAny();
    if (optionalDependency.isPresent()) {
      Dependency dependency = optionalDependency.get();
      WorkflowExecutionCondition condition = dependency.getExecutionCondition();
      String node = destTask.getTaskId();
      if (condition != null
          && (status == TaskStatus.failure && condition == WorkflowExecutionCondition.failure)
          || (status == TaskStatus.completed && condition == WorkflowExecutionCondition.success)
          || (condition == WorkflowExecutionCondition.always)) {
        matchedNodes.add(node);
      }
    }
  }

  private Graph<String, DefaultEdge> createGraph(List<Task> tasks) {
    final List<String> vertices = tasks.stream().map(Task::getTaskId).collect(Collectors.toList());

    final List<Pair<String, String>> edgeList = new LinkedList<>();
    for (final Task task : tasks) {
      for (final String dep : task.getDependencies()) {
        final Pair<String, String> pair = Pair.of(dep, task.getTaskId());
        edgeList.add(pair);
      }
    }
    return GraphProcessor.createGraph(vertices, edgeList);
  }

  private Task getTaskByid(List<Task> tasks, String id) {
    return tasks.stream().filter(tsk -> id.equals(tsk.getTaskId())).findAny().orElse(null);
  }

  private List<Task> createTaskList(RevisionEntity revisionEntity, ActivityEntity activity) {

    final Dag dag = revisionEntity.getDag();

    final List<Task> taskList = new LinkedList<>();
    for (final DAGTask dagTask : dag.getTasks()) {

      final Task newTask = new Task();
      newTask.setTaskId(dagTask.getTaskId());
      newTask.setTaskType(dagTask.getType());
      newTask.setTaskName(dagTask.getLabel());
      
      TaskExecutionEntity task =
          taskActivityService.findByTaskIdAndActiityId(dagTask.getTaskId(), activity.getId());
      if (task != null) {
        newTask.setTaskActivityId(task.getId());
      }
      
      

      final String workFlowId = revisionEntity.getWorkFlowId();
      newTask.setWorkflowId(workFlowId);

      if (dagTask.getType() == TaskType.template || dagTask.getType() == TaskType.customtask) {
        String templateId = dagTask.getTemplateId();
        final FlowTaskTemplateEntity flowTaskTemplate =
            templateService.getTaskTemplateWithId(templateId);
        Integer templateVersion = dagTask.getTemplateVersion();
        List<Revision> revisions = flowTaskTemplate.getRevisions();
        if (revisions != null) {
          Optional<Revision> result = revisions.stream().parallel()
              .filter(revision -> revision.getVersion().equals(templateVersion)).findAny();
          if (result.isPresent()) {
            Revision revision = result.get();
            newTask.setRevision(revision);
          } else {
            Optional<Revision> latestRevision = revisions.stream()
                .sorted(Comparator.comparingInt(Revision::getVersion).reversed()).findFirst();
            if (latestRevision.isPresent()) {
              newTask.setRevision(latestRevision.get());
            }
          }
        } else {
          throw new IllegalArgumentException("Invalid task template selected: " + templateId);

        }

        Map<String, String> properties = new HashMap<>();
        if (dagTask.getProperties() != null) {
          for (CoreProperty property : dagTask.getProperties()) {
            properties.put(property.getKey(), property.getValue());
          }
        }
        newTask.setInputs(properties);
      } else if (dagTask.getType() == TaskType.decision) {
        newTask.setDecisionValue(dagTask.getDecisionValue());
      }

      final List<String> taskDepedancies = new LinkedList<>();
      for (Dependency dependency : dagTask.getDependencies()) {
        taskDepedancies.add(dependency.getTaskId());
      }
      newTask.setDetailedDepednacies(dagTask.getDependencies());
      newTask.setDependencies(taskDepedancies);
      taskList.add(newTask);
    }
    return taskList;
  }

  private void determineNodeMatching(final String currentVert, List<String> matchedNodes,
      List<String> defaultNodes, String value, Task destTask) {
    Optional<Dependency> optionalDependency = destTask.getDetailedDepednacies().stream()
        .filter(d -> d.getTaskId().equals(currentVert)).findAny();
    if (optionalDependency.isPresent()) {
      Dependency dependency = optionalDependency.get();
      String linkValue = dependency.getSwitchCondition();

      String node = destTask.getTaskId();

      boolean matched = false;

      if (linkValue != null) {
        String[] lines = linkValue.split("\\r?\\n");
        for (String line : lines) {
          String patternString = line;
          Pattern pattern = Pattern.compile(patternString);
          Matcher matcher = pattern.matcher(value);
          if (matcher.matches()) {
            matched = true;
          }
        }
        if (matched) {
          matchedNodes.add(node);
        }
      } else {
        defaultNodes.add(node);
      }
    }
  }

  public List<String> calculateNodesToRemove(Graph<String, DefaultEdge> graph,
      List<Task> tasksToRun, String activityId, final Map<String, String> executionProperties,
      final String currentVert, Task currentTask) {
    Set<DefaultEdge> outgoingEdges = graph.outgoingEdgesOf(currentVert);

    List<String> matchedNodes = new LinkedList<>();
    List<String> defaultNodes = new LinkedList<>();

    String value = currentTask.getDecisionValue();
    value = replaceValueWithProperty(value, executionProperties, activityId);

    for (DefaultEdge edge : outgoingEdges) {
      String destination = graph.getEdgeTarget(edge);
      Task destTask = tasksToRun.stream().filter(t -> t.getTaskId().equals(destination)).findFirst()
          .orElse(null);
      if (destTask != null) {
        determineNodeMatching(currentVert, matchedNodes, defaultNodes, value, destTask);
      }
    }
    List<String> removeList = matchedNodes;
    if (matchedNodes.isEmpty()) {
      removeList = defaultNodes;
    }
    return removeList;
  }

  private String replaceValueWithProperty(String value, Map<String, String> executionProperties,
      String activityId) {
    String regex = "\\$\\{p:(.*?)\\}";
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(value);
    if (matcher.find()) {
      String group = matcher.group(1);
      String[] components = group.split("/");
      if (components.length == 1) {
        if (executionProperties.get(components[0]) != null) {

          return executionProperties.get(components[0]);
        }
      } else if (components.length == 2) {
        String taskName = components[0];
        String outputProperty = components[1];
        TaskExecutionEntity taskExecution =
            taskActivityService.findByTaskNameAndActiityId(taskName, activityId);
        if (taskExecution != null && taskExecution.getOutputs() != null
            && taskExecution.getOutputs().get(outputProperty) != null) {
          return taskExecution.getOutputs().get(outputProperty);
        }
      }
    }
    return value;
  }

  public void processDecision(Graph<String, DefaultEdge> graph, List<Task> tasksToRun,
      String activityId, final Map<String, String> executionProperties, final String currentVertex,
      Task currentTask) {
    List<String> removeList = calculateNodesToRemove(graph, tasksToRun, activityId,
        executionProperties, currentVertex, currentTask);
    Iterator<DefaultEdge> itrerator = graph.edgesOf(currentVertex).iterator();
    while (itrerator.hasNext()) {
      DefaultEdge e = itrerator.next();
      String destination = graph.getEdgeTarget(e);
      String source = graph.getEdgeSource(e);

      if (source.equals(currentVertex)
          && removeList.stream().noneMatch(str -> str.trim().equals(destination))) {
        graph.removeEdge(e);
      }
    }
  }

  public void validateWorkflow(String activityId, final Task start, final Task end,
      final Graph<String, DefaultEdge> graph) {

    final ActivityEntity activityEntity = activityService.findWorkflowActivtyById(activityId);

    if (start == null || end == null) {
      activityEntity.setStatus(TaskStatus.invalid);
      activityService.saveWorkflowActivity(activityEntity);
      throw new InvalidWorkflowRuntimeException();
    }

    final List<String> nodes =
        GraphProcessor.createOrderedTaskList(graph, start.getTaskId(), end.getTaskId());

    if (nodes.isEmpty()) {
      activityEntity.setStatus(TaskStatus.invalid);
      activityService.saveWorkflowActivity(activityEntity);
      throw new InvalidWorkflowRuntimeException();
    }

    final DijkstraShortestPath<String, DefaultEdge> dijkstraAlg = new DijkstraShortestPath<>(graph);
    final SingleSourcePaths<String, DefaultEdge> pathFromStart =
        dijkstraAlg.getPaths(start.getTaskId());
    final boolean singlePathExists = (pathFromStart.getPath(end.getTaskId()) != null);
    if (!singlePathExists) {

      activityEntity.setStatus(TaskStatus.invalid);
      activityEntity.setStatusMessage("Failed to run workflow: Incomplete workflow");
      activityService.saveWorkflowActivity(activityEntity);
      throw new InvalidWorkflowRuntimeException();
    }
  }


  
  public String getOutputProperty(String expression, ActivityEntity activity) {
    
    WorkflowEntity workflow = this.flowWorkflowService.getWorkflow(activity.getWorkflowId());
    final Map<String, String> executionProperties = buildExecutionProperties(activity, workflow);
    String regex = "\\$\\{p:(.*?)\\}";
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(expression);
    if (matcher.find()) {
      String group = matcher.group(1);
      String[] components = group.split("/");
      if (components.length == 1) {
        if (executionProperties.get(components[0]) != null) {
          return executionProperties.get(components[0]);
        }
      } else if (components.length == 2) {
        String taskName = components[0];
        String outputProperty = components[1];
        TaskExecutionEntity taskExecution =
            taskActivityService.findByTaskNameAndActiityId(taskName, activity.getId());
        if (taskExecution != null && taskExecution.getOutputs() != null
            && taskExecution.getOutputs().get(outputProperty) != null) {
          return taskExecution.getOutputs().get(outputProperty);
        }
      }
    }
    
    if (expression != null && !expression.isBlank()) {
      return expression;
    }
    return null;
  }
}
