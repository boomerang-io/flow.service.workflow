package net.boomerangplatform.tests;

import static org.hamcrest.CoreMatchers.containsString;
import static org.springframework.test.web.client.ExpectedCount.manyTimes;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.boomerangplatform.model.FlowActivity;
import net.boomerangplatform.model.FlowExecutionRequest;


public class IntegrationTests extends AbstractFlowTests {


  private static final Logger LOGGER = LogManager.getLogger("IntegrationTests");

  protected MockRestServiceServer mockServer;

  public static final String AUTHORIZATION_HEADER = "Authorization";
  public static final String TOKEN_PREFIX = "Bearer ";

  protected String BEARER_TOKEN =
      "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0cmJ1bGFAdXMuaWJtLmNvbSIsImF0X2hhc2giOiJlUzBlcy12MHdWQ3dNNDdQaVItZ0ZBIiwicmVhbG1OYW1lIjoiVzNJRFJlYWxtIiwiaXNzIjoiaHR0cHM6Ly93M2lkLmFscGhhLnNzby5pYm0uY29tL2lzYW0iLCJhdWQiOiJNemsxTXpObE16a3ROell4TXkwMCIsImV4cCI6MTgyNDY4OTY2MSwiaWF0IjoxNTI0NTk4MjIwLCJuYmYiOjE1MjQ1OTgxMDAsImVtYWlsQWRkcmVzcyI6InRyYnVsYUB1cy5pYm0uY29tIiwibGFzdE5hbWUiOiJSb3kiLCJibHVlR3JvdXBzIjpbIk1hbmFnZXJzYW5kSVNTdXBwb3J0IiwiV1BQTCUyMFVTJTIwTm9uLUxlbm92byUyMEVtcGxveWVlcyIsIklUU0FTJTIwRHluYW1pYyUyME1hbmFnZXJzIiwiSUJNTWFuYWdlcnMiLCJNYW5hZ2VycyUyMCUyNiUyMEhSJTIwUGFydG5lcnMiLCJpYm1sZWFybmluZyIsIklUU0FTJTIwR2VuZXJhbCUyMEFjY2VzcyUyMDIiLCJ1c2VyJTIwLSUyMHBlcmYiLCJXV19NR1JfRElTVCIsIkxBX1N0YXRzIiwiQkhQRU9DQVRTIiwiSUJNJTIwTWFuYWdlcnMiLCJFT0RfQ0NfQ29nbm9zX0dyb3VwIiwiMTM5TWdyIiwidGhpbmttYW5hZ2VtZW50LXByb2QiLCJBbGwlMjBJQk0lMjBNYW5hZ2VycyUyMChZKSIsIkNUUkVHbG9iYWxNZ3JzIiwiQkhQRU9FVkVSWU9ORSIsIm1hYyIsIkdsb2JhbE1ncnNDb21wU291cmNlIiwiSFJXZWJDb21wU291cmNlIiwiaHIuYWxsbWdycyIsIkhSRUNNV1dNYW5hZ2VycyIsIkhSRUNNV1dNYW5hZ2Vyc0FuZEhSV2ViU3VwcG9ydCIsIkNUUkVHbG9iYWxNZ3JzUGFyZW50IiwiSFJFQ01XV01hbmFnZXJzQW5kSFJBbmRIUldlYlN1cHBvcnQiLCJIUkVDTVdXTWFuYWdlcnNBbmRNYW5hZ2VyU3VwcG9ydCIsIkJIU0FQTUdSR1JQIiwiY29tLmlibS50YXAubWxkYi5hY2Nlc3MuaW50ZXJuYWwuYXV0by51cyIsImNvbS5pYm0udGFwLm1sZGIuYWNjZXNzLmludGVybmFsIiwiY29tLmlibS50YXAubWxkYi5hY2Nlc3MubmV3IiwiQkhQRU9TVUJFWFAiLCJCSFBFT0FQUFJERUwiLCJNU19fVklTSU9fX18yMDEzU0VfX0MiLCJNU19fVklTSU9fX18yMDEwU0VfX01BU1RFUiIsIk1TX19WSVNJT19fXzIwMDdTRV9fTUFTVEVSIiwiTVNfX1ZJU0lPX19fMjAxM1NFX19NQVNURVIiLCJJQk0lMjBVUyUyMFJlZ3VsYXJzIiwiTElTJTIwUmVndWxhciUyMFVTIiwibGVnYWxpYm0iLCJhaHVzZXIiLCJPU1BGX01BTkFHRVIiLCJNQU5BR0VSX09TUEYiLCJNSF9USElOS21hbmFnZW1lbnQiLCJBU0VBTiUyMFBlb3BsZSUyME1hbmFnZXJzX0VuZ2FnZW1lbnRfSW5kb25lc2lhIiwicmVjZWl2SWRHcm91cCIsIlVTTWFuYWdlcnNDNEciLCJjb2dub3MucHJvZC5oci53cGEuY29tLnVzIiwiRFNUQVBQTEVJQk0iLCJ3dy1tdXJhbGx5IiwiR2l0TGFiQWNjZXNzIiwiU0wzMjI4OTRfdnBub25seSIsIkRTVCUyMFNvZnRMYXllciUyMEludGVybmFsJTIwc3ViQWNjdHMiLCJTQ19QUk9YWV80X0dST1VQIiwiV0lOX1ZNX01BQ19NQVNURVIiLCJNU19XSU44X01BQyIsIlZNX0ZVU0lPTl9QUk84X01BQyIsIk1GcyUyMGZvciUyMGlPUyUyMC0lMjBubyUyMEpwIiwiQXBwbGVHYXJhZ2VHaXRsYWIiLCJ3dy1zbGFjay1zd2lmdCIsIldXSERDLmJhc2UiLCJUUF9HQlMyIiwiVFBfdXNlcnMiLCJ3dy1wYWdlcmR1dHkiLCJ3dy1pbnZpc2lvbi1yZXZpZXdlci1DQUlPZmZlcmluZ3NSZXZpZXciLCJ3dy1pbnZpc2lvbi1yZXZpZXdlciIsIm5vbi1kc3RJQ0QiLCJ0ZXN0ZGF5by1kYXlvIiwiV1dIREMucnRwLmJvb21lcmFuZyIsIldXSERDLlJUUCIsIklTJTIwV1NSIiwiSVMlMjBXU1IlMjBNYW5hZ2VycyUyMC0lMjBVUyIsIlNlYXJjaExpZ2h0IiwiUEFDX0FVRElUX01BTkFHRVIyIiwiY2I1IiwiZ2JzYWxsdGVjaCIsImludmlzaW9udXNlcnMiLCJib29tZXJhbmdwbGF0Zm9ybS1pY3AtZ2JzLWVuZ2luZWVyaW5nIiwiYm9vbWVyYW5ncGxhdGZvcm0taWNwLWlzYXAiLCJ3dy1pbnZpc2lvbi1pYm1kZXNpZ24yMDE4cHVyY2hhc2UiLCJ3dy1pbnZpc2lvbi1pYm1iZXRhIiwid3ctaW52aXNpb24tYWxsIl0sImNsaWVudElQIjoiMTYyLjE1OC42Mi41MCIsImF1dGhNZXRob2QiOiJleHQtYXV0aC1pbnRlcmZhY2UiLCJ1c2VyQWdlbnQiOiJNb3ppbGxhLzUuMCAoTWFjaW50b3NoOyBJbnRlbCBNYWMgT1MgWCAxMC4xMzsgcnY6NjAuMCkgR2Vja28vMjAxMDAxMDEgRmlyZWZveC82MC4wIiwiY24iOiJUWVNPTiUyMFcuJTIwTEFXUklFIiwiZG4iOiJ1aWQ9NEc3NjA5ODk3LGM9dXMsb3U9Ymx1ZXBhZ2VzLG89aWJtLmNvbSIsInVpZCI6IjRHNzYwOTg5NyIsImZpcnN0TmFtZSI6Ik1BUkNVUyIsImp0aSI6IjE4NDRkN2UwLTI1OTEtNGMxMi1hYzgzLThiNThlYmMxNmIxMSJ9.SLQViFy9RvcYIDlrhrrlQ72WFcGlKv6qxiPBYki3dZc";

