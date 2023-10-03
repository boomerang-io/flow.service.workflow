package io.boomerang.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AbstractParam {

  private String key;
  private String description;
  private String label;
  private String type;
  private Integer minValueLength;
  private Integer maxValueLength;
  private List<KeyValuePair> options;
  private Boolean required;
  private String placeholder;
  @JsonProperty("helperText")
  private String helpertext;
  private String language;
  private Boolean disabled;
  private String defaultValue;
  private String value;
  private List<String> values;
  private boolean readOnly;
  private Boolean hiddenValue;

  public AbstractParam() {
  }

  @Override
  public String toString() {
    return "AbstractParam [key=" + key + ", description=" + description + ", label=" + label
        + ", type=" + type + "]";
  }



  public Boolean isHiddenValue() {
    return hiddenValue;
  }

  public void setHiddenValue(Boolean hiddenValue) {
    this.hiddenValue = hiddenValue;
  }

  public boolean isReadOnly() {
    return readOnly;
  }

  public void setReadOnly(boolean readOnly) {
    this.readOnly = readOnly;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Integer getMinValueLength() {
    return minValueLength;
  }

  public void setMinValueLength(Integer minValueLength) {
    this.minValueLength = minValueLength;
  }

  public Integer getMaxValueLength() {
    return maxValueLength;
  }

  public void setMaxValueLength(Integer maxValueLength) {
    this.maxValueLength = maxValueLength;
  }

  public List<KeyValuePair> getOptions() {
    return options;
  }

  public void setOptions(List<KeyValuePair> options) {
    this.options = options;
  }

  public Boolean getRequired() {
    return required;
  }

  public void setRequired(Boolean required) {
    this.required = required;
  }

  public String getPlaceholder() {
    return placeholder;
  }

  public void setPlaceholder(String placeholder) {
    this.placeholder = placeholder;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public Boolean getDisabled() {
    return disabled;
  }

  public void setDisabled(Boolean disabled) {
    this.disabled = disabled;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public List<String> getValues() {
    return values;
  }

  public void setValues(List<String> values) {
    this.values = values;
  }

  @JsonIgnore
  public boolean getBooleanValue() {
    if ("boolean".equals(this.getType())) {
      return Boolean.parseBoolean(this.getValue());
    } else {
      throw new IllegalArgumentException("Configuration object is not of type boolean.");
    }
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }

  public String getHelpertext() {
    return helpertext;
  }

  public void setHelpertext(String helpertext) {
    this.helpertext = helpertext;
  }

}
