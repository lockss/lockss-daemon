/*
 * $Id$
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

package org.lockss.plugin.highwire;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

public class HighWireDrupalUrlNormalizer implements UrlNormalizer {
  
  private static final Logger log = Logger.getLogger(HighWireDrupalUrlNormalizer.class);
  
  protected static final String WEB_VIEWER =
      "/sites/all/libraries/pdfjs/web/viewer.html?file=/";
  protected static final Pattern URL_PAT = Pattern.compile("/content/[^/0-9]+/");
  
  protected static final String CITATION = "/highwire/citation/";
  protected static final Pattern RIS_PAT = Pattern.compile(
      "/(bookends|easybib|mendeley|papers|reference-manager|refworks|ris|zotero)$");
  
  protected static final String LARGE_JPG = ".large.jpg?";
  protected static final String JS_SUFFIX = ".js?";
  protected static final String CSS_SUFFIX = ".css?";
  
  protected static final String PDF_HTML_VARIANT_SUFFIX = ".pdf%2Bhtml";
  protected static final String PDF_HTML_SUFFIX = ".pdf+html";
  protected static final String PDF = ".pdf";
  protected static final String FT_PDF = ".full-text.pdf";
  protected static final String FULL_PDF_SUFFIX = ".full.pdf";
  
  protected static final String RSS_PARAM = "?rss=";
  protected static final String IJKEY_PARAM = "?ijkey=";
  protected static final String ELTR_PARAM = ".e-letters?";
  protected static final String EXPAND_PARAM = "/expansion?";
  
  
  @Override
  public String normalizeUrl(String url, ArchivalUnit au)
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
    // map 
    // http://ajpcell.physiology.org/highwire/citation/1814/bookends
    // http://ajpcell.physiology.org/highwire/citation/1814/easybib
    // http://ajpcell.physiology.org/highwire/citation/1814/mendeley
    // http://ajpcell.physiology.org/highwire/citation/1814/papers
    // http://ajpcell.physiology.org/highwire/citation/1814/reference-manager
    // http://ajpcell.physiology.org/highwire/citation/1814/zotero
    // to
    // http://ajpcell.physiology.org/highwire/citation/1814/ris
    
    if (url.contains(WEB_VIEWER)) { 
      url = url.replace(WEB_VIEWER, "/");
      Matcher  mat = URL_PAT.matcher(url);
      url = mat.replaceFirst("/content/");
    }
    
    if (url.contains(CITATION)) { 
      Matcher  mat = RIS_PAT.matcher(url);
      url = mat.replaceFirst("/ris");
    }
    
    if (url.contains(LARGE_JPG) ||
        url.contains(JS_SUFFIX) ||
        url.contains(CSS_SUFFIX)) {
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
        url.contains(EXPAND_PARAM)) {
      url = url.replaceFirst("[?].+$", "");
    }
    
    return(url);
  }
}
