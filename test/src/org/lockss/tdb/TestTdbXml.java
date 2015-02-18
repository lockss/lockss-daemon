/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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
  
}
