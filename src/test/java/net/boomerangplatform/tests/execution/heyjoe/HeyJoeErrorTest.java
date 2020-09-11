package net.boomerangplatform.tests.execution.heyjoe;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.client.ExpectedCount.manyTimes;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
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
import net.boomerangplatform.mongo.entity.TaskExecutionEntity;
import net.boomerangplatform.mongo.entity.ActivityEntity;
import net.boomerangplatform.service.crud.FlowActivityService;
import net.boomerangplatform.tests.execution.FlowExecutionTest;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {Application.class, MongoConfig.class})
@SpringBootTest
@ActiveProfiles("local")
public class HeyJoeErrorTest extends FlowExecutionTest {

  @Autowired
  protected FlowActivityService activityService;

  @Test
  public void testExecuteFlow() throws InterruptedException, ExecutionException {

    ActivityEntity activity = this.testFailedExecuteFlow("5e17a4b1e638b70001fae9ca");

    List<TaskExecutionEntity> tasks = activityService.getTaskExecutions(activity.getId());
    assertEquals(5, tasks.size());


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

    try {

      mockServer.expect(times(1), requestTo(containsString("controller/task/execute")))
          .andExpect(method(HttpMethod.POST))
          .andExpect(jsonPath("$.workflowName").value("HeyJoe Slack Slash Command"))
          .andExpect(jsonPath("$.taskName").value("Tail Log")).andRespond(withSuccess(
              getMockFile("scenarios/heyjoe/mock/task1-request.json"), MediaType.APPLICATION_JSON));

      mockServer.expect(times(1), requestTo(containsString("controller/task/execute")))
          .andExpect(method(HttpMethod.POST))
          .andExpect(jsonPath("$.workflowName").value("HeyJoe Slack Slash Command"))
          .andExpect(jsonPath("$.taskName").value("Send Simple Slack Message 1"))
          .andExpect(method(HttpMethod.POST))
          .andRespond(withSuccess(getMockFile("scenarios/heyjoe/mock/task2-response.json"),
              MediaType.APPLICATION_JSON));

    } catch (IOException e) {
      e.printStackTrace();
    }


    mockServer.expect(times(1), requestTo(containsString("controller/workflow/terminate")))
        .andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.OK));

  }
}
