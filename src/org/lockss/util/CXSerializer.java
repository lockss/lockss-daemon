/*
 * $Id: CXSerializer.java,v 1.14 2005-12-01 23:28:00 troberts Exp $
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

import org.exolab.castor.mapping.Mapping;

import org.lockss.app.LockssApp;
import org.lockss.config.CurrentConfig;

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
   * <p>A {@link CastorSerializer} adaptee.</p>
   */
  private CastorSerializer castor;

  /**
   * <p>This serializer's current mode.</p>
   * <p>Must be one of the three mode constants {@link #CASTOR_MODE},
   * {@link #XSTREAM_MODE} or {@link #XSTREAM_OVERWRITE_MODE}.</p>
   * @see #CASTOR_MODE
   * @see #XSTREAM_MODE
   * @see #XSTREAM_OVERWRITE_MODE
   */
  private int currentMode;

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
   */
  public CXSerializer(LockssApp lockssContext,
                      Mapping targetMapping,
                      Class targetClass) {
    super(lockssContext);
    this.castor = new CastorSerializer(lockssContext, targetMapping, targetClass);
    this.xstream = new XStreamSerializer(lockssContext);
    setCurrentMode(getModeFromConfiguration());
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
    this(lockssContext, CastorSerializer.getMapping(mappingFilename), targetClass);
  }

  /**
   * <p>Builds a new CXSerializer instance with the given reference
   * mapping and class, and with a null context.</p>
   * @param targetMapping The
   *                      {@link org.exolab.castor.mapping.Mapping}
   *                      of objects intended for processing by this
   *                      serializer.
   * @param targetClass   The Class of objects intended for processing
   *                      by this serializer.
   * @see #CXSerializer(LockssApp, Mapping, Class)
   */
  public CXSerializer(Mapping targetMapping,
                      Class targetClass) {
    this(null, targetMapping, targetClass);
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
    this(null, mappingFilename, targetClass);
  }

  /**
   * <p>Deserializes an object from the given file, and overwrites the
   * input file in XStream format if the original file was in Castor
   * format (when this serializer is in XSTREAM_OVERWIRTE_MODE).</p>
   * <p>See {@link ObjectSerializer#deserialize(File)} for a
   * description of this method's specification.</p>
   * @param inputFile {@inheritDoc}
   * @throws FileNotFoundException  {@inheritDoc}
   * @throws IOException            {@inheritDoc}
   * @throws SerializationException {@inheritDoc}
   */
  public Object deserialize(File inputFile)
      throws FileNotFoundException, IOException, SerializationException {
    BufferedReader reader = new BufferedReader(new FileReader(inputFile));
    MutableBoolean wasCastor = new MutableBoolean(false);
    boolean success = false;
    Object ret = null;

    try {
      ret = deserialize(reader, wasCastor);
      success = true;
      return ret;
    }
    catch (SerializationException se) {
      throw failDeserialize(se, inputFile);
    }
    finally {
      IOUtil.safeClose(reader);
      if (   getCurrentMode() == XSTREAM_OVERWRITE_MODE
          && wasCastor.booleanValue()
          && success) {
        // Overwrite
        serialize(inputFile, ret);
      }
    }
  }

  /* Inherit documentation */
  public Object deserialize(Reader reader)
      throws IOException, SerializationException {
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
   * @throws IOException            if input or output fails.
   * @throws SerializationException if an internal serialization error
   *                                occurs.
   */
  public Object deserialize(Reader reader, MutableBoolean wasCastor)
      throws IOException, SerializationException {
    // Constants
    final String recognizeCastor = "<?xml";

    // Make rewinding possible
    BufferedReader bufReader =
      new BufferedReader(reader);

    // Peek at beginning of input
    char[] buffer = new char[recognizeCastor.length()];
    bufReader.mark(recognizeCastor.length() + 1);
    if (StreamUtil.readChars(bufReader, buffer, buffer.length) != buffer.length) {
      throw new SerializationException("Could not peek at first "
          + buffer.length + " bytes");
    }
    bufReader.reset();

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
   */
  public int getCurrentMode() {
    return currentMode;
  }

  /**
   * <p>Sets the current mode for this serializer.</p>
   * <p>Must be one of the three mode constants {@link #CASTOR_MODE},
   * {@link #XSTREAM_MODE} or {@link #XSTREAM_OVERWRITE_MODE}.
   * If the argument is not a valid mode, no action is taken.</p>
   * @param mode The mode to which this serializer should be set.
   * @see #CASTOR_MODE
   * @see #XSTREAM_MODE
   * @see #XSTREAM_OVERWRITE_MODE
   */
  public void setCurrentMode(int mode) {
    switch (mode) {
      case CASTOR_MODE: case XSTREAM_MODE: case XSTREAM_OVERWRITE_MODE:
        this.currentMode = mode;
        break;
      default:
        logger.error("Attempt to set CXSerializer mode to " + mode);
        break; // ignore
    }
  }

  protected void serialize(Writer writer, Object obj)
      throws IOException, SerializationException {
    throwIfNull(obj);
    ObjectSerializer serializer;
    if (getCurrentMode() == CASTOR_MODE) { serializer = castor; }
    else                                 { serializer = xstream; }
    serializer.serialize(writer, obj);
  }

  /**
   * <p>Serialization always in Castor format.</p>
   */
  public static final int CASTOR_MODE = 1;

  /**
   * <p>A configuration parameter that governs the mode of operation
   * of instances of this class. Must be one of:
   * <table>
   * <thead>
   * <tr>
   * <td>Value</td>
   * <td>Reference</td>
   * <td>Description</td>
   * </tr>
   * </thead>
   * <tbody>
   * <tr>
   * <td>1</td>
   * <td>{@link #CASTOR_MODE}</td>
   * <td>Always write output in Castor format.</td>
   * </tr>
   * <tr>
   * <td>2</td>
   * <td>{@link #XSTREAM_MODE}</td>
   * <td>Always write output in XStream format.</td>
   * </tr>
   * <tr>
   * <td>3</td>
   * <td>{@link #XSTREAM_OVERWRITE_MODE}</td>
   * <td>Always write output in XStream format, and overwrite any
   * files read in Castor format by files in XStream format.</td>
   * </tr>
   * </tbody>
   * </table>
   * </p>
   */
  public static final String PARAM_COMPATIBILITY_MODE =
    "org.lockss.serialization.compatibilityMode";

  /**
   * <p>Serialization always in XStream format.</p>
   */
  public static final int XSTREAM_MODE = 2;

  /**
   * <p>Serialization always in XStream format; additionally, any
   * deserialization performed on a {@link File} or {@link String}
   * (ie filename) also results in the corresponding file being
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
   * <p>Returns the mode for this class from the configuration.</p>
   * @return The mode constant currently in the configuration.
   * @see #PARAM_COMPATIBILITY_MODE
   */
  public static int getModeFromConfiguration() {
    return CurrentConfig.getIntParam(PARAM_COMPATIBILITY_MODE,
                                     DEFAULT_COMPATIBILITY_MODE);
  }

}
