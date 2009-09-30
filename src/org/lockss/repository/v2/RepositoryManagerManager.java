/*
 * $Id: RepositoryManagerManager.java,v 1.1.2.3 2009-09-30 23:02:32 edwardsb1 Exp $
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
package org.lockss.repository.v2;

import java.util.*;

import org.lockss.app.ConfigurableManager;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.UniqueRefLruCache;
import org.lockss.util.PlatformUtil.DF;

/**
 * @author edwardsb
 *
 */
public interface RepositoryManagerManager extends ConfigurableManager {
  public List<String /* CoarSpec */> getRepositoryList();
  public DF getRepositoryDf();
  public Map<String, DF> getRepositoryMap();
  public String /* CoarSpec */ findLeastFullRepository();
  public LockssAuRepository getRepository(String nameRepository, ArchivalUnit au);
  public List<String /* CoarSpec */> getExistingCoarSpecsForAuid(String auid);
  public LockssAuRepository getAuRepository(String coarSpec, ArchivalUnit au);
  public DF getDiskWarnThreshold();
  public DF getDiskFullThreshold();
  public UniqueRefLruCache getGlobalNodeCache();  // Just used by the old repository.
  public void queueSizeCale(ArchivalUnit au, RepositoryNode node);
  public void doSizeCalc(RepositoryNode node);
}
