package org.lockss.plugin;

import java.net.HttpURLConnection;

public interface CacheExceptionHandler {
  public CacheException handleException(int code, HttpURLConnection connection);
}
