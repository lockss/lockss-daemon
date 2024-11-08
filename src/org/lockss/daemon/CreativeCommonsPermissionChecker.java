/*

Copyright (c) 2000-2024, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.daemon;

import java.io.*;
import java.util.regex.*;

import org.lockss.config.*;
import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.state.*;
import org.lockss.extractor.*;

/**
 * This Permission checker recognizes all versions of the newer form of a
 * Creative Commons license, which consists of an A or LINK tag containing
 * the URL of a valid CC license page, and the REL attribute with the value
 * LICENSE.
 */
public class CreativeCommonsPermissionChecker extends BasePermissionChecker {

  private static Logger log =
    Logger.getLogger(CreativeCommonsPermissionChecker.class);

  /**
   * @deprecated Since 1.78.
   */
  @Deprecated
  public static final String PREFIX =
    Configuration.PREFIX + "creativeCommonsPermission.";

  /**
   * <p>
   * Accepts 1.0 Generic, 2.0 Generic, 2.1, 2.5 Generic, 3.0 Unported, 4.0
   * International, CC0 1.0, CERTIFICATION 1.0, and PDM 1.0, regardless of
   * country and language codes, with HTTP or HTTPS, with or without WWW, with
   * the addition of inverted "by-nc-nd" and "nc-nd" for 1.0 Generic.
   * </p>
   * 
   * @since 1.78
   * @see https://creativecommons.org/licenses/list.en
   * @see https://creativecommons.org/publicdomain/list.en
   */
  public static final String LICENSE_PATTERN_STRING =
    "^https?://(?:www\\.)?creativecommons\\.org/"
    + "(?:"
            + "licenses/"
            + "(?:"
                    // - 1.0 Generic (https://creativecommons.org/licenses/list.en#generic-10), but:
                    //     - by-nc-nd, nc-nd don't explicitly exist but they would be the modern spelling of by-nd-nc, nd-nc
                    + "(?:by|by-nc|by-nc-sa|by-nd|by-nd-nc|by-sa|nc|nc-sa|nc-sampling(?:\\+|%2B)|nd|nd-nc|sa|sampling|sampling(?:\\+|%2B)|by-nc-nd|nc-nd)/1\\.0"
            + "|"
                    // - 2.0 Generic (https://creativecommons.org/licenses/list.en#generic-20), but:
                    //     - nc, nc-sa, nd, nd-nc, sa only in Japan (https://creativecommons.org/licenses/list.en#japan-20)
                    //     - nc-nd doesn't explicitly exist but it would be the modern spelling of nd-nc
                    + "(?:by|by-nc|by-nc-nd|by-nc-sa|by-nd|by-sa|devnations|nc|nc-sa|nd|nd-nc|sa|nc-nd)/2\\.0"
            + "|"
                    // - 2.1 (https://creativecommons.org/licenses/list.en#licenses-21), but only in:
                    //     - Australia (https://creativecommons.org/licenses/list.en#australia-21),
                    //     - Canada (https://creativecommons.org/licenses/list.en#canada-21),
                    //     - Japan (https://creativecommons.org/licenses/list.en#japan-21)
                    //     - Spain (https://creativecommons.org/licenses/list.en#spain-21)
                    // - 2.5 Generic (https://creativecommons.org/licenses/list.en#generic-25)
                    // - 3.0 Unported (https://creativecommons.org/licenses/list.en#unported-30)
                    // - 4.0 International (https://creativecommons.org/licenses/list.en#international-40)
                    + "(?:by|by-nc|by-nc-nd|by-nc-sa|by-nd|by-sa)/(?:2\\.1|2\\.5|3\\.0|4\\.0)"
            + ")"
    + "|"
            // - CC0 1.0 (https://creativecommons.org/publicdomain/list.en#publicdomain-cc0-10)
            // - CERTIFICATION 1.0 (https://creativecommons.org/publicdomain/list.en#publicdomain-certification-10-us)
            // - PDM 1.0 (https://creativecommons.org/publicdomain/list.en#publicdomain-pdm-10)
            + "publicdomain/(?:zero|certification|mark)/1\\.0"
    + ")"
    + "(?:$|/)";
  
  /**
   * List of Creative Commons license types that are accepted (deprecated since
   * 1.78)
   * 
   * @deprecated Since 1.78.
   */
  @Deprecated
  public static final String PARAM_VALID_LICENSE_TYPES =
    PREFIX + "validLicenseTypes";

