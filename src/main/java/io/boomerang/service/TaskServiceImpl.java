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
import io.boomerang.client.TaskResponsePage;
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
import io.boomerang.model.User;
import io.boomerang.model.enums.RelationshipLabel;
import io.boomerang.model.enums.RelationshipType;
import io.boomerang.model.ref.ChangeLog;
import io.boomerang.model.ref.ChangeLogVersion;
import io.boomerang.model.ref.Task;
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
public class TaskServiceImpl implements TaskService {

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
   */
  @Override
  public Task get(String team, String name, Optional<Integer> version) {
    // Checks principal and provided Task has relationship to Team.
    if (!Objects.isNull(name) && !name.isBlank() && relationshipServiceImpl.hasTeamRelationship(Optional.of(RelationshipType.TASK),
          Optional.of(name), RelationshipLabel.BELONGSTO, team, false)) {
        String ref = relationshipServiceImpl.getRefFromSlug(RelationshipType.TASK, name);
        return internalGet(ref, version);
    }
    throw new BoomerangException(BoomerangError.TASK_INVALID_REF, name, version.isPresent() ? version.get() : "latest");
  }
    
  /*
   * Retrieve a GLOBALTASK by name and optional version. If no version specified, will retrieve the latest.
   */
  @Override
  public Task get(String name, Optional<Integer> version) {
    if (!Objects.isNull(name) && !name.isBlank()
        && relationshipServiceImpl.hasRootRelationship(
            RelationshipType.GLOBALTASK, name)) {
      String ref = relationshipServiceImpl.getRefFromSlug(RelationshipType.GLOBALTASK, name);
      return internalGet(ref, version);
    }
    throw new BoomerangException(BoomerangError.TASK_INVALID_REF, name, version.isPresent() ? version.get() : "latest");
  }

