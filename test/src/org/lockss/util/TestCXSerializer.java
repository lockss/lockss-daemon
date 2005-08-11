/*
 * $Id: TestCXSerializer.java,v 1.2 2005-08-11 17:04:37 thib_gc Exp $
 */

/*

Copyright (c) 2002-2005 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;

import org.lockss.util.ObjectSerializer.SerializationException;

public class TestCXSerializer extends ObjectSerializerTest {

  /**
   * <p>Tests whether a CXSerializer in Castor mode successfully
   * writes output in Castor format.</p>
   * @throws Exception if an unexpected or unhandled problem arises.
   */
  public void testCastorMode()
      throws Exception {
    CXSerializer serializer = makeExtMapBeanSerializer();
    CastorSerializer deserializer =
      new CastorSerializer(null, ExternalizableMap.MAPPING_FILE_NAME, ExtMapBean.class);
    serializer.setCurrentMode(CXSerializer.CASTOR_MODE);
    performRoundTrip(serializer, deserializer);
  }

  /**
   * <p>Tests whether a CXSerializer successfully reads input in
   * Castor format.</p>
   * @throws Exception if an unexpected or unhandled problem arises.
   */
  public void testReadingCastor()
      throws Exception {
    CastorSerializer serializer =
      new CastorSerializer(null, ExternalizableMap.MAPPING_FILE_NAME, ExtMapBean.class);
    CXSerializer deserializer = makeExtMapBeanSerializer();
    performRoundTrip(serializer, deserializer);
  }

  /**
   * <p>Tests whether a CXSerializer successfully reads input in
   * XStream format.</p>
   * @throws Exception if an unexpected or unhandled problem arises.
   */
  public void testReadingXStream()
      throws Exception {
    XStreamSerializer serializer = new XStreamSerializer(null);
    CXSerializer deserializer = makeExtMapBeanSerializer();
    performRoundTrip(serializer, deserializer);
  }
  
  /**
   * <p>Tests whether a CXSerializer in XStream mode successfully
   * writes output in XStream format.</p>
   * @throws Exception if an unexpected or unhandled problem arises.
   */
  public void testXStreamMode()
      throws Exception {
    CXSerializer serializer = makeExtMapBeanSerializer();
    XStreamSerializer deserializer = new XStreamSerializer(null);
    serializer.setCurrentMode(CXSerializer.XSTREAM_MODE);
    performRoundTrip(serializer, deserializer);
  }
  
  /**
   * <p>Tests whether a CXSerializer in XStream Overwrite mode
   * replaces a file read in Castor format by a file in XStream
   * format.</p>
   * @throws Exception if an unexpected or unhandled problem arises.
   */
  public void testXStreamOverwriteMode()
      throws Exception {
    // Set up needed objects
    CastorSerializer serializer =
      new CastorSerializer(null, ExternalizableMap.MAPPING_FILE_NAME, ExtMapBean.class);
    CXSerializer deserializer1 = makeExtMapBeanSerializer();
    deserializer1.setCurrentMode(CXSerializer.XSTREAM_OVERWRITE_MODE);
    XStreamSerializer deserializer2 = new XStreamSerializer();
    File tempFile = File.createTempFile("test", ".xml");
    tempFile.deleteOnExit();
    ExtMapBean original = makeSample_ExtMapBean();
    ExtMapBean clone1;
    ExtMapBean clone2;
    
    // Test
    serializer.serialize(tempFile, original);
    clone1 = (ExtMapBean)deserializer1.deserialize(tempFile);
    clone2 = (ExtMapBean)deserializer2.deserialize(tempFile);
    assertEquals(clone1.getMap(), clone2.getMap());
  }
  
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
    return new CXSerializer(
        null, ExternalizableMap.MAPPING_FILE_NAME, ExtMapBean.class);
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
    StringWriter writer = new StringWriter();
    StringReader reader;
    
    try {
      serializer.serialize(writer, original);
      reader = new StringReader(writer.toString());
      clone = (ExtMapBean)deserializer.deserialize(reader);
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
