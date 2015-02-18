/* $Id$
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clockss;

/* This will require daemon 1.62 and later for JsoupHtmlLinkExtractor support
The vanilla JsoupHtmlLinkExtractor will generate URLs from any forms that it finds on pages
without restrictions (inclusion/exclusion rules) and so long as those resulting URLs satisfy the crawl rules
they will be collected which is too broad because you can't know everything you might encounter. 
*/

import java.io.IOException;
import java.io.InputStream;
import org.lockss.config.Configuration;
/*
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.lockss.util.SetUtil;
import org.lockss.extractor.HtmlFormExtractor;
*/ 
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