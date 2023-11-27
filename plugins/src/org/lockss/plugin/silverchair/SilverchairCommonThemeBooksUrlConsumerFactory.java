package org.lockss.plugin.silverchair;

import org.lockss.daemon.Crawler;
import org.lockss.plugin.FetchedUrlData;
import org.lockss.plugin.UrlConsumer;
import org.lockss.plugin.UrlConsumerFactory;
import org.lockss.plugin.base.SimpleUrlConsumer;
import org.lockss.util.Logger;
import java.io.IOException;


public class SilverchairCommonThemeBooksUrlConsumerFactory implements UrlConsumerFactory {
    private static final Logger log = Logger.getLogger(SilverchairCommonThemeBooksUrlConsumerFactory.class);

    @Override
    public UrlConsumer createUrlConsumer(Crawler.CrawlerFacade facade, FetchedUrlData fud) {
        log.debug3("Creating a SilverchairCommonTheme UrlConsumer");
        return new SilverchairCommonThemeBooksUrlConsumer(facade, fud);
    }

    /**
     * <p>
     * A custom URL consumer that identifies specific redirect chains and stores the
     * content at the origin of the chain (e.g. to support collecting and repairing
     * redirect chains that begin with fixed URLs but go through showCaptcha).
     * @since 1.67.5
     */
    public class SilverchairCommonThemeBooksUrlConsumer extends SimpleUrlConsumer {

        /*
        CASE-1, url without "journal_id" redirected to url with "journal_id"
        x-lockss-orig-url: 	https://pubs.geoscienceworld.org/books/book/1811/chapter/107700022/Front-Matter
        x-lockss-redirected-to: 	https://pubs.geoscienceworld.org/clays/books/book/1811/chapter/107700022/Front-Matter
        x-lockss-referrer: 	https://pubs.geoscienceworld.org/clays/books/book/1811/Kaolin-Genesis-and-Utilization

        CASE-2: url without "journal_id" did not get redirected
        x-lockss-node-url: 	https://pubs.geoscienceworld.org/clays/books/book/1811/chapter/107700022/Front-Matter
        x-lockss-orig-url: 	https://pubs.geoscienceworld.org/books/book/1811/chapter/107700022/Front-Matter
        x-lockss-referrer: 	https://pubs.geoscienceworld.org/clays/books/book/1811/Kaolin-Genesis-and-Utilization
         */

        public SilverchairCommonThemeBooksUrlConsumer(Crawler.CrawlerFacade facade,
                                  FetchedUrlData fud) {
            super(facade, fud);
        }

        @Override
        public void consume() throws IOException {
            if (shouldStoreRedirectsAtOrigUrl()) {
                storeAtOrigUrl();
            }
            super.consume();
        }

        protected boolean shouldStoreRedirectsAtOrigUrl() {
            boolean should =  fud.redirectUrls != null
                    && fud.redirectUrls.size() >= 1
                    &&  (fud.origUrl.contains("chapter")
                    || fud.redirectUrls.get(0).contains("Expires=2147483647"));


            if (fud.redirectUrls != null) {
                log.debug3("origUrl = " + fud.origUrl + ", redirect= " + fud.redirectUrls.get(0) + ", should = " + should);
            }
            return should;
        }
    }
}
