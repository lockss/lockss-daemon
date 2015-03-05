/*
 * $Id$
 */

/*

Copyright (c) 2014 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.util;

import org.apache.commons.lang3.tuple.Pair;
import org.lockss.test.LockssTestCase;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;

/** 
* CharsetUtil Tester. 
* @version 1.0
*/ 
public class TestCharsetUtil extends LockssTestCase {
   static final byte[] UTF8_BOM =
     {(byte)0xEF,(byte)0xBB,(byte)0xBF};
   static final byte[] UTF16_BOM_BE =
     {(byte)0xFE,(byte)0xFF};
   static final byte[] UTF16_BOM_LE =
     {(byte)0xFF,(byte)0xFE};
   static final byte[] UTF32_BOM_BE =
     {(byte)0x00,(byte)0x00,(byte)0xFE,(byte)0xFF};
   static final byte[] UTF32_BOM_LE =
     {(byte)0xFF,(byte)0xFE,(byte)0x00,(byte)0x00};
   static final byte[] UTF7_BOM_v1 =
     {(byte)0x2B,(byte)0x2F,(byte)0x76,(byte)0x38};
   static final byte[] UTF7_BOM_v2 =
     {(byte)0x2B,(byte)0x2F,(byte)0x76,(byte)0x39};
   static final byte[] UTF7_BOM_v3 =
     {(byte)0x2B,(byte)0x2F,(byte)0x76,(byte)0x2B};
   static final byte[] UTF7_BOM_v4 =
     {(byte)0x2B,(byte)0x2F,(byte)0x76,(byte)0x2F};
   static final byte[] UTF1_BOM =
     {(byte)0xF7,(byte)0x64,(byte)0x4C};

  static final String  HTML_HEADER =
    "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"\n" +
    "        \"http://www.w3.org/TR/html4/loose.dtd\">\n" +
    "<HTML>\n" +
    "\n" +
    "<head>\n" +
    "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">\n" +
    "<title>\n" +
    "Test display of HTML elements\n" +
    "</title>\n" +
    "</head>";

  String HTML_FILE =
    "<HTML>\n" +
    "<HEAD>\n" +
    "<TITLE>HTML DOCUMENT TEST</TITLE>\n" +
    "</HEAD>\n" +
    "<BODY BGCOLOR=\"FFFFFF\">\n" +
    "<CENTER><IMG SRC=\"clouds.jpg\" ALIGN=\"BOTTOM\"> </CENTER>\n" +
    "<HR>\n" +
    "<a href=\"http://somegreatsite.com\">Link Name</a>\n" +
    "is a link to another nifty site\n" +
    "<H1>This is a Header</H1>\n" +
    "<H2>This is a Medium Header</H2>\n" +
    "<h2>Character test</h2>\n" +
    "<p>The following table has some sample characters with\n" +
    "annotations. If the browser&#8217;s default font does not\n" +
    "contain all of them, they may get displayed using backup fonts.\n" +
    "This may cause stylistic differences, but it should not\n" +
    "prevent the characters from being displayed at all.</p>\n" +
    "\n" +
    "<table>\n" +
    "<tr><th>Char. <th>Explanation <th>Notes\n" +
    "<tr><td>Ãª <td>e with circumflex <td>Latin 1 character, should be ok\n" +
    "<tr><td>&#8212; <td>em dash <td>Windows Latin 1 character, should be ok, too\n" +
    "<tr><td>&#x100; <td>A with macron (line above) <td>Latin Extended-A character, not present in all fonts\n" +
    "<tr><td>&Omega; <td>capital omega <td>A Greek letter\n" +
    "<tr><td>&#x2212; <td>minus sign <td>Unicode minus\n" +
    "<tr><td>&#x2300; <td>diameter sign <td>relatively rare in fonts\n" +
    "</table>\n" +
    "<P> This is a new paragraph!\n" +
    "<P> <B>This is a new paragraph!</B>\n" +
    "<BR> <B><I>This is a new sentence without a paragraph break, in bold italics.</I></B>\n" +
    "<HR>\n" +
    "</BODY>\n" +
    "</HTML>\n";

