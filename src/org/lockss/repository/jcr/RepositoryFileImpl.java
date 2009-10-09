/**

Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.repository.jcr;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import javax.jcr.*;

import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.repository.*;
import org.lockss.repository.v2.*;
// So that we prefer the v2 RepositoryFile over the v1.
import org.lockss.repository.v2.RepositoryFile;   
import org.lockss.state.NodeState;
import org.lockss.util.*;

/**
 * @author Brent E. Edwards
 *
 * This class holds all information for a single repository file, including all
 * metainformation.
 * 
 */
public class RepositoryFileImpl extends RepositoryNodeImpl implements 
RepositoryFile {
  // Constants, internally used.
  
  // Where the PersistentPeerIdSet is stored.
  private final static String k_propPreferredVersion = "PreferredVersion";
  // The previous version, in our linked list of versions.
  private final static String k_propPreviousVersion = "PreviousVersion";
  private final static String k_propSizePreferred = "SizePreferred";
  private final static String k_propSizeTotal = "SizeTotal"; 
  private final static String k_propVersionEnd = "VersionEnd";
  private final static int k_thresholdDeferredStream = 10240;

  // This class uses permanent version numbers.  These version numbers 
  // are not shared among computers. If two versions of the same node 
  // had the same version number, then we might try to put two versions 
  // into the same directory.  The only time that versions might have the
  // same version number is if:
  
  // - We have generated versions on average more rapidly than 1 
  //   per millisecond for its entire run.
  // - The repository is stopped and restarted, so that the AtomicLong
  //   is restarted.
 
  // As far as I can tell, the first condition is unlikely to happen for
  // the whole time that it runs.  Most versions will be fetched from 
  // the Internet.  Until the 'net becomes much, much faster, they
  // take more than 1 ms to load.
  
  // If you know that a repository will regularly generate more than 1 
  // version per millisecond -- then multiply "TimeBase.nowMs()" by a 
  // finagle factor of 1000 (requiring that your repository won't 
  // generate more than 1 version per microsecond) or 1000000 (1 version
  // per nanosecond).
  
  private static AtomicLong sm_alVersion = new AtomicLong(TimeBase.nowMs());
  
  // Static variables.  These variables must work when multiple 
  // threads are happening.
  
  private static Logger logger = Logger.getLogger("RepositoryFileImpl");

  // Member variables
  protected Node m_nodeVersionEnd;  // The last version in the linked list.
  private RepositoryFileVersion m_rfvPreferred;
  private long m_sizePreferred;  // -1 means no stored size.
  private long m_sizeTotal;      // -1 means no stored size.
  
  /**
   * This constructor creates a new fileContent.
   * IMPORTANT: fileContent is ASSUMED to have a writable directory.
   * This condition -=>must<=- be tested further up the chain.
   * 
   * **** TODO: Get session from <code>JcrRepositoryHelper</code>
   * 
   * @param session        The session for the JCR session 
   * @param node           The current node in the JCR session
   * @param stemFile       The base that all WARC files should have.
   * For example, '/lockss/a/jcr/file' would create WARC files
   * '/lockss/a/jcr/file00001.warc', '/lockss/a/jcr/file00002.warc', etc.
   * @param url            The URL for this file
   * @throws LockssRepositoryException
   * @throws FileNotFoundException
   */

  protected RepositoryFileImpl(Session session, Node node, String stemFile, String url)
      throws LockssRepositoryException, FileNotFoundException {
    super(session, node, stemFile, url);
    
    m_rfvPreferred = null;
    m_nodeVersionEnd = null;
    m_sizePreferred = -1;
    m_sizeTotal = -1;
   
    try {
      m_node.setProperty(k_propIsFile, true);
      
      m_session.save();
      m_session.refresh(true);
    } catch (RepositoryException e) {
      logger.error("constructor(5): " + e.getMessage());
      throw new LockssRepositoryException(e);
    }
    
  }

  /**
   * This constructor assumes that the file already has the fileContent
   * installed.
   *
   * @param session
   * @param node
   * @throws NoUrlException
   * @throws LockssRepositoryException
   */
  protected RepositoryFileImpl(Session session, Node node) 
      throws NoUrlException, LockssRepositoryException {
    super(session, node);

    Node nodePreferredVersion;
    Property propIsFile;
    Property propSizePreferred;
    Property propSizeTotal;
    Property propPreferredVersion;
    Property propVersionEnd;
    
    testIfNull(session, "session");
    testIfNull(node, "node");

    try {
      constructorShared(session, node);
      
      if (m_node.hasProperty(k_propIsFile)) {
        propIsFile = m_node.getProperty(k_propIsFile);
        if (!propIsFile.getBoolean()) {
          logger.error("Attempting to load a repository node " + 
              "into a repository file.");
          throw new LockssRepositoryException("Attempting to load a " + 
              "repository node into a repository file.");
        }
      }
      
      if (m_node.hasProperty(k_propPreferredVersion)) {
        propPreferredVersion = m_node.getProperty(k_propPreferredVersion);
        nodePreferredVersion = propPreferredVersion.getNode();
        m_rfvPreferred = new RepositoryFileVersionImpl(m_session, 
                nodePreferredVersion, this);
      } else {
        // It is not a bug if setPreferredVersion was never called.
        logger.info("RepositoryFileImpl: No preferred version was found for this node.");
        m_rfvPreferred = null;
      }
      
      if (m_node.hasProperty(k_propVersionEnd)) {
        propVersionEnd = m_node.getProperty(k_propVersionEnd);
        m_nodeVersionEnd = propVersionEnd.getNode();
      } else {
        // It's not a bug if there is no end version.
        logger.info("RepositoryFileImpl: No end version was found.");
        m_nodeVersionEnd = null;
      }
      
      if (m_node.hasProperty(k_propSizePreferred)) {
        propSizePreferred = m_node.getProperty(k_propSizePreferred);
        m_sizePreferred = propSizePreferred.getLong();
      } else {
        logger.info("RepositoryFileImpl: No preferred size found.");
        m_sizePreferred = -1;
      }
      
      if (m_node.hasProperty(k_propSizeTotal)) {
        propSizeTotal = m_node.getProperty(k_propSizeTotal);
        m_sizePreferred = propSizeTotal.getLong();
      } else {
        logger.info("RepositoryFileImpl: No total size found.");
        m_sizeTotal = -1;
      }
    } catch (RepositoryException e) {
      logger.error("Repository Exception in constructor(2): " + 
          e.getMessage());
      throw new LockssRepositoryException(e);
    }
  }

  // ----------------- Public methods -----------------
  
  public void cleanDatabase() throws LockssRepositoryException {
    // Anything else to added?
    clearTempContent();
  }

  /* The name of new nodes comes from an AtomicInteger.
   */
  public RepositoryFileVersion createNewVersion() 
      throws LockssRepositoryException, FileNotFoundException
  {
    long lVersion;
    Node nodeNew;
    String strVersion;
    RepositoryFileVersion rfvVersion;
    
    // Create the new node.
    try {
      lVersion = sm_alVersion.incrementAndGet();
      strVersion = Long.toString(lVersion);
      nodeNew = createNode(strVersion);

      nodeNew.addMixin("mix:referenceable");
      nodeNew.setProperty(k_propPreviousVersion, m_nodeVersionEnd);
      m_nodeVersionEnd = nodeNew;
      m_node.setProperty(k_propVersionEnd, m_nodeVersionEnd);
    } catch (RepositoryException e) {
      throw new LockssRepositoryException(e);
    }
    
    // Create the new version.
    rfvVersion = new RepositoryFileVersionImpl(m_session, nodeNew, 
        m_stemFile, m_url, this, k_thresholdDeferredStream);
    
    m_sizePreferred = -1;
    m_sizeTotal = -1;
    
    invalidateTreeSize();
    
    return rfvVersion;
  }


  /**
   * Create a new version under the current RepositoryFile, before another version.
   * 
   * @return RepositoryFileVersion.
   * @throws LockssRepositoryException
   */
  public RepositoryFileVersion createNewVersionBefore(RepositoryFileVersion 
      rfvCurrent) 
      throws LockssRepositoryException, FileNotFoundException {
    long lVersion;
    Node nodeCurrent = null;
    Node nodeNew;
    Node nodePrevious = null;
    Property propPrevious;
    String strVersion;
    RepositoryFileVersion rfvVersion;
    RepositoryFileVersionImpl rfviCurrent;
    
    if (!(rfvCurrent instanceof RepositoryFileVersionImpl)) {
      logger.error("createNewVersionBefore: Wrong data type for rfvBefore.");
      throw new LockssRepositoryException("Wrong data type for rfvBefore.");
    }
          
    // Create the new node.
    try {
      // Set nodeCurrent and nodePrevious.
      // rfvCurrent's node will go between these two nodes.
        
      rfviCurrent = (RepositoryFileVersionImpl) rfvCurrent;
      nodeCurrent = rfviCurrent.m_node;
      if (nodeCurrent.hasProperty(k_propPreviousVersion)) {
        propPrevious = nodeCurrent.getProperty(k_propPreviousVersion);
        nodePrevious = propPrevious.getNode();
      }

      lVersion = sm_alVersion.incrementAndGet();
      strVersion = Long.toString(lVersion);
      nodeNew = createNode(strVersion);
      nodeNew.addMixin("mix:referenceable");

      nodeCurrent.setProperty(k_propPreviousVersion, nodeNew);
      nodeNew.setProperty(k_propPreviousVersion, nodePrevious);
    } catch (RepositoryException e) {
      throw new LockssRepositoryException(e);
    }
    
    // Create the new version.
    rfvVersion = new RepositoryFileVersionImpl(m_session, nodeNew, 
        m_stemFile, m_url, this, k_thresholdDeferredStream);
    
    m_sizePreferred = -1;
    m_sizeTotal = -1;
    
    invalidateTreeSize();
    
    return rfvVersion;
  }

  /**
   * This method marks the preferred version of a node as deleted. 
   * (It does not actually delete the version.)
   * 
   * See 'isDeleted' for important information about how deletion
   * is handled by this repository!
   * 
   * NOTE: delete() is NOT in the interface 'RepositoryFile'.
   */
  public void delete() throws LockssRepositoryException {
    RepositoryFileVersion rfvPreferred;
    
    rfvPreferred = getPreferredVersion();
    
    if (rfvPreferred == null) {
      logger.error("The RepositoryFileVersion was NOT set before " + 
          "'isDeleted' was called.  To fix this error, please call " +
          "'setPreferredVersion' on your RepositoryFileImpl.");
      // NullPointerException is not checked, so does not need to be named.
      throw new NullPointerException("No preferred version available.");
    }
    
    rfvPreferred.delete();
  }

  /** 
   * Used by hashes.
   * 
   * @see org.lockss.repository.jcr.RepositoryNodeImpl#equals(Object)
   * @param obj What we're comparing against
   * @return Whether <code>this</code> is equal to <code>obj</code>
   */
  
  public boolean equals(Object obj) {
    RepositoryFileImpl rfiObj;
    
    if (obj instanceof RepositoryFileImpl) {
      rfiObj = (RepositoryFileImpl) obj;
      
      try {
        return rfiObj.m_node.isSame(m_node);
      } catch (RepositoryException e) {
        logger.error("equals: ", e);
        logger.error("Tossing the exception into the bit bucket; returning false.");
        return false;
      }
    }
    
    // obj is not RepositoryFileImpl.
    return false;
  }

  
  public PersistentPeerIdSet getAgreeingPeerIdSet() 
      throws LockssRepositoryException {
    IdentityManager idman;
    JcrRepositoryHelperFactory jrhf;
    PersistentPeerIdSet ppis;
    StreamerJcr strjcr;
    
    try {
      if (m_node.hasProperty(k_propAgreeingPeerId)) {
        jrhf = JcrRepositoryHelperFactory.getSingleton();
        if (jrhf == null) {
          throw new LockssRepositoryException("You must call JcrRepositoryHelperFactory.preconstructor before you call this method.");
        }
        idman = jrhf.getIdentityManager();
        
        strjcr = new StreamerJcr(k_propAgreeingPeerId, m_node);
        
        ppis = new PersistentPeerIdSetImpl(strjcr, idman);
      } else {
        logger.debug3("getAgreeingPeerIDSet: Internal "
                + "error: Agreeing Peer ID set did not exist.  Returning null.");
        ppis = null;
      }
    } catch (RepositoryException e) {
      throw new LockssRepositoryException(e);
    }
    
    return ppis;
  }

    
  /**
   * A file is a leaf.  It has no nodes as children.
   * (File versions are NOT nodes, so they are not children.)
   * 
   * @return 0  Always.
   */
  
  public int getChildCount()
      throws LockssRepositoryException {
    return 0;
  }
  
  /**
   * @return The length of your preferred content.
   * @throws LockssRepositoryException
   */
  public long getContentSize(/* preferredOnly = true */) 
      throws LockssRepositoryException {
    return getContentSize(true);
  }

  /**
   * Returns the size of the current version of stored cache.
   * Note that deleted objects are ALWAYS included in this version.
   * 
   * @param preferredOnly
   *                If true, then only consider the most recent version. If
   *                false, then compute the size for ALL versions.
   * @return How long IS your content?  That long?  Impressive.
   */
  public long getContentSize(boolean preferredOnly) 
      throws LockssRepositoryException {
    Node nodeIter;
    Property propIter;
    RepositoryFileVersion rfvIter;
    RepositoryFileVersion rfvPreferred;

    if (preferredOnly) {  
      rfvPreferred = getPreferredVersion();
      
      if (rfvPreferred != null) {
        if (m_sizePreferred == -1) {
          m_sizePreferred = rfvPreferred.getContentSize();
        }
        
        return m_sizePreferred;
      } 
      // rfvPreferred is null.
      logger.error("No preferred version.");
      throw new NullPointerException("No preferred version.");
    } 
    
    // !preferredOnly -- give the sum of all versions.
    if (m_sizeTotal == -1) {
      try {
        m_sizeTotal = 0;
        nodeIter = m_nodeVersionEnd;
        while (nodeIter != null) {
          rfvIter = new RepositoryFileVersionImpl(m_session, nodeIter, this);
          m_sizeTotal += rfvIter.getContentSize();
          
          if (nodeIter.hasProperty(k_propPreviousVersion)) {
            propIter = nodeIter.getProperty(k_propPreviousVersion);
            nodeIter = propIter.getNode();
          } else {
            break;
          }
        }
      } catch (RepositoryException e) {
        logger.error("getContentSize: " + e.getMessage());
        throw new LockssRepositoryException(e);
      }
    }
    
    return m_sizeTotal;
    
  }

  
  /**
   * Asembles a list of immediate children, possibly filtered. Sorted
   * alphabetically by File.compareTo().
   * 
   * @param filter            a spec to filter on. Null for no filtering.
   * @param includeDeleted    true iff deleted nodes should be included.
   * @return the list of child RepositoryNodes.
   *
   * If the filter is null, then return the repository file.
   * 
   * (This method will be used by the RepositoryNode and
   * RepositoryNodeImpl classes.  This method is the base case for
   * a recursive definition.)
   */
  public List<org.lockss.repository.v2.RepositoryFile> getFileList(CachedUrlSetSpec filter,
      boolean includeDeleted) throws LockssRepositoryException {
    List<RepositoryFile> lirf;
    
    lirf = new ArrayList<RepositoryFile>();
    
    if (includeDeleted || !isDeleted()) {
      if (filter == null || filter.matches(m_url)) {
        lirf.add(this);
      }
    } 
    
    return lirf;
  }
  
  /**
   * Very similar to 'getFileList', except that this returns an array.
   * 
   * @see org.lockss.repository.v2.RepositoryNode#getFiles(int, boolean)
   * @param maxVersions -- the number of versions to return.
   * @param includeDeleted
   * @return an array of <code>RepositoryFile</code>
   * @throws LockssRepositoryException
   */
  
  public RepositoryFile[] getFiles(int maxVersions, boolean includeDeleted) 
      throws LockssRepositoryException {    
    RepositoryFile[] arrf;
    
    if ((includeDeleted || !isDeleted()) && 0 < maxVersions) {
      arrf = new RepositoryFile[1];
      arrf[0] = this;
    } else {
      arrf = new RepositoryFile[0];
    }
    
    return arrf;
  }


  /**
   * This method has been intentionally stubbed out.
   * @return An <code>UnsupportedOperationException</code>.  Always.
   */
  public NodeState getPollHistories() throws LockssRepositoryException {
    throw new UnsupportedOperationException("getPollHistories is no longer supported.");
  }
  
  /**
   * The preferred version is a complex beast.
   * If 'setPreferredVersion' was called, then m_rfvPreferred is set. 
   *     That version, no matter what it was, should be returned.
   * If 'setPreferredVersion' was not called, then m_rfvPreferred
   *     is null.  This method should return (if it exists) the 
   *     RepositoryFileVersion that's closest to the end and that's
   *     undeleted.
   *     
   * In my opinion, this method is too complex in concept, and I strongly
   * recommend that you always call 'setPreferredVersion' first.  
   * 
   * @see RepositoryFile#getPreferredVersion()
   * @return A RepositoryFileVersion according to the above formula.
   * @throws LockssRepositoryException
   */
  public RepositoryFileVersion getPreferredVersion() 
      throws LockssRepositoryException {
    Node nodeIter;
    Property propIter;
    RepositoryFileVersion rfvIter;
    
    // If you called setPreferredVersion, then return it. 
    if (m_rfvPreferred != null) {
      return m_rfvPreferred;
    } 
    
    // setPreferredVersion was not called.
    try {
      nodeIter = m_nodeVersionEnd;
      while (nodeIter != null) {
        rfvIter = new RepositoryFileVersionImpl(m_session, nodeIter, this);
        
        if (!rfvIter.isDeleted()) {
          return rfvIter;
        }
        
        if (nodeIter.hasProperty(k_propPreviousVersion)) {
          propIter = nodeIter.getProperty(k_propPreviousVersion);
          nodeIter = propIter.getNode();
        } else {
          break;
        }
      }
    } catch (RepositoryException e) {
      logger.error("getContentSize: " + e.getMessage());
      throw new LockssRepositoryException(e);
    }
        
    // No undeleted node was found.
    return null;
  }
  

  public Properties getProperties() 
      throws IOException, LockssRepositoryException {
    InputStream istrReturn = null;
    Property propReturn;
    Properties propsReturn = new Properties();
    
    try {
      if (m_node.hasProperty(k_propProperties)) {
        propReturn = m_node.getProperty(k_propProperties);
        istrReturn = propReturn.getStream();
        propsReturn.load(istrReturn);
      } else { // Doesn't have property k_propProperties
        logger.debug3("getProperties: the requested " +
                        "properties do not exist.  Returning null.");
        propsReturn = null;
      }
    } catch (RepositoryException e) {
      throw new LockssRepositoryException(e);
    } finally {
      IOUtil.safeClose(istrReturn);      
    }
    
    return propsReturn;
  }
  
  
  /**
   * Returns the size of the content tree under (and including) this cache in
   * bytes.
   * 
   * @param filter          Whether to filter the URLs being searched.
   * @param calcIfUnknown   If true, then calculate what's not known.
   *                This parameter does nothing, for this version
   *                of the software.
   * @param preferredOnly   If true, then only consider the preferred version. If
   *                false, then compute for ALL versions.
   */
  public long getTreeContentSize(CachedUrlSetSpec filter,
      boolean calcIfUnknown, boolean preferredOnly ) 
      throws LockssRepositoryException {
    Node nodeIter;
    Property propIter;
    RepositoryFileVersion rfvIter;
    RepositoryFileVersion rfvPreferred;

    if (filter == null || filter.matches(m_url)) {
      if (preferredOnly) {
        rfvPreferred = getPreferredVersion();
        
        if (rfvPreferred != null) {
          if (m_sizePreferred == -1) {
            m_sizePreferred = rfvPreferred.getContentSize();
          }
          
          return m_sizePreferred;
        } 
        
        // !preferredOnly
        return 0;
      } 
      
      // Not preferredOnly.
      if (m_sizeTotal == -1) {
        try {
          m_sizeTotal = 0;
          
          nodeIter = m_nodeVersionEnd;
          while (nodeIter != null) {
            rfvIter = new RepositoryFileVersionImpl(m_session, nodeIter, this);
            
            m_sizeTotal += rfvIter.getContentSize();
            
            if (nodeIter.hasProperty(k_propPreviousVersion)) {
              propIter = nodeIter.getProperty(k_propPreviousVersion);
              nodeIter = propIter.getNode();
            } else {
              break;
            }
          }
          
        } catch (RepositoryException e) {
          logger.error("getContentSize: " + e.getMessage());
          throw new LockssRepositoryException(e);
        }
      }
      
      return m_sizeTotal;
    } 
    
    // The filter doesn't match our URL. 
    return 0;
  }


  /**
   * Since we have equals(), we must have hashCode().
   * 
   * @see org.lockss.repository.jcr.RepositoryNodeImpl#hashCode()
   * @return An arbitrary integer, related to the m_node.
   */
  public int hashCode() {
    return m_node.hashCode();
  }
  
  /**
   * Does the preferred file have stored content? This method refers to the
   * preferred version of the Repository File; it is a convenience method.
   */
  public boolean hasContent() 
      throws LockssRepositoryException {
    RepositoryFileVersion rfvPreferred;
    
    rfvPreferred = getPreferredVersion();
    
    if (rfvPreferred != null) {
      return getPreferredVersion().hasContent();
    }
    
    // rfvPreferred == null.
    return false;
  }
  

  /**
   * A file is considered deleted iff its preferred version is deleted.
   * This has several significant effects:
   * 
   * 1. If no preferred version is set by the user, then this program
   *    will choose the preferred version according to its default
   *    preferred version algorithm.  
   * 2. Changing the preferred version changes whether a file is 
   *    deleted.
   *    
   * As far as I understand, both of these effects are DESIRABLE; we
   * do not want files to have deletions independent of their preferred
   * versions.  If you do not want this behavior (ie: if you want files
   * to have deletion independent of their preferred versions), then 
   * please change this function.
   * 
   * This method returns 'false' if no preferred version is set.  I chose
   * this to match the requested behavior in RepositoryFileVersionImpl.
   * isDeleted().  Feel free to change this behavior.
   * 
   * NOTE: isDeleted() is NOT in the interface 'RepositoryFile'.
   * 
   */
  public boolean isDeleted() 
      throws LockssRepositoryException {
    RepositoryFileVersion rfvPreferred;
    
    rfvPreferred = getPreferredVersion();
    
    if (rfvPreferred == null) {
      return false;
    }
    
    return rfvPreferred.isDeleted();
  }

  
  /**
   * What versions does this code know about? 
   */
  public List<RepositoryFileVersion> listVersions() 
      throws LockssRepositoryException {
    return listVersions(Integer.MAX_VALUE);
  }
  
  /**
   * Which versions does this code know about? 
   */
  public List<RepositoryFileVersion> listVersions(int numVersions) 
      throws LockssRepositoryException {
    ArrayList<RepositoryFileVersion> alrfvVersions;
    int i;
    Node nodeIter;
    Property propIter;
    RepositoryFileVersion rfvIter;

    alrfvVersions = new ArrayList<RepositoryFileVersion>();
    
    try {
      for (i = 0, nodeIter = m_nodeVersionEnd; 
           i < numVersions && nodeIter != null; 
           i++) {
        rfvIter = new RepositoryFileVersionImpl(m_session, nodeIter, this);
        alrfvVersions.add(rfvIter);
        
        if (nodeIter.hasProperty(k_propPreviousVersion)) {
          propIter = nodeIter.getProperty(k_propPreviousVersion);
          nodeIter = propIter.getNode();
        } else {
          break;
        }
      }
    } catch (RepositoryException e) {
      logger.error("listVersions: " + e.getMessage());
      throw new LockssRepositoryException(e);
    }
    
    return alrfvVersions;
  }

  public NodeState loadNodeState(CachedUrlSet cus) 
      throws LockssRepositoryException {
    logger.error("loadNodeState has no associated node states in files.");
    throw new LockssRepositoryException("loadNodeState has no associated" + 
        "node states in files.");
  }

  /**
   * For pure nodes, this method creates a new version, and returns the 
   * associated RepositoryFile.
   * 
   * However, that makes no sense for repository files themselves.
   */
  public RepositoryFile makeNewRepositoryFile(String name) 
      throws LockssRepositoryException {
    throw new LockssRepositoryException("A repository file may not create " +
        "more repository files directly.");
  }


  /**
   * Move the .warc files to a specific directory.
   */
  public void move(String stemNewLocation) throws LockssRepositoryException {
    Node nodeIter;
    Property propIter;
    RepositoryFileVersion rfvIter;

    try {
      m_node.setProperty(k_propStemFile, stemNewLocation);
      
      m_session.save();
      m_session.refresh(true);
    } catch (RepositoryException e) {
      logger.error("move: ", e);
      throw new LockssRepositoryException(e);
    }
    
    try {
      nodeIter = m_nodeVersionEnd;
      
      while (nodeIter != null) {
        rfvIter = new RepositoryFileVersionImpl(m_session, nodeIter, this);
        rfvIter.move(stemNewLocation);
        
        if (nodeIter.hasProperty(k_propPreviousVersion)) {
          propIter = nodeIter.getProperty(k_propPreviousVersion);
          nodeIter = propIter.getNode();
        } else {
          break;
        }
      }
    } catch (RepositoryException e) {
      logger.error("move: ", e);
      throw new LockssRepositoryException(e);
    }
  }

  
  // The tests specify that a null agreeing peer id set is fine.
  public void setAgreeingPeerIdSet(PersistentPeerIdSet sepi) 
      throws LockssRepositoryException {
    PersistentPeerIdSetImpl ppisiSepi;
    StreamerJcr strjcr;
    
    if (sepi == null) {
      try {
        m_node.setProperty(k_propAgreeingPeerId, (String) null);
      } catch (RepositoryException e) {
        logger.error("setAgreeingPeerIdSet: " + e.getMessage());
        throw new LockssRepositoryException(e);        
      }
      
      return;
    }
    
    if (sepi instanceof PersistentPeerIdSetImpl) {
      ppisiSepi = (PersistentPeerIdSetImpl) sepi;
    } else {
      logger.error("setAgreeingPeerIdSet: wrong implementation of PersistentPeerIdSet.");
      throw new LockssRepositoryException("setAgreeingPeerIdSet: wrong implementation of PersistentPeerIdSet.");
    }
    
    try {
      strjcr = new StreamerJcr(k_propAgreeingPeerId, m_node);
      ppisiSepi.setStreamer(strjcr);
      ppisiSepi.store();
      
      m_session.save();
      m_session.refresh(true);
    } catch (RepositoryException e) {
      logger.error("setAgreeingPeerIdSet: " + e.getMessage());
      throw new LockssRepositoryException(e);
    } catch (IOException e) {
      logger.error("setAgreeingPeerIdSet: " + e.getMessage());
      throw new LockssRepositoryException(e);
    }
  }


  /**
   * This method has been intentionally stubbed out.
   */
  public void setPollHistories(NodeState nsPollHistories) 
      throws LockssRepositoryException {
    // As requested, this method has been stubbed out.
  }
  
  /**
   *  It is not an error for this method to set a null RepositoryFileVersion.
   * 
   *  IMPORTANT NOTE: This method uses RepositoryFileVersionImpl, not a generic
   *  RepositoryFileVersion, because it uses 'getNode'.  
   * 
   * @see org.lockss.repository.v2.RepositoryFile#setPreferredVersion(
   * org.lockss.repository.v2.RepositoryFileVersion)
   * @param rfv
   * @throws LockssRepositoryException
   */
  public void setPreferredVersion(RepositoryFileVersion rfv) 
      throws LockssRepositoryException {
    Node nodeVersion;
    RepositoryFileVersionImpl rfvi;
    
    if (rfv != null) {
      m_rfvPreferred = rfv;
      
      if (rfv instanceof RepositoryFileVersionImpl) {
        rfvi = (RepositoryFileVersionImpl) rfv;
        
        // Verify that the version was already checked in.
        if (!rfvi.m_isLocked) {
          throw new LockssRepositoryException("The RepositoryFileVersion " + 
              "was not committed before it was set as preferred version.");
        }
        
        // Verify that the version belongs to this node.
        if (!rfvi.m_rfiParent.equals(this)) {
          throw new LockssRepositoryException("The parent of the preferred" +
              "version must be this repository file.");
        }

        // Store the version in the repository.
        try {
          nodeVersion = rfvi.getNode();
          m_node.setProperty(k_propPreferredVersion, nodeVersion);
          
          m_session.save();
          m_session.refresh(true);
        } catch (RepositoryException e) {
          throw new LockssRepositoryException(e);
        }
      } else {
        logger.error("setPreferredVersion: the RepositoryFileVersion must be" + 
            "of type RepositoryFileVersionImpl.");
        throw new LockssRepositoryException("RepositoryFileVersion was not of " +
            "right implementation.");
      }      
    } else {  // rfv == null, which is A-OK!
      try {
        m_node.setProperty(k_propPreferredVersion, (Node) null);
        
        m_session.save();
        m_session.refresh(true);
      } catch (RepositoryException e) {
        logger.error("setPreferredVersion: Tried to set a null 'next version': " + 
            e.getMessage());
        throw new LockssRepositoryException(e);
      }
    }
    
    m_sizePreferred = -1;
  }
  
  public void storeNodeState(NodeState nodeState) 
      throws LockssRepositoryException {
    logger.error("storeNodeState does not work with repository files.");
    throw new LockssRepositoryException("storeNodeState does not work with" +
        "repository files.");
  }
  
  /**
   * If the preferred version is marked as deleted, this method removes the mark.
   * 
   * See 'isDeleted' for information about the choice of how to implement
   * deletions.
   */
  public void undelete() 
      throws LockssRepositoryException {
    RepositoryFileVersion rfvPreferred;
    
    rfvPreferred = getPreferredVersion();
    
    if (rfvPreferred == null) {
      logger.error("The RepositoryFileVersion was NOT set before " + 
          "'isDeleted' was called.  To fix this error, please call " +
          "'setPreferredVersion' on your RepositoryFileImpl.");
      // NullPointerException is not checked, so does not need to be named.
      throw new NullPointerException("No preferred version available.");
    }
    
    rfvPreferred.undelete();
  }

  
  // Helper functions...
  
  /**
   * This method is called by 'commit()'.
   */
  public void clearTempContent() {
    // This method does nothing.
  }

}
