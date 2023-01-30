package io.boomerang.v4.service;

import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
import io.boomerang.v4.client.EngineClient;
import io.boomerang.v4.client.WorkflowRunResponsePage;
import io.boomerang.v4.model.ref.WorkflowRun;

@Service
public class WorkflowRunServiceImpl implements WorkflowRunService {

  private static final Logger LOGGER = LogManager.getLogger();

  @Autowired
  private EngineClient engineClient;

  @Autowired
  private FilterServiceV4 filterService;

  @Override
  public ResponseEntity<WorkflowRun> get(String workflowRunId, boolean withTasks) {
    if (workflowRunId == null || workflowRunId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_RUN_INVALID_REF);
    }

    List<String> workflowIdsList = filterService.getFilteredWorkflowIds(Optional.empty(), Optional.empty(), Optional.empty());
    if (!workflowIdsList.isEmpty() && workflowIdsList.contains(workflowRunId)) {
      WorkflowRun wfRun = engineClient.getWorkflowRun(workflowRunId, withTasks);
      return ResponseEntity.ok(wfRun);
    } else {
      //TODO: do we want to return invalid ref or unauthorized
      throw new BoomerangException(BoomerangError.WORKFLOW_RUN_INVALID_REF);
    }
  }

  @Override
  // TODO switch to WorkflowRun
  /*
   * Pass query onto EngineClient
   * 
   * No need to validate params as they are either defaulted or optional
   */
  public WorkflowRunResponsePage query(int page, int limit, Sort sort, Optional<List<String>> queryLabels,
      Optional<List<String>> queryStatus, Optional<List<String>> queryPhase) {
    List<String> workflowIdsList = filterService.getFilteredWorkflowIds(Optional.empty(), Optional.empty(), Optional.empty());
    LOGGER.debug("Query Ids: ", workflowIdsList);
    
    return engineClient.queryWorkflowRuns(page, limit, sort, queryLabels, queryStatus, queryPhase, Optional.of(workflowIdsList));
  }

//  @Override
//  public ResponseEntity<WorkflowRun> submit(String workflowId, Optional<Integer> version, boolean start,
//      Optional<WorkflowRunRequest> optRunRequest) {
//    if (workflowId == null || workflowId.isBlank()) {
//      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
//    }
//    final Optional<WorkflowEntity> optWorkflow = workflowRepository.findById(workflowId);
//    WorkflowEntity workflow = new WorkflowEntity();
//    if (optWorkflow.isPresent()) {
//      workflow = optWorkflow.get();
//    } else {
//      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
//    }
//    
//    Optional<WorkflowRevisionEntity> optWorkflowRevisionEntity;
//    if (version.isPresent()) {
//      optWorkflowRevisionEntity =
//          workflowRevisionRepository.findByWorkflowRefAndVersion(workflowId, version.get());
//      if (!optWorkflowRevisionEntity.isPresent()) {
//        throw new BoomerangException(BoomerangError.WORKFLOW_REVISION_NOT_FOUND);
//      }
//
//      LOGGER.debug("Workflow Revision: " + optWorkflowRevisionEntity.get().toString());
//    } else {
//      optWorkflowRevisionEntity =
//          workflowRevisionRepository.findByWorkflowRefAndLatestVersion(workflowId);
//    }
//    if (optWorkflowRevisionEntity.isPresent()) {
//      WorkflowRevisionEntity wfRevision = optWorkflowRevisionEntity.get();
//      final WorkflowRunEntity wfRunEntity = new WorkflowRunEntity();
//      wfRunEntity.setWorkflowRevisionRef(wfRevision.getId());
//      wfRunEntity.setWorkflowRef(wfRevision.getWorkflowRef());
//      wfRunEntity.setCreationDate(new Date());
//      wfRunEntity.setStatus(RunStatus.notstarted);
//      wfRunEntity.putLabels(workflow.getLabels());
//      wfRunEntity.setParams(ParameterUtil.paramSpecToRunParam(wfRevision.getParams()));
//      wfRunEntity.setWorkspaces(wfRevision.getWorkspaces());
//
//      // Add values from Run Request if Present
//      if (optRunRequest.isPresent()) {
//        logPayload(optRunRequest.get());
//        wfRunEntity.putLabels(optRunRequest.get().getLabels());
//        wfRunEntity.putAnnotations(optRunRequest.get().getAnnotations());
//        wfRunEntity.setParams(ParameterUtil.addUniqueParams(wfRunEntity.getParams(), optRunRequest.get().getParams()));
//        wfRunEntity.getWorkspaces().addAll(optRunRequest.get().getWorkspaces());
//      }
//
//      // TODO: add trigger and set initiatedBy
//      // workflowRun.setTrigger(null);
//      // if (!trigger.isPresent() || "manual".equals(trigger.get())) {
//      // final UserEntity userEntity = userIdentityService.getCurrentUser();
//      // activity.setInitiatedById(userEntity.getId());
//      // }
//
//      workflowRunRepository.save(wfRunEntity);
//
//      // TODO: Check if Workflow is active and triggers enabled
//      // Throws Execution exception if not able to
//      // workflowService.canExecuteWorkflow(workflowId);
//
//      workflowExecutionClient.queueRevision(workflowExecutionService, wfRunEntity);
//
//      if (start) {
//        return this.start(wfRunEntity.getId(), Optional.empty());
//      } else {
//        final WorkflowRun response = new WorkflowRun(wfRunEntity);
//        response.setTasks(getTaskRuns(wfRunEntity.getId()));
//        return ResponseEntity.ok(response);
//      }
//    } else {
//      throw new BoomerangException(BoomerangError.WORKFLOW_RUN_INVALID_REQ);
//    }
//  }
//
//  @Override
//  public ResponseEntity<WorkflowRun> start(String workflowRunId,
//      Optional<WorkflowRunRequest> optRunRequest) {
//    if (workflowRunId == null || workflowRunId.isBlank()) {
//      throw new BoomerangException(BoomerangError.WORKFLOW_RUN_INVALID_REF);
//    }
//    final Optional<WorkflowRunEntity> optWfRunEntity =
//        workflowRunRepository.findById(workflowRunId);
//    if (optWfRunEntity.isPresent()) {
//      WorkflowRunEntity wfRunEntity = optWfRunEntity.get();
//      // Add values from Run Request
//      if (optRunRequest.isPresent()) {
//        logPayload(optRunRequest.get());
//        wfRunEntity.putLabels(optRunRequest.get().getLabels());
//        wfRunEntity.putAnnotations(optRunRequest.get().getAnnotations());
//        wfRunEntity.setParams(ParameterUtil.addUniqueParams(wfRunEntity.getParams(), optRunRequest.get().getParams()));
//        wfRunEntity.getWorkspaces().addAll(optRunRequest.get().getWorkspaces());
//        workflowRunRepository.save(wfRunEntity);
//      }
//
//      workflowExecutionClient.startRevision(workflowExecutionService, wfRunEntity);
//      
//      //Retrieve the refreshed status
//      WorkflowRunEntity updatedWfRunEntity = 
//          workflowRunRepository.findById(workflowRunId).get();
//      final WorkflowRun response = new WorkflowRun(updatedWfRunEntity);
//      response.setTasks(getTaskRuns(workflowRunId));
//      return ResponseEntity.ok(response);
//    } else {
//      throw new BoomerangException(BoomerangError.WORKFLOW_RUN_INVALID_REF);
//    }
//  }
//
//  @Override
//  public ResponseEntity<WorkflowRun> end(String workflowRunId) {
//    if (workflowRunId == null || workflowRunId.isBlank()) {
//      throw new BoomerangException(BoomerangError.WORKFLOW_RUN_INVALID_REF);
//    }
//    final Optional<WorkflowRunEntity> optWfRunEntity =
//        workflowRunRepository.findById(workflowRunId);
//    if (optWfRunEntity.isPresent()) {
//      WorkflowRunEntity wfRunEntity = optWfRunEntity.get();
//
//      workflowExecutionClient.endRevision(workflowExecutionService, wfRunEntity);
//      final WorkflowRun response = new WorkflowRun(wfRunEntity);
//      response.setTasks(getTaskRuns(wfRunEntity.getId()));
//      return ResponseEntity.ok(response);
//    } else {
//      throw new BoomerangException(BoomerangError.WORKFLOW_RUN_INVALID_REF);
//    }
//  }

