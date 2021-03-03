/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

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
