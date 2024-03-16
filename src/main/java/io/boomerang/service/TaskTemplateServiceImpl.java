package io.boomerang.service;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import io.boomerang.client.EngineClient;
import io.boomerang.client.TaskTemplateResponsePage;
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
import io.boomerang.model.User;
import io.boomerang.model.enums.RelationshipLabel;
import io.boomerang.model.enums.RelationshipNodeType;
import io.boomerang.model.ref.ChangeLog;
import io.boomerang.model.ref.ChangeLogVersion;
import io.boomerang.model.ref.TaskTemplate;
import io.boomerang.security.service.IdentityService;
import io.boomerang.tekton.TektonConverter;
import io.boomerang.tekton.TektonTask;
import io.boomerang.util.ParameterUtil;

/*
 * This service replicates the required calls for Engine TaskTemplateV1 APIs
 * 
 * - Checks Relationships
 * - Determines if to add or remove elements
 * - Forward call onto Engine (converts slug to ID)
 * - Converts response as needed for UI (including converting ID to Slug)
 */
@Service
public class TaskTemplateServiceImpl implements TaskTemplateService {

  private static final Logger LOGGER = LogManager.getLogger();
  
  private static final String NAME_REGEX = "^([0-9a-zA-Z\\-]+)$";

  @Autowired
  private EngineClient engineClient;

  @Autowired
  private RelationshipServiceImpl relationshipServiceImpl;

  @Autowired
  private IdentityService identityService;

  /*
   * Retrieve a TASK by name and optional version. If no version specified, will retrieve the latest.
   * TODO: determine if all users need to be able to access (READ) but not edit (CREATE, UPDATE, DELETE)
   */
  @Override
  public TaskTemplate get(String name, Optional<Integer> version, String team) {
    if (!Objects.isNull(name) && !name.isBlank() && relationshipServiceImpl.hasTeamRelationship(Optional.of(RelationshipNodeType.TASK),
          Optional.of(name), RelationshipLabel.BELONGSTO, team, false)) {
        String ref = relationshipServiceImpl.getRefFromSlug(RelationshipNodeType.TASK, name);
        return internalGet(ref, version);
    }
    throw new BoomerangException(BoomerangError.TASKTEMPLATE_INVALID_REF);
  }
    
  /*
   * Retrieve a GLOBALTASK by name and optional version. If no version specified, will retrieve the latest.
   * TODO: determine if all users need to be able to access (READ) but not edit (CREATE, UPDATE, DELETE)
   */
  @Override
  public TaskTemplate get(String name, Optional<Integer> version) {
    if (!Objects.isNull(name) && !name.isBlank()
        && relationshipServiceImpl.hasGlobalRelationship(
            Optional.of(RelationshipNodeType.GLOBALTASK), Optional.of(name),
            RelationshipLabel.BELONGSTO, false)) {
      String ref = relationshipServiceImpl.getRefFromSlug(RelationshipNodeType.GLOBALTASK, name);
      return internalGet(ref, version);
    }
    throw new BoomerangException(BoomerangError.TASKTEMPLATE_INVALID_REF);
  }

  private TaskTemplate internalGet(String id, Optional<Integer> version) {
    TaskTemplate taskTemplate = engineClient.getTaskTemplate(id, version);
    
    // Process Parameters - create configs for any Params
    taskTemplate.getSpec().setParams(ParameterUtil.abstractParamsToParamSpecs(taskTemplate.getConfig(), taskTemplate.getSpec().getParams()));
    taskTemplate.setConfig(ParameterUtil.paramSpecToAbstractParam(taskTemplate.getSpec().getParams(), taskTemplate.getConfig()));
    
    // Switch author from ID to Name
    switchChangeLogAuthorToUserName(taskTemplate.getChangelog());
    LOGGER.debug("Changelog: " + taskTemplate.getChangelog().toString());
    
    // Remove ID
    taskTemplate.setId(null);
    
    return taskTemplate;
  }

  /*
   * Query for TASKS.
   */
  @Override
  public TaskTemplateResponsePage query(Optional<Integer> queryLimit, Optional<Integer> queryPage, Optional<Direction> querySort,
      Optional<List<String>> queryLabels, Optional<List<String>> queryStatus,
      Optional<List<String>> queryNames, String queryTeam) {
    
    //The filteredRefs call will check for having a relationship
    List<String> refs = relationshipServiceImpl.getFilteredRefs(Optional.of(RelationshipNodeType.TASK), queryNames, RelationshipLabel.BELONGSTO, RelationshipNodeType.TEAM, queryTeam, false);
    LOGGER.debug("Task Refs: {}", refs.toString());
    if (refs == null || refs.size() == 0) {
      return new TaskTemplateResponsePage();
    }
    return internalQuery(queryLimit, queryPage, querySort, queryLabels, queryStatus, refs);
  }
  
