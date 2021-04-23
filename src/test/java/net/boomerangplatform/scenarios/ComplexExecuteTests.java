package net.boomerangplatform.scenarios;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.ExpectedCount.manyTimes;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
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
import net.boomerangplatform.model.FlowActivity;
import net.boomerangplatform.mongo.model.TaskStatus;
import net.boomerangplatform.tests.IntegrationTests;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
public class ComplexExecuteTests extends IntegrationTests {

  @Test
  public void testExecution() throws Exception {
    String workflowId = "5f5fddd25683833cf0b133ff";

    FlowActivity activity = submitWorkflow(workflowId);

    String id = activity.getId();
    Thread.sleep(1000000);
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

    mockServer.expect(manyTimes(), requestTo(containsString("internal/users/user")))
        .andExpect(method(HttpMethod.GET)).andRespond(
            withSuccess(getMockFile("mock/users/users.json"), MediaType.APPLICATION_JSON));
    mockServer.expect(times(1), requestTo(containsString("controller/workflow/create")))
        .andExpect(jsonPath("$.labels.foo").value("bar"))
        .andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.OK));

    mockServer.expect(times(5), requestTo(containsString("controller/task/execute")))
        .andExpect(jsonPath("$.labels.foo").value("bar"))
        .andExpect(jsonPath("$.configuration.lifecycle").value(true))
        .andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.OK));

    mockServer.expect(times(1), requestTo(containsString("controller/workflow/terminate")))
        .andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.OK));
  }

  @Override
  protected void getTestCaseData(Map<String, List<String>> data) {
    data.put("flow_workflows", Arrays.asList("tests/scenarios/complex/complex-workflow.json"));
    data.put("flow_workflows_revisions",
        Arrays.asList(
            "tests/scenarios/complex/complex-revision1.json"));
  }

}