  String HTML_FILE_NOT_UTF =
    "<HTML>\n" +
    "<HEAD>\n" +
    "<TITLE>HTML DOCUMENT TEST</TITLE>\n" +
    "</HEAD>\n" +
    "<BODY BGCOLOR=\"FFFFFF\">\n" +
    "<CENTER><IMG SRC=\"clouds.jpg\" ALIGN=\"BOTTOM\"> </CENTER>\n" +
    "<HR>\n" +
    "<a href=\"http://somegreatsite.com\">Link Name</a>\n" +
    "is a link to another nifty site\n" +
    "<H1>This is a Header</H1>\n" +
    "<H2>This is a Medium Header</H2>\n" +
    "<h2>Character test</h2>\n" +
    "<p>The following table has some sample characters with\n" +
    "annotations. If the browser&#8217;s default font does not\n" +
    "contain all of them, they may get displayed using backup fonts.\n" +
    "This may cause stylistic differences, but it should not\n" +
    "prevent the characters from being displayed at all.</p>\n" +
    "\n" +
    "<table>\n" +
    "<tr><th>Char. <th>Explanation <th>Notes\n" +
    "<tr><td>&#A2; <td> a accent grave<td> accent mark\n " +
    "<tr><td>&#FD; <td>one half superscriptn<td> one-half\n" +
    "<tr><td>&#E4; <td>euro <td>The Euro Sign<td> The Euro sign\n"+
    "<tr><td>&#A9; <td>copyright sign <td>Copyright Sign\n" +
    "<tr><td>&#BF; <td>Greek<td> Greek letter\n" +
    "</table>\n" +
    "<P> This is a new paragraph!\n" +
    "<P> <B>This is a new paragraph!</B>\n" +
    "<BR> <B><I>This is a new sentence without a paragraph break, in bold italics.</I></B>\n" +
    "<HR>\n" +
    "</BODY>\n" +
    "</HTML>\n";

  File mTempDir;

  public void setUp() throws Exception {
      super.setUp();
      mTempDir = FileUtil.createTempDir("testCharsetUtil","");
   }

   public void tearDown() throws Exception {
     FileUtil.delTree(mTempDir);
     super.tearDown();
   }


  /**
   * Method: guessCharsetFromBytes(byte[] bytes)
   */
  public void testGuessCharsetFromBytes() throws Exception {
    byte[] html_bytes = HTML_FILE.getBytes();
    // 1) a utf-8 document with no declared encoding
    assertEquals("UTF-8",
                 CharsetUtil.guessCharsetFromBytes(html_bytes));

    //2 a utf-8 document encoded as utf-8
    html_bytes = HTML_FILE.getBytes("UTF-8");
    assertEquals("UTF-8", CharsetUtil.guessCharsetFromBytes(html_bytes));

    //3 utf-16 encoded text
    html_bytes = HTML_FILE.getBytes("UTF-16");
    assertEquals("UTF-16BE", CharsetUtil.guessCharsetFromBytes(html_bytes));

    //4 iso-8859-1

    // MacRoman
  }

  /**
   * Method: guessCharsetFromStream(InputStream in)
   */
  public void testGuessCharsetFromStream() throws Exception {
    // 1) a utf-8 document with no declared encoding
    ByteArrayInputStream bais =
      new ByteArrayInputStream(HTML_FILE.getBytes());
    assertEquals("UTF-8", CharsetUtil.guessCharsetFromStream(bais));
    //2 a utf-8 document encoded as utf-8
    bais =
      new ByteArrayInputStream(HTML_FILE.getBytes("UTF-8"));

    assertEquals("UTF-8", CharsetUtil.guessCharsetFromStream(bais));

    // utf-8 encoded as utf-16
    bais =
      new ByteArrayInputStream(HTML_FILE.getBytes("UTF-16"));
    assertEquals("UTF-16BE", CharsetUtil.guessCharsetFromStream(bais));

    bais =
      new ByteArrayInputStream(HTML_FILE_NOT_UTF.getBytes("iso-8859-1"));
    assertEquals("ISO-8859-1", CharsetUtil.guessCharsetFromStream(bais,
                                                             "ISO-8859-1"));


  }