  /*
   * Query for GLOBALTASKS.
   */
  @Override
  public TaskTemplateResponsePage query(Optional<Integer> queryLimit, Optional<Integer> queryPage, Optional<Direction> querySort,
      Optional<List<String>> queryLabels, Optional<List<String>> queryStatus,
      Optional<List<String>> queryNames) {
    
    List<String> refs = relationshipServiceImpl.getGlobalTaskRefs(queryNames);
    LOGGER.debug("Global Task Refs: {}", refs.toString());
    if (refs == null || refs.size() == 0) {
      return new TaskTemplateResponsePage();
    }
    return internalQuery(queryLimit, queryPage, querySort, queryLabels, queryStatus, refs);
  }
  
  private TaskTemplateResponsePage internalQuery(Optional<Integer> queryLimit, Optional<Integer> queryPage, Optional<Direction> querySort,
      Optional<List<String>> queryLabels, Optional<List<String>> queryStatus,
      List<String> queryRefs) {
    TaskTemplateResponsePage response = engineClient.queryTaskTemplates(queryLimit, queryPage, querySort,
        queryLabels, queryStatus, queryRefs);

    if (!response.getContent().isEmpty()) {
      response.getContent().forEach(t -> {
        switchChangeLogAuthorToUserName(t.getChangelog());
        // Remove ID
        t.setId(null);
      });
    }
    return response;
  }
  
  /*
   * Creates the TaskTemplate and Relationship
   * 
   * The Engine checks for unique names so this method does not have to, however it does need to add the 
   */
  @Override
  public TaskTemplate create(TaskTemplate request,
      String team) {
    // Validate Access
    if (!relationshipServiceImpl.hasTeamRelationship(Optional.empty(),
        Optional.empty(), RelationshipLabel.MEMBEROF, team, false)) {
      //TODO throw correct error - not a member of team
      throw new BoomerangException(BoomerangError.TASKTEMPLATE_INVALID_NAME, request.getName());
    }
    
    // Check name matches the requirements
    if (request.getName().isBlank() || !request.getName().matches(NAME_REGEX)) {
      throw new BoomerangException(BoomerangError.TASKTEMPLATE_INVALID_NAME, request.getName());
    }
    
    // Check Slugs for Tasks in team
    if (relationshipServiceImpl.doesSlugExistInTeam(RelationshipNodeType.TASK, request.getName(), team)) {
     //TODO throw correct error - already exists
      throw new BoomerangException(BoomerangError.TASKTEMPLATE_INVALID_NAME, request.getName());
    }
    
    // Create Task
    TaskTemplate task = internalCreate(request);

    // Create Relationship
    // TODO allow node to be created with a connection
    relationshipServiceImpl.createNode(RelationshipNodeType.TASK, task.getId(), task.getName(), Optional.empty());
    relationshipServiceImpl.upsertTeamConnectionBySlug(RelationshipNodeType.TASK, task.getId(), RelationshipLabel.BELONGSTO,
        team, Optional.empty());
    
    // Remove ID
    task.setId(null);
    return task;    
  }

  @Override
  public TaskTemplate create(TaskTemplate request) {
    // Check name matches the requirements
    if (request.getName().isBlank() || !request.getName().matches(NAME_REGEX)) {
      throw new BoomerangException(BoomerangError.TASKTEMPLATE_INVALID_NAME, request.getName());
    }
    
    // Check Slugs for GlobalTasks
    if (relationshipServiceImpl.doesSlugExistForType(RelationshipNodeType.GLOBALTASK, request.getName())) {
     //TODO throw correct error
      throw new BoomerangException(BoomerangError.TASKTEMPLATE_INVALID_NAME, request.getName());
    }
    
    // Create Task
    TaskTemplate task = internalCreate(request);

    // Create Relationship
    relationshipServiceImpl.createNode(RelationshipNodeType.GLOBALTASK, task.getId(), task.getName(), Optional.empty());

    // Remove ID
    task.setId(null);
    return task;  
  }
  
