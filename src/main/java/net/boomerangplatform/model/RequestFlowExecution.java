package net.boomerangplatform.model;

import java.util.Map;

public class RequestFlowExecution {

	private String token;
	private String workflowId;

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	private Map<String, String> properties;

	public Map<String, String> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

  public String getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }

}
