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