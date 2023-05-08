package io.boomerang.scenarios;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import io.boomerang.model.FlowActivity;
import io.boomerang.mongo.model.TaskStatus;
import io.boomerang.mongo.model.TokenScope;
import io.boomerang.security.service.IdentityService;
import io.boomerang.tests.IntegrationTests;
import io.boomerang.v4.data.entity.UserEntity;
import io.boomerang.v4.model.Action;
import io.boomerang.v4.model.UserType;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@WithMockUser(roles = {"admin"})
@WithUserDetails("mdroy@us.ibm.com")
class PMOExecuteTests extends IntegrationTests {

  @MockBean
  private IdentityService service;

  @Test
  void testExecution() throws Exception {
    UserEntity user = new UserEntity();
    user.setEmail("amhudson@us.ibm.com");
    user.setName("Adrienne Hudson");
    user.setType(UserType.admin);

    when(service.getCurrentScope()).thenReturn(TokenPermission.user);
    when(service.getCurrentUser()).thenReturn(user);

    String workflowId = "5fd0099a2dfe2d6d5e4295de";

    FlowActivity activity = submitWorkflow(workflowId);

    String id = activity.getId();
    Thread.sleep(10000);

    List<Action> approvals = this.getApprovals();

    this.approveWorkflow(true, approvals.get(0).getId());

    Thread.sleep(5000);

    approvals = this.getApprovals();


    this.approveWorkflow(true, approvals.get(0).getId());

    Thread.sleep(5000);
    FlowActivity finalActivity = this.checkWorkflowActivity(id);
    assertEquals(TaskStatus.completed, finalActivity.getStatus());
    assertNotNull(finalActivity.getDuration());
    mockServer.verify();
  }

  @Override
  @BeforeEach
  public void setUp() throws IOException {
    super.setUp();
    mockServer = MockRestServiceServer.bindTo(this.restTemplate).ignoreExpectOrder(true).build();

    // mockServer.expect(manyTimes(), requestTo(containsString("internal/users/user")))
    // .andExpect(method(HttpMethod.GET)).andRespond(
    // withSuccess(getMockFile("mock/users/users.json"), MediaType.APPLICATION_JSON));
    mockServer.expect(times(1), requestTo(containsString("controller/workflow/execute")))
        .andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.OK));

    mockServer.expect(times(1), requestTo(containsString("controller/task/execute")))
        .andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.OK));

    mockServer.expect(times(1), requestTo(containsString("controller/workflow/terminate")))
        .andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.OK));
  }

  @Override
  protected void getTestCaseData(Map<String, List<String>> data) {
    data.put("flow_workflows", Arrays.asList("tests/scenarios/pmo/pmo-workflow.json"));
    data.put("flow_workflows_revisions", Arrays.asList("tests/scenarios/pmo/pmo-revision1.json"));
  }

}
