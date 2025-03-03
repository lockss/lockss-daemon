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

package org.lockss.plugin.atypon.uchicagopress;

import org.htmlparser.NodeFilter;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.atypon.BaseAtyponHtmlHashFilterFactory;
import org.lockss.util.Logger;

import java.io.InputStream;

public class UChicagoHtmlHashFilterFactory
		extends BaseAtyponHtmlHashFilterFactory {

	private static final Logger log = Logger.getLogger(UChicagoHtmlHashFilterFactory.class);


	@Override
	public InputStream createFilteredInputStream(ArchivalUnit au,
												 InputStream in, String encoding) {

		NodeFilter[] sageFilters = new NodeFilter[] {
		// header filtered in BaseAtypon

		HtmlNodeFilters.tagWithAttributeRegex("div", "class", "issueSerialNavigation"),
		HtmlNodeFilters.tagWithAttributeRegex("div", "class", "articleJournalButtons"),
		HtmlNodeFilters.tagWithAttributeRegex("div", "class", "toc-quick-links"),
		HtmlNodeFilters.tagWithAttributeRegex("div", "class", "journalNavInnerContainer"),
		HtmlNodeFilters.tagWithAttributeRegex("div", "class", "journalHeaderOuterContainer"),
		HtmlNodeFilters.tagWithAttributeRegex("div", "class", "issueBookNavPager"),


		// Exclude left part
		//HtmlNodeFilters.tagWithAttributeRegex("div", "class", "Article"),
		HtmlNodeFilters.tagWithAttributeRegex("div", "class", "abstractKeywords"),
		HtmlNodeFilters.tagWithAttributeRegex("table", "class", "references"),
		HtmlNodeFilters.tagWithAttributeRegex("div", "class", "journalHomeAlerts"),
		HtmlNodeFilters.tagWithAttributeRegex("div", "class", "totoplink"),
		HtmlNodeFilters.tagWithAttributeRegex("div", "id", "journalSearchButton"),
		HtmlNodeFilters.tagWithAttributeRegex("div", "class", "doNotShow"),
		HtmlNodeFilters.tagWithAttributeRegex("ul", "class", "article-toc-widget"),
		HtmlNodeFilters.tagWithAttributeRegex("div", "id", "article-tools"),
		HtmlNodeFilters.tagWithAttributeRegex("div", "class", "publication-tabs-header"),
		HtmlNodeFilters.tagWithAttributeRegex("div", "class", "articleAccessOptionsContainer"),
		HtmlNodeFilters.tagWithAttributeRegex("div", "class", "eCommercePurchaseAccessWidgetContainer"),
		HtmlNodeFilters.tagWithAttributeRegex("section", "class", "relatedArticles"),
		HtmlNodeFilters.tagWithAttributeRegex("div", "class", "offersList"),
		HtmlNodeFilters.tagWithAttributeRegex("div", "id", "relatedArticlesColumn"),
		HtmlNodeFilters.tagWithAttributeRegex("div", "class", "citingArticles"),
		HtmlNodeFilters.tagWithAttributeRegex("div", "class", "articleTabContainer"),
		HtmlNodeFilters.tagWithAttributeRegex("div", "class", "publicationContentAuthors"),
		HtmlNodeFilters.tagWithAttributeRegex("a", "class", "sf-back-to-top"),
		HtmlNodeFilters.tagWithAttributeRegex("div", "class", "purchaseArea"),
		HtmlNodeFilters.tagWithAttributeRegex("ul", "class", "tab-nav"),


		HtmlNodeFilters.tagWithAttributeRegex("div", "class", "tocAuthors"),
		HtmlNodeFilters.tagWithAttributeRegex("div", "class", "showAbstract"),
		HtmlNodeFilters.tagWithAttributeRegex("div", "class", "tocDeliverFormatsLinks"),
		HtmlNodeFilters.tagWithAttributeRegex("fieldset", "class", "tocListWidgetContainer"),
		HtmlNodeFilters.tagWithAttributeRegex("fieldset", "class", "tocTools"),

		HtmlNodeFilters.tagWithAttributeRegex("a", "class", "sr-only-focusable"),
		HtmlNodeFilters.tagWithAttributeRegex("span", "class", "content-navigation__type-text"),
		HtmlNodeFilters.tagWithAttributeRegex("a", "class", "sr-only-focusable"),
		HtmlNodeFilters.tagWithAttributeRegex("div","id","figure-viewer"),
		HtmlNodeFilters.tagWithAttributeRegex("div","class","citation-count"),
		HtmlNodeFilters.tagWithAttributeRegex("div","class","content-navigation"),

		};

		return super.createFilteredInputStream(au, in, encoding, sageFilters);
	}

	@Override
	public boolean doWSFiltering() {
		return true;
	}

}
