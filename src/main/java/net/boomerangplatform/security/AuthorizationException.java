package net.boomerangplatform.security;

import net.boomerangplatform.security.model.ErrorResponse;

public class AuthorizationException extends RuntimeException {

  /**
   *
   */
  private static final long serialVersionUID = 1L;
  private final ErrorResponse error; // NOSONARR

  public AuthorizationException(ErrorResponse errorResponse) {
    super();
    this.error = errorResponse;
  }

  public ErrorResponse getError() {
    return error;
  }
}
