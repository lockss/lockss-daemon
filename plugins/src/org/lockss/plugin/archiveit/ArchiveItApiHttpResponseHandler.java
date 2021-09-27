package org.lockss.plugin.archiveit;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;
import org.lockss.util.urlconn.CacheException;
import org.lockss.util.urlconn.CacheResultHandler;
import org.lockss.util.urlconn.CacheResultMap;
import org.lockss.util.urlconn.HttpResultMap;

public class ArchiveItApiHttpResponseHandler  implements CacheResultHandler {

  private static final Logger logger = Logger.getLogger(ArchiveItApiHttpResponseHandler.class);

  @Override
  public void init(CacheResultMap crmap) {
    HttpResultMap hrmap = ((HttpResultMap) crmap);
    // replace the default CacheExceptions for 500 errors with a more forgiving ones.
    hrmap.storeResultCategoryEntries(
        HttpResultMap.HttpResultCodeCategory.RETRY_SAME_URL,
        CacheException.RetryableNetworkException_5_30S.class
    );
  }

  @Override
  public CacheException handleResult(ArchivalUnit au,
                                     String url,
                                     int responseCode) {
    logger.warning("Unexpected responseCode (" + responseCode + ") in handleResult(): AU " + au.getName() + "; URL " + url);
    throw new UnsupportedOperationException("Unexpected responseCode (" + responseCode + ")");
  }

  @Override
  public CacheException handleResult(ArchivalUnit au, String url, Exception ex) throws PluginException {
    logger.warning("Unexpected exception (" + ex + ") in handleResult(): AU " + au.getName() + "; URL " + url);
    throw new UnsupportedOperationException("Unexpected exception (" + ex + ")");
  }

}