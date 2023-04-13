package io.boomerang.tests.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
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
import io.boomerang.controller.TeamController;
import io.boomerang.misc.FlowTests;
import io.boomerang.model.WorkflowSummary;
import io.boomerang.v4.data.entity.TeamEntity;
import io.boomerang.v4.data.model.CurrentQuotas;
import io.boomerang.v4.data.model.Quotas;
import io.boomerang.v4.data.model.TeamParameter;
import io.boomerang.v4.model.TeamRequest;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@WithMockUser(roles = {"admin"})
@WithUserDetails("mdroy@us.ibm.com")
public class TeamControllerTests extends FlowTests {

  @Autowired
  private TeamController controller;

  @Test
  public void testGetTeams() {
    Assertions.assertEquals(3, controller.getTeams().size());

    Assertions.assertEquals(Integer.valueOf(15),
        controller.getTeams().get(0).getQuotas().getMaxWorkflowCount());
    Assertions.assertEquals(Integer.valueOf(150),
        controller.getTeams().get(0).getQuotas().getMaxWorkflowExecutionMonthly());
    Assertions.assertEquals(Integer.valueOf(10),
        controller.getTeams().get(0).getQuotas().getMaxWorkflowStorage());
    Assertions.assertEquals(Integer.valueOf(60),
        controller.getTeams().get(0).getQuotas().getMaxWorkflowExecutionTime());
    Assertions.assertEquals(Integer.valueOf(2),
        controller.getTeams().get(0).getQuotas().getMaxConcurrentWorkflows());

    Assertions.assertEquals(Integer.valueOf(9),
        controller.getTeams().get(0).getWorkflowQuotas().getCurrentWorkflowCount());
    Assertions.assertEquals(Integer.valueOf(3),
        controller.getTeams().get(0).getWorkflowQuotas().getCurrentConcurrentWorkflows());
    Assertions.assertEquals(Integer.valueOf(0),
        controller.getTeams().get(0).getWorkflowQuotas().getCurrentWorkflowExecutionMonthly());
    Assertions.assertEquals(Integer.valueOf(0),
        controller.getTeams().get(0).getWorkflowQuotas().getCurrentWorkflowsPersistentStorage());
    Assertions.assertEquals(firstOfNextMonth(),
        controller.getTeams().get(0).getWorkflowQuotas().getMonthlyResetDate());
  }

  @Test
  public void testCreateFlowTeam() {
    TeamRequest request = new TeamRequest();
    request.setName("WDC2 ISE Dev");
    request.setCreatedGroupId("5cedb53261a23a0001e4c1b6");

    controller.createCiTeam(request);

    Assertions.assertEquals(4, controller.getTeams().size());

    Assertions.assertEquals("WDC2 ISE Dev", controller.getTeams().get(3).getName());
    Assertions.assertEquals(Integer.valueOf(10),
        controller.getTeams().get(3).getQuotas().getMaxWorkflowCount());
    Assertions.assertEquals(Integer.valueOf(4),
        controller.getTeams().get(3).getQuotas().getMaxConcurrentWorkflows());
    Assertions.assertEquals(Integer.valueOf(100),
        controller.getTeams().get(3).getQuotas().getMaxWorkflowExecutionMonthly());
    Assertions.assertEquals(Integer.valueOf(5),
        controller.getTeams().get(3).getQuotas().getMaxWorkflowStorage());
    Assertions.assertEquals(Integer.valueOf(30),
        controller.getTeams().get(3).getQuotas().getMaxWorkflowExecutionTime());
    Assertions.assertEquals(Integer.valueOf(0),
        controller.getTeams().get(3).getWorkflowQuotas().getCurrentConcurrentWorkflows());
    Assertions.assertEquals(Integer.valueOf(0),
        controller.getTeams().get(3).getWorkflowQuotas().getCurrentWorkflowCount());
    Assertions.assertEquals(Integer.valueOf(0),
        controller.getTeams().get(3).getWorkflowQuotas().getCurrentWorkflowExecutionMonthly());
    Assertions.assertEquals(Integer.valueOf(0),
        controller.getTeams().get(3).getWorkflowQuotas().getCurrentWorkflowsPersistentStorage());
    Assertions.assertEquals(firstOfNextMonth(),
        controller.getTeams().get(3).getWorkflowQuotas().getMonthlyResetDate());
  }

