package org.lockss.plugin.cloudpublish.liverpool;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.tags.HeadTag;
import org.htmlparser.tags.MetaTag;
import org.htmlparser.tags.ScriptTag;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.rewriter.LinkRewriterFactory;
import org.lockss.rewriter.NodeFilterHtmlLinkRewriterFactory;
import org.lockss.servlet.ServletUtil.LinkTransform;
import org.lockss.util.Logger;

import java.io.IOException;
import java.io.InputStream;

public class LupHtmlLinkRewriterFactory implements LinkRewriterFactory {
  private static final Logger log = Logger.getLogger(LupHtmlLinkRewriterFactory.class);

  private static final String READ_URL_PATTERN = "read/?item_type=journal_article&item_id=";
  private static final String LIVERPOOL_SUBDOMAIN = "https://liverpoolup.cloudpublish.co.uk";
  private static final String RELATIVE_LINK_REGEX = "^/[^/].*$";
  private static final String HREF_TAG = "(LINK|A|IMG)";
  private static final String HREF_ATTR = "href";
  private static final String SRC_ATTR = "src";

  @Override
  public InputStream createLinkRewriter(String mimeType,
                                        ArchivalUnit au,
                                        InputStream in,
                                        String encoding,
                                        String url,
                                        LinkTransform xform
  ) throws PluginException, IOException {
    NodeFilterHtmlLinkRewriterFactory fact =
        new NodeFilterHtmlLinkRewriterFactory();
    fact.addPreXform(new LupPreFilter(url));
    return fact.createLinkRewriter(mimeType, au, in, encoding, url, xform);
  }

  static class LupPreFilter implements NodeFilter {

    private String srcUrl;

    public LupPreFilter(String url) {
      super();
      srcUrl = url;
    }

    @Override
    public boolean accept(Node node) {
      if ((srcUrl != null) && srcUrl.contains(READ_URL_PATTERN)) {
        if (node instanceof TagNode) {
          String tagName = ((TagNode) node).getTagName();
          log.debug3("found a TagNode on the page: " + tagName);
          rewriteOrNot(node, SRC_ATTR);
          rewriteOrNot(node, HREF_ATTR);
        }
      }
      return false;
    }
    public void rewriteOrNot(Node node, String attr) {
      // the ServeContent prepending has already been done, so, we actually rewrite the base_url
      String url = ((TagNode) node).getAttribute(attr);
      log.info("checking url on readurl: " + url );
      if ((url != null) && url.matches(RELATIVE_LINK_REGEX)) {
        log.debug3("found relative link: " + url);
        url =  LIVERPOOL_SUBDOMAIN + url;
        log.debug3("rewrote: " + url);
        ((TagNode) node).setAttribute(attr, url);
      }
    }
  }
}
