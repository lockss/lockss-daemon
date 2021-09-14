package org.lockss.plugin.atypon.aaas;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.tags.LinkTag;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.atypon.BaseAtyponHtmlLinkRewriterFactory;
import org.lockss.rewriter.LinkRewriterFactory;
import org.lockss.servlet.ServletUtil.LinkTransform;
import org.lockss.util.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AaasHtmlLinkRewriterFactory implements LinkRewriterFactory {
  
  private static final Logger logger = Logger.getLogger(AaasHtmlLinkRewriterFactory.class);

  /**
   * This link rewriter adds special processing to substitute a link to show the RIS citation
   *
   */
  @Override
  public InputStream createLinkRewriter(String mimeType,
                                        ArchivalUnit au,
                                        InputStream in,
                                        String encoding,
                                        String url,
                                        LinkTransform xfm)
      throws PluginException, IOException {

    return BaseAtyponHtmlLinkRewriterFactory.createLinkRewriter(mimeType,
                                                                au,
                                                                in,
                                                                encoding,
                                                                url,
                                                                xfm,
                                                                new AaasPreFilter(au,url),
                                                                null);
  }

  static class AaasPreFilter implements NodeFilter {
    /*
     * <a href="#pill-citations">Citations</a>
     * becomes
     * <a href="/action/downloadCitation?doi=...&format=ris&amp;include=cit" target="_blank">
     */

    private static final Pattern DOI_URL_PATTERN =
        Pattern.compile("^(?:https?://.*/)doi/([.0-9]+)/([^/]+)");
    private static final String CIT_DOWNLOAD_ACTION = "action/downloadCitation";
    private static final String PILL_CITATIONS_ANCHOR = "#pill-citations";
    private static final String DOWNLOAD_RIS_TAIL = "&format=ris&include=cit";

    private String html_url = null;
    private ArchivalUnit thisau = null;

    public AaasPreFilter(ArchivalUnit au, String url) {
      super();
      html_url = url;
      thisau = au;
    }

    public boolean accept(Node node) {
      // store the value of tgit statuhe link arguments for later reassembly
      if (node instanceof LinkTag) {
        Matcher doiMat = DOI_URL_PATTERN.matcher(html_url);
        // Are we on a page for which this would be pertinent?
        if (doiMat.find()) {
          // now do we have a link to #pill-citations
          String linkval = ((LinkTag) node).getLink();
          if (linkval == null) {
            return false;
          }
          if (linkval.contains(PILL_CITATIONS_ANCHOR)) {

            logger.info("found a pill citations anchor, rewriting");
            String newUrl =  "/" + CIT_DOWNLOAD_ACTION + "?doi=" + doiMat.group(1) + "/" + doiMat.group(2) + DOWNLOAD_RIS_TAIL;
            ((TagNode) node).setAttribute("target", "_blank");
            ((LinkTag) node).setLink(newUrl);
          }
        }
      }
      return false;
    }
  }
}
