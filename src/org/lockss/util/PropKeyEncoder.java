/*
 * $Id$
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

package org.lockss.util;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.util.BitSet;

public class PropKeyEncoder {
  static BitSet dontNeedEncoding;
  static final int caseDiff = ('a' - 'A');

  static {
    dontNeedEncoding = new BitSet(256);
    int i;
    for (i = 'a'; i <= 'z'; i++) {
      dontNeedEncoding.set(i);
    }
    for (i = 'A'; i <= 'Z'; i++) {
      dontNeedEncoding.set(i);
    }
    for (i = '0'; i <= '9'; i++) {
      dontNeedEncoding.set(i);
    }
    dontNeedEncoding.set(' '); /* encoding a space to a + is done in the encode() method */
    dontNeedEncoding.set('-');
    dontNeedEncoding.set('_');
    dontNeedEncoding.set('*');
  }

  /**
   * You can't call the constructor.
   */
  private PropKeyEncoder() { }

  /**
   * Translates a string into <code>x-www-form-urlencoded</code> format.
   *
   * @param   s   <code>String</code> to be translated.
   * @return  the translated <code>String</code>.
   */
  // This is very much like URLEncoder.encode, but with a slightly
  // different set of characters needing encoding.
  public static String encode(String s) {
    int maxBytesPerChar = 10;
    StringBuffer out = new StringBuffer(s.length());
    ByteArrayOutputStream buf = new ByteArrayOutputStream(maxBytesPerChar);
    OutputStreamWriter writer = new OutputStreamWriter(buf);

    for (int i = 0, len = s.length(); i < len; i++) {
      int c = (int)s.charAt(i);
      if (dontNeedEncoding.get(c)) {
	if (c == ' ') {
	  c = '+';
	}
	out.append((char)c);
      } else {
	// convert to external encoding before hex conversion
	try {
	  writer.write(c);
	  writer.flush();
	} catch(IOException e) {
	  buf.reset();
	  continue;
	}
	byte[] ba = buf.toByteArray();
	for (int j = 0; j < ba.length; j++) {
	  out.append('%');
	  char ch = Character.forDigit((ba[j] >> 4) & 0xF, 16);
	  // converting to use uppercase letter as part of
	  // the hex value if ch is a letter.
	  if (Character.isLetter(ch)) {
	    ch -= caseDiff;
	  }
	  out.append(ch);
	  ch = Character.forDigit(ba[j] & 0xF, 16);
	  if (Character.isLetter(ch)) {
	    ch -= caseDiff;
	  }
	  out.append(ch);
	}
	buf.reset();
      }
    }

    return out.toString();
  }

  /**
   * Undoes <code>x-www-form-urlencode</code> format.
   *
   * @param   s A <code>x-www-form-urlencoded</code> String
   * @return  the original String
   */
  public static String decode(String s) {
    // we encode in the same format as URLEncoder, so can use URLDecoder to
    // decode.
    return UrlUtil.decodeUrl(s);
  }
}
