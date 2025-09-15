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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.extractor.JsoupHtmlLinkExtractor;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.*;
import org.jsoup.nodes.*;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.LinkExtractor;
import org.lockss.extractor.LinkExtractorFactory;
import org.lockss.extractor.JsoupHtmlLinkExtractor.ScriptTagLinkExtractor;
import org.lockss.extractor.LinkExtractor.Callback;


public class Ojs3HtmlLinkExtractorFactory implements LinkExtractorFactory {

	protected static final Logger log = Logger.getLogger(Ojs3HtmlLinkExtractorFactory.class);

	// instead of a crawl filter, limit which links are found on which pages
	protected static final String MANIFEST_PATH = "/gateway/";
	protected static final String TOC_PATH = "/issue/view/";
	protected static final Pattern TOC_PAT = Pattern.compile(TOC_PATH + "[^/]+$", Pattern.CASE_INSENSITIVE);
	protected static final Pattern LAND_PAT = Pattern.compile("/article/view/[^/]+$", Pattern.CASE_INSENSITIVE);

	@Override
	public LinkExtractor createLinkExtractor(String mimeType) throws PluginException {
		return new Ojs3JsoupHtmlLinkExtractor();
	}

	public static class Ojs3JsoupHtmlLinkExtractor extends JsoupHtmlLinkExtractor{

		public Ojs3JsoupHtmlLinkExtractor() {
			super();
			registerScriptTagExtractor();
		}

		@Override
		public void extractUrls(ArchivalUnit au, InputStream in, String encoding, String srcUrl, Callback cb)
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
		
		/*public static class Ojs3ScriptTagLinkExtractor extends ScriptTagLinkExtractor{
		@Override
		public void tagBegin(Node node, ArchivalUnit au, Callback cb) {
			log.debug3("Inside Ojs3ScriptTagLinkExtractor");
			super.tagBegin(node, au, cb);
			//get whole text, pass to buffered line reader, if line contains lens.css, then look
		}
	}*/

	protected void registerScriptTagExtractor() {
    registerTagExtractor("script", new ScriptTagLinkExtractor() {
      @Override
      public void tagBegin(Node node, ArchivalUnit au, Callback cb) {
		log.debug3("Inside Ojs3 registerScriptTagExtractor, base uri is " + node.baseUri());
		//look only inside article pages 
        if (node.baseUri().contains("euchembioj.com/index.php/pub/article/view/")) {
          String scriptHtml = ((Element)node).html();
          if (!StringUtil.isNullString(scriptHtml)) {
            String baseUrl = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
			String journalID = au.getConfiguration().get(ConfigParamDescr.JOURNAL_ID.getKey());
            Pattern lensPat = Pattern.compile(String.format("linkElement.href = \"(%s[^\"]+lens\\.css)\"; //Replace here", baseUrl),
                                                 Pattern.CASE_INSENSITIVE);
            Matcher lensMat = lensPat.matcher(scriptHtml);
            if (lensMat.find()) {
				/*This script tag lives at https://euchembioj.com/index.php/pub/article/view/13/26. 
				Example script tag that contains css and xml download links: 
				  <script type="text/javascript">

					var linkElement = document.createElement("link");
					linkElement.rel = "stylesheet";
					linkElement.href = "https://euchembioj.com/plugins/generic/lensGalley/lib/lens/lens.css"; //Replace here

					document.head.appendChild(linkElement);

					$(document).ready(function(){
						var app = new Lens({
							document_url: "https://euchembioj.com/index.php/pub/article/download/13/26/491"
						});
						app.start();
						window.app = app;
					});
				</script>
				 */
			  log.debug3("found correct script tag");
			  log.debug3("the script tag is " + scriptHtml);
			  cb.foundLink(lensMat.group(1).trim());
			  Pattern lensXmlPat = Pattern.compile(String.format("document_url: \"(%sindex.php/%s/article/download/[^\"]+)\"", baseUrl, journalID));
			  Matcher lensXmlMat = lensXmlPat.matcher(scriptHtml);
			  if(lensXmlMat.find()){
				cb.foundLink(lensXmlMat.group(1).trim());
			  }else{
				log.debug("Unable to find xml download link in script tag that contains lens.css link: " + node.baseUri());
			  }
            }
          }
        }
        super.tagBegin(node, au, cb);
      }
    });
  }
}
}
