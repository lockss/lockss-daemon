package org.lockss.plugin.scalar;

import org.lockss.crawler.CrawlSeed;
import org.lockss.crawler.CrawlSeedFactory;
import org.lockss.daemon.Crawler;

public class ScalarRDFCrawlSeedFactory implements CrawlSeedFactory {

    @Override
    public CrawlSeed createCrawlSeed(Crawler.CrawlerFacade facade) {
        return new ScalarRDFCrawlSeed(facade);
    }

}

