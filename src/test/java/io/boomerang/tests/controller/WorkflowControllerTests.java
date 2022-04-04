package io.boomerang.tests.controller;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.boomerang.controller.InternalController;
import io.boomerang.controller.WorkflowController;
import io.boomerang.misc.FlowTests;
import io.boomerang.model.FlowWorkflowRevision;
import io.boomerang.model.GenerateTokenResponse;
import io.boomerang.model.RevisionResponse;
import io.boomerang.model.WorkflowExport;
import io.boomerang.model.WorkflowShortSummary;
import io.boomerang.model.WorkflowSummary;
import io.boomerang.model.projectstormv5.RestConfig;
import io.boomerang.mongo.entity.RevisionEntity;
import io.boomerang.mongo.entity.WorkflowEntity;
import io.boomerang.mongo.model.ActivityStorage;
import io.boomerang.mongo.model.Storage;
import io.boomerang.mongo.model.TaskConfigurationNode;
import io.boomerang.mongo.model.TriggerEvent;
import io.boomerang.mongo.model.TriggerScheduler;
import io.boomerang.mongo.model.Triggers;
import io.boomerang.mongo.model.WorkflowConfiguration;
import io.boomerang.mongo.model.WorkflowProperty;
import io.boomerang.mongo.model.WorkflowScope;
import io.boomerang.mongo.model.WorkflowStatus;


@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@ActiveProfiles("local")
@WithMockUser(roles = {"admin"})
@WithUserDetails("mdroy@us.ibm.com")
public class WorkflowControllerTests extends FlowTests {

  @Autowired
  private WorkflowController controller;

  @Autowired
  private InternalController internalController;

  @Test
  public void testInternalWorkflowListing() {
    List<WorkflowShortSummary> summaryList = internalController.getAllWorkflows();

    Assertions.assertNotNull(summaryList);
    Assertions.assertEquals(18, summaryList.size());
  }

  @Test
  public void testGetWorkflowLatestVersion() {

    FlowWorkflowRevision entity = controller.getWorkflowLatestVersion("5d1a188af6ca2c00014c4314");

    Assertions.assertEquals("5d1a188af6ca2c00014c4314", entity.getWorkFlowId());
  }

  @Test
  public void testGetWorkflowVersion() {
    FlowWorkflowRevision entity = controller.getWorkflowVersion("5d1a188af6ca2c00014c4314", 1L);
    Assertions.assertEquals(1L, entity.getVersion());
    Assertions.assertEquals("5d1a188af6ca2c00014c4314", entity.getWorkFlowId());
  }

  @Test
  public void testGetWorkflowWithId() {
    WorkflowSummary summary = controller.getWorkflowWithId("5d1a188af6ca2c00014c4314");
    Assertions.assertEquals("5d1a188af6ca2c00014c4314", summary.getId());
  }

  @Test
  public void testInsertWorkflow() {
    WorkflowSummary entity = new WorkflowSummary();
    entity.setName("TestWorkflow");
    entity.setStatus(WorkflowStatus.deleted);
    WorkflowSummary summary = controller.insertWorkflow(entity);
    Assertions.assertEquals("TestWorkflow", summary.getName());
    Assertions.assertEquals(WorkflowStatus.active, summary.getStatus());

  }

  @Test
  public void testinsertWorkflow() throws IOException {

    File resource = new ClassPathResource("json/updated-model-v5.json").getFile();
    String json = new String(Files.readAllBytes(resource.toPath()));
    ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    FlowWorkflowRevision revision = objectMapper.readValue(json, FlowWorkflowRevision.class);
    revision.setMarkdown("test");

    FlowWorkflowRevision revisionEntity =
        controller.insertWorkflow("5d1a188af6ca2c00014c4314", revision);
    Assertions.assertEquals(2L, revisionEntity.getVersion());
    Assertions.assertEquals("test", revisionEntity.getMarkdown());
  }

