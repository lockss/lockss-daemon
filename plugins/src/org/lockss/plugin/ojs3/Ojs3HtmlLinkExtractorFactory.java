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

package org.lockss.plugin.ojs3;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.extractor.JsoupHtmlLinkExtractor;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.*;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.LinkExtractor;
import org.lockss.extractor.LinkExtractorFactory;


public class Ojs3HtmlLinkExtractorFactory implements LinkExtractorFactory {

	protected static final Logger log = Logger.getLogger(Ojs3HtmlLinkExtractorFactory.class);


	// instead of a crawl filter, limit which links are found on which pages
	protected static final String MANIFEST_PATH = "/gateway/";
	protected static final String TOC_PATH = "/issue/view/";
	protected static final Pattern LAND_PAT = Pattern.compile("/article/view/[^/]+$", Pattern.CASE_INSENSITIVE);


	@Override
	public LinkExtractor createLinkExtractor(String mimeType) throws PluginException {
		return new JsoupHtmlLinkExtractor() {
			@Override
			public void extractUrls(final ArchivalUnit au,
					InputStream in,
					String encoding,
					final String srcUrl,
					final Callback cb)
							throws IOException, PluginException {
				super.extractUrls(au,in,encoding,srcUrl,
						new Callback() {
					@Override
					public void foundLink(String url) {
						// If the found url is an issue TOC then we should only "find" it on a manifest
						// to avoid overcrawling
						if ((url.contains(TOC_PATH)) && !(srcUrl.contains(MANIFEST_PATH))) {
							log.debug3("Suppressing found link: " + url + " from page " + srcUrl);
							return;
						}
						// if the found url is for an article landing page, we should only "find"
						// it on an issue TOC to avoid overcrawling
						Matcher landMat = LAND_PAT.matcher(url);
						if (landMat.find() && !(srcUrl.contains(TOC_PATH))) {
							log.debug3("Suppressing found link: " + url + " from page " + srcUrl);
							return;
						}
						cb.foundLink(url);
					}
				});
			}
		};
	}

}
