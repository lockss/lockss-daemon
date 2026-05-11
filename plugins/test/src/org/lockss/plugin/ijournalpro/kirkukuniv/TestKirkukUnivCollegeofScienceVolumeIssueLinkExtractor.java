/*

Copyright (c) 2000-2026, Board of Trustees of Leland Stanford Jr. University

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
package org.lockss.plugin.ijournalpro.kirkukuniv;

import org.lockss.extractor.LinkExtractor;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.MockLockssDaemon;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.lockss.config.Tdb;
import org.lockss.config.TdbAu;
import org.lockss.config.TdbPublisher;
import org.lockss.config.TdbTitle;


public class TestKirkukUnivCollegeofScienceVolumeIssueLinkExtractor extends LockssTestCase {

    private static final String BASE_URL = "https://kirkukjournal.com/";
    private static final String ARCHIVE_URL = BASE_URL + "archive.html";
    private static final String ENCODING = "UTF-8";

    // HTML taken from the comment block in the extractor class
    private static final String ARCHIVE_HTML = "<!DOCTYPE html><html><body>" +
            "<div class=\"accordion mb-2\" id=\"\">" +
            "  <div class=\"accordion-item\">" +
            "    <h2 class=\"accordion-header\" id=\"pVol_15176\">" +
            "      <button class=\"accordion-button bg-body collapsed text-primary\" type=\"button\" " +
            "              data-bs-toggle=\"collapse\" data-bs-target=\"#pVol_15176_\" " +
            "              aria-expanded=\"true\" aria-controls=\"pVol_15176_\">" +
            "        <b><i class=\"fa-regular fa-file-lines me-2\"></i>Volume 20 (2025)</b>" +
            "      </button>" +
            "    </h2>" +
            "    <div id=\"pVol_15176_\" class=\"accordion-collapse collapse\" aria-labelledby=\"pVol_15176\">" +
            "      <div class=\"accordion-body p-2 row\">" +
            "        <div class=\"col-md-3 col-lg-3\">" +
            "          <div>" +
            "            <a class=\"js_click pointer_cursor\" data-handler=\"loadModal\" " +
            "               data-param_a=\"Kirkuk Journal of Science\" " +
            "               data-param_b=\"./data/kscis/coversheet/cover_en.jpg\">" +
            "              <img src=\"data/kscis/coversheet/cover_en.jpg\" alt=\"Kirkuk Journal of Science\" class=\"col-12 shadow-sm\"/>" +
            "            </a>" +
            "          </div>" +
            "          <div>" +
            "            <h5 class=\"text-center mt-3\"><a href=\"issue_15176_15451.html\">Issue 4</a></h5>" +
            "          </div>" +
            "        </div>" +
            "        <div class=\"col-md-3 col-lg-3\">" +
            "          <div>" +
            "            <a class=\"js_click pointer_cursor\" data-handler=\"loadModal\" " +
            "               data-param_a=\"Kirkuk Journal of Science\" " +
            "               data-param_b=\"./data/kscis/coversheet/cover_en.jpg\">" +
            "              <img src=\"data/kscis/coversheet/cover_en.jpg\" alt=\"Kirkuk Journal of Science\" class=\"col-12 shadow-sm\"/>" +
            "            </a>" +
            "          </div>" +
            "          <div>" +
            "            <h5 class=\"text-center mt-3\"><a href=\"issue_15176_15403.html\">Issue 3</a></h5>" +
            "          </div>" +
            "        </div>" +
            "        <div class=\"col-md-3 col-lg-3\">" +
            "          <div>" +
            "            <a class=\"js_click pointer_cursor\" data-handler=\"loadModal\" " +
            "               data-param_a=\"Kirkuk Journal of Science\" " +
            "               data-param_b=\"./data/kscis/coversheet/cover_en.jpg\">" +
            "              <img src=\"data/kscis/coversheet/cover_en.jpg\" alt=\"Kirkuk Journal of Science\" class=\"col-12 shadow-sm\"/>" +
            "            </a>" +
            "          </div>" +
            "          <div>" +
            "            <h5 class=\"text-center mt-3\"><a href=\"issue_15176_15376.html\">Issue 2</a></h5>" +
            "          </div>" +
            "        </div>" +
            "        <div class=\"col-md-3 col-lg-3\">" +
            "          <div>" +
            "            <a class=\"js_click pointer_cursor\" data-handler=\"loadModal\" " +
            "               data-param_a=\"Kirkuk Journal of Science\" " +
            "               data-param_b=\"./data/kscis/coversheet/cover_en.jpg\">" +
            "              <img src=\"data/kscis/coversheet/cover_en.jpg\" alt=\"Kirkuk Journal of Science\" class=\"col-12 shadow-sm\"/>" +
            "            </a>" +
            "          </div>" +
            "          <div>" +
            "            <h5 class=\"text-center mt-3\"><a href=\"issue_15176_15177.html\">Issue 1</a></h5>" +
            "          </div>" +
            "        </div>" +
            "      </div>" +
            "    </div>" +
            "  </div>" +
            "</div>" +
            "</body></html>";

    private List<String> extractLinks(String html, String srcUrl)
            throws Exception {

        TdbAu tdbAu = new TdbAu("Kirkuk Journal of Science Vol 20", "pluginId") {
            @Override
            public String getVolume() {
                return "20";
            }
        };

        MockArchivalUnit mau = new MockArchivalUnit();
        mau.setTdbAu(tdbAu);

        List<String> foundLinks = new ArrayList<>();
        LinkExtractor.Callback cb = foundLinks::add;

        KirkukUnivCollegeofScienceVolumeIssueLinkExtractor extractor =
                new KirkukUnivCollegeofScienceVolumeIssueLinkExtractor(null);

        InputStream in = new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8));
        extractor.extractUrls(mau, in, ENCODING, srcUrl, cb);

        return foundLinks;
    }

    public void testExtractsAllIssueLinksForMatchingVolume() throws Exception {
        List<String> links = extractLinks(ARCHIVE_HTML, ARCHIVE_URL);

        assertEquals("Expected exactly 4 issue links for Volume 20", 4, links.size());
        assertTrue(links.contains(BASE_URL + "issue_15176_15451.html")); // Issue 4
        assertTrue(links.contains(BASE_URL + "issue_15176_15403.html")); // Issue 3
        assertTrue(links.contains(BASE_URL + "issue_15176_15376.html")); // Issue 2
        assertTrue(links.contains(BASE_URL + "issue_15176_15177.html")); // Issue 1
    }
}