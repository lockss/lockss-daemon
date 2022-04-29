/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University,
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.tdb;

import java.io.*;
import java.util.*;

import org.apache.commons.io.output.UnsynchronizedByteArrayOutputStream;

/**
 * Temporary until a few things are in lockss-util or similar.
 */
public class TdbUtil {

  // See Constants
  public static final String ENCODING_UTF_8 = "UTF-8";
  
  // See PluginManager
  public static String generateAuId(String pluginId, Map<String, String> params) {
    return pluginId.replace('.', '|') + "&" + propsToCanonicalEncodedString(params);
  }
  
  // See PropUtil
  protected static String propsToCanonicalEncodedString(Map<String, String> params) {
    if (params == null || params.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    SortedSet<String> sortedKeys = new TreeSet<String>(params.keySet());
    
    for (Iterator<String> it = sortedKeys.iterator() ; it.hasNext() ; ) {
      String key = it.next().toString();
      sb.append(encode(key));
      sb.append("~");
      sb.append(encode(params.get(key).toString()));
      if (it.hasNext()) {
        sb.append("&");
      }
    }
    return sb.toString();
  }

  // See PropKeyEncoder
  public static String encode(String str) {
    int maxBytesPerChar = 10;
    StringBuffer out = new StringBuffer(str.length());
    UnsynchronizedByteArrayOutputStream buf = new UnsynchronizedByteArrayOutputStream(maxBytesPerChar);
    OutputStreamWriter writer = new OutputStreamWriter(buf);
    
    for (int i = 0, len = str.length(); i < len; i++) {
      int c = (int)str.charAt(i);
      if (DONTNEEDENCODING.get(c)) {
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
            ch -= CASEDIFF;
          }
          out.append(ch);
          ch = Character.forDigit(ba[j] & 0xF, 16);
          if (Character.isLetter(ch)) {
            ch -= CASEDIFF;
          }
          out.append(ch);
        }
        buf.reset();
      }
    }
    
    return out.toString();
  }
  
  // See PropKeyEncoder
  private static BitSet DONTNEEDENCODING;
  
  // See PropKeyEncoder
  static {
    DONTNEEDENCODING = new BitSet(256);
    int i;
    for (i = 'a'; i <= 'z'; i++) {
      DONTNEEDENCODING.set(i);
    }
    for (i = 'A'; i <= 'Z'; i++) {
      DONTNEEDENCODING.set(i);
    }
    for (i = '0'; i <= '9'; i++) {
      DONTNEEDENCODING.set(i);
    }
    DONTNEEDENCODING.set(' '); /* encoding a space to a + is done in the encode() method */
    DONTNEEDENCODING.set('-');
    DONTNEEDENCODING.set('_');
    DONTNEEDENCODING.set('*');
  }

  // See PropKeyEncoder
  private static final int CASEDIFF = ('a' - 'A');

}
