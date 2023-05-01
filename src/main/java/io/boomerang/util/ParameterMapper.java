package io.boomerang.util;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import io.boomerang.v4.model.KeyValuePair;

public class ParameterMapper {
  
  /*
   * Converts a Parameter Map to a KeyValuePair List.
   * This makes it safe for MongoDB to store the parameters if there are dots in the key.
   * 
   * Will check if parameterMap is null and return a new LinkedList.
   * 
   * @param the parameter map, typically from a model
   * @return the parameter keyvalue list, typically to save into an entity.
   */
  public static List<KeyValuePair> mapToKeyValuePairList(Map<String, String> parameterMap) {
    List<KeyValuePair> parameterList = new LinkedList<>();
    if (parameterMap != null) {
      for (Map.Entry<String, String> entry : parameterMap.entrySet()) {
        String key = entry.getKey();
        String value = parameterMap.get(key);
        KeyValuePair prop = new KeyValuePair();
        prop.setKey(key);
        prop.setValue(value);
        parameterList.add(prop);
      }
    }
    return parameterList;
  }
  
  public static Map<String, String> keyValuePairListToMap(List<KeyValuePair> parameterList) {
    Map<String, String> parameterMap = new HashMap<>();
    if (parameterList != null) {
      parameterList.stream().forEach(p -> {
        parameterMap.put(p.getKey(), p.getValue());
      });
    }
    return parameterMap;
  }

}
