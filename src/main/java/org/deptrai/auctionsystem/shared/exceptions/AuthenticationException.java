package org.deptrai.auctionsystem.shared.exceptions;

public class AuthenticationException extends RuntimeException {
  public AuthenticationException(String msg) {
    super(msg);
  }

  @Override
  public String toString() {
    return getMessage();
  }
}
