package org.lockss.plugin.silverchair;

import org.lockss.daemon.Crawler;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.plugin.base.HttpToHttpsUrlConsumer;
import org.lockss.plugin.silverchair.ama.AmaScUrlConsumerFactory;
import org.lockss.util.Logger;

import java.util.regex.Pattern;

/*
PDF link redirect to one time url:
https://pubs.geoscienceworld.org/rmg/article-pdf/52/1/1/2315810/1-16.pdf

Location: https://gsw.silverchair-cdn.com/gsw/Content_public/Journal/rmg/52/1/10.24872_rmgjournal.52.1.1/1/1-16.pdf?Expires=1581014679&Signature=let2Z6hadTnDi3xGWwaY1EaMS2knLVY9ro2X02eRB64WRihfinMwNsGMoG9CcnAK8GRcJYRCpFdniYJuwyYuqxjKBAbRN5QKHNTPZgGCKtvgKuczXx-MAfLctW12mJEMR74Yswfyq2LX7zJKroC8Wi0MV5xONFJ4xxyXjTGYNdd1noUBjqkQBksEFKOfnCAjj5KvXlqLZcDGBWS5vXrBKf1xgasxy8AcLb3ey6o1Nc2euXmsX9VA10-tm15LkCuT0nfJFbBKN2EM9q93mo95o1X0DsX5Mqp2DGSQkKb15oVtLN~F-BNtPB8WqiV4t2KuxPAcgnYAsCzoauXvgrEr6w__&Key-Pair-Id=APKAIE5G5CRDK6RD3PGA [following]


(python27) feili18s-MacBook-Pro:Desktop feili18$ ./wget-url.sh "https://pubs.geoscienceworld.org/rmg/article-pdf/52/1/1/2315810/1-16.pdf" delete.pdf
  HTTP/1.1 301 Moved Permanently
  Location: https://pubs.geoscienceworld.org/uwyo/rmg/article-pdf/52/1/1/2315810/1-16.pdf [following]

  HTTP/1.1 302 Found
  Location: https://gsw.silverchair-cdn.com/gsw/Content_public/Journal/rmg/52/1/10.24872_rmgjournal.52.1.1/1/1-16.pdf?Expires=1584211156&Signature=lJ8J-0Txs7c-oZnPR6QLwQwvSY6FdgeGHZUeKV34tLNTOCzFtbsUfsl4XhGmkbZhG45~wVjdzBxEyDvBVlmrRnJNR9nJ9S6swnPUsmNiielE918AI93~Xg-81--U~U25m0eNxFp8pmOb85pw18FSGMivtnvp1jyoPL7zkEdNd0HYneTxlrFU~QTEiNArGUbZME~HzAUBfXEMta5cm7vAjqTxzVaI~SiqbEcviesALLyEtfNKmmvSAuQTZJIHEJO3lVtR6Sb38YCGF7mrSq7f5tV5lGTQeRf-eg3LibsqydWcnGnqQSuwxbpjo1tKsPPTgNjhUPxsEOYNS1W15RYLLw__&Key-Pair-Id=APKAIE5G5CRDK6RD3PGA

  HTTP/1.1 200 OK

  Length: ignored [application/pdf]
  Saving to: ‘delete.pdf’
  delete.pdf                            [            <=>                                             ]   4.38M  1.95MB/s    in 2.2s
  2020-03-13 09:39:18 (1.95 MB/s) - ‘delete.pdf’ saved [4597569]

  These two URLs will not work without token:
  <Error>
    <Code>MissingKey</Code>
    <Message>
    Missing Key-Pair-Id query parameter or cookie value
    </Message>
    </Error>

  https://gsw.silverchair-cdn.com/gsw/Content_public/Journal/gsabulletin/Issue/131/1-2/4/abull_131_1_2_coverfig.png?Expires=2147483647&Signature=Ga6A4bPUsQ3P10aESGe03k9jIt~Yv5ExaTXwuHuX5yXGCi9kXGSGMFzPD5jm6CGTGsmY1XN2WEhlY6twGHAdi9lPrELEI5HXc2sbrn352FDhLfLYF~iTm2ySv5BM4vE-Cp6~ghqZrCNlYJwYQ9HAQzu8QdYeIdITxXK7CyATuRn1S6wuR~PNdvpeI9SVqNeal3iWODWrPzekq~~HPOwBiZ-BHONUX98~RjYnoVq3pjRXu1YjkfDoQ2h8K4tE~ax96gnZl7YlDFKJF-yQxFHjtQkkmjwbdxmLt72Uvx0gBkcfIm~4nNAnzRO88vcX-OO0VbtothNNpjuDqmE-GcRk7w__&Key-Pair-Id=APKAIE5G5CRDK6RD3PGA		1		39271077		-		-
  https://gsw.silverchair-cdn.com/gsw/Content_public/Journal/gsabulletin/Issue/131/1-2/4/toc.pdf?Expires=1584215055&Signature=lrSpTFpQ4anKpij8DXXZcackjyvY1uaGg0b9mXSrJr6QuKumPkxPs6YoYATOAXZWZvt6qQTN9B3KVRjVcZZEK~00s~hfrp~Q0TV6gZeWHVgVF0~QwhEdgDJVok~Hq74OlfzJTc5pRmbjNn9yaK3nEpeiElvnxllTc3r~Tylv1gDRTBG7YUzjPds5Flnb3y1YUfFD3ycVNYpj3N5UsjTNQXoFc38BhEzpnJtfFMZZ59jt5lfm~sDmcXvdhr5pF833pbXwymWzz8EbK1uztMSNxfUATedvux4xJAoeee4xoje2vgxMbz5UP0BFffIBJZdfjWZiuoQki0lPOwcEInM5Ow__&Key-Pair-Id=APKAIE5G5CRDK6RD3PGA
 */
public class SilverchairCommonThemeUrlConsumerFactory implements UrlConsumerFactory {
    private static final Logger log = Logger.getLogger(SilverchairCommonThemeUrlConsumerFactory.class);

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


