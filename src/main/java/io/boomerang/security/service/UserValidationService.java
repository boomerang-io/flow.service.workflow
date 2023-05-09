package io.boomerang.security.service;

public interface UserValidationService {

  void validateUserForTeam(String teamId);

  void validateUserForWorkflow(String workflowId);

  void validateUser();

}
