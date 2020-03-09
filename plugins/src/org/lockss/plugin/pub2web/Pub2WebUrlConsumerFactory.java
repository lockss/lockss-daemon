/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.pub2web;

import java.io.IOException;
import java.util.regex.Pattern;

import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.*;
import org.lockss.plugin.base.HttpToHttpsUrlConsumer;
import org.lockss.plugin.base.SimpleUrlConsumer;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;

/**
 * @since 1.67.5 
 */
public class Pub2WebUrlConsumerFactory implements UrlConsumerFactory {
	private static final Logger log = Logger.getLogger(Pub2WebUrlConsumerFactory.class);

	@Override
	public UrlConsumer createUrlConsumer(CrawlerFacade facade, FetchedUrlData fud) {
		log.debug3("Creating a Pub2Web UrlConsumer");
		return new Pub2WebUrlConsumer(facade, fud);
	}

	/**
	 * <p>
	 * A custom URL consumer that identifies specific redirect chains and stores the
	 * content at the origin of the chain (e.g. to support collecting and repairing
	 * redirect chains that begin with fixed URLs but end with one-time URLs).
	 * 
	 * Many article PDFs will now use the crawler stable version url but the consumer
	 * is still used for TOC pdfs or supplementary data so leave it in place for any 
	 * redirection URLs that aren't normalized to a crawler version
	 * </p>
	 * 
	 * BOOKS: 
	 * the link on the page is:
	 *   https://www.asmscience.org/deliver/fulltext/10.1128/9781683670247/9781683670230_FM.pdf?itemId=/content/book/10.1128/9781683670247.cont01&mimeType=pdf
	 *   http://www.asmscience.org/deliver/fulltext/10.1128/9781555818357/9781555819101_Chap09.pdf?itemId=/content/book/10.1128/9781555818357.chap9&mimeType=pdf
	 * it redirects to a one-time expiring URL under docserver
	 *   http://www.asmscience.org/docserver/fulltext/10.1128/9781555818357/9781555819101_Chap09.pdf?expires=1426886854&id=id&accname=4398&checksum=B4D5048F55C46BDB558351CDD802FC1D
	 * we only want to store the original, stable URL with the contents collected from the one-time URL
	 * 
	 * similarly to the PDF above, is the xml version, which doesn't (currently?) have a link on the browser page
	 * But the html version of the same link just leads to a blank page, so we don't collect it.
	 * Still leave "html" in the match in case they ever implement this
	 * 
	 * JOURNALS - slightly different URL layout, but same function
	 * http://www.asmscience.org/deliver/fulltext/microbiolspec/2/6/AID-0022-2014.pdf
	 *       ?itemId=/content/journal/microbiolspec/10.1128/microbiolspec.AID-0022-2014&mimeType=pdf
	 * becomes one-time URL
	 * http://www.asmscience.org/docserver/fulltext/microbiolspec/2/6/AID-0022-2014.pdf
	 *      ?expires=1427934811&id=id&accname=guest&checksum=0288048D2DD65A740231D10DDF18CE58
	 *      
	 * other examples of items that need to be consumed in to their original (non-expiring) url
	 *   - supplementary data - excel spreadsheets, movies
	 *      
	 * also found an example where supplementary data redirects
	 * <base>/deliver/fulltext/microbiolspec/3/2/PLAS_0039_2014_supp.xlsx
	 *  ?itemId=/content/suppdata/microbiolspec/10.1128/microbiolspec.PLAS-0039-2014-1
	 *   &mimeType=vnd.openxmlformats-officedocument.spreadsheetml.sheet
	 *   
	 * <base>/docserver/fulltext/microbiolspec/3/2/PLAS_0039_2014_supp.xlsx
	 *   ?expires=1428810292
	 *   &id=id
	 *   &accname=guest
	 *   &checksum=13930B0F0BDC404265CC8B6FB17F3FB3
	 *   
	 * from:
	 *   http://jmm.microbiologyresearch.org/deliver/fulltext/jmm/64/10/000143-S1.xlsx
	 *   ?itemId=/content/suppdata/jmm/10.1099/jmm.0.000143-1&mimeType=xlsx&isFastTrackArticle=
	 * 
	 * http://www.microbiologyresearch.org/docserver/fulltext/jmm/64/10/000143-S1.xlsx
	 *   ?expires=1462406457
	 *   &id=id
	 *   &accname=guest
	 *   &checksum=EADF255B848FA047F15DB2C10E8E0973
	 *
	 * from: http://jmm.microbiologyresearch.org/deliver/fulltext/jmm/64/2/000003c.mov
	 *   ?itemId=/content/suppdata/jmm/10.1099/jmm.0.000003-3&mimeType=quicktime&isFastTrackArticle=
	 * http://www.microbiologyresearch.org/docserver/fulltext/jmm/64/2/000003b.mov
	 *   ?expires=1462410081
	 *   &id=id&accname=guest
	 *   &checksum=BAE31918F398930F23AA6FF787ADEA8   
	 *      
	 * @since 1.67.5
	 *
	 * http://mic.microbiologyresearch.org/deliver/fulltext/micro/164/12/1567_micro000734.pdf?itemId=/content/journal/micro/10.1099/mic.0.000734mimeType=application/pdf
	 * Certain .PDF urls passed in without "&" in front of "mimeType", still need to handle it in order to avoid multiple copies saved
	 *
	 *
	 * 5/22/2019, the following two URLs points to the same PDF file
	 * https://mic.microbiologyresearch.org/content/journal/micro/10.1099/00221287-139-1-49?crawler=true&mimetype=application/pdf
	 * https://mic.microbiologyresearch.org/deliver/fulltext/micro/139/1/mic-139-1-49.pdf?itemId=/content/journal/micro/10.1099/00221287-139-1-49mimeType=application/pdf
	 * And they all be saved to the final location at:
	 * https://www.microbiologyresearch.org/docserver/fulltext/micro/139/1/mic-139-1-49.pdf?expires=1558562688&id=id&accname=guest&checksum=C5016D69F74F8B015CB2A6A62134C4F9
	 */
	public class Pub2WebUrlConsumer extends HttpToHttpsUrlConsumer {

