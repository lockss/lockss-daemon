package org.lockss.plugin.michigan;

import org.lockss.daemon.PluginException;
import org.lockss.filter.FilterUtil;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.rewriter.LinkRewriterFactory;
import org.lockss.servlet.ServletUtil;
import org.lockss.util.LineEndingBufferedReader;
import org.lockss.util.LineRewritingReader;
import org.lockss.util.Logger;
import org.lockss.util.ReaderInputStream;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UMichJavaScriptLinkRewriterFactory implements LinkRewriterFactory {
    static final Logger logger =
            Logger.getLogger(UMichJavaScriptLinkRewriterFactory.class);

    //Modified the patter from the one inside UMichHtmlLinkExtractorFactory, since we need the complete line replacement
    private static final Pattern PATTERN_LEAFLET_TILELAYER_IIIF =  Pattern.compile("(.*tileLayer\\.iiif\\(\")([^?\"]+)(\\?[^\"]*)?(\",.*)");

    public InputStream createLinkRewriter(
            String mimeType, ArchivalUnit au, InputStream in,
            String encoding, final String srcUrl,
            ServletUtil.LinkTransform srvLinkXform)
            throws PluginException, IOException {

        //Need to know how to get base_url here
        final String baseUrl = "https://www.fulcrum.org/";

        LineEndingBufferedReader br = new LineEndingBufferedReader(new InputStreamReader(in));

        Reader filteredReader = FilterUtil.getReader(in, encoding);
        LineRewritingReader rewritingReader = new LineRewritingReader(filteredReader) {
            @Override
            public String rewriteLine(String line) {
                Matcher mat = PATTERN_LEAFLET_TILELAYER_IIIF.matcher(line);

                if (mat.find()) {

                    String replacement = "/ServeContent?url=" + baseUrl.substring(0, baseUrl.length() - 1) + mat.group(2);
                    String replacedScriptLine = line.replaceAll(mat.group(2),replacement);

                    logger.debug3("scrLine is " + srcUrl + ", replaced Line  = " + replacedScriptLine);

                    return replacedScriptLine;
                } else {
                    logger.debug3("No replacement happened  = " + line);
                    return line;
                }
            }
        };
        return new ReaderInputStream(rewritingReader);
    }
}

