/*
 * $Id: ExternalizableMap.java,v 1.4 2004-01-17 00:55:27 clairegriffin Exp $
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

import org.exolab.castor.mapping.*;
import org.exolab.castor.xml.*;
import org.lockss.app.*;

/**
 * ExternalizableMap: A class which allows a map to be loaded in or written as
 * an external xml file.
 * @version 1.0
 */
public class ExternalizableMap {
  static final String MAPPING_FILE_NAME = "externalmap.xml";
  HashMap descrMap;
  Mapping mapping = null;
  private static Logger logger = Logger.getLogger("ExternalizableMap");

  public ExternalizableMap() {
    descrMap = new HashMap();
  }

  public Object getMapElement(String descrKey) {
    synchronized(descrMap) {
      return descrMap.get(descrKey);
    }
  }

  public void setMapElement(String descrKey, Object descrElement) {
    synchronized(descrMap) {
      descrMap.put(descrKey, descrElement);
    }
  }

  public void loadMapFromResource(String mapLocation)
  throws FileNotFoundException {
    InputStream mapStream = getClass().getResourceAsStream(mapLocation);
    if (mapStream == null) {
      descrMap = new HashMap();
      String err = "Unable to load:" + mapLocation;
      throw new FileNotFoundException(err);
    }

    try {
      Reader reader = new BufferedReader(new InputStreamReader(mapStream));
      Unmarshaller unmarshaller = new Unmarshaller(ExtMapBean.class);
      unmarshaller.setMapping(getMapping());
      ExtMapBean emp = (ExtMapBean) unmarshaller.unmarshal(reader);
      descrMap = emp.getMapFromLists();
      reader.close();
    }
    catch (org.exolab.castor.xml.MarshalException me) {
      // we have a damaged file
      logger.error(me.toString());
      descrMap = new HashMap();
    }
    catch (Exception e) {
      // some other error occured
      logger.error(e.toString());
      descrMap = new HashMap();
    }
  }

  public void loadMap(String mapLocation, String mapName) {
    File mapFile = null;
    try {
      mapFile = new File(mapLocation, mapName);
      if (!mapFile.exists()) {
        descrMap = new HashMap();
        return;
      }
      FileReader reader = new FileReader(mapFile);
      Unmarshaller unmarshaller = new Unmarshaller(ExtMapBean.class);
      unmarshaller.setMapping(getMapping());
      ExtMapBean emp = (ExtMapBean)unmarshaller.unmarshal(reader);
      descrMap = emp.getMapFromLists();
      reader.close();
    } catch (org.exolab.castor.xml.MarshalException me) {
      // we have a damaged file
      logger.error(me.toString());
      descrMap = new HashMap();
    } catch (Exception e) {
      // some other error occured
      logger.error(e.toString());
      descrMap = new HashMap();
    }
  }

  public void storeMap(String mapLocation, String mapName) {
    try {
      File mapDir = new File(mapLocation);
      if (!mapDir.exists()) {
        mapDir.mkdirs();
      }
      File mapFile = new File(mapDir, mapName);
      FileWriter writer = new FileWriter(mapFile);
      ExtMapBean emp = new ExtMapBean();
      emp.setListsFromMap(descrMap);
      Marshaller marshaller = new Marshaller(new FileWriter(mapFile));
      marshaller.setMapping(getMapping());
      marshaller.marshal(emp);
      writer.close();
    } catch (Exception e) {
      //logger.error("Couldn't store map: ", e);
    }
  }

  private void loadMapping() {
    if (mapping==null) {
      URL mappingLoc = getClass().getResource(MAPPING_FILE_NAME);
      if (mappingLoc==null) {
        logger.error("Couldn't find resource '"+MAPPING_FILE_NAME+"'");
        throw new LockssDaemonException("Couldn't find mapping file.");
      }

      mapping = new Mapping();
      try {
        mapping.loadMapping(mappingLoc);
      } catch (Exception e) {
        logger.error("Couldn't load mapping file '"+mappingLoc+"'", e);
        throw new LockssDaemonException("Couldn't load mapping file.");
      }
    }
  }

  Mapping getMapping() {
    if (mapping==null) {
      loadMapping();
      if (mapping==null) {
        logger.error("Mapping file not loaded.");
        throw new LockssDaemonException("Mapping file not loaded.");
      }
    } else if (mapping.getRoot().getClassMappingCount()==0) {
      logger.error("Mapping file is empty.");
      throw new LockssDaemonException("Mapping file is empty.");
    }
    return mapping;
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
