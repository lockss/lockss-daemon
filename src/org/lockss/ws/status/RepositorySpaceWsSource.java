/*
 * $Id$
 */

/*

 Copyright (c) 2014 Board of Trustees of Leland Stanford Jr. University,
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

/**
 * Container for the information that is used as the source for a query related
 * to repository spaces.
 */
package org.lockss.ws.status;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeSet;
import org.lockss.app.LockssDaemon;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.config.CurrentConfig;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.Plugin;
import org.lockss.plugin.PluginManager;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.util.Logger;
import org.lockss.util.PlatformUtil;
import org.lockss.util.PropUtil;
import org.lockss.util.StringUtil;
import org.lockss.ws.entities.RepositorySpaceWsResult;

public class RepositorySpaceWsSource extends RepositorySpaceWsResult {
  private static Logger log = Logger.getLogger(RepositorySpaceWsSource.class);

  private PlatformUtil.DF puDf;

  private boolean sizePopulated;
  private boolean usedPopulated;
  private boolean freePopulated;
  private boolean percentageFullPopulated;
  private boolean activeCountPopulated;
  private boolean inactiveCountPopulated;
  private boolean deletedCountPopulated;
  private boolean orphanedCountPopulated;

  private int allActiveCount = -1;
  private int allInactiveCount = -1;
  private int allDeletedCount = -1;
  private int allOrphanedCount = -1;

  public RepositorySpaceWsSource(String repositorySpaceId, PlatformUtil.DF puDf)
  {
    setRepositorySpaceId(repositorySpaceId);
    this.puDf = puDf;
  }

  @Override
  public String getRepositorySpaceId() {
    return super.getRepositorySpaceId();
  }

  @Override
  public Long getSize() {
    if (!sizePopulated) {
      setSize(Long.valueOf(puDf.getSize()));
      sizePopulated = true;
    }

    return super.getSize();
  }

  @Override
  public Long getUsed() {
    if (!usedPopulated) {
      setUsed(Long.valueOf(puDf.getUsed()));
      usedPopulated = true;
    }

    return super.getUsed();
  }

  @Override
  public Long getFree() {
    if (!freePopulated) {
      setFree(Long.valueOf(puDf.getAvail()));
      freePopulated = true;
    }

    return super.getFree();
  }

  @Override
  public Double getPercentageFull() {
    if (!percentageFullPopulated) {
      setPercentageFull(Double.valueOf(puDf.getPercent()));
      percentageFullPopulated = true;
    }

    return super.getPercentageFull();
  }

  @Override
  public Integer getActiveCount() {
    if (!activeCountPopulated) {
      if (allActiveCount < 0) {
	populateCounts();
      }

      setActiveCount(Integer.valueOf(allActiveCount));
      activeCountPopulated = true;
    }

    return super.getActiveCount();
  }

  @Override
  public Integer getInactiveCount() {
    if (!inactiveCountPopulated) {
      if (allInactiveCount < 0) {
	populateCounts();
      }

      setInactiveCount(Integer.valueOf(allInactiveCount));
      inactiveCountPopulated = true;
    }

    return super.getInactiveCount();
  }

  @Override
  public Integer getDeletedCount() {
    if (!deletedCountPopulated) {
      if (allDeletedCount < 0) {
	populateCounts();
      }

      setDeletedCount(Integer.valueOf(allDeletedCount));
      deletedCountPopulated = true;
    }

    return super.getDeletedCount();
  }

  @Override
  public Integer getOrphanedCount() {
    if (!orphanedCountPopulated) {
      if (allOrphanedCount < 0) {
	populateCounts();
      }

      setOrphanedCount(Integer.valueOf(allOrphanedCount));
      orphanedCountPopulated = true;
    }

    return super.getOrphanedCount();
  }

  private void populateCounts() {
    TreeSet<String> roots = new TreeSet<String>();
    Collection<String> specs = StringUtil.breakAt(getRepositorySpaceId(), ";");

    for (String repoSpec : specs) {
      String path = LockssRepositoryImpl.getLocalRepositoryPath(repoSpec);

      if (path != null) {
	roots.add(path);
      }
    }

    allActiveCount = 0;
    allInactiveCount = 0;
    allDeletedCount = 0;
    allOrphanedCount = 0;

    for (Iterator<String> iter = roots.iterator(); iter.hasNext(); ) {
      String root = iter.next();
	
      StringBuilder buffer = new StringBuilder(root);

      if (!root.endsWith(File.separator)) {
	buffer.append(File.separator);
      }

      buffer.append(LockssRepositoryImpl.CACHE_ROOT_NAME);
      buffer.append(File.separator);
      String extendedCacheLocation = buffer.toString();

      File dir = new File(extendedCacheLocation);
      File[] subs = dir.listFiles();

      if (subs != null) {
	for (int ix = 0; ix < subs.length; ix++) {
	  File sub = subs[ix];

	  if (sub.isDirectory()) {
	    String auid = null;
	    File auidfile = new File(sub, LockssRepositoryImpl.AU_ID_FILE);

	    if (auidfile.exists()) {
	      Properties props = propsFromFile(auidfile);

	      if (props != null) {
		auid = props.getProperty("au.id");
	      }
	    }

	    if (auid != null) {
	      PluginManager pluginMgr = (PluginManager)LockssDaemon
		  .getManager(LockssDaemon.PLUGIN_MANAGER);
	      ArchivalUnit au = pluginMgr.getAuFromId(auid);

	      if (au != null) {
		String repoSpec = au.getConfiguration()
		    .get(PluginManager.AU_PARAM_REPOSITORY);

		String repoRoot = (repoSpec == null) ? CurrentConfig
		    .getParam(LockssRepositoryImpl.PARAM_CACHE_LOCATION)
		    : LockssRepositoryImpl.getLocalRepositoryPath(repoSpec);

		if (!LockssRepositoryImpl
		    .isDirInRepository(extendedCacheLocation, repoRoot)) {
		  au = null;
		}
	      }

	      if (au != null) {
		allActiveCount++;
	      } else {
		String auKey = PluginManager.auKeyFromAuId(auid);
		Properties auidProps = null;

		try {
		  auidProps = PropUtil.canonicalEncodedStringToProps(auKey);
		} catch (Exception e) {
		  log.warning("Couldn't decode AUKey in " + sub + ": " + auKey,
		      e);
		}
		  
		boolean isOrphaned = true;
		String pluginKey = PluginManager
		    .pluginKeyFromId(PluginManager.pluginIdFromAuId(auid));
		Plugin plugin = pluginMgr.getPlugin(pluginKey);

		if (plugin != null && auidProps != null) {
		  Configuration defConfig =
		      ConfigManager.fromProperties(auidProps);
		  isOrphaned =
		      !AuUtil.isConfigCompatibleWithPlugin(defConfig, plugin);
		}

		if (isOrphaned) {
		  allOrphanedCount++;
		} else {
		  Configuration config =
		      pluginMgr.getStoredAuConfiguration(auid);
		      
		  if (config == null || config.isEmpty()) {
		    allDeletedCount++;
		  } else {
		    boolean isInactive = config
			.getBoolean(PluginManager.AU_PARAM_DISABLED, false);
			
		    if (isInactive) {
		      allInactiveCount++;
		    } else {
		      allDeletedCount++;
		    }	  
		  }
		}
	      }
	    }
	  }
	}
      }
    }
  }

  private Properties propsFromFile(File file) {
    try {
	InputStream is = new FileInputStream(file);
	Properties props = new Properties();
	props.load(is);
	is.close();
	return props;
    } catch (IOException e) {
	log.warning("Error loading au id from " + file);
	return null;
    }
  }
}
