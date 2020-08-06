package net.boomerangplatform.tests.controller;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.boomerangplatform.Application;
import net.boomerangplatform.MongoConfig;
import net.boomerangplatform.controller.ActivityController;
import net.boomerangplatform.model.FlowActivity;
import net.boomerangplatform.model.InsightsSummary;
import net.boomerangplatform.model.ListActivityResponse;
import net.boomerangplatform.mongo.model.FlowTaskStatus;
import net.boomerangplatform.tests.FlowTests;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {Application.class, MongoConfig.class})
@SpringBootTest
@ActiveProfiles("local")
@WithMockUser(roles = {"admin"})
@WithUserDetails("mdroy@us.ibm.com")
public class ActivityControllerTests extends FlowTests {

  @Autowired
  private ActivityController activityController;

  @Test
  public void testGetFlowActivity() {

    FlowActivity activity = activityController.getFlowActivity("5d1a18c8f6ca2c00014c4325");
    assertEquals("5d1a18c8f6ca2c00014c4325", activity.getId());
  }

  @Test
  public void testGetTaskLog() throws IOException {

    mockServer = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();
    mockServer.expect(times(1), requestTo(containsString("controller/log/stream")))
        .andExpect(method(HttpMethod.GET)).andRespond(withStatus(HttpStatus.OK));
    MockHttpServletResponse response = new MockHttpServletResponse();
    ResponseEntity<StreamingResponseBody> streamingResponse = activityController
        .getTaskLog(response, "5d1a18c8f6ca2c00014c4325", "58340aec-4661-4768-aeec-307c1553409e");
    streamingResponse.getBody().writeTo(System.out);
    assertEquals(HttpStatus.OK, streamingResponse.getStatusCode());
  }

  @Test
  public void testGetFlowActivities() {
    List<String> workflowIds = new ArrayList<>();
    workflowIds.add("5d1a188af6ca2c00014c4314");

    List<String> teamIds = new ArrayList<>();
    teamIds.add("5d1a1841f6ca2c00014c4309");

    ListActivityResponse response = activityController.getFlowActivities(
        getOptionalOrder(Direction.ASC), getOptionalString("sort"),
        getOptionalListString(workflowIds), getOptionalListString(teamIds), 0, 2147483647,
        Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

    assertEquals(2, response.getRecords().size());
  }

  @Test
  public void testGetInsightsSummary() {
    InsightsSummary summary = activityController.getInsightsSummary(getOptionalOrder(Direction.ASC),
        getOptionalString("sort"), getOptionalString("5d1a1841f6ca2c00014c4309"), 0, 2147483647,
        Optional.empty(), Optional.empty());

    assertEquals(2, summary.getExecutions().size());
    Long executiontime = (summary.getExecutions().get(0).getDuration()
        + summary.getExecutions().get(1).getDuration()) / summary.getExecutions().size();
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      System.out.println(objectMapper.writeValueAsString(summary));
    } catch (JsonProcessingException e) {
      // log an error
    }
    assertEquals(executiontime, summary.getMedianExecutionTime());
    assertEquals(2, summary.getTotalActivitiesExecuted().intValue());

  }

  @Test
  public void testGetActivitySummary() {
    Map<String, Long> activitySummary = activityController.getFlowActivitySummary(Direction.ASC, 0,
        2147483647, null, null, null, null, null);

    assertEquals(5, activitySummary.get("all").longValue());
    assertEquals(3, activitySummary.get(FlowTaskStatus.completed.getStatus()).longValue());
    assertEquals(2, activitySummary.get(FlowTaskStatus.inProgress.getStatus()).longValue());
  }

  Optional<String> getOptionalString(String string) {
    return Optional.of(string);
  }

  Optional<List<String>> getOptionalListString(List<String> strings) {
    return Optional.of(strings);
  }

  Optional<Direction> getOptionalOrder(Direction direction) {
    return Optional.of(direction);
  }
}