  private Task internalGet(String id, Optional<Integer> version) {
    Task taskTemplate = engineClient.getTask(id, version);
    
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
  public TaskResponsePage query(String queryTeam, Optional<Integer> queryLimit, Optional<Integer> queryPage, Optional<Direction> querySort,
      Optional<List<String>> queryLabels, Optional<List<String>> queryStatus,
      Optional<List<String>> queryNames) {
    
    //The filteredRefs call will check for having a relationship
    List<String> refs = relationshipServiceImpl.getFilteredRefs(Optional.of(RelationshipType.TASK), queryNames, RelationshipLabel.BELONGSTO, RelationshipType.TEAM, queryTeam, false);
    LOGGER.debug("Task Refs: {}", refs.toString());
    if (refs == null || refs.size() == 0) {
      return new TaskResponsePage();
    }
    return internalQuery(queryLimit, queryPage, querySort, queryLabels, queryStatus, refs);
  }
  
  /*
   * Query for GLOBALTASKS.
   */
  @Override
  public TaskResponsePage query(Optional<Integer> queryLimit, Optional<Integer> queryPage, Optional<Direction> querySort,
      Optional<List<String>> queryLabels, Optional<List<String>> queryStatus,
      Optional<List<String>> queryNames) {
    
    List<String> refs = relationshipServiceImpl.getRootRefs(RelationshipType.GLOBALTASK, queryNames);
    LOGGER.debug("Global Task Refs: {}", refs.toString());
    if (refs == null || refs.size() == 0) {
      return new TaskResponsePage();
    }
    return internalQuery(queryLimit, queryPage, querySort, queryLabels, queryStatus, refs);
  }
  
  private TaskResponsePage internalQuery(Optional<Integer> queryLimit, Optional<Integer> queryPage, Optional<Direction> querySort,
      Optional<List<String>> queryLabels, Optional<List<String>> queryStatus,
      List<String> queryRefs) {
    TaskResponsePage response = engineClient.queryTask(queryLimit, queryPage, querySort,
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
   * Creates the Task and Relationship
   */
  @Override
  public Task create(String team, Task request) {
    // Validate Access
    if (!relationshipServiceImpl.hasTeamRelationship(Optional.empty(),
        Optional.empty(), RelationshipLabel.MEMBEROF, team, false)) {
      throw new BoomerangException(BoomerangError.PERMISSION_DENIED);
    }
    
    // Check name matches the requirements
    if (request.getName().isBlank() || !request.getName().matches(NAME_REGEX)) {
      throw new BoomerangException(BoomerangError.TASK_INVALID_NAME, request.getName());
    }
    
    // Check Slugs for Tasks in team
    if (relationshipServiceImpl.doesSlugOrRefExistForTypeInTeam(RelationshipType.TASK, request.getName(), team)) {
      throw new BoomerangException(BoomerangError.TASK_ALREADY_EXISTS, request.getName());
    }
    
    // Create Task
    Task task = internalCreate(request);

    // Create Relationship
    relationshipServiceImpl.createNodeWithTeamConnection(RelationshipType.TASK, task.getId(), task.getName(), team, Optional.empty());
    
    // Remove ID
    task.setId(null);
    return task;    
  }

  @Override
  public Task create(Task request) {
    // Check name matches the requirements
    if (request.getName().isBlank() || !request.getName().matches(NAME_REGEX)) {
      throw new BoomerangException(BoomerangError.TASK_INVALID_NAME, request.getName());
    }
    
    // Check Slugs for GlobalTasks
    if (relationshipServiceImpl.doesSlugOrRefExistForType(RelationshipType.GLOBALTASK, request.getName())) {
      throw new BoomerangException(BoomerangError.TASK_ALREADY_EXISTS, request.getName());
    }
    
    // Create Task
    Task task = internalCreate(request);

    // Create Relationship
    relationshipServiceImpl.createNode(RelationshipType.GLOBALTASK, task.getId(), task.getName(), Optional.empty());

    // Remove ID
    task.setId(null);
    return task;  
  }
  
  private Task internalCreate(Task request) {
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
    Task taskTemplate = engineClient.createTask(request);
    switchChangeLogAuthorToUserName(taskTemplate.getChangelog());

    return taskTemplate;
  }

  /*
   * Apply allows you to create a new version as well as create new
   */
  @Override
  public Task apply(String team, String name, Task request, boolean replace) {
    if (request.getName().isBlank() || !request.getName().matches(NAME_REGEX)) {
      throw new BoomerangException(BoomerangError.TASK_INVALID_NAME, request.getName());
    }
    // Check if slug exists
    // TODO evaluate if this check can change - can we just check for relationship to team for this object or will that also check principal's relationship to team.
    if (relationshipServiceImpl.doesSlugOrRefExistForTypeInTeam(RelationshipType.TASK, request.getName(), team)) {
      if (!relationshipServiceImpl.hasTeamRelationship(Optional.of(RelationshipType.TASK),
          Optional.of(request.getName()), RelationshipLabel.BELONGSTO, team, false)) {
        throw new BoomerangException(BoomerangError.TASK_INVALID_REF, name, request.getVersion() != null ? request.getVersion() : "latest");
      }
      String ref = relationshipServiceImpl.getRefFromSlug(RelationshipType.TASK, request.getName());
      request.setId(ref);
      Task task = this.internalApply(name, request, replace);
      
      //Update relationship if slug changes
      if (request.getName() != null && !request.getName().isBlank() && !name.equals(request.getName())) {
        relationshipServiceImpl.updateNodeSlug(RelationshipType.GLOBALTASK, name, request.getName());
      }

      // Remove ID
      task.setId(null);
      return task;
    } else {
      return this.create(team, request);
    }
  }
  
  @Override
  public Task apply(String name, Task request, boolean replace) {
    if (request.getName().isBlank() || !request.getName().matches(NAME_REGEX)) {
      throw new BoomerangException(BoomerangError.TASK_INVALID_NAME, name);
    }
    if (relationshipServiceImpl.doesSlugOrRefExistForType(RelationshipType.GLOBALTASK, name)) {
      String ref = relationshipServiceImpl.getRefFromSlug(RelationshipType.GLOBALTASK, name);
      request.setId(ref);
      Task task = this.internalApply(name, request, replace);
      
      //Update relationship if slug changes
      if (request.getName() != null && !request.getName().isBlank() && !name.equals(request.getName())) {
        relationshipServiceImpl.updateNodeSlug(RelationshipType.GLOBALTASK, name, request.getName());
      }

      // Remove ID
      task.setId(null);
      return task;
    } else {
      return this.create(request);
    }
  }
  
  private Task internalApply(String name, Task request, boolean replace) {  
    // Set verfied to false - this is only able to be set via Engine or Loader
    request.setVerified(false);
    
    // Update Changelog
    updateChangeLog(request.getChangelog());
    
    // Process Parameters - ensure Param and Config share the same params
    request.getSpec().setParams(ParameterUtil.abstractParamsToParamSpecs(request.getConfig(), request.getSpec().getParams()));
    request.setConfig(ParameterUtil.paramSpecToAbstractParam(request.getSpec().getParams(), request.getConfig()));
    
    Task template = engineClient.applyTask(request, replace);
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
  public TektonTask getAsTekton(String team, String name, Optional<Integer> version) {
    Task template = this.get(team, name, version);
    return TektonConverter.convertTaskTemplateToTektonTask(template);
  }

  @Override
  public TektonTask getAsTekton(String name, Optional<Integer> version) {
    Task template = this.get(name, version);
    return TektonConverter.convertTaskTemplateToTektonTask(template);
  }

  @Override
  public TektonTask createAsTekton(String team, TektonTask tektonTask) {
    Task template = TektonConverter.convertTektonTaskToTaskTemplate(tektonTask);
    this.create(team, template);
    return tektonTask;
  }

  @Override
  public TektonTask createAsTekton(TektonTask tektonTask) {
    Task template = TektonConverter.convertTektonTaskToTaskTemplate(tektonTask);
    this.create(template);
    return tektonTask;
  }

  @Override
  public TektonTask applyAsTekton(String team, String name, TektonTask tektonTask, boolean replace) {
    Task template = TektonConverter.convertTektonTaskToTaskTemplate(tektonTask);
    this.apply(team, name, template, replace);
    return tektonTask;
  }

  @Override
  public TektonTask applyAsTekton(String name, TektonTask tektonTask, boolean replace) {
    Task template = TektonConverter.convertTektonTaskToTaskTemplate(tektonTask);
    this.apply(name, template, replace);
    return tektonTask;
  }

  @Override
  public void validateAsTekton(TektonTask tektonTask) {
    TektonConverter.convertTektonTaskToTaskTemplate(tektonTask);
  }

  @Override
  public List<ChangeLogVersion> changelog(String team, String name) {
    if (name.isBlank() || !name.matches(NAME_REGEX)) {
      throw new BoomerangException(BoomerangError.TASK_INVALID_NAME, name);
    }
    if (relationshipServiceImpl.doesSlugOrRefExistForTypeInTeam(RelationshipType.TASK, name, team)) {
      if (!relationshipServiceImpl.hasTeamRelationship(Optional.of(RelationshipType.TASK),
          Optional.of(name), RelationshipLabel.BELONGSTO, team, false)) {
        //TODO - change error to don't have access
        throw new BoomerangException(BoomerangError.TASK_INVALID_NAME, name);
      }
      String ref = relationshipServiceImpl.getRefFromSlug(RelationshipType.TASK, name);
      return internalChangelog(ref);
    }
    //TODO - change error to don't have access
    throw new BoomerangException(BoomerangError.TASK_INVALID_NAME, name);
  }

  @Override
  public List<ChangeLogVersion> changelog(String name) {
    if (relationshipServiceImpl.doesSlugOrRefExistForType(RelationshipType.GLOBALTASK, name)) {
      String ref = relationshipServiceImpl.getRefFromSlug(RelationshipType.GLOBALTASK, name);
      return internalChangelog(ref);
    }
    //TODO - change error to don't have access
    throw new BoomerangException(BoomerangError.TASK_INVALID_NAME, name);
  }
  
  private List<ChangeLogVersion> internalChangelog(String id) {
    List<ChangeLogVersion> changeLog = engineClient.getTaskChangeLog(id);
    changeLog.forEach(clv -> switchChangeLogAuthorToUserName(clv));
    return changeLog;
  }

  /*
   * Deletes a TaskTemplate - team is required as you cannot delete a global template (only make
   * inactive)
   */
  @Override
  public void delete(String team, String name) {
    if (Objects.isNull(name) || name.isBlank()) {
      throw new BoomerangException(BoomerangError.TASK_INVALID_REF);
    }
    if (relationshipServiceImpl.doesSlugOrRefExistForTypeInTeam(RelationshipType.TASK, name, team)) {
      if (!relationshipServiceImpl.hasTeamRelationship(Optional.of(RelationshipType.TASK),
          Optional.of(name), RelationshipLabel.BELONGSTO, team, false)) {
        //TODO - change error to don't have access
        throw new BoomerangException(BoomerangError.TASK_INVALID_NAME, name);
      }
      String ref = relationshipServiceImpl.getRefFromSlug(RelationshipType.TASK, name);
      engineClient.deleteTask(ref);
    }
    //TODO - change error to don't have access
    throw new BoomerangException(BoomerangError.TASK_INVALID_NAME, name);
  }
}
