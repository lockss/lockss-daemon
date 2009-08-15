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
import java.sql.*;
import java.util.*;

import javax.jcr.*;

import org.apache.jackrabbit.core.*;
import org.apache.jackrabbit.core.config.*;
import org.apache.jackrabbit.core.data.*;
import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.protocol.*;
import org.lockss.repository.LockssRepositoryException;
import org.lockss.repository.v2.RepositoryNode;
import org.lockss.util.*;

/**
 * @author edwardsb
 *
 * This class holds all preferences for the Jackrabbit installation, and 
 * it generates the sessions.  
 */
public class LockssJackrabbitHelper extends BaseLockssDaemonManager {
  // Constants
  // Note: the password and user name are emphatically NOT private.
  // (The default for a JCR repository is that any user name and
  // password works for the database.)
  static private final String k_DATASTORE = "LargeDatastore.xml";
  static private final String k_FILENAME_DATASTORE = "/org/lockss/repository/jcr/DatastoreFiles/LargeDatastore.xml"; 
  static private final String k_PASSWORD = "password";
  static protected final String k_SIZE_WARC_MAX = "SizeWarcMax";
  static protected final String k_STEM_FILE = "StemFile";
  static protected final String k_USERNAME = "user";
  
  // Static variables
  static private LockssJackrabbitHelper m_ljrsThis = null;
  private static Logger logger = Logger.getLogger("LockssJackrabbitHelper");
  
  // Class variables
  private IdentityManager m_idman;
  private Node m_nodeRoot;
  private RepositoryImpl m_repos;
  private RepositoryConfig m_repconfig;
  private Map<String, RepositoryNode> m_marepnode;
  private Session m_session;
  private long m_sizeWarcMax;
  private String m_stemFile;
  private String m_url;
  
  // Constructor...
  
  /**
   * @param strDirectory: Directory for the datastore .xml 
   * @param filename Filename for the datastore .xml file
   * @param stemFile Start of filename to store data
   * @param sizeWarcMax Maximum size of any WARC files
   * @param url Base of any URL
   * @param idman The ID Manager of the parent process
   * @param ld The Lockss Daemon of the parent process.
   */
  private LockssJackrabbitHelper(String strDirectory, 
    String stemFile, long sizeWarcMax, String url, IdentityManager idman,
    LockssDaemon ld, String auId) 
    throws LockssRepositoryException {
    super();
    
    File fileDirectory;
    InputStream istrXml;
    FileOutputStream fosXml;
    RepositoryNode repnode;

    // Set up the node and session
    m_idman = idman;
    initService(ld);
    
    // Create the directory if necessary.
    fileDirectory = new File(strDirectory);
    if (fileDirectory.exists()) {
      if (! fileDirectory.isDirectory()) {
        logger.error("(constructor): Given location for files was not a directory.  Aborting!");
        return;
      }
    } else {  // The directory does not exist; create it (and any parents).
      fileDirectory.mkdirs();
    }
    
    // Copy the files into the directory.
    try {
      istrXml = getClass().getResourceAsStream(k_FILENAME_DATASTORE);
      fosXml = new FileOutputStream(strDirectory + File.pathSeparator + k_DATASTORE);
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
      m_stemFile = stemFile;
      m_repconfig = RepositoryConfig.create(strDirectory + File.pathSeparator + k_DATASTORE,
          strDirectory);
      m_repos = RepositoryImpl.create(m_repconfig);
      m_session = m_repos.login(new SimpleCredentials(k_USERNAME, k_PASSWORD
          .toCharArray()));
      m_nodeRoot = m_session.getRootNode();
      m_sizeWarcMax = sizeWarcMax;
      m_url = url;
      
      m_marepnode = new HashMap<String, RepositoryNode>();
      
      repnode = RepositoryNodeImpl.constructor(m_session, m_nodeRoot, stemFile, sizeWarcMax, url, m_idman);
      m_marepnode.put(auId, repnode);
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
    } catch (FileNotFoundException e) {
      logger.error("(constructor): ", e);
      throw new LockssRepositoryException(e);
    }
  }

  /**
   * @param strDirectory -- the directory where the documents will be stored
   * @param filenameXml -- the XML file that holds the description of the database
   * @param stemFile Start of filename to store data
   * @param sizeWarcMax Maximum size of any WARC files
   * @param url Base of any URL
   * @param idman -- The identity manager of the parent process
   * @param lockssDaemon -- The Lockss daemon of the parent process
   * @throws LockssRepositoryException
   */
  static public void preconstructor(String strDirectory, String filenameXml, 
      String stemFile, long sizeWarcMax, String url, IdentityManager idman,
      LockssDaemon lockssDaemon, String strAuId)
  throws LockssRepositoryException {
    if (m_ljrsThis == null) {
      m_ljrsThis = new LockssJackrabbitHelper(strDirectory,
          stemFile, sizeWarcMax, url, idman, lockssDaemon, strAuId);
    } else {
      logger.debug2("Preconstructor should only be run once.");
    }
  }
  
