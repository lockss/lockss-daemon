/*
 * $Id: RepositoryNodeImpl.java,v 1.51 2004-04-06 07:30:51 tlipkis Exp $
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
import org.lockss.daemon.*;
import org.lockss.plugin.AuUrl;

/**
 * RepositoryNode is used to store the contents and meta-information of urls
 * being cached.  It stores the content in the following form:
 *   /cache (root)
 *     /'a' (AU directory, 'a'->'z'->'aa' and so on)
 *       /www.example.com (domain)
 *         /http (protocol, reverse order for more logical grouping)
 *           /branch (intermediate path branch)
 *             /file (root of file's storage)
 *               -#node_props (the node properties file)
 *               /#content (the content dir)
 *                 -current (current version '2' content)
 *                 -current.props (current version '2' props)
 *                 -1 (version '1' content)
 *                 -1.props (version '1' props)
 *                 -2.props-1234563216 (time-stamped props for identical
 *                                      version of '2' content)
 *               /child (child node, with its own '#content' and the like)
 *
 *  When deactiveated, 'current'->'inactive' and 'current.props'->'inactive.props'.
 */
public class RepositoryNodeImpl implements RepositoryNode {

  /**
   * Parameter to adjust 'makeNewVersion()' timeout interval.
   */
  public static final String PARAM_VERSION_TIMEOUT = Configuration.PREFIX +
      "repository.version.timeout";
  static final long DEFAULT_VERSION_TIMEOUT = 5 * Constants.HOUR; // 5 hours

  // properties set in the content properties, such as 'current.props'
  static final String LOCKSS_VERSION_NUMBER = "org.lockss.version.number";
  static final String NODE_WAS_INACTIVE_PROPERTY = "org.lockss.node.was.inactive";
  // properties set in the node properties
  static final String INACTIVE_CONTENT_PROPERTY = "node.content.isInactive";
  static final String DELETION_PROPERTY = "node.isDeleted";
  static final String TREE_SIZE_PROPERTY = "node.tree.size";
  static final String CHILD_COUNT_PROPERTY = "node.child.count";
  // the filenames associated with the filesystem storage structure
  // the node property file
  static final String NODE_PROPS_FILENAME = "#node_props";
  // 'node/#content' contains 'current', 'current.props', and any variations
  // such as 'inactive.props' or '1', '2.props', etc.
  static final String CONTENT_DIR = "#content";
  static final String CURRENT_FILENAME = "current";
  static final String PROPS_FILENAME = "props";
  static final String TEMP_FILENAME = "temp";
  static final String INACTIVE_FILENAME = "inactive";
  // special-case versions
  static final int INACTIVE_VERSION = -99;
  static final int DELETED_VERSION = -98;

  // state booleans
  private boolean newVersionOpen = false;
  private boolean newPropsSet = false;
  private boolean wasInactive = false;
  // used to cache outputstream for automatic closing
  private OutputStream curOutputStream = null;
  // current info
  private File curInputFile;
  protected Properties curProps;
  protected Properties nodeProps = new Properties();
  protected int currentVersion = -1;

  // convenience file handles
  private String contentBufferStr = null;
  protected File nodeRootFile = null;
  protected File nodePropsFile = null;
  protected File cacheLocationFile;
  protected File currentCacheFile;
  protected File currentPropsFile;
  private File tempCacheFile;
  private File tempPropsFile;

  // identity url and location
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

  public boolean isContentInactive() {
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
    // this caches the size recursively (for unfiltered queries)
    ensureCurrentInfoLoaded();
    // only cache if not filtered
    if (filter==null) {
      String treeSize = nodeProps.getProperty(TREE_SIZE_PROPERTY);
      if (treeSize != null) {
        // return if found
        return Long.parseLong(treeSize);
      }
    }

    long totalSize = 0;
    if (hasContent()) {
      totalSize = currentCacheFile.length();
    }

    // since RepositoryNodes update and cache tree size, efficient to use them
    for (Iterator subNodes = listChildren(filter, false); subNodes.hasNext(); ) {
      // call recursively on all children
      RepositoryNode subNode = (RepositoryNode)subNodes.next();
      totalSize += subNode.getTreeContentSize(null);
    }

    if (filter==null) {
      // store value
      nodeProps.setProperty(TREE_SIZE_PROPERTY, Long.toString(totalSize));
      writeNodeProperties();
    }
    return totalSize;
  }

