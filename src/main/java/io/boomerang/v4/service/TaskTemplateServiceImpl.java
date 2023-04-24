package io.boomerang.v4.service;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
import io.boomerang.model.FlowTaskTemplate;
import io.boomerang.model.TemplateScope;
import io.boomerang.model.WorkflowSummary;
import io.boomerang.model.tekton.TektonTask;
import io.boomerang.mongo.model.FlowTaskTemplateStatus;
import io.boomerang.mongo.model.Revision;
import io.boomerang.mongo.model.UserType;
import io.boomerang.mongo.model.WorkflowScope;
import io.boomerang.service.tekton.TektonConverter;
import io.boomerang.util.DataAdapterUtil;
import io.boomerang.util.ParameterUtil;
import io.boomerang.util.DataAdapterUtil.FieldType;
import io.boomerang.v4.client.EngineClient;
import io.boomerang.v4.client.TaskTemplateResponsePage;
import io.boomerang.v4.data.entity.UserEntity;
import io.boomerang.v4.model.enums.RelationshipRef;
import io.boomerang.v4.model.enums.RelationshipType;
import io.boomerang.v4.model.ref.ChangeLog;
import io.boomerang.v4.model.ref.TaskTemplate;
import io.boomerang.v4.model.ref.Workflow;

/*
 * This service replicates the required calls for Engine TaskTemplateV1 APIs
 * 
 * - Checks authorization using Relationships
 * - Determines if to add or remove elements
 * - Forward call onto Engine (if applicable)
 * - Converts response as needed for UI
 */
@Service
public class TaskTemplateServiceImpl implements TaskTemplateService {

  private static final Logger LOGGER = LogManager.getLogger();

  @Autowired
  private EngineClient engineClient;

  @Autowired
  private RelationshipService relationshipService;

