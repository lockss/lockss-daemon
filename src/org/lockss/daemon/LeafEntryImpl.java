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

package org.lockss.daemon;
import java.io.*;
import java.util.*;

/**
 * LeafEntry is a leaf-specific subclass of RepositoryEntry.
 */
public class LeafEntryImpl extends RepositoryEntryImpl implements LeafEntry {
  private boolean newVersionReady = false;
  private String versionName;
  private int currentVersion;

  public LeafEntryImpl(String url, String rootLocation) {
    super(url, rootLocation);
    currentVersion = loadCurrentVersion();
  }

  public int getCurrentVersion() {
    return currentVersion;
  }

  public boolean isLeaf() {
    return true;
  }

  public boolean exists() {
    return (currentVersion > 0);
  }

  public void makeNewVersion() {
    if (currentVersion == 0) {
      File cacheDir = getCacheLocation();
      if (!cacheDir.exists()) {
        cacheDir.mkdirs();
      }
    }
    currentVersion++;
    //XXX log if wasn't closed
    newVersionReady = true;
  }

  public void closeCurrentVersion() {
    newVersionReady = false;
  }

  public InputStream getInputStream() {
    File file = getCurrentCacheFile();
    try {
      if (file.exists()) {
        return new FileInputStream(file);
      }
    } catch (FileNotFoundException fnfe) {
      fnfe.printStackTrace();
    }
    return null;
  }

  public Properties getProperties() {
    try {
      File file = getCurrentPropertiesFile();
      InputStream is = new FileInputStream(file);
      Properties props = new Properties();
      props.load(is);
      is.close();
      return props;
    } catch (IOException e) {
      return null;
    }
  }

  public OutputStream getNewOutputStream() throws NoNewVersionException {
    if (!newVersionReady) {
      throw new NoNewVersionException("New version not initialized.");
    }
    try {
      File file = getCurrentCacheFile();
      return new FileOutputStream(file);
    } catch (FileNotFoundException fnfe) {
      System.out.println(fnfe);
      return null;
    }
  }

  public void setNewProperties(Properties newProps) throws NoNewVersionException {
    if (!newVersionReady) {
      throw new NoNewVersionException("New version not initialized.");
    }
    if (newProps!=null) {
      try {
        File file = getCurrentPropertiesFile();
        OutputStream os = new FileOutputStream(file);
        newProps.store(os, "HTTP headers for " + url);
        os.close();
      } catch (Exception e) { System.out.println(e); }
    }
  }

  private int loadCurrentVersion() {
    File cacheDir = getCacheLocation();
    versionName = cacheDir.getName();
    if (!cacheDir.exists()) {
      return 0;
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
    return versionFound;
  }

  private File getCurrentCacheFile() {
    String cacheName = mapUrlToCacheLocation() + File.separator +
                       versionName + "." + currentVersion;
    return new File(cacheName);
  }

  private File getCurrentPropertiesFile() {
    String propName = mapUrlToCacheLocation() + File.separator + versionName +
                      ".props." + currentVersion;
    return new File(propName);
  }

  private File getCacheLocation() {
    return new File(LockssRepositoryImpl.mapUrlToCacheLocation(url));
  }

  private String mapUrlToCacheLocation() {
    return rootLocation + LockssRepositoryImpl.mapUrlToCacheLocation(url);

  }
}
