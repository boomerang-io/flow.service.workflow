package net.boomerangplatform.scenarios;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import net.boomerangplatform.model.FlowActivity;
import net.boomerangplatform.model.FlowWebhookResponse;
import net.boomerangplatform.model.RequestFlowExecution;
import net.boomerangplatform.model.controller.TaskWorkspace;
import net.boomerangplatform.mongo.model.TaskStatus;
import net.boomerangplatform.service.refactor.TaskClient;
import net.boomerangplatform.tests.IntegrationTests;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@ActiveProfiles("local")
@WithMockUser(roles = {"admin"})
@WithUserDetails("mdroy@us.ibm.com")
public class RunWorkflowExecutionTest extends IntegrationTests {

  @SpyBean
  private TaskClient taskClient;
  
  @Test
  public void testExecution() throws Exception {
  
   
    doReturn(null).when(taskClient).submitWebhookEvent(ArgumentMatchers.any(RequestFlowExecution.class));
 
    String workflowId = "603936f5c3a72a0d655fb337";
    RequestFlowExecution request = new RequestFlowExecution();

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
    verify(taskClient).submitWebhookEvent(ArgumentMatchers.any(RequestFlowExecution.class));
  }

  @Override
  @Before
  public void setUp() throws IOException {
    super.setUp();
    mockServer = MockRestServiceServer.bindTo(this.restTemplate).ignoreExpectOrder(true).build();
    mockServer.expect(times(1), requestTo(containsString("controller/workflow/create")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withStatus(HttpStatus.OK));
    mockServer.expect(times(1), requestTo(containsString("internal/users/user")))
       .andExpect(method(HttpMethod.GET)).andRespond(
           withSuccess(getMockFile("mock/users/users.json"), MediaType.APPLICATION_JSON));
    mockServer.expect(times(1), requestTo(containsString("controller/workflow/terminate")))
        .andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.OK));
  }

  @Override
  protected void getTestCaseData(Map<String, List<String>> data) {
    data.put("flow_workflows", Arrays.asList("tests/scenarios/runworkflow/runworkflow-workflow.json"));
    data.put("flow_workflows_revisions",
        Arrays.asList("tests/scenarios/runworkflow/runworkflow-revision.json"));
  }

}