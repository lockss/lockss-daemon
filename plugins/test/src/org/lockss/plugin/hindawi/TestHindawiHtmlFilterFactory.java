/*
 * $Id$
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.util.*;
import org.lockss.test.*;

public class TestHindawiHtmlFilterFactory extends LockssTestCase {
  
  private HindawiHtmlFilterFactory fact;
  
  public void setUp() throws Exception {
    super.setUp();
    fact = new HindawiHtmlFilterFactory();
  }
  
  private static final String[] DOCTYPE_STATEMENTS = {
    "<!DOCTYPE html>",
    "<!DOCTYPE html PUBLIC \"-//W3C//DTD MathML 2.0//EN\" \"http://www.w3.org/Math/DTD/mathml2/mathml2.dtd\">",
    "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1 plus MathML 2.0//EN\" \"http://www.w3.org/TR/MathML2/dtd/xhtml-math11-f.dtd\"[<!ENTITY mathml 'http://www.w3.org/1998/Math/MathML'>]>",
  };
  
  private static final String[] HTML_TAGS = {
    "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
    "<html xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:mml=\"http://www.w3.org/1998/Math/MathML\">",
    "<html xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:xlink=\"http://www.w3.org/1999/xlink\">",
  };
  
  private static final String[] PRE_TAGS = {
    "<pre>Journal of Foo<br />Volume 1 (2001), Issue 2, Pages 33-44<br /><a href=\"http://dx.doi.org/10.1155/2001/123456\">http://dx.doi.org/10.1155/2001/123456</a></pre>",
    "<pre>Journal of Foo<br />Volume 1 (2001), Issue 2, Pages 33-44<br />doi:10.1155/2001/123456</pre>",
  };
  
  private static final String[] LICENSE_STATEMENTS = {
    "<p>Copyright &copy; 2001 Author N. One et al. This is an open access article distributed under the <a rel=\"license\" href=\"http://creativecommons.org/licenses/by/3.0/\">Creative Commons Attribution License</a>, which permits unrestricted use, distribution, and reproduction in any medium, provided the original work is properly cited.</p>",
    "<p>Copyright &copy; 2001 Author N. One et al. This is an open access article distributed under the <a rel=\"license\" href=\"http://creativecommons.org/licenses/by/3.0/\">Creative Commons Attribution License</a>, which permits unrestricted use, distribution, and reproduction in any medium, provided the original work is properly cited. </p>",
  };
  
  private static final String[] XML_CONTENTS_TAGS = {
    "<div class=\"xml-content\">",
    "<div id=\"divXMLContent\" class=\"xml-content\">",
    "<div id=\"ctl00_ContentPlaceHolder1_divXMLContent\" class=\"xml-content\">",
  };
  
  private static final String[] SVG_TAGS = {
    "<svg style=\"vertical-align:-0.10033pt;width:35.099998px;\" id=\"M1\" height=\"10.8125\" version=\"1.1\" viewBox=\"0 0 35.099998 10.8125\" width=\"35.099998\" xmlns=\"http://www.w3.org/2000/svg\"><g transform=\"matrix(1.25,0,0,-1.25,0,10.8125)\"><g transform=\"translate(72,-63.35)\"><text transform=\"matrix(1,0,0,-1,-71.95,63.5)\"><tspan style=\"font-size: 12.50px; \" x=\"0\" y=\"0\">n</tspan><tspan style=\"font-size: 12.50px; \" x=\"9.6773224\" y=\"0\">=</tspan><tspan style=\"font-size: 12.50px; \" x=\"21.71771\" y=\"0\">2</tspan></text></g></g></svg>",
    "<svg style=\"vertical-align:-0.1638pt;width:36.237499px;\" id=\"M1\" height=\"11.125\" version=\"1.1\" viewBox=\"0 0 36.237499 11.125\" width=\"36.237499\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns=\"http://www.w3.org/2000/svg\"><g transform=\"matrix(.017,-0,0,-.017,.062,10.862)\"><path id=\"x1D45B\" d=\"M495 86q-46 -47 -87 -72.5t-63 -25.5q-43 0 -16 107l49 210q7 34 8 50.5t-3 21t-13 4.5q-35 0 -109.5 -72.5t-115.5 -140.5q-21 -75 -38 -159q-50 -10 -76 -21l-6 8l84 340q8 35 -4 35q-17 0 -67 -46l-15 26q44 44 85.5 70.5t64.5 26.5q35 0 10 -103l-24 -98h2 q42 56 97 103.5t96 71.5q46 26 74 26q9 0 16 -2.5t14 -11.5t9.5 -24.5t-1 -44t-13.5 -68.5q-30 -117 -47 -200q-4 -19 -3.5 -25t6.5 -6q21 0 70 48z\"/></g><g transform=\"matrix(.017,-0,0,-.017,13.305,10.862)\"><path id=\"x3D\" d=\"M535 323h-483v50h483v-50zM535 138h-483v50h483v-50z\"/></g><g transform=\"matrix(.017,-0,0,-.017,28.008,10.862)\"><path id=\"x32\" d=\"M412 140l28 -9q0 -2 -35 -131h-373v23q112 112 161 170q59 70 92 127t33 115q0 63 -31 98t-86 35q-75 0 -137 -93l-22 20l57 81q55 59 135 59q69 0 118.5 -46.5t49.5 -122.5q0 -62 -29.5 -114t-102.5 -130l-141 -149h186q42 0 58.5 10.5t38.5 56.5z\"/></g></svg>",
  };
  
  private static final String PAGE_TEMPLATE =
      "@DOCTYPE_STATEMENT\n" +
      "@HTML_TAG\n" +
      "<head>\n" +
      "  <meta charset=\"UTF-8\" />\n" +
      "  <title>!!!title!!!</title>\n" +
      "  <link href=\"/stylesheet1.css\" rel=\"stylesheet\" type=\"text/css\" />\n" +
      "  <meta name=\"key1\" content=\"value1\"/>\n" + 
      "  <script type=\"text/javascript\" src=\"/script1.js\" />\n" + 
      "  <script type=\"text/javascript\">!!!javascript!!!</script>\n" + 
      "</head>\n" +
      "<body>\n" +
      "  <div id=\"container\">\n" +
      "    <div id=\"site_head\">!!!site_head!!!</div>\n" +
      "    <div id=\"dvLinks\" class=\"hindawi_links\">!!!dvLinks!!!</div>\n" +
      "    <div id=\"ctl00_dvLinks\" class=\"hindawi_links\">!!!ctl00_dvLinks!!!</div>\n" +
      "    <div id=\"banner\">!!!banner!!!</div>\n" +
      "    <div id=\"journal_navigation\">!!!journal_navigation!!!</div>\n" +
      "    <div id=\"content\">\n" +
      "      <div id=\"left_column\">!!!left_column!!!</div>\n" +
      "      <div id=\"middle_content\">\n" + 
      "        <div class=\"right_column_actions\">!!!right_column_actions!!!</div>" +
      "        <div>\n" + 
      "          @PRE_TAG\n" + 
      "          <div class=\"article_type\">Research Article</div>\n" +
      "          <h2>!!!h2!!!</h2>\n" +
      "          <div class=\"author_gp\">!!!author_gp!!!</div>\n" +
      "          <p>!!!_author_affiliations!!!</p>\n" +
      "          <p>Received 1 January 2001; Accepted 15 January 2001</p>\n" +
      "          <p>Academic Editor: John Q. Smith </p>\n" +
      "          <div class=\"xml-content\">@LICENSE_STATEMENT</div>\n" +
      "          @XML_CONTENT_TAG!!!_article_contents_1!!!@SVG_TAG!!!_article_contents_2!!!</div>\n" +
      "        </div>\n" +
      "      </div>\n" +
      "    </div>\n" +
      "    <div class=\"footer_space\">!!!footer_space!!!</div>\n" +
      "  </div>\n" +
      "  <div id=\"footer\">!!!footer!!!</div>\n" +
      "</body>\n" +
      "</html>\n";
  
  private static final String RESULT = " Journal of FooVolume 1 (2001), Issue 2, Pages 33-44 Research Article !!!h2!!! !!!author_gp!!! !!!_author_affiliations!!! Received 1 January 2001; Accepted 15 January 2001 Academic Editor: John Q. Smith Copyright &copy; 2001 Author N. One et al. This is an open access article distributed under the Creative Commons Attribution License, which permits unrestricted use, distribution, and reproduction in any medium, provided the original work is properly cited. @XML_CONTENT_TAG!!!_article_contents_1!!!!!!_article_contents_2!!! ";

  public void testFilterWithTemplate() throws Exception {
    for (String doctypeStatement : DOCTYPE_STATEMENTS) {
      for (String htmlTag : HTML_TAGS) {
        for (String preTag : PRE_TAGS) {
          for (String licenseStatement : LICENSE_STATEMENTS) {
            for (String xmlContentsTag : XML_CONTENTS_TAGS) {
              for (String svgTag : SVG_TAGS) {
                String input = PAGE_TEMPLATE.replaceAll("@DOCTYPE_STATEMENT", doctypeStatement)
                                            .replaceAll("@HTML_TAG", htmlTag)
                                            .replaceAll("@PRE_TAG", preTag)
                                            .replaceAll("@LICENSE_STATEMENT", licenseStatement)
                                            .replaceAll("@XML_CONTENTS_TAG", xmlContentsTag)
                                            .replaceAll("@SVG_TAG", svgTag);
                InputStream actIn = fact.createFilteredInputStream(null,
                                                                   new StringInputStream(input),
                                                                   Constants.DEFAULT_ENCODING);
                assertEquals(RESULT, StringUtil.fromInputStream(actIn));
              }
            }
          }
        }
      }
    }
    
  }

}

