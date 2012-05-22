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

package org.lockss.plugin.libertasacademica;

import java.io.*;

import junit.framework.Test;

import org.lockss.util.*;
import org.lockss.plugin.FilterFactory;
import org.lockss.test.*;

public class TestLibertasAcademicaHtmlFilterFactory extends LockssTestCase {
  private static FilterFactory fact;
  private static MockArchivalUnit mau;
  
  //Example instances mostly from pages of strings of HTMl that should be filtered
  //and the expected post filter HTML strings.
  
  //Related articles
  private static final String alsoReadHtml = 
  "<div class=\"alsoRead\">\n" +
	"<h3>Readers of this Article also read </h3>\n" +
	"<ul id=\"readers\">\n" +
      "<li><a href=\"a-new-support-measure-to-quantify-the-impact-of-local-optima-in-phylog-article-a2858\">A New Support Measure to Quantify the Impact of Local Optima in Phylogenetic Analyses</a></li>\n" +
              "<li><a href=\"a-novel-model-for-dna-sequence-similarity-analysis-based-on-graph-theo-article-a2855\">A Novel Model for DNA Sequence Similarity Analysis Based on Graph Theory</a></li>\n" +
              "<li><a href=\"factors-affecting-synonymous-codon-usage-bias-in-chloroplast-genome-of-article-a2916\">Factors Affecting Synonymous Codon Usage Bias in Chloroplast Genome of Oncidium Gower Ramsey</a></li>\n" +
              "<li><a href=\"single-domain-parvulins-constitute-a-specific-marker-for-recently-prop-article-a2835\">Single-Domain Parvulins Constitute a Specific Marker for Recently Proposed Deep-Branching Archaeal Subgroups</a></li>\n" +
              "<li><a href=\"phylogenomics-based-reconstruction-of-protozoan-species-tree-article-a2785\">Phylogenomics-Based Reconstruction of Protozoan Species Tree</a></li>\n" +
      "</ul>\n" +
	"</div>Hello";
  
  private static final String alsoReadHtmlFiltered = "Hello";
    
 //Variant to test with Crawl Filter
 public static class TestCrawl extends TestLibertasAcademicaHtmlFilterFactory {
	  
	  public void setUp() throws Exception {
		  super.setUp();
		  fact = new LibertasAcademicaHtmlCrawlFilterFactory();
	  }

  }
  
//Variant to test with Hash Filter
public static class TestHash extends TestLibertasAcademicaHtmlFilterFactory {
	//Example instances mostly from pages of strings of HTMl that should be filtered
	//and the expected post filter HTML strings.
	//Ad
	private static final String adHolderHtmlHash = 
	"<div id=\"ad_holder\">\n" +
        "<a href=\"sign_up_for_email_alerts.php\" style=\"position: absolute; top: 0px; left: 0px; display: block; z-index: 3; opacity: 0.713; width: 120px; height: 600px;\"><img src=\"./images/side_banners/email_alerts.jpg\"></a>\n" +
        "<a href=\"author_loyalty_discount.php\" style=\"position: absolute; top: 0px; left: 0px; display: block; z-index: 4; opacity: 0.287; width: 120px; height: 600px;\"><img src=\"./images/side_banners/author_loyalty.jpg\"></a>\n" +
        "<a href=\"author_resources.php?folder_id=134\" style=\"position: absolute; top: 0px; left: 0px; display: none; z-index: 3; opacity: 0; width: 120px; height: 600px;\"><img src=\"./images/side_banners/reprints.jpg\"></a>\n" +
    "</div>\n" +
    "<div id=\"not_ad_holder\"></div>";
	private static final String adHolderHtmlHashFiltered = 
	"\n" +
    "<div id=\"not_ad_holder\"></div>";
    
    
    //Dicussion and comments
	private static final String commentsBoxesHtmlHash = 
    "<div id=\"commentsBoxes\" class=\"abstract\">\n" +
    "\n" +
    "    <p>\n" +
    "        <strong>Discussion</strong>\n" +
	"<img class=\"postAComment\" style=\"float: right; cursor: pointer;\" alt=\"Add A Comment\" src=\"img/add_a_comment_btn.gif\">\n" +
	"<br style=\"clear:both\">\n" +
    "       			<b>No comments yet...Be the first to comment.</b>\n" +
	"		            </p>\n" +
    "<br>\n" +
    "<span style=\"float: left;margin-left: -30px\" class=\"shareon\">share on</span><br>\n" +
	"<div style=\"float: left;\" class=\"socialmedia\">\n" +
	"   <img src=\"images/connotea_icon.gif\">&nbsp;<a href=\"http://www.connotea.org/addpopup?continue=confirm&amp;uri=http://la-press.com/a-new-support-measure-to-quantify-the-impact-of-local-optima-in-phylog-article-a2858&amp;title=A New Support Measure to Quantify the Impact of Local Optima in Phylogenetic Analyses\" target=\"_blank\">CONNOTEA</a>\n" +
	"    <img src=\"images/citeulike_icon.gif\">&nbsp;<a href=\"http://www.citeulike.org/posturl?&amp;url=http%3A%2F%2Fla-press.com%2Fa-new-support-measure-to-quantify-the-impact-of-local-optima-in-phylog-article-a2858&amp;title=A+New+Support+Measure+to+Quantify+the+Impact+of+Local+Optima+in+Phylogenetic+Analyses\" target=\"_blank\">CITEULIKE</a>\n" +
	"    <img src=\"images/facebook_logo.gif\"><a href=\"http://www.facebook.com/sharer.php?u=http://la-press.com/article.php?article_id=2858&amp;t=A New Support Measure to Quantify the Impact of Local Optima in Phylogenetic Analyses\">FACEBOOK</a>\n" +
	"    <img src=\"images/linkedin_logo.gif\"><a href=\"http://www.linkedin.com/shareArticle?mini=true&amp;url=http://la-press.com/article.php?article_id=2858&amp;title=A New Support Measure to Quantify the Impact of Local Optima in Phylogenetic Analyses&amp;source=http://www.la-press.com\">LINKED IN</a>\n" +
	"</div><!-- socialmedia ends -->\n" +
    "</div>/n" +
	"<div class=\"abstract\"></div>";
	
