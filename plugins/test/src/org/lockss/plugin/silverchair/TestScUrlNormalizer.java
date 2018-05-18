/*
 * $Id$
 */

/*

Copyright (c) 2000-2018 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.Properties;

import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.*;
import org.lockss.plugin.definable.DefinablePlugin;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;

public class TestScUrlNormalizer extends LockssTestCase {

  protected UrlNormalizer norm;
	public static final String BASE_URL = "http://www.example.com";
	public static final String HTTPS_BASE_URL = "https://www.example.com";

	static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
	static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
	static final String RES_ID_KEY = "resource_id";
	private DefinablePlugin plugin;
	private MockArchivalUnit http_mau;
	private MockArchivalUnit https_mau2;

	public void setUp() throws Exception {
		super.setUp();
	    this.norm = new ScUrlNormalizer();
		plugin = new DefinablePlugin();
		plugin.initPlugin(getMockLockssDaemon(),
				"org.lockss.plugin.silverchair.ClockssSilverchairJournalsPlugin");
		Properties props = new Properties();
		//http
		props.setProperty(YEAR_KEY, "2016");
		props.setProperty(RES_ID_KEY, "140");
		props.setProperty(BASE_URL_KEY, BASE_URL);
		Configuration config = ConfigurationUtil.fromProps(props);
		http_mau = new MockArchivalUnit();
		http_mau.setConfiguration(config);

		props.setProperty(BASE_URL_KEY, HTTPS_BASE_URL);
		Configuration config2 = ConfigurationUtil.fromProps(props);
		https_mau2 = new MockArchivalUnit();
		https_mau2.setConfiguration(config2);
	}	
  

  public void testArticleId() throws Exception {
	assertEquals("http://www.example.com/article.aspx?articleid=1234444",
               norm.normalizeUrl("http://www.example.com/article.aspx?articleid=1234444", http_mau));
	    // do we normalize https back to http??
	assertEquals("http://www.example.com/article.aspx?articleid=1234444",
                norm.normalizeUrl("https://www.example.com/article.aspx?articleid=1234444", http_mau));
    assertEquals("http://www.example.com/article.aspx?articleid=1234444",
                 norm.normalizeUrl("http://www.example.com/article.aspx?articleID=1234444", http_mau));
    assertEquals("http://www.example.com/article.aspx?articleid=1234444",
                 norm.normalizeUrl("http://www.example.com/Article.aspx?ArticleID=1234444", http_mau));
    // now starting from https - stay on https
    assertEquals("https://www.example.com/article.aspx?articleid=1234444",
            norm.normalizeUrl("https://www.example.com/article.aspx?articleid=1234444", https_mau2));
  }
  
  public void testProceedingId() throws Exception {
    assertEquals("http://www.example.com/proceeding.aspx?articleid=1234444",
                 norm.normalizeUrl("http://www.example.com/proceeding.aspx?articleid=1234444", http_mau));
    assertEquals("http://www.example.com/proceeding.aspx?articleid=1234444",
                 norm.normalizeUrl("http://www.example.com/proceeding.aspx?articleID=1234444", http_mau));
    assertEquals("http://www.example.com/proceeding.aspx?articleid=1234444",
                 norm.normalizeUrl("http://www.example.com/Proceeding.aspx?ArticleID=1234444", http_mau));
  }
  
  public void testIssue() throws Exception {
    assertEquals("http://www.example.com/Issue.aspx?issueid=111111&journalid=77",
                 norm.normalizeUrl("http://www.example.com/Issue.aspx?issueid=111111&journalid=77", http_mau));
    assertEquals("http://www.example.com/Issue.aspx?issueid=111111&journalid=77",
                 norm.normalizeUrl("http://www.example.com/Issue.aspx?journalid=77&issueid=111111", http_mau));
    assertEquals("http://www.example.com/Issue.aspx?issueid=111111&journalid=77",
                 norm.normalizeUrl("http://www.example.com/issue.aspx?issueid=111111&journalid=77", http_mau));
    assertEquals("http://www.example.com/Issue.aspx?issueid=111111&journalid=77",
                 norm.normalizeUrl("http://www.example.com/issue.aspx?journalid=77&issueid=111111", http_mau));
  }  
  
  public void testAtab() throws Exception {
    assertEquals("http://www.example.com/article.aspx?articleid=1234444",
                 norm.normalizeUrl("http://www.example.com/article.aspx?articleid=1234444&atab=123", http_mau));
    assertEquals("http://www.example.com/article.aspx?articleid=1234444",
                 norm.normalizeUrl("http://www.example.com/article.aspx?articleid=1234444&atab=", http_mau));
    assertEquals("http://www.example.com/article.aspx?articleid=1234444&foo=bar",
                 norm.normalizeUrl("http://www.example.com/article.aspx?articleid=1234444&foo=bar&atab=123", http_mau));
    assertEquals("http://www.example.com/article.aspx?articleid=1234444&foo=bar",
                 norm.normalizeUrl("http://www.example.com/article.aspx?articleid=1234444&atab=123&foo=bar", http_mau));
  }
 
  public void testUnicode2111() throws Exception {
    assertEquals("http://www.example.com/DownloadImage.aspx?image=/data/Journals/JABC/999123/jxy100001t1.png&sec=45688888&ar=1234444&imagename=",
                 norm.normalizeUrl("http://www.example.com/DownloadImage.aspx?image=/data/Journals/JABC/999123/jxy100001t1.png&sec=45688888&ar=1234444\u2111name=", http_mau));
    assertEquals("http://www.example.com/DownloadImage.aspx?image=/data/Journals/JABC/999123/jxy100001t1.png&sec=45688888&ar=1234444&imagename=jxy100001t1.png",
                 norm.normalizeUrl("http://www.example.com/DownloadImage.aspx?image=/data/Journals/JABC/999123/jxy100001t1.png&sec=45688888&ar=1234444\u2111name=jxy100001t1.png", http_mau));
  }
  
}
