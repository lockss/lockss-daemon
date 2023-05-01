/*
 * $Id$
 */

/*

Copyright (c) 2000-2023 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.silverchair.geoscienceworld;

import org.lockss.daemon.ConfigParamDescr;
import org.lockss.extractor.LinkExtractor.Callback;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;

import java.util.ArrayList;
import java.util.List;

public class TestGeoscienceWorldLinkExtractor extends LockssTestCase {

  protected MockArchivalUnit mau;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    mau = new MockArchivalUnit();
    mau.setConfiguration(ConfigurationUtil.fromArgs(ConfigParamDescr.BASE_URL.getKey(),
                                                    "https://pubs.geoscienceworld.org/",
                                                    ConfigParamDescr.JOURNAL_ID.getKey(),
                                                    "geology",
                                                    ConfigParamDescr.VOLUME_NAME.getKey(),
                                                    "49"));
  }
  
  public void testATag() throws Exception {
    String str1 = " <a href=\"javascript:;\" class=\"js-add-to-citation-download-manager\"\n" +
            "           data-resource-id=\"590534\"\n" +
            "           data-resource-type-id=\"3\">Add to Citation Manager</a>";
    final List<String> urlList = new ArrayList<String>();
    new GeoscienceWorldLinkExtractor().extractUrls(mau,
                                              new StringInputStream(str1),
                                              Constants.ENCODING_UTF_8,
                                              "https://pubs.geoscienceworld.org/geology/issue/49/1",
                                              new Callback() {
                                                @Override
                                                public void foundLink(String url) {
                                                    urlList.add(url);
                                                }
                                              });
    assertEquals(0, urlList.size());
  }
}
