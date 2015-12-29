/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.plugin.UrlNormalizer;
import org.lockss.test.LockssTestCase;

public class TestSciELOUrlNormalizer extends LockssTestCase {
  
  public void testUrlNormalizer() throws Exception {
    UrlNormalizer normalizer = new SciELOUrlNormalizer();
    
    assertEquals(
        "http://www.example.com/pdf/abcd/v26n4/en_v26n4a20.pdf", normalizer.normalizeUrl(
        "http://www.example.com/readcube/epdf.php?doi=10.1590/S0102-67202013000400020&pid=S0102-67202013000400020&pdf_path=abcd/v26n4/en_v26n4a20.pdf&lang=en", null));
    assertEquals(
        "http://www.example.com/scielo.php?script=sci_arttext&pid=S0102-67202013000600001&lng=pt&tlng=pt", normalizer.normalizeUrl(
        "http://www.example.com/scielo.php?script=sci_arttext%26pid=S0102-67202013000600001%26lng=pt%26nrm=iso%26tlng=pt", null));
    assertEquals(
        "http://www.mendeley.com/import/?url=http://www.example.com/scielo.php?script=sci_arttext%26p\n" + 
        "id=S0102-67202013000600001%26lng=pt%26nrm=iso%26tlng=pt", normalizer.normalizeUrl(
        "http://www.mendeley.com/import/?url=http://www.example.com/scielo.php?script=sci_arttext%26p\n" + 
        "id=S0102-67202013000600001%26lng=pt%26nrm=iso%26tlng=pt", null));
    assertEquals(
        "http://www.example.com/scielo.php?script=sci_pdf&pid=S0102-67202013000600001&lng=pt&tlng=pt", normalizer.normalizeUrl(
        "http://www.example.com/scielo.php?script=sci_pdf&pid=S0102-67202013000600001&lng=pt&nrm=iso&tlng=pt", null));
    assertEquals(
        "http://www.example.com/scielo.php?script=sci_issuetoc&pid=0102-672020130006&lng=pt", normalizer.normalizeUrl(
        "http://www.example.com/scielo.php?script=sci_issuetoc&pid=0102-672020130006&lng=pt&nrm=iso", null));
    assertEquals(
        "http://www.example.com/scielo.php?script=sci_alphabetic&lng=pt", normalizer.normalizeUrl(
        "http://www.example.com/scielo.php?script=sci_alphabetic&lng=pt&nrm=iso", null));
    assertEquals(
        "http://www.example.com/scielo.php?script=sci_alphabetic&lng=pt&nrm=utf8", normalizer.normalizeUrl(
        "http://www.example.com/scielo.php?script=sci_alphabetic&lng=pt&nrm=utf8", null));
    assertEquals(
        "http://www.example.com/scielo.php?script=sci_issues&lng=en", normalizer.normalizeUrl(
        "http://www.example.com/scielo.php?script=sci_issues", null));
    assertEquals(
        "http://www.example.com/scielo.php?script=sci_issuetoc&pid=0102-672020140003&lng=en", normalizer.normalizeUrl(
        "http://www.example.com/scielo.php?script=sci_issuetoc&pid=0102-672020140003&lng=en&nrm=iso", null));
    assertEquals(
        "http://www.example.com/scielo.php?script=sci_pdf&pid=S0102-67202014000300167&lng=en", normalizer.normalizeUrl(
        "http://www.example.com/scielo.php?script=sci_pdf&pid=S0102-67202014000300167&lng=en&nrm=iso&tlng=en", null));
    assertEquals(
        "http://www.example.com/scieloOrg/php/articleXML.php?pid=S0102-67202014000200092&lang=en", normalizer.normalizeUrl(
        "http://www.example.com/scieloOrg/php/articleXML.php?pid=S0102-67202014000200092&lang=pt", null));
    assertEquals(
        "http://www.example.com/scielo.php?script=sci_pdf&pid=S0102-67202014000200091&lng=pt&tlng=pt", normalizer.normalizeUrl(
        "http://www.example.com/scielo.php?script=sci_pdf&pid=S0102-67202014000200091&lng=pt&nrm=iso&tlng=pt", null));
    // scielo.php?download
    assertEquals(
        "http://www.example.com/scielo.php?download&pid=S0102-67202014000200091&format=BibTex", normalizer.normalizeUrl(
        "http://www.example.com/scielo.php?download&pid=S0102-67202014000200091&format=BibTex", null));
    assertEquals(
        "http://www.example.com/scielo.php?download&pid=S0102-67202014000200091&format=BibTex", normalizer.normalizeUrl(
        "http://www.example.com/scielo.php?download&format=BibTex&pid=S0102-67202014000200091", null));
    assertEquals(
        "http://www.example.com/scielo.php?download&pid=S0102-67202014000200091&format=BibTex", normalizer.normalizeUrl(
        "http://www.example.com/scielo.php?format=BibTex&pid=S0102-67202014000200091&download", null));
    assertEquals(
        "foo", normalizer.normalizeUrl(
        "foo", null));
  }

  public void testParseQueryString() throws Exception {
    Map<String, String> map = SciELOUrlNormalizer.parseQueryString("foo1&foo2=&foo3=bar3&foo4=bar4=baz4%26foo5=bar5");
    assertEquals(5, map.size());
    assertNull(map.get("foo1")); // key without value nor equal sign
    assertNull(map.get("foo2")); // key without value but with equal sign
    assertEquals("bar3", map.get("foo3")); // key with value
    assertEquals("bar4=baz4", map.get("foo4")); // key with value containing equal sign
    assertEquals("bar5", map.get("foo5")); // key with value preceded by URL-encoded ampersand
  }
  
}
