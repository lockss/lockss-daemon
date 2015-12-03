package org.lockss.plugin.pensoft.oai;

import org.lockss.crawler.CrawlSeed;
import org.lockss.crawler.CrawlSeedFactory;
import org.lockss.daemon.Crawler;

public class PensoftOaiCrawlSeedFactory implements CrawlSeedFactory {
	@Override
	public CrawlSeed createCrawlSeed(Crawler.CrawlerFacade crawlFacade) {
		return new PensoftOaiCrawlSeed(crawlFacade);
	}

}
