/*
 * $Id:
 */

/*

Copyright (c) 2017 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.plugin.bmc;

import java.io.*;

import org.lockss.daemon.PluginException;
import org.lockss.test.*;
import org.lockss.util.*;

public class TestBMCLoginPageChecker extends LockssTestCase {
  
  public static String goodPageText = "" +
      "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">" +
      "<html><head>" +
      "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=windows-1252\">" +
      "<title>Article Title of One Sort : Download PDF</title>" +
      "<meta name=\"DC.Title\" content=\"Crit Care\">" +
      "<meta name=\"DC.Publisher.CorporateName\" content=\"BMC Publications\">" +
      "<meta name=\"DC.Date\" content=\"1990\">" +
      "<meta name=\"DC.Format\" content=\"text/html\">" +
      "</head>" +
      "<body class=\"dbody\">" +
      "There is stuff here\n" +
      "Even mentions\n" +
      "<h2>Header text</h2>" +
      "subscription required, subscribers, and existing subscribers" +
      "</body>" +
      "</html>";
  
  public static String loginPageText = "" +
      "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">" +
          "<html><head>" +
          "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=windows-1252\">" +
          "<title>Article Title of One Sort : Download PDF</title>" +
          "<meta name=\"DC.Title\" content=\"Crit Care\">" +
          "<meta name=\"DC.Publisher.CorporateName\" content=\"BMC Publications\">" +
          "<meta name=\"DC.Date\" content=\"1990\">" +
          "<meta name=\"DC.Format\" content=\"text/html\">" +
          "</head>" +
          "<body class=\"dbody\">" +
          "There is stuff here" +
          "<div class=\"msg msg-info\">\n" + 
          "  <div class=\"msg-info-img\"></div><div class=\"msg-text\">\n" + 
          "  <h2>Subscription required</h3>\n" + 
          "  <p>All research articles in <em>Critical Care</em> are open access." +
          " The journal also publishes a range of other articles that are available to" +
          " <a href=\"/subscriptions\">subscribers</a>.</p>\n" + 
          "  </div>\n" + 
          "</div>\n" + 
          "</body>" +
          "</html>";

  public static String altLoginPageText =
      "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">" +
          "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=windows-1252\">" +
          "<title>Article Title of One Sort : Download PDF</title>" +
          "<meta name=\"DC.Title\" content=\"Crit Care\">" +
          "<meta name=\"DC.Publisher.CorporateName\" content=\"BMC Publications\">" +
          "<meta name=\"DC.Date\" content=\"1990\">" +
          "<meta name=\"DC.Format\" content=\"text/html\">" +
          "<body class=\"dbody\">" +
          "<h2>Existing subscribers</h2>\n" + 
          "<form method=\"post\" action=\"#\">\n" + 
          "  <input type=\"hidden\" id=\"url\" name=\"url\" value=\"/content/16/4/136\" />" +
          "<fieldset class=\"details\" id=\"subscription-log-in\">\n" + 
          "</fieldset>\n" + 
          "<div class=\"right\">\n" + 
          "<a href=\"/authenticate/athens?returnURL=/content/16/4/136\" id=\"athens-logon\">Logon via Athens</a>\n" + 
          "<button id=\"logon_button\" value=\"Log on\" type=\"submit\" class=\"w62\" name=\"\">Log on</button>\n" + 
          "</div>" +
          "</form>\n" + 
          "</body>" +
          "</html>";
  
  public void testNotLoginPage() throws IOException {
    BMCLoginPageChecker checker = new BMCLoginPageChecker();
    try {
      assertFalse(checker.isLoginPage(new CIProperties(),
          new StringReader("blah")));
    } catch (PluginException e) {
    }
  }
  
  public void testIsLoginPage() throws IOException {
    BMCLoginPageChecker checker = new BMCLoginPageChecker();
    CIProperties props = new CIProperties();
    props.put("Content-Type", "text/html; charset=windows-1252");
    
    StringReader reader = new StringReader(loginPageText);
    
    try {
      assertTrue(checker.isLoginPage(props, reader));
    } catch (PluginException e) {
    }
  }
  
  public void testAltIsLoginPage() throws IOException {
    BMCLoginPageChecker checker = new BMCLoginPageChecker();
    CIProperties props = new CIProperties();
    props.put("Content-Type", "text/html; charset=windows-1252");
    
    StringReader reader = new StringReader(altLoginPageText);
    
    try {
      assertTrue(checker.isLoginPage(props, reader));
    } catch (PluginException e) {
    }
  }
  
  public void testIsNotLoginPage() throws IOException {
    BMCLoginPageChecker checker = new BMCLoginPageChecker();
    CIProperties props = new CIProperties();
    props.put("Content-Type", "text/html; charset=windows-1252");
    
    StringReader reader = new StringReader(goodPageText);
    
    try {
      assertFalse(checker.isLoginPage(props, reader));
    } catch (PluginException e) {
    }
  }
  
  private static class MyStringReader extends StringReader {
    
    public MyStringReader(String str) {
      super(str);
    }
    
    public int read(char[] cbuf, int off, int len) throws IOException {
      return super.read(cbuf, off, len);
    }
    
  }
}
