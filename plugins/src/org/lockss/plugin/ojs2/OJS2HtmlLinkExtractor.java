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

package org.lockss.plugin.ojs2;

import java.io.IOException;
import java.net.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.extractor.GoslingHtmlLinkExtractor;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.*;

public class OJS2HtmlLinkExtractor extends GoslingHtmlLinkExtractor {

  protected static final Logger log = Logger.getLogger(OJS2HtmlLinkExtractor.class);
  
  protected static final String NAME = "name";
  protected static final String PDF_NAME = "citation_pdf_url";
  protected static final String FT_NAME = "citation_fulltext_html_url";
  protected static final String CONTENT = "content";

  protected static final Pattern OPEN_RT_WINDOW_PATTERN =
      Pattern.compile("javascript:openRTWindow\\('([^']*)'\\);", Pattern.CASE_INSENSITIVE);
  
  protected static final Pattern IFRAME_PDF_VIEWER_PATTERN =
      Pattern.compile("/plugins/generic/pdfJsViewer/pdf\\.js/web/viewer\\.html\\?file=([^&]+)", Pattern.CASE_INSENSITIVE);
  
  protected static final Pattern JLA_ARTICLE_PATTERN = Pattern.compile(
      "(http://www[.]logicandanalysis[.]org/index.php/jla/article/view/[\\d]+)/[\\d]+$");
  
  @Override
  protected String extractLinkFromTag(StringBuffer link,
                                      ArchivalUnit au,
                                      Callback cb)
      throws IOException {
    switch (link.charAt(0)) {
      case 'a':
      case 'A':
        if (beginsWithTag(link, ATAG)) {
          // <a href="...">
          String href = getAttributeValue(HREF, link);
          if (href != null) {
            // javascript:openRTWindow(url);
            Matcher mat = OPEN_RT_WINDOW_PATTERN.matcher(href);
            if (mat.find()) {
              if (baseUrl == null) { baseUrl = new URL(srcUrl); } // Copycat of parseLink()
              cb.foundLink(resolveUri(baseUrl, mat.group(1)));
            }
            mat = JLA_ARTICLE_PATTERN.matcher(href);
            if (mat.find()) {
              String url = mat.group(1);
              cb.foundLink(url);
            }
          }
        }
        break;
        
      case 'f':
      case 'F':
        if (beginsWithTag(link, FORMTAG)) {
          // <form action="...">
          String action = getAttributeValue("action", link);
          if (action != null) {
            // javascript:openRTWindow(url);
            Matcher mat = OPEN_RT_WINDOW_PATTERN.matcher(action);
            if (mat.find()) {
              if (baseUrl == null) { baseUrl = new URL(srcUrl); } // Copycat of parseLink()
              cb.foundLink(resolveUri(baseUrl, mat.group(1)));
            }
          }
        }
        break;

      case 'i':
      case 'I':
        if (beginsWithTag(link, IFRAMETAG)) {
          // <iframe src="...">
          String src = getAttributeValue(SRC, link);
          if (src != null) {
            // <baseurl?>/plugins/generic/pdfJsViewer/pdf.js/web/viewer.html?file=<encodedurl>
            Matcher mat = IFRAME_PDF_VIEWER_PATTERN.matcher(src);
            if (mat.find()) {
              if (baseUrl == null) { baseUrl = new URL(srcUrl); } // Copycat of parseLink()
              cb.foundLink(resolveUri(baseUrl, URLDecoder.decode(mat.group(1), "UTF-8")));
            }
          }
        }
        break;
        
      case 'm':
      case 'M':
        if (beginsWithTag(link, METATAG)) {
          if (REFRESH.equalsIgnoreCase(getAttributeValue(HTTP_EQUIV, link))) {
            String value = getAttributeValue(HTTP_EQUIV_CONTENT, link);
            if (value != null) {
              // <meta http-equiv="refresh" content="...">
              int i = value.indexOf(";url=");
              if (i >= 0) {
                String refreshUrl = value.substring(i+5);
                // javascript:openRTWindow(url);
                Matcher mat = OPEN_RT_WINDOW_PATTERN.matcher(refreshUrl);
                if (mat.find()) {
                  if (baseUrl == null) { baseUrl = new URL(srcUrl); } // Copycat of parseLink()
                  cb.foundLink(resolveUri(baseUrl, mat.group(1)));
                }
              }
            }
          } else {
            if (PDF_NAME.equalsIgnoreCase(getAttributeValue(NAME, link)) ||
                FT_NAME.equalsIgnoreCase(getAttributeValue(NAME, link))) {
              
              String url = getAttributeValue(CONTENT, link);
              if (url != null) {
                cb.foundLink(url);
              }
            }
          }
        }
        break;
    }
    
    return super.extractLinkFromTag(link, au, cb);
  }
  
}

