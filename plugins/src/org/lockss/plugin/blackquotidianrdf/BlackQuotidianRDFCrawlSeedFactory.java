package org.lockss.plugin.blackquotidianrdf;

import org.lockss.crawler.CrawlSeed;
import org.lockss.crawler.CrawlSeedFactory;
import org.lockss.daemon.Crawler;

public class BlackQuotidianRDFCrawlSeedFactory implements CrawlSeedFactory {

    @Override
    public CrawlSeed createCrawlSeed(Crawler.CrawlerFacade facade) {
        return new BlackQuotidianRDFCrawlSeed(facade);
    }

}

