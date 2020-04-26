/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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
import java.nio.charset.*; 
import java.net.MalformedURLException;
import java.util.*;
import java.util.Queue;

import org.apache.commons.io.FileUtils;
import org.apache.oro.text.regex.*;

import org.lockss.config.*;
import org.lockss.protocol.*;
import org.lockss.daemon.CachedUrlSetSpec;
import org.lockss.plugin.AuUrl;
import org.lockss.util.*;
import org.lockss.repository.RepositoryManager.CheckUnnormalizedMode;

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
  public static final String LOCKSS_VERSION_NUMBER =
    "org.lockss.version.number";
  static final String NODE_WAS_INACTIVE_PROPERTY = "org.lockss.node.was.inactive";
  // properties set in the node properties
  static final String INACTIVE_CONTENT_PROPERTY = "node.content.isInactive";
  static final String DELETION_PROPERTY = "node.isDeleted";

  static final String TREE_SIZE_PROPERTY = "node.tree.size";
  static final String CHILD_COUNT_PROPERTY = "node.child.count";
  // Token used in above props to indicate explicitly invalid.  Used to
  // distinguish invalidated from never-been-set.
  static final String INVALID = "U";

  public static final String PARAM_KEEP_ALL_PROPS_FOR_DUP_FILE =
    Configuration.PREFIX + "repository.keepAllPropsForDupFile";
  public static final boolean DEFAULT_KEEP_ALL_PROPS_FOR_DUP_FILE = false;

  public static final String PARAM_INVALIDATE_CACHED_SIZE_ON_DUP_STORE =
    Configuration.PREFIX + "repository.invalidateCachedSizeOnDupStore";
  public static final boolean DEFAULT_INVALIDATE_CACHED_SIZE_ON_DUP_STORE =
    true;



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
  private File curInputFile;
  protected Properties curProps;
  protected Properties nodeProps = new Properties();
  private boolean nodePropsLoaded = false;
  protected int currentVersion = -1;

  // convenience file handles
  protected File contentDir = null;

  protected File nodeRootFile = null;
  protected File nodePropsFile = null;
  protected File agreementFile = null;
  protected File tempAgreementFile = null;
