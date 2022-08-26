package io.boomerang.mongo.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.boomerang.mongo.entity.RevisionEntity;

public class WorkFlowRevisionCount {
	
	@JsonProperty("_id")
	private String id;  // workflowId

	private long count; // revision count of the workflow

	private RevisionEntity latestVersion; // latest revision of the workflow

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public long getCount() {
		return count;
	}

	public void setCount(long count) {
		this.count = count;
	}

	public RevisionEntity getLatestVersion() {
		return latestVersion;
	}

	public void setLatestVersion(RevisionEntity latestVersion) {
		this.latestVersion = latestVersion;
	}

}
