package org.lockss.plugin.chinesedeathscape;

import org.htmlparser.Attribute;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.MetaTag;
import org.htmlparser.util.NodeList;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.rewriter.LinkRewriterFactory;
import org.lockss.rewriter.NodeFilterHtmlLinkRewriterFactory;
import org.lockss.servlet.ServletUtil;
import org.lockss.util.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChineseDeathscapeHtmlLinkRewriterFactory implements LinkRewriterFactory {

    private static final Logger log =
            Logger.getLogger(ChineseDeathscapeHtmlLinkRewriterFactory.class);

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

        fact.addPreXform(new ChineseDeathscapePreFilter(url));
        return fact.createLinkRewriter(mimeType, au, in, encoding, url, xfm);
    }

    static class ChineseDeathscapePreFilter implements NodeFilter {

        /*
        <div class="btn-wrap">
            <a href="http://chinesedeathscape.supdigital.org" class="btn sticky">Enter</a>
        </div>
         */
        protected static final Pattern PATTERN =
                Pattern.compile("(http://chinesedeathscape\\.supdigital\\.org)", Pattern.CASE_INSENSITIVE);

        public ChineseDeathscapePreFilter(String url) {
            super();
        }

        public boolean accept(Node node) {
            log.debug3("Fei - inside accept method = ");

            if (node instanceof LinkTag) {
                String linkUrl = ((LinkTag) node).extractLink();
                if (linkUrl == null) {
                    return false;
                }
                //<a href="/ViewContent?url=http%3A%2F%2Fchinesedeathscape.org&amp;auid=org%7Clockss%7Cplugin%7Cchinesedeathscape%7CClockssChineseDeathscapePlugin%26base_url%7Ehttp%253A%252F%252Fchinesedeathscape%252Eorg%252F%26base_url2%7Ehttp%253A%252F%252Fchinesedeathscape%252Esupdigital%252Eorg%252F">http://chinesedeathscape.org</a>
                Matcher mat = PATTERN.matcher(linkUrl);

                log.debug3("Fei - linkUrl = " + linkUrl);
                if (mat.find()) {
                    String origialUrl = mat.group(1);
                    String newUrl = "/ViewContent?url=" + origialUrl;

                    log.debug3("Fei - newUrl = " + newUrl);
                    ((LinkTag)node).setLink(newUrl);
                }  else {
                    log.debug3("Fei - linkUrl does not match ");
                }
            }
            return false;
        }
    }
}
