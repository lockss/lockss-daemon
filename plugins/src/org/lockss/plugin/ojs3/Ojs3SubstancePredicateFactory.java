/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.ojs3;

import org.lockss.daemon.PluginException.LinkageError;
import org.lockss.plugin.*;
import org.lockss.state.SubstanceChecker;
import org.lockss.state.SubstanceChecker.UrlPredicate;
import org.lockss.util.Logger;
import java.util.List;
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


