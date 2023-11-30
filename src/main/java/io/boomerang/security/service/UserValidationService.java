package io.boomerang.security.service;

import io.boomerang.model.TemplateScope;
import io.boomerang.mongo.model.WorkflowScope;

public interface UserValidationService {

  void validateUserForTeam(String teamId);

  void validateUserForWorkflow(String workflowId);

  void validateUserAdminOrOperator();
  
  void validateUserAccessForWorkflow(WorkflowScope scope, String flowTeamId, String flowOwnerUserId, boolean editable);

  void validateUserAccessForTaskTemplate(TemplateScope scope, String taskTeamId, boolean editable);
}
