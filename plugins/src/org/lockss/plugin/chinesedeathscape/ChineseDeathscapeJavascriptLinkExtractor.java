package org.lockss.plugin.chinesedeathscape;

import org.lockss.extractor.LinkExtractor;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChineseDeathscapeJavascriptLinkExtractor implements LinkExtractor {

    private static final Logger log = Logger.getLogger("ChineseDeathscapeJavascriptLinkExtractor");

    private static final int MAX_URL_LENGTH = 2100;
    private static final int DEFAULT_MAX_BUF = 32 * 1024;
    private static final int DEFAULT_OVERLAP = 2 * 1024;

    // pattern to get extra js file
    private static final String JS_URI_EXTRACTOR =
            "(https?://[/+])?/javascripts/(.*)";
    private static Pattern JS_URL_PAT = Pattern.compile(JS_URI_EXTRACTOR);

    // pattern to get embedded images for tutorial image model
    private static final String READ_IS_IMAGE_SRC = "src:\"(/images/tutorial/[^./]+.png)\"";
    private static Pattern READ_IS_IMAGE_SRC_PAT =
            Pattern.compile(READ_IS_IMAGE_SRC);

    // pattern to get embedded json for time slider model
    private static final String API_JSON_SRC = "getJSON\\(\"(/api/[^./]+.json)\"";
    private static Pattern API_JSON_SRC_PAT =
            Pattern.compile(API_JSON_SRC);

    private int maxBuf = DEFAULT_MAX_BUF;

    private int overlap = DEFAULT_OVERLAP;

    public ChineseDeathscapeJavascriptLinkExtractor() {
    }

    public ChineseDeathscapeJavascriptLinkExtractor(int maxBuf, int overlap) {
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

        log.debug3("Fei - JS Parsing " + srcUrl + ", enc " + encoding);

        if (in == null) {
            throw new IllegalArgumentException("Called with null InputStream");
        }
        if (cb == null) {
            throw new IllegalArgumentException("Called with null callback");
        }

        // Handle "read.js" case
        if (srcUrl.contains("read.js")) {

            URL baseUrl = new URL(srcUrl);
            log.debug3("Fei - found read.js");

            Reader rdr = new BufferedReader(StringUtil.getLineReader(in, encoding));
            rdr = StringUtil.getLineContinuationReader(rdr);
            StringBuilder sb = new StringBuilder(maxBuf);
            int shift = Math.min(overlap, maxBuf / 2);

            try {
                while (StringUtil.fillFromReader(rdr, sb, maxBuf - sb.length())) {
                    // handle "/image/tutorial/*.png"
                    Matcher tutorialImagePatternMatch = READ_IS_IMAGE_SRC_PAT.matcher(sb);
                    log.debug3("Fei - read.js before tutorialImagePatternMatch match");
                    while (tutorialImagePatternMatch.find()) {
                        String url = tutorialImagePatternMatch.group(1);
                        if (!StringUtil.isNullString(url)) {
                            try {
                                String resolved = UrlUtil.resolveUri(baseUrl, url);
                                log.debug3("Fei - readjs, Found tutorial images: " + url + " which resolves to " + resolved);
                                cb.foundLink(resolved);
                            } catch (MalformedURLException e) {
                                log.siteError("Resolving " + url + " in JS at " + baseUrl
                                        + ": " + e.toString());
                            }
                        }
                    }

                    // handle "/api/*.json"
                    Matcher apiJsonPatternMatch = API_JSON_SRC_PAT.matcher(sb);
                    while (apiJsonPatternMatch.find()) {
                        String url = apiJsonPatternMatch.group(1);
                        log.debug3(" Fei - json url = " + url);
                        if (!StringUtil.isNullString(url)) {
                            try {
                                String resolved = UrlUtil.resolveUri(baseUrl, url);
                                log.debug3("Fei - readjs, Found api json " + url + " which resolves to " + resolved);
                                cb.foundLink(resolved);
                            } catch (MalformedURLException e) {
                                log.siteError("Resolving " + url + " in JS at " + baseUrl
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
    }
}
