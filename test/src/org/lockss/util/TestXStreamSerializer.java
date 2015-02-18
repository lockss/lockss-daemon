/*
 * $Id$
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

import org.lockss.app.LockssApp;
import org.lockss.test.StringInputStream;

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

  /**
   * <p>A specialized version of {@link ExtMapBean} whose instances
   * require post-deserialization.</p>
   * @author Thib Guicherd-Callin
   */
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

  /**
   * <p>A specialized version of {@link ExtMapBean} whose instances
   * require post-deserialization resolving.</p>
   * @author Thib Guicherd-Callin
   */
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
  public void testPostDeserialization_TortureTest() throws Exception {
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
    final ClassD original = d1;

    DoRoundTrip[] actions = getRoundTripActions(original);

    for (int action = 0 ; action < actions.length ; ++action) {
      logger.debug("Begin with action " + action);
      ObjectSerializer[] serializers = getObjectSerializers_ExtMapBean();
      ObjectSerializer[] deserializers = getObjectSerializers_ExtMapBean();
      for (int serializer = 0 ; serializer < serializers.length ; ++serializer) {
        logger.debug("Begin with serializer/deserializer " + serializer);
        actions[action].doRoundTrip(serializers[serializer],
                                    deserializers[serializer]);
        ClassD clone = (ClassD)actions[action].result;
        clone.detonate(); // aka d1
        clone.first.detonate(); // aka d2
        clone.first.first.detonate(); // aka d3
        clone.second.detonate(); // aka d4
        clone.second.second.detonate(); // aka d5
      }
    }
  }

  /**
   * <p>Tests the behavior of the post-unmarshal mechanism.</p>
   * @throws Exception if an unexpected or unhandled problem arises.
   */
  public void testPostUnmarshal() throws Exception {

    // Make a sample object
    final PostUnmarshalExtMapBean original = new PostUnmarshalExtMapBean();
    original.setMap(makeSample_TypedEntryMap().m_map);

    // Define variant actions
    DoRoundTrip[] actions = getRoundTripActions(original);

    // For each variant action...
    for (int action = 0 ; action < actions.length ; ++action) {
      logger.debug("Begin with action " + action);

      // For each serializer/deserializer pair...
      ObjectSerializer[] serializers = getObjectSerializers_ExtMapBean();
      ObjectSerializer[] deserializers = getObjectSerializers_ExtMapBean();
      for (int serializer = 0 ; serializer < serializers.length ; ++serializer) {
        logger.debug("Begin with serializer/deserializer " + serializer);

        // Perform variant action
        actions[action].doRoundTrip(serializers[serializer],
                                    deserializers[serializer]);

        // Verify results
        PostUnmarshalExtMapBean clone = (PostUnmarshalExtMapBean)actions[action].result;
        assertEquals(original.getMap(), clone.getMap());
        assertTrue("postUnmarshal was not invoked", clone.invoked);
      }
    }
  }

  /**
   * <p>Tests the behavior of the post-unmarshal resolving
   * mechanism.</p>
   * @throws Exception if an unexpected or unhandled problem arises.
   */
  public void testPostUnmarshalResolve() throws Exception {

    // Make a sample object
    final PostUnmarshalResolveExtMapBean original = new PostUnmarshalResolveExtMapBean();
    original.setMap(makeSample_TypedEntryMap().m_map);

    // Define vairant actions
    DoRoundTrip[] actions = getRoundTripActions(original);

    // For each variant action...
    for (int action = 0 ; action < actions.length ; ++action) {
      logger.debug("Begin with action " + action);

      // For each serializer/deserializer pair...
      ObjectSerializer[] serializers = getObjectSerializers_ExtMapBean();
      ObjectSerializer[] deserializers = getObjectSerializers_ExtMapBean();
      for (int serializer = 0 ; serializer < serializers.length ; ++serializer) {
        logger.debug("Begin with serializer/deserializer " + serializer);

        // Perform variant action
        actions[action].doRoundTrip(serializers[serializer],
                                    deserializers[serializer]);

        // Verify results
        PostUnmarshalResolveExtMapBean clone = (PostUnmarshalResolveExtMapBean)actions[action].result;
        assertTrue("postUnmarshalResolve was not invoked", clone.invoked);
        assertSame("The object substitution was not performed by postUnmarshalResolve",
                   PostUnmarshalResolveExtMapBean.singleton,
                   clone);
      }
    }
  }

  /**
   * <p>Checks that {@link InstantiationError} are handled
   * gracefully.</p>
   * @throws Exception if an unexpected or unhandled problem arises.
   */
  public void testSupportsInstantiationError() throws Exception {

    // Define variant actions
    DoSomething[] actions = new DoSomething[] {
        // With a Reader
        new DoSomething() {
          public void doSomething(ObjectSerializer serializer) throws Exception {
            serializer.deserialize(new StringReader("") {
              public int read(char[] cbuf, int off, int len) throws IOException {
                throw new InstantiationError();
              }
            });
          }
        },
        // With an InputStream
        new DoSomething() {
          public void doSomething(ObjectSerializer serializer) throws Exception {
            serializer.deserialize(new StringInputStream("") {
              public int read(byte[] b, int off, int len) throws IOException {
                throw new InstantiationError();
              }
            });
          }
        },
    };

    // For each variant action...
    for (int action = 0 ; action < actions.length ; ++action) {
      logger.debug("Begin with action " + action);

      // For each type for deserializer...
      ObjectSerializer[] deserializers = getObjectSerializers_ExtMapBean();
      for (int deserializer = 0 ; deserializer < deserializers.length ; ++deserializer) {
        logger.debug("Begin with deserializer " + deserializer);

        try {
          // Perform variant action
          actions[action].doSomething(deserializers[deserializer]);
        }
        catch (SerializationException ignore) {
          try {
            Throwable thr = ignore.getCause().getCause();
            if (thr instanceof InstantiationError) {
              // expected; success
            }
            else {
              throw ignore; // unexpected
            }
          }
          catch (NullPointerException npe) {
            throw ignore; // unexpected
          }
        }
      }
    }
  }

  /* Inherit documentation */
  protected ObjectSerializer[] getObjectSerializers_ExtMapBean() {
    return getMinimalObjectSerializers_ExtMapBean();
  }

  /* Inherit documentation */
  protected ObjectSerializer makeObjectSerializer_ExtMapBean(boolean saveTempFiles,
                                                             int failedDeserializationMode) {
    return new XStreamSerializer(saveTempFiles, failedDeserializationMode);
  }

}
