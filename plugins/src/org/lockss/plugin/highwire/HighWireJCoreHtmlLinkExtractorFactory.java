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

package org.lockss.plugin.highwire;

/*
 * This will require daemon 1.62 and later for JsoupHtmlLinkExtractor support
 * The vanilla JsoupHtmlLinkExtractor will generate URLs from tags that it finds on pages
 * without restrictions (inclusion/exclusion rules) and so long as those resulting URLs
 * satisfy the crawl rules they will be collected. 
 */

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.JsoupHtmlLinkExtractor;
import org.lockss.extractor.LinkExtractor;
import org.lockss.extractor.LinkExtractorFactory;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;

// an implementation of JsoupHtmlLinkExtractor
public class HighWireJCoreHtmlLinkExtractorFactory implements LinkExtractorFactory {
  
  private static final Logger log = Logger.getLogger(HighWireJCoreHtmlLinkExtractorFactory.class);
  
  // Previously Thib deemed it acceptable that we collect pages that did not have the volume match 
  // due to articles not appearing in any other AU and not using the same volume name.
  // Now we find article links in the content of AAP that also do not match (in this case 137)
  // http://pediatrics.aappublications.org/content/101/2/315         http://pediatrics.aappublications.org/content/137/2/e20154272
  // however, we should NOT collect the TOC of said page, which causes significant over-crawl
  // http://pediatrics.aappublications.org/content/101/2             http://pediatrics.aappublications.org/content/101/2/315

  // after content is required vol, optional issue, then optional .toc
  private static final Pattern TOC_PATTERN = Pattern.compile("/content/([^/.]+)(?:/[^/.]+)?(?:[.]toc)?$");
  
  // Example http://emboj.embopress.org/content/28/1/4 adds 
  // http://emboj.embopress.org/content/28/1/4.full.pdf
  // http://surgicaltechniques.jbjs.org/content/os-86/1_suppl_2/103
  
  @Override
  public LinkExtractor createLinkExtractor(String mimeType) throws PluginException {
    return new JsoupHtmlLinkExtractor() {
      @Override
      public void extractUrls(final ArchivalUnit au,
                              InputStream in,
                              String encoding,
                              final String srcUrl,
                              final Callback cb)
          throws IOException, PluginException {
        super.extractUrls(au,
                          in,
                          encoding,
                          srcUrl,
                          new Callback() {
                              @Override
                              public void foundLink(String url) {
                                if (au != null) {
                                  if (UrlUtil.isSameHost(srcUrl, url)) {
                                    String volume = au.getConfiguration().get(ConfigParamDescr.VOLUME_NAME.getKey());
                                    if ((volume != null) && !volume.isEmpty()) {
                                      Matcher mat = TOC_PATTERN.matcher(url);
                                      if (mat.find() && !volume.contentEquals(mat.group(1))) {
                                        log.warning("Not extracting TOC url that does not match AU volume: "
                                            + volume + "  " + url);
                                        return;
                                      }
                                    } else {
                                      log.warning("No config value for volume found");
                                    }
                                  }
                                  url = AuUtil.normalizeHttpHttpsFromBaseUrl(au, url);
                                  cb.foundLink(url);
                                }
                              }
                          });
      }
    };
  }
  
}
