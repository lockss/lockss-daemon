/*
 * $Id: LeafNodeImpl.java,v 1.5 2002-11-07 02:21:48 aalto Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.repository;
import java.io.*;
import java.util.*;

/**
 * LeafNodeImpl is a leaf-specific subclass of RepositoryNodeImpl.
 */

//XXX the synchronization issues are unresolved.  How can you be sure that
// a call to getInputStream() and getProperties() can't get out of sync?
public class LeafNodeImpl extends RepositoryNodeImpl implements LeafNode {
  /**
   * The name of the file created in leaf cache directories.
   */
  public static final String LEAF_FILE_NAME = "isLeaf";

  private static final String CURRENT_SUFFIX = ".current";
  private static final String PROPS_SUFFIX = ".props";
  private static final String TEMP_SUFFIX = ".temp";

  private boolean newVersionOpen = false;
  private OutputStream newVersionOutput;
  private File curInputFile;
  private Properties curProps;
  private String versionName;
  private int currentVersion = -1;

  private File cacheLocationFile;
  private File currentCacheFile;
  private File currentPropsFile;
  private File tempCacheFile;
  private File tempPropsFile;

  public LeafNodeImpl(String url, String cacheLocation,
                      LockssRepositoryImpl repository) {
    super(url, cacheLocation, repository);
  }

  public int getCurrentVersion() {
    ensureCurrentVersionLoaded();
    return currentVersion;
  }

  public boolean isLeaf() {
    return true;
  }

  public boolean exists() {
    ensureCurrentVersionLoaded();
    return (currentVersion > 0);
  }

  public void makeNewVersion() {
    if (newVersionOpen) {
      throw new UnsupportedOperationException("New version already"+
                                              " initialized.");
    }
    ensureCurrentVersionLoaded();
    if (currentVersion == 0) {
      if (!cacheLocationFile.exists()) {
        cacheLocationFile.mkdirs();
      }
      File leafFile = new File(cacheLocationFile, LEAF_FILE_NAME);
      try {
        leafFile.createNewFile();
      } catch (IOException ioe) {
        logger.error("Couldn't create leaf file for " +
                     cacheLocationFile.getAbsolutePath()+".");
      }
    }
    newVersionOpen = true;
  }

  public void sealNewVersion() {
    if (!newVersionOpen) {
      throw new UnsupportedOperationException("New version not initialized.");
    }
    synchronized (this) {
      // rename current
      currentCacheFile.renameTo(getVersionedCacheFile(currentVersion));
      currentPropsFile.renameTo(getVersionedPropertiesFile(currentVersion));
      // rename new
      tempCacheFile.renameTo(currentCacheFile);
      tempPropsFile.renameTo(currentPropsFile);

      currentVersion++;
      newVersionOutput = null;
      curInputFile = null;
      curProps = null;
      newVersionOpen = false;
    }
  }

  public InputStream getInputStream() {
    ensureCurrentVersionLoaded();
    ensureReadInfoLoaded();
    try {
      return new FileInputStream(curInputFile);
    } catch (FileNotFoundException fnfe) {
      logger.error("Couldn't get inputstream for '"+curInputFile.getAbsolutePath()+"'");
      return null;
    }
  }

  public Properties getProperties() {
    ensureCurrentVersionLoaded();
    ensureReadInfoLoaded();
    return curProps;
  }

  public OutputStream getNewOutputStream() {
    if (!newVersionOpen) {
      throw new UnsupportedOperationException("New version not initialized.");
    }
    if (newVersionOutput!=null) {
      return newVersionOutput;
    }
    try {
      newVersionOutput = new FileOutputStream(tempCacheFile);
      return newVersionOutput;
    } catch (FileNotFoundException fnfe) {
      logger.error("No new version file for "+tempCacheFile.getAbsolutePath()+".");
      return null;
    }
  }

  public void setNewProperties(Properties newProps) {
    if (!newVersionOpen) {
      throw new UnsupportedOperationException("New version not initialized.");
    }
    if (newProps!=null) {
      try {
        OutputStream os = new FileOutputStream(tempPropsFile);
        newProps.setProperty("version_number", ""+(currentVersion+1));
        newProps.store(os, "HTTP headers for " + url);
        os.close();
      } catch (IOException ioe) {
        logger.error("Couldn't write properties for " +
                     tempPropsFile.getAbsolutePath()+".");
      }
    }
  }

