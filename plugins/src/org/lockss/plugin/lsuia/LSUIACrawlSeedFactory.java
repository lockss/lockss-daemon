package org.lockss.plugin.lsuia;

import org.lockss.crawler.CrawlSeed;
import org.lockss.crawler.CrawlSeedFactory;
import org.lockss.daemon.Crawler;
import org.lockss.plugin.spandidos.SpandidosCrawlSeed;

public class LSUIACrawlSeedFactory implements CrawlSeedFactory {

    @Override
    public CrawlSeed createCrawlSeed(Crawler.CrawlerFacade facade) {
        return new LSUIACrawlSeed(facade);
    }

}