//  protected File ppisAgreementFile = null;
  protected File currentCacheFile;
  protected File currentPropsFile;
  File tempCacheFile;
  File tempPropsFile;

  // identity url and location
  protected String url;
  protected String nodeLocation;
  
  // Shared with AuNodeImpl
  protected static final Logger logger = Logger.getLogger("RepositoryNode");
  
  protected LockssRepositoryImpl repository;
  // preset so testIllegalOperations() doesn't null pointer
  private Deadline versionTimeout = Deadline.MAX;

  /*
   * Keys that may be added to a the properties of a sealed node.
   */
  private static final String[] allowedKeys = {
    org.lockss.plugin.CachedUrl.PROPERTY_CHECKSUM,
  };

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

  Object treeSizeLock = new Object();

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
    // Don't allow two threads to calculate size of same node
    synchronized (treeSizeLock) {
      // Check cache again when lock obtained.  Don't think this is
      // double-checked locksing anti-pattern due to value being in
      // Properties, but only harm would be to redundantly recalc a single
      // node.
      if (filter==null) {
	String treeSize = nodeProps.getProperty(TREE_SIZE_PROPERTY);
	if (isPropValid(treeSize)) {
	  // return if found
	  logger.debug2("Found cached size at " + nodeLocation);
	  return Long.parseLong(treeSize);
	}
      }
      long totalSize = 0;
      if (hasContent()) {
	totalSize = currentCacheFile.length();
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
    File[] children = nodeRootFile.listFiles();
    if (children == null) {
      String msg = "No cache directory located for: " + url;
      logger.error(msg);
      throw new LockssRepository.RepositoryStateException(msg);
    }
    for (int ii=0; ii<children.length; ii++) {
      File child = children[ii];
      if (child.getName().equals(contentDir.getName())) continue;
      if (!child.isDirectory()) continue;
      return false;
    }
    return true;
  }

  public Iterator listChildren(CachedUrlSetSpec filter, boolean includeInactive) {
    return getNodeList(filter, includeInactive).iterator();
  }

  /**
   * Assembles a list of immediate children, possibly filtered.  The order
   * of the returned list is undefined.
   * @param filter a spec to filter on.  Null for no filtering.
   * @param includeInactive true iff inactive nodes to be included
   * @return List the child list of RepositoryNodes
   */
  protected List getNodeList(CachedUrlSetSpec filter, boolean includeInactive) {
    if (nodeRootFile == null)
      initNodeRoot();
    if (contentDir == null)
      getContentDir();
    File[] children = nodeRootFile.listFiles();
    if (children == null) {
      String msg = "No cache directory located for: " + url;
      logger.error(msg);
      throw new LockssRepository.RepositoryStateException(msg);
    }
    
    if (RepositoryManager.isEnableLongComponents()) {
      // Trigger long-component code only if at least one encoded component
      // is present.  There are AUs in the field with >500,000 nodes at a
      // single level, and the new code requires several times as much
      // storage.  Also less risk of a bug affecting existing repositories.
      for (File file : children) {
	if (file.getName().endsWith("\\")) {
	  return enumerateEncodedChildren(children, filter, includeInactive);
	}
      }
    }
    int listSize;
    if (filter==null) {
      listSize = children.length;
    } else {
      // give a reasonable minimum since, if it's filtered, the array size
      // may be much smaller than the total children, particularly in very
      // flat trees
      listSize = Math.min(40, children.length);
    }

    CheckUnnormalizedMode unnormMode =
      RepositoryManager.getCheckUnnormalizedMode();

    ArrayList childL = new ArrayList(listSize);
    for (int ii=0; ii<children.length; ii++) {
      File child = children[ii];
      if ((child.getName().equals(CONTENT_DIR)) || (!child.isDirectory())) {
        // all children are in their own directories, and the content dir
        // must be ignored
        continue;
      }
      switch (unnormMode) {
      case Log:
      case Fix:
	child = checkUnnormalized(unnormMode, child, children, ii);
	break;
      }
      if (child == null) {
	continue;
      }
      String childUrl = constructChildUrl(url, child.getName());
      if ((filter==null) || (filter.matches(childUrl))) {
        try {
          RepositoryNode node = repository.getNode(childUrl);
	  if (node != null) {
	    
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
	  } else {
	    logger.warning("Child node not found; disappeared or is unnormalized: " + childUrl);
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

  private List enumerateEncodedChildren(File[] children,
					CachedUrlSetSpec filter,
					boolean includeInactive) {
    // holds fully decoded immediate children
    List<File> expandedDirectories = new ArrayList<File>();
    
    // holds immediate children that still need to be decoded, and may
    // yield more than one expanded child
    Queue<File> unexpandedDirectories = new LinkedList<File>();

    // add initial set of unexpanded directories
    for (File file : children) {
      if (file.getName().endsWith("\\")) {
        unexpandedDirectories.add(file);
      } else {
        expandedDirectories.add(file);
      }
    }
    
    // keep expanding directories until no more unexpanded directories exist
    // core algorithm: BFS
    while(!unexpandedDirectories.isEmpty()) {
      File child = unexpandedDirectories.poll();
      if(child.getName().endsWith("\\")) {
        File[] newChildren = child.listFiles();
        for(File newChild : newChildren) {
          unexpandedDirectories.add(newChild);
        }
      } else {
        expandedDirectories.add(child);
      }
    }
    
    // using iterator to traverse safely
    Iterator<File> iter = expandedDirectories.iterator();
    while(iter.hasNext()) {
      File child = iter.next();
      if ((child.getName().equals(CONTENT_DIR)) || (!child.isDirectory())) {
        // iter remove instead of list.remove
        iter.remove();
      }
    }
    
    // normalization needed?
    CheckUnnormalizedMode unnormMode =
      RepositoryManager.getCheckUnnormalizedMode();

    // We switch to using a sorted set, this time we hold strings
    // representing the url
    List<String> subUrls = new ArrayList<String>();
    for(File child : expandedDirectories) {
      try{
        // http://root/child -> /child
        String location = child.getCanonicalPath().substring(nodeRootFile.getCanonicalFile().toString().length());
        location = decodeUrl(location);
        String oldLocation = location;
	switch (unnormMode) {
	case Log:
	case Fix:
          // Normalization done here against the url string, instead of
          // against the file in the repository. This alleviates us from
          // dealing with edge conditions where the file split occurs
          // around an encoding. e.g. %/5C is special in file, but decoded
          // URL string is %5C and we handle it correctly.
          location = normalizeTrailingQuestion(location);
          location = normalizeUrlEncodingCase(location);
          if(!oldLocation.equals(location)) {
	    switch (unnormMode) {
	    case Fix:
	      // most dangerous part done here, where we copy and
	      // delete. Maybe we should move to a lost in found instead? :)
	      String newRepoLocation = LockssRepositoryImpl.mapUrlToFileLocation(repository.getRootLocation(), url + location);
	      logger.debug("Fixing unnormalized " + oldLocation + " => "
			   + location);
	      FileUtils.copyDirectory(child, new File(newRepoLocation));
	      FileUtils.deleteDirectory(child);
	      break;
	    case Log:
	      logger.debug("Detected unnormalized " + oldLocation +
			   ", s.b. " + location);
	      break;
	    }
          }
	  break;
        }
        location = url + location;
        subUrls.add(location);
      } catch (IOException e) {
	logger.error("Normalizing (" + unnormMode + ") " + child, e);
      } catch(NullPointerException ex) {
	logger.error("Normalizing (" + unnormMode + ") " + child, ex);
      }
    }
    
    int listSize;
    if (filter == null) {
      listSize = subUrls.size();
    } else {
      // give a reasonable minimum since, if it's filtered, the array size
      // may be much smaller than the total children, particularly in very
      // flat trees
      listSize = Math.min(40, subUrls.size());
    }

    // generate the arraylist with urls and return
    ArrayList childL = new ArrayList();
    for(String childUrl : subUrls) {
      if ((filter == null) || (filter.matches(childUrl))) {
        try {
          RepositoryNode node = repository.getNode(childUrl);
          if(node == null)
            continue;
          // add all nodes which are internal or active leaves
          // deleted nodes never included
          // boolean activeInternal = !node.isLeaf() && !node.isDeleted();
          // boolean activeLeaf = node.isLeaf() && !node.isDeleted() &&
          // (!node.isContentInactive() || includeInactive);
          // if (activeInternal || activeLeaf) {
          if (!node.isDeleted() && (!node.isContentInactive() || (includeInactive || !node .isLeaf()))) {
            childL.add(node);
          }
        } catch (MalformedURLException ignore) {
          // this can safely skip bad files because they will
          // eventually be trimmed by the repository integrity checker
          // and the content will be replaced by a poll repair
          logger.error("Malformed child url: " + childUrl);
        }
      }
    }
    return childL;
  }

  private static Pattern UNNORMALIZED =
    RegexpUtil.uncheckedCompile(".*%([a-z]|.[a-z]).*",
				Perl5Compiler.READ_ONLY_MASK);

  File normalize(File file) {
    String name = file.getName();
    String normName = normalizeUrlEncodingCase(name);
    normName = normalizeTrailingQuestion(normName);
    if (normName.equals(name)) {
      return file;
    }
    return new File(file.getParent(), normName);
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

  File checkUnnormalized(RepositoryManager.CheckUnnormalizedMode unnormMode,
			 File file, File[] all, int ix) {
    File norm = normalize(file);
    if (norm == file) {
      return file;
    }
    switch (unnormMode) {
    case Fix:
      synchronized (this) {
	if (file.exists()) {
	  if (norm.exists()) {
	    if (FileUtil.delTree(file)) {
	      logger.debug("Deleted redundant unnormalized: " + file);
	    } else {
	      logger.error("Couldn't delete unnormalized: " + file);
	    }
	    all[ix] = null;
	  } else {
	    if (file.renameTo(norm)) {
	      logger.debug("Renamed unnormalized: " + file + " to " + norm);
	      all[ix] = norm;
	    } else {
	      logger.error("Couldn't rename unnormalized: " + file +
			   " to " + norm);
	      all[ix] = null;
	    }
	  }
	}
      }
      return all[ix];
    case Log:
      logger.debug("Detected unnormalized " + file + ", s.b. " + norm);
      return file;
    default:
      return file;
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
    // We're operating on the URL, so looking for the last slash doesn't
    // work if there's a query arg containing a slash.  Should really
    // operate on the path, but then need to handle long components, and
    // the node cache is keyed by URL.  As a hack, strip off any query arg.
    int queryIx = childUrl.indexOf("?");
    if (queryIx >= 0) {
      childUrl = childUrl.substring(0, queryIx);
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
    if (!nodeRootFile.exists()) {
      logger.debug3("Creating root directory for CUS '"+url+"'");
      if (!nodeRootFile.mkdirs()) {
	throw new LockssRepository.RepositoryStateException("mkdirs(" +
							    nodeRootFile +
							    ") failed for "
							    + nodeRootFile);
      }
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

    // Needs to be done unconditionally in case node or content dir has
    // disappeared.  (Was:   if ( currentVersion == 0) {  )
    if (!FileUtil.ensureDirExists(contentDir)) {
      logger.error("Couldn't create cache directory: " +contentDir);
      throw new LockssRepository.RepositoryStateException("mkdirs(" +
							  contentDir +
							  ") failed.");
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
    isIdenticalVersion = false;
  }

  private boolean isIdenticalVersion = false;

  public boolean isIdenticalVersion() {
    return isIdenticalVersion;
  }

  public synchronized void sealNewVersion() {
    try {
      if (curOutputStream==null) {
	throw new UnsupportedOperationException("getNewOutputStream() not called, or already sealed.");
      }
      // make sure outputstream was closed
      IOUtil.safeClose(curOutputStream);
      curOutputStream = null;

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
             !PlatformUtil.updateAtomically(inactiveCacheFile,
                                            currentCacheFile))
            ||
            (inactivePropsFile.exists() &&
             !PlatformUtil.updateAtomically(inactivePropsFile,
                                            currentPropsFile))) {
          logger.error("Couldn't rename inactive versions: " + url);
          throw new LockssRepository.RepositoryStateException(
              "Couldn't rename inactive versions.");
        }

        // add the 'was inactive' property so the knowledge isn't lost
        try {
          Properties myProps = loadProps(currentPropsFile);
          myProps.setProperty(NODE_WAS_INACTIVE_PROPERTY, "true");
	  writeProps(currentPropsFile, myProps, url);
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
      if (currentCacheFile.exists()) {
        try {
          // if identical, don't rename
          if (FileUtil.isContentEqual(currentCacheFile, tempCacheFile)) {
            // don't rename
	    if (logger.isDebug2()) {
	      logger.debug2("New version identical to old: " +
			    currentCacheFile);
	    }
            isIdenticalVersion = true;
          } else if (!PlatformUtil.updateAtomically(currentCacheFile,
                                                    getVersionedCacheFile(currentVersion))) {
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
      if (isIdenticalVersion) {
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

      if (CurrentConfig.getBooleanParam(PARAM_KEEP_ALL_PROPS_FOR_DUP_FILE,
                                        DEFAULT_KEEP_ALL_PROPS_FOR_DUP_FILE)
                                        || !isIdenticalVersion) {
	// rename current properties to chosen file name
	if (currentPropsFile.exists() &&
	    !PlatformUtil.updateAtomically(currentPropsFile,
	                                   verPropsFile)) {
	  String err = "Couldn't rename current property file: " + url;
	  logger.error(err);
	  throw new LockssRepository.RepositoryStateException(err);
	}
      }

      // if not identical, rename content from 'temp' to 'current'
      if (!isIdenticalVersion) {
        // rename new content file (if non-identical)
        if (!PlatformUtil.updateAtomically(tempCacheFile,
                                           currentCacheFile)) {
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
          Properties myProps = loadProps(tempPropsFile);
          myProps.setProperty(LOCKSS_VERSION_NUMBER,
                              Integer.toString(currentVersion));
	  writeProps(tempPropsFile, myProps, url);
        } catch (IOException ioe) {
          String err = "Couldn't reset property version number: " + url;
          logger.error(err);
          throw new LockssRepository.RepositoryStateException(err);
        }
      }

      // rename new properties
      if (!PlatformUtil.updateAtomically(tempPropsFile,
                                         currentPropsFile)) {
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
      if (!isIdenticalVersion ||
	  CurrentConfig.getBooleanParam(PARAM_INVALIDATE_CACHED_SIZE_ON_DUP_STORE,
                                        DEFAULT_INVALIDATE_CACHED_SIZE_ON_DUP_STORE)) {
        invalidateCachedValues(true);
      }
    }
  }

  private void writeProps(File toFile, Properties props, String url)
      throws IOException {
    OutputStream os = null;
    try {
      os = new BufferedOutputStream(FileUtil.newFileOutputStream(toFile));
      props.store(os, "HTTP headers for " + url);
    } finally {
      IOUtil.safeClose(os);
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
          !PlatformUtil.updateAtomically(currentCacheFile,
                                         getInactiveCacheFile()))
          ||
          (currentPropsFile.exists() &&
          !PlatformUtil.updateAtomically(currentPropsFile,
                                         getInactivePropsFile()))) {
        logger.error("Couldn't deactivate: " + url);
        throw new LockssRepository.RepositoryStateException(
            "Couldn't deactivate.");
      }
    } else {
      if (!contentDir.exists()) {
	logger.warning("Creating content dir in order to deactivate node: " +
		       url);
        contentDir.mkdirs();
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

        if (!PlatformUtil.updateAtomically(inactiveCacheFile,
                                           currentCacheFile) ||
            !PlatformUtil.updateAtomically(inactivePropsFile,
                                           currentPropsFile)) {
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
    if (!PlatformUtil.updateAtomically(lastContentFile,
                                       currentCacheFile) ||
        !PlatformUtil.updateAtomically(lastPropsFile,
                                       currentPropsFile)) {
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
          FileUtil.newFileOutputStream(tempCacheFile));
      return curOutputStream;
    } catch (IOException e) {
//       logFailedCreate(tempCacheFile);
      try {
	logger.error("No new version file for "+tempCacheFile.getPath()+".", e);
        throw new LockssRepository.RepositoryStateException("Couldn't create/write to repository file.", e);
      } finally {
        abandonNewVersion();
      }
    }
  }

  void logFailedCreate(File f) {
    String sss = tempCacheFile.toString();
    logger.error("FNF: isAscii: " + StringUtil.isAscii(sss));
    logger.error("FNF: len: " + sss.length());
    for (String s : StringUtil.breakAt(sss, "/")) {
      logger.error("comp: " + s.length() + " : " + s);
    }
  }

  SortedProperties loadProps(File propsFile)
      throws FileNotFoundException, IOException {
    SortedProperties props = new SortedProperties();
    loadPropsInto(propsFile, props);
    return props;
  }

  void loadPropsInto(File propsFile, Properties props)
      throws FileNotFoundException, IOException {
    InputStream is =
      new BufferedInputStream(FileUtil.newFileInputStream(propsFile));
    try {
      props.load(is);
    } catch (IllegalArgumentException e) {
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
        int versionToWrite = currentVersion + 1;
        if (versionToWrite <= 0) {
          // this is an error condition which shouldn't occur
          logger.error("Content being written with version of '0'; adjusting to '1'");
          currentVersion = 0;
          versionToWrite = 1;
        }
        myProps.setProperty(LOCKSS_VERSION_NUMBER, Integer.toString(versionToWrite));
	writeProps(tempPropsFile, myProps, url);
      } catch (IOException ioe) {
        try {
          logger.error("Couldn't write properties for " +
                       tempPropsFile.getPath()+".", ioe);
          throw new LockssRepository.RepositoryStateException("Couldn't write properties file.", ioe);
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
	  if (!PlatformUtil.updateAtomically(nodePropsFile,
					     new File(nodePropsFile.getAbsolutePath()
						      + FAULTY_FILE_EXTENSION))) {
	    logger.error("Error renaming nodeProps file");
	  }
	}
      }

      // no content, so version 0
      if (!contentDir.exists()) {
        currentVersion = 0;
        curInputFile = null;
        curProps = null;
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
    initNodeRoot();
    initCurrentCacheFile();
    initCurrentPropsFile();
    initTempCacheFile();
    initTempPropsFile();
    initNodePropsFile();
  }

  
  /**
   * Load the node properties.
   */
  void loadNodeProps(boolean okIfNotThere) {
    try {
      // check properties to see if deleted
      loadPropsInto(nodePropsFile, nodeProps);
      nodePropsLoaded = true;
    } catch (FileNotFoundException e) {
      if (!okIfNotThere) {
	String msg = "No node props file: " + nodePropsFile.getPath();
	logger.error(msg);
	throw new LockssRepository.RepositoryStateException(msg);
      }
    } catch (Exception e) {
      logger.error("Error loading node props from " + nodePropsFile.getPath(),
		   e);
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
      initAgreementFile();
    }
    
    DataInputStream is = null;
    try {
//      ppisReturn = new PersistentPeerIdSetImpl(ppisAgreementFile, repository.getDaemon().getIdentityManager());
      ppisReturn = new PersistentPeerIdSetImpl(agreementFile, repository.getDaemon().getIdentityManager());
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
      PlatformUtil.updateAtomically(agreementFile,
                                    new File(agreementFile.getCanonicalFile() + ".old"));
    } catch (IOException ex) {
      // This would only be caused by getCanonicalFile() throwing IOException.
      // Worthy of a stack trace.
      logger.error("Unable to back-up suspect agreement history file:", ex);
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
      if ((getInactiveCacheFile().exists()) && (!currentCacheFile.exists())) {
	currentVersion = INACTIVE_VERSION;
	curInputFile = null;
	curProps = null;
	return true;
      }
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
	loadPropsInto(inactivePropFile, oldProps);
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
        if (getContentDir().exists()) {
          logger.debug("Content dir exists, though no content expected.");
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

    // -if node properties exists, check that it's readable
    if (nodePropsFile.exists()) {
      try {
        loadNodeProps(false);
      } catch (LockssRepositoryImpl.RepositoryStateException rse) {
        logger.warning("Renaming faulty 'nodeProps' to 'nodeProps.ERROR'");
        // as long as the rename goes correctly, we can proceed
        if (!PlatformUtil.updateAtomically(nodePropsFile,
                                           new File(nodePropsFile.getAbsolutePath()
                                                    + FAULTY_FILE_EXTENSION))) {
          logger.error("Error renaming nodeProps file");
          return false;
        }
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
  boolean ensureDirExists(File dirFile) {
    // --rename if a file at that position
    if (dirFile.isFile()) {
      logger.error("Exists but as a file: " + dirFile.getAbsolutePath());
      logger.error("Renaming file to 'xxx.ERROR'...");
      if (!PlatformUtil.updateAtomically(dirFile,
                                         new File(dirFile.getAbsolutePath()
                                                  + FAULTY_FILE_EXTENSION))) {
        logger.error("Error renaming file");
        return false;
      }
    }

    // create the dir if absent
    if (!dirFile.exists()) {
      logger.warning("Directory missing: " + dirFile.getAbsolutePath());
      logger.warning("Creating directory...");
      dirFile.mkdirs();
    }
    // make sure no problems
    return (dirFile.exists() && dirFile.isDirectory());
  }

  /**
   * Checks to see if the file exists.  Returns false if it doesn't, or if
   * it's a directory.  Renames the directory to 'dir.ERROR' before returning,
   * so that the node can be fixed later.
   * @param testFile File to test
   * @param desc the file description, used for logging
   * @return boolean true iff the file exists
   */
  static boolean checkFileExists(File testFile, String desc) {
    if (!testFile.exists()) {
      logger.warning(desc+" not found.");
      return false;
    } else if (testFile.isDirectory()) {
      logger.error(desc+" a directory.");
      PlatformUtil.updateAtomically(testFile,
                                    new File(testFile.getAbsolutePath()
                                             + FAULTY_FILE_EXTENSION));
      return false;
    }
    return true;
  }

  /**
   * Checks the accuracy of the cached child count, and invalidates it
   * if wrong.
   */
  void checkChildCountCacheAccuracy() {
    int count = 0;
    List children = getNodeList(null, true);
    if (children == null) {
      String msg = "No cache directory located for: " + url;
      logger.error(msg);
      throw new LockssRepository.RepositoryStateException(msg);
    }
    count = children.size();

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
      OutputStream os =
	new BufferedOutputStream(FileUtil.newFileOutputStream(nodePropsFile));
      nodeProps.store(os, "Node properties");
      os.close();
    } catch (IOException ioe) {
      logger.error("Couldn't write node properties for " +
                   nodePropsFile.getPath()+".", ioe);
      throw new LockssRepository.RepositoryStateException("Couldn't write node properties file.", ioe);
    }
  }

  // functions to initialize the file handles

  private void initCurrentCacheFile() {
    currentCacheFile = new File(getContentDir(), CURRENT_FILENAME);
  }

  private void initCurrentPropsFile() {
    currentPropsFile = new File(getContentDir(), CURRENT_PROPS_FILENAME);
  }

  private void initTempCacheFile() {
    tempCacheFile = new File(getContentDir(), TEMP_FILENAME);
  }

  private void initTempPropsFile() {
    tempPropsFile = new File(getContentDir(), TEMP_PROPS_FILENAME);
  }
  
  private void initAgreementFile() {
    agreementFile = new File(nodeLocation, AGREEMENT_FILENAME);
//    ppisAgreementFile = new File(nodeLocation, AGREEMENT_FILENAME + ".ppis");
  }
  
  private void initTempAgreementFile() {
    tempAgreementFile = new File(nodeLocation, TEMP_AGREEMENT_FILENAME);
  }

  private void initNodePropsFile() {
    nodePropsFile = new File(nodeLocation, NODE_PROPS_FILENAME);
  }

  protected void initNodeRoot() {
    nodeRootFile = new File(nodeLocation);
  }

  File getInactiveCacheFile() {
    return new File(getContentDir(), INACTIVE_FILENAME);
  }

  File getInactivePropsFile() {
    return new File(getContentDir(), INACTIVE_PROPS_FILENAME);
  }

  File getContentDir() {
    if (contentDir == null) {
      contentDir = new File(nodeLocation, CONTENT_DIR);
    }
    return contentDir;
  }

  // return array of version numbers of all present previous versions
  int[] getVersionNumbers() {
    String[] names = getContentDir().list(NumericFilenameFilter.INSTANCE);
    if (names == null) {
      return new int[0];
    }
    int[] res = new int[names.length];
    for (int ix = names.length - 1; ix >= 0; ix--) {
      try {
	res[ix] = Integer.parseInt(names[ix]);
      } catch (NumberFormatException e) {
	logger.warning("Non-numeric numeric filename: " +
		       names[ix] + ": " + e.getMessage());
      }
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

  File getVersionedCacheFile(int version) {
    return new File(getContentDir(), Integer.toString(version));
  }

  File getVersionedPropsFile(int version) {
    StringBuffer buffer = new StringBuffer();
    buffer.append(version);
    buffer.append(PROPS_EXTENSION);
    return new File(getContentDir(), buffer.toString());
  }

  File getDatedVersionedPropsFile(int version, long date) {
    StringBuffer buffer = new StringBuffer();
    buffer.append(version);
    buffer.append(PROPS_EXTENSION);
    buffer.append("-");
    buffer.append(date);
    return new File(getContentDir(), buffer.toString());
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

  public RepositoryNodeContents getUnsealedRnc() {
    if (!newVersionOpen) {
      throw new IllegalStateException("UnsealedNodeContents available only when new version open");
    }
    return new UnsealedNodeContents();
  }

  class UnsealedNodeContents implements RepositoryNodeContents {
    List<InputStream> streams = new ArrayList<InputStream>();

    @Override
    public InputStream getInputStream() {
      if (!newVersionOpen) {
	throw new IllegalStateException("UnsealedNodeContents not usable after seal()");
      }
      try {
	InputStream is =
	  new BufferedInputStream(FileUtil.newFileInputStream(tempCacheFile));
	if (CurrentConfig.getBooleanParam(PARAM_MONITOR_INPUT_STREAMS,
					  DEFAULT_MONITOR_INPUT_STREAMS)) {
	  is = new MonitoringInputStream(is, tempCacheFile.toString());
	}
	streams.add(is);
	return is;
      } catch (IOException e) {
	logger.error("Couldn't get inputstream for tempCacheFile: " +
		     tempCacheFile, e);
	throw new LockssRepository.RepositoryStateException ("Couldn't open TempInputStream: " + e.toString());
      }
    }


    @Override
    public Properties getProperties() {
      throw new UnsupportedOperationException("Not allowed for UnsealedNodeContents");
    }

    @Override
    public void addProperty(String key, String value) {
      throw new UnsupportedOperationException("Not allowed for UnsealedNodeContents");
    }

    @Override
    public synchronized void release() {
      for (InputStream is : streams) {
        IOUtil.safeClose(is);
      }
      streams = new ArrayList<InputStream>();
    }
  }

  public class RepositoryNodeVersionImpl implements RepositoryNodeVersion {
    private int version;
    private File contentFile = null;

    RepositoryNodeVersionImpl(int version) {
      this.version = version;
    }

    public boolean hasContent() {
      return getContentFile().exists();
    }

    private File getContentFile() {
      if (contentFile == null) {
	contentFile = getVersionedCacheFile(version);
      }
      return contentFile;
    }

    public long getContentSize() {
      if (!hasContent()) {
	throw new UnsupportedOperationException("Version has no content");
      }
      return getContentFile().length();
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

      protected File getContentFile() {
	return RepositoryNodeVersionImpl.this.getContentFile();
      }

      protected File getPropsFile() {
	return getVersionedPropsFile(version);
      }

      protected Properties getProps() {
	if (props == null) {
	  File propsFile = getVersionedPropsFile(version);
	  try {
	    props = loadProps(propsFile);
	  } catch (IOException e) {
	    throw new LockssRepository.RepositoryStateException("Couldn't load versioned properties file: " + propsFile, e);
	  }
	}
	return props;
      }

      protected void updateProps(Properties newProps) {
	props = newProps;
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
    List<InputStream> streams = new ArrayList<InputStream>();

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

    protected void updateProps(Properties newProps) {
      props = newProps;
      curProps = props;
    }

    /**
     * Add a new property - only to be used for a restricted set of properties
     * @param key
     * @param value
     */
    public synchronized void addProperty(String key, String value) {
      for (String allowed : allowedKeys) {
	if (allowed.equalsIgnoreCase(key)) {
	  addProperty0(key, value);
	  return;
	}
      }
      throw new IllegalArgumentException("Not allowed: " + key);
    }

    private void addProperty0(String key, String value) {
      CIProperties newProps = CIProperties.fromProperties(getProps());
      if (newProps.containsKey(key)) {
	throw new IllegalStateException("Props already contain: " + key);
      }
      newProps.setProperty(key, value);

      updateCurrentVerProperties(newProps);
    }

    private void updateCurrentVerProperties(Properties props) {
      File tempVerPropsFile =
	new File(getContentDir(), TEMP_PROPS_FILENAME + "." + currentVersion);
      try {
	writeProps(tempVerPropsFile, props, url);
      } catch (IOException e) {
	String err = "Couldn't add property: " + url;
	logger.error(err, e);
	throw new LockssRepository.RepositoryStateException(err, e);
      }
      // rename new properties
      File propsFile = getPropsFile();

      if (!PlatformUtil.updateAtomically(tempVerPropsFile, propsFile)) {
	String err = "Couldn't rename temp property version: " + url;
	logger.error(err);
	throw new LockssRepository.RepositoryStateException(err);
      }
      updateProps(props);
    }

    public synchronized void release() {
      if (CurrentConfig.getBooleanParam(PARAM_ENABLE_RELEASE,
					DEFAULT_ENABLE_RELEASE)) {
	for (InputStream is : streams) {
	  IOUtil.safeClose(is);
	}
	streams = new ArrayList<InputStream>();
      }
    }

    private void assertContent() {
      if (!hasContent()) {
	throw new UnsupportedOperationException("No content for url '" +
						url + "'");
      }
    }

    protected File getContentFile() {
      return curInputFile;
    }

    protected File getPropsFile() {
      return currentPropsFile;
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
	try {
	  is = new BufferedInputStream(FileUtil.newFileInputStream(getContentFile()));
	  if (CurrentConfig.getBooleanParam(PARAM_MONITOR_INPUT_STREAMS,
	                                    DEFAULT_MONITOR_INPUT_STREAMS)) {
	    is = new MonitoringInputStream(is, getContentFile().toString());
	  }
	  streams.add(is);
	  props = getProps();
	} catch (IOException e) {
	  logger.error("Couldn't get inputstream for '" +
		       getContentFile().getPath() + "'");
	  handleOpenError(e);
	  throw new LockssRepository.RepositoryStateException ("Couldn't open InputStream: " + e.toString());
	}
      }
    }
  }

  /**
   * Simple comparator which uses File.compareTo() for sorting.
   */
  private static class FileComparator implements Comparator {
    public int compare(Object o1, Object o2) {
      // compares file pathnames
      return ((File)o1).getName().compareTo(((File)o2).getName());
    }
  }

  /**
   * Encodes URL
   * Encodes the URL for storage in the file system.
   *
   * 1. Convert all backslashes to %5C
   * 2. Tokenize string by '/'
   * 3. All strings longer than 254 characters are separated by a / at each 254'th character
   */
  public static String encodeUrl(String url) {
    if(url == null || url.isEmpty())
      return url;
    if(url.charAt(0) == '/' && url.length() > 1) {
      url = url.substring(1);
    }
    // 1. convert all backslashes to %5C
    String backNorm =
      RepositoryManager.isEnableLongComponentsCompatibility() ? "%5c" : "%5C";
    url = url.replaceAll("\\\\", backNorm);
    // 2. tokenize string by '/'
    StringBuffer result = new StringBuffer();
    StringTokenizer strtok = new StringTokenizer(url, "/", true);
    while(strtok.hasMoreTokens()) {
      result.append(encodeLongComponents(strtok.nextToken()));
    }
    return result.toString();
  }
  
  private static String encodeLongComponents(String string) {
    int maxComponentLen = RepositoryManager.getMaxComponentLength();
    if(maxComponentLen < 3) {
      throw new IllegalStateException("maxComponentLength must be >= 3");
    }
    if (string.length() <= maxComponentLen && StringUtil.isAscii(string)) {
      // Short enough & no unicode, no further check necessary.
      return string;
    }
    StringBuilder sb = new StringBuilder(string.length() + 10);
    encodeLongComponents(sb, string, maxComponentLen);
    return sb.toString();
  }
  
  // Break into pieces whose encoded byte length fits within maxComponentLen
  private static void encodeLongComponents(StringBuilder sb,
					   String current,
					   int maxComponentLen) {
    if (current.equals("..")) {
      sb.append("\\..");
    } else if (byteLength(current) <= maxComponentLen) {
      sb.append(current);
    } else {
      for (int charlen = maxComponentLen-1; charlen > 1; charlen--) {
	String candidate = current.substring(0, charlen);
	if (byteLength(candidate) <= maxComponentLen - 1) {
	  sb.append(candidate);
	  sb.append("\\/");
	  encodeLongComponents(sb,
			       current.substring(charlen),
			       maxComponentLen);
	  return;
	}
      }
      throw new IllegalStateException("Single char unicode string too long for filesystem");
    }
  }

  private static final Charset FS_PATH_CHARSET =
    Charset.forName(Constants.ENCODING_UTF_8);

  static int byteLength(String s) {
    return s.getBytes(FS_PATH_CHARSET).length;
  }

  /**
   *
   * Decodes URL
   * Decodes the URL for storage in the file system.
   *
   * 1. Remove all \/
   * 2. Remove all \
   * 
   * Design Decision: Don't convert %5c to URL
   * This allows valid URLs that should have 
   */
  static String decodeUrl(String path) {
    if(path == null || path.isEmpty())
      return path;
    path = path.replaceAll("\\\\/", "");
    // No backslashes should be left except in files created before long
    // componenets enabled - leave them alone.
//     path = path.replaceAll("\\\\", "");
    path = LockssRepositoryImpl.unescape(path);
    return path;
  }
}
