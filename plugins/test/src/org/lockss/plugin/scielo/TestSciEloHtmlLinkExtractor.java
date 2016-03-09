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

package org.lockss.plugin.scielo;

import java.util.*;

import org.lockss.daemon.ConfigParamDescr;
import org.lockss.extractor.LinkExtractor.Callback;
import org.lockss.test.*;
import org.lockss.util.Constants;

public class TestSciEloHtmlLinkExtractor extends LockssTestCase {

  protected MockArchivalUnit mau;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    mau = new MockArchivalUnit();
    mau.setConfiguration(ConfigurationUtil.fromArgs(ConfigParamDescr.BASE_URL.getKey(),
                                                    "http://www.scielo.org.co/",
                                                    ConfigParamDescr.JOURNAL_ISSN.getKey(),
                                                    "1657-7027",
                                                    ConfigParamDescr.YEAR.getKey(),
                                                    "2014"));
  }
  
  public void testAOnclick() throws Exception {
    String str1 = "<a onclick=\"setTimeout(&quot;window.open('http://www.scielo.org.co/scielo.php?script=sci_pdf&amp;pid=S1657-70272014000200002&amp;lng=en&amp;nrm=iso&amp;tlng=es ','_self')&quot;, 3000);\" href=\"foo.pdf\">pdf in  Spanish</a>";
    final List<String> urlList = new ArrayList<String>();
    new SciEloHtmlLinkExtractor().extractUrls(mau,
                                              new StringInputStream(str1),
                                              Constants.ENCODING_UTF_8,
                                              "http://www.scielo.org.co/scielo.php?script=sci_issuetoc&pid=1657-702720140001&lng=en&nrm=iso",
                                              new Callback() {
                                                @Override
                                                public void foundLink(String url) {
                                                  urlList.add(url);
                                                }
                                              });
    assertEquals(2, urlList.size());
    assertContains(urlList, "http://www.scielo.org.co/scielo.php?script=sci_pdf&pid=S1657-70272014000200002&lng=en&nrm=iso&tlng=es");
    assertContains(urlList, "http://www.scielo.org.co/foo.pdf");
  }
  
  public void testScript() throws Exception {
    String str1 = "<script>setTimeout(function(){ window.location=\"http://www.scielo.org.co/pdf/rgps/v13n26/v13n26a09.pdf\";}, 5000);</script>";
    final List<String> urlList = new ArrayList<String>();
    new SciEloHtmlLinkExtractor().extractUrls(mau,
                                              new StringInputStream(str1),
                                              Constants.ENCODING_UTF_8,
                                              "http://www.scielo.org.co/scielo.php?script=sci_pdf&pid=S1657-70272014000100009&lng=en&nrm=iso&tlng=es",
                                              new Callback() {
                                                @Override
                                                public void foundLink(String url) {
                                                  urlList.add(url);
                                                }
                                              });
    assertEquals(1, urlList.size());
    assertEquals("http://www.scielo.org.co/pdf/rgps/v13n26/v13n26a09.pdf", urlList.get(0));
  }
  
}
