/*
 * $Id:$
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