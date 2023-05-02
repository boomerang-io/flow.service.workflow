package io.boomerang.security;

public class AuthorizationException extends RuntimeException {

  /**
   *
   */
  private static final long serialVersionUID = 1L;
  private final SecurityErrorResponse error; // NOSONARR

  public AuthorizationException(SecurityErrorResponse errorResponse) {
    super();
    this.error = errorResponse;
  }

  public SecurityErrorResponse getError() {
    return error;
  }
}