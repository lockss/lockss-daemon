/*
 * $Id: TestCXSerializer.java,v 1.6 2006-01-20 19:01:02 thib_gc Exp $
 */

/*

Copyright (c) 2002-2006 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.util.ObjectSerializer.SerializationException;

/**
 * <p>Tests the Castor/XStream hybrid serializer.</p>
 * @author Thib Guicherd-Callin
 */
public abstract class TestCXSerializer extends ObjectSerializerTester {

  /**
   * <p>A version of {@link TestCXSerializer} that uses
   * {@link CXSerializer#CASTOR_MODE}.</p>
   * @author Thib Guicherd-Callin
   */
  public static class CastorMode extends TestCXSerializer {

    /* Inherit documentation */
    protected int getMode() {
      return CXSerializer.CASTOR_MODE;
    }

    /* Inherit documentation */
    protected ObjectSerializer getStrictDeserializer() {
      return new CastorSerializer(
          null,
          ExternalizableMap.MAPPING_FILE_NAME,
          ExtMapBean.class
      );
    }

  }

  /**
   * <p>A version of {@link TestCXSerializer} that uses
   * {@link CXSerializer#XSTREAM_MODE}.</p>
   * @author Thib Guicherd-Callin
   */
  public static class XStreamMode extends TestCXSerializer {

    /* Inherit documentation */
    protected int getMode() {
      return CXSerializer.XSTREAM_MODE;
    }

    /* Inherit documentation */
    protected ObjectSerializer getStrictDeserializer() {
      return new XStreamSerializer();
    }

  }

  /**
   * <p>A version of {@link TestCXSerializer} that uses
   * {@link CXSerializer#XSTREAM_OVERWRITE_MODE}.</p>
   * @author Thib Guicherd-Callin
   */
  public static class XStreamOverwriteMode extends TestCXSerializer {

    /* Inherit documentation */
    protected int getMode() {
      return CXSerializer.XSTREAM_OVERWRITE_MODE;
    }

    /* Inherit documentation */
    protected ObjectSerializer getStrictDeserializer() {
      return new XStreamSerializer();
    }

  }

  /**
   * <p>Checks that the deserializer can read input in Castor
   * format.</p>
   * @throws Exception if an error condition arises.
   */
  public void testReadCastorInput() throws Exception {
    CastorSerializer serializer = new CastorSerializer(
        null,
        ExternalizableMap.MAPPING_FILE_NAME,
        ExtMapBean.class
    );
    CXSerializer deserializer = makeExtMapBeanSerializer();
    performRoundTrip(serializer, deserializer);
  }

  /**
   * <p>Checks that the serializer writes output in a format that is
   * appropriate for its mode, by re-reading it through a
   * corresponding single-mode deserializer.</p>
   * @throws Exception if an error condition arises.
   * @see #getMode
   * @see #getStrictDeserializer
   */
  public void testOutputFormat() throws Exception {
    CXSerializer serializer = makeExtMapBeanSerializer();
    ObjectSerializer deserializer = getStrictDeserializer();
    performRoundTrip(serializer, deserializer);
  }

  /**
   * <p>Checks that the deserializer can read input in XStream
   * format.</p>
   * @throws Exception if an error condition arises.
   */
  public void testReadXStreamInput() throws Exception {
    XStreamSerializer serializer = new XStreamSerializer(null);
    CXSerializer deserializer = makeExtMapBeanSerializer();
    performRoundTrip(serializer, deserializer);
  }

  /**
   * <p>Returns the CXSerializer mode currently being tested.</p>
   * @return A CXSerializer compatibility mode number.
   */
  protected abstract int getMode();

  /**
   * <p>Builds a deserializer of the type returned by {@link #getMode}
   * (of type {@link CastorSerializer} or
   * {@link XStreamSerializer}).</p>
   * @return A single-mode deserializer instance.
   */
  protected abstract ObjectSerializer getStrictDeserializer();

  /* Inherit documentation */
  protected ObjectSerializer makeObjectSerializer_ExtMapBean() {
    return makeExtMapBeanSerializer();
  }

  /**
   * <p>Produces an instance based on objects of type
   * {@link ExtMapBean}.</p>
   * @return A newly instantiated instance of this class, configured
   *         properly to operate on objects of type
   *         {@link ExtMapBean}.
   */
  private CXSerializer makeExtMapBeanSerializer() {
    CXSerializer cxs = new CXSerializer(
        null,
        ExternalizableMap.MAPPING_FILE_NAME,
        ExtMapBean.class
    );
    cxs.setCurrentMode(getMode());
    return cxs;
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

  /**
   * <p>Produces a test suite encompassing all three modes of
   * operation of {@link CXSerializer}.</p>
   * @return {@inheritDoc}
   */
  public static Test suite() {
    return LockssTestCase.variantSuites(new Class[] {
        CastorMode.class,
        XStreamMode.class,
        XStreamOverwriteMode.class
    });
  }

}
