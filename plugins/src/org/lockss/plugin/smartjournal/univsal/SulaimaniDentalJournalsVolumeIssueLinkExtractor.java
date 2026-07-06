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

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.LinkExtractor;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SulaimaniDentalJournalsVolumeIssueLinkExtractor implements LinkExtractor {

    private static final Logger log =
            Logger.getLogger(SulaimaniDentalJournalsVolumeIssueLinkExtractor.class);

    // Matches both "Vol. 12 No. 1 (2025)" and "Vol. 4 (2017)" (special issue).
    // Word boundary \\b prevents "Vol. 1" from matching "Vol. 12", "Vol. 13" etc.
    private static final Pattern VOLUME_PATTERN =
            Pattern.compile("Vol\\.\\s+(\\d+)\\b");

    private final LinkExtractor defaultExtractor;

    public SulaimaniDentalJournalsVolumeIssueLinkExtractor(LinkExtractor defaultExtractor) {
        this.defaultExtractor = defaultExtractor;
    }

    /*
    <div class="mt-4 card p-3">
       <div class="p-0 card-body d-flex flex-column flex-xl-row align-items-center align-items-xl-start gap-4">
          <div class="mx-app-xl-unset w-100 journal-cover" style="max-width: 10.5rem">
             <img src="/SmartJournal_Uploads/1/issue/v4special250317050203.jpg" onerror="this.src='/assets/image/main/journal-cover.jpeg'" alt="hero image"
                class="w-100 rounded-3"/>
          </div>
          <div class="flex-grow-1">
             <h5 class="card-title mt-1 mb-0 fs-1-1">Vol. 4 (2017) : SDJ
             </h5>
             <p class="card-text mt-2 mb-0 fs-0-7">2017-04-01
             </p>
             <p class="card-text my-0 fs-0-7">Articles No. 1
             </p>
             <p class="card-text mt-2 mb-0 fs-1-0 line-clamp-3" >
             <p>Special Issue</p>
             </p>
             <div class="mt-3 d-flex flex-wrap align-items-center gap-2">
                <a href="/SmartJournal_Uploads/1/issueGalley/Conference_papers-1-6250507074249.pdf" download="Conference_papers-1-6.pdf" onclick="updateIssueDownloadCount(24)">
                <button class="btn btn-app-secondary btn-sm rounded-3">Cover Page to TOC</button>
                </a>
                <a href="/SmartJournal_Uploads/1/issueGalley/Conference_papers250507074336.pdf" download="Conference_papers.pdf" onclick="updateIssueDownloadCount(24)">
                <button class="btn btn-app-secondary btn-sm rounded-3">Volume 4 Special Issue</button>
                </a>
             </div>
             <p class="mt-2 mb-0"><a href="/issue?id=24"
                class="btn btn-stealth-app-primary p-0">View issue</a></p>
          </div>
       </div>
    </div>
    <hr class="hr-gradient my-4" data-content="All Issues"/>
    <div class="mt-4 card p-3">
       <div class="p-0 card-body d-flex flex-column flex-xl-row align-items-center align-items-xl-start gap-4">
          <div class="mx-app-xl-unset w-100 journal-cover" style="max-width: 10.5rem">
             <img src="/SmartJournal_Uploads/1/issue/v3i2250317052659.jpg" onerror="this.src='/assets/image/main/journal-cover.jpeg'" alt="hero image"
                class="w-100 rounded-3"/>
          </div>
          <div class="flex-grow-1">
             <h5 class="card-title mt-1 mb-0 fs-1-1">Vol. 3 No. 2 (2016) : SDJ
             </h5>
             <p class="card-text mt-2 mb-0 fs-0-7">2016-12-01
             </p>
             <p class="card-text my-0 fs-0-7">Articles No. 5
             </p>
             <p class="card-text mt-2 mb-0 fs-1-0 line-clamp-3" >
             </p>
             <div class="mt-3 d-flex flex-wrap align-items-center gap-2">
                <a href="/SmartJournal_Uploads/1/issueGalley/Cover_to_TOC250509065033.pdf" download="Cover to TOC.pdf" onclick="updateIssueDownloadCount(6)">
                <button class="btn btn-app-secondary btn-sm rounded-3">Cover page to TOC</button>
                </a>
                <a href="/SmartJournal_Uploads/1/issueGalley/SDJ_V3_I2250509065055.pdf" download="SDJ V3 I2.pdf" onclick="updateIssueDownloadCount(6)">
                <button class="btn btn-app-secondary btn-sm rounded-3">Volume 3 Issue 2 (full issue)</button>
                </a>
             </div>
             <p class="mt-2 mb-0"><a href="/issue?id=6"
                class="btn btn-stealth-app-primary p-0">View issue</a></p>
          </div>
       </div>
    </div>
    <hr class="hr-gradient my-4" data-content="All Issues"/>
    <div class="mt-4 card p-3">
       <div class="p-0 card-body d-flex flex-column flex-xl-row align-items-center align-items-xl-start gap-4">
          <div class="mx-app-xl-unset w-100 journal-cover" style="max-width: 10.5rem">
             <img src="/SmartJournal_Uploads/1/issue/v3i1250317051508.jpg" onerror="this.src='/assets/image/main/journal-cover.jpeg'" alt="hero image"
                class="w-100 rounded-3"/>
          </div>
          <div class="flex-grow-1">
             <h5 class="card-title mt-1 mb-0 fs-1-1">Vol. 3 No. 1 (2016) : SDJ
             </h5>
             <p class="card-text mt-2 mb-0 fs-0-7">2016-07-01
             </p>
             <p class="card-text my-0 fs-0-7">Articles No. 9
             </p>
             <p class="card-text mt-2 mb-0 fs-1-0 line-clamp-3" >
             </p>
             <div class="mt-3 d-flex flex-wrap align-items-center gap-2">
                <a href="/SmartJournal_Uploads/1/issueGalley/Cover_to_TOC250509115955.pdf" download="Cover to TOC.pdf" onclick="updateIssueDownloadCount(5)">
                <button class="btn btn-app-secondary btn-sm rounded-3">Cover page to TOC</button>
                </a>
                <a href="/SmartJournal_Uploads/1/issueGalley/SDJ_V3_I1250509120026.pdf" download="SDJ V3 I1.pdf" onclick="updateIssueDownloadCount(5)">
                <button class="btn btn-app-secondary btn-sm rounded-3">Volume 3 Issue 1 (full issue)</button>
                </a>
             </div>
             <p class="mt-2 mb-0"><a href="/issue?id=5"
                class="btn btn-stealth-app-primary p-0">View issue</a></p>
          </div>
       </div>
    </div>
    */

    @Override
    public void extractUrls(ArchivalUnit au, InputStream in, String encoding,
                            String srcUrl, Callback cb)
            throws IOException, PluginException {

        byte[] content = IOUtils.toByteArray(in);

        if (au.getTdbAu() == null) {
            log.debug2("No TdbAu available — cannot determine target volume");
            return;
        }

        if (!srcUrl.contains("all-issues")) {
            // Not the all-issues page — fall through to default extractor
            if (defaultExtractor != null) {
                defaultExtractor.extractUrls(au,
                        new ByteArrayInputStream(content),
                        encoding, srcUrl, cb);
            }
            return;
        }

        // volume is the definitional param carrying the expected volume number
        String targetVolumeId = au.getConfiguration().get("volume");
        if (targetVolumeId == null) {
            log.warning("volume param not found in AU configuration");
            return;
        }

        log.debug2("Extracting issue URLs for volume=" + targetVolumeId
                + " from " + srcUrl);

        Document doc = Jsoup.parse(
                new ByteArrayInputStream(content), encoding, srcUrl);

        // Each issue is a card; find all card bodies
        // Structure: div.card > div.card-body > h5.card-title (volume label)
        //                                     > p.mt-2 > a (View issue link)
        Elements cards = doc.select("div.card-body");
        int found = 0;

        for (Element card : cards) {

            Element titleEl = card.selectFirst("h5.card-title");
            if (titleEl == null) continue;

            String titleText = titleEl.text().trim();
            Matcher m = VOLUME_PATTERN.matcher(titleText);

            if (!m.find()) continue;

            String pageVolume = m.group(1);
            if (!pageVolume.equals(targetVolumeId)) continue;

            // Found a card matching the target volume — get the "View issue" link
            // It is always in: <p class="mt-2 mb-0"><a href="/issue?id=N">View issue</a>
            Element viewIssueLink = card.selectFirst("p.mt-2 a[href*='issue?id=']");
            if (viewIssueLink == null) {
                log.debug2("No 'View issue' link found in card for: " + titleText);
                continue;
            }

            String issueUrl = viewIssueLink.absUrl("href");
            if (!issueUrl.isEmpty()) {
                log.debug3(String.format(
                        "Found issue URL for Vol.%s: %s [%s]",
                        targetVolumeId, issueUrl, titleText));
                cb.foundLink(issueUrl);
                found++;
            }
        }

        log.debug2(String.format(
                "Extracted %d issue URL(s) for volume=%s",
                found, targetVolumeId));

        if (found == 0) {
            log.warning("No issue URLs found for volume=" + targetVolumeId
                    + " on " + srcUrl + " — check volume value in TDB");
        }
    }
}
