package net.boomerangplatform.scenarios;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import net.boomerangplatform.model.FlowActivity;
import net.boomerangplatform.model.FlowWebhookResponse;
import net.boomerangplatform.model.RequestFlowExecution;
import net.boomerangplatform.model.controller.TaskWorkspace;
import net.boomerangplatform.mongo.model.TaskStatus;
import net.boomerangplatform.tests.IntegrationTests;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.client.MockRestServiceServer;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@ActiveProfiles("local")
@WithMockUser(roles = {"admin"})
@WithUserDetails("mdroy@us.ibm.com")
public class SimpleExecuteTests extends IntegrationTests {

  @Test
  public void testExecution() throws Exception {
    String workflowId = "5f4fc9e95683833cf0b1335b";
    RequestFlowExecution request = new  RequestFlowExecution();
    
    request.setWorkflowId(workflowId);

    TaskWorkspace taskWorkspace = new TaskWorkspace();
    taskWorkspace.setOptional(false);
    taskWorkspace.setReadOnly(false);
    taskWorkspace.setWorkspaceId("12345");
    taskWorkspace.setWorkspaceName("Test");
    
    List<TaskWorkspace> taskWorkspaceList = new LinkedList<>();
    taskWorkspaceList.add(taskWorkspace);

    request.setTaskWorkspaces(taskWorkspaceList);
    
    Map<String, String> map = new HashMap<>();
    map.put("foobar", "Hello World");
    request.setProperties(map);
    FlowWebhookResponse activity = this.submitInternalWorkflow(workflowId, request);
    
    String id = activity.getActivityId();
    Thread.sleep(5000);
    FlowActivity finalActivity = this.checkWorkflowActivity(id);
    assertEquals(TaskStatus.completed, finalActivity.getStatus());
    assertNotNull(finalActivity.getDuration());
    mockServer.verify();
  }

  @Override
  @Before
  public void setUp() throws IOException {
    super.setUp();
    mockServer = MockRestServiceServer.bindTo(this.restTemplate).ignoreExpectOrder(true).build();

    mockServer.expect(times(1), requestTo(containsString("controller/workflow/create")))
        .andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.OK));

    mockServer.expect(times(1), requestTo(containsString("controller/task/execute")))
        .andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.OK));

    mockServer.expect(times(1), requestTo(containsString("controller/workflow/terminate")))
        .andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.OK));
  }

  @Override
  protected void getTestCaseData(Map<String, List<String>> data) {
    data.put("flow_workflows", Arrays.asList("tests/scenarios/simple/simple-workflow.json"));
    data.put("flow_workflows_revisions",
        Arrays.asList("tests/scenarios/simple/simple-revision1.json",
            "tests/scenarios/simple/simple-revision2.json"));
  }

}
