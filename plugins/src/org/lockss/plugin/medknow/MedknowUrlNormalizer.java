/* $Id$

Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.regex.Pattern;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.util.Logger;


/* 
 *   The same functional link can have variants based on the URL of the referring page
 *   We want to normalize these to be the same to avoid duplicate functional pages with minor URL differences.
 *   Specifically, 
 *      The citation landing page url is of the form:
 *      <base_url>/citation.asp?issn=X;year=Y;volume=Q;issue=P;spage=N;epage=M;aulast=foo
 *          but may also have one or both of the following additional arguments
 *          aid=<article_identifier>
 *          type=1
 *       depending on the url of the referring article page (type informs aspect; aid is sometimes there)
 *    In this specific case, we want to remove the type argument but we cannot always remove it because it is
 *    a necessary part of an article aspect url.
 *
 */

public class MedknowUrlNormalizer implements UrlNormalizer {
  
  private static final Logger log = Logger.getLogger(MedknowUrlNormalizer.class);

  public String normalizeUrl(String url, ArchivalUnit au)
      throws PluginException {
    
    /** "http://www.afrjpaedsurg.org/article.asp?issn=0189-6725;year=2012;
     volume=9;issue=1;spage=13;epage=16;aulast=Kothari
      ;aid=AfrJPaediatrSurg_2012_9_1_13_93295" */
    if (url.contains(";aid=")) {
      log.debug("ur contains ';aid=': " + url);
      url = url.replaceFirst(";aid=.+$", "");
    } else if (url.contains(";month=")) {
      /*
       * Issue TOC from the manifest page contain the month descriptor:
       * http://www.jpgmonline.com/showBackIssue.asp?issn=0022-3859;year=2013;volume=59;issue=1;month=January-March
       * but from the "next-TOC-prev" navigators on the article pages, they do not
       * http://www.jpgmonline.com/showbackIssue.asp?issn=0022-3859;year=2013;volume=59;issue=1
       * so normalize this unnecessary bit of info, off
       * 
       */
      url = url.replaceFirst(";month=.+$", "");
    }
    // The citation landing page may have the "type" argument depending on which 
    // article aspect generated the link - normalize to the  no-type-argument url 
    // note that the "aid" argument has already been removed so "type" will be the final argument
    if (url.contains("/citation.asp")) {
      if (log.isDebug3()) {
        // just for debugging do unnecessary additional check
        if (url.contains(";type=")) {
          log.debug3("must remove the ';type' argumen from citation landing page url");
        }
      }
      url = url.replaceFirst(";type=.+$", "");
    }
    log.debug3("normalized url: " + url);
    return(url);

  }

}