  @Test
  public void testUpdateWorkflow() {
    WorkflowSummary entity = new WorkflowSummary();
    entity.setId("5d1a188af6ca2c00014c4314");
    entity.setName("TestUpdateWorkflow");
    entity.setStorage(new Storage());
    entity.getStorage().setActivity(new ActivityStorage());
    WorkflowSummary updatedEntity = controller.updateWorkflow(entity);
    Assertions.assertEquals("5d1a188af6ca2c00014c4314", updatedEntity.getId());
    Assertions.assertEquals("TestUpdateWorkflow", updatedEntity.getName());
  }

  @Test
  public void testUpdateWorkflowProperties() {

    WorkflowProperty property = new WorkflowProperty();
    property.setKey("testKey");
    property.setDescription("testDescription");
    property.setLabel("testLabel");
    property.setRequired(true);
    property.setType("testing");

    List<WorkflowProperty> properties = new ArrayList<>();
    properties.add(property);

    WorkflowEntity entity =
        controller.updateWorkflowProperties("5d1a188af6ca2c00014c4314", properties);

    Assertions.assertNotNull(entity.getProperties());
    Assertions.assertEquals(1, entity.getProperties().size());
    Assertions.assertEquals("testDescription", entity.getProperties().get(0).getDescription());

  }

  @Test
  public void testExportWorkflow() {
    ResponseEntity<InputStreamResource> export =
        controller.exportWorkflow("5d1a188af6ca2c00014c4314");
    Assertions.assertEquals(HttpStatus.OK, export.getStatusCode());
  }

  @Test
  public void testImportWorkflowUpdate() throws IOException {
    WorkflowExport export = new WorkflowExport();
    export.setDescription("testImportDescription");
    export.setName("testImportName");
    export.setId("5d7177af2c57250007e3d7a1");
    export.setStorage(new Storage());
    export.getStorage().setActivity(new ActivityStorage());
    TriggerEvent manual = new TriggerEvent();
    manual.setEnable(true);

    Triggers triggers = new Triggers();
    triggers.setManual(manual);

    export.setTriggers(triggers);

    List<TaskConfigurationNode> nodes = new ArrayList<>();

    WorkflowConfiguration config = new WorkflowConfiguration();
    config.setNodes(nodes);

    File resource = new ClassPathResource("json/json-sample.json").getFile();
    String json = new String(Files.readAllBytes(resource.toPath()));
    ObjectMapper objectMapper = new ObjectMapper();
    RevisionEntity revision = objectMapper.readValue(json, RevisionEntity.class);

    export.setLatestRevision(revision);

    controller.importWorkflow(export, true, "", WorkflowScope.team);

    WorkflowSummary summary = controller.getWorkflowWithId("5d1a188af6ca2c00014c4314");
    Assertions.assertEquals("test", summary.getDescription());
  }

  @Test
  public void testImportWorkflow() throws IOException {

    File resource = new ClassPathResource("scenarios/import/import-sample.json").getFile();
    String json = new String(Files.readAllBytes(resource.toPath()));
    ObjectMapper objectMapper = new ObjectMapper();
    WorkflowExport importedWorkflow = objectMapper.readValue(json, WorkflowExport.class);
    controller.importWorkflow(importedWorkflow, false, "", WorkflowScope.team);
    Assertions.assertTrue(true);
  }

  @Test
  public void testGenerateWebhookToken() {

    GenerateTokenResponse response = controller.createToken("5d1a188af6ca2c00014c4314", "Token");
    Assertions.assertNotEquals("", response.getToken());
  }

  @Test
  public void testDeleteWorkflow() {
    controller.deleteWorkflowWithId("5d1a188af6ca2c00014c4314");
    Assertions.assertEquals(WorkflowStatus.deleted,
        controller.getWorkflowWithId("5d1a188af6ca2c00014c4314").getStatus());
  }

  @Test
  public void testViewChangeLog() {
    List<RevisionResponse> response =
        controller.viewChangelog(getOptionalString("5d1a188af6ca2c00014c4314"),
            getOptionalOrder(Direction.ASC), getOptionalString("sort"), 0, 2147483647);
    Assertions.assertEquals(1, response.size());
    Assertions.assertEquals(1, response.get(0).getVersion());
  }