  public boolean isLeaf() {
    // if you have no children, you're a leaf
    return (getChildCount() == 0);
  }

  public Iterator listChildren(CachedUrlSetSpec filter, boolean includeInactive) {
    return getNodeList(filter, includeInactive).iterator();
  }

  /**
   * Assembles a list of immediate children, possibly filtered.  Sorted
   * alphabetically by File.compareTo().
   * @param filter a spec to filter on.  Null for no filtering.
   * @param includeInactive true iff inactive nodes to be included
   * @return List the child list of RepositoryNodes
   */
  protected List getNodeList(CachedUrlSetSpec filter, boolean includeInactive) {
    if (nodeRootFile==null) initNodeRoot();
    if (!nodeRootFile.exists()) {
      logger.error("No cache directory located for: "+url);
      throw new LockssRepository.RepositoryStateException("No cache directory located.");
    }
    if (cacheLocationFile==null) initCacheLocation();
    File[] children = nodeRootFile.listFiles();
    // sorts alphabetically relying on File.compareTo()
    Arrays.sort(children, new FileComparator());
    int listSize;
    if (filter==null) {
      listSize = children.length;
    } else {
      // give a reasonable minimum since, if it's filtered, the array size
      // may be much smaller than the total children, particularly in very
      // flat trees
      listSize = Math.min(40, children.length);
    }

    ArrayList childL = new ArrayList(listSize);
    for (int ii=0; ii<children.length; ii++) {
      File child = children[ii];
      if ((!child.isDirectory()) || (child.getName().equals(CONTENT_DIR))) {
        // all children are in their own directories, and the content dir
        // must be ignored
        continue;
      }

      String childUrl = constructChildUrl(url, child.getName());
      if ((filter==null) || (filter.matches(childUrl))) {
        try {
          RepositoryNode node = repository.getNode(childUrl);
          // add all nodes which are internal or active leaves
          // deleted nodes never included
          boolean activeInternal = !node.isLeaf() && !node.isDeleted();
          boolean activeLeaf = node.isLeaf() && !node.isDeleted() &&
              (!node.isContentInactive() || includeInactive);
          if (activeInternal || activeLeaf) {
            childL.add(repository.getNode(childUrl));
          }
        } catch (MalformedURLException ignore) {
          // this can safely skip bad files because they will
          // eventually be trimmed by the repository integrity checker
          // and the content will be replaced by a poll repair
          logger.error("Malformed child url: "+childUrl);
        }
      }
    }
    return childL;
  }

  public int getChildCount() {
    // caches the value for efficiency
    ensureCurrentInfoLoaded();
    String childCount = nodeProps.getProperty(CHILD_COUNT_PROPERTY);
    if (childCount!=null) {
      // return if found
      return Integer.parseInt(childCount);
    }

    int count = getNodeList(null, false).size();
    nodeProps.setProperty(CHILD_COUNT_PROPERTY, Integer.toString(count));
    writeNodeProperties();
    return count;
  }

  /**
   * This call wipes the cached values from the node properties, and calls itself
   * on the parent node, if any.  This allows a change in value to propagate
   * upwards, forcing the nodes to recalculate when queried.
   * @param startNode true iff this node started the chain
   */
  void invalidateCachedValues(boolean startNode) {
    if (!startNode) {
      // make sure node is loaded if not the start node (which should always
      // be loaded)
      ensureCurrentInfoLoaded();
    }

    boolean wasCleared = false;
    if ((nodeProps.getProperty(TREE_SIZE_PROPERTY)!=null) ||
        (nodeProps.getProperty(CHILD_COUNT_PROPERTY)!=null)) {
      nodeProps.remove(TREE_SIZE_PROPERTY);
      nodeProps.remove(CHILD_COUNT_PROPERTY);
      writeNodeProperties();
      wasCleared = true;
    }

    // continue to parent if start node, or had cached values
    // this forces the first level, but then stops if we hit an already
    // wiped node (for efficiency)
    if (startNode || wasCleared) {
      // call invalidate on parent
      RepositoryNodeImpl parentNode = determineParentNode();
      if (parentNode != null) {
        if (!parentNode.getNodeUrl().equals(getNodeUrl())) {
          // don't loop by mistake if we've reached the top
          // (shouldn't be possible anyway)
          parentNode.invalidateCachedValues(false);
        }
      }
    }
  }

