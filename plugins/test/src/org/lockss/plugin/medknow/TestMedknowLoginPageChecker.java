/*
 * $Id:
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
package org.lockss.plugin.medknow;
import java.io.*;

import org.lockss.daemon.PluginException;
import org.lockss.test.*;
import org.lockss.util.*;

public class TestMedknowLoginPageChecker extends LockssTestCase {

  public void testNotLoginPage() throws IOException {
    MedknowLoginPageChecker checker = new MedknowLoginPageChecker();
    try {
      assertFalse(checker.isLoginPage(new CIProperties(),
      		    new MyStringReader("blah")));
    } catch (PluginException e) {

    }
  }

  public static String downloadPageText =
      "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">" +
          "<html><head>" +
          "<!-- TAG START-->" +
          "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=windows-1252\">" +
          "<title>Article Title of One Sort : Download PDF</title>" +
          "<meta name=\"DC.Title\" content=\"Indian J Crit Care Med\">" +
          "<meta name=\"DC.Publisher.CorporateName\" content=\"Medknow Publications\">" +
          "<meta name=\"DC.Publisher.CorporateName.Address\" content=\"Mumbai, India\"> " +
          "<meta name=\"DC.Date\" content=\"1990\">" +
          "<meta name=\"DC.Format\" content=\"text/html\">" +
          "<meta name=\"DC.Type\" content=\"text.serial.Journal\">" +
          "<meta name=\"DC.Language\" content=\"(SCHEME=ISO639) en\"><!-- TAG START-->" +
          "" +
          "" +
          "</head>" +
          "<body class=\"dbody\">" +
          "There is stuff here" +
          "</body>" +
          "</html>";
  
  public static String altDownloadPageText =
      "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">" +
          "<html><head>" +
          "<!-- TAG START-->" +
          "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=windows-1252\">" +
          "<title>Article Title of One Sort : Download PDFs</title>" +
          "<meta name=\"DC.Title\" content=\"Indian J Crit Care Med\">" +
          "<meta name=\"DC.Publisher.CorporateName\" content=\"Medknow Publications\">" +
          "<meta name=\"DC.Publisher.CorporateName.Address\" content=\"Mumbai, India\"> " +
          "<meta name=\"DC.Date\" content=\"1990\">" +
          "<meta name=\"DC.Format\" content=\"text/html\">" +
          "<meta name=\"DC.Type\" content=\"text.serial.Journal\">" +
          "<meta name=\"DC.Language\" content=\"(SCHEME=ISO639) en\"><!-- TAG START-->" +
          "" +
          "" +
          "</head>" +
          "<body class=\"dbody\">" +
          "There is stuff here" +
          "</body>" +
          "</html>";

  public static String notDownloadPageText =
      "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">" +
          "<html><head>" +
          "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=windows-1252\">" +
          "<title>And article title - goes" +                                                                                                                                       
          "here but isn't a : download button page" +                                                                                                                               
          " </title>" +
          "</head>" +
          "" +
          "<body class=\"dbody\">" +
          "not a download page" +
          "</body>" +
          "</html>";
  
  public void testIsLoginPage() throws IOException {
    MedknowLoginPageChecker checker = new MedknowLoginPageChecker();
    CIProperties props = new CIProperties();
    props.put("Content-Type", "text/html; charset=windows-1252");

    MyStringReader reader = new MyStringReader(downloadPageText);

    try {
      assertTrue(checker.isLoginPage(props, reader));
    } catch (PluginException e) {

    }
  }
  
  public void testAltIsLoginPage() throws IOException {
    MedknowLoginPageChecker checker = new MedknowLoginPageChecker();
    CIProperties props = new CIProperties();
    props.put("Content-Type", "text/html; charset=windows-1252");

    MyStringReader reader = new MyStringReader(altDownloadPageText);

    try {
      assertTrue(checker.isLoginPage(props, reader));
    } catch (PluginException e) {

    }
  }

  public void testIsNotLoginPage() throws IOException {
    MedknowLoginPageChecker checker = new MedknowLoginPageChecker();
    CIProperties props = new CIProperties();
    props.put("Content-Type", "text/html; charset=windows-1252");

    MyStringReader reader = new MyStringReader(notDownloadPageText);

    try {
      assertFalse(checker.isLoginPage(props, reader));
    } catch (PluginException e) {

    }
  }


  private static class MyStringReader extends StringReader {
    boolean readCalled = false;

    public MyStringReader(String str) {
      super(str);
    }

    public int read(char[] cbuf, int off, int len) throws IOException {
      readCalled = true;
      return super.read(cbuf, off, len);
    }


  }
}
