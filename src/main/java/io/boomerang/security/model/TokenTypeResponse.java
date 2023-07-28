package io.boomerang.security.model;

public class TokenTypeResponse {

  private PermissionScope object;
  private PermissionAction access;
  private AuthType[] types;

  public PermissionAction getAccess() {
    return access;
  }
  public void setAccess(PermissionAction access) {
    this.access = access;
  }
  public AuthType[] getTypes() {
    return types;
  }
  public void setTypes(AuthType[] types) {
    this.types = types;
  }
  public PermissionScope getObject() {
    return object;
  }
  public void setObject(PermissionScope object) {
    this.object = object;
  }


}