  private void ensureReadInfoLoaded() {
    if (currentVersion==0) {
      curInputFile = null;
      curProps = new Properties();
      return;
    }
    if ((curInputFile==null) || (curProps==null)) {
      synchronized (this) {
        if (curInputFile==null) {
          curInputFile = currentCacheFile;
        }
        if (curProps==null) {
          try {
            InputStream is = new FileInputStream(currentPropsFile);
            curProps = new Properties();
            curProps.load(is);
            is.close();
          } catch (IOException e) {
            logger.error("No properties file for "+currentPropsFile.getAbsolutePath()+".");
            curProps = new Properties();
          }
        }
      }
    }
  }

  private void ensureCurrentVersionLoaded() {
    if (currentVersion!=-1) return;
    cacheLocationFile = getCacheLocation();
    versionName = cacheLocationFile.getName();
    currentCacheFile = getCurrentCacheFile();
    currentPropsFile = getCurrentPropsFile();
    tempCacheFile = getTempCacheFile();
    tempPropsFile = getTempPropsFile();
    if (!cacheLocationFile.exists()) {
      currentVersion = 0;
      return;
    }
    //XXX getting version from props probably a mistake
    if (curProps==null) {
      synchronized (this) {
        if (currentPropsFile.exists()) {
          try {
            InputStream is = new FileInputStream(currentPropsFile);
            curProps = new Properties();
            curProps.load(is);
            is.close();
          } catch (Exception e) {
            logger.error("Error loading version from "+
                          currentPropsFile.getAbsolutePath()+".");
            curProps = new Properties();
          }
        } else {
          curProps = new Properties();
        }
      }
    }
    currentVersion = Integer.parseInt(
                     curProps.getProperty("version_number", "0"));
  }

  private File getCurrentCacheFile() {
    if (currentCacheFile==null) {
      StringBuffer buffer = new StringBuffer(cacheLocation);
      buffer.append(File.separator);
      buffer.append(versionName);
      buffer.append(CURRENT_SUFFIX);
      currentCacheFile = new File(buffer.toString());
    }
    return currentCacheFile;
  }

  private File getCurrentPropsFile() {
    if (currentPropsFile==null) {
      StringBuffer buffer = new StringBuffer(cacheLocation);
      buffer.append(File.separator);
      buffer.append(versionName);
      buffer.append(PROPS_SUFFIX);
      buffer.append(CURRENT_SUFFIX);
      currentPropsFile = new File(buffer.toString());
    }
    return currentPropsFile;
  }

  private File getTempCacheFile() {
    if (tempCacheFile==null) {
      StringBuffer buffer = new StringBuffer(cacheLocation);
      buffer.append(File.separator);
      buffer.append(versionName);
      buffer.append(TEMP_SUFFIX);
      tempCacheFile = new File(buffer.toString());
    }
    return tempCacheFile;
  }

  private File getTempPropsFile() {
    if (tempPropsFile==null) {
      StringBuffer buffer = new StringBuffer(cacheLocation);
      buffer.append(File.separator);
      buffer.append(versionName);
      buffer.append(PROPS_SUFFIX);
      buffer.append(TEMP_SUFFIX);
      tempPropsFile = new File(buffer.toString());
    }
    return tempPropsFile;
  }

  private File getVersionedCacheFile(int version) {
    StringBuffer buffer = new StringBuffer(cacheLocation);
    buffer.append(File.separator);
    buffer.append(versionName);
    buffer.append(".");
    buffer.append(version);
    return new File(buffer.toString());
  }

  private File getVersionedPropertiesFile(int version) {
    StringBuffer buffer = new StringBuffer(cacheLocation);
    buffer.append(File.separator);
    buffer.append(versionName);
    buffer.append(PROPS_SUFFIX);
    buffer.append(".");
    buffer.append(version);
    return new File(buffer.toString());
  }

  private File getCacheLocation() {
    if (cacheLocationFile==null) {
      cacheLocationFile = new File(cacheLocation);
    }
    return cacheLocationFile;
  }
}
