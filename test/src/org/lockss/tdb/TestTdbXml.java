/*

Copyright (c) 2000-2024, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.tdb;

import org.lockss.test.LockssTestCase;

public class TestTdbXml extends LockssTestCase {

  public void testXmlEscaper() throws Exception {
    assertEquals("foo&lt;bar", TdbXml.xmlEscaper.translate("foo<bar"));
    assertEquals("foo&gt;bar", TdbXml.xmlEscaper.translate("foo>bar"));
    assertEquals("foo&quot;bar", TdbXml.xmlEscaper.translate("foo\"bar"));
    assertEquals("foo&apos;bar", TdbXml.xmlEscaper.translate("foo'bar"));
    assertEquals("foo&#233;bar", TdbXml.xmlEscaper.translate("foo\u00e9bar")); // hex E9 = dec 233
    // Test 0x7e/0x7f boundary
    assertEquals("foo~bar", TdbXml.xmlEscaper.translate("foo\u007ebar")); // U+007E is ~
    assertEquals("foo&#127;bar", TdbXml.xmlEscaper.translate("foo\u007fbar")); // hex 7F = dec 127
  }
  
  public void testUnicodeNormalizer() throws Exception {
    assertEquals("aeiou", TdbXml.unicodeNormalizer.apply("aeiou"));
    assertEquals("aeiou", TdbXml.unicodeNormalizer.apply("\u00e1\u00e8\u00ee\u00f5\u00fc")); // a acute, e grave, i circumflex, o tilde, u umlaut
    
    // 'a' modified with every combining diacritical mark
    StringBuilder sb = new StringBuilder();
    sb.append('a');
    for (char c = '\u0300' ; c <= '\u036f' ; ++c) {
      sb.append(c);
    }
    assertEquals("a", TdbXml.unicodeNormalizer.apply(sb.toString()));
  }
  
}
