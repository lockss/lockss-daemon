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
            final ServletUtil.LinkTransform srvLinkXform)
            throws PluginException, IOException {

        final String baseUrl = srcUrl.substring(0,(srcUrl.indexOf("/concern/file_sets/") + 1));

        LineEndingBufferedReader br = new LineEndingBufferedReader(new InputStreamReader(in));

        Reader filteredReader = FilterUtil.getReader(in, encoding);
        LineRewritingReader rewritingReader = new LineRewritingReader(filteredReader) {
            @Override
            public String rewriteLine(String line) {
                Matcher mat = PATTERN_LEAFLET_TILELAYER_IIIF.matcher(line);

                if (mat.find()) {

                    String found1 = mat.group(1);
                    String found2 = mat.group(2);
                    String found3 = mat.group(3);
                    String found4 = mat.group(4);

                    logger.debug3("line = " + line + " + , #found1 = " + found1 + ", #found2 = " + found2 + ", #found3 = " + found3 + ", #found4 = " + found4 + "#");

                    //https://www.fulcrum.org/concern/file_sets/7w62f903v?locale=en, replaced Line  =         layer = L.tileLayer.iiif("/ServeContent?url=https://www.fulcrum.org/image-service/7w62f903v/info.json?1555623447", { bestFit: true } );
                    //http://localhost:8081/ServeContent?url=https://www.fulcrum.org/image-service/7w62f903v/info.json?1555623447
                    String replacement = "/ServeContent?url=" + baseUrl.substring(0, baseUrl.length() - 1) + mat.group(2);

                    //#found2 = /image-service/xg94hq387/info.json#, #found3 = ?1555632458#, #found4 = ", { bestFit: true } );#
                    //Replace found2 with new link and drop found3, it is the timestamp part
                    //use StringBuilder, since replaceAll will give "Dangling meta character '?' near index 0 \\?1555623447" error
                    StringBuilder replacedUrl = new StringBuilder();
                    replacedUrl.append(found1);
                    replacedUrl.append(replacement);
                    replacedUrl.append(found4);

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