  @Autowired
  public TestRestTemplate testRestTemplate;

  @Autowired
  @Qualifier("internalRestTemplate")
  protected RestTemplate restTemplate;

  @LocalServerPort
  public int port;

  @Override
  protected String[] getCollections() {
    return new String[] {"core_access_tokens", "core_groups_higher_level",
        "core_groups_lower_level", "core_tool_templates", "core_tools", "core_users",
        "core_settings", "core_audit", "requests_creategroup", "requests_createtool",
        "requests_removegroup", "requests_leavetool", "flow_teams", "flow_workflows",
        "flow_workflows_activity", "flow_workflows_activity_task", "flow_workflows_revisions",
        "flow_task_templates", "flow_settings"};
  }

  @Override
  protected Map<String, List<String>> getData() {
    LinkedHashMap<String, List<String>> data = new LinkedHashMap<>();
    data.put("core_users", Arrays.asList("db/core_users/user1.json", "db/core_users/user2.json",
        "db/core_users/user3.json", "db/core_users/user4.json"));

    data.put("core_groups_higher_level",
        Arrays.asList("db/core_groups_higher_level/highlevelgroup.json",
            "db/core_groups_higher_level/highlevelgroup2.json",
            "db/core_groups_higher_level/highlevelgroup3.json"));
    data.put("core_groups_lower_level",
        Arrays.asList("db/core_groups_lower_level/lowerlevelgroup.json"));

    data.put("flow_workflows_activity", Arrays.asList("refactor/activity.json"));

    data.put("flow_workflows_activity_task", Arrays.asList("refactor/activity_task.json"));

    data.put("flow_task_templates", Arrays.asList("refactor/template.json"));

    data.put("flow_teams", Arrays.asList("refactor/team.json"));

    data.put("flow_settings", Arrays.asList("db/flow_settings/setting1.json"));

    data.put("flow_workflows", Arrays.asList("refactor/workflow.json"));

    data.put("flow_workflows_revisions",
        Arrays.asList("refactor/revision1.json", "refactor/revision2.json"));

    return data;
  }

