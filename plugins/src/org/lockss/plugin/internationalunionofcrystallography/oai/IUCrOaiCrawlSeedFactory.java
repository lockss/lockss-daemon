package org.lockss.plugin.internationalunionofcrystallography.oai;

import org.lockss.crawler.CrawlSeed;
import org.lockss.crawler.CrawlSeedFactory;
import org.lockss.daemon.Crawler;

public class IUCrOaiCrawlSeedFactory implements CrawlSeedFactory {
	@Override
	public CrawlSeed createCrawlSeed(Crawler.CrawlerFacade crawlFacade) {
		return new IUCrOaiCrawlSeed(crawlFacade);
	}

}
