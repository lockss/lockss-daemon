/*
 * $Id: TestBaseAtyponUrlNormalizer.java,v 1.1 2013-07-31 21:43:56 alexandraohlson Exp $
 */

/*

Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.atypon;

import org.lockss.plugin.UrlNormalizer;
import org.lockss.plugin.atypon.BaseAtyponUrlNormalizer;
import org.lockss.test.LockssTestCase;

public class TestBaseAtyponUrlNormalizer extends LockssTestCase {

  public void testUrlNormalizer() throws Exception {
    UrlNormalizer normalizer = new BaseAtyponUrlNormalizer();

    // don't do anything to a normal url
    assertEquals("http://www.baseatypon.com/doi/pdf/11.1111/12345",
        normalizer.normalizeUrl("http://www.baseatypon.com/doi/pdf/11.1111/12345", null));
    // remove cookie at end of url
    assertEquals("http://www.baseatypon.com/doi/pdf/11.1111/12345",
        normalizer.normalizeUrl("http://www.baseatypon.com/doi/pdf/11.1111/12345?cookieSet=1", null));

    /* 
     * citation download stuff : each child has a slightly different starting URL
     * add to this section as you add children - in lieu of a full child URL normalization test
     */
    //citaton download stuff - make sure it has format=ris&include=cit if they are missing
    assertEquals("http://www.baseatypon.com/action/downloadCitation?doi=11.1111%2F12345&format=ris&include=cit",
        normalizer.normalizeUrl("http://www.baseatypon.com/action/downloadCitation?doi=11.1111%2F12345", null));

    // siam type citation download URL
    assertEquals("http://epubs.siam.org/action/downloadCitation?doi=11.1111%2F12345&format=ris&include=cit",
        normalizer.normalizeUrl("http://epubs.siam.org/action/downloadCitation?doi=11.1111%2F12345&downloadFileName=siam_mmsubt10_61&format=ris&include=cit", null));
    // citation download stuff - remove the extra stuff at the end of the url
    assertEquals("http://epubs.siam.org/action/downloadCitation?doi=11.1111%2F12345&format=ris&include=cit",
        normalizer.normalizeUrl("http://epubs.siam.org/action/downloadCitation?doi=11.1111%2F12345&format=ris&include=cit&submit=Download+publication+citation+data", null));

    // future science type citation download 
    assertEquals("http://future-science.com/action/downloadCitation?doi=11.1111%2F12345&format=ris&include=cit",
        normalizer.normalizeUrl("http://future-science.com/action/downloadCitation?direct=true&doi=11.1111%2F12345&downloadFileName=fus_bio4_1843&include=cit&submit=Download+article+metadata", null));

    // AMetSoc type citation download
    assertEquals("http://journals.ametsoc.org/action/downloadCitation?doi=10.1175%2FJCLI-D-11-00582.1&format=ris&include=cit",
        normalizer.normalizeUrl("http://journals.ametsoc.org/action/downloadCitation?direct=true&doi=10.1175%2FJCLI-D-11-00582.1&downloadFileName=ams_clim25_4476&include=cit&submit=Download+citation+data", null));  
  }
  
}