  @Test
  public void testUpdateWorkflowTriggers() {

    TriggerScheduler scheduler = new TriggerScheduler();
    scheduler.setEnable(true);

    TriggerEvent webhook = new TriggerEvent();
    webhook.setEnable(false);
    webhook.setToken("token");

    WorkflowSummary entity = controller.getWorkflowWithId("5d1a188af6ca2c00014c4314");
    Assertions.assertNotNull(entity.getTriggers());
    Assertions.assertNotNull(entity.getTriggers().getWebhook());
    Assertions.assertEquals(false, entity.getTriggers().getScheduler().getEnable());
    Assertions.assertEquals(true, entity.getTriggers().getWebhook().getEnable());
    Assertions.assertEquals("A5DF2F840C0DFF496D516B4F75BD947C9BC44756A8AE8571FC45FCB064323641",
        entity.getTriggers().getWebhook().getToken());


    entity.getTriggers().setScheduler(scheduler);
    entity.getTriggers().setWebhook(webhook);

    WorkflowSummary updatedEntity = controller.updateWorkflow(entity);

    Assertions.assertEquals("5d1a188af6ca2c00014c4314", updatedEntity.getId());

    Assertions.assertEquals(true, updatedEntity.getTriggers().getScheduler().getEnable());
    Assertions.assertEquals(false, updatedEntity.getTriggers().getWebhook().getEnable());
    Assertions.assertEquals("token",
        updatedEntity.getTriggers().getWebhook().getToken());
  }

  @Test
  @Disabled
  public void testUpdateWorkflowTriggerNull() {

    WorkflowSummary entity = controller.getWorkflowWithId("5d1a188af6ca2c00014c4314");
    Assertions.assertEquals(false, entity.getTriggers().getScheduler().getEnable());
    entity.setTriggers(null);
    Assertions.assertNull(entity.getTriggers());

    WorkflowSummary updatedEntity = controller.updateWorkflow(entity);

    Assertions.assertEquals("5d1a188af6ca2c00014c4314", updatedEntity.getId());
    Assertions.assertEquals(false, updatedEntity.getTriggers().getScheduler().getEnable());

  }

  @Test
  @Disabled
  public void testUpdateWorkflowTriggerEvent() {

  }

  Optional<String> getOptionalString(String string) {
    return Optional.of(string);
  }

  Optional<Direction> getOptionalOrder(Direction direction) {
    return Optional.of(direction);
  }

  @Test
  public void testMissingTemplateVersionRevision() {

    FlowWorkflowRevision entity = controller.getWorkflowVersion("5d7177af2c57250007e3d7a1", 1l);
    Assertions.assertNotNull(entity);
    verifyTemplateVersions(entity);
  }

  @Test
  public void testMissingTemplateVersionLatestRevision() {

    FlowWorkflowRevision entity = controller.getWorkflowLatestVersion("5d7177af2c57250007e3d7a1");
    Assertions.assertNotNull(entity);
    verifyTemplateVersions(entity);
  }

  @Test
  public void testAvaliableParameters() {
    List<String> parameters = controller.getWorkflowParameters("5d1a188af6ca2c00014c4314");
    Assertions.assertEquals(14, parameters.size());
    Assertions.assertEquals("workflow.params.hello", parameters.get(0));
    Assertions.assertEquals("params.hello", parameters.get(1));
    Assertions.assertEquals("system.params.workflow-id", parameters.get(2));
    Assertions.assertEquals("params.workflow-id", parameters.get(3));

  }

  private void verifyTemplateVersions(FlowWorkflowRevision entity) {
    RestConfig config = entity.getConfig();
    for (io.boomerang.model.projectstormv5.ConfigNodes taskNode : config.getNodes()) {
      if (taskNode.getTaskId() != null) {
        Assertions.assertNotNull(taskNode.getTaskVersion());
      }
    }
  }
}
