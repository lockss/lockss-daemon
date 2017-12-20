/*
 * $Id$
 */

/*

Copyright (c) 2000-2017 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;

import org.lockss.util.*;
import org.lockss.config.Configuration;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.servlet.ServletUtil;
import org.lockss.test.*;

public class TestBaseAtyponHtmlLinkRewriterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  BaseAtyponHtmlLinkRewriterFactory fact;

  public void setUp() throws Exception {
    super.setUp();
    fact = new BaseAtyponHtmlLinkRewriterFactory();
  }
  
  static final String testCitForm = 
      "<form action=\"/action/downloadCitation\" name=\"frmCitmgr\" method=\"post\" target=\"_self\">" +
          "<input type=\"hidden\" name=\"doi\" value=\"10.3920/CEP160023\" />" +
          "<input type=\"hidden\" name=\"downloadFileName\" value=\"wagen_cep13_31\" />" +
          "<input type=\"hidden\" name=\"include\" value=\"abs\" />" +
          "<table summary=\"\">" +
          "<tr class=\"formats\">" +
          "<th>Format</th>" +
          "<td>" +
          "<input onclick=\"toggleImport(this);\" id=\"ris\" type=\"radio\" name=\"format\" value=\"ris\" checked=\"checked\" />" +
          "<label for=\"ris\">RIS (ProCite, Reference Manager)</label>" +
          "<br />" +
          "</td>" +
          "</tr>" +
         "<tr>" +
          "<td class=\"submit\" colspan='2'>" +
          "<input onclick=\"onCitMgrSubmit()\" class=\"formbutton\" type=\"submit\" name=\"submit\" value=\"Download article citation data\" />" +
          "</td>" +
          "</tr>" +
          "</table>" +
          "</form>";
  
  static final String testCitFormRewritten = 
      "<form " +
          "action=\"http://www.foobar.org/ServeContent?url=http%3A%2F%2Fwww.xyz.com%2Faction%2FdownloadCitation%3Fdoi%3D10.3920%252FCEP160023%26format%3Dris%26include%3Dcit\" " +
          "name=\"frmCitmgr\" method=\"post\" target=\"_self\">" +
          "<input type=\"hidden\" name=\"doi\" value=\"10.3920/CEP160023\" />" +
          "<input type=\"hidden\" name=\"downloadFileName\" value=\"wagen_cep13_31\" />" +
          "<input type=\"hidden\" name=\"include\" value=\"abs\" />" +
          "<table summary=\"\"><tr class=\"formats\">" +
          "<th>Format</th>" +
          "<td>" +
          "<input onclick=\"http://www.foobar.org/ServeContent?url=http%3A%2F%2Fwww.xyz.com%2Faction%2FtoggleImport%28this%29%3B\" id=\"ris\" type=\"radio\" name=\"format\" value=\"ris\" checked=\"checked\" />" +
          "<label for=\"ris\">RIS (ProCite, Reference Manager)</label>" +
          "<br /></td>" +
          "</tr>" +
          "<tr><td class=\"submit\" colspan='2'>" +
          "<input onclick=\"http://www.foobar.org/ServeContent?url=http%3A%2F%2Fwww.xyz.com%2Faction%2FdownloadCitation%3Fdoi%3D10.3920%252FCEP160023%26format%3Dris%26include%3Dcit\" " +
          "class=\"formbutton\" type=\"submit\" name=\"submit\" value=\"Download article citation data\" />" +
          "</td></tr></table></form>";
  
  private static final String otherTest = 
    "<form action=\"/action/downloadCitation\" name=\"frmCitmgr\" method=\"post\" target=\"_self\"> " +
    "<input type=\"hidden\" name=\"doi\" value=\"10.2514/6.2016-3143\" /> " +
    "<input type=\"hidden\" name=\"downloadFileName\" value=\"aiaa_6.2016-3143\" /> " +
    "<input type='hidden' name='include' value='cit' /> " +
    "<div style=\"text-align: center;\"> " +
     "<input type='hidden' name='direct' value=\"true\" /> " +
      "<input type='submit' name='submit' value='Download article citation data' " + 
       " onclick=\"onCitMgrSubmit()\" class=\"formbutton\"/> " +
      "</div>" +
      "</form>";      

  private static final String otherTestRewritten =
      "<form action=\"http://www.foobar.org/ServeContent?url=http%3A%2F%2Fwww.xyz.com%2Faction%2FdownloadCitation%3Fdoi%3D10.3920%252FCEP160023%26format%3Dris%26include%3Dcit\" " +
          "name=\"frmCitmgr\" method=\"post\" target=\"_self\"> " +
          "<input type=\"hidden\" name=\"doi\" value=\"10.2514/6.2016-3143\" /> " +
          "<input type=\"hidden\" name=\"downloadFileName\" value=\"aiaa_6.2016-3143\" /> " +
          "<input type='hidden' name='include' value='cit' /> " +
          "<div style=\"text-align: center;\"> " +
          "<input type='hidden' name='direct' value=\"true\" /> " +
          "<input type='submit' name='submit' value='Download article citation data'  " +
          "onclick=\"http://www.foobar.org/ServeContent?url=http%3A%2F%2Fwww.xyz.com%2Faction%2FdownloadCitation%3Fdoi%3D10.3920%252FCEP160023%26format%3Dris%26include%3Dcit\" " +
          "class=\"formbutton\"/> </div></form>";


  
  /**
   * Make a basic BaseAtypon test AU to which URLs can be added.
   * 
   */
  MockArchivalUnit makeAu() throws ConfigurationException {
    MockArchivalUnit mau = new MockArchivalUnit();
    Configuration config =ConfigurationUtil.fromArgs(
        "base_url", "http://www.xyz.com/");
    mau.setConfiguration(config);
    mau.setUrlStems(ListUtil.list(
        "http://www.xyz.com/"
        ));
    return mau;
  }


  public void testBaseLinkRewritingShowCitJavascript() throws Exception {
    MockArchivalUnit mockAu = makeAu();
    
    InputStream in = new ByteArrayInputStream(testCitForm.getBytes());
    ServletUtil.LinkTransform xfm = new ServletUtil.LinkTransform() {
      public String rewrite(String url) {
        return "http://www.foobar.org/ServeContent?url=" + url;
      }
    };
    InputStream newIn = 
        fact.createLinkRewriter("text/html", mockAu, in, "UTF-8", "http://www.xyz.com/action/showCitFormats?doi=10.3920%2FCEP160023", xfm);
    String fout = StringUtil.fromReader(new InputStreamReader(newIn, "UTF-8"));
    assertEquals(testCitFormRewritten, fout);
  }

  public void testOtherTest() throws Exception {
    MockArchivalUnit mockAu = makeAu();
    
    InputStream in = new ByteArrayInputStream(otherTest.getBytes());
    ServletUtil.LinkTransform xfm = new ServletUtil.LinkTransform() {
      public String rewrite(String url) {
        return "http://www.foobar.org/ServeContent?url=" + url;
      }
    };
    InputStream newIn = 
        fact.createLinkRewriter("text/html", mockAu, in, "UTF-8", "http://www.xyz.com/action/showCitFormats?doi=10.3920%2FCEP160023", xfm);
    String fout = StringUtil.fromReader(new InputStreamReader(newIn, "UTF-8"));
    assertEquals(otherTestRewritten,fout);
  }

}
