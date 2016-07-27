/**
 * $Id$
 */
/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

 */

package org.lockss.plugin.highwire;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.PluginException.LinkageError;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.SubstancePredicate;
import org.lockss.plugin.SubstancePredicateFactory;
import org.lockss.state.SubstanceChecker;
import org.lockss.state.SubstanceChecker.UrlPredicate;
import org.lockss.util.Constants;
import org.lockss.util.HeaderUtil;
import org.lockss.util.Logger;


public class HighWireDrupalSubstancePredicate {
  
  public static class Factory implements SubstancePredicateFactory {
    
    /* (non-Javadoc)
     * @see org.lockss.plugin.SubstancePredicateFactory#makeSubstancePredicate(org.lockss.plugin.ArchivalUnit)
     */
    @Override
    public DrupalSubstancePredicate makeSubstancePredicate(ArchivalUnit au)
        throws LinkageError {
      return new DrupalSubstancePredicate(au);
    }
    
  }
  
  public static class DrupalSubstancePredicate implements SubstancePredicate {
    
    static Logger log = Logger.getLogger(DrupalSubstancePredicate.class);
    private ArchivalUnit au;
    private UrlPredicate up = null;
    
    final static Pattern PDF_PATTERN = Pattern.compile("[.]full[.]pdf$", Pattern.CASE_INSENSITIVE);
    final static Pattern FULL_PATTERN = Pattern.compile(".full$", Pattern.CASE_INSENSITIVE);
    
    
    public DrupalSubstancePredicate (ArchivalUnit au) {
      this.au = au;
      try {
        up = new SubstanceChecker.UrlPredicate(au, 
            au.makeSubstanceUrlPatterns(), au.makeNonSubstanceUrlPatterns());
      } catch (ArchivalUnit.ConfigurationException e) {
        log.error("Error in substance or non-substance pattern for HighWire Drupal Plugin", e);
      }
    }
    
    /* (non-Javadoc)
     * isSubstanceUrl(String url)
     * checking that the "substantial" url matches its mime-type
     * This will serve two purposes - 
     *   do not count as substance a url that doesn't match its expected mime-type
     *   log a site warning when the url pattern and mime type don't match (redirection)
     * @see org.lockss.plugin.SubstancePredicate#isSubstanceUrl(java.lang.String)
     * @Return false if the url does not match the substance pattern, or the mime-type does not match 
     * the content type
     * @Return true when url matches the pattern and the mime-type matches and has content
     */
    @Override
    public boolean isSubstanceUrl(String url) {
      // check url against substance rules for publisher
      if (log.isDebug3()) log.debug3("isSubstanceURL("+url+")");
      //Check #1: Does the URL match substance pattern
      if ((up == null) || !(up.isMatchSubstancePat(url))) {
        return false;
      }
      //Check #2: Do we have content at this URL
      CachedUrl cu = au.makeCachedUrl(url);
      if ( (cu == null) || !(cu.hasContent()) ){
        return false;
      }
      //Check #3: Does the mime-type match the URL pattern
      try {
        String mime = HeaderUtil.getMimeTypeFromContentType(cu.getContentType());
        if (isPdfUrl(url) && !Constants.MIME_TYPE_PDF.equals(mime)) {
          log.siteWarning("the URL " + url + "is of the mime type " + mime);
          return false;
        } else if (isHtmlUrl(url) && !Constants.MIME_TYPE_HTML.equals(mime)) {
          log.siteWarning("the URL " + url + "is of the mime type " + mime);
          return false;
        }
        return true;
      } finally {
        AuUtil.safeRelease(cu);
      }
    }
    
    private static boolean isPdfUrl(String url) {
      Matcher mat = PDF_PATTERN.matcher(url);
      return (mat.find());
    }
    private static boolean isHtmlUrl(String url) {
      Matcher mat = FULL_PATTERN.matcher(url);
      return (mat.find());
    }
    
  }
  
}

