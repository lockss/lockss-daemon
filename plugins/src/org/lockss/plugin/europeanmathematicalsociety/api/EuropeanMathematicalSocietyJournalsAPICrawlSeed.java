/*

Copyright (c) 2000-2020, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.plugin.europeanmathematicalsociety.api;

import org.lockss.crawler.BaseCrawlSeed;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.Crawler;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.plugin.UrlCacher;
import org.lockss.plugin.UrlData;
import org.lockss.plugin.UrlFetcher;
import org.lockss.util.CIProperties;
import org.lockss.util.Constants;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;
import org.lockss.util.urlconn.CacheException;
import org.lockss.plugin.base.SimpleUrlConsumer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import org.lockss.extractor.LinkExtractor;
import org.lockss.plugin.*;
import org.lockss.util.*;



public class EuropeanMathematicalSocietyJournalsAPICrawlSeed extends BaseCrawlSeed {

    private static final Logger log = Logger.getLogger(EuropeanMathematicalSocietyJournalsAPICrawlSeed.class);

    //https://content.ems.press/serial-issues?filter[serial]=11&filter[year]=2018
    
    protected Crawler.CrawlerFacade facade;

    protected List<String> urlList;

    protected String baseUrl;
    protected String apiUrl;

    protected String journalID;
    protected String journalSerialNumber;
    protected int year;

    public EuropeanMathematicalSocietyJournalsAPICrawlSeed(Crawler.CrawlerFacade facade) {
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
        this.apiUrl = au.getConfiguration().get("api_url");
        this.journalID = au.getConfiguration().get(ConfigParamDescr.JOURNAL_ID.getKey());
        this.journalSerialNumber = au.getConfiguration().get("journal_serial_number");
        this.year = Integer.parseInt(au.getConfiguration().get(ConfigParamDescr.YEAR.getKey()));

        this.urlList = null;
    }

    @Override
    public Collection<String> doGetStartUrls() throws PluginException, IOException {
        if (urlList == null) {
            populateUrlList();
        }
        if (urlList.isEmpty()) {
            throw new CacheException.UnexpectedNoRetryFailException("Found no start urls");
        }
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

        urlList = new ArrayList<String>();

        String storeUrl = String.format("%slockss-generated?journal_id=%s&year=%d", this.baseUrl, this.journalID, this.year);

        String apiStartUrl = String.format("%sserial-issues?filter[serial]=%s&filter[year]=%d",
                this.apiUrl, 
                this.journalSerialNumber,
                this.year);

        log.debug3("EuropeanMathematicalSocietyJournalsAPICrawlSeed: storeUrl = " + storeUrl + ", apiStartUrl: " + apiStartUrl);

        EuropeanMathematicalSocietyJournalsJsonLinkExtractor ple = new EuropeanMathematicalSocietyJournalsJsonLinkExtractor();

        // Make a URL fetcher
        UrlFetcher uf = makeApiUrlFetcher(ple, apiStartUrl, apiStartUrl);

        // Set refetch flag
        BitSet permFetchFlags = uf.getFetchFlags();
        permFetchFlags.set(UrlCacher.REFETCH_FLAG);
        uf.setFetchFlags(permFetchFlags);

        // Make request
        UrlFetcher.FetchResult fr = null;
        try {
            fr = uf.fetch();
            log.debug3("EuropeanMathematicalSocietyJournalsAPICrawlSeed: uf.fetch");
        } catch (CacheException ce) {
            if (ce.getCause() != null) {
                log.debug3("EuropeanMathematicalSocietyJournalsAPICrawlSeed: ce getCause", ce);
                return;
            } else {
                log.debug2("EuropeanMathematicalSocietyJournalsAPICrawlSeed: Stopping due to fatal CacheException", ce);
                Throwable cause = ce.getCause();
                if (cause != null && IOException.class.equals(cause.getClass())) {
                    throw (IOException) cause;
                } else {
                    throw ce;
                }
            }
        }
        if (fr == UrlFetcher.FetchResult.FETCHED) {
            facade.getCrawlerStatus().removePendingUrl(apiStartUrl);
            facade.getCrawlerStatus().signalUrlFetched(apiStartUrl);

            Collections.sort(urlList);
            storeStartUrls(urlList, storeUrl);

            log.debug3("EuropeanMathematicalSocietyJournalsAPICrawlSeed: add to UrlList: " + apiStartUrl);
            urlList.add(apiStartUrl);

        } else {
            log.debug2("EuropeanMathematicalSocietyJournalsAPICrawlSeed: Stopping due to fetch result " + apiStartUrl);

            Map<String, String> errors = facade.getCrawlerStatus().getUrlsWithErrors();
            if (errors.containsKey(apiStartUrl)) {
                errors.put(apiStartUrl, errors.remove(apiStartUrl));
            } else {
                facade.getCrawlerStatus().signalErrorForUrl(apiStartUrl, "EuropeanMathematicalSocietyJournalsAPICrawlSeed: Cannot fetch seed URL");
            }
            throw new CacheException("EuropeanMathematicalSocietyJournalsAPICrawlSeed: Cannot fetch seed URL");

        }
    }

    protected UrlFetcher makeApiUrlFetcher( final EuropeanMathematicalSocietyJournalsJsonLinkExtractor ple,
                                            final String url,
                                            final String apiSingleDoiAPIUrl) {
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
                        final Set<String> partial = new HashSet<String>();
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
                                        public void foundLink(String doiUrl) {
                                            log.debug3("doiUrl is added = " + doiUrl);
                                            if (doiUrl.contains("issues")) {
                                                partial.add(doiUrl);
                                            }
                                        }
                                    });
                        }
                        catch (IOException | PluginException ioe) {
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



    protected void storeStartUrls(Collection<String> urlList, String url) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>\n");
        for (String u : urlList) {
            log.debug3("EuropeanMathematicalSocietyJournalsAPICrawlSeed: Issue link is :"  + u);
            sb.append("<a href=\"" + u + "\">" + u + "</a><br/>\n");
        }
        sb.append("</html>");
        CIProperties headers = new CIProperties();
        //Should use a constant here
        headers.setProperty("content-type", "text/html; charset=utf-8");
        UrlData ud = new UrlData(new ByteArrayInputStream(sb.toString().getBytes(Constants.ENCODING_UTF_8)), headers, url);
        UrlCacher cacher = facade.makeUrlCacher(ud);
        cacher.storeContent();
    }

}
