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
import io.boomerang.model.enums.RelationshipRef;
import io.boomerang.model.enums.RelationshipType;
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

  @Autowired
  private IdentityService identityService;

  /*
   * Get TaskTemplate by name and optional version. If no version specified, will retrieve the latest.
   */
  @Override
  public TaskTemplate get(String name, Optional<Integer> version) {
    if (!Objects.isNull(name) && !name.isBlank()) {
      // Check if requester has access to refs
      // TODO: determine if all users need to be able to access (READ) but not edit (CREATE, UPDATE, DELETE)
      List<String> refs = relationshipService.getFilteredFromRefs(Optional.of(RelationshipRef.TASKTEMPLATE),
          Optional.of(List.of(name)), Optional.of(RelationshipType.BELONGSTO), Optional.of(RelationshipRef.GLOBAL), Optional.empty());
      if (refs.isEmpty()) {
        refs = relationshipService.getFilteredFromRefs(Optional.of(RelationshipRef.TASKTEMPLATE),
            Optional.of(List.of(name)), Optional.of(RelationshipType.BELONGSTO), Optional.of(RelationshipRef.TEAM), Optional.empty());
      }
      
      if (!refs.isEmpty()) {
        TaskTemplate taskTemplate = engineClient.getTaskTemplate(name, version);
        
        // Process Parameters - create configs for any Params
        if (taskTemplate.getSpec().getParams() != null && !taskTemplate.getSpec().getParams().isEmpty()) {
          ParameterUtil.paramSpecToAbstractParam(taskTemplate.getSpec().getParams(), taskTemplate.getConfig());
        }
        
        // Switch from UserId to Users Name
        switchChangeLogAuthorToUserName(taskTemplate.getChangelog());
        return taskTemplate;
      }
    }
    throw new BoomerangException(BoomerangError.TASKTEMPLATE_INVALID_REF);
  }

  /*
   * Query for Workflows.
   */
  @Override
  public TaskTemplateResponsePage query(Optional<Integer> queryLimit, Optional<Integer> queryPage, Optional<Direction> querySort,
      Optional<List<String>> queryLabels, Optional<List<String>> queryStatus,
      Optional<List<String>> queryNames, Optional<List<String>> queryTeams) {
    
    // Get Refs that request has access to
    List<String> refs = null;
    if (queryTeams.isPresent()) {
      refs =
          relationshipService.getFilteredFromRefs(Optional.of(RelationshipRef.TASKTEMPLATE), queryNames,
              Optional.of(RelationshipType.BELONGSTO), Optional.of(RelationshipRef.TEAM),
              queryTeams);
      //Return empty with no teams, otherwise when sending to the engine, the refs is empty and all task-templates will be returned.
      if (refs == null || refs.size() == 0) {
        return new TaskTemplateResponsePage();
      }
    } else {
      refs =
          relationshipService.getFilteredFromRefs(Optional.of(RelationshipRef.TASKTEMPLATE), queryNames,
              Optional.of(RelationshipType.BELONGSTO), Optional.of(RelationshipRef.GLOBAL),
              Optional.empty());
    }
    LOGGER.debug("Query Refs: {}", refs.toString());

    TaskTemplateResponsePage response = engineClient.queryTaskTemplates(queryLimit, queryPage, querySort,
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
  public TaskTemplate create(TaskTemplate request,
      Optional<String> team) {
    // Set verfied to false - this is only able to be set via Engine or Loader
    request.setVerified(false);
    
    // Process Parameters - ensure Param and Config share the same params
    ParameterUtil.abstractParamsToParamSpecs(request.getConfig(), request.getSpec().getParams());
    ParameterUtil.paramSpecToAbstractParam(request.getSpec().getParams(), request.getConfig());
    
    // Update Changelog
    updateChangeLog(request.getChangelog());
    
    // TODO: add a check that they are prefixed with the current team scope OR are a valid Global TaskTemplate
    // Come back to this once we have separated the controllers - works better for scope checks.
    TaskTemplate taskTemplate = engineClient.createTaskTemplate(request);
    if (team.isPresent()) {
      //Check user has access to team

      List<String> refs = relationshipService.getFilteredFromRefs(Optional.of(RelationshipRef.USER),
          Optional.empty(), Optional.of(RelationshipType.MEMBEROF), Optional.of(RelationshipRef.TEAM), Optional.of(List.of(team.get())));
      if (refs.isEmpty()) {
        throw new BoomerangException(BoomerangError.TASKTEMPLATE_INVALID_REF);
      }
      // Create BELONGSTO relationship for mapping Workflow to Owner
      relationshipService.addRelationshipRef(RelationshipRef.TASKTEMPLATE, taskTemplate.getName(), RelationshipType.BELONGSTO, RelationshipRef.TEAM,
          team, Optional.empty());
    } else {
      // Creates a relationship to GLOBAL
      //TODO: check user is ADMIN
      relationshipService.addRelationshipRef(RelationshipRef.TASKTEMPLATE,
          taskTemplate.getName(), RelationshipType.BELONGSTO ,RelationshipRef.GLOBAL, Optional.empty(), Optional.empty());
    }
    switchChangeLogAuthorToUserName(taskTemplate.getChangelog());
    return taskTemplate;
  }

  /*
   * Apply allows you to create a new version as well as create new
   */
  @Override
  public TaskTemplate apply(TaskTemplate request, boolean replace, Optional<String> team) {
    String templateName = request.getName();
    //Refs check is different to normal as we are checking the users access to the team and not the template
    //The engine will check if the template exists or not
    // TODO: figure out what to do with name collisions between teams
    if (team.isPresent()) {
    List<String> refs = relationshipService.getFilteredFromRefs(Optional.of(RelationshipRef.TASKTEMPLATE),
        Optional.of(List.of(templateName)), Optional.of(RelationshipType.BELONGSTO), Optional.of(RelationshipRef.TEAM), Optional.of(List.of(team.get())));
      if (refs.isEmpty()) {
        return this.create(request, team);
      }
    } else {
      List<String> refs = relationshipService.getFilteredFromRefs(Optional.of(RelationshipRef.TASKTEMPLATE),
          Optional.of(List.of(templateName)), Optional.of(RelationshipType.BELONGSTO), Optional.of(RelationshipRef.GLOBAL), Optional.empty());
      if (refs.isEmpty()) {
        return this.create(request, team);
      }
    }

    if (templateName != null && !templateName.isBlank()) {
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
    } else {
      throw new BoomerangException(BoomerangError.TASKTEMPLATE_INVALID_REF);
    }
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

  private void switchChangeLogAuthorToUserName(ChangeLog changelog) {
    //TODO: needs to handle Principal's of team / global
    if (changelog != null && changelog.getAuthor() != null) {
      Optional<User> user = identityService.getUserByID(changelog.getAuthor());
      if (user.isPresent()) {
        changelog.setAuthor(user.get().getName());
      }
    }
  }

  @Override
  public TektonTask getAsTekton(String name, Optional<Integer> version) {
    TaskTemplate template = this.get(name, version);
    if (template != null) {
      return TektonConverter.convertTaskTemplateToTektonTask(template);
    }
    throw new BoomerangException(BoomerangError.TASKTEMPLATE_INVALID_REF);
  }

  @Override
  public TektonTask createAsTekton(TektonTask tektonTask, Optional<String> teamId) {
    TaskTemplate template = TektonConverter.convertTektonTaskToTaskTemplate(tektonTask);
    this.create(template, teamId);
    return tektonTask;
  }

  @Override
  public TektonTask applyAsTekton(TektonTask tektonTask, boolean replace, Optional<String> teamId) {
    TaskTemplate template = TektonConverter.convertTektonTaskToTaskTemplate(tektonTask);
    this.apply(template, replace, teamId);
    return tektonTask;
  }

  @Override
  public void validateAsTekton(TektonTask tektonTask) {
    TektonConverter.convertTektonTaskToTaskTemplate(tektonTask);
  }

  @Override
  public List<ChangeLogVersion> changelog(String name) {
    if (!Objects.isNull(name) && !name.isBlank()) {
      List<String> refs =
          relationshipService.getFilteredFromRefs(Optional.of(RelationshipRef.TASKTEMPLATE),
              Optional.of(List.of(name)), Optional.of(RelationshipType.BELONGSTO),
              Optional.of(RelationshipRef.GLOBAL), Optional.empty());
      if (refs.isEmpty()) {
        refs = relationshipService.getFilteredFromRefs(Optional.of(RelationshipRef.TASKTEMPLATE),
            Optional.of(List.of(name)), Optional.of(RelationshipType.BELONGSTO),
            Optional.of(RelationshipRef.TEAM), Optional.empty());
      }
      if (!refs.isEmpty()) {
        return engineClient.getTaskTemplateChangeLog(name);
      }
    }
    throw new BoomerangException(BoomerangError.TASKTEMPLATE_INVALID_REF);
  }
}
