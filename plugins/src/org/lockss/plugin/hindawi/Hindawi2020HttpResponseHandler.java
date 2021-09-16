package org.lockss.plugin.hindawi;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;
import org.lockss.util.urlconn.CacheException;
import org.lockss.util.urlconn.CacheResultHandler;
import org.lockss.util.urlconn.CacheResultMap;

public class Hindawi2020HttpResponseHandler implements CacheResultHandler {

  protected static Logger logger = Logger.getLogger(Hindawi2020HttpResponseHandler.class);

  /*
   * 500 a non-fatal, non-retryable error if the URL contains "'data:".
   * e.g. https://www.hindawi.com/journals/ppar/2013/612971/'data:image/svg+xml
   */
  // Want to match on URLs that contain
  protected static String DATA_STR = "'data:";

  public void init(CacheResultMap crmap) {
    logger.warning("Unexpected call to init()");
    throw new UnsupportedOperationException("Unexpected call to init()");
  }

  public CacheException handleResult(ArchivalUnit au,
                                     String url,
                                     int responseCode)
      throws PluginException {

    logger.debug2(url);
    switch (responseCode) {
      case 400:
        return new CacheException.NoRetryDeadLinkException("400 Bad Request (non-fatal)");

      case 403:
        return new CacheException.NoRetryDeadLinkException("403 Forbidden (non-fatal)");

      case 500:
        logger.debug2("500");
        if (url.contains(DATA_STR)) {
          return new CacheException.NoRetryDeadLinkException("500 Service Unavailable (non-fatal)");
        }
        else {
          return new CacheException.RetrySameUrlException("500 Service Unavailable");
        }

      default:
        logger.warning(String.format("Unexpected response code %d for %s", responseCode, url));
        throw new UnsupportedOperationException("Unpexpected response code: " + responseCode);
    }
  }

  public CacheException handleResult(ArchivalUnit au,
                                     String url,
                                     Exception ex)
      throws PluginException {
    logger.warning("Unexpected call to handleResult(): AU " + au.getName() + "; URL " + url, ex);
    throw new UnsupportedOperationException();
  }

}