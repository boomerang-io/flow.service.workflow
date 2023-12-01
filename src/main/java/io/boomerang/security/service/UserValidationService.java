package io.boomerang.security.service;

import io.boomerang.model.TemplateScope;
import io.boomerang.mongo.entity.FlowUserEntity;
import io.boomerang.mongo.model.WorkflowScope;

public interface UserValidationService {

  void validateUserForTeam(String teamId);
  
  void validateUserForTeam(FlowUserEntity user, String teamId);

  void validateUserAdminOrOperator();
  
  void validateUserAdminOrOperator(FlowUserEntity user);
  
  void validateUserAccessForWorkflow(WorkflowScope scope, String flowTeamId, String flowOwnerUserId, boolean editable);
  
  void validateUserAccessForWorkflow(FlowUserEntity user, WorkflowScope scope, String flowTeamId, String flowOwnerUserId, boolean editable);

  void validateUserAccessForTaskTemplate(TemplateScope scope, String taskTeamId, boolean editable);
  
  void validateUserAccessForTaskTemplate(FlowUserEntity user, TemplateScope scope, String taskTeamId, boolean editable);
}
