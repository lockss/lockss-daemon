/**
 * $Id$
 */
/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.atypon;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;

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
import org.lockss.util.ListUtil;
import org.lockss.util.Logger;
import org.lockss.util.RegexpUtil;


public class BaseAtyponSubstancePredicateFactory implements
SubstancePredicateFactory {

  /* (non-Javadoc)
   * @see org.lockss.plugin.SubstancePredicateFactory#makeSubstancePredicate(org.lockss.plugin.ArchivalUnit)
   */
  @Override
  public BaseAtyponSubstancePredicate makeSubstancePredicate(ArchivalUnit au)   
      throws LinkageError {
    return new BaseAtyponSubstancePredicate(au);
  }

  protected static class BaseAtyponSubstancePredicate implements SubstancePredicate {
    static Logger log = Logger.getLogger(BaseAtyponSubstancePredicate.class);; 
    private ArchivalUnit au;
    private UrlPredicate up = null;
    final static Pattern PDF_OR_PDFPLUS_PATTERN = Pattern.compile("/doi/(pdf|pdfplus)/[.0-9]+/[^?&]+$", Pattern.CASE_INSENSITIVE);
    final static Pattern ABSTRACT_OR_FULL_PATTERN = Pattern.compile("/doi/(abs|full)/[.0-9]+/[^?&]+$", Pattern.CASE_INSENSITIVE);
    final static Pattern ABSTRACT_ONLY_PATTERN = Pattern.compile("/doi/abs/[.0-9]+/[^?&]+$", Pattern.CASE_INSENSITIVE);

    // Abstract will never be substance, but we want to check for redirection
    // use different implementation of PATTERN to meet needs of UrlPredicate
    final static String ABSTRACT_STRING = "/doi/abs/[.0-9]+/[^?&]+$";
    final static org.apache.oro.text.regex.Pattern ABSTRACT_PATTERN = 
        RegexpUtil.uncheckedCompile(ABSTRACT_STRING);


    public BaseAtyponSubstancePredicate (ArchivalUnit au) {
      this.au = au;
      try {
      List<org.apache.oro.text.regex.Pattern> yesPatterns = au.makeSubstanceUrlPatterns();
      // allow abstract in to the "yes" patterns so we can look for redirection.
      // We will disallow it as substance after the check.
      yesPatterns.add(ABSTRACT_PATTERN);
      up = new SubstanceChecker.UrlPredicate(au, yesPatterns, au.makeNonSubstanceUrlPatterns());
      } catch (ArchivalUnit.ConfigurationException e) {
        log.error("Error in substance or non-substance pattern for Atypon Plugin", e);
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
      //Check #4 - abstract is never substance
        return (!isAbstractUrl(url));
      } finally {
        AuUtil.safeRelease(cu);
      }
    }
    
    private static boolean isPdfUrl(String url) {
      Matcher mat = PDF_OR_PDFPLUS_PATTERN.matcher(url);
      return (mat.find());
    }
    private static boolean isHtmlUrl(String url) {
      Matcher mat = ABSTRACT_OR_FULL_PATTERN.matcher(url);
      return (mat.find());
    }
    private static boolean isAbstractUrl(String url) {
      Matcher mat = ABSTRACT_ONLY_PATTERN.matcher(url);
      return (mat.find());
    }    


  }

}


