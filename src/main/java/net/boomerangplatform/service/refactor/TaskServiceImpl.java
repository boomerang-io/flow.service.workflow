package net.boomerangplatform.service.refactor;

import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.github.alturkovic.lock.Lock;
import com.github.alturkovic.lock.exception.LockNotAvailableException;
import net.boomerangplatform.model.Task;
import net.boomerangplatform.mongo.entity.ActivityEntity;
import net.boomerangplatform.mongo.entity.FlowTaskTemplateEntity;
import net.boomerangplatform.mongo.entity.RevisionEntity;
import net.boomerangplatform.mongo.entity.TaskExecutionEntity;
import net.boomerangplatform.mongo.entity.WorkflowEntity;
import net.boomerangplatform.mongo.model.CoreProperty;
import net.boomerangplatform.mongo.model.Dag;
import net.boomerangplatform.mongo.model.TaskStatus;
import net.boomerangplatform.mongo.model.Revision;
import net.boomerangplatform.mongo.model.TaskType;
import net.boomerangplatform.mongo.model.internal.InternalTaskRequest;
import net.boomerangplatform.mongo.model.internal.InternalTaskResponse;
import net.boomerangplatform.mongo.model.next.DAGTask;
import net.boomerangplatform.mongo.model.next.Dependency;
import net.boomerangplatform.mongo.service.FlowTaskTemplateService;
import net.boomerangplatform.mongo.service.FlowWorkflowActivityService;
import net.boomerangplatform.mongo.service.ActivityTaskService;
import net.boomerangplatform.mongo.service.FlowWorkflowService;
import net.boomerangplatform.mongo.service.RevisionService;
import net.boomerangplatform.service.runner.misc.ControllerClient;

@Service
/**
 * Start, complete and initiate next task in DAG.
 * 
 * @author mdroy
 *
 */
public class TaskServiceImpl implements TaskService {

  @Autowired
  private ControllerClient controllerClient;

  @Autowired
  private FlowWorkflowActivityService activityService;

  @Autowired
  private FlowWorkflowService workflowService;

  @Autowired
  private FlowTaskTemplateService templateService;

  @Autowired
  private RevisionService workflowVersionService;

  @Autowired
  private ActivityTaskService taskActivityService;

  @Autowired
  private TaskClient flowClient;

  @Autowired
  private DAGUtility dagUtility;

  @Autowired
  private Lock lock;

  @Override
  @Async
  public void createTask(InternalTaskRequest request) {

    String taskId = request.getActivityId();
    TaskExecutionEntity taskExecution = taskActivityService.findById(taskId);
    ActivityEntity activity =
        activityService.findWorkflowActiivtyById(taskExecution.getActivityId());
    WorkflowEntity workflow = workflowService.getWorkflow(activity.getWorkflowId());
    String workflowName = workflow.getName();

    Task task = getTask(taskExecution, workflow);
    TaskType taskType = task.getTaskType();

    if (taskExecution.getFlowTaskStatus() != TaskStatus.notstarted) {
      return;
    }

    taskExecution.setStartTime(new Date());
    taskExecution.setFlowTaskStatus(TaskStatus.inProgress);
    taskExecution = taskActivityService.save(taskExecution);

    boolean canRunTask = dagUtility.canCompleteTask(activity, task.getTaskId());

    String activityId = activity.getId();

    if (canRunTask) {
      if (taskType == TaskType.decision) {
        InternalTaskResponse response = new InternalTaskResponse();
        response.setActivityId(taskExecution.getId());
        response.setStatus(TaskStatus.completed);
        this.endTask(response);
      } else if (taskType == TaskType.template) {
        controllerClient.submitTemplateTask(task, activityId, workflowName);
      } else if (taskType == TaskType.customtask) {
        controllerClient.submitCustomTask(task, activityId, workflowName);
      } else if (taskType == TaskType.approval) {
        throw new IllegalArgumentException("Approvals not implemented");
      }
    } else {
      InternalTaskResponse response = new InternalTaskResponse();
      response.setStatus(TaskStatus.skipped);
      response.setActivityId(taskExecution.getId());

      endTask(response);
    }
  }

  @Override
  public void endTask(InternalTaskResponse request) {
    String activityId = request.getActivityId();
    TaskExecutionEntity activity = taskActivityService.findById(activityId);


    ActivityEntity workflowActivity =
        this.activityService.findWorkflowActiivtyById(activity.getActivityId());

    WorkflowEntity workflow = workflowService.getWorkflow(workflowActivity.getWorkflowId());
    RevisionEntity revision =
        workflowVersionService.getWorkflowlWithId(workflowActivity.getWorkflowRevisionid());
    Task currentTask = getTask(activity, workflow);
    List<Task> tasks = this.createTaskList(revision, workflowActivity);
    
    long timeout = 105000;
    String storeId = workflow.getId();

    List<String> keys = new LinkedList<>();
    keys.add(storeId);

    boolean finishedAll = this.finishedAll(workflowActivity, tasks, currentTask);

    RetryTemplate retryTemplate = getRetryTemplate();
    System.out.println("Try to get lock");
    String tokenId = retryTemplate.execute(ctx -> {
      final String token = lock.acquire(keys, "locks", timeout);
      if (StringUtils.isEmpty(token)) {
        throw new LockNotAvailableException(
            String.format("Lock not available for keys: %s in store %s", keys, storeId));
      }
      return token;
    });

    System.out.println("Acquired lock: " + tokenId);

    workflowActivity = this.activityService.findWorkflowActiivtyById(activity.getActivityId());

    activity.setFlowTaskStatus(request.getStatus());
    long duration = new Date().getTime() - activity.getStartTime().getTime();
    activity.setDuration(duration);
    activity = taskActivityService.save(activity);

    executeNextStep(workflowActivity, tasks, currentTask, finishedAll);
    lock.release(keys, "locks", tokenId);
    System.out.println("Released lock");
  }

