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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.github.alturkovic.lock.Lock;
import com.github.alturkovic.lock.exception.LockNotAvailableException;
import net.boomerangplatform.model.ApprovalStatus;
import net.boomerangplatform.model.RequestFlowExecution;
import net.boomerangplatform.model.Task;
import net.boomerangplatform.mongo.entity.ActivityEntity;
import net.boomerangplatform.mongo.entity.ApprovalEntity;
import net.boomerangplatform.mongo.entity.FlowTaskTemplateEntity;
import net.boomerangplatform.mongo.entity.RevisionEntity;
import net.boomerangplatform.mongo.entity.TaskExecutionEntity;
import net.boomerangplatform.mongo.entity.WorkflowEntity;
import net.boomerangplatform.mongo.model.CoreProperty;
import net.boomerangplatform.mongo.model.Dag;
import net.boomerangplatform.mongo.model.ManualType;
import net.boomerangplatform.mongo.model.Revision;
import net.boomerangplatform.mongo.model.TaskStatus;
import net.boomerangplatform.mongo.model.TaskType;
import net.boomerangplatform.mongo.model.internal.InternalTaskRequest;
import net.boomerangplatform.mongo.model.internal.InternalTaskResponse;
import net.boomerangplatform.mongo.model.next.DAGTask;
import net.boomerangplatform.mongo.model.next.Dependency;
import net.boomerangplatform.mongo.service.ActivityTaskService;
import net.boomerangplatform.mongo.service.ApprovalService;
import net.boomerangplatform.mongo.service.FlowTaskTemplateService;
import net.boomerangplatform.mongo.service.FlowWorkflowActivityService;
import net.boomerangplatform.mongo.service.FlowWorkflowService;
import net.boomerangplatform.mongo.service.RevisionService;
import net.boomerangplatform.service.PropertyManager;
import net.boomerangplatform.service.runner.misc.ControllerClient;

