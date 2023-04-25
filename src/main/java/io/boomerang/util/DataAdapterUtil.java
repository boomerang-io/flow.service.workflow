package io.boomerang.util;

import java.util.List;
import io.boomerang.v4.model.AbstractParam;
import io.boomerang.v4.model.ref.ParamSpec;
import io.boomerang.v4.model.ref.RunParam;

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
      config.stream().filter(p -> fieldType.equals(p.getType())).forEach(p -> {
        p.setValue(null);
        params.stream().filter(param -> param.getName().equalsIgnoreCase((p.getKey()))).findFirst().get().setDefaultValue(null);
      });
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
      config.stream().filter(p -> fieldType.equals(p.getType())).forEach(p -> {
        p.setValue(null);
        params.stream().filter(param -> param.getName().equalsIgnoreCase((p.getKey()))).findFirst().get().setValue(null);
      });
    }
}
