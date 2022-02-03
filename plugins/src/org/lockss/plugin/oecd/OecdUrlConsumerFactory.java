package org.lockss.plugin.oecd;

import org.lockss.daemon.Crawler;
import org.lockss.plugin.FetchedUrlData;
import org.lockss.plugin.UrlConsumer;
import org.lockss.plugin.UrlConsumerFactory;
import org.lockss.plugin.base.SimpleUrlConsumer;
import org.lockss.util.Logger;

import java.io.IOException;

public class OecdUrlConsumerFactory implements UrlConsumerFactory {
  private static final Logger log = Logger.getLogger(OecdUrlConsumerFactory.class);
  /*
  https://www.oecd-ilibrary.org/frequency-based-co-movement-of-inflation-in-selected-euro-area-countries_5jm26ttlxdd1.pdf?itemId=%2Fcontent%2Fpaper%2Fjbcma-2015-5jm26ttlxdd1&mimeType=pdf
  https://www.oecd-ilibrary.org/docserver/jbcma-2015-5jm26ttlxdd1.pdf?expires=1643909821&id=id&accname=ocid194777&checksum=DD36836B974F98914B836656A0A287E4
   */

  private static final String DOC_SERVER_REDIRECT = "/docserver/";
  private static final String KXCDN_DOMAIN = "kxcdn.com/";

  @Override
  public UrlConsumer createUrlConsumer(Crawler.CrawlerFacade crawlFacade,
                                       FetchedUrlData fud) {
    return new OecdUrlConsumer(crawlFacade, fud);
  }

  public class OecdUrlConsumer extends SimpleUrlConsumer {

    public OecdUrlConsumer(Crawler.CrawlerFacade facade, FetchedUrlData fud) {
      super(facade, fud);
    }

    @Override
    public void consume() throws IOException {
      if (shouldStoreAtOrigUrl()) {
        storeAtOrigUrl();
      }
      super.consume();
    }

    public boolean shouldStoreAtOrigUrl() throws IOException {
      boolean should = false;
    	if (fud.redirectUrls != null && (fud.redirectUrls.size() > 0)) {
    		// if redirected to the docserver or kxcdn domain (zip files) we dont need both
    		if (fud.fetchUrl.contains(DOC_SERVER_REDIRECT) ||
            fud.fetchUrl.contains(KXCDN_DOMAIN)
        ) {
    			should = true;
    		}
    	}
      return should;
    }
  }
}
