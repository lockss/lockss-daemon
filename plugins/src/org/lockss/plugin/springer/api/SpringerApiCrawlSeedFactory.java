package org.lockss.plugin.springer.api;

import org.lockss.crawler.CrawlSeed;
import org.lockss.crawler.CrawlSeedFactory;
import org.lockss.crawler.IdentifierListOaiPmhCrawlSeed;
import org.lockss.daemon.Crawler;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;

public class SpringerApiCrawlSeedFactory implements CrawlSeedFactory {
	@Override
	public CrawlSeed createCrawlSeed(Crawler.CrawlerFacade crawlFacade) {
		return new SpringerApiCrawlSeed(crawlFacade);
	}

}
