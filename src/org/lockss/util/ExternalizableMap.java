/*
 * $Id: ExternalizableMap.java,v 1.12 2004-07-14 20:43:16 clairegriffin Exp $
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
public class ExternalizableMap {
  /**
   * The mapping file for the map bean.
   */
  public static final String MAPPING_FILE_NAME =
      "/org/lockss/util/externalmap.xml";
  HashMap descrMap;
  private static Logger logger = Logger.getLogger("ExternalizableMap");
  XmlMarshaller marshaller = new XmlMarshaller();

  public ExternalizableMap() {
    descrMap = new HashMap();
  }

  public Object getMapElement(String descrKey) {
    synchronized(descrMap) {
      return descrMap.get(descrKey);
    }
  }

  public Set keySet() {
    return descrMap.keySet();
  }

  public Set entrySet() {
    return descrMap.entrySet();
  }

  public void setMapElement(String descrKey, Object descrElement) {
    synchronized(descrMap) {
      if (descrElement instanceof URL) {
        descrMap.put(descrKey, descrElement.toString());
      } else {
        descrMap.put(descrKey, descrElement);
      }
    }
  }

  public Object removeMapElement(String descrKey) {
    synchronized(descrMap) {
      return descrMap.remove(descrKey);
    }
  }


  public void loadMapFromResource(String mapLocation, ClassLoader loader)
      throws FileNotFoundException {
    InputStream mapStream = loader.getResourceAsStream(mapLocation);
    if (mapStream == null) {
      String err = "Unable to load:" + mapLocation;
      throw new FileNotFoundException(err);
    }
    try {
      Reader reader = new BufferedReader(new InputStreamReader(mapStream));
      ExtMapBean em = (ExtMapBean) marshaller.loadFromReader(reader,
          ExtMapBean.class, marshaller.getMapping(MAPPING_FILE_NAME));
      if (em != null) {
        descrMap = em.getMap();
        //storeMap("src", mapLocation);
      }
    }
    catch (XmlMarshaller.MarshallingException me) {
      // we have a damaged file
      throw new FileNotFoundException("Damaged XML file: " + me.toString());
    }
    catch (Exception e) {
      // some other error occured
      e.printStackTrace();
      throw new FileNotFoundException("Error: " + e.toString());
    }
  }

  public void loadMap(String mapLocation, String mapName, String mapFileName) {
    String mapFile = mapLocation + File.separator + mapName;
    try {
      ExtMapBean em;
      mapFileName = mapFileName == null ? MAPPING_FILE_NAME: mapFileName;
        em = (ExtMapBean) marshaller.load(mapFile, ExtMapBean.class,
                                          mapFileName);
      if (em==null) {
        return;
      }
      descrMap = em.getMap();
   } catch (XmlMarshaller.MarshallingException me) {
      // we have a damaged file
      logger.error(me.toString());
    } catch (Exception e) {
      // some other error occured
      logger.error(e.toString());
    }
  }

  public void storeMap(String mapLocation, String mapName, String mapFileName) {
    try {
      mapFileName = mapFileName == null ? MAPPING_FILE_NAME : mapFileName;
      ExtMapBean em = new ExtMapBean(descrMap);
      marshaller.store(mapLocation, mapName, em, MAPPING_FILE_NAME);

    }
    catch (Exception e) {
      logger.error("Couldn't store map: ", e);
    }
  }


  /*
   *
   *  methods for retrieving typed data or returning a default
   *
   */
  public String getString(String key, String def) {
    String ret = def;
    String value = (String)getMapElement(key);
    if (value != null) {
      ret = value;
    }
    return ret;
  }

  public boolean getBoolean(String key, boolean def) {
    boolean ret = def;

    Boolean value = (Boolean)getMapElement(key);
    if (value != null) {
      ret = value.booleanValue();
    }
    return ret;
  }


  public double getDouble(String key, double def) {
    double ret = def;

    try {
      Double value = (Double)getMapElement(key);
      if (value != null) {
        ret = value.doubleValue();
      }
    } catch (NumberFormatException ex) { }
    return ret;
  }


  public float getFloat(String key, float def) {
    float ret = def;
    try {
      Float value = (Float)getMapElement(key);
      if (value != null) {
        ret = value.floatValue();
      }
    } catch (NumberFormatException ex) { }
    return ret;
  }


  public int getInt(String key, int def) {
    int ret = def;
    try {
      Integer value = (Integer)getMapElement(key);
      if (value != null) {
        ret = value.intValue();
      }
    } catch (NumberFormatException ex) { }
    return ret;
  }

  public long getLong(String key, long def) {
    long ret = def;

    try {
      Long value = (Long)getMapElement(key);
      if (value != null) {
        ret = value.longValue();
      }
    } catch (NumberFormatException ex) { }
    return ret;
  }

  public URL getUrl(String key, URL def) {
    URL ret = def;

    String valueStr = (String)getMapElement(key);
    if (valueStr!=null) {
      try {
        URL value = new URL(valueStr);
        ret = value;
      } catch (MalformedURLException mue) { }
    }
    return ret;
  }

  public Collection getCollection(String key, Collection def) {
    Collection ret = def;
    Collection value = (Collection)getMapElement(key);
    if (value != null) {
      ret = value;
    }
    return ret;
  }

/* removed because Castor can't marshal Maps properly

  public Map getMap(String key, Map def) {
    Map ret = def;
    Map value = (Map)getMapElement(key);
    if (value != null) {
      ret = value;
    }
    return ret;
  }
 */

  /*
   *
   * methods for storing typed primitive data
   *
   */

  public void putString(String key, String value) {
    setMapElement(key, value);
  }

  public void putBoolean(String key, boolean value) {
    setMapElement(key, new Boolean(value));
  }

  public void putDouble(String key, double value) {
    setMapElement(key, new Double(value));
  }

  public void putFloat(String key, float value) {
    setMapElement(key, new Float(value));
  }

  public void putInt(String key, int value) {
    setMapElement(key, new Integer(value));
  }

  public void putLong(String key, long value) {
    setMapElement(key, new Long(value));
  }

  public void putUrl(String key, URL url) {
    setMapElement(key, url.toString());
  }

  public void putCollection(String key, Collection value) {
    setMapElement(key, value);
  }

/* removed because Castor can't marshal Maps properly

  public void putMap(String key, Map value) {
    setMapElement(key, value);
  }
 */
}

