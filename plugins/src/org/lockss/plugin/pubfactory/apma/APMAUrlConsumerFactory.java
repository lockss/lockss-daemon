package org.lockss.plugin.pubfactory.apma;

import org.lockss.daemon.Crawler;
import org.lockss.plugin.FetchedUrlData;
import org.lockss.plugin.UrlConsumer;
import org.lockss.plugin.UrlConsumerFactory;
import org.lockss.plugin.base.HttpToHttpsUrlConsumer;
import org.lockss.util.Logger;

import java.util.regex.Pattern;

/*

 */
public class APMAUrlConsumerFactory implements UrlConsumerFactory {
    private static final Logger log = Logger.getLogger(APMAUrlConsumerFactory.class);

    protected static final String ORIG_PDF_STRING = "/article-pdf/[^.]+\\.pdf";
    protected static final Pattern origPdfPat = Pattern.compile(ORIG_PDF_STRING, Pattern.CASE_INSENSITIVE);


    @Override
    public UrlConsumer createUrlConsumer(Crawler.CrawlerFacade facade, FetchedUrlData fud) {
        log.debug3("Creating a UrlConsumer");
        return new SilverchairCommonThemeUrlConsumer(facade, fud);
    }

    public class SilverchairCommonThemeUrlConsumer extends HttpToHttpsUrlConsumer {

        public SilverchairCommonThemeUrlConsumer(Crawler.CrawlerFacade facade,
                                FetchedUrlData fud) {
            super(facade, fud);
        }

        @Override
        public boolean shouldStoreAtOrigUrl() {
            // first just check for simple http to https transition
            boolean should = super.shouldStoreAtOrigUrl();

            log.debug3("origUrl = " + fud.origUrl);
            if ((!should) && (fud.redirectUrls != null && fud.redirectUrls.size() >=1)) {

                should =  origPdfPat.matcher(fud.origUrl).find();

            }
            return should;
        }
    }
}


