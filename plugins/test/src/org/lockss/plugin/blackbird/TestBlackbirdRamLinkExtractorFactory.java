/*
 * $Id: TestBlackbirdRamLinkExtractorFactory.java,v 1.1.8.1 2008-09-09 07:58:04 tlipkis Exp $
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.blackbird;

import java.io.*;
import java.util.*;
import junit.framework.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.extractor.*;

public class TestBlackbirdRamLinkExtractorFactory
  extends LinkExtractorTestCase {

  static final String FROM_URL = "rtsp://video.vcu.edu/blackbird/";
  static final String TO_URL = "http://www.blackbird.vcu.edu/lockss_media/";

  public String getMimeType() {
    return MIME_TYPE_RAM;
  }

  public LinkExtractorFactory getFactory() {
    return new BlackbirdRamLinkExtractorFactory();
  }

  public void testTranslateUrls() throws Exception {
    Set expected = SetUtil.set(TO_URL + "blah.rm",
			       TO_URL + "blah2.rm",
			       TO_URL + "blah3.rm");
    Collection actual = extractUrls(FROM_URL + "blah.rm\n"+
				    TO_URL + "blah2.rm\n"+
				    FROM_URL + "blah3.rm\n");
    assertEquals(expected, SetUtil.theSet(actual));
  }

  public void testTranslateUrlsHandlesCase() throws Exception {
    Set expected =
      SetUtil.set("http://www.blackbird.vcu.edu/lockss_media/blah.rm",
		  "http://www.blackbird.vcu.edu/lockss_media/blah2.rm",
		  "http://www.blackbird.vcu.edu/lockss_media/blah3.rm");
    Collection actual = extractUrls("rtsp://video.vcu.edu/blackbird/blah.rm\n"+
				    "rtsp://VIDEO.VCU.EDU/blackbird/blah2.rm\n"+
				    "RTSP://video.vcu.edu/blackbird/blah3.rm\n");
    assertEquals(expected, SetUtil.theSet(actual));
  }
}
