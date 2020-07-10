package org.lockss.plugin.chinesedeathscape;

import org.lockss.daemon.PluginException;
import org.lockss.filter.FilterUtil;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.rewriter.LinkRewriterFactory;
import org.lockss.servlet.ServletUtil;
import org.lockss.util.LineRewritingReader;
import org.lockss.util.Logger;
import org.lockss.util.ReaderInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

public class ChineseDeathscapeApplicationLinkRewriterFactory implements LinkRewriterFactory {
    static final Logger logger =
            Logger.getLogger(ChineseDeathscapeApplicationLinkRewriterFactory.class);
    
    public InputStream createLinkRewriter(
            String mimeType, ArchivalUnit au, InputStream in,
            String encoding, final String srcUrl,
            final ServletUtil.LinkTransform srvLinkXform)
            throws PluginException, IOException {

        logger.debug3("Fei - ChineseDeathscapeApplicationLinkRewriterFactory src = " + srcUrl + ", mimeType = " + mimeType);

        String server = "http://localhost:8081/ServeContent?url=http://chinesedeathscape.supdigital.org/";
        String local_read_js_file = "/javascripts/read.js";
        
        Reader filteredReader = FilterUtil.getReader(in, encoding);
        LineRewritingReader rewritingReader = new LineRewritingReader(filteredReader) {
            @Override
            public String rewriteLine(String line) {

                logger.debug3("Fei - ChineseDeathscapeApplicationLinkRewriterFactory, rewriteLine" + line);
                return line;
            }
        };
        return new ReaderInputStream(rewritingReader);
    }
}


