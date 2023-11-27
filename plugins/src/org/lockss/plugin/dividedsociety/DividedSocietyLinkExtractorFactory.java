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

package org.lockss.plugin.dividedsociety;



import org.lockss.extractor.*;
import org.lockss.extractor.JsoupHtmlLinkExtractor.SimpleTagLinkExtractor;
import org.lockss.util.Logger;

/* an addition to JsoupHtmlLinkExtractor  */
/*
 * The audio links are found in:
 * <audio id="mediaelement_player_file_78451_xPZN57WyKwRtVU2M" controls>
 *   <source src="https://dl.dropboxusercontent.com/s/5177me3l2dcs9vv/Living%20in%20Divis%2C%20sweeping%20up%20and%20starting%20again.mp3">
 *    Your browser does not support the audio element.
 * </audio>
 */
public class DividedSocietyLinkExtractorFactory 
implements LinkExtractorFactory {
	public static final String SOURCE_TAG = "source";
	public static final String IMG_TAG = "img";
	public static final String DIV_TAG = "div";

	private static final Logger log = 
			Logger.getLogger(DividedSocietyLinkExtractorFactory.class);


	@Override
	public LinkExtractor createLinkExtractor(String mimeType) {
		JsoupHtmlLinkExtractor extractor = new JsoupHtmlLinkExtractor(false,false,null,null);
		registerExtractors(extractor);
		return extractor;
	}

	/*
	 *  pick up the "src" attribute on <source> tags
     *  pick up the "data-src" attribute on <img> tags
     *  pick up the "data-thumb" attribute on <div> tags
	 */
	protected void registerExtractors(JsoupHtmlLinkExtractor extractor) {


		extractor.registerTagExtractor(SOURCE_TAG,
				new SimpleTagLinkExtractor(new String[]{"src"}));
		extractor.registerTagExtractor(IMG_TAG,
				new SimpleTagLinkExtractor(new String[]
                        {"src", "longdesc", "data-src"}));
		extractor.registerTagExtractor(DIV_TAG,
				new SimpleTagLinkExtractor(new String[]
                        {"data-thumb"}));

	}
}