  /**
   * Method: findCharsetInText(byte[] buf, int len)
   */
  public void testFindCharsetInText() throws Exception {
    String html1 = "<!DOCTYPE html>\n" +
                   "<html>\n" +
                   "<head>\n" +
                   "<meta charset=\"UTF-8\">\n" +
                   "</head>\n" +
                   "<body>\n" +
                   "<h1>My Website</h1>\n" +
                   "<p>Some text...</p>\n" +
                   "</body>\n" +
                   "</html>\n";
    String html2 = "<!DOCTYPE html>\n" +
                   "<html>\n" +
                   "\n" +
                   "<head>\n" +
                   "<TITLE>CHARSET=\"UTF-8\"</TITLE>\n" +
                   "</head>\n" +
                   "\n" +
                   "<body>\n" +
                   "<h1>My Website</h1>\n" +
                   "<p>Some text...</p>\n" +
                   "</body>\n" +
                   "\n" +
                   "</html>\n";
    String xml1 ="<?xml version=\"1.0\"?>\n";
    String xml2 ="<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
    String xml3
      = "<?xml version=\"1.0\" encoding=\"UTF-16\" standalone=\"yes\"?>";

    // this has a html4 content-type charset
    byte[] buf = HTML_HEADER.getBytes();
    assertEquals("ISO-8859-1", CharsetUtil.findCharsetInText(buf, buf.length));

    // this has a html5 charset=  utf-8
    buf = html1.getBytes();
    assertEquals("UTF-8",CharsetUtil.findCharsetInText(buf,buf.length));

    // this has a charset= in the title so it's ignored.
    buf = html2.getBytes();
    assertEquals(null,CharsetUtil.findCharsetInText(buf,buf.length));

    //xml no declaration
    buf = xml1.getBytes();
    assertEquals(null,CharsetUtil.findCharsetInText(buf,buf.length));

    // xml utf 8 declaration
    buf = xml2.getBytes();
    assertEquals("UTF-8",CharsetUtil.findCharsetInText(buf,buf.length));
    // xml utf 16 declaration & standalone attribute
    buf = xml3.getBytes();
    assertEquals("UTF-16",CharsetUtil.findCharsetInText(buf,buf.length));
  }

  /**
   * Method: joinStreamsWithCharset(byte[] buf,
   *                                InputStream tail, String charset)
   */
  public void testJoinStreamsWithCharset() throws Exception {
    byte[] buf = new byte[100];    // create an input stream
    String buf_string;
    ByteArrayInputStream bais =
      new ByteArrayInputStream(HTML_FILE.getBytes("UTF-8"));
    // read the first 100 bytes.
    int in_length = bais.available();
    bais.read(buf,0,buf.length);
    buf_string = new String(buf,"UTF-8");
    int str_length = buf_string.length();
    assertEquals(in_length - buf.length, bais.available());
    // create a new CharsetReader
    InputStream is = CharsetUtil.joinStreamsWithCharset(buf,bais,"UTF-8");
    Reader rdr = new InputStreamReader(is,"UTF-8");
    char[] charbuf = new char[str_length];
    // read in the chars that we already read...
    rdr.read(charbuf);
    assertEquals(buf_string.toCharArray(), charbuf);
  }

  /**
    * Method: supportedCharsetName(String s)
    */
   public void testSupportedCharsetName() throws Exception {
     // a bogus charset name should return null
     assertNull(CharsetUtil.supportedCharsetName("bogus"));
     // allowable utf 8 variants
     //lower case works
     assertEquals("UTF-8", CharsetUtil.supportedCharsetName("utf-8"));
     // the older java.io form works
     assertEquals("UTF-8", CharsetUtil.supportedCharsetName("utf8"));
     // and the usual standard uppercase works
     assertEquals("UTF-8", CharsetUtil.supportedCharsetName("UTF-8"));
     assertEquals("UTF-32", CharsetUtil.supportedCharsetName("UTF_32"));

   }

