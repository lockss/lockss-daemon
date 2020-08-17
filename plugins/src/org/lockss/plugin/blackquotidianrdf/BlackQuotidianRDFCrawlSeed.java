package org.lockss.plugin.blackquotidianrdf;

import org.lockss.config.Configuration;
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

public class BlackQuotidianRDFCrawlSeed extends BaseCrawlSeed {

    private static final Logger log = Logger.getLogger(BlackQuotidianRDFCrawlSeed.class);

    // Documentation: https://scalar.usc.edu/works/guide2/query-scope?path=working-with-the-api
    //http://blackquotidian.supdigital.org/bq/rdf/instancesof/content - list all the content
    //http://blackquotidian.supdigital.org/bq/rdf/instancesof/page - list all text-based nodes (pages)
    //http://blackquotidian.supdigital.org/bq/rdf/instancesof/path - list all paths.

    protected Crawler.CrawlerFacade facade;

    protected List<String> urlList;

    protected String baseUrl;
    protected String apiSingleNodeUrl;
    protected int year;

    public BlackQuotidianRDFCrawlSeed(Crawler.CrawlerFacade facade) {
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
        this.apiSingleNodeUrl = this.baseUrl;
        
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

        String apiStartUrl = "http://blackquotidian.supdigital.org/bq/rdf/instancesof/content" ;

        log.debug2("apiStartUrl: " + apiStartUrl);

        BlackQuotidianLinkExtractor ple = new BlackQuotidianLinkExtractor();

        UrlFetcher uf = makeApiUrlFetcher(ple, apiStartUrl, this.apiSingleNodeUrl);
        log.debug2("Request URL: " + apiStartUrl);
        facade.getCrawlerStatus().addPendingUrl(apiStartUrl);

        // Make request
        UrlFetcher.FetchResult fr = null;
        try {
            fr = uf.fetch();
        }
        catch (CacheException ce) {
            if(ce.getCause() != null && ce.getCause().getMessage().contains("LOCKSS")) {
                log.debug("RDF result errored due to LOCKSS audit proxy. Trying alternate start Url", ce);
                urlList.add(apiStartUrl);
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
            facade.getCrawlerStatus().removePendingUrl(apiStartUrl);
            facade.getCrawlerStatus().signalUrlFetched(apiStartUrl);
        }
        else {
            log.debug2("Stopping due to fetch result " + fr);
            Map<String, String> errors = facade.getCrawlerStatus().getUrlsWithErrors();
            if (errors.containsKey(apiStartUrl)) {
                errors.put(apiStartUrl, errors.remove(apiStartUrl));
            }
            else {
                facade.getCrawlerStatus().signalErrorForUrl(apiStartUrl, "Cannot fetch seed URL");
            }
            throw new CacheException("Cannot fetch seed URL");
        }

        Collections.sort(urlList);
        storeStartUrls(urlList, apiStartUrl);
    }

    protected UrlFetcher makeApiUrlFetcher( final BlackQuotidianLinkExtractor ple,
                                            final String url,
                                            final String apiSingleNodeUrl) {
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
                                        public void foundLink(String nodeUrl) {
                                            log.debug3("nodeUrl is added = " + nodeUrl);
                                            partial.add(nodeUrl);
                                        }
                                    });
                        }
                        catch (IOException ioe) {
                            log.debug2("Link extractor threw", ioe);
                            throw new IOException("Error while parsing response for " + url, ioe);
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
            log.debug3("SINGLE_node_API_URL is :"  + u);
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
