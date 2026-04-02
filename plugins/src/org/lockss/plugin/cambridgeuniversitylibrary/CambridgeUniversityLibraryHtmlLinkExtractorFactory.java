/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.cambridgeuniversitylibrary;

import org.apache.commons.lang.StringUtils;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.JsoupHtmlLinkExtractor;
import org.lockss.extractor.JsoupHtmlLinkExtractor.ScriptTagLinkExtractor;
import org.lockss.extractor.JsoupHtmlLinkExtractor.SimpleTagLinkExtractor;
import org.lockss.extractor.LinkExtractor;
import org.lockss.extractor.LinkExtractor.Callback;
import org.lockss.extractor.LinkExtractorFactory;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// an implementation of JsoupHtmlLinkExtractor
public class CambridgeUniversityLibraryHtmlLinkExtractorFactory implements LinkExtractorFactory {

	private static final Logger log = Logger.getLogger(CambridgeUniversityLibraryHtmlLinkExtractorFactory.class);

	/*
	<div _ngcontent-dspace-angular-c2713611356="" class="ngx-gallery-image ng-trigger ng-trigger-animation ng-tns-c2713611356-7 ng-star-inserted" style="background-image: url(&quot;https://demo.dspace.org/server/api/core/bitstreams/e473cebf-c871-44d6-99a7-bfbc6bf5b64d/content&quot;);">
	 */
	private static final String DIV_TAG = "div";
	private static final String STYLE_ATTR = "style";

	//https://demo.dspace.org/items/f8617918-4921-4cda-aa1c-250f14727052
	protected static final Pattern PATTERN_ARTICLE_LANDING_URL = Pattern.compile("^(https?://[^/]+)/items/[^/]+$", Pattern.CASE_INSENSITIVE);

	@Override
	public LinkExtractor createLinkExtractor(String mimeType) throws PluginException {
		JsoupHtmlLinkExtractor extractor = new JsoupHtmlLinkExtractor();
		registerExtractors(extractor);
		return extractor;
	}

	protected void registerExtractors(JsoupHtmlLinkExtractor extractor) {
		extractor.registerTagExtractor(DIV_TAG, new MsDivTagLinkExtractor(STYLE_ATTR));
	}

	public static class MsDivTagLinkExtractor extends SimpleTagLinkExtractor {

		public MsDivTagLinkExtractor(String attr) {
			super(attr);
		}


		public void tagBegin(Node node, ArchivalUnit au, Callback cb) {
			String srcUrl = node.baseUri();
			Matcher landingMat = PATTERN_ARTICLE_LANDING_URL.matcher(srcUrl);

			if ( (srcUrl != null) && landingMat.matches()) {
				if (DIV_TAG.equals(node.nodeName())) {
					String styleVal = node.attr(STYLE_ATTR);
					if ( styleVal  != null & !(StringUtils.isEmpty(styleVal ))) {
						log.debug3("Get the url: " + styleVal);
						cb.foundLink(styleVal);
						return;
					}
				}
			}
		}
	}
}
