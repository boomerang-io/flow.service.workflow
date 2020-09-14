package net.boomerangplatform.scenarios;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import net.boomerangplatform.Application;
import net.boomerangplatform.model.FlowActivity;
import net.boomerangplatform.tests.IntegrationTests;
import net.boomerangplatform.tests.MongoConfig;

@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {Application.class, MongoConfig.class})
@ActiveProfiles("local")
public class InvalidFlowExecutionTests extends IntegrationTests {

  @Test
  public void testExecution() throws Exception {
    String workflowId = "5d1a188af6ca2c00014c4314";

    FlowActivity activity = submitWorkflow(workflowId);
    assertNull(activity);

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
  }

  @Override
  protected void getTestCaseData(Map<String, List<String>> data) {
    data.put("flow_workflows", Arrays.asList("tests/scenarios/invalid/invalid-workflow.json"));
    data.put("flow_workflows_revisions",
        Arrays.asList("tests/scenarios/invalid/invalid-revision1.json"));
  }

}


