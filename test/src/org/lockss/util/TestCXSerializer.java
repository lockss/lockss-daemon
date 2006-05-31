/*
 * $Id: TestCXSerializer.java,v 1.8 2006-05-31 17:54:50 thib_gc Exp $
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

import junit.framework.Test;

import org.lockss.test.LockssTestCase;

/**
 * <p>Tests the Castor/XStream hybrid serializer.</p>
 * @author Thib Guicherd-Callin
 */
public class TestCXSerializer extends ObjectSerializerTester {

  /**
   * <p>Tests that the deserializer in XStreamOverWrite mode behaves
   * appropriately with Castor input.</p>
   * @throws Exception if an unexpected or unhandled problem arises.
   */
  public void testXStreamOverwriteMode() throws Exception {

    // Make a sample object
    ExtMapBean original = makeSample_ExtMapBean();

    // Define variant actions
    RememberFile[] actions = new RememberFile[] {
        // Wit a File
        new RememberFile() {
          public void doSomething(ObjectSerializer serializer) throws Exception {
            serializer.deserialize(file);
          }
        },
        // With a String
        new RememberFile() {
          public void doSomething(ObjectSerializer serializer) throws Exception {
            serializer.deserialize(file.toString());
          }
        },
    };

    // For each variant action...
    for (int action = 0 ; action < actions.length ; ++action) {
      logger.debug("Begin with action " + action);

      // For each type of deserializer...
      ObjectSerializer[] deserializers = getObjectSerializers_ExtMapBean();
      for (int deserializer = 0 ; deserializer < deserializers.length ; ++deserializer) {
        if (((CXSerializer)deserializers[deserializer]).getCompatibilityMode() == CXSerializer.XSTREAM_OVERWRITE_MODE) {
          logger.debug("Begin with deserializer " + deserializer);

          // Assign a file to the action
          actions[action].file = File.createTempFile("testfile", ".xml");
          actions[action].file.deleteOnExit();

          // Serialize in Castor format
          CastorSerializer serializer = new CastorSerializer(ExternalizableMap.MAPPING_FILE_NAME,
                                                             ExtMapBean.class);

          // Read back
          serializer.serialize(actions[action].file, original);
          ExtMapBean clone1 = (ExtMapBean)deserializers[deserializer].deserialize(actions[action].file);
          assertEquals(original.getMap(), clone1.getMap());

          // Verify that the conversion has been made
          XStreamSerializer verify = new XStreamSerializer();
          ExtMapBean clone2 = (ExtMapBean)verify.deserialize(actions[action].file);
          assertEquals(original.getMap(), clone2.getMap());
        }
        else {
          // Skip if not in XStreamOverwriteMode
          logger.debug("Skipping deserializer " + deserializer);
        }

      }
    }

  }

  /**
   * <p>Checks that the serializer writes output in a format that is
   * appropriate for its mode, by re-reading it through a
   * corresponding single-mode deserializer.</p>
   * @throws Exception if an error condition arises.
   */
  public void testOutputFormat() throws Exception {
    ObjectSerializer[] serializers = getObjectSerializers_ExtMapBean();
    for (int serializer = 0 ; serializer < serializers.length ; ++serializer) {
      ObjectSerializer deserializer = null;
      switch (((CXSerializer)serializers[serializer]).getCompatibilityMode()) {
        case CXSerializer.CASTOR_MODE:
          deserializer = new CastorSerializer(ExternalizableMap.MAPPING_FILE_NAME,
                                              ExtMapBean.class);
          break;
        case CXSerializer.XSTREAM_MODE:
        case CXSerializer.XSTREAM_OVERWRITE_MODE:
          deserializer = new XStreamSerializer();
          break;
        default: // shouldn't happen
          fail("Unexpected compatibility mode (#" + serializer + ")");
      }
      performRoundTrip(serializers[serializer], deserializer);
    }

  }

  /**
   * <p>Checks that the deserializer can read input in Castor
   * format.</p>
   * @throws Exception if an error condition arises.
   */
  public void testReadCastorInput() throws Exception {
    ObjectSerializer[] deserializers = getObjectSerializers_ExtMapBean();
    for (int deserializer = 0 ; deserializer < deserializers.length ; ++deserializer) {
      CastorSerializer serializer = new CastorSerializer(ExternalizableMap.MAPPING_FILE_NAME,
                                                         ExtMapBean.class);
      performRoundTrip(serializer, deserializers[deserializer]);
    }
  }

  /**
   * <p>Checks that the deserializer can read input in XStream
   * format.</p>
   * @throws Exception if an error condition arises.
   */
  public void testReadXStreamInput() throws Exception {
    ObjectSerializer[] deserializers = getObjectSerializers_ExtMapBean();
    for (int deserializer = 0 ; deserializer < deserializers.length ; ++deserializer) {
      XStreamSerializer serializer = new XStreamSerializer();
      performRoundTrip(serializer, deserializers[deserializer]);
    }
  }

  /**
   * <p>A specialized version of
   * {@link ObjectSerializerTester#getObjectSerializers_ExtMapBean}
   * that produces on series of serializers for each compatibility
   * mode.</p>
   * @see ObjectSerializerTester#getObjectSerializers_ExtMapBean
   */
  protected ObjectSerializer[] getObjectSerializers_ExtMapBean() {
    // Compatibility modes
    int[] compatModes = new int[] {
        CXSerializer.CASTOR_MODE,
        CXSerializer.XSTREAM_MODE,
        CXSerializer.XSTREAM_OVERWRITE_MODE,
    };

    int parentLength = getMinimalObjectSerializers_ExtMapBean().length;
    CXSerializer[] result = new CXSerializer[parentLength * compatModes.length];

    // For each compatibility mode...
    int current = 0;
    for (int compatMode = 0 ; compatMode < compatModes.length ; ++compatMode) {
      /// ...get a full stack of serializers (produced by this class)
      ObjectSerializer[] parent = getMinimalObjectSerializers_ExtMapBean();
      // and set their compatibility mode
      for (int serializer = 0 ; serializer < parentLength ; ++serializer) {
        result[current] = (CXSerializer)parent[serializer];
        result[current++].setCompatibilityMode(compatModes[compatMode]);
      }
    }

    return result;
  }

  /* Inherit documentation */
  protected ObjectSerializer makeObjectSerializer_ExtMapBean(boolean saveTempFiles,
                                                             int failedDeserializationMode) {
    return new CXSerializer(ExternalizableMap.MAPPING_FILE_NAME,
                            ExtMapBean.class,
                            saveTempFiles,
                            failedDeserializationMode);
  }

  /**
   * <p>Performs a quick round trip into the serializer and back from
   * the deserializer, expecting the operation to succeed.</p>
   * @param serializer   Any ObjectSerializer.
   * @param deserializer Any ObjectSerializer.
   * @throws Exception if an unexpected or unhandled problem arises.
   */
  private void performRoundTrip(ObjectSerializer serializer,
                                ObjectSerializer deserializer)
      throws Exception {
    // Set up needed objects
    ExtMapBean original = makeSample_ExtMapBean();
    ExtMapBean clone;
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayInputStream in;

    try {
      serializer.serialize(out, original);
      in = new ByteArrayInputStream(out.toByteArray());
      clone = (ExtMapBean)deserializer.deserialize(in);
    }
    catch (SerializationException se) {
      StringBuffer buffer = new StringBuffer();
      buffer.append("SerializationException thrown while in correct mode. ");
      buffer.append("Nested message: ");
      buffer.append(se.getMessage());
      fail(buffer.toString());
    }
  }

}
