/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.util;

import java.io.*;
import java.nio.charset.Charset;
import org.apache.commons.lang3.*;

import org.lockss.test.*;
import org.lockss.plugin.*;

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

  static final String  HTML_FRAGMENT =
    "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"\n" +
    "        \"http://www.w3.org/TR/html4/loose.dtd\">\n" +
    "<HTML>\n" +
    "\n" +
    "<head>\n";

  String metaTag(String charset) {
    return "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=" +
      charset + "\">\n";
  }


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

  static final String HTML_FILE =
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

  static final String HTML_FILE_NOT_UTF =
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

  static final String NO_CHARSET_HTML =
    "<HTML>\n" +
    "\n" +
    "<head>\n" +
     "</head>\n" +
     "<body>\n" +
     "<h1>My Website</h1>\n" +
     "<p>Some text...</p>\n" +
     "</body>\n" +
     "</html>\n";



  /**
   * Method: guessCharsetFromBytes(byte[] bytes)
   */
  public void testGuessCharsetFromBytes() throws Exception {
    byte[] html_bytes = HTML_FILE.getBytes(Constants.DEFAULT_ENCODING);
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
      new ByteArrayInputStream(HTML_FILE.getBytes(Constants.DEFAULT_ENCODING));
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

  static final String URL1 = "http://u.r/l";

  public void testCu() throws IOException {
    MockCachedUrl mcu = new MockCachedUrl(URL1);
    InputStream is =
      new ByteArrayInputStream(HTML_FILE_NOT_UTF.getBytes("iso-8859-1"));
    mcu.storeContent(is);
    CharsetUtil.InputStreamAndCharset isc = CharsetUtil.getCharsetStream(mcu);
    assertEquals("ISO-8859-1", isc.getCharset());
    // Should always call getUncompressedInputStream()
    assertTrue(mcu.getUncompressedCalled());

    // Should ignore CU-specified charset
    mcu = new MockCachedUrl(URL1);
    is = new ByteArrayInputStream(HTML_FILE_NOT_UTF.getBytes("iso-8859-1"));
    CIProperties ctype = new CIProperties();
    ctype.put(CachedUrl.PROPERTY_CONTENT_TYPE, "UTF-8");
    mcu.storeContent(is, ctype);
    isc = CharsetUtil.getCharsetStream(mcu);
    assertEquals("ISO-8859-1", isc.getCharset());
    assertTrue(mcu.getUncompressedCalled());
    
    mcu = new MockCachedUrl(URL1);
    byte [] conc = ArrayUtils.addAll(UTF16_BOM_LE,
				     NO_CHARSET_HTML.getBytes("ISO-8859-1"));
    is =  new ByteArrayInputStream(conc);
    ctype = new CIProperties();
    ctype.put(CachedUrl.PROPERTY_CONTENT_TYPE, "UTF-8");
    mcu.storeContent(is, ctype);
    isc = CharsetUtil.getCharsetStream(mcu);
    assertEquals("UTF-16LE", isc.getCharset());
    assertTrue(mcu.getUncompressedCalled());
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
    byte[] buf = HTML_HEADER.getBytes(Constants.DEFAULT_ENCODING);
    assertEquals("ISO-8859-1", CharsetUtil.findCharsetInText(buf, buf.length));

    // this has a html5 charset=  utf-8
    buf = html1.getBytes(Constants.DEFAULT_ENCODING);
    assertEquals("UTF-8",CharsetUtil.findCharsetInText(buf,buf.length));

    // this has a charset= in the title so it's ignored.
    buf = html2.getBytes(Constants.DEFAULT_ENCODING);
    assertEquals(null,CharsetUtil.findCharsetInText(buf,buf.length));

    //xml no declaration
    buf = xml1.getBytes(Constants.DEFAULT_ENCODING);
    assertEquals(null,CharsetUtil.findCharsetInText(buf,buf.length));

    // xml utf 8 declaration
    buf = xml2.getBytes(Constants.DEFAULT_ENCODING);
    assertEquals("UTF-8",CharsetUtil.findCharsetInText(buf,buf.length));
    // xml utf 16 declaration & standalone attribute
    buf = xml3.getBytes(Constants.DEFAULT_ENCODING);
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
    int in_length = bais.available(); // returns an estimate
//    assertEquals(in_length, HTML_FILE.getBytes(Constants.DEFAULT_ENCODING).length);
    bais.read(buf,0,buf.length);
    // convert the first 100 bytes into a string
    buf_string = new String(buf,"UTF-8");
    int str_length = buf_string.length();
    assertEquals(100, buf_string.getBytes(Constants.DEFAULT_ENCODING).length);
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

  public void testGuessCharsetNoMark() throws Exception {
    try {
      CharsetUtil.guessCharsetFromStream(new NoMarkStringInputStream("foo"));
      fail("guessCharsetFromStream should require markSupported()");
    } catch (IllegalArgumentException e) {
    }
    try {
      CharsetUtil.guessCharsetName(new NoMarkStringInputStream("foo"));
      fail("guessCharsetName should require markSupported()");
    } catch (IllegalArgumentException e) {
    }
  }

  static class NoMarkStringInputStream extends StringInputStream {
    NoMarkStringInputStream(String s) {
      super(s);
    }
    @Override
    public boolean markSupported() {
      return false;
    }
  }

  String guessCharsetName(byte[] initialBytes) throws IOException {
    byte [] conc = ArrayUtils.addAll(initialBytes,
				     NO_CHARSET_HTML.getBytes("ISO-8859-1"));
    InputStream is =  new ByteArrayInputStream(conc);
    return CharsetUtil.guessCharsetName(is);
  }

  public void testGuessCharsetName() throws Exception {
    assertEquals("UTF-8", guessCharsetName(UTF8_BOM));
    assertEquals("UTF-16LE", guessCharsetName(UTF16_BOM_LE));
    assertEquals("UTF-16BE", guessCharsetName(UTF16_BOM_BE));
    if (CharsetUtil.supportedCharsetName("UTF-32LE") != null) {
      assertEquals("UTF-32LE", guessCharsetName(UTF32_BOM_LE));
      assertEquals("UTF-32BE", guessCharsetName(UTF32_BOM_BE));
    }
    if (CharsetUtil.supportedCharsetName("UTF-7") != null) {
      assertEquals("UTF-7", guessCharsetName(UTF7_BOM_v1));
      assertEquals("UTF-7", guessCharsetName(UTF7_BOM_v2));
      assertEquals("UTF-7", guessCharsetName(UTF7_BOM_v3));
      assertEquals("UTF-7", guessCharsetName(UTF7_BOM_v4));
    }
    if (CharsetUtil.supportedCharsetName("UTF-1") != null) {
      assertEquals("UTF-1", guessCharsetName(UTF1_BOM));
    } else {
      assertEquals("UTF-8", guessCharsetName(UTF1_BOM));
    }
  }

  void assertStreamCharsetFromBOM(String expCharset, byte[] initialBytes)
      throws IOException {
    byte [] conc = ArrayUtils.addAll(initialBytes,
				     NO_CHARSET_HTML.getBytes("ISO-8859-1"));
    InputStream is =  new ByteArrayInputStream(conc);
    CharsetUtil.InputStreamAndCharset isc = CharsetUtil.getCharsetStream(is);
    assertEquals(expCharset, isc.getCharset());
    // The charset returned reflects the BOM, not the encoding of the byte
    // stream we passed in.  We're just checking here that the InputStream
    // wasn't mangled and the BOM was correctly removed.
    assertReaderMatchesString(NO_CHARSET_HTML,
			      new InputStreamReader(isc.getInStream(),
						    "ISO-8859-1"));
  }

  void assertStreamCharset(String expCharset, String str)
      throws IOException {
    InputStream is =  new ByteArrayInputStream(str.getBytes("ISO-8859-1"));
    CharsetUtil.InputStreamAndCharset isc = CharsetUtil.getCharsetStream(is);
    assertEquals(expCharset, isc.getCharset());
  }

  public void testGetCharsetStream() throws Exception {
    assertStreamCharsetFromBOM("UTF-8", UTF8_BOM);
    assertStreamCharsetFromBOM("UTF-16LE", UTF16_BOM_LE);
    assertStreamCharsetFromBOM("UTF-16BE", UTF16_BOM_BE);
    if (CharsetUtil.supportedCharsetName("UTF-32LE") != null) {
      assertStreamCharsetFromBOM("UTF-32LE", UTF32_BOM_LE);
      assertStreamCharsetFromBOM("UTF-32BE", UTF32_BOM_BE);
    }
    if (CharsetUtil.supportedCharsetName("UTF-7") != null) {
      assertStreamCharsetFromBOM("UTF-7", UTF7_BOM_v1);
      assertStreamCharsetFromBOM("UTF-7", UTF7_BOM_v2);
      assertStreamCharsetFromBOM("UTF-7", UTF7_BOM_v3);
      assertStreamCharsetFromBOM("UTF-7", UTF7_BOM_v4);
    }
    if (CharsetUtil.supportedCharsetName("UTF-1") != null) {
      assertStreamCharsetFromBOM("UTF-1", UTF1_BOM);
    } else {
      assertStreamCharsetFromBOM("UTF-8", UTF1_BOM);
    }
  }

  public void testBufSize() throws Exception {
    assertStreamCharset("ISO-8859-1", HTML_HEADER);
    ConfigurationUtil.setFromArgs(CharsetUtil.PARAM_INFER_CHARSET_BUFSIZE, "4");
    assertStreamCharset("ISO-8859-2", HTML_HEADER);

    String u = HTML_FRAGMENT +
      StringUtils.repeat("          ", 2000) + metaTag("UTF-8");
    assertStreamCharset("ISO-8859-2", u);
    ConfigurationUtil.setFromArgs(CharsetUtil.PARAM_INFER_CHARSET_BUFSIZE,
				  "");
    assertStreamCharset("UTF-8", u);
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
      ConfigurationUtil.setFromArgs(CharsetUtil.PARAM_INFER_CHARSET_BUFSIZE,
				    "10");
      assertCharset(xmlUtf8, xmlUtf8.getBytes("UTF-8"), "ISO-8859-1");
      ConfigurationUtil.setFromArgs(CharsetUtil.PARAM_INFER_CHARSET_BUFSIZE, "");
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
     InputStreamReader reader =
       CharsetUtil.getReader(new ByteArrayInputStream(bytes), expectedCharset);
     assertEquals(expectedCharset,
		  Charset.forName(reader.getEncoding()).displayName());
     StringBuilder sb = new StringBuilder();
     assertEquals(golden, StringUtil.fromReader(reader));
   }

  private File writeEncodedFile(String content, String enc) throws IOException {
    File retFile = getTempFile("charset", enc);
    OutputStreamWriter osw = new OutputStreamWriter(
                                 new FileOutputStream(retFile),
                                 Charset.forName(enc).newEncoder());
    osw.write(content);
    osw.flush();
    osw.close();
    return retFile;
  }
}
