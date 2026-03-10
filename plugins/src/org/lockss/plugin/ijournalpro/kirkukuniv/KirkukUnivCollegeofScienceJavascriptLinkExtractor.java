package org.lockss.plugin.ijournalpro.kirkukuniv;

import org.lockss.extractor.LinkExtractor;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.IOUtil;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.*;
import java.net.URL;
import java.util.regex.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.extractor.LinkExtractor;

/**
 * Extractor for Kirkuk University (KUJSS).
 * Extracts JS and CSS files with their version query strings (e.g., ?v=0.02).
 */
public class KirkukUnivCollegeofScienceJavascriptLinkExtractor implements LinkExtractor {

    private static final Logger log = Logger.getLogger(KirkukUnivCollegeofScienceJavascriptLinkExtractor.class);

    private static final int DEFAULT_MAX_BUF = 32 * 1024;
    private static final int DEFAULT_OVERLAP = 2 * 1024;

    // Relative path where plugins are stored on the server
    private static final String PLUGIN_PATH_URI = "themes/base/front/assets/plugins/";

    // Regex to capture: plugin_path + 'path/to/file.ext?v=123'
    private static final String RESOURCE_PATH_SRC = "plugin_path\\s*\\+\\s*['\"]([^'\"]+)['\"]";
    private static final Pattern RESOURCE_PATH_PAT = Pattern.compile(RESOURCE_PATH_SRC);

    private int maxBuf = DEFAULT_MAX_BUF;
    private int overlap = DEFAULT_OVERLAP;

    public KirkukUnivCollegeofScienceJavascriptLinkExtractor() {
    }

    public KirkukUnivCollegeofScienceJavascriptLinkExtractor(int maxBuf, int overlap) {
        this.maxBuf = maxBuf;
        this.overlap = overlap;
    }

    @Override
    public void extractUrls(ArchivalUnit au,
                            InputStream in,
                            String encoding,
                            String srcUrl,
                            Callback cb)
            throws IOException {

        if (in == null) throw new IllegalArgumentException("Null InputStream");
        if (cb == null) throw new IllegalArgumentException("Null callback");

        log.debug3("Parsing script for links: " + srcUrl);

        // Targeted specifically at the scripts.js file
        if (srcUrl.contains("scripts.js")) {

            // Determine base host (e.g., https://kujss.uokirkuk.edu.iq)
            String base = srcUrl.substring(0, (srcUrl.indexOf(".iq/") + 4));
            URL pluginBaseUrl = new URL(base + PLUGIN_PATH_URI);

            Reader rdr = new BufferedReader(StringUtil.getLineReader(in, encoding));
            rdr = StringUtil.getLineContinuationReader(rdr);
            StringBuilder sb = new StringBuilder(maxBuf);
            int shift = Math.min(overlap, maxBuf / 2);

            try {
                while (StringUtil.fillFromReader(rdr, sb, maxBuf - sb.length())) {

                    Matcher matcher = RESOURCE_PATH_PAT.matcher(sb);
                    while (matcher.find()) {
                        String rawPath = matcher.group(1);

                        if (!StringUtil.isNullString(rawPath)) {
                            // Resolve to full URL including the version query string
                            String resolved = pluginBaseUrl.toString() + rawPath;

                            log.debug3("Extracted versioned link: " + resolved);
                            cb.foundLink(resolved);
                        }
                    }

                    int sblen = sb.length();
                    if (sblen < maxBuf) break;

                    sb.delete(0, sblen - shift);
                }
            } finally {
                IOUtil.safeClose(rdr);
            }
        }
    }
}
