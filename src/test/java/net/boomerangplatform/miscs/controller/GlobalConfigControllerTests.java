package net.boomerangplatform.miscs.controller;

import static org.junit.Assert.assertEquals;
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
import net.boomerangplatform.controller.GlobalConfigController;
import net.boomerangplatform.misc.FlowTests;
import net.boomerangplatform.service.config.model.GlobalConfig;
import net.boomerangplatform.tests.MongoConfig;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {Application.class, MongoConfig.class})
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@ActiveProfiles("local")
@WithMockUser(roles = {"admin"})
@WithUserDetails("mdroy@us.ibm.com")
public class GlobalConfigControllerTests extends FlowTests {

  @Autowired
  private GlobalConfigController globalConfigController;

  @Test
  public void testGlobalConfigUpdate() {
    GlobalConfig newConfig = new GlobalConfig();
    newConfig.setKey("test");
    newConfig.setLabel("Test");
    newConfig.setDescription("Test Description");
    newConfig.setDefaultValue("test");

    globalConfigController.createNewGlobalConfig(newConfig);

    List<GlobalConfig> allConfigs = this.globalConfigController.getAllGlobalConfigurations();
    assertEquals(1, allConfigs.size());

    GlobalConfig updatedConfig = allConfigs.get(0);
    updatedConfig.setDescription("New Description");

    updatedConfig =
        this.globalConfigController.updateGlobalConfig(updatedConfig, updatedConfig.getId());

    assertEquals("New Description", updatedConfig.getDescription());

    this.globalConfigController.deleteConfiguration(updatedConfig.getId());

  }
}
