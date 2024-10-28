/*

Copyright (c) 2000-2024, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.lbnl;

import org.lockss.crawler.BaseCrawlSeed;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.Crawler;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.LinkExtractor;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.plugin.*;
import org.lockss.plugin.base.SimpleUrlConsumer;
import org.lockss.util.CIProperties;
import org.lockss.util.CharsetUtil;
import org.lockss.util.Constants;
import org.lockss.util.Logger;
import org.lockss.util.urlconn.CacheException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class NamesforLifeCrawlSeed extends BaseCrawlSeed {

    private static final Logger log = Logger.getLogger(NamesforLifeCrawlSeed.class);
    
    protected Crawler.CrawlerFacade facade;

    protected List<String> urlList;

    protected String baseUrl;
    
    public NamesforLifeCrawlSeed(Crawler.CrawlerFacade facade) {
        super(facade);
        if (au == null) {
            throw new IllegalArgumentException("Valid archival unit required for crawl seed");
        }
        this.facade = facade;
    }

    @Override
    protected void initialize() throws ConfigurationException, PluginException, IOException {
        super.initialize();
        this.baseUrl = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
        this.urlList = null;
    }

    @Override
    public Collection<String> doGetStartUrls() throws PluginException, IOException {

        populateUrlList();

        if (urlList.isEmpty()) {
            throw new CacheException.UnexpectedNoRetryFailException("Found no start urls");
        }

        Collections.sort(urlList);
        return urlList;
    }

    /**
     * <p>
     * Populates the URL list with start URLs.
     * </p>
     *
     * @throws IOException
     * @since 1.67.5
     */
    protected void populateUrlList() throws IOException {

        log.debug3("===populateUrlList start====");

        urlList = new ArrayList<String>();

        // The three topic sitemaps:
        //   sitemap-name-information-objects.xml
        //   sitemap-taxon-information-objects.xml
        //   sitemap-exemplar-information-objects.xml
        Collection<String> startUrls = Arrays.asList(
            baseUrl + "sitemap-name-information-objects.xml",
            baseUrl + "sitemap-taxon-information-objects.xml",
            baseUrl + "sitemap-exemplar-information-objects.xml"
        );

        for (String startUrl : startUrls) {
            log.debug3("startUrl =  :"  + startUrl);
            processSingleStartUrl(startUrl);
        }

        storeStartUrls();
        
    }


    private void processSingleStartUrl(String startUrl) throws IOException {

        NamesforLifeLocLinkExtractor ple = new NamesforLifeLocLinkExtractor();

        UrlFetcher uf = makeApiUrlFetcher(ple, startUrl);
        log.debug2("Request URL: " + startUrl);
        facade.getCrawlerStatus().addPendingUrl(startUrl);

        // Make request
        UrlFetcher.FetchResult fr = null;
        try {
            fr = uf.fetch();
        }
        catch (CacheException ce) {
            if(ce.getCause() != null && ce.getCause().getMessage().contains("LOCKSS")) {
                log.debug("OAI result errored due to LOCKSS audit proxy. Trying alternate start Url", ce);
                urlList.add(startUrl);
                return;
            } else {
                log.debug2("Stopping due to fatal CacheException", ce);
                Throwable cause = ce.getCause();
                if (cause != null && IOException.class.equals(cause.getClass())) {
                    throw (IOException)cause; // Unwrap IOException
                }
                else {
                    throw ce;
                }
            }
        }
        if (fr == UrlFetcher.FetchResult.FETCHED) {
            facade.getCrawlerStatus().removePendingUrl(startUrl);
            facade.getCrawlerStatus().signalUrlFetched(startUrl);
        }
        else {
            log.debug2("Stopping due to fetch result " + fr);
            Map<String, String> errors = facade.getCrawlerStatus().getUrlsWithErrors();
            if (errors.containsKey(startUrl)) {
                errors.put(startUrl, errors.remove(startUrl));
            }
            else {
                facade.getCrawlerStatus().signalErrorForUrl(startUrl, "Cannot fetch seed URL");
            }
            throw new CacheException("Cannot fetch seed URL");
        }

    }

    /**
     * <p>
     * Makes a URL fetcher for the given API request, that will parse the result
     * using the given NamesforLifeLocLinkExtractor instance.
     * </p>
     *
     * @param ple
     *          A  NamesforLifeLocLinkExtractor instance to parse the API
     *          response with.
     * @param url
     *          A query URL.
     * @return A URL fetcher for the given query URL.
     * @since 1.67.5
     */
    protected UrlFetcher makeApiUrlFetcher(final NamesforLifeLocLinkExtractor ple,
                                           final String url) {
        // Make a URL fetcher
        UrlFetcher uf = facade.makeUrlFetcher(url);

        // Set refetch flag
        BitSet permFetchFlags = uf.getFetchFlags();
        permFetchFlags.set(UrlCacher.REFETCH_FLAG);
        uf.setFetchFlags(permFetchFlags);

        // Set custom URL consumer factory
        uf.setUrlConsumerFactory(new UrlConsumerFactory() {
            @Override
            public UrlConsumer createUrlConsumer(Crawler.CrawlerFacade ucfFacade,
                                                 FetchedUrlData ucfFud) {
                // Make custom URL consumer
                return new SimpleUrlConsumer(ucfFacade, ucfFud) {
                    @Override
                    public void consume() throws IOException {
                        // Apply link extractor to URL and output results into a list
                        final ArrayList<String> partial = new ArrayList<>();
                        try {
                            String au_cset = AuUtil.getCharsetOrDefault(fud.headers);
                            String cset = CharsetUtil.guessCharsetFromStream(fud.input,au_cset);
                            if (cset == null) {
                                cset = au_cset;
                            }
                            ple.extractUrls(au,
                                    fud.input,
                                    cset,
                                    url,
                                    new LinkExtractor.Callback() {
                                        @Override
                                        public void foundLink(String locUrl) {
                                            log.debug3("locUrl is added = " + locUrl);
                                            partial.add(locUrl);
                                        }
                                    });
                        }
                        catch (IOException ioe) {
                            log.debug2("Link extractor threw", ioe);
                            throw new IOException("Error while parsing PAM response for " + url, ioe);
                        }
                        finally {
                            // Logging
                            log.debug2(String.format("Step ending with %d URLs", partial.size()));
                            if (log.isDebug3()) {
                                log.debug3("URLs from step: " + partial.toString());
                            }
                            // Output accumulated URLs to start URL list
                            urlList.addAll(partial);
                        }
                    }
                };
            }
        });
        return uf;
    }

    protected void storeStartUrls() throws IOException {

        class Thing {
          String fullName;
          String htmlId;
          String abbr;
          Thing(String fullName, String htmlId, String abbr) {
            this.fullName = fullName;
            this.htmlId = htmlId;
            this.abbr = abbr;
          }
          Stream<String> filteredAndSorted() {
            Pattern pat = Pattern.compile(baseUrl + "10\\.1601/" + abbr + "\\.(\\d+)", Pattern.CASE_INSENSITIVE);
            return urlList.stream().filter(pat.asPredicate()).sorted((u, v) -> Integer.compare(Integer.valueOf(pat.matcher(u).group(1)),
                                                                                               Integer.valueOf(pat.matcher(v).group(1))));
          }
        }

        Thing names = new Thing("Name Information Objects", "names", "nm");
        Thing taxa = new Thing("Taxon Information Objects", "taxa", "tx");
        Thing exemplars = new Thing("Exemplar Information Objects", "exemplars", "ex");
        List<Thing> things = Arrays.asList(names, taxa, exemplars);
        
        String storageUrl = baseUrl + "lockss-generated/start.html";
        StringBuilder sb = new StringBuilder();
        sb.append("<html>\n");
        sb.append("  <head>\n");
        sb.append("    <title>NamesforLife, LLC - A semantic services company</title>\n");
        sb.append("    <style>");

        sb.append("/* Inspired by https://www.w3schools.com/css/css_navbar_horizontal.asp */\n"
            + "\n"
            + "nav ul {\n"
            + "  list-style-type: none;\n"
            + "  margin: 0;\n"
            + "  padding: 0;\n"
            + "  overflow: hidden;\n"
            + "  background-color: #333;\n"
            + "}\n"
            + "\n"
            + "nav ul li {\n"
            + "  float: left;\n"
            + "}\n"
            + "\n"
            + "nav ul li a {\n"
            + "  display: block;\n"
            + "  color: white;\n"
            + "  text-align: center;\n"
            + "  padding: 14px 16px;\n"
            + "  text-decoration: none;\n"
            + "}\n"
            + "\n"
            + "/* Change the link color to #111 (black) on hover */\n"
            + "li a:hover {\n"
            + "  background-color: #111;\n"
            + "}");
        
        sb.append("    </style>");
        sb.append("  </head>\n");
        sb.append("  <body>\n");
        sb.append("    <p><img width=\"256\" height=\"64\" alt=\"NamesforLife logo\" src=\"https://www.namesforlife.com/images/namesforlife_logo_name_tagline_256x64.svg\" /></p>\n");
        sb.append("    <h1>NamesforLife</h1>\n");
        sb.append("    <nav>\n");
        sb.append("      <ul>\n");
        sb.append("        <li><a href=\"https://www.namesforlife.com/\">Home</a></li>\n");
        for (Thing t : things) {
          sb.append(String.format("        <li><a href=\"#%s\">%s</a></li>\n", t.htmlId, t.fullName));
        }
        sb.append("      </ul>\n");
        sb.append("    </nav>\n");

        for (Thing t : things) {
          sb.append(String.format("    <h2 id=\"#%s\">%s</h2>\n", t.htmlId, t.fullName));
          sb.append("    <ul>");
          t.filteredAndSorted().forEach(u -> {
            sb.append(String.format("      <li><a href=\"%s\">%s</a></li>\n", u, u));
          });
          sb.append("    </ul>");
        }
        
        sb.append("  </body>\n");
        sb.append("</html>\n");

        CIProperties headers = new CIProperties();
        //Should use a constant here
        headers.setProperty("content-type", "text/html; charset=utf-8");
        UrlData ud = new UrlData(new ByteArrayInputStream(sb.toString().getBytes(Constants.ENCODING_UTF_8)), headers, storageUrl);
        UrlCacher cacher = facade.makeUrlCacher(ud);
        cacher.storeContent();
    }

}
