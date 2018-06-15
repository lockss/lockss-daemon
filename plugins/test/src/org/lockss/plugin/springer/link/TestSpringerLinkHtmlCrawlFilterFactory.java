/*  $Id$
 
 Copyright (c) 2000-2018 Board of Trustees of Leland Stanford Jr. University,

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

package org.lockss.plugin.springer.link;

import java.io.InputStream;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.StringUtil;

public class TestSpringerLinkHtmlCrawlFilterFactory extends LockssTestCase {
  private SpringerLinkHtmlCrawlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new SpringerLinkHtmlCrawlFilterFactory();
  }
  

  private static final String references = 
    "<h2 class=\"Heading\">References</h2>" +
    "<div class=\"content\">" +
    "<ol class=\"BibliographyWrapper\">" +
    "<li class=\"Citation\">"+
    "<div class=\"CitationNumber\">1.</div>" +
    "<div class=\"CitationContent\" id=\"CR1\">Bohm, D.: A suggested interpretation of the quantum theory in terms of “Hidden” variables. I. Phys. Rev. " +
    "<strong class=\"EmphasisTypeBold \">85</strong>(2), 166 (1952). doi:" +
    "<span class=\"ExternalRef\"> " +
    "    <a target=\"_blank\" rel=\"noopener\" href=\"https://doi.org/10.1103/PhysRev.85.166\">" +
    "    <span class=\"RefSource\">10.1103/PhysRev.85.166</span></a>" +
    "</span>" +
    "<span class=\"Occurrences\"><span class=\"Occurrence OccurrenceBibcode\">" +
    "    <a class=\"gtm-reference\" data-reference-type=\"ADS\" target=\"_blank\" rel=\"noopener\" href=\"http://adsabs.harvard.edu/cgi-bin/nph-data_query?link_type=ABSTRACT&amp;bibcode=1952PhRv...85..166B\">" +
    "    <span>" +
    "        <span>ADS</span>" +
    "    </span></a>" +
    "</span>" +
    "</span></div></li>" +
    "Hello World";

  private static final String referencesFiltered = 
    "<h2 class=\"Heading\">References</h2>" +
    "<div class=\"content\">" +
    "<ol class=\"BibliographyWrapper\">" +
    "Hello World";
  

  /*
   *  Compare Html and HtmlHashFiltered
   */
  public void testOverlay() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(references), Constants.DEFAULT_ENCODING);
    System.out.printf("["+StringUtil.fromInputStream(actIn) + "]");
    System.out.printf(referencesFiltered);
    //assertEquals(referencesFiltered, StringUtil.fromInputStream(actIn));
  }
  
}