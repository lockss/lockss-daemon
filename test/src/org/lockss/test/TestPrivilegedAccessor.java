/*
 * $Id: TestPrivilegedAccessor.java,v 1.3 2002-09-11 00:59:14 tal Exp $
 */

/*

Copyright (c) 2000-2002 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.test;
import junit.framework.*;

public class TestPrivilegedAccessor extends TestCase {
  public TestPrivilegedAccessor(String name) {
    super(name);
  }
  
  public void testParent() throws Exception {
    MockParent parent = new MockParent("Charlie");
    assertEquals("Charlie", PrivilegedAccessor.getValue(parent, "m_name"));
    PrivilegedAccessor.invokeMethod(parent, "setName", "Timmah!");
    assertEquals("Timmah!", PrivilegedAccessor.getValue(parent,"m_name"));
  }

  public void testChild() throws Exception {
    MockChild child = new MockChild("Charlie", 8);
    assertEquals("Charlie", PrivilegedAccessor.getValue(child, "m_name"));
    assertEquals(new Integer(8),
		 PrivilegedAccessor.getValue(child, "m_number"));

    PrivilegedAccessor.invokeMethod(child, "setName", "Timmah!");
    PrivilegedAccessor.invokeMethod(child, "setNumber", new Integer(3));

    assertEquals("Timmah!", PrivilegedAccessor.getValue(child,"m_name"));
    assertEquals(new Integer(3),
		 PrivilegedAccessor.getValue(child, "m_number"));
  }

  public void testChildWithParentReference() throws Exception {
    MockParent parent = new MockChild("Charlie", 8);
    assertEquals("Charlie", PrivilegedAccessor.getValue(parent, "m_name"));
    assertEquals(new Integer(8),
		 PrivilegedAccessor.getValue(parent, "m_number"));

    Object args[] = {"Timmah!", new Integer(3)};
    PrivilegedAccessor.invokeMethod(parent, "setData", args);

    assertEquals("Timmah!", PrivilegedAccessor.getValue(parent,"m_name"));
    assertEquals(new Integer(3),
		 PrivilegedAccessor.getValue(parent, "m_number"));

    PrivilegedAccessor.invokeMethod(parent, "setName", "prashant");
    assertEquals("prashant", PrivilegedAccessor.getValue(parent,"m_name"));
  }

  public void testInvalidField() throws Exception {
    MockParent parent = new MockParent("Charlie");
    try {
      Object value = PrivilegedAccessor.getValue(parent, "zzz");
      fail("Should throw NoSuchFieldException");
    } catch (NoSuchFieldException e) {
    }
  }

  public void testInvalidMethodName() throws Exception {
    MockChild child = new MockChild("Charlie", 8);
    try {
      PrivilegedAccessor.invokeMethod(child, "zzz", "Timmah!");
      fail("Should throw NoSuchMethodException");
    } catch(NoSuchMethodException e) {
    }
  }

  public void testInvalidArguments() throws Exception {
    MockChild child = new MockChild("Charlie", 8);
    try {
      PrivilegedAccessor.invokeMethod(child, "setData", "Timmah!");
      fail("Should throw NoSuchMethodException");
    } catch (NoSuchMethodException e) {
    }
  }

  public void testInstanceParam() throws Exception {
    MockParent parent = new MockParent();
    Object nullString = new PrivilegedAccessor.Instance(String.class, null);
    Boolean bool = 
      (Boolean)PrivilegedAccessor.invokeMethod(parent, "isNullString", 
					       nullString);
    assertTrue(bool.booleanValue());
  }

  public void testUnambiguousNullArg() throws Exception {
    MockParent parent = new MockParent();
    Object[] args = {null};
    Boolean bool =
      (Boolean)PrivilegedAccessor.invokeMethod(parent, "isNullString", args);
    assertTrue(bool.booleanValue());
  }

  public void testAmbiguousNullArg() throws Exception {
    MockChild child = new MockChild("Charlie", 8);
    Object[] args1 = {"foo"};
    Object[] args2 = {null};
    assertEquals("child.string",
		 PrivilegedAccessor.invokeMethod(child, "over", args1));
    try {
      PrivilegedAccessor.invokeMethod(child, "over", args2);
      fail("invokeMethod should have thrown an AmbiguousMethodException for null parameter");
    } catch (PrivilegedAccessor.AmbiguousMethodException e) {
    }
  }
  public void testUnambiguousArg() throws Exception {
    MockParent parent = new MockParent();
    MockChild child = new MockChild("Charlie", 8);
    Object[] args1 = {new Integer(1), new Float(2)};
    Object[] args2 = {new Float(1), new Integer(2)};
    Object[] args3 = {new Float(1), new PrivilegedAccessor.Instance(Number.class, new Float(2.0))};
    assertEquals("parent.string",
		 PrivilegedAccessor.invokeMethod(parent, "over", "foo"));
    assertEquals("child.string",
		 PrivilegedAccessor.invokeMethod(child, "over", "foo"));
    assertEquals("child.number",
		 PrivilegedAccessor.invokeMethod(child, "over",
						 new Integer(1)));
    assertEquals("child.float",
		 PrivilegedAccessor.invokeMethod(child, "over",
						 new Float(1.2)));
    assertEquals("child.number",
		 PrivilegedAccessor.invokeMethod(child, "over",
						 new PrivilegedAccessor.
						   Instance(Number.class, 
							    new Float(1.2))));
		 
    assertEquals("child.number.float",
		 PrivilegedAccessor.invokeMethod(child, "over", args1));
    assertEquals("child.float.number",
		 PrivilegedAccessor.invokeMethod(child, "over", args2));
    assertEquals("child.float.number",
		 PrivilegedAccessor.invokeMethod(child, "over", args3));
  }

  public void testAambiguousArg() throws Exception {
    MockChild child = new MockChild("Charlie", 8);
    Object[] args1 = {new Float(1), new Float(2)};
    try {
      PrivilegedAccessor.invokeMethod(child, "over", args1);
      fail("invokeMethod should have thrown an AmbiguousMethodException");
    } catch (PrivilegedAccessor.AmbiguousMethodException e) {
    }
  }

  public void testStatic() throws Exception {
    MockParent parent = new MockParent();
    MockChild child = new MockChild("Charlie", 8);
    assertEquals("parent.static",
		 PrivilegedAccessor.invokeMethod(parent, "stat", null));
    assertEquals("child.static",
		 PrivilegedAccessor.invokeMethod(child, "stat", null));
    assertEquals("parent.static",
		 PrivilegedAccessor.invokeMethod(new PrivilegedAccessor.
		   Instance(MockParent.class, child),
						 "stat", null));
  }

  // Test utility classes

  public static class MockParent {
    private String m_name;

    public MockParent() {
    }
    public MockParent(String name) {
      m_name = name;
    }

    public String getName() {
      return m_name;
    }

    protected void setName(String newName) {
      m_name = newName;
    }

    public boolean isNullString(String str) {
      return (str == null);
    }
    protected String over(String s) {
      return "parent.string";
    }
    private static String stat() {
      return "parent.static";
    }
  }

  public static class MockChild extends MockParent {
    private int m_number;

    public MockChild(String name, int number) {
      super(name);
      m_number = number;
    }

    public int getNumber() {
      return m_number;
    }

    private void setNumber(Integer number) {
      m_number = number.intValue();
    }
    private void setData(String name, Integer number) {
      setName(name);
      m_number = number.intValue();
    }

    protected String over(String s) {
      return "child.string";
    }
    protected String over(Float s) {
      return "child.float";
    }
    protected String over(Number s) {
      return "child.number";
    }
    protected String over(Float f, Number n) {
      return "child.float.number";
    }
    protected String over(Number n, Float f) {
      return "child.number.float";
    }
    private static String stat() {
      return "child.static";
    }
  }
}