   /**
    * Method: isAlnum(byte b)
    */
   public void testIsAlnum() throws Exception {
      byte alpha = 'a';
      byte num = '1';
      byte space = ' ';
      byte tab = '\t';
      assertTrue(CharsetUtil.isAlnum(alpha));
      assertTrue(CharsetUtil.isAlnum(num));
      assertFalse(CharsetUtil.isAlnum(space));
      assertFalse(CharsetUtil.isAlnum(tab));
   }

   /**
    * Method: isSpace(byte b)
    */
   public void testIsSpace() throws Exception {
      byte alpha = 'a';
      byte num = '1';
      byte space = ' ';
      byte tab = '\t';
      assertFalse(CharsetUtil.isSpace(alpha));
      assertFalse(CharsetUtil.isSpace(num));
      assertTrue(CharsetUtil.isSpace(space));
      assertTrue(CharsetUtil.isSpace(tab));
   }

   public void testHasUtf8BOM() throws Exception {
      assertTrue(CharsetUtil.hasUtf8BOM(UTF8_BOM, UTF8_BOM.length));
      assertFalse(CharsetUtil.hasUtf8BOM(UTF16_BOM_BE, UTF16_BOM_BE.length));
      assertFalse(CharsetUtil.hasUtf8BOM(UTF16_BOM_LE, UTF16_BOM_LE.length));
      assertFalse(CharsetUtil.hasUtf8BOM(UTF32_BOM_BE, UTF32_BOM_BE.length));
      assertFalse(CharsetUtil.hasUtf8BOM(UTF32_BOM_LE, UTF32_BOM_LE.length));
      assertFalse(CharsetUtil.hasUtf8BOM(UTF7_BOM_v1, UTF7_BOM_v1.length));
      assertFalse(CharsetUtil.hasUtf8BOM(UTF7_BOM_v2, UTF7_BOM_v2.length));
      assertFalse(CharsetUtil.hasUtf8BOM(UTF7_BOM_v3, UTF7_BOM_v3.length));
      assertFalse(CharsetUtil.hasUtf8BOM(UTF7_BOM_v4, UTF7_BOM_v4.length));
     assertFalse(CharsetUtil.hasUtf8BOM(UTF1_BOM, UTF1_BOM.length));
   }

   public void testHasUtf16BEBOM() throws Exception {
      assertFalse(CharsetUtil.hasUtf16BEBOM(UTF8_BOM, UTF8_BOM.length));
      assertTrue(CharsetUtil.hasUtf16BEBOM(UTF16_BOM_BE, UTF16_BOM_BE.length));
      assertFalse(CharsetUtil.hasUtf16BEBOM(UTF16_BOM_LE, UTF16_BOM_LE.length));
      assertFalse(CharsetUtil.hasUtf16BEBOM(UTF32_BOM_BE, UTF32_BOM_BE.length));
      assertFalse(CharsetUtil.hasUtf16BEBOM(UTF32_BOM_LE, UTF32_BOM_LE.length));
      assertFalse(CharsetUtil.hasUtf16BEBOM(UTF7_BOM_v1, UTF7_BOM_v1.length));
      assertFalse(CharsetUtil.hasUtf16BEBOM(UTF7_BOM_v2, UTF7_BOM_v2.length));
      assertFalse(CharsetUtil.hasUtf16BEBOM(UTF7_BOM_v3, UTF7_BOM_v3.length));
      assertFalse(CharsetUtil.hasUtf16BEBOM(UTF7_BOM_v4, UTF7_BOM_v4.length));
      assertFalse(CharsetUtil.hasUtf16BEBOM(UTF1_BOM, UTF1_BOM.length));
   }