		public static final String DEL_URL = "deliver/fulltext/";
		public static final String DEL_ARGS = "\\?itemId=[^&]+(&|&amp;)?mimeType=[^&]+";
		public static final String DEL_URL2 = "content/journal/";
		public static final String DEL_ARGS2 = "\\?crawler=true&mimetype=application/pdf";
		public static final String DOC_URL = "docserver/fulltext/";
		public static final String DOC_ARGS = "\\?expires=[^&]+(&|&amp;)id=id&accname=[^&]+(&|&amp;)checksum=.+$";

		// This needs to be pretty broad because we can't know what format (suffixes) of supplementary
		// data the publisher will end up supporting. Just check that the redirect starts at a 
		// deliver/fulltext/ + <jid>/<vol>/<iss>/artid.foo + argument section and goes to a 
		// docserver/fulltext + argument section with expiration and checksum
		//"[^?]+" catches the "JID/VOL/ISSUE/ARTID_supp.suff" portion
		public static final String ORIG_FULLTEXT_STRING = DEL_URL + "[^?]+" + DEL_ARGS; // it has arguments...
		public static final String ORIG_FULLTEXT_STRING2 = DEL_URL2 + "[^?]+" + DEL_ARGS2; // it has arguments...
		public static final String DEST_FULLTEXT_STRING = DOC_URL + "[^?]+" + DOC_ARGS;


		protected Pattern origFullTextPat = Pattern.compile(ORIG_FULLTEXT_STRING, Pattern.CASE_INSENSITIVE);
		protected Pattern origFullTextPat2 = Pattern.compile(ORIG_FULLTEXT_STRING2, Pattern.CASE_INSENSITIVE);
		protected Pattern destFullTextPat = Pattern.compile(DEST_FULLTEXT_STRING, Pattern.CASE_INSENSITIVE);

		public Pub2WebUrlConsumer(CrawlerFacade facade,
				FetchedUrlData fud) {
			super(facade, fud);
		}

		@Override
		public boolean shouldStoreAtOrigUrl() {
			// Let the parent handle all standard http to https consumption - one hop only
			boolean should = shouldStoreRedirectsAtOrigUrl();
			// If that failed, then see if this case should be part of pub2web specific consumption
			// which will also handle http to https as part of longer redirect chain
			// because the patterns checked are independent of protocol

			if (!should) {
				should =  fud.redirectUrls != null
						&& fud.redirectUrls.size() >= 1
						&& destFullTextPat.matcher(fud.fetchUrl).find()
						&& (origFullTextPat.matcher(fud.origUrl).find() || origFullTextPat2.matcher(fud.origUrl).find());
						if (!should) {
							log.debug3("NOT swallowing this pub2web redirect");
							log.debug3("[orig,fetch] = [" + fud.origUrl + "," + fud.fetchUrl + "]");
						}
			}
			return should;
		}

		/*
		Same as super shouldStoreAtOrigUrl but allow for multiple hops.
		 */
		protected boolean shouldStoreRedirectsAtOrigUrl() {

			if (fud.redirectUrls != null && fud.redirectUrls.size() >= 1) {
				for (String redirectUrl : fud.redirectUrls) {
					log.debug3("Fei: shouldStoreRedirectsAtOrigUrl redirectUrl= " + redirectUrl);
				}
			}

			boolean should = AuUtil.isBaseUrlHttp(au)
							&& fud.redirectUrls != null
							&& fud.redirectUrls.size() >= 1
							&& UrlUtil.isHttpUrl(fud.origUrl)
							&& UrlUtil.isHttpsUrl(fud.fetchUrl)
							&& UrlUtil.stripProtocol(fud.origUrl).equals(UrlUtil.stripProtocol(fud.fetchUrl));

			if (!should) {
				log.debug3("NOT swallowing this redirect by shouldStoreRedirectsAtOrigUrl originalUrl = " + fud.origUrl + ", fetchUrl = " + fud.fetchUrl );
			}
			return should;
		}
	}
}

