/**
 * 
 */
package org.lockss.repository;

import java.io.*;
import java.util.*;

import org.lockss.util.Logger;

/**
 * @author edwardsb
 *
 */
public class JcrRepositoryManager implements LockssRepositoryManager {
  // Constants
  private final String k_filenameRepo = "jcrcache";
  
  // Class variables
  private static Logger logger = Logger.getLogger("UnixRepositoryManager");
  
  // Member variables
  private File m_fileRepoCache;
  private Map<String, String> m_mapAu;
  private Map<String, String> m_mapNodeName;
  private String m_pathRepo;
  
  public JcrRepositoryManager(String pathRepo) {
    m_pathRepo = pathRepo;
    m_fileRepoCache = new File(m_pathRepo, k_filenameRepo); 
  }

  /* (non-Javadoc)
   * @see org.lockss.repository.LockssRepositoryManager#getAuMap()
   */

  public Map<String, String> getAuMap() {
    if (m_mapAu == null) {
      loadJcrCache();
    }
    return m_mapAu;
  }

  /* (non-Javadoc)
   * @see org.lockss.repository.LockssRepositoryManager#getRepositoryPath()
   */
 
  public String getRepositoryPath() {
    return m_pathRepo;
  }

  /* (non-Javadoc)
   * @see org.lockss.repository.LockssRepositoryManager#resetMap()
   */
 
  public void resetMap() {
    m_mapAu = null;
  }

  private void loadJcrCache()
  {
    logger.debug3("Loading name map for '" + m_fileRepoCache.getPath() + "'.");
    m_mapAu = new HashMap<String, String>();
    if (!m_fileRepoCache.exists()) {
      logger.debug3("Creating cache dir:" + m_fileRepoCache.getPath() + "'.");
      if (!m_fileRepoCache.mkdirs()) {
        logger.critical("Couldn't create directory, check owner/permissions: "
                        + m_fileRepoCache);
        
        m_mapAu = null;
        return;
      }
    } else {
      // read each dir's property file and store mapping auid -> dir
      File[] auDirs = m_fileRepoCache.listFiles();
      for (int ii = 0; ii < auDirs.length; ii++) {
        String path = auDirs[ii].getAbsolutePath();
        Properties idProps = getAuIdProperties(path);
        if (idProps != null) {
          String auid = idProps.getProperty(LockssRepositoryImpl.AU_ID_PROP);
          StringBuffer sb = new StringBuffer(path.length() +
                                             File.separator.length());
          sb.append(path);
          sb.append(File.separator);
          m_mapAu.put(auid, sb.toString());
          logger.debug3("Mapping to: " + m_mapAu.get(auid) + ": " + auid);
        } else {
          logger.debug3("Not mapping " + path + ", no auid file.");
        }
      }
    }
  }
  
  // Taken from LockssRepositoryImpl.
  Properties getAuIdProperties(String location) {
    File propFile = new File(location + File.separator + LockssRepositoryImpl.AU_ID_FILE);
    try {
      InputStream is = new BufferedInputStream(new FileInputStream(propFile));
      Properties idProps = new Properties();
      idProps.load(is);
      is.close();
      return idProps;
    } catch (Exception e) {
      logger.warning("Error loading au id from " + propFile.getPath() + ".");
      return null;
    }
  }
}
