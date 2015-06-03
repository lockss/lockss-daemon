/*
 * $Id: HighWireDrupalHtmlLinkExtractorFactory.java 39864 2015-02-18 09:10:24Z thib_gc $
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.projmuse;

/*
 * This will require daemon 1.62 and later for JsoupHtmlLinkExtractor support
 * The vanilla JsoupHtmlLinkExtractor will generate URLs from tags that it finds on pages
 * without restrictions (inclusion/exclusion rules) and so long as those resulting URLs
 * satisfy the crawl rules they will be collected. 
 */

import java.io.IOException;
import java.io.InputStream;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.JsoupHtmlLinkExtractor;
import org.lockss.extractor.LinkExtractor;
import org.lockss.extractor.LinkExtractorFactory;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;

// an implementation of JsoupHtmlLinkExtractor
public class ProjectMuseHtmlLinkExtractorFactory implements LinkExtractorFactory {
  
  private static final Logger log = Logger.getLogger(ProjectMuseHtmlLinkExtractorFactory.class);
  
  private static final String SUMM = "/summary/";
  private static final String HTML = ".html";
  
  @Override
  public LinkExtractor createLinkExtractor(String mimeType) throws PluginException {
    return new JsoupHtmlLinkExtractor() {
      @Override
      public void extractUrls(final ArchivalUnit au,
                              InputStream in,
                              String encoding,
                              String srcUrl,
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
                                  url = ProjectMuseUtil.baseUrlHostCheck(au, url);
                                  if (url.contains(SUMM) && url.endsWith(HTML)) {
                                    String purl = url.replace(SUMM, "/");
                                    cb.foundLink(purl);
                                  }
                                }
                                cb.foundLink(url);
                              }
                          });
      }
    };
  }
  
}
