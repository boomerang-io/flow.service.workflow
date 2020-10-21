package net.boomerangplatform.miscs.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import net.boomerangplatform.Application;
import net.boomerangplatform.controller.TaskTemplateController;
import net.boomerangplatform.misc.FlowTests;
import net.boomerangplatform.model.FlowTaskTemplate;
import net.boomerangplatform.mongo.model.ChangeLog;
import net.boomerangplatform.mongo.model.FlowTaskTemplateStatus;
import net.boomerangplatform.mongo.model.Revision;
import net.boomerangplatform.tests.MongoConfig;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {Application.class, MongoConfig.class})
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@ActiveProfiles("local")
@WithMockUser(roles = {"admin"})
@WithUserDetails("mdroy@us.ibm.com")
public class TaskTemplateControllerTests extends FlowTests {

  @Autowired
  private TaskTemplateController controller;

  @Test
  public void testGetTaskTemplateWithId() {
    FlowTaskTemplate template = controller.getTaskTemplateWithId("5bd9d0825a5df954ad5bb5c3");
    assertEquals("5bd9d0825a5df954ad5bb5c3", template.getId());
    assertEquals(1, template.getCurrentVersion());
    assertEquals(true, template.isVerified());

  }

  @Test
  public void testGetAllTaskTemplates() {
    List<FlowTaskTemplate> templates = controller.getAllTaskTemplates();
    assertEquals(6, templates.size());
    assertEquals(1, templates.get(0).getCurrentVersion());
    assertEquals(true, templates.get(0).isVerified());
    assertEquals(false, templates.get(1).isVerified());

  }

  @Test
  public void testInsertTaskTemplate() {
    FlowTaskTemplate entity = new FlowTaskTemplate();
    entity.setDescription("test");

    entity.setName("TestTaskTemplate");
    entity.setNodetype("custom");
    FlowTaskTemplate testTemplate = controller.insertTaskTemplate(entity);
    assertEquals("TestTaskTemplate", testTemplate.getName());
    assertEquals(false, testTemplate.isVerified());

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
    assertEquals(updatedEntity.getId(), entity.getId());
    assertEquals("test", updatedEntity.getDescription());
    // assertEquals(date, updatedEntity.getCreatedDate());
    assertEquals(1, updatedEntity.getCurrentVersion());

    assertNotNull(updatedEntity.getRevisions().get(0).getChangelog().getUserId());
    assertEquals("reason", updatedEntity.getRevisions().get(0).getChangelog().getReason());
    assertEquals(true, updatedEntity.isVerified());
  }

  @Test
  public void testDeleteTaskTemplate() {
    controller.deleteTaskTemplateWithId("5bd9d0825a5df954ad5bb5c3");
    assertNotNull(controller.getTaskTemplateWithId("5bd9d0825a5df954ad5bb5c3"));
    assertEquals(FlowTaskTemplateStatus.inactive,
        controller.getTaskTemplateWithId("5bd9d0825a5df954ad5bb5c3").getStatus());


    controller.activateTaskTemplate("5bd9d0825a5df954ad5bb5c3");
    assertNotNull(controller.getTaskTemplateWithId("5bd9d0825a5df954ad5bb5c3"));
    assertEquals(FlowTaskTemplateStatus.active,
        controller.getTaskTemplateWithId("5bd9d0825a5df954ad5bb5c3").getStatus());

  }

}