  @Test
  public void testGetAllTeamProperties() {
    List<TeamParameter> configs =
        controller.getAllTeamProperties("5d1a1841f6ca2c00014c4309");
    Assertions.assertEquals(0, configs.size());

    List<TeamParameter> configs2 =
        controller.getAllTeamProperties("5d1a1841f6ca2c00014c4302");
    Assertions.assertEquals(1, configs2.size());
  }

  @Test
  public void testDeleteTeamProperty() {
    controller.deleteTeamProperty("5d1a1841f6ca2c00014c4302",
        "df5f5749-4d30-41c3-803e-56b54b768407");
    Assertions.assertEquals(0, controller.getAllTeamProperties("5d1a1841f6ca2c00014c4302").size());
  }

  @Test
  public void testUpdateTeamProperty() {
    Assertions.assertEquals(null,
        controller.getAllTeamProperties("5d1a1841f6ca2c00014c4302").get(0).getValue());

    TeamParameter property = new TeamParameter();
    property.setId("df5f5749-4d30-41c3-803e-56b54b768407");
    property.setValue("Updated Value");

    List<TeamParameter> updatedConfigs = controller.updateTeamProperty(
        "5d1a1841f6ca2c00014c4302", property, "df5f5749-4d30-41c3-803e-56b54b768407");
    Assertions.assertEquals("Updated Value", updatedConfigs.get(0).getValue());
  }

  @Test
  public void testCreateNewTeamProperty() {
    TeamParameter property = new TeamParameter();
    property.setKey("dylan.new.key");
    property.setValue("Dylan's New Value");

    TeamParameter newConfig =
        controller.createNewTeamProperty("5d1a1841f6ca2c00014c4309", property);
    TeamParameter newConfig2 =
        controller.createNewTeamProperty("5d1a1841f6ca2c00014c4302", property);

    Assertions.assertEquals("dylan.new.key", newConfig.getKey());
    Assertions.assertEquals("Dylan's New Value", newConfig.getValue());

    Assertions.assertEquals("dylan.new.key", newConfig2.getKey());
    Assertions.assertEquals("Dylan's New Value", newConfig2.getValue());
  }
  
  @Test
  public void testCreateNewTeamPropertyPassword() {
    TeamParameter property = new TeamParameter();
    property.setKey("dylan.new.key");
    property.setValue("Sensitive data");
    property.setType("password");

    TeamParameter newConfig =
        controller.createNewTeamProperty("5d1a1841f6ca2c00014c4309", property);
    
    Assertions.assertNull(newConfig.getValue());
   }

  @Test
  public void testGetTeamQuotas() {
    CurrentQuotas quotas = controller.getTeamQuotas("5d1a1841f6ca2c00014c4309");
    Assertions.assertEquals(Integer.valueOf(15), quotas.getMaxWorkflowCount());
    Assertions.assertEquals(Integer.valueOf(150), quotas.getMaxWorkflowExecutionMonthly());
    Assertions.assertEquals(Integer.valueOf(10), quotas.getMaxWorkflowStorage());
    Assertions.assertEquals(Integer.valueOf(60), quotas.getMaxWorkflowExecutionTime());
    Assertions.assertEquals(Integer.valueOf(2), quotas.getMaxConcurrentWorkflows());

    Assertions.assertEquals(Integer.valueOf(9), quotas.getCurrentWorkflowCount());
    Assertions.assertEquals(Integer.valueOf(3), quotas.getCurrentConcurrentWorkflows());
    Assertions.assertEquals(Integer.valueOf(0), quotas.getCurrentWorkflowExecutionMonthly());
    Assertions.assertEquals(Integer.valueOf(0), quotas.getCurrentWorkflowsPersistentStorage());
    Assertions.assertEquals(firstOfNextMonth(), quotas.getMonthlyResetDate());

    Assertions.assertEquals("5d1a1841f6ca2c00014c4309", controller.getTeams().get(0).getId());
    Assertions.assertEquals(quotas.getMaxWorkflowCount(),
        controller.getTeams().get(0).getQuotas().getMaxWorkflowCount());
    Assertions.assertEquals(quotas.getMaxConcurrentWorkflows(),
        controller.getTeams().get(0).getQuotas().getMaxConcurrentWorkflows());
    Assertions.assertEquals(quotas.getMaxWorkflowExecutionMonthly(),
        controller.getTeams().get(0).getQuotas().getMaxWorkflowExecutionMonthly());
    Assertions.assertEquals(quotas.getMaxWorkflowExecutionTime(),
        controller.getTeams().get(0).getQuotas().getMaxWorkflowExecutionTime());
    Assertions.assertEquals(quotas.getMaxWorkflowStorage(),
        controller.getTeams().get(0).getQuotas().getMaxWorkflowStorage());

  }

