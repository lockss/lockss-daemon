/*

Copyright (c) 2014-2021 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.illiesia;

import org.lockss.util.CharsetUtil;
import org.lockss.util.StreamUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

/**
 * A subclass to override the default behavior of the org.lockss.util.CharsetUtil.getCharsetStream() method
 * We override getCharsetStream() to NOT get the encoding declared in the Html document itself.
 *  i.e. String charset = findCharsetInText(buf, len);
 * This is done because the illiesia html pages are not encoded with the encoding they claim to use.
 * We ignore the declared encoding and have the getCharsetStream() method determine it by checking the BOM.
 */
public class IlliesiaCharsetUtil extends CharsetUtil {

  // Redeclaring these private variables here
  private static final String UTF8 = "UTF-8";
  private static final String UTF16BE = "UTF-16BE";
  private static final String UTF16LE = "UTF-16LE";
  private static final String UTF32BE = "UTF-32BE";
  private static final String UTF32LE = "UTF-32LE";
  private static final String UTF7 = "UTF-7";
  private static final String UTF1 = "UTF-1";

  // We need this method as well,
  static String supportedCharsetName(String s) {
    try {
      return Charset.forName(s).name();
    } catch (UnsupportedCharsetException ex) {
      return null;
    } catch (IllegalCharsetNameException ex) {
      return null;
    }
  }
  // This is the method we need to modify, note initializing charset to null, instead of calling findCharsetInText()
  public static InputStreamAndCharset getCharsetStream(InputStream inStream,
                                                       String expectedCharset)
      throws IOException {
      if (!CharsetUtil.inferCharset()) {
        return new InputStreamAndCharset(inStream, expectedCharset);
      }
      ByteArrayOutputStream buffered = new ByteArrayOutputStream();
      int len = 0;
      byte[] buf = new byte[DEFAULT_INFER_CHARSET_BUFSIZE];
      if(inStream != null) {
        len = StreamUtil.readBytes(inStream, buf, buf.length);
      }
      if (len <= 0) {
        return new InputStreamAndCharset(inStream, expectedCharset);
      }
      String charset = null;
      if (hasUtf8BOM(buf, len)) {
        charset = UTF8;
        buffered.write(buf, 3, len - 3);
        // Check UTF32 before UTF16 since a little endian UTF16 BOM is a prefix of
        // a little endian UTF32 BOM.
      } else if (hasUtf32BEBOM(buf, len)) {
        charset = UTF32BE;
        buffered.write(buf, 4, len - 4);
      } else if (hasUtf32LEBOM(buf, len)) {
        charset = UTF32LE;
        buffered.write(buf, 4, len - 4);
      } else if (hasUtf16BEBOM(buf, len)) {
        charset = UTF16BE;
        buffered.write(buf, 2, len - 2);
      } else if (hasUtf16LEBOM(buf, len)) {
        charset = UTF16LE;
        buffered.write(buf, 2, len - 2);
      } else if (hasUtf7BOM(buf, len)) {
        charset = UTF7;
        buffered.write(buf, 4, len - 4);
      } else if (hasUtf1BOM(buf, len)) {
        charset = UTF1;
        buffered.write(buf, 3, len - 3);
      } else {
        // Use icu4j to choose an  encoding.
        buffered.write(buf, 0, len);
        charset = guessCharsetFromBytes(buf);
      }
      if (charset != null) { charset = supportedCharsetName(charset); }
      if (charset == null) { charset = (expectedCharset == null) ? UTF8:expectedCharset; }
      InputStream is = joinStreamsWithCharset(buffered.toByteArray(),
          inStream,
          charset);
      return new InputStreamAndCharset(is, charset);
    }

}
