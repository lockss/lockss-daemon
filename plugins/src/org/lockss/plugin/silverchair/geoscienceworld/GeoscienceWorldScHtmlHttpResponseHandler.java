package org.lockss.plugin.silverchair.geoscienceworld;

import org.lockss.plugin.silverchair.BaseScHtmlHttpResponseHandler;
import org.lockss.util.Logger;

import java.util.regex.Pattern;

public class GeoscienceWorldScHtmlHttpResponseHandler extends BaseScHtmlHttpResponseHandler {

  private static final Logger log = Logger.getLogger(GeoscienceWorldScHtmlHttpResponseHandler.class);

  // ignore errors on static files
  protected static final Pattern NON_FATAL_PAT =
      Pattern.compile("\\.(bmp|css|eot|gif|ico|jpe?g|js|otf|png|svg|tif?f|ttf|woff)($|\\?)");

  @Override
  protected Pattern getNonFatalPattern() {
    return NON_FATAL_PAT;
  }

}
