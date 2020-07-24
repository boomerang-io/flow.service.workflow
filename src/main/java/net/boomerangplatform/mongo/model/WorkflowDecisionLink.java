package net.boomerangplatform.mongo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkflowDecisionLink extends WorkflowLink {

  @JsonInclude(Include.ALWAYS)
  private String switchCondition;

  public String getSwitchCondition() {
    return switchCondition;
  }

  public void setSwitchCondition(String switchCondition) {
    this.switchCondition = switchCondition;
  }

}
