package org.lockss.plugin;

import java.io.IOException;

public class CacheException
    extends IOException {

  String message;

  public CacheException() {
    super();
  }

  public CacheException(String message) {
    super(message);
    this.message = message;
  }

  void initMessage(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }

  public static class UnknownCodeException
      extends CacheException {

    public UnknownCodeException() {
      super();
    }

    public UnknownCodeException(String message) {
      super(message);
    }
  }

  public static class RetryableException
      extends CacheException {
    public RetryableException() {
      super();
    }

    public RetryableException(String message) {
      super(message);
    }
  }

  public static class RetrySameUrlException
      extends RetryableException {
    public RetrySameUrlException() {
      super();
    }

    public RetrySameUrlException(String message) {
      super(message);
    }
  }

  public static class RetryNewUrlException
      extends RetryableException {
    public RetryNewUrlException() {
      super();
    }

    public RetryNewUrlException(String message) {
      super(message);
    }
  }

  public static class RetryPermUrlException
      extends RetryNewUrlException {
    public RetryPermUrlException() {
      super();
    }

    public RetryPermUrlException(String message) {
      super(message);
    }
  }

  public static class RetryTempUrlException
      extends RetryNewUrlException {
    public RetryTempUrlException() {
      super();
    }

    public RetryTempUrlException(String message) {
      super(message);
    }
  }

  public static class UnretryableException
      extends CacheException {
    public UnretryableException() {
      super();
    }

    public UnretryableException(String message) {
      super(message);
    }
  }

  public static class HostException
      extends UnretryableException {
    public HostException() {
      super();
    }

    public HostException(String message) {
      super(message);
    }
  }

  public static class RepositoryException
      extends UnretryableException {
    public RepositoryException() {
      super();
    }

    public RepositoryException(String message) {
      super(message);
    }
  }

  public static class ExpectedNoRetryException
      extends UnretryableException {
    public ExpectedNoRetryException() {
      super();
    }

    public ExpectedNoRetryException(String message) {
      super(message);
    }
  }

  public static class UnexpectedNoRetryException
      extends UnretryableException {
    public UnexpectedNoRetryException() {
      super();
    }

    public UnexpectedNoRetryException(String message) {
      super(message);
    }
  }

  public static class NoRetryHostException
      extends UnretryableException {
    public NoRetryHostException() {
      super();
    }

    public NoRetryHostException(String message) {
      super(message);
    }
  }

  public static class NoRetryRepositoryException
      extends UnretryableException {
    public NoRetryRepositoryException() {
      super();
    }

    public NoRetryRepositoryException(String message) {
      super(message);
    }
  }

  public static class UnimplementedCodeException
      extends ExpectedNoRetryException {
    public UnimplementedCodeException() {
      super();
    }

    public UnimplementedCodeException(String message) {
      super(message);
    }
  }
}
