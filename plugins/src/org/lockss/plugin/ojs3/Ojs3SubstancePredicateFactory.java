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

package org.lockss.plugin.ojs3;

import org.lockss.daemon.PluginException.LinkageError;
import org.lockss.plugin.*;
import org.lockss.state.SubstanceChecker;
import org.lockss.state.SubstanceChecker.UrlPredicate;
import org.lockss.util.Constants;
import org.lockss.util.HeaderUtil;
import org.lockss.util.Logger;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.util.RegexpUtil;


public class Ojs3SubstancePredicateFactory implements
SubstancePredicateFactory {
  
  @Override
  public Ojs3SubstancePredicate makeSubstancePredicate(ArchivalUnit au)
      throws LinkageError {
    return new Ojs3SubstancePredicate(au);
  }

  protected static class Ojs3SubstancePredicate implements SubstancePredicate {
    static Logger log = Logger.getLogger(Ojs3SubstancePredicate.class);;
    private ArchivalUnit au;
    private UrlPredicate up = null;
    private boolean hasArticleDownloadablePDFLink = false;

    // This journal only have issue level PDF, no article level content
    // https://scholarworks.iu.edu/journals/index.php/psource/issue/view/1257/14 -- this is a PDF

    // This is a PDF file
    // https://journals.library.ualberta.ca/jpps/index.php/JPPS/article/view/30446/21549

    // For this journal, both of the following links are PDF
    // Journal of Pharmacy and Pharmaceutical Science Volume 23
    // https://journals.library.ualberta.ca/jpps/index.php/JPPS/article/download/30446/21549/82146
    // https://journals.library.ualberta.ca/jpps/index.php/JPPS/article/view/30446/21549
    final static Pattern PDF_PATTERN = Pattern.compile(".*/(issue|article)/view/.*", Pattern.CASE_INSENSITIVE);

    final static String URL_STRING = ".*/(issue|article)/view/.*";
    final static org.apache.oro.text.regex.Pattern URL_PATTERN =
            RegexpUtil.uncheckedCompile(URL_STRING);


    public Ojs3SubstancePredicate (ArchivalUnit au) {
      this.au = au;

      for (CachedUrl cu : AuUtil.getCuIterable(au)) {
        String url = cu.getUrl();
        log.debug3("au cached url: " + url);
        if (url.contains("/article/download/") || url.contains("/article/view/")) {
          hasArticleDownloadablePDFLink = true;
          log.debug3("this has downloadable url, url = " + url);
          break;
        }
      }

      try {
        List<org.apache.oro.text.regex.Pattern> yesPatterns = au.makeSubstanceUrlPatterns();
        up = new SubstanceChecker.UrlPredicate(au, yesPatterns, au.makeNonSubstanceUrlPatterns());
      } catch (ArchivalUnit.ConfigurationException e) {
        log.error("Error in substance or non-substance pattern for OJS3 Plugin", e);
      }
    }

    @Override
    public boolean isSubstanceUrl(String url) {
      // check url against substance rules for publisher
      if (log.isDebug3()) log.debug3("isSubstanceURL("+url+")");

      //Check #2: Do we have content at this URL
      CachedUrl cu = au.makeCachedUrl(url);
      if ( (cu == null) || !(cu.hasContent()) || (up == null) ){
        return false;
      }

      // We have a special case for"scholarworks.iu.edu/", they have no PDF files
      // https://scholarworks.iu.edu/journals/index.php/pders/article/view/28644
      // https://scholarworks.iu.edu/journals/index.php/pders/article/view/28644
      // https://scholarworks.iu.edu/journals/index.php/pders/issue/view/2071

      if (url.contains("scholarworks.iu.edu") && PDF_PATTERN.matcher(url).matches()) {
        log.debug3("Special case for scholarworks.iu.edu the url is considered as PDF: " + url);
        return true;
      }

      // If it has no downloadable link, only check PDF pattern
      if (isPdfUrl(url) && !hasArticleDownloadablePDFLink) {
        // Some journal return "html/text" as their PDF file
        log.siteWarning("PDF file sent as wrong mime type, accepted special behavior " + url );
        return true;
      }

      // If URL match Substance pattern, return true
      if (up.isMatchSubstancePat(url)) {
        log.debug3("matched cases ="  + url);
        return true;
      } 
      return false;
    }
    
    private static boolean isPdfUrl(String url) {

      log.siteWarning("isPdfUrl " + url);

      Matcher mat = PDF_PATTERN.matcher(url);

      if (mat.matches()) {
        log.siteWarning("isPdfUrl, found PDF " + url);
      } else {
        log.siteWarning("isPdfUrl, NOT found");
      }

      return (mat.matches());
    }
  }

}


