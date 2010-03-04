package org.lockss.plugin.informaworld;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;

public class InformaworldUrlNormalizer implements UrlNormalizer {

  public String normalizeUrl(String url,
                             ArchivalUnit au)
      throws PluginException {
    return url.replaceFirst("&cktry=[^&]+", "");
  }

}