  protected HttpEntity<String> createHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.add(AUTHORIZATION_HEADER, TOKEN_PREFIX + BEARER_TOKEN);
    headers.add("Content-type", "application/json");
    HttpEntity<String> requestUpdate = new HttpEntity<>("", headers);
    return requestUpdate;
  }

  protected FlowActivity submitWorkflow(String workflowId, HttpEntity<String> requestUpdate) {
    ResponseEntity<FlowActivity> response =
        testRestTemplate.exchange("http://localhost:" + port + "/workflow/execute/" + workflowId,
            HttpMethod.POST, requestUpdate, FlowActivity.class);

    FlowActivity activity = response.getBody();
    logPayload(activity);
    return activity;
  }

  protected FlowActivity checkWorkflowActivity(HttpEntity<String> requestUpdate, String id) {
    ResponseEntity<FlowActivity> latestActivity =
        testRestTemplate.exchange("http://localhost:" + port + "/workflow/activity/" + id,
            HttpMethod.GET, requestUpdate, FlowActivity.class);
    FlowActivity finalActivity = latestActivity.getBody();
    logPayload(finalActivity);
    return finalActivity;
  }

  private void logPayload(FlowActivity request) {
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      String payload = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
      LOGGER.info("Logging Flow Activity: ");
      LOGGER.info(payload);
    } catch (JsonProcessingException e) {
      LOGGER.error(ExceptionUtils.getStackTrace(e));
    }
  }

  @Override
  @Before
  public void setUp() throws IOException {
    super.setUp();
    mockServer = MockRestServiceServer.bindTo(this.restTemplate).ignoreExpectOrder(true).build();
    mockServer
        .expect(times(1), requestTo(containsString("http://localhost:8084/launchpad/users")))
        .andExpect(method(HttpMethod.GET)).andRespond(
            withSuccess(getMockFile("mock/launchpad/users.json"), MediaType.APPLICATION_JSON));

    mockServer.expect(times(1), requestTo(containsString("controller/workflow/create")))
        .andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.OK));
    mockServer.expect(times(1), requestTo(containsString("controller/task/execute")))
        .andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.OK));

    mockServer.expect(times(1), requestTo(containsString("controller/workflow/terminate")))
        .andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.OK));
  }
}
