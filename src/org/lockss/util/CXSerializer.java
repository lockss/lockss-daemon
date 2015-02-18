/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.exolab.castor.mapping.Mapping;

import org.lockss.app.LockssApp;
import org.lockss.config.*;
import org.lockss.util.SerializationException;

/**
 * <p>An adapter class implementing
 * {@link org.lockss.util.ObjectSerializer} using both
 * {@link org.lockss.util.CastorSerializer} and
 * {@link org.lockss.util.XStreamSerializer}.</p>
 * <p>This class provides a bridge between Castor-based serialization
 * (which is being phased out) and XStream-based serialization (which
 * is being phased in). It may be deprecated in the future. New client
 * code should not use this class for serialization tasks.</p>
 * <p>It is generally <em>not safe</em> to use an instance of this
 * class for multiple unrelated serialization operations. It may be
 * possible to re-use the same instance for multiple marshalling
 * or for multiple unmarshalling operations based on the same
 * mapping.</p>
 * <p>In some modes of operation, this class may not be able to enforce
 * that object graphs are serializable when carrying out serialization
 * tasks.</p>
 * @author Thib Guicherd-Callin
 * @see CastorSerializer
 * @see XStreamSerializer
 */
public class CXSerializer extends ObjectSerializer {

  /**
   * <p>A {@link SerializationException} used when XStream overwrite
   * mode is in effect but the overwrite fails after the
   * deserialization has succeeded.</p>
   * @author Thib Guicherd-Callin
   */
  protected static class OverwriteFailed extends SerializationException {

    public OverwriteFailed() {
      super();
    }

    public OverwriteFailed(String message) {
      super(message);
    }

    public OverwriteFailed(String message, Throwable cause) {
      super(message, cause);
    }

    public OverwriteFailed(Throwable cause) {
      super(cause);
    }

  }

  /**
   * <p>A {@link CastorSerializer} adaptee.</p>
   */
  private CastorSerializer castor;

  /**
   * <p>This serializer's current compatibility mode.</p>
   * <p>Must be one of the three mode constants {@link #CASTOR_MODE},
   * {@link #XSTREAM_MODE} or {@link #XSTREAM_OVERWRITE_MODE}.</p>
   * @see #CASTOR_MODE
   * @see #XSTREAM_MODE
   * @see #XSTREAM_OVERWRITE_MODE
   */
  private int compatibilityMode;

  /**
   * <p>An {@link XStreamSerializer} adaptee.</p>
   */
  private XStreamSerializer xstream;

  /**
   * <p>Builds a new CXSerializer instance with the given reference
   * mapping and class, and with the given context.</p>
   * @param lockssContext A serialization context object.
   * @param targetMapping The {@link Mapping} of objects intended for
   *                      processing by this serializer.
   * @param targetClass   The Class of objects intended for processing
   *                      by this serializer.
   * @see ObjectSerializer#ObjectSerializer(LockssApp)
   */
  public CXSerializer(LockssApp lockssContext,
                      Mapping targetMapping,
                      Class targetClass) {
    super(lockssContext);
    this.castor = new CastorSerializer(lockssContext, targetMapping, targetClass);
    this.xstream = new XStreamSerializer(lockssContext);
    setCompatibilityMode(getCompatibilityModeFromConfiguration());
  }

  /**
   * <p>Builds a new CXSerializer instance with the given reference
   * mapping and class, and with the given context.</p>
   * @param lockssContext A serialization context object.
   * @param targetMapping             The {@link Mapping} of objects
   *                                  intended for processing by this
   *                                  serializer.
   * @param targetClass               The Class of objects intended for
   *                                  processing by this serializer.
   * @param saveTempFiles             A failed serialization mode.
   * @param failedDeserializationMode A failed deserialization mode.
   * @see ObjectSerializer#ObjectSerializer(LockssApp, boolean, int)
   */
  public CXSerializer(LockssApp lockssContext,
                      Mapping targetMapping,
                      Class targetClass,
                      boolean saveTempFiles,
                      int failedDeserializationMode) {
    super(lockssContext,
          saveTempFiles,
          failedDeserializationMode);
    this.castor = new CastorSerializer(lockssContext, targetMapping, targetClass);
    this.xstream = new XStreamSerializer(lockssContext);
    setCompatibilityMode(getCompatibilityModeFromConfiguration());
  }

