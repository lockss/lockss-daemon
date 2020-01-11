/*
 * $Id: $
 */

/*

Copyright (c) 2019 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.michigan;

/*
 * This will require daemon 1.62 and later for JsoupHtmlLinkExtractor support
 * The vanilla JsoupHtmlLinkExtractor will generate URLs from tags that it finds on pages
 * without restrictions (inclusion/exclusion rules) and so long as those resulting URLs
 * satisfy the crawl rules they will be collected. 
 */

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.lockss.extractor.JsoupHtmlLinkExtractor;
import org.lockss.extractor.LinkExtractor;
import org.lockss.extractor.LinkExtractorFactory;
import org.lockss.extractor.JsoupHtmlLinkExtractor.*;
import org.lockss.extractor.LinkExtractor.Callback;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;

// an implementation of JsoupHtmlLinkExtractor
public class UMichHtmlLinkExtractorFactory implements LinkExtractorFactory {

	private static final Logger log = Logger.getLogger(UMichHtmlLinkExtractorFactory.class);

	private static final String SCRIPT_TAG = "script";

	/*
	 * (non-Javadoc)
	 * @see org.lockss.extractor.LinkExtractorFactory#createLinkExtractor(java.lang.String)
	 * Simple version for most Atypon children
	 * restrict the form download URLs to just those forms with the name="frmCitMgr"
	 */
	@Override
	public LinkExtractor createLinkExtractor(String mimeType) {
		// set up the base link extractor to use specific includes and excludes
		// TURN on form extraction version of Jsoup for when the default is off
		JsoupHtmlLinkExtractor extractor = new JsoupHtmlLinkExtractor(false, true, null, null);
		extractor.registerTagExtractor("source", new SimpleTagLinkExtractor("src")); // not needed in 1.74.8+
                extractor.registerTagExtractor("track", new SimpleTagLinkExtractor("src")); // not needed in 1.74.8+
		extractor.registerTagExtractor(SCRIPT_TAG, new UMichScriptTagLinkExtractor());      
		return extractor;
	}


	public static class UMichScriptTagLinkExtractor extends ScriptTagLinkExtractor {

		private static Logger log = Logger.getLogger(UMichScriptTagLinkExtractor.class);

		public UMichScriptTagLinkExtractor() {
			super();
		}

		/* Make sure we're on a page that we care to parse for download information
		 * Note that the base_url in this match does not include final "/" on purpose
		 */
		protected Pattern PATTERN_BOOK_LANDING_URL = Pattern.compile("^(https?://[^/]+)/epubs/[^/]+$");
		/*
		 * 
		 *    <script type="text/javascript">
		 * if ( true ) {
		 * //$("body").addClass("reading");
		 * var reader = cozy.reader('reader', {
		 * href: "https://www.fulcrum.org/epubs/9s161681f/",
		 * skipLink: '.skip',
		 *  useArchive: false,
		 *  download_links: [{"format":"EPUB","size":"1.99 MB","href":"/downloads/9s161681f"}],
		 *  loader_template: '<div class="fulcrum-loading"><div class="rect rect1"></div><div class="circle circ1"></div><div class="rect rect2"></div><div class="circle circ2"></div></div>',
		 *  metadata: {
		 *  (    doi: 'https://hdl.handle.net/2027/fulcrum.9s161681f',
		 *   location: 'Ann Arbor, MI'
		 * }
		 * });
		 * 
		 * Match the download_links: array - if we need to later we can get it as an argument of the cozy.reader, 
		 * but it probably doens't matter
		 */

		//download_links: [{"format":"EPUB","size":"1.99 MB","href":"/downloads/9s161681f"}],
		//$1 will be the inner contents of the array
		// assuming there could be multiple {} elements within the array
		// Use of Pattern.DOTALL to allow '.' to handle newlines
		protected static final Pattern PATTERN_DOWNLOAD_LINKS_ARRAY =        
				Pattern.compile("download_links\\s*:\\s*\\[([^\\]]+)\\]",
						Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

		// data inside each {} of the array
		protected static final Pattern PATTERN_DOWNLOAD_LINK_ELEMENT =        
				Pattern.compile("\\s*\\{([^\\}]+)\\}",
						Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
		//"format":"EPUB","size":"1.99 MB","href":"/downloads/9s161681f"
		//for each one in the array
		protected static final Pattern PATTERN_HREF =        
				Pattern.compile(".*\"href\\s*\"\\s*:\\s*\"([^\"]+)",
						Pattern.CASE_INSENSITIVE | Pattern.DOTALL);


		/*
		 * Extending the way links are extracted by the Jsoup link extractor in a specific case:
		 *   - we are on a full article page
		 *   - we hit an script tag of the format:
		 *   <script type="text/javascript">...contents...</script>
		 *   where the content match the PATTERN_DOWNLOAD_LINKS_ARRAY
		 * In any case other than this one, fall back to standard Jsoup implementation    
		 */
		public void tagBegin(Node node, ArchivalUnit au, Callback cb) {
			//log.setLevel("debug3");
			String srcUrl = node.baseUri();
			Matcher fullArticleMat = PATTERN_BOOK_LANDING_URL.matcher(srcUrl);
			// For the moment we only get here on a <script> tag, so no need to check 
			// Are we a page for which this would be pertinent?
			if ( (srcUrl != null) && fullArticleMat.matches()) {
				String base_url = fullArticleMat.group(1);
				// the interior javascript is html, not text
				String scriptHTML = ((Element)node).html();
				log.debug3("script string: " + scriptHTML);
				Matcher downloadMat = PATTERN_DOWNLOAD_LINKS_ARRAY.matcher(scriptHTML);
				if (downloadMat.find()) {
					String arraySt = downloadMat.group(1);
					log.debug3("array info: " + arraySt);
					Matcher linkInfoMat = PATTERN_DOWNLOAD_LINK_ELEMENT.matcher(arraySt);
					// For each download_links element
					while (linkInfoMat.find()) {
						// This will be the information for ONE of the link argument lists
						String linkInfoStr = linkInfoMat.group(1);
						log.debug3("one link info: " + linkInfoStr);
						//"format":"EPUB","size":"1.99 MB","href":"/downloads/9s161681f"
						Matcher linkMat = PATTERN_HREF.matcher(linkInfoStr);
						if (linkMat.find()) {
							String newUrl = base_url + linkMat.group(1);
							log.debug3("new URL: " + newUrl);
							cb.foundLink(newUrl);
						}
					}
					// it was a download_links script tag, no further extraction needed even if nothing found
					return;
				}
			}
			// Not the special case, fall back to standard Jsoup
			super.tagBegin(node, au, cb);
		}
	} 


}
