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

package org.lockss.plugin.smartjournal.univsal;

import org.lockss.config.TdbAu;
import org.lockss.extractor.LinkExtractor;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests for SmartJournalVolumeIssueLinkExtractor.
 *
 * HTML structure confirmed from sdj.univsul.edu.iq/all-issues source:
 *
 * Each issue is a flat card (no accordion grouping):
 *   <div class="card-body">
 *     <h5 class="card-title mt-1 mb-0 fs-1-1">Vol. 12 No. 1 (2025) : SDJ</h5>
 *     ...
 *     <p class="mt-2 mb-0">
 *       <a href="/issue?id=25" class="btn btn-stealth-app-primary p-0">View issue</a>
 *     </p>
 *   </div>
 *
 * Special issue has no "No. X" in the title:
 *   <h5 class="card-title mt-1 mb-0 fs-1-1">Vol. 4 (2017) : SDJ</h5>
 */
public class TestSulaimaniDentalJournalsVolumeIssueLinkExtractor extends LockssTestCase {

    private static final String BASE_URL = "https://sdj.univsul.edu.iq/";
    private static final String ALL_ISSUES_URL = BASE_URL + "all-issues";
    private static final String ENCODING = "UTF-8";

    // -----------------------------------------------------------------------
    // Minimal HTML fragments — one card per issue, matching confirmed structure
    // -----------------------------------------------------------------------

    /** One card for a standard issue — used to build multi-issue HTML. */
    private static String issueCard(String titleText, String issueId) {
        return "<div class=\"mt-4 card p-3\">" +
               "  <div class=\"p-0 card-body d-flex flex-column\">" +
               "    <h5 class=\"card-title mt-1 mb-0 fs-1-1\">" + titleText + "</h5>" +
               "    <p class=\"card-text mt-2 mb-0 fs-0-7\">2025-01-01</p>" +
               "    <p class=\"mt-2 mb-0\">" +
               "      <a href=\"/issue?id=" + issueId + "\" " +
               "         class=\"btn btn-stealth-app-primary p-0\">View issue</a>" +
               "    </p>" +
               "  </div>" +
               "</div>";
    }

