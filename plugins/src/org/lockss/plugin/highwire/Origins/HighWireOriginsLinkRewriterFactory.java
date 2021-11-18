package org.lockss.plugin.highwire.Origins;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.tags.LinkTag;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.rewriter.LinkRewriterFactory;
import org.lockss.rewriter.NodeFilterHtmlLinkRewriterFactory;
import org.lockss.servlet.ServletUtil;
import org.lockss.util.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HighWireOriginsLinkRewriterFactory implements LinkRewriterFactory {

  private static final Logger log =
      Logger.getLogger(HighWireOriginsLinkRewriterFactory.class);

  /**
   * This link rewriter removes all but the href attributes
   * from the 3 tab Anchor tags on article landing pages
   * ARTICLE | FIGURES & DATA | INFO & METRICS
   * so that the href sends the user to the page, cancelling the jQuery
   */

  @Override
  public InputStream createLinkRewriter(String mimeType,
                                        ArchivalUnit au,
                                        InputStream in,
                                        String encoding,
                                        String url,
                                        ServletUtil.LinkTransform xfm)
      throws PluginException, IOException {

    NodeFilterHtmlLinkRewriterFactory fact =
        new NodeFilterHtmlLinkRewriterFactory();

    fact.addPreXform(new HighWireOriginsLinkRewriterFactory.HighWireOriginsPreFilter(url));
    return fact.createLinkRewriter(mimeType, au, in, encoding, url, xfm);
  }

  static class HighWireOriginsPreFilter implements NodeFilter {
    /*
        <a
          href="http://localhost:8081/ServeContent?url=https%3A%2F%2Fwww.rcpjournals.org%2Fcontent%2Ffuturehosp%2F8%2F1%2Fe32%2Ftab-figures-data"
          class="panels-ajax-tab-tab panels-ajax-tabs-once-processed panels-ajax-tabs-processed hw-panels-ajax-tabs-once-processed"
          data-panel-name="article_tab_data"
          data-target-id="highwire_article_tabs"
          data-entity-context="node:12153"
          data-trigger="tab-figures-data"
          data-url-enabled="1"
          style="cursor: pointer;"
          >
          Figures &amp; Data
        </a>
    */
    private static final Pattern ARTICLE_URL_PATTERN =
        Pattern.compile("^https?.*/content/.*/([^/]+/)([^/]+/)?([^./?&]+([.]\\d{1,4})?)($|/tab-(article-info|figures-data))");
    private String html_url = null;

    public HighWireOriginsPreFilter(String url) {
      super();
      html_url = url;
    }

    public boolean accept(Node node) {
      // store the value of the PDF link arguments for later reassembly
      if (node instanceof LinkTag) {
        Matcher artMatcher = ARTICLE_URL_PATTERN.matcher(html_url);
        // Are we on a page for which this would be pertinent?
        if (artMatcher.find()) {
          String data_target_id = ((LinkTag) node).getAttribute("data-panel-name");
          // are we on an anchor tag that is one of
          // article_tab_art, article_tab_fig, article_tab_info
          if (data_target_id != null && data_target_id.contains("article_tab")) {
            ((TagNode) node).removeAttribute("data-panel-name");
            ((TagNode) node).removeAttribute("data-target-id");
            ((TagNode) node).removeAttribute("data-entity-context");
            ((TagNode) node).removeAttribute("data-trigger");
            ((TagNode) node).removeAttribute("data-url-enabled");
            ((TagNode) node).removeAttribute("style");
            ((TagNode) node).removeAttribute("class");
          }
        }
      }
      return false;
    }
  }
}
