package org.lockss.plugin.blackquotidianrdf;

import org.lockss.extractor.LinkExtractor;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.IOUtil;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import org.lockss.util.UrlUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BlackQuotidianRegexpCssLinkExtractor implements LinkExtractor {
    private static final Logger log = Logger.getLogger(BlackQuotidianRegexpCssLinkExtractor.class);

    private static final int MAX_URL_LENGTH = 2100;
    // Amount of CSS input to buffer up for matcher
    private static final int DEFAULT_MAX_BUF = 32 * 1024;
    // Amount at end of buffer to rescan at beginning of next bufferfull
    private static final int DEFAULT_OVERLAP = 2 * 1024;

    /*# sourceMappingURL=bootstrap.min.css.map */
    // Adapted from Heritrix's ExtractorCSS
    private static final String CSS_URI_EXTRACTOR =
            "(?i)(?:@import\\s+(?:url[(]|)|url[(])\\s*([\\\"\']?)" + // G1
                    "(.{0," + MAX_URL_LENGTH + "}?)\\1\\s*[);]"; // G2
    // GROUPS:
    // (G1) optional ' or "
    // (G2) URI

    private static Pattern CSS_URL_PAT = Pattern.compile(CSS_URI_EXTRACTOR);

    // Pattern for character escapes to be removed from URLs
    private static final String CSS_BACKSLASH_ESCAPE = "\\\\([,'\"\\(\\)\\s])";

    private static Pattern CSS_BACKSLASH_PAT =
            Pattern.compile(CSS_BACKSLASH_ESCAPE);

    private int maxBuf = DEFAULT_MAX_BUF;

    private int overlap = DEFAULT_OVERLAP;

    public BlackQuotidianRegexpCssLinkExtractor() {
    }

    public BlackQuotidianRegexpCssLinkExtractor(int maxBuf, int overlap) {
        this.maxBuf = maxBuf;
        this.overlap = overlap;
    }

    /* Inherit documentation */
    public void extractUrls(ArchivalUnit au,
                            InputStream in,
                            String encoding,
                            String srcUrl,
                            LinkExtractor.Callback cb)
            throws IOException {
        log.debug2("Parsing " + srcUrl + ", enc " + encoding);
        if (in == null) {
            throw new IllegalArgumentException("Called with null InputStream");
        }
        if (cb == null) {
            throw new IllegalArgumentException("Called with null callback");
        }
        URL baseUrl = new URL(srcUrl);

        // This needs a regexp matcher that can match against a Reader.
        // Interim solution is to loop matching against a rolling fixed-length
        // chunk of input, with overlaps between chunks.  Can miss URLs in
        // pathological situations.

        Reader rdr = new BufferedReader(StringUtil.getLineReader(in, encoding));
        rdr = StringUtil.getLineContinuationReader(rdr);
        StringBuilder sb = new StringBuilder(maxBuf);
        int shift = Math.min(overlap, maxBuf / 2);

        try {
            while (StringUtil.fillFromReader(rdr, sb, maxBuf - sb.length())) {
                Matcher m1 = CSS_URL_PAT.matcher(sb);
                while (m1.find()) {
                    String url = processUrlEscapes(m1.group(2));
                    if (!StringUtil.isNullString(url)) {
                        try {
                            String resolved = UrlUtil.resolveUri(baseUrl, url);
                            if (log.isDebug2()) {
                                log.debug2("Found " + url + " which resolves to " + resolved);
                            }
                            cb.foundLink(resolved);
                        } catch (MalformedURLException e) {
                            log.siteError("Resolving " + url + " in CSS at " + baseUrl
                                    + ": " + e.toString());
                        }
                    }
                }
                int sblen = sb.length();
                if (sblen < maxBuf) {
                    break;
                }
                // Move the overlap amount to the beginning of the buffer
                sb.delete(0, sblen - shift);
            }
        } finally {
            IOUtil.safeClose(rdr);
        }
    }

    // Remove backslashes when used as escape character in CSS URL
    // Should probably also process hex URL encodings
    String processUrlEscapes(String url) {
        Matcher m2 = CSS_BACKSLASH_PAT.matcher(url);
        return m2.replaceAll("$1");
    }
}

