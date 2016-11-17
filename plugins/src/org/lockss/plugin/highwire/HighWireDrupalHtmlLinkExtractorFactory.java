/*
 * $Id: $
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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
public class HighWireDrupalHtmlLinkExtractorFactory implements LinkExtractorFactory {
  
  private static final Logger log = Logger.getLogger(HighWireDrupalHtmlLinkExtractorFactory.class);
  
  private static final Pattern LPAGE =
      Pattern.compile("content(/[^/.]+|(?=.*/bmj[.]))/([^/.]+)/([^/.]*?)((?:(bmj|[ivx]+)[.])?([^/.]+?|\\d+[.]\\d+))$");
  private static final String FULL_PDF = ".full.pdf";
  
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
                                    
                                    Matcher mat = LPAGE.matcher(url);
                                    if (mat.find()) {
                                      String purl = url + FULL_PDF;
                                      purl = AuUtil.normalizeHttpHttpsFromBaseUrl(au, purl);
                                      cb.foundLink(purl);
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
