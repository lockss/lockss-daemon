/*
 * $Id: TestPensoftHtmlHashFilterFactory.java,v 1.3 2013-07-10 21:57:00 aishizaki Exp $
 */

/*

 Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.pensoft;

import java.io.*;

import org.lockss.util.*;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.MetadataField;
import org.lockss.test.*;

public class TestPensoftHtmlHashFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private PensoftHtmlHashFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new PensoftHtmlHashFilterFactory();
    mau = new MockArchivalUnit();
  }
  // this example is from an abstract
  private static final String HtmlHashA =
    "<tr>"+
    "<td class=\"green2\" valign=\"top\">"+ // will be filtered
    "<b>doi: </b>"+   // will be filtered
    "10.3897/compcytogen.v3i1.2"+     // will be filtered
    "<br><b>Published:</b> 06.08.2009<br /><br /><b>Viewed by: </b>1106"+     // will be filtered
    "<td class=\"more3\"><font class=\"newsdata\">Abstract</font><br><br>"+
    "Hello World"+
    "<p align=\"right\">Full text:"+
    "<a class=\"more3\" href='inc/journals/download.php?fileId=1794&fileTable=J_GALLEYS'>PDF</a> </p>"+
    "</td></tr>";
 
  private static final String HtmlHashAFiltered =
    "<tr>"+
    "<td class=\"more3\"><font class=\"newsdata\">Abstract</font><br><br>"+
    "Hello World"+
    "<p align=\"right\">Full text:"+
    "<a class=\"more3\" href='inc/journals/download.php?fileId=1794&fileTable=J_GALLEYS'>PDF</a> </p>"+
    "</td></tr>"; 
  
  // this example is a little different; it's from an article
  private static final String HtmlHashB =
    "<table width=\"186\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\">"+
    "<tr> "+
    "<td class=\"texttah11\" width=\"184\">"+
    "<table class=\"texttah11\" width=\"177\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\">"+
    "<tbody>" + "<tr>"+
    "<td valign=\"top\" width=\"13\"><img src=\"img/kv.gif\" vspace=\"4\" width=\"5\" height=\"5\"></td>"+
    "<td width=\"165\" class=\"green\">Viewed by : <span class=more3 >1106</span></td>"+
    "</tr>"+
    "</tbody> </table> </td> </tr> </table>Hello World";
  private static final String HtmlHashBFiltered =
    "Hello World";
  private static final String HtmlHashC =
    "<p><!--Load time 0.240586 seconds.-->Hello World</p>"+
    "<!--SESID=\"020254c9122ebd1bf1f37e24b639181d\"-->";
  private static final String HtmlHashCFiltered =
    "<p>Hello World</p>";
  private static final String HtmlHashD =
    "<a class=\"antetka\" href='journal_home_page.php?journal_id=1&page=taxon&SESID=020254c9122ebd1bf1f37e24b639181d'>Taxon</a>"+
    "<a class=\"green\" href=\"journals/HelloWorld\">Hello World</a>";
  private static final String HtmlHashDFiltered =
    "<a class=\"green\" href=\"journals/HelloWorld\">Hello World</a>";
  private static final String HtmlHashE =
    "<input type=\"hidden\" name=\"SESID\" value=\"020254c9122ebd1bf1f37e24b639181d\">Hello World";
  private static final String HtmlHashEFiltered = "Hello World";
  private static final String HtmlHashF =
    "<iframe target=basketadd src =\"/none.php?SESID=754755beffdf3915cf0ea0ff54719eeb\""+
    "name=basketadd width=\"0\" scrolling=\"no\" frameborder=\"0\" height=\"0\">\""+
    "</iframe>Hello World";
  private static final String HtmlHashG = "<tr height=19 bgcolor=\"#f0f0E0\" onmouseover=\"this.style.backgroundColor='#fefef5';style.cursor='hand';\""+
    "onclick=\"document.location.href='journal_home_page.php?journal_id=2&page=home&SESID=020254c9122ebd1bf1f37e24b639181d';\""+
    "onmouseout=\"this.style.backgroundColor='#f0f0E0';\"></tr>Hello World";
  private static final String HtmlHashH = "<a href=\"javascript:void(0);\" "+
    "onclick=\"displayMessage2('login-form.php?SESID=020254c9122ebd1bf1f37e24b639181d',604,348);return false;\" class=\"menu\">Email/RSS Alerts</a>"+
    "Hello World";
  private static final String HtmlHashI = "<script type=\"text/javascript\">"+
    "</script>Hello World";
  private static final String HtmlHashJ = "<noscript>"+
  "</noscript>Hello World"; 
  private static final String HtmlHashK = "<td width=\"186\" valign=\"top\" class=textver10>"+
  "</td>Hello World";
  private static final String HtmlHashL = "<table width=\"186\">"+
  "</table>Hello World";
  private static final String HtmlHashM = "<div id=newscont>"+
  "</div>Hello World";
  
  public void testFilterA() throws Exception {
    InputStream inA;

    /* viewed-by test  */ 
    inA = fact.createFilteredInputStream(mau, 
          new StringInputStream(HtmlHashA), ENC);
    String filtStrA = StringUtil.fromInputStream(inA);

    assertEquals(HtmlHashAFiltered, filtStrA);
   
  }
  public void testFilterB() throws Exception {
    InputStream inB;

    inB = fact.createFilteredInputStream(mau, 
        new StringInputStream(HtmlHashB), ENC);    
    String filtStrB = StringUtil.fromInputStream(inB);
    assertEquals(HtmlHashBFiltered, filtStrB);
   
  }
  public void testFilterC() throws Exception {
    InputStream in;

    in = fact.createFilteredInputStream(mau, 
        new StringInputStream(HtmlHashC), ENC);    
    String filtStr = StringUtil.fromInputStream(in);
    assertEquals(HtmlHashCFiltered, filtStr);
   
  }
  public void testFilterD() throws Exception {
    InputStream in;

    in = fact.createFilteredInputStream(mau, 
        new StringInputStream(HtmlHashD), ENC);    
    String filtStr = StringUtil.fromInputStream(in);
    assertEquals(HtmlHashDFiltered, filtStr);
   
  }
  public void testFilterE() throws Exception {
    InputStream in;
    // all these should match, once filtered, the string HtmlHashEFiltered
    in = fact.createFilteredInputStream(mau, 
        new StringInputStream(HtmlHashE), ENC);    
    String filtStr = StringUtil.fromInputStream(in);
    assertEquals(HtmlHashEFiltered, filtStr);
    in = fact.createFilteredInputStream(mau, 
        new StringInputStream(HtmlHashF), ENC);    
    filtStr = StringUtil.fromInputStream(in);
    assertEquals(HtmlHashEFiltered, filtStr);
    in = fact.createFilteredInputStream(mau, 
        new StringInputStream(HtmlHashG), ENC);    
    filtStr = StringUtil.fromInputStream(in);
    assertEquals(HtmlHashEFiltered, filtStr);
    in = fact.createFilteredInputStream(mau, 
        new StringInputStream(HtmlHashH), ENC);    
    filtStr = StringUtil.fromInputStream(in);
    assertEquals(HtmlHashEFiltered, filtStr);
    in = fact.createFilteredInputStream(mau, 
        new StringInputStream(HtmlHashI), ENC);    
    filtStr = StringUtil.fromInputStream(in);
    assertEquals(HtmlHashEFiltered, filtStr);
    in = fact.createFilteredInputStream(mau, 
        new StringInputStream(HtmlHashJ), ENC);    
    filtStr = StringUtil.fromInputStream(in);
    assertEquals(HtmlHashEFiltered, filtStr);
    in = fact.createFilteredInputStream(mau, 
        new StringInputStream(HtmlHashK), ENC);    
    filtStr = StringUtil.fromInputStream(in);
    assertEquals(HtmlHashEFiltered, filtStr);
    in = fact.createFilteredInputStream(mau, 
        new StringInputStream(HtmlHashL), ENC);    
    filtStr = StringUtil.fromInputStream(in);
    assertEquals(HtmlHashEFiltered, filtStr);
    in = fact.createFilteredInputStream(mau, 
        new StringInputStream(HtmlHashM), ENC);    
    filtStr = StringUtil.fromInputStream(in);
    assertEquals(HtmlHashEFiltered, filtStr);
  }

}