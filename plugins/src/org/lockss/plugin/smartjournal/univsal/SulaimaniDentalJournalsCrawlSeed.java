/*

Copyright (c) 2000-2026, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.smartjournal.univsal;

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

/**
 * CrawlSeed for SmartJournal platform (sdj.univsul.edu.iq).
 *
 * The all-issues page lists every issue across all volumes as a flat list
 * of cards. There is no volume-level manifest page on this platform.
 * This seed fetches all-issues, extracts all issue URLs belonging to the
 * target volume via SmartJournalVolumeIssueLinkExtractor, then stores
 * them as a synthetic LOCKSS manifest page at:
 *   {base_url}lockss-generated/{volume}
 *
 * The crawler then uses that manifest as its start URL (au_start_url),
 * walking: manifest -> issue pages -> article pages.
 *
 * All-issues URL: https://sdj.univsul.edu.iq/all-issues
 * Issue link pattern found on page: <a href="/issue?id=25">View issue</a>
 * Volume label pattern: <h5 class="card-title ...">Vol. 12 No. 1 (2025) : SDJ</h5>
 */
public class SulaimaniDentalJournalsCrawlSeed extends BaseCrawlSeed {

    private static final Logger log =
            Logger.getLogger(SulaimaniDentalJournalsCrawlSeed.class);

    protected Crawler.CrawlerFacade facade;
    protected List<String> urlList;

    protected String baseUrl;
    protected String allIssuesUrl;
    protected String volumeId;

    public SulaimaniDentalJournalsCrawlSeed(Crawler.CrawlerFacade facade) {
        super(facade);
        if (au == null) {
            throw new IllegalArgumentException(
                    "Valid archival unit required for crawl seed");
        }
        this.facade = facade;
    }

    @Override
    protected void initialize()
            throws ConfigurationException, PluginException, IOException {
        super.initialize();
        this.baseUrl = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
        this.volumeId = au.getConfiguration().get("volume");
        this.allIssuesUrl = this.baseUrl + "all-issues";
        this.urlList = null;
    }

    @Override
    public Collection<String> doGetStartUrls() throws PluginException, IOException {
        if (urlList == null) {
            populateUrlList();
        }
        if (urlList.isEmpty()) {
            throw new CacheException.UnexpectedNoRetryFailException(
                    "Found no start urls for volume=" + volumeId);
        }
        return urlList;
    }

    protected void populateUrlList() throws IOException {

        urlList = new ArrayList<String>();

        // Synthetic manifest page stored in the AU for crawl tracking
        String storeUrl = String.format("%slockss-generated/%s",
                this.baseUrl, this.volumeId);

        log.debug2("Fetching all-issues page: " + allIssuesUrl);

        LinkExtractor ple =
                new SulaimaniDentalJournalsVolumeIssueLinkExtractor(null);

        UrlFetcher uf = makeApiUrlFetcher(
                (SulaimaniDentalJournalsVolumeIssueLinkExtractor) ple,
                allIssuesUrl);

        facade.getCrawlerStatus().addPendingUrl(allIssuesUrl);

        UrlFetcher.FetchResult fr = null;
        try {
            fr = uf.fetch();
        } catch (CacheException ce) {
            if (ce.getCause() != null
                    && ce.getCause().getMessage().contains("LOCKSS")) {
                log.debug("Result errored due to LOCKSS audit proxy. " +
                        "Trying alternate start URL", ce);
                urlList.add(allIssuesUrl);
                return;
            } else {
                log.debug2("Stopping due to fatal CacheException", ce);
                Throwable cause = ce.getCause();
                if (cause != null && IOException.class.equals(cause.getClass())) {
                    throw (IOException) cause;
                } else {
                    throw ce;
                }
            }
        }

        if (fr == UrlFetcher.FetchResult.FETCHED) {
            facade.getCrawlerStatus().removePendingUrl(allIssuesUrl);
            facade.getCrawlerStatus().signalUrlFetched(allIssuesUrl);
        } else {
            log.debug2("Stopping due to fetch result " + fr);
            Map<String, String> errors =
                    facade.getCrawlerStatus().getUrlsWithErrors();
            if (errors.containsKey(allIssuesUrl)) {
                errors.put(allIssuesUrl, errors.remove(allIssuesUrl));
            } else {
                facade.getCrawlerStatus().signalErrorForUrl(
                        allIssuesUrl, "Cannot fetch seed URL");
            }
            throw new CacheException("Cannot fetch seed URL");
        }

        Collections.sort(urlList);
        storeStartUrls(urlList, storeUrl);
    }

    protected UrlFetcher makeApiUrlFetcher(
            final SulaimaniDentalJournalsVolumeIssueLinkExtractor ple,
            final String url) {

        UrlFetcher uf = facade.makeUrlFetcher(url);

        BitSet permFetchFlags = uf.getFetchFlags();
        permFetchFlags.set(UrlCacher.REFETCH_FLAG);
        uf.setFetchFlags(permFetchFlags);

        uf.setUrlConsumerFactory(new UrlConsumerFactory() {
            @Override
            public UrlConsumer createUrlConsumer(
                    Crawler.CrawlerFacade ucfFacade, FetchedUrlData ucfFud) {
                return new SimpleUrlConsumer(ucfFacade, ucfFud) {
                    @Override
                    public void consume() throws IOException {
                        final Set<String> partial = new HashSet<String>();
                        try {
                            String au_cset =
                                    AuUtil.getCharsetOrDefault(fud.headers);
                            String cset = CharsetUtil.guessCharsetFromStream(
                                    fud.input, au_cset);
                            if (cset == null) {
                                cset = au_cset;
                            }
                            ple.extractUrls(au, fud.input, cset, url,
                                    new LinkExtractor.Callback() {
                                        @Override
                                        public void foundLink(String issueUrl) {
                                            log.debug3("Issue URL found: "
                                                    + issueUrl);
                                            partial.add(issueUrl);
                                        }
                                    });
                        } catch (IOException ioe) {
                            log.debug2("Link extractor threw", ioe);
                            throw new IOException(
                                    "Error while parsing all-issues page for "
                                            + url, ioe);
                        } catch (PluginException e) {
                            throw new RuntimeException(e);
                        } finally {
                            log.debug2(String.format(
                                    "Step ending with %d URLs", partial.size()));
                            if (log.isDebug3()) {
                                log.debug3("URLs from step: " + partial);
                            }
                            urlList.addAll(partial);
                        }
                    }
                };
            }
        });
        return uf;
    }

    protected void storeStartUrls(Collection<String> urlList, String url)
            throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>\n");
        for (String u : urlList) {
            log.debug3("Start URL: " + u);
            sb.append("<a href=\"" + u + "\">" + u + "</a><br/>\n");
        }
        sb.append("</html>");
        CIProperties headers = new CIProperties();
        headers.setProperty("content-type", "text/html; charset=utf-8");
        UrlData ud = new UrlData(
                new ByteArrayInputStream(
                        sb.toString().getBytes(Constants.ENCODING_UTF_8)),
                headers, url);
        UrlCacher cacher = facade.makeUrlCacher(ud);
        cacher.storeContent();
    }

    @Override
    public boolean isFailOnStartUrlError() {
        return false;
    }
}
