package net.boomerangplatform.refactor;

import static org.hamcrest.CoreMatchers.containsString;
import static org.springframework.test.web.client.ExpectedCount.manyTimes;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import net.boomerangplatform.Application;
import net.boomerangplatform.MongoConfig;
import net.boomerangplatform.mongo.model.TaskStatus;
import net.boomerangplatform.mongo.model.internal.InternalTaskRequest;
import net.boomerangplatform.mongo.model.internal.InternalTaskResponse;
import net.boomerangplatform.service.refactor.TaskService;
import net.boomerangplatform.tests.IntegrationTests;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {Application.class, MongoConfig.class})
@SpringBootTest
@ActiveProfiles("local")
public class TaskServiceTests extends IntegrationTests {

  @Autowired
  private TaskService taskService;

  @Test
  public void testSubmitTask() {
    String taskId = "5f4fd3095683833cf0b1335f";
    InternalTaskRequest request = new InternalTaskRequest();
    request.setActivityId(taskId);
    taskService.createTask(request);

    InternalTaskResponse internalTaskResponse = new InternalTaskResponse();
    internalTaskResponse.setActivityId(taskId);
    internalTaskResponse.setStatus(TaskStatus.completed);

    taskService.endTask(internalTaskResponse);
  }

  @Override
  @Before
  public void setUp() throws IOException {
    super.setUp();
    mockServer = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();
    mockServer
        .expect(manyTimes(), requestTo(containsString("http://localhost:8888/internal/task/start")))
        .andExpect(method(HttpMethod.POST)).andRespond(withSuccess());
  }


}
