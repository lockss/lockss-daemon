/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.plugin.highwire;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import org.lockss.util.UrlUtil;

public class HighWireDrupalUrlNormalizer extends BaseUrlHttpHttpsUrlNormalizer {
  
  private static final Logger log = Logger.getLogger(HighWireDrupalUrlNormalizer.class);
  
  protected static final String WEB_VIEWER =
      "/sites/all/libraries/pdfjs/web/viewer.html?file=/";
  protected static final Pattern URL_PAT = Pattern.compile("/content/[^/0-9]+/");
  
  protected static final String CITATION = "/highwire/citation/";
  protected static final Pattern RIS_PAT = Pattern.compile(
      "/(bookends|easybib|mendeley|papers|reference-manager|refworks|ris|zotero)$");
  
  protected static final String MEDIUM_GIF = ".medium.gif?";
  protected static final String LARGE_JPG = ".large.jpg?";
  protected static final String JS_SUFFIX = ".js?";
  protected static final String CSS_SUFFIX = ".css?";
  protected static final String EOT_SUFFIX = ".eot?";
  protected static final String SVG_SUFFIX = ".svg?";
  protected static final String TTF_SUFFIX = ".ttf?";
  protected static final String WOFF_SUFFIX = ".woff?";
  
  protected static final String PDF_HTML_VARIANT_SUFFIX = ".pdf%2Bhtml";
  protected static final String PDF_HTML_SUFFIX = ".pdf+html";
  protected static final String PDF = ".pdf";
  protected static final String FT_PDF = ".full-text.pdf";
  protected static final String FULL_PDF_SUFFIX = ".full.pdf";
  
  protected static final String RSS_PARAM = "?rss=";
  protected static final String IJKEY_PARAM = "?ijkey=";
  protected static final String ELTR_PARAM = ".e-letters?";
  protected static final String EXPAND_PARAM = "/expansion?";
  protected static final String ITOK_PARAM = "?itok=";
  protected static final String DOWNLOAD_PARAM = "?download=";
  
  protected static final String TOC_SEC_ID_PARAM = "facet[toc-section-id]";
  
  
  @Override
  public String additionalNormalization(String url, ArchivalUnit au)
      throws PluginException {
    // map 
    // http://ajpcell.physiology.org/content/ajpcell/303/1/C1/F1.large.jpg?width=800&height=600
    // & http://ajpcell.physiology.org/content/ajpcell/303/1/C1/F1.large.jpg?download=true
    // to http://ajpcell.physiology.org/content/ajpcell/303/1/C1/F1.large.jpg
    // 
    // http://ajplung.physiology.org/sites/default/files/color/jcore_1-15d49f53/colors.css?n3sdk7
    // & http://ajplung.physiology.org/sites/default/files/color/jcore_1-15d49f53/colors.css?n3u6ya
    // to http://ajplung.physiology.org/sites/default/files/color/jcore_1-15d49f53/colors.css
    // 
    // http://ajpheart.physiology.org/content/ajpheart/304/2/H253.full-text.pdf
    // to http://ajpheart.physiology.org/content/ajpheart/304/2/H253.full.pdf
    // 
    // http://ajpheart.physiology.org/content/304/2/H253.full-text.pdf
    // to http://ajpheart.physiology.org/content/304/2/H253.full.pdf
    // 
    // http://ajpheart.physiology.org/content/304/2/H253.full-text.pdf+html
    // & http://ajpheart.physiology.org/content/304/2/H253.full.pdf%2Bhtml
    // to http://ajpheart.physiology.org/content/304/2/H253.full.pdf+html
    // 
    // http://ajprenal.physiology.org/sites/all/libraries/pdfjs/web/viewer.html
    //  ?file=/content/ajprenal/304/1/F33.full.pdf
    // to http://ajprenal.physiology.org/content/304/1/F33.full.pdf
    // 
    // while PDFs with download param are not collected, the links should work in audit proxy
    // http://ajpheart.physiology.org/content/304/2/H253.full.pdf?download=true
    // to http://ajpheart.physiology.org/content/304/2/H253.full.pdf
    //
    // map
    // http://ajpcell.physiology.org/highwire/citation/1814/bookends
    // http://ajpcell.physiology.org/highwire/citation/1814/easybib
    // http://ajpcell.physiology.org/highwire/citation/1814/mendeley
    // http://ajpcell.physiology.org/highwire/citation/1814/papers
    // http://ajpcell.physiology.org/highwire/citation/1814/reference-manager
    // http://ajpcell.physiology.org/highwire/citation/1814/zotero
    // to
    // http://ajpcell.physiology.org/highwire/citation/1814/ris
    
    if (url.contains(TOC_SEC_ID_PARAM)) {
      log.debug3(url);
    }
    if (url.contains(UrlUtil.minimallyEncodeUrl(TOC_SEC_ID_PARAM))) {
      log.debug3(url);
    }
    
    if (url.contains(WEB_VIEWER)) { 
      url = url.replace(WEB_VIEWER, "/");
      Matcher  mat = URL_PAT.matcher(url);
      url = mat.replaceFirst("/content/");
    }
    
    if (url.contains(CITATION)) { 
      Matcher  mat = RIS_PAT.matcher(url);
      url = mat.replaceFirst("/ris");
    }
    
    if (url.contains(MEDIUM_GIF) ||
        url.contains(LARGE_JPG) ||
        url.contains(JS_SUFFIX) ||
        url.contains(CSS_SUFFIX) ||
        url.contains(EOT_SUFFIX) ||
        url.contains(SVG_SUFFIX) ||
        url.contains(TTF_SUFFIX) ||
        url.contains(WOFF_SUFFIX)) {
      url = url.replaceFirst("[?].+$", "");
    } else if (url.contains(PDF)) {
      if (url.endsWith(PDF_HTML_VARIANT_SUFFIX)) {
        url = StringUtil.replaceLast(url, PDF_HTML_VARIANT_SUFFIX, PDF_HTML_SUFFIX);
      }
      if (url.contains(FT_PDF)) {
        url = StringUtil.replaceLast(url, FT_PDF, FULL_PDF_SUFFIX);
      }
    }
    
    if (url.contains(RSS_PARAM) ||
        url.contains(IJKEY_PARAM) ||
        url.contains(ELTR_PARAM) || 
        url.contains(EXPAND_PARAM) ||
        url.contains(ITOK_PARAM) ||
        url.contains(DOWNLOAD_PARAM)) {
      url = url.replaceFirst("[?].+$", "");
    }
    
    return(url);
  }
}
