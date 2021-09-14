package org.lockss.plugin.atypon.aaas;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Node;
import org.lockss.extractor.JsoupHtmlLinkExtractor.LinkExtractor;
import org.lockss.extractor.LinkExtractor.Callback;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.atypon.BaseAtyponHtmlLinkExtractorFactory;
import org.lockss.util.Logger;

public class AaasHtmlLinkExtractorFactory
    extends BaseAtyponHtmlLinkExtractorFactory {

  /*
   *  AaasHtmlLinkExtractorFactory creates a url to a metadata file (.ris) for every article DOI_HTML page it
   *  encounters.
   */

  private static final Pattern DOI_URL_PATTERN = Pattern.compile("^(https?://.*/)doi/([.0-9]+)/([^/]+)$");
  private static final String ACTION_DOWNLOAD_CITATION = "action/downloadCitation";
  private static final String PILL_CITATIONS_ANCHOR = "#pill-citations";
  private static final String DOWNLOAD_RIS_TAIL = "&format=ris&include=cit";

  @Override
  protected LinkExtractor createLinkTagExtractor(String attr) {
    return new AaasLinkTagLinkExtractor(attr);
  }

  /*
   *  Extend the BaseAtyponLinkTagLinkExtractor to create the
   *  RIS metadata URL from the #pill-citations anchor (which links to a form)
   *  all other link tag cases are handled as per BaseAtypon and Jsoup
   */
  public static class AaasLinkTagLinkExtractor extends BaseAtyponLinkTagLinkExtractor {

    private static final Logger logger = Logger.getLogger(AaasLinkTagLinkExtractor.class);

    // nothing needed in the constructor - just call the parent
    public AaasLinkTagLinkExtractor(String attr) {
      super(attr);
    }

    /*
     * Extending the way links are extracted by the Jsoup link extractor in a specific case:
     * In this case we create a link to the citations download page by pulling the DOI from the srcUrl
     * and generating a link like this:
     *     <base_url>action/downloadCitation?doi=...&format=ris&include=cit
     * after creating this link, fall back to standard atypon/jsoup implementations
     */
    public void tagBegin(Node node, ArchivalUnit au, Callback cb) {
      String srcUrl = node.baseUri();
      Matcher doiMat = DOI_URL_PATTERN.matcher(srcUrl);
      // Are we on a page for which this would be pertinent?
      if ( (srcUrl != null) && doiMat.find()) {
        // now do we have a link to the citations export

        if ((node.hasAttr("href")) ) {
          String hrefval = node.attr("href");
          if (hrefval.contains(PILL_CITATIONS_ANCHOR)) {
            logger.info("found #pill-citations, rewriting");
            // the "/" gets normalized to %2F
            String newUrl = doiMat.group(1) + ACTION_DOWNLOAD_CITATION + "?doi=" + doiMat.group(2) + "/" + doiMat.group(3) + DOWNLOAD_RIS_TAIL;
            logger.debug3("Generated ris citation url: " + newUrl);
            cb.foundLink(newUrl);
          }
        }
      }
      // allow the parent to get the actual pdf link
      super.tagBegin(node, au, cb);
    }
  }
}
