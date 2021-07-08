package org.lockss.plugin.silverchair.aota;

import org.lockss.daemon.Crawler;
import org.lockss.plugin.FetchedUrlData;
import org.lockss.plugin.UrlConsumer;
import org.lockss.plugin.UrlConsumerFactory;
import org.lockss.plugin.base.HttpToHttpsUrlConsumer;
import org.lockss.plugin.base.SimpleUrlConsumer;
import org.lockss.util.Logger;

import java.io.IOException;
import java.util.regex.Pattern;

/*
PDF link redirect to one time url:
./wget-url.sh "https://ajot.aota.org/aota/content_public/journal/ajot/937760/7301205070p1.pdf" delete.pdf
--2021-05-11 14:07:14--  https://ajot.aota.org/aota/content_public/journal/ajot/937760/7301205070p1.pdf
Resolving ajot.aota.org (ajot.aota.org)... 52.191.96.132
Connecting to ajot.aota.org (ajot.aota.org)|52.191.96.132|:443... connected.
HTTP request sent, awaiting response...
  HTTP/1.1 301 Moved Permanently
  cache-control: private
  content-type: text/html; charset=utf-8
  server: Microsoft-IIS/10.0
  set-cookie: AJOT_SessionId=sjqh5eo02d0wgadw5hbrv40m; domain=.aota.org; path=/; HttpOnly; SameSite=Lax
  set-cookie: AOTA_UmbrellaMachineID=637563640356380802; domain=.aota.org; expires=Sun, 11-May-2031 21:07:15 GMT; path=/; secure; SameSite=None
  set-cookie: IsMobile=False; domain=.aota.org; path=/
  x-aspnet-version: 4.0.30319
  x-powered-by: ASP.NET
  x-scm-server-number: WEB02
  access-control-allow-origin: *
  access-control-allow-headers: accept, cache-control, content-type, authorization
  access-control-allow-methods: GET, POST, PUT, DELETE, OPTIONS
  date: Tue, 11 May 2021 21:07:14 GMT
  content-length: 227
  location: https://ajot.aota.org/pdfaccess.ashx?url=%2faota%2fcontent_public%2fjournal%2fajot%2f937760%2f7301205070p1.pdf
  connection: close
Location: https://ajot.aota.org/pdfaccess.ashx?url=%2faota%2fcontent_public%2fjournal%2fajot%2f937760%2f7301205070p1.pdf [following]
--2021-05-11 14:07:14--  https://ajot.aota.org/pdfaccess.ashx?url=%2faota%2fcontent_public%2fjournal%2fajot%2f937760%2f7301205070p1.pdf
Connecting to ajot.aota.org (ajot.aota.org)|52.191.96.132|:443... connected.
HTTP request sent, awaiting response...
  HTTP/1.1 302 Found
  cache-control: private
  content-type: text/html; charset=utf-8
  server: Microsoft-IIS/10.0
  link: <http://ajot.aota.org/article.aspx?articleid=2724345>;rel="canonical"
  x-aspnet-version: 4.0.30319
  x-powered-by: ASP.NET
  x-scm-server-number: WEB01
  access-control-allow-origin: *
  access-control-allow-headers: accept, cache-control, content-type, authorization
  access-control-allow-methods: GET, POST, PUT, DELETE, OPTIONS
  date: Tue, 11 May 2021 21:07:15 GMT
  content-length: 621
  location: https://aota.silverchair-cdn.com/aota/content_public/journal/ajot/937760/7301205070p1.pdf?Expires=2147483647&Signature=jxeuHDVMHcjWmbDTHB4GLT74nVifhtLw45Y1wrjIOtivhHTXsUOkWqC15O3pqLrlvnZkbOlAW6Z2aWA9HVxJU-uLvoCUXnDN0hzQQ2y5cRxdrOGjEFkZbFP9I5RGTR-IkglJ~ON06fHiw4dazhuRcRQosUATIPLX-nq96rnKfSd5MD4DfMsPKFuX0wgVFBYoS-c86qSlFX6UK2dHa3vXQJWzcOrB4UQ~2ZPRkNgA-UT8VI978PaSCe1L9sycWChht5n2t1gd9W41Kwvsi2AaZRiZLEVxfIx2co4b0fOAZBFjCl~Vr942ch3npIVWdJbSRHmwnBPMpzPZqx2WC6RhWQ__&Key-Pair-Id=APKAIE5G5CRDK6RD3PGA
  connection: close
Location: https://aota.silverchair-cdn.com/aota/content_public/journal/ajot/937760/7301205070p1.pdf?Expires=2147483647&Signature=jxeuHDVMHcjWmbDTHB4GLT74nVifhtLw45Y1wrjIOtivhHTXsUOkWqC15O3pqLrlvnZkbOlAW6Z2aWA9HVxJU-uLvoCUXnDN0hzQQ2y5cRxdrOGjEFkZbFP9I5RGTR-IkglJ~ON06fHiw4dazhuRcRQosUATIPLX-nq96rnKfSd5MD4DfMsPKFuX0wgVFBYoS-c86qSlFX6UK2dHa3vXQJWzcOrB4UQ~2ZPRkNgA-UT8VI978PaSCe1L9sycWChht5n2t1gd9W41Kwvsi2AaZRiZLEVxfIx2co4b0fOAZBFjCl~Vr942ch3npIVWdJbSRHmwnBPMpzPZqx2WC6RhWQ__&Key-Pair-Id=APKAIE5G5CRDK6RD3PGA [following]
--2021-05-11 14:07:15--  https://aota.silverchair-cdn.com/aota/content_public/journal/ajot/937760/7301205070p1.pdf?Expires=2147483647&Signature=jxeuHDVMHcjWmbDTHB4GLT74nVifhtLw45Y1wrjIOtivhHTXsUOkWqC15O3pqLrlvnZkbOlAW6Z2aWA9HVxJU-uLvoCUXnDN0hzQQ2y5cRxdrOGjEFkZbFP9I5RGTR-IkglJ~ON06fHiw4dazhuRcRQosUATIPLX-nq96rnKfSd5MD4DfMsPKFuX0wgVFBYoS-c86qSlFX6UK2dHa3vXQJWzcOrB4UQ~2ZPRkNgA-UT8VI978PaSCe1L9sycWChht5n2t1gd9W41Kwvsi2AaZRiZLEVxfIx2co4b0fOAZBFjCl~Vr942ch3npIVWdJbSRHmwnBPMpzPZqx2WC6RhWQ__&Key-Pair-Id=APKAIE5G5CRDK6RD3PGA
Resolving aota.silverchair-cdn.com (aota.silverchair-cdn.com)... 13.226.228.78, 13.226.228.44, 13.226.228.50, ...
Connecting to aota.silverchair-cdn.com (aota.silverchair-cdn.com)|13.226.228.78|:443... connected.
HTTP request sent, awaiting response...
  HTTP/1.1 200 OK
  Content-Type: application/pdf
  Content-Length: 693859
  Connection: close
  Date: Tue, 11 May 2021 21:07:17 GMT
  Last-Modified: Tue, 30 Jul 2019 11:15:38 GMT
  ETag: "30118f98594b76875e0bfb31d508e430"
  Accept-Ranges: bytes
  Server: AmazonS3
  X-Cache: Miss from cloudfront
  Via: 1.1 5ef2a900d38e51af436412dffc086198.cloudfront.net (CloudFront)
  X-Amz-Cf-Pop: LAX50-C3
  X-Amz-Cf-Id: sHUErU1MIiFg89SrGGVtR_JLB5isMWVQhc-qzBzAy1kn9PVs10jsTA==
Length: ignored [application/pdf]
Saving to: ‘delete.pdf’

delete.pdf                           [  <=>                                                      ] 677.60K  1.74MB/s    in 0.4s

2021-05-11 14:07:16 (1.74 MB/s) - ‘delete.pdf’ saved [693859]
*/
public class AOTAUrlConsumerFactory implements UrlConsumerFactory {
    private static final Logger log = Logger.getLogger(AOTAUrlConsumerFactory.class);

