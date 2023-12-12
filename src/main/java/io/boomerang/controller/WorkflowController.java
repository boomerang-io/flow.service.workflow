package io.boomerang.controller;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

import io.boomerang.error.BoomerangException;
import io.boomerang.model.DuplicateRequest;
import io.boomerang.model.FlowWorkflowRevision;
import io.boomerang.model.GenerateTokenResponse;
import io.boomerang.model.RevisionResponse;
import io.boomerang.model.WorkflowExport;
import io.boomerang.model.WorkflowSchedule;
import io.boomerang.model.WorkflowScheduleCalendar;
import io.boomerang.model.WorkflowSummary;
import io.boomerang.mongo.entity.WorkflowEntity;
import io.boomerang.mongo.model.WorkflowProperty;
import io.boomerang.mongo.model.WorkflowScope;
import io.boomerang.mongo.model.WorkflowStatus;
import io.boomerang.mongo.service.FlowWorkflowService;
import io.boomerang.security.service.UserValidationService;
import io.boomerang.service.crud.WorkflowScheduleService;
import io.boomerang.service.crud.WorkflowService;
import io.boomerang.service.crud.WorkflowVersionService;

@RestController
@RequestMapping("/workflow/workflow")
public class WorkflowController {

  @Autowired
  private WorkflowService workflowService;

  @Autowired
  private WorkflowVersionService workflowVersionService;

  @Autowired
  private FlowWorkflowService workFlowRepository;

  @Autowired
  private WorkflowScheduleService workflowScheduleService;

  @Autowired
  private UserValidationService userValidationService;

  @DeleteMapping(value = "{id}")
  public void deleteWorkflowWithId(@PathVariable String id) {
    workflowService.deleteWorkflow(id);
  }

  @PostMapping(value = "{id}/token")
  public GenerateTokenResponse createToken(@PathVariable String id, @RequestParam String label) {
    return workflowService.generateTriggerToken(id, label);
  }

  @DeleteMapping(value = "{id}/token")
  public void deleteToken(@PathVariable String id, @RequestParam String label) {
    workflowService.deleteToken(id, label);
  }

  @GetMapping(value = "/{workFlowId}/revision")
  public FlowWorkflowRevision getWorkflowLatestVersion(@PathVariable String workFlowId) {
    final WorkflowEntity entity = workFlowRepository.getWorkflow(workFlowId);
    try {
      userValidationService.validateUserAccessForWorkflow(entity.getScope(), 
    		  entity.getFlowTeamId(), entity.getOwnerUserId(), false);
    } catch (ResponseStatusException e) {
      throw new HttpClientErrorException(e.getStatus());
    }
    return workflowVersionService.getLatestWorkflowVersion(workFlowId);
  }

  @GetMapping(value = "/{workFlowId}/revision/{version}")
  public FlowWorkflowRevision getWorkflowVersion(@PathVariable String workFlowId,
      @PathVariable Long version) {
    return workflowVersionService.getWorkflowVersion(workFlowId, version);
  }

  @GetMapping(value = "{id}/summary")
  public WorkflowSummary getWorkflowWithId(@PathVariable String id) {
//    final WorkflowEntity entity = workFlowRepository.getWorkflow(id);
//    try {
//      userValidationService.validateUserAccessForWorkflow(entity.getScope(), 
//      		  entity.getFlowTeamId(), entity.getOwnerUserId(), false);
//    } catch (ResponseStatusException e) {
//      throw new HttpClientErrorException(e.getStatus());
//    }
    return workflowService.getWorkflow(id);
  }

  @PostMapping(value = "/{workFlowId}/duplicate")
  public WorkflowSummary duplicateWorkflow(@PathVariable String workFlowId,
      @RequestBody(required = false) DuplicateRequest duplicateRequest) {
    return workflowService.duplicateWorkflow(workFlowId, duplicateRequest);
  }

