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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.util.Constants;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;


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

public class MedknowUrlNormalizer extends BaseUrlHttpHttpsUrlNormalizer {
  
  private static final Logger log = Logger.getLogger(MedknowUrlNormalizer.class);
  private static final String authLast = ";aulast=";
  private static final String authLastPat = "aulast=[^;]*";
  private static final Pattern authPat = Pattern.compile(authLastPat);

  public String additionalNormalization(String url, ArchivalUnit au)
      throws PluginException {
    
    /** links like...
     href="article.asp?issn=0189-6725;year=2015;volume=12;issue=3;spage=200;epage=202;aulast=P<E9>rez-Egido;type=0"
     need to be encoded to 
     href="article.asp?issn=0189-6725;year=2015;volume=12;issue=3;spage=200;epage=202;aulast=P%E9rez-Egido;type=0"
     for the links to work correctly
     */
    if (url.contains(authLast)) {
      Matcher authMatch = authPat.matcher(url);
      if (authMatch.find()) {
        String u_str = authMatch.group();
        if(log.isDebug2()) log.debug2("in:" + u_str);
        u_str = UrlUtil.encodeUri(u_str, Constants.ENCODING_ISO_8859_1).replace("%25", "%");
        if(log.isDebug2()) log.debug2("out:" + u_str);
        url = url.replaceFirst(authLastPat, u_str);
      }
    }
    /** "http://www.afrjpaedsurg.org/article.asp?issn=0189-6725;year=2012;
     volume=9;issue=1;spage=13;epage=16;aulast=Kothari
      ;aid=AfrJPaediatrSurg_2012_9_1_13_93295" */
    if (url.contains(";aid=")) {
      if(log.isDebug2()) log.debug2("ur contains ';aid=': " + url);
      url = url.replaceFirst(";aid=.+$", "");
    } else if (url.contains(";month=")) {
      /*
       * Issue TOC from the manifest page contain the month descriptor:
       * http://www.jpgmonline.com/showBackIssue.asp?issn=0022-3859;year=2013;volume=59;issue=1;month=January-March
       * but from the "next-TOC-prev" navigators on the article pages, they do not
       * http://www.jpgmonline.com/showbackIssue.asp?issn=0022-3859;year=2013;volume=59;issue=1
       * so normalize this unnecessary bit of info, off
       * BUT - if it's a supplement issue, it will have a ";supp=Y" which we do need
       * and which might come after the month...keep that bit
       * 
       */
      url = url.replaceFirst(";month=[^;]+", "");
    }
    // The citation landing page may have the "type" argument depending on which 
    // article aspect generated the link - normalize to the  no-type-argument url 
    // note that the "aid" argument has already been removed so "type" will be the final argument
    if (url.contains("/citation.asp")) {
      if (log.isDebug3()) {
        // just for debugging do unnecessary additional check
        if (url.contains(";type=")) {
          log.debug3("must remove the ';type' argument from citation landing page url");
        }
      }
      url = url.replaceFirst(";type=.+$", "");
    }
    log.debug3("normalized url: " + url);
    return(url);
  }

}
