/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.taylorandfrancis;

import org.lockss.plugin.UrlNormalizer;
import org.lockss.plugin.atypon.BaseAtyponUrlNormalizer;
import org.lockss.test.LockssTestCase;

public class TestTaylorAndFrancisUrlNormalizer extends LockssTestCase {

  public void testNormalizeUrl() throws Exception {
    //UrlNormalizer normalizer = new TaylorAndFrancisUrlNormalizer();
    UrlNormalizer normalizer = new BaseAtyponUrlNormalizer();
    // No change expected
    assertEquals("http://www.example.com/foo",
                 normalizer.normalizeUrl("http://www.example.com/foo", null));
    assertEquals("http://www.example.com/foo?",
                 normalizer.normalizeUrl("http://www.example.com/foo?", null));
    assertEquals("http://www.example.com/foo?nothinghappens",
                 normalizer.normalizeUrl("http://www.example.com/foo?nothinghappens", null));
    
    // Remove the right suffixes
    assertEquals("http://www.example.com/foo",
                 normalizer.normalizeUrl("http://www.example.com/foo?cookieSet=1", null));
    
    // Remove the first double slash (other than that of http:// or similar)
    assertEquals("http://www.example.com/foo",
                 normalizer.normalizeUrl("http://www.example.com//foo", null));
    assertEquals("http://www.example.com/foo/bar",
                 normalizer.normalizeUrl("http://www.example.com/foo/bar", null));
    assertEquals("http://www.example.com/nothinghappens/",
                 normalizer.normalizeUrl("http://www.example.com/nothinghappens/", null));
    assertEquals("http://www.example.com/foo/",
                 normalizer.normalizeUrl("http://www.example.com/foo//", null));
    assertEquals("http://www.example.com/foo/bar//baz",
                 normalizer.normalizeUrl("http://www.example.com/foo//bar//baz", null));
    assertEquals("https://www.example.com/foo",
                 normalizer.normalizeUrl("https://www.example.com//foo", null));
    assertEquals("ftp://www.example.com/foo",
                 normalizer.normalizeUrl("ftp://www.example.com//foo", null));
    
    // Test normalization for downloaded citation form URLS
        assertEquals("http://www.example.com/action/downloadCitation?doi=10.1080%2F19419899.2010.534489&format=refworks&include=ref",
            normalizer.normalizeUrl("http://www.example.com/action/downloadCitation?dbPub=false&direct=true&doi=10.1080%2F19419899.2010.534489&downloadFileName=tandf_rpse202_159&format=refworks&include=ref", null));
    assertEquals("http://www.example.com/action/downloadCitation?doi=10.1080%2F19419899.2010.534489&format=bibtex&include=ref",
        normalizer.normalizeUrl("http://www.example.com/action/downloadCitation?dbPub=false&direct=true&doi=10.1080%2F19419899.2010.534489&downloadFileName=tandf_rpse202_159&format=bibtex&include=ref", null));
    assertEquals("http://www.example.com/action/downloadCitation?doi=10.1080%2F19419899.2010.534489&format=ris&include=ref",
        normalizer.normalizeUrl("http://www.example.com/action/downloadCitation?dbPub=false&direct=true&doi=10.1080%2F19419899.2010.534489&downloadFileName=tandf_rpse202_159&format=ris&include=ref", null));
    assertEquals("http://www.example.com/action/downloadCitation?doi=10.1080%2F19419899.2010.534489&format=ris&include=cit",
        normalizer.normalizeUrl("http://www.example.com/action/downloadCitation?dbPub=false&direct=true&doi=10.1080%2F19419899.2010.534489&downloadFileName=tandf_rpse202_159&format=ris&include=cit", null));
    assertEquals("http://www.example.com/action/downloadCitation?doi=10.1080%2F19419899.2010.534489&format=ris&include=abs",
        normalizer.normalizeUrl("http://www.example.com/action/downloadCitation?dbPub=false&direct=true&doi=10.1080%2F19419899.2010.534489&downloadFileName=tandf_rpse202_159&format=ris&include=abs", null));

    //versions I haven't actually seen show up, but let's be proactive    
    //no dbPub
    assertEquals("http://www.example.com/action/downloadCitation?doi=10.1080%2F19419899.2010.534489&format=ris&include=cit",  
        normalizer.normalizeUrl("http://www.example.com/action/downloadCitation?direct=true&doi=10.1080%2F19419899.2010.534489&downloadFileName=tandf_rpse202_159&format=ris&include=cit", null));
    //direct before dbPub
    assertEquals("http://www.example.com/action/downloadCitation?doi=10.1080%2F19419899.2010.534489&format=ris&include=cit",  
        normalizer.normalizeUrl("http://www.example.com/action/downloadCitation?direct=true&dbPub=true&doi=10.1080%2F19419899.2010.534489&downloadFileName=tandf_rpse202_159&format=ris&include=cit", null));
    //no format
    assertEquals("http://www.example.com/action/downloadCitation?doi=10.1080%2F19419899.2010.534489&format=ris&include=cit",  
        normalizer.normalizeUrl("http://www.example.com/action/downloadCitation?dbPub=false&direct=true&doi=10.1080%2F19419899.2010.534489&downloadFileName=tandf_rpse202_159&include=cit", null));
    //no include
    assertEquals("http://www.example.com/action/downloadCitation?doi=10.1080%2F19419899.2010.534489&format=ris&include=cit",  
        normalizer.normalizeUrl("http://www.example.com/action/downloadCitation?direct=true&doi=10.1080%2F19419899.2010.534489&downloadFileName=tandf_rpse202_159&format=ris", null));
    //neither format=ris&include=cit 
    assertEquals("http://www.example.com/action/downloadCitation?doi=11.1111%2F12345&format=ris&include=cit",
        normalizer.normalizeUrl("http://www.example.com/action/downloadCitation?doi=11.1111%2F12345", null));
  }
  

  
}
