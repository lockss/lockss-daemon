package org.lockss.plugin.dspace;

import org.lockss.crawler.CrawlSeed;
import org.lockss.crawler.CrawlSeedFactory;
import org.lockss.daemon.Crawler;

public class DSpaceCrawlSeedFactory implements CrawlSeedFactory {
	@Override
	public CrawlSeed createCrawlSeed(Crawler.CrawlerFacade crawlFacade) {
		return new DSpaceCrawlSeed(crawlFacade);
	}

}
