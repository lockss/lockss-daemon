package org.lockss.plugin.ijournalpro.univofanbar;

import org.lockss.crawler.CrawlSeed;
import org.lockss.crawler.CrawlSeedFactory;
import org.lockss.daemon.Crawler;

public class UnivofAnbarCollegeofAgCrawlSeedFactory implements CrawlSeedFactory {

    @Override
    public CrawlSeed createCrawlSeed(Crawler.CrawlerFacade facade) {
        return new UnivofAnbarCollegeofAgCrawlSeed(facade);
    }

}

