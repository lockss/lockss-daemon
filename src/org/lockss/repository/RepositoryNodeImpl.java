/*
 * $Id: RepositoryNodeImpl.java,v 1.6 2002-11-23 03:40:49 aalto Exp $
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
import java.net.MalformedURLException;
import org.lockss.util.Logger;
import org.lockss.daemon.CachedUrlSetSpec;

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
  private boolean newOutputCalled = false;
  private boolean newPropsSet = false;
  private File curInputFile;
  private Properties curProps;
  private String versionName;
  private int currentVersion = -1;

  private String contentBufferStr = null;
  private File nodeRootFile = null;
  private File cacheLocationFile;
  private File currentCacheFile;
  private File currentPropsFile;
  private File tempCacheFile;
  private File tempPropsFile;

  private String url;
  private String nodeLocation;
  private static Logger logger = Logger.getLogger("RepositoryNode");
  private LockssRepositoryImpl repository;

  RepositoryNodeImpl(String url, String nodeLocation,
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
    ensureCurrentInfoLoaded();
    return currentVersion>0;
  }

  public Properties getState() {
    //XXX implement
    return null;
  }

  public void storeState(Properties newProps) {
    //XXX implement
  }

  public Iterator listNodes(CachedUrlSetSpec filter) {
    if (nodeRootFile==null) loadNodeRoot();
    if (!nodeRootFile.exists()) {
      logger.error("No cache directory located for: "+url);
      throw new RepositoryStateException("No cache directory located.");
    }
    if (cacheLocationFile==null) loadCacheLocation();
    File[] children = nodeRootFile.listFiles();
    // sorts alphabetically relying on File.compareTo()
    Arrays.sort(children, new FileComparator());
    ArrayList childL = new ArrayList(Math.min(40, children.length));
    for (int ii=0; ii<children.length; ii++) {
      File child = children[ii];
      if (!child.isDirectory()) continue;
      if (child.getName().equals(cacheLocationFile.getName())) continue;
      int bufMaxLength = url.length() + child.getName().length() + 1;
      StringBuffer buffer = new StringBuffer(bufMaxLength);
      buffer.append(url);
      if (!url.endsWith(File.separator)) {
        buffer.append(File.separator);
      }
      buffer.append(child.getName());

      String childUrl = buffer.toString();
      if ((filter==null) || (filter.matches(childUrl))) {
        try {
          childL.add(repository.getRepositoryNode(childUrl));
        } catch (MalformedURLException mue) {
          // this can safely skip bad files because they will
          // eventually be trimmed by the repository integrity checker
          // and the content will be replaced by a poll repair
          logger.error("Malformed child url: "+childUrl);
        }
      }
    }
    return childL.iterator();
  }

  public int getCurrentVersion() {
    if (!hasContent()) {
      logger.error("Cannot get version if no content: "+url);
      throw new UnsupportedOperationException("No content to version.");
    }
    return currentVersion;
  }

  public synchronized void makeNewVersion() {
    if (newVersionOpen) {
      throw new UnsupportedOperationException("New version already"+
                                              " initialized.");
    }
    ensureCurrentInfoLoaded();
    if (currentVersion == 0) {
      if (!cacheLocationFile.exists()) {
        cacheLocationFile.mkdirs();
      }
    }
    newVersionOpen = true;
  }

  public synchronized void sealNewVersion() {
    if (!newVersionOpen) {
      throw new UnsupportedOperationException("New version not initialized.");
    }
    if (!newOutputCalled) {
      throw new UnsupportedOperationException("getNewOutputStream() not called.");
    }
    if (!newPropsSet) {
      throw new UnsupportedOperationException("setNewProperties() not called.");
    }

    // rename current
    currentCacheFile.renameTo(getVersionedCacheFile(currentVersion));
    currentPropsFile.renameTo(getVersionedPropertiesFile(currentVersion));
    // rename new
    tempCacheFile.renameTo(currentCacheFile);
    tempPropsFile.renameTo(currentPropsFile);

    currentVersion++;
    curProps = null;
    newOutputCalled = false;
    newPropsSet = false;
    newVersionOpen = false;
  }

  public synchronized void abandonNewVersion() {
    if (!newVersionOpen) {
      throw new UnsupportedOperationException("New version not initialized.");
    }
    // clear temp files
    tempCacheFile.delete();
    tempPropsFile.delete();

    newOutputCalled = false;
    newPropsSet = false;
    newVersionOpen = false;
  }


  public synchronized RepositoryNodeContents getNodeContents() {
    if (!hasContent()) {
      throw new UnsupportedOperationException("No content for url '"+url+"'");
    }
    try {
      InputStream is = new BufferedInputStream(new FileInputStream(curInputFile));
      return new RepositoryNodeContents(is, curProps);
    } catch (FileNotFoundException fnfe) {
      logger.error("Couldn't get inputstream for '"+curInputFile.getAbsolutePath()+"'");
      throw new RepositoryStateException("Couldn't get info for node.");
    }
  }

  public OutputStream getNewOutputStream() {
    if (!newVersionOpen) {
      throw new UnsupportedOperationException("New version not initialized.");
    }
    if (newOutputCalled) {
      throw new UnsupportedOperationException("getNewOutputStream() called twice.");
    }
    newOutputCalled = true;
    try {
      return new BufferedOutputStream(new FileOutputStream(tempCacheFile));
    } catch (FileNotFoundException fnfe) {
      logger.error("No new version file for "+tempCacheFile.getAbsolutePath()+".");
      throw new RepositoryStateException("Couldn't load new outputstream.");
    }
  }

  public void setNewProperties(Properties newProps) {
    if (!newVersionOpen) {
      throw new UnsupportedOperationException("New version not initialized.");
    }
    if (newPropsSet) {
      throw new UnsupportedOperationException("setNewProperties() called twice.");
    }
    newPropsSet = true;
    if (newProps!=null) {
      try {
        OutputStream os = new BufferedOutputStream(new FileOutputStream(tempPropsFile));
        newProps.setProperty("lockss_version_number", ""+(currentVersion+1));
        newProps.store(os, "HTTP headers for " + url);
        os.close();
      } catch (IOException ioe) {
        logger.error("Couldn't write properties for " +
                     tempPropsFile.getAbsolutePath()+".");
        throw new RepositoryStateException("Couldn't write properties file.");
      }
    }
  }

  private void ensureCurrentInfoLoaded() {
    if (currentVersion==-1) {
      loadCacheLocation();
      loadCurrentCacheFile();
      loadCurrentPropsFile();
      loadTempCacheFile();
      loadTempPropsFile();
      versionName = cacheLocationFile.getName();
    }
    if (!cacheLocationFile.exists()) {
      currentVersion = 0;
      curInputFile = null;
      curProps = null;
      return;
    }
    //XXX getting version from props probably a mistake
    if ((curInputFile==null) || (curProps==null)) {
      synchronized (this) {
        if (curInputFile==null) {
          curInputFile = currentCacheFile;
        }
        if (curProps==null) {
          if (currentPropsFile.exists()) {
            try {
              InputStream is = new BufferedInputStream(new FileInputStream(currentPropsFile));
              curProps = new Properties();
//XXX load immutably
              curProps.load(is);
              is.close();
            } catch (Exception e) {
              logger.error("Error loading version from "+
                            currentPropsFile.getAbsolutePath()+".");
              throw new RepositoryStateException("Couldn't load version from properties file.");
            }
          }
        }
      }
      if (curProps!=null) {
        currentVersion = Integer.parseInt(
                       curProps.getProperty("lockss_version_number", "-1"));
      }
    }
  }

  private void loadCurrentCacheFile() {
    StringBuffer buffer = getContentDirBuffer();
    buffer.append(versionName);
    buffer.append(CURRENT_SUFFIX);
    currentCacheFile = new File(buffer.toString());
  }

  private void loadCurrentPropsFile() {
    StringBuffer buffer = getContentDirBuffer();
    buffer.append(versionName);
    buffer.append(PROPS_SUFFIX);
    buffer.append(CURRENT_SUFFIX);
    currentPropsFile = new File(buffer.toString());
  }

  private void loadTempCacheFile() {
    StringBuffer buffer = getContentDirBuffer();
    buffer.append(versionName);
    buffer.append(TEMP_SUFFIX);
    tempCacheFile = new File(buffer.toString());
  }

  private void loadTempPropsFile() {
    StringBuffer buffer = getContentDirBuffer();
    buffer.append(versionName);
    buffer.append(PROPS_SUFFIX);
    buffer.append(TEMP_SUFFIX);
    tempPropsFile = new File(buffer.toString());
  }

  private void loadCacheLocation() {
    cacheLocationFile = new File(getContentDirBuffer().toString());
  }

  private void loadNodeRoot() {
    nodeRootFile = new File(nodeLocation);
  }

  private File getVersionedCacheFile(int version) {
    StringBuffer buffer = getContentDirBuffer();
    buffer.append(versionName);
    buffer.append(".");
    buffer.append(version);
    return new File(buffer.toString());
  }

  private File getVersionedPropertiesFile(int version) {
    StringBuffer buffer = getContentDirBuffer();
    buffer.append(versionName);
    buffer.append(PROPS_SUFFIX);
    buffer.append(".");
    buffer.append(version);
    return new File(buffer.toString());
  }

  private StringBuffer getContentDirBuffer() {
    if (contentBufferStr==null) {
      StringBuffer buffer = new StringBuffer(nodeLocation);
      buffer.append(File.separator);
      buffer.append(nodeLocation);
      buffer.append(CONTENT_DIR_SUFFIX);
      buffer.append(File.separator);
      contentBufferStr = buffer.toString();
    }
    return new StringBuffer(contentBufferStr);
  }

  private class FileComparator implements Comparator {
    public int compare(Object o1, Object o2) {
      // compares file pathnames
      return ((File)o1).getName().compareToIgnoreCase(((File)o2).getName());
    }
  }

  public class RepositoryStateException extends RuntimeException {
    public RepositoryStateException(String msg) {
      super(msg);
    }
  }
}
