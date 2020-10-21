package net.boomerangplatform.service.runner.misc;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import net.boomerangplatform.model.Task;
import net.boomerangplatform.model.TaskResult;
import net.boomerangplatform.mongo.entity.TaskExecutionEntity;
import net.boomerangplatform.mongo.model.TaskStatus;
import net.boomerangplatform.mongo.model.next.Dependency;
import net.boomerangplatform.mongo.service.ActivityTaskService;

@Service
public class DecisionLifecycleService {

  @Autowired
  private ActivityTaskService taskService;

  public TaskResult submitDecision(Task task, String activityId) {
    TaskResult taskResult = new TaskResult();
    TaskExecutionEntity taskExecution =
        taskService.findByTaskIdAndActiityId(task.getTaskId(), activityId);
    final Date startDate = new Date();
    taskExecution.setStartTime(startDate);
    taskExecution.setFlowTaskStatus(TaskStatus.completed);

    final Date finishDate = new Date();
    final long duration = finishDate.getTime() - startDate.getTime();
    taskExecution.setDuration(duration);
    taskExecution.setFlowTaskStatus(TaskStatus.completed);
    taskResult.setNode(task.getTaskId());
    taskResult.setStatus(taskExecution.getFlowTaskStatus());
    taskService.save(taskExecution);
    return taskResult;
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
            taskService.findByTaskNameAndActiityId(taskName, activityId);
        if (taskExecution != null && taskExecution.getOutputs() != null
            && taskExecution.getOutputs().get(outputProperty) != null) {
          return taskExecution.getOutputs().get(outputProperty);
        }
      }
    }
    return value;
  }

}
