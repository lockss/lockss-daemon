/*
 * $Id$
 */

/*

Copyright (c) 2014-2016 Board of Trustees of Leland Stanford Jr. University,
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


import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.tika.parser.txt.*;
import org.lockss.plugin.*;

/**
 * A class meant to encapsulate static character encoding/decoding using icu4j
 */
public class CharsetUtil {
  private static final Logger log = Logger.getLogger(CharsetUtil.class);

  public static final String PREFIX = org.lockss.config.Configuration.PREFIX + "crawler.";
  /** If true, CharsetUtil will try to infer the proper charset to use,
   * falling back to the specified one if it can't. */
  public static final String PARAM_INFER_CHARSET = PREFIX + "inferCharset";
  public static final boolean DEFAULT_INFER_CHARSET = true;

  /** Number of bytes from the stream that will be searched for an HTML or
   * XML charset spec, or fed to CharsetDetector (which may not look at all
   * of them). */
  public static final String PARAM_INFER_CHARSET_BUFSIZE =
    PREFIX + "inferCharsetBufSize";
  public static final int DEFAULT_INFER_CHARSET_BUFSIZE = 8192;

  private static final String UTF8 = "UTF-8";
  private static final String UTF16BE = "UTF-16BE";
  private static final String UTF16LE = "UTF-16LE";
  private static final String UTF32BE = "UTF-32BE";
  private static final String UTF32LE = "UTF-32LE";
  private static final String UTF7 = "UTF-7";
  private static final String UTF1 = "UTF-1";
  private static final String ISO_8859_1 = "ISO-8859-1";

  private static boolean inferCharset = DEFAULT_INFER_CHARSET;
  private static int inferCharsetBufSize = DEFAULT_INFER_CHARSET_BUFSIZE;

  public static void setConfig(final org.lockss.config.Configuration config,
    final org.lockss.config.Configuration oldConfig,
    final org.lockss.config.Configuration.Differences diffs) {
    inferCharset =
      config.getBoolean(PARAM_INFER_CHARSET,DEFAULT_INFER_CHARSET);
    inferCharsetBufSize =
      config.getInt(PARAM_INFER_CHARSET_BUFSIZE,
		    DEFAULT_INFER_CHARSET_BUFSIZE);
  }

  public static boolean inferCharset() {return inferCharset;}

  /**
   * This will guess the charset of an inputstream.  If the inpust
   * @param in an input stream which we will be checking
   * @return the charset or null if nothing could be determined with greater
   * than 50% accuracy
   * @throws IOException if mark() not supported or read fails
   */
  public static String guessCharsetName(InputStream in) throws IOException
  {
    if(!in.markSupported())
      throw new IllegalArgumentException("InputStream must support mark.");
    ByteArrayOutputStream buffered = new ByteArrayOutputStream();
    byte[] buf = new byte[inferCharsetBufSize];
    in.mark(inferCharsetBufSize + 1024);
    int len = StreamUtil.readBytes(in, buf, buf.length);
    if (len <= 0) {
      return UTF8; // this is just a default for 0 len stream
    }
    // If the charset is specified in the document, use that.
    String charset = findCharsetInText(buf, len);
    if (charset == null) {  // we didn't find it check BOM
      if (hasUtf8BOM(buf, len)) {
        charset = UTF8;
        // Check UTF32 before UTF16 since a little endian UTF16 BOM is a prefix of
        // a little endian UTF32 BOM.
      } else if (hasUtf32BEBOM(buf, len)) {
        charset = UTF32BE;
      } else if (hasUtf32LEBOM(buf, len)) {
        charset = UTF32LE;
      } else if (hasUtf16BEBOM(buf, len)) {
        charset = UTF16BE;
      } else if (hasUtf16LEBOM(buf, len)) {
        charset = UTF16LE;
      } else if (hasUtf7BOM(buf, len)) {
        charset = UTF7;
      } else if (hasUtf1BOM(buf, len)) {
        charset = UTF1;
      } else {
        // Use icu4j to guess an encoding.
        charset = guessCharsetFromBytes(buf);
      }
    }
    if (charset != null) { charset = supportedCharsetName(charset); }
    if (charset == null) { charset = UTF8; }
    in.reset();
    return charset;
  }

