/*
 * $Id: TestConfigParamAssignment.java,v 1.1 2004-01-03 06:14:58 tlipkis Exp $
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

package org.lockss.daemon;

import java.util.*;
import org.lockss.test.*;
import org.lockss.util.*;

/**
 * This is the test class for org.lockss.daemon.ConfigParamAssignment
 */

public class TestConfigParamAssignment extends LockssTestCase {
  ConfigParamDescr d1;
  ConfigParamDescr d2;

  public void setUp() {
    d1 = new ConfigParamDescr("key1");
    d2 = new ConfigParamDescr("key2");
  }

  public void testAccessors() {
    ConfigParamAssignment a1 = new ConfigParamAssignment(d1);
    ConfigParamAssignment a2 = new ConfigParamAssignment(d2, "foo");
    assertEquals(d2, a2.getParamDescr());
    assertEquals("foo", a2.getValue());
    assertFalse(a2.isDefault());
    a2.setDefault(true);
    assertTrue(a2.isDefault());
    a2.setDefault(false);
    assertFalse(a2.isDefault());
    assertEquals(d1, a1.getParamDescr());
    assertNull(a1.getValue());
    a1.setValue("bar");
    assertEquals("bar", a1.getValue());
    assertFalse(a1.isDefault());
  }

  public void testEquals() {
    ConfigParamAssignment a1 = new ConfigParamAssignment(d1, "foo");
    ConfigParamAssignment a2 = new ConfigParamAssignment(d1, "foo");
    assertEquals(a1, a2);
    a1.setDefault(true);
    assertNotEquals(a1, a2);
    a2.setDefault(true);
    assertEquals(a1, a2);

    assertNotEquals(a1, new ConfigParamAssignment(d1, "bar"));
    assertNotEquals(a1, new ConfigParamAssignment(d2, "foo"));

    assertNotEquals(a1, "foo");
  }

  public void testHash() {
    ConfigParamAssignment a1 = new ConfigParamAssignment(d1, "foo");
    ConfigParamAssignment a2 = new ConfigParamAssignment(d1, "foo");
    assertEquals(a1.hashCode(), a2.hashCode());
    a1.setDefault(true);
    a2.setDefault(true);
    assertEquals(a1.hashCode(), a2.hashCode());
  }

  public void testIllArg() {
    try {
      new ConfigParamAssignment(null);
      fail("ConfigParamAssignment(null) should throw IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // expected
    }
    try {
      new ConfigParamAssignment(null, "foo");
      fail("ConfigParamAssignment(null, \"foo\") should throw IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }
}
