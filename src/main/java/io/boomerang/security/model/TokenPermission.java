package io.boomerang.security.model;

public enum TokenPermission {

  WORKFLOW_READ(new TokenType[] {TokenType.global, TokenType.user}, TokenObject.workflow, TokenAccess.read),
  WORKFLOW_WRITE(new TokenType[] {TokenType.global}, TokenObject.workflow, TokenAccess.write),
  WORKFLOW_DELETE(new TokenType[] {TokenType.global}, TokenObject.workflow, TokenAccess.delete),
  TOKEN_READ(new TokenType[] {TokenType.global}, TokenObject.token, TokenAccess.read),
  TOKEN_WRITE(new TokenType[] {TokenType.global},TokenObject.token, TokenAccess.write),
  TOKEN_DELETE(new TokenType[] {TokenType.global},TokenObject.token, TokenAccess.delete);

  private TokenType[] types;

  private TokenObject object;
  private TokenAccess access;

  public TokenType[] types() {
    return types;
  }


  public TokenObject object() {
    return object;
  }

  public TokenAccess access() {
    return access;
  }

  TokenPermission(TokenType[] types, TokenObject object, TokenAccess access) {
    this.types = types;
    this.object = object;
    this.access = access;
  }
}


