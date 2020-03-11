package org.lockss.plugin.resiliencealliance;

import org.lockss.daemon.PluginException;
import org.lockss.filter.FilterUtil;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.rewriter.LinkRewriterFactory;
import org.lockss.servlet.ServletUtil;
import org.lockss.util.LineEndingBufferedReader;
import org.lockss.util.LineRewritingReader;
import org.lockss.util.Logger;
import org.lockss.util.ReaderInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResilienceAllianceJavaScriptLinkRewriterFactory implements LinkRewriterFactory {
    static final Logger logger =
            Logger.getLogger(ResilienceAllianceJavaScriptLinkRewriterFactory.class);

    /*
    <a href="javascript:awin('table1.html','pAttachment',850,720)" title="Socioeconomic and environmental characteristics of cities included within the four study areas.">Table1</a>
    */
    protected static final Pattern PATTERN_IMAGE =
            Pattern.compile("<a href=\"javascript\\:awin\\('(.*)(','.*',\\d+\\s*,\\d+\\s*\\)\" title=\".*\">.*</a>)",
                    Pattern.CASE_INSENSITIVE);

    public InputStream createLinkRewriter(
            String mimeType, ArchivalUnit au, InputStream in,
            String encoding, final String srcUrl,
            final ServletUtil.LinkTransform srvLinkXform)
            throws PluginException, IOException {

        final String baseUrl = srcUrl.substring(0,(srcUrl.indexOf("/vol") + 1));

        LineEndingBufferedReader br = new LineEndingBufferedReader(new InputStreamReader(in));

        Reader filteredReader = FilterUtil.getReader(in, encoding);
        LineRewritingReader rewritingReader = new LineRewritingReader(filteredReader) {
            @Override
            public String rewriteLine(String line) {
                Matcher mat = PATTERN_IMAGE.matcher(line);

                if (mat.find()) {

                    String found1 = mat.group(1);
                    String found2 = mat.group(2);

                    logger.debug3("line = " + line + " + , #found1 = " + found1 + ", #found2 = " + found2);

                    String replacement = "/ServeContent?url=" + baseUrl.substring(0, baseUrl.length() - 1) + mat.group(2);

                    StringBuilder replacedUrl = new StringBuilder();
                    replacedUrl.append(found1);
                    replacedUrl.append(replacement);

                    logger.debug3("srcUrl = " + srcUrl + ", replacedUrl = " + replacedUrl);

                    return replacedUrl.toString();
                } else {
                    logger.debug3("No replacement happened  = " + line);
                    return line;
                }
            }
        };
        return new ReaderInputStream(rewritingReader);
    }
}