   public void testHasUtf16LEBOM() throws Exception {
      assertFalse(CharsetUtil.hasUtf16LEBOM(UTF8_BOM, UTF8_BOM.length));
      assertFalse(CharsetUtil.hasUtf16LEBOM(UTF16_BOM_BE, UTF16_BOM_BE.length));
      assertTrue(CharsetUtil.hasUtf16LEBOM(UTF16_BOM_LE, UTF16_BOM_LE.length));
      assertFalse(CharsetUtil.hasUtf16LEBOM(UTF32_BOM_BE, UTF32_BOM_BE.length));
      // because utf32Le and utf16le have the same opening bytes this is true
     assertTrue(CharsetUtil.hasUtf16LEBOM(UTF32_BOM_LE, UTF32_BOM_LE.length));
      assertFalse(CharsetUtil.hasUtf16LEBOM(UTF7_BOM_v1, UTF7_BOM_v1.length));
      assertFalse(CharsetUtil.hasUtf16LEBOM(UTF7_BOM_v2, UTF7_BOM_v2.length));
      assertFalse(CharsetUtil.hasUtf16LEBOM(UTF7_BOM_v3, UTF7_BOM_v3.length));
      assertFalse(CharsetUtil.hasUtf16LEBOM(UTF7_BOM_v4, UTF7_BOM_v4.length));
      assertFalse(CharsetUtil.hasUtf16LEBOM(UTF1_BOM, UTF1_BOM.length));
   }

   public void testHasUtf32BEBOM() throws Exception {
      assertFalse(CharsetUtil.hasUtf32BEBOM(UTF8_BOM, UTF8_BOM.length));
      assertFalse(CharsetUtil.hasUtf32BEBOM(UTF16_BOM_BE, UTF16_BOM_BE.length));
      assertFalse(CharsetUtil.hasUtf32BEBOM(UTF16_BOM_LE, UTF16_BOM_LE.length));
      assertTrue(CharsetUtil.hasUtf32BEBOM(UTF32_BOM_BE, UTF32_BOM_BE.length));
      assertFalse(CharsetUtil.hasUtf32BEBOM(UTF32_BOM_LE, UTF32_BOM_LE.length));
      assertFalse(CharsetUtil.hasUtf32BEBOM(UTF7_BOM_v1, UTF7_BOM_v1.length));
      assertFalse(CharsetUtil.hasUtf32BEBOM(UTF7_BOM_v2, UTF7_BOM_v2.length));
      assertFalse(CharsetUtil.hasUtf32BEBOM(UTF7_BOM_v3, UTF7_BOM_v3.length));
      assertFalse(CharsetUtil.hasUtf32BEBOM(UTF7_BOM_v4, UTF7_BOM_v4.length));
      assertFalse(CharsetUtil.hasUtf32BEBOM(UTF1_BOM, UTF1_BOM.length));
   }

   public void testHasUtf32LEBOM() throws Exception {
      assertFalse(CharsetUtil.hasUtf32LEBOM(UTF8_BOM, UTF8_BOM.length));
      assertFalse(CharsetUtil.hasUtf32LEBOM(UTF16_BOM_BE, UTF16_BOM_BE.length));
      assertFalse(CharsetUtil.hasUtf32LEBOM(UTF16_BOM_LE, UTF16_BOM_LE.length));
      assertFalse(CharsetUtil.hasUtf32LEBOM(UTF32_BOM_BE, UTF32_BOM_BE.length));
      assertTrue(CharsetUtil.hasUtf32LEBOM(UTF32_BOM_LE, UTF32_BOM_LE.length));
      assertFalse(CharsetUtil.hasUtf32LEBOM(UTF7_BOM_v1, UTF7_BOM_v1.length));
      assertFalse(CharsetUtil.hasUtf32LEBOM(UTF7_BOM_v2, UTF7_BOM_v2.length));
      assertFalse(CharsetUtil.hasUtf32LEBOM(UTF7_BOM_v3, UTF7_BOM_v3.length));
      assertFalse(CharsetUtil.hasUtf32LEBOM(UTF7_BOM_v4, UTF7_BOM_v4.length));
      assertFalse(CharsetUtil.hasUtf32LEBOM(UTF1_BOM, UTF1_BOM.length));
   }

