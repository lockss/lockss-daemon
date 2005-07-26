/*
 * $Id: ObjectSerializer.java,v 1.3 2005-07-26 17:28:37 thib_gc Exp $
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

import java.io.*;

import org.lockss.app.LockssApp;

/**
 * <p>Specifies an interface for serializers that marshal Java objects
 * to XML.</p>
 * <p>Unit tests for classes that extend this class must themselves
 * extend the abstract unit test for this class,
 * <code>org.lockss.util.ObjectSerializerTest</code>.</p>
 * @author Thib Guicherd-Callin
 */
public abstract class ObjectSerializer {

  /*
   * begin PUBLIC STATIC INNER CLASS
   * ===============================
   */
  
  /**
   * <p>Denotes serious marshalling/unmarshalling error conditions
   * caused by failures of the underlying serialization
   * mechanism.</p>
   * <p>Other well-known exception subclasses, such as
   * {@link FileNotFoundException} or {@link IOException}, are thrown
   * if they arise, as they better describe the problem they flag than
   * this class. This exception is intended for unrecoverable internal
   * error conditions that client code cannot easily have control over
   * but should still reasonably expect.</p>
   * @author Thib Guicherd-Callin
   * @see Exception
   */
  public static class SerializationException extends Exception {
    
    /*
     * IMPLEMENTATION NOTES
     * 
     * I had to revert from a nice 1.4-like implementation with
     * four constructors to the code below because 1.3 was still
     * around on the platform at the time, and Java 1.3's Exception
     * constructor only accepts nothing or a String. As soon as Java
     * 1.3 is not around anymore, please retrofit this code to be
     * four simple constructors with or without a String, with or
     * without a Throwable, that invoke the corresponding
     * superconstructor. (A previously checked-in version of this
     * file is that way already.)
     */
    
    public SerializationException() { this(null, null); }
    public SerializationException(String message) { this(message, null); }
    public SerializationException(Throwable cause) { this(null, cause); }
    public SerializationException(String message, Throwable cause) {
      super(format(message, cause));
    }
    
    private static String format(String message, Throwable cause) {
      StringBuffer buffer = new StringBuffer();
      if (   message != null 
          && message.length() > 0) {
        buffer.append(message);
      }
      if (   message != null 
          && message.length() > 0
          && cause != null
          && cause.getMessage() != null
          && cause.getMessage().length() > 0) {
        buffer.append(" Nested message: ");
      }
      if (cause != null
          && cause.getMessage() != null
          && cause.getMessage().length() > 0) {
        buffer.append(cause.getMessage());
      }
      return buffer.toString();
    }
  }
  
  /*
   * end PUBLIC STATIC INNER CLASS
   * =============================
   */
  
  /**
   * <p>Saved reference to a serialization context object.</p>
   */
  private LockssApp lockssContext;
  
  /**
   * <p>Builds a new ObjectSerializer instance.</p>
   * @param lockssContext A serialization context object.
   */
  public ObjectSerializer(LockssApp lockssContext) {
    this.lockssContext = lockssContext;
  }
  
  /**
   * <p>Convenience method to unmarshal a Java object from an XML file
   * that accepts a File instead of a Reader.</p>
   * <p>The result of deserializing an object with a file must be the
   * same as deserializing it with a {@link Reader} on the
   * same file, in the sense of the {@link Object#equals} method.
   * @param inputFile A File instance representing the XML file where
   *                  the object is serialized.
   * @return An Object reference whose field were populated from the
   *         data found in the XML file.
   * @throws FileNotFoundException  if the given file is invalid.
   * @throws IOException            if input or output fails.
   * @throws SerializationException if an internal serialization error
   *                                occurs.
   * @see #deserialize(InputStream)
   */
  public Object deserialize(File inputFile)
      throws FileNotFoundException, IOException, SerializationException {
    FileInputStream inStream = new FileInputStream(inputFile);
    try     { return deserialize(inStream); }
    finally { IOUtil.safeClose(inStream); }
  }

  /**
   * <p>Convenience method to unmarshal a Java object from an XML file
   * that accepts an InputStream instead of a Reader.</p>
   * <p>The result of deserializing an object with a stream must be
   * the same as deserializing it with a {@link Reader} on the
   * same file, in the sense of the {@link Object#equals} method.
   * @param inputStream An input stream instance from which the
   *                    serialized object is to be read.
   * @return An Object reference whose field were populated from the
   *         data found in the XML file.
   * @throws IOException            if input or output fails.
   * @throws SerializationException if an internal serialization error
   *                                occurs.
   * @see #deserialize(Reader)
   */
  public Object deserialize(InputStream inputStream)
      throws IOException, SerializationException {
    BufferedReader reader =
      new BufferedReader(
          new InputStreamReader(inputStream, Constants.DEFAULT_ENCODING));
    return deserialize(reader);
  }
  
