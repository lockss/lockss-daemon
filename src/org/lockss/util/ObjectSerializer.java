/*
 * $Id: ObjectSerializer.java,v 1.24 2006-05-31 17:54:49 thib_gc Exp $
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
import org.lockss.config.*;
import org.lockss.util.SerializationException;

/**
 * <p>Specifies an interface for serializers that marshal Java objects
 * to XML.</p>
 * <p>By convention, subclasses throw either
 * {@link SerializationException} or one of a set of distinguished
 * exception classes. Currently these are:</p>
 * <ul>
 *  <li>{@link InterruptedIOException}</li>
 * </ul>
 * <p>Unit tests for classes that extend this class must themselves
 * extend the abstract unit test for this class,
 * <code>org.lockss.util.ObjectSerializerTester</code>.</p>
 * @author Thib Guicherd-Callin
 * @see SerializationException
 */
public abstract class ObjectSerializer {

  protected interface TempFileFactory {

    /**
     * <p>Creates a temporary file.</p>
     * @param prefix    A file name prefix.
     * @param suffix    A file name suffix.
     * @param directory A parent directory.
     * @return          A new temporary file.
     * @throws IOException as thrown by {@link File#createTempFile(String, String, File)}.
     * @see File#createTempFile(String, String, File)
     */
    File createTempFile(String prefix,
                        String suffix,
                        File directory)
        throws IOException;

  }

  /**
   * <p>Saved reference to a serialization context object.</p>
   */
  protected LockssApp lockssContext;

  /**
   * <p>A temporary file factory.</p>
   * @see ObjectSerializer.TempFileFactory
   */
  protected TempFileFactory tempFileFactory;

  /**
   * <p>Builds a new {@link ObjectSerializer} instance with the
   * current default failed serialization and deserialization modes.
   * @param lockssContext A serialization context object.
   * @see #PARAM_FAILED_DESERIALIZATION_MODE
   * @see #PARAM_FAILED_SERIALIZATION_MODE
   */
  public ObjectSerializer(LockssApp lockssContext) {
    this(lockssContext,
         CurrentConfig.getBooleanParam(PARAM_FAILED_SERIALIZATION_MODE,
                                       DEFAULT_FAILED_SERIALIZATION_MODE),
         CurrentConfig.getIntParam(PARAM_FAILED_DESERIALIZATION_MODE,
                                   DEFAULT_FAILED_DESERIALIZATION_MODE));
  }

  /**
   * <p>Builds a new {@link ObjectSerializer} instance with the given
   * failed deserialization mode.</p>
   * @param lockssContext             A serialization context object.
   * @param saveTempFiles             A failed serialization mode.
   * @param failedDeserializationMode A failed deserialization mode.
   * @see #PARAM_FAILED_DESERIALIZATION_MODE
   */
  public ObjectSerializer(LockssApp lockssContext,
                          boolean saveTempFiles,
                          int failedDeserializationMode) {
    this.lockssContext = lockssContext;
    setFailedSerializationMode(saveTempFiles);
    setFailedDeserializationMode(failedDeserializationMode);
    this.tempFileFactory = new TempFileFactory() {
      public File createTempFile(String prefix, String suffix, File directory) throws IOException {
        return File.createTempFile(prefix, suffix, directory);
      }
    };
  }

  /**
   * <p>Convenience method to unmarshal a Java object from an XML file
   * that accepts a {@link File} instead of a {@link Reader}.</p>
   * <p>The result of deserializing an object with a file must be the
   * same as deserializing it with a {@link Reader} on the
   * same file.
   * @param inputFile A File instance representing the XML file where
   *                  the object is serialized.
   * @return An {@link Object} reference whose fields were populated
   *         from the data found in the XML file.
   * @throws SerializationException.FileNotFound if the given file is
   *                                             invalid.
   * @throws SerializationException              if input or output
   *                                             fails.
   * @throws InterruptedIOException              if input or output is
   *                                             interrupted.
   * @see #deserialize(InputStream)
   */
  public Object deserialize(File inputFile)
      throws SerializationException.FileNotFound,
             SerializationException,
             InterruptedIOException {
    FileInputStream inputStream = null;
    try {
      inputStream = new FileInputStream(inputFile);
    }
    catch (FileNotFoundException fnfe) {
      String errorString = "File not found: " + inputFile;
      logger.debug2(errorString);
      throw new SerializationException.FileNotFound(errorString, fnfe);
    }

    try {
      return deserialize(inputStream);
    }
    catch (SerializationException se) {
      throw failDeserialize(se, inputFile);
    }
    catch (InterruptedIOException iioe) {
      throw failDeserialize(iioe, inputFile);
    }
    finally {
      IOUtil.safeClose(inputStream);
    }
  }

