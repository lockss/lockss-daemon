/*
 * $Id: $
 */

/*

Copyright (c) 2018 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

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
                                if (srcUrl.matches(base_url + "publications/(journals/)?" + journal_id + "([?]page=[0-9]+)?")) {
                                  if (url.matches(base_url + "node/[0-9]+/download")) {
                                    log.debug2("Not extracting url: " + url);
                                    return;
                                  }
                                  String volume_name = au.getConfiguration().get(ConfigParamDescr.VOLUME_NAME.getKey()); //
                                  if ((volume_name == null) || volume_name.isEmpty()) {
                                    log.warning("No config value for volume_name found");
                                  }
                                  else if (url.matches(base_url + "publications/(journals/)?" + journal_id + "-(issue|volume)-(?!" + volume_name + ")(?i:[0-9]+|[IVXLCDM]+)(-(.+))?$")) {
                                    log.debug2("Not extracting url that does not match AU volume: "
                                        + volume_name + "  " + url);
                                    return;
                                  }
                                }
                                url = AuUtil.normalizeHttpHttpsFromBaseUrl(au, url);
                                cb.foundLink(url);
                              }
                          });
      }
    };
  }
  
}
