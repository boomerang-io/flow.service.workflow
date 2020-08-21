package net.boomerangplatform.model.controller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@JsonIgnoreProperties
public class TaskConfiguration {

  private Boolean debug;

  private TaskDeletion deletion;

	public Boolean getDebug() {
		return debug ;
	}

	public void setDebug(Boolean debug) {
		this.debug = debug;
	}

	public TaskDeletion getDeletion() {
		return deletion ;
	}

	public void setDeletion(TaskDeletion deletion) {
		this.deletion = deletion;
	}
}
