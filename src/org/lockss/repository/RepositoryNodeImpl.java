/*
 * $Id: RepositoryNodeImpl.java,v 1.86.8.7 2011-01-14 19:23:16 dshr Exp $
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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
import java.net.MalformedURLException;
import java.util.*;
import org.apache.oro.text.regex.*;
import org.apache.commons.vfs.FileName;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystem;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileContent;
import org.apache.commons.vfs.FileType;
import org.apache.commons.vfs.VFS;

import org.lockss.config.*;
import org.lockss.protocol.*;
import org.lockss.daemon.CachedUrlSetSpec;
import org.lockss.plugin.AuUrl;
import org.lockss.util.*;

/**
 * <p>RepositoryNode is used to store the contents and meta-information of urls
 * being cached.  It stores the content in the following form:</p>
 * <pre>
 *   /cache (root)
 *     /'a' (AU directory, 'a'->'z'->'aa' and so on)
 *       /www.example.com (domain)
 *         /http (protocol, reverse order for more logical grouping)
 *           /branch (intermediate path branch)
 *             /file (root of file's storage)
 *               -#node_props (the node properties file)
 *               -#agreement (the peers who we know have agreed with this node)
 *               /#content (the content dir)
 *                 -current (current version '2' content)
 *                 -current.props (current version '2' props)
 *                 -1 (version '1' content)
 *                 -1.props (version '1' props)
 *                 -2.props-1234563216 (time-stamped props for identical
 *                                      version of '2' content)
 *               /child (child node, with its own '#content' and the like)
 *  </pre>
 *
 *  <p>When deactiveated, 'current'->'inactive' and 
 *  'current.props'->'inactive.props'.</p>
 */
public class RepositoryNodeImpl implements RepositoryNode {

  /**
   * Parameter to adjust 'makeNewVersion()' timeout interval.
   */
  public static final String PARAM_VERSION_TIMEOUT = Configuration.PREFIX +
      "repository.version.timeout";
  static final long DEFAULT_VERSION_TIMEOUT = 5 * Constants.HOUR; // 5 hours
  
  /** If true, enable the tracking of agreement. */
  public static final String PARAM_ENABLE_AGREEMENT = Configuration.PREFIX +
      "repository.enableAgreement";
  public static final boolean DEFAULT_ENABLE_AGREEMENT = true;

  /** If true, input streams are monitored for missed close()s */
  public static final String PARAM_MONITOR_INPUT_STREAMS =
    Configuration.PREFIX + "monitor.inputStreams";
  public static final boolean DEFAULT_MONITOR_INPUT_STREAMS = false;

  /** If true, the release method actually closes streams.  If false it
   * does nothing. */
  public static final String PARAM_ENABLE_RELEASE =
    Configuration.PREFIX + "repository.enableRelease";
  public static final boolean DEFAULT_ENABLE_RELEASE = true;

  /** Determines whether errors that occur when opening a node cause the
   * node to be deactivated. */
  public static final String PARAM_DEACTIVATE_NODE_ON_ERROR =
    Configuration.PREFIX + "repository.deactivateNodeOnError";
  public static final boolean DEFAULT_DEACTIVATE_NODE_ON_ERROR = true;

  // properties set in the content properties, such as 'current.props'
  static final String LOCKSS_VERSION_NUMBER = "org.lockss.version.number";
  static final String NODE_WAS_INACTIVE_PROPERTY = "org.lockss.node.was.inactive";
  // properties set in the node properties
  static final String INACTIVE_CONTENT_PROPERTY = "node.content.isInactive";
  static final String DELETION_PROPERTY = "node.isDeleted";

  static final String TREE_SIZE_PROPERTY = "node.tree.size";
  static final String CHILD_COUNT_PROPERTY = "node.child.count";
  // Token used in above props to indicate explicitly invalid.  Used to
  // distinguish invalidated from never-been-set.
  static final String INVALID = "U";

  public static final String PARAM_KEEP_ALL_PROPS_FOR_DUPE_FILE =
    Configuration.PREFIX + "repository.keepAllPropsForDupeFile";
  public static final boolean DEFAULT_KEEP_ALL_PROPS_FOR_DUPE_FILE = false;

  /** If true, repair nodes that have lowercase URL-encoding chars */
  public static final String PARAM_FIX_UNNORMALIZED =
    Configuration.PREFIX + "repository.fixUnnormalized";
  public static final boolean DEFAULT_FIX_UNNORMALIZED = true;

  // the agreement history file
  static final String AGREEMENT_FILENAME = "#agreement";
  // Temporary file used when writing a new agreement history.
  static final String TEMP_AGREEMENT_FILENAME = "#agreement.temp";
  // the filenames associated with the filesystem storage structure
  // the node property file
  static final String NODE_PROPS_FILENAME = "#node_props";
  // 'node/#content' contains 'current', 'current.props', and any variations
  // such as 'inactive.props' or '1', '2.props', etc.
  static final String CONTENT_DIR = "#content";
  static final String CURRENT_FILENAME = "current";
  static final String PROPS_EXTENSION = ".props";
  static final String CURRENT_PROPS_FILENAME = "current.props";
  static final String TEMP_FILENAME = "temp";
  static final String TEMP_PROPS_FILENAME = "temp.props";
  static final String INACTIVE_FILENAME = "inactive";
  static final String INACTIVE_PROPS_FILENAME = "inactive.props";

  // the extension appended to faulty files when the consistency check
  // renames them (to allow the node to function properly)
  static final String FAULTY_FILE_EXTENSION = ".ERROR";
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
  private FileObject curInputFile;
  protected Properties curProps;
  protected Properties nodeProps = new Properties();
  private boolean nodePropsLoaded = false;
  protected int currentVersion = -1;

  // convenience file handles
  protected FileObject contentDir = null; // Used LockssRepositoryImpl

