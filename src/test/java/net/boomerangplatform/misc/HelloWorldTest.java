package net.boomerangplatform.misc;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.junit.Test;

public class HelloWorldTest {

  @Test
  public void testing() {
    String json = "= :";
    System.out.println(json);

    Map<String, String> map = new HashMap<String, String>();
    map.put("json", json);


        
    System.out.println(createConfigMapProp(map));

  }


  protected String createConfigMapProp(Map<String, String> properties) {

    Properties props = new Properties();
    StringWriter propsSW = new StringWriter();
    if (properties != null && !properties.isEmpty()) {
      properties.forEach((key, value) -> {
        String valueStr = value != null ? value : "";

        props.setProperty(key, valueStr);
      });
    }

    try {
      props.store(propsSW, null);

    } catch (IOException ex) {

    }

    return propsSW.toString();
  }

}