	private static final String commentsBoxesHtmlHashFiltered = 
    "/n" +
	"<div class=\"abstract\"></div>";
    
    //in page scripts
	private static final String javascriptHtmlHash = 
			  "<noscript> onversion/1070139620/?label=_s1RCLTo-QEQ5JGk_gM&amp;amp;guid=ON&amp;amp; </noscript>\n" +
			  "<script type=\"text/javascript\" src=\"http://nejm.resultspage.com/autosuggest/searchbox_suggest_v1.js\" language=\"javascript\">Hello</script>";
	private static final String javascriptHtmlHashFiltered = 
			  "<noscript> onversion/1070139620/?label=_s1RCLTo-QEQ5JGk_gM&amp;amp;guid=ON&amp;amp; </noscript>\n";
	
	
	//Chat with support availability status changes
	private static final String searchLeftHtmlHash = 
	"<div class=\"searchleft\">\n" +
	"			<a class=\"help\" title=\"\" href=\"\" onclick=\"psuEEQow(); return false;\">Need Help?</a>\n" +
	"			<!-- BEGIN ProvideSupport.com Graphics Chat Button Code -->\n" +
	"			<div id=\"ciuEEQ\"></div><div style=\"display:inline\" id=\"scuEEQ\"><a onclick=\"psuEEQow(); return false;\" href=\"#\"><img width=\"77\" height=\"16\" border=\"0\" src=\"http://image.providesupport.com/image/lap/offline-1285941633.jpg\" name=\"psuEEQimage\"></a></div>\n" +
	"			<div style=\"display:none\" id=\"sduEEQ\"><script type=\"text/javascript\" src=\"http://image.providesupport.com/js/lap/safe-standard.js?ps_h=uEEQ&amp;ps_t=1333135181979\"></script></div>\n" +
	"			<script type=\"text/javascript\">\n" +
	"			var seuEEQ=document.createElement(\"script\");\n" +
	"				seuEEQ.type=\"text/javascript\";\n" +
	"				var seuEEQs=(location.protocol.indexOf(\"https\")==0?\"https://secure.providesupport.com/image\":\"http://image.providesupport.com\")+\"/js/lap/safe-standard.js?ps_h=uEEQ\u0026ps_t=\"+new Date().getTime();\n" +
	"				setTimeout(\"seuEEQ.src=seuEEQs;document.getElementById('sduEEQ').appendChild(seuEEQ)\",1)\n" +
	"			</script>\n" +
	"			<noscript>\n" +
	"				&lt;div style=\"display:inline\"&gt;&lt;a href=\"http://www.providesupport.com?messenger=lap\"&gt;Live Chat&lt;/a&gt;&lt;/div&gt;\n" +
	"			</noscript>\n" +
	"			<!-- END ProvideSupport.com Graphics Chat Button Code -->\n" +
	"		</div><div class=\"searchleft2\"></div>";
	
	private static final String searchLeftHtmlHashFiltered = 
	"<div class=\"searchleft2\"></div>";
			
