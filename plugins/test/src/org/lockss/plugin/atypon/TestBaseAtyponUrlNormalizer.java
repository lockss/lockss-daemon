/*
 * $Id$
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
    
    // remove resultBean stuff - so far seen in BIR and T&F, probably spreading
    assertEquals("http://www.baseatypon.com/doi/abs/10.1080/19416520.2013.759433",
        normalizer.normalizeUrl("http://www.baseatypon.com/doi/abs/10.1080/19416520.2013.759433?queryID=%24%7BresultBean.queryID%7D", null));
    
    // test one that popped up in taylorandfrancis and is still there?
    assertEquals("http://www.tandfonline.com/doi/abs/10.5504/50YRTIMB.2011.0036",
        normalizer.normalizeUrl("http://www.tandfonline.com/doi/abs/10.5504/50YRTIMB.2011.0036?queryID=%24%7BresultBean.queryID%7D", null));
    
    assertEquals("http://www.tandfonline.com/doi/abs/10.1080/10610271003736871",
        normalizer.normalizeUrl("http://www.tandfonline.com/doi/abs/10.1080/10610271003736871?queryID=%24%7BresultBean.queryID%7D", null));
    
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

    assertEquals("http://www.ajronline.org/action/downloadCitation?doi=10.2214%2FAJR.12.9692&format=ris&include=cit",
        normalizer.normalizeUrl("http://www.ajronline.org/action/downloadCitation?direct=true&doi=10.2214%2FAJR.12.9692&downloadFileName=arrs_ajr200_1197&include=cit&submit=Download+publication+citation+data", null));

    assertEquals("http://www.ajronline.org/action/downloadCitation?doi=10.2214%2FAJR.12.10039&format=ris&include=cit",
        normalizer.normalizeUrl("http://www.ajronline.org/action/downloadCitation?direct=true&doi=10.2214%2FAJR.12.10039&downloadFileName=arrs_ajr201_1204&include=cit&submit=Download+publication+citation+data", null));
  }
  
  public void testCssArgumentNormalizer() throws Exception {
    UrlNormalizer normalizer = new BaseAtyponUrlNormalizer();

    // don't do anything to a normal url
    assertEquals("http://www.baseatypon.com/pb/css/head_3_8.css",
        normalizer.normalizeUrl("http://www.baseatypon.com/pb/css/head_3_8.css", null));
    assertEquals("http://www.baseatypon.com/pb/js/blah.js",
        normalizer.normalizeUrl("http://www.baseatypon.com/pb/js/blah.js", null));

    // normalize off argument
    assertEquals("http://www.baseatypon.com/pb/css/head_3_8.css",       
        normalizer.normalizeUrl("http://www.baseatypon.com/pb/css/head_3_8.css?1397594718000", null));
    assertEquals("http://www.baseatypon.com/pb/js/head_3_8.js",
        normalizer.normalizeUrl("http://www.baseatypon.com/pb/js/head_3_8.js?1397594718000", null));
    
    // don't do anything if not actually css or js file
    assertEquals("http://www.baseatypon.com/doi/abs/10.1111/blah.css.foo?argument",       
        normalizer.normalizeUrl("http://www.baseatypon.com/doi/abs/10.1111/blah.css.foo?argument", null));
    
    // still remove cookieSet argument if there
    assertEquals("http://www.baseatypon.com/pb/js/head_3_8.js",
        normalizer.normalizeUrl("http://www.baseatypon.com/pb/js/head_3_8.js?cookieSet=1", null));
    assertEquals("http://www.baseatypon.com/doi/abs/10.1111/blah.html",
        normalizer.normalizeUrl("http://www.baseatypon.com/doi/abs/10.1111/blah.html?cookieSet=1", null));

  }
  
  public void testShowImageNormalizer() throws Exception {
    UrlNormalizer normalizer = new BaseAtyponUrlNormalizer();

    /* Make sure a correct url going in, doesn't get modified */
    assertEquals("http://www.baseatypon.com/action/showPopup?citid=citart1&id=F1&doi=10.2466%2F05.08.IT.3.3",
        normalizer.normalizeUrl("http://www.baseatypon.com/action/showPopup?citid=citart1&id=F1&doi=10.2466%2F05.08.IT.3.3", null));

    /* Make sure a correct url going in, has the DOI "/" properly encoded */
    assertEquals("http://www.baseatypon.com/action/showPopup?citid=citart1&id=F1&doi=10.2466%2F05.08.IT.3.3",
        normalizer.normalizeUrl("http://www.baseatypon.com/action/showPopup?citid=citart1&id=F1&doi=10.2466/05.08.IT.3.3", null));

    /* Now check that it gets properly reordered */
    assertEquals("http://www.baseatypon.com/action/showPopup?citid=citart1&id=F1&doi=10.2466%2F05.08.IT.3.3",
        normalizer.normalizeUrl("http://www.baseatypon.com/action/showPopup?citid=citart1&doi=10.2466%2F05.08.IT.3.3&id=F1", null));
    /* and properly reordered and url encoded */
    assertEquals("http://www.baseatypon.com/action/showPopup?citid=citart1&id=F1&doi=10.2466%2F05.08.IT.3.3",
        normalizer.normalizeUrl("http://www.baseatypon.com/action/showPopup?citid=citart1&doi=10.2466/05.08.IT.3.3&id=F1", null));

    /* what do we do with a bogus argument */
    assertEquals("http://www.baseatypon.com/action/showPopup?citid=citart1",
        normalizer.normalizeUrl("http://www.baseatypon.com/action/showPopup?citid=citart1&doi", null));
    /* Get rid of extraneous args */
    assertEquals("http://www.baseatypon.com/action/showPopup?citid=citart1&id=F1&doi=10.2466%2F05.08.IT.3.3",
        normalizer.normalizeUrl("http://www.baseatypon.com/action/showPopup?citid=citart1&foo=blah&doi=10.2466%2F05.08.IT.3.3&id=F1", null));
    /* handle weird double "=" in an arg value */
    assertEquals("http://www.baseatypon.com/action/showPopup?citid=citart1&id=F1&doi=10.2466%2F05.08.IT.3.3",
        normalizer.normalizeUrl("http://www.baseatypon.com/action/showPopup?citid=citart1&foo=blah=rah&doi=10.2466%2F05.08.IT.3.3&id=F1", null));

  }  
  public void testShowImageFullNormalizer() throws Exception {
    UrlNormalizer normalizer = new BaseAtyponUrlNormalizer();

    /* Make sure a correct url going in, doesn't get modified */
    assertEquals("http://www.baseatypon.com/action/showFullPopup?id=i1520-0469-66-1-187-f01&doi=10.1175%2F2008JAS2765.1",
        normalizer.normalizeUrl("http://www.baseatypon.com/action/showFullPopup?id=i1520-0469-66-1-187-f01&doi=10.1175%2F2008JAS2765.1", null));

    /* Make sure a correct url going in, has the DOI "/" properly encoded */
    assertEquals("http://www.baseatypon.com/action/showFullPopup?id=i1520-0469-66-1-187-f01&doi=10.1175%2F2008JAS2765.1",
        normalizer.normalizeUrl("http://www.baseatypon.com/action/showFullPopup?id=i1520-0469-66-1-187-f01&doi=10.1175/2008JAS2765.1", null));

    /* Now make sure it gets reordered */
    assertEquals("http://www.baseatypon.com/action/showFullPopup?id=i1520-0469-66-1-187-f01&doi=10.1175%2F2008JAS2765.1",
        normalizer.normalizeUrl("http://www.baseatypon.com/action/showFullPopup?doi=10.1175%2F2008JAS2765.1&id=i1520-0469-66-1-187-f01", null));
    /* reordered and encoded */
    assertEquals("http://www.baseatypon.com/action/showFullPopup?id=i1520-0469-66-1-187-f01&doi=10.1175%2F2008JAS2765.1",
        normalizer.normalizeUrl("http://www.baseatypon.com/action/showFullPopup?doi=10.1175/2008JAS2765.1&id=i1520-0469-66-1-187-f01", null));

    /* do nothing but don't die for "bad" arg list */
    assertEquals("http://www.baseatypon.com/action/showFullPopup?doi=11.1111%2Ffoo",
        normalizer.normalizeUrl("http://www.baseatypon.com/action/showFullPopup?doi=11.1111/foo", null));
    assertEquals("http://www.baseatypon.com/action/showFullPopup?doi=11.1111%2Ffoo",
        normalizer.normalizeUrl("http://www.baseatypon.com/action/showFullPopup?doi=11.1111/foo&id", null));
    
    /* do nothing if it doesn't meet the requirements (match pattern AND have doi) */
    assertEquals("http://www.baseatypon.com/action/showFullPopup?id=blah&key=otherval",
        normalizer.normalizeUrl("http://www.baseatypon.com/action/showFullPopup?id=blah&key=otherval", null));

  }

}
