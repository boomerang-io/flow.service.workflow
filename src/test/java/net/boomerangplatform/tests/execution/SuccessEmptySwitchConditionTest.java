

package net.boomerangplatform.tests.execution;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.client.ExpectedCount.manyTimes;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import net.boomerangplatform.Application;
import net.boomerangplatform.MongoConfig;
import net.boomerangplatform.model.FlowExecutionRequest;
import net.boomerangplatform.mongo.entity.FlowWorkflowActivityEntity;
import net.boomerangplatform.mongo.entity.FlowWorkflowEntity;
import net.boomerangplatform.mongo.entity.FlowWorkflowRevisionEntity;
import net.boomerangplatform.mongo.model.FlowTaskStatus;
import net.boomerangplatform.mongo.model.FlowTriggerEnum;
import net.boomerangplatform.service.crud.FlowActivityService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {Application.class, MongoConfig.class})
@SpringBootTest
@ActiveProfiles("local")
public class SuccessEmptySwitchConditionTest extends FlowExecutionTest {


  @Autowired
  protected FlowActivityService activityService;

  @Test
  public void testExecuteFlow() throws InterruptedException, ExecutionException {

    FlowExecutionRequest request = new FlowExecutionRequest();

    FlowWorkflowEntity workflow = controller.getWorkflowWithId("5d800606e5959c00075c5b9c");
    FlowWorkflowRevisionEntity latestRevision =
        this.flowRevisionService.getLatestWorkflowVersion(workflow.getId());

    final FlowWorkflowActivityEntity activity = activityService
        .createFlowActivity(latestRevision.getId(), Optional.of(FlowTriggerEnum.manual), request);

    CompletableFuture<Boolean> executionFuture =
        flowExecutionService.executeWorkflowVersion(latestRevision.getId(), activity.getId());
    Boolean result = executionFuture.get();

    assertNotNull(result);
    assertTrue(result.booleanValue());


    final FlowWorkflowActivityEntity updatedActivity =
        activityService.findWorkflowActivity(activity.getId());

    assertEquals(FlowTaskStatus.completed, updatedActivity.getStatus());

  }

  @Override
  @Before
  public void setUp() throws IOException {
    super.setUp();
    mockServer = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();
    mockServer
        .expect(manyTimes(), requestTo(containsString("http://localhost:8084/launchpad/users")))
        .andExpect(method(HttpMethod.GET)).andRespond(
            withSuccess(getMockFile("mock/launchpad/users.json"), MediaType.APPLICATION_JSON));

    mockServer.expect(times(1), requestTo(containsString("controller/workflow/create")))
        .andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.OK));
    mockServer.expect(times(2), requestTo(containsString("controller/task/execute")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

    mockServer.expect(times(1), requestTo(containsString("controller/workflow/terminate")))
        .andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.OK));

  }
}
