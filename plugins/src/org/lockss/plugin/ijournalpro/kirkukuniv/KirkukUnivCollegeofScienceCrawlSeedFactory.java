package org.lockss.plugin.ijournalpro.kirkukuniv;

import org.lockss.crawler.CrawlSeed;
import org.lockss.crawler.CrawlSeedFactory;
import org.lockss.daemon.Crawler;

public class KirkukUnivCollegeofScienceCrawlSeedFactory implements CrawlSeedFactory {

    @Override
    public CrawlSeed createCrawlSeed(Crawler.CrawlerFacade facade) {
        return new KirkukUnivCollegeofScienceCrawlSeed(facade);
    }

}

