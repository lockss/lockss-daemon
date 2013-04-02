/* $Id: ArticleTester.java,v 1.1 2013-04-02 21:18:45 ldoan Exp $
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.americansocietyofcivilengineers;

import junit.framework.Assert;
import org.lockss.plugin.*;

/*
 * Contains methods to test a single article files
 * with different roles: abstract, full-text HTML, full-text PDF, and References..
 * Called from class TestASCEArticleIterator.
 */

public class ArticleTester {
  private ArticleFiles af;
  private String expectedUrlInitial;
            
  public ArticleTester() {
  }
       
  public ArticleTester(ArticleFiles af, String expectedUrlInitial) {
    this.af = af;
    this.expectedUrlInitial = expectedUrlInitial;
  }
  
  // Verify the return URL associated with the article role Abstract
  public String testAbstract(String matchStr) {
    String abstractUrl = af.getRoleUrl(ArticleFiles.ROLE_ABSTRACT);
    if ((abstractUrl != null) && (abstractUrl.contains(matchStr))) {
      String expectedUrl = expectedUrlInitial.replaceFirst("/fromString/", "/" + matchStr + "/");
      Assert.assertEquals(expectedUrl, abstractUrl);
      return (abstractUrl);
    }
    return (null);
  }
  
  // Verify the return url associated with the article role FullTextHtml.
  public String testFullTextHtml(String matchStr) {
    String fullTextHtmlUrl = af.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_HTML);
    if ((fullTextHtmlUrl != null) && (fullTextHtmlUrl.contains(matchStr))) {
      String expectedUrl = expectedUrlInitial.replaceFirst("/fromString/", "/" + matchStr + "/");
      Assert.assertEquals(expectedUrl, fullTextHtmlUrl);
      return (fullTextHtmlUrl);
    }
    return (null);
  }
    
  // Verify the return url associated with the article role FullTextPdf.
  public String testRef(String matchStr) {
    String refUrl = af.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_PDF);
    if ((refUrl != null) && (refUrl.contains(matchStr))) {
      String expectedUrl = expectedUrlInitial.replaceFirst("/fromString/", "/" + matchStr + "/");
      Assert.assertEquals(expectedUrl, refUrl);
      return (refUrl);
    }
    return (null);
  }

  // Verify the return URL associated with the article role FullTextPdf.
  public String testFullTextPdf(String matchStr) {
    String fullTextPdfUrl = af.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_PDF);
    if ((fullTextPdfUrl != null) && (fullTextPdfUrl.contains(matchStr))) {
      String expectedUrl = expectedUrlInitial.replaceFirst("/fromString/", "/" + matchStr + "/");
      Assert.assertEquals(expectedUrl, fullTextPdfUrl);
      return (fullTextPdfUrl);
    }
    return (null);
  }

}