  /**
   * <p>Unmarshals a Java object from an XML file through the given
   * Reader argument.</p>
   * @param reader A Reader instance ready to read the XML file where
   *               the object is serialized.
   * @return An Object reference whose field were populated from the
   *         data found in the XML file.
   * @throws IOException            if input or output fails.
   * @throws SerializationException if an internal serialization error
   *                                occurs.
   */
  public abstract Object deserialize(Reader reader)
      throws IOException, SerializationException;

  /**
   * <p>Convenience method to unmarshal a Java object from an XML file
   * that accepts a filename instead of a Reader.</p>
   * <p>The result of deserializing an object with a filename must be
   * the same as deserializing it with a {@link java.io.Reader} on the
   * same filename, in the sense of the {@link Object#equals}
   * method.
   * @param inputFilename A filename representing the XML file where
   *                      the object is serialized.
   * @return An Object reference whose field were populated from the
   *         data found in the XML file.
   * @throws FileNotFoundException  if the given filename is invalid.
   * @throws IOException            if input or output fails.
   * @throws SerializationException if an internal serialization error
   *                                occurs.
   * @see #deserialize(File)
   */
  public Object deserialize(String inputFilename)
      throws IOException, SerializationException {
    File inputFile = new File(inputFilename);
    return deserialize(inputFile);   
  }

  /**
   * <p>Convenience method to marshal a Java object to an XML file
   * that accepts a File instead of a Writer.</p>
   * <p>The result of serializing an object with a file must be the
   * same as serializing it with a {@link java.io.Writer}</p> on the
   * same file, in the sense of deserialization.</p>
   * @param outputStream An output stream instance into which the
   *                     object is being serialized.
   * @param obj          An object to be serialized.
   * @throws IOException            if input or output fails.
   * @throws SerializationException if an internal serialization error
   *                                occurs.
   * @see #serialize(OutputStream, Object)
   */
  public void serialize(OutputStream outputStream, Object obj)
      throws IOException, SerializationException {
    BufferedWriter writer =
      new BufferedWriter(
          new OutputStreamWriter(outputStream, Constants.DEFAULT_ENCODING));
    serialize(writer, obj);
  }
  
  /**
   * <p>Convenience method to marshal a Java object to an XML file
   * that accepts a File instead of a Writer.</p>
   * <p>The result of serializing an object with a file must be the
   * same as serializing it with a {@link java.io.Writer}</p> on the
   * same file, in the sense of deserialization.</p>
   * @param outputFile A File instance representing the file into
   *                   which the object is being serialized.
   * @param obj    An object to be serialized.
   * @throws FileNotFoundException  if the given file is invalid.
   * @throws IOException            if input or output fails.
   * @throws SerializationException if an internal serialization error
   *                                occurs.
   * @see #serialize(Writer, Object)
   */
  public void serialize(File outputFile, Object obj)
      throws FileNotFoundException, IOException, SerializationException {
    File tempFile = File.createTempFile("tmp", ".xml", outputFile.getParentFile());
    FileOutputStream outStream = new FileOutputStream(tempFile);
    boolean success = false;

    try {
      serialize(outStream, obj);
      success = true;
    }
    finally {
      IOUtil.safeClose(outStream);
      if (success) {
        /* Serialization succeeded */
        success = tempFile.renameTo(outputFile);
        if (success) {
          // File renaming succeeded
          tempFile.deleteOnExit();
        }
        else {
          // File renaming failed
          throw new IOException("Could not create a final file from a temporary file.");
        }
      }
    }
  }

  /**
   * <p>Convenience method to marshal a Java object to an XML file
   * that accepts a filename instead of a Writer.</p>
   * <p>The result of serializing an object with a filename must be
   * the same as serializing it with a {@link java.io.Writer}</p> on
   * the same filename, in the sense of deserialization.</p>
   * @param outputFilename A filename representing the file into which
   *                       the object is being serialized.
   * @param obj            An object to be serialized.
   * @throws FileNotFoundException  if the given file is invalid.
   * @throws IOException            if input or output fails.
   * @throws SerializationException if an internal serialization error
   *                                occurs.
   * @see #serialize(File, Object)
   */
  public void serialize(String outputFilename, Object obj)
      throws FileNotFoundException, IOException, SerializationException {
    File outputFile = new File(outputFilename);
    serialize(outputFile, obj);
  }
  
  /**
   * <p>Marshals a Java object to an XML file through the given
   * Writer argument.</p>
   * @param writer A Writer instance ready to write to the XML file
   *               into which the object is being serialized.
   * @param obj    An object to be serialized.
   * @throws IOException            if input or output fails.
   * @throws SerializationException if an internal serialization error
   *                                occurs.
   */
  public abstract void serialize(Writer writer, Object obj)
      throws IOException, SerializationException;

  /**
   * <p>Retrieves the serialization context.</p>
   * @return The {@link org.lockss.app.LockssApp} context.
   */
  protected LockssApp getLockssContext() {
    return lockssContext;
  }
  
}
