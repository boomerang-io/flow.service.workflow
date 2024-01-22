package io.boomerang.controller;

import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import io.boomerang.service.TaskRunService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v2/taskrun")
@Tag(name = "TaskRun Management", description = "View, Start, Stop, and Update Status of your Task Runs.")
public class TaskRunV2Controller {
  
  @Autowired
  private TaskRunService taskRunService;

  @GetMapping(value = "/{taskRunId}/log")
  @Operation(summary = "Retrieve a TaskRuns log from a specific WorkflowRun.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  @ResponseBody
  public ResponseEntity<StreamingResponseBody> streamTaskRunLog(
      @Parameter(name = "taskRunId",
      description = "Id of TaskRun",
      required = true) @PathVariable String taskRunId,
      HttpServletResponse response) {
  response.setContentType("text/plain");
  response.setCharacterEncoding("UTF-8");
  return new ResponseEntity<StreamingResponseBody>(taskRunService.streamLog(taskRunId), HttpStatus.OK);
  }
}
