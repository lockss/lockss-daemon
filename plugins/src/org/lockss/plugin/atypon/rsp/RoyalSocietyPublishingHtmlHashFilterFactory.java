package org.lockss.plugin.atypon.rsp;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.tags.Bullet;
import org.htmlparser.tags.BulletList;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.atypon.BaseAtyponHtmlHashFilterFactory;
import org.lockss.util.Logger;

import java.io.InputStream;
import java.util.Vector;

public class RoyalSocietyPublishingHtmlHashFilterFactory extends BaseAtyponHtmlHashFilterFactory {
  Logger log = Logger.getLogger(RoyalSocietyPublishingHtmlHashFilterFactory.class);

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding) {
    NodeFilter[] includeNodes = new NodeFilter[] {
            // Parsing the manifest page and get the list of publications in standard manifest page format.
            // Often times it is in html bullet list format
            new NodeFilter() {
              @Override
              public boolean accept(Node node) {
                if (HtmlNodeFilters.tagWithAttributeRegex("a", "href",
                        "/toc/").accept(node)) {
                  Node liParent = node.getParent();
                  if (liParent instanceof Bullet) {
                    Bullet li = (Bullet)liParent;
                    Vector liAttr = li.getAttributesEx();
                    if (liAttr != null && liAttr.size() == 1) {
                      Node ulParent = li.getParent();
                      if (ulParent instanceof BulletList) {
                        BulletList ul = (BulletList)ulParent;
                        Vector ulAttr = ul.getAttributesEx();
                        return ulAttr != null && ulAttr.size() == 1;
                      }
                    }
                  }
                }
                return false;
              }
            },
            // toc - contents only
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "table-of-content"),
            // doi full/abs/reference content
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "article__body"),
            // Download Citations page
            HtmlNodeFilters.tagWithAttribute("div", "class", "articleList"),
    };

    NodeFilter[] excludeNodes = new NodeFilter[] {
            // Remove from toc page
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "publication-header"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "navigation-column"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "toc-right-side"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "content-navigation"),


            // Remove from doi/full page
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "article-row-right")
    };
    return super.createFilteredInputStream(au, in, encoding,
            includeNodes, excludeNodes);
  }

  @Override
  public boolean doTagIDFiltering() {
    return false;
  }

  @Override
  public boolean doWSFiltering() {
    return true;
  }

  /* removes tags and comments after other processing */
  @Override
  public boolean doTagRemovalFiltering() {
    return true;
  }

  @Override
  public boolean doHttpsConversion() {
    return false;
  }

}