/*
 * $Id$
 */

/*

Copyright (c) 2018 Board of Trustees of Leland Stanford Jr. University,
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
in this Software without prior written authorization from Stanford University.
be used in advertising or otherwise to promote the sale, use or other dealings

*/

package org.lockss.plugin.silverchair.iwap;

import java.util.regex.Pattern;

import org.lockss.plugin.silverchair.BaseScArticleIteratorFactory;
import org.lockss.util.Logger;

public class IwapArticleIteratorFactory extends BaseScArticleIteratorFactory {
  
  private static final String ROOT_TEMPLATE = "\"%s%s/article\", base_url, journal_id";
  private static final String PATTERN_TEMPLATE = "\"^%s%s/article(-abstract)?/\", base_url, journal_id";
  
  private static final Pattern HTML_PATTERN = Pattern.compile("/article/([0-9i]+)/(.*)$", Pattern.CASE_INSENSITIVE);
  private static final String HTML_REPLACEMENT = "/article/$1/$2";
  private static final String ABSTRACT_REPLACEMENT = "/article-abstract/$1/$2";
  private static final String CITATION_REPLACEMENT = "/downloadcitation/$1?format=ris";
  // <meta name="citation_pdf_url" 
  protected static final Pattern PDF_PATTERN = Pattern.compile(
      "<meta[\\s]*name=\"citation_pdf_url\"[\\s]*content=\"(.+/article-pdf/[^.]+\\.pdf)\"", Pattern.CASE_INSENSITIVE);
  
  protected static Logger getLog() {
    return Logger.getLogger(IwapArticleIteratorFactory.class);
  }

  @Override
  protected String getRootTemplate() {
    return ROOT_TEMPLATE;
  }

  @Override
  protected String getPatternTemplate() {
    return PATTERN_TEMPLATE;
  }

  protected static Pattern getHtmlPattern() {
    return HTML_PATTERN;
  }

  protected static String getHtmlReplacement() {
    return HTML_REPLACEMENT;
  }

  protected static String getAbstractReplacement() {
    return ABSTRACT_REPLACEMENT;
  }

  protected static String getCitationReplacement() {
    return CITATION_REPLACEMENT;
  }

  protected static Pattern getPdfPattern() {
    return PDF_PATTERN;
  }
}
