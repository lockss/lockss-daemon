/**
 
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

package org.lockss.repository.jcr;

import java.io.*;
import java.util.*;

import javax.jcr.*;

import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.repository.*;
import org.lockss.repository.v2.RepositoryFile;
import org.lockss.repository.v2.RepositoryNode;
import org.lockss.state.*;
import org.lockss.util.*;

/**
 * @author edwardsb
 *
 * One can write collections (like this one) in two extreme ways:
 * 1. Everything is held in memory, and the program retrieves data
 *    quickly from in-memory data structures.  This method is fast but 
 *    uses much memory.
 * 2. As much as possible is held on disk, and the program retrieves
 *    everything from disk.  This method is slow but it uses little 
 *    memory.
 *    
 * Every programmer gets this balance wrong.  Without exception.  
 * 
 * This implementation strives to be closer to the second way (holding 
 * everything on disk) than to the first way.  This balance will change
 * with feedback on the tradeoff between memory and speed.  
 */

public class RepositoryNodeImpl extends JcrRepositoryBase implements 
RepositoryNode  {
  
  // Internal constants
  protected final static String k_propAgreeingPeerId = "AgreeingPeerId";
  protected final static String k_propIsFile = "IsFile";
  protected final static String k_propNodeState = "NodeState";
  protected final static String k_propPollHistoryList = "PollHistoryList";
  protected final static String k_propProperties = "Properties";

  // Static variables.  These variables must work when multiple 
  // threads are happening.
  
  private static Logger logger = Logger.getLogger("RepositoryNodeImpl");
  
  /**
   * This method generates a new repository node.
   * 
   * @param session
   * @param node
   * @param stemFile
   * @param sizeMax
   * @param url
   * @throws LockssRepositoryException
   */
  protected RepositoryNodeImpl(Session session, Node node, String stemFile,
      long sizeWarcMax, String url, IdentityManager idman) 
      throws LockssRepositoryException {
    super(session, node, stemFile, sizeWarcMax, url, idman);
    
    try {
      m_node.setProperty(k_propIsFile, false);
      
      m_session.save();
      m_session.refresh(true);
    } catch (RepositoryException e) {
      throw new LockssRepositoryException(e);
    }
    
    invalidateTreeSize();
  }
  
  /**
   * Important: the node passed in must have had JcrRepositoryBase.k_propStemFile,
   * JcrRepositoryBase.k_propSizeMax, and JcrRepositoryBase.k_propUrl set. 
   * 
   * @param session
   * @param node
   * @param idman
   */
  protected RepositoryNodeImpl(Session session, Node node, IdentityManager idman) 
      throws LockssRepositoryException {
    super(session, node, idman);
    
    invalidateTreeSize();
  }

  
  // Call this method instead of the other constructor for nodes that
  // already exist in the database.
  
  // It reads whether the item being constructed is a node or a file.
  // If it's a new node, then this method creates a new RepositoryNode.
  
  // TODO: Verify that all fields are available after the constructor is done.
  public static RepositoryNode constructor(Session session, Node node, 
      IdentityManager idman)
      throws LockssRepositoryException {
    Property propIsFile;
    RepositoryNode nodeReturn = null;
    
    try {
      if (node.hasProperty(k_propIsFile)) {
        propIsFile = node.getProperty(k_propIsFile);
        
        if (propIsFile.getBoolean()) {
          nodeReturn = new RepositoryFileImpl(session, node, idman);
        } else {
          nodeReturn = new RepositoryNodeImpl(session, node, idman);
        }
      } else {
        logger.debug3("constructor: k_propIsFile was null.  Constructing a new node.");
        nodeReturn = new RepositoryNodeImpl(session, node, idman);
      }
    } catch (RepositoryException e) {
      logger.error("constructor: " + e.getMessage());
      throw new LockssRepositoryException(e);
    }
    
    return nodeReturn;
  }
  
  public static RepositoryNode constructor(Session session, Node node,
      String stemFile, long sizeWarcMax, String url, 
      IdentityManager idman)
      throws LockssRepositoryException, FileNotFoundException {
    Property propIsFile;
    RepositoryNode nodeReturn = null;
    
    try {
      if (node.hasProperty(k_propIsFile)) {
        propIsFile = node.getProperty(k_propIsFile);
        
        if (propIsFile.getBoolean()) {
          nodeReturn = new RepositoryFileImpl(session, node, stemFile, sizeWarcMax, url, idman);
        } else {
          nodeReturn = new RepositoryNodeImpl(session, node, stemFile, sizeWarcMax, url, idman);
        }
      } else {
        logger.debug3("constructor: k_propIsFile was null.  Constructing a new node.");
        nodeReturn = new RepositoryNodeImpl(session, node, stemFile, sizeWarcMax, url, idman);
      }
    } catch (RepositoryException e) {
      logger.error("constructor: " + e.getMessage());
      throw new LockssRepositoryException(e);
    }
    
    return nodeReturn;
  }

  
  
  /** Used by hashes.
   *  The only thing that matters is the m_node.
   *  
   * @see java.lang.Object#equals(java.lang.Object)
   * @param obj
   * @return
   */
  public boolean equals(Object obj) {
    RepositoryNodeImpl rniObj;
    
    if (obj instanceof RepositoryNodeImpl) {
      rniObj = (RepositoryNodeImpl) obj;
      
      try {
        return rniObj.m_node.isSame(m_node);
      } catch (RepositoryException e) {
        logger.error("equals: ", e);
        logger.error("Throwing exception into the bit bucket; returning false.");
        return false;
      }
    } else {  // obj is not a repository node impl.
      return false;
    }
  }
  
  
  /**
   * Returns the number of children at this node.  
   * 
   * This method (intentionally) does not distinguish between deleted
   * files and undeleted files.  (Files are a type of node.  If the
   * preferred version of a file is deleted, then the file is considered
   * deleted.)
   */
  public int getChildCount() 
      throws LockssRepositoryException {
    int countChildren = 0;
    NodeIterator ni;
    
    try {
      // For each node under m_node...
      ni = m_node.getNodes();
      while (ni.hasNext()) {
        countChildren += 1;        
        ni.nextNode();
      }
    } catch (RepositoryException e) {
      logger.error("Repository Exception: " + e.getMessage());
      throw new LockssRepositoryException(e);
    }   
    
    return countChildren;
  }
  
  
  /**
   * Returns the file under a repository node.
   * This method is not in the interface. 
   *
   * TODO: Add a parameter to getFile, specifying whether it should create the
   * file.  
   * 
   * @param nameSubtree
   * @return
   * @throws LockssRepositoryException
   */
  protected RepositoryNode getFile(String nameSubtree, String strUrl, boolean fCreate)
      throws LockssRepositoryException {
    Node node = null; 
    
    try {
      try {        
        node = m_node.getNode(nameSubtree);
      } catch (PathNotFoundException e) {
        if (fCreate) {
          node = m_node.addNode(nameSubtree);
        } else {
          logger.error("getFile: '" + nameSubtree + "' does not exist, and I was not asked to create it.", e);
          throw e;
        }
      }
    } catch (RepositoryException e) {
      try {
        if (fCreate) {
          node = m_node.addNode(nameSubtree);
        } else {
          logger.error("getNode: '"+ nameSubtree + "' does not exist, and I was not asked to create it.", e);
          throw new LockssRepositoryException(e);
        }
      } catch (RepositoryException e2) {
        logger.error("getNode: '" + nameSubtree + "' doesn't exist, and I got an error on trying to create it.", e);
        throw new LockssRepositoryException(e2);
      }
    } 
    
    try {
      if (node.hasProperty(k_propIsFile)) {
        return new RepositoryFileImpl(m_session, node, m_idman);
      } else {
        return new RepositoryFileImpl(m_session, node, m_stemFile, 
            m_sizeWarcMax, strUrl,  m_idman);
      }
    } catch (FileNotFoundException e) {
      logger.error("getFile: ", e);
      throw new LockssRepositoryException(e);
    } catch (RepositoryException e) {
      logger.error("getFile: ", e);
      throw new LockssRepositoryException(e);
    }
  }


  public List<org.lockss.repository.v2.RepositoryFile> getFileList(CachedUrlSetSpec filter) 
      throws LockssRepositoryException {
    return getFileList(filter, false);
  }

  /**
   * Asembles a list of immediate children, possibly filtered. Sorted
   * alphabetically by File.compareTo().
   * 
   * @param filter:
   *                a spec to filter on. Null for no filtering.
   * @param includeInactive:
   *                true iff inactive nodes should be included.
   * @return the list of child RepositoryNodes.
   *
   * If the filter is null, then return the repository file.
   * 
   * (This method will be used by the RepositoryNode and
   * RepositoryNodeImpl classes.  This method is the base case for
   * a recursive definition.)
   */
  public List<org.lockss.repository.v2.RepositoryFile> getFileList(CachedUrlSetSpec filter,
      boolean includeDeleted) 
      throws LockssRepositoryException {
    List<RepositoryFile> lirf;
    Node nodeIter;
    NodeIterator ni;
    RepositoryNode rnIter;
    
    lirf = new ArrayList<RepositoryFile>();
    
    try {
      // For each node under m_node...
      ni = m_node.getNodes();
      while (ni.hasNext()) {
        nodeIter = ni.nextNode();
        rnIter = constructor(m_session, nodeIter, m_idman);
        
        lirf.addAll(rnIter.getFileList(filter, includeDeleted));
      }
    } catch (RepositoryException e) {
      logger.error("getFileList: " + e.getMessage());
      throw new LockssRepositoryException(e);
    }
    
    return lirf;
  }

  public RepositoryFile[] getFiles() 
      throws LockssRepositoryException {
    return getFiles(Integer.MAX_VALUE, false);
  }

  public RepositoryFile[] getFiles(boolean includeDeleted) 
      throws LockssRepositoryException {
    return getFiles(Integer.MAX_VALUE, includeDeleted);
  }

  public RepositoryFile[] getFiles(int maxVersions) 
      throws LockssRepositoryException {
    return getFiles(maxVersions, false);
  }

  /**
   * Very similar to getFileList, except for the parameters.  This method is
   * useful when you want a limit to the number of versions to return.
   *  
   * @see org.lockss.repository.v2.RepositoryNode#getFiles(int, boolean)
   * @param maxVersion
   * @param includeDeleted
   * @return
   * @throws LockssRepositoryException
   */
  public RepositoryFile[] getFiles(int maxVersion, boolean includeDeleted) 
      throws LockssRepositoryException {
    RepositoryFile[] arrf = null;
    List<RepositoryFile> lirf;
    Node nodeIter;
    NodeIterator ni;
    RepositoryNode rnIter;
    
    lirf = new ArrayList<RepositoryFile>();
    
    try {
      // For each node under m_node...
      ni = m_node.getNodes();
      while (ni.hasNext()) {
        nodeIter = ni.nextNode();
        rnIter = constructor(m_session, nodeIter, m_idman);
        
        arrf = rnIter.getFiles(maxVersion, includeDeleted);
        
        for (RepositoryFile rf : arrf) {
          lirf.add(rf);
        }
      }
    } catch (RepositoryException e) {
      logger.error("getFiles: " + e.getMessage());
      throw new LockssRepositoryException(e);
    }
    
    return lirf.toArray(arrf);
  }
  
  /**
   * Returns a direct descendant of a repository node.
   * This method is not in the interface. 
   * 
   * @param nameSubtree
   * @return
   * @throws LockssRepositoryException
   */
  protected RepositoryNode getNode(String nameSubtree, String urlNew, boolean fCreate)
      throws LockssRepositoryException {
    Node node = null; 
    
    try {
      try {
        node = m_node.getNode(nameSubtree);
      } catch (PathNotFoundException e) {
        if (fCreate) {
          node = m_node.addNode(nameSubtree);
        } else {
          logger.error("getNode: '" + nameSubtree + "' does not exist, and I was not asked to create it.", e);
          throw e;
        }
      }      
    } catch (RepositoryException e) {
      try {
        if (fCreate) {
          node = m_node.addNode(nameSubtree);
        } else {
          logger.error("getNode: '"+ nameSubtree + "' does not exist, and I was not asked to create it.", e);
          throw new LockssRepositoryException(e);
        }
      } catch (RepositoryException e2) {
        logger.error("getNode: '" + nameSubtree + "' doesn't exist, and I got an error on trying to create it.", e);
        throw new LockssRepositoryException(e2);
      }
    } 
    
    try {
      return constructor(m_session, node, m_stemFile, m_sizeWarcMax, urlNew, m_idman);
    } catch (FileNotFoundException e) {
      logger.error("getNode: " + e.getMessage());
      throw new LockssRepositoryException(e);      
    }
  }

  
  public String getNodeUrl() {
    return m_url;
  }
  
  
  /**
   * Used by LockssOneAuRepository.loadPollHistories.
   * 
   * @see org.lockss.repository.v2.RepositoryNode#getTreeContentSize(org.lockss.daemon.CachedUrlSetSpec, boolean)
   * @param filter
   * @param calcIfUnknown
   * @return
   * @throws LockssRepositoryException
   */
  public List<PollHistory> getPollHistoryList() 
      throws LockssRepositoryException
  {
    List<PollHistory> liphReturn;
    Property propPollHistoryList;
    String strPollHistoryList;
    
    try {
      if (m_node.hasProperty(k_propPollHistoryList)) {
        propPollHistoryList = m_node.getProperty(k_propPollHistoryList);
        strPollHistoryList = propPollHistoryList.getString();
        liphReturn = (List<PollHistory>) sm_sotTransformer.transformStringToObject(strPollHistoryList);
        
      } else {  // Node does not have a prop history list.
        logger.debug3("getPollHistoryList: you should have set the poll history list prior to calling this method.");
        liphReturn = null;
      }
    } catch (RepositoryException e) {
      logger.error("Repository Exception: " + e.getMessage());
      throw new LockssRepositoryException(e);
    }
    
    return liphReturn;
  }
  
  public long getTreeContentSize(CachedUrlSetSpec filter,
      boolean calcIfUnknown /*
                             * , boolean mostRecentOnly = true */) 
      throws LockssRepositoryException {
    return getTreeContentSize(filter, calcIfUnknown, true);
  }
  
  /**
   * Notice that the filter is only used in the {@link RepositoryFile#getTreeContentSize()}
   * 
   * @see org.lockss.repository.v2.RepositoryNode#getTreeContentSize(org.lockss.daemon.CachedUrlSetSpec, boolean, boolean)
   * @param filter
   * @param calcIfUnknown
   * @param mostRecentOnly
   * @return
   * @throws LockssRepositoryException
   */
  public long getTreeContentSize(CachedUrlSetSpec filter,
      boolean calcIfUnknown, boolean mostRecentOnly) 
      throws LockssRepositoryException {
    boolean isComputed;
    long lContentSize;
    Node nodeIter;
    NodeIterator ni;
    Property propContentSize;
    Property propIsComputed;
    RepositoryNode rnIter;
    
    if (filter == null || filter.matches(m_url)) {
      try {
        propIsComputed = m_node.getProperty(k_propIsTreeSizeFilled);
        isComputed = propIsComputed.getBoolean();
        
        if (calcIfUnknown && !isComputed) {
          lContentSize = 0;
  
          // For each node under m_node...
          ni = m_node.getNodes();
          while (ni.hasNext()) {
            nodeIter = ni.nextNode();
            rnIter = constructor(m_session, nodeIter, m_idman);
  
            lContentSize += rnIter.getTreeContentSize(filter, calcIfUnknown, 
                    mostRecentOnly);
          }
  
          m_node.setProperty(k_propTreeContentSize, lContentSize);
          m_node.setProperty(k_propIsTreeSizeFilled, true);
        } else {
          propContentSize = m_node.getProperty(k_propTreeContentSize);
          lContentSize = propContentSize.getLong();
        }
      } catch (RepositoryException e) {
        logger.error("getTreeContentSize: " + e.getMessage());
        throw new LockssRepositoryException(e);
      }
    } else {  // Filtered out.
      lContentSize = 0;
    }
    
    return lContentSize;
  }

  
  public int hashCode() {
    return m_node.hashCode();
  }
  
  // BEST GUESS:
  // The node state is stored with each node.
  // We retrieve it from the storage, and set a few items on it.
  
  public NodeState loadNodeState(CachedUrlSet cus)
      throws LockssRepositoryException {
    NodeStateImpl nsiCurrent;
    Object objNodeState;
    Property propNodeState;
    String strNodeState;
    
    try {
      // If there is a node state already stored on the node,
      if (m_node.hasProperty(k_propNodeState)) {
        
        // Retrieve it
        propNodeState = m_node.getProperty(k_propNodeState);
        strNodeState = propNodeState.getString();
        objNodeState = sm_sotTransformer.transformStringToObject(strNodeState);
        if (objNodeState instanceof NodeStateImpl) {

          nsiCurrent = (NodeStateImpl) objNodeState;

          // Set parameters on it.
          // These changes come from HistoryRepositoryImpl.getNodeState.
          nsiCurrent.setCachedUrlSet(cus);
          
          // Restore if/when the RepositoryNodeImpl becomes a repository.
          // nodeState.repository = this;                
        } else {
          logger.error("loadNodeState: Major "
                  + "internal error: the node state was not the right type.");
          nsiCurrent = null;
        }
      } else {
        // No node state; create a default one.
        // This comes from HistoryRepositoryImpl.getNodeState.
        nsiCurrent = new NodeStateImpl(cus,
            -1,
            new CrawlState(-1, CrawlState.FINISHED, 0),
            new ArrayList(),
            null);
      }
    } catch (RepositoryException e) {
      logger.error("loadNodeState: " + e.getMessage());
      throw new LockssRepositoryException(e);
    }
    
    return nsiCurrent;
  }

  /**
   * These two methods assume that we're using the same session, and that the name of
   * the new repository file is the concatenation of:
   *   - the current node's name 
   *   - "/"
   *   - the parameter "name"
   *   
   * @see org.lockss.repository.v2.RepositoryNode#makeNewRepositoryFile(java.lang.String)
   * @param name
   * @return
   * @throws LockssRepositoryException
   */
  public RepositoryFile makeNewRepositoryFile(String name) 
      throws LockssRepositoryException {
    Node node;
    RepositoryFile rfNew = null;
    String url;
    
    try {
      node = createNode(name);
      url = m_url + "/" + name;
      rfNew = new RepositoryFileImpl(m_session, node, m_stemFile, 
          m_sizeWarcMax, url, m_idman);
    } catch (FileNotFoundException e) {
      logger.error("makeNewRepositoryFile: FileNotFoundException: " + e.getMessage());
      throw new LockssRepositoryException(e);      
    }
    
    invalidateTreeSize();
    
    return rfNew;
  }

  public RepositoryNode makeNewRepositoryNode(String name) 
      throws LockssRepositoryException {
    Node node;
    RepositoryNode rnNew = null;
    String url;
    
    node = createNode(name);
    url = m_url + "/" + name;
    rnNew = new RepositoryNodeImpl(m_session, node, m_stemFile, 
        m_sizeWarcMax, url, m_idman);
    
    invalidateTreeSize();
    
    return rnNew;
  }

  public void move(String stemNewLocation) throws LockssRepositoryException {
    Node nodeIter;
    NodeIterator ni;
    RepositoryNode rnIter;
    
    try {
      m_node.setProperty(k_propStemFile, stemNewLocation);
      
      m_session.save();
      m_session.refresh(true);
    } catch (RepositoryException e) {
      logger.error("move: ", e);
      throw new LockssRepositoryException(e);
    }
    
    try {
      // For each node under m_node...
      ni = m_node.getNodes();
      while (ni.hasNext()) {
        nodeIter = ni.nextNode();
        rnIter = constructor(m_session, nodeIter, m_idman);
        rnIter.move(stemNewLocation);
      }
    } catch (RepositoryException e) {
      logger.error("move: " + e.getMessage());
      throw new LockssRepositoryException(e);
    }
  }

  

  /**
   * Sets the properties within a RepositoryFile.
   * 
   * IMPORTANT: nodes don't have a node.setProperty(OutputStream) method.
   * Therefore, I temporarily store the whole properties file in a
   * byte[] array.
   * 
   * If these property sets become too large, then this method should
   * use a DeferredTempFileOutputStream. 
   */
  public void setProperties(Properties prop)
      throws IOException, LockssRepositoryException {
    ByteArrayInputStream baisProp = null;
    ByteArrayOutputStream baosProp = null;
    byte[] arbyProp;

    testIfNull(prop, "prop");
    
    try {
      baosProp = new ByteArrayOutputStream();
      prop.store(baosProp, "");
      arbyProp = baosProp.toByteArray();
    } finally {
      IOUtil.safeClose(baosProp);
    }
    
    try {
      baisProp = new ByteArrayInputStream(arbyProp);    
      m_node.setProperty(k_propProperties, baisProp);
      
      m_session.save();
      m_session.refresh(true);
    } catch (RepositoryException e) {
      throw new LockssRepositoryException(e);
    } finally {
      IOUtil.safeClose(baisProp);
    }
  }

  
  public void storeNodeState(NodeState nodeState)
      throws LockssRepositoryException {
    String strNodeState;
    
    strNodeState = sm_sotTransformer.transformObjectToString(nodeState);
    
    try {
      m_node.setProperty(k_propNodeState, strNodeState);
    } catch (RepositoryException e) {
      logger.error("storeNodeState: " + e.getMessage());
      throw new LockssRepositoryException(e);
    }
  }

  // Internally-used methods
  
}
