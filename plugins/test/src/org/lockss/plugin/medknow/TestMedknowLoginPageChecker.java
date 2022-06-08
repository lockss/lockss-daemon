/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.medknow;

import java.io.*;

import org.lockss.daemon.PluginException;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.CacheException;

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
      // old return value would be true, which would result in fatal error
      assertTrue(checker.isLoginPage(props, reader));
    } catch (CacheException e) {
      assertClass(CacheException.UnexpectedNoRetryFailException.class, e);
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
    } catch (CacheException e) {
      assertClass(CacheException.UnexpectedNoRetryFailException.class, e);
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
