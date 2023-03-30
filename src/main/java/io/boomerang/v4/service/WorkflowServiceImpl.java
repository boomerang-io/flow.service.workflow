package io.boomerang.v4.service;

import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
import io.boomerang.v4.client.EngineClient;
import io.boomerang.v4.client.WorkflowRunResponsePage;
import io.boomerang.v4.model.enums.RelationshipRefType;
import io.boomerang.v4.model.ref.Workflow;
import io.boomerang.v4.model.ref.WorkflowRun;
import io.boomerang.v4.model.ref.WorkflowRunRequest;

/*
 * This service replicates the required calls for Engine WorkflowV1 APIs
 * 
 * It will
 * - Check authorization using Relationships
 * - Determines if to add or remove elements
 * - Forward call onto Engine (if applicable)
 * - Converts response as needed for UI
 */
@Service
public class WorkflowServiceImpl implements WorkflowService {

  private static final Logger LOGGER = LogManager.getLogger();

  @Autowired
  private EngineClient engineClient;

  @Autowired
  private FilterServiceV4 filterService;
  
  /*
   * Get Worklfow
   * 
   * No need to validate params as they are either defaulted or optional
   */
  @Override
  public ResponseEntity<Workflow> get(String workflowRunId, Optional<Integer> version, boolean withTasks) {
    if (workflowRunId == null || workflowRunId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }

    List<String> workflowRefs = filterService.getFilteredRefs(RelationshipRefType.WORKFLOW, Optional.empty(), Optional.empty(), Optional.empty());
    if (!workflowRefs.isEmpty() && workflowRefs.contains(workflowRunId)) {
      Workflow workflow = engineClient.getWorkflow(workflowRunId, version, withTasks);
      return ResponseEntity.ok(workflow);
    } else {
      //TODO: do we want to return invalid ref or unauthorized
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
  }

  @Override
  // TODO switch to WorkflowRun
  /*
   * Pass query onto EngineClient
   * 
   * No need to validate params as they are either defaulted or optional
   */
  public WorkflowRunResponsePage query(int page, int limit, Sort sort, Optional<List<String>> queryLabels,
      Optional<List<String>> queryStatus, Optional<List<String>> queryPhase) {
    List<String> workflowRunRefs = filterService.getFilteredRefs(RelationshipRefType.WORKFLOWRUN, Optional.empty(), Optional.empty(), Optional.empty());
    LOGGER.debug("Query Ids: ", workflowRunRefs);
    
    return engineClient.queryWorkflowRuns(page, limit, sort, queryLabels, queryStatus, queryPhase, Optional.of(workflowRunRefs));
  }

  
}
