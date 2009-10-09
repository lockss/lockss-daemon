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
import java.net.*;
import java.util.*;

import javax.jcr.*;

import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.repository.*;
import org.lockss.repository.v2.RepositoryNode;
import org.lockss.repository.v2.*;
import org.lockss.state.*;
import org.lockss.util.*;

/**
 * @author edwardsb
 *
 * It is important that each AU be kept in separate directories.
 */
public class LockssAuRepositoryImpl extends BaseLockssManager
    implements HistoryRepository, LockssAuRepository {
  // == Constants
  // The following are used as properties in the JCR.
  private static final String k_propAuState = "AuState";
  private static final String k_propCreationTime = "CreationTime";
  private static final String k_propIdentityAgreement = "IdentityAgreement";
  private static final String k_propPeerId = "PeerId";
  private static final String k_strWarcInitial = "WARC";  // The string that appears before the numbers designating which WARC file to create.
      
  // Static variables
  private static Logger logger = Logger.getLogger("LockssAuRepositoryImpl");
  protected static ObjectSerializer sm_xssTransformer = 
    new XStreamSerializer();
  
  // Variables
  private ArchivalUnit m_au;
  private DatedPeerIdSet m_dpisNoAu;
  private JcrRepositoryHelper m_jrh;

  /**
   * You must call JcrRepositoryHelperFactory.preconstructor()
   * before you call this method.
   * 
   * @param au
   * @throws LockssRepositoryException
   */
  public LockssAuRepositoryImpl(
      ArchivalUnit au) 
      throws LockssRepositoryException {
    this(au, null);    
  }
   
  /**
   * Important note: If jrh is the special value 'null', then the code picks a random
   * helper repository.  It would be far better if the one-operator constructor could call 
   * LockssRepositoryImpl(au, m_jrhf.chooseRepositoryHelper()) -- but m_jrhf isn't 
   * initialized when the one-argument constructor is created.
   * 
   * @param au
   * @param jrh
   * @throws LockssRepositoryException
   */
  public LockssAuRepositoryImpl(
      ArchivalUnit au, JcrRepositoryHelper jrh) 
      throws LockssRepositoryException {
    JcrRepositoryHelperFactory jrhf;
    Node node;    
    
    
    // Store some variables
    if (!JcrRepositoryHelperFactory.isPreconstructed()) {
      logger.error("You must call JcrRepositoryHelperFactory.preconstructor before you call this routine.");
      throw new LockssRepositoryException("JcrRepositoryHelperFactory.preconstructor has not been called.");
    }
    
    jrhf = JcrRepositoryHelperFactory.getSingleton();

    m_au = au;
       
    try {
      // Put the LockssAuRepositoryImpl into an appropriate helper repository.
      if (m_jrh != null) {
        m_jrh = jrh;
      } else {  // Probably called by the one-argument constructor
        m_jrh = jrhf.chooseRepositoryHelper();
      }
            
      node = m_jrh.getRootNode();
      if (!node.hasProperty(k_propCreationTime)) {
        // Run the constructor.
        constructNewAu();
      }
    } catch (RepositoryException e) {
      logger.error("(constructor)", e);
      throw new LockssRepositoryException(e);
    }
  }

  
  
  /**
   * This method should perform consistency checks on the set of AU repositories
   * under its control.
   * 
   * At this time, there are no consistency checks.
   * 
   * @see org.lockss.repository.v2.LockssAuRepository#checkConsistency()
   */
  public void checkConsistency() {
    // For now, there is no consistency check.
    // As bugs get noticed (and auto-repairable), this method exists to 
    // find those bugs.
  }

  /**
   * This method is called to create this AU.
   * 
   * @throws LockssRepositoryException
   */
  private void constructNewAu() throws LockssRepositoryException {
    long lCreationTime;
    Node node;
    
    try {
      // Set the creation time.
      lCreationTime = System.currentTimeMillis();
      
      node = m_jrh.getRootNode();
      node.setProperty(k_propCreationTime, lCreationTime);      
      node.save();
      node.refresh(true);
    } catch (RepositoryException e) {
      logger.error("constructNewAu: Repository Exception: " + e.getMessage());
      throw new LockssRepositoryException(e);
    }
  }
  
  /**
   * Returns the creation time for this AU.
   * 
   * @see org.lockss.repository.v2.LockssAuRepository#getAuCreationTime()
   * @return A <code>long</code> representing the number of seconds after the epoch that this AU was created.
   */
  public long getAuCreationTime() 
      throws LockssRepositoryException {
    long lCreationTime = -1;
    Node node;
    Property propCreationTime;
    String strAuId;

    try {
      strAuId = m_au.getAuId();
      node = m_jrh.getRootNode();
      
      if (node.hasProperty(k_propCreationTime)) {
        propCreationTime = node.getProperty(k_propCreationTime);
        lCreationTime = propCreationTime.getLong();
      } else {  // k_propCreationTime was not set!
        logger.error("getAuCreationTime: The creation time was not set!");
        throw new LockssRepositoryException("The creation time was not set!");
      }
    } catch (RepositoryException e) {
      logger.error("getAuCreationTime: Repository Exception: " + e.getMessage());
      throw new LockssRepositoryException(e);
    }
    
    return lCreationTime;
  }
  
  /**
   * Method required by org.lockss.state.HistoryRepository
   * 
   * @deprecated
   * @return null.  Always.  This method is deprecated.
   */
  public File getAuStateFile() {
    logger.debug3("getAuStateFile called: Please use getAuStateRawContents instead.");
    return null;
  }

  /**
   * Following the behavior of HistoryRepositoryImpl, there is only one
   * AU State file per AU.
   * 
   * The calling method must close the InputStream. 
   * @return The raw contents of the AU state.
   */
  public InputStream getAuStateRawContents()
     throws LockssRepositoryException
  {
      return getInputStreamFromJcr(k_propAuState);
  }

  /**
   * This method returns the RepositoryFile associated with a URL.  You
   * will mostly use this method.
   * 
   * I assume that the URL is absolute, not a relative URL. 
   *
   * @see org.lockss.repository.v2.LockssAuRepository#getFile(java.lang.String, boolean)
   * @param strUrl  The URL that we're looking up 
   * @param create  If the URL does not exist, should we create it?
   * @return The associated <code>RepositoryFile</code>
   * @throws MalformedURLException
   * @throws LockssRepositoryException
   */
  public RepositoryFile getFile(String strUrl, boolean create)
      throws MalformedURLException, LockssRepositoryException {
    // Right now, the method constructs nodes along the way, until we
    // reach the end of the URL.  At that point, it constructs the file.
    
    // These restrictions make for slightly weird code...
    String[] arstrPath;
    int i;
    JcrRepositoryHelperFactory jrhf;
    RepositoryNodeImpl rniHost;
    RepositoryNodeImpl rniNode;
    RepositoryNodeImpl rniPath;
    RepositoryNodeImpl rniProtocol;
    String strHost;
    String strPath;
    String strProtocol;
    StringBuilder sbUrlConstructed;
    URL url;
    
    sbUrlConstructed = new StringBuilder();
    
    // Retrieve the base node.
    rniNode = (RepositoryNodeImpl) m_jrh.getRepositoryNode(strUrl); 
    
    if (rniNode == null) {
      if (create) {
        rniNode = createRepositoryNode(strUrl);
        // Create the base node.
        try {
          jrhf = JcrRepositoryHelperFactory.getSingleton();
          rniNode = (RepositoryNodeImpl) RepositoryNodeImpl.constructor(m_jrh.getSession(), 
              m_jrh.getRootNode(), m_jrh.getDirectory().toString() + "/" + k_strWarcInitial , strUrl);
        } catch (FileNotFoundException e) {
          logger.error("File Not Found Exception: ", e);
          throw new LockssRepositoryException(e);
        }
      } else {  // !create
        logger.debug3("The RepositoryFile didn't exist, and I was asked not to create it.  Returning null.");
        return null;
      }
    }
      
    url = new URL(strUrl);
    
    if (url.getProtocol() != null) {
      strProtocol = url.getProtocol();
      sbUrlConstructed.append(strProtocol);
      sbUrlConstructed.append("://");
      
      rniProtocol = (RepositoryNodeImpl) rniNode.getNode(
          createJcrSafeName(strProtocol), sbUrlConstructed.toString(), true);
    } else {
      // Getting this error would be very impressive:
      // I would expect that the call to new URL(strUrl) would throw
      // a Malformed URL Exception first...
      logger.error("getNode: URL does not have a protocol!  URL given: " + strUrl);
      throw new LockssRepositoryException("getNode: URL does not have a protocol!");
    }
    
    if (url.getHost() != null) {
      strHost = url.getHost();
      sbUrlConstructed.append(strHost);
      
      if (isEmptyString(url.getPath()) && 
          isEmptyString(url.getFile()) && 
          isEmptyString(url.getQuery())) {
        
       
        return (RepositoryFile) rniProtocol.getFile(createJcrSafeName(strHost), 
            sbUrlConstructed.toString(), true);
      }
      
      rniHost = (RepositoryNodeImpl) rniProtocol.getNode(createJcrSafeName(strHost), 
          sbUrlConstructed.toString(), true);
    } else {
      // No host is definitely an error.
      logger.error("getNode: the URL must be absolute, not relative.  URL given: " + strUrl);
      throw new LockssRepositoryException("getNode: URL must be absolute, not relative.");
    }
    
    // In the URL decoder, the "path" contains the file, but NOT 
    // the query.
    
    rniPath = rniHost;
    if (url.getPath() != null) {      
      arstrPath = url.getPath().split("/");
      for (i=0; i<arstrPath.length; i++) {
        strPath = arstrPath[i];
        if (strPath.length() > 0) {
          sbUrlConstructed.append("/" + strPath);
        }
        
        if (i == arstrPath.length - 1 && 
            isEmptyString(url.getQuery())) {
          return (RepositoryFile) rniPath.getFile(createJcrSafeName(strPath), 
              sbUrlConstructed.toString(), true);
        }
        
        if (strPath.length() > 0) {
          rniPath = (RepositoryNodeImpl) rniPath.getNode(createJcrSafeName(arstrPath[i]), 
              sbUrlConstructed.toString(), true);
        }
      }
    } 
    
    // These lines are executed only when there exists a query.
    sbUrlConstructed.append("?");
    sbUrlConstructed.append(url.getQuery());
    return (RepositoryFile) rniPath.getFile(createJcrSafeName(url.getQuery()), 
        sbUrlConstructed.toString(), true);
  }
  

  /**
   * Method required by org.lockss.state.HistoryRepository.
   * 
   *  @return null.  Always.
   *  @deprecated
   */
  public File getIdentityAgreementFile() {
    logger.debug3("getIdentityAgreementFile called: Please use getAuStateRawContents instead.");    
    return null;
  }


  /**
   * This method returns the identity agreement file as raw data.  It is
   * used for backups.
   * 
   * Notice that, following the behavior of HistoryRepositoryImpl, there is
   * only one identity agreement file in the directory.
   * 
   * The calling method must close the returned InputStream.
   * 
   * @see org.lockss.repository.v2.LockssAuRepository#getIdentityAgreementFile()
   * @return   The Identity Agreement contents.
   */
  public InputStream getIdentityAgreementRawContents() throws LockssRepositoryException {
    return getInputStreamFromJcr(k_propIdentityAgreement);
  }

  /**
   * Returns the DatedPeerIdSet associated with this AU.
   * 
   * @see org.lockss.state.HistoryRepository#getNoAuPeerSet()
   * @return  The dated, persistent peer ID set.
   */
  public DatedPeerIdSet getNoAuPeerSet() {
    IdentityManager idman;
    StreamerJcr strjcr;
    
    if (m_dpisNoAu == null) {
      idman = m_jrh.getIdentityManager();
      strjcr = new StreamerJcr(k_propPeerId, m_jrh.getRootNode());
      m_dpisNoAu = new DatedPeerIdSetImpl(strjcr, idman);
    }
    
    return m_dpisNoAu;
  }

  
  /**
   * Get a stream representing the peer ids associated with this repository.
   * 
   * @return An input stream containing the peer ids associated with this AU.
   * @throws LockssRepositoryException 
   */
  public InputStream getNoAuPeerSetRawContents() throws LockssRepositoryException {
    return getInputStreamFromJcr(k_propPeerId);
  }

  /**
   * This method returns the RepositoryNode associated with a URL.  Unless your URL
   * represents a directory (that you plan to iterate through), you probably will
   * want to use getFile(). 
   * 
   * This method assumes that the URL is absolute, not a relative URL.
   * 
   * @see org.lockss.repository.v2.LockssAuRepository#getNode(java.lang.String, boolean)
   * @param strUrl   What URL holds the repository node?     
   * @param create   If the URL doesn't yet exist, should we create it?
   * @return RepositoryNode corresponding to the URL.
   * @throws MalformedURLException
   * 
   */
  public RepositoryNode getNode(String strUrl, boolean create)
      throws MalformedURLException, LockssRepositoryException {
    int i;
    String[] arstrPath;
    RepositoryNodeImpl rniHost;
    RepositoryNodeImpl rniNode;
    RepositoryNodeImpl rniPath;
    RepositoryNodeImpl rniProtocol;
    RepositoryNodeImpl rniQuery;
    StringBuilder sbUrlConstructed;
    String strHost;
    String strPath;
    String strProtocol;
    URL url;
    
    sbUrlConstructed = new StringBuilder();
    
    // Construct the base node. 
    rniNode = (RepositoryNodeImpl) m_jrh.getRepositoryNode(m_au.getAuId()); 
    
    if (rniNode == null) {
      if (create) {
        rniNode = createRepositoryNode(strUrl);
      } else {  // Don't create...
        logger.debug3("getNode was not asked to create anything.  The repository node did not exist.");
        return null;
      }
    }
     
    url = new URL(strUrl);
    
    if (url.getProtocol() != null) {
      strProtocol = url.getProtocol();
      sbUrlConstructed.append(strProtocol);
      sbUrlConstructed.append("://");

      rniProtocol = (RepositoryNodeImpl) rniNode.getNode(
          createJcrSafeName(strProtocol), sbUrlConstructed.toString(), 
          true);
    } else {
      // Getting this error would be very impressive:
      // I would expect that the call to new URL(strUrl) would throw
      // a Malformed URL Exception first...
      logger.error("getNode: URL does not have a protocol!  URL given: " + strUrl);
      throw new LockssRepositoryException("getNode: URL does not have a protocol!");
    }
    
    if (url.getHost() != null) {
      strHost = url.getHost();
      sbUrlConstructed.append(strHost);
      sbUrlConstructed.append("/");
      
      rniHost = (RepositoryNodeImpl) rniProtocol.getNode(createJcrSafeName(url.getHost()), 
          sbUrlConstructed.toString(), true);
    } else {
      // No host is definitely an error.
      logger.error("getNode: the URL must be absolute, not relative.  URL given: " + strUrl);
      throw new LockssRepositoryException("getNode: URL must be absolute, not relative.");
    }
    
    // In the URL decoder, the path contains the file, but not the query.
    
    rniPath = rniHost;
    if (url.getPath() != null) {      
      arstrPath = url.getPath().split("/");
      for (i=0; i<arstrPath.length; i++) {
        strPath = arstrPath[i];
        if (strPath.length() > 0) {
          sbUrlConstructed.append(strPath);
          sbUrlConstructed.append("/");
  
          rniPath = (RepositoryNodeImpl) rniNode.getNode(createJcrSafeName(strPath), 
              sbUrlConstructed.toString(), true);
        }
      }
    } 
    // No 'else' needed for two reasons:
    // 1. A URL can have no path.
    // 2. The rnPath was set before the 'if' statement.
    
          
    if (url.getQuery() != null) {
      sbUrlConstructed.append("?");
      sbUrlConstructed.append(url.getQuery());
      
      rniQuery = (RepositoryNodeImpl) rniPath.getNode(createJcrSafeName(url.getQuery()), 
          sbUrlConstructed.toString(), true);
    } else {
      rniQuery = rniPath;
    }
      
    return rniQuery;
  }

  /**
   * Does this AU Repository have a NoAuPeerSet?
   * 
   * @return Does this AU Repository have a NoAuPeerSet?
   */
  public boolean hasNoAuPeerSet() {
    Node nodeRoot;
    try {
      nodeRoot = m_jrh.getRootNode();
      return nodeRoot.hasProperty(k_propPeerId);
    } catch (RepositoryException e) {
      logger.error("hasNoAuPeerSet: RepositoryException: ", e);
      logger.error("Thrown into the bit bucket; I'm just returning false.");
      return false;
    }
  }
  

  /**
   * Slightly misnamed: This method returns the current AU State.
   * 
   * @see org.lockss.repository.v2.LockssAuRepository#loadAuState()
   * @return AuState
   */
  public AuState loadAuState() {
    AuState auState;
    InputStream istrAu;
    String strError = "Could not load AU state for AU '" + m_au.getName() + "' :"; 

    logger.debug3("Loading state for AU '" + m_au.getName() + "'");

    // Get all the text in the AU state file.
    try {
      istrAu = getAuStateRawContents();
      auState = (AuState) sm_xssTransformer.deserialize(istrAu);
    } catch (LockssRepositoryException e) {
      logger.error(strError, e);
      return new AuState(m_au, this);
    } catch (SerializationException.FileNotFound e) {
      logger.error(strError, e);
      return new AuState(m_au, this);
    } catch (SerializationException e) {
      logger.error(strError, e);
      return new AuState(m_au, this);      
    } catch (InterruptedIOException e) {
      logger.error(strError, e);
      return new AuState(m_au, this);      
    } 

    return auState;
  }

  /** 
   * This method is a stub, and does nothing useful.
   * 
   * @see org.lockss.repository.v2.LockssAuRepository#loadDamagedNodeSet()
   * @return null
   * @deprecated
   */
  public DamagedNodeSet loadDamagedNodeSet() {
    logger.debug1("loadDamagedNodeSet called; it's a stub and does nothing useful.");
    
    return null;
  }

  /**
   * HistoryRepositoryImpl.loadIdentityAgreements is badly named: it doesn't 
   * load the identity agreements; it only retrieves them.  This method follows
   * that pattern.
   * 
   * @see org.lockss.repository.v2.LockssAuRepository#loadIdentityAgreements()
   * @return List of identity agreements.
   * @exception LockssRepositoryException
   */
  public List loadIdentityAgreements() 
      throws LockssRepositoryException {
    InputStream istrIdentityAgreements;
    List listIdentityAgreements;
    
    try {
      istrIdentityAgreements = getInputStreamFromJcr(k_propIdentityAgreement);
      
      listIdentityAgreements = (List) sm_xssTransformer.deserialize(istrIdentityAgreements);
    } catch (SerializationException e) {
      logger.error("loadIdentityAgreements: " + e.getMessage());
      throw new LockssRepositoryException(e);
    } catch (InterruptedIOException e) {
      logger.error("loadIdentityAgreements: " + e.getMessage());
      throw new LockssRepositoryException(e);      
    }
    
    return listIdentityAgreements;
  }

  /**
   * Stub method.
   * 
   * @see org.lockss.state.HistoryRepository#loadNodeState(org.lockss.plugin.CachedUrlSet)
   * @param cus
   * @return null.  Always.
   * @deprecated
   */
  public NodeState loadNodeState(CachedUrlSet cus) {
    logger.debug3("loadNodeState called.  This method is a stub.");
    
    return null;
  }

  /**
   * This method is a stub method.  It does nothing any longer.
   * 
   * @see org.lockss.repository.v2.LockssAuRepository#loadPollHistories(org.lockss.state.NodeState)
   * @param nodeState
   * @deprecated
   */
  public void loadPollHistories(NodeState nodeState) {
    logger.debug3("loadPollHistories is now a stub.");
    throw new RuntimeException("loadPollHistories is a stub.");
  }

  /**
   * Used to start computing the AU sizes.  The sizes are stored
   * inside the RepositoryNode nodes; this method returns nothing.
   * 
   * This method should be called inside its own thread.  The repository
   * nodes should cache their sizes.
   * 
   * Because this method only calculates a queue size, it only
   * reports -- then swallows -- Lockss Repository Exceptions.
   * Feel free to disagree with me on this choice.
   * 
   * @param rn  Which node to compute on
   */

  public void queueSizeCalc(RepositoryNode rn) {
    synchronized (m_jrh.m_sizeCalcQueue) {
      if (m_jrh.m_sizeCalcQueue.add(rn)) {
        logger.debug2("Queue size calc: " + rn);
        m_jrh.startOrKickThread();
      }
    }
  }

  /**
   * This method is now a stub.
   * 
   * @see org.lockss.app.LockssAuManager#setAuConfig(org.lockss.config.Configuration)
   * @param auConfig
   * @deprecated
   */
  public void setAuConfig(Configuration auConfig) {
    // Both HistoryRepositoryImpl.setAuConfig() and 
    // LockssRepositoryImpl.setAuConfig() are empty.  Therefore,
    // this method is (currently) empty.
    logger.debug3("setAuConfig called -- this method is a stub.");
  }

  /**
   * This method restores an input stream from backup.
   * 
   * @see org.lockss.repository.v2.LockssAuRepository#storeAuState(org.lockss.state.AuState)
   * @param istrAuState
   * @throws LockssRepositoryException
   */
  public void storeAuStateRawContents(InputStream istrAuState) 
      throws LockssRepositoryException {
    storeStreamForJcr(istrAuState, k_propAuState);
  }

  /** 
   * This method is a stub, and does nothing useful.
   * 
   * @see org.lockss.repository.v2.LockssAuRepository#storeDamagedNodeSet(org.lockss.state.DamagedNodeSet)
   * @param dns
   * @deprecated
   */
  public void storeDamagedNodeSet(DamagedNodeSet dns) {
    logger.debug3("storeDamagedNodeSet called.  This method is a stub.");
  }

  /**
   * Re-create the identity agreements from a stream.
   * 
   * This method useful when restoring from a backup.
   * 
   * @see org.lockss.repository.v2.LockssAuRepository#storeIdentityAgreements(java.util.List)
   * @param istrIdentityAgreements  A stream containing the identity agreements
   * @throws LockssRepositoryException
   */
  public void storeIdentityAgreementsRawContents(InputStream istrIdentityAgreements) 
  throws LockssRepositoryException {
    storeStreamForJcr(istrIdentityAgreements, k_propIdentityAgreement);
  }

  /**
   * Stub method.  Not to be called.
   * 
   * @see org.lockss.state.HistoryRepository#storeNodeState(org.lockss.state.NodeState)
   * @param nodeState
   * @deprecated
   */
  public void storeNodeState(NodeState nodeState) {
    logger.debug3("storeNodeState called.  This method is a stub.");    
  }

  
  /**
   * Stub method.  Not to be called.
   * 
   * @see org.lockss.state.HistoryRepository#storePollHistories(org.lockss.state.NodeState)
   * @param nodeState
   * @deprecated
   */
  public void storePollHistories(NodeState nodeState) {
    logger.debug3("storePollHistories called.  This method is a stub.");
  }

  
  /**
   * This is a stub method.
   * 
   * I'm using the logger.error() because this routine does NOT do what I
   * guess people expect.  It does NOT compute the queue size under an
   * old-style repository node.
   * 
   * However, I do not throw an exception because people don't expect this
   * routine to throw anything.  Calling the wrong queue size calc will only
   * cause later examinations of the queue size to be slower.
   * 
   * Feel free to disagree with me on either of the above assumptions.
   * 
   * @see org.lockss.repository.v2.LockssAuRepository#queueSizeCalc(org.lockss.repository.v2.RepositoryNode)
   * @param node
   * @deprecated
   */
  public void queueSizeCalc(org.lockss.repository.RepositoryNode node) {
    logger.error("Called queueSizeCalc with the wrong kind of RepositoryNode.");
  }
  
  /**
   * Store the (one and only) AU State associated with this AU Repository.
   * 
   * NOTE: This method tosses LockssRepositoryExceptions into the bit bucket,
   * due to <code>HistoryRepository</code>.
   * 
   * @param auState  What to store.
   */
  public void storeAuState(AuState auState){
    try {
      storeObjectInJcr(auState, k_propAuState);
    } catch (LockssRepositoryException e) {
      logger.error("storeAuState: LockssRepositoryException: ", e);
      /* Throw the exception into the bit bucket. */
    }
  }

  /**
   * Store the identity agreements.
   * 
   * @param list  The list of identity agreements.
   * @throws LockssRepositoryException 
   */
  public void storeIdentityAgreements(List list)
          throws LockssRepositoryException {
    if (list instanceof Serializable) {
      Serializable serList = (Serializable) list;

      storeObjectInJcr(serList, k_propIdentityAgreement);
    } else {
      logger.error("The identity agreements were not serializable.  Therefore, they could not be serialized.");
    }
  }

  /**
   * Store the NoAu set, as an input stream.  This method is useful for restoring
   * from backups.
   * 
   * @param istr  An input stream containing the NoAu raw contents.
   * @throws LockssRepositoryException
   */
  public void storeNoAuRawContents(InputStream istr)
  throws LockssRepositoryException
  {
    try {
      storeStreamForJcr(istr, k_propPeerId);
    } catch (LockssRepositoryException e) {
      logger.error("storeAuState: LockssRepositoryException: ", e);
      /* Throw the exception into the bit bucket. */
    }    
  }

  /**
   * Retrieve the disk usage for this repository.
   * 
   * @param calcIfUnknown Calculate the repository disk usage, if we don't know it.
   * @throws LockssRepositoryException
   * @see org.lockss.repository.v2.LockssAuRepository#getRepoDiskUsage(boolean)
   */
  public long getRepoDiskUsage(boolean calcIfUnknown) throws LockssRepositoryException {
    RepositoryNode rn;
    
    rn = m_jrh.getRepositoryNode(m_au.getAuId());
    
    if (rn == null) {
      rn = createRepositoryNode(m_au.getAuId());
    }
    
    return rn.getTreeContentSize(null, calcIfUnknown);
  }  

  // --- Private methods ---
  
  /**
   * Convert a generic string to one that can be used by JCR.
   * This method must be one-to-one.
   * 
   * JCR cannot use any of the following characters:
   *  . / : [ ] * ' " | or whitespace
   *  
   *  This method generates an intended-to-be-one-way map from any string to 
   *  a JCR-safe name.
   *  
   *  This encoding is similar to the conversion that's done to URLs.
   *  A # is used instead of %, so that URL substitutions and JCR
   *  substitutions don't clash.  (A # character can never be part of a URL.)
   *  
   *  @param strName   The name to be converted
   */
  private String createJcrSafeName(String strName) {
    // According to a meeting, which white space used may impact the
    // URL.  Therefore, I must convert all white space individually...
    
    // This list of spaces comes from "http://www.fileformat.info/info/unicode/category/Zs/list.htm".
    strName = strName.replace("\\s", "#0020");
    strName = strName.replace("\u00a0", "#00a0");
    strName = strName.replace("\u1680", "#1680");
    strName = strName.replace("\u180e", "#1803");
    strName = strName.replace("\u2000", "#2000");
    strName = strName.replace("\u2001", "#2001");
    strName = strName.replace("\u2002", "#2002");
    strName = strName.replace("\u2003", "#2003");
    strName = strName.replace("\u2004", "#2004");
    strName = strName.replace("\u2005", "#2005");
    strName = strName.replace("\u2006", "#2006");
    strName = strName.replace("\u2007", "#2007");
    strName = strName.replace("\u2008", "#2008");
    strName = strName.replace("\u2009", "#2009");
    strName = strName.replace("\u200a", "#200a");
    strName = strName.replace("\u202f", "#202f");
    strName = strName.replace("\u205f", "#205f");
    strName = strName.replace("\u3000", "#3000");
    
    // This list of control structures comes from "http://www.unicode.org/versions/Unicode5.0.0/ch16.pdf".
    strName = strName.replace("\u0000", "#0000");
    strName = strName.replace("\u0009", "#0009");
    // "\\u000a" without the double backslash gives an error!
    strName = strName.replace("\n", "#000a");
    strName = strName.replace("\u000b", "#000b");
    strName = strName.replace("\u000c", "#000c");
    strName = strName.replace("\r", "#000d");
    strName = strName.replace("\u001c", "#001c");
    strName = strName.replace("\u001d", "#001d");
    strName = strName.replace("\u001e", "#001e");
    strName = strName.replace("\u001f", "#001f");
    strName = strName.replace("\u0085", "#0085");
    strName = strName.replace("\u00a0", "#00a0");
    strName = strName.replace("\u00ad", "#00ad");
    strName = strName.replace("\u2060", "#2060");
    strName = strName.replace("\u200b", "#200b");
    strName = strName.replace("\u200c", "#200c");
    strName = strName.replace("\u200d", "#200d");
    strName = strName.replace("\u2028", "#2028");
    strName = strName.replace("\u2029", "#2029");
    strName = strName.replace("\ufeff", "#feff");
    
    // The above are all whitespace (or similar things.)  Many other
    // Unicode characters might be considered whitespace (for example, 
    // combining marks, titlecase marks, and the like) but aren't in
    // the list above.  I hope -- but have not tested -- that JCR
    // accepts these characters as-is.
    
    // The rest of the characters...
    strName = strName.replace(".", "#002e");
    strName = strName.replace("/", "#002f");
    strName = strName.replace(":", "#003a");
    strName = strName.replace("[", "#005b");
    strName = strName.replace("]", "#005d");
    strName = strName.replace("*", "#002a");
    strName = strName.replace("'", "#0027");
    strName = strName.replace("\"", "#0022");
    strName = strName.replace("|", "#007c");
    strName = strName.replace("\\", "#005c");
    
    return strName;
  }

  // The following methods are derived from the ones in RepositoryManager.

  /**
   * Used to copy an InputStream to a temporary file.
   * 
   * It is the calling method's responsibility to delete this temporary
   * file when it's done with it.
   * 
   * @param istr       The stream to copy from
   * @param filename   Part of the file name.  It's still a temporary file.
   * @return           A File, containing the input stream.
   */
  private File createFileFromInputStream(InputStream istr, String filename) {
    File fileReturn;
    FileOutputStream fostr;
    
    try {
      fileReturn = FileUtil.createTempFile(filename, null);
      fostr = new FileOutputStream(fileReturn);
      
      StreamUtil.copy(istr, fostr);
    } catch (IOException e) {
      logger.error("createFileFromInputStream: IOException ", e);
      return null;
    }
    
    return fileReturn;
  }

  /**
   * Just create a repository node.
   * 
   * @param strUrl  The URL to create
   * @return  A <code>RepositoryNodeImpl</code> at the correct URL.
   * @throws LockssRepositoryException
   */
  private RepositoryNodeImpl createRepositoryNode(String strUrl) throws LockssRepositoryException {
    JcrRepositoryHelperFactory jrhf;
    RepositoryNodeImpl rniNode;
    
    try {
      jrhf = JcrRepositoryHelperFactory.getSingleton();
      rniNode = (RepositoryNodeImpl) RepositoryNodeImpl.constructor(m_jrh.getSession(), m_jrh.getRootNode(),
          m_jrh.getDirectory().toString() + "/" + k_strWarcInitial, strUrl);
      m_jrh.addRepositoryNode(strUrl, rniNode);
    } catch (FileNotFoundException e) {
      logger.error("In getNode", e);
      throw new LockssRepositoryException(e);
    }
    return rniNode;
  }
  
  
  /**
   * Return an input stream from a given prop in the JCR.
   * 
   * @param prop  Where to find the input stream
   * @return  InputStream
   * @throws LockssRepositoryException
   */
  private InputStream getInputStreamFromJcr(String prop) 
        throws LockssRepositoryException {
    InputStream istr;
    Node nodeRoot;
    Property propReturn;
    
    nodeRoot = m_jrh.getRootNode();
    
    try {
      if (nodeRoot.hasProperty(prop)) {
        propReturn = nodeRoot.getProperty(prop);
        istr = propReturn.getStream();
      } else { // Doesn't have property k_propAuState
        logger.debug3("getInputStreamFromJcr: property " + prop +
                        "does not exist.  Returning null.");
        istr = null;
      }
    } catch (RepositoryException e) {
      logger.error("getInputStreamFromJcr", e);
      throw new LockssRepositoryException(e);
    }
    
    return istr;
  }
  
  /**
   * Whether a string is null or has length zero.  It's simple, actually.
   * 
   * @param str  What we're testing
   * @return  boolean: false if there's something in the string.
   */
  private boolean isEmptyString(String str) {
    return str == null || str.length() == 0;
  }
  
  /**
   * Store any object in the JCR database.
   * 
   * @param objStore  What you're storing
   * @param prop      Where you're storing it.
   * @throws LockssRepositoryException
   */
  private void storeObjectInJcr(Serializable objStore, String prop) 
      throws LockssRepositoryException {
    byte[] arbyAuState = null;
    ByteArrayInputStream baisAuState = null;
    ByteArrayOutputStream baosAuState = null;
    Node node;
    Session session;

    try {
      baosAuState = new ByteArrayOutputStream();
      sm_xssTransformer.serialize(baosAuState, objStore);
      arbyAuState = baosAuState.toByteArray();
    } catch (InterruptedIOException e) {
      logger.error("storeObjectInJcr: InterruptedIOException ", e);
      throw new LockssRepositoryException(e);
    } catch (SerializationException e) {
      logger.error("storeObjectInJcr: SerializationException ", e);
      throw new LockssRepositoryException(e);     
    } finally {
      IOUtil.safeClose(baosAuState);
    }
    
    try {
      node = m_jrh.getRootNode();
      session = m_jrh.getSession();
      
      baisAuState = new ByteArrayInputStream(arbyAuState);    
      node.setProperty(prop, baisAuState);
      
      session.save();
      session.refresh(true);
    } catch (RepositoryException e) {
      logger.error("storeObjectInJcr: RepositoryException ", e);
      throw new LockssRepositoryException(e);
    } finally {
      IOUtil.safeClose(baisAuState);
    }
  }

  /**
   * Since LockssSerializable is NOT derived from Serializable, I
   * need two methods to store objects.
   * 
   * @param objStore
   * @param prop
   * @throws LockssRepositoryException
   */
  private void storeObjectInJcr(LockssSerializable objStore, String prop) 
  throws LockssRepositoryException {
    byte[] arbyAuState = null;
    ByteArrayInputStream baisAuState = null;
    ByteArrayOutputStream baosAuState = null;
    Node node;
    Session session;

    try {
      baosAuState = new ByteArrayOutputStream();
      sm_xssTransformer.serialize(baosAuState, objStore);
      arbyAuState = baosAuState.toByteArray();
    } catch (InterruptedIOException e) {
      logger.error("storeObjectInJcr: InterruptedIOException ", e);
      throw new LockssRepositoryException(e);
    } catch (SerializationException e) {
      logger.error("storeObjectInJcr: SerializationException ", e);
      throw new LockssRepositoryException(e);     
    } finally {
      IOUtil.safeClose(baosAuState);
    }

    try {
      node = m_jrh.getRootNode();
      session = m_jrh.getSession();

      baisAuState = new ByteArrayInputStream(arbyAuState);    
      node.setProperty(prop, baisAuState);

      session.save();
      session.refresh(true);
    } catch (RepositoryException e) {
      logger.error("storeObjectInJcr: RepositoryException ", e);
      throw new LockssRepositoryException(e);
    } finally {
      IOUtil.safeClose(baisAuState);
    }
  }


  /**
   * Store a stream in the repository.
   * 
   * @param istr is an <code>InputStream</code>, not an <code>OutputStream</code>
   * @param prop  Where to store it.
   * @throws LockssRepositoryException
   */
  private void storeStreamForJcr(InputStream istr, String prop)
      throws LockssRepositoryException {
    Node nodeRoot;
    
    nodeRoot = m_jrh.getRootNode();
    
    try {
      nodeRoot.setProperty(prop, istr);
    } catch (RepositoryException e) {
      logger.error("setStreamForJcr", e);
      throw new LockssRepositoryException(e);
    }
  }
  
  /**
   * Calculate the size of a node.
   * 
   * @param node
   */
  void doSizeCalc(RepositoryNode node) {
    try {
      node.getTreeContentSize(null, true);
      if (node instanceof AuNodeImpl) {
        ((AuNodeImpl)node).getDiskUsage(true);
      }
    } catch (LockssRepositoryException e) {
      logger.debug("doSizeCalc: LockssRepositoryException: ", e);
    }
  }



  // --- Other classes ---
  

  /**
   * This factory returns the same LockssAuRepositoryImpl for the same
   * AU.  It depends on LockssLinkedHashMap.
   * 
   * @author edwardsb
   *
   */
  public static class LockssAuRepositoryFactory 
  implements LockssAuManager.Factory {
    
    public LockssAuManager createAuManager(ArchivalUnit au) {
      LockssAuRepository lar;
      
      try {
        lar = new LockssAuRepositoryImpl(au);
      } catch (LockssRepositoryException e) {
        logger.error("LockssAuRepositoryFactory.createAuManager: Lockss Repository Exception", e);
        lar = null;
      }
      
      return lar;
    }
  }
}
