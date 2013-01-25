/*
 * $Id: TestNatureHtmlFilterFactory.java,v 1.2 2013-01-25 20:26:06 alexandraohlson Exp $
 */

package org.lockss.plugin.nature;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestNatureHtmlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private NaturePublishingGroupHtmlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new NaturePublishingGroupHtmlFilterFactory();
    mau = new MockArchivalUnit();
  }

  private static final String breadcrumbHtmlHash =
      "<div><div id=\"breadcrumb\"><div><a href=\"/onc/index.html\">Journal home</a>" +
          "<span class=\"divider\"> &#x0003E; </span>" +
          "<a href=\"/onc/journal/v29/n50/index.html\"> Archive</a>" +
          "<span class=\"divider\"> &#x0003E; </span>" +
          "<a href=\"/onc/journal/v29/n50/index.html#oa\">Original Articles</a>" +
          "<span class=\"divider\"> &#x0003E; </span>" +
          "<span class=\"thisitem\">Full text</span></div></div></div>";
  private static final String breadcrumbHtmlHashFiltered =
      "<div></div>";
  
 
 
  private static final String WhiteSpace1 = "\n  <li><a href=\"/content/pdf/1477-7525-8-103.pdf\">PDF</a>\n (543KB)\n </li>";
  
  private static final String WhiteSpace2 = "\n\n      <li><a href=\"/content/pdf/1477-7525-8-103.pdf\">PDF</a>\n       (543KB)\n      </li>";

  
  private static final String whitespace_a =
      " <!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">" +
          " \n" +
          " <html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"en\" xml:lang=\"en\">" +
          " <head>";

  private static final String whitespace_b = 
      " <!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">" +
          " <html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"en\" xml:lang=\"en\">" +
          " <head>";

  private static final String headHtml =
      " <head>" +
          " <!-- super head top -->" +
          " <title>" +
          "My Big Title</title>" +
          " <meta name=\"robots\" content=\"noarchive\" />" +
          " <link rel=\"home\" href=\"/onc/\" title=\"home\" />" +
          " <!-- style -->" +
          " </head>" +
          " <body class=\"wwwblah\" id=\"article\">";
  private static final String headHtmlFiltered =
          " <body class=\"wwwblah\" id=\"article\">";

  private static final String commentTags =
  " <!--[if IE 6]>" +
      " <script type=\"text/javascript\" src=\"/common/scripts/ie-specific/ie-6.js\">" +
      "</script>" +
      " <style type=\"text/css\" media=\"screen, projection, print\">" +
      " @import \"/common/includes/blah.css\"; @import \"/common/includes/more.css\"; </style>" +
      " <![endif]-->" +
      " <!-- end style -->" +
      " <link title=\"schema(PRISM)\" rel=\"schema.prism\" href=\"http://prismstandard.org/namespaces/1.2/basic/\" />";
  private static final String commentTagsFiltered =
      " <link title=\"schema(PRISM)\" rel=\"schema.prism\" href=\"http://prismstandard.org/namespaces/1.2/basic/\" />";

  private static final String footerHtml =
      " <div id=\"ftr\" class=\"footer\">" +
          " <div class=\"cope cope-bottom-align\">" +
          "<a href=\'http://publicationethics.org/\' title=\'Committee on Publication Ethics - external website.\'>" +
          "<img src=\'/aj/images/logo_cope.gif\' alt=\'\' />" +
          "</a>" +
          "<br/>" +
          "This journal is a member of and subscribes to the principles of the <a href=\'http://publicationethics.org/\' alt=\'\'>" +
          "Committee on Publication Ethics</a>" +
          ".</div>" +
          "</div>" +
          " <!-- end webtrends Version: 8.6.0 -->" +
          " </body>" +
          " </html>";
  private static final String footerHtmlFiltered =
          " </body>" +
          " </html>";
  
  public void testFiltering() throws Exception {
    InputStream inA;
    InputStream inB;
    
    /* impactFactor test */
    inA = fact.createFilteredInputStream(mau, new StringInputStream(breadcrumbHtmlHash),
        ENC);

    assertEquals(breadcrumbHtmlHashFiltered,StringUtil.fromInputStream(inA));

    /* whiteSpace test */
    inA = fact.createFilteredInputStream(mau, new StringInputStream(WhiteSpace1),
        ENC);
    
    inB = fact.createFilteredInputStream(mau, new StringInputStream(WhiteSpace2),
        ENC);

    assertEquals(StringUtil.fromInputStream(inA),StringUtil.fromInputStream(inB));
    
    //another whitespace test - is it really working?
    inA = fact.createFilteredInputStream(mau, new StringInputStream(whitespace_a),
        ENC);
    
    inB = fact.createFilteredInputStream(mau, new StringInputStream(whitespace_b),
        ENC);

    assertEquals(StringUtil.fromInputStream(inA),StringUtil.fromInputStream(inB));
    
    // test removing head section
    inA = fact.createFilteredInputStream(mau, new StringInputStream(headHtml),
        ENC);

    assertEquals(headHtmlFiltered,StringUtil.fromInputStream(inA));
    
    // test removing comments section
    inA = fact.createFilteredInputStream(mau, new StringInputStream(commentTags),
        ENC);

    assertEquals(commentTagsFiltered,StringUtil.fromInputStream(inA));
    
    // test removing footer section
    inA = fact.createFilteredInputStream(mau, new StringInputStream(footerHtml),
        ENC);

    assertEquals(footerHtmlFiltered,StringUtil.fromInputStream(inA));
    

  }
}




