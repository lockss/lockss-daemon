/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

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
   * These HTML docs are poorly formatted, so searching for the <title>Access Denied</title> as a regex incorporating
   * much white space is probably the safest.
   *
   * Releavnt portion of roughly what is in the Page Source:
   * <header>[HEADER STUFF]<title>
   *    Access Denied
   *   </title>
   * </header>
   * <p>
   *  <body>
   *    <div>
   *      Access Denied
   *    </div>
   *  </body>
   * </p>
   */

  protected static final String ACCESS_DENIED_SNIPPET = "<title>\\s*Access Denied\\s*</title>";
  protected static final Pattern ACCESS_DENIED_PATTERN = Pattern.compile(ACCESS_DENIED_SNIPPET, Pattern.CASE_INSENSITIVE);

  public boolean isLoginPage(Properties props,
                             Reader reader)
      throws IOException,
      PluginException {

    boolean found = false;
    if ("text/html".equalsIgnoreCase(HeaderUtil.getMimeTypeFromContentType(props.getProperty("Content-Type")))) {
      String theContents = StringUtil.fromReader(reader);  // This returns an empty string most of the time.
      Matcher matcher = ACCESS_DENIED_PATTERN.matcher(theContents);
      //log.debug3( theContents.length() + "<<<" + theContents + ">>>" );
      found = matcher.find();
      if (found) {
        log.debug3("found a match with: '" + ACCESS_DENIED_SNIPPET + "'");
        throw new CacheException.UnexpectedNoRetryFailException("Found an Access Denied page.");
      }
    }
    return found;
  }

}