  protected FileObject nodeRootFile = null; // Used AuNodeImpl
  protected FileObject nodePropsFile = null; // Used TestLockssRepositoryImpl
  private FileObject agreementFile = null;
  private FileObject tempAgreementFile = null;
  protected FileObject currentCacheFile; // Used TestLockssRepositoryImpl
  protected FileObject currentPropsFile; // Used TestLockssRepositoryImpl
  protected FileObject tempCacheFile; // Used TestLockssRepositoryImpl
  protected FileObject tempPropsFile; // Used TestLockssRepositoryImpl

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
    logger.debug3("new node url " + url + " location " + nodeLocation);
  }

  public String getNodeUrl() {
    return url;
  }

  public boolean hasContent() {
    ensureCurrentInfoLoaded();
    logger.debug3("hasContent " + nodeLocation + " ver " + currentVersion);
    return currentVersion>0;
  }

  public boolean isContentInactive() {
    ensureCurrentInfoLoaded();
    logger.debug3("isContentInactive " + nodeLocation + " ver " + currentVersion);
    return currentVersion==INACTIVE_VERSION;
  }

  public boolean isDeleted() {
    ensureCurrentInfoLoaded();
    logger.debug3("isDeleted " + nodeLocation + " ver " + currentVersion);
    return currentVersion==DELETED_VERSION;
  }

  public long getContentSize() {
    if (!hasContent()) {
      logger.error("Cannot get size if no content: "+url);
      throw new UnsupportedOperationException("No content to get size from.");
    }
    try {
      return currentCacheFile.getContent().getSize();
    } catch (FileSystemException e) {
      throw new UnsupportedOperationException("getContentSize() threw: " + e);
    }
  }

  public long getTreeContentSize(CachedUrlSetSpec filter,
				 boolean calcIfUnknown) {
    // this caches the size recursively (for unfiltered queries)
    ensureCurrentInfoLoaded();
    // only cache if not filtered
    if (filter==null) {
      String treeSize = nodeProps.getProperty(TREE_SIZE_PROPERTY);
      if (isPropValid(treeSize)) {
        // return if found
	logger.debug2("Found cached size at " + nodeLocation);
        return Long.parseLong(treeSize);
      }
    }
    logger.debug2("No cached size at " + nodeLocation);
    if (!calcIfUnknown) {
      repository.queueSizeCalc(this);
      return -1;
    }

    long totalSize = 0;
    if (hasContent()) {
      try {
	totalSize = currentCacheFile.getContent().getSize();
      } catch (FileSystemException e) {
	logger.error("getTreeContentSize() threw: " + e);
      }
    }

    // since RepositoryNodes update and cache tree size, efficient to use them
    int children = 0;
    for (Iterator subNodes = listChildren(filter, false); subNodes.hasNext(); ) {
      // call recursively on all children
      RepositoryNode subNode = (RepositoryNode)subNodes.next();
      totalSize += subNode.getTreeContentSize(null, true);
      children++;
    }

    if (filter==null) {
      // cache values
      nodeProps.setProperty(TREE_SIZE_PROPERTY, Long.toString(totalSize));
      nodeProps.setProperty(CHILD_COUNT_PROPERTY, Integer.toString(children));
      writeNodeProperties();
    }
    return totalSize;
  }

  public boolean isLeaf() {
    // use cached number of children if available
    ensureCurrentInfoLoaded();
    String childCount = nodeProps.getProperty(CHILD_COUNT_PROPERTY);
    if (isPropValid(childCount)) {
      // If no children, we're a leaf
      return Integer.parseInt(childCount) == 0;
    }
    // No child count available.  Don't call getChildCount ...xxx
    // It's a leaf if no subdirs (excluding content dir)
    FileObject[] children = null;
    try {
      children = nodeRootFile.getChildren();
    } catch (FileSystemException e) {
      logger.error("isLeaf() threw: " + e);
    }
    if (children == null) {
      String msg = "No cache directory located for: " + url;
      logger.error(msg);
      throw new LockssRepository.RepositoryStateException(msg);
    }
    for (int ii=0; ii<children.length; ii++) {
      FileObject child = children[ii];
      logger.debug3("isLeaf " + child.getName().getBaseName() +
		    " vs. " + contentDir.getName().getBaseName());
      if (child.getName().getBaseName().equals(contentDir.getName().getBaseName())) continue;
      if (!isFolder(child)) continue;
      return false;
    }
    return true;
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
    if (contentDir==null) getContentDir();
    FileObject[] children = null;
    try {
      children = nodeRootFile.getChildren();
    } catch (FileSystemException e) {
      logger.error("getNodeList() threw: " + e);
    }
    if (children == null) {
      String msg = "No cache directory located for: " + url;
      logger.error(msg);
      throw new LockssRepository.RepositoryStateException(msg);
    }
    // sorts alphabetically relying on File.compareTo()
    Arrays.sort(children, new FileObjectComparator());
    int listSize;
    if (filter==null) {
      listSize = children.length;
    } else {
      // give a reasonable minimum since, if it's filtered, the array size
      // may be much smaller than the total children, particularly in very
      // flat trees
      listSize = Math.min(40, children.length);
    }

    boolean checkUnnormalized =
      CurrentConfig.getBooleanParam(PARAM_FIX_UNNORMALIZED,
				    DEFAULT_FIX_UNNORMALIZED);

    ArrayList childL = new ArrayList(listSize);
    for (int ii=0; ii<children.length; ii++) {
      FileObject child = children[ii];
      String baseName = child.getName().getBaseName();
      if (CONTENT_DIR.equals(baseName) || !isFolder(child)) {
        // all children are in their own directories, and the content dir
        // must be ignored
        continue;
      }
      if (checkUnnormalized) {
	child = checkUnnormalized(child, children, ii);
      }
      if (child == null) {
	continue;
      }
      baseName = UrlUtil.encodeUrl(baseName);
      logger.debug3("Child base name is: " + baseName);
      String childUrl = constructChildUrl(url, baseName);
      if ((filter==null) || (filter.matches(childUrl))) {
        try {
          RepositoryNode node = repository.getNode(childUrl);
          // add all nodes which are internal or active leaves
          // deleted nodes never included
//           boolean activeInternal = !node.isLeaf() && !node.isDeleted();
//           boolean activeLeaf = node.isLeaf() && !node.isDeleted() &&
//               (!node.isContentInactive() || includeInactive);
//           if (activeInternal || activeLeaf) {
	  if (!node.isDeleted() && (!node.isContentInactive() ||
				    (includeInactive || !node.isLeaf()))) {
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

  private static Pattern UNNORMALIZED =
    RegexpUtil.uncheckedCompile(".*%([a-z]|.[a-z]).*",
				Perl5Compiler.READ_ONLY_MASK);

  protected String normalize(String path) {
    int ix = path.lastIndexOf(File.separator) + 1;
    String normName = normalizeUrlEncodingCase(path.substring(ix));
    normName = normalizeTrailingQuestion(normName);
    return path.substring(0,ix) + normName;
  }
    
  protected FileObject normalize(FileObject file) { // Used TestRepositoryNodeImpl
    String name = file.getName().getBaseName();
    String normName = normalize(name);
    if (normName.equals(name)) {
      return file;
    }
    try {
      FileObject parent = file.getParent();
      file = parent.resolveFile(normName);
    } catch (FileSystemException e) {
      logger.error("normalize() threw: " + e);
    }
    return file;
  }

  String normalizeUrlEncodingCase(String name) {
    if (name.indexOf('%') == -1) {
      return name;
    }
    Perl5Matcher matcher = RegexpUtil.getMatcher();
    if (!matcher.matches(name, UNNORMALIZED)) {
      return name;
    }
    return UrlUtil.normalizeUrlEncodingCase(name);
  }

  String normalizeTrailingQuestion(String name) {
    if (CurrentConfig.getBooleanParam(UrlUtil.PARAM_NORMALIZE_EMPTY_QUERY,
				      UrlUtil.DEFAULT_NORMALIZE_EMPTY_QUERY)) {
      return StringUtil.removeTrailing(name, "?");
    } else {
      return name;
    }
  }

  private FileObject checkUnnormalized(FileObject file, FileObject[] all, int ix) {
    FileObject norm = normalize(file);
    if (norm == file) {
      return file;
    }
    synchronized (this) {
      try {
	if (file.exists()) {
	  if (norm.exists()) {
	    if (file.delete()) {
	      logger.debug("Deleted redundant unnormalized: " +
			   file.getName().getPath());
	    } else {
	      logger.error("Couldn't delete unnormalized: " + file.getName().getPath());
	    }
	    all[ix] = null;
	  } else {
	    if (file.canRenameTo(norm)) {
	      file.moveTo(norm);
	      logger.debug("Renamed unnormalized: " + file.getName().getPath() + " to " +
			   norm.getName().getPath());
	      all[ix] = norm;
	    } else {
	      logger.error("Couldn't rename unnormalized: " + file.getName().getPath() +
			   " to " + norm.getName().getPath());
	      all[ix] = null;
	    }
	  }
	}
      } catch (FileSystemException e) {
	logger.error("checkUnnormalized() threw: " + e);
      }
      return all[ix];
    }
  }

  public int getChildCount() {
    // caches the value for efficiency
    ensureCurrentInfoLoaded();
    String childCount = nodeProps.getProperty(CHILD_COUNT_PROPERTY);
    if (isPropValid(childCount)) {
      // return if found
      return Integer.parseInt(childCount);
    }

    // get unfiltered node list
    int count = getNodeList(null, false).size();
    nodeProps.setProperty(CHILD_COUNT_PROPERTY, Integer.toString(count));
    writeNodeProperties();
    return count;
  }

  /** return true if value is null or INVALID token */
  static boolean isPropValid(String val) {
    return val != null && !val.equals(INVALID);
  }

  /** return true if value is INVALID token */
  static boolean isPropInvalid(String val) {
    return val != null && val.equals(INVALID);
  }

  /** return true if FileObject is folder */
  private static boolean isFolder(FileObject f) {
    boolean ret = false;
    try {
      ret = f.getType() == FileType.FOLDER;
    } catch (FileSystemException e) {
      logger.debug2("isFolder() threw: " + e);
    }
    return ret;
  }

  /** return true if FileObject is file */
  private static boolean isFile(FileObject f) {
    boolean ret = false;
    try {
      ret = f.getType() == FileType.FILE;
    } catch (FileSystemException e) {
      logger.debug2("isFile() threw: " + e);
    }
    return ret;
  }

  /**
   * Invalidate the cached values in this node's properties, and recurse up
   * the tree to the root of the AU.  Called whenever anything changes at a
   * node which might require the cached values to be recomputed.  Stops if
   * a node is reached whose cache has already been invalidated, but
   * continues through dirs that have never had their cache values set, as
   * they may have been created en masse by a mkdirs, and never had a props
   * file written.
   * @param startNode true iff this node started the chain
   */
  void invalidateCachedValues(boolean startNode) {
    if (!startNode) {
      // make sure node is loaded if not the start node (which should always
      // be loaded)
      ensureCurrentInfoLoaded();
    }

    String treeSize = nodeProps.getProperty(TREE_SIZE_PROPERTY);
    String childCount = nodeProps.getProperty(CHILD_COUNT_PROPERTY);
    if (isPropValid(treeSize) || isPropValid(childCount)) {
      nodeProps.setProperty(TREE_SIZE_PROPERTY, INVALID);
      nodeProps.setProperty(CHILD_COUNT_PROPERTY, INVALID);
      writeNodeProperties();
    }

    // continue to parent if start node, or had not already been explicitly
    // invalidated (in which case there might be cached values above)
    if (startNode || !isPropInvalid(treeSize) || !isPropInvalid(childCount)) {
      RepositoryNodeImpl parentNode = determineParentNode();
      if (parentNode != null) {
	logger.debug3("parent: " + parentNode.getNodeUrl() + " child: " +
		      getNodeUrl());
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
    logger.debug3("childUrl: " + childUrl);
    int index = childUrl.lastIndexOf(UrlUtil.URL_PATH_SEPARATOR);
    try {
      if (index >= 0) {
        // splits 'root/parent/child' to 'root/parent'
        String parentUrl = childUrl.substring(0, index);
        RepositoryNodeImpl node =
            (RepositoryNodeImpl)repository.getNode(parentUrl);
        if (node != null) {
	  logger.debug3("return: " + node.getNodeUrl());
          return node;
        }
      }
      // if nothing found, use the AUCUS
      RepositoryNodeImpl ret =
	(RepositoryNodeImpl)repository.getNode(AuUrl.PROTOCOL_COLON);
      logger.debug3("AUCUS: " + ret.getNodeUrl());
      return ret;
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

  public int getVersion() {
    return getCurrentVersion();
  }

  public int getCurrentVersion() {
    if ((!hasContent()) && ((!isContentInactive() && !isDeleted()))) {
      logger.error("Cannot get version if no content: "+url);
      throw new UnsupportedOperationException("No content, so no version.");
    }
    return currentVersion;
  }

  /**
   * Creates the directory for the node location, if non-existent.
   */
  public void createNodeLocation() {
    ensureCurrentInfoLoaded();
    String nodeUrl = "BAD url";
    try {
      nodeUrl =  nodeRootFile.getName().getPath();
      if (!nodeRootFile.exists()) {
	logger.debug3("Creating root directory for CUS '"+url+"'");
	nodeRootFile.createFolder();
      }
      if (nodeRootFile.exists() && isFolder(nodeRootFile)) {
	return;
      }
      logger.error(nodeUrl + " is not a folder");
    } catch (FileSystemException e) {
      logger.error("createNodeLocation() threw: " + e);
    }
    throw new LockssRepository.RepositoryStateException("mkdirs(" +
							nodeUrl +
							") failed");
  }

  public synchronized void makeNewVersion() {
    logger.debug3("makeNewVersion: " + nodeLocation + " ver " + currentVersion);
    try {
      FileObject nr = VFS.getManager().resolveFile(nodeLocation);
      logger.debug3("nodeRootFile: 1 " + nr.exists());
    } catch (FileSystemException e) {
      throw new UnsupportedOperationException("nodeRootFile.exists() threw " +
					      e);
    }
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
    try {
      FileObject nr = VFS.getManager().resolveFile(nodeLocation);
      logger.debug3("nodeRootFile: 2 " + nr.exists());
    } catch (FileSystemException e) {
      throw new UnsupportedOperationException("nodeRootFile.exists() threw " +
					      e);
    }

    // Needs to be done unconditionally in case node or content dir has
    // disappeared.  (Was:   if ( currentVersion == 0) {  )
    if (!ensureDirExists(contentDir)) {
      logger.error("Couldn't create cache directory: " +contentDir);
      throw new LockssRepository.RepositoryStateException("mkdirs(" +
							  contentDir +
							  ") failed.");
    }
    try {
      FileObject nr = VFS.getManager().resolveFile(nodeLocation);
      logger.debug3("nodeRootFile: 3 " + nr.exists());
    } catch (FileSystemException e) {
      throw new UnsupportedOperationException("nodeRootFile.exists() threw " +
					      e);
    }

    // if restoring from deletion or inactivation
    if (isContentInactive() || isDeleted()) {
      wasInactive = true;
      currentVersion = determineLastActiveVersion();
    }

    newVersionOpen = true;
    versionTimeout = Deadline.in(
      CurrentConfig.getLongParam(PARAM_VERSION_TIMEOUT,
                                 DEFAULT_VERSION_TIMEOUT));
  }

  public synchronized void sealNewVersion() {
    boolean identicalVersion = false;
    logger.debug3("sealNewVersion: " + nodeLocation + " ver " + currentVersion);
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
        FileObject inactiveCacheFile = getInactiveCacheFile();
        FileObject inactivePropsFile = getInactivePropsFile();

        // if the files exist but there's a problem renaming them, throw
	boolean ok = false;
	try {
	  ok = (inactiveCacheFile.exists() &&
		!updateAtomically(inactiveCacheFile, currentCacheFile));
	  if (ok) {
	    ok = (inactivePropsFile.exists() &&
		  !updateAtomically(inactivePropsFile, currentPropsFile));
	  }
	} catch (FileSystemException e) {
	  logger.error("sealNewVersion() threw: " + e);
	}
	if (!ok) {
          logger.error("Couldn't rename inactive versions: " + url);
          throw new LockssRepository.RepositoryStateException(
              "Couldn't rename inactive versions.");
        }

        // add the 'was inactive' property so the knowledge isn't lost
        try {
          Properties myProps = loadProps(currentPropsFile);
          myProps.setProperty(NODE_WAS_INACTIVE_PROPERTY, "true");
          OutputStream os =
	    new BufferedOutputStream(currentPropsFile.getContent().getOutputStream());
          myProps.store(os, "HTTP headers for " + url);
          os.close();
        } catch (IOException e) {
          logger.error("Couldn't set 'was inactive' property for last version of: "+url,
		       e);
        } catch (LockssRepository.RepositoryStateException e) {
          logger.error("Couldn't set 'was inactive' property for last version of: "+url,
		       e);
	}	  


        // remove any deletion values
        nodeProps.remove(INACTIVE_CONTENT_PROPERTY);
        nodeProps.remove(DELETION_PROPERTY);
        writeNodeProperties();
        wasInactive = false;
      }

      // check temp vs. last version, so as not to duplicate identical versions
      try {
	if (currentCacheFile.exists()) {
          // if identical, don't rename
          if (isContentEqual(currentCacheFile, tempCacheFile)) {
            // don't rename
	    if (logger.isDebug2()) {
	      logger.debug2("New version identical to old: " +
			    currentCacheFile);
	    }
            identicalVersion = true;
          } else if (!updateAtomically(currentCacheFile,
				       getVersionedCacheFile(currentVersion))) {
            String err = "Couldn't rename current content file: " + url;
            logger.error(err);
            throw new LockssRepository.RepositoryStateException(err);
          }
	}
      } catch (IOException ioe) {
	String err = "Error comparing files: "+ url;
	logger.error(err);
	throw new LockssRepository.RepositoryStateException(err);
      }

      // get versioned props file
      FileObject verPropsFile;
      // name 'identical version' props differently
      if (identicalVersion) {
	try {
	  // rename to dated property version, using 'File.lastModified()'
	  long date = currentPropsFile.getContent().getLastModifiedTime();
	  verPropsFile = getDatedVersionedPropsFile(currentVersion, date);
	  while (verPropsFile.exists()) {
	    date++;
	    verPropsFile = getDatedVersionedPropsFile(currentVersion, date);
	  }
	} catch (FileSystemException e) {
	  String err = "Couldn't rename to dated property file: " + url + " " +
	    e;
	  logger.error(err);
	  throw new LockssRepository.RepositoryStateException(err);
	}
      } else {
        // rename to standard property version
        verPropsFile = getVersionedPropsFile(currentVersion);
      }

      if (CurrentConfig.getBooleanParam(PARAM_KEEP_ALL_PROPS_FOR_DUPE_FILE,
                                        DEFAULT_KEEP_ALL_PROPS_FOR_DUPE_FILE)
                                        || !identicalVersion) {
	try {
	  // rename current properties to chosen file name
	  if (currentPropsFile.exists() &&
	      !updateAtomically(currentPropsFile, verPropsFile)) {
	    String err = "Couldn't rename current property file: " + url;
	    logger.error(err);
	    throw new LockssRepository.RepositoryStateException(err);
	  }
	} catch (FileSystemException e) {
	  String err = "Couldn't rename current property file: " + url;
	  logger.error(err);
	  throw new LockssRepository.RepositoryStateException(err);
	}
      }

      // if not identical, rename content from 'temp' to 'current'
      if (!identicalVersion) {
        // rename new content file (if non-identical)
        if (!updateAtomically(tempCacheFile, currentCacheFile)) {
          String err = "Couldn't rename temp content version: " + url;
          logger.error(err);
          throw new LockssRepository.RepositoryStateException(err);
        }
        // update version number
        currentVersion++;
      } else {
        // remove temp content file, since identical
	try {
	  tempCacheFile.delete();
	} catch (FileSystemException e) {
          String err = "Couldn't reset property version number: " + url +
	    e;
          logger.error(err);
          throw new LockssRepository.RepositoryStateException(err);
	}

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
          Properties myProps = loadProps(tempPropsFile);
          myProps.setProperty(LOCKSS_VERSION_NUMBER,
                              Integer.toString(currentVersion));
          OutputStream os =
	    new BufferedOutputStream(tempPropsFile.getContent().getOutputStream());
          myProps.store(os, "HTTP headers for " + url);
          os.close();
        } catch (IOException ioe) {
          String err = "Couldn't reset property version number: " + url;
          logger.error(err);
          throw new LockssRepository.RepositoryStateException(err);
	}
      }

      // rename new properties
      if (!updateAtomically(tempPropsFile, currentPropsFile)) {
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
    logger.debug3("abandonNewVersion: " + nodeLocation);
    if (logger.isDebug3()) {
      (new Throwable()).printStackTrace();
    }
    try {
      if (!newVersionOpen) {
        throw new UnsupportedOperationException("New version not initialized.");
      }
      // clear temp files
      // unimportant if this isn't done, as they're overwritten
      try {
	tempCacheFile.delete();
	tempPropsFile.delete();
      } catch (FileSystemException e) {
	logger.warning("abandonNewVersion() threw: " + e);
      }

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
    boolean ok = false;
    try {
      if (hasContent()) {
	if ((currentCacheFile.exists() &&
	     !updateAtomically(currentCacheFile,
			       getInactiveCacheFile()))
	    ||
	    (currentPropsFile.exists() &&
	     !updateAtomically(currentPropsFile,
			       getInactivePropsFile()))) {
	  logger.error("Couldn't deactivate: " + url);
	  throw
	    new LockssRepository.RepositoryStateException("Couldn't deactivate.");
	}
      } else {
	if (!contentDir.exists()) {
	  contentDir.createFolder();
	  logger.debug3("create content dir" + ok);
	}
      }
    } catch (FileSystemException e) {
      logger.error("deactivateContent() threw: " + e);
      logger.error("Couldn't deactivate: " + url);
      throw
	new LockssRepository.RepositoryStateException("Couldn't deactivate.");
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
        FileObject inactiveCacheFile = getInactiveCacheFile();
        FileObject inactivePropsFile = getInactivePropsFile();

        if (!updateAtomically(inactiveCacheFile, currentCacheFile) ||
            !updateAtomically(inactivePropsFile, currentPropsFile)) {
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
    FileObject lastContentFile = getVersionedCacheFile(lastVersion);
    FileObject lastPropsFile = getVersionedPropsFile(lastVersion);

    try {
      // delete current version
      // XXX probably should rename these instead
      currentCacheFile.delete();
      currentPropsFile.delete();
    } catch (FileSystemException e) {
      logger.error("Couldn't delete current versions: "+url);
      throw new LockssRepository.RepositoryStateException("Couldn't rename old versions.");
    }

    // rename old version to current
    if (!updateAtomically(lastContentFile, currentCacheFile) ||
        !updateAtomically(lastPropsFile, currentPropsFile)) {
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
    logger.debug3("getNewOutputStream: " + tempCacheFile.getName().getPath());
    if (logger.isDebug3()) {
      (new Throwable()).printStackTrace();
    }
    if (!newVersionOpen) {
      throw new UnsupportedOperationException("New version not initialized.");
    }
    if (curOutputStream!=null) {
      throw new UnsupportedOperationException("getNewOutputStream() called twice.");
    }
    logger.debug3("getNewOutputStream: is null");
    try {
      curOutputStream =
	new BufferedOutputStream(tempCacheFile.getContent().getOutputStream());
      logger.debug3("getNewOutputStream: is " + ( curOutputStream == null ?
						  "null" : "not null"));
      return curOutputStream;
    } catch (FileSystemException e) {
      try {
        logger.error("No new version file for " + nodeLocation, e);
        throw new LockssRepository.RepositoryStateException("Couldn't load new outputstream.");
      } finally {
        abandonNewVersion();
      }
    }
  }

  SortedProperties loadProps(FileObject propsFile)
      throws FileNotFoundException, IOException {
    SortedProperties props = new SortedProperties();
    loadPropsInto(propsFile, props);
    return props;
  }

  void loadPropsInto(FileObject propsFile, Properties props)
      throws FileNotFoundException, IOException {
    if (!propsFile.exists()) {
      logger.debug3(nodeLocation + ": props file not exist");
      FileNotFoundException ex =
	new FileNotFoundException(nodeLocation + ": props file");
      if (logger.isDebug3()) {
	ex.printStackTrace();
      }
      throw ex;
    }
    if (!propsFile.isReadable()) {
      logger.debug3(nodeLocation + "props file not readable");
      throw new IOException(nodeLocation + ": node props unreadable");
    }
    InputStream is = new BufferedInputStream(propsFile.getContent().getInputStream());
    try {
      props.load(is);
      logger.debug3("props loaded");
    } catch (IllegalArgumentException e) {
      logger.debug3("prop load threw");
      // Usually means a malformed encoding in the props file
      throw new LockssRepository.RepositoryStateException("Can't read properties file.",
							  e);
    } finally {
      is.close();
    }
  }
  
  public synchronized boolean hasAgreement(PeerIdentity id) {
    PersistentPeerIdSet agreeingPeers = loadAgreementHistory();
    try {
      return agreeingPeers.contains(id);
    } catch (IOException e) {
      return false;
    }
  }
  
  public synchronized void signalAgreement(Collection peers) {
    PersistentPeerIdSet agreeingPeers = loadAgreementHistory();
    for (Iterator it = peers.iterator(); it.hasNext(); ) {
      PeerIdentity key = (PeerIdentity)it.next();
      try {
        agreeingPeers.add(key);
      } catch (IOException e) {
        logger.warning("impossible error in loaded PeerIdSet");
        return;   /* TODO: Should this pass up an exception? */
      }
    }
    try {
      agreeingPeers.store(true);
    } catch (IOException e) {
      logger.error("Couldn't store node agreement: " + getNodeUrl(), e);
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
        OutputStream os = new BufferedOutputStream(tempPropsFile.getContent().getOutputStream());
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
		       tempPropsFile.getName().getPath() + ".");
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
  void ensureCurrentInfoLoaded() {
    if ((currentVersion==0) || (currentVersion==INACTIVE_VERSION) ||
        (currentVersion==DELETED_VERSION)) {
      // node already initialized and has no active content,
      // so exit early
      logger.debug3("ensureCurrentInfoLoaded: already inited");
      return;
    } else if (currentVersion==-1) {
      // XXX happens multiple times for dir nodes with no content, as
      // currentVersion doesn't get changed.  Fix this.
      initFiles();

      // load the node properties
      if (!nodePropsLoaded) {
	try {
	  loadNodeProps(true);
	} catch (LockssRepositoryImpl.RepositoryStateException rse) {
	  currentVersion = DELETED_VERSION;
	  logger.warning("Renaming faulty 'nodeProps' to 'nodeProps.ERROR'");
	  String errFilePath = nodePropsFile.getName().getPath() + "." +
	    FAULTY_FILE_EXTENSION;
	  try {
	  FileObject errFile =
	    repository.getFileSystem().resolveFile(errFilePath);
	  if (!updateAtomically(nodePropsFile, errFile)) {
	    logger.error("Error renaming nodeProps file");
	  }
	  } catch (FileSystemException e) {
	    logger.error("ensureCurrentInfoLoaded() threw: " + e);
	  }
	}
      }

      // no content, so version 0
      try {
        currentVersion = 0;
        curInputFile = null;
        curProps = null;
	if (!contentDir.exists()) {
	  return;
	}
      } catch (FileSystemException e) {
	logger.error("ensureCurrentInfoLoaded() threw: " + e);
	return;
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
  void initFiles() {
    logger.debug3("initFiles: initNodeRoot " + nodeLocation);
    initNodeRoot();
    logger.debug3("initFiles: initCurrentCacheFile " + nodeLocation);
    initCurrentCacheFile();
    logger.debug3("initFiles: initCurrentPropsFile " + nodeLocation);
    initCurrentPropsFile();
    logger.debug3("initFiles: initTempCacheFile " + nodeLocation);
    initTempCacheFile();
    logger.debug3("initFiles: initTempPropsFile " + nodeLocation);
    initTempPropsFile();
    logger.debug3("initFiles: initNodePropsFile " + nodeLocation);
    initNodePropsFile();
    logger.debug3("initFiles: done " + nodeLocation);
  }

  
  /**
   * Load the node properties.
   */
  void loadNodeProps(boolean okIfNotThere) {
    String nodeUrl = nodePropsFile.getName().getPath();
    try {
      // check properties to see if deleted
      loadPropsInto(nodePropsFile, nodeProps);
      nodePropsLoaded = true;
    } catch (FileNotFoundException e) {
      if (!okIfNotThere) {
	String msg = "No node props file: " + nodeUrl;
	logger.error(msg);
	throw new LockssRepository.RepositoryStateException(msg);
      }
    } catch (Exception e) {
      logger.error("Error loading node props from " + nodeUrl, e);
      throw new LockssRepository.RepositoryStateException("Couldn't load properties file.");
    }
  }
  
  /**
   * Return a set of PeerIdentity keys that have agreed with this node.
   * 
   * The previous version of this routine used 'Set<String>' (without declaring
   * that it was a set of String)'.  This version returns a PersistentPeerIdSet.
   */
  PersistentPeerIdSet loadAgreementHistory() {
    PersistentPeerIdSet ppisReturn;
 
     if (agreementFile == null) {
      initAgreementFile(AGREEMENT_FILENAME);
    }
    
    DataInputStream is = null;
    try {
      IdentityManager im = repository.getDaemon().getIdentityManager();
      ppisReturn = new PersistentPeerIdSetImpl(this, AGREEMENT_FILENAME, im);
      ppisReturn.load();
      
    } catch (Exception e) {
      logger.error("Error loading agreement history" + e.getMessage());
      throw new LockssRepository.RepositoryStateException("Couldn't load agreement file.");
    } finally {
      IOUtil.safeClose(is);
    }
    
    return ppisReturn;
  }
  
  
  /** Consume the input stream, decoding peer identity keys  */
  Set decodeAgreementHistory(DataInputStream is) {
    Set history = new HashSet();
    String id;
    try {
      while ((id = IDUtil.decodeOneKey(is)) != null) {
        history.add(id);
      }
    } catch (IdentityParseException ex) {
      // IDUtil.decodeOneKey will do its best to leave us at the
      // start of the next key, but there's no guarantee.  All we can
      // do here is log the fact that there was an error, and try
      // again.
      logger.error("Parse error while trying to decode agreement " +
                   "history file " + agreementFile + ": " + ex);
    }
    return history;
  }

  /* Rename a potentially corrupt agreement history file */
  void backupAgreementHistoryFile() {
    try {
      String oldPath = agreementFile.getName().getPath() + ".old";
      FileObject oldFile = repository.getFileSystem().resolveFile(oldPath);
      updateAtomically(agreementFile, oldFile);
    } catch (IOException ex) {
      // This would only be caused by getCanonicalFile() throwing IOException.
      // Worthy of a stack trace.
      logger.error("Unable to back-up suspect agreement history file:", ex);
    }
  }

  public InputStream getPeerIdInputStream(String fileName) {
    logger.debug3("getPeerIdInputStream " + fileName);
    InputStream is = null;
    if (agreementFile == null) {
      initAgreementFile(fileName);
    }
    if (agreementFile != null) try {
	is = agreementFile.getContent().getInputStream();
      } catch (FileSystemException ex) {
	logger.error(fileName + " getContent() threw: " + ex);
      }
    return is;
  }

  public OutputStream getPeerIdOutputStream(String fileName)
    throws IOException {
    logger.debug3("getPeerIdOutputStream " + fileName);
    OutputStream os = null;
    if (tempAgreementFile == null) {
      initTempAgreementFile(fileName);
    }
    if (tempAgreementFile != null) try {
	os = tempAgreementFile.getContent().getOutputStream();
      } catch (FileSystemException ex) {
	logger.error(fileName + " getContent() threw: " + ex);
      }
    return os;
  }

  public FileObject getPeerIdFileObject(String fileName) {
    logger.debug3("getPeerIdFileObject " + fileName);
    return (AGREEMENT_FILENAME.equals(fileName) ?
	    agreementFile : tempAgreementFile);
  }

  public boolean updatePeerIdFile(String fileName) {
    logger.debug3("updatePeerIdFile " + fileName);
    boolean ret = false;
    if (agreementFile == null) {
      initAgreementFile(AGREEMENT_FILENAME);
    }
    if (tempAgreementFile == null) {
      initTempAgreementFile(fileName);
    }
    if (tempAgreementFile != null && agreementFile != null) {
      // XXX we should create a temporary copy of agreementFile so that
      // XXX if the move fails and in the process deletes it,  we can
      // XXX put it back
      try {
	if (tempAgreementFile.canRenameTo(agreementFile)) {
	  tempAgreementFile.moveTo(agreementFile);
	  ret = true;
	}
      } catch (FileSystemException ex) {
	logger.error(fileName + " threw: " + ex);
      } finally {
	try {
	  tempAgreementFile.delete();
	} catch (FileSystemException ex) {
	  logger.error(TEMP_AGREEMENT_FILENAME + " delete() threw: " + ex);
	}
	tempAgreementFile = null;
      }
    }
    return ret;
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
	try {
	  curProps = loadProps(currentPropsFile);
	} catch (FileNotFoundException e) {
	  // No error if file not found, just treat as deleted
	  currentVersion = DELETED_VERSION;
	  return;
	} catch (IOException e) {
	  logger.error("Error loading props from " + currentPropsFile, e);
	  throw new LockssRepository.RepositoryStateException("Couldn't load version from properties file.", e);
	} catch (RuntimeException e) {
	  // Error loading props, treat as deleted
	  currentVersion = DELETED_VERSION;
	  return;
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

  void maybeDeactivateInconsistentNode() {
    if (CurrentConfig.getBooleanParam(PARAM_DEACTIVATE_NODE_ON_ERROR,
                                      DEFAULT_DEACTIVATE_NODE_ON_ERROR)) {
      repository.deactivateInconsistentNode(this);
    } else {
      logger.debug("Not deactivating inconsistent node.");
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
    // XXX This happens a lot.  Does it maybe need to check files only if
    // props aren't loaded, not if neither value is true?
    if (!nodePropsLoaded) {
      try {
	if ((getInactiveCacheFile().exists()) && (!currentCacheFile.exists())) {
	  currentVersion = INACTIVE_VERSION;
	  curInputFile = null;
	  curProps = null;
	  return true;
	}
      } catch (FileSystemException e) {
	logger.error("Error loading props from " + currentPropsFile, e);
	throw new LockssRepository.RepositoryStateException("exists() threw: ",
			    e);
      }
    }
    return false;
  }

  /**
   * Queries the 'inactive.props' to determine which version was the last.
   * @return int the last version, or '-1' if unable to determine
   */
  private int determineLastActiveVersion() {
    FileObject inactivePropFile = getInactivePropsFile();
    try {
      if (inactivePropFile.exists()) {
	Properties oldProps = new Properties();
	loadPropsInto(inactivePropFile, oldProps);
	if (oldProps!=null) {
	  return Integer.parseInt(oldProps.getProperty(LOCKSS_VERSION_NUMBER,
						       "-1"));
	}
      }
    } catch (Exception e) {
      String inactiveUrl = inactivePropFile.getName().getPath();
      logger.error("Error loading last active version from "+inactiveUrl+".");
      throw new LockssRepository.RepositoryStateException("Couldn't load version from properties file.");
    }
    return -1;
  }

  /**
   * Checks the internal consistency of the node.  It repairs any damage it
   * can, but otherwise returns false if the node is inconsistent.
   * @return boolean false iff irreparably inconsistent
   */
  boolean checkNodeConsistency() {
    logger.debug2("Checking consistency on '"+url+"'.");
    if (newVersionOpen) {
      //XXX should throw?  Not sure.
      logger.debug("New version open.  Skipping...");
      return true;
    }
    // if broken repair if possible, else return false
    // don't start with 'ensureCurrentInfoLoaded()', since it may be broken
    // init the first files
    initFiles();
    if (!checkNodeRootConsistency()) {
      logger.error("Couldn't create node root location.");
      return false;
    }

    // check if content expected
    if ((currentVersion>0) || (currentVersion==INACTIVE_VERSION) ||
        (currentVersion==DELETED_VERSION)) {
      if (!checkContentConsistency()) {
        return false;
      }
    } else {
      // check if version inaccurate (0, but content present)
      if (currentVersion==0) {
	try {
	  if (getContentDir().exists()) {
	    logger.debug("Content dir exists, though no content expected.");
	  }
	} catch (FileSystemException e) {
	  logger.error("checkNodeConsistency() threw: " + e);
	}
      }
    }

    // try to load info
    try {
      ensureCurrentInfoLoaded();
    } catch (LockssRepositoryImpl.RepositoryStateException rse) {
      logger.error("Still can't load info: "+rse);
      return false;
    }

    logger.debug3("Node consistent.");
    return true;
  }

  /**
   * Checks the consistency of the node root location and properties.
   * @return boolean false iff inconsistent
   */
  boolean checkNodeRootConsistency() {
    // -root directory exists and is a directory
    if (!ensureDirExists(nodeRootFile)) {
      return false;
    }

    try {
      if (!nodePropsFile.exists()) {
	return true;
      }
    } catch (FileSystemException e) {
      logger.error("checkNodeRootConsistency() threw:" + e);
    }
    // -if node properties exists, check that it's readable
    try {
        loadNodeProps(false);
    } catch (LockssRepositoryImpl.RepositoryStateException rse) {
      logger.warning("Renaming faulty 'nodeProps' to 'nodeProps.ERROR'");
      // as long as the rename goes correctly, we can proceed
      String errFilePath = nodePropsFile.getName().getPath() +
	FAULTY_FILE_EXTENSION;
      FileObject errFile = null;
      try {
	errFile = repository.getFileSystem().resolveFile(errFilePath);
      } catch (FileSystemException e) {
	logger.error("checkNodeRootConsistency(" + errFilePath +
		     ") threw: " + e);
	return false;
      }	  
      if (!updateAtomically(nodePropsFile, errFile)) {
	logger.error("Error renaming nodeProps file:" + errFilePath);
	return false;
      }
    }
    return true;
  }

  /**
   * Check if the content directory is consistent.  Repair if possible, and
   * return 'false' whenever no repair possible.
   * @return boolean true if repaired or no damage
   */
  boolean checkContentConsistency() {
    // -content dir should exist as a directory
    if (!ensureDirExists(getContentDir())) {
      logger.error("Couldn't create content directory.");
      return false;
    }
    logger.debug3("checkContentConsistency: " + nodeLocation + " ver " +
		  currentVersion);
    // assuming the content dir exists, check the content

    // -if content, content files exist
    if (currentVersion>0) {
      if (!checkFileExists(currentCacheFile, "Current cache file") ||
          !checkFileExists(currentPropsFile, "Current props file")) {
        return false;
      }
    }
    // -if inactive, inactive files exist
    if (currentVersion==INACTIVE_VERSION) {
      if (!checkFileExists(getInactiveCacheFile(), "Inactive cache file") ||
          !checkFileExists(getInactivePropsFile(), "Inactive props file")) {
        return false;
      }
    }

    // check if child count accurate in cache
    checkChildCountCacheAccuracy();

    try {
    // remove any residual files
    // -check temp cache and prop files
    if (tempCacheFile.exists()) {
      logger.debug("Deleting temp cache file...");
      tempCacheFile.delete();
    }
    if (tempPropsFile.exists()) {
      logger.debug("Deleting temp props file...");
      tempPropsFile.delete();
    }
    } catch (FileSystemException e) {
      logger.error("Deleting temp files threw: " + e);
    }
    return true;
  }

  /**
   * Makes sure the given file exists as a directory.  If a file exists in
   * that location, the file is renamed to 'file.ERROR' and the directory is
   * created.  Returns false if the rename fails, or the dir couldn't be
   * created.
   * @param dirFile the directory
   * @return boolean false iff the dir couldn't be created
   */
  boolean ensureDirExists(FileObject dirFile) {
    try {
      FileObject nr = VFS.getManager().resolveFile(nodeLocation);
      logger.debug3("ensureDirExists: 1 " + nr.exists());
    } catch (FileSystemException e) {
      throw new UnsupportedOperationException("nodeRootFile.exists() threw " +
					      e);
    }
    try {
      logger.debug3("ensureDirExists " + dirFile.getName().getPath());
      {
	(new Throwable()).printStackTrace();
      }
      // --rename if a file at that position
      if (isFile(dirFile)) {
	logger.error("Exists but as a file: " + dirFile.getName().getPath());
	logger.error("Renaming file to 'xxx.ERROR'...");
	String errFilePath = dirFile.getName().getPath() + FAULTY_FILE_EXTENSION;
	FileObject errFile = dirFile.getFileSystem().resolveFile(errFilePath);
	if (!updateAtomically(dirFile, errFile)) {
	  logger.error("Error renaming file:" + errFilePath);
	  return false;
	}
      }

      // create the dir if absent
      if (!dirFile.exists()) {
	logger.warning("Directory missing: " + dirFile.getName().getPath());
	logger.warning("Creating directory...");
	dirFile.createFolder();
	if (!dirFile.exists()) {
	  logger.error("Create of " + dirFile.getName().getPath());
	}
      } else {
	logger.debug3(dirFile.getName().getPath() + " existed");
      }
      // make sure no problems
      return (dirFile.exists() && isFolder(dirFile));
    } catch (FileSystemException e) {
      logger.error("ensureDirExists() threw: " + e);
      return false;
    }
  }

  /**
   * Checks to see if the file exists.  Returns false if it doesn't, or if
   * it's a directory.  Renames the directory to 'dir.ERROR' before returning,
   * so that the node can be fixed later.
   * @param testFile FileObject to test
   * @param desc the file description, used for logging
   * @return boolean true iff the file exists
   */
  protected boolean checkFileExists(FileObject testFile, String desc) {
    try {
      if (!testFile.exists()) {
	logger.warning(desc+" not found.");
	if (logger.isDebug3()) {
	  logger.debug3("checkFileExists " + desc + " " + testFile.getName().getPath());
	  (new Throwable()).printStackTrace();
	}
	return false;
      } else if (isFolder(testFile)) {
	logger.error(desc+" a directory.");
	String errFilePath = testFile.getName().getPath() +
	  FAULTY_FILE_EXTENSION;
	FileObject errFile =
	  repository.getFileSystem().resolveFile(errFilePath);
	updateAtomically(testFile, errFile);
	return false;
      }
      return true;
    } catch (FileSystemException e) {
      logger.error(desc + " threw: " + e);
      return false;
    }
  }

  /**
   * Checks the accuracy of the cached child count, and invalidates it
   * if wrong.
   */
  void checkChildCountCacheAccuracy() {
    int count = 0;
    FileObject[] children = null;
    try {
      children = nodeRootFile.getChildren();
    } catch (FileSystemException e) {
      logger.error("checkchildCountCacheAccuracy() threw: " + e);
    }
    if (children == null) {
      String msg = "No cache directory located for: " + url;
      logger.error(msg);
      throw new LockssRepository.RepositoryStateException(msg);
    }
    for (int ii=0; ii<children.length; ii++) {
      FileObject child = children[ii];
      if (!isFolder(child)) continue;
      if (child.getName().equals(contentDir.getName())) continue;
      count++;
    }

    String childCount = nodeProps.getProperty(CHILD_COUNT_PROPERTY);
    if (isPropValid(childCount)) {
      // return if found
      int cachedCount = Integer.parseInt(childCount);
      if (cachedCount != count) {
        logger.warning("Cached child count erroneous: was " +
            cachedCount + ", but is "+count);
        nodeProps.setProperty(CHILD_COUNT_PROPERTY, INVALID);
      } else {
        logger.debug3("No inaccuracy in cached child count.");
      }
    } else {
      logger.debug3("No cached child count.");
    }
  }

  /**
   * Writes the node properties to disk.
   */
  protected void writeNodeProperties() {
    try {
      logger.debug3("Writing node properties: " + nodePropsFile.getName().getPath());
      OutputStream os = new BufferedOutputStream(nodePropsFile.getContent().getOutputStream());
      nodeProps.store(os, "Node properties");
      os.close();
    } catch (IOException ioe) {
      String nodeUrl = nodePropsFile.getName().getPath();
      logger.error("Couldn't write node properties for " + nodeUrl+".");
      throw new LockssRepository.RepositoryStateException("Couldn't write node properties file.");
    }
  }

  // functions to initialize the file handles

  private void initCurrentCacheFile() {
    logger.debug3("initCurrentCacheFile: " +
		  getContentDir().getName().getPath() +
						     File.separator +
						     CURRENT_FILENAME);
    try {
      currentCacheFile = getContentDir().resolveFile(CURRENT_FILENAME);
      if (logger.isDebug3()) {
	String name = currentCacheFile.getName().getBaseName();
	if (!CURRENT_FILENAME.equals(name)) {
	  throw new UnsupportedOperationException(name);
	}
      }
    } catch (FileSystemException e) {
      logger.error(CURRENT_FILENAME + " threw: " + e);
      currentCacheFile = null;
    }
  }

  private void initCurrentPropsFile() {
    logger.debug3("initCurrentPropsFile: " +
		  getContentDir().getName().getPath() +
						     File.separator +
						     CURRENT_PROPS_FILENAME);
    try {
      currentPropsFile = getContentDir().resolveFile(CURRENT_PROPS_FILENAME);
    } catch (FileSystemException e) {
      logger.error(CURRENT_PROPS_FILENAME + " threw: " + e);
      currentPropsFile = null;
    }
  }

  private void initTempCacheFile() {
    try {
      tempCacheFile = getContentDir().resolveFile(TEMP_FILENAME);
    } catch (FileSystemException e) {
      logger.error(TEMP_FILENAME + " threw: " + e);
      tempCacheFile = null;
    }
  }

  private void initTempPropsFile() {
    try {
      tempPropsFile = getContentDir().resolveFile(TEMP_PROPS_FILENAME);
    } catch (FileSystemException e) {
      logger.error(TEMP_PROPS_FILENAME + " threw: " + e);
      tempPropsFile = null;
    }
  }
  
  private void initAgreementFile(String fileName) {
    try {
      agreementFile = nodeRootFile.resolveFile(fileName);
    } catch (FileSystemException e) {
      logger.error(fileName + " threw: " + e);
      agreementFile = null;
    }
  }
  
  private void initTempAgreementFile(String fileName) {
    try {
      tempAgreementFile = nodeRootFile.resolveFile(fileName);
    } catch (FileSystemException e) {
      logger.error(fileName + " threw: " + e);
      tempAgreementFile = null;
    }
  }

  private void initNodePropsFile() {
    String propsLocation = nodeLocation + File.separator + NODE_PROPS_FILENAME;
    try {
      nodePropsFile = nodeRootFile.resolveFile(NODE_PROPS_FILENAME);
      logger.debug3(propsLocation + " props initialized");
    } catch (FileSystemException e) {
      logger.error(propsLocation + " threw: " + e);
      nodePropsFile = null;
    }
  }

  protected void initNodeRoot() {
    logger.debug3(nodeLocation + " node root location");
    try {
      nodeRootFile = repository.getFileSystem().resolveFile(nodeLocation);
    } catch (FileSystemException e) {
      logger.error(nodeLocation + " threw: " + e);
      nodeRootFile = null;
    }
  }

  protected FileObject getInactiveCacheFile() { // Used TestRepositoryNodeImpl
    try {
      FileObject ret = getContentDir().resolveFile(INACTIVE_FILENAME);
      logger.debug3("getInactiveCacheFile: " + ret.getName().getPath());
      return ret;
    } catch (FileSystemException e) {
      logger.error(nodeLocation + INACTIVE_FILENAME + " threw: " + e);
      return null;
    }
  }

  protected FileObject getInactivePropsFile() { // Used TestRepositoryNodeImpl
    try {
      return getContentDir().resolveFile(INACTIVE_PROPS_FILENAME);
    } catch (FileSystemException e) {
      logger.error(nodeLocation + File.separator +
		   INACTIVE_PROPS_FILENAME + " threw: " + e);
      return null;
    }
  }

  protected FileObject getContentDir() { // Used TestRepositoryNodeImpl
    String contentDirName = nodeLocation + File.separator + CONTENT_DIR;
    if (contentDir == null) {
      try {
	contentDir = repository.getFileSystem().resolveFile(contentDirName);
	logger.debug3("getContentDir - resolves to " +
		      contentDir.getName().getPath() + " vs " + contentDirName + " vs " +
		      contentDir.getName().getPath());
	if (logger.isDebug3()) {
	  String name = contentDir.getName().getBaseName();
	  if (!CONTENT_DIR.equals(name)) {
	    throw new UnsupportedOperationException(name);
	  }
	}
      } catch (FileSystemException e) {
	logger.error(nodeLocation + File.separator + CONTENT_DIR + " threw: " +
		     e);
	return null;
      }
    }
    return contentDir;
  }

  protected FileObject getFileObject(String name) { // Used TestRepositoryNodeImpl
    try {
      if (nodeRootFile == null) {
	nodeRootFile = repository.getFileSystem().resolveFile(nodeLocation);
      }
      return nodeRootFile.resolveFile(name);
    } catch (FileSystemException e) {
      logger.error(nodeLocation + File.separator + name + " threw: " + e);
      return null;
    }
  }

  // return array of version numbers of all present previous versions
  int[] getVersionNumbers() {
    FileObject[] files = null;
    try {
      files = getContentDir().getChildren();
    } catch (FileSystemException e) {
      logger.error("getVersionNumbers() threw: " + e);
    }
    if (files == null) {
      return new int[0];
    }
    int[] temp = new int[files.length];
    int count = 0;
    for (int ix = 0; ix < files.length; ix++) {
      try {
	FileName fn = files[ix].getName();
	logger.debug3("Index: " + ix + " = " + fn.getPath() + " " + fn.getBaseName());
	temp[count] = Integer.parseInt(fn.getBaseName());
      } catch (NumberFormatException e) {
	continue;
      }
      count++;
    }
    int[] res = new int[count];
    for (int ix = 0; ix < count; ix++) {
      res[ix] = temp[ix];
    }
    return res;
  }

  public RepositoryNodeVersion[] getNodeVersions() {
    return getNodeVersions(Integer.MAX_VALUE);
  }

  public RepositoryNodeVersion[] getNodeVersions(int maxVersions) {
    int[] vers = getVersionNumbers();
    Arrays.sort(vers);
    int size = vers.length + 1;
    if (size > maxVersions) size = maxVersions;
    RepositoryNodeVersion[] res = new RepositoryNodeVersion[size];
    res[0] = this;		  // most recent version (current) is first
    for (int ix = 1; ix < size; ix++) {
      res[ix] = new RepositoryNodeVersionImpl(vers[vers.length - ix]);
    }
    return res;
  }

  public RepositoryNodeVersion getNodeVersion(int version) {
    if (version == getCurrentVersion()) {
      return this;
    }
    return new RepositoryNodeVersionImpl(version);
  }

  // functions to get a 'versioned' content or props file, such as
  // '1', '1.props', or '1.props-123135131' (the dated props)

  protected FileObject getVersionedCacheFile(int version) { // Used TestRepositoryNodeImpl
    try {
      return getContentDir().resolveFile(Integer.toString(version));
    } catch (FileSystemException e) {
      logger.error(url + " v " + version + " threw: " + e);
      return null;
    }
  }

  protected FileObject getVersionedPropsFile(int version) { // Used TestRepositoryNodeImpl
    StringBuffer buffer = new StringBuffer();
    buffer.append(version);
    buffer.append(PROPS_EXTENSION);
    try {
      return getContentDir().resolveFile(buffer.toString());
    } catch (FileSystemException e) {
      logger.error(buffer + " threw: " + e);
      return null;
    }
  }

  protected FileObject getDatedVersionedPropsFile(int version, long date) { // Used TestRepositoryNodeImpl
    StringBuffer buffer = new StringBuffer();
    buffer.append(version);
    buffer.append(PROPS_EXTENSION);
    buffer.append("-");
    buffer.append(date);
    try {
      return getContentDir().resolveFile(buffer.toString());
    } catch (FileSystemException e) {
      logger.error(buffer + " threw: " + e);
      return null;
    }
  }

  /*
   * Version of PlatformUtil.updateAtomically for FileObject
   */
  protected static boolean updateAtomically(FileObject f1, FileObject f2) {
    boolean res = false;
    if (f1.canRenameTo(f2)) {
      String f1Name = f1.getName().getPath();
      try {
	f1.moveTo(f2);
	res = true;
      } catch (FileSystemException ex) {
	logger.error(f1Name + " moveTo threw: " + ex);
      }
    } else {
      String n1 = f1.getName().getPath();
      String n2 = f2.getName().getPath();
      logger.debug3("Can't rename " + n1 + " to " + n2);
    }
    return res;
  }

  /*
   * Version of FileUtil.isContentEqual() for FileObject
   */
  public static boolean isContentEqual(FileObject file1, FileObject file2)
      throws IOException {
    if ((file1==null) || (file2==null)) {
      // null is never equal
      return false;
    }

    if (!isFile(file1) || !isFile(file2)) {
      // don't compare directories
      return false;
    }

    // compare both streams
    InputStream is1 = null;
    InputStream is2 = null;

    try {
      FileContent fc1 = file1.getContent();
      FileContent fc2 = file2.getContent();
      if (fc1.getSize() != fc2.getSize()) {
	// easy length check
	return false;
      }

      is1 = fc1.getInputStream();
      is2 = fc2.getInputStream();

      return FileUtil.isContentEqual(is1, is2);
    } catch (FileSystemException e) {
      // if the file is absent, no comparison
      logger.error("isContentEqual() threw: " + e);
      return false;
    } finally {
      // make sure to close open input streams
      if (is1!=null) {
        is1.close();
      }
      if (is2!=null) {
        is2.close();
      }
    }
  }


  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[reponode: (");
    List flags = new ArrayList();
    if (newVersionOpen) flags.add("newver");
    if (newPropsSet) flags.add("np");
    if (wasInactive) flags.add("wasinact");
    if (nodePropsLoaded) flags.add("p");
    if (curOutputStream != null) flags.add("hasout");
    sb.append(StringUtil.separatedString(flags, ","));
    sb.append(") ver: ");
    sb.append(currentVersion);
    sb.append(", ");
    sb.append(url);
    sb.append("]");
    return sb.toString();
  }

  public class RepositoryNodeVersionImpl implements RepositoryNodeVersion {
    private int version;
    private FileObject contentFile = null;

    RepositoryNodeVersionImpl(int version) {
      this.version = version;
    }

    public boolean hasContent() {
      boolean ret = false;
      try {
	ret = getContentFile().exists();
      } catch (FileSystemException e) {
	// no action intended
      }
      return ret;
    }

    private FileObject getContentFile() {
      if (contentFile == null) {
	contentFile = getVersionedCacheFile(version);
      }
      return contentFile;
    }

    public long getContentSize() {
      if (!hasContent()) {
	throw new UnsupportedOperationException("Version has no content");
      }
      try {
	return getContentFile().getContent().getSize();
      } catch (FileSystemException e) {
	throw new UnsupportedOperationException("GetContentSize: " +
						e.toString());
      }
    }

    public int getVersion() {
      return version;
    }

    public RepositoryNode.RepositoryNodeContents getNodeContents() {
      if (!hasContent()) {
	throw new UnsupportedOperationException("No content for version " +
						getVersion() + "of " + url);
      }
      return new VRepositoryNodeContentsImpl(version);
    }

    class VRepositoryNodeContentsImpl extends RepositoryNodeContentsImpl {
      private int version;
      private Properties props = null;

      private VRepositoryNodeContentsImpl(int version) {
	this.version = version;
      }

      protected FileObject getContentFile() {
	return RepositoryNodeVersionImpl.this.getContentFile();
      }

      protected Properties getProps() {
	if (props == null) {
	  FileObject propsFile = getVersionedPropsFile(version);
	  try {
	    props = loadProps(propsFile);
	  } catch (IOException e) {
	    throw new LockssRepository.RepositoryStateException("Couldn't load versioned properties file: " + propsFile, e);
	  }
	}
	return props;
      }

      protected void handleOpenError(IOException e) {
      }
  }

  }



  /**
   * Intended to ensure props and stream reflect a consistent view of a
   * single version.  This version gurantees that only if the stream is
   * fetched only once.
   */
  class RepositoryNodeContentsImpl implements RepositoryNodeContents {
    private Properties props;
    private InputStream is;

    private RepositoryNodeContentsImpl() {
    }

    public synchronized InputStream getInputStream() {
      ensureInputStream();
      InputStream res = is;
      // stream can only be used once.
      is = null;
      return res;
    }

    public synchronized Properties getProperties() {
      if (props == null) {
	ensureInputStream();
      }
      return props;
    }

    public synchronized void release() {
      if (is != null) {
	if (CurrentConfig.getBooleanParam(PARAM_ENABLE_RELEASE,
	                                  DEFAULT_ENABLE_RELEASE)) {
	  try {
	    is.close();
	  } catch (IOException e) {
	    logger.warning("Error closing RNC stream", e);
	  }
	}
	is = null;
      }
    }

    private void assertContent() {
      if (!hasContent()) {
	throw new UnsupportedOperationException("No content for url '" +
						url + "'");
      }
    }

    protected FileObject getContentFile() {
      return curInputFile;
    }

    protected Properties getProps() {
      return (Properties)curProps.clone();
    }

    protected void handleOpenError(IOException e) {
      if (!FileUtil.isTemporaryResourceException(e)) {
	maybeDeactivateInconsistentNode();
      }
    }

    private void ensureInputStream() {
      if (is == null) {
	assertContent();
	FileObject f = getContentFile();
	String url = "Bad URL";
	url = f.getName().getPath();
	try {
	  if (!f.exists()) {
	    throw new FileNotFoundException(url);
	  }
	  is = new BufferedInputStream(f.getContent().getInputStream());
	  if (CurrentConfig.getBooleanParam(PARAM_MONITOR_INPUT_STREAMS,
	                                    DEFAULT_MONITOR_INPUT_STREAMS)) {
	    is = new MonitoringInputStream(is, url);
	  }
	  props = getProps();
	} catch (IOException e) {
	  logger.error("Couldn't get inputstream for '" + url + "'");
	  handleOpenError(e);
	  throw new LockssRepository.RepositoryStateException ("Couldn't open InputStream: " + e.toString());
	}
      }
    }
  }


  /**
   * Simple comparator which uses File.compareTo() for sorting.
   */
  private static class FileObjectComparator implements Comparator {
    public int compare(Object o1, Object o2) {
      // compares file pathnames
      return ((FileObject)o1).getName().compareTo(((FileObject)o2).getName());
    }
  }
}
