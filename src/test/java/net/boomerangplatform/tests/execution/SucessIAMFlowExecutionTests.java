package net.boomerangplatform.tests.execution;

import static org.hamcrest.CoreMatchers.containsString;
import static org.springframework.test.web.client.ExpectedCount.manyTimes;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import net.boomerangplatform.mongo.model.TaskStatus;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {Application.class, MongoConfig.class})
@SpringBootTest
@ActiveProfiles("local")
public class SucessIAMFlowExecutionTests extends FlowExecutionTest {

  @Test
  public void testExecuteFlow() throws InterruptedException, ExecutionException {

    FlowExecutionRequest request = new FlowExecutionRequest();
    Map<String, String> properties = new HashMap<String, String>();
    properties.put("execution_id", "hello world");
    request.setProperties(properties);

    this.testSuccessExecuteFlow("5d7177af2c57250007e3d7a2", request, TaskStatus.completed);
  }

  @Override
  @Before
  public void setUp() throws IOException {
    super.setUp();

    try {
      mockServer = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();
      mockServer
          .expect(manyTimes(), requestTo(containsString("http://localhost:8084/launchpad/users")))
          .andExpect(method(HttpMethod.GET)).andRespond(
              withSuccess(getMockFile("mock/launchpad/users.json"), MediaType.APPLICATION_JSON));
      mockServer.expect(times(1), requestTo(containsString("controller/workflow/create")))
          .andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.OK));


      mockServer.expect(times(2), requestTo(containsString("/acc/customEvent")))
          .andExpect(method(HttpMethod.POST)).andRespond(
              withSuccess(getMockFile("mock/acc/acc-response.json"), MediaType.APPLICATION_JSON));


      mockServer.expect(times(1), requestTo(containsString("controller/task/execute")))
          .andExpect(method(HttpMethod.POST))
          .andExpect(jsonPath("$.workflowName").value("Unit Test Demo"))
          .andExpect(jsonPath("$.taskName").value("Echo Test"))
          .andRespond(withStatus(HttpStatus.OK));
      mockServer.expect(times(1), requestTo(containsString("controller/task/execute")))
          .andExpect(method(HttpMethod.POST))
          .andExpect(jsonPath("$.workflowName").value("Unit Test Demo"))
          .andExpect(jsonPath("$.taskName").value("Sleep 1")).andRespond(withStatus(HttpStatus.OK));
      mockServer.expect(times(1), requestTo(containsString("controller/workflow/terminate")))
          .andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.OK));

      mockServer.expect(times(1), requestTo(containsString("/acc/customEvent")))
          .andExpect(method(HttpMethod.POST)).andRespond(
              withSuccess(getMockFile("mock/acc/acc-response.json"), MediaType.APPLICATION_JSON));


    } catch (IOException e) {

    }

  }
}
