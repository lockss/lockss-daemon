/*
 * $Id: TestXStreamSerializer.java,v 1.12 2006-02-08 23:05:14 thib_gc Exp $
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

package org.lockss.util;

import java.io.*;
import java.nio.CharBuffer;

import org.lockss.app.LockssApp;
import org.lockss.util.ObjectSerializer.SerializationException;

/**
 * <p>Tests the {@link org.lockss.util.XStreamSerializer} class.</p>
 * @author Thib Guicherd-Callin
 */
public class TestXStreamSerializer extends ObjectSerializerTester {

  /**
   * <p>Used in
   * {@link TestXStreamSerializer#testPostDeserialization_TortureTest()}.</p>
   * @author Thib Guicherd-Callin
   */
  private static class ClassA implements LockssSerializable {

    private boolean explode1 = true;

    private boolean explode2 = true;

    public void detonate() {
      if (explode1) {
        fail("Failed torture test: ClassA::explode1");
      }
      else if (explode2) {
        fail("Failed torture test: ClassA::explode2");
      }
    }

    protected void postUnmarshal(LockssApp lockssContext) {
      explode1 = false; // defuse bomb
    }

    protected Object postUnmarshalResolve(LockssApp lockssContext) {
      explode2 = false; // defuse bomb
      return this;
    }

  }

  /**
   * <p>Used in
   * {@link TestXStreamSerializer#testPostDeserialization_TortureTest()}.</p>
   * @author Thib Guicherd-Callin
   */
  private static class ClassB extends ClassA {
    // Separate ClassA and ClassC's post-deserialization
    // methods by a level
  }

  /**
   * <p>Used in
   * {@link TestXStreamSerializer#testPostDeserialization_TortureTest()}.</p>
   * @author Thib Guicherd-Callin
   */
  private static class ClassC extends ClassB {

    private boolean explode1 = true;

    private boolean explode2 = true;

    public void detonate() {
      if (explode1) {
        fail("Failed torture test: ClassC::explode1");
      }
      else if (explode2) {
        fail("Failed torture test: ClassC::explode2");
      }
    }

    protected void postUnmarshal(LockssApp lockssContext) {
      super.postUnmarshal(lockssContext);
      explode1 = false; // defuse bomb
    }

    protected Object postUnmarshalResolve(LockssApp lockssContext) {
      super.postUnmarshalResolve(lockssContext);
      explode2 = false; // defuse bomb
      return this;
    }

  }

  /**
   * <p>Used in
   * {@link TestXStreamSerializer#testPostDeserialization_TortureTest()}.</p>
   * @author Thib Guicherd-Callin
   */
  private static class ClassD extends ClassC {

    private ClassD first;

    private ClassD second;

  }

  private static class PostUnmarshalExtMapBean
      extends ExtMapBean
      implements LockssSerializable {

    private boolean invoked;

    public PostUnmarshalExtMapBean() {
      this.invoked = false;
    }

    protected void postUnmarshal(LockssApp lockssContext) {
      this.invoked = true;
    }

  }

  private static class PostUnmarshalResolveExtMapBean
      extends ExtMapBean
      implements LockssSerializable {

    private boolean invoked;

    public PostUnmarshalResolveExtMapBean() {
      this.invoked = false;
    }

    protected Object postUnmarshalResolve(LockssApp lockssContext) {
      singleton.invoked = true;
      return singleton;
    }

    public static final PostUnmarshalResolveExtMapBean singleton =
      new PostUnmarshalResolveExtMapBean();

  }

