/*
 * $Id: XmlMarshaller.java,v 1.7 2005-07-25 01:20:45 tlipkis Exp $
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
import java.net.URL;
import java.util.HashMap;

import org.lockss.app.LockssAppException;

import org.exolab.castor.xml.*;
import org.exolab.castor.mapping.*;

/**
 * A utility class to handle marshalling files.
 */
public class XmlMarshaller {
  private static Logger logger = Logger.getLogger("XmlMarshaller");
  static HashMap mappingFileMap = new HashMap();

  public XmlMarshaller() { }

  /**
   * Stores an Object into a File
   * @param file the File
   * @param storeObj the object to be stored
   * @param mapping the Mapping
   * @throws IOException
   * @throws MarshallingException
   */
  public void store(File file, Object storeObj, Mapping mapping)
      throws IOException, MarshallingException {
    FileWriter writer = new FileWriter(file);
    storeToWriter(writer, storeObj, mapping);
  }

  /**
   * Stores an Object into a File
   * @param file the File
   * @param storeObj the object to be stored
   * @param mappingFile the name of the mapping file
   * @throws IOException
   * @throws MarshallingException
   */
  public void store(File file, Object storeObj, String mappingFile)
      throws IOException, MarshallingException {
    store(file, storeObj, getMapping(mappingFile));
  }

  /**
   * Stores an Object into a file location specified.
   * @param root the file's directory.  Created if non-existant.
   * @param fileName the name of the file
   * @param storeObj the object to be stored
   * @param mappingFile the name of the mapping file
   * @throws IOException
   * @throws MarshallingException
   */
  public void store(String root, String fileName, Object storeObj,
		    String mappingFile)
      throws IOException, MarshallingException {
    store(root, fileName, storeObj, getMapping(mappingFile));
  }

  /**
   * Stores an Object into a file location specified.
   * @param root the file's directory.  Created if non-existant.
   * @param fileName the name of the file
   * @param storeObj the object to be stored
   * @param mapping the Mapping
   * @throws IOException
   * @throws MarshallingException
   */
  public void store(String root, String fileName, Object storeObj,
		    Mapping mapping)
      throws IOException, MarshallingException {
    File rootDir = new File(root);
    if (!rootDir.exists()) {
      rootDir.mkdirs();
    }
    store(new File(rootDir, fileName), storeObj, mapping);
  }

  /**
   * Stores an Object into a Writer specified.
   * @param writer the Writer
   * @param storeObj the object to be stored
   * @param mapping the Mapping
   * @throws IOException
   * @throws MarshallingException
   */
  public void storeToWriter(Writer writer, Object storeObj, Mapping mapping)
      throws IOException, MarshallingException {
    Marshaller marshaller = new Marshaller(writer);
    try {
      marshaller.setMapping(mapping);
      marshaller.marshal(storeObj);
    } catch (org.exolab.castor.mapping.MappingException me) {
      logger.debug3(me.toString());
      throw new MarshallingException(me.getMessage());
    } catch (org.exolab.castor.xml.MarshalException me) {
      logger.debug3(me.toString());
      throw new MarshallingException(me.getMessage());
    } catch (org.exolab.castor.xml.ValidationException ve) {
      logger.debug3(ve.toString());
      throw new MarshallingException(ve.getMessage());
    } finally {
      writer.close();
    }
  }


  /**
   * Loads an Object from the file location specified.  Returns null if
   * the file wasn't found.
   * @param fileName the name of the file
   * @param loadClass the class to be loaded
   * @param mappingFile the name of the mapping file
   * @throws IOException
   * @throws MarshallingException
   * @return Object the unmarshalled object
   */
  public Object load(String fileName, Class loadClass, String mappingFile)
      throws IOException, MarshallingException {
    return load(new File(fileName), loadClass, getMapping(mappingFile));
  }

  /**
   * Loads an Object from the file location specified.  Returns null if
   * the file wasn't found.
   * @param file the File
   * @param loadClass the class to be loaded
   * @param mappingFile the name of the mapping file
   * @throws IOException
   * @throws MarshallingException
   * @return Object the unmarshalled object
   */
  public Object load(File file, Class loadClass, String mappingFile)
      throws IOException, MarshallingException {
    return load(file, loadClass, getMapping(mappingFile));
  }