  /**
   * <p>Convenience method to unmarshal a Java object from an XML file
   * that accepts an {@link InputStream} instead of a
   * {@link Reader}.</p>
   * <p>The result of deserializing an object with a stream must be
   * the same as deserializing it with a {@link Reader} on the
   * same file.
   * @param inputStream An {@link InputStream} instance from which the
   *                    serialized object is to be read.
   * @return An {@link Object} reference whose fields were populated
   *         from the data found in the XML file.
   * @throws SerializationException if input or output fails.
   * @throws InterruptedIOException if input or output is
   *                                interrupted.
   * @see #deserialize(Reader)
   */
  public Object deserialize(InputStream inputStream)
      throws SerializationException,
             InterruptedIOException {
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new InputStreamReader(inputStream,
                                                        Constants.DEFAULT_ENCODING));
    }
    catch (UnsupportedEncodingException shouldnt) {
      logger.error("InputStreamReader did not accept " + Constants.DEFAULT_ENCODING,
                   shouldnt);
      throw new RuntimeException(shouldnt);
    }

    try {
      return deserialize(reader);
    }
    finally {
      IOUtil.safeClose(reader);
    }
  }

  /**
   * <p>Unmarshals a Java object from an XML file through the given
   * {@link Reader} argument.</p>
   * @param reader A {@link Reader} instance ready to read the XML
   *               file where the object is serialized.
   * @return An {@link Object} reference whose fields were populated
   *         from the data found in the XML file.
   * @throws SerializationException   if input or output fails.
   * @throws InterruptedIOException   if input or output is
   *                                  interrupted.
   */
  public abstract Object deserialize(Reader reader)
      throws SerializationException,
             InterruptedIOException;

  /**
   * <p>Convenience method to unmarshal a Java object from an XML file
   * that accepts a filename instead of a {@link Reader}.</p>
   * <p>The result of deserializing an object with a filename must be
   * the same as deserializing it with a {@link Reader} on the same
   * filename.
   * @param inputFilename A filename representing the XML file where
   *                      the object is serialized.
   * @return An {@link Object} reference whose field were populated
   *         from the data found in the XML file.
   * @throws SerializationException.FileNotFound if the given file is
   *                                             invalid.
   * @throws SerializationException              if input or output
   *                                             fails.
   * @throws InterruptedIOException              if input or output is
   *                                             interrupted.
   * @see #deserialize(File)
   */
  public Object deserialize(String inputFilename)
      throws SerializationException.FileNotFound,
             SerializationException,
             InterruptedIOException {
    File inputFile = new File(inputFilename);
    return deserialize(inputFile);
  }

  /**
   * <p>Convenience method to marshal a Java object to an XML file
   * that accepts a {@link File} instead of a {@link Writer}.</p>
   * <p>The result of serializing an object with a file must be the
   * same as serializing it with a {@link Writer} on the same
   * file.</p>
   * @param outputFile A {@link File} instance representing the file
   *                   into which the object is to be serialized.
   * @param obj An object to be serialized.
   * @throws NullPointerException   if obj is null.
   * @throws SerializationException if input or output fails.
   * @throws InterruptedIOException if input or output is
   *                                interrupted.
   * @see #serialize(File, Object)
   */
  public void serialize(File outputFile,
                        LockssSerializable obj)
      throws SerializationException,
             InterruptedIOException {
    throwIfNull(obj);
    serialize(outputFile, (Object)obj);
  }

  /**
   * <p>Convenience method to marshal a Java object to an XML file
   * that accepts a {@link File} instead of a {@link Writer}.</p>
   * <p>The result of serializing an object with a file must be the
   * same as serializing it with a {@link Writer} on the same
   * file.</p>
   * @param outputFile A {@link File} instance representing the file
   *                   into which the object is to be serialized.
   * @param obj An object to be serialized.
   * @throws NullPointerException   if obj is null.
   * @throws SerializationException if input or output fails.
   * @throws InterruptedIOException if input or output is
   *                                interrupted.
   * @see #serialize(File, Object)
   */
  public void serialize(File outputFile,
                        Serializable obj)
      throws SerializationException,
             InterruptedIOException {
    throwIfNull(obj);
    serialize(outputFile, (Object)obj);
  }

  /**
   * <p>Convenience method to marshal a Java object to an XML file
   * that accepts a file name instead of a {@link Writer}.</p>
   * <p>The result of serializing an object with a filename must be
   * the same as serializing it with a {@link Writer} on the same
   * filename.</p>
   * @param outputFilename A file name representing the file into
   *                       which the object is to be serialized.
   * @param obj An object to be serialized.
   * @throws NullPointerException   if obj is null.
   * @throws SerializationException if input or output fails.
   * @throws InterruptedIOException if input or output is
   *                                interrupted.
   * @see #serialize(String, Object)
   */
  public void serialize(String outputFilename,
                        LockssSerializable obj)
      throws SerializationException,
             InterruptedIOException {
    throwIfNull(obj);
    serialize(outputFilename, (Object)obj);
  }

  /**
   * <p>Convenience method to marshal a Java object to an XML file
   * that accepts a file name instead of a {@link Writer}.</p>
   * <p>The result of serializing an object with a filename must be
   * the same as serializing it with a {@link Writer} on the same
   * filename.</p>
   * @param outputFilename A file name representing the file into
   *                       which the object is to be serialized.
   * @param obj An object to be serialized.
   * @throws NullPointerException   if obj is null.
   * @throws SerializationException if input or output fails.
   * @throws InterruptedIOException if input or output is
   *                                interrupted.
   * @see #serialize(String, Object)
   */
  public void serialize(String outputFilename,
                        Serializable obj)
      throws SerializationException,
             InterruptedIOException {
    serialize(outputFilename, (Object)obj);
    throwIfNull(obj);
  }

  /**
   * <p>This serializer's failed serialization mode.</p>
   * @see #PARAM_FAILED_SERIALIZATION_MODE
   * @see #getFailedSerializationMode
   * @see #setFailedSerializationMode
   */
  protected boolean failedSerializationMode;

  /**
   * <p>Returns this instance's failed serialization mode.</p>
   * @return The current failed serialization mode for this
   *         instance.
   * @see #PARAM_FAILED_SERIALIZATION_MODE
   */
  public boolean getFailedSerializationMode() {
    return failedSerializationMode;
  }

  /**
   * <p>Sets the failed serialization mode for this instance.</p>
   * @param saveTempFiles A failed serialization mode.
   * @see #PARAM_FAILED_SERIALIZATION_MODE
   */
  public void setFailedSerializationMode(boolean saveTempFiles) {
    failedSerializationMode = saveTempFiles;
  }

  /**
   * <p>This serializer's current failed deserialization mode.</p>
   * @see #PARAM_FAILED_DESERIALIZATION_MODE
   * @see #getFailedDeserializationMode
   * @see #setFailedDeserializationMode
   */
  protected int failedDeserializationMode;

  /**
   * <p>Returns this instance's failed deserialization mode.</p>
   * @return The current failed deserialization mode for this
   *         instance.
   * @see #PARAM_FAILED_DESERIALIZATION_MODE
   */
  public int getFailedDeserializationMode() {
    return failedDeserializationMode;
  }

  /**
   * <p>Sets the failed deserialization mode for this instance.</p>
   * @param mode A failed deserialization mode.
   * @throws IllegalArgumentException if <code>mode</code> is not a
   *                                  valid mode.
   * @see #PARAM_FAILED_DESERIALIZATION_MODE
   */
  public void setFailedDeserializationMode(int mode) {
    switch (mode) {
      case FAILED_DESERIALIZATION_IGNORE:
      case FAILED_DESERIALIZATION_RENAME:
      case FAILED_DESERIALIZATION_COPY:
        this.failedDeserializationMode = mode;
        break;
      default:
        String errorString = "Attempt to set failed deserialization mode to " + mode;
        logger.error(errorString);
        throw new IllegalArgumentException(errorString);
    }
  }

  protected void maybeDelTempFile(File file) {
    if (!getFailedSerializationMode()) {
      logger.warning("Deleting unsuccessful serialization file " + file);
      file.delete();
    }
  }

  /**
   * <p>Retrieves the serialization context.</p>
   * @return The {@link org.lockss.app.LockssApp} context.
   */
  protected LockssApp getLockssContext() {
    return lockssContext;
  }

  /**
   * <p>Convenience method to marshal a Java object to an XML file
   * that accepts a {@link File} instead of a {@link Writer}.</p>
   * <p>The result of serializing an object with a file must be the
   * same as serializing it with a {@link Writer} on the same
   * file.</p>
   * @param outputFile A {@link File} instance representing the file
   *                   into which the object is to be serialized.
   * @param obj An object to be serialized.
   * @throws NullPointerException   if obj is null.
   * @throws SerializationException if input or output fails.
   * @throws InterruptedIOException if input or output is
   *                                interrupted.
   * @see #serialize(File, Object)
   */
  protected void serialize(File outputFile,
                           Object obj)
      throws SerializationException,
             InterruptedIOException {
    // Bail if null
    throwIfNull(obj);

    // Create temporary file
    File temporaryFile = null;
    try {
      temporaryFile = tempFileFactory.createTempFile(outputFile.getName(),
                                                     CurrentConfig.getParam(PARAM_TEMPFILE_SERIALIZATION_EXTENSION,
                                                                            DEFAULT_TEMPFILE_SERIALIZATION_EXTENSION),
                                                     outputFile.getParentFile());
    }
    catch (IOException ioe) {
      throw failSerialize("IOException while setting up temporary serialization file",
                          ioe,
                          new SerializationException(ioe));
    }

    // Create stream
    FileOutputStream outputStream = null;
    try {
      outputStream = new FileOutputStream(temporaryFile);
    }
    catch (IOException ioe) {
      throw failSerialize("IOException while setting up serialization stream",
                          ioe,
                          new SerializationException(ioe),
                          temporaryFile);
    }
    catch (RuntimeException re) {
      maybeDelTempFile(temporaryFile);
      throw re;
    }

    // Serialize object
    try {
      serialize(outputStream, obj);
    }
    catch (InterruptedIOException iioe) {
      IOUtil.safeClose(outputStream);
      maybeDelTempFile(temporaryFile);
      throwIfInterrupted(iioe);
    }
    catch (SerializationException se) {
      IOUtil.safeClose(outputStream);
      throw failSerialize("Failed to serialize an object of type " + obj.getClass().getName(),
                          se,
                          new SerializationException(se),
                          temporaryFile);
    }
    catch (RuntimeException re) {
      IOUtil.safeClose(outputStream);
      maybeDelTempFile(temporaryFile);
      throw re;
    }

    // Close stream
    try {
      outputStream.close();
    }
    catch (IOException ioe) {
      throw failSerialize("Could not close " + temporaryFile,
                          ioe,
                          new SerializationException.CloseFailed(ioe),
                          temporaryFile);
    }
    catch (RuntimeException re) {
      maybeDelTempFile(temporaryFile);
      throw re;
    }

    // Rename temporary file
    if (!temporaryFile.renameTo(outputFile)) {
      String errorString = "Could not rename from " + temporaryFile + " to " + outputFile;
      throw failSerialize(errorString,
                          null,
                          new SerializationException.RenameFailed(errorString),
                          temporaryFile);
    }
  }

  /**
   * <p>Convenience method to marshal a Java object to an XML file
   * that accepts an {@link OutputStream} instead of a
   * {@link Writer}.</p>
   * <p>The result of serializing an object with a file must be the
   * same as serializing it with a {@link Writer} on the same
   * file.</p>
   * @param outputStream An {@link OutputStream} instance into which
   *                     the object is to be serialized.
   * @param obj An object to be serialized.
   * @throws NullPointerException   if obj is null.
   * @throws SerializationException if input or output fails.
   * @throws InterruptedIOException if input or output is
   *                                interrupted.
   * @see #serialize(OutputStream, Object)
   */
  protected void serialize(OutputStream outputStream,
                           LockssSerializable obj)
      throws SerializationException,
             InterruptedIOException {
    throwIfNull(obj);
    serialize(outputStream, (Object)obj);
  }

  /**
   * <p>Convenience method to marshal a Java object to an XML file
   * that accepts an {@link OutputStream} instead of a
   * {@link Writer}.</p>
   * <p>The result of serializing an object with a file must be the
   * same as serializing it with a {@link Writer} on the same
   * file.</p>
   * @param outputStream An {@link OutputStream} instance into which
   *                     the object is to be serialized.
   * @param obj An object to be serialized.
   * @throws NullPointerException   if obj is null.
   * @throws SerializationException if input or output fails.
   * @throws InterruptedIOException if input or output is
   *                                interrupted.
   * @see #serialize(OutputStream, Object)
   */
  protected void serialize(OutputStream outputStream,
                           Object obj)
      throws SerializationException,
             InterruptedIOException {
    throwIfNull(obj);

    BufferedWriter writer = null;
    try {
      writer = new BufferedWriter(new OutputStreamWriter(outputStream,
                                                         Constants.DEFAULT_ENCODING));
    }
    catch (UnsupportedEncodingException shouldnt) {
      logger.error("OutputStreamWriter did not accept " + Constants.DEFAULT_ENCODING,
                   shouldnt);
      throw new RuntimeException(shouldnt);
    }

    try {
      serialize(writer, obj);
    }
    finally {
      IOUtil.safeClose(writer);
    }
  }

  /**
   * <p>Convenience method to marshal a Java object to an XML file
   * that accepts an {@link OutputStream} instead of a
   * {@link Writer}.</p>
   * <p>The result of serializing an object with a file must be the
   * same as serializing it with a {@link Writer} on the same
   * file.</p>
   * @param outputStream An {@link OutputStream} instance into which
   *                     the object is to be serialized.
   * @param obj An object to be serialized.
   * @throws NullPointerException   if obj is null.
   * @throws SerializationException if input or output fails.
   * @throws InterruptedIOException if input or output is
   *                                interrupted.
   * @see #serialize(OutputStream, Object)
   */
  protected void serialize(OutputStream outputStream,
                           Serializable obj)
      throws SerializationException,
             InterruptedIOException {
    throwIfNull(obj);
    serialize(outputStream, (Object)obj);
  }

  /**
   * <p>Convenience method to marshal a Java object to an XML file
   * that accepts a file name instead of a {@link Writer}.</p>
   * <p>The result of serializing an object with a filename must be
   * the same as serializing it with a {@link Writer} on the same
   * file name.</p>
   * @param outputFilename A file name representing the file into
   *                       which the object is to be serialized.
   * @param obj An object to be serialized.
   * @throws NullPointerException   if obj is null.
   * @throws SerializationException if input or output fails.
   * @throws InterruptedIOException if input or output is
   *                                interrupted.
   * @see #serialize(File, Object)
   */
  protected void serialize(String outputFilename,
                           Object obj)
      throws SerializationException,
             InterruptedIOException {
    File outputFile = new File(outputFilename);
    throwIfNull(obj);
    serialize(outputFile, obj);
  }

  /**
   * <p>Marshals a Java object to an XML file through the given
   * {@link Writer} argument.</p>
   * <p>This contract mandates that the entire object graph be
   * {@link Serializable} or {@link LockssSerializable} but it is
   * not always easy to enforce. If the object graph is not fully
   * serializable, a
   * {@link SerializationException.NotSerializableOrLockssSerializable}
   * exception should be thrown by subclasses.</p>
   * @param writer A {@link Writer} instance ready to write to the XML
   *               file into which the object is to be serialized.
   * @param obj    An object to be serialized.
   * @throws NullPointerException   if obj is null.
   * @throws SerializationException if input or output fails.
   * @throws InterruptedIOException if input or output is
   *                                interrupted.
   * @see #serialize(Writer, Object)
   */
  protected void serialize(Writer writer,
                           LockssSerializable obj)
      throws SerializationException,
             InterruptedIOException {
    throwIfNull(obj);
    serialize(writer, (Object)obj);
  }

  /**
   * <p>Marshals a Java object to an XML file through the given
   * {@link Writer} argument.</p>
   * <p>This contract mandates that the entire object graph be
   * {@link Serializable} or {@link LockssSerializable} but it is
   * not always easy to enforce. If the object graph is not fully
   * serializable, a
   * {@link SerializationException.NotSerializableOrLockssSerializable}
   * exception should be thrown by subclasses.</p>
   * @param writer A {@link Writer} instance ready to write to the XML
   *               file into which the object is to be serialized.
   * @param obj    An object to be serialized.
   * @throws NullPointerException   if obj is null.
   * @throws SerializationException if input or output fails.
   * @throws InterruptedIOException if input or output is
   *                                interrupted.
   * @see #serialize(Writer, Object)
   */
  protected abstract void serialize(Writer writer,
                                    Object obj)
      throws SerializationException,
             InterruptedIOException;

  /**
   * <p>Marshals a Java object to an XML file through the given
   * {@link Writer} argument.</p>
   * <p>This contract mandates that the entire object graph be
   * {@link Serializable} or {@link LockssSerializable} but it is
   * not always easy to enforce. If the object graph is not fully
   * serializable, a
   * {@link SerializationException.NotSerializableOrLockssSerializable}
   * exception should be thrown by subclasses.</p>
   * @param writer A {@link Writer} instance ready to write to the XML
   *               file into which the object is to be serialized.
   * @param obj    An object to be serialized.
   * @throws NullPointerException   if obj is null.
   * @throws SerializationException if input or output fails.
   * @throws InterruptedIOException if input or output is
   *                                interrupted.
   * @see #serialize(Writer, Object)
   */
  protected void serialize(Writer writer,
                           Serializable obj)
      throws SerializationException,
             InterruptedIOException {
    throwIfNull(obj);
    serialize(writer, (Object)obj);
  }

  /**
   * <p>A configuration prefix for serialization classes.</p>
   */
  public static final String PREFIX = Configuration.PREFIX + "serialization.";

  /**
   * <p>This mode controls what happens when a file being deserialized
   * causes the deserialization to fail. It does not apply to
   * deserialization at the {@link InputStream} or {@link Reader}
   * level.</p>
   * <p>The possible values are:</p>
   * <dl>
   *  <dt>{@link #FAILED_DESERIALIZATION_IGNORE}</dt>
   *  <dd>Ignore, that is do not do anything specific to remember
   *  the current state other than logging and throwing an
   *  exception. (Not recommended.)</dd>
   *  <dt>{@link #FAILED_DESERIALIZATION_RENAME}</dt>
   *  <dd>Rename the faulty input file (in the same directory).</dd>
   *  <dt>{@link #FAILED_DESERIALIZATION_COPY}</dt>
   *  <dd>Copy the faulty input file (int the same directory).</dd>
   * </dl>
   * @see #FAILED_DESERIALIZATION_COPY
   * @see #FAILED_DESERIALIZATION_IGNORE
   * @see #FAILED_DESERIALIZATION_RENAME
   * @see #getFailedDeserializationMode
   * @see #setFailedDeserializationMode
   */
  public static final String PARAM_FAILED_DESERIALIZATION_MODE = PREFIX + "failedDeserializationMode";

  /**
   * <p>A value for {@link #PARAM_FAILED_DESERIALIZATION_MODE}.</p>
   * @see #PARAM_FAILED_DESERIALIZATION_MODE
   */
  public static final int FAILED_DESERIALIZATION_COPY = 1;

  /**
   * <p>A value for {@link #PARAM_FAILED_DESERIALIZATION_MODE}.</p>
   * @see #PARAM_FAILED_DESERIALIZATION_MODE
   */
  public static final int FAILED_DESERIALIZATION_IGNORE = 2;

  /**
   * <p>A value for {@link #PARAM_FAILED_DESERIALIZATION_MODE}.</p>
   * @see #PARAM_FAILED_DESERIALIZATION_MODE
   */
  public static final int FAILED_DESERIALIZATION_RENAME = 3;

  /**
   * <p>The default value for
   * {@link #PARAM_FAILED_DESERIALIZATION_MODE}.</p>
   * @see #PARAM_FAILED_DESERIALIZATION_MODE
   */
  public static final int DEFAULT_FAILED_DESERIALIZATION_MODE = FAILED_DESERIALIZATION_RENAME;

  /**
   * <p>The extension appended to files being renamed when the failed
   * deserialization mode is
   * {@link #FAILED_DESERIALIZATION_RENAME}.</p>
   * @see #PARAM_FAILED_DESERIALIZATION_MODE
   */
  public static final String PARAM_FAILED_DESERIALIZATION_EXTENSION = PREFIX + "failedDeserializationExtension";

  /**
   * <p>The default value for
   * {@link #PARAM_FAILED_DESERIALIZATION_EXTENSION}.</p>
   * @see #PARAM_FAILED_DESERIALIZATION_EXTENSION
   */
  public static final String DEFAULT_FAILED_DESERIALIZATION_EXTENSION = ".deser.old";

  /**
   * <p>The extension appended to files being renamed when
   * {@link #PARAM_FAILED_SERIALIZATION_MODE} is true.</p>
   * @see #PARAM_FAILED_SERIALIZATION_MODE
   */
  public static final String PARAM_TEMPFILE_SERIALIZATION_EXTENSION = PREFIX + "failedSerializationExtension";

  /**
   * <p>The default value for
   * {@link #PARAM_TEMPFILE_SERIALIZATION_EXTENSION}.</p>
   * @see #PARAM_TEMPFILE_SERIALIZATION_EXTENSION
   */
  public static final String DEFAULT_TEMPFILE_SERIALIZATION_EXTENSION = ".ser.tmp";

  /**
   * <p>Set true to keep temporary serialization files that either
   * are not successfully written or cannot be renamed. Normally they
   * are deleted.</p>
   */
  public static final String PARAM_FAILED_SERIALIZATION_MODE = PREFIX + "saveFailedTempSerializationFiles";

  /**
   * <p>The default value for {@link #PARAM_FAILED_SERIALIZATION_MODE}.</p>
   */
  public static final boolean DEFAULT_FAILED_SERIALIZATION_MODE = false;

  /**
   * <p>A logger for use by this serializer.</p>
   */
  protected static Logger logger = Logger.getLogger("ObjectSerializer");

  /**
   * <p>An exception message formatter used when deserialization
   * fails.</p>
   * @param cause The exception thrown.
   */
  protected SerializationException failDeserialize(Exception cause)
      throws InterruptedIOException {
    // Throw InterruptedIOException if cause is InterruptedIOException
    throwIfInterrupted(cause);

    StringBuffer buffer = new StringBuffer();
    buffer.append("Failed to deserialize an object (");
    buffer.append(cause.getClass().getName());
    buffer.append(")");
    String str = buffer.toString();
    logger.debug(str, cause);

    return new SerializationException(str, cause);
  }

  /**
   * <p>An exception message formatter used when deserialization
   * fails and the original file is known.</p>
   * @param exc  The exception thrown.
   * @param file The file that was being read.
   */
  protected SerializationException failDeserialize(Exception exc,
                                                   File file)
      throws InterruptedIOException {
    // Throw InterruptedIOException if cause is InterruptedIOException
    throwIfInterrupted(exc);

    StringBuffer buffer = new StringBuffer();
    buffer.append("Failed to deserialize an object from ");
    buffer.append(file);
    buffer.append(" (");
    buffer.append(exc.getClass().getName());
    buffer.append("); the file ");

    // Take action
    switch (getFailedDeserializationMode()) {

      // Ignore
      case FAILED_DESERIALIZATION_IGNORE:
        buffer.append(" was left alone");
        break;

      // Rename
      case FAILED_DESERIALIZATION_RENAME:
        String renamed = file + CurrentConfig.getParam(PARAM_FAILED_DESERIALIZATION_EXTENSION,
                                                       DEFAULT_FAILED_DESERIALIZATION_EXTENSION);
        boolean success = file.renameTo(new File(renamed));
        if (success) {
          // Rename succeeded
          buffer.append(" was renamed ");
          buffer.append(renamed);
        }
        else {
          // Rename failed
          logger.error("Failed to rename from " + file + " to " + renamed);
          buffer.append(" could not be renamed ");
          buffer.append(renamed);
          return new SerializationException.RenameFailed(buffer.toString(), exc);
        }
        break;

      // Copy
      case FAILED_DESERIALIZATION_COPY:
        String copied = file
                        + CurrentConfig.getParam(PARAM_FAILED_DESERIALIZATION_EXTENSION,
                                                 DEFAULT_FAILED_DESERIALIZATION_EXTENSION)
                        + "."
                        + Long.toString(System.currentTimeMillis());
        try {
          InputStream inputStream = new FileInputStream(file);
          OutputStream outputStream = new FileOutputStream(copied);
          StreamUtil.copy(inputStream, outputStream);
          IOUtil.safeClose(inputStream);
          outputStream.close();
          // Copy succeeded
          buffer.append(" was copied to ");
          buffer.append(copied);
        }
        catch (IOException ioe) {
          // Copy failed
          logger.error("Failed to copy from " + file + " to " + copied);
          buffer.append(" could not be copied to ");
          buffer.append(copied);
          return new SerializationException.CopyFailed(buffer.toString(), exc);
        }
        break;

      // Safety net
      default:
        logger.error("Invalid failed deserialization mode: " + getFailedDeserializationMode());
        buffer.append(" invalid failed deserialization mode: " + getFailedDeserializationMode());
        break;
    }

    // Log and return a new SerializationException
    String str = buffer.toString();
    logger.debug(str, exc);
    return new SerializationException(str, exc);
  }

  protected SerializationException failSerialize(String errorString,
                                                 Exception cause,
                                                 SerializationException consequence,
                                                 File temporaryFile)
      throws InterruptedIOException {
    maybeDelTempFile(temporaryFile);
    return failSerialize(errorString, cause, consequence);
  }

  protected SerializationException failSerialize(String errorString,
                                                 Exception cause,
                                                 SerializationException consequence)
      throws InterruptedIOException {
    throwIfInterrupted(cause);
    logger.error(errorString, cause);
    return consequence;
  }

  /**
   * <p>Throws an {@link InterruptedIOException} if the argument is or
   * has a nested {@link InterruptedIOException}.</p>
   * @param exc The exception thrown.
   * @throws InterruptedIOException if <code>exc</code> is or has a
   * nested {@link InterruptedIOException}.
   */
  protected void throwIfInterrupted(Exception exc)
      throws InterruptedIOException {
    for (Throwable cause = exc ; cause != null ; cause = cause.getCause()) {
      if (cause instanceof InterruptedIOException) {
        logger.debug2("Exception contains InterruptedIOException", exc);
        InterruptedIOException iioe = new InterruptedIOException();
        iioe.initCause(exc);
        throw iioe;
      }
    }
  }

  /**
   * <p>Throws a {@link NullPointerException} if the argument is
   * null.</p>
   * @param obj Any object reference.
   * @throws NullPointerException if <code>obj</code> is <code>null</code>.
   */
  protected void throwIfNull(Object obj) {
    if (obj == null) {
      logger.debug("Attempting to serialize null");
      throw new NullPointerException();
    }
  }

}
