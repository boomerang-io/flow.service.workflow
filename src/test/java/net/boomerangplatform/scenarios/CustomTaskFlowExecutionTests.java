package net.boomerangplatform.scenarios;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.client.ExpectedCount.manyTimes;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import net.boomerangplatform.Application;
import net.boomerangplatform.model.FlowActivity;
import net.boomerangplatform.service.crud.FlowActivityService;
import net.boomerangplatform.tests.IntegrationTests;
import net.boomerangplatform.tests.MongoConfig;

@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {Application.class, MongoConfig.class})
@ActiveProfiles("local")

public class CustomTaskFlowExecutionTests extends IntegrationTests {

  @Autowired
  protected FlowActivityService activityService;

  @Test
  public void testExecuteFlow() throws InterruptedException, ExecutionException {
    
    String workflowId = "5d7177af2c57250007e3d7a9";
   
    FlowActivity activity = submitWorkflow(workflowId);

    String id = activity.getId();
    Thread.sleep(5000);
    FlowActivity finalActivity = this.checkWorkflowActivity(id);
  
    assertNotNull(finalActivity.getDuration()); 
    mockServer.verify();

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
    
    mockServer.expect(times(1), requestTo(containsString("controller/task/execute")))
        .andExpect(method(HttpMethod.POST))
        .andExpect(jsonPath("$.workflowName").value("Unit Test Demo"))
        .andExpect(jsonPath("$.taskType").value("template"))
        .andExpect(jsonPath("$.taskName").value("Echo Test")).andRespond(withStatus(HttpStatus.OK));

    mockServer.expect(times(1), requestTo(containsString("controller/task/execute")))
        .andExpect(method(HttpMethod.POST))
        .andExpect(jsonPath("$.workflowName").value("Unit Test Demo"))
        .andExpect(jsonPath("$.taskType").value("custom"))
        .andExpect(jsonPath("$.command").value("uname"))
        .andExpect(jsonPath("$.image").value("busybox"))
        .andExpect(jsonPath("$.arguments").value("test"))
        .andExpect(jsonPath("$.taskName").value("Custom Task 1"))
        .andRespond(withStatus(HttpStatus.OK));

    mockServer.expect(times(1), requestTo(containsString("controller/workflow/terminate")))
        .andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.OK));

  }

  @Override
  protected void getTestCaseData(Map<String, List<String>> data) {
    data.put("flow_workflows", Arrays.asList(
        "tests/scenarios/custom/custom-workflow.json"));
    data.put("flow_workflows_revisions",
        Arrays.asList(
            "tests/scenarios/custom/custom-revision1.json"));
  }
}
