/*
 * $Id: ExternalizableMap.java,v 1.18.4.1 2006-07-06 17:21:50 thib_gc Exp $
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>A {@link TypedEntryMap} that does not allow keys of type
 * {@link Map} and that can be serialized to an XML file.</p>
 */
public class ExternalizableMap extends TypedEntryMap {

  /*
   * IMPLEMENTATION NOTES
   *
   * Historically, this class exists to help Castor serialize
   * TypedEntryMap instances to XML, which involves preventing nested
   * maps and providing default constructors. Hopefully this class will
   * become deprecated when Castor is phased out and the supertype
   * TypedEntryMap can be used instead.
   */

  // CASTOR: Phase out this class; use superclass TypedEntryMap

  /**
   * <p>The last message resulting from a failure to load a file.</p>
   */
  private String errorString = "";

  /**
   * <p>Retrieves the last message resulting from a failure to load
   * or store a file.</p>
   * @return An error message dating back to the last load or store
   *         operation; may be null if no error occurred.
   */
  public String getErrorString() {
    return errorString;
  }

  /**
   * <p>Throws an {@link IllegalArgumentException}.</p>
   * @param key {@inheritDoc}
   * @param def {@inheritDoc}
   * @return {@inheritDoc}
   */
  public Map getMap(String key, Map def) {
    throw new IllegalArgumentException(MAP_WARNING);
  }

  /**
   * <p>Loads the contents of a serialized map into this object,
   * replacing its contents if the deserialized map is non-null.</p>
   * @param mapLocation The path of the file where the map is stored.
   * @param mapName     The name of the file where the map is stored.
   */
  public void loadMap(String mapLocation,
                      String mapName) {
    loadMap(makeHashMapSerializer(), mapLocation, mapName);
  }

  /**
   * <p>Loads the contents of a serialized map into this object,
   * replacing its contents if the deserialized map is non-null,
   * using the given deserializer.</p>
   * @param deserializer The object serializer to use.
   * @param mapLocation  The path of the file where the map is stored.
   * @param mapName      The name of the file where the map is stored.
   */
  public void loadMap(ObjectSerializer deserializer,
                      String mapLocation,
                      String mapName) {
    errorString = null;
    File mapFile = new File(mapLocation, mapName);

    try {
      // CASTOR: remove unwrap() call
      HashMap map = unwrap(deserializer.deserialize(mapFile));
      m_map = map;
    }
    catch (Exception e) {
      errorString = "Exception loading XML file: " + e.toString();
      logger.error(errorString);
    }
  }

  /**
   * <p>Loads the contents of a serialized map into this object,
   * replacing its contents if the deserialized map is non-null,
   * by reading from a resource obtained from the class loader.</p>
   * @param mapLocation The location of the serialized map.
   * @param loader      The class loader.
   * @throws FileNotFoundException if the resource is not found by the
   *                               class loader.
   */
  public void loadMapFromResource(String mapLocation,
                                  ClassLoader loader)
      throws FileNotFoundException {
    loadMapFromResource(makeHashMapSerializer(), mapLocation, loader);
  }

  /**
   * <p>Loads the contents of a serialized map into this object,
   * replacing its contents if the deserialized map is non-null,
   * by reading from a resource obtained from the class loader
   * using the given deserializer.</p>
   * @param deserializer The object serializer to use.
   * @param mapLocation  The location of the serialized map.
   * @param loader       The class loader.
   * @throws FileNotFoundException if the resource is not found by the
   *                               class loader.
   */
  public void loadMapFromResource(ObjectSerializer deserializer,
                                  String mapLocation,
                                  ClassLoader loader)
      throws FileNotFoundException {
    errorString = null;
    InputStream mapStream = loader.getResourceAsStream(mapLocation);
    if (mapStream == null) {
      errorString = "Unable to load: " + mapLocation;
      throw new FileNotFoundException(errorString);
    }

    try {
      // CASTOR: Remove unwrap() call
      HashMap map = unwrap(deserializer.deserialize(mapStream));
      m_map = map;
    }
    catch (Exception e) {
      logger.warning("Could not load: " + mapLocation, e);
      errorString = "Exception loading XML file: " + e.toString();
      throw new FileNotFoundException(errorString);
    }
  }