  /**
   * <p>Builds a new CXSerializer instance with the given reference
   * mapping and class, and with the given context.</p>
   * @param lockssContext   A serialization context object.
   * @param mappingFilename A filename where the mapping file for
   *                        objects intended for processing by this
   *                        serializer can be located.
   * @param targetClass     The Class of objects intended for
   *                        processing by this serializer.
   * @see #CXSerializer(LockssApp, Mapping, Class)
   */
  public CXSerializer(LockssApp lockssContext,
                      String mappingFilename,
                      Class targetClass) {
    this(lockssContext,
         CastorSerializer.getMapping(mappingFilename),
         targetClass);
  }

  /**
   * <p>Builds a new CXSerializer instance with the given reference
   * mapping and class, and with the given context.</p>
   * @param lockssContext             A serialization context object.
   * @param mappingFilename           A filename where the mapping
   *                                  file for objects intended for
   *                                  processing by this serializer
   *                                  can be located.
   * @param targetClass               The Class of objects intended for
   *                                  processing by this serializer.
   * @param saveTempFiles             A failed serialization mode.
   * @param failedDeserializationMode A failed deserialization mode.
   * @see #CXSerializer(LockssApp, Mapping, Class, boolean, int)
   */
  public CXSerializer(LockssApp lockssContext,
                      String mappingFilename,
                      Class targetClass,
                      boolean saveTempFiles,
                      int failedDeserializationMode) {
    this(lockssContext,
         CastorSerializer.getMapping(mappingFilename),
         targetClass,
         saveTempFiles,
         failedDeserializationMode);
  }

  /**
   * <p>Builds a new CXSerializer instance with the given reference
   * mapping and class, and with a null context.</p>
   * @param mappingFilename A filename where the mapping file for
   *                        objects intended for processing by this
   *                        serializer can be located.
   * @param targetClass     The Class of objects intended for
   *                        processing by this serializer.
   * @see #CXSerializer(LockssApp, String, Class)
   */
  public CXSerializer(String mappingFilename,
                      Class targetClass) {
    this(null,
         mappingFilename,
         targetClass);
  }

  /**
   * <p>Builds a new CXSerializer instance with the given reference
   * mapping and class, and with a null context.</p>
   * @param mappingFilename           A filename where the mapping
   *                                  file for objects intended for
   *                                  processing by this serializer
   *                                  can be located.
   * @param targetClass               The Class of objects intended for
   *                                  processing by this serializer.
   * @param saveTempFiles             A failed serialization mode.
   * @param failedDeserializationMode A failed deserialization mode.
   * @see #CXSerializer(LockssApp, String, Class, boolean, int)
   */
  public CXSerializer(String mappingFilename,
                      Class targetClass,
                      boolean saveTempFiles,
                      int failedDeserializationMode) {
    this(null,
         mappingFilename,
         targetClass,
         saveTempFiles,
         failedDeserializationMode);
  }

