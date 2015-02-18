/*
 * $Id$
 */

/*

Copyright (c) 2013-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.hasher;

import java.io.*;
import java.util.*;
import org.lockss.test.*;
import org.lockss.util.*;

public class TestHashResult extends LockssTestCase {
  public static byte[] bytes = {0, 1, 2};
  public static byte[] bytes2 = {2, 1, 0};
  public static byte[] empty_bytes = {};

  public void testCreate() {
    HashResult.make(bytes);
  }

  public void testCreateNoBytes() {
    try {
      HashResult.make((byte[])null);
      fail();
    } catch (HashResult.IllegalByteArray e) {
      // expected
    }
    try {
      HashResult.make(empty_bytes);
      fail();
    } catch (HashResult.IllegalByteArray e) {
      // expected
    }
    try {
      HashResult.make((String)null);
      fail();
    } catch (HashResult.IllegalByteArray e) {
      // expected
    }
  }

  public void testEqualsBytes() {
    HashResult o1 = HashResult.make(bytes);
    assertTrue(o1.equalsBytes(bytes));

    try {
      o1.equalsBytes(null);
    } catch (HashResult.IllegalByteArray e) {
      // expected
    }

    try {
      o1.equalsBytes(empty_bytes);
    } catch (HashResult.IllegalByteArray e) {
      // expected
    }
  }

  public void testEqualsAndHashCode() {
    HashResult o1 = HashResult.make(bytes);
    HashResult o2 = HashResult.make(bytes);
    HashResult o3 = HashResult.make(bytes2);
    HashResult o4 = HashResult.make(bytes, "fooalg");
    HashResult o5 = HashResult.make(bytes, "baralg");
    
    // o1 and o2 are not == but are equals.
    assertNotSame(o1, o2);
    assertTrue(o1.equals(o2));
    assertTrue(o2.equals(o1));

    assertFalse(o1.equals(o3));
    assertFalse(o3.equals(o1));

    assertFalse(o4.equals(o1));
    assertFalse(o1.equals(o4));

    assertFalse(o4.equals(o5));
    assertFalse(o5.equals(o4));

    assertEquals(o1.hashCode(), o2.hashCode());
  }

  private String[] bad = {
    "deadbeef",
    "SHA-1:",
    "SHA-1:bar",
    ":deadbeef"
  };

  public void testMakeFromString() {
    String good = "SHA-1:deadbeef";
    HashResult hr = HashResult.make(good);
    assertEquals(good.toUpperCase(), hr.toString());
    assertEquals(0, good.compareToIgnoreCase(hr.toString()));
    for (int i = 0; i < bad.length; i++) {
      try {
	hr = HashResult.make(bad[i]);
	fail("Should be illegal: " + bad[i]);
      } catch (HashResult.IllegalByteArray ex) {
	// Expected
      }
    }
  }

  public void testMakeFromStringWithLeadingZeroes() {
    String good = "SHA-1:0000266B7F1B82B2689A485FFC00C02D65E9C8D1";
    HashResult hr = HashResult.make(good);
    assertEquals(20, hr.getBytes().length);
    assertEquals(good, hr.toString());
    assertEquals(0, good.compareToIgnoreCase(hr.toString()));
  }

  // Simple holder class to serialize/deserialize a HashResult field
  static class Holder implements LockssSerializable {
    HashResult hr;

    Holder() {}
    Holder(HashResult hr) {
      this.hr = hr;
    }
  }

  // Deserialize and return a Holder from the string
  Holder deser(String s) throws Exception {
    return (Holder)(new XStreamSerializer().deserialize(new StringReader(s)));
  }

  // Return the serialized representation of the object
  String ser(LockssSerializable o) throws Exception {
    File tf = getTempFile("ser", ".xml");
    new XStreamSerializer().serialize(tf, o);
    return StringUtil.fromFile(tf);
  }

  String xmlhr(String hr) {
    return "<org.lockss.hasher.TestHashResult-Holder>" +
      "<hr>" + hr + "</hr>" +
      "</org.lockss.hasher.TestHashResult-Holder>";
  }

  public void testDeser() throws Exception {
    String good[] = {
      "SHA-1:deadbeef",
      "foo:0123456789abcdef",
    };
    String bad[] = {
      "SHA-1:deadfeet",	      // not hex
      "foo:",		      // no hash value
      "feed",		      // no algorithm - currently illegal
    };

    for (String s : good) {
      assertEquals(HashResult.make(s), deser(xmlhr(s)).hr);
    }

    for (String s : bad) {
      try {
	deser(xmlhr(s));
	fail("Should fail to deserialize: " + s);
      } catch (SerializationException e) {
      }
    }
  }

  // Uppercase the hash part of the string to agree with HashResult.toString()
  String canonHr(String s) {
    List<String> l = StringUtil.breakAt(s, ":");
    if (l.size() == 2) {
      return l.get(0) + ":" + l.get(1).toUpperCase();
    }
    return s.toUpperCase();
  }

  public void testSer() throws Exception {
    String good[] = {
      "SHA-1:deadbeef",
      "SHA-256:5454ABCD",
      "foo:0123456789abcdef",
    };
    String bad[] = {
      "SHA-1:deadfeet",
    };

    for (String s : good) {
      String xml = ser(new Holder(HashResult.make(s)));
      assertMatchesRE("<hr>" + canonHr(s) + "</hr>", xml);
    }

    // Currently illegal to serialize HashResult with no algorithm
    HashResult hr1 = HashResult.make(ByteArray.fromHexString("feed1234"));
    Holder hd1 = new Holder(hr1);
    try {
      ser(hd1);
      fail("Should fail to serialize: " + hr1);
    } catch (SerializationException e) {
    }
  }


}
