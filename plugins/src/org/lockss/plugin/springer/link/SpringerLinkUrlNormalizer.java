/*
 * $Id: ScUrlNormalizer.java 39864 2015-02-18 09:10:24Z thib_gc $
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

package org.lockss.plugin.springer.link;

import java.util.regex.*;

import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.util.StringUtil;

/**
 */
public class SpringerLinkUrlNormalizer implements UrlNormalizer {

  public static final String SLASH_ENCODED = "%2F";
  public static final String SLASH = "/";
  public static final Pattern CHAPTER_DOI_PATTERN = Pattern.compile("(.+/chapter/.+)(_[0-9]+)(/fulltext.html)?");
  public static final Pattern CHAPTER_PDF_DOI_PATTERN = Pattern.compile("(.+/content/pdf/.+)(_[0-9]+)(\\.pdf)$");
  public static final String DOWNLOAD_URL_KEY = "download_url";
  
  @Override
  public String normalizeUrl(String url, ArchivalUnit au) throws PluginException {
    String baseUrl = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
    String downloadUrl = au.getConfiguration().get(DOWNLOAD_URL_KEY);
    if(url.startsWith(baseUrl) || url.startsWith(downloadUrl)) {
      if(!url.contains("pdf")) {
        url = StringUtil.replaceString(url, SLASH_ENCODED, SLASH);
      }
      if(url.contains("_")) {
        Matcher pdfMatcher = CHAPTER_PDF_DOI_PATTERN.matcher(url);
        Matcher chapterMatcher = CHAPTER_DOI_PATTERN.matcher(url);    
        if(chapterMatcher.matches()) {
          url = chapterMatcher.group(1);
          url = StringUtil.replaceString(url, "chapter", "book");
        } else if(pdfMatcher.matches()) {
          url = pdfMatcher.group(1) + pdfMatcher.group(3);
        }
      }
    }
    return url;
  }
}