  /**
   * List of Creative Commons license versions that are accepted (deprecated
   * since 1.78 
   * 
   * @deprecated Since 1.78.
   */
  @Deprecated
  public static final String PARAM_VALID_LICENSE_VERSIONS =
    PREFIX + "validLicenseVersions";

  protected static Pattern licensePat = Pattern.compile(LICENSE_PATTERN_STRING, Pattern.CASE_INSENSITIVE);
  
  /** Called by org.lockss.config.MiscConfig
   */
  public static void setConfig(Configuration config,
                               Configuration oldConfig,
                               Configuration.Differences diffs) {
    if (diffs.contains(PREFIX)) {
      if (diffs.contains(PARAM_VALID_LICENSE_TYPES) || diffs.contains(PARAM_VALID_LICENSE_VERSIONS)) {
        log.warning(String.format("The configuration parameters %s and %s are deprecated since LOCKSS 1.78",
            PARAM_VALID_LICENSE_TYPES, PARAM_VALID_LICENSE_VERSIONS));
      }
    }
  }

  protected boolean foundCcLicense = false;

  public boolean checkPermission(Crawler.CrawlerFacade crawlFacade,
                                 Reader inputReader, String permissionUrl) {
    foundCcLicense = false;
    log.debug3("Checking permission on " + permissionUrl);
    if (permissionUrl == null) {
      return false;
    }
    if (licensePat == null) {
      log.error("Invalid pattern");
      return false;
    }
    
    ArchivalUnit au = null;
    if (crawlFacade != null) {
      au = crawlFacade.getAu();
      if (log.isDebug3()) {
        log.debug3("crawlFacade: " + crawlFacade);
        log.debug3("AU: " + au);
      }
    }
    CustomHtmlLinkExtractor extractor = new CustomHtmlLinkExtractor();
    try {
      // XXX ReaderInputStream needed until PermissionChecker changed to
      // take InputStream instead of Reader
      extractor.extractUrls(au, new ReaderInputStream(inputReader), null,
                            permissionUrl, new MyLinkExtractorCallback());
    } catch (IOException ex) {
      log.error("Exception trying to parse permission URL " + permissionUrl,
                ex);
      return false;
    }
    if (foundCcLicense) {
      log.debug3("Found CC license on " + permissionUrl);
      setAuAccessType(crawlFacade, AuState.AccessType.OpenAccess);
      return true;
    }
    return false;
  }

  private static final String REL = "rel";
  private static final String LICENSE = "license";

  private class CustomHtmlLinkExtractor
    extends GoslingHtmlLinkExtractor {

    protected String extractLinkFromTag(StringBuffer link, ArchivalUnit au,
                                        LinkExtractor.Callback cb) {
      switch (link.charAt(0)) {
        case 'l': //<link href="blah" rel="license">
        case 'L':
        case 'a': //<a href="blah" rel="license">
        case 'A':
          if (log.isDebug3()) {
            log.debug3("Looking for license in " + link);
          }
          if (beginsWithTag(link, LINKTAG) || beginsWithTag(link, ATAG)) {
            String relStr = getAttributeValue(REL, link);
            if (LICENSE.equalsIgnoreCase(relStr)) {
              // This tag has the rel="license" attribute
              String candidateUrl = getAttributeValue(HREF, link);
              if (candidateUrl == null) {
                break;
              }
              candidateUrl = candidateUrl.trim();
              log.debug2("Candidate license URL: " + candidateUrl);
              Matcher mat = licensePat.matcher(candidateUrl);
              if (mat.find()) {
                log.debug2("CC license found: " + candidateUrl);
                foundCcLicense = true;
              }
              else {
                log.debug2("CC license not found: " + candidateUrl);
              }
            }
          }
          break;
        default:
          return null;
      }
      return null;
    }
  }

  /** This may be called with URLs extracted by subsidiary extractors for
   * other MIME types embedded in permission page (e.g., CSS), not just
   * with the URL selected by the special purpose extractLinkFromTag()
   * method above.  So no decisions can be made here. */
  private class MyLinkExtractorCallback implements LinkExtractor.Callback {
    public MyLinkExtractorCallback() {
    }

    public void foundLink(String url) {
    }
  }
}