  /**
   * <p>Gives the post-deserialization mechanism a thorough test.</p>
   * <p>This test consists of a cyclic graph of objects of type D.
   * Type D inherits from type C which inherits from type B which
   * inherits from type A. Types C and A define post-deserialization
   * processing (but not D and B). The class hierarchy is such that
   * the test will fail unless all post-deserialization methods at all
   * levels get called.</p>
   * @throws Exception if an unexpected or unhandled problem arises.
   */
  public void testPostDeserialization_TortureTest()
      throws Exception {

    // Set up object graph
    ClassD d1 = new ClassD();
    ClassD d2 = new ClassD();
    ClassD d3 = new ClassD();
    ClassD d4 = new ClassD();
    ClassD d5 = new ClassD();
    d1.first = d2; d1.second = d4;
    d2.first = d3; d2.second = d5;
    d3.first = d1; d3.second = d2;
    d4.first = d2; d4.second = d5;
    d5.first = d2; d5.second = d3;

    // Set up needed objects
    ObjectSerializer serializer = new XStreamSerializer(null);
    ObjectSerializer deserializer = new XStreamSerializer(null);
    StringWriter writer = new StringWriter();
    StringReader reader;

    // Round trip
    serializer.serialize(writer, d1);
    reader = new StringReader(writer.toString());
    ClassD d = (ClassD)deserializer.deserialize(reader);

    // Tests
    d.detonate(); // aka d1
    d.first.detonate(); // aka d2
    d.first.first.detonate(); // aka d3
    d.second.detonate(); // aka d4
    d.second.second.detonate(); // aka d5
  }

  public void testPostUnmarshal() throws Exception {
    // Set up needed objects
    ObjectSerializer serializer = makeObjectSerializer_ExtMapBean();
    ObjectSerializer deserializer = makeObjectSerializer_ExtMapBean();
    PostUnmarshalExtMapBean original = new PostUnmarshalExtMapBean();
    original.setMap(makeSample_TypedEntryMap().m_map);
    PostUnmarshalExtMapBean clone;
    StringWriter writer = new StringWriter();
    StringReader reader;

    // Round trip
    serializer.serialize(writer, (LockssSerializable)original);
    reader = new StringReader(writer.toString());
    clone = (PostUnmarshalExtMapBean)deserializer.deserialize(reader);

    // Tests
    assertEquals(original.getMap(), clone.getMap());
    assertTrue("postUnmarshal was not invoked", clone.invoked);
  }

  public void testPostUnmarshalResolve() throws Exception {
    // Set up needed objects
    ObjectSerializer serializer = makeObjectSerializer_ExtMapBean();
    ObjectSerializer deserializer = makeObjectSerializer_ExtMapBean();
    PostUnmarshalResolveExtMapBean original = new PostUnmarshalResolveExtMapBean();
    original.setMap(makeSample_TypedEntryMap().m_map);
    PostUnmarshalResolveExtMapBean clone;
    StringWriter writer = new StringWriter();
    StringReader reader;

    // Round trip
    serializer.serialize(writer, (LockssSerializable)original);
    reader = new StringReader(writer.toString());
    clone = (PostUnmarshalResolveExtMapBean)deserializer.deserialize(reader);

    // Tests
    assertTrue("postUnmarshalResolve was not invoked", clone.invoked);
    assertSame(
        "The object substitution was not performed by postUnmarshalResolve",
        PostUnmarshalResolveExtMapBean.singleton,
        clone
    );
  }

  public void testSupportsInstantiationError() throws Exception {
    /*
     * begin LOCAL CLASS
     * =================
     */
    class InstantiationErrorReader extends Reader {
      private void die() { throw new InstantiationError(); }
      public void close() { die(); }
      public void mark(int i) { die(); }
      public boolean markSupported() { die(); return false; }
      public int read() { die(); return 0; }
      public int read(char[] c, int i, int j) { die(); return 0; }
      public int read(char[] c) { die(); return 0; }
      public int read(CharBuffer c) { die(); return 0; }
      public boolean ready() { die(); return false; }
      public void reset() { die(); }
      public long skip(long n) { die(); return 0L; }
    }
    /*
     * end LOCAL CLASS
     * ===============
     */

    XStreamSerializer deserializer = new XStreamSerializer();
    Reader bomb = new InstantiationErrorReader();
    try {
      deserializer.deserialize(bomb);
      fail("Should have thrown a SerializationException");
    }
    catch (SerializationException seIgnore) {
      // all is well
    }
  }

  protected ObjectSerializer makeObjectSerializer_ExtMapBean() {
    return new XStreamSerializer();
  }

}
