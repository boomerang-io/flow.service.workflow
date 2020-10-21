package net.boomerangplatform.scenarios;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.ExpectedCount.manyTimes;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import net.boomerangplatform.Application;
import net.boomerangplatform.model.FlowActivity;
import net.boomerangplatform.model.FlowExecutionRequest;
import net.boomerangplatform.mongo.entity.TaskExecutionEntity;
import net.boomerangplatform.mongo.model.TaskStatus;
import net.boomerangplatform.tests.IntegrationTests;
import net.boomerangplatform.tests.MongoConfig;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.client.MockRestServiceServer;

@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {Application.class, MongoConfig.class})
@ActiveProfiles("local")

public class SucessRedBranchFlowExecutionTest extends IntegrationTests {

  @Test
  public void testExecution() throws Exception {
    String workflowId = "5d72be2ce7a4aa00072f9ee3";

    FlowExecutionRequest request = new FlowExecutionRequest();

    Map<String, String> inputs = new HashMap<String, String>();
    inputs.put("color", "red");
    request.setProperties(inputs);

    FlowActivity activity = submitWorkflow(workflowId, request);

    String id = activity.getId();
    Thread.sleep(10000);
    FlowActivity finalActivity = this.checkWorkflowActivity(id);
    assertEquals(TaskStatus.completed, finalActivity.getStatus());
    assertNotNull(finalActivity.getDuration());
    mockServer.verify();
    
    
    List<TaskExecutionEntity> steps = finalActivity.getSteps();
    TaskExecutionEntity executeShell1 = steps.stream().filter(e -> e.getTaskName().equals("Execute Shell 1")).findFirst().orElse(null);
    TaskExecutionEntity executeShell2 = steps.stream().filter(e -> e.getTaskName().equals("Execute Shell 2")).findFirst().orElse(null);
    TaskExecutionEntity executeShell3 = steps.stream().filter(e -> e.getTaskName().equals("Execute Shell 3")).findFirst().orElse(null);
    
    assertEquals(TaskStatus.skipped, executeShell1.getFlowTaskStatus());
    assertEquals(TaskStatus.completed, executeShell2.getFlowTaskStatus());
    assertEquals(TaskStatus.completed, executeShell3.getFlowTaskStatus());
  }

  @Override
  @Before
  public void setUp() throws IOException {
    super.setUp();
    mockServer = MockRestServiceServer.bindTo(this.restTemplate).ignoreExpectOrder(true).build();
    mockServer
        .expect(manyTimes(), requestTo(containsString("http://localhost:8084/launchpad/users")))
        .andExpect(method(HttpMethod.GET)).andRespond(
            withSuccess(getMockFile("mock/launchpad/users.json"), MediaType.APPLICATION_JSON));
    mockServer.expect(times(1), requestTo(containsString("controller/workflow/create")))
        .andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.OK));
    mockServer.expect(times(1), requestTo(containsString("controller/task/execute")))
        .andExpect(jsonPath("$.taskName").value("Execute Shell 2"))
        .andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.OK));
    mockServer.expect(times(1), requestTo(containsString("controller/task/execute")))
        .andExpect(jsonPath("$.taskName").value("Execute Shell 3"))
        .andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.OK));
    mockServer.expect(times(1), requestTo(containsString("controller/workflow/terminate")))
        .andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.OK));
  }

  @Override
  protected void getTestCaseData(Map<String, List<String>> data) {
    data.put("flow_workflows", Arrays.asList("tests/scenarios/branch/branch-workflow.json"));
    data.put("flow_workflows_revisions",
        Arrays.asList("tests/scenarios/branch/branch-revision1.json"));
  }

}

