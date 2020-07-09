package org.lockss.plugin.chinesedeathscape;

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

public class ChineseDeathscapeJavaScriptLinkRewriterFactory implements LinkRewriterFactory {
    static final Logger logger =
            Logger.getLogger(ChineseDeathscapeJavaScriptLinkRewriterFactory.class);
    
    public InputStream createLinkRewriter(
            String mimeType, ArchivalUnit au, InputStream in,
            String encoding, final String srcUrl,
            final ServletUtil.LinkTransform srvLinkXform)
            throws PluginException, IOException {

        logger.debug3("Fei - js file, JavaScriptLinkRewriterFactory src = " + srcUrl + ", mimeType" + mimeType);

        String server = "http://localhost:8081/ServeContent?url=http://chinesedeathscape.supdigital.org/";
        String local_read_js_file = "/javascripts/read.js";


        /*
        Somehow, need to find ways to rewrite read.js content
         */

        Reader filteredReader = FilterUtil.getReader(in, encoding);
        LineRewritingReader rewritingReader = new LineRewritingReader(filteredReader) {
            @Override
            public String rewriteLine(String line) {

                logger.debug3("Fei - rewriteLine" + line);
                return line;
            }
        };
        return new ReaderInputStream(rewritingReader);
    }
}


