/*
 * $Id: ExternalizableMap.java,v 1.15 2005-03-19 09:08:39 tlipkis Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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
import java.net.*;
import java.util.*;
import org.exolab.castor.mapping.Mapping;

/**
 * ExternalizableMap: A class which allows a map to be loaded in or written as
 * an external xml file.
 * @version 1.0
 */
public class ExternalizableMap extends TypedEntryMap {
  /**
   * The exception to throw should a user attempt to store a map
   * This should be removed when map storage is allowed.
   */
  private static String MAP_WARNING =
    "Entry of type Map not allowed in Externalizable Maps";

  /**
   * The mapping file for the map bean.
   */
  public static final String MAPPING_FILE_NAME =
      "/org/lockss/util/externalmap.xml";
  private static Logger logger = Logger.getLogger("ExternalizableMap");
  XmlMarshaller marshaller = new XmlMarshaller();
  private String loadErr = "";

  public String getLoadErr() {
    return loadErr;
  }

  public void loadMapFromResource(String mapLocation, ClassLoader loader)
      throws FileNotFoundException {
    loadErr = null;
    InputStream mapStream = loader.getResourceAsStream(mapLocation);
    if (mapStream == null) {
      loadErr = "Unable to load:" + mapLocation;
      throw new FileNotFoundException(loadErr);
    }
    try {
      Reader reader = new BufferedReader(new InputStreamReader(mapStream));
      ExtMapBean em = (ExtMapBean) marshaller.loadFromReader(reader,
          ExtMapBean.class, marshaller.getMapping(MAPPING_FILE_NAME));
      if (em != null) {
        m_map = em.getMap();
        //storeMap("src", mapLocation);
      }
    }
    catch (XmlMarshaller.MarshallingException me) {
      // we have a damaged file
      loadErr = "Exception parsing XML file: " + me.toString();
      throw new FileNotFoundException(loadErr);
    }
    catch (Exception e) {
      // some other error occured
      logger.warning("Couldn't load: " + mapLocation, e);
      loadErr = "Exception Loading XML file: " + e.toString();
      throw new FileNotFoundException(loadErr);
    }
  }

  public void loadMap(String mapLocation, String mapName, String mapFileName) {
    loadErr = null;
    String mapFile = mapLocation + File.separator + mapName;
    try {
      ExtMapBean em;
      mapFileName = mapFileName == null ? MAPPING_FILE_NAME: mapFileName;
        em = (ExtMapBean) marshaller.load(mapFile, ExtMapBean.class,
                                          mapFileName);
      if (em==null) {
        return;
      }
      m_map = em.getMap();
   } catch (XmlMarshaller.MarshallingException me) {
      // we have a damaged file
      loadErr = "Exception parsing XML file: " + me.toString();
      logger.error(loadErr);
    } catch (Exception e) {
      // some other error occured
      loadErr = "Exception Loading XML file: " + e.toString();
      logger.error(loadErr);
    }
  }

  public void storeMap(String mapLocation, String mapName, String mapFileName) {
    loadErr = null;
    try {
      mapFileName = mapFileName == null ? MAPPING_FILE_NAME : mapFileName;
      ExtMapBean em = new ExtMapBean(m_map);
      marshaller.store(mapLocation, mapName, em, MAPPING_FILE_NAME);

    }
    catch (Exception e) {
      loadErr = "Exception storing map : " + e.toString();
      logger.error(loadErr);
    }
  }

  public Map getMap(String key, Map def) {
    throw new IllegalArgumentException(MAP_WARNING);
  }

  public void putMap(String key, Map value) {
    throw new IllegalArgumentException(MAP_WARNING);
  }

}