  /**
   * <p>Throws an {@link IllegalArgumentException}.</p>
   * @param key {@inheritDoc}
   * @param value {@inheritDoc}
   */
  public void putMap(String key, Map value) {
    throw new IllegalArgumentException(MAP_WARNING);
  }

  /**
   * <p>Stores the contents of this object into an XML file.</p>
   * @param mapLocation The path of the file to serialize into.
   * @param mapName     The name of the file to serialize into.
   */
  public void storeMap(String mapLocation,
                       String mapName) {
    storeMap(makeHashMapSerializer(), mapLocation, mapName);
  }

  /**
   * <p>Stores the contents of this object into an XML file
   * using the given serializer.</p>
   * @param serializer  The object serializer to use.
   * @param mapLocation The path of the file to serialize into.
   * @param mapName     The name of the file to serialize into.
   */
  public void storeMap(ObjectSerializer serializer,
                       String mapLocation,
                       String mapName) {
    errorString = null;
    File mapDir = new File(mapLocation);
    if (!mapDir.exists()) { mapDir.mkdirs(); }
    File mapFile = new File(mapDir, mapName);

    try {
      // CASTOR: Remove wrap() call
      serializer.serialize(mapFile, wrap(m_map));
    }
    catch (Exception e) {
      errorString = "Exception storing map: " + e.toString();
      logger.error(errorString);
    }
  }

  /**
   * <p>The mapping file for the map bean, {@link ExtMapBean} (which
   * is the type of object actually serialized under the current
   * implementation).</p>
   */
  public static final String MAPPING_FILE_NAME =
      "/org/lockss/util/externalmap.xml";

  /**
   * <p>A logger for events associated with this class.</p>
   */
  private static Logger logger = Logger.getLogger("ExternalizableMap");

  /**
   * <p>The exception message to use when a user attempts to store a
   * map.</p>
   */
  private static String MAP_WARNING =
    "Entry of type Map not allowed in Externalizable Maps";

  /**
   * <p>Retrieves the current serialization mode.</p>
   * @return A mode constant from {@link CXSerializer}.
   */
  private static int getSerializationMode() {
    return CXSerializer.getCompatibilityModeFromConfiguration();
  }

  /**
   * <p>A generic way to obtain an object serializer instance
   * appropriate for objects of this class.</p>
   * @return An instance of {@link ObjectSerializer} ready to process
   *         instance of this class.
   */
  private static ObjectSerializer makeHashMapSerializer() {
    // CASTOR: Change to returning an XStreamSerializer
    CXSerializer serializer =
      new CXSerializer(MAPPING_FILE_NAME, ExtMapBean.class);
    serializer.setCompatibilityMode(getSerializationMode());
    return serializer;
  }

  /**
   * <p>Unwraps whatever comes out of the deserializer so that the
   * final result is a HashMap (as expected by client code).</p>
   * @param obj The object unmarshalled by the deserializer.
   * @return The HashMap client code thinks it has deserialized.
   */
  private static HashMap unwrap(Object obj) {
    // CASTOR: This goes away with Castor
    if (obj == null) {
      return null;
    }
    else if (obj instanceof ExtMapBean) {
      return ((ExtMapBean)obj).getMap();
    }
    else {
      return (HashMap)obj;
    }
  }

  /**
   * <p>Wraps a HashMap that client code is trying to serialize, so
   * that the serializer receives an object of a type it expects
   * (which may not be HashMap).</p>
   * @param map A HashMap intended for marshalling.
   * @return The object that will actually be serialized.
   */
  private static Object wrap(HashMap map) {
    // CASTOR: This goes away with Castor
    if (getSerializationMode() == CXSerializer.CASTOR_MODE) {
      return new ExtMapBean(map);
    }
    else {
      return map;
    }
  }

}
