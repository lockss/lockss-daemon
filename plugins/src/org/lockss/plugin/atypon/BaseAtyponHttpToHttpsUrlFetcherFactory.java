/*
 * $Id$
 */

/*

Copyright (c) 2000-2017 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

 */

package org.lockss.plugin.atypon;


import java.net.MalformedURLException;

import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.UrlFetcher;
import org.lockss.plugin.UrlFetcherFactory;
import org.lockss.plugin.base.HttpToHttpsUrlFetcher;
import org.lockss.util.*;

public class BaseAtyponHttpToHttpsUrlFetcherFactory implements UrlFetcherFactory {
	private static final Logger log = Logger.getLogger(BaseAtyponHttpToHttpsUrlFetcherFactory.class);
	private static final String PDF_PATH = "doi/pdf"; //could be "doi/pdf/" or "doi/pdfplus/"

	@Override
	public UrlFetcher createUrlFetcher(CrawlerFacade crawlFacade, String url) {
		return new BaseAtyponHttpToHttpsUrlFetcher(crawlFacade, url);
	}

	public class BaseAtyponHttpToHttpsUrlFetcher extends HttpToHttpsUrlFetcher {

		protected static final String SHOWCITFORMAT = "action/showCitFormats";

		public BaseAtyponHttpToHttpsUrlFetcher(CrawlerFacade crawlFacade, String url) {
			super(crawlFacade, url);
		}

		/*
		 * Differs from HttpToHttpsUrlFetcher
		 * 
		 * For the action/... URLs, it allows the encoding of arguments
		 * For example:
		 * http://arc.aiaa.org/action/showCitFormats?doi=10.2514/1.C032918
		 * redirects to:
		 * https://arc.aiaa.org/action/showCitFormats?doi=10.2514%2F1.C032918
		 * which will normalize to
		 * http://arc.aiaa.org/action/showCitFormats?doi=10.2514%2F1.C032918
		 * 
		 */
		@Override
		protected boolean isHttpToHttpsRedirect(String fetched,
				String redirect,
				String normalized) {
			return UrlUtil.isHttpUrl(fetched)
					&& UrlUtil.isHttpsUrl(redirect)
					&& UrlUtil.isHttpUrl(normalized)
					&& (UrlUtil.stripProtocol(fetched).equals(UrlUtil.stripProtocol(redirect)) ||
							UrlUtil.stripProtocol(fetched).equals(UrlUtil.stripProtocol(redirect.replace("%2F", "/"))))
					&& (fetched.equals(normalized) ||
							fetched.equals(normalized.replace("%2F","/")));
		}

		/*
		 * In early 2019 we noticed that Atypon had added new functionality where if the referer was a different host, it 
		 * redirected to the "full text" version of the article, even when the url was for doi/abs . 
		 * when
		 * 	http abstract --> redirected to https abstract with the referer from an original http 
		 * it was identified as "a different site" by their algorithm and caused them to redirect to full text, which was
		 * sometimes (Emerald books, for example) PDF.  So we were getting and storing PDF content at doi/abs and not getting
		 * any meta tags or bibliographic information.
		 * This function overrides the default and checks for the above scenario. When it happens, it
		 * makes the referer the https equivalent of what it was.
		 * 
		 */
		@Override
		protected void addPluginRequestHeaders() {
			super.addPluginRequestHeaders();
			if ((reqProps != null) && (reqProps.getProperty(Constants.HTTP_REFERER) != null) ){
				String ref = reqProps.getProperty(Constants.HTTP_REFERER);
				if (origUrl != null && UrlUtil.isHttpUrl(origUrl) &&
						fetchUrl != null && UrlUtil.isHttpsUrl(fetchUrl) &&
						UrlUtil.isHttpUrl(ref) ) {
					Boolean makeChange = false;
					try {
						makeChange = (UrlUtil.getHost(ref).equals(UrlUtil.getHost(fetchUrl)));
					} catch (MalformedURLException e) {
						//referer wasn't a valid url - no need to change it
						log.debug("referer URL was malformed: " + ref, e);
					}
					if (makeChange) {
						//actually the same host, make the protocol match to avoid "different host" behavior
						log.debug3("changing referer protocol to match redirected https to avoid Atypon full-text-for-other-host-referers feature");
						reqProps.setProperty(Constants.HTTP_REFERER, ref.replaceFirst("^http:", "https:"));
					}
				}
			}
		}
		
		/* 
		 * Disregard the if-modified-since and force a refetch if the existing cached content is PDF and the url
		 * should not be.  Otherwise previously collected incorrect redirects (now avoided by addPluginRequestHeaders above
		 * could stay incorrect based on modifcation date (EmeraldBooks
		 * 
		 */
		@Override
		protected boolean forceRefetch(){
			if (super.forceRefetch()) {
				return true;
			}
			CachedUrl cachedVersion = au.makeCachedUrl(origUrl);
			try {
			if ((cachedVersion!=null) && cachedVersion.hasContent()) {
				CIProperties cachedProps = cachedVersion.getProperties();
				String cached_mime =
						cachedProps.getProperty(CachedUrl.PROPERTY_CONTENT_TYPE);
				String cmime = HeaderUtil.getMimeTypeFromContentType(cached_mime); // returns lower case for direct comparison
				if (!origUrl.contains(PDF_PATH) && Constants.MIME_TYPE_PDF.equals(cmime)) {
					log.debug("forcing a refetch because the url should not be pdf");
					return true;
				}
			}
			return false;
			} finally {
				AuUtil.safeRelease(cachedVersion);
			}
		}
	}
}
