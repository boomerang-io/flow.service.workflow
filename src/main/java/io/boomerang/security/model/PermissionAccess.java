//package io.boomerang.security.model;
//
//public enum PermissionAccess {
//  
//  ANY_READ(PermissionScope.any, PermissionAction.read),
//  ANY_WRITE(PermissionScope.any, PermissionAction.write),
//  ANY_DELETE(PermissionScope.any, PermissionAction.delete),
//  ANY_ACTION(PermissionScope.any, PermissionAction.action),
//  WORKFLOW_READ(PermissionScope.workflow, PermissionAction.read),
//  WORKFLOW_WRITE(PermissionScope.workflow, PermissionAction.write),
//  WORKFLOW_DELETE(PermissionScope.workflow, PermissionAction.delete),
//  WORKFLOWRUN_READ(PermissionScope.workflowrun, PermissionAction.read),
//  WORKFLOWRUN_ACTION(PermissionScope.workflowrun, PermissionAction.action),
//  WORKFLOWTEMPLATE_READ(PermissionScope.workflowtemplate, PermissionAction.read),
//  WORKFLOWTEMPLATE_WRITE(PermissionScope.workflowtemplate, PermissionAction.write),
//  WORKFLOWTEMPLATE_DELETE(PermissionScope.workflowtemplate, PermissionAction.delete),
//  TASKRUN_READ(PermissionScope.taskrun, PermissionAction.read),
//  TASKRUN_ACTION(PermissionScope.taskrun, PermissionAction.action),
//  TASKTEMPLATE_READ(PermissionScope.tasktemplate, PermissionAction.read),
//  TASKTEMPLATE_WRITE(PermissionScope.tasktemplate, PermissionAction.write),
//  TASKTEMPLATE_DELETE(PermissionScope.tasktemplate, PermissionAction.delete),
//  ACTION_READ(PermissionScope.action, PermissionAction.read),
//  ACTION_ACTION(PermissionScope.action, PermissionAction.action),
//  USER_READ(PermissionScope.user, PermissionAction.read),
//  USER_WRITE(PermissionScope.user, PermissionAction.write),
//  USER_DELETE(PermissionScope.user, PermissionAction.delete),
//  TEAM_READ(PermissionScope.team, PermissionAction.read),
//  TEAM_WRITE(PermissionScope.team, PermissionAction.write),
//  TEAM_DELETE(PermissionScope.team, PermissionAction.delete),
//  TOKEN_READ(PermissionScope.token, PermissionAction.read),
//  TOKEN_WRITE(PermissionScope.token, PermissionAction.write),
//  TOKEN_DELETE(PermissionScope.token, PermissionAction.delete),
//  PARAMETER_READ(PermissionScope.parameter, PermissionAction.read),
//  PARAMETER_WRITE(PermissionScope.parameter, PermissionAction.write),
//  PARAMETER_DELETE(PermissionScope.parameter, PermissionAction.delete);
//
//  private PermissionScope object;
//  private PermissionAction access;
//
//  public PermissionScope object() {
//    return object;
//  }
//
//  public PermissionAction access() {
//    return access;
//  }
//
//  PermissionAccess(PermissionScope object, PermissionAction access) {
//    this.object = object;
//    this.access = access;
//  }
//}
//
//
