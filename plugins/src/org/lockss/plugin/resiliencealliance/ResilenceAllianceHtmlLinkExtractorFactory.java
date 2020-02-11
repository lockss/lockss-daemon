package org.lockss.plugin.resiliencealliance;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.JsoupHtmlLinkExtractor;
import org.lockss.extractor.LinkExtractor;
import org.lockss.extractor.LinkExtractorFactory;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResilenceAllianceHtmlLinkExtractorFactory implements LinkExtractorFactory {

    private static final Logger log = Logger.getLogger(ResilenceAllianceHtmlLinkExtractorFactory.class);

    private static final String ANCHOR_TAG = "a";

    protected static final Pattern PATTERN_CITATION =
            Pattern.compile("/downloadCitation\\.aspx\\?(format=[^&]+)?$",
                    Pattern.CASE_INSENSITIVE);

    //https://www.ace-eco.org/vol14/iss2/art2/
    protected static final Pattern PATTERN_ARTICLE =
            Pattern.compile("^(https?://[^/]+)/vol[^/]+/iss[^/]+/art[^/]+",
                    Pattern.CASE_INSENSITIVE);

    /*
    <a href="javascript:awin('table1.html','pAttachment',850,720)" title="Socioeconomic and environmental characteristics of cities included within the four study areas.">Table1</a>
    */
    protected static final Pattern PATTERN_IMAGE =
            Pattern.compile("<a href=\"javascript\\:awin\\('(.*)(','.*',\\d+\\s*,\\d+\\s*\\)\" title=\".*\">.*</a>)",
                    Pattern.CASE_INSENSITIVE);


    @Override
    public LinkExtractor createLinkExtractor(String mimeType) throws PluginException {
        JsoupHtmlLinkExtractor extractor = new JsoupHtmlLinkExtractor(false,false,null,null);
        registerExtractors(extractor);
        return extractor;
    }

    /*
     *  For when it is insufficient to simply use a different link tag or script
     *  tag link extractor class, a child plugin can override this and register
     *  additional or alternate extractors
     */
    protected void registerExtractors(JsoupHtmlLinkExtractor extractor) {

        extractor.registerTagExtractor(ANCHOR_TAG,
                new ResilenceAllianceHtmlLinkExtractor(new String[]{"href"}));

    }

    public static class ResilenceAllianceHtmlLinkExtractor extends JsoupHtmlLinkExtractor.SimpleTagLinkExtractor {

        public ResilenceAllianceHtmlLinkExtractor(final String[] attrs) {
            super(attrs);
        }

        public void tagBegin(Node node, ArchivalUnit au, LinkExtractor.Callback cb) {
            String srcUrl = node.baseUri();

            Matcher articleSetsMat = PATTERN_ARTICLE.matcher(srcUrl);
            if (articleSetsMat.matches()) {
                doSupportiveImages(articleSetsMat.group(1), node, au, cb);
                return;
            }
        }

        public void doSupportiveImages(String srcUrl, Node node, ArchivalUnit au, LinkExtractor.Callback cb) {
            String javascript = ((Element)node).html();
            if (log.isDebug3()) {
                log.debug3("Fei: figure/table contents: " + javascript);
            }
            try (BufferedReader br = new BufferedReader(new StringReader(javascript))) {
                for (String line = br.readLine() ; line != null ; line = br.readLine()) {
                    Matcher mat = PATTERN_IMAGE.matcher(line);
                    if (mat.find()) {
                        String found = mat.group(1);
                        String url = null;
                        if (found.startsWith("http")) { // assume absolute
                            url = found;
                        }
                        else if (found.startsWith("/")) { // assume relative to base_url
                            url = srcUrl.substring(0, srcUrl.indexOf('/', srcUrl.indexOf("://") + 3)) + found;
                        }
                        else { // assume relative to srcUrl
                            url = srcUrl.substring(0, srcUrl.lastIndexOf('/') + 1) + found;
                        }
                        log.debug2(String.format("Fei found table/figure url: %s", url));
                        cb.foundLink(url);
                        break;
                    }
                }
            }
            catch (IOException ioe) {
                log.debug(String.format("I/O exception while parsing <script> tag in %s", srcUrl), ioe);
            }
        }
    }
}