  @Test
  public void testResetTeamQuotas() {
    CurrentQuotas previousQuotas = controller.getTeamQuotas("5d1a1841f6ca2c00014c4309");

    Assertions.assertEquals(Integer.valueOf(15), previousQuotas.getMaxWorkflowCount());
    Assertions.assertEquals(Integer.valueOf(150), previousQuotas.getMaxWorkflowExecutionMonthly());
    Assertions.assertEquals(Integer.valueOf(10), previousQuotas.getMaxWorkflowStorage());
    Assertions.assertEquals(Integer.valueOf(60), previousQuotas.getMaxWorkflowExecutionTime());
    Assertions.assertEquals(Integer.valueOf(2), previousQuotas.getMaxConcurrentWorkflows());

    controller.resetTeamQuotas("5d1a1841f6ca2c00014c4309");

    CurrentQuotas updatedQuotas = controller.getTeamQuotas("5d1a1841f6ca2c00014c4309");

    Assertions.assertEquals(Integer.valueOf(10), updatedQuotas.getMaxWorkflowCount());
    Assertions.assertEquals(Integer.valueOf(100), updatedQuotas.getMaxWorkflowExecutionMonthly());
    Assertions.assertEquals(Integer.valueOf(5), updatedQuotas.getMaxWorkflowStorage());
    Assertions.assertEquals(Integer.valueOf(30), updatedQuotas.getMaxWorkflowExecutionTime());
    Assertions.assertEquals(Integer.valueOf(4), updatedQuotas.getMaxConcurrentWorkflows());

    Assertions.assertEquals(Integer.valueOf(9), updatedQuotas.getCurrentWorkflowCount());
    Assertions.assertEquals(Integer.valueOf(3), updatedQuotas.getCurrentConcurrentWorkflows());
    Assertions.assertEquals(Integer.valueOf(0), updatedQuotas.getCurrentWorkflowExecutionMonthly());
    Assertions.assertEquals(Integer.valueOf(0),
        updatedQuotas.getCurrentWorkflowsPersistentStorage());
    Assertions.assertEquals(firstOfNextMonth(), updatedQuotas.getMonthlyResetDate());
  }

  @Test
  public void testUpdateQuotas() {
    CurrentQuotas current = controller.getTeamQuotas("5d1a1841f6ca2c00014c4303"); // team3.json
    Assertions.assertEquals(Integer.valueOf(10), current.getMaxWorkflowCount());
    Assertions.assertEquals(Integer.valueOf(4), current.getMaxConcurrentWorkflows());
    Assertions.assertEquals(Integer.valueOf(100), current.getMaxWorkflowExecutionMonthly());
    Assertions.assertEquals(Integer.valueOf(30), current.getMaxWorkflowExecutionTime());
    Assertions.assertEquals(Integer.valueOf(5), current.getMaxWorkflowStorage());

    Quotas quotas = new Quotas();
    quotas.setMaxWorkflowCount(20);
    quotas.setMaxConcurrentWorkflows(8);
    quotas.setMaxWorkflowExecutionMonthly(200);
    quotas.setMaxWorkflowExecutionTime(60);
    quotas.setMaxWorkflowStorage(10);

    Quotas updateQuotas = controller.updateTeamQuotas("5d1a1841f6ca2c00014c4303", quotas);
    Assertions.assertEquals(Integer.valueOf(20), updateQuotas.getMaxWorkflowCount());
    Assertions.assertEquals(Integer.valueOf(8), updateQuotas.getMaxConcurrentWorkflows());
    Assertions.assertEquals(Integer.valueOf(200), updateQuotas.getMaxWorkflowExecutionMonthly());
    Assertions.assertEquals(Integer.valueOf(60), updateQuotas.getMaxWorkflowExecutionTime());
    Assertions.assertEquals(Integer.valueOf(10), updateQuotas.getMaxWorkflowStorage());

    CurrentQuotas updated = controller.getTeamQuotas("5d1a1841f6ca2c00014c4303");
    Assertions.assertEquals(updateQuotas.getMaxWorkflowCount(), updated.getMaxWorkflowCount());
    Assertions.assertEquals(updateQuotas.getMaxConcurrentWorkflows(),
        updated.getMaxConcurrentWorkflows());
    Assertions.assertEquals(updateQuotas.getMaxWorkflowExecutionMonthly(),
        updated.getMaxWorkflowExecutionMonthly());
    Assertions.assertEquals(updateQuotas.getMaxWorkflowExecutionTime(),
        updated.getMaxWorkflowExecutionTime());
    Assertions.assertEquals(updateQuotas.getMaxWorkflowStorage(), updated.getMaxWorkflowStorage());
  }

