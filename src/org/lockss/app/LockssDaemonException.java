package org.lockss.app;

public class LockssDaemonException extends RuntimeException {

  public LockssDaemonException() {
    super();
  }

  public LockssDaemonException(String msg) {
    super(msg);
  }
}