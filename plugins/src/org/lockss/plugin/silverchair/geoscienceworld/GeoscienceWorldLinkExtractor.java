package org.lockss.plugin.silverchair.geoscienceworld;

import org.jsoup.nodes.Node;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.extractor.JsoupHtmlLinkExtractor;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeoscienceWorldLinkExtractor extends JsoupHtmlLinkExtractor {

    private static final Logger log = Logger.getLogger(GeoscienceWorldLinkExtractor.class);

    public GeoscienceWorldLinkExtractor() {
        super();
        registerATagExtractor();
    }

    protected void registerATagExtractor() {
        registerTagExtractor("a", new SimpleTagLinkExtractor(new String[] {"href"}) {
            @Override
            public void tagBegin(Node node, ArchivalUnit au, Callback cb) {
                String hrefLink = node.attr("href");

                if (!StringUtil.isNullString(hrefLink)) {

                    if (hrefLink.equals("javascript:;") ) {
                        log.debug3("found target invalid javascript link = " + hrefLink);
                        return;
                    }
                }
                super.tagBegin(node, au, cb);
            }
        });
    }
}

