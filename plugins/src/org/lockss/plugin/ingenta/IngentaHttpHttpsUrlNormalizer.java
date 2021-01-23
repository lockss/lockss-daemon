package org.lockss.plugin.ingenta;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.BaseUrlHttpHttpsUrlNormalizer;
import org.lockss.plugin.UrlNormalizer;
import org.lockss.plugin.ingenta.IngentaUrlNormalizer;
import org.lockss.util.Logger;

/*
 * Adds in the http-to-https conversion then calls the standard Ingenta normalization
 */
public class IngentaHttpHttpsUrlNormalizer extends BaseUrlHttpHttpsUrlNormalizer {
  protected static Logger log =
      Logger.getLogger(org.lockss.plugin.ingenta.IngentaHttpHttpsUrlNormalizer.class);
  protected static UrlNormalizer baseNorm = new IngentaUrlNormalizer();

  @Override
  public String additionalNormalization(String url, ArchivalUnit au)
      throws PluginException {
    return baseNorm.normalizeUrl(url, au);
  }
}

