package io.boomerang.security.model;

public class TokenAccessScope {
  public TokenObject getObject() {
    return object;
  }
  public void setObject(TokenObject object) {
    this.object = object;
  }
  public TokenAccess getAccess() {
    return access;
  }
  public void setAccess(TokenAccess access) {
    this.access = access;
  }
  private TokenObject object;
  private TokenAccess access;

}
