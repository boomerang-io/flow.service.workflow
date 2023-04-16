package io.boomerang.v4.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.boomerang.v4.model.ref.Workflow;

/*
 * Workflow Model joining Workflow Entity and Workflow Revision Entity
 * 
 * A number of the Workflow Revision elements are put under metadata
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_EMPTY)
@JsonPropertyOrder({"id", "name", "status", "version", "creationDate", "timeout", "retries" })
public class WorkflowTemplate extends Workflow {
  

}
