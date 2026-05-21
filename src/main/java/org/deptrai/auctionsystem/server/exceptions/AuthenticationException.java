package org.deptrai.auctionsystem.server.exceptions;

public class AuthenticationException extends RuntimeException {
  public AuthenticationException(String msg) {
    super(msg);
  }

  @Override
  public String toString() {
    return getMessage();
  }
}
