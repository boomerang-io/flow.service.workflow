package io.boomerang.tests.controller;

import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import io.boomerang.controller.ExecutionController;
import io.boomerang.error.BoomerangException;
import io.boomerang.misc.FlowTests;
import io.boomerang.model.FlowActivity;
import io.boomerang.model.FlowExecutionRequest;
import io.boomerang.mongo.model.FlowTriggerEnum;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
@WithMockUser(roles = {"admin"})
@WithUserDetails("mdroy@us.ibm.com")
public class ExecutionControllerTests extends FlowTests {

  @Autowired
  protected ExecutionController executionController;

  @Test
  public void testExecuteWorkflowNotActive() {
    String workflowId = "5d1a188af6ca2c00014c4369"; // workflow13.json

    FlowActivity activity = executionController.executeWorkflow(workflowId,
        Optional.of(FlowTriggerEnum.manual.toString()), Optional.of(new FlowExecutionRequest()));

    Assertions.assertNull(activity);
  }

  @Test
  public void testExecuteWorkflowExceedQuotaMax() {
    try {
      executionController.executeWorkflow("5d1a188af6ca2c00014c4314", // workflow1.json
          Optional.of(FlowTriggerEnum.manual.toString()), Optional.of(new FlowExecutionRequest()));
    } catch (BoomerangException e) {
      Assertions.assertEquals(429, e.getCode());
      Assertions.assertEquals("TOO_MANY_REQUESTS", e.getDescription());
      Assertions.assertEquals(HttpStatus.TOO_MANY_REQUESTS, e.getHttpStatus());
    }
  }

}
