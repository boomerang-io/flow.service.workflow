package io.boomerang.security.model;

public enum TokenPermission {
  
  ANY_READ(TokenObject.any, TokenAccess.read),
  ANY_WRITE(TokenObject.any, TokenAccess.write),
  ANY_DELETE(TokenObject.any, TokenAccess.delete),
  ANY_ACTION(TokenObject.any, TokenAccess.action),
  WORKFLOW_READ(TokenObject.workflow, TokenAccess.read),
  WORKFLOW_WRITE(TokenObject.workflow, TokenAccess.write),
  WORKFLOW_DELETE(TokenObject.workflow, TokenAccess.delete),
  WORKFLOWRUN_READ(TokenObject.workflowrun, TokenAccess.read),
  WORKFLOWRUN_ACTION(TokenObject.workflowrun, TokenAccess.action),
  WORKFLOWTEMPLATE_READ(TokenObject.workflowtemplate, TokenAccess.read),
  WORKFLOWTEMPLATE_WRITE(TokenObject.workflowtemplate, TokenAccess.write),
  WORKFLOWTEMPLATE_DELETE(TokenObject.workflowtemplate, TokenAccess.delete),
  TASKRUN_READ(TokenObject.taskrun, TokenAccess.read),
  TASKRUN_ACTION(TokenObject.taskrun, TokenAccess.action),
  TASKTEMPLATE_READ(TokenObject.tasktemplate, TokenAccess.read),
  TASKTEMPLATE_WRITE(TokenObject.tasktemplate, TokenAccess.write),
  TASKTEMPLATE_DELETE(TokenObject.tasktemplate, TokenAccess.delete),
  ACTION_READ(TokenObject.action, TokenAccess.read),
  ACTION_ACTION(TokenObject.action, TokenAccess.action),
  USER_READ(TokenObject.user, TokenAccess.read),
  USER_WRITE(TokenObject.user, TokenAccess.write),
  USER_DELETE(TokenObject.user, TokenAccess.delete),
  TEAM_READ(TokenObject.team, TokenAccess.read),
  TEAM_WRITE(TokenObject.team, TokenAccess.write),
  TEAM_DELETE(TokenObject.team, TokenAccess.delete),
  TOKEN_READ(TokenObject.token, TokenAccess.read),
  TOKEN_WRITE(TokenObject.token, TokenAccess.write),
  TOKEN_DELETE(TokenObject.token, TokenAccess.delete),
  PARAMETER_READ(TokenObject.parameter, TokenAccess.read),
  PARAMETER_WRITE(TokenObject.parameter, TokenAccess.write),
  PARAMETER_DELETE(TokenObject.parameter, TokenAccess.delete);

  private TokenObject object;
  private TokenAccess access;

  public TokenObject object() {
    return object;
  }

  public TokenAccess access() {
    return access;
  }

  TokenPermission(TokenObject object, TokenAccess access) {
    this.object = object;
    this.access = access;
  }
}


