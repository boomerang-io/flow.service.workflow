package io.boomerang.scenarios;

import static org.hamcrest.CoreMatchers.containsString;
import static org.springframework.test.web.client.ExpectedCount.manyTimes;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import io.boomerang.model.FlowActivity;
import io.boomerang.mongo.model.TaskStatus;
import io.boomerang.tests.IntegrationTests;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@WithMockUser(roles = {"admin"})
@WithUserDetails("mdroy@us.ibm.com")
public class LockExecutionTests extends IntegrationTests {

  @Test
  public void testExecution() throws Exception {
    
    String workflowId = "600efc3e9e161e473d6b7b4e";
    FlowActivity activity = submitWorkflow(workflowId);
    String id = activity.getId();
    Thread.sleep(15000);
    FlowActivity finalActivity = this.checkWorkflowActivity(id);
    Assertions.assertEquals(TaskStatus.completed, finalActivity.getStatus());
    Assertions.assertNotNull(finalActivity.getDuration());
    mockServer.verify();
  }

  @Override
  @BeforeEach
  public void setUp() throws IOException {
    super.setUp();
    mockServer = MockRestServiceServer.bindTo(this.restTemplate).ignoreExpectOrder(true).build();
    mockServer.expect(manyTimes(), requestTo(containsString("internal/users/user")))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(getMockFile("mock/users/users.json"), MediaType.APPLICATION_JSON));
    mockServer.expect(times(1), requestTo(containsString("controller/workflow/execute")))
        .andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.OK));
    

    mockServer.expect(times(1), requestTo(containsString("controller/task/execute")))
        .andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.OK));
    

    mockServer.expect(times(1), requestTo(containsString("controller/task/execute")))
        .andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.OK));
    
    mockServer.expect(times(1), requestTo(containsString("controller/workflow/terminate")))
        .andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.OK));
  }

  @Override
  protected void getTestCaseData(Map<String, List<String>> data) {
    data.put("flow_workflows", Arrays.asList("tests/scenarios/locks/lock-workflow.json"));
    data.put("flow_workflows_revisions",
        Arrays.asList("tests/scenarios/locks/lock-revision1.json"));
  }

}
