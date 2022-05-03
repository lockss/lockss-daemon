/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.extractor.GoslingHtmlLinkExtractor;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.util.*;

public class OJS2HtmlLinkExtractor extends GoslingHtmlLinkExtractor {

	protected static final Logger log = Logger.getLogger(OJS2HtmlLinkExtractor.class);

	protected static final String NAME = "name";
	protected static final String PDF_NAME = "citation_pdf_url";
	protected static final String FT_NAME = "citation_fulltext_html_url";
	protected static final String CONTENT = "content";

	// url path to citations, need to append the citation format
	protected static String CITATION_DOWNLOAD_URL = null;

	// instead of a crawl filter, limit which links are foundon which pages
	protected static final String MANIFEST_PATH = "/gateway/";
	protected static final String TOC_PATH = "/issue/view/";
	protected static final Pattern TOC_PATH_PAT = Pattern.compile(".*/issue/view/([^/]+)(/.*)?", Pattern.CASE_INSENSITIVE);
	protected static final Pattern LAND_PAT = Pattern.compile("/article/view/[^/]+$", Pattern.CASE_INSENSITIVE);


	protected static final Pattern OPEN_RT_WINDOW_PATTERN =
			Pattern.compile("javascript:openRTWindow\\('([^']*)'\\);", Pattern.CASE_INSENSITIVE);

	protected static final Pattern IFRAME_PDF_VIEWER_PATTERN =
			Pattern.compile("/plugins/generic/pdfJsViewer/pdf\\.js/web/viewer\\.html\\?file=([^&]+)", Pattern.CASE_INSENSITIVE);

	protected static final Pattern JLA_ARTICLE_PATTERN = Pattern.compile(
			"(http://www[.]logicandanalysis[.]org/index.php/jla/article/view/[\\d]+)/[\\d]+$");

	protected static final Pattern JS_CITATION_REPLACEMENT_PATTERN = Pattern.compile(
			"document\\.location='(https?://.*/rt/captureCite/.*/)([^/)]+)'\\.replace\\('\\2',.*",
			Pattern.CASE_INSENSITIVE);

	protected static final Pattern CITATION_FORMAT_PATH = Pattern.compile(".+CitationPlugin", Pattern.CASE_INSENSITIVE);

	@Override
	public void extractUrls(final ArchivalUnit au, InputStream in, String encoding, final String srcUrl,
			final Callback cb) throws IOException {

		super.extractUrls(au, in, encoding, srcUrl,
				new Callback() {
			@Override
			public void foundLink(String url) {
				// If the found url is an issue TOC then we should only "find" it on a manifest
				// to avoid overcrawling
				/*
				Because different OJS publishers construct their showToc link differently, some of the "showToc" links to
				issue content itself, however some of the "showToc" links to other related/archived articles.
				So the following rules will be applied here to make sure we pick the right showToc without over-crawling:
				1. From manifest page, it can go to any url
				2. From a toc page, it can only goes to the same toc
				3. From any other places other than toc, it can not go to any toc
				 */

				// From manifest page, it can go to any url
				if (!srcUrl.contains(MANIFEST_PATH)) {
					// From any other places other than toc, it can not go to any toc
					if (!srcUrl.contains(TOC_PATH) && url.contains(TOC_PATH)) {
						log.debug3("Suppressing found link for non TOC_PATH: " + url + " from page " + srcUrl);
						return;
					}

					// From a toc page, it can only goes to the same toc
					if (srcUrl.contains(TOC_PATH) && url.contains(TOC_PATH)) {
						Matcher srcMat = TOC_PATH_PAT.matcher(srcUrl);
						Matcher targetMat = TOC_PATH_PAT.matcher(url);

						if (srcMat.matches() && targetMat.matches() && (!srcMat.group(1).equals(targetMat.group(1)))) {
							log.debug3("Suppressing found link for different issue number, url is: " + url + " srcUrl is: " + srcUrl);
							return;
						}
					}

					// if the found url is for an article landing page, we should only "find"
					// it on an issue TOC to avoid overcrawling
					Matcher landMat = LAND_PAT.matcher(url);
					if (landMat.find() && !(srcUrl.contains(TOC_PATH))) {
						log.debug3("Suppressing found link: " + url + " from page " + srcUrl);
						return;
					}
				}

				//
				if (au != null) {
						if (UrlUtil.isSameHost(srcUrl, url)) {
							url = AuUtil.normalizeHttpHttpsFromBaseUrl(au, url);
						}
					}

				cb.foundLink(url);
			}
		}
				);
	}

