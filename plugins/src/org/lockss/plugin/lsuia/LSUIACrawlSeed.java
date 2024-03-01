package org.lockss.plugin.lsuia;

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
import org.lockss.util.urlconn.CacheException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

public class LSUIACrawlSeed extends BaseCrawlSeed {

    private static final Logger log = Logger.getLogger(LSUIACrawlSeed.class);
    
    //"https://sls-journal.com.ua/en/archive", 
    //Need to create link ourselves, like this: https://sls-journal.com.ua/en/journals/tom-6-4-2023

    protected Crawler.CrawlerFacade facade;

    protected List<String> urlList;

    protected String baseUrl;
    protected int year;
    protected String volumeName;


    public LSUIACrawlSeed(Crawler.CrawlerFacade facade) {
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
        this.year = Integer.parseInt(au.getConfiguration().get(ConfigParamDescr.YEAR.getKey()));
        this.volumeName = au.getConfiguration().get(ConfigParamDescr.VOLUME_NAME.getKey());

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

        for (int i = 1; i <= 100; i++) {

            String apiStartUrl = String.format("%sen/journals/tom-%s-%d-%d",
                    this.baseUrl,
                    this.volumeName,
                    i,
                    this.year);

            log.debug3("LSUIACrawlSeed: apiStartUrl: " + apiStartUrl);

            // Make a URL fetcher
            UrlFetcher uf = facade.makeUrlFetcher(apiStartUrl);

            // Set refetch flag
            BitSet permFetchFlags = uf.getFetchFlags();
            permFetchFlags.set(UrlCacher.REFETCH_FLAG);
            uf.setFetchFlags(permFetchFlags);

            // Make request
            UrlFetcher.FetchResult fr = null;
            try {
                fr = uf.fetch();
                log.debug3("LSUIACrawlSeed: uf.fetch");
            } catch (CacheException ce) {
                if (ce.getCause() != null) {
                    log.debug3("LSUIACrawlSeed: ce getCause", ce);
                    return;
                } else {
                    log.debug2("LSUIACrawlSeed: Stopping due to fatal CacheException", ce);
                    break;
                    /*
                    Throwable cause = ce.getCause();
                    if (cause != null && IOException.class.equals(cause.getClass())) {
                        throw (IOException) cause;
                    } else {
                        throw ce;
                    }
                     */
                }
            }
            if (fr == UrlFetcher.FetchResult.FETCHED) {
                facade.getCrawlerStatus().removePendingUrl(apiStartUrl);
                facade.getCrawlerStatus().signalUrlFetched(apiStartUrl);

                Collections.sort(urlList);
                storeStartUrls(urlList, apiStartUrl);

                log.debug3("LSUIACrawlSeed: add to UrlList: " + apiStartUrl);
                urlList.add(apiStartUrl);
                
            } else {
                log.debug2("LSUIACrawlSeed: Stopping due to fetch result " + apiStartUrl);
                break;
                /*
                Map<String, String> errors = facade.getCrawlerStatus().getUrlsWithErrors();
                if (errors.containsKey(apiStartUrl)) {
                    errors.put(apiStartUrl, errors.remove(apiStartUrl));
                } else {
                    facade.getCrawlerStatus().signalErrorForUrl(apiStartUrl, "LSUIACrawlSeed: Cannot fetch seed URL");
                }
                throw new CacheException("LSUIACrawlSeed: Cannot fetch seed URL");
                 */
            }
        }
    }
    
    protected void storeStartUrls(Collection<String> urlList, String url) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>\n");
        for (String u : urlList) {
            log.debug3("LSUIACrawlSeed: Issue link is :"  + u);
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
