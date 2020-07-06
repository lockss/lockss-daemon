package org.lockss.plugin.chinesedeathscape;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.lockss.extractor.JsoupHtmlLinkExtractor;
import org.lockss.extractor.LinkExtractor;
import org.lockss.extractor.LinkExtractorFactory;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChineseDeathscapeHtmlLinkExtractorFactory implements LinkExtractorFactory {

    private static final Logger log = Logger.getLogger(ChineseDeathscapeHtmlLinkExtractorFactory.class);

    private static final String SCRIPT_TAG = "script";

    private static final List<String> TUTORIAL_IMAGES = new ArrayList<String>() {{
        add("/images/tutorial/bookmark_button.png");
        add("/images/tutorial/grave_size_legend.png");
        add("/images/tutorial/map_options_menus.png");
        add("/images/tutorial/map_options_slider.png");
        add("/images/tutorial/overlapping_circles.png");
        add("/images/tutorial/reset_button.png");
        add("/images/tutorial/single_circle.png");
        add("/images/tutorial/time_slider.png");
        add("/images/tutorial/timeline.png");
        add("/images/tutorial/underlined_text.png");
    }};

    @Override
    public LinkExtractor createLinkExtractor(String mimeType) {
        JsoupHtmlLinkExtractor extractor = new JsoupHtmlLinkExtractor(false, true, null, null);
        extractor.registerTagExtractor(SCRIPT_TAG, new MyScriptTagLinkExtractor());
        //extractor.registerTagExtractor(SCRIPT_TAG, new JsoupHtmlLinkExtractor.SimpleTagLinkExtractor(new String[]{"src"}));
        return extractor;
    }


    public static class MyScriptTagLinkExtractor extends JsoupHtmlLinkExtractor.ScriptTagLinkExtractor {

        private static Logger log = Logger.getLogger(MyScriptTagLinkExtractor.class);

        public MyScriptTagLinkExtractor() {
            super();
        }

        /*
         *
         <script src="/javascripts/read.js"></script>
         */

        protected static final Pattern PATTERN_FILE_SETS =
                Pattern.compile("^(https://chinesedeathscape\\.supdigital\\.org/read/[^/]+)$");


        public void tagBegin(Node node, ArchivalUnit au, LinkExtractor.Callback cb) {
            String srcUrl = node.baseUri();

            log.debug3("Fei - tagBegin srcUrl = " + srcUrl);

            Matcher fileSetsMat = PATTERN_FILE_SETS.matcher(srcUrl);
            if (fileSetsMat.matches()) {
                doFileSets(fileSetsMat.group(1), node, au, cb);
                return;
            }

            if (srcUrl.contains("/javascripts/read.js") ) {
                log.debug3("Fei - read.js is found: ");
            }

            // Not a special case, fall back to standard Jsoup
            super.tagBegin(node, au, cb);
        }

        public void doFileSets(String srcUrl, Node node, ArchivalUnit au, LinkExtractor.Callback cb) {
            String js_src = node.attr("src");

            log.debug3("Fei - srcUrl " + srcUrl + " for the following");
            log.debug3("Fei - js_src: " + js_src);

            String readjs_url = "/javascripts/read.js";

            if (js_src.contains(readjs_url)) {
                String complete_js_url = srcUrl.substring(0, srcUrl.indexOf(".org") + ".org".length()) + readjs_url;
                log.debug3("Fei - complete_js_url is found: " + complete_js_url);

                cb.foundLink(complete_js_url);
            }

        }

        /*
            "read.js" was compiled to one-line javascript

        public void doFileSets(String srcUrl, Node node, ArchivalUnit au, LinkExtractor.Callback cb) {

            String javascript = ((Element)node).html();
            if (log.isDebug3()) {
                log.debug3("<script> contents: " + javascript);
            }
            try (BufferedReader br = new BufferedReader(new StringReader(javascript))) {
                for (String line = br.readLine() ; line != null ; line = br.readLine()) {
                    log.debug3("Fei - line: " + line);

                    if (line.contains("window.GRAVES = {\"baseLayers\"")) {
                        log.debug3("Fei - found read.js: " + line);

                        for (String image_url : TUTORIAL_IMAGES) {
                            if (line.contains(image_url)) {
                                String complete_image_url = srcUrl.substring(0, srcUrl.indexOf(".org") + ".org".length()) + image_url;
                                log.debug3("Fei - image_url is found: " + complete_image_url);
                                cb.foundLink(complete_image_url);
                            }
                        }
                    }
                }
            }
            catch (IOException ioe) {
                log.debug(String.format("I/O exception while parsing <script> tag in %s", srcUrl), ioe);
            }
        }

         */

    }
}

