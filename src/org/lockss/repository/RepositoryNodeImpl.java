/*
 * $Id: RepositoryNodeImpl.java,v 1.32 2003-09-05 22:46:58 eaalto Exp $
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

package org.lockss.repository;

import java.io.*;
import java.util.*;
import java.net.MalformedURLException;
import org.lockss.util.*;
import org.lockss.daemon.CachedUrlSetSpec;

/**
 * RepositoryNode is used to store the contents and
 * meta-information of urls being cached.
 */
public class RepositoryNodeImpl implements RepositoryNode {
  static final long VERSION_TIMEOUT = 5 * Constants.HOUR; // 5 hours
  static final String LOCKSS_VERSION_NUMBER = "org.lockss.version.number";
  static final String INACTIVE_CONTENT_PROPERTY = "node.content.isInactive";
  static final String DELETION_PROPERTY = "node.isDeleted";
  static final String CONTENT_DIR = "#content";
  static final String CURRENT_FILENAME = "current";
  static final String PROPS_FILENAME = "props";
  static final String NODE_PROPS_FILENAME = "node_props";
  static final String TEMP_FILENAME = "temp";
  static final String INACTIVE_FILENAME = "inactive";
  static final int INACTIVE_VERSION = -99;
  static final int DELETED_VERSION = -98;

  private boolean newVersionOpen = false;
  private boolean newOutputCalled = false;
  private boolean newPropsSet = false;
  private File curInputFile;
  protected Properties curProps;
  protected Properties nodeProps = new Properties();
  protected int currentVersion = -1;

  private String contentBufferStr = null;
  protected File nodeRootFile = null;
  protected File nodePropsFile = null;
  protected File cacheLocationFile;
  protected File currentCacheFile;
  protected File currentPropsFile;
  private File tempCacheFile;
  private File tempPropsFile;

  protected String url;
  protected String nodeLocation;
  protected static Logger logger = Logger.getLogger("RepositoryNode");
  protected LockssRepositoryImpl repository;
  // preset so testIllegalOperations() doesn't null pointer
  private Deadline versionTimeout = Deadline.MAX;

  RepositoryNodeImpl(String url, String nodeLocation,
                     LockssRepositoryImpl repository) {
    this.url = url;
    this.nodeLocation = nodeLocation;
    this.repository = repository;
  }

  public String getNodeUrl() {
    return url;
  }

  public boolean hasContent() {
    ensureCurrentInfoLoaded();
    return currentVersion>0;
  }

  public boolean isInactive() {
    ensureCurrentInfoLoaded();
    return currentVersion==INACTIVE_VERSION;
  }

  public boolean isDeleted() {
    ensureCurrentInfoLoaded();
    return currentVersion==DELETED_VERSION;
  }

  public long getContentSize() {
    if (!hasContent()) {
      logger.error("Cannot get size if no content: "+url);
      throw new UnsupportedOperationException("No content to get size from.");
    }
    return currentCacheFile.length();
  }

  public long getTreeContentSize(CachedUrlSetSpec filter) {
    // do direct directory traversal, rather than creating RepositoryNodes
    long totalSize = 0;
    if (hasContent()) {
      totalSize = currentCacheFile.length();
    }

    // filter the immediate level
    File[] children = nodeRootFile.listFiles();
    for (int ii=0; ii<children.length; ii++) {
      File child = children[ii];
      if ((!child.isDirectory()) || (child.getName().equals(CONTENT_DIR))) {
        continue;
      }
      // check if in initial spec
      int bufMaxLength = url.length() + child.getName().length() + 1;
      StringBuffer buffer = new StringBuffer(bufMaxLength);
      buffer.append(url);
      if (!url.endsWith(File.separator)) {
        buffer.append(File.separator);
      }
      buffer.append(child.getName());

      String childUrl = buffer.toString();
      if ( (filter == null) || (filter.matches(childUrl))) {
        totalSize += recurseDirContentSize(child);
      }
    }

    return totalSize;
  }

  private long recurseDirContentSize(File nodeDir) {
    // add size of this dir, if any
    long subSize = 0;
    File contentFile = new File(nodeDir, CONTENT_DIR + File.separator +
                                CURRENT_FILENAME);
    if (contentFile.exists()) {
      subSize = contentFile.length();
    }

    // recurse on children
    File[] children = nodeDir.listFiles();
    for (int ii=0; ii<children.length; ii++) {
      File child = children[ii];
      if ((!child.isDirectory()) || (child.getName().equals(CONTENT_DIR))) {
        continue;
      }
      subSize += recurseDirContentSize(child);
    }

    //XXX store here?
    return subSize;
  }

