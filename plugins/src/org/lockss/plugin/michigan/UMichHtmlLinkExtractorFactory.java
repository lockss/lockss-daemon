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

package org.lockss.plugin.michigan;

import java.io.*;

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

		protected static final Pattern PATTERN_FILE_SETS =
		    Pattern.compile("^(https?://[^/]+/concern/file_sets/[^/]+)(?:\\?.*)?$");
		
		/*
		 * group 1: function call + open parenthesis + open quotation mark
		 * group 2: primary JSON URL
		 * group 3: optional JSON URL query (question mark + query string)
		 * group 4: close quotatin mark + comma
		 */
                protected static final Pattern PATTERN_LEAFLET_TILELAYER_IIIF =
                    Pattern.compile("(tileLayer\\.iiif\\(\")([^?\"]+)(\\?[^\"]*)?(\",)");
                
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

                public void tagBegin(Node node, ArchivalUnit au, Callback cb) {
                  String srcUrl = node.baseUri();
                  
                  Matcher fileSetsMat = PATTERN_FILE_SETS.matcher(srcUrl);
                  if (fileSetsMat.matches()) {
                    doFileSets(fileSetsMat.group(1), node, au, cb);
                    return;
                  }
                  
                  Matcher fullArticleMat = PATTERN_BOOK_LANDING_URL.matcher(srcUrl);
                  if (fullArticleMat.matches()) {
                    doFullArticle(node, au, cb);
                    return;
                  }
                  
                  // Not a special case, fall back to standard Jsoup
                  super.tagBegin(node, au, cb);
                }
                
                public void doFileSets(String srcUrl, Node node, ArchivalUnit au, Callback cb) {
                  String javascript = ((Element)node).html();
                  if (log.isDebug3()) {
                    log.debug3("<script> contents: " + javascript);
                  }
                  try (BufferedReader br = new BufferedReader(new StringReader(javascript))) {
                    for (String line = br.readLine() ; line != null ; line = br.readLine()) {
                      Matcher mat = PATTERN_LEAFLET_TILELAYER_IIIF.matcher(line);
                      if (mat.find()) {
                        String found = mat.group(2);
                        String url = null;
                        if (found.startsWith("http")) { // assume absolute
                          url = found;
                        }
                        else if (found.startsWith("/")) { // assume relative to base_url
                          url = srcUrl.substring(0, srcUrl.indexOf('/', srcUrl.indexOf("://") + 3)) + found;
                        }
                        else { // assume relative to srcUrl
                          url = srcUrl.substring(0, srcUrl.lastIndexOf('/') + 1) + found;
                        }
                        log.debug2(String.format("Found IIIF URL: %s", url));
                        cb.foundLink(url);
                        break;
                      }
                    }
                  }
                  catch (IOException ioe) {
                    log.debug(String.format("I/O exception while parsing <script> tag in %s", srcUrl), ioe);
                  }
                }
		
		/*
		 * Extending the way links are extracted by the Jsoup link extractor in a specific case:
		 *   - we are on a full article page
		 *   - we hit an script tag of the format:
		 *   <script type="text/javascript">...contents...</script>
		 *   where the content match the PATTERN_DOWNLOAD_LINKS_ARRAY
		 * In any case other than this one, fall back to standard Jsoup implementation    
		 */
		public void doFullArticle(Node node, ArchivalUnit au, Callback cb) {
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
		}
	} 


}