  static public boolean isConstructed() {
    return m_ljrsThis != null;
  }
  
  static public LockssJackrabbitHelper constructor() 
  throws LockssRepositoryException {
    if (m_ljrsThis == null) {
      logger.error("Please call preconstructor before you call the constructor.");
      throw new LockssRepositoryException("constructor: Please call preconstructor before you call the constructor.");
    }
    
    return m_ljrsThis;
  }

  // Public methods (other than constructor)
  
  /**
   * This method sets the location for the AU texts.  It does not, currently, set
   * the location for the database. 
   * 
   * @param strAuId -- which AU Id are we handling?
   * @param directory -- the location for the AU
   * @throws LockssRepositoryException 
   * @throws FileNotFoundException 
   */
  static public void addRepository(String strAuId, String directory) throws FileNotFoundException, LockssRepositoryException {
    RepositoryNode repnode;
    
    repnode = RepositoryNodeImpl.constructor(m_ljrsThis.m_session, 
        m_ljrsThis.m_nodeRoot, directory, m_ljrsThis.m_sizeWarcMax, 
        m_ljrsThis.m_url, m_ljrsThis.m_idman);
    m_ljrsThis.m_marepnode.put(strAuId, repnode);
  }
  
  public IdentityManager getIdentityManager() {
    return m_idman;
  }
  
  static public RepositoryImpl getRepository() {
    return m_ljrsThis.m_repos;
  }
  
  static public RepositoryConfig getRepositoryConfig() {
    return m_ljrsThis.m_repconfig;
  }
  
  public RepositoryNode getRepositoryNode(String strAuId) {
    try {
      RepositoryNodeImpl rniRoot;
      
//      rniRoot = (RepositoryNodeImpl) RepositoryNodeImpl.constructor(m_session, m_nodeRoot, m_stemFile, 
//          m_sizeWarcMax, m_url, m_idman);
      if (m_marepnode.containsKey(strAuId)) {  
        rniRoot = (RepositoryNodeImpl) m_marepnode.get(strAuId);
        return rniRoot.getNode(strAuId, "", true);
      } else {
        logger.error("AuID was not available.  Please call 'addRepository' before you call this method.");
        logger.error("Tossing the exception into the bit bucket.");
        return null;
      }
    } catch (LockssRepositoryException e) {
      logger.error("getRepositoryNode: ", e);
      logger.error("Tossing the exception into the bit bucket.");
      return null;
    }
  }
  
  public Node getRootNode(String strAuId) {
    try {
      if (!m_nodeRoot.hasNode(strAuId)) {
        m_nodeRoot.addNode(strAuId);
        m_session.save();
        m_session.refresh(true);
      }

      return m_nodeRoot.getNode(strAuId);
    } catch (PathNotFoundException e) {
      logger.error("getRootNode: PathNotFound even AFTER checking for .hasNode.", e);
      return null;
    } catch (RepositoryException e) {
      // Something other than having to create the root node.
      logger.error("getRootNode: ", e);
      return null;
    } 
  }

  static public Session getSession() {
    return m_ljrsThis.m_session;
  }
  
  public long getSizeWarcMax() {
    return m_sizeWarcMax;
  }
  
  public String getStemFile() {
    return m_stemFile;
  }
  
  static public void moveRepository(String strAuId, String stemNew) {
    RepositoryNode rnMove;
    
    rnMove = m_ljrsThis.m_marepnode.get(strAuId);
    if (rnMove != null) {
      try {
        rnMove.move(stemNew);
      } catch (LockssRepositoryException e) {
        logger.error("moveRepository: ", e);
        logger.error("Throwing the exception into the bit-bucket.");
      }
    } else {  // rnMove == null
      logger.error("moveRepository: The requested repository does not exist.");
      logger.error("Throwing the exception into the bit-bucket.");
    }
  }
  
  /**
   * This method is used for testing.
   */
  
  static public void reset() {
    DataStore ds;
    
    if (m_ljrsThis != null) {
      if (m_ljrsThis.m_repos != null) {
        ds = m_ljrsThis.m_repos.getDataStore();
        if (ds != null) {
          try {
            ds.clearInUse();
            ds.close();
          } catch (DataStoreException e) {
            e.printStackTrace();
          }
        }
  
        m_ljrsThis.m_repos.shutdown();       
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
       
      m_ljrsThis = null;
    }
  }
  
  /**
   * This code is descended from ClockssParams.setConfig.
   * This method changes (and sets) the stem file and the WarcMax
   * value.
   * 
   * @param config
   * @param oldConfig
   * @param changedKeys
   */
  public void setConfig(Configuration config, Configuration oldConfig,
      Configuration.Differences changedKeys) {
      m_stemFile = config.get(k_STEM_FILE, m_stemFile);
      m_sizeWarcMax = config.getLong(k_SIZE_WARC_MAX,
                    m_sizeWarcMax);
  }

  
  public void startService() {
    super.startService();
  }
}
