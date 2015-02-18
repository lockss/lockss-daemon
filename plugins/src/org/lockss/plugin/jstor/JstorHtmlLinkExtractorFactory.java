/* $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.jstor;

import org.lockss.extractor.HtmlFormExtractor;
import org.lockss.extractor.JsoupHtmlLinkExtractor;
import org.lockss.extractor.LinkExtractorFactory;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;

/* Jstor needs its own LinkExtractor so that it can generate a link
 * for citation download from the DOI information stored on TOC pages.
 * Because the information is stored on the TOC page in a checkbox <input> tag
 * the html link extractor itself cannot do the work. It must use a custom
 * Jstor FORM element link extractor. 
 * This plugin version of the link extractor is needed to specify use of the 
 * custom HtmlFormExtractor which has the necessary custom FormElementLinkExtractor
 * 
 * This custom JsoupHtmlLinkExtractor could also be used to set up additional
 * restrictions for form processing but at least for now this isn't needed because
 * Jstor only allows us such limited crawling (because of redirection) that we
 * control the creation of the citation URLs anyway.
 */

public class JstorHtmlLinkExtractorFactory 
implements LinkExtractorFactory {

  public org.lockss.extractor.LinkExtractor createLinkExtractor(String mimeType) {

    return new JstorHtmlLinkExtractor();
  }

  public static class JstorHtmlLinkExtractor extends JsoupHtmlLinkExtractor {
    
    private static Logger log = Logger.getLogger(JstorHtmlLinkExtractor.class);

    @Override
    protected HtmlFormExtractor getFormExtractor(final ArchivalUnit au,
        final String encoding,
        final Callback cb) {
      log.debug3("Creating new JstorHtmlFormExtractor");
      return new JstorHtmlFormExtractor(au, cb, encoding,   getFormRestrictors(), getFormGenerators());
    }
  }


}