  private TaskTemplate internalCreate(TaskTemplate request) {
    // Ignore any provided Ids as this is a create
    request.setId(null);
    // Set verified to false - this is only able to be set via Engine or Loader
    request.setVerified(false);
    
    // Process Parameters - ensure Param and Config share the same params
    ParameterUtil.abstractParamsToParamSpecs(request.getConfig(), request.getSpec().getParams());
    ParameterUtil.paramSpecToAbstractParam(request.getSpec().getParams(), request.getConfig());
    
    // Update Changelog
    updateChangeLog(request.getChangelog());
    
    // Come back to this once we have separated the controllers - works better for scope checks.
    TaskTemplate taskTemplate = engineClient.createTaskTemplate(request);
    switchChangeLogAuthorToUserName(taskTemplate.getChangelog());

    return taskTemplate;
  }

  /*
   * Apply allows you to create a new version as well as create new
   */
  @Override
  public TaskTemplate apply(TaskTemplate request, boolean replace, String team) {
    if (request.getName().isBlank() || !request.getName().matches(NAME_REGEX)) {
      throw new BoomerangException(BoomerangError.TASKTEMPLATE_INVALID_NAME, request.getName());
    }
    if (relationshipServiceImpl.doesSlugExistInTeam(RelationshipNodeType.TASK, request.getName(), team)) {
      // Update template
      if (!relationshipServiceImpl.hasTeamRelationship(Optional.of(RelationshipNodeType.TASK),
          Optional.of(request.getName()), RelationshipLabel.BELONGSTO, team, false)) {
        //TODO - change error to don't have access
        throw new BoomerangException(BoomerangError.TASKTEMPLATE_INVALID_NAME, request.getName());
      }
      String ref = relationshipServiceImpl.getRefFromSlug(RelationshipNodeType.TASK, request.getName());
      request.setId(ref);
      return this.internalApply(request, replace);
    } else {
      return this.create(request, team);
    }
  }
  
  @Override
  public TaskTemplate apply(TaskTemplate request, boolean replace) {
    if (request.getName().isBlank() || !request.getName().matches(NAME_REGEX)) {
      throw new BoomerangException(BoomerangError.TASKTEMPLATE_INVALID_NAME, request.getName());
    }
    if (relationshipServiceImpl.doesSlugExistForType(RelationshipNodeType.GLOBALTASK, request.getName())) {
      String ref = relationshipServiceImpl.getRefFromSlug(RelationshipNodeType.GLOBALTASK, request.getName());
      request.setId(ref);
      return this.internalApply(request, replace);
    } else {
      return this.create(request);
    }
  }
  
  private TaskTemplate internalApply(TaskTemplate request, boolean replace) {  
    // Set verfied to false - this is only able to be set via Engine or Loader
    request.setVerified(false);
    
    // Update Changelog
    updateChangeLog(request.getChangelog());
    
    // Process Parameters - ensure Param and Config share the same params
    request.getSpec().setParams(ParameterUtil.abstractParamsToParamSpecs(request.getConfig(), request.getSpec().getParams()));
    request.setConfig(ParameterUtil.paramSpecToAbstractParam(request.getSpec().getParams(), request.getConfig()));
    
    TaskTemplate template = engineClient.applyTaskTemplate(request, replace);
    switchChangeLogAuthorToUserName(template.getChangelog());
    
    return template;
  }

  // Override changelog date and set author. Used on creation/update of TaskTemplate
  private void updateChangeLog(ChangeLog changelog) {
    if (changelog == null) {
      changelog = new ChangeLog();
    }
    changelog.setDate(new Date());
    if (identityService.getCurrentIdentity().getPrincipal() != null) {
      changelog.setAuthor(identityService.getCurrentIdentity().getPrincipal());
    }
  }

  //TODO - need to make more performant
  private void switchChangeLogAuthorToUserName(ChangeLog changelog) {
    if (changelog != null && changelog.getAuthor() != null) {
      Optional<User> user = identityService.getUserByID(changelog.getAuthor());
      if (user.isPresent()) {
        changelog.setAuthor(user.get().getDisplayName().isEmpty() ? user.get().getName() : user.get().getDisplayName());
      } else {
        changelog.setAuthor("---");
      }
    }
  }

  @Override
  public TektonTask getAsTekton(String name, Optional<Integer> version, String team) {
    TaskTemplate template = this.get(name, version, team);
    return TektonConverter.convertTaskTemplateToTektonTask(template);
  }

