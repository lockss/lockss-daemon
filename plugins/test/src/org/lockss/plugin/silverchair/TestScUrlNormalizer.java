/*
 * $Id: TestScUrlNormalizer.java,v 1.1 2015-02-03 03:07:34 thib_gc Exp $
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

package org.lockss.plugin.silverchair;

import org.lockss.plugin.*;
import org.lockss.test.LockssTestCase;

public class TestScUrlNormalizer extends LockssTestCase {

  protected UrlNormalizer norm;
  
  public void setUp() {
    this.norm = new ScUrlNormalizer();
  }
  
  public void testArticleId() throws Exception {
    assertEquals("http://www.example.com/article.aspx?articleid=1234444",
                 norm.normalizeUrl("http://www.example.com/article.aspx?articleid=1234444", null));
    assertEquals("http://www.example.com/article.aspx?articleid=1234444",
                 norm.normalizeUrl("http://www.example.com/article.aspx?articleID=1234444", null));
    assertEquals("http://www.example.com/article.aspx?articleid=1234444",
                 norm.normalizeUrl("http://www.example.com/Article.aspx?ArticleID=1234444", null));
  }
  
  public void testIssue() throws Exception {
    assertEquals("http://www.example.com/Issue.aspx?issueid=111111&journalid=77",
                 norm.normalizeUrl("http://www.example.com/Issue.aspx?issueid=111111&journalid=77", null));
    assertEquals("http://www.example.com/Issue.aspx?issueid=111111&journalid=77",
                 norm.normalizeUrl("http://www.example.com/Issue.aspx?journalid=77&issueid=111111", null));
    assertEquals("http://www.example.com/Issue.aspx?issueid=111111&journalid=77",
                 norm.normalizeUrl("http://www.example.com/issue.aspx?issueid=111111&journalid=77", null));
    assertEquals("http://www.example.com/Issue.aspx?issueid=111111&journalid=77",
                 norm.normalizeUrl("http://www.example.com/issue.aspx?journalid=77&issueid=111111", null));
  }  
  
  public void testAtab() throws Exception {
    assertEquals("http://www.example.com/article.aspx?articleid=1234444",
                 norm.normalizeUrl("http://www.example.com/article.aspx?articleid=1234444&atab=123", null));
    assertEquals("http://www.example.com/article.aspx?articleid=1234444",
                 norm.normalizeUrl("http://www.example.com/article.aspx?articleid=1234444&atab=", null));
    assertEquals("http://www.example.com/article.aspx?articleid=1234444&foo=bar",
                 norm.normalizeUrl("http://www.example.com/article.aspx?articleid=1234444&foo=bar&atab=123", null));
    assertEquals("http://www.example.com/article.aspx?articleid=1234444&foo=bar",
                 norm.normalizeUrl("http://www.example.com/article.aspx?articleid=1234444&atab=123&foo=bar", null));
  }
 
  public void testUnicode2111() throws Exception {
    assertEquals("http://www.example.com/DownloadImage.aspx?image=/data/Journals/JABC/999123/jxy100001t1.png&sec=45688888&ar=1234444&imagename=",
                 norm.normalizeUrl("http://www.example.com/DownloadImage.aspx?image=/data/Journals/JABC/999123/jxy100001t1.png&sec=45688888&ar=1234444\u2111name=", null));
    assertEquals("http://www.example.com/DownloadImage.aspx?image=/data/Journals/JABC/999123/jxy100001t1.png&sec=45688888&ar=1234444&imagename=jxy100001t1.png",
                 norm.normalizeUrl("http://www.example.com/DownloadImage.aspx?image=/data/Journals/JABC/999123/jxy100001t1.png&sec=45688888&ar=1234444\u2111name=jxy100001t1.png", null));
  }
  
}
