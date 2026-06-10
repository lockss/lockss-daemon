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
package org.lockss.plugin.ijournalpro.univofanbar;


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

public class UnivofAnbarCollegeofAgVolumeIssueLinkExtractor implements LinkExtractor {
    private static final Logger log = Logger.getLogger(UnivofAnbarCollegeofAgVolumeIssueLinkExtractor.class.getName());
    private final LinkExtractor defaultExtractor;

    public UnivofAnbarCollegeofAgVolumeIssueLinkExtractor(LinkExtractor defaultExtractor) {
        this.defaultExtractor = defaultExtractor;
    }

    @Override
    public void extractUrls(ArchivalUnit au, InputStream in, String encoding,
                            String srcUrl, Callback cb) throws IOException, PluginException {
        byte[] content = IOUtils.toByteArray(in);
        if (au.getTdbAu() != null) {
            if (srcUrl.contains("browse?_action=issue")) {
                String targetVolume = au.getTdbAu().getVolume();
                log.debug3(String.format("targetVolume: %s", targetVolume));
                Document doc = Jsoup.parse(new ByteArrayInputStream(content), encoding, srcUrl);

                // Each volume is now an accordion div
                Elements accordions = doc.select("div.accordion");
                for (Element accordion : accordions) {
                    // Volume name is in the accordion-button text
                    Element button = accordion.selectFirst("button.accordion-button");
                    if (button == null) continue;

                    String volumeName = button.text().trim();
                    Matcher m = java.util.regex.Pattern.compile("Volume\\s+(\\d+)").matcher(volumeName);
                    if (m.find() && m.group(1).equals(targetVolume)) {
                        // Each issue is in a col-md-3 div; the link is in h5 > a
                        Elements issueCols = accordion.select("div.accordion-body div.col-md-3");
                        for (Element col : issueCols) {
                            Element issueLinkElement = col.selectFirst("h5 a");
                            if (issueLinkElement != null) {
                                String issueLink = issueLinkElement.absUrl("href");
                                String issueName = issueLinkElement.text().trim();
                                log.debug3(String.format("Found - Volume Match: %s, Issue: %s, Link: %s, targetVolume: %s",
                                        volumeName, issueName, issueLink, targetVolume));
                                if (!issueLink.isEmpty()) {
                                    cb.foundLink(issueLink);
                                }
                            }
                        }
                    } else {
                        log.debug3(String.format("Skipping - Volume: %s does not match target: %s",
                                volumeName, targetVolume));
                    }
                }
            } else {
                log.debug3(String.format("All other urls - %s", srcUrl));
                if (defaultExtractor != null) {
                    defaultExtractor.extractUrls(au, new ByteArrayInputStream(content), encoding, srcUrl, cb);
                }
            }
        } else {
            log.debug3(String.format("tdbAu is null: %s", au.getAuId()));
        }
    }
}