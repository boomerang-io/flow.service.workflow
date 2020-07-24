package net.boomerangplatform.tests.controller;

import static org.junit.Assert.assertNull;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import net.boomerangplatform.Application;
import net.boomerangplatform.MongoConfig;
import net.boomerangplatform.controller.ExecutionController;
import net.boomerangplatform.model.FlowActivity;
import net.boomerangplatform.model.FlowExecutionRequest;
import net.boomerangplatform.mongo.model.FlowTriggerEnum;
import net.boomerangplatform.tests.FlowTests;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {Application.class, MongoConfig.class})
@SpringBootTest
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
        Optional.of(FlowTriggerEnum.manual), Optional.of(new FlowExecutionRequest()));

    assertNull(activity);
  }

}
