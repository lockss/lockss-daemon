/*

Copyright (c) 2000-2020, Board of Trustees of Leland Stanford Jr. University
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

package org.lockss.plugin.respediatrica;

import junit.framework.Test;
import org.lockss.plugin.FilterFactory;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.StringUtil;

import java.io.InputStream;

public class TestRespediatricaHtmlFilterFactory extends LockssTestCase {
    private static FilterFactory fact;
    private static MockArchivalUnit mau;

    private static final String HtmlTest1 =
            "<div class=\"wr-main-menu\">\n" +
                    "                     <ul>\n" +
                    "                        <li class=\"\">\n" +
                    "                           <a href=\"/\">Home</a>\n" +
                    "                        </li>\n" +
                    "                        <li class=\" \">\n" +
                    "                           <a href=\"#\">About RP</a>\n" +
                    "                           <ul class=\"sub-menu\">\n" +
                    "                              <li><a href=\"/sobre-rp\">Who we are</a></li>\n" +
                    "                              <li><a href=\"/corpo-editorial\">Editorial Board</a></li>\n" +
                    "                              <li><a href=\"/diretoria\">SBP</a></li>\n" +
                    "                           </ul>\n" +
                    "                        </li>\n" +
                    "                        <li class=\"   \">\n" +
                    "                           <a href=\"#\">Issues</a>\n" +
                    "                           <ul class=\"sub-menu\">\n" +
                    "                              <li><a href=\"/sumario\">Current Issue</a></li>\n" +
                    "                              <li><a href=\"/numeros-anteriores\">Past Issues</a></li>\n" +
                    "                              <li><a href=\"/relatorio-de-audiencia\">Read More</a></li>\n" +
                    "                           </ul>\n" +
                    "                        </li>\n" +
                    "                        <li class=\"  \">\n" +
                    "                           <a href=\"#\">Instructions</a>\n" +
                    "                           <ul class=\"sub-menu\">\n" +
                    "                              <li><a href=\"/instrucoes-aos-autores\">Instructions to Authors</a></li>\n" +
                    "                              <li><a href=\"/instrucoes-aos-revisores\">Instructions to Reviewers</a></li>\n" +
                    "                              <li><a href=\"/instrucoes-aos-leitores\">Instructions to Readers</a></li>\n" +
                    "                           </ul>\n" +
                    "                        </li>\n" +
                    "                        <li><a href=\"https://www.gnpapers.com.br/rp/default.asp?lang=en\" target=\"_blank\">Submission</a></li>\n" +
                    "                        <li class=\"\">\n" +
                    "                           <a href=\"#\">Media</a>\n" +
                    "                           <ul class=\"sub-menu\">\n" +
                    "                              <li><a href=\"/video\">Videos archive – 2011/2017</a></li>\n" +
                    "                              <li><a href=\"/midia/Podcast\">Podcast</a></li>\n" +
                    "                           </ul>\n" +
                    "                        </li>\n" +
                    "                        <li class=\"\">\n" +
                    "                           <a href=\"#\">Contact</a>\n" +
                    "                           <ul class=\"sub-menu\">\n" +
                    "                              <li><a href=\"/contato\">Contact us</a></li>\n" +
                    "                              <li><a href=\"/assinar-newsletter\">Newsletter</a></li>\n" +
                    "                           </ul>\n" +
                    "                        </li>\n" +
                    "                        <li class=\"\">\n" +
                    "                           <a href=\"/busca-avancada\">Search</a>\n" +
                    "                        </li>\n" +
                    "                     </ul>\n" +
                    "                  </div>" +
                    "<section class=\"front\">\n" +
                    "    <div class=\"clearfix\"></div>\n" +
                    "    <h3>\n" +
                    "        <strong>\n" +
                    "            Medical Ethics -\n" +
                    "            Year 2011 -\n" +
                    "            Volume 1 -\n" +
                    "            Issue<span> </span>\n" +
                    "            1\n" +
                    "        </strong>\n" +
                    "    </h3>\n" +
                    "    <h1>Article title</h1>\n" +
                    "    <h2 class=\"item\">\n" +
                    "        h2 content\n" +
                    "    </h2>\n" +
                    "    <p class=\"article-author\">author</p>\n" +
                    "</section>\n" +
                    "<section style=\"text-align:justify\">\n" +
                    "    <article>\n" +
                    "    </article>\n" +
                    "    <article>\n" +
                    "    </article>\n" +
                    "    <article>\n" +
                    "    </article>\n" +
                    "</section>\n" +
                    "<section class=\"body\" style=\"text-align:justify\">\n" +
                    "    <section>\n" +
                    "        main content\n" +
                    "    <p>&nbsp;</p>\n" +
                    "    <p></p>\n" +
                    "</section>\n";

    private static final String HtmlTest1Filtered = "" +
            "<section class=\"front\">\n" +
            "    <div class=\"clearfix\"></div>\n" +
            "    <h3>\n" +
            "        <strong>\n" +
            "            Medical Ethics -\n" +
            "            Year 2011 -\n" +
            "            Volume 1 -\n" +
            "            Issue<span> </span>\n" +
            "            1\n" +
            "        </strong>\n" +
            "    </h3>\n" +
            "    <h1>Article title</h1>\n" +
            "    <h2 class=\"item\">\n" +
            "        h2 content\n" +
            "    </h2>\n" +
            "    <p class=\"article-author\">author</p>\n" +
            "</section>\n" +
            "<section style=\"text-align:justify\">\n" +
            "    <article>\n" +
            "    </article>\n" +
            "    <article>\n" +
            "    </article>\n" +
            "    <article>\n" +
            "    </article>\n" +
            "</section>\n" +
            "<section class=\"body\" style=\"text-align:justify\">\n" +
            "    <section>\n" +
            "        main content\n" +
            "    <p>&nbsp;</p>\n" +
            "    <p></p>\n" +
            "</section>\n";

    //Variant to test with Crawl Filter
    public static class TestCrawl extends TestRespediatricaHtmlFilterFactory {

        public void setUp() throws Exception {
            super.setUp();
            fact = new ResPediatricaHtmlFilterFactory();
        }

    }


    public static Test suite() {
        return variantSuites(new Class[] {
                TestRespediatricaHtmlFilterFactory.TestCrawl.class,
        });
    }

    public void testAlsoReadHtmlFiltering() throws Exception {
        InputStream actIn1 = fact.createFilteredInputStream(mau,
                new StringInputStream(HtmlTest1),
                Constants.DEFAULT_ENCODING);
        
        String filteredStr = StringUtil.fromInputStream(actIn1);
        
        assertEquals(HtmlTest1Filtered, filteredStr);
    }
}