  /*
   * Get TaskTemplate by name and optional version. If no version specified, will retrieve the latest.
   */
  @Override
  public ResponseEntity<TaskTemplate> get(String name, Optional<Integer> version) {
    if (name == null || name.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }

    // Check if requester has access to refs
    // TODO: determine if all users need to be able to access (READ) but not edit (CREATE, UPDATE, DELETE) 
    List<String> refs = relationshipService.getFilteredRefs(Optional.of(RelationshipRef.TASKTEMPLATE),
        Optional.of(List.of(name)), Optional.of(RelationshipType.BELONGSTO), Optional.empty(), Optional.empty());
    if (!refs.isEmpty()) {
      TaskTemplate taskTemplate = engineClient.getTaskTemplate(name, version);
      
      // Process Parameters - create configs for any Params
      if (taskTemplate.getSpec().getParams() != null && !taskTemplate.getSpec().getParams().isEmpty()) {
        ParameterUtil.paramSpecToAbstractParam(taskTemplate.getSpec().getParams(), taskTemplate.getConfig());
      }
      
      // Switch from UserId to Users Name
      updateChangeLogAuthor(taskTemplate.getChangelog());
      return ResponseEntity.ok(taskTemplate);
    } else {
      // TODO: do we want to return invalid ref or unauthorized
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
  }

  /*
   * Query for Workflows.
   */
  @Override
  public TaskTemplateResponsePage query(int page, int limit, Sort sort,
      Optional<List<String>> queryLabels, Optional<List<String>> queryStatus,
      Optional<List<String>> queryNames, Optional<List<String>> queryTeams) {

    // Get Refs that request has access to
    // TODO: this doesn't work for Global TaskTemplates
    List<String> refs =
        relationshipService.getFilteredRefs(Optional.of(RelationshipRef.TASKTEMPLATE), queryNames,
            Optional.of(RelationshipType.BELONGSTO), Optional.ofNullable(RelationshipRef.TEAM),
            queryTeams);
    LOGGER.debug("Query Ids: ", refs);

    TaskTemplateResponsePage response = engineClient.queryTaskTemplates(page, limit, sort,
        queryLabels, queryStatus, Optional.of(refs));

    if (!response.getContent().isEmpty()) {
      response.getContent().forEach(t -> updateChangeLogAuthor(t.getChangelog()));
    }
    return response;
  }
  
  /*
   * Creates the TaskTemplate and Relationship
   * 
   * The Engine checks for unique names so this method does not have to, however it does need to add the 
   */
  @Override
  public ResponseEntity<TaskTemplate> create(TaskTemplate request,
      Optional<String> team) {
    // Set verfied to false - this is only able to be set via Engine or Loader
    request.setVerified(false);
    
    // Process Parameters - if no Params are set but Config is then create ParamSpecs
    if (request.getConfig() != null && !request.getConfig().isEmpty() && request.getSpec() != null) {
      ParameterUtil.abstractParamToParamSpec(request.getConfig(), request.getSpec().getParams());
    }
    
    // Update Changelog
    // TODO: figure out how to get current user or if token then type of token
//    ChangeLog changelog = new ChangeLog(CHANGELOG_INITIAL);
//    if (taskTemplate.getChangelog() != null) {
//      if (taskTemplate.getChangelog().getAuthor() != null) {
//        changelog.setAuthor(taskTemplate.getChangelog().getAuthor());
//      }
//      if (taskTemplate.getChangelog().getReason() != null) {
//        changelog.setReason(taskTemplate.getChangelog().getReason());
//      }
//      if (taskTemplate.getChangelog().getDate() != null) {
//        changelog.setDate(taskTemplate.getChangelog().getDate());
//      }
//    }
    
    // TODO: add a check that they are prefixed with the current team scope OR are a valid Global TaskTemplate
    
    TaskTemplate taskTemplate = engineClient.createTaskTemplate(request);
    if (team.isPresent()) {
    // Create BELONGSTO relationship for mapping Workflow to Owner
      relationshipService.addRelationshipRef(RelationshipRef.TASKTEMPLATE, taskTemplate.getName(), RelationshipRef.TEAM,
          team);
    } else {
      // Creates a relationship based on current used Security Scope
      relationshipService.addRelationshipRefForCurrentScope(RelationshipRef.TASKTEMPLATE,
          taskTemplate.getName());
    }
    return ResponseEntity.ok(taskTemplate);
  }

  /*
   * Apply allows you to create a new version as well as create new
   */
  @Override
  public ResponseEntity<Workflow> apply(Workflow workflow, boolean replace) {
    String workflowId = workflow.getId();
    List<String> workflowRefs = relationshipService.getFilteredRefs(Optional.of(RelationshipRef.WORKFLOW),
        Optional.of(List.of(workflowId)), Optional.of(RelationshipType.BELONGSTO), Optional.of(RelationshipRef.TEAM), Optional.empty());

    if (workflowId != null && !workflowId.isBlank() && !workflowRefs.isEmpty()) {
      updateScheduleTriggers(workflow, this.get(workflowId, Optional.empty(), false).getBody().getTriggers());
      setupTriggerDefaults(workflow);
      Workflow appliedWorkflow = engineClient.applyWorkflow(workflow, replace);
      // Filter out sensitive values
      DataAdapterUtil.filterParamSpecValueByFieldType(appliedWorkflow.getConfig(), appliedWorkflow.getParams(), FieldType.PASSWORD.value());
      return ResponseEntity.ok(appliedWorkflow);
    } else {
      workflow.setId(null);
      return this.create(workflow, Optional.empty());
    }
  }

  private void updateChangeLogAuthor(ChangeLog changelog) {
    if (changelog != null && changelog.getAuthor() != null) {
      UserEntity user = userIdentityService.getUserByID(changelog.getAuthor());
      if (user != null) {
        changelog.setAuthor(user.getName());
      }
    }
  }

  @Override
  public FlowTaskTemplate insertTaskTemplate(FlowTaskTemplate flowTaskTemplateEntity) {
    UserEntity user = userIdentityService.getCurrentUser();

    if (user.getType() == UserType.admin || user.getType() == UserType.operator) {

      Date creationDate = new Date();

      flowTaskTemplateEntity.setCreatedDate(creationDate);
      flowTaskTemplateEntity.setLastModified(creationDate);
      flowTaskTemplateEntity.setVerified(false);

      updateChangeLog(flowTaskTemplateEntity);

      return new FlowTaskTemplate(
          flowTaskTemplateService.insertTaskTemplate(flowTaskTemplateEntity));

    } else {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

  }

  @Override
  public FlowTaskTemplate updateTaskTemplate(FlowTaskTemplate flowTaskTemplateEntity) {
    UserEntity user = userIdentityService.getCurrentUser();

    if (user.getType() == UserType.admin || user.getType() == UserType.operator) {

      updateChangeLog(flowTaskTemplateEntity);

      flowTaskTemplateEntity.setLastModified(new Date());
      flowTaskTemplateEntity.setVerified(flowTaskTemplateService
          .getTaskTemplateWithId(flowTaskTemplateEntity.getId()).isVerified());
      return new FlowTaskTemplate(
          flowTaskTemplateService.updateTaskTemplate(flowTaskTemplateEntity));

    } else {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
  }

  @Override
  public void deleteTaskTemplateWithId(String id) {
    UserEntity user = userIdentityService.getCurrentUser();

    if (user.getType() == UserType.admin || user.getType() == UserType.operator) {
      flowTaskTemplateService.deleteTaskTemplate(flowTaskTemplateService.getTaskTemplateWithId(id));
    } else {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
  }

  @Override
  public void activateTaskTemplate(String id) {
    flowTaskTemplateService.activateTaskTemplate(flowTaskTemplateService.getTaskTemplateWithId(id));

  }

  private void updateChangeLog(FlowTaskTemplate flowTaskTemplateEntity) {
    List<Revision> revisions = flowTaskTemplateEntity.getRevisions();
    final UserEntity user = userIdentityService.getCurrentUser();

    if (revisions != null) {
      for (Revision revision : revisions) {
        ChangeLog changelog = revision.getChangelog();
        if (changelog != null && changelog.getUserId() == null) {
          changelog.setUserId(user.getId());
          changelog.setDate(new Date());
          changelog.setUserName(user.getName());
        }
      }
    }
  }

  @Override
  public TektonTask getTaskTemplateYamlWithId(String id) {
    FlowTaskTemplateEntity template = flowTaskTemplateService.getTaskTemplateWithId(id);
    return TektonConverter.convertFlowTaskToTekton(template, Optional.empty());
  }

  @Override
  public TektonTask getTaskTemplateYamlWithIdAndRevision(String id, Integer revisionNumber) {
    FlowTaskTemplateEntity template = flowTaskTemplateService.getTaskTemplateWithId(id);
    return TektonConverter.convertFlowTaskToTekton(template, Optional.of(revisionNumber));
  }

  @Override
  public FlowTaskTemplate insertTaskTemplateYaml(TektonTask tektonTask, TemplateScope scope,
      String teamId) {
    FlowTaskTemplateEntity template = TektonConverter.convertTektonTaskToNewFlowTask(tektonTask);
    template.setStatus(FlowTaskTemplateStatus.active);
    template.setScope(scope);
    template.setFlowTeamId(teamId);

    flowTaskTemplateService.insertTaskTemplate(template);
    return new FlowTaskTemplate(template);
  }

  @Override
  public FlowTaskTemplate updateTaskTemplateWithYaml(String id, TektonTask tektonTask) {
    FlowTaskTemplateEntity tektonTemplate =
        TektonConverter.convertTektonTaskToNewFlowTask(tektonTask);
    FlowTaskTemplateEntity dbTemplate = flowTaskTemplateService.getTaskTemplateWithId(id);

    if (tektonTemplate.getName() != null && !tektonTemplate.getName().isBlank()) {
      dbTemplate.setName(tektonTemplate.getName());
    }
    if (tektonTemplate.getCategory() != null && !tektonTemplate.getCategory().isBlank()) {
      dbTemplate.setCategory(tektonTemplate.getCategory());
    }

    if (tektonTemplate.getDescription() != null && !tektonTemplate.getDescription().isBlank()) {
      dbTemplate.setDescription(tektonTemplate.getDescription());
    }

    List<Revision> revisions = tektonTemplate.getRevisions();
    if (revisions.size() == 1) {
      Revision revision = revisions.get(0);

      final UserEntity user = userIdentityService.getCurrentUser();
      if (user != null) {
        ChangeLog changelog = revision.getChangelog();
        changelog.setUserId(user.getId());
        changelog.setDate(new Date());
      }


      List<Revision> existingRevisions = dbTemplate.getRevisions();
      int count = existingRevisions.size();
      revision.setVersion(count + 1);
      existingRevisions.add(revision);
    }
    dbTemplate.setLastModified(new Date());
    flowTaskTemplateService.updateTaskTemplate(dbTemplate);
    return this.getTaskTemplateWithId(id);
  }

  @Override
  public FlowTaskTemplate updateTaskTemplateWithYaml(String id, TektonTask tektonTask,
      Integer revisionId, String comment) {
    FlowTaskTemplateEntity tektonTemplate =
        TektonConverter.convertTektonTaskToNewFlowTask(tektonTask);
    FlowTaskTemplateEntity dbTemplate = flowTaskTemplateService.getTaskTemplateWithId(id);

    if (tektonTemplate.getName() != null && !tektonTemplate.getName().isBlank()) {
      dbTemplate.setName(tektonTemplate.getName());
    }
    if (tektonTemplate.getCategory() != null && !tektonTemplate.getCategory().isBlank()) {
      dbTemplate.setCategory(tektonTemplate.getCategory());
    }

    if (tektonTemplate.getDescription() != null && !tektonTemplate.getDescription().isBlank()) {
      dbTemplate.setDescription(tektonTemplate.getDescription());
    }

    List<Revision> revisions = tektonTemplate.getRevisions();
    if (revisions.size() == 1) {
      Revision revision = revisions.get(0);
      revision.setVersion(revisionId);

      final UserEntity user = userIdentityService.getCurrentUser();
      if (user != null) {
        ChangeLog changelog = revision.getChangelog();
        changelog.setUserId(user.getId());
        changelog.setDate(new Date());
        changelog.setReason(comment);
      }

      List<Revision> existingRevisions = dbTemplate.getRevisions();

      Revision oldRevision = existingRevisions.stream()
          .filter(a -> a.getVersion().equals(revisionId)).findFirst().orElse(null);
      if (oldRevision != null) {
        existingRevisions.remove(oldRevision);

      }
      existingRevisions.add(revision);
    }
    dbTemplate.setLastModified(new Date());
    flowTaskTemplateService.updateTaskTemplate(dbTemplate);
    return this.getTaskTemplateWithId(id);
  }

  @Override
  public List<FlowTaskTemplate> getAllTaskTemplatesForWorkfow(String workflowId) {
    List<FlowTaskTemplate> templates = new LinkedList<>();

    WorkflowSummary workflow = this.workflowService.getWorkflow(workflowId);
    String flowTeamId = workflow.getFlowTeamId();
    if (workflow.getScope() == WorkflowScope.team || workflow.getScope() == null) {
      templates = flowTaskTemplateService.getAllTaskTemplatesforTeamId(flowTeamId).stream()
          .map(FlowTaskTemplate::new).collect(Collectors.toList());
    } else if (workflow.getScope() == WorkflowScope.system || workflow.getScope() == WorkflowScope.user || workflow.getScope() == WorkflowScope.template) {
      templates = flowTaskTemplateService.getAllTaskTemplatesForSystem().stream()
          .map(FlowTaskTemplate::new).collect(Collectors.toList());
    }

    return templates;
  }

  @Override
  public FlowTaskTemplate validateTaskTemplate(TektonTask tektonTask) {
    FlowTaskTemplateEntity template = TektonConverter.convertTektonTaskToNewFlowTask(tektonTask);
    template.setStatus(FlowTaskTemplateStatus.active);
    return new FlowTaskTemplate(template);
  }
}
