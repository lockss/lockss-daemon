/*
 * $Id: RepositoryNodeImpl.java,v 1.4 2002-11-20 01:18:58 aalto Exp $
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
import java.util.*;
import org.lockss.util.Logger;
import java.io.*;
import org.lockss.daemon.CachedUrlSetSpec;
import java.net.*;

/**
 * RepositoryNode is used to store the contents and
 * meta-information of urls being cached.
 */
public class RepositoryNodeImpl implements RepositoryNode {
  private static final String CONTENT_DIR_SUFFIX = ".content";
  private static final String CURRENT_SUFFIX = ".current";
  private static final String PROPS_SUFFIX = ".props";
  private static final String TEMP_SUFFIX = ".temp";

  private boolean newVersionOpen = false;
  private OutputStream newVersionOutput;
  private File curInputFile;
  private Properties curProps;
  private String versionName;
  private int currentVersion = -1;

  private File nodeRootFile = null;
  private File cacheLocationFile;
  private File currentCacheFile;
  private File currentPropsFile;
  private File tempCacheFile;
  private File tempPropsFile;

  private String url;
  private String nodeLocation;
  private static Logger logger = Logger.getLogger("RepositoryNode",
                                                    Logger.LEVEL_DEBUG);
  private LockssRepositoryImpl repository;

  protected RepositoryNodeImpl(String url, String nodeLocation,
                       LockssRepositoryImpl repository) {
    this.url = url;
    this.nodeLocation = nodeLocation;
    this.repository = repository;
  }

  public void finalize() {
    if (repository!=null) {
      repository.removeReference(url);
    }
  }

  public String getNodeUrl() {
    return url;
  }

  public boolean hasContent() {
    ensureCurrentVersionLoaded();
    return getCurrentCacheFile().exists();
  }

  public Properties getState() {
    //XXX implement
    return null;
  }

  public void storeState(Properties newProps) {
    //XXX implement
  }

  public Iterator listNodes(CachedUrlSetSpec filter) {
    if (nodeRootFile==null) getNodeRoot();
    if (!nodeRootFile.exists()) {
      return (new Vector()).iterator();
    }
    File[] children = nodeRootFile.listFiles();
    Arrays.sort(children, new FileComparator());
    Vector childV = new Vector();
    for (int ii=0; ii<children.length; ii++) {
      File child = children[ii];
      if (!child.isDirectory()) continue;
      if (child.getName().equals(getCacheLocation().getName())) continue;
      StringBuffer buffer = new StringBuffer(this.url);
      if (!url.endsWith(File.separator)) buffer.append(File.separator);
      buffer.append(child.getName());

      String childUrl = buffer.toString();
      if ((filter==null) || (filter.matches(childUrl))) {
        try {
          childV.addElement(repository.getRepositoryNode(childUrl));
        } catch (MalformedURLException mue) {
          logger.error("Malformed child url: "+childUrl);
        }
      }
    }
    return childV.iterator();
  }

  public int getCurrentVersion() {
    if (hasContent()) {
      return currentVersion;
    } else return 0;
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

  public void abandonNewVersion() {
    if (!newVersionOpen) {
      throw new UnsupportedOperationException("New version not initialized.");
    }
    synchronized (this) {
      // clear temp files
      tempCacheFile.delete();
      tempPropsFile.delete();

      newVersionOutput = null;
      newVersionOpen = false;
    }
  }


  public InputStream getInputStream() {
    if (!hasContent()) {
      throw new UnsupportedOperationException("No content for url '"+url+"'");
    }
    ensureReadInfoLoaded();
    try {
      return new BufferedInputStream(new FileInputStream(curInputFile));
    } catch (FileNotFoundException fnfe) {
      logger.error("Couldn't get inputstream for '"+curInputFile.getAbsolutePath()+"'");
      return null;
    }
  }

  public Properties getProperties() {
    if (!hasContent()) {
      throw new UnsupportedOperationException("No content for url '"+url+"'");
    }
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
      newVersionOutput = new BufferedOutputStream(new FileOutputStream(tempCacheFile));
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
        OutputStream os = new BufferedOutputStream(new FileOutputStream(tempPropsFile));
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
            InputStream is = new BufferedInputStream(new FileInputStream(currentPropsFile));
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
            InputStream is = new BufferedInputStream(new FileInputStream(currentPropsFile));
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
      StringBuffer buffer = new StringBuffer(nodeLocation);
      buffer.append(File.separator);
      buffer.append(nodeLocation);
      buffer.append(CONTENT_DIR_SUFFIX);
      buffer.append(File.separator);
      buffer.append(versionName);
      buffer.append(CURRENT_SUFFIX);
      currentCacheFile = new File(buffer.toString());
    }
    return currentCacheFile;
  }

  private File getCurrentPropsFile() {
    if (currentPropsFile==null) {
      StringBuffer buffer = new StringBuffer(nodeLocation);
      buffer.append(File.separator);
      buffer.append(nodeLocation);
      buffer.append(CONTENT_DIR_SUFFIX);
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
      StringBuffer buffer = new StringBuffer(nodeLocation);
      buffer.append(File.separator);
      buffer.append(nodeLocation);
      buffer.append(CONTENT_DIR_SUFFIX);
      buffer.append(File.separator);
      buffer.append(versionName);
      buffer.append(TEMP_SUFFIX);
      tempCacheFile = new File(buffer.toString());
    }
    return tempCacheFile;
  }

  private File getTempPropsFile() {
    if (tempPropsFile==null) {
      StringBuffer buffer = new StringBuffer(nodeLocation);
      buffer.append(File.separator);
      buffer.append(nodeLocation);
      buffer.append(CONTENT_DIR_SUFFIX);
      buffer.append(File.separator);
      buffer.append(versionName);
      buffer.append(PROPS_SUFFIX);
      buffer.append(TEMP_SUFFIX);
      tempPropsFile = new File(buffer.toString());
    }
    return tempPropsFile;
  }

  private File getVersionedCacheFile(int version) {
    StringBuffer buffer = new StringBuffer(nodeLocation);
    buffer.append(File.separator);
    buffer.append(nodeLocation);
    buffer.append(CONTENT_DIR_SUFFIX);
    buffer.append(File.separator);
    buffer.append(versionName);
    buffer.append(".");
    buffer.append(version);
    return new File(buffer.toString());
  }

  private File getVersionedPropertiesFile(int version) {
    StringBuffer buffer = new StringBuffer(nodeLocation);
    buffer.append(File.separator);
    buffer.append(nodeLocation);
    buffer.append(CONTENT_DIR_SUFFIX);
    buffer.append(File.separator);
    buffer.append(versionName);
    buffer.append(PROPS_SUFFIX);
    buffer.append(".");
    buffer.append(version);
    return new File(buffer.toString());
  }

  private File getCacheLocation() {
    if (cacheLocationFile==null) {
      StringBuffer buffer = new StringBuffer(nodeLocation);
      buffer.append(File.separator);
      buffer.append(nodeLocation);
      buffer.append(CONTENT_DIR_SUFFIX);
      cacheLocationFile = new File(buffer.toString());
    }
    return cacheLocationFile;
  }

  private File getNodeRoot() {
    if (nodeRootFile==null) {
      nodeRootFile = new File(nodeLocation);
    }
    return nodeRootFile;
  }

  private class FileComparator implements Comparator {
    public int compare(Object o1, Object o2) {
      if ((o1 instanceof File) && (o2 instanceof File)) {
        return ((File)o1).compareTo((File)o2);
      } else return -1;
    }
  }
}
