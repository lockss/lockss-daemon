/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.michigan.deepblue;

import junit.framework.Test;
import org.lockss.plugin.FilterFactory;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.StringUtil;

import java.io.InputStream;

public class TestDeepBlueHtmlCrawlFilterFactory extends LockssTestCase {
  private static FilterFactory fact;
  private static MockArchivalUnit mau;
  
  //Example instances mostly from pages of strings of HTMl that should be filtered
  //and the expected post filter HTML strings.
  
  //Related articles
  private static final String HtmlTest1 =
            "<ul aria-labelledby=\"trail-dropdown-toggle\" role=\"menu\" class=\"dropdown-menu\">\n" +
            "   <li role=\"presentation\">\n" +
            "      <a role=\"menuitem\" href=\"/documents\"><i aria-hidden=\"true\" class=\"glyphicon glyphicon-home\"></i>&nbsp;\n" +
            "      Home</a>\n" +
            "   </li>\n" +
            "   <li role=\"presentation\">\n" +
            "      <a role=\"menuitem\" href=\"/handle/2027.42/13913\">Research Collections</a>\n" +
            "   </li>\n" +
            "   <li role=\"presentation\">\n" +
            "      <a role=\"menuitem\" href=\"/handle/2027.42/41251\">Paleontology, Museum of - Publications</a>\n" +
            "   </li>\n" +
            "   <li role=\"presentation\" class=\"disabled\">\n" +
            "      <a href=\"#\" role=\"menuitem\">View Item</a>\n" +
            "   </li>\n" +
            "</ul>" +
            "<ul class=\"breadcrumb hidden-xs\" style=\"max-width: 75%\">\n" +
            "<li>\n" +
            "   <i aria-hidden=\"true\" class=\"glyphicon glyphicon-home\"></i>&nbsp;\n" +
            "   <div style=\"display: inline; font-size: 18px; font-weight: normal;\">\n" +
            "      <a href=\"/documents\">Home</a>\n" +
            "   </div>\n" +
            "</li>\n" +
            "<li>\n" +
            "   <div style=\"display: inline; font-size: 18px; font-weight: normal;\">\n" +
            "      <a href=\"/handle/2027.42/13913\">Research Collections</a>\n" +
            "   </div>\n" +
            "</li>\n" +
            "<li>\n" +
            "   <div style=\"display: inline; font-size: 18px; font-weight: normal;\">\n" +
            "      <a href=\"/handle/2027.42/41251\">Paleontology, Museum of - Publications</a>\n" +
            "   </div>\n" +
            "</li>\n" +
            "<li class=\"active\">\n" +
            "   <div style=\"display: inline; font-size: 18px; font-weight: bold; font-family: var(--font-base-family);\">View Item</div>\n" +
            "</li>\n" +
            "</ul>";
  private static final String HtmlTest1Filtered = ""; //It should be empty

 public static class TestCrawl extends TestDeepBlueHtmlCrawlFilterFactory {
          
          public void setUp() throws Exception {
                  super.setUp();
                  fact = new DeepBlueHtmlCrawlFilterFactory();
          }

  }

public static Test suite() {
    return variantSuites(new Class[] {
        TestCrawl.class,
      });
  }
  
  public void testAlsoReadHtmlFiltering() throws Exception {
    InputStream actIn1 = fact.createFilteredInputStream(mau,
                        new StringInputStream(HtmlTest1),
                        Constants.DEFAULT_ENCODING);
    
    assertEquals(HtmlTest1Filtered, StringUtil.fromInputStream(actIn1));

  }
  
}