	@Override
	protected String extractLinkFromTag(StringBuffer link,
			ArchivalUnit au,
			Callback cb)
					throws IOException {
		switch (link.charAt(0)) {
		case 'a':
		case 'A':
			if (beginsWithTag(link, ATAG)) {
				// <a href="...">
				String href = getAttributeValue(HREF, link);
				if (href != null) {
					// javascript:openRTWindow(url);
					Matcher mat = OPEN_RT_WINDOW_PATTERN.matcher(href);
					if (mat.find()) {
						if (baseUrl == null) { baseUrl = new URL(srcUrl); } // Copycat of parseLink()
						cb.foundLink(resolveUri(baseUrl, mat.group(1)));
					}
					mat = JLA_ARTICLE_PATTERN.matcher(href);
					if (mat.find()) {
						String url = mat.group(1);
						cb.foundLink(url);
					}
				}
			}
			break;

		case 'f':
		case 'F':
			if (beginsWithTag(link, FORMTAG)) {
				// <form action="...">
				String action = getAttributeValue("action", link);
				if (action != null) {
					// javascript:openRTWindow(url);
					Matcher mat = OPEN_RT_WINDOW_PATTERN.matcher(action);
					if (mat.find()) {
						if (baseUrl == null) { baseUrl = new URL(srcUrl); } // Copycat of parseLink()
						cb.foundLink(resolveUri(baseUrl, mat.group(1)));
					}
				}
			}
			break;

		case 'i':
		case 'I':
			if (beginsWithTag(link, IFRAMETAG)) {
				// <iframe src="...">
				String src = getAttributeValue(SRC, link);
				if (src != null) {
					// <baseurl?>/plugins/generic/pdfJsViewer/pdf.js/web/viewer.html?file=<encodedurl>
					Matcher mat = IFRAME_PDF_VIEWER_PATTERN.matcher(src);
					if (mat.find()) {
						if (baseUrl == null) { baseUrl = new URL(srcUrl); } // Copycat of parseLink()
						cb.foundLink(resolveUri(baseUrl, URLDecoder.decode(mat.group(1), "UTF-8")));
					}
				}
			}
			break;

		case 'm':
		case 'M':
			if (beginsWithTag(link, METATAG)) {
				if (REFRESH.equalsIgnoreCase(getAttributeValue(HTTP_EQUIV, link))) {
					String value = getAttributeValue(HTTP_EQUIV_CONTENT, link);
					if (value != null) {
						// <meta http-equiv="refresh" content="...">
						int i = value.indexOf(";url=");
						if (i >= 0) {
							String refreshUrl = value.substring(i+5);
							// javascript:openRTWindow(url);
							Matcher mat = OPEN_RT_WINDOW_PATTERN.matcher(refreshUrl);
							if (mat.find()) {
								if (baseUrl == null) { baseUrl = new URL(srcUrl); } // Copycat of parseLink()
								cb.foundLink(resolveUri(baseUrl, mat.group(1)));
							}
						}
					}
				} else {
					if (PDF_NAME.equalsIgnoreCase(getAttributeValue(NAME, link)) ||
							FT_NAME.equalsIgnoreCase(getAttributeValue(NAME, link))) {

						String url = getAttributeValue(CONTENT, link);
						if (url != null) {
							cb.foundLink(url);
						}
					}
				}
			}
			break;

		case 'o':
		case 'O':
			if (beginsWithTag(link, OPTIONTAG)) {
				String valueAttr = getAttributeValue("value", link);
				Matcher mat = CITATION_FORMAT_PATH.matcher(valueAttr);
				//       <option value="BibtexCitationPlugin">BibTeX</option>
				//       <option value="ProCiteCitationPlugin">ProCite - RIS format (Macintosh &amp; Windows)</option>
				if (mat.matches()) {
					if (CITATION_DOWNLOAD_URL != null) {
						String citationFormatUrl = CITATION_DOWNLOAD_URL + valueAttr;
						log.debug3("FOUND A MATCH FOR CITATION DOWNLOAD, setting to: " + citationFormatUrl);
						cb.foundLink(citationFormatUrl);
					}
				}
			}
			break;

		case 's':
		case 'S':
			if (beginsWithTag(link, "select")) {
				String onchange = getAttributeValue("onchange", link);
				Matcher mat = JS_CITATION_REPLACEMENT_PATTERN.matcher(onchange);
				//     <select onchange="document.location='https://www.afrjournal.org/index.php/afr/rt/captureCite/356/0/REPLACE'.replace('REPLACE', this.options[this.selectedIndex].value)">
				//       <option selected="selected" value="AbntCitationPlugin">ABNT</option>
				//       ...
				//       <option value="TurabianCitationPlugin">Turabian</option>
				//     </select>
				if (mat.matches()) {
					// set CITATION_DOWNLOAD_URL to this link, this gets used in the
					// following option tags to construct citation urls.
					CITATION_DOWNLOAD_URL = mat.group(1);
					log.debug3("FOUND A MATCH FOR JAVASCRIPT CITATION REPLACE, setting to: " + CITATION_DOWNLOAD_URL);
				}
			}
			break;

		}

		return super.extractLinkFromTag(link, au, cb);
	}

}

