package org.lockss.plugin.chinesedeathscape;

import org.lockss.daemon.PluginException;
import org.lockss.filter.FilterUtil;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.rewriter.LinkRewriterFactory;
import org.lockss.servlet.ServletUtil;
import org.lockss.util.*;
import org.springframework.beans.factory.config.PreferencesPlaceholderConfigurer;

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

        final String localReadJSFile = "/javascripts/read.js";

        final String tutorialModelImagePath = "/images/tutorial/";

        final String apiJson = "/api";

        Reader filteredReader = FilterUtil.getReader(in, encoding);
        LineRewritingReader rewritingReader = new LineRewritingReader(filteredReader) {
            @Override
            public String rewriteLine(String line) {
                // handle tutorial-image part
                if (srcUrl.contains(localReadJSFile)) {
                    logger.debug3("Fei - rewriteLine" + line);

                    String serveContentUrl =  "/ServeContent?url=" + srcUrl.replace(localReadJSFile, "");
                    String serverImageUrl = serveContentUrl + tutorialModelImagePath;
                    String apiJsonUrl = serveContentUrl + apiJson;

                    int imageUrlFound = line.indexOf(tutorialModelImagePath);
                    int apiJsonFound = line.indexOf(apiJson);

                    String replacedLine = line;
                    
                    // handle tutorial-image part
                    if (imageUrlFound > -1) {
                        logger.debug3("imageUrlFound = " + imageUrlFound);
                        logger.debug3("line = " + line);
                        logger.debug3("serveContentUrl = " + serveContentUrl + ", serverImageUrl = " + serverImageUrl);
                        replacedLine = replacedLine.replaceAll(tutorialModelImagePath, serverImageUrl);
                        logger.debug3("imageUrlFound = " + replacedLine);

                    }

                    // handle tutorial-image part
                    if (apiJsonFound  > -1) {
                        logger.debug3("imageUrlFound = " + imageUrlFound);
                        logger.debug3("line = " + line);
                        logger.debug3("(apiJsonUrl = " + apiJsonUrl + ", apiJson = " + apiJson);
                        replacedLine = replacedLine.replaceAll(apiJson, apiJsonUrl);
                        logger.debug3("apiJson = " + replacedLine);
                    }

                    return replacedLine;

                }
                return line;
            }
        };
        return new ReaderInputStream(rewritingReader);
    }
}


