package io.boomerang.exceptions;

@SuppressWarnings("serial")
public class RunWorkflowException extends RuntimeException {

  public RunWorkflowException(String message) {
    super(message);
  }

  public RunWorkflowException() {
    super();
  }

}
