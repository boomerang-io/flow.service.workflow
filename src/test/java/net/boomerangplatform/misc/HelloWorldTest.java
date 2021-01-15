package net.boomerangplatform.misc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


public class HelloWorldTest {

  @Test
  public void hello() throws IOException {

    Map<String, String> map = new HashMap<String, String>();
    map.put("api.admanager.clientId", "clientId");
    map.put("api.admanager.refreshToken", "refreshToken");

    String output = this.getEncodedPropertiesForMap(map);
    System.out.println(output);
  }


  public String getEncodedPropertiesForMap(Map<String, String> map) {
    try {
      Properties properties = new Properties();
      properties.putAll(map);
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      properties.store(outputStream, null);
      String text = outputStream.toString();
      String[] lines = text.split("\\n");

      StringBuilder sb = new StringBuilder();
      for (String line : lines) {
        if (!line.startsWith("#")) {
          sb.append(line + '\n');
        }

      }
      String propertiesFile = sb.toString();
      String encodedString = Base64.getEncoder().encodeToString(propertiesFile.getBytes());
      return encodedString;
    } catch (IOException e) {
      return "";
    }
  }

}
