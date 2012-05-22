/*
Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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


package org.lockss.plugin.taylorandfrancis;

import java.io.*;

import junit.framework.Test;

import org.lockss.util.*;
import org.lockss.plugin.FilterFactory;
import org.lockss.test.*;

public class TestTaylorAndFrancisHtmlFilterFactory extends LockssTestCase {
  public FilterFactory fact;
  private static MockArchivalUnit mau;
  
  //Example instances mostly from pages of strings of HTMl that should be filtered
  //and the expected post filter HTML strings. These are shared crawl & hash filters
  
  private static final String newsArticlesHtml =
  "<div id=\"newsArticles\" class=\"module widget\"><h2>Journal news</h2><ul><li>" +
  "<a href=\"http://www.tandfonline.com/doi/abs/10.1080/00018732.2011.598293\"><b>2010 Impact Factor: 21.214 / 2nd in Condensed Matter Physics</b></a>" +
  "</li></ul></div>";
  private static final String newsArticlesHtmlFiltered =
  "";

  private static final String relatedHtml =
  "<div id=\"relatedArticles\" class=\"module widget\"><h2>Related Articles</h2><div class=\"jqueryTab tabs clear\">" +
  "<ul class=\"tabsNav clear\"><li class=\"active\" id=\"readTab\"><a class='jslink' href=\"#read\">Most read</a>" +
  "</li></ul></div></div><div class=\"tabsPanel articleSummaries hide\" id=\"citedPanel\"></div>";
  private static final String relatedHtmlFiltered =
  "<div class=\"tabsPanel articleSummaries hide\" id=\"citedPanel\"></div>";

  private static final String moduleHtml =
  "<!-- ads module --><div class=\"ad module\"><!-- Literatum Advertisement --><!-- width:200 -->" +
  "<!-- placeholder id=null, description=Journal right column 1 --></div>";
  private static final String moduleHtmlFiltered =
  "<!-- ads module -->";

  // Variant to test with Crawl Filter
  public static class TestCrawl extends TestTaylorAndFrancisHtmlFilterFactory {
	  
	  
	  public void setUp() throws Exception {
		  super.setUp();
		  fact = new TaylorAndFrancisHtmlCrawlFilterFactory();
	  }
  }

  // Variant to test with Hash Filter
  public static class TestHash extends TestTaylorAndFrancisHtmlFilterFactory {
	  
	  //Example instances mostly from pages of strings of HTMl that should be filtered
	  //and the expected post filter HTML strings.
	  private static final String sfxLinkHtmlHash = 
			  "<a class=\"sfxLink\"></a>";
	  private static final String sfxLinkHtmlHashFiltered = 
			  "";
	  
	  private static final String brandingHtmlHash = 
			  "<div id=\"branding\"></div>";
	  private static final String brandingHtmlHashFiltered = 
			  "";
	  
	  private static final String rssHtmlHash = 
			  "<link type=\"application/rss+xml\" rel=\"hello\" href=\"world\"/>";
	  private static final String rssHtmlHashFiltered = 
			  "";
	  
	  private static final String creditsHtmlHash = 
	  		  "<div class=\"re-creditss-due\"></div>";
	  private static final String creditsHtmlHashFiltered = 
	  		  "";
	  
	  private static final String rssLinkHtmlHash = 
			  "<a href=\"/example&feed=rss&foo=bar\">Example</a>";
	  private static final String rssLinkHtmlHashFiltered = 
			  "";
	  
	  private static final String accessHtmlHash = 
			  "<div class=\"foo accessIconWrapper bar\">Yes</div>";
	  private static final String accessHtmlHashFiltered = 
			  "";
	  
	  private static final String linkoutHtmlHash = 
			  "<a href=\"/servlet/linkout?yes=no&foo=bar\">Example</a>";
	  private static final String linkoutHtmlHashFiltered = 
			  "";
	  
	  private static final String javascriptHtmlHash = 
			  "<noscript> onversion/1070139620/?label=_s1RCLTo-QEQ5JGk_gM&amp;amp;guid=ON&amp;amp; </noscript>\n" +
			  "<script type=\"text/javascript\" src=\"http://nejm.resultspage.com/autosuggest/searchbox_suggest_v1.js\" language=\"javascript\">Hello</script>";
	  private static final String javascriptHtmlHashFiltered = 
			  "<noscript> onversion/1070139620/?label=_s1RCLTo-QEQ5JGk_gM&amp;amp;guid=ON&amp;amp; </noscript>\n";
	  
  
	  public void setUp() throws Exception {
		  super.setUp();
		  fact = new TaylorAndFrancisHtmlHashFilterFactory();
	  }
	  
	  public void testSfxLinkHtmlHashFiltering() throws Exception {
	    InputStream actIn = fact.createFilteredInputStream(mau,
	    												   new StringInputStream(sfxLinkHtmlHash),
	    												   Constants.DEFAULT_ENCODING);
	    
	    assertEquals(sfxLinkHtmlHashFiltered, StringUtil.fromInputStream(actIn));
	  }
	  
	  public void testBrandingHtmlHashFiltering() throws Exception {
	    InputStream actIn = fact.createFilteredInputStream(mau,
	    												   new StringInputStream(brandingHtmlHash),
	    												   Constants.DEFAULT_ENCODING);
	    
	    assertEquals(brandingHtmlHashFiltered, StringUtil.fromInputStream(actIn));
	  }
	  
	  public void testRssHtmlHashFiltering() throws Exception {
	    InputStream actIn = fact.createFilteredInputStream(mau,
	    												   new StringInputStream(rssHtmlHash),
	    												   Constants.DEFAULT_ENCODING);
	    
	    assertEquals(rssHtmlHashFiltered, StringUtil.fromInputStream(actIn));
	  }
	  
	  public void testRssLinkHtmlHashFiltering() throws Exception {
	    InputStream actIn = fact.createFilteredInputStream(mau,
	    												   new StringInputStream(rssLinkHtmlHash),
	    												   Constants.DEFAULT_ENCODING);
	    
	    assertEquals(rssLinkHtmlHashFiltered, StringUtil.fromInputStream(actIn));
	  }
	  
	  public void testCreditsHtmlHashFiltering() throws Exception {
	    InputStream actIn = fact.createFilteredInputStream(mau,
	    												   new StringInputStream(creditsHtmlHash),
	    												   Constants.DEFAULT_ENCODING);
	    												   
	    assertEquals(creditsHtmlHashFiltered, StringUtil.fromInputStream(actIn));
	  }
	  
	  public void testJavascriptHtmlHashFiltering() throws Exception {
	    InputStream actIn = fact.createFilteredInputStream(mau, 
	    												   new StringInputStream(javascriptHtmlHash),
	    												   Constants.DEFAULT_ENCODING);
	    
	    assertEquals(javascriptHtmlHashFiltered, StringUtil.fromInputStream(actIn));
	  }
	  
	  
	  
	  public void testAccessHtmlHashFiltering() throws Exception {
	    InputStream actIn = fact.createFilteredInputStream(mau, 
	    												   new StringInputStream(accessHtmlHash),
	    												   Constants.DEFAULT_ENCODING);
	    
	    assertEquals(accessHtmlHashFiltered, StringUtil.fromInputStream(actIn));
	  }
	  
	  public void testLinkoutHtmlHashFiltering() throws Exception {
	    InputStream actIn = fact.createFilteredInputStream(mau, 
	    												   new StringInputStream(linkoutHtmlHash),
	    												   Constants.DEFAULT_ENCODING);
	    
	    assertEquals(linkoutHtmlHashFiltered, StringUtil.fromInputStream(actIn));
	  }
	  
  }

  public static Test suite() {
    return variantSuites(new Class[] {
	TestCrawl.class,
	TestHash.class
      });
  }
  
  public void testRelatedHtmlFiltering() throws Exception {
    InputStream actIn =
      fact.createFilteredInputStream(mau, new StringInputStream(relatedHtml),
				     Constants.DEFAULT_ENCODING);
    assertEquals(StringUtil.fromInputStream(actIn),
    		relatedHtmlFiltered);
  }
  
  public void testNewsArticlesHtmlFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
    												   new StringInputStream(newsArticlesHtml),
    												   Constants.DEFAULT_ENCODING);
    
    assertEquals(newsArticlesHtmlFiltered, StringUtil.fromInputStream(actIn));
  }
  
  public void testModuleHtmlFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau, 
    												   new StringInputStream(moduleHtml),
    												   Constants.DEFAULT_ENCODING);
    
    assertEquals(moduleHtmlFiltered, StringUtil.fromInputStream(actIn));
  }
  
}
