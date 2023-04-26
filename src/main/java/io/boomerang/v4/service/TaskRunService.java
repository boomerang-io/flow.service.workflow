package io.boomerang.v4.service;

import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

public interface TaskRunService {

  StreamingResponseBody getTaskRunLog(String workflowRunId, String taskRunId);

}
