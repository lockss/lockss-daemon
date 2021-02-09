package org.lockss.plugin.royalsocietyofchemistry;

import org.lockss.daemon.LoginPageChecker;
import org.lockss.daemon.PluginException;
import org.lockss.util.HeaderUtil;
import org.lockss.util.StringUtil;
import org.lockss.util.Logger;
import org.lockss.util.urlconn.CacheException;

import java.io.IOException;
import java.io.Reader;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>Detects Access Denied pages in RSC Books pages.
 *   These pages are html pages that say simply 'Access Denied'
 *   The HTTP Response is 200
 * </p>
 * @author Mark Mcadam
 */

public class RSCLoginPageChecker implements LoginPageChecker {

  private static final Logger log = Logger.getLogger(RSCLoginPageChecker.class);

  /**
   * <p>
   *  <body>
   *    <div>
   *      Access Denied
   *    </div>
   *  </body>
   * </p>
   */

  ///protected static final String ACCESS_DENIED_SNIPPET = "<title>\\s*Access Denied\\s*</title>";
  protected static final String ACCESS_DENIED_SNIPPET = "Access Denied";
  protected static final Pattern ACCESS_DENIED_PATTERN = Pattern.compile(ACCESS_DENIED_SNIPPET, Pattern.CASE_INSENSITIVE);

  public boolean isLoginPage(Properties props,
                             Reader reader)
      throws IOException,
      PluginException {
    //log.debug2(props.toString());
    boolean found = false;
    if ("text/html".equalsIgnoreCase(HeaderUtil.getMimeTypeFromContentType(props.getProperty("Content-Type")))) {
      //String theContents = StringUtil.fromReader(reader);  // This returns an empty string most of the time.
      //Matcher matcher = ACCESS_DENIED_PATTERN.matcher(theContents);
      //log.debug(theContents);
      //found = matcher.find();
      found = StringUtil.containsString(reader, ACCESS_DENIED_SNIPPET, true);
      if (found) {
        log.debug("found a match with: '" + ACCESS_DENIED_SNIPPET + "'");
        throw new CacheException.UnexpectedNoRetryFailException("Found an Access Denied page.");
      }
    }
    return found;
  }

}