   public void testHasUtf7BOM() throws Exception {
      assertFalse(CharsetUtil.hasUtf7BOM(UTF8_BOM, UTF8_BOM.length));
      assertFalse(CharsetUtil.hasUtf7BOM(UTF16_BOM_BE, UTF16_BOM_BE.length));
      assertFalse(CharsetUtil.hasUtf7BOM(UTF16_BOM_LE, UTF16_BOM_LE.length));
      assertFalse(CharsetUtil.hasUtf7BOM(UTF32_BOM_BE, UTF32_BOM_BE.length));
      assertFalse(CharsetUtil.hasUtf7BOM(UTF32_BOM_LE, UTF32_BOM_LE.length));
      assertTrue(CharsetUtil.hasUtf7BOM(UTF7_BOM_v1, UTF7_BOM_v1.length));
      assertTrue(CharsetUtil.hasUtf7BOM(UTF7_BOM_v2, UTF7_BOM_v2.length));
      assertTrue(CharsetUtil.hasUtf7BOM(UTF7_BOM_v3, UTF7_BOM_v3.length));
      assertTrue(CharsetUtil.hasUtf7BOM(UTF7_BOM_v4, UTF7_BOM_v4.length));
      assertFalse(CharsetUtil.hasUtf7BOM(UTF1_BOM, UTF1_BOM.length));

   }

   public void testHasUtf1BOM() throws Exception {
      assertFalse(CharsetUtil.hasUtf1BOM(UTF8_BOM, UTF8_BOM.length));
      assertFalse(CharsetUtil.hasUtf1BOM(UTF16_BOM_BE, UTF16_BOM_BE.length));
      assertFalse(CharsetUtil.hasUtf1BOM(UTF16_BOM_LE, UTF16_BOM_LE.length));
      assertFalse(CharsetUtil.hasUtf1BOM(UTF32_BOM_BE, UTF32_BOM_BE.length));
      assertFalse(CharsetUtil.hasUtf1BOM(UTF32_BOM_LE, UTF32_BOM_LE.length));
      assertFalse(CharsetUtil.hasUtf1BOM(UTF7_BOM_v1, UTF7_BOM_v1.length));
      assertFalse(CharsetUtil.hasUtf1BOM(UTF7_BOM_v2, UTF7_BOM_v2.length));
      assertFalse(CharsetUtil.hasUtf1BOM(UTF7_BOM_v3, UTF7_BOM_v3.length));
      assertFalse(CharsetUtil.hasUtf1BOM(UTF7_BOM_v4, UTF7_BOM_v4.length));
      assertTrue(CharsetUtil.hasUtf1BOM(UTF1_BOM, UTF1_BOM.length));
   }
   public final void testEmptyDocument() throws IOException {
      assertCharset("", new byte[0], "UTF-8");
   }

   public final void testMetaHttpEquiv() throws IOException {
      String metaInputUtf8 = (
                               ""
                               + "<html>"
                               + "<head>"
                               + "<meta http-equiv=\"Content-type\" value=\"text/html;charset=UTF-8\">"
                               + "</head>"
                               + "<body>Hello, World!</body>"
                               + "</html>");
      assertCharset(metaInputUtf8, metaInputUtf8.getBytes("UTF-8"), "UTF-8");
      String metaInputUtf16BE = (
                                  ""
                                  + "<html>"
                                  + "<head>"
                                  + "<meta http-equiv=\"Content-type\""
                                  + " value=\"text/html;charset =UTF-16BE\">"
                                  + "</head>"
                                  + "<body>Hello, World!</body>"
                                  + "</html>");
      assertCharset(metaInputUtf16BE, metaInputUtf16BE.getBytes("UTF-16BE"),
                    "UTF-16BE");
      String metaInputUtf16LE = (
                                  ""
                                  + "<html>"
                                  + "<head>"
                                  + "<meta http-equiv=\"Content-type\""
                                  + " value=\"text/html;charset= 'UTF-16LE\">"
                                  + "</head>"
                                  + "<body>Hello, World!</body>"
                                  + "</html>");
      assertCharset(
                     metaInputUtf16LE, metaInputUtf16LE.getBytes("UTF-16LE"), "UTF-16LE");
   }