  /**
   * Attempts to extract a 'parent' url from the child url by eliminating the
   * last section of the path.  If unsuccessful, it returns the AuUrl.
   * @return RepositoryNodeImpl the parent
   */
  RepositoryNodeImpl determineParentNode() {
    String childUrl = getNodeUrl();
    if (childUrl.charAt(childUrl.length()-1) ==
        UrlUtil.URL_PATH_SEPARATOR_CHAR) {
      // trim the final '/', if present
      childUrl = childUrl.substring(0, childUrl.length()-1);
    }
    int index = childUrl.lastIndexOf(UrlUtil.URL_PATH_SEPARATOR);
    try {
      if (index >= 0) {
        // splits 'root/parent/child' to 'root/parent'
        String parentUrl = childUrl.substring(0, index);
        RepositoryNodeImpl node =
            (RepositoryNodeImpl)repository.getNode(parentUrl);
        if (node != null) {
          return node;
        }
      }
      // if nothing found, use the AUCUS
      return (RepositoryNodeImpl)repository.getNode(AuUrl.PROTOCOL_COLON);
    } catch (MalformedURLException ignore) { return null; }
  }

  /**
   * Convenience method to construct a url.
   * @param root the parent's url
   * @param child the child name
   * @return String the full child url
   */
  static String constructChildUrl(String root, String child) {
    int bufMaxLength = root.length() + child.length() + 1;
    StringBuffer buffer = new StringBuffer(bufMaxLength);
    buffer.append(root);
    if (!root.endsWith(UrlUtil.URL_PATH_SEPARATOR)) {
      buffer.append(UrlUtil.URL_PATH_SEPARATOR);
    }
    buffer.append(LockssRepositoryImpl.unescape(child));
    return buffer.toString();
  }

