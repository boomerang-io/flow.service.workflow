package net.boomerangplatform.service;

import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import net.boomerangplatform.exceptions.RunWorkflowException;
import net.boomerangplatform.model.Task;
import net.boomerangplatform.mongo.entity.ActivityEntity;
import net.boomerangplatform.mongo.entity.FlowTaskTemplateEntity;
import net.boomerangplatform.mongo.entity.RevisionEntity;
import net.boomerangplatform.mongo.entity.TaskExecutionEntity;
import net.boomerangplatform.mongo.entity.WorkflowEntity;
import net.boomerangplatform.mongo.model.CoreProperty;
import net.boomerangplatform.mongo.model.Dag;
import net.boomerangplatform.mongo.model.Revision;
import net.boomerangplatform.mongo.model.TaskStatus;
import net.boomerangplatform.mongo.model.TaskType;
import net.boomerangplatform.mongo.model.internal.InternalTaskRequest;
import net.boomerangplatform.mongo.model.next.DAGTask;
import net.boomerangplatform.mongo.model.next.Dependency;
import net.boomerangplatform.mongo.service.ActivityTaskService;
import net.boomerangplatform.mongo.service.FlowTaskTemplateService;
import net.boomerangplatform.mongo.service.FlowWorkflowActivityService;
import net.boomerangplatform.mongo.service.RevisionService;
import net.boomerangplatform.service.crud.FlowActivityService;
import net.boomerangplatform.service.crud.WorkflowService;
import net.boomerangplatform.service.refactor.DAGUtility;
import net.boomerangplatform.service.refactor.TaskClient;
import net.boomerangplatform.service.runner.misc.ControllerClient;
import net.boomerangplatform.util.GraphProcessor;

@Service
public class FlowExecutionServiceImpl implements FlowExecutionService {

  @Autowired
  private FlowActivityService flowActivityService;

  @Autowired
  private RevisionService flowRevisionService;

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
  private ActivityTaskService taskActivityService;
  
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

  public void prepareExecution(List<Task> tasks, String activityId) {
    final Task start = getTaskByName(tasks, TaskType.start);
    final Task end = getTaskByName(tasks, TaskType.end);
    final Graph<String, DefaultEdge> graph = createGraph(tasks);
    dagUtility.validateWorkflow(activityId, start, end, graph);
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

      final FlowTaskTemplateEntity taskTemplateEntity =
          taskTemplateService.getTaskTemplateWithId(task.getTaskId());
      TaskExecutionEntity taskExecution = new TaskExecutionEntity();
      taskExecution.setActivityId(activityId);
      taskExecution.setTaskId(task.getTaskId());
      taskExecution.setFlowTaskStatus(TaskStatus.notstarted);
      taskExecution.setOrder(order);
      taskExecution.setTaskName(task.getTaskName());
      taskExecution.setTaskType(task.getTaskType());
      
      if (taskTemplateEntity != null) {
        taskExecution.setTaskName(taskTemplateEntity.getName());
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

    
    final ActivityEntity activityEntity =
        this.flowActivityService.findWorkflowActivity(activityId);
    activityEntity.setStatus(TaskStatus.inProgress);
    activityEntity.setCreationDate(new Date());
    activityService.saveWorkflowActivity(activityEntity);
    
    WorkflowEntity workflow = workflowService.getWorkflow(activityEntity.getWorkflowId());
    
    boolean enablePVC = workflow.isEnablePersistentStorage();
    String workflowName = workflow.getName();
    String workflowId = workflow.getId();
    
    Map<String, String> executionProperties = dagUtility.buildExecutionProperties(activityEntity, workflow);
    controllerClient.createFlow(workflowId, workflowName, activityId, enablePVC, executionProperties);
    
    final Task startTask =  tasksToRun.stream().filter(tsk -> TaskType.start.equals(tsk.getTaskType())).findAny().orElse(null);
    executeNextStep(activityEntity, tasksToRun,startTask, start, end, graph);
  }
  
  private void executeNextStep(ActivityEntity workflowActivity, List<Task> tasks,
      Task currentTask, final Task start, final Task end,
      final Graph<String, DefaultEdge> graph) {
    
    try {
      List<Task> nextNodes = this.getTasksDependants(tasks, currentTask);
      for (Task next : nextNodes) {
     
        final List<String> nodes =
            GraphProcessor.createOrderedTaskList(graph, start.getTaskId(), end.getTaskId());
        
        if (nodes.contains(next.getTaskId())) {
          InternalTaskRequest taskRequest = new InternalTaskRequest();
          taskRequest.setActivityId(next.getTaskActivityId());
          taskClient.startTask(taskRequest);
        }       
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
   
  }
  
  private List<Task> getTasksDependants(List<Task> tasks, Task currentTask) {
   return
        tasks.stream().filter(c -> c.getDependencies().contains(currentTask.getTaskId()))
            .collect(Collectors.toList());
  }


  @Override
  public CompletableFuture<Boolean> executeWorkflowVersion(String workFlowId, String activityId) {
    final RevisionEntity entity =
        this.flowRevisionService.getWorkflowlWithId(workFlowId);
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
