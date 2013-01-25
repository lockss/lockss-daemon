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

package org.lockss.plugin.hindawi;

import java.io.*;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.lockss.util.*;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.test.*;

public class TestHindawiPublishingCorporationHtmlFilterFactory extends LockssTestCase {
  private HindawiPublishingCorporationHtmlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new HindawiPublishingCorporationHtmlFilterFactory();
  }
  
 /* throws PluginException {
      NodeFilter[] filters = new NodeFilter[] {
      // Filter out <script> tags that seem to be edited often
	    new TagNameFilter("script"),
      // Filter out <div id="left_column">...</div>
      HtmlNodeFilters.tagWithAttribute("div", "id", "left_column"),
      // ASP cookies; once without '__', now with  
      HtmlNodeFilters.tagWithAttribute("input", "id", "VIEWSTATE"),
      HtmlNodeFilters.tagWithAttribute("input", "id", "__VIEWSTATE"),
      // ASP cookies; once without '__', now with  
      HtmlNodeFilters.tagWithAttribute("input", "id", "EVENTVALIDATION"),
      HtmlNodeFilters.tagWithAttribute("input", "id", "__EVENTVALIDATION"),
  };*/
  //Example instances mostly from pages of strings of HTMl that should be filtered
  //and the expected post filter HTML strings.
  
  private static final String scriptHtml =
		 "<script type=\"text/javascript\"></script>";
  private static final String scriptHtmlFiltered =
  		 "";
  
  private static final String stuffScriptHtml =
		 "<body>" +
		 "<div id=\"footer\">" +
		 "</div>" +
		 "<script type=\"text/javascript\"></script>" +
		 "</body>";
  private static final String stuffScriptHtmlFiltered =
	  	 "<body>" +
		 "<div id=\"footer\">" +
		 "</div>" +
		 "</body>";
  
  private static final String stuffScriptStuffHtml =
		  "<header> \"Stuff\" </header>" +
          "<img scr= \"http://images.jpg\"/>" +
          "<script type=\"text/javascript\"></script>" +
          "<div class=\"logo\"></div>" +
          "<a href=\"/\" id=\"ctl00_logourl\"></a>" +
          "<script type=\"text/javascript\"></script>" +
          "<div id=\"footer\"></div>";	  
  private static final String stuffScriptStuffHtmlFiltered =
		  "<header> \"Stuff\" </header>" +
		  "<img scr= \"http://images.jpg\"/>" +
		  "<div class=\"logo\"></div>" +
          "<a href=\"/\" id=\"ctl00_logourl\"></a>" +	  
		  "<div id=\"footer\"></div>";
  
  private static final String leftColumnHtml =
		"<div class=\"left_column\"><div id=\"left_column\" class=\"InnerRight\">\n" +
  		"Foo bar</div></div>";
  private static final String leftColumnHtmlFiltered =
		  "<div class=\"left_column\"></div>";
  
  private static final String viewStateHtml =
		"<input id=\"VIEWSTATE\">";
  private static final String viewStateHtmlFiltered =
		  "";
  
  private static final String viewState2Html =
		"<input id=\"__VIEWSTATE\" value=\"__VIEWSTATE\">";
  private static final String viewState2HtmlFiltered =
		  "";
  
  private static final String eventValidationHtml =
		"<input id=\"EVENTVALIDATION\" value=\"EVENTVALIDATION\">";
		  
  private static final String eventValidationHtmlFiltered =
		  "";
  
  private static final String eventValidation2Html =
		"<input id=\"__EVENTVALIDATION\" value=\"__EVENTVALIDATION\">";
			  
  private static final String eventValidation2HtmlFiltered =
		  "";
  
  private static final String cleaningUpHeaderStuffHtml = 
      "<!DOCTYPE html PUBLIC \"-//W3C//DTD MathML 2.0//EN\" \"http://www.w3.org/Math/DTD/mathml2/mathml2.dtd\">" +
          "<html xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:mml=\"http://www.w3.org/1998/Math/MathML\">" +
          "<head id=\"ctl00_Header\">" +
          "<title>Commentary: Personal computers&#x2013;valuable tools or expensive luxuries? </title>\">" +
          "</head></html>";

  private static final String cleaningUpHeaderStuffHtmlFiltered = 
          "<html>" +
          "</html>";
  
  private static final String svgContentHtml = 
      "</a></div><div class=\"groupcaption\"><b>Figure 10: </b>" +
          "Coherency caption goes here" +
          "<svg style=\"vertical-align:-3.39066pt;width:30.025px;\" " +
          "id=\"M47\" height=\"16.299999\" version=\"1.1\" viewBox=\"0 0 30.025 16.299999\" width=\"30.025\" " +     
          "xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns=\"http://www.w3.org/2000/svg\">" +
          "<g transform=\"matrix(.017,0,0,-.017,.062,12.013)\"><use xlink:href=\"#x1D463\"/></g>" +
          "<g transform=\"matrix(.012,0,0,-.012,8.225,16.088)\"><use xlink:href=\"#x1D460\"/></g>" +
          "" +
          "<g transform=\"matrix(.017,0,0,-.017,13.313,12.013)\"><use xlink:href=\"#x2F\"/></g><g transform=\"matrix(.017,0,0,-.017,20.316,12.013)\">" +
          "<use xlink:href=\"#x1D6FC\"/></g></svg>"; 

  private static final String svgContentFiltered =
      "</a></div><div class=\"groupcaption\"><b>Figure 10: </b>" +
          "Coherency caption goes here";     

  private static final String styleWidthHtml=
      "<td align=\"center\" colspan=\"2\"><span style=\"width: 17.7875px;\">" +
          "<svg style=\"vertical\">" +
          "<g transform=\"matrix(.012,0,0,-.012,8.225,16.088)\"><use xlink:href=\"#x1D460\"/></g>" +
          "</svg>" +
          "</span></td>" +
          "<td align=\"center\" colspan=\"2\">Bias</td>";
  private static final String styleWidthFiltered=
      "<td align=\"center\" colspan=\"2\"></td>" +
          "<td align=\"center\" colspan=\"2\">Bias</td>";
  
  public void testScriptFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(scriptHtml),
        Constants.DEFAULT_ENCODING);

    assertEquals(scriptHtmlFiltered, StringUtil.fromInputStream(actIn));
  }

  public void testStuffScriptFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(stuffScriptHtml),
        Constants.DEFAULT_ENCODING);

    assertEquals(stuffScriptHtmlFiltered, StringUtil.fromInputStream(actIn));
  }



  public void testStuffScriptStuffFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(stuffScriptStuffHtml),
        Constants.DEFAULT_ENCODING);

    assertEquals(stuffScriptStuffHtmlFiltered, StringUtil.fromInputStream(actIn));
  }



  public void testLeftColumnFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(leftColumnHtml),
        Constants.DEFAULT_ENCODING);

    assertEquals(leftColumnHtmlFiltered, StringUtil.fromInputStream(actIn));
  }

  public void testViewStateFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(viewStateHtml),
        Constants.DEFAULT_ENCODING);

    assertEquals(viewStateHtmlFiltered, StringUtil.fromInputStream(actIn));
  }

  public void testViewState2Filtering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau, 
        new StringInputStream(viewState2Html),
        Constants.DEFAULT_ENCODING);

    assertEquals(viewState2HtmlFiltered, StringUtil.fromInputStream(actIn));
  }

  public void testEventValidationFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau, 
        new StringInputStream(eventValidationHtml),
        Constants.DEFAULT_ENCODING);

    assertEquals(eventValidationHtmlFiltered, StringUtil.fromInputStream(actIn));
  }

  public void testeventValidation2Filtering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(eventValidation2Html),
        Constants.DEFAULT_ENCODING);
    assertEquals(eventValidation2HtmlFiltered, StringUtil.fromInputStream(actIn));

  }
  
  public void testHeaderFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(cleaningUpHeaderStuffHtml),
        Constants.DEFAULT_ENCODING);

    assertEquals(cleaningUpHeaderStuffHtmlFiltered, StringUtil.fromInputStream(actIn));
  }
  
  public void testSVGContent() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(svgContentHtml),
        Constants.DEFAULT_ENCODING);

    assertEquals(svgContentFiltered, StringUtil.fromInputStream(actIn));
  }

  public void testAutoWidth() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(styleWidthHtml),
        Constants.DEFAULT_ENCODING);

    assertEquals(styleWidthFiltered, StringUtil.fromInputStream(actIn));
  } 

}