  public int getCurrentVersion() {
    if ((!hasContent()) && ((!isContentInactive() && !isDeleted()))) {
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
    if (isContentInactive() || isDeleted()) {
      wasInactive = true;
      currentVersion = determineLastActiveVersion();
    }

    newVersionOpen = true;
    versionTimeout = Deadline.in(
      Configuration.getLongParam(PARAM_VERSION_TIMEOUT,
                                 DEFAULT_VERSION_TIMEOUT));
  }

  public synchronized void sealNewVersion() {
    boolean identicalVersion = false;
    try {
      if (curOutputStream==null) {
         throw new UnsupportedOperationException("getNewOutputStream() not called.");
      } else {
        try {
          // make sure outputstream was closed
          curOutputStream.close();
        } catch (IOException ignore) { }
        curOutputStream = null;
      }
      if (!newVersionOpen) {
        throw new UnsupportedOperationException("New version not initialized.");
      }
      if (!newPropsSet) {
        throw new UnsupportedOperationException("setNewProperties() not called.");
      }

      // if the node was inactive, we need to copy the inactive files to
      // 'current' so they can join the standard versioning
      if (wasInactive) {
        File inactiveCacheFile = getInactiveCacheFile();
        File inactivePropsFile = getInactivePropsFile();

        // if the files exist but there's a problem renaming them, throw
        if ((inactiveCacheFile.exists() &&
             !inactiveCacheFile.renameTo(currentCacheFile)) ||
            (inactivePropsFile.exists() &&
             !inactivePropsFile.renameTo(currentPropsFile))) {
          logger.error("Couldn't rename inactive versions: " + url);
          throw new LockssRepository.RepositoryStateException(
              "Couldn't rename inactive versions.");
        }

        // add the 'was inactive' property so the knowledge isn't lost
        try {
          Properties myProps = new SortedProperties();
          // make sure to close all streams or will fail on Windows
          InputStream is =
              new BufferedInputStream(new FileInputStream(currentPropsFile));
          myProps.load(is);
          is.close();
          myProps.setProperty(NODE_WAS_INACTIVE_PROPERTY, "true");
          OutputStream os =
              new BufferedOutputStream(new FileOutputStream(currentPropsFile));
          myProps.store(os, "HTTP headers for " + url);
          os.close();
        } catch (IOException ignore) {
          logger.error("Couldn't set 'was inactive' property for last version of: "+url);
        }


        // remove any deletion values
        nodeProps.remove(INACTIVE_CONTENT_PROPERTY);
        nodeProps.remove(DELETION_PROPERTY);
        writeNodeProperties();
        wasInactive = false;
      }

      // check temp vs. last version, so as not to duplicate identical versions
      if (currentCacheFile.exists()) {
        try {
          // if identical, don't rename
          if (FileUtil.isContentEqual(currentCacheFile, tempCacheFile)) {
            // don't rename
	    logger.debug("New version identical to old: " + currentCacheFile);
            identicalVersion = true;
          } else if (!currentCacheFile.renameTo(getVersionedCacheFile(
              currentVersion))) {
            String err = "Couldn't rename current content file: " + url;
            logger.error(err);
            throw new LockssRepository.RepositoryStateException(err);
          }
        } catch (IOException ioe) {
          String err = "Error comparing files: "+ url;
          logger.error(err);
          throw new LockssRepository.RepositoryStateException(err);
        }
      }

      // get versioned props file
      File verPropsFile;
      // name 'identical version' props differently
      if (identicalVersion) {
        // rename to dated property version, using 'File.lastModified()'
        long date = currentPropsFile.lastModified();
        verPropsFile = getDatedVersionedPropsFile(currentVersion, date);
        while (verPropsFile.exists()) {
          date++;
          verPropsFile = getDatedVersionedPropsFile(currentVersion, date);
        }
      } else {
        // rename to standard property version
        verPropsFile = getVersionedPropsFile(currentVersion);
      }

      // rename current properties to chosen file name
      if (currentPropsFile.exists() &&
          !currentPropsFile.renameTo(verPropsFile)) {
        String err = "Couldn't rename current property file: " + url;
        logger.error(err);
        throw new LockssRepository.RepositoryStateException(err);
      }

      // if not identical, rename content from 'temp' to 'current'
      if (!identicalVersion) {
        // rename new content file (if non-identical)
        if (!tempCacheFile.renameTo(currentCacheFile)) {
          String err = "Couldn't rename temp content version: " + url;
          logger.error(err);
          throw new LockssRepository.RepositoryStateException(err);
        }
        // update version number
        currentVersion++;
      } else {
        // remove temp content file, since identical
        tempCacheFile.delete();

        // check for erroneous version
        if (currentVersion<=0) {
          // this shouldn't occur, but we'll bump it to fix the error state
          logger.error("Content found with version '0'; changing to '1'");
          currentVersion = 1;
        }

        // reset version number in new props back to current version, since
        // it was originally written as 'current + 1'
        // (done this way to make property writing more efficient for
        // non-identical cases)
        try {
          Properties myProps = new SortedProperties();
          // make sure to close all streams or will fail on Windows
          InputStream is = new BufferedInputStream(new FileInputStream(
              tempPropsFile));
          myProps.load(is);
          is.close();
          myProps.setProperty(LOCKSS_VERSION_NUMBER,
                              Integer.toString(currentVersion));
          OutputStream os = new BufferedOutputStream(new FileOutputStream(
              tempPropsFile));
          myProps.store(os, "HTTP headers for " + url);
          os.close();
        } catch (IOException ioe) {
          String err = "Couldn't reset property version number: " + url;
          logger.error(err);
          throw new LockssRepository.RepositoryStateException(err);
        }
      }

      // rename new properties
      if (!tempPropsFile.renameTo(currentPropsFile)) {
        String err = "Couldn't rename temp property version: " + url;
        logger.error(err);
        throw new LockssRepository.RepositoryStateException(err);
      }

      // set to null to force update of new info
      curProps = null;
    } finally {
      // new version complete
      newPropsSet = false;
      newVersionOpen = false;
      versionTimeout.expire();
      // blank the stored sizes for this and its parents
      if (!identicalVersion) {
        invalidateCachedValues(true);
      }
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

      if (wasInactive) {
        // set to reinitialize to force proper state restore
        currentVersion = -1;
        wasInactive = false;
        // reload proper
        ensureCurrentInfoLoaded();
      }
    } finally {
      // new version abandoned
      if (curOutputStream!=null) {
        try {
          curOutputStream.close();
        } catch (IOException ignore) { }
        curOutputStream = null;
      }
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

    // copy 'current' files to 'inactive'
    if (hasContent()) {
      if ((currentCacheFile.exists() &&
          !currentCacheFile.renameTo(getInactiveCacheFile())) ||
          (currentPropsFile.exists() &&
          !currentPropsFile.renameTo(getInactivePropsFile()))) {
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
    // remove caching
    invalidateCachedValues(true);

    currentVersion = INACTIVE_VERSION;
    curProps = null;
  }

  public synchronized void markAsDeleted() {
    ensureCurrentInfoLoaded();
    if (hasContent() && !isContentInactive()) {
      deactivateContent();
    }

    // store the deletion value
    nodeProps.setProperty(DELETION_PROPERTY, "true");
    // blank caches
    invalidateCachedValues(true);

    currentVersion = DELETED_VERSION;
  }

  public synchronized void markAsNotDeleted() {
    ensureCurrentInfoLoaded();

    // store the deletion value
    nodeProps.remove(DELETION_PROPERTY);
    invalidateCachedValues(true);

    currentVersion = INACTIVE_VERSION;

    // restore any inactivated content
    restoreLastVersion();
  }


  public synchronized void restoreLastVersion() {
    if (isDeleted()) {
      // removes the 'deleted' mark, then treats as inactive and returns here
      markAsNotDeleted();
      return;
    }

    if (isContentInactive()) {
      int lastVersion = determineLastActiveVersion();
      if (lastVersion > 0) {
        File inactiveCacheFile = getInactiveCacheFile();
        File inactivePropsFile = getInactivePropsFile();

        if (!inactiveCacheFile.renameTo(currentCacheFile) ||
            !inactivePropsFile.renameTo(currentPropsFile)) {
          logger.error("Couldn't rename inactive versions: "+url);
          throw new LockssRepository.RepositoryStateException("Couldn't rename inactive versions.");
        }
        currentVersion = lastVersion;

        // remove the inactivation value
        nodeProps.remove(INACTIVE_CONTENT_PROPERTY);
        writeNodeProperties();
      }
      return;
    }

    if ((!hasContent()) || (getCurrentVersion() <= 1)) {
      logger.error("Version restore attempted on node without previous versions.");
      throw new UnsupportedOperationException("Node must have previous versions.");
    }

    int lastVersion = getCurrentVersion() - 1;
    File lastContentFile = getVersionedCacheFile(lastVersion);
    File lastPropsFile = getVersionedPropsFile(lastVersion);

    // delete current version
    // XXX probably should rename these instead
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
    invalidateCachedValues(true);
  }

  public synchronized RepositoryNode.RepositoryNodeContents getNodeContents() {
    if (!hasContent()) {
      throw new UnsupportedOperationException("No content for url '"+url+"'");
    }
    return new RepositoryNodeContentsImpl();
  }

  public OutputStream getNewOutputStream() {
    if (!newVersionOpen) {
      throw new UnsupportedOperationException("New version not initialized.");
    }
    if (curOutputStream!=null) {
      throw new UnsupportedOperationException("getNewOutputStream() called twice.");
    }
    try {
      curOutputStream = new BufferedOutputStream(
          new FileOutputStream(tempCacheFile));
      return curOutputStream;
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
      // copy the props and sort
      Properties myProps = SortedProperties.fromProperties(newProps);
      try {
        OutputStream os = new BufferedOutputStream(new FileOutputStream(tempPropsFile));
        int versionToWrite = currentVersion + 1;
        if (versionToWrite <= 0) {
          // this is an error condition which shouldn't occur
          logger.error("Content being written with version of '0'; adjusting to '1'");
          currentVersion = 0;
          versionToWrite = 1;
        }
        myProps.setProperty(LOCKSS_VERSION_NUMBER, Integer.toString(versionToWrite));
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

  /**
   * Run to make sure that all values are initialized and up-to-date.
   */
  private void ensureCurrentInfoLoaded() {
    if ((currentVersion==0) || (currentVersion==INACTIVE_VERSION) ||
        (currentVersion==DELETED_VERSION)) {
      // node already initialized and has no active content,
      // so exit early
      return;
    } else if (currentVersion==-1) {
      // initialize
      initFiles();

      // no content, so version 0
      if (!cacheLocationFile.exists()) {
        currentVersion = 0;
        curInputFile = null;
        curProps = null;
        return;
      }

      // load the node properties
      if (nodePropsFile.exists()) {
        loadNodeProps();
      }

      // determine if deleted or inactive
      if (checkIfInactiveOrDeleted()) {
        return;
      }
    }

    // make sure content handles are up-to-date
    // load current info and version
    if ((curInputFile==null) || (curProps==null)) {
      loadCurrentInfo();
    }
  }

  /**
   * Initializes the file handles.
   */
  private void initFiles() {
    initNodeRoot();
    initCacheLocation();
    initCurrentCacheFile();
    initCurrentPropsFile();
    initTempCacheFile();
    initTempPropsFile();
    initNodePropsFile();
  }

  /**
   * Load the node properties.
   */
  private void loadNodeProps() {
    try {
      // check properties to see if deleted
      InputStream is = new BufferedInputStream(
          new FileInputStream(nodePropsFile));
      nodeProps.load(is);
      is.close();
    } catch (Exception e) {
      logger.error("Error loading properties from "+
                   nodePropsFile.getPath()+".");
      throw new LockssRepository.RepositoryStateException("Couldn't load properties file.");
    }
  }

  /**
   * Load the current input file and properties, if needed.  Extract current
   * version from the properties.
   */
  private void loadCurrentInfo() {
    synchronized (this) {
      if (curInputFile==null) {
        curInputFile = currentCacheFile;
      }
      if (curProps==null) {
        if (currentPropsFile.exists()) {
          try {
            InputStream is =
                new BufferedInputStream(new FileInputStream(currentPropsFile));
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
      currentVersion =
          Integer.parseInt(curProps.getProperty(LOCKSS_VERSION_NUMBER, "-1"));
      if (currentVersion <= 0) {
        logger.error("Bad content version found: "+currentVersion);
        repository.deactivateInconsistentNode(this);
      }
    }
  }

  /**
   * Looks at the properties and files to determine if the version is active.
   * Sets the version and info correctly before returning.
   * @return boolean true iff inactive or deleted.
   */
  private boolean checkIfInactiveOrDeleted() {
    String isDeleted = nodeProps.getProperty(DELETION_PROPERTY);
    if ((isDeleted != null) && (isDeleted.equals("true"))) {
      currentVersion = DELETED_VERSION;
      curInputFile = null;
      curProps = null;
      return true;
    }
    String isInactive = nodeProps.getProperty(INACTIVE_CONTENT_PROPERTY);
    if ((isInactive != null) && (isInactive.equals("true"))) {
      currentVersion = INACTIVE_VERSION;
      curInputFile = null;
      curProps = null;
      return true;
    }
    if ((!currentCacheFile.exists()) && (getInactiveCacheFile().exists())) {
      currentVersion = INACTIVE_VERSION;
      curInputFile = null;
      curProps = null;
      return true;
    }
    return false;
  }

  /**
   * Queries the 'inactive.props' to determine which version was the last.
   * @return int the last version, or '-1' if unable to determine
   */
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

  /**
   * Writes the node properties to disk.
   */
  private void writeNodeProperties() {
    if (currentVersion == 0) {
      if (!cacheLocationFile.exists()) {
        if (!cacheLocationFile.mkdirs()) {
          logger.error("Couldn't create cache directory for '"+
                       cacheLocationFile.getAbsolutePath()+"'");
          throw new LockssRepository.RepositoryStateException("Couldn't create cache directory.");
        }
      }
    }
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

  // functions to initialize the file handles

  private void initCurrentCacheFile() {
    StringBuffer buffer = getContentDirBuffer();
    buffer.append(CURRENT_FILENAME);
    currentCacheFile = new File(buffer.toString());
  }

  private void initCurrentPropsFile() {
    StringBuffer buffer = getContentDirBuffer();
    buffer.append(CURRENT_FILENAME);
    buffer.append(".");
    buffer.append(PROPS_FILENAME);
    currentPropsFile = new File(buffer.toString());
  }

  private void initTempCacheFile() {
    StringBuffer buffer = getContentDirBuffer();
    buffer.append(TEMP_FILENAME);
    tempCacheFile = new File(buffer.toString());
  }

  private void initTempPropsFile() {
    StringBuffer buffer = getContentDirBuffer();
    buffer.append(TEMP_FILENAME);
    buffer.append(".");
    buffer.append(PROPS_FILENAME);
    tempPropsFile = new File(buffer.toString());
  }

  private void initNodePropsFile() {
    StringBuffer buffer = new StringBuffer(nodeLocation);
    buffer.append(File.separator);
    buffer.append(NODE_PROPS_FILENAME);
    nodePropsFile = new File(buffer.toString());
  }

  private void initCacheLocation() {
    cacheLocationFile = new File(getContentDirBuffer().toString());
  }

  protected void initNodeRoot() {
    nodeRootFile = new File(nodeLocation);
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

  // functions to get a 'versioned' content or props file, such as
  // '1', '1.props', or '1.props-123135131' (the dated props)

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

  File getDatedVersionedPropsFile(int version, long date) {
    StringBuffer buffer = getContentDirBuffer();
    buffer.append(version);
    buffer.append(".");
    buffer.append(PROPS_FILENAME);
    buffer.append("-");
    buffer.append(date);
    return new File(buffer.toString());
  }

  /**
   * Intended to ensure props and stream reflect a consistent view of a
   * single version.  This version gurantees that only if the stream is
   * fetched only once.
   */
  public class RepositoryNodeContentsImpl implements RepositoryNodeContents {
    private Properties props;
    private InputStream is;

    private RepositoryNodeContentsImpl() {
    }

    public InputStream getInputStream() {
      ensureInputStream();
      InputStream res = is;
      // stream can only be used once.
      is = null;
      return res;
    }

    public Properties getProperties() {
      if (props == null) {
	ensureInputStream();
      }
      return props;
    }

    public void release() {
      is = null;
    }

    private void assertContent() {
      if (!hasContent()) {
	throw new UnsupportedOperationException("No content for url '" +
						url + "'");
      }
    }

    private synchronized void ensureInputStream() {
      if (is == null) {
	assertContent();
	try {
	  is = new BufferedInputStream(new FileInputStream(curInputFile));
	  props = (Properties)curProps.clone();
	} catch (IOException e) {
	  logger.error("Couldn't get inputstream for '" +
		       curInputFile.getPath() + "'");
	  logger.debug3("Running consistency check on node '"+url+"'");
	  repository.deactivateInconsistentNode(RepositoryNodeImpl.this);
	  throw new LockssRepository.RepositoryStateException ("Couldn't open InputStream: " + e.toString());
	}
      }
    }
  }


  /**
   * Simple comparator which uses File.compareTo() for sorting.
   */
  private class FileComparator implements Comparator {
    public int compare(Object o1, Object o2) {
      // compares file pathnames
      return ((File)o1).getName().compareToIgnoreCase(((File)o2).getName());
    }

  }
}
