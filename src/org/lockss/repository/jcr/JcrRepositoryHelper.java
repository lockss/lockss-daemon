/*
 * $Id: JcrRepositoryHelper.java,v 1.1.2.1 2009-09-30 23:02:32 edwardsb1 Exp $
 */
/*
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
import java.sql.*;
import java.util.*;

import javax.jcr.*;

import org.apache.commons.collections.map.LRUMap;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.*;
import org.apache.jackrabbit.core.data.*;
import org.lockss.app.*;
import org.lockss.config.Configuration;
import org.lockss.protocol.IdentityManager;
import org.lockss.repository.LockssRepositoryException;
import org.lockss.repository.v2.RepositoryNode;
import org.lockss.util.*;

/**
 * @author edwardsb
 *
 * This class represents one repository of JcrHelperPerAu's.
 */
public class JcrRepositoryHelper extends BaseLockssDaemonManager {
  // Constants 
  static private final String k_DATASTORE = "LargeDatastore.xml";
  static private final String k_FILENAME_DATASTORE = "/org/lockss/repository/jcr/DatastoreFiles/LargeDatastore.xml";
  static private final int k_LRUSize = 20;
  static private final String k_PASSWORD = "password";
  static protected final String k_SIZE_WARC_MAX = "SizeWarcMax";
  static protected final String k_USERNAME = "user";

  // Static variables
  static private Logger logger = Logger.getLogger("JcrRepositoryHelper.java");
  
  // Class Variables
  private File m_directory;
  private IdentityManager m_idman;
  private LRUMap m_mapstrrnCache = new LRUMap (k_LRUSize);
  private Node m_nodeRoot;
  private RepositoryConfig m_repconfig;
  private RepositoryImpl m_repos;
  private Session m_session;
  private long m_sizeWarcMax;
  
  // Constructor...
  JcrRepositoryHelper(
      File directory, 
      long sizeWarcMax, 
      IdentityManager idman,
      LockssDaemon ld) throws LockssRepositoryException {
    super();
    
    InputStream istrXml;
    FileOutputStream fosXml;

    m_sizeWarcMax = sizeWarcMax;
    m_directory = directory;
    
    // Set up the node and session.
    m_idman = idman;
    initService(ld);
    
    // Create the directory if necessary.
    if (directory.exists()) {
      if (! directory.isDirectory()) {
        logger.error("(constructor): Given location for files was not a directory.  Aborting!");
        return;
      }
    } else {  // The directory does not exist; create it (and any parents).
      directory.mkdirs();
    }
    
    // Copy the files into the directory.
    try {
      istrXml = getClass().getResourceAsStream(k_FILENAME_DATASTORE);
      fosXml = new FileOutputStream(directory + File.separator + k_DATASTORE);
      StreamUtil.copy(istrXml, fosXml);
    } catch (FileNotFoundException e) {
      logger.error("(constructor): Very unexpected exception: ", e);
      logger.error("Aborting.");
      return;
    } catch (IOException e) {
      logger.error("(constructor): ", e);
      logger.error("Aborting.");
      return;
    }
    
    // For most Jackrabbit repositories, the user name and password
    // don't matter.  
    try {
      m_repconfig = RepositoryConfig.create(directory + File.separator + k_DATASTORE,
          directory.toString());
      m_repos = RepositoryImpl.create(m_repconfig);
      m_session = m_repos.login(new SimpleCredentials(k_USERNAME, k_PASSWORD
          .toCharArray()));
      m_nodeRoot = m_session.getRootNode();
      m_sizeWarcMax = sizeWarcMax;      
    } catch (ConfigurationException e) {
      logger.error("(constructor): Configuration exception: ", e);
      throw new LockssRepositoryException(e);
    } catch (LoginException e) {
      // Obviously, in this case, the user name and password did matter!
      logger.error("(constructor): Login exception: ", e);
      throw new LockssRepositoryException(e);
    } catch (RepositoryException e) {
      logger.error("(constructor): Repository Exception: ", e);
      throw new LockssRepositoryException(e);
    } 
  }
  
  // I assume that the repository node is part of the repository here.
  public void addRepositoryNode(String key, RepositoryNode rnAdd) throws LockssRepositoryException
  { 
    JcrRepositoryHelperFactory jrhf;
    
    m_mapstrrnCache.put(key, rnAdd);
    
    jrhf = JcrRepositoryHelperFactory.getSingleton();
    jrhf.addHelperRepository(key, this);
  }
  
  public File getDirectory() {
    return m_directory;
  }
  
  public IdentityManager getIdentityManager() {
    return m_idman;
  }
  
  public RepositoryConfig getRepositoryConfig() {
    return m_repconfig;
  }
  
  // If the node isn't in memory, this method looks for it in the repository.  
  // 
  
  public RepositoryNode getRepositoryNode(String key) throws LockssRepositoryException {
    JcrRepositoryHelperFactory jrhf;
    RepositoryNode rnReturn;
    
    rnReturn = (RepositoryNode) m_mapstrrnCache.get(key);
    
    if (rnReturn != null) {
      return rnReturn;
    }

    // Get it from the repository (which will construct the node
    // if necessary.
    
    jrhf = JcrRepositoryHelperFactory.getSingleton();
    
    rnReturn = RepositoryNodeImpl.constructor(m_session, m_nodeRoot, m_idman);
    
    if (rnReturn != null) {
      m_mapstrrnCache.put(key, rnReturn);
    }
    
    return rnReturn;
  }
  
  public Node getRootNode() {
    return m_nodeRoot;
  }
  
  public Session getSession() {
    return m_session;
  }
  
  public long getSizeWarcMax() {
    return m_sizeWarcMax;
  }
  
  public void moveRepository(String strAuId, String stemNew) throws LockssRepositoryException {
    RepositoryNode rn;
    
    rn = (RepositoryNode) m_mapstrrnCache.get(strAuId);
    
    if (rn != null) {
        rn.move(stemNew);
    } else {  // rn == null
      logger.error("moveRepository: The requested repository does not exist.");
      logger.error("Throwing the exception into the bit-bucket.");
    }
  }
  
  // This method is used for testing.
  void reset() {
    DataStore ds;
    
    if (m_repos != null) {
      ds = m_repos.getDataStore();
      if (ds != null) {
        try {
          ds.clearInUse();
          ds.close();
        } catch (DataStoreException e) {
          e.printStackTrace();
        }
      }

      m_repos.shutdown();       
      m_repos = null;
    }

    // In order to shut down the Derby database, we need to do this...
    // See:
    // http://publib.boulder.ibm.com/infocenter/cldscp10/index.jsp?topic=/com.ibm.cloudscape.doc/develop15.htm

    try {
      DriverManager.getConnection("jdbc:derby:;shutdown=true");
    } catch (SQLException e) {
      // From the documentation:

      // A successful shutdown always results in an SQLException to indicate
      // that Cloudscape [Derby]
      // has shut down and that there is no other exception.
    }
  }
  
  public void setConfig(Configuration config, Configuration oldConfig, 
      Configuration.Differences changedKeys) {
    m_sizeWarcMax = config.getLong(k_SIZE_WARC_MAX, m_sizeWarcMax);
  }
  
  public void startService() {
    super.startService();
  }
}
