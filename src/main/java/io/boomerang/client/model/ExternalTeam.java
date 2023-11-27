package io.boomerang.client.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalTeam {

	private String name;
    private String id;
    private List<String> userRoles;
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
	public List<String> getUserRoles() {
		return userRoles;
	}
	public void setUserRoles(List<String> userRoles) {
		this.userRoles = userRoles;
	}
}