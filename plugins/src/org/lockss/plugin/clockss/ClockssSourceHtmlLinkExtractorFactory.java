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

package org.lockss.plugin.clockss;

/* This will require daemon 1.62 and later for JsoupHtmlLinkExtractor support
The vanilla JsoupHtmlLinkExtractor will generate URLs from any forms that it finds on pages
without restrictions (inclusion/exclusion rules) and so long as those resulting URLs satisfy the crawl rules
they will be collected which is too broad because you can't know everything you might encounter. 
*/

import java.io.IOException;
import java.io.InputStream;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.JsoupHtmlLinkExtractor;
import org.lockss.extractor.LinkExtractorFactory;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Constants;
import org.lockss.util.Logger;

/* an implementation of JsoupHtmlLinkExtractor to handle utf-8 character encodings */
public class ClockssSourceHtmlLinkExtractorFactory 
implements LinkExtractorFactory {
  protected static Logger log = Logger.getLogger(ClockssSourceHtmlLinkExtractorFactory.class);
  final static String ENC = Constants.ENCODING_UTF_8;

  /*
   * (non-Javadoc)
   * @see org.lockss.extractor.LinkExtractorFactory#createLinkExtractor(java.lang.String)
   */
  public org.lockss.extractor.LinkExtractor createLinkExtractor(String mimeType) {
    // set up the link extractor with specific includes and excludes
    ClockssSourceHtmlLinkExtractor extractor = new ClockssSourceHtmlLinkExtractor(mimeType);
    return extractor;
  }

  /*
   * A version of the method that allows a child to add additional restrictions
   */
  public org.lockss.extractor.LinkExtractor createLinkExtractor(String mimeType, String encoding) {
            
    // set up the link extractor with specific includes and excludes
    JsoupHtmlLinkExtractor extractor = new JsoupHtmlLinkExtractor();
    return extractor;
  }
  
  protected class ClockssSourceHtmlLinkExtractor extends JsoupHtmlLinkExtractor {
    public ClockssSourceHtmlLinkExtractor (String mimeType) {
      super();
    }
    /**
     * Parse content on InputStream,  call cb.foundUrl() for each URL found
     *
     * @param au       the archival unit
     * @param in       the input stream
     * @param encoding 
     * @param srcUrl   The URL at which the content lives.  Used as the base for
     *                 resolving relative URLs (unless/until base set otherwise by content)
     * @param cb       the callback used to forward all found urls
     */
    @Override
    public void extractUrls(final ArchivalUnit au, final InputStream in,
                            final String encoding, final String srcUrl,
                            final Callback cb)
      throws IOException, PluginException {
      // if srcUrl ends with '/', it's a generated html "manifest|directory" page that
      // needs to be extracted
      if (srcUrl.endsWith("/")) {
        log.debug3("extracting from "+srcUrl);
        super.extractUrls(au, in, encoding, srcUrl, cb);
      } else {
      // if srcUrl doesn't end in '/', it's likely an html file that does not
      // need to be extracted
        log.debug3("NOT extracting from "+srcUrl);
      }
    }
  }
  
}