  private RetryTemplate getRetryTemplate() {
    RetryTemplate retryTemplate = new RetryTemplate();
    FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
    fixedBackOffPolicy.setBackOffPeriod(2000l);
    retryTemplate.setBackOffPolicy(fixedBackOffPolicy);

    SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
    retryPolicy.setMaxAttempts(100);
    retryTemplate.setRetryPolicy(retryPolicy);
    return retryTemplate;
  }

  private void finishWorkflow(ActivityEntity activity) {

    WorkflowEntity workflow = workflowService.getWorkflow(activity.getWorkflowId());


    this.controllerClient.terminateFlow(workflow.getId(), workflow.getName(), activity.getId());
    boolean workflowCompleted = dagUtility.validateWorkflow(activity);
    if (workflowCompleted) {
      activity.setStatus(TaskStatus.completed);
    } else {
      activity.setStatus(TaskStatus.failure);
    }
    final Date finishDate = new Date();
    final long duration = finishDate.getTime() - activity.getCreationDate().getTime();
    activity.setDuration(duration);


    this.activityService.saveWorkflowActivity(activity);

  }

  private void executeNextStep(ActivityEntity workflowActivity, List<Task> tasks, Task currentTask,
      boolean finishedAll) {
    System.out.println("Current Task: " + currentTask.getTaskName());

    List<Task> nextNodes = this.getTasksDependants(tasks, currentTask);
    for (Task next : nextNodes) {

      if (next.getTaskType() == TaskType.end) {

        if (finishedAll) {
          System.out.println("All finished");
          this.finishWorkflow(workflowActivity);
  
        }
        return;
      }

      boolean executeTask = canExecuteTask(workflowActivity, next, tasks);
      if (executeTask) {
        
        TaskExecutionEntity task = this.taskActivityService
            .findByTaskIdAndActiityId(next.getTaskId(), workflowActivity.getId());

        InternalTaskRequest taskRequest = new InternalTaskRequest();
        taskRequest.setActivityId(task.getId());
        flowClient.startTask(taskRequest);
      }
    }
  }

  private boolean finishedAll(ActivityEntity workflowActivity, List<Task> tasks, Task currentTask) {
    boolean finishedAll = true;

    List<Task> nextNodes = this.getTasksDependants(tasks, currentTask);
    for (Task next : nextNodes) {

      if (next.getTaskType() == TaskType.end) {
        List<String> deps = next.getDependencies();

        for (String dep : deps) {

          TaskExecutionEntity task =
              this.taskActivityService.findByTaskIdAndActiityId(dep, workflowActivity.getId());

          System.out.println("Examining: " + task.getTaskName() + " - " + task.getFlowTaskStatus());

          TaskStatus status = task.getFlowTaskStatus();
          if (status == TaskStatus.inProgress || status == TaskStatus.notstarted) {
            finishedAll = false;
          }
        }

      }
    }

    return finishedAll;
  }

  private boolean canExecuteTask(ActivityEntity workflowActivity, Task next, List<Task> tasks) {
    List<String> deps = next.getDependencies();
    for (String dep : deps) {
      TaskExecutionEntity task =
          this.taskActivityService.findByTaskIdAndActiityId(dep, workflowActivity.getId());
      TaskStatus status = task.getFlowTaskStatus();
      if (status == TaskStatus.inProgress || status == TaskStatus.notstarted) {
        return false;
      }
    }
    return true;
  }

  private List<Task> getTasksDependants(List<Task> tasks, Task currentTask) {
    List<Task> dep =
        tasks.stream().filter(c -> c.getDependencies().contains(currentTask.getTaskId()))
            .collect(Collectors.toList());
    return dep;
  }

  private Task getTask(TaskExecutionEntity taskActivity, WorkflowEntity workflow) {
    ActivityEntity activity =
        this.activityService.findWorkflowActiivtyById(taskActivity.getActivityId());
    RevisionEntity revision =
        workflowVersionService.getWorkflowlWithId(activity.getWorkflowRevisionid());
    List<Task> tasks = this.createTaskList(revision, activity);
    String taskId = taskActivity.getTaskId();
    final Task task =
        tasks.stream().filter(tsk -> taskId.equals(tsk.getTaskId())).findAny().orElse(null);

    return task;
  }

  private List<Task> createTaskList(RevisionEntity revisionEntity, ActivityEntity activity) { // NOSONAR

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

        TaskExecutionEntity task =
            taskActivityService.findByTaskIdAndActiityId(dagTask.getTaskId(), activity.getId());
        newTask.setTaskActivityId(task.getId());

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
}