//  private List<TaskRun> getTaskRuns(String workflowRunId) {
//    List<TaskRunEntity> taskRunEntities = taskRunRepository.findByWorkflowRunRef(workflowRunId);
//    return taskRunEntities.stream().map(t -> new TaskRun(t)).collect(Collectors.toList());
//
//
//    //
//    // TODO: Update the following or make sure they are set on the run at execution end task time.
//    // if (TaskType.approval.equals(run.getTaskType())
//    // || TaskType.manual.equals(run.getTaskType())) {
//    // Action approval = approvalService.getApprovalByTaskActivits(task.getId());
//    // response.setApproval(approval);
//    // } else if (TaskType.runworkflow == task.getTaskType()
//    // && task.getRunWorkflowActivityId() != null) {
//    //
//    // String runWorkflowActivityId = task.getRunWorkflowActivityId();
//    // ActivityEntity activity =
//    // this.flowActivityService.findWorkflowActivtyById(runWorkflowActivityId);
//    // if (activity != null) {
//    // response.setRunWorkflowActivityStatus(activity.getStatus());
//    // }
//    // } else if (TaskType.eventwait == task.getTaskType()) {
//    // List<TaskOutputResult> results = new LinkedList<>();
//    // TaskOutputResult result = new TaskOutputResult();
//    // result.setName("eventPayload");
//    // result.setDescription("Payload that was received with the Wait For Event");
//    // if (task.getOutputs() != null) {
//    // String json = task.getOutputs().get("eventPayload");
//    // result.setValue(json);
//    // }
//    // results.add(result);
//    // response.setResults(results);
//    // } else if (TaskType.template == task.getTaskType()
//    // || TaskType.customtask == task.getTaskType() || TaskType.script == task.getTaskType()) {
//    // List<TaskOutputResult> results = new LinkedList<>();
//    // setupTaskOutputResults(task, response, results);
//    //
//    // }
//  }
//
//  private void logPayload(WorkflowRunRequest request) {
//    try {
//      ObjectMapper objectMapper = new ObjectMapper();
//      String payload = objectMapper.writeValueAsString(request);
//      LOGGER.info("Received Request Payload: ");
//      LOGGER.info(payload);
//    } catch (JsonProcessingException e) {
//      LOGGER.error(e.getStackTrace());
//    }
//  }
}
