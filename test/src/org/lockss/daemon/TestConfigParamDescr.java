/*
 * $Id: TestConfigParamDescr.java,v 1.7 2006-07-20 18:26:48 thib_gc Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.daemon;

import java.io.*;
import java.util.*;

import org.lockss.app.LockssApp;
import org.lockss.test.*;
import org.lockss.util.*;

/**
 * This is the test class for org.lockss.daemon.ConfigParamDescr
 */
public class TestConfigParamDescr extends LockssTestCase {
  ConfigParamDescr d1;
  ConfigParamDescr d2;

  public void setUp() {
    d1 = new ConfigParamDescr("key1");
    d2 = new ConfigParamDescr("key2");
  }

  public void testAccessors() {
    ConfigParamDescr d1 = new ConfigParamDescr("k1");
    assertEquals("k1", d1.getKey());
    assertSame(d1, d1.setKey("k2"));
    assertEquals("k2", d1.getKey());

    assertEquals("k2", d1.getDisplayName());
    assertSame(d1, d1.setKey("foob"));
    assertEquals("foob", d1.getDisplayName());

    assertNull(d1.getDescription());
    assertSame(d1, d1.setDescription("ddd"));
    assertEquals("ddd", d1.getDescription());

    assertSame(d1, d1.setType(3));
    assertEquals(3, d1.getType());

    assertSame(d1, d1.setSize(47));
    assertEquals(47, d1.getSize());

    assertTrue(d1.isDefinitional());
    assertSame(d1, d1.setDefinitional(false));
    assertFalse(d1.isDefinitional());

    assertFalse(d1.isDefaultOnly());
    assertSame(d1, d1.setDefaultOnly(true));
    assertTrue(d1.isDefaultOnly());
  }

  public void testSizeDefault() {
    ConfigParamDescr d1 = new ConfigParamDescr("k1");
    assertEquals(0, d1.getSize());
    d1.setType(ConfigParamDescr.TYPE_BOOLEAN);
    assertEquals(4, d1.getSize());

    ConfigParamDescr d2 = new ConfigParamDescr("k1");
    d2.setType(ConfigParamDescr.TYPE_YEAR);
    assertEquals(4, d2.getSize());

    ConfigParamDescr d3 = new ConfigParamDescr("k1");
    d3.setType(ConfigParamDescr.TYPE_INT);
    assertEquals(10, d3.getSize());
    d3.setSize(12);
    assertEquals(12, d3.getSize());

    ConfigParamDescr d4 = new ConfigParamDescr("k1");
    d4.setSize(12);
    d4.setType(ConfigParamDescr.TYPE_INT);
    assertEquals(12, d4.getSize());
  }

  public void testCompareTo() {
    ConfigParamDescr d1 = new ConfigParamDescr("cc");
    ConfigParamDescr d2 = new ConfigParamDescr("dd");
    assertTrue(d1.compareTo(d2) < 0);
    d2.setDisplayName("bb");
    assertTrue(d1.compareTo(d2) > 0);
    d1.setKey("bb");
    assertEquals(0, d1.compareTo(d2));
  }

  public void testEquals() {
    ConfigParamDescr d1 = new ConfigParamDescr("k1");
    ConfigParamDescr d2 = new ConfigParamDescr(new String("k1"));
    ConfigParamDescr d3 = new ConfigParamDescr("k2");
    assertEquals(d1, d1);
    assertEquals(d1, d2);
    assertNotEquals(d1, d3);
    d2.setType(3);
    assertNotEquals(d1, d2);
    d2 = new ConfigParamDescr(new String("k1"));
    assertEquals(d1, d2);
    d2.setSize(10);
    assertNotEquals(d1, d2);
  }

  public void testIsReserved() {
    assertTrue(ConfigParamDescr.isReservedParam("reserved.foo"));
    assertTrue(ConfigParamDescr.isReservedParam("reserved."));
    assertFalse(ConfigParamDescr.isReservedParam("year"));
    assertFalse(ConfigParamDescr.isReservedParam("reservedfoo"));
  }

  public void testHash() {
    ConfigParamDescr d1 = new ConfigParamDescr("foo");
    ConfigParamDescr d2 = new ConfigParamDescr("foo");
    assertEquals(d1.hashCode(), d2.hashCode());
  }

  /**
   * <p>Tests that {@link ConfigParamDescr#postUnmarshalResolve(LockssApp)}
   * works at least for the elements of
   * {@link ConfigParamDescr#DEFAULT_DESCR_ARRAY}.</p>
   * @throws Exception if an unexpected error occurs.
   */
  public void testPostMarshalUnresolve() throws Exception {
    XStreamSerializer serializer = new XStreamSerializer();
    for (int ix = 0 ; ix < ConfigParamDescr.DEFAULT_DESCR_ARRAY.length ; ++ix) {
      File file = File.createTempFile("testfile", ".xml");
      file.deleteOnExit();
      serializer.serialize(file, ConfigParamDescr.DEFAULT_DESCR_ARRAY[ix]);
      assertSame(ConfigParamDescr.DEFAULT_DESCR_ARRAY[ix],
                 serializer.deserialize(file));
    }
  }

}
