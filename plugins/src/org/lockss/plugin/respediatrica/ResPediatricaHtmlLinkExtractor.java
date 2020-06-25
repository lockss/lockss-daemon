/*
 * $Id$
 */

/*

Copyright (c) 2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.respediatrica;

import org.lockss.daemon.ConfigParamDescr;
import org.lockss.extractor.GoslingHtmlLinkExtractor;
import org.lockss.extractor.LinkExtractor;
import org.lockss.extractor.LinkExtractorFactory;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;

import java.io.IOException;
import java.io.InputStream;

public class ResPediatricaHtmlLinkExtractor extends GoslingHtmlLinkExtractor {
  
  private static final Logger logger = Logger.getLogger(ResPediatricaHtmlLinkExtractor.class);
  
  @Override
  public void extractUrls(final ArchivalUnit au,
                          InputStream in,
                          String encoding,
                          final String srcUrl,
                          final Callback cb)
      throws IOException {
    
    String url = UrlUtil.stripProtocol(au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey()));
    // Do NOT extract links from home page, as this lead to 
    // over-crawling from current article links on that page
    // The journal home page is the permission page, so it is needed
    if (srcUrl.endsWith(url)) {
      return;
    }
    super.extractUrls(au, in, encoding, srcUrl, cb);
  }
  
  public static class Factory implements LinkExtractorFactory {
    public LinkExtractor createLinkExtractor(String mimeType) {
      return new ResPediatricaHtmlLinkExtractor();
    }
  }
  
}
