package io.boomerang.service;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
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
import io.boomerang.model.enums.RelationshipNodeType;
import io.boomerang.model.enums.RelationshipLabel;
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
  
  private static final String NAME_SEPARATOR = "_";

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
  public TaskTemplate get(String name, Optional<Integer> version, Optional<String> team) {
    if (!Objects.isNull(name) && !name.isBlank()) {
      // Check if requester has access to refs
      // TODO: determine if all users need to be able to access (READ) but not edit (CREATE, UPDATE, DELETE)
      List<String> refs = relationshipService.getFilteredFromRefs(Optional.of(RelationshipNodeType.TASKTEMPLATE),
          Optional.of(List.of(name)), Optional.of(RelationshipLabel.BELONGSTO), Optional.of(RelationshipNodeType.GLOBAL), Optional.empty());
      if (refs.isEmpty()) {
        refs = relationshipService.getFilteredFromRefs(Optional.of(RelationshipNodeType.TASKTEMPLATE),
            Optional.of(List.of(name)), Optional.of(RelationshipLabel.BELONGSTO), Optional.of(RelationshipNodeType.TEAM), Optional.empty());
      }
      
      // Prefix name with team scope
      String scopedName = name;
      if (team.isPresent()) {
        scopedName = team.get() + NAME_SEPARATOR + name;
      }
      
      if (!refs.isEmpty()) {
        TaskTemplate taskTemplate = engineClient.getTaskTemplate(scopedName, version);
        
        // Process Parameters - create configs for any Params
        taskTemplate.getSpec().setParams(ParameterUtil.abstractParamsToParamSpecs(taskTemplate.getConfig(), taskTemplate.getSpec().getParams()));
        taskTemplate.setConfig(ParameterUtil.paramSpecToAbstractParam(taskTemplate.getSpec().getParams(), taskTemplate.getConfig()));
        
        // Switch from UserId to Users Name
        switchChangeLogAuthorToUserName(taskTemplate.getChangelog());
        LOGGER.debug("Changelog: " + taskTemplate.getChangelog().toString());
        //Revert name for end user
        taskTemplate.setName(name);
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
      Optional<List<String>> queryNames, Optional<String> queryTeam) {
    
    // Get Refs that request has access to
    List<String> refs = null;
    if (queryTeam.isPresent()) {
      refs =
          relationshipService.getFilteredFromRefs(Optional.of(RelationshipNodeType.TASKTEMPLATE), queryNames,
              Optional.of(RelationshipLabel.BELONGSTO), Optional.of(RelationshipNodeType.TEAM),
              Optional.of(List.of(queryTeam.get())));
      //Return empty with no teams, otherwise when sending to the engine, the refs is empty and all task-templates will be returned.
      if (refs == null || refs.size() == 0) {
        return new TaskTemplateResponsePage();
      }
      
      //Prefix name with team scope
      refs = refs.stream().map(t -> queryTeam.get() + NAME_SEPARATOR + t).collect(Collectors.toList());
    } else {
      refs =
          relationshipService.getFilteredFromRefs(Optional.of(RelationshipNodeType.TASKTEMPLATE), queryNames,
              Optional.of(RelationshipLabel.BELONGSTO), Optional.of(RelationshipNodeType.GLOBAL),
              Optional.empty());
    }
    LOGGER.debug("Query Refs: {}", refs.toString());

    TaskTemplateResponsePage response = engineClient.queryTaskTemplates(queryLimit, queryPage, querySort,
        queryLabels, queryStatus, Optional.of(refs));

    if (!response.getContent().isEmpty()) {
      response.getContent().forEach(t -> {
        switchChangeLogAuthorToUserName(t.getChangelog());
        t.setName(t.getName().contains(NAME_SEPARATOR) ? t.getName().split(NAME_SEPARATOR)[1]: t.getName());
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
      Optional<String> team) {
    // Set verified to false - this is only able to be set via Engine or Loader
    request.setVerified(false);
    
    // Process Parameters - ensure Param and Config share the same params
    ParameterUtil.abstractParamsToParamSpecs(request.getConfig(), request.getSpec().getParams());
    ParameterUtil.paramSpecToAbstractParam(request.getSpec().getParams(), request.getConfig());
    
    // Update Changelog
    updateChangeLog(request.getChangelog());
    
    String templateName = request.getName();
    // Prefix name with team scope
    if (team.isPresent()) {
      request.setName(team.get() + NAME_SEPARATOR + request.getName());
    }
    
    // Come back to this once we have separated the controllers - works better for scope checks.
    TaskTemplate taskTemplate = engineClient.createTaskTemplate(request);
    if (team.isPresent()) {
      //Check user has access to team

      List<String> refs = relationshipService.getFilteredFromRefs(Optional.of(RelationshipNodeType.USER),
          Optional.empty(), Optional.of(RelationshipLabel.MEMBEROF), Optional.of(RelationshipNodeType.TEAM), Optional.of(List.of(team.get())));
      if (refs.isEmpty()) {
        throw new BoomerangException(BoomerangError.TASKTEMPLATE_INVALID_REF);
      }
      // Create BELONGSTO relationship for mapping Workflow to Owner
      relationshipService.addRelationshipRef(RelationshipNodeType.TASKTEMPLATE, templateName, RelationshipLabel.BELONGSTO, RelationshipNodeType.TEAM,
          team, Optional.empty());
    } else {
      // Creates a relationship to GLOBAL
      //TODO: check user is ADMIN
      relationshipService.addRelationshipRef(RelationshipNodeType.TASKTEMPLATE,
          templateName, RelationshipLabel.BELONGSTO ,RelationshipNodeType.GLOBAL, Optional.empty(), Optional.empty());
    }
    switchChangeLogAuthorToUserName(taskTemplate.getChangelog());
    
    //Revert name for end user
    taskTemplate.setName(templateName);
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
    List<String> refs = relationshipService.getFilteredFromRefs(Optional.of(RelationshipNodeType.TASKTEMPLATE),
        Optional.of(List.of(templateName)), Optional.of(RelationshipLabel.BELONGSTO), Optional.of(RelationshipNodeType.TEAM), Optional.of(List.of(team.get())));
      if (refs.isEmpty()) {
        return this.create(request, team);
      }
    } else {
      List<String> refs = relationshipService.getFilteredFromRefs(Optional.of(RelationshipNodeType.TASKTEMPLATE),
          Optional.of(List.of(templateName)), Optional.of(RelationshipLabel.BELONGSTO), Optional.of(RelationshipNodeType.GLOBAL), Optional.empty());
      if (refs.isEmpty()) {
        return this.create(request, team);
      }
    }

    if (templateName != null && !templateName.isBlank()) {
      // Set verfied to false - this is only able to be set via Engine or Loader
      request.setVerified(false);
      
      // Prefix name with team scope
      if (team.isPresent()) {
        request.setName(team.get() + NAME_SEPARATOR + templateName);
      }
      
      // Update Changelog
      updateChangeLog(request.getChangelog());
      
      // Process Parameters - ensure Param and Config share the same params
      request.getSpec().setParams(ParameterUtil.abstractParamsToParamSpecs(request.getConfig(), request.getSpec().getParams()));
      request.setConfig(ParameterUtil.paramSpecToAbstractParam(request.getSpec().getParams(), request.getConfig()));
      
      TaskTemplate template = engineClient.applyTaskTemplate(request, replace);
      switchChangeLogAuthorToUserName(template.getChangelog());
      template.setName(templateName);
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
  public TektonTask getAsTekton(String name, Optional<Integer> version, Optional<String> team) {
    TaskTemplate template = this.get(name, version, team);
    if (template != null) {
      return TektonConverter.convertTaskTemplateToTektonTask(template);
    }
    throw new BoomerangException(BoomerangError.TASKTEMPLATE_INVALID_REF);
  }

  @Override
  public TektonTask createAsTekton(TektonTask tektonTask, Optional<String> team) {
    TaskTemplate template = TektonConverter.convertTektonTaskToTaskTemplate(tektonTask);
    this.create(template, team);
    return tektonTask;
  }

  @Override
  public TektonTask applyAsTekton(TektonTask tektonTask, boolean replace, Optional<String> team) {
    TaskTemplate template = TektonConverter.convertTektonTaskToTaskTemplate(tektonTask);
    this.apply(template, replace, team);
    return tektonTask;
  }

  @Override
  public void validateAsTekton(TektonTask tektonTask) {
    TektonConverter.convertTektonTaskToTaskTemplate(tektonTask);
  }

  @Override
  public List<ChangeLogVersion> changelog(String name, Optional<String> team) {
    if (!Objects.isNull(name) && !name.isBlank()) {
      if (team.isPresent()) {
        List<String> refs =
          relationshipService.getFilteredFromRefs(Optional.of(RelationshipNodeType.TASKTEMPLATE),
              Optional.of(List.of(name)), Optional.of(RelationshipLabel.BELONGSTO),
              Optional.of(RelationshipNodeType.TEAM), Optional.of(List.of(team.get())));
        if (!refs.isEmpty()) {
          List<ChangeLogVersion> changeLog = engineClient.getTaskTemplateChangeLog(team.get() + NAME_SEPARATOR + name);
          changeLog.forEach(clv -> switchChangeLogAuthorToUserName(clv));
          return changeLog;
        }
      } else {
        List<String> refs = relationshipService.getFilteredFromRefs(Optional.of(RelationshipNodeType.TASKTEMPLATE),
            Optional.of(List.of(name)), Optional.of(RelationshipLabel.BELONGSTO),
            Optional.of(RelationshipNodeType.GLOBAL), Optional.empty());
        if (!refs.isEmpty()) {
          List<ChangeLogVersion> changeLog = engineClient.getTaskTemplateChangeLog(name);
          changeLog.forEach(clv -> switchChangeLogAuthorToUserName(clv));
          return changeLog;
        }
      }
      
    }
    throw new BoomerangException(BoomerangError.TASKTEMPLATE_INVALID_REF);
  }

  /*
   * Deletes a TaskTemplate - team is required as you cannot delete a global template (only make
   * inactive)
   */
  @Override
  public void delete(String name, String team) {
    if (Objects.isNull(name) || name.isBlank() || Objects.isNull(team) || team.isBlank()) {
      throw new BoomerangException(BoomerangError.TASKTEMPLATE_INVALID_REF);
    }

    // Check if requester has access
    List<String> refs =
        relationshipService.getFilteredFromRefs(Optional.of(RelationshipNodeType.TASKTEMPLATE),
            Optional.of(List.of(name)), Optional.of(RelationshipLabel.BELONGSTO),
            Optional.of(RelationshipNodeType.TEAM), Optional.empty());

    // Prefix name with team scope

    if (refs.isEmpty()) {
      throw new BoomerangException(BoomerangError.TASKTEMPLATE_INVALID_REF);
    }

    String scopedName = team + NAME_SEPARATOR + name;
    engineClient.deleteTaskTemplate(scopedName);
  }
}
