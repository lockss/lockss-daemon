package org.lockss.util;

public class IdentityParseException extends Exception {
  public IdentityParseException() { super(); }
  public IdentityParseException(String msg) { super(msg); }
  public IdentityParseException(Exception e) { super(e); }
  public IdentityParseException(String msg, Exception e) { super(msg, e); }
}
