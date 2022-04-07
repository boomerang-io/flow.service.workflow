package io.boomerang.tests.controller;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import io.boomerang.controller.TaskTemplateController;
import io.boomerang.misc.FlowTests;
import io.boomerang.model.FlowTaskTemplate;
import io.boomerang.model.tekton.TektonTask;
import io.boomerang.mongo.model.ChangeLog;
import io.boomerang.mongo.model.FlowTaskTemplateStatus;
import io.boomerang.mongo.model.Revision;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
@WithMockUser(roles = {"admin"})
@WithUserDetails("mdroy@us.ibm.com")
public class TaskTemplateControllerTests extends FlowTests {

  @Autowired
  private TaskTemplateController controller;

  @Test
  public void testGetTaskTemplateWithId() {
    FlowTaskTemplate template = controller.getTaskTemplateWithId("5bd9d0825a5df954ad5bb5c3");
    Assertions.assertEquals("5bd9d0825a5df954ad5bb5c3", template.getId());
    Assertions.assertEquals(1, template.getCurrentVersion());
    Assertions.assertEquals(true, template.isVerified());

  }

  @Test
  public void testGetAllTaskTemplates() {
    List<FlowTaskTemplate> templates = controller.getAllTaskTemplates(null, null);
    Assertions.assertEquals(6, templates.size());
    Assertions.assertEquals(1, templates.get(0).getCurrentVersion());
    Assertions.assertEquals(true, templates.get(0).isVerified());
    Assertions.assertEquals(false, templates.get(1).isVerified());

  }

  @Test
  public void testInsertTaskTemplate() {
    FlowTaskTemplate entity = new FlowTaskTemplate();
    entity.setDescription("test");

    entity.setName("TestTaskTemplate");
    entity.setNodetype("custom");
    FlowTaskTemplate testTemplate = controller.insertTaskTemplate(entity);
    Assertions.assertEquals("TestTaskTemplate", testTemplate.getName());
    Assertions.assertEquals(false, testTemplate.isVerified());

  }

  @Test
  public void testUpdateTaskTemplate() {
    FlowTaskTemplate entity = new FlowTaskTemplate();

    entity.setId("5bd9d0825a5df954ad5bb5c3");
    entity.setDescription("test");
    entity.setName("TestTaskTemplate");


    Revision revision = new Revision();
    ChangeLog changelog = new ChangeLog();
    changelog.setReason("reason");

    revision.setChangelog(changelog);

    revision.setChangelog(changelog);

    List<Revision> revisions = new ArrayList<>();
    revisions.add(revision);

    entity.setRevisions(revisions);
    FlowTaskTemplate updatedEntity = controller.updateTaskTemplate(entity);
    Assertions.assertEquals(updatedEntity.getId(), entity.getId());
    Assertions.assertEquals("test", updatedEntity.getDescription());
    // Assertions.assertEquals(date, updatedEntity.getCreatedDate());
    Assertions.assertEquals(1, updatedEntity.getCurrentVersion());

    Assertions.assertNotNull(updatedEntity.getRevisions().get(0).getChangelog().getUserId());
    Assertions.assertEquals("reason",
        updatedEntity.getRevisions().get(0).getChangelog().getReason());
    Assertions.assertEquals(true, updatedEntity.isVerified());
  }

  @Test
  public void testDeleteTaskTemplate() {
    controller.deleteTaskTemplateWithId("5bd9d0825a5df954ad5bb5c3");
    Assertions.assertNotNull(controller.getTaskTemplateWithId("5bd9d0825a5df954ad5bb5c3"));
    Assertions.assertEquals(FlowTaskTemplateStatus.inactive,
        controller.getTaskTemplateWithId("5bd9d0825a5df954ad5bb5c3").getStatus());


    controller.activateTaskTemplate("5bd9d0825a5df954ad5bb5c3");
    Assertions.assertNotNull(controller.getTaskTemplateWithId("5bd9d0825a5df954ad5bb5c3"));
    Assertions.assertEquals(FlowTaskTemplateStatus.active,
        controller.getTaskTemplateWithId("5bd9d0825a5df954ad5bb5c3").getStatus());

  }

  @Test
  public void testLatestTaskYaml() {
    String templateId = "5bd9d0825a5df954ad5bb5c3";

    TektonTask task = this.controller.getTaskTemplateYamlWithId(templateId);
    Assertions.assertNotNull(task);
    Assertions.assertNotNull(task.getSpec());
    Assertions.assertNotNull(task.getSpec().getParams());
  }

  @Test
  public void testLatestTaskYamlWithRevision() {
    String templateId = "5bd9d0825a5df954ad5bb5c3";

    TektonTask task = this.controller.getTaskTemplateYamlWithIdandRevision(templateId, 1);
    Assertions.assertNotNull(task.getSpec().getParams());
  }

}
