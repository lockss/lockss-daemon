/*
 * $Id: RepositoryManagerManagerImpl.java,v 1.1.2.1 2009-09-30 23:02:32 edwardsb1 Exp $
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

import java.io.File;
import java.util.*;

import org.lockss.config.Configuration;
import org.lockss.config.Configuration.Differences;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.repository.v2.*;
import org.lockss.util.*;
import org.lockss.util.PlatformUtil.DF;

/**
 * @author edwardsb
 *
 */
public class RepositoryManagerManagerImpl implements RepositoryManagerManager {
  public RepositoryManagerManagerImpl() {
    
  }

  /* (non-Javadoc)
   * @see org.lockss.repository.v2.RepositoryManagerManager#doSizeCalc(org.lockss.repository.v2.RepositoryNode)
   */
  public void doSizeCalc(RepositoryNode node) {
  }

  /* (non-Javadoc)
   * @see org.lockss.repository.v2.RepositoryManagerManager#findLeastFullRepository()
   */
  public String findLeastFullRepository() {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see org.lockss.repository.v2.RepositoryManagerManager#getAuRepository(java.lang.String, org.lockss.plugin.ArchivalUnit)
   */
  public LockssAuRepository getAuRepository(String coarSpec, ArchivalUnit au) {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see org.lockss.repository.v2.RepositoryManagerManager#getDiskFullThreshold()
   */
  public DF getDiskFullThreshold() {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see org.lockss.repository.v2.RepositoryManagerManager#getDiskWarnThreshold()
   */
  public DF getDiskWarnThreshold() {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see org.lockss.repository.v2.RepositoryManagerManager#getExistingCoarSpecsForAuid(java.lang.String)
   */
  public List<String> getExistingCoarSpecsForAuid(String auid) {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see org.lockss.repository.v2.RepositoryManagerManager#getGlobalNodeCache()
   */
  public UniqueRefLruCache getGlobalNodeCache() {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see org.lockss.repository.v2.RepositoryManagerManager#getRepository(java.lang.String, org.lockss.plugin.ArchivalUnit)
   */
  public LockssAuRepository getRepository(String nameRepository, ArchivalUnit au) {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see org.lockss.repository.v2.RepositoryManagerManager#getRepositoryDf()
   */
  public DF getRepositoryDf() {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see org.lockss.repository.v2.RepositoryManagerManager#getRepositoryList()
   */
  public List<String> getRepositoryList() {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see org.lockss.repository.v2.RepositoryManagerManager#getRepositoryMap()
   */
  public Map<String, DF> getRepositoryMap() {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see org.lockss.repository.v2.RepositoryManagerManager#queueSizeCale(org.lockss.plugin.ArchivalUnit, org.lockss.repository.v2.RepositoryNode)
   */
  public void queueSizeCale(ArchivalUnit au, RepositoryNode node) {
    // TODO Auto-generated method stub

  }

  /* (non-Javadoc)
   * @see org.lockss.app.ConfigurableManager#setConfig(org.lockss.config.Configuration, org.lockss.config.Configuration, org.lockss.config.Configuration.Differences)
   */
  public void setConfig(Configuration newConfig, Configuration prevConfig,
      Differences changedKeys) {
    // TODO Auto-generated method stub

  }

}
