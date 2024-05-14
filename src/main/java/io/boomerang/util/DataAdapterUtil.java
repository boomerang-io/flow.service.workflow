package io.boomerang.util;

import java.util.List;
import io.boomerang.model.AbstractParam;
import io.boomerang.model.ref.ParamSpec;
import io.boomerang.model.ref.RunParam;

public class DataAdapterUtil {
	public enum FieldType {
		PASSWORD("password");

		private final String value;

		private FieldType(String value) {
			this.value = value;
		}

		public String value() {
			return value;
		}
	}
	
	/**
	 * Method for filtering sensitive data from AbstractConfigs (e.g. make null the value of any password
	 * type field)
	 * 
	 * @param properties
	 * @param isDefaultValue - Specify if the defaultValue or the value should be
	 *                       made null
	 * @param fieldType
	 * @return
	 */
	public static List<AbstractParam> filterValueByFieldType(
			List<AbstractParam> properties, boolean isDefaultValue, String fieldType) {
	  if (properties == null || fieldType == null) {
	    return null;
	  }
	  
	  for(AbstractParam property: properties) {
	    if(!fieldType.equals(property.getType())){
	      continue;
	    }
	    if(isDefaultValue) {
	      property.setDefaultValue(null);
	    }else {
	      property.setValue(null);
	    }
	    property.setHiddenValue(Boolean.TRUE);
	  }
	  return properties;
	}
	
    public static AbstractParam filterAbstractParam(AbstractParam param,
        boolean isDefaultValue, String fieldType) {
      if (param == null || fieldType == null || !fieldType.equals(param.getType())) {
        return null;
      }
      if (isDefaultValue) {
        param.setDefaultValue(null);
      } else {
        param.setValue(null);
      }
      param.setHiddenValue(Boolean.TRUE);
      return param;
    }
    
    /**
     * Method for filtering sensitive data from Parameters based on AbstractConfig type (e.g. make null the value of any password
     * type field)
     * 
     * @param properties
     * @param fieldType
     * @return
     */
    public static void filterParamSpecValueByFieldType(
            List<AbstractParam> config, List<ParamSpec> params, String fieldType) {   
      if (config.stream().anyMatch(c -> fieldType.equals(c.getType()))) {
        config.stream().filter(c -> fieldType.equals(c.getType())).forEach(c -> {
          c.setValue("");
          params.stream().filter(param -> param.getName().equalsIgnoreCase((c.getKey()))).forEach(p -> {
            p.setDefaultValue("");
          });
        });
      }
    }
    
    /**
     * Method for filtering sensitive data from Parameters based on AbstractConfig type (e.g. make null the value of any password
     * type field)
     * 
     * @param properties
     * @param fieldType
     * @return
     */
    public static void filterRunParamValueByFieldType(
            List<AbstractParam> config, List<RunParam> params, String fieldType) {    
      if (config.stream().anyMatch(c -> fieldType.equals(c.getType()))) {    
        config.stream().filter(c -> fieldType.equals(c.getType())).forEach(c -> {
          c.setValue("");
          params.stream().filter(param -> param.getName().equalsIgnoreCase((c.getKey()))).forEach(p -> {
            p.setValue("");
          });
        });
      }
    }
}
