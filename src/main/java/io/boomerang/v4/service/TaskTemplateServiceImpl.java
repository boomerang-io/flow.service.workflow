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
      switchChangeLogAuthorToUserName(taskTemplate.getChangelog());
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
      response.getContent().forEach(t -> switchChangeLogAuthorToUserName(t.getChangelog()));
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
    updateChangeLog(request.getChangelog());
    
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
  public ResponseEntity<TaskTemplate> apply(TaskTemplate request, boolean replace) {
    String templateName = request.getName();
    List<String> refs = relationshipService.getFilteredRefs(Optional.of(RelationshipRef.TASKTEMPLATE),
        Optional.of(List.of(templateName)), Optional.of(RelationshipType.BELONGSTO), Optional.empty(), Optional.empty());

    if (templateName != null && !templateName.isBlank() && !refs.isEmpty()) {
      // Set verfied to false - this is only able to be set via Engine or Loader
      request.setVerified(false);
      
      // Update Changelog
      updateChangeLog(request.getChangelog());
      
      // Process Parameters - if no Params are set but Config is then create ParamSpecs
      if (request.getConfig() != null && !request.getConfig().isEmpty() && request.getSpec() != null) {
        ParameterUtil.abstractParamToParamSpec(request.getConfig(), request.getSpec().getParams());
      }
      TaskTemplate template = engineClient.applyTaskTemplate(request, replace);
      return ResponseEntity.ok(template);
    } else {
      // TODO: do we want to return invalid ref or unauthorized
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
  }

  // Override changelog date and set author. Used on creation/update of TaskTemplate
  private void updateChangeLog(ChangeLog changelog) {
    if (changelog == null) {
      changelog = new ChangeLog();
    }
    changelog.setDate(new Date());
    //TODO: update Author ID
  }

  //TODO: update to correct User Service. Used on retrieval of TaskTemplate
  private void switchChangeLogAuthorToUserName(ChangeLog changelog) {
    if (changelog != null && changelog.getAuthor() != null) {
      UserEntity user = userIdentityService.getUserByID(changelog.getAuthor());
      if (user != null) {
        changelog.setAuthor(user.getName());
      }
    }
  }

  @Override
  public void enable(String name) {
    if (name == null || name.isBlank()) {
      // TODO: do we want to return invalid ref or unauthorized
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
    List<String> refs = relationshipService.getFilteredRefs(Optional.of(RelationshipRef.TASKTEMPLATE),
        Optional.of(List.of(name)), Optional.of(RelationshipType.BELONGSTO), Optional.empty(), Optional.empty());

    if (refs.isEmpty()) {
      // TODO: do we want to return invalid ref or unauthorized
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
    engineClient.enableTaskTemplate(name);
  }

  @Override
  public void disable(String name) {
    if (name == null || name.isBlank()) {
      // TODO: do we want to return invalid ref or unauthorized
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
    List<String> refs = relationshipService.getFilteredRefs(Optional.of(RelationshipRef.TASKTEMPLATE),
        Optional.of(List.of(name)), Optional.of(RelationshipType.BELONGSTO), Optional.empty(), Optional.empty());

    if (refs.isEmpty()) {
      // TODO: do we want to return invalid ref or unauthorized
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
    engineClient.disableTaskTemplate(name);
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