  @Override
  public TektonTask getAsTekton(String name, Optional<Integer> version) {
    TaskTemplate template = this.get(name, version);
    return TektonConverter.convertTaskTemplateToTektonTask(template);
  }

  @Override
  public TektonTask createAsTekton(TektonTask tektonTask, String team) {
    TaskTemplate template = TektonConverter.convertTektonTaskToTaskTemplate(tektonTask);
    this.create(template, team);
    return tektonTask;
  }

  @Override
  public TektonTask createAsTekton(TektonTask tektonTask) {
    TaskTemplate template = TektonConverter.convertTektonTaskToTaskTemplate(tektonTask);
    this.create(template);
    return tektonTask;
  }

  @Override
  public TektonTask applyAsTekton(TektonTask tektonTask, boolean replace, String team) {
    TaskTemplate template = TektonConverter.convertTektonTaskToTaskTemplate(tektonTask);
    this.apply(template, replace, team);
    return tektonTask;
  }

  @Override
  public TektonTask applyAsTekton(TektonTask tektonTask, boolean replace) {
    TaskTemplate template = TektonConverter.convertTektonTaskToTaskTemplate(tektonTask);
    this.apply(template, replace);
    return tektonTask;
  }

  @Override
  public void validateAsTekton(TektonTask tektonTask) {
    TektonConverter.convertTektonTaskToTaskTemplate(tektonTask);
  }

  @Override
  public List<ChangeLogVersion> changelog(String name, String team) {
    if (name.isBlank() || !name.matches(NAME_REGEX)) {
      throw new BoomerangException(BoomerangError.TASKTEMPLATE_INVALID_NAME, name);
    }
    if (relationshipServiceImpl.doesSlugExistInTeam(RelationshipNodeType.TASK, name, team)) {
      if (!relationshipServiceImpl.hasTeamRelationship(Optional.of(RelationshipNodeType.TASK),
          Optional.of(name), RelationshipLabel.BELONGSTO, team, false)) {
        //TODO - change error to don't have access
        throw new BoomerangException(BoomerangError.TASKTEMPLATE_INVALID_NAME, name);
      }
      String ref = relationshipServiceImpl.getRefFromSlug(RelationshipNodeType.TASK, name);
      return internalChangelog(ref);
    }
    //TODO - change error to don't have access
    throw new BoomerangException(BoomerangError.TASKTEMPLATE_INVALID_NAME, name);
  }

  @Override
  public List<ChangeLogVersion> changelog(String name) {
    if (relationshipServiceImpl.doesSlugExistForType(RelationshipNodeType.GLOBALTASK, name)) {
      String ref = relationshipServiceImpl.getRefFromSlug(RelationshipNodeType.GLOBALTASK, name);
      return internalChangelog(ref);
    }
    //TODO - change error to don't have access
    throw new BoomerangException(BoomerangError.TASKTEMPLATE_INVALID_NAME, name);
  }
  
  private List<ChangeLogVersion> internalChangelog(String id) {
    List<ChangeLogVersion> changeLog = engineClient.getTaskTemplateChangeLog(id);
    changeLog.forEach(clv -> switchChangeLogAuthorToUserName(clv));
    return changeLog;
  }

  /*
   * Deletes a TaskTemplate - team is required as you cannot delete a global template (only make
   * inactive)
   */
  @Override
  public void delete(String name, String team) {
    if (Objects.isNull(name) || name.isBlank()) {
      throw new BoomerangException(BoomerangError.TASKTEMPLATE_INVALID_REF);
    }
    if (relationshipServiceImpl.doesSlugExistInTeam(RelationshipNodeType.TASK, name, team)) {
      if (!relationshipServiceImpl.hasTeamRelationship(Optional.of(RelationshipNodeType.TASK),
          Optional.of(name), RelationshipLabel.BELONGSTO, team, false)) {
        //TODO - change error to don't have access
        throw new BoomerangException(BoomerangError.TASKTEMPLATE_INVALID_NAME, name);
      }
      String ref = relationshipServiceImpl.getRefFromSlug(RelationshipNodeType.TASK, name);
      engineClient.deleteTaskTemplate(ref);
    }
    //TODO - change error to don't have access
    throw new BoomerangException(BoomerangError.TASKTEMPLATE_INVALID_NAME, name);
  }
}