   public final void testBOM() throws IOException {
      String html = "<html>Hello, World!</html>";
      String htmlWithBom = "\ufeff" + html;
      assertCharset(html, htmlWithBom.getBytes("UTF-8"), "UTF-8");
      assertCharset(html, htmlWithBom.getBytes("UTF-16LE"), "UTF-16LE");
      assertCharset(html, htmlWithBom.getBytes("UTF-16BE"), "UTF-16BE");
      if (CharsetUtil.supportedCharsetName("UTF-32LE") != null) {
         assertCharset(html, htmlWithBom.getBytes("UTF-32LE"), "UTF-32LE");
         assertCharset(html, htmlWithBom.getBytes("UTF-32BE"), "UTF-32BE");
      }
      if (CharsetUtil.supportedCharsetName("UTF-7") != null) {
         assertCharset(html, htmlWithBom.getBytes("UTF-7"), "UTF-7");
      }
      if (CharsetUtil.supportedCharsetName("UTF-1") != null) {
         assertCharset(html, htmlWithBom.getBytes("UTF-1"), "UTF-1");
      }
   }

   public final void testCharsetInXmlHeader() throws IOException {
      String html = "<html>Hello, World!</html>";
      String xmlUtf8 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + html;
      assertCharset(xmlUtf8, xmlUtf8.getBytes("UTF-8"), "UTF-8");
      String xmlUtf16BE = "<?xml version=\"1.0\" encoding=\"UTF-16BE\"?>" + html;
      assertCharset(xmlUtf16BE, xmlUtf16BE.getBytes("UTF-16BE"), "UTF-16BE");
      String xmlUtf16LE = "<?xml version=\"1.0\" encoding=\"UTF-16LE\"?>" + html;
      assertCharset(xmlUtf16LE, xmlUtf16LE.getBytes("UTF-16LE"), "UTF-16LE");
   }

    public final void testCharsetNotInHeader() throws IOException {
      String charset;
      BufferedInputStream bis;
      File encFile = writeEncodedFile(HTML_FILE_NOT_UTF, "ISO-8859-1");
      bis = new BufferedInputStream(new FileInputStream(encFile));
      charset = CharsetUtil.guessCharsetName(bis);
      assertEquals("ISO-8859-1", charset);

      encFile = writeEncodedFile(HTML_FILE, "UTF-8");
      bis = new BufferedInputStream(new FileInputStream(encFile));
      charset = CharsetUtil.guessCharsetName(bis);
      assertEquals("UTF-8", charset);

    }

   public final void testCharsetInText() throws IOException {
      String html = (
                      ""
                      + "<html>"
                      + "<head>"
                      + "<title>charset=UTF-16LE</title>"
                      + "</head>"
                      + "<body>"
                      + "Hello, World!"
                      + "</body>"
                      + "</html>");

      for (String encoding : new String[] { "UTF-8", "UTF-16LE", "UTF-16BE" }) {
         assertCharset(html, ("\ufeff" + html).getBytes(encoding), encoding);
      }
   }

   private static void assertCharset(String golden, byte[] bytes,
                                     String expectedCharset)
     throws IOException {
      Pair<Reader, String> p =
        CharsetUtil.getCharsetReader(new ByteArrayInputStream(bytes));
      assertEquals(expectedCharset, p.getRight());
      StringBuilder sb = new StringBuilder();
      char[] buf = new char[1024];
      for (int n; (n = p.getLeft().read(buf)) > 0;) {
         sb.append(buf, 0, n);
      }
      assertEquals(golden, sb.toString());
   }

  private File writeEncodedFile(String content, String enc) throws IOException {
    File retFile = FileUtil.createTempFile("charset",enc, mTempDir);
    OutputStreamWriter osw = new OutputStreamWriter(
                                 new FileOutputStream(retFile),
                                 Charset.forName(enc).newEncoder());
    osw.write(content);
    osw.flush();
    osw.close();
    return retFile;
  }
}