    protected static final String ORIG_PDF_STRING = "/content_public/journal/[^/]+/[^/]+/.*\\.pdf";
    protected static final Pattern origPdfPat = Pattern.compile(ORIG_PDF_STRING, Pattern.CASE_INSENSITIVE);

    protected static final String SECOND_HOP_STRING = "/pdfaccess\\.ashx\\?url=";
    protected static final Pattern secondHopPat = Pattern.compile(SECOND_HOP_STRING, Pattern.CASE_INSENSITIVE);

    protected static final String DESTINATION_PDF_STRING = "/content_public/journal/[^/]+/[^/]+/.*\\.pdf\\?Expires=2147483647";
    protected static final Pattern destPdfPat = Pattern.compile(DESTINATION_PDF_STRING, Pattern.CASE_INSENSITIVE);


    @Override
    public UrlConsumer createUrlConsumer(Crawler.CrawlerFacade facade, FetchedUrlData fud) {
        log.debug3("Creating a UrlConsumer");
        return new AOTAUrlConsumer(facade, fud);
    }

    // extends simple
    public class AOTAUrlConsumer extends SimpleUrlConsumer {

        public AOTAUrlConsumer(Crawler.CrawlerFacade crawlFacade, FetchedUrlData fud) {
            super(crawlFacade, fud);
        }

        @Override
        public void consume() throws IOException {
            if (shouldStoreAtOrigUrl()) {
                storeAtOrigUrl();
            }
            super.consume();
        }
        
        public boolean shouldStoreAtOrigUrl() {
            boolean should = false;

            log.debug3("origUrl ==== " + fud.origUrl);

            if (origPdfPat.matcher(fud.origUrl).find() && fud.redirectUrls != null ) {
                log.debug3("origUrl === " + fud.origUrl + ", redirectedUrl = ");

                for (int i = 0; i < fud.redirectUrls.size(); i++) {
                    log.debug3("origUrl === " + fud.origUrl + ", redirectedUrl i = " + i + ", url = " + fud.redirectUrls.get(i));
                }

                if (fud.redirectUrls != null && fud.redirectUrls.size() >= 2) {

                    if (secondHopPat.matcher(fud.redirectUrls.get(0)).find()) {
                        log.debug3("origUrl === " + fud.origUrl + ", redirectedUrl i = " + 0 + ", url = " + fud.redirectUrls.get(0));
                        if (destPdfPat.matcher(fud.redirectUrls.get(1)).find()) {
                            log.debug3("origUrl === " + fud.origUrl + ", redirectedUrl i = " + 1 + ", url = " + fud.redirectUrls.get(1));
                             should = true;

                        }
                    }
                }
            }
            return should;
        }
    }
}


