/*
 * $Id: LeafNodeImpl.java,v 1.1 2002-10-31 01:52:41 aalto Exp $
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

  private boolean newVersionOpen = false;
  private OutputStream newVersionOutput;
  private String versionName;
  private int currentVersion = -1;

  public LeafNodeImpl(String url, String cacheLocation) {
    super(url, cacheLocation);
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
    //XXX use constant version name
    if (newVersionOpen) {
      throw new UnsupportedOperationException("New version already initialized.");
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
      } catch (IOException ioe) { System.out.println(ioe); }
    }
    newVersionOpen = true;
  }

  public void sealNewVersion() {
//XXX use constant version name
    if (!newVersionOpen) {
      throw new UnsupportedOperationException("New version not initialized.");
    }
    newVersionOutput = null;
    currentVersion++;
    newVersionOpen = false;
  }

  public InputStream getInputStream() {
    ensureCurrentVersionLoaded();
    File file = getCurrentCacheFile();
    try {
      return new FileInputStream(file);
    } catch (FileNotFoundException fnfe) { }
    return null;
  }

  public Properties getProperties() {
    ensureCurrentVersionLoaded();
    try {
      File file = getCurrentPropertiesFile();
      InputStream is = new FileInputStream(file);
      Properties props = new Properties();
      props.load(is);
      is.close();
      return props;
    } catch (IOException e) {
      return new Properties();
    }
  }

  public OutputStream getNewOutputStream() {
    if (!newVersionOpen) {
      throw new UnsupportedOperationException("New version not initialized.");
    }
    if (newVersionOutput!=null) {
      return newVersionOutput;
    }
    try {
      File file = getNewVersionCacheFile();
      newVersionOutput = new FileOutputStream(file);
      return newVersionOutput;
    } catch (FileNotFoundException fnfe) {
      System.out.println(fnfe);
      return null;
    }
  }

  public void setNewProperties(Properties newProps) {
    if (!newVersionOpen) {
      throw new UnsupportedOperationException("New version not initialized.");
    }
    if (newProps!=null) {
      try {
        File file = getNewVersionPropertiesFile();
        OutputStream os = new FileOutputStream(file);
        newProps.store(os, "HTTP headers for " + url);
        os.close();
      } catch (Exception e) { System.out.println(e); }
    }
  }

  private void ensureCurrentVersionLoaded() {
    //XXX use constant version name
    if (currentVersion!=-1) return;
    File cacheDir = getCacheLocation();
    versionName = cacheDir.getName();
    if (!cacheDir.exists()) {
      currentVersion = 0;
      return;
    }
    int versionFound = 0;
    File[] children = cacheDir.listFiles();
    for (int ii=0; ii<children.length; ii++) {
      String fileName = children[ii].getName();
      if (fileName.startsWith(versionName)) {
        int idx = fileName.lastIndexOf(".");
        try {
          int version = Integer.parseInt(fileName.substring(idx+1));
          if (version>versionFound) versionFound = version;
        } catch (NumberFormatException ex) {
          System.out.println("Bad filename: "+fileName);
        }
      }
    }
    currentVersion = versionFound;
  }

  private File getCurrentCacheFile() {
    String cacheName = cacheLocation + File.separator +
                       versionName + "." + currentVersion;
    return new File(cacheName);
  }

  private File getCurrentPropertiesFile() {
    String propName = cacheLocation + File.separator + versionName +
                      ".props." + currentVersion;
    return new File(propName);
  }

  private File getNewVersionCacheFile() {
    String cacheName = cacheLocation + File.separator +
                       versionName + "." + (currentVersion+1);
    return new File(cacheName);
  }

  private File getNewVersionPropertiesFile() {
    String propName = cacheLocation + File.separator + versionName +
                      ".props." + (currentVersion+1);
    return new File(propName);
  }

  private File getCacheLocation() {
    return new File(cacheLocation);
  }
}