  public Properties getState() {
    //XXX implement
    return null;
  }

  public void storeState(Properties newProps) {
    //XXX implement
  }

  public boolean isLeaf() {
    ensureCurrentInfoLoaded();
    if (!nodeRootFile.exists()) {
      logger.error("No cache directory located for: "+url);
      throw new LockssRepository.RepositoryStateException("No cache directory located.");
    }
    File[] children = nodeRootFile.listFiles();
    for (int ii=0; ii<children.length; ii++) {
      File child = children[ii];
      if (!child.isDirectory()) continue;
      if (child.getName().equals(cacheLocationFile.getName())) continue;
      return false;
    }
    return true;
  }

  public Iterator listNodes(CachedUrlSetSpec filter, boolean includeInactive) {
    if (nodeRootFile==null) loadNodeRoot();
    if (!nodeRootFile.exists()) {
      logger.error("No cache directory located for: "+url);
      throw new LockssRepository.RepositoryStateException("No cache directory located.");
    }
    if (cacheLocationFile==null) loadCacheLocation();
    File[] children = nodeRootFile.listFiles();
    // sorts alphabetically relying on File.compareTo()
    Arrays.sort(children, new FileComparator());
    ArrayList childL = new ArrayList(Math.min(40, children.length));
    for (int ii=0; ii<children.length; ii++) {
      File child = children[ii];
      if ((!child.isDirectory()) || (child.getName().equals(CONTENT_DIR))) {
        continue;
      }
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
          RepositoryNode node = repository.getNode(childUrl);
          // add all nodes which are internal or active leaves
          boolean activeInternal = !node.isLeaf() && !node.isDeleted();
          boolean activeLeaf = node.isLeaf() && !node.isDeleted() &&
              (!node.isInactive() || includeInactive);
          if (activeInternal || activeLeaf) {
            childL.add(repository.getNode(childUrl));
          }
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
    if ((!hasContent()) && ((!isInactive() && !isDeleted()))) {
      logger.error("Cannot get version if no content: "+url);
      throw new UnsupportedOperationException("No content to version.");
    }
    return currentVersion;
  }

  /**
   * Creates the directory for the node location, if non-existent.
   */
  public void createNodeLocation() {
    ensureCurrentInfoLoaded();
    if (!nodeRootFile.exists()) {
      logger.debug3("Creating root directory for CUS '"+url+"'");
      nodeRootFile.mkdirs();
    }
  }

  public synchronized void makeNewVersion() {
    if (newVersionOpen) {
      // check if time since new version exceeds timeout
      if (versionTimeout.expired()) {
        logger.info("Current open version is timed out.  Abandoning...");
        abandonNewVersion();
      } else {
        throw new UnsupportedOperationException("New version already" +
            " initialized.");
      }
    }
    ensureCurrentInfoLoaded();
    if (currentVersion == 0) {
      if (!cacheLocationFile.exists()) {
        if (!cacheLocationFile.mkdirs()) {
          logger.error("Couldn't create cache directory for '"+
                       cacheLocationFile.getAbsolutePath()+"'");
          throw new LockssRepository.RepositoryStateException("Couldn't create cache directory.");
        }
      }
    }

    // if restoring from deletion or inactivation
    if (isInactive() || isDeleted()) {
      int lastVersion = determineLastActiveVersion();
      File inactiveCacheFile = getInactiveCacheFile();
      File inactivePropsFile = getInactivePropsFile();

      // if the files exist but there's a problem renaming them, throw
      if ((inactiveCacheFile.exists() &&
          !inactiveCacheFile.renameTo(currentCacheFile)) ||
          (inactivePropsFile.exists() &&
          !inactivePropsFile.renameTo(currentPropsFile))) {
        logger.error("Couldn't rename inactive versions: "+url);
        throw new LockssRepository.RepositoryStateException("Couldn't rename inactive versions.");
      }
      currentVersion = lastVersion;

      // store the deletion value
      nodeProps.setProperty(INACTIVE_CONTENT_PROPERTY, "false");
      nodeProps.setProperty(DELETION_PROPERTY, "false");
      try {
        OutputStream os = new BufferedOutputStream(new FileOutputStream(nodePropsFile));
        nodeProps.store(os, "Node properties");
        os.close();
      } catch (IOException ioe) {
        logger.error("Couldn't write node properties for " +
                     nodePropsFile.getPath()+".");
        throw new LockssRepository.RepositoryStateException("Couldn't write node properties file.");
      }
    }

    newVersionOpen = true;
    versionTimeout = Deadline.in(VERSION_TIMEOUT);
  }

  public synchronized void sealNewVersion() {
    try {
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
      String err;
      if (currentCacheFile.exists()) {
        if (!currentCacheFile.renameTo(getVersionedCacheFile(currentVersion))) {
          err = "Couldn't rename current versions: "+url;
          logger.error(err);
          throw new LockssRepository.RepositoryStateException(err);
        }
        if (currentPropsFile.exists()) {
          if(!currentPropsFile.renameTo(getVersionedPropsFile(currentVersion))) {
            err = "Couldn't rename current version prop file: " + url;
            logger.error(err);
            throw new LockssRepository.RepositoryStateException(err);
          }
        }
      }
      // rename new
      if (!tempCacheFile.renameTo(currentCacheFile) ||
          !tempPropsFile.renameTo(currentPropsFile)) {
        err = "Couldn't rename temp versions: "+url;
        logger.error(err);
        throw new LockssRepository.RepositoryStateException(err);
      }

      currentVersion++;
      curProps = null;
    } finally {
      newOutputCalled = false;
      newPropsSet = false;
      newVersionOpen = false;
      versionTimeout.expire();
    }
  }

  public synchronized void abandonNewVersion() {
    try {
      if (!newVersionOpen) {
        throw new UnsupportedOperationException("New version not initialized.");
      }
      // clear temp files
      // unimportant if this isn't done, as they're overwritten
      tempCacheFile.delete();
      tempPropsFile.delete();
    } finally {
      newOutputCalled = false;
      newPropsSet = false;
      newVersionOpen = false;
      versionTimeout.expire();
    }
  }

  public synchronized void deactivateContent() {
    if (newVersionOpen) {
      throw new UnsupportedOperationException("Can't deactivate while new version open.");
    }
    ensureCurrentInfoLoaded();
    if (isDeleted()) {
      logger.warning("Node already deleted; not deactivating: " + url);
      return;
    }

    if (hasContent()) {
      if (!currentCacheFile.renameTo(getInactiveCacheFile()) ||
          !currentPropsFile.renameTo(getInactivePropsFile())) {
        logger.error("Couldn't deactivate: " + url);
        throw new LockssRepository.RepositoryStateException(
            "Couldn't deactivate.");
      }
    } else {
      if (!cacheLocationFile.exists()) {
        cacheLocationFile.mkdirs();
      }
    }

    // store the inactive value
    nodeProps.setProperty(INACTIVE_CONTENT_PROPERTY, "true");
    try {
      OutputStream os = new BufferedOutputStream(new FileOutputStream(nodePropsFile));
      nodeProps.store(os, "Node properties");
      os.close();
    } catch (IOException ioe) {
      logger.error("Couldn't write node properties for " +
                   nodePropsFile.getPath()+".");
      throw new LockssRepository.RepositoryStateException("Couldn't write node properties file.");
    }

    currentVersion = INACTIVE_VERSION;
    curProps = null;
  }

  public synchronized void markAsDeleted() {
    ensureCurrentInfoLoaded();
    if (hasContent() && !isInactive()) {
      deactivateContent();
    }

    // store the deletion value
    nodeProps.setProperty(DELETION_PROPERTY, "true");
    try {
      OutputStream os = new BufferedOutputStream(new FileOutputStream(nodePropsFile));
      nodeProps.store(os, "Node properties");
      os.close();
    } catch (IOException ioe) {
      logger.error("Couldn't write node properties for " +
                   nodePropsFile.getPath()+".");
      throw new LockssRepository.RepositoryStateException("Couldn't write node properties file.");
    }

    currentVersion = DELETED_VERSION;
  }

  public synchronized void markAsNotDeleted() {
    ensureCurrentInfoLoaded();

    // store the deletion value
    nodeProps.setProperty(DELETION_PROPERTY, "false");
    try {
      OutputStream os = new BufferedOutputStream(new FileOutputStream(nodePropsFile));
      nodeProps.store(os, "Node properties");
      os.close();
    } catch (IOException ioe) {
      logger.error("Couldn't write node properties for " +
                   nodePropsFile.getPath()+".");
      throw new LockssRepository.RepositoryStateException("Couldn't write node properties file.");
    }

    currentVersion = INACTIVE_VERSION;

    // restore any inactivated content
    restoreLastVersion();
  }


  public synchronized void restoreLastVersion() {
    if (isDeleted()) {
      markAsNotDeleted();
      return;
    }

    if (isInactive()) {
      int lastVersion = determineLastActiveVersion();
      File inactiveCacheFile = getInactiveCacheFile();
      File inactivePropsFile = getInactivePropsFile();

      if (!inactiveCacheFile.renameTo(currentCacheFile) ||
          !inactivePropsFile.renameTo(currentPropsFile)) {
        logger.error("Couldn't rename inactive versions: "+url);
        throw new LockssRepository.RepositoryStateException("Couldn't rename inactive versions.");
      }
      currentVersion = lastVersion;

      // store the deletion value
      nodeProps.setProperty(INACTIVE_CONTENT_PROPERTY, "false");
      try {
        OutputStream os = new BufferedOutputStream(new FileOutputStream(nodePropsFile));
        nodeProps.store(os, "Node properties");
        os.close();
      } catch (IOException ioe) {
        logger.error("Couldn't write node properties for " +
                     nodePropsFile.getPath()+".");
        throw new LockssRepository.RepositoryStateException("Couldn't write node properties file.");
      }

      return;
    }
    if ((!hasContent()) || (getCurrentVersion() == 1)) {
      logger.error("Version restore attempted on node without previous versions.");
      throw new UnsupportedOperationException("Node must have previous versions.");
    }
    int lastVersion = getCurrentVersion() - 1;
    File lastContentFile = getVersionedCacheFile(lastVersion);
    File lastPropsFile = getVersionedPropsFile(lastVersion);

    // delete current version
    currentCacheFile.delete();
    currentPropsFile.delete();

    // rename old version to current
    if (!lastContentFile.renameTo(currentCacheFile) ||
        !lastPropsFile.renameTo(currentPropsFile)) {
      logger.error("Couldn't rename old versions: "+url);
      throw new LockssRepository.RepositoryStateException("Couldn't rename old versions.");
    }

    currentVersion--;
    curProps = null;
  }

  public synchronized RepositoryNode.RepositoryNodeContents getNodeContents() {
    if (!hasContent()) {
      throw new UnsupportedOperationException("No content for url '"+url+"'");
    }
    try {
      InputStream is =
          new BufferedInputStream(new FileInputStream(curInputFile));
      Properties props = (Properties)curProps.clone();
      Reader reader = new BufferedReader(new FileReader(curInputFile));
      return new RepositoryNode.RepositoryNodeContents(is, props, reader);
    } catch (FileNotFoundException fnfe) {
      logger.error("Couldn't get inputstream for '"+curInputFile.getPath()+"'");
      logger.debug3("Running consistency check on node '"+url+"'");
      repository.consistencyCheck(this);
      throw new LockssRepository.RepositoryStateException("Couldn't get info for node.");
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
      try {
        logger.error("No new version file for "+tempCacheFile.getPath()+".");
        throw new LockssRepository.RepositoryStateException("Couldn't load new outputstream.");
      } finally {
        abandonNewVersion();
      }
    }
  }

  public void setNewProperties(Properties newProps) {
    if (!newVersionOpen) {
      throw new UnsupportedOperationException("New version not initialized.");
    }
    if (newPropsSet) {
      throw new UnsupportedOperationException("setNewProperties() called twice.");
    }
    if (newProps!=null) {
      newPropsSet = true;
      Properties myProps = (Properties)newProps.clone();
      try {
        OutputStream os = new BufferedOutputStream(new FileOutputStream(tempPropsFile));
        myProps.setProperty(LOCKSS_VERSION_NUMBER, ""+(currentVersion+1));
        myProps.store(os, "HTTP headers for " + url);
        os.close();
      } catch (IOException ioe) {
        try {
          logger.error("Couldn't write properties for " +
                       tempPropsFile.getPath()+".");
          throw new LockssRepository.RepositoryStateException("Couldn't write properties file.");
        } finally {
          abandonNewVersion();
        }
      }
    }
  }

  private void ensureCurrentInfoLoaded() {
    if (currentVersion==-1) {
      loadNodeRoot();
      loadCacheLocation();
      loadCurrentCacheFile();
      loadCurrentPropsFile();
      loadTempCacheFile();
      loadTempPropsFile();
      loadNodePropsFile();
    } else if ((currentVersion==0) || (currentVersion==INACTIVE_VERSION) ||
               (currentVersion==DELETED_VERSION)) {
      return;
    }
    if (!cacheLocationFile.exists()) {
      currentVersion = 0;
      curInputFile = null;
      curProps = null;
      return;
    }
    if (nodePropsFile.exists()) {
      try {
        // check properties to see if deleted
        InputStream is = new BufferedInputStream(
            new FileInputStream(nodePropsFile));
        nodeProps.load(is);
        is.close();
        String isDeleted =  nodeProps.getProperty(DELETION_PROPERTY);
        if ((isDeleted!=null) && (isDeleted.equals("true"))) {
          currentVersion = DELETED_VERSION;
          curInputFile = null;
          curProps = null;
          return;
        }
      } catch (Exception e) {
        logger.error("Error loading properties from "+
                     nodePropsFile.getPath()+".");
        throw new LockssRepository.RepositoryStateException("Couldn't load properties file.");
      }
    }
    if ((!currentCacheFile.exists()) && (getInactiveCacheFile().exists())) {
      currentVersion = INACTIVE_VERSION;
      curInputFile = null;
      curProps = null;
      return;
    }
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
              curProps.load(is);
              is.close();
            } catch (Exception e) {
              logger.error("Error loading version from "+
                            currentPropsFile.getPath()+".");
              throw new LockssRepository.RepositoryStateException("Couldn't load version from properties file.");
            }
          }
        }
      }
      if (curProps!=null) {
        currentVersion = Integer.parseInt(
                       curProps.getProperty(LOCKSS_VERSION_NUMBER, "-1"));
      }
    }
  }

  private int determineLastActiveVersion() {
    File inactivePropFile = getInactivePropsFile();
    if (inactivePropFile.exists()) {
      Properties oldProps = new Properties();
      try {
        InputStream is = new BufferedInputStream(new FileInputStream(inactivePropFile));
        oldProps.load(is);
        is.close();
      } catch (Exception e) {
        logger.error("Error loading last active version from "+
                      inactivePropFile.getPath()+".");
        throw new LockssRepository.RepositoryStateException("Couldn't load version from properties file.");
      }
      if (oldProps!=null) {
         return Integer.parseInt(oldProps.getProperty(LOCKSS_VERSION_NUMBER,
             "-1"));
       }
    }
    return -1;
  }

  private void loadCurrentCacheFile() {
    StringBuffer buffer = getContentDirBuffer();
    buffer.append(CURRENT_FILENAME);
    currentCacheFile = new File(buffer.toString());
  }

  private void loadCurrentPropsFile() {
    StringBuffer buffer = getContentDirBuffer();
    buffer.append(CURRENT_FILENAME);
    buffer.append(".");
    buffer.append(PROPS_FILENAME);
    currentPropsFile = new File(buffer.toString());
  }

  private void loadTempCacheFile() {
    StringBuffer buffer = getContentDirBuffer();
    buffer.append(TEMP_FILENAME);
    tempCacheFile = new File(buffer.toString());
  }

  private void loadTempPropsFile() {
    StringBuffer buffer = getContentDirBuffer();
    buffer.append(TEMP_FILENAME);
    buffer.append(".");
    buffer.append(PROPS_FILENAME);
    tempPropsFile = new File(buffer.toString());
  }

  private void loadNodePropsFile() {
    StringBuffer buffer = getContentDirBuffer();
    buffer.append(NODE_PROPS_FILENAME);
    nodePropsFile = new File(buffer.toString());
  }


  private void loadCacheLocation() {
    cacheLocationFile = new File(getContentDirBuffer().toString());
  }

  protected void loadNodeRoot() {
    nodeRootFile = new File(nodeLocation);
  }

  File getVersionedCacheFile(int version) {
    StringBuffer buffer = getContentDirBuffer();
    buffer.append(version);
    return new File(buffer.toString());
  }

  File getVersionedPropsFile(int version) {
    StringBuffer buffer = getContentDirBuffer();
    buffer.append(version);
    buffer.append(".");
    buffer.append(PROPS_FILENAME);
    return new File(buffer.toString());
  }

  File getInactiveCacheFile() {
    StringBuffer buffer = getContentDirBuffer();
    buffer.append(INACTIVE_FILENAME);
    return new File(buffer.toString());
  }

  File getInactivePropsFile() {
    StringBuffer buffer = getContentDirBuffer();
    buffer.append(INACTIVE_FILENAME);
    buffer.append(".");
    buffer.append(PROPS_FILENAME);
    return new File(buffer.toString());
  }

  StringBuffer getContentDirBuffer() {
    if (contentBufferStr==null) {
      StringBuffer buffer = new StringBuffer(nodeLocation);
      buffer.append(File.separator);
      buffer.append(CONTENT_DIR);
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
}
