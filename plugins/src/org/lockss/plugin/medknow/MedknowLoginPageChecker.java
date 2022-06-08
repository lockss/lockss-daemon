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

package org.lockss.plugin.medknow;

import java.io.*;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.CacheException;

/**
 * <p>Detects login pages in Medknow journals.
 * The plugin also uses an au_redirect_to_login_url_pattern of base_url/subscriberlogin.asp
 * but some journals don't redirect, instead providing a download page using the PDF url  
 * </p>
 * @author Alexandra R Ohlson
 */
public class MedknowLoginPageChecker implements LoginPageChecker {

  /**
   * <p>When the pdf url returns a page allowing for download, the title of 
   * the document is the title of the article with ": Download PDF" at the end.
   */
  
  //Darn. Found an oddball that has Download PDFs</title>, so use regexp to identify...
  // The following page has "Download PDF", but it is not a login page
  // http://www.wjcs.us.com/downloadpdf.asp?issn=1941-8213;year=2018;volume=7;issue=1;spage=1;epage=7;aulast=Stringfield;type=2
  // http://www.wjtcm.net/article.asp?issn=2311-8571;year=2018;volume=4;issue=4;spage=135;epage=136;aulast=Li;type=2
  protected static final String DOWNLOAD_TITLE_PATTERN_STRING = ":\\s+Download PDF[^<]*</title>";
  protected static final Pattern DOWNLOAD_TITLE_PATTERN = Pattern.compile(DOWNLOAD_TITLE_PATTERN_STRING, Pattern.CASE_INSENSITIVE);
  
  public boolean isLoginPage(Properties props,
                             Reader reader)
      throws IOException,
             PluginException {
    if ("text/html".equalsIgnoreCase(HeaderUtil.getMimeTypeFromContentType(props.getProperty("Content-Type")))) {
      String theContents = StringUtil.fromReader(reader);
      Matcher downloadMat = DOWNLOAD_TITLE_PATTERN.matcher(theContents); 
      boolean found = downloadMat.find();
      if (found) {
        throw new CacheException.UnexpectedNoRetryFailException("Found a login page for pdf");
      }
    }
    return false;
  }

}
