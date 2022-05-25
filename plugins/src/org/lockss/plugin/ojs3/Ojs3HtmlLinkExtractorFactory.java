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
protected static final Pattern TOC_PAT = Pattern.compile(TOC_PATH + "[^/]+$", Pattern.CASE_INSENSITIVE);
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
						// we allow issue/view/123/456 because these are pdfs, htmls etc. that are on the TOC page
						Matcher tocMat = TOC_PAT.matcher(url);
						if (tocMat.find() && !srcUrl.contains(MANIFEST_PATH)) {
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