    /** Wraps card HTML in a minimal valid page. */
    private static String page(String... cards) {
        StringBuilder sb = new StringBuilder("<!DOCTYPE html><html><body>");
        for (String c : cards) sb.append(c);
        sb.append("</body></html>");
        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // Three Vol. 12 cards (ids 25/26/27) confirmed from all-issues page
    // -----------------------------------------------------------------------
    private static final String VOL12_HTML = page(
        issueCard("Vol. 12 No. 3 (2025) : SDJ", "27"),
        issueCard("Vol. 12 No. 2 (2025) : SDJ", "26"),
        issueCard("Vol. 12 No. 1 (2025) : SDJ", "25")
    );

    // -----------------------------------------------------------------------
    // Vol. 4 special issue — no "No. X" in the title (confirmed id=24)
    // -----------------------------------------------------------------------
    private static final String VOL4_SPECIAL_HTML = page(
        issueCard("Vol. 4 No. 1 (2017) : SDJ", "20"),
        issueCard("Vol. 4 (2017) : SDJ",        "24")
    );

    // -----------------------------------------------------------------------
    // Mixed page: Vol. 12 and Vol. 11 cards together
    // -----------------------------------------------------------------------
    private static final String MIXED_HTML = page(
        issueCard("Vol. 12 No. 1 (2025) : SDJ", "25"),
        issueCard("Vol. 11 No. 3 (2024) : SDJ", "19"),
        issueCard("Vol. 11 No. 2 (2024) : SDJ", "18"),
        issueCard("Vol. 11 No. 1 (2024) : SDJ", "17")
    );

    // -----------------------------------------------------------------------
    // Helper: run extractor with a given volume and HTML
    // -----------------------------------------------------------------------
    private List<String> extractLinks(String html, String srcUrl, String volumeId)
            throws Exception {

        TdbAu tdbAu = new TdbAu("Sulaimani Dental Journal Vol " + volumeId, "pluginId") {
            @Override
            public String getVolume() {
                return volumeId;
            }
        };

        MockArchivalUnit mau = new MockArchivalUnit();
        mau.setTdbAu(tdbAu);

        // Set volume in the AU configuration so the extractor can read it
        org.lockss.config.Configuration config =
                org.lockss.config.ConfigManager.newConfiguration();
        config.put("volume", volumeId);
        mau.setConfiguration(config);

        List<String> foundLinks = new ArrayList<>();
        LinkExtractor.Callback cb = foundLinks::add;

        SulaimaniDentalJournalsVolumeIssueLinkExtractor extractor =
                new SulaimaniDentalJournalsVolumeIssueLinkExtractor(null);

        InputStream in = new ByteArrayInputStream(
                html.getBytes(StandardCharsets.UTF_8));
        extractor.extractUrls(mau, in, ENCODING, srcUrl, cb);

        return foundLinks;
    }

    // -----------------------------------------------------------------------
    // Test 1: Standard multi-issue volume — all three Vol. 12 issues extracted
    // -----------------------------------------------------------------------
    public void testExtractsAllIssueLinksForMatchingVolume() throws Exception {
        List<String> links = extractLinks(VOL12_HTML, ALL_ISSUES_URL, "12");

        assertEquals("Expected exactly 3 issue links for Volume 12", 3, links.size());
        assertTrue("Should contain issue?id=25 (Vol. 12 No. 1)",
                links.contains(BASE_URL + "issue?id=25"));
        assertTrue("Should contain issue?id=26 (Vol. 12 No. 2)",
                links.contains(BASE_URL + "issue?id=26"));
        assertTrue("Should contain issue?id=27 (Vol. 12 No. 3)",
                links.contains(BASE_URL + "issue?id=27"));
    }

    // -----------------------------------------------------------------------
    // Test 2: Special issue — "Vol. 4 (2017)" with no "No. X" is matched
    // -----------------------------------------------------------------------
    public void testExtractsSpecialIssueWithNoIssueNumber() throws Exception {
        List<String> links = extractLinks(VOL4_SPECIAL_HTML, ALL_ISSUES_URL, "4");

        assertEquals("Expected 2 issue links for Volume 4 (regular + special)", 2, links.size());
        assertTrue("Should contain issue?id=20 (Vol. 4 No. 1)",
                links.contains(BASE_URL + "issue?id=20"));
        assertTrue("Should contain issue?id=24 (Vol. 4 Special Issue)",
                links.contains(BASE_URL + "issue?id=24"));
    }

    // -----------------------------------------------------------------------
    // Test 3: Non-matching volume — no links returned for wrong volume
    // -----------------------------------------------------------------------
    public void testReturnsNoLinksForNonMatchingVolume() throws Exception {
        List<String> links = extractLinks(MIXED_HTML, ALL_ISSUES_URL, "99");

        assertEquals("Expected no links when volume does not match any card", 0, links.size());
    }

    // -----------------------------------------------------------------------
    // Test 4: Mixed page — only target volume links returned, not neighbours
    // -----------------------------------------------------------------------
    public void testDoesNotExtractLinksFromAdjacentVolumes() throws Exception {
        List<String> links = extractLinks(MIXED_HTML, ALL_ISSUES_URL, "12");

        assertEquals("Expected exactly 1 link for Volume 12 from mixed page", 1, links.size());
        assertTrue("Should contain issue?id=25 (Vol. 12 No. 1)",
                links.contains(BASE_URL + "issue?id=25"));
        assertFalse("Should NOT contain Vol. 11 issue?id=19",
                links.contains(BASE_URL + "issue?id=19"));
        assertFalse("Should NOT contain Vol. 11 issue?id=18",
                links.contains(BASE_URL + "issue?id=18"));
        assertFalse("Should NOT contain Vol. 11 issue?id=17",
                links.contains(BASE_URL + "issue?id=17"));
    }

    // -----------------------------------------------------------------------
    // Test 5: Volume boundary — "Vol. 1" must NOT match "Vol. 12" or "Vol. 13"
    //         (word boundary \b guard in VOLUME_PATTERN)
    // -----------------------------------------------------------------------
    public void testVolumeOneDoesNotMatchVolumeTwelveOrThirteen() throws Exception {
        String html = page(
            issueCard("Vol. 12 No. 1 (2025) : SDJ", "25"),
            issueCard("Vol. 13 No. 1 (2026) : SDJ", "28"),
            issueCard("Vol. 1 No. 1 (2014) : SDJ",  "1")
        );

        List<String> links = extractLinks(html, ALL_ISSUES_URL, "1");

        assertEquals("Vol. 1 should match only its own card, not Vol. 12 or Vol. 13", 1, links.size());
        assertTrue("Should contain issue?id=1 (Vol. 1 No. 1)",
                links.contains(BASE_URL + "issue?id=1"));
        assertFalse("Should NOT contain Vol. 12 issue?id=25",
                links.contains(BASE_URL + "issue?id=25"));
        assertFalse("Should NOT contain Vol. 13 issue?id=28",
                links.contains(BASE_URL + "issue?id=28"));
    }

    // -----------------------------------------------------------------------
    // Test 6: Non-all-issues URL — extractor does not process it
    //         (falls through to default extractor, which is null here)
    // -----------------------------------------------------------------------
    public void testDoesNotProcessNonAllIssuesUrl() throws Exception {
        List<String> links = extractLinks(VOL12_HTML,
                BASE_URL + "some-other-page", "12");

        assertEquals("Should return no links when srcUrl is not the all-issues page",
                0, links.size());
    }
}
