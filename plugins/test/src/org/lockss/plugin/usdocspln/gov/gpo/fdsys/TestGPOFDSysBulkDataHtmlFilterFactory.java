/*
 * $Id: TestGPOFDSysBulkDataHtmlFilterFactory.java,v 1.2 2012-09-27 21:49:50 davidecorcoran Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.usdocspln.gov.gpo.fdsys;

import java.io.*;
import org.lockss.util.*;
import org.lockss.test.*;

public class TestGPOFDSysBulkDataHtmlFilterFactory extends LockssTestCase {
  private GPOFDSysBulkDataHtmlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new GPOFDSysBulkDataHtmlFilterFactory();
  }

  // Example instances of HTMl that should be filtered
  // and the expected post-filter HTML strings.
  
  private static final String tokenHtml =
    "<!--<input type=\"hidden\" name=\"struts.token.name\" " +
    "value=\"struts.token\" /> <input type=\"hidden\" name=\"struts.token\" " +
    "value=\"PJ6XVF3GABC08YRN5D7LRKJRB8DEFH66\" />-->";
  private static final String tokenHtmlFiltered =
    "";
  
  private static final String scriptHtml =
    "<script type=\"text/javascript\">" +
    "var SEARCHWEBAPP_BUILD = 'FDSYS_R2_28';" +
    "</script>";
  private static final String scriptHtmlFiltered =
    "";
  
  private static final String noScriptHtml =
    "<noscript>" +
    "<div style=\"display: none;\">" +
    "<img alt=\"DCSIMG\" id=\"DCSIMG\" width=\"1\" height=\"1\"" +
    "src=\"http://162.140.239.17/dcsjsw8h600000gotf0vyrmly_2j7v/njs.gif?" +
    "dcsuri=/nojavascript&amp;WT.js=No&amp;dcscfg=1&amp;WT.tv=1.3.7\" />" +
    "</div>" +
    "</noscript>";
  private static final String noScriptHtmlFiltered =
    "";
  
  private static final String retrievalDateHtml = 
    "<tr><td> <a href=\"bulkdata/FR/2012/01/FR-2012-01.zip\" " +
    "onclick=\"logRetrievalStats(RETRIEVAL_TYPE_BULKDATA," +
    "'FR',FILE_TYPE_ZIP,this.href,'Bulk Data')\" target=\"_blank\"> " +
    "FR-2012-01.zip </a> </td> <td>18-Sep-2012 07:08</td> " +
    "<td>8.20 M</td> </tr>";
  
  private static final String retrievalDateHtmlFiltered =
    "";
  
  public void testTokenFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
                                 new StringInputStream(tokenHtml),
                                 Constants.DEFAULT_ENCODING);
    
    assertEquals(tokenHtmlFiltered, StringUtil.fromInputStream(actIn));
  }
    
  public void testScriptFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
                                   new StringInputStream(scriptHtml),
                                   Constants.DEFAULT_ENCODING);
    
    assertEquals(scriptHtmlFiltered, StringUtil.fromInputStream(actIn));
  }
  
  public void testNoScriptFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
                                   new StringInputStream(noScriptHtml),
                                   Constants.DEFAULT_ENCODING);
    
    assertEquals(noScriptHtmlFiltered, StringUtil.fromInputStream(actIn));
  }
  
  public void testRetrievalDateFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
                                   new StringInputStream(retrievalDateHtml),
                                   Constants.DEFAULT_ENCODING);
    
    assertEquals(retrievalDateHtmlFiltered, StringUtil.fromInputStream(actIn));
  }
  
}