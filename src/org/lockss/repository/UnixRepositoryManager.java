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
public class UnixRepositoryManager implements LockssRepositoryManager {
  // Class variables...
  private static Logger logger = Logger.getLogger("LockssRepository");
  
  // Member variables...
  String m_repoPath;
  File m_repoCacheFile;
  Map<String, String> m_auMap;

  UnixRepositoryManager(String m_repoPath) {
    this.m_repoPath = m_repoPath;
    m_repoCacheFile = new File(m_repoPath, LockssRepositoryImpl.CACHE_ROOT_NAME);
  }

  public String getRepositoryPath() {
    return m_repoPath;
  }
  
  public void resetMap() {
    m_auMap = null;
  }

  /** Return the auid -> au-subdir-path mapping.  Enumerating the
   * directories if necessary to initialize the map */
  public Map<String, String> getAuMap() {
    if (m_auMap == null) {
      logger.debug3("Loading name map for '" + m_repoCacheFile + "'.");
      m_auMap = new HashMap<String, String>();
      if (!m_repoCacheFile.exists()) {
        logger.debug3("Creating cache dir:" + m_repoCacheFile + "'.");
        if (!m_repoCacheFile.mkdirs()) {
          logger.critical("Couldn't create directory, check owner/permissions: "
                          + m_repoCacheFile);
          // return empty map
          return m_auMap;
        }
      } else {
        // read each dir's property file and store mapping auid -> dir
        File[] auDirs = m_repoCacheFile.listFiles();
        for (int ii = 0; ii < auDirs.length; ii++) {
          // String dirName = auDirs[ii].getName();
          //       if (dirName.compareTo(lastPluginDir) == 1) {
          //         // adjust the 'lastPluginDir' upwards if necessary
          //         lastPluginDir = dirName;
          //       }

          String path = auDirs[ii].getAbsolutePath();
          Properties idProps = getAuIdProperties(path);
          if (idProps != null) {
            String auid = idProps.getProperty(LockssRepositoryImpl.AU_ID_PROP);
            StringBuffer sb = new StringBuffer(path.length() +
                                               File.separator.length());
            sb.append(path);
            sb.append(File.separator);
            m_auMap.put(auid, sb.toString());
            logger.debug3("Mapping to: " + m_auMap.get(auid) + ": " + auid);
          } else {
            logger.debug3("Not mapping " + path + ", no auid file.");
          }
        }

      }
    }
    return m_auMap;
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
