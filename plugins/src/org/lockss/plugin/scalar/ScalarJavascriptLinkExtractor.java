package org.lockss.plugin.scalar;

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

public class ScalarJavascriptLinkExtractor implements LinkExtractor {

    private static final Logger log = Logger.getLogger(ScalarJavascriptLinkExtractor.class);

    private static final int MAX_URL_LENGTH = 2100;
    private static final int DEFAULT_MAX_BUF = 32 * 1024;
    private static final int DEFAULT_OVERLAP = 2 * 1024;

    private static final String BASE_URI = "system/application/views/arbors/html5_RDFa";
    private static final String WIDGET_URI = "system/application/views/widgets";

    /*
    base_uri+'/js/jquery.rdfquery.rules-1.0.js',
    base_uri+'/js/jquery.RDFa.js',
    base_uri+'/js/form-validation.js?v=2',
    widgets_uri+'/nav/jquery.scalarrecent.js',
    widgets_uri+'/cookie/jquery.cookie.js',
    widgets_uri+'/api/scalarapi.js'
    widgets_uri+'/cookie/jquery.cookie.js'
    widgets_uri+'/notice/jquery.scalarnotice.js'
    widgets_uri+'/spinner/spin.min.js'
	widgets_uri+'/d3/d3.min.js'
	widgets_uri+'/mediaelement/annotorious.debug.js',
	widgets_uri+'/mediaelement/css/annotorious.css',
	widgets_uri+'/mediaelement/mediaelement.css',
	widgets_uri+'/mediaelement/jquery.mediaelement.js'
	widgets_uri+'/replies/replies.js'


    base_uri = "http://blackquotidian.supdigital.org/system/application/views/arbors/html5_RDFa"
    system_uri = "http://blackquotidian.supdigital.org/system"
    widgets_uri = "http://blackquotidian.supdigital.org/system/application/views/widgets"

     */

    // pattern to get embedded images for tutorial image model
    private static final String BASE_URI_IMAGE_SRC = "base_uri\\+'(.*)'";
    private static Pattern BASE_URI_IMAGE_SRC_PAT =
            Pattern.compile(BASE_URI_IMAGE_SRC);

    // pattern to get embedded json for time slider model
    private static final String WIDGET_URI_SRC = "widgets_uri\\+'(.*)'";
    private static Pattern WIDGET_URI_SRC_PAT =
            Pattern.compile(WIDGET_URI_SRC);

    private int maxBuf = DEFAULT_MAX_BUF;

    private int overlap = DEFAULT_OVERLAP;

    public ScalarJavascriptLinkExtractor() {
    }

    public ScalarJavascriptLinkExtractor(int maxBuf, int overlap) {
        this.maxBuf = maxBuf;
        this.overlap = overlap;
    }

    /* Inherit documentation */
    public void extractUrls(ArchivalUnit au,
                            InputStream in,
                            String encoding,
                            String srcUrl,
                            Callback cb)
            throws IOException {

        log.debug3("Fei - JS Parsing " + srcUrl + ", enc " + encoding);

        if (in == null) {
            throw new IllegalArgumentException("Called with null InputStream");
        }
        if (cb == null) {
            throw new IllegalArgumentException("Called with null callback");
        }

        // Handle "read.js" case
        if (srcUrl.contains("main.js")) {

            String base = srcUrl.substring(0,(srcUrl.indexOf(".org/") + 4));
            log.debug3("Fei - baseUrl = " + base);

            URL baseUrl = new URL(base + BASE_URI);
            URL widgetUrl = new URL(base + WIDGET_URI);
            log.debug3("Fei - found main.js");

            Reader rdr = new BufferedReader(StringUtil.getLineReader(in, encoding));
            rdr = StringUtil.getLineContinuationReader(rdr);
            StringBuilder sb = new StringBuilder(maxBuf);
            int shift = Math.min(overlap, maxBuf / 2);

            try {
                while (StringUtil.fillFromReader(rdr, sb, maxBuf - sb.length())) {
                    // handle "/image/tutorial/*.png"
                    Matcher baseUriPatternMatch = BASE_URI_IMAGE_SRC_PAT.matcher(sb);
                    log.debug3("Fei - main.js before baseUriPatternMatch match");
                    while (baseUriPatternMatch.find()) {
                        String url = baseUriPatternMatch.group(1);
                        if (!StringUtil.isNullString(url)) {
                            //String resolved = UrlUtil.resolveUri(baseUrl, url);
                            String resolved = baseUrl + url;
                            log.debug3("Fei - mainjs, Found base uri: " + url + " which resolves to " + resolved);
                            cb.foundLink(resolved);
                        }
                    }

                    // handle "/api/*.json"
                    Matcher widgetUriPatternMatch = WIDGET_URI_SRC_PAT.matcher(sb);
                    while (widgetUriPatternMatch.find()) {
                        String url = widgetUriPatternMatch.group(1);
                        log.debug3(" Fei - json url = " + url);
                        if (!StringUtil.isNullString(url)) {
                            String resolved = widgetUrl + url;
                            log.debug3("Fei - mainjs, Found widget uri " + url + " which resolves to " + resolved);
                            cb.foundLink(resolved);

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
