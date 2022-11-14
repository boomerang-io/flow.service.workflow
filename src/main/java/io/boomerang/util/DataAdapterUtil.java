package io.boomerang.util;

import java.util.List;

import io.boomerang.mongo.model.AbstractConfigurationProperty;

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
	 * Method for filtering sensitive data (e.g. make null the value of any password
	 * type field)
	 * 
	 * @param properties
	 * @param isDefaultValue - Specify if the defaultValue or the value should be
	 *                       made null
	 * @param fieldType
	 * @return
	 */
	public static List<? extends AbstractConfigurationProperty> filterValueByFieldType(
			List<? extends AbstractConfigurationProperty> properties, boolean isDefaultValue, String fieldType) {
	  if (properties == null || fieldType == null) {
	    return null;
	  }
	  
	  for(AbstractConfigurationProperty property: properties) {
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
}
