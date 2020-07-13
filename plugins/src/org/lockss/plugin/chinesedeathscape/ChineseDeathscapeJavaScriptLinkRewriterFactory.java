package org.lockss.plugin.chinesedeathscape;

import org.lockss.daemon.PluginException;
import org.lockss.filter.FilterUtil;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.rewriter.LinkRewriterFactory;
import org.lockss.servlet.ServletUtil;
import org.lockss.util.*;
import java.io.*;

public class ChineseDeathscapeJavaScriptLinkRewriterFactory implements LinkRewriterFactory {

    static final Logger logger =
            Logger.getLogger(ChineseDeathscapeJavaScriptLinkRewriterFactory.class);
    
    public InputStream createLinkRewriter(
            String mimeType, ArchivalUnit au, InputStream in,
            String encoding, final String srcUrl,
            final ServletUtil.LinkTransform srvLinkXform)
            throws PluginException, IOException {

        logger.debug3("Fei - JavaScriptLinkRewriterFactory src = " + srcUrl + ", mimeType = " + mimeType);

        if (in == null) {
            throw new IllegalArgumentException("Called with null InputStream");
        }

        final String local_read_js_file = "/javascripts/read.js";
        final String image_dir = "/images/tutorial/";

        Reader filteredReader = FilterUtil.getReader(in, encoding);
        LineRewritingReader rewritingReader = new LineRewritingReader(filteredReader) {
            @Override
            public String rewriteLine(String line) {

                if (srcUrl.contains(local_read_js_file)) {
                    logger.debug3("Fei - rewriteLine" + line);
                    String serveContentUrl =  "/ServeContent?url=" + srcUrl.replace(local_read_js_file, "");
                    String serverImageUrl = serveContentUrl + image_dir;

                    int imageUrlFound = line.indexOf(image_dir);

                    if (imageUrlFound > -1) {
                        logger.debug3("imageUrlFound = " + imageUrlFound);
                        logger.debug3("line = " + line);
                        logger.debug3("serveContentUrl = " + serveContentUrl + ", serverImageUrl = " + serverImageUrl);
                        String replacedLine = line.replaceAll(image_dir, serverImageUrl);
                        logger.debug3("imageUrlFound = " + replacedLine);

                        return replacedLine;
                    }
                }
                return line;
            }
        };
        return new ReaderInputStream(rewritingReader);
    }
}


