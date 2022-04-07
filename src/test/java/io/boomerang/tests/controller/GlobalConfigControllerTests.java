package io.boomerang.tests.controller;

<<<<<<< HEAD:src/test/java/net/boomerangplatform/miscs/controller/GlobalConfigControllerTests.java
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;
=======
import java.util.List;
import org.junit.jupiter.api.Assertions;
>>>>>>> main:src/test/java/io/boomerang/tests/controller/GlobalConfigControllerTests.java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
<<<<<<< HEAD:src/test/java/net/boomerangplatform/miscs/controller/GlobalConfigControllerTests.java
import net.boomerangplatform.controller.GlobalConfigController;
import net.boomerangplatform.misc.FlowTests;
import net.boomerangplatform.service.config.model.GlobalConfig;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
=======
import io.boomerang.controller.GlobalConfigController;
import io.boomerang.misc.FlowTests;
import io.boomerang.service.config.model.GlobalConfig;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
>>>>>>> main:src/test/java/io/boomerang/tests/controller/GlobalConfigControllerTests.java
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
    Assertions.assertEquals(1, allConfigs.size());

    GlobalConfig updatedConfig = allConfigs.get(0);
    updatedConfig.setDescription("New Description");

    updatedConfig =
        this.globalConfigController.updateGlobalConfig(updatedConfig, updatedConfig.getId());

    Assertions.assertEquals("New Description", updatedConfig.getDescription());

    this.globalConfigController.deleteConfiguration(updatedConfig.getId());

  }
}
