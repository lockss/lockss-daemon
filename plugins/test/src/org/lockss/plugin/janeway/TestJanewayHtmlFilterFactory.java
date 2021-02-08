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

package org.lockss.plugin.janeway;

import junit.framework.Test;
import org.lockss.plugin.FilterFactory;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.StringUtil;

import java.io.InputStream;

public class TestJanewayHtmlFilterFactory extends LockssTestCase {
    private static FilterFactory fact;
    private static MockArchivalUnit mau;

    private static final String HtmlTest1 =
            "<div class=\"summary\">summary content</div>\n" +
            "<div class=\"sticky\">sticky content</div>\n" +
            "<div id=\"options-menu\">options menu content</div>\n" +
            "<div class=\"main content\">main content</div>\n" +
            "<ul class=\"menu\"><li>menu- 1 content </li><ul>";

    private static final String HtmlTest1Filtered = "" +
            "\n" +
            "\n" +
            "\n" +
            "<div class=\"main content\">main content</div>\n" +
            "";

    //Variant to test with Crawl Filter
    public static class TestCrawl extends TestJanewayHtmlFilterFactory {

        public void setUp() throws Exception {
            super.setUp();
            fact = new JanewayHtmlFilterFactory();
        }

    }


    public static Test suite() {
        return variantSuites(new Class[] {
                TestJanewayHtmlFilterFactory.TestCrawl.class,
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