	//Rotating user testimonials	
	private static final String testimonialsHtmlHash = 
	"<div class=\"categoriescolumn4\">\n" +
	"\n" +
	"		<span>Our Testimonials</span>\n" +
	"		<div>\n" +
	"		\n" +
	"						\n" +
	"		<blockquote>\n" +
	"	I found the process of going through submission, review, editing, and publication to be quite easy. Everything was handled professionally and competently. The quality of the reviews were as good as any I have experienced in 30 years of publishing in scientific journals.				</blockquote>\n" +
	"		\n" +
	"		Professor Bonnie Kaplan 				\n" +
	"		<em>(Departments of Paediatrics and Community Health Sciences, University of Calgary)</em>\n" +
	"		\n" +
	"		<a href=\"./testimonials.php\" style=\"font-weight: bolder;\">What Your Colleagues Say</a>\n" +
	"	\n" +
	"	</div><!-- categoriescolumn4 -->\n" +
	"</div><!-- categoriescolumn4 -->";
	
	private static final String testimonialsHtmlHashFiltered = 
	"<!-- categoriescolumn4 -->";
	
	
	//# article views
	public static final String articleViewHtmlHash = 	
	"<div style=\"float: right; width: 257px;\" class=\"yellowbgright1\">\n" +
	"<span>495,300</span> Article Views\n" +
	"</div>\n" +
	"<span>495,300</span> Article Views\n";
	
	public static final String articleViewHtmlHashFiltered = 	
	"\n" +
	"<span>495,300</span> Article Views\n";
	
	//# total libertas academica article views
	public static final String totalArticleViewHtmlHash = 
	"<p class=\"laarticleviews\">\n" +
	"\n" +
	"	<span>6,821,389</span> Libertas Article Views\n" +
	"\n" +
	"</p> ";
	
	public static final String totalArticleViewHtmlHashFiltered = 
	" ";

	  public void setUp() throws Exception {
		  super.setUp();
		  fact = new LibertasAcademicaHtmlHashFilterFactory();
	  }
	  
	  public void testJavascriptHtmlHashFiltering() throws Exception {
	    InputStream actIn = fact.createFilteredInputStream(mau, 
	    												   new StringInputStream(javascriptHtmlHash),
	    												   Constants.DEFAULT_ENCODING);
	    
	    assertEquals(javascriptHtmlHashFiltered, StringUtil.fromInputStream(actIn));
	  }
	  
	  public void testAdHolderHtmlHashFiltering() throws Exception {
	    InputStream actIn = fact.createFilteredInputStream(mau, 
	    												   new StringInputStream(adHolderHtmlHash),
	    												   Constants.DEFAULT_ENCODING);
	    
	    assertEquals(adHolderHtmlHashFiltered, StringUtil.fromInputStream(actIn));
	  }
	  
	  public void testCommentsBoxHtmlHashFiltering() throws Exception {
	    InputStream actIn = fact.createFilteredInputStream(mau, 
	    												   new StringInputStream(commentsBoxesHtmlHash),
	    												   Constants.DEFAULT_ENCODING);
	    
	    assertEquals(commentsBoxesHtmlHashFiltered, StringUtil.fromInputStream(actIn));
	  }
	  
	  public void testSearchLeftHtmlHashFiltering() throws Exception {
	    InputStream actIn = fact.createFilteredInputStream(mau, 
	    												   new StringInputStream(searchLeftHtmlHash),
	    												   Constants.DEFAULT_ENCODING);
	    
	    assertEquals(searchLeftHtmlHashFiltered, StringUtil.fromInputStream(actIn));
	  }
	  
	  public void testTestimonialsHtmlHashFiltering() throws Exception {
	    InputStream actIn = fact.createFilteredInputStream(mau, 
	    												   new StringInputStream(testimonialsHtmlHash),
	    												   Constants.DEFAULT_ENCODING);
	    
	    assertEquals(testimonialsHtmlHashFiltered, StringUtil.fromInputStream(actIn));
	  }
	  
	  public void testArticleViewHtmlHashFiltering() throws Exception {
	    InputStream actIn = fact.createFilteredInputStream(mau, 
	    												   new StringInputStream(articleViewHtmlHash),
	    												   Constants.DEFAULT_ENCODING);
	    
	    assertEquals(articleViewHtmlHashFiltered, StringUtil.fromInputStream(actIn));
	  }
	  
	  public void testTotalArticleViewHtmlHashFiltering() throws Exception {
	    InputStream actIn = fact.createFilteredInputStream(mau, 
	    												   new StringInputStream(totalArticleViewHtmlHash),
	    												   Constants.DEFAULT_ENCODING);
	    
	    assertEquals(totalArticleViewHtmlHashFiltered, StringUtil.fromInputStream(actIn));
	  }
	  
}

public static Test suite() {
    return variantSuites(new Class[] {
	TestCrawl.class,
	TestHash.class
      });
  }
  
  public void testAlsoReadHtmlFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
    											       new StringInputStream(alsoReadHtml),
    											       Constants.DEFAULT_ENCODING);
    
    assertEquals(alsoReadHtmlFiltered, StringUtil.fromInputStream(actIn));
  }
  
}