@Service
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
  private DAGUtility dagUtility;

  @Autowired
  private Lock lock;

  @Autowired
  private ApprovalService approvalService;

  @Autowired
  private PropertyManager propertyManager;

  @Autowired
  private LockManager lockManager;

  private static final Logger LOGGER = LogManager.getLogger();

  private TaskClient flowClient;

  @Autowired
  public TaskServiceImpl(@Lazy TaskClient flowClient) {
    this.flowClient = flowClient;
  }


  @Override
  @Async("flowAsyncExecutor")
  public void createTask(InternalTaskRequest request) {

    String taskId = request.getActivityId();
    LOGGER.debug("[{}] Recieved creating task request", taskId);

    TaskExecutionEntity taskExecution = taskActivityService.findById(taskId);
    ActivityEntity activity =
        activityService.findWorkflowActivtyById(taskExecution.getActivityId());

    if (activity.getStatus() == TaskStatus.cancelled) {
      LOGGER.error("[{}] Workflow has been marked as cancelled, not starting task.",
          activity.getId());
      return;
    }

    WorkflowEntity workflow = workflowService.getWorkflow(activity.getWorkflowId());
    String workflowName = workflow.getName();

    Task task = getTask(taskExecution);

    if (task == null || taskExecution.getFlowTaskStatus() != TaskStatus.notstarted) {
      LOGGER.debug("Task is null or hasn't started yet");
      return;
    }

    TaskType taskType = task.getTaskType();
    taskExecution.setStartTime(new Date());
    taskExecution.setFlowTaskStatus(TaskStatus.inProgress);
    taskExecution = taskActivityService.save(taskExecution);

    boolean canRunTask = dagUtility.canCompleteTask(activity, task.getTaskId());

    String activityId = activity.getId();

    LOGGER.debug("[{}] Examining task type: {}", taskId, taskType);

    if (canRunTask) {
      LOGGER.debug("[{}] Can run task? {}", taskId, canRunTask);

      if (taskType == TaskType.decision) {
        InternalTaskResponse response = new InternalTaskResponse();
        response.setActivityId(taskExecution.getId());
        response.setStatus(TaskStatus.completed);
        processDecision(task, activity.getId());
        this.endTask(response);
      } else if (taskType == TaskType.template || taskType == TaskType.script) {
        List<CoreProperty> labels = workflow.getLabels();
        controllerClient.submitTemplateTask(task, activityId, workflowName, labels);
      } else if (taskType == TaskType.customtask) {
        List<CoreProperty> labels = workflow.getLabels();
        controllerClient.submitCustomTask(task, activityId, workflowName, labels);
      } else if (taskType == TaskType.acquirelock) {
        createLock(task, activity);
      } else if (taskType == TaskType.releaselock) {
        releaseLock(task, activity);
      } else if (taskType == TaskType.runworkflow) {
        this.runWorkflow(task, activity);
      } else if (taskType == TaskType.setwfstatus) {
        saveWorkflowStatus(task, activity);
        InternalTaskResponse response = new InternalTaskResponse();
        response.setActivityId(taskExecution.getId());
        response.setStatus(TaskStatus.completed);
        this.endTask(response);
      } else if (taskType == TaskType.setwfproperty) {
        saveWorkflowProperty(task, activity);
        InternalTaskResponse response = new InternalTaskResponse();
        response.setActivityId(taskExecution.getId());
        response.setStatus(TaskStatus.completed);
        this.endTask(response);
      } else if (taskType == TaskType.approval) {
        createApprovalNotification(taskExecution, activity, workflow, ManualType.approval);
      } else if (taskType == TaskType.manual) {
        createApprovalNotification(taskExecution, activity, workflow, ManualType.task);
      } else if (taskType == TaskType.eventwait) {
        createWaitForEventTask(taskExecution);
      }
    } else {
      LOGGER.debug("[{}] Skipping task", taskId);
      InternalTaskResponse response = new InternalTaskResponse();
      response.setStatus(TaskStatus.skipped);
      response.setActivityId(taskExecution.getId());

      endTask(response);
    }
  }

  private void saveWorkflowStatus(Task task, ActivityEntity activity) {
    String status = task.getInputs().get("status");
    if (status.isEmpty() || status.isBlank()) {
      TaskStatus taskStatus = TaskStatus.valueOf(status);
      activity.setStatusOverride(taskStatus);
      this.activityService.saveWorkflowActivity(activity);
    }
  }

  private void processDecision(Task task, String activityId) {
    String decisionValue = task.getDecisionValue();
    ControllerRequestProperties properties =
        propertyManager.buildRequestPropertyLayering(task, activityId, task.getWorkflowId());
    String value = decisionValue;
    value = propertyManager.replaceValueWithProperty(value, activityId, properties);
    TaskExecutionEntity taskExecution = taskActivityService.findById(task.getTaskActivityId());
    taskExecution.setSwitchValue(value);
    taskActivityService.save(taskExecution);
  }

  private void releaseLock(Task task, ActivityEntity activity) {

    LOGGER.debug("[{}] Releasing lock: ", task.getTaskActivityId());

    lockManager.releaseLock(task, activity.getId());
    InternalTaskResponse response = new InternalTaskResponse();
    response.setActivityId(task.getTaskActivityId());
    response.setStatus(TaskStatus.completed);
    this.endTask(response);
  }

  private void runWorkflow(Task task, ActivityEntity activity) {

    if (task.getInputs() != null) {
      RequestFlowExecution request = new RequestFlowExecution();
      request.setWorkflowId(task.getInputs().get("workflowId"));
      Map<String, String> properties = new HashMap<>();
      for (Map.Entry<String, String> entry : task.getInputs().entrySet()) {
        if (!"workflowId".equals(entry.getKey())) {
          properties.put(entry.getKey(), entry.getValue());
        }
      }

      request.setProperties(properties);
      String workflowActivityId = flowClient.submitWebhookEvent(request);
      if (workflowActivityId != null) {
        TaskExecutionEntity taskExecution = taskActivityService.findById(task.getTaskActivityId());
        taskExecution.setRunWorkflowActivityId(workflowActivityId);
        taskExecution.setRunWorkflowId(request.getWorkflowId());
        taskActivityService.save(taskExecution);
      }
    }

    InternalTaskResponse response = new InternalTaskResponse();
    response.setActivityId(task.getTaskActivityId());
    response.setStatus(TaskStatus.completed);
    this.endTask(response);
  }

  private void createLock(Task task, ActivityEntity activity) {

    LOGGER.debug("[{}] Creating lock: ", task.getTaskActivityId());

    lockManager.acquireLock(task, activity.getId());

    LOGGER.debug("[{}] Finishing lock: ", task.getTaskActivityId());

    InternalTaskResponse response = new InternalTaskResponse();
    response.setActivityId(task.getTaskActivityId());
    response.setStatus(TaskStatus.completed);
    this.endTask(response);
  }

  private void createWaitForEventTask(TaskExecutionEntity taskExecution) {

    LOGGER.debug("[{}] Creating wait for event task", taskExecution.getActivityId());

    taskExecution.setFlowTaskStatus(TaskStatus.waiting);
    taskActivityService.save(taskExecution);

    if (taskExecution.isPreApproved()) {
      InternalTaskResponse response = new InternalTaskResponse();
      response.setActivityId(taskExecution.getId());
      response.setStatus(TaskStatus.completed);
      this.endTask(response);
    }
  }

  private void createApprovalNotification(TaskExecutionEntity taskExecution,
      ActivityEntity activity, WorkflowEntity workflow, ManualType type) {
    taskExecution.setFlowTaskStatus(TaskStatus.waiting);
    taskExecution = taskActivityService.save(taskExecution);
    ApprovalEntity approval = new ApprovalEntity();
    approval.setTaskActivityId(taskExecution.getId());
    approval.setActivityId(activity.getId());
    approval.setWorkflowId(workflow.getId());
    approval.setTeamId(workflow.getFlowTeamId());
    approval.setStatus(ApprovalStatus.submitted);

    approvalService.save(approval);

    activity.setAwaitingApproval(true);
    this.activityService.saveWorkflowActivity(activity);
  }

  private void saveWorkflowProperty(Task task, ActivityEntity activity) {
    if (activity.getOutputProperties() == null) {
      activity.setOutputProperties(new LinkedList<>());
    }

    List<CoreProperty> outputProperties = activity.getOutputProperties();
    String input = task.getInputs().get("value");
    String output = task.getInputs().get("output");
    CoreProperty outputProperty = new CoreProperty();
    outputProperty.setKey(output);

    ControllerRequestProperties requestProperties = propertyManager
        .buildRequestPropertyLayering(task, activity.getId(), activity.getWorkflowId());
    String outputValue =
        propertyManager.replaceValueWithProperty(input, activity.getId(), requestProperties);

    outputProperty.setValue(outputValue);
    outputProperties.add(outputProperty);
    this.activityService.saveWorkflowActivity(activity);
  }

  @Override
  @Async("flowAsyncExecutor")
  public void endTask(InternalTaskResponse request) {

    String activityId = request.getActivityId();
    LOGGER.info("[{}] Recieved end task request", activityId);
    TaskExecutionEntity activity = taskActivityService.findById(activityId);

    ActivityEntity workflowActivity =
        this.activityService.findWorkflowActivtyById(activity.getActivityId());

    if (workflowActivity.getStatus() == TaskStatus.cancelled) {
      LOGGER.error("[{}] Workflow has been marked as cancelled, not ending task", activityId);
      activity.setFlowTaskStatus(TaskStatus.cancelled);
      long duration = new Date().getTime() - activity.getStartTime().getTime();
      activity.setDuration(duration);
      taskActivityService.save(activity);
      return;
    }

    RevisionEntity revision =
        workflowVersionService.getWorkflowlWithId(workflowActivity.getWorkflowRevisionid());
    Task currentTask = getTask(activity);
    List<Task> tasks = this.createTaskList(revision, workflowActivity);

    String storeId = workflowActivity.getId();

    List<String> keys = new LinkedList<>();
    keys.add(storeId);

    workflowActivity = this.activityService.findWorkflowActivtyById(activity.getActivityId());


    activity.setFlowTaskStatus(request.getStatus());
    long duration = new Date().getTime() - activity.getStartTime().getTime();
    activity.setDuration(duration);


    if (request.getOutputProperties() != null && !request.getOutputProperties().isEmpty()) {
      activity.setOutputs(request.getOutputProperties());
    }

    activity = taskActivityService.save(activity);

    boolean finishedAll = this.finishedAll(workflowActivity, tasks, currentTask);

    LOGGER.debug("[{}] Finished all previous tasks? {}", activityId, finishedAll);


    LOGGER.debug("[{}] Attempting to get lock", activityId);
    String tokenId = getLock(storeId, keys, 105000);
    LOGGER.debug("[{}] Obtained lock", activityId);

    workflowActivity = this.activityService.findWorkflowActivtyById(activity.getActivityId());
    updatePendingAprovalStatus(workflowActivity);

    activity.setFlowTaskStatus(request.getStatus());
    executeNextStep(workflowActivity, tasks, currentTask, finishedAll);
    lock.release(keys, "locks", tokenId);
    LOGGER.debug("[{}] Released lock", activityId);
  }

  private void updatePendingAprovalStatus(ActivityEntity workflowActivity) {
    long count = approvalService.getApprovalCountForActivity(workflowActivity.getId(),
        ApprovalStatus.submitted);
    boolean existingApprovals = (count > 0);
    workflowActivity.setAwaitingApproval(existingApprovals);
    this.activityService.saveWorkflowActivity(workflowActivity);
  }

  private String getLock(String storeId, List<String> keys, long timeout) {
    RetryTemplate retryTemplate = getRetryTemplate();
    return retryTemplate.execute(ctx -> {
      final String token = lock.acquire(keys, "locks", timeout);
      if (StringUtils.isEmpty(token)) {
        throw new LockNotAvailableException(
            String.format("Lock not available for keys: %s in store %s", keys, storeId));
      }
      return token;
    });
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
    
    if (activity.getStatusOverride() != null) {
      activity.setStatus(activity.getStatusOverride());
    } else {
      if (workflowCompleted) {
        activity.setStatus(TaskStatus.completed);
      } else {
        activity.setStatus(TaskStatus.failure);
      }
    }

    final Date finishDate = new Date();
    final long duration = finishDate.getTime() - activity.getCreationDate().getTime();
    activity.setDuration(duration);


    this.activityService.saveWorkflowActivity(activity);

  }

  private void executeNextStep(ActivityEntity workflowActivity, List<Task> tasks, Task currentTask,
      boolean finishedAll) {
    LOGGER.debug("[{}] Looking at next tasks", workflowActivity.getId());
    LOGGER.debug("Testing at next tasks");
    List<Task> nextNodes = this.getTasksDependants(tasks, currentTask);
    LOGGER.debug("Testing at next tasks: {}", nextNodes.size());

    for (Task next : nextNodes) {

      if (next.getTaskType() == TaskType.end) {
        if (finishedAll) {
          LOGGER.debug("FINISHED ALL");
          this.finishWorkflow(workflowActivity);
          return;
        }
        continue;
      }

      boolean executeTask = canExecuteTask(workflowActivity, next);
      LOGGER.debug("[{}] Task: {}", workflowActivity.getId(), next.getTaskName());


      if (executeTask) {
        TaskExecutionEntity task = this.taskActivityService
            .findByTaskIdAndActivityId(next.getTaskId(), workflowActivity.getId());
        if (task == null) {
          LOGGER.debug("Reached node which should not be executed.");
        } else {
          InternalTaskRequest taskRequest = new InternalTaskRequest();
          taskRequest.setActivityId(task.getId());
          flowClient.startTask(taskRequest);
        }
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
              this.taskActivityService.findByTaskIdAndActivityId(dep, workflowActivity.getId());
          if (task == null) {
            continue;
          }

          TaskStatus status = task.getFlowTaskStatus();
          if (status == TaskStatus.inProgress || status == TaskStatus.notstarted
              || status == TaskStatus.waiting) {
            finishedAll = false;
          }
        }
      }
    }

    return finishedAll;
  }

  private boolean canExecuteTask(ActivityEntity workflowActivity, Task next) {
    List<String> deps = next.getDependencies();
    for (String dep : deps) {
      TaskExecutionEntity task =
          taskActivityService.findByTaskIdAndActivityId(dep, workflowActivity.getId());
      if (task != null) {
        TaskStatus status = task.getFlowTaskStatus();
        if (status == TaskStatus.inProgress || status == TaskStatus.notstarted
            || status == TaskStatus.waiting) {
          return false;
        }
      }
    }
    return true;
  }

  private List<Task> getTasksDependants(List<Task> tasks, Task currentTask) {
    return tasks.stream().filter(c -> c.getDependencies().contains(currentTask.getTaskId()))
        .collect(Collectors.toList());
  }

  private Task getTask(TaskExecutionEntity taskActivity) {
    ActivityEntity activity = activityService.findWorkflowActivtyById(taskActivity.getActivityId());
    RevisionEntity revision =
        workflowVersionService.getWorkflowlWithId(activity.getWorkflowRevisionid());
    List<Task> tasks = createTaskList(revision, activity);
    String taskId = taskActivity.getTaskId();
    return tasks.stream().filter(tsk -> taskId.equals(tsk.getTaskId())).findAny().orElse(null);
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

      if (dagTask.getType() == TaskType.script || dagTask.getType() == TaskType.template
          || dagTask.getType() == TaskType.customtask) {

        TaskExecutionEntity task =
            taskActivityService.findByTaskIdAndActivityId(dagTask.getTaskId(), activity.getId());
        if (task != null) {
          newTask.setTaskActivityId(task.getId());
        }

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
          for (CoreProperty property : dagTask.getProperties()) {
            properties.put(property.getKey(), property.getValue());
          }
        }
        newTask.setInputs(properties);
        if (newTask.getResults() == null) {
          newTask.setResults(dagTask.getResults());
        }
      } else if (dagTask.getType() == TaskType.decision) {
        TaskExecutionEntity task =
            taskActivityService.findByTaskIdAndActivityId(dagTask.getTaskId(), activity.getId());
        if (task != null) {
          newTask.setTaskActivityId(task.getId());
        }


        newTask.setDecisionValue(dagTask.getDecisionValue());
      } else if (dagTask.getType() == TaskType.runworkflow
          || dagTask.getType() == TaskType.setwfproperty
          || dagTask.getType() == TaskType.setwfstatus
          || dagTask.getType() == TaskType.acquirelock
          || dagTask.getType() == TaskType.releaselock) {

        TaskExecutionEntity task =
            taskActivityService.findByTaskIdAndActivityId(dagTask.getTaskId(), activity.getId());
        if (task != null) {
          newTask.setTaskActivityId(task.getId());
        }

        Map<String, String> properties = new HashMap<>();
        if (dagTask.getProperties() != null) {
          for (CoreProperty property : dagTask.getProperties()) {
            properties.put(property.getKey(), property.getValue());
          }
        }
        newTask.setInputs(properties);
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

  @Override
  public List<String> updateTaskActivityForTopic(String activityId, String topic) {

    List<String> ids = new LinkedList<>();

    LOGGER.info("[{}] Fidning task actiivty id based on topic.", activityId);
    ActivityEntity activity = activityService.findWorkflowActivtyById(activityId);
    RevisionEntity revision =
        workflowVersionService.getWorkflowlWithId(activity.getWorkflowRevisionid());


    List<DAGTask> tasks = revision.getDag().getTasks();
    for (DAGTask task : tasks) {
      if (TaskType.eventwait.equals(task.getType())) {
        List<CoreProperty> coreProperties = task.getProperties();
        if (coreProperties != null) {
          CoreProperty coreProperty = coreProperties.stream()
              .filter(c -> "topic".contains(c.getKey())).findAny().orElse(null);

          if (coreProperty != null && topic.equals(coreProperty.getValue())) {

            String text = coreProperty.getValue();
            ControllerRequestProperties properties = propertyManager
                .buildRequestPropertyLayering(null, activityId, activity.getWorkflowId());
            text = propertyManager.replaceValueWithProperty(text, activityId, properties);

            String taskId = task.getTaskId();
            TaskExecutionEntity taskExecution =
                this.taskActivityService.findByTaskIdAndActivityId(taskId, activityId);
            if (taskExecution != null) {
              LOGGER.info("[{}] Found task id: {} ", activityId, taskExecution.getId());
              taskExecution.setPreApproved(true);
              this.taskActivityService.save(taskExecution);

              ids.add(taskExecution.getId());
            }
          }
        }
      }
    }
    LOGGER.info("[{}] No task activity ids found for topic: {}", activityId, topic);
    return ids;
  }

  @Override
  @Async("flowAsyncExecutor")
  public void submitActivity(String taskActivityId, String taskStatus,
      Map<String, String> outputProperties) {

    LOGGER.info("submitActivity: {}", taskStatus);

    TaskStatus status = TaskStatus.completed;
    if ("success".equals(taskStatus)) {
      status = TaskStatus.completed;
    } else if ("failure".equals(taskStatus)) {
      status = TaskStatus.failure;
    }

    LOGGER.info("Submit Activity (Task Status): {}", status.toString());


    TaskExecutionEntity taskExecution = this.taskActivityService.findById(taskActivityId);
    if (taskExecution != null && !taskExecution.getFlowTaskStatus().equals(TaskStatus.notstarted)) {
      InternalTaskResponse request = new InternalTaskResponse();
      request.setActivityId(taskActivityId);
      request.setStatus(status);

      if (outputProperties != null) {
        request.setOutputProperties(outputProperties);
      }

      endTask(request);
    }
  }
}
