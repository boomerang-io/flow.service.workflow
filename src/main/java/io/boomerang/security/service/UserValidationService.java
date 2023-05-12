package io.boomerang.security.service;

public interface UserValidationService {

  void validateUserForTeam(String teamId);

  void validateUserForWorkflow(String workflowId);

  void validateUserAdminOrOperator();

  void validateUserById(String userId);

}
