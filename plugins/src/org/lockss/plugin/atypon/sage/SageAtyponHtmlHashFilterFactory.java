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

package org.lockss.plugin.atypon.sage;

import java.io.InputStream;
import java.util.Vector;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.tags.Bullet;
import org.htmlparser.tags.BulletList;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.atypon.BaseAtyponHtmlHashFilterFactory;
import org.lockss.util.Logger;


//5/9/18 changed to include/exclude
// Keeps contents only (includeNodes), then hashes out unwanted nodes 
//within the content (excludeNodes).

//March/2021, page change to html5 structure
public class SageAtyponHtmlHashFilterFactory
		extends BaseAtyponHtmlHashFilterFactory {

	private static final Logger log = Logger.getLogger(SageAtyponHtmlHashFilterFactory.class);


	@Override
	public InputStream createFilteredInputStream(ArchivalUnit au,
												 InputStream in, String encoding) {

		NodeFilter[] sageFilters = new NodeFilter[] {
		// header filtered in BaseAtypon

		// Toc Page: https://journals.sagepub.com/toc/jeda/14/1
		HtmlNodeFilters.tagWithAttributeRegex("div", "class", "issueSerialNavigation"),
		HtmlNodeFilters.tagWithAttributeRegex("div", "class", "articleJournalButtons"),
		HtmlNodeFilters.tagWithAttributeRegex("div", "class", "toc-quick-links"),
		HtmlNodeFilters.tagWithAttributeRegex("div", "class", "journalNavInnerContainer"),
		HtmlNodeFilters.tagWithAttributeRegex("div", "class", "journalHeaderOuterContainer"),
		HtmlNodeFilters.tagWithAttributeRegex("div", "class", "issueBookNavPager"),

		//Abs Page: https://journals.sagepub.com/doi/abs/10.1177/1070496504273512
		//Full Page: https://journals.sagepub.com/doi/full/10.1177/1070496504273512
		// Ref Page: https://journals.sagepub.com/doi/ref/10.1177/1070496504273512
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

		// Toc Page: https://journals.sagepub.com/toc/jeda/14/1
		HtmlNodeFilters.tagWithAttributeRegex("div", "class", "tocAuthors"),
		HtmlNodeFilters.tagWithAttributeRegex("div", "class", "showAbstract"),
		HtmlNodeFilters.tagWithAttributeRegex("div", "class", "tocDeliverFormatsLinks"),
		HtmlNodeFilters.tagWithAttributeRegex("fieldset", "class", "tocListWidgetContainer"),
		HtmlNodeFilters.tagWithAttributeRegex("fieldset", "class", "tocTools"),


				// footer filtered in BaseAtypon
		};

		return super.createFilteredInputStream(au, in, encoding, sageFilters);
	}

	@Override
	public boolean doWSFiltering() {
		return true;
	}

}