  @Test
  public void testUpdateQuotasForTeam() {
    Quotas quotas = new Quotas();
    quotas.setMaxWorkflowCount(25);
    quotas.setMaxConcurrentWorkflows(11);
    quotas.setMaxWorkflowExecutionMonthly(250);
    quotas.setMaxWorkflowExecutionTime(74);
    quotas.setMaxWorkflowStorage(8);

    Quotas newQuotas = controller.updateQuotasForTeam("5d1a1841f6ca2c00014c4302", quotas);

    Assertions.assertEquals(Integer.valueOf(25), newQuotas.getMaxWorkflowCount());
    Assertions.assertEquals(Integer.valueOf(11), newQuotas.getMaxConcurrentWorkflows());
    Assertions.assertEquals(Integer.valueOf(250), newQuotas.getMaxWorkflowExecutionMonthly());
    Assertions.assertEquals(Integer.valueOf(74), newQuotas.getMaxWorkflowExecutionTime());
    Assertions.assertEquals(Integer.valueOf(8), newQuotas.getMaxWorkflowStorage());

    CurrentQuotas updated = controller.getTeamQuotas("5d1a1841f6ca2c00014c4302");
    Assertions.assertEquals(newQuotas.getMaxWorkflowCount(), updated.getMaxWorkflowCount());
    Assertions.assertEquals(newQuotas.getMaxConcurrentWorkflows(),
        updated.getMaxConcurrentWorkflows());
    Assertions.assertEquals(newQuotas.getMaxWorkflowExecutionMonthly(),
        updated.getMaxWorkflowExecutionMonthly());
    Assertions.assertEquals(newQuotas.getMaxWorkflowExecutionTime(),
        updated.getMaxWorkflowExecutionTime());
    Assertions.assertEquals(newQuotas.getMaxWorkflowStorage(), updated.getMaxWorkflowStorage());
  }

  @Test
  public void testGetDefaultQuotas() {
    Quotas quota = controller.getDefaultQuotas();
    Assertions.assertEquals(Integer.valueOf(4), quota.getMaxConcurrentWorkflows());
    Assertions.assertEquals(Integer.valueOf(10), quota.getMaxWorkflowCount());
    Assertions.assertEquals(Integer.valueOf(100), quota.getMaxWorkflowExecutionMonthly());
    Assertions.assertEquals(Integer.valueOf(30), quota.getMaxWorkflowExecutionTime());
    Assertions.assertEquals(Integer.valueOf(5), quota.getMaxWorkflowStorage());
  }

  @Test
  public void deactivateTeam() {
    TeamEntity entity = controller.deactivateTeam("5d1a1841f6ca2c00014c4309");
    Assertions.assertEquals(false, entity.getIsActive());
    Assertions.assertEquals(2, controller.getTeams().size());
  }
  

  @Test
  public void testInternalTeamWorkflows() {
    List<WorkflowSummary> summaryList = controller.getTeamWorkflows("5d1a1841f6ca2c00014c4309");

     assertNotNull(summaryList);
     assertEquals(9, summaryList.size());

  }

  private Date firstOfNextMonth() {
    Calendar nextMonth = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    nextMonth.add(Calendar.MONTH, 1);
    nextMonth.set(Calendar.DAY_OF_MONTH, 1);
    nextMonth.set(Calendar.HOUR_OF_DAY, 0);
    nextMonth.set(Calendar.MINUTE, 0);
    nextMonth.set(Calendar.SECOND, 0);
    nextMonth.set(Calendar.MILLISECOND, 0);
    return nextMonth.getTime();
  }
}
