/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.ojs2;

import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.UrlFetcher;
import org.lockss.plugin.UrlFetcherFactory;
import org.lockss.plugin.base.HttpToHttpsUrlFetcher;
import org.lockss.util.*;

public class OJS2HttpToHttpsUrlFetcherFactory implements UrlFetcherFactory {
	private static final Logger log = Logger.getLogger(OJS2HttpToHttpsUrlFetcherFactory.class);


	@Override
	public UrlFetcher createUrlFetcher(CrawlerFacade crawlFacade, String url) {
		return new OJS2HttpToHttpsUrlFetcher(crawlFacade, url);
	}

	public class OJS2HttpToHttpsUrlFetcher extends HttpToHttpsUrlFetcher {

		private static final String GATEWAY_PATH = "/gateway/";

		public OJS2HttpToHttpsUrlFetcher(CrawlerFacade crawlFacade, String url) {
			super(crawlFacade, url);
		}

		/*
		 * Differs from HttpToHttpsUrlFetcher
		 * 
		 * For the redirect of http manifest page to https, OJS was adding redundant
		 * "?year=1234" argument which we normalize off. So redirected and fetched differ
		 * by this extraneous argument
		 * 
		 * http://www.psychoanalyse-journal.ch/index.php/psychoanalyse/gateway/lockss?year=0
		 * redirects to
		 * https://www.psychoanalyse-journal.ch/index.php/psychoanalyse/gateway/lockss?year=0?year=0
		 * which is normalized back to:
		 * http://www.psychoanalyse-journal.ch/index.php/psychoanalyse/gateway/lockss?year=0
		 * redirect.startsWith(fetched) rather than redirect.equals(fetched) other than protocol
		 */
		@Override
		protected boolean isHttpToHttpsRedirect(String fetched,
				String redirect,
				String normalized) {

			log.debug3("f: " + fetched + " r: " + redirect + " n: " + normalized);
			return UrlUtil.isHttpUrl(fetched)
					&& UrlUtil.isHttpsUrl(redirect)
					&& UrlUtil.isHttpUrl(normalized)
					&& UrlUtil.stripProtocol(redirect).startsWith(UrlUtil.stripProtocol(fetched))
					&& fetched.equals(normalized);
		}
	}


}
