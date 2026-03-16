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

import org.apache.commons.io.IOUtils;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.LinkExtractor;
import org.lockss.plugin.ArchivalUnit;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.lockss.util.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.util.regex.Matcher;

public class KirkukUnivCollegeofScienceVolumeIssueLinkExtractor implements LinkExtractor {

    private static final Logger log = Logger.getLogger(KirkukUnivCollegeofScienceVolumeIssueLinkExtractor.class.getName());
    private final LinkExtractor defaultExtractor; // Store the original extractor

    // Update constructor to take the default extractor
    public KirkukUnivCollegeofScienceVolumeIssueLinkExtractor(LinkExtractor defaultExtractor) {
        this.defaultExtractor = defaultExtractor;
    }
    /*
    <div class="accordion mb-2" id="">
        <div class="accordion-item">
          <h2 class="accordion-header" id="pVol_15176">
             <button class="accordion-button bg-body collapsed text-primary" type="button" data-bs-toggle="collapse" data-bs-target="#pVol_15176_" aria-expanded="true" aria-controls="pVol_15176_"><b><i class="fa-regular fa-file-lines me-2"></i>Volume 20 (2025)</b></button>
          </h2>
          <div id="pVol_15176_" class="accordion-collapse collapse" aria-labelledby="pVol_15176">
             <div class="accordion-body p-2 row">
                <div class="col-md-3 col-lg-3">
                   <div>
                      <a class="js_click pointer_cursor" data-handler="loadModal"  data-param_a="Kirkuk Journal of Science" data-param_b="./data/kscis/coversheet/cover_en.jpg">
                      <img src="data/kscis/coversheet/cover_en.jpg" alt="Kirkuk Journal of Science" class="col-12 shadow-sm"/>
                      </a>
                   </div>
                   <div>
                      <h5 class="text-center mt-3"><a href="issue_15176_15451.html">Issue 4</a></h5>
                   </div>
                </div>
                <div class="col-md-3 col-lg-3">
                   <div>
                      <a class="js_click pointer_cursor" data-handler="loadModal"  data-param_a="Kirkuk Journal of Science" data-param_b="./data/kscis/coversheet/cover_en.jpg">
                      <img src="data/kscis/coversheet/cover_en.jpg" alt="Kirkuk Journal of Science" class="col-12 shadow-sm"/>
                      </a>
                   </div>
                <div>
                      <h5 class="text-center mt-3"><a href="issue_15176_15403.html">Issue 3</a></h5>
                   </div>
                </div>
                <div class="col-md-3 col-lg-3">
                   <div>
                      <a class="js_click pointer_cursor" data-handler="loadModal"  data-param_a="Kirkuk Journal of Science" data-param_b="./data/kscis/coversheet/cover_en.jpg">
                      <img src="data/kscis/coversheet/cover_en.jpg" alt="Kirkuk Journal of Science" class="col-12 shadow-sm"/>
                      </a>
                   </div>
                   <div>
                      <h5 class="text-center mt-3"><a href="issue_15176_15376.html">Issue 2</a></h5>
                   </div>
                </div>
                <div class="col-md-3 col-lg-3">
                   <div>
                      <a class="js_click pointer_cursor" data-handler="loadModal"  data-param_a="Kirkuk Journal of Science" data-param_b="./data/kscis/coversheet/cover_en.jpg">
                      <img src="data/kscis/coversheet/cover_en.jpg" alt="Kirkuk Journal of Science" class="col-12 shadow-sm"/>
                      </a>
                   </div>
                   <div>
                      <h5 class="text-center mt-3"><a href="issue_15176_15177.html">Issue 1</a></h5>
                   </div>
                </div>
             </div>
          </div>
        </div>
    </div>
     */

    @Override
    public void extractUrls(ArchivalUnit au, InputStream in, String encoding,
                            String srcUrl, Callback cb) throws IOException, PluginException {

        byte[] content = IOUtils.toByteArray(in);

        if (au.getTdbAu() != null) {
            // Updated check for the new URL structure if necessary
            if (srcUrl.contains("browse?_action=issue") || srcUrl.contains("archive.html")) {

                String targetVolume = au.getTdbAu().getVolume();
                Document doc = Jsoup.parse(new ByteArrayInputStream(content), encoding, srcUrl);

                // 1. Find all accordion headers (e.g., "Volume 20 (2025)")
                Elements accordionItems = doc.select("div.accordion-item");

                for (Element item : accordionItems) {
                    Element header = item.selectFirst("h2.accordion-header button");
                    if (header == null) continue;

                    String volumeText = header.text().trim();
                    // Pattern matches "Volume 20"
                    Matcher m = java.util.regex.Pattern.compile("Volume\\s+(\\d+)").matcher(volumeText);

                    if (m.find() && m.group(1).equals(targetVolume)) {

                        // 2. Find the corresponding body for this specific volume
                        Element body = item.selectFirst("div.accordion-body");
                        if (body == null) continue;

                        // 3. Extract all links inside the h5 tags
                        Elements issueLinks = body.select("h5 a[href]");
                        for (Element link : issueLinks) {
                            String issueUrl = link.absUrl("href");
                            String issueName = link.text().trim();

                            log.debug3(String.format("Found Match - Vol: %s, Issue: %s, Link: %s",
                                    targetVolume, issueName, issueUrl));

                            if (!issueUrl.isEmpty()) {
                                cb.foundLink(issueUrl);
                            }
                        }
                    }
                }
            } else {
                if (defaultExtractor != null) {
                    defaultExtractor.extractUrls(au, new ByteArrayInputStream(content), encoding, srcUrl, cb);
                }
            }
        }
    }
}

