/*
 * $Id:$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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
