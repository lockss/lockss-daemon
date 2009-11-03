/*
 * $Id: RepositoryManagerManagerImpl.java,v 1.1.2.4 2009-11-03 23:44:52 edwardsb1 Exp $
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
import java.net.*;
import java.util.*;

import org.apache.commons.collections.*;

import org.lockss.app.*;
import org.lockss.config.Configuration;
import org.lockss.config.Configuration.Differences;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.repository.*;
import org.lockss.repository.v2.*;
import org.lockss.repository.v2.RepositoryNode;
import org.lockss.util.*;
import org.lockss.util.PlatformUtil.*;

/**
 * @author edwardsb
 *
 */
public class RepositoryManagerManagerImpl extends BaseLockssDaemonManager 
implements RepositoryManagerManager {
  // Constants
  private static final String k_schemeJcr = "jcr";

  // Constants taken from RepositoryManager
  public static final String PREFIX = Configuration.PREFIX + "repository.";
  static final String DISK_PREFIX = PREFIX + "diskSpace.";
  static final String PARAM_DISK_WARN_FREE_MB = DISK_PREFIX + "warn.freeMB";
  static final int DEFAULT_DISK_WARN_FREE_MB = 5000;
  static final String PARAM_DISK_FULL_FREE_MB = DISK_PREFIX + "full.freeMB";
  static final int DEFAULT_DISK_FULL_FREE_MB = 100;
  static final String PARAM_DISK_WARN_FREE_PERCENT =
    DISK_PREFIX + "warn.freePercent";
  static final double DEFAULT_DISK_WARN_FREE_PERCENT = .02;
  static final String PARAM_DISK_FULL_FREE_PERCENT =
    DISK_PREFIX + "full.freePercent";
  static final double DEFAULT_DISK_FULL_FREE_PERCENT = .01;

  static final String GLOBAL_CACHE_PREFIX = PREFIX + "globalNodeCache.";
  public static final String PARAM_MAX_GLOBAL_CACHE_SIZE =
    GLOBAL_CACHE_PREFIX + "size";
  public static final int DEFAULT_MAX_GLOBAL_CACHE_SIZE = 500;

  public static final String PARAM_GLOBAL_CACHE_ENABLED =
    GLOBAL_CACHE_PREFIX + "enabled";
  public static final boolean DEFAULT_GLOBAL_CACHE_ENABLED = false;

  
  // Static variables
  private static Logger logger = Logger.getLogger("JcrRepositoryHelperFactory");
  
  // Class Variables
  private UniqueRefLruCache m_globalNodeCache = new UniqueRefLruCache(DEFAULT_MAX_GLOBAL_CACHE_SIZE);;
  private MultiMap m_mmapAuidToCoarSpec;
  private Map<String, CollectionOfAuRepositories> m_mapCoarSpecToCoar;
  int m_paramGlobalNodeCacheSize = DEFAULT_MAX_GLOBAL_CACHE_SIZE;
  private boolean m_paramIsGlobalNodeCache = DEFAULT_GLOBAL_CACHE_ENABLED;


  PlatformUtil.DF paramDFWarn =
    PlatformUtil.DF.makeThreshold(DEFAULT_DISK_WARN_FREE_MB,
                                  DEFAULT_DISK_WARN_FREE_PERCENT);
  PlatformUtil.DF paramDFFull =
    PlatformUtil.DF.makeThreshold(DEFAULT_DISK_FULL_FREE_MB,
                                  DEFAULT_DISK_FULL_FREE_PERCENT);

  

  public RepositoryManagerManagerImpl() {
    // Empty for now.
  }

  // Initialization code ---
  
  /**
   * Called at the start of the RepositoryManagerManagerImpl.
   */
  public void startService() throws LockssAppException {
    super.startService();
    
    m_mapCoarSpecToCoar = new HashMap<String, CollectionOfAuRepositories>();
    
    // TODO: initialize m_mapCoarSpecToCoar.
  }

  // Routines.
  
  /**
   * This version only works with JCR CollectionOfAuRepositories.  It will
   * need to be expanded to work with Unix CollectionOfAuRepositories.
   * 
   * @param AUID  The name of the AUID
   * @param RepositorySpec  The specification for the files of the repository.
   * @see org.lockss.repository.v2.RepositoryManagerManager#addAuidToCoar(java.lang.String, java.lang.String)
   * @throws LockssRepositoryException
   * @throws URISyntaxException
   * @throws IOException
   */
  public void addAuidToCoar(String AUID, String coarSpec) 
  throws LockssRepositoryException, URISyntaxException, IOException {
    File path;
    String strPath;
    String strScheme;
    URI uriRepositorySpec;
    
    // The RepositorySpec is a URI.  
    uriRepositorySpec = new URI(coarSpec);
    strScheme = uriRepositorySpec.getScheme();
    
    if (strScheme.equalsIgnoreCase(k_schemeJcr)) {
      if (m_mmapAuidToCoarSpec.containsKey(AUID)) {
        logger.info("AUID " + AUID + " already exists in the map of AUID to CoarSpec.  Aren't you glad that it's a multimap?");
      }
      
      m_mmapAuidToCoarSpec.put(AUID, coarSpec);      
    } else {
      logger.error("Unknown scheme.  Please use jcr: as your scheme.");
      throw new LockssRepositoryException("Unknown scheme.");
    }
  }

  /**
   * @see org.lockss.repository.v2.RepositoryManagerManager#doSizeCalc(org.lockss.repository.v2.RepositoryNode)
   */
  public void doSizeCalc(RepositoryNode node) {
    try {
      node.getTreeContentSize(null, true);
      if (node instanceof AuNodeImpl) {
        ((AuNodeImpl)node).getDiskUsage(true);
      }
    } catch (LockssRepositoryException e) {
      logger.debug("doSizeCalc: LockssRepositoryException: ", e);
    }
  }

  
  /* (non-Javadoc)
   * @see org.lockss.repository.v2.RepositoryManagerManager#findLeastFullRepository()
   */
  public String /* coarSpec */ findLeastFullCoar() {
    String coarSpecLeastFull = null;
    DF dfBest;
    DF dfCurrent;
    
    dfBest = null;
    for (String coarSpecCurrent : m_mapCoarSpecToCoar.keySet()) {
      try {
        dfCurrent = m_mapCoarSpecToCoar.get(coarSpecCurrent).getDF();
      
        if (dfBest == null || dfBest.isFullerThan(dfCurrent)) {
          coarSpecLeastFull = coarSpecCurrent;
          dfBest = dfCurrent;
        }
      } catch (UnsupportedException e) {
        logger.error("Unsupported Exception ", e);
        return null;
      }
    } // end for coarSpecCurrent...
    
    return coarSpecLeastFull;
  }

  /* (non-Javadoc)
   * @see org.lockss.repository.v2.RepositoryManagerManager#getAuRepository(java.lang.String, org.lockss.plugin.ArchivalUnit)
   */
  public LockssAuRepository getAuRepository(String coarSpec, ArchivalUnit au) throws LockssRepositoryException {
    CollectionOfAuRepositoriesImpl coar;
    
    coar = (CollectionOfAuRepositoriesImpl) m_mapCoarSpecToCoar.get(coarSpec);
    if (coar == null) {
      return null;
    }
    
    try {
      return coar.openAuRepository(au);
    } catch (FileNotFoundException e) {
      logger.error("File not found exception: ", e);
      return null;
    }
  }

  /**
   * @see org.lockss.repository.v2.RepositoryManagerManager#getDiskFullThreshold()
   * @see org.lockss.repository.RepositoryManager.getDiskFullThreshold()
   */
  public DF getDiskFullThreshold() {
    return paramDFFull;
  }

  /* (non-Javadoc)
   * @see org.lockss.repository.v2.RepositoryManagerManager#getDiskWarnThreshold()
   * @see org.lockss.repository.RepositoryManager.getDiskFullThreshold()
   */
  public DF getDiskWarnThreshold() {
    return paramDFWarn;
  }

  /**
   * I assume that this class wants to know all COARs that contain a particular AUID.
   * 
   * @see org.lockss.repository.v2.RepositoryManagerManager#getExistingCoarSpecsForAuid(java.lang.String)
   */
  public List<String> getExistingCoarSpecsForAuid(String auid) {
    Collection<String> collstrCoarSpecs;
    List<String> listrCoarSpecs;
    
    collstrCoarSpecs = (Collection<String>) m_mmapAuidToCoarSpec.get(auid);
    
    listrCoarSpecs = new ArrayList<String>();
    listrCoarSpecs.addAll(collstrCoarSpecs);
    
    return listrCoarSpecs;
  }

  
  /* (non-Javadoc)
   * @see org.lockss.repository.v2.RepositoryManagerManager#getGlobalNodeCache()
   */
  public UniqueRefLruCache getGlobalNodeCache() {
    return m_globalNodeCache;
  }

   /* (non-Javadoc)
   * @see org.lockss.repository.v2.RepositoryManagerManager#getRepositoryList()
   */
  public List<String> getRepositoryList() {
    List<String> listrCoarSpec;
    Set<String> sestrCoarSpec;

    sestrCoarSpec = m_mapCoarSpecToCoar.keySet();
    
    listrCoarSpec = new ArrayList<String>();
    listrCoarSpec.addAll(sestrCoarSpec);
    
    return listrCoarSpec;
  }

  /* (non-Javadoc)
   * @see org.lockss.repository.v2.RepositoryManagerManager#getRepositoryMap()
   */
  public Map<String /* CoarSpec */, DF> getRepositoryMap() throws LockssRepositoryException {
    CollectionOfAuRepositories coar;
    DF df;
    Map<String /* CoarSpec */, DF> mapCoarSpecDF;
    
    mapCoarSpecDF = new HashMap<String, DF>();
    
    try {
      for (String strCoarSpec : m_mapCoarSpecToCoar.keySet()) {
        coar = m_mapCoarSpecToCoar.get(strCoarSpec);  
        df = coar.getDF();
        
        mapCoarSpecDF.put(strCoarSpec, df);
      }
      
      return mapCoarSpecDF;
    } catch (PlatformUtil.UnsupportedException e) {
      throw new LockssRepositoryException(e);
    }
  }

  /* (non-Javadoc)
   * @see org.lockss.repository.v2.RepositoryManagerManager#queueSizeCale(org.lockss.plugin.ArchivalUnit, org.lockss.repository.v2.RepositoryNode)
   */
  public void queueSizeCalc(ArchivalUnit au, RepositoryNode node) {
    LockssAuRepository lar;
    
    try {
      lar = getAuRepository(node.getNodeUrl(), au);
      lar.queueSizeCalc(node);
    } catch (LockssRepositoryException e) {
      logger.error("queueSizeCalc caused a Lockss Repository Exception.  Dropping into the bit bucket.", e);      
    }
  }

  /* (non-Javadoc)
   * @see org.lockss.app.ConfigurableManager#setConfig(org.lockss.config.Configuration, org.lockss.config.Configuration, org.lockss.config.Configuration.Differences)
   */
  public void setConfig(Configuration newConfig, Configuration prevConfig,
      Differences changedKeys) {
    
    // Taken from RepositoryManager.
    if (changedKeys.contains(GLOBAL_CACHE_PREFIX)) {
      m_paramIsGlobalNodeCache = newConfig.getBoolean(PARAM_GLOBAL_CACHE_ENABLED,
                                                 DEFAULT_GLOBAL_CACHE_ENABLED);
      if (m_paramIsGlobalNodeCache) {
        m_paramGlobalNodeCacheSize = newConfig.getInt(PARAM_MAX_GLOBAL_CACHE_SIZE,
                                                 DEFAULT_MAX_GLOBAL_CACHE_SIZE);
        logger.debug("global node cache size: " + m_paramGlobalNodeCacheSize);
        m_globalNodeCache.setMaxSize(m_paramGlobalNodeCacheSize);
      }
    }
  }
}
