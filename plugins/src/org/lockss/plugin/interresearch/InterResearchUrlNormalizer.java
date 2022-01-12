package org.lockss.plugin.interresearch;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.UrlNormalizer;
import org.lockss.util.Logger;

import java.util.regex.Pattern;

public class InterResearchUrlNormalizer implements UrlNormalizer {

  protected static Logger log =
      Logger.getLogger(InterResearchUrlNormalizer.class);

  protected static final Pattern HASH_ARG_PATTERN = Pattern.compile("(\\.(css|js))\\?\\d+$");

  public String normalizeUrl(String url, ArchivalUnit au)
      throws PluginException {
    // some CSS/JS files have a hash argument that isn't needed
    String returnString = HASH_ARG_PATTERN.matcher(url).replaceFirst("$1");
    if (!returnString.equals(url)) {
      // if we were a normalized css/js, then we're done - return
      log.debug3("normalized css or js url: " + returnString);
      return returnString;
    }
    return url;
  }

}
