package org.lockss.plugin.cloudpublish.liverpool;

import org.jsoup.nodes.Node;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.extractor.JsoupHtmlLinkExtractor.ScriptTagLinkExtractor;
import org.lockss.extractor.JsoupHtmlLinkExtractor.SimpleTagLinkExtractor;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;

import java.util.Map;

public class LupHtmlLinkExtractorFactory
    implements LinkExtractorFactory {
  private static final Logger logger = Logger.getLogger(LupHtmlLinkExtractorFactory.class);

  /*
   *  LupHtmlLinkExtractorFactory checks if we are on a page for a pdf viewer and if so rewrites the relative links
   *  to be absolute. This is because of the LupUrlConsumer factory that stores a redirect chain at the original url.
   */

  private static final String READ_URL_PATTERN = "read/?item_type=journal_article&item_id=";
  private static final String LIVERPOOL_SUBDOMAIN = "https://liverpoolup.cloudpublish.co.uk";
  private static final String RELATIVE_LINK_REGEX = "^/[^/].*";
  private static final String LINK_TAG = "link";
  private static final String A_TAG = "a";
  private static final String IMG_TAG = "img";
  private static final String SCRIPT_TAG = "script";
  private static final String HREF_ATTR = "href";
  private static final String SRC_ATTR = "src";

  @Override
  public LinkExtractor createLinkExtractor(String mimeType) {
    // set up the base link extractor to use specific includes and excludes
    // TURN on form extraction version of Jsoup for when the default is off
    JsoupHtmlLinkExtractor extractor = new JsoupHtmlLinkExtractor(false, true, null, null);
    registerExtractors(extractor);
    return extractor;
  }

  protected JsoupHtmlLinkExtractor.LinkExtractor createLinkTagExtractor(String attr) {
    return new LupLinkTagLinkExtractor(attr);
  }

  protected JsoupHtmlLinkExtractor.LinkExtractor createScriptTagExtractor() {
    return new LupScriptTagLinkExtractor();
  }

  /*
   *  For when it is insufficient to simply use a different link tag or script
   *  tag link extractor class, a child plugin can override this and register
   *  additional or alternate extractors
   */
  protected void registerExtractors(JsoupHtmlLinkExtractor extractor) {
    // this is a little inefficient because the link extractor may get called TWICE
    // if there is BOTH an href and a class attribute on the link tag
    // but we can't guarantee that we'll have both so we need to ensure we catch link

    // the super will still only use href (which is default)
    // but this extractor tagBegin will get called for ALL link tags and we can check other attrs there
    extractor.registerTagExtractor(LINK_TAG, createLinkTagExtractor(HREF_ATTR));
    extractor.registerTagExtractor(A_TAG, createLinkTagExtractor(HREF_ATTR));
    extractor.registerTagExtractor(IMG_TAG, createLinkTagExtractor(SRC_ATTR));
    extractor.registerTagExtractor(SCRIPT_TAG, createScriptTagExtractor());
  }

  /*
  * looks for link tags that are relative and on the rewritten url
   */
  public static class LupLinkTagLinkExtractor extends SimpleTagLinkExtractor {
    // nothing needed in the constructor - just call the parent
    public LupLinkTagLinkExtractor(String attr) {
      super(attr);
    }

    public void tagBegin(Node node, ArchivalUnit au, LinkExtractor.Callback cb) {
      lupTagBegin(node, au, cb, HREF_ATTR);
      lupTagBegin(node, au, cb, SRC_ATTR);
      super.tagBegin(node, au, cb);
    }
  }

  public static class LupScriptTagLinkExtractor extends ScriptTagLinkExtractor {
    public LupScriptTagLinkExtractor() { super(); }

    public void tagBegin(Node node, ArchivalUnit au, LinkExtractor.Callback cb) {
      lupTagBegin(node, au, cb, SRC_ATTR);
      super.tagBegin(node, au, cb);
    }
  }
  private static void lupTagBegin(Node node,
                                  ArchivalUnit au,
                                  LinkExtractor.Callback cb,
                                  String attribute) {

    String srcUrl = node.baseUri();
    // Are we on a page for which this would be pertinent?
    if ((srcUrl != null) && srcUrl.contains(READ_URL_PATTERN)) {
      // now do we have a link to the citations export
      if ((node.hasAttr(attribute))) {
        String attrVal = node.attr(attribute);
        if (attrVal != null && attrVal.matches(RELATIVE_LINK_REGEX)) {
          logger.debug3("found relative link, appending proper url");
          String newUrl = LIVERPOOL_SUBDOMAIN + attrVal;
          logger.debug3("Generated absolute url: " + newUrl);
          cb.foundLink(newUrl);
        }
      }
    }
  }
}