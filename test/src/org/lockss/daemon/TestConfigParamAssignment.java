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

package org.lockss.daemon;

import java.util.*;
import org.lockss.test.*;
import org.lockss.util.*;

/**
 * This is the test class for org.lockss.daemon.ConfigParamAssignment
 */

public class TestConfigParamAssignment extends LockssTestCase {
  protected static Logger log = Logger.getLogger("TestCPA");
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
    assertFalse(a2.isEditable());
    a2.setEditable(true);
    assertTrue(a2.isEditable());
    a2.setEditable(false);
    assertFalse(a2.isEditable());
    assertEquals(d1, a1.getParamDescr());
    assertNull(a1.getValue());
    a1.setValue("bar");
    assertEquals("bar", a1.getValue());
    assertFalse(a1.isEditable());
  }

  public void testIsEditable() {
    ConfigParamAssignment a1 = new ConfigParamAssignment(d1, null);
    ConfigParamAssignment a2 = new ConfigParamAssignment(d2, "foo");
    ConfigParamAssignment a3 = new ConfigParamAssignment(d1);
    assertTrue(a1.isEditable());
    assertTrue(a3.isEditable());
    assertFalse(a2.isEditable());
    log.debug("Editable: " + a1);
    log.debug("Not editable: " + a2);
    a1.setEditable(false);
    a2.setEditable(false);
    assertFalse(a1.isEditable());
    assertFalse(a2.isEditable());
    a1.setEditable(true);
    a2.setEditable(true);
    assertTrue(a1.isEditable());
    assertTrue(a2.isEditable());
  }

  public void testEquals() {
    ConfigParamAssignment a1 = new ConfigParamAssignment(d1, "foo");
    ConfigParamAssignment a2 = new ConfigParamAssignment(d1, "foo");
    assertEquals(a1, a2);
    a1.setEditable(true);
    assertNotEquals(a1, a2);
    a2.setEditable(true);
    assertEquals(a1, a2);

    assertNotEquals(a1, new ConfigParamAssignment(d1, "bar"));
    assertNotEquals(a1, new ConfigParamAssignment(d2, "foo"));

    assertNotEquals(a1, "foo");
  }

  public void testHash() {
    ConfigParamAssignment a1 = new ConfigParamAssignment(d1, "foo");
    ConfigParamAssignment a2 = new ConfigParamAssignment(d1, "foo");
    assertEquals(a1.hashCode(), a2.hashCode());
    a1.setEditable(true);
    a2.setEditable(true);
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
