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

package org.lockss.plugin.janeway;


import org.lockss.extractor.JsoupHtmlLinkExtractor;
import org.lockss.extractor.JsoupHtmlLinkExtractor.SimpleTagLinkExtractor;
import org.lockss.extractor.LinkExtractor;
import org.lockss.extractor.LinkExtractorFactory;
import org.lockss.util.Logger;

/*
	Article page:
    https://journals.publishing.umich.edu/mp/article/id/5973/

    <iframe id="fulcrum-embed-iframe-sn00b133k" src="https://www.fulcrum.org/embed?hdl=2027%2Ffulcrum.sn00b133k" title="Audio of B.S.B.M Orchestra guided improvised session" allowfullscreen="true"></iframe>

    audio link inside iframe page

    <audio id="audio"

    <audio id="audio"
       preload="auto"
       width="8000px"
       data-able-player
       data-skin="2020"
       data-heading-level="0"
       data-include-transcript="false">
  		<source src="/downloads/sn00b133k?file=mp3&amp;locale=en" type="audio/mpeg" />
	</audio>
 */

/*
	Article page:
    https://journals.publishing.umich.edu/conversations/article/id/2354/ has the following chain

    https://journals.publishing.umich.edu/conversations/article/id/2354/ => https://www.fulcrum.org/embed?hdl=2027%2Ffulcrum.jm214r214&fs=1 => https://www.fulcrum.org/downloads/jm214r214?file=mp4&locale=en

    <video id="video"
       preload="metadata"
       width="8000px"
       data-able-player
       data-skin="2020"
       data-captions-position="overlay"
       data-include-transcript="false"
       data-heading-level="0"
       data-allow-fullscreen=true
       poster="/downloads/jm214r214?file=jpeg&amp;locale=en"
       data-transcript-div="video-hidden-transcript-container" data-lyrics-mode>
  <source src="/downloads/jm214r214?file=mp4&amp;locale=en" type="video/mp4" />

    <track kind="subtitles" src="/downloads/jm214r214?file=captions_vtt&amp;locale=en" srclang="en" label="English"  />
  Your browser does not support the video tag.
</video>
 */
public class HtmlLinkExtractorFactory
implements LinkExtractorFactory {
	public static final String SOURCE_TAG = "source";

	private static final Logger log = 
			Logger.getLogger(HtmlLinkExtractorFactory.class);


	@Override
	public LinkExtractor createLinkExtractor(String mimeType) {
		JsoupHtmlLinkExtractor extractor = new JsoupHtmlLinkExtractor(false,false,null,null);
		registerExtractors(extractor);
		return extractor;
	}

	/*
	 *  pick up the "src" attribute on <source> tags
	 */
	protected void registerExtractors(JsoupHtmlLinkExtractor extractor) {
		extractor.registerTagExtractor(SOURCE_TAG,
				new SimpleTagLinkExtractor(new String[]{"src"}));

	}
}