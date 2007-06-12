package org.lockss.plugin.acs;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.util.StringUtil;

/** ACS site redirects to
 * <code><i>orig-url</i>?sessid=<blah></code> when you try to fetch a PDF.
 * This removes the session id if it it is present.
 */

public class AcsUrlNormalizer implements UrlNormalizer {
//  public String QUERY_ARG = "cookieSet=1";
//  private String ONLY_QUERY = "?" + QUERY_ARG;
//  private String FIRST_QUERY = "?" + QUERY_ARG + "&";
//  private String NTH_QUERY = "&" + QUERY_ARG;
//
//  public String normalizeUrl (String url, ArchivalUnit au) {
//    if (-1 == url.indexOf(QUERY_ARG)) {
//      return url;
//    }
//    url = StringUtil.replaceFirst(url, FIRST_QUERY, "?");
//    url = StringUtil.replaceFirst(url, ONLY_QUERY, "");
//    url = StringUtil.replaceFirst(url, NTH_QUERY, "");
//    return url;
//  }

  private static final String QUERY_ARG = "?sessid=";

  public String normalizeUrl(String url, ArchivalUnit au)
      throws PluginException {
    int idx = url.indexOf(QUERY_ARG);
    return idx == -1 ? url : url.replaceFirst("sessid=[0-9]+", "sessid=");
  }

}
