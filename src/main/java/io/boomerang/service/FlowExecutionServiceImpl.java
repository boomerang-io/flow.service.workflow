package io.boomerang.service;

import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.boomerang.exceptions.InvalidWorkflowRuntimeException;
import io.boomerang.exceptions.RunWorkflowException;
import io.boomerang.model.Task;
import io.boomerang.mongo.entity.ActivityEntity;
import io.boomerang.mongo.entity.FlowTaskTemplateEntity;
import io.boomerang.mongo.entity.RevisionEntity;
import io.boomerang.mongo.entity.TaskExecutionEntity;
import io.boomerang.mongo.entity.WorkflowEntity;
import io.boomerang.mongo.model.ActivityStorage;
import io.boomerang.mongo.model.Dag;
import io.boomerang.mongo.model.KeyValuePair;
import io.boomerang.mongo.model.Revision;
import io.boomerang.mongo.model.Storage;
import io.boomerang.mongo.model.TaskStatus;
import io.boomerang.mongo.model.TaskType;
import io.boomerang.mongo.model.internal.InternalTaskRequest;
import io.boomerang.mongo.model.next.DAGTask;
import io.boomerang.mongo.model.next.Dependency;
import io.boomerang.mongo.service.FlowTaskTemplateService;
import io.boomerang.mongo.service.FlowWorkflowActivityService;
import io.boomerang.mongo.service.RevisionService;
import io.boomerang.service.crud.FlowActivityService;
import io.boomerang.service.crud.WorkflowService;
import io.boomerang.service.refactor.DAGUtility;
import io.boomerang.service.refactor.TaskClient;
import io.boomerang.service.refactor.TaskService;
import io.boomerang.service.runner.misc.ControllerClient;
import io.boomerang.util.GraphProcessor;

@Service
public class FlowExecutionServiceImpl implements FlowExecutionService {

  @Autowired
  private FlowActivityService flowActivityService;

  @Autowired
  private RevisionService flowRevisionService;
  @Autowired
  private TaskService taskService;



  @Autowired
  private FlowTaskTemplateService taskTemplateService;

  @Autowired
  private FlowTaskTemplateService templateService;

  @Autowired
  private FlowWorkflowActivityService activityService;

  @Autowired
  private TaskClient taskClient;

  @Autowired
  private DAGUtility dagUtility;

  @Autowired
  private WorkflowService workflowService;

  @Autowired
  private ControllerClient controllerClient;

  private static final Logger LOGGER = LogManager.getLogger(FlowExecutionServiceImpl.class);

