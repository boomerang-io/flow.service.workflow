package io.boomerang.tests.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import io.boomerang.controller.InternalController;
import io.boomerang.model.FlowExecutionRequest;
import io.boomerang.model.WorkflowSummary;
import io.boomerang.model.WorkflowToken;
import io.boomerang.model.eventing.EventResponse;
import io.boomerang.mongo.model.TriggerEvent;
import io.boomerang.mongo.model.Triggers;
import io.boomerang.service.ExecutionService;
import io.boomerang.service.crud.FlowActivityService;
import io.boomerang.service.crud.WorkflowService;
import io.boomerang.service.refactor.TaskService;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CloudEventsServiceTests {

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String TOKEN_PREFIX = "Bearer ";
  private String BEARER_TOKEN =
      "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0cmJ1bGFAdXMuaWJtLmNvbSIsImF0X2hhc2giOiJlUzBlcy12MHdWQ3dNNDdQaVItZ0ZBIiwicmVhbG1OYW1lIjoiVzNJRFJlYWxtIiwiaXNzIjoiaHR0cHM6Ly93M2lkLmFscGhhLnNzby5pYm0uY29tL2lzYW0iLCJhdWQiOiJNemsxTXpObE16a3ROell4TXkwMCIsImV4cCI6MTgyNDY4OTY2MSwiaWF0IjoxNTI0NTk4MjIwLCJuYmYiOjE1MjQ1OTgxMDAsImVtYWlsQWRkcmVzcyI6InRyYnVsYUB1cy5pYm0uY29tIiwibGFzdE5hbWUiOiJSb3kiLCJibHVlR3JvdXBzIjpbIk1hbmFnZXJzYW5kSVNTdXBwb3J0IiwiV1BQTCUyMFVTJTIwTm9uLUxlbm92byUyMEVtcGxveWVlcyIsIklUU0FTJTIwRHluYW1pYyUyME1hbmFnZXJzIiwiSUJNTWFuYWdlcnMiLCJNYW5hZ2VycyUyMCUyNiUyMEhSJTIwUGFydG5lcnMiLCJpYm1sZWFybmluZyIsIklUU0FTJTIwR2VuZXJhbCUyMEFjY2VzcyUyMDIiLCJ1c2VyJTIwLSUyMHBlcmYiLCJXV19NR1JfRElTVCIsIkxBX1N0YXRzIiwiQkhQRU9DQVRTIiwiSUJNJTIwTWFuYWdlcnMiLCJFT0RfQ0NfQ29nbm9zX0dyb3VwIiwiMTM5TWdyIiwidGhpbmttYW5hZ2VtZW50LXByb2QiLCJBbGwlMjBJQk0lMjBNYW5hZ2VycyUyMChZKSIsIkNUUkVHbG9iYWxNZ3JzIiwiQkhQRU9FVkVSWU9ORSIsIm1hYyIsIkdsb2JhbE1ncnNDb21wU291cmNlIiwiSFJXZWJDb21wU291cmNlIiwiaHIuYWxsbWdycyIsIkhSRUNNV1dNYW5hZ2VycyIsIkhSRUNNV1dNYW5hZ2Vyc0FuZEhSV2ViU3VwcG9ydCIsIkNUUkVHbG9iYWxNZ3JzUGFyZW50IiwiSFJFQ01XV01hbmFnZXJzQW5kSFJBbmRIUldlYlN1cHBvcnQiLCJIUkVDTVdXTWFuYWdlcnNBbmRNYW5hZ2VyU3VwcG9ydCIsIkJIU0FQTUdSR1JQIiwiY29tLmlibS50YXAubWxkYi5hY2Nlc3MuaW50ZXJuYWwuYXV0by51cyIsImNvbS5pYm0udGFwLm1sZGIuYWNjZXNzLmludGVybmFsIiwiY29tLmlibS50YXAubWxkYi5hY2Nlc3MubmV3IiwiQkhQRU9TVUJFWFAiLCJCSFBFT0FQUFJERUwiLCJNU19fVklTSU9fX18yMDEzU0VfX0MiLCJNU19fVklTSU9fX18yMDEwU0VfX01BU1RFUiIsIk1TX19WSVNJT19fXzIwMDdTRV9fTUFTVEVSIiwiTVNfX1ZJU0lPX19fMjAxM1NFX19NQVNURVIiLCJJQk0lMjBVUyUyMFJlZ3VsYXJzIiwiTElTJTIwUmVndWxhciUyMFVTIiwibGVnYWxpYm0iLCJhaHVzZXIiLCJPU1BGX01BTkFHRVIiLCJNQU5BR0VSX09TUEYiLCJNSF9USElOS21hbmFnZW1lbnQiLCJBU0VBTiUyMFBlb3BsZSUyME1hbmFnZXJzX0VuZ2FnZW1lbnRfSW5kb25lc2lhIiwicmVjZWl2SWRHcm91cCIsIlVTTWFuYWdlcnNDNEciLCJjb2dub3MucHJvZC5oci53cGEuY29tLnVzIiwiRFNUQVBQTEVJQk0iLCJ3dy1tdXJhbGx5IiwiR2l0TGFiQWNjZXNzIiwiU0wzMjI4OTRfdnBub25seSIsIkRTVCUyMFNvZnRMYXllciUyMEludGVybmFsJTIwc3ViQWNjdHMiLCJTQ19QUk9YWV80X0dST1VQIiwiV0lOX1ZNX01BQ19NQVNURVIiLCJNU19XSU44X01BQyIsIlZNX0ZVU0lPTl9QUk84X01BQyIsIk1GcyUyMGZvciUyMGlPUyUyMC0lMjBubyUyMEpwIiwiQXBwbGVHYXJhZ2VHaXRsYWIiLCJ3dy1zbGFjay1zd2lmdCIsIldXSERDLmJhc2UiLCJUUF9HQlMyIiwiVFBfdXNlcnMiLCJ3dy1wYWdlcmR1dHkiLCJ3dy1pbnZpc2lvbi1yZXZpZXdlci1DQUlPZmZlcmluZ3NSZXZpZXciLCJ3dy1pbnZpc2lvbi1yZXZpZXdlciIsIm5vbi1kc3RJQ0QiLCJ0ZXN0ZGF5by1kYXlvIiwiV1dIREMucnRwLmJvb21lcmFuZyIsIldXSERDLlJUUCIsIklTJTIwV1NSIiwiSVMlMjBXU1IlMjBNYW5hZ2VycyUyMC0lMjBVUyIsIlNlYXJjaExpZ2h0IiwiUEFDX0FVRElUX01BTkFHRVIyIiwiY2I1IiwiZ2JzYWxsdGVjaCIsImludmlzaW9udXNlcnMiLCJib29tZXJhbmdwbGF0Zm9ybS1pY3AtZ2JzLWVuZ2luZWVyaW5nIiwiYm9vbWVyYW5ncGxhdGZvcm0taWNwLWlzYXAiLCJ3dy1pbnZpc2lvbi1pYm1kZXNpZ24yMDE4cHVyY2hhc2UiLCJ3dy1pbnZpc2lvbi1pYm1iZXRhIiwid3ctaW52aXNpb24tYWxsIl0sImNsaWVudElQIjoiMTYyLjE1OC42Mi41MCIsImF1dGhNZXRob2QiOiJleHQtYXV0aC1pbnRlcmZhY2UiLCJ1c2VyQWdlbnQiOiJNb3ppbGxhLzUuMCAoTWFjaW50b3NoOyBJbnRlbCBNYWMgT1MgWCAxMC4xMzsgcnY6NjAuMCkgR2Vja28vMjAxMDAxMDEgRmlyZWZveC82MC4wIiwiY24iOiJUWVNPTiUyMFcuJTIwTEFXUklFIiwiZG4iOiJ1aWQ9NEc3NjA5ODk3LGM9dXMsb3U9Ymx1ZXBhZ2VzLG89aWJtLmNvbSIsInVpZCI6IjRHNzYwOTg5NyIsImZpcnN0TmFtZSI6Ik1BUkNVUyIsImp0aSI6IjE4NDRkN2UwLTI1OTEtNGMxMi1hYzgzLThiNThlYmMxNmIxMSJ9.SLQViFy9RvcYIDlrhrrlQ72WFcGlKv6qxiPBYki3dZc";

  @Autowired
  private InternalController internalController;

  @MockBean
  private WorkflowService workflowService;

  @MockBean
  private ExecutionService executionService;

  @MockBean
  private TaskService taskService;

  @MockBean
  private FlowActivityService flowActivityService;

  private String workflowId;
  private WorkflowSummary workflowEntity = new WorkflowSummary();
  private HttpHeaders headers = new HttpHeaders();

  @BeforeEach
  void setUp() {
    headers.add(AUTHORIZATION_HEADER, TOKEN_PREFIX + BEARER_TOKEN);
    headers.add("Content-type", "application/cloudevents+json");

    workflowId = "5f7f8cf69a7d401d9e584c90";
    workflowEntity.setId(workflowId);

    Triggers triggers = new Triggers();
    TriggerEvent customTrig = new TriggerEvent();
    customTrig.setEnable(true);
    customTrig.setTopic("foobar");
    triggers.setCustom(customTrig);
    workflowEntity.setTriggers(triggers);

    WorkflowToken token = new WorkflowToken();
    token.setToken("RXgGaXBzdW0gZG9sb3Ih");
    workflowEntity.setTokens(List.of(token));

    when(workflowService.getWorkflow(workflowId)).thenReturn(workflowEntity);
  }

  @Test
  void testAcceptTriggerEvent() {
    // @formatter:off
    String payload = String.join("", "{",
        "\"id\":\"36965047-1191-4aff-8e17-fe4e8c8e528a\"",
        ",\"type\":\"io.boomerang.event.workflow.trigger\"",
        ",\"source\":\"http://wdc2.cloud.boomerangplatform.net/listener/event\"",
        ",\"specversion\":\"1.0\"",
        ",\"datacontenttype\":\"application/json\"",
        ",\"subject\":\"/workflow/5f7f8cf69a7d401d9e584c90/topic/foobar\"",
        ",\"token\":\"RXgGaXBzdW0gZG9sb3Ih\"",
        ",\"time\":\"2022-04-30T11:33:22Z\"",
        "}");
    
    // @formatter:on

    when(executionService.executeWorkflow(workflowId, Optional.of("Custom Event"),
        Optional.of(new FlowExecutionRequest()), Optional.empty())).thenReturn(null);

    ResponseEntity<EventResponse> response = internalController.acceptEvent(headers, payload);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void testAcceptTriggerEventInvalidToken() {
  // @formatter:off
  String payload = String.join("", "{",
      "\"id\":\"36965047-1191-4aff-8e17-fe4e8c8e528a\"",
      ",\"type\":\"io.boomerang.event.workflow.trigger\"",
      ",\"source\":\"http://wdc2.cloud.boomerangplatform.net/listener/event\"",
      ",\"specversion\":\"1.0\"",
      ",\"datacontenttype\":\"application/json\"",
      ",\"subject\":\"/workflow/5f7f8cf69a7d401d9e584c90/topic/foobar\"",
      ",\"token\":\"invalid\"",
      ",\"time\":\"2022-04-30T11:33:22Z\"",
      "}");
  
  // @formatter:on

    when(executionService.executeWorkflow(workflowId, Optional.of("Custom Event"),
        Optional.of(new FlowExecutionRequest()), Optional.empty())).thenReturn(null);

    ResponseEntity<EventResponse> response = internalController.acceptEvent(headers, payload);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void testAcceptWFEEvent() {
    // @formatter:off
    String payload = String.join("", "{",
        "\"id\":\"36965047-1191-4aff-8e17-fe4e8c8e528a\"",
        ",\"type\":\"io.boomerang.event.workflow.wfe\"",
        ",\"source\":\"http://wdc2.cloud.boomerangplatform.net/listener/event\"",
        ",\"specversion\":\"1.0\"",
        ",\"status\":\"success\"",
        ",\"datacontenttype\":\"application/json\"",
        ",\"subject\":\"/workflow/5f7f8cf69a7d401d9e584c90/activity/cb4007aaf8b79b41ad598e25/topic/foobar\"",
        ",\"token\":\"RXgGaXBzdW0gZG9sb3Ih\"",
        ",\"time\":\"2022-05-06T12:45:15Z\"",
        "}");
    // @formatter:on

    when(taskService.updateTaskActivityForTopic(anyString(), anyString())).thenReturn(List.of());

    ResponseEntity<EventResponse> response = internalController.acceptEvent(headers, payload);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void testAcceptCancelEvent() {
    // @formatter:off
    String payload = String.join("", "{",
        "\"id\":\"36965047-1191-4aff-8e17-fe4e8c8e528a\"",
        ",\"type\":\"io.boomerang.event.workflow.cancel\"",
        ",\"source\":\"http://wdc2.cloud.boomerangplatform.net/listener/event\"",
        ",\"specversion\":\"1.0\"",
        ",\"datacontenttype\":\"application/json\"",
        ",\"subject\":\"/workflow/5f7f8cf69a7d401d9e584c90/activity/cb4007aaf8b79b41ad598e25\"",
        ",\"token\":\"RXgGaXBzdW0gZG9sb3Ih\"",
        ",\"time\":\"2022-04-30T11:33:22Z\"",
        "}");
    // @formatter:on

    ResponseEntity<EventResponse> response = internalController.acceptEvent(headers, payload);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }
}
