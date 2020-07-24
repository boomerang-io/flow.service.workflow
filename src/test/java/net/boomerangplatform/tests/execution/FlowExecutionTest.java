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
import net.boomerangplatform.mongo.entity.FlowWorkflowActivityEntity;
import net.boomerangplatform.mongo.entity.FlowWorkflowEntity;
import net.boomerangplatform.mongo.entity.FlowWorkflowRevisionEntity;
import net.boomerangplatform.mongo.model.FlowTaskStatus;
import net.boomerangplatform.mongo.model.FlowTriggerEnum;
import net.boomerangplatform.mongo.service.FlowWorkflowVersionService;
import net.boomerangplatform.service.FlowExecutionService;
import net.boomerangplatform.service.crud.FlowActivityService;
import net.boomerangplatform.tests.FlowTests;

public abstract class FlowExecutionTest extends FlowTests {

  @Autowired
  protected WorkflowController controller;

  @Autowired
  protected FlowActivityService activityService;

  @Autowired
  protected FlowWorkflowVersionService flowRevisionService;

  @Autowired
  protected FlowExecutionService flowExecutionService;

  @Autowired
  protected ExecutionController executionController;

  public void testInvalidExecutionFlow(String workflowId)
      throws InterruptedException, ExecutionException {
    /* Retrieve workflow and revision. */
    FlowWorkflowEntity workflow = controller.getWorkflowWithId(workflowId);
    FlowWorkflowRevisionEntity latestRevision =
        this.flowRevisionService.getLatestWorkflowVersion(workflow.getId());
    assertNotNull(workflow);
    /* Prepare a request. */
    FlowExecutionRequest flowExecutionRequest = new FlowExecutionRequest();
    final FlowWorkflowActivityEntity activity = activityService.createFlowActivity(
        latestRevision.getId(), Optional.of(FlowTriggerEnum.manual), flowExecutionRequest);
    assertNotNull(activity);

    CompletableFuture<Boolean> executionFuture =
        flowExecutionService.executeWorkflowVersion(latestRevision.getId(), activity.getId());

  }

  public FlowWorkflowActivityEntity testFailedExecuteFlow(String workflowId)
      throws InterruptedException, ExecutionException {
    return this.testSuccessExecuteFlow(workflowId, new FlowExecutionRequest(),
        FlowTaskStatus.failure);
  }

  public FlowWorkflowActivityEntity testSuccessExecuteFlow(String workflowId)
      throws InterruptedException, ExecutionException {
    return this.testSuccessExecuteFlow(workflowId, new FlowExecutionRequest(),
        FlowTaskStatus.completed);
  }

  public FlowWorkflowActivityEntity testSuccessExecuteFlow(String workflowId,
      FlowExecutionRequest flowExecutionRequest, FlowTaskStatus matchStatus)
      throws InterruptedException, ExecutionException {
    FlowWorkflowEntity workflow = controller.getWorkflowWithId(workflowId);
    FlowWorkflowRevisionEntity latestRevision =
        this.flowRevisionService.getLatestWorkflowVersion(workflow.getId());

    final FlowWorkflowActivityEntity activity = activityService.createFlowActivity(
        latestRevision.getId(), Optional.of(FlowTriggerEnum.manual), flowExecutionRequest);

    CompletableFuture<Boolean> executionFuture =
        flowExecutionService.executeWorkflowVersion(latestRevision.getId(), activity.getId());
    Boolean result = executionFuture.get();

    assertNotNull(result);
    assertTrue(result.booleanValue());

    final FlowWorkflowActivityEntity updatedActivity =
        activityService.findWorkflowActivity(activity.getId());

    assertEquals(matchStatus, updatedActivity.getStatus());

    return updatedActivity;
  }

}
