package io.boomerang.security;

import io.boomerang.error.RestErrorResponse;

public class AuthorizationException extends RuntimeException {

  /**
   *
   */
  private static final long serialVersionUID = 1L;
  private final RestErrorResponse error; // NOSONARR

  public AuthorizationException(RestErrorResponse errorResponse) {
    super();
    this.error = errorResponse;
  }

  public RestErrorResponse getError() {
    return error;
  }
}