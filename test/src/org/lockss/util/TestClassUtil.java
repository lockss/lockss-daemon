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

import java.util.*;
import java.io.*;
import junit.framework.*;
import org.lockss.test.*;

/**
 * This is the test class for org.lockss.util.ClassUtil
 */
public class TestClassUtil extends LockssTestCase {
  public TestClassUtil(String msg) {
    super(msg);
  }

  public void testIsPrimitive() {
    assertTrue(ClassUtil.isPrimitive("double"));
    assertTrue(ClassUtil.isPrimitive("float"));
    assertTrue(ClassUtil.isPrimitive("int"));
    assertTrue(ClassUtil.isPrimitive("long"));
    assertTrue(ClassUtil.isPrimitive("short"));
    assertTrue(ClassUtil.isPrimitive("byte"));
    assertTrue(ClassUtil.isPrimitive("char"));
    assertTrue(ClassUtil.isPrimitive("boolean"));
    assertFalse(ClassUtil.isPrimitive("Object"));
    assertFalse(ClassUtil.isPrimitive("String"));
    assertFalse(ClassUtil.isPrimitive("java.util.List"));
    assertFalse(ClassUtil.isPrimitive("Short"));
    assertFalse(ClassUtil.isPrimitive("java.lang.Boolean"));
  }

  private class Dummy {}

  public void testGetClassNameWithoutPackage() {
   String str = new String();
   assertEquals("String",ClassUtil.getClassNameWithoutPackage(str.getClass()));
   assertEquals("TestClassUtil",ClassUtil.getClassNameWithoutPackage(
        this.getClass()));
   Dummy dum = new Dummy();
   assertEquals("TestClassUtil$Dummy",ClassUtil.getClassNameWithoutPackage(
        dum.getClass()));
  }

  public void testObjectTypeName() {
    assertEquals("String",ClassUtil.objectTypeName("String"));
    assertEquals("Integer",ClassUtil.objectTypeName("int"));
    assertEquals("Character",ClassUtil.objectTypeName("char"));
    assertEquals("Byte",ClassUtil.objectTypeName("byte"));
  }

  public static Test suite() {
    return new TestSuite(TestClassUtil.class);
  }
}