  @PostMapping(value = "")
  public WorkflowSummary insertWorkflow(@RequestBody WorkflowSummary workflowSummaryEntity) {
	userValidationService.validateUserAccessForWorkflow(workflowSummaryEntity.getScope(), 
			workflowSummaryEntity.getFlowTeamId(), workflowSummaryEntity.getOwnerUserId(), true);
    workflowSummaryEntity.setStatus(WorkflowStatus.active);
    return workflowService.saveWorkflow(workflowSummaryEntity);
  }

  @PostMapping(value = "/{workFlowId}/revision")
  public FlowWorkflowRevision insertWorkflow(@PathVariable String workFlowId,
      @RequestBody FlowWorkflowRevision workflowSummaryEntity) {
    workflowSummaryEntity.setId(null);
    workflowSummaryEntity.setWorkFlowId(workFlowId);
    return workflowVersionService.insertWorkflowVersion(workflowSummaryEntity);
  }

  @PatchMapping(value = "")
  public WorkflowSummary updateWorkflow(@RequestBody WorkflowSummary workflowSummaryEntity) {
    return workflowService.updateWorkflow(workflowSummaryEntity);
  }

  @PatchMapping(value = "/{workFlowId}/properties")
  public WorkflowSummary updateWorkflowProperties(@PathVariable String workFlowId,
      @RequestBody List<WorkflowProperty> properties) {
    return workflowService.updateWorkflowProperties(workFlowId, properties);
  }

  @GetMapping(value = "/{workFlowId}/changelog")
  public List<RevisionResponse> viewChangelog(
      @PathVariable(value = "workFlowId", required = true) Optional<String> workFlowId,
      @RequestParam(defaultValue = "ASC") Optional<Direction> order,
      @RequestParam Optional<String> sort, @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "2147483647") int size) {

    Sort pagingSort = Sort.by(new Order(Direction.DESC, "date"));

    if (sort.isPresent()) {
      Direction direction = Direction.ASC;
      final String sortByKey = sort.get();

      if (order.isPresent()) {
        direction = order.get();
      }

      pagingSort = Sort.by(new Order(direction, sortByKey));
    }

    final Pageable pageable = PageRequest.of(page, size, pagingSort);
    return workflowVersionService.viewChangelog(workFlowId, pageable);
  }

  @GetMapping(value = "/export/{workFlowId}", produces = "application/json")
  public ResponseEntity<InputStreamResource> exportWorkflow(@PathVariable String workFlowId) {
    return workflowService.exportWorkflow(workFlowId);
  }

  @PostMapping(value = "/import")
  public void importWorkflow(@RequestBody WorkflowExport export, @RequestParam Boolean update,
      @RequestParam(required = false) String flowTeamId,
      @RequestParam(required = true) WorkflowScope scope) {
    workflowService.importWorkflow(export, update, flowTeamId, scope);
  }

  @GetMapping(value = "/{workFlowId}/available-parameters")
  public List<String> getWorkflowParameters(@PathVariable String workFlowId) {
    return workflowService.getWorkflowParameters(workFlowId);
  }

  @PostMapping(value = "/{workFlowId}/available-parameters")
  public List<String> getWorkflowParametersWithBody(@PathVariable String workFlowId,
      @RequestBody FlowWorkflowRevision workflowSummaryEntity) {
    return workflowService.getWorkflowParameters(workFlowId, workflowSummaryEntity);
  }

  @GetMapping(value = "/{workflowId}/schedules")
  public List<WorkflowSchedule> getSchedulesForWorkflow(@PathVariable String workflowId) {
    return workflowScheduleService.getSchedulesForWorkflow(workflowId);
  }

  @GetMapping(value = "/{workflowId}/schedules/calendar")
  public List<WorkflowScheduleCalendar> getCalendarsForWorkflow(@PathVariable String workflowId,
      @RequestParam Long fromDate, @RequestParam Long toDate) {
    if (workflowId != null && fromDate != null && toDate != null) {
      Date from = new Date(fromDate * 1000);
      Date to = new Date(toDate * 1000);
      return workflowScheduleService.getCalendarsForWorkflow(workflowId, from, to);
    } else {
      throw new BoomerangException(0, "Invalid fromDate or toDate", HttpStatus.BAD_REQUEST);
    }
  }
}
