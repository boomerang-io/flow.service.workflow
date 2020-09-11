package net.boomerangplatform.tests.execution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import net.boomerangplatform.controller.ExecutionController;
import net.boomerangplatform.controller.WorkflowController;
import net.boomerangplatform.model.FlowExecutionRequest;
import net.boomerangplatform.mongo.entity.ActivityEntity;
import net.boomerangplatform.mongo.entity.WorkflowEntity;
import net.boomerangplatform.mongo.entity.RevisionEntity;
import net.boomerangplatform.mongo.model.TaskStatus;
import net.boomerangplatform.mongo.model.FlowTriggerEnum;
import net.boomerangplatform.mongo.service.RevisionService;
import net.boomerangplatform.service.FlowExecutionService;
import net.boomerangplatform.service.crud.FlowActivityService;
import net.boomerangplatform.tests.FlowTests;

public abstract class FlowExecutionTest extends FlowTests {

  @Autowired
  protected WorkflowController controller;

  @Autowired
  protected FlowActivityService activityService;

  @Autowired
  protected RevisionService flowRevisionService;

  @Autowired
  protected FlowExecutionService flowExecutionService;

  @Autowired
  protected ExecutionController executionController;

  public void testInvalidExecutionFlow(String workflowId)
      throws InterruptedException, ExecutionException {
    /* Retrieve workflow and revision. */
    WorkflowEntity workflow = controller.getWorkflowWithId(workflowId);
    RevisionEntity latestRevision =
        this.flowRevisionService.getLatestWorkflowVersion(workflow.getId());
    assertNotNull(workflow);
    /* Prepare a request. */
    FlowExecutionRequest flowExecutionRequest = new FlowExecutionRequest();
    final ActivityEntity activity = activityService.createFlowActivity(
        latestRevision.getId(), Optional.of(FlowTriggerEnum.manual), flowExecutionRequest);
    assertNotNull(activity);

    CompletableFuture<Boolean> executionFuture =
        flowExecutionService.executeWorkflowVersion(latestRevision.getId(), activity.getId());

  }

  public ActivityEntity testFailedExecuteFlow(String workflowId)
      throws InterruptedException, ExecutionException {
    return this.testSuccessExecuteFlow(workflowId, new FlowExecutionRequest(),
        TaskStatus.failure);
  }

  public ActivityEntity testSuccessExecuteFlow(String workflowId)
      throws InterruptedException, ExecutionException {
    return this.testSuccessExecuteFlow(workflowId, new FlowExecutionRequest(),
        TaskStatus.completed);
  }

  public ActivityEntity testSuccessExecuteFlow(String workflowId,
      FlowExecutionRequest flowExecutionRequest, TaskStatus matchStatus)
      throws InterruptedException, ExecutionException {
    WorkflowEntity workflow = controller.getWorkflowWithId(workflowId);
    RevisionEntity latestRevision =
        this.flowRevisionService.getLatestWorkflowVersion(workflow.getId());

    final ActivityEntity activity = activityService.createFlowActivity(
        latestRevision.getId(), Optional.of(FlowTriggerEnum.manual), flowExecutionRequest);

    CompletableFuture<Boolean> executionFuture =
        flowExecutionService.executeWorkflowVersion(latestRevision.getId(), activity.getId());
    Boolean result = executionFuture.get();

    assertNotNull(result);
    assertTrue(result.booleanValue());

    final ActivityEntity updatedActivity =
        activityService.findWorkflowActivity(activity.getId());

    assertEquals(matchStatus, updatedActivity.getStatus());

    return updatedActivity;
  }

}
