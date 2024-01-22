package io.boomerang.service;

import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

public interface TaskRunService {

  StreamingResponseBody streamLog(String taskRunId);

}
