package io.boomerang.security.model;

public enum TokenPermission {

  WORKFLOW_READ(TokenObject.workflow, TokenAccess.read),
  WORKFLOW_WRITE(TokenObject.workflow, TokenAccess.write),
  WORKFLOW_DELETE(TokenObject.workflow, TokenAccess.delete),
  TOKEN_READ(TokenObject.token, TokenAccess.read),
  TOKEN_WRITE(TokenObject.token, TokenAccess.write),
  TOKEN_DELETE(TokenObject.token, TokenAccess.delete);

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


