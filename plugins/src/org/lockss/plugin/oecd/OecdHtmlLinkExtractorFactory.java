/*
Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University
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
package org.lockss.plugin.oecd;

import org.jsoup.nodes.Node;
import org.lockss.extractor.JsoupHtmlLinkExtractor;
import org.lockss.extractor.LinkExtractor;
import org.lockss.extractor.LinkExtractorFactory;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;

public class OecdHtmlLinkExtractorFactory
    implements LinkExtractorFactory {
  private static final Logger logger = Logger.getLogger(OecdHtmlLinkExtractorFactory.class);

  /* we grab this link.
    <form action="/content/paper/jbcma-2015-5jrtfl953jxp/citation" method="get"></form>
  */

  private static final String CITATION_PATTERN = "/content/paper/.*/citation";
  private static final String FORM_TAG = "form";
  private static final String ACTION_ATTR = "action";

  @Override
  public LinkExtractor createLinkExtractor(String mimeType) {
    // set up the base link extractor to use specific includes and excludes
    // TURN on form extraction version of Jsoup for when the default is off
    JsoupHtmlLinkExtractor extractor = new JsoupHtmlLinkExtractor(false, true, null, null);
    registerExtractors(extractor);
    return extractor;
  }

  protected JsoupHtmlLinkExtractor.LinkExtractor createLinkTagExtractor(String attr) {
    return new OecdLinkTagLinkExtractor(attr);
  }
  /*
   *  For when it is insufficient to simply use a different link tag
   *  tag link extractor class, a child plugin can override this and register
   *  additional or alternate extractors
   */
  protected void registerExtractors(JsoupHtmlLinkExtractor extractor) {
    extractor.registerTagExtractor(FORM_TAG, createLinkTagExtractor(ACTION_ATTR));
  }

  /*
   * looks for link tags that are relative and on the rewritten url
   */
  public static class OecdLinkTagLinkExtractor extends JsoupHtmlLinkExtractor.SimpleTagLinkExtractor {
    // nothing needed in the constructor - just call the parent
    public OecdLinkTagLinkExtractor(String attr) {
      super(attr);
    }

    public void tagBegin(Node node, ArchivalUnit au, LinkExtractor.Callback cb) {
      // now do we have a link to the citations export
      if ((node.hasAttr(ACTION_ATTR))) {
        String attrVal = node.attr(ACTION_ATTR);
        if (attrVal != null && attrVal.matches(CITATION_PATTERN)) {
          cb.foundLink(attrVal);
        }
      }
      super.tagBegin(node, au, cb);
    }
  }
}