  /**
   * Loads an Object from the file location specified.  Returns null if
   * the file wasn't found.
   * @param loadFile the file
   * @param loadClass the class to be loaded
   * @param mapping the Mapping
   * @throws IOException
   * @throws MarshallingException
   * @return Object the unmarshalled object
   */
  public Object load(File loadFile, Class loadClass, Mapping mapping)
      throws IOException, MarshallingException {
    if (!loadFile.exists()) {
      logger.debug3("File '"+loadFile.getAbsolutePath()+"' not found.");
      return null;
    }

    FileReader reader = new FileReader(loadFile);
    return loadFromReader(reader, loadClass, mapping);
  }

  /**
   * Loads an Object from the Reader specified.
   * @param reader a Reader for the source
   * @param loadClass the class to be loaded
   * @param mapping the Mapping
   * @throws IOException
   * @throws MarshallingException
   * @return Object the unmarshalled object
   */
  public Object loadFromReader(Reader reader, Class loadClass, Mapping mapping)
      throws IOException, MarshallingException {
    Object obj = null;
    Unmarshaller unmarshaller = new Unmarshaller(loadClass);
    try {
      unmarshaller.setMapping(mapping);
      obj = unmarshaller.unmarshal(reader);
    } catch (org.exolab.castor.mapping.MappingException me) {
      logger.debug3(me.toString());
      throw new MarshallingException(me.getMessage());
    } catch (org.exolab.castor.xml.MarshalException me) {
      logger.debug3(me.toString());
      throw new MarshallingException(me.getMessage());
    } catch (org.exolab.castor.xml.ValidationException ve) {
      logger.debug3(ve.toString());
      throw new MarshallingException(ve.getMessage());
    } finally {
      reader.close();
    }
    return obj;
  }


  /**
   * Gets a Mapping from the cache, or creates one if needed (and caches it).
   * @param fileName the mapping file name
   * @return Mapping the Mapping class
   */
  public Mapping getMapping(String fileName) {
    Mapping mapping = (Mapping)mappingFileMap.get(fileName);
    if (mapping==null) {
      mapping = loadMappingFile(fileName);
      mappingFileMap.put(fileName, mapping);
    } else if (mapping.getRoot().getClassMappingCount()==0) {
      logger.error("Mapping file is empty.");
      throw new LockssAppException("Mapping file is empty.");
    }
    return mapping;
  }

  /**
   * Gets a Mapping from the cache, or creates one if needed (and caches it).
   * @param fileNames the mapping file names
   * @return Mapping the Mapping class
   */
  public Mapping getMapping(String[] fileNames) {
    Mapping mapping = (Mapping)mappingFileMap.get(fileNames);
    if (mapping==null) {
      mapping = loadMappingFile(fileNames);
      mappingFileMap.put(fileNames, mapping);
    } else if (mapping.getRoot().getClassMappingCount()==0) {
      logger.error("Mapping file is empty.");
      throw new LockssAppException("Mapping file is empty.");
    }
    return mapping;
  }

  /**
   * Loads a Mapping from a fileName.
   * @param fileName the mapping file name
   * @return Mapping the loaded Mapping
   */
  Mapping loadMappingFile(String fileName) {
    URL mappingLoc = getClass().getResource(fileName);
    if (mappingLoc==null) {
      logger.error("Couldn't find resource '"+fileName+"'");
      throw new LockssAppException("Couldn't find mapping file.");
    }
    Mapping mapping = new Mapping();
    try {
      mapping.loadMapping(mappingLoc);
      return mapping;
    } catch (Exception e) {
      logger.error("Couldn't load mapping file '"+mappingLoc+"'", e);
      throw new LockssAppException("Couldn't load mapping file.");
    }
  }

  /**
   * Loads a Mapping from a number of files.
   * @param fileNames the list of file names
   * @return Mapping the Mapping
   */
  Mapping loadMappingFile(String[] fileNames) {
    Mapping mapping = new Mapping();

    for (int ii=0; ii<fileNames.length; ii++) {
      URL mappingLoc = getClass().getResource(fileNames[ii]);
      if (mappingLoc == null) {
        logger.error("Couldn't find resource '" + fileNames[ii] + "'");
        throw new LockssAppException("Couldn't find mapping file.");
      }
      try {
        mapping.loadMapping(mappingLoc);
      } catch (Exception e) {
        logger.error("Couldn't load mapping file '" + mappingLoc + "'", e);
        throw new LockssAppException("Couldn't load mapping file.");
      }
    }
    return mapping;
  }

  public static class MarshallingException extends Exception {
    public MarshallingException(String msg) {
      super(msg);
    }
  }
}