  /**
   * Given a byte stream, figure out an encoding and return a character stream
   * and the encoding used to convert bytes to characters. This will look for a
   * document based charset statement, then check for BOM, then use text
   * analysis to 'guess' the encoding.
   * @param inStream  the InputStream from which to determine the encoding
   * @return a InputStreamAndCharset containing a JoinedStream the consumed bytes and the
   * inputstream and a new a String containing the name of the character
   * encoding.
   * @throws IOException
   */
  public static InputStreamAndCharset getCharsetStream(InputStream inStream)
    throws IOException {
    return getCharsetStream(inStream, UTF8);
  }

  /**
   * Given a byte stream, figure out an encoding and return a character stream
   * and the encoding used to convert bytes to characters. This will look for a
   * document based charset statement, then check for BOM, then use text
   * analysis to 'guess' the encoding.
   *
   * @param inStream the InputStream from which to determine the encoding
   * @param expectedCharset the expected charset
   * @return a Pair containing a JoinedStream the consumed bytes and the
   * inputstream and a new a String containing the name of the character
   * encoding.
   * @throws IOException
   */
  public static InputStreamAndCharset getCharsetStream(InputStream inStream,
						       String expectedCharset)
      throws IOException {
    if (!CharsetUtil.inferCharset()) {
      return new InputStreamAndCharset(inStream, expectedCharset);
    }
    ByteArrayOutputStream buffered = new ByteArrayOutputStream();
    int len = 0;
    byte[] buf = new byte[inferCharsetBufSize];
    if(inStream != null) {
      len = StreamUtil.readBytes(inStream, buf, buf.length);
    }
    if (len <= 0) {
      return new InputStreamAndCharset(inStream, expectedCharset);
    }
    String charset = findCharsetInText(buf, len);
    if (charset != null) {
      // If the charset is specified in the document, use that.
      buffered.write(buf, 0, len);
      // Otherwise, look for a BOM at the start of the content.
    } else if (hasUtf8BOM(buf, len)) {
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

  public static InputStreamAndCharset getCharsetStream(CachedUrl cu)
      throws IOException {
    return getCharsetStream(cu.getUncompressedInputStream(),
			    getAllegedCharset(cu));
  }


  static String getAllegedCharset(CachedUrl cu) {
    return HeaderUtil.getCharsetOrDefaultFromContentType(cu.getContentType());
  }

  /**
   * Given a byte stream, figure out an encoding and return a character stream
   * and the encoding used to convert bytes to characters. This will look for a
   * document based charset statement, then check for BOM, then use text
   * analysis to 'guess' the encoding.
   * @param inStream  the InputStream from which to determine the encoding
   * @return a Pair of a InputStreamReader containing the and a new a String containing
   * the name of the character encoding.
   * @throws IOException
   */
  public static InputStreamReader getReader(InputStream inStream) throws IOException {
    return getReader(inStream, UTF8);
  }
  /**
   * Given a byte stream, figure out an encoding and return a character stream
   * and the encoding used to convert bytes to characters. This will look for a
   * document based charset statement, then check for BOM, then use text
   * analysis to 'guess' the encoding.
   * @param inStream  the InputStream from which to determine the encoding
   * @return a Reader containing the inputstream and with the character encoding.
   * @throws IOException
   */
  public static InputStreamReader getReader(InputStream inStream,
    String expectedCharset) throws IOException {

    InputStreamAndCharset charsetStream = getCharsetStream(inStream, expectedCharset);
    return new InputStreamReader(charsetStream.getInStream(),
                                 charsetStream.getCharset());
  }

  public static InputStreamReader getReader(CachedUrl cu) throws IOException {
    return (InputStreamReader)
      getCharsetReader(cu.getUncompressedInputStream(),
		       getAllegedCharset(cu)).getLeft();
  }

  /**
   * Given a byte stream, figure out an encoding and return a character stream
   * and the encoding used to convert bytes to characters. This will look for a
   * document based charset statement, then check for BOM, then use text
   * analysis to 'guess' the encoding.
   * @param inStream  the InputStream from which to determine the encoding
   * @return a Pair of a JoinedReader containing the consumed bytes and the
   * inputstream and a new a String containing the name of the character encoding.
   * @throws IOException
   * @deprecated
   */
  public static Pair<Reader, String> getCharsetReader(InputStream inStream) throws IOException {
    InputStreamAndCharset isc = getCharsetStream(inStream, UTF8);
    Reader charsetReader = new InputStreamReader(isc.getInStream(),isc.getCharset());
    return new ImmutablePair<>(charsetReader, isc.getCharset());
  }
  /**
   * Given a byte stream, figure out an encoding and return a character stream
   * and the encoding used to convert bytes to characters. This will look for a
   * document based charset statement, then check for BOM, then use text
   * analysis to 'guess' the encoding.
   * @param inStream  the InputStream from which to determine the encoding
   * @return a Pair of a JoinedReader containing the consumed bytes and the
   * inputstream and a new a String containing the name of the character encoding.
   * @throws IOException
   * @deprecated
   */
  public static Pair<java.io.Reader, String> getCharsetReader(InputStream inStream,
                                                              String expectedCharset)
    throws IOException {
    InputStreamAndCharset isc = getCharsetStream(inStream, expectedCharset);
    Reader charsetReader = new InputStreamReader(isc.getInStream(),isc.getCharset());
    return new ImmutablePair<>(charsetReader, isc.getCharset());
  }

  public static Pair<java.io.Reader, String> getCharsetReader(CachedUrl cu)
      throws IOException {
    return getCharsetReader(cu.getUncompressedInputStream(),
			    getAllegedCharset(cu));
  }

  /**
   * given a sampling of bytes determine the charset with the best match
   * @param bytes the bytes from which to make our guess
   * @return the best charset with > 50% confidence or null
   */
  public static String guessCharsetFromBytes(byte[] bytes) {
    return guessCharsetFromBytes(bytes, null);
  }

  /**
   * given a sampling of bytes determine the charset with the best match
   * @param in the byte array containing a sampling of bytes
   * @param expected the encoding to give preference to when looking for a
   * match, null if unknown
   * @return the charset with > 35% confidence with a prefer or null
   */
  public static String guessCharsetFromBytes(byte[] in, String expected)
  {
    CharsetDetector detector = new CharsetDetector();
    if(expected != null) {
      detector.setDeclaredEncoding(expected);
    }
    detector.setText(in);
    CharsetMatch match = detector.detect();
    if(match != null && match.getConfidence() > 35) {// we want at least a 35% match
      return match.getName();
    }
    else {
      return null;
    }
  }

  /**
   * given an input stream determine the charset with the best match
   * @param inStream the inputstream containing the bytes
   * @return charset with > 50% confidence or null
   */
  public static String guessCharsetFromStream(InputStream inStream)
    throws IOException{
    return guessCharsetFromStream(inStream, null);
  }

  /**
   * given an input stream determine the charset with the best match
   * @param inStream the inputstream containing the bytes m
   * @param expected the anticipated match which is given preference when
   * determing a match
   * @return charset with > 50% confidence or expected
   * @throws IOException if InputStream cannot be reset
   */
  public static String guessCharsetFromStream(InputStream inStream,
                                              String expected)
    throws IOException
  {
    if(!inStream.markSupported())
      throw new IllegalArgumentException("InputStream must support mark.");
    CharsetDetector detector = new CharsetDetector();
    if(expected != null) {
      detector.setDeclaredEncoding(expected);
    }
    detector.setText(inStream);
    CharsetMatch match = detector.detect();
    if(match != null && match.getConfidence() > 50) {// we want at least a 50% match
      return match.getName();
    }
    else {
      return expected;
    }
  }


  private static final byte[] CHARSET_BYTES;
  private static final byte[] ENCODING_BYTES;
  static {
    try {
      CHARSET_BYTES = "charset".getBytes(ISO_8859_1);
      ENCODING_BYTES = "encoding".getBytes(ISO_8859_1);
    } catch (UnsupportedEncodingException ex) {
      throw new RuntimeException("Unsupported Encoding: " +
                                 ISO_8859_1 + " (shouldn't happen)");
    }
  }

  /**
   * Looks for sequences like {@code charset="..."} inside angle brackets to
   * match {@code <meta value="text/html;charset=...">} and after {@code <?}
   * sequences like {@code encoding="..."} to match XML prologs.
   */
  public static String findCharsetInText(final byte[] buf, final int len) {
    for (int i = 0; i < len; ++i) {
      if ('<' != buf[i]) { continue; }
      byte lastByte = '<';
      byte[] attrBytes = CHARSET_BYTES;
      // Now we're inside <, so look for attrBytes.
      for (int j = i + 1, n = len; j < n; ++j) {
        byte b = buf[j];
        if (b == 0) { continue; }
        if (b == '?' && lastByte == '<') { attrBytes = ENCODING_BYTES; }
        if ((b | 0x20) == attrBytes[0] && !isAlnum(lastByte)) {
          int wordLen = attrBytes.length;
          int pos = j + 1, k = 1;
          // Match attrBytes against buf[pos:]
          while (pos < n && k < wordLen) {
            b = buf[pos];
            if (b == 0 || b == '-') {  // Skip over NULs in UTF-16 and UTF-32.
              ++pos;
            } else if ((b | 0x20) == attrBytes[k]) {
              ++k;
              ++pos;
            } else {
              break;
            }
          }
          if (k == wordLen) {
            // Now we've found the attribute or parameter name.
            // Skip over spaces and NULs looking for '='
            while (pos < len) {
              b = buf[pos];
              if (b == '=') {
                // Skip over spaces and NULs looking for alnum or quote.
                while (++pos < len) {
                  b = buf[pos];
                  if (b == 0 || isSpace(b)) { continue; }
                  int start;
                  if (b == '"' || b == '\'') {
                    start = pos + 1;
                  } else if (isAlnum(b)) {
                    start = pos;
                  } else {
                    break;
                  }
                  int end = start;
                  boolean sawLetter = false;
                  // Now, find the end of the charset.
                  while (end < len) {
                    b = buf[end];
                    if (b == 0 || b == '-' || b == '_') {
                      ++end;
                    } else if (isAlnum(b)) {
                      sawLetter = true;
                      ++end;
                    } else {
                      break;
                    }
                  }
                  if (sawLetter) {
                    StringBuilder sb = new StringBuilder(end - start);
                    for (int bi = start; bi < end; ++bi) {
                      if (buf[bi] != 0) { sb.append((char) buf[bi]); }
                    }
                    // Only use the charset if it's recognized.
                    // Otherwise, we continue looking.
                    String charset = supportedCharsetName(sb.toString());
                    if (charset != null) { return charset; }
                  }
                }
                break;
              }
              if (b != 0 && !isSpace(b)) {
                break;
              }
              ++pos;
            }
          }
          if (b == '<' || b == '>') {
            i = pos - 1;
            break;
          }
        } else if (b == '<' || b == '>') {
          i = j - 1;
          break;
        }
        lastByte = buf[j];
      }
    }
    return null;
  }

  /**
   * Produces a character stream from an underlying byte stream.
   *
   * @param buffered lookahead bytes read from tail.
   * @param tail the unread portion of the stream
   * @param charset the character set to use to decode the bytes in buffered and
   *     tail.
   * @return a joined input stream.
   * @throws IOException
   */
  public static InputStream joinStreamsWithCharset(
      byte[] buffered, InputStream tail, String charset)
    throws IOException {
    //return new SequenceInputStream(new ByteArrayInputStream(buffered),tail);

    return new JoinedStream(buffered, tail);
  }

  public static boolean isAlnum(byte b) {
    if (b < '0' || b > 'z') { return false; }
    if (b < 'A') { return b <= '9'; }
    return b >= 'a' || b <= 'Z';
  }

  public static boolean isSpace(byte b) {
    return b <= ' '
           && (b == ' ' || b == '\r' || b == '\n' || b == '\t' || b == '\f');
  }

  /**
   * Return the official java charset name for a string
   * @param s the name of the charset
   * @return the official name of the charset or null if unsupported or illegal
   */
  static String supportedCharsetName(String s) {
    try {
      return Charset.forName(s).name();
    } catch (UnsupportedCharsetException ex) {
      return null;
    } catch (IllegalCharsetNameException ex) {
      return null;
    }
  }

  public static final byte
    _00 = (byte) 0,
    _2B = (byte) 0x2b,
    _2F = (byte) 0x2f,
    _38 = (byte) 0x38,
    _39 = (byte) 0x39,
    _4C = (byte) 0x4c,
    _64 = (byte) 0x64,
    _76 = (byte) 0x76,
    _BB = (byte) 0xbb,
    _BF = (byte) 0xbf,
    _EF = (byte) 0xef,
    _F7 = (byte) 0xf7,
    _FE = (byte) 0xfe,
    _FF = (byte) 0xff;

  // See http://en.wikipedia.org/wiki/Byte_order_mark for a table of byte
  // sequences.
  public static boolean hasUtf8BOM(byte[] b, int len) {
    return len >= 3 && b[0] == _EF && b[1] == _BB && b[2] == _BF;
  }

  public static boolean hasUtf16BEBOM(byte[] b, int len) {
    return len >= 2 && b[0] == _FE && b[1] == _FF;
  }

  public static boolean hasUtf16LEBOM(byte[] b, int len) {
    return len >= 2 && b[0] == _FF && b[1] == _FE;
  }

  public static boolean hasUtf32BEBOM(byte[] b, int len) {
    return len >= 4 && b[0] == _00 && b[1] == _00
           && b[2] == _FE && b[3] == _FF;
  }

  public static boolean hasUtf32LEBOM(byte[] b, int len) {
    return len >= 4 && b[0] == _FF && b[1] == _FE
           && b[2] == _00 && b[3] == _00;
  }

  public static boolean hasUtf7BOM(byte[] b, int len) {
    if (len < 4 || b[0] != _2B || b[1] != _2F || b[2] != _76) {
      return false;
    }
    byte b3 = b[3];
    return b3 == _38 || b3 == _39 || b3 == _2B || b3 == _2F;
  }

  public static boolean hasUtf1BOM(byte[] b, int len) {
    return len >= 3 && b[0] == _F7 && b[1] == _64 && b[2] == _4C;
  }


  public static class JoinedStream extends InputStream {
    byte[] buffered;
    int pos;
    final InputStream tail;

    JoinedStream(byte[] buffered, InputStream tail) {
      this.buffered = buffered;
      this.tail = tail;
    }

    @Override
    public int read() throws IOException {
      if (buffered != null) {
        if (pos < buffered.length) { return buffered[pos++]; }
        buffered = null;
      }
      return tail.read();
    }

    @Override
    public int available() throws IOException {
      int avail = tail.available();
      if (buffered != null) {
        avail += Math.max(buffered.length - pos,0);
      }
      return avail;
    }

    @Override
    public int read(byte[] out, int off, int len) throws IOException {
      int nRead = 0;
      if (buffered != null) {
        int avail = buffered.length - pos;
        if (avail != 0) {
          int k = Math.min(len, avail);
          int p1 = pos + k;
          int p2 = off + k;
          pos = p1;
          while (--p2 >= off) { out[p2] = buffered[--p1]; }
          off += k;
          len -= k;
          nRead = k;
        } else {
          buffered = null;
        }
      }
      if (len == 0) { return nRead; }
      int nFromTail = tail.read(out, off, len);
      if (nFromTail > 0) { return nFromTail + nRead; }
      return nRead != 0 ? nRead : -1;
    }

    @Override
    public void close() throws IOException {
      buffered = null;
      tail.close();
    }
  }
  public static class InputStreamAndCharset {
    private String charset;
    private InputStream inStream;

    public InputStreamAndCharset(final InputStream inStream,
      final String charset) {

      this.charset = charset;
      this.inStream = inStream;
    }

    public String getCharset() {
      return charset;
    }

    public InputStream getInStream() {
      return inStream;
    }
  }

}
