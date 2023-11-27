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

package org.lockss.plugin.anu;

/*
 * This will require daemon 1.62 and later for JsoupHtmlLinkExtractor support
 * The vanilla JsoupHtmlLinkExtractor will generate URLs from tags that it finds on pages
 * without restrictions (inclusion/exclusion rules) and so long as those resulting URLs
 * satisfy the crawl rules they will be collected. 
 */

import java.io.IOException;
import java.io.InputStream;

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
public class AnuHtmlLinkExtractorFactory implements LinkExtractorFactory {
  
  private static final Logger log = Logger.getLogger(AnuHtmlLinkExtractorFactory.class);
  
  // we should NOT collect links to other volumes from the start page(s)
  // https://press.anu.edu.au/publications/aboriginal-history-journal-volume-40
  
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
                                if (au == null) {
                                  log.warning("No au specified");
                                  return;
                                }
                                String base_url = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
                                String journal_id = au.getConfiguration().get(ConfigParamDescr.JOURNAL_ID.getKey());
                                // XXX may be able to simplify the test to optional ([?].*)?
                                if (srcUrl.matches(base_url + "publications/(journals/)?" + journal_id + "([?]page=[0-9]+|[?]field_id_value=)?")) {
                                  if (url.matches(base_url + "node/[0-9]+/download")) {
                                    log.debug2("Not extracting url: " + url);
                                    return;
                                  }
                                  String volume_name = au.getConfiguration().get(ConfigParamDescr.VOLUME_NAME.getKey()); //
                                  if ((volume_name == null) || volume_name.isEmpty()) {
                                    log.warning("No config value for volume_name found");
                                  }
                                  // NOTE: the pattern ought to match "(issue|volume|no-[1-9]|winter)-%s" part of crawl rule
                                  // we exclude urls that match the general rule and yet don't match the au volume
                                  // the following matches on 20 in '2005'
                                  // https://press.anu.edu.au/publications/journals/humanities-research-journal-series-volume-xii-no-1-2005
                                  else if (url.matches("(?i)" + base_url + "publications/(journals/)?" + journal_id + "-(issue|volume|no-[1-9]|winter)-(?!" + volume_name + ")([0-9]+|[IVXLCDM]+)(-.+)?$")) {
                                    log.debug2("Url does not match AU volume: " + volume_name + "  " + url);
                                    return;
                                  }
                                }
                                if (UrlUtil.isSameHost(au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey()), url) ||
                                    url.contains("://press-files.anu.edu.au")) {
                                  url = AuUtil.normalizeHttpHttpsFromBaseUrl(au, url);
                                }
                                cb.foundLink(url);
                              }
                          });
      }
    };
  }
  
}
