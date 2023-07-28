package io.boomerang.security.model;

public class Permission {
  private PermissionScope scope;
  private String principal;
  private PermissionAction action;

  public Permission() {
  }

  public Permission(String permission) {
    String[] spread = permission.split("/");
    PermissionScope.valueOf(spread[0]);
  }

  public Permission(PermissionScope scope, String principal, PermissionAction action) {
      this.scope = scope;
      this.principal = principal;
      this.action = action;
  }

  public PermissionScope getScope() {
      return scope;
  }

  public void setScope(PermissionScope scope) {
      this.scope = scope;
  }

  public String getPrincipal() {
      return principal;
  }

  public void setPrincipal(String principal) {
      this.principal = principal;
  }

  public PermissionAction getAction() {
      return action;
  }

  public void setAction(PermissionAction action) {
      this.action = action;
  }

  @Override
  public String toString() {
      return scope + "\\" + principal + "\\" + action;
  }
}