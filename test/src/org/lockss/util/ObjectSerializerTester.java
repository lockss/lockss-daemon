/*
 * $Id: ObjectSerializerTester.java,v 1.3 2006-01-20 19:01:02 thib_gc Exp $
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
import java.net.URL;

import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.XMLUnit;

/**
 * <p>Tests the {@link org.lockss.util.ObjectSerializer} abstract
 * class.</p>
 * <p>This test case uses the standard pattern for unit-testing
 * interfaces and abstract classes. Subclasses of
 * {@link org.lockss.util.ObjectSerializer} are unit-tested by
 * extending this class and implementing the specified abstract
 * methods, so that unit tests for functionality inherited from
 * the abstract class are automatically inherited by subclasses
 * of this unit test.</p>
 * <p>For the convenience of subclasses, this class extends
 * {@link org.custommonkey.xmlunit.XMLTestCase} rather than
 * {@link junit.framework.TestCase}.</p>
 * @author Thib Guicherd-Callin
 */
public abstract class ObjectSerializerTester
    extends XMLTestCase {

  /**
   * <p>Tests whether the same input data deserialized into two new
   * Object instances produces a consistent result. </p>
   * @throws Exception if an unexpected or unhandled problem arises.
   */
  public void testDeserializeTwice()
      throws Exception {
    // Set up needed objects
    ObjectSerializer serializer = makeObjectSerializer_ExtMapBean();
    ObjectSerializer deserializer1 = makeObjectSerializer_ExtMapBean();
    ObjectSerializer deserializer2 = makeObjectSerializer_ExtMapBean();
    ExtMapBean original = makeSample_ExtMapBean();
    ExtMapBean clone1;
    ExtMapBean clone2;
    File tempFile = File.createTempFile("test", ".xml");
    tempFile.deleteOnExit();

    // Serialize
    serializer.serialize(tempFile, original);
    // Deserialize twice
    clone1 = (ExtMapBean)deserializer1.deserialize(tempFile);
    clone2 = (ExtMapBean)deserializer2.deserialize(tempFile);
    // Test
    assertEquals(clone1.getMap(), clone2.getMap());
  }

  /**
   * <p>Tests if a sample ExtMapBean object can be serialized
   * and deserialized without changing.</p>
   * @throws Exception if an unexpected or unhandled problem arises.
   */
  public void testRoundTrip_ExtMapBean()
      throws Exception {
    // Set up needed objects
    ObjectSerializer serializer = makeObjectSerializer_ExtMapBean();
    ObjectSerializer deserializer = makeObjectSerializer_ExtMapBean();
    ExtMapBean original = makeSample_ExtMapBean();
    ExtMapBean clone;
    StringWriter writer = new StringWriter();
    StringReader reader;

    try {
      // Round trip
      serializer.serialize(writer, original);
      reader = new StringReader(writer.toString());
      clone = (ExtMapBean)deserializer.deserialize(reader);
      // Test for equality
      assertEquals(original.getMap(), clone.getMap());
    }
    catch (ClassCastException cce) {
      failClassCastException(
          "testRoundTrip_ExtMapBean", original.getClass());
    }
  }

  /**
   * <p>Checks that serializing with an OutputStream argument gives
   * the same result as serializing with a Writer argument.</p>
   * @throws Exception if an unexpected or unhandled problem arises.
   */
  public void testSameAsSerializeWriter_InputStream()
      throws Exception {
    // Set up needed objects
    ObjectSerializer serializer1 = makeObjectSerializer_ExtMapBean();
    ObjectSerializer serializer2 = makeObjectSerializer_ExtMapBean();
    ExtMapBean original = makeSample_ExtMapBean();
    File tempFile = File.createTempFile("test", ".xml");
    tempFile.deleteOnExit();
    StringWriter writer = new StringWriter();
    FileOutputStream outStream = new FileOutputStream(tempFile);

    // Serialize twice
    serializer1.serialize(writer, original);
    serializer2.serialize(outStream, original);
    // Compare
    performXmlAssertion(new StringReader(writer.toString()), tempFile);
  }

  /**
   * <p>Checks that serializing with a File argument gives the same
   * result as serializing with a Writer argument.</p>
   * @throws Exception if an unexpected or unhandled problem arises.
   */
  public void testSameAsSerializeWriter_File()
      throws Exception {
    // Set up needed objects
    ObjectSerializer serializer1 = makeObjectSerializer_ExtMapBean();
    ObjectSerializer serializer2 = makeObjectSerializer_ExtMapBean();
    ExtMapBean original = makeSample_ExtMapBean();
    File tempFile = File.createTempFile("test", ".xml");
    tempFile.deleteOnExit();
    StringWriter writer = new StringWriter();

    // Serialize twice
    serializer1.serialize(writer, original);
    serializer2.serialize(tempFile /* a File */, original);
    // Compare
    performXmlAssertion(new StringReader(writer.toString()), tempFile);
  }

  /**
   * <p>Checks that serializing with a String argument gives the same
   * result as serializing with a Writer argument.</p>
   * @throws Exception if an unexpected or unhandled problem arises.
   */
  public void testSameAsSerializeWriter_String()
      throws Exception {
    // Set up needed objects
    ObjectSerializer serializer1 = makeObjectSerializer_ExtMapBean();
    ObjectSerializer serializer2 = makeObjectSerializer_ExtMapBean();
    ExtMapBean original = makeSample_ExtMapBean();
    File tempFile = File.createTempFile("test", ".xml");
    tempFile.deleteOnExit();
    StringWriter writer = new StringWriter();

    // Serialize twice
    serializer1.serialize(writer, original);
    serializer2.serialize(tempFile.getPath() /* a String */, original);
    // Compare
    performXmlAssertion(new StringReader(writer.toString()), tempFile);
  }

  /**
   * <p>Tests whether the serializer correctly throws a
   * {@link java.io.FileNotFoundException} when attempting to read
   * from a file that does not exist.</p>
   * @throws Exception if an unexpected or unhandled problem arises.
   */
  public void testThrowsFileNotFoundException()
      throws Exception {
    // Set up needed objects
    ObjectSerializer serializer = makeObjectSerializer_ExtMapBean();
    ObjectSerializer deserializer = makeObjectSerializer_ExtMapBean();
    ExtMapBean original = makeSample_ExtMapBean();
    ExtMapBean clone;
    File tempFile = File.createTempFile("test", ".xml");
    tempFile.deleteOnExit();

    try {
      serializer.serialize(tempFile, original);
      tempFile.delete();
      clone = (ExtMapBean)deserializer.deserialize(tempFile);
      failGeneric("testThrowsFileNotFoundException",
                  "FileNotFoundException not thrown on read.");
    }
    catch (FileNotFoundException e) {
      // succeed
    }
  }

  public void testThrowsNullPointerException()
      throws Exception {
    ObjectSerializer serializer = makeObjectSerializer_ExtMapBean();
    File tempFile = File.createTempFile("test", ".xml");
    tempFile.deleteOnExit();

    try {
      serializer.serialize(tempFile, (Serializable)null);
      failGeneric("testThrowsNullArgumentException",
                  "NullArgumentException not thrown: (Serializable)null");
    }
    catch (NullPointerException npe) {
      // succeed
    }

    try {
      serializer.serialize(tempFile, (LockssSerializable)null);
      failGeneric("testThrowsNullArgumentException",
                  "NullArgumentException not thrown: (LockssSerializable)null");
    }
    catch (NullPointerException npe) {
      // succeed
    }
  }

  /**
   * <p>Produces an ObjectSerializer instance to conduct tests based
   * on objects of type {@link org.lockss.util.ExtMapBean}.</p>
   * @return A newly instantiated instance of the ObjectSerializer
   *         subclass under test, configured properly to operate on
   *         objects of type {@link org.lockss.util.ExtMapBean}.
   */
  protected abstract ObjectSerializer makeObjectSerializer_ExtMapBean();

  /**
   * <p>Builds a sample {@link org.lockss.util.ExtMapBean}
   * ready to marshal in tests.</p>
   * <p>If subclasses override this method, they must make sure to
   * create an ExtMapBean full of many data types to maximize the
   * difficulty of the test.</p>
   * @return A newly allocated
   *         {@link org.lockss.util.ExtMapBean} filled with
   *         many interesting keys and values.
   */
  protected ExtMapBean makeSample_ExtMapBean()
      throws Exception {
    return new ExtMapBean(makeSample_TypedEntryMap().m_map);
  }

  /**
   * <p>Builds a sample {@link org.lockss.util.TypedEntryMap}
   * ready to marshal in tests.</p>
   * <p>If subclasses override this method, they must make sure to
   * create a TypedEntryMap full of many data types to maximize the
   * difficulty of the test.</p>
   * @return A newly allocated
   *         {@link org.lockss.util.TypedEntryMap} filled with
   *         many interesting keys and values.
   */
  protected TypedEntryMap makeSample_TypedEntryMap()
      throws Exception {
    TypedEntryMap tmap = new TypedEntryMap();

    /* Basic data types */
    tmap.putBoolean("boolean.true", true);
    tmap.putBoolean("boolean.false", false);
    tmap.putDouble("double.Math.PI", Math.PI);
    tmap.putDouble("double.Math.E", Math.E);
    tmap.putFloat("float.Float.MAX_VALUE", Float.MAX_VALUE);
    tmap.putFloat("float.0.12345f", 0.12345f);
    tmap.putInt("int.Integer.MIN_VALUE", Integer.MIN_VALUE);
    tmap.putInt("int.Integer.12345", 12345);
    tmap.putLong("long.Long.MAX_VALUE", Long.MAX_VALUE);
    tmap.putLong("long.6507245723L", 6507245723L);
    tmap.putString("string.\"LOCKSS rocks\"", "LOCKSS rocks");
    tmap.putString("string.\"\"", "");

    tmap.putUrl(
        "url.\"http://www.lockss.org/\"",
        new URL("http://www.lockss.org/"));
    tmap.putUrl(
        "url.\"http://www.stanford.edu/\"",
        new URL("http://www.stanford.edu/"));

    /* Collections */
    tmap.putCollection(
        "collection.list.homogeneous",
        ListUtil.list("E", "F", "G", "H"));
    tmap.putCollection(
        "collection.list.heterogeneous",
        ListUtil.list(new Float(1.0), "two", new Long(3)));
    return tmap;
  }

  /**
   * <p>Asserts that two XML documents are alike.</p>
   * @param referenceReader A Reader from which the reference document
   *                        can be input.
   * @param testFile        A File from which the test document can be
   *                        read and tested against the reference.
   * @throws Exception      if an input/output error occurs, or if the
   *                        likeness assertion fails.
   */
  private void performXmlAssertion(Reader referenceReader, File testFile)
      throws Exception {
    Reader testReader = new FileReader(testFile);
    XMLUnit.setIgnoreWhitespace(true);
    assertXMLEqual(referenceReader, testReader);
  }

  /**
   * <p>A general-purpose failure message formatter for
   * {@link java.lang.ClassCastException}s.</p>
   * <p>This method calls {@link #failGeneric} and therefore always
   * throws an {@link junit.framework.AssertionFailedError}.</p>
   * @param methodName  The name of the method throwing the failure.
   * @param targetClass The name of the target class of the cast.
   */
  private static void failClassCastException(String methodName, Class targetClass) {
    StringBuffer buffer = new StringBuffer();
    buffer.append("A ClassCastException was caught when attempting to cast to ");
    buffer.append(targetClass.getName());
    buffer.append(".");
    failGeneric(methodName, buffer.toString());
  }

  /**
   * <p>A general-purpose failure message formatter.</p>
   * <p>This method calls
   * {@link junit.framework.Assert#fail(String)} and therefore always
   * throws a {@link junit.framework.AssertionFailedError}.</p>
   * @param methodName The name of the method throwing the failure.
   * @param message    A message to be attached.
   */
  private static void failGeneric(String methodName, String message) {
    String separator = ": ";
    StringBuffer buffer = new StringBuffer();
    buffer.append(ObjectSerializerTester.class.getName());
    buffer.append(separator);
    buffer.append(methodName);
    buffer.append(separator);
    buffer.append(message);
    fail(buffer.toString());
  }

}
