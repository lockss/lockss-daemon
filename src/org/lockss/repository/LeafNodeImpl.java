/*
 * $Id: LeafNodeImpl.java,v 1.4 2002-11-06 00:01:30 aalto Exp $
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
  private InputStream curInput;
  private Properties curProps;
  private String versionName;
  private int currentVersion = -1;
  private StringBuffer buffer;

  public LeafNodeImpl(String url, String cacheLocation, LockssRepositoryImpl repository) {
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
      File cacheDir = getCacheLocation();
      if (!cacheDir.exists()) {
        cacheDir.mkdirs();
      }
      File leafFile = new File(cacheDir, LEAF_FILE_NAME);
      try {
        leafFile.createNewFile();
      } catch (IOException ioe) {
        logger.error("Couldn't create leaf file for " +
                     cacheDir.getAbsolutePath()+".");
      }
    }
    newVersionOpen = true;
  }

  public void sealNewVersion() {
    if (!newVersionOpen) {
      throw new UnsupportedOperationException("New version not initialized.");
    }
    synchronized (this) {
      File curContentF = getCurrentCacheFile();
      File curPropsF = getCurrentPropertiesFile();
      File newContentF = getTempCacheFile();
      File newPropsF = getTempPropertiesFile();

      // rename current
      curContentF.renameTo(getVersionedCacheFile(currentVersion));
      curPropsF.renameTo(getVersionedPropertiesFile(currentVersion));
      // rename new
      newContentF.renameTo(getCurrentCacheFile());
      newPropsF.renameTo(getCurrentPropertiesFile());

      currentVersion++;
      newVersionOutput = null;
      curInput = null;
      curProps = null;
      newVersionOpen = false;
    }
  }

  public InputStream getInputStream() {
    ensureCurrentVersionLoaded();
    ensureReadInfoLoaded();
    return curInput;
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
    File file = getTempCacheFile();
    try {
      newVersionOutput = new FileOutputStream(file);
      return newVersionOutput;
    } catch (FileNotFoundException fnfe) {
      logger.error("No new version file for "+file.getAbsolutePath()+".");
      return null;
    }
  }

  public void setNewProperties(Properties newProps) {
    if (!newVersionOpen) {
      throw new UnsupportedOperationException("New version not initialized.");
    }
    if (newProps!=null) {
      File file = getTempPropertiesFile();
      try {
        OutputStream os = new FileOutputStream(file);
        newProps.setProperty("version_number", ""+(currentVersion+1));
        newProps.store(os, "HTTP headers for " + url);
        os.close();
      } catch (IOException ioe) {
        logger.error("Couldn't write properties for " +
                     file.getAbsolutePath()+".");
      }
    }
  }

  private void ensureReadInfoLoaded() {
    if (currentVersion==0) {
      curInput = null;
      curProps = new Properties();
      return;
    }
    if ((curInput==null) || (curProps==null)) {
      synchronized (this) {
        File file = null;
        if (curInput==null) {
          file = getCurrentCacheFile();
          try {
            curInput = new FileInputStream(file);
          } catch (FileNotFoundException fnfe) {
            logger.error("No inputstream for "+file.getAbsolutePath()+".");
          }
        }
        if (curProps==null) {
          file = getCurrentPropertiesFile();
          try {
            InputStream is = new FileInputStream(file);
            curProps = new Properties();
            curProps.load(is);
            is.close();
          } catch (IOException e) {
            logger.error("No properties file for "+file.getAbsolutePath()+".");
            curProps = new Properties();
          }
        }
      }
    }
  }

  private void ensureCurrentVersionLoaded() {
    if (currentVersion!=-1) return;
    File cacheDir = getCacheLocation();
    versionName = cacheDir.getName();
    if (!cacheDir.exists()) {
      currentVersion = 0;
      return;
    }
    //XXX getting version from props probably a mistake
    if (curProps==null) {
      synchronized (this) {
        File curPropsFile = getCurrentPropertiesFile();
        if (curPropsFile.exists()) {
          try {
            InputStream is = new FileInputStream(curPropsFile);
            curProps = new Properties();
            curProps.load(is);
            is.close();
          } catch (Exception e) {
            logger.error("Error loading version from "+
                          curPropsFile.getAbsolutePath()+".");
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
    buffer = new StringBuffer(cacheLocation);
    buffer.append(File.separator);
    buffer.append(versionName);
    buffer.append(CURRENT_SUFFIX);
    return new File(buffer.toString());
  }

  private File getCurrentPropertiesFile() {
    buffer = new StringBuffer(cacheLocation);
    buffer.append(File.separator);
    buffer.append(versionName);
    buffer.append(PROPS_SUFFIX);
    buffer.append(CURRENT_SUFFIX);
    return new File(buffer.toString());
  }

  private File getTempCacheFile() {
    buffer = new StringBuffer(cacheLocation);
    buffer.append(File.separator);
    buffer.append(versionName);
    buffer.append(TEMP_SUFFIX);
    return new File(buffer.toString());
  }

  private File getTempPropertiesFile() {
    buffer = new StringBuffer(cacheLocation);
    buffer.append(File.separator);
    buffer.append(versionName);
    buffer.append(PROPS_SUFFIX);
    buffer.append(TEMP_SUFFIX);
    return new File(buffer.toString());
  }

  private File getVersionedCacheFile(int version) {
    buffer = new StringBuffer(cacheLocation);
    buffer.append(File.separator);
    buffer.append(versionName);
    buffer.append(".");
    buffer.append(version);
    return new File(buffer.toString());
  }

  private File getVersionedPropertiesFile(int version) {
    buffer = new StringBuffer(cacheLocation);
    buffer.append(File.separator);
    buffer.append(versionName);
    buffer.append(PROPS_SUFFIX);
    buffer.append(".");
    buffer.append(version);
    return new File(buffer.toString());
  }

  private File getCacheLocation() {
    return new File(cacheLocation);
  }
}
