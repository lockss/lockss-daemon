/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.ubiquitypress.upn;

import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.StringUtil;
import java.io.InputStream;

public class TestUbiquityPartnerNetworkBookHtmlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private UbiquityPartnerNetworkBookHtmlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new UbiquityPartnerNetworkBookHtmlFilterFactory();
    mau = new MockArchivalUnit();
  }
  private static final String aritlePage = "<div class=\"table-of-contents\">table-of-contents</div>\n" +
          "<div class=\"disciplines\">disciplines</div>\n" +
          "<div class=\"keywords\">keywords</div>\n" +
          "<div class=\"metrics-block\">metrics-block</div>\n" +
          "<div class=\"metrics\">metrics</div>\n" +
          "<div class=\"article\">main content</div>\n" +
          "<div id=\"thread__wrapper\">thread__wrapper</div>\n" +
          "<div class=\"read-list\">read-list</div>\n" +
          "<section class=\"brand-header\">brand-header</section>\n" +
          "<section class=\"book-cover\">book-cover</section>\n" +
          "<section class=\"how-tos\">how-tos</section>\n" +
          "<section class=\"comments\">comments </section>";

  private static final String aritlePageFiltered =
          "\n" +
          "\n" +
          "\n" +
          "\n" +
          "\n" +
          "<div class=\"article\">main content</div>" +
          "\n" +
          "\n" +
          "\n" +
          "\n" +
          "\n" +
          "\n";


  public void testArticlePage() throws Exception {
    InputStream inStream;
    inStream = fact.createFilteredInputStream(mau,
        new StringInputStream(aritlePage),
        Constants.DEFAULT_ENCODING);

    String filteredStr = StringUtil.fromInputStream(inStream);
    
    assertEquals(aritlePageFiltered, filteredStr);
  }

}
