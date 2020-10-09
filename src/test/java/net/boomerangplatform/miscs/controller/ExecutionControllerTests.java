package net.boomerangplatform.miscs.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import net.boomerangplatform.Application;
import net.boomerangplatform.controller.ExecutionController;
import net.boomerangplatform.error.BoomerangException;
import net.boomerangplatform.misc.FlowTests;
import net.boomerangplatform.model.FlowActivity;
import net.boomerangplatform.model.FlowExecutionRequest;
import net.boomerangplatform.mongo.model.FlowTriggerEnum;
import net.boomerangplatform.tests.MongoConfig;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {Application.class, MongoConfig.class})
@ActiveProfiles("test")
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

    assertNull(activity);
  }
  
  @Test
  public void testExecuteWorkflowExceedQuotaMax() {
    try {
      executionController.executeWorkflow("5d1a188af6ca2c00014c4314", // workflow1.json
          Optional.of(FlowTriggerEnum.manual.toString()), Optional.of(new FlowExecutionRequest()));
    } catch (BoomerangException e) {
      assertEquals(429, e.getCode());
      assertEquals("TOO_MANY_REQUESTS", e.getDescription());
      assertEquals(HttpStatus.TOO_MANY_REQUESTS, e.getHttpStatus());
    }
  }

}
