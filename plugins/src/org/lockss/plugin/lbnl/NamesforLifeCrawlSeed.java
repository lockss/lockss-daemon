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
import java.util.regex.*;
import java.util.stream.Stream;

public class NamesforLifeCrawlSeed extends BaseCrawlSeed {

    private static final Logger log = Logger.getLogger(NamesforLifeCrawlSeed.class);
    
    protected Crawler.CrawlerFacade facade;

    protected List<String> urlList;

    protected String baseUrl;
    
    protected Collection<String> sitemaps;
    
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
        sitemaps = Arrays.asList(
            baseUrl + "sitemap-name-information-objects.xml",
            baseUrl + "sitemap-taxon-information-objects.xml",
            baseUrl + "sitemap-exemplar-information-objects.xml"
        );

        for (String startUrl : sitemaps) {
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
          Stream<String> filteredAndSorted(Collection<String> coll) {
            Pattern pat = Pattern.compile(baseUrl + "10\\.1601/" + abbr + "\\.(\\d+)", Pattern.CASE_INSENSITIVE);
            class StrComparator implements Comparator<String> {
              public int compare(String x, String y) {
                Matcher m1 = pat.matcher(x);
                Matcher m2 = pat.matcher(y);
                if (m1.matches() && m2.matches()) {
                  return Integer.compare(Integer.valueOf(m1.group(1)), Integer.valueOf(m2.group(1)));
                }
                throw new RuntimeException(String.format("Invalid comparison: <%s> vs. <%s>", x, y));
              };
            }
            return coll.stream().filter(pat.asPredicate()).sorted(new StrComparator());
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
        sb.append("    <style>\n");

        sb.append("/* Inspired by https://www.w3schools.com/css/css_navbar_horizontal.asp */\n"
            + "\n"
            + "ul.nav {\n"
            + "  list-style-type: none;\n"
            + "  margin: 0;\n"
            + "  padding: 0;\n"
            + "  overflow: hidden;\n"
            + "  background-color: #d4edda;\n"
            + "}\n"
            + "\n"
            + "ul.nav li {\n"
            + "  float: left;\n"
            + "}\n"
            + "\n"
            + "ul.nav a {\n"
            + "  display: block;\n"
            + "  color: #155724;\n"
            + "  text-align: center;\n"
            + "  padding: 14px 16px;\n"
            + "  text-decoration: none;\n"
            + "}\n"
            + "\n"
            + "ul.nav a:hover {\n"
            + "  color: #eeeeee;\n"
            + "  background-color: #111111;\n"
            + "}\n");
        
        sb.append("    </style>\n");
        sb.append("  </head>\n");
        sb.append("  <body>\n");
        sb.append("    <p><a href=\"https://www.namesforlife.com/\"><img width=\"256\" height=\"64\" alt=\"NamesforLife logo\" src=\"https://www.namesforlife.com/images/namesforlife_logo_name_tagline_256x64.svg\" /></a></p>\n");
        sb.append("    <nav id=\"nav\">\n");
        sb.append("      <ul class=\"nav\">\n");
        sb.append("        <li><a href=\"https://www.namesforlife.com/\">Home</a></li>\n");
        for (Thing t : things) {
          sb.append(String.format("        <li><a href=\"#%s\">%s</a></li>\n", t.htmlId, t.fullName));
        }
        sb.append("      </ul>\n");
        sb.append("    </nav>\n");
        sb.append("    <h1>NamesforLife</h1>\n");
        sb.append("    <p>NamesforLife was a privately held U.S. company providing semantic services to the scientific and technical publishing industry. The company ceased operations effective December 31, 2022. The NamesforLife, LLC Intellectual Property Portfolio has been acquired by Lawrence Berkeley National Laboratory.</p>\n");

        for (Thing t : things) {
          sb.append(String.format("    <h2 id=\"%s\">%s</h2>\n", t.htmlId, t.fullName));
          sb.append("    <ul>\n");
          t.filteredAndSorted(urlList).forEach(u -> {
            sb.append(String.format("      <li><a href=\"%s\">%s</a></li>\n", u, u));
          });
          sb.append("    </ul>\n");
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
