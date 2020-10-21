package net.boomerangplatform.scenarios;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import net.boomerangplatform.Application;
import net.boomerangplatform.model.Approval;
import net.boomerangplatform.model.FlowActivity;
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
public class ApproveExecuteTests extends IntegrationTests {

  @Test
  public void testExecution() throws Exception {
    String workflowId = "5f4fc9e95683833cf0b1335b";
    FlowActivity activity = submitWorkflow(workflowId);
    String id = activity.getId();
    Thread.sleep(5000);
    FlowActivity waitingAprpoval = this.checkWorkflowActivity(id);
    assertEquals(TaskStatus.inProgress, waitingAprpoval.getStatus());
    List<Approval> approvals = this.getApprovals();
    this.approveWorkflow(true, approvals.get(0).getId());
  
    Thread.sleep(5000);
    FlowActivity finalActivity = this.checkWorkflowActivity(id);
    assertEquals(TaskStatus.completed, finalActivity.getStatus());
    mockServer.verify();
  }

  @Override
  @Before
  public void setUp() throws IOException {
    super.setUp();
    mockServer = MockRestServiceServer.bindTo(this.restTemplate).ignoreExpectOrder(true).build();

    mockServer.expect(times(1), requestTo(containsString("launchpad/users")))
        .andExpect(method(HttpMethod.GET)).andRespond(
            withSuccess(getMockFile("mock/launchpad/users.json"), MediaType.APPLICATION_JSON));
    mockServer.expect(times(1), requestTo(containsString("controller/workflow/create")))
        .andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.OK));
    
    mockServer.expect(times(3), requestTo(containsString("launchpad/users")))
    .andExpect(method(HttpMethod.GET)).andRespond(
        withSuccess(getMockFile("mock/launchpad/users.json"), MediaType.APPLICATION_JSON));
    
    mockServer.expect(times(1), requestTo(containsString("users/user/5e736fb0a97b78000125ebe3")))
    .andExpect(method(HttpMethod.GET)).andRespond(
        withSuccess(getMockFile("mock/launchpad/users.json"), MediaType.APPLICATION_JSON));
    
    
    
    
    mockServer.expect(times(1), requestTo(containsString("controller/workflow/terminate")))
    .andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.OK));


  }

  @Override
  protected void getTestCaseData(Map<String, List<String>> data) {
    data.put("flow_workflows", Arrays.asList("tests/scenarios/approval/approval-workflow.json"));
    data.put("flow_workflows_revisions",
        Arrays.asList("tests/scenarios/approval/approval-revision1.json",
            "tests/scenarios/approval/approval-revision2.json"));
  }

}
