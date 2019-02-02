/*
 * $Id$
 */

/*

Copyright (c) 2000-2019 Board of Trustees of Leland Stanford Jr. University,
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