  private List<Task> createTaskList(RevisionEntity revisionEntity) { // NOSONAR

    final Dag dag = revisionEntity.getDag();

    final List<Task> taskList = new LinkedList<>();
    for (final DAGTask dagTask : dag.getTasks()) {

      final Task newTask = new Task();
      newTask.setTaskId(dagTask.getTaskId());
      newTask.setTaskType(dagTask.getType());
      newTask.setTaskName(dagTask.getLabel());

      final String workFlowId = revisionEntity.getWorkFlowId();
      newTask.setWorkflowId(workFlowId);

      if (dagTask.getType() == TaskType.script || dagTask.getType() == TaskType.template
          || dagTask.getType() == TaskType.customtask) {
        String templateId = dagTask.getTemplateId();
        final FlowTaskTemplateEntity flowTaskTemplate =
            templateService.getTaskTemplateWithId(templateId);
        newTask.setTemplateId(flowTaskTemplate.getId());

        Integer templateVersion = dagTask.getTemplateVersion();
        List<Revision> revisions = flowTaskTemplate.getRevisions();
        if (revisions != null) {
          Optional<Revision> result = revisions.stream().parallel()
              .filter(revision -> revision.getVersion().equals(templateVersion)).findAny();
          if (result.isPresent()) {
            Revision revision = result.get();
            newTask.setRevision(revision);
            newTask.setResults(revision.getResults());
          } else {
            Optional<Revision> latestRevision = revisions.stream()
                .sorted(Comparator.comparingInt(Revision::getVersion).reversed()).findFirst();
            if (latestRevision.isPresent()) {
              newTask.setRevision(latestRevision.get());
              newTask.setResults(newTask.getRevision().getResults());
            }
          }
        } else {
          throw new IllegalArgumentException("Invalid task template selected: " + templateId);

        }

        Map<String, String> properties = new HashMap<>();
        if (dagTask.getProperties() != null) {
          for (KeyValuePair property : dagTask.getProperties()) {
            properties.put(property.getKey(), property.getValue());
          }
        }

        newTask.setInputs(properties);

        if (newTask.getResults() == null) {
          newTask.setResults(dagTask.getResults());
        }

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

  public void prepareExecution(List<Task> tasks, String activityId) {
    final Task start = getTaskByName(tasks, TaskType.start);
    final Task end = getTaskByName(tasks, TaskType.end);
    final Graph<String, DefaultEdge> graph = createGraph(tasks);

    final ActivityEntity activityEntity = activityService.findWorkflowActivtyById(activityId);

    boolean validWorkflow = dagUtility.validateWorkflow(activityEntity);

    if (!validWorkflow) {
      //Workflow activity changed to invalid
      activityEntity.setStatus(TaskStatus.invalid);
      activityEntity.setStatusMessage("Failed to run workflow: Incomplete workflow");
      activityService.saveWorkflowActivity(activityEntity);
      throw new InvalidWorkflowRuntimeException();
    }

    createTaskPlan(tasks, activityId, start, end, graph);
  }


  private void createTaskPlan(List<Task> tasks, String activityId, final Task start, final Task end,
      final Graph<String, DefaultEdge> graph) {

    final List<String> nodes =
        GraphProcessor.createOrderedTaskList(graph, start.getTaskId(), end.getTaskId());
    final List<Task> tasksToRun = new LinkedList<>();
    for (final String node : nodes) {
      final Task taskToAdd =
          tasks.stream().filter(tsk -> node.equals(tsk.getTaskId())).findAny().orElse(null);
      tasksToRun.add(taskToAdd);
    }

    long order = 1;
    for (final Task task : tasksToRun) {


      TaskExecutionEntity taskExecution = new TaskExecutionEntity();
      taskExecution.setActivityId(activityId);
      taskExecution.setTaskId(task.getTaskId());
      taskExecution.setFlowTaskStatus(TaskStatus.notstarted);
      taskExecution.setOrder(order);
      taskExecution.setTaskName(task.getTaskName());
      taskExecution.setTaskType(task.getTaskType());

      if (task.getTemplateId() != null) {
        final FlowTaskTemplateEntity taskTemplateEntity =
            taskTemplateService.getTaskTemplateWithId(task.getTemplateId());
        taskExecution.setTemplateId(taskTemplateEntity.getId());
        taskExecution.setTemplateRevision(task.getRevision().getVersion());
      }


      taskExecution = this.flowActivityService.saveTaskExecution(taskExecution);

      task.setTaskActivityId(taskExecution.getId());

      order++;
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

  private Task getTaskByName(List<Task> tasks, TaskType type) {
    return tasks.stream().filter(tsk -> type.equals(tsk.getTaskType())).findAny().orElse(null);
  }

  private void executeWorkflowAsync(String activityId, final Task start, final Task end,
      final Graph<String, DefaultEdge> graph, final List<Task> tasksToRun)
      throws ExecutionException {

    if (tasksToRun.size() == 2) {
      final ActivityEntity activityEntity =
          this.flowActivityService.findWorkflowActivity(activityId);
      //Workflow activity changed to completed
      activityEntity.setStatus(TaskStatus.completed);
      activityEntity.setCreationDate(new Date());
      activityService.saveWorkflowActivity(activityEntity);

      return;
    }

    final ActivityEntity activityEntity = this.flowActivityService.findWorkflowActivity(activityId);
    activityEntity.setStatus(TaskStatus.inProgress);
    activityEntity.setCreationDate(new Date());
    activityService.saveWorkflowActivity(activityEntity);

    WorkflowEntity workflow = workflowService.getWorkflow(activityEntity.getWorkflowId());
    if (workflow.getStorage() == null) {
      workflow.setStorage(new Storage());
    }
    if (workflow.getStorage().getActivity() == null) {
      workflow.getStorage().setActivity(new ActivityStorage());
    }

    boolean enablePVC = workflow.getStorage().getActivity().getEnabled();

    String workflowName = workflow.getName();
    String workflowId = workflow.getId();

    Map<String, String> executionProperties = new HashMap<>();
    Map<String, Object> inputs = new HashMap<>();
    try {
      for (Entry<String, Object> entry : inputs.entrySet()) {
        if (entry.getValue() != null) {
          executionProperties.put(entry.getKey(), entry.getValue().toString());
        } else {
          executionProperties.put(entry.getKey(), null);
        }

      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    List<KeyValuePair> labels = workflow.getLabels();

    controllerClient.createFlow(workflowId, workflowName, activityId, enablePVC, labels,
        executionProperties);

    final Task startTask = tasksToRun.stream()
        .filter(tsk -> TaskType.start.equals(tsk.getTaskType())).findAny().orElse(null);
    executeNextStep(activityEntity, tasksToRun, startTask, start, end, graph);
  }

  private void executeNextStep(ActivityEntity workflowActivity, List<Task> tasks, Task currentTask,
      final Task start, final Task end, final Graph<String, DefaultEdge> graph) {

    try {
      List<Task> nextNodes = this.getTasksDependants(tasks, currentTask);
      for (Task next : nextNodes) {

        final List<String> nodes =
            GraphProcessor.createOrderedTaskList(graph, start.getTaskId(), end.getTaskId());

        if (nodes.contains(next.getTaskId())) {
          InternalTaskRequest taskRequest = new InternalTaskRequest();
          taskRequest.setActivityId(next.getTaskActivityId());
          taskClient.startTask(taskService, taskRequest);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  private List<Task> getTasksDependants(List<Task> tasks, Task currentTask) {
    return tasks.stream().filter(c -> c.getDependencies().contains(currentTask.getTaskId()))
        .collect(Collectors.toList());
  }


  @Override
  public CompletableFuture<Boolean> executeWorkflowVersion(String workFlowId, String activityId) {
    final RevisionEntity entity = this.flowRevisionService.getWorkflowlWithId(workFlowId);
    final List<Task> tasks = createTaskList(entity);
    prepareExecution(tasks, activityId);
    return CompletableFuture.supplyAsync(createProcess(activityId, tasks));
  }

  private Supplier<Boolean> createProcess(String activityId, List<Task> tasks) {
    return () -> {
      final Task start = getTaskByName(tasks, TaskType.start);
      final Task end = getTaskByName(tasks, TaskType.end);
      final Graph<String, DefaultEdge> graph = createGraph(tasks);
      try {

        executeWorkflowAsync(activityId, start, end, graph, tasks);
      } catch (ExecutionException e) {
        LOGGER.error(ExceptionUtils.getStackTrace(e));
        throw new RunWorkflowException();
      }
      return true;
    };
  }
}
