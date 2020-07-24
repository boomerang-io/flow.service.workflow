package net.boomerangplatform.service.runner.misc;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import net.boomerangplatform.model.controller.Workflow;
import net.boomerangplatform.model.controller.WorkflowStorage;

@Service
public class WorkflowLifecycleService {

  @Value("${controller.createworkflow.url}")
  private String createWorkflowURL;

  @Value("${controller.terminateworkflow.url}")
  private String terminateWorkflowURL;

  @Autowired
  @Qualifier("internalRestTemplate")
  private RestTemplate restTemplate;

  public boolean createFlow(String workflowId, String workflowName, String activityId,
      boolean enableStorage, Map<String, String> properties) {

    final Workflow request = new Workflow();
    request.setWorkflowActivityId(activityId);
    request.setWorkflowName(workflowName);
    request.setWorkflowId(workflowId);

    request.setProperties(properties);

    final WorkflowStorage storage = new WorkflowStorage();

    storage.setEnable(enableStorage);
    request.setWorkflowStorage(storage);

    restTemplate.postForObject(createWorkflowURL, request, String.class);

    return true;
  }

  public boolean terminateFlow(String workflowId, String workflowName, String activityId) {
    final Workflow request = new Workflow();
    request.setWorkflowActivityId(activityId);
    request.setWorkflowName(workflowName);
    request.setWorkflowId(workflowId);
    final WorkflowStorage storage = new WorkflowStorage();
    storage.setEnable(true);
    request.setWorkflowStorage(storage);

    restTemplate.postForObject(terminateWorkflowURL, request, String.class);
    return true;
  }

}
