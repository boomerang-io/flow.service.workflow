package net.boomerangplatform.miscs.controller;

import static org.junit.Assert.assertEquals;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import net.boomerangplatform.controller.FeatureController;
import net.boomerangplatform.model.FlowFeatures;
import net.boomerangplatform.tests.AbstractFlowTests;
import net.boomerangplatform.tests.MongoConfig;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {Application.class, MongoConfig.class})
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@ActiveProfiles("local")
@WithMockUser(roles = {"admin"})
@WithUserDetails("mdroy@us.ibm.com")
public class FeatureControllerTests extends AbstractFlowTests {
  @Autowired
  FeatureController controller;

  @Test
  public void testGetFeatures() {
    FlowFeatures features =controller.getFlowFeatures();
    assertEquals(11,features.getFeatures().size());
  }

  @Override
  protected Map<String, List<String>> getData() {
    LinkedHashMap<String, List<String>> data = new LinkedHashMap<>();
    data.put("flow_settings", Arrays.asList("db/flow_settings/setting1.json"));
    return data;
  }

  @Override
  protected String[] getCollections() {
    return new String[] { "flow_settings"};
  }

}