  /**
   * <p>Deserializes an object from the given file, and overwrites the
   * input file in XStream format if the original file was in Castor
   * format (when this serializer is in XSTREAM_OVERWIRTE_MODE).</p>
   * <p>See {@link ObjectSerializer#deserialize(File)} for a
   * description of this method's specification.</p>
   * @param inputFile {@inheritDoc}
   * @throws SerializationException.FileNotFound {@inheritDoc}
   * @throws SerializationException              {@inheritDoc}
   * @throws InterruptedIOException              {@inheritDoc}
   */
  public Object deserialize(File inputFile)
      throws SerializationException,
             InterruptedIOException {
    // Open reader
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new FileReader(inputFile));
    }
    catch (FileNotFoundException fnfe) {
      throw new SerializationException.FileNotFound(fnfe);
    }

    // Local variables
    MutableBoolean wasCastor = new MutableBoolean(false);
    Object ret = null;

    // Deserialize
    try {
      ret = deserialize(reader, wasCastor);
    }
    catch (SerializationException se) {
      // must close reader to unlock inputFile
      // because failDeserialize manipulates it
      IOUtil.safeClose(reader);  
      throw failDeserialize(se, inputFile);
    }
    catch (InterruptedIOException iioe) {
      // must close reader to unlock inputFile
      // because failDeserialize manipulates it
      IOUtil.safeClose(reader);
      throw failDeserialize(iioe, inputFile);
    }
    finally {
      IOUtil.safeClose(reader);
    }

    // Overwrite
    if (getCompatibilityMode() == XSTREAM_OVERWRITE_MODE && wasCastor.booleanValue()) {
      boolean shouldThrow = CurrentConfig.getBooleanParam(PARAM_FAILED_OVERWRITE_THROWS,
                                                          DEFAULT_FAILED_OVERWRITE_THROWS);
      try {
        serialize(inputFile, ret);
      }
      catch (SerializationException se) {
        logger.debug("Overwrite failed", se);
        if (shouldThrow) {
          throw failDeserialize(se, inputFile);
        }
      }
      catch (InterruptedIOException iioe) {
        logger.debug("Overwrite interrupted", iioe);
        if (shouldThrow) {
          throw failDeserialize(iioe, inputFile);
        }
      }
      catch (RuntimeException re) {
        logger.debug("Overwrite caused runtime exception", re);
        if (shouldThrow) {
          throw re;
        }
      }
    }

    return ret;
  }

  /* Inherit documentation */
  public Object deserialize(Reader reader)
      throws SerializationException,
             InterruptedIOException {
    return deserialize(reader, new MutableBoolean(false));
  }

  /**
   * <p>Deserializes an object from a reader, determining on the fly
   * if the incoming object is in Castor or XStream format.</p>
   * @param reader    A reader instance.
   * @param wasCastor A mutable boolean. After the method executes,
   *                  its value will be true if the input was in
   *                  Castor format, false otherwise.
   * @return An Object reference whose field were populated from the
   *         data found in the XML file.
   * @throws SerializationException   if input or output fails.
   * @throws InterruptedIOException   if input or output is
   *                                  interrupted.
   */
  public Object deserialize(Reader reader,
                            MutableBoolean wasCastor)
      throws SerializationException,
             InterruptedIOException {
    // Constants
    final String recognizeCastor = "<?xml";

    // Make rewinding possible
    BufferedReader bufReader = new BufferedReader(reader);

    // Peek at beginning of input
    char[] buffer = new char[recognizeCastor.length()];
    try {
      bufReader.mark(recognizeCastor.length() + 1);
      if (StreamUtil.readChars(bufReader, buffer, buffer.length) != buffer.length) {
        throw failDeserialize(new SerializationException("Could not peek at first "
                                                         + buffer.length + " bytes"));
      }
      bufReader.reset();
    }
    catch (IOException exc) {
      throw failDeserialize(exc);
    }

    // Guess format and deserialize
    ObjectSerializer deserializer;
    if (recognizeCastor.equals(new String(buffer))) {
      deserializer = castor;
      wasCastor.setValue(true);
    }
    else {
      deserializer = xstream;
      wasCastor.setValue(false);
    }
    return deserializer.deserialize(bufReader);
  }

  /**
   * <p>Returns the current mode for this serializer.</p>
   * @return The current mode constant for this serializer.
   * @see #PARAM_COMPATIBILITY_MODE
   */
  public int getCompatibilityMode() {
    return compatibilityMode;
  }

  /**
   * <p>Sets the current mode for this serializer.</p>
   * <p>Must be one of the three mode constants {@link #CASTOR_MODE},
   * {@link #XSTREAM_MODE} or {@link #XSTREAM_OVERWRITE_MODE}.
   * If the argument is not a valid mode, no action is taken.</p>
   * @param mode The mode to which this serializer should be set.
   * @see #PARAM_COMPATIBILITY_MODE
   */
  public void setCompatibilityMode(int mode) {
    switch (mode) {
      case CASTOR_MODE:
      case XSTREAM_MODE:
      case XSTREAM_OVERWRITE_MODE:
        this.compatibilityMode = mode;
        break;
      default:
        String errorString = "Attempt to set CXSerializer mode to " + mode;
        logger.error(errorString);
        throw new IllegalArgumentException(errorString);
    }
  }

  /* Inherit documentation */
  protected void serialize(Writer writer,
                           Object obj)
      throws SerializationException,
             InterruptedIOException {
    throwIfNull(obj);

    ObjectSerializer serializer = null;
    if (getCompatibilityMode() == CASTOR_MODE) {
      serializer = castor;
    }
    else {
      serializer = xstream;
    }

    serializer.serialize(writer, obj);
  }

  /**
   * <p>Serialization always in Castor format.</p>
   */
  public static final int CASTOR_MODE = 1;

  /**
   * <p>The default value of {@link #PARAM_FAILED_OVERWRITE_THROWS}
   * (currently true).</p>
   */
  public static final boolean DEFAULT_FAILED_OVERWRITE_THROWS = true;

  /**
   * <p>A configuration parameter that governs the mode of operation
   * of instances of this class. Must be one of:</p>
   * <table>
   *  <thead>
   *   <tr>
   *    <td>Value</td>
   *    <td>Reference</td>
   *    <td>Description</td>
   *   </tr>
   *  </thead>
   *  <tbody>
   *   <tr>
   *    <td>1</td>
   *    <td>{@link #CASTOR_MODE}</td>
   *    <td>Always write output in Castor format.</td>
   *   </tr>
   *   <tr>
   *    <td>2</td>
   *    <td>{@link #XSTREAM_MODE}</td>
   *    <td>Always write output in XStream format.</td>
   *   </tr>
   *   <tr>
   *    <td>3</td>
   *    <td>{@link #XSTREAM_OVERWRITE_MODE}</td>
   *    <td>Always write output in XStream format, and overwrite any
   *    files read in Castor format by files in XStream format.</td>
   *   </tr>
   *  </tbody>
   * </table>
   */
  public static final String PARAM_COMPATIBILITY_MODE = PREFIX + "compatibilityMode";

  /**
   * <p>A parameter that controls if failure to overwrite a file
   * while in XStream Overwrite mode causes the whole deserialization
   * to fail by throwing (true) or to be logged without throwing
   * (false).</p>
   */
  public static final String PARAM_FAILED_OVERWRITE_THROWS = PREFIX + "failedOverwriteThrows";

  /**
   * <p>Serialization always in XStream format.</p>
   */
  public static final int XSTREAM_MODE = 2;

  /**
   * <p>Serialization always in XStream format; additionally, any
   * deserialization performed on a {@link File} or {@link String}
   * (i.e. file name) also results in the corresponding file being
   * overwritten by one in XStream format if it is in Castor
   * format. (Does not work for other <code>deserialize</code>
   * calls.)</p>
   * @see ObjectSerializer#deserialize(File)
   * @see ObjectSerializer#deserialize(String)
   */
  public static final int XSTREAM_OVERWRITE_MODE = 3;

  /**
   * <p>The default value of {@link #PARAM_COMPATIBILITY_MODE}
   * (currently {@link #XSTREAM_MODE}).</p>
   */
  private static final int DEFAULT_COMPATIBILITY_MODE = XSTREAM_MODE;

  /**
   * <p>A logger for use by this serializer.</p>
   */
  private static final Logger logger = Logger.getLogger(CXSerializer.class);

  /**
   * <p>Returns the mode for this class from the configuration.</p>
   * @return The mode constant currently in the configuration.
   * @see #PARAM_COMPATIBILITY_MODE
   */
  public static int getCompatibilityModeFromConfiguration() {
    return CurrentConfig.getIntParam(PARAM_COMPATIBILITY_MODE,
                                     DEFAULT_COMPATIBILITY_MODE);
  }


}
