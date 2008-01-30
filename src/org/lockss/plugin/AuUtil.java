/*
 * $Id: AuUtil.java,v 1.22 2008-01-30 00:48:43 tlipkis Exp $
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin;

import java.net.*;
import java.util.*;

import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.state.*;
import org.lockss.poller.*;
import org.lockss.repository.*;
import org.lockss.plugin.definable.*;
import org.lockss.plugin.exploded.*;

/**
 * Static AU- and plugin-related utility methods.  These might logically
 * belong in either Plugin or ArchivalUnit, but they are defined entirely
 * in terms of already-public methods, so need not be implemented by plugin
 * writers, thus there's no need to muddy those interfaces.
 */
public class AuUtil {
  private static Logger log = Logger.getLogger("AuUtil");
  
  /** The default poll protocol to use, unless otherwise overridden by the
   * Archival Unit's poll_protocol config param.=
   */
  public static final String PARAM_POLL_PROTOCOL_VERSION =
    Configuration.PREFIX + "poll.defaultPollProtocol";
  private static final int DEFAULT_POLL_PROTOCOL_VERSION =
    Poll.V3_PROTOCOL;

  public static LockssDaemon getDaemon(ArchivalUnit au) {
    return au.getPlugin().getDaemon();
  }

  /**
   * Return the AuState object for the AU
   * @param au the AU
   * @return the AuState
   */
  public static AuState getAuState(ArchivalUnit au) {
    NodeManager nodeManager = getDaemon(au).getNodeManager(au);
    return nodeManager.getAuState();
  }

  public static AuNodeImpl getAuRepoNode(ArchivalUnit au) {
    LockssDaemon daemon = getDaemon(au);
    LockssRepository repo = daemon.getLockssRepository(au);
    try {
      return(AuNodeImpl)repo.getNode(au.getAuCachedUrlSet().getUrl());
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * @param au An ArchivalUnit
   * @param url A URL
   * @return The RepositoryNode representing the URL in the given AU.
   * 
   * @throws MalformedURLException if the URL cannot be parsed.
   */
  public static RepositoryNode getRepositoryNode(ArchivalUnit au, String url) 
      throws MalformedURLException {
    LockssDaemon daemon = getDaemon(au);
    LockssRepository repo = daemon.getLockssRepository(au);
    return repo.getNode(url);
  }

  /**
   * Return the size of the AU, calculating it if necessary.
   * @param au the AU
   * @return the AU's total content size.
   */
  public static long getAuContentSize(ArchivalUnit au,
				      boolean calcIfUnknown) {
    LockssDaemon daemon = getDaemon(au);
    RepositoryNode repoNode = getAuRepoNode(au);
    return repoNode.getTreeContentSize(null, calcIfUnknown);
  }

  /**
   * Return the disk space used by the AU, including all overhead,
   * calculating it if necessary.
   * @param au the AU
   * @param calcIfUnknown if true, disk usage will calculated if unknown
   * (time consumeing)
   * @return the AU's disk usage in bytes.
   */
  public static long getAuDiskUsage(ArchivalUnit au, boolean calcIfUnknown) {
    LockssDaemon daemon = getDaemon(au);
    AuNodeImpl repoNode = getAuRepoNode(au);
    return repoNode.getDiskUsage(calcIfUnknown);
  }

  public static String getConfigUserMessage(ArchivalUnit au) {
    // XXX change this to not require string to be copied into each AU
    TypedEntryMap map = au.getProperties();
    String str =
      map.getString(DefinableArchivalUnit.KEY_AU_CONFIG_USER_MSG, null);
    if (str == null) {
      return null;
    }
    return str;
  }

  /** Return true if the supplied AU config appears to be compatible with
   * the plugin.  Checks only that all required (definitional) parameters
   * have values. */
  public static boolean isConfigCompatibleWithPlugin(Configuration config,
						     Plugin plugin) {
    for (Iterator iter = plugin.getAuConfigDescrs().iterator();
	 iter.hasNext();) {
      ConfigParamDescr descr = (ConfigParamDescr)iter.next();
      String key = descr.getKey();
      String val = config.get(key);
      if (val == null) {
	if (descr.isDefinitional()) {
	  log.debug(descr + " is definitional, absent from " + config);
	  return false;
	}
      } else {
	if (!descr.isValidValueOfType(val)) {
	  log.debug(val + " is not a valid value of type " + descr);
	  return false;
	}
      }
    }
    return true;
  }

  /** Search titles belonging to <i>plugin</i> in the title DB for one that
   * matches the config.
   * @param config an AU config (unqualified)
   * @param plugin a plugin
   * @return the matching TitleConfig, or null if none found
   */
  // Unit test for this is in TestBaseArchivalUnit
  public static TitleConfig findTitleConfig(Configuration config,
					    Plugin plugin) {
    if (plugin.getSupportedTitles() == null)  {
      return null;
    }
    for (Iterator iter = plugin.getSupportedTitles().iterator();
	 iter.hasNext(); ) {
      String title = (String)iter.next();
      TitleConfig tc = plugin.getTitleConfig(title);
      if (tc != null && tc.matchesConfig(config) && tc.isSingleAu(plugin)) {
	return tc;
      }
    }
    return null;
  }

  public static boolean isClosed(ArchivalUnit au) {
    return getBoolValue(getAuParamOrTitleDefault(au,
						 ConfigParamDescr.AU_CLOSED),
			false);
  }

  public static boolean isPubDown(ArchivalUnit au) {
    return isPubNever(au) ||
      getBoolValue(getAuParamOrTitleDefault(au, ConfigParamDescr.PUB_DOWN),
		   false);
  }

  public static boolean okDeleteExtraFiles(ArchivalUnit au) {
    return !(au instanceof ExplodedArchivalUnit);
  }

  public static boolean isPubNever(ArchivalUnit au) {
    return getBoolValue(getAuParamOrTitleDefault(au,
						 ConfigParamDescr.PUB_NEVER),
			false);
  }

  public static boolean isPubDown(TitleConfig tc) {
    return isPubNever(tc) ||
      getBoolValue(getTitleDefault(tc, ConfigParamDescr.PUB_DOWN),
		   false);
  }

  public static boolean isPubNever(TitleConfig tc) {
    return getBoolValue(getTitleDefault(tc, ConfigParamDescr.PUB_NEVER),
			false);
  }

  public static int getProtocolVersion(ArchivalUnit au) {
    return getIntValue(getAuParamOrTitleDefault(au, ConfigParamDescr.PROTOCOL_VERSION),
                       CurrentConfig.getIntParam(PARAM_POLL_PROTOCOL_VERSION,
                                                 DEFAULT_POLL_PROTOCOL_VERSION));
  }

  /** Return an attribute value from the AU's title DB entry, if any */
  public static String getTitleAttribute(ArchivalUnit au, String key) {
    TitleConfig tc = au.getTitleConfig();
    if (tc != null) {
      Map attrs = tc.getAttributes();
      if (attrs != null) {
	return (String)attrs.get(key);
      }
    }
    return null;
  }

  /** Return an attribute value from the AU's title DB entry, if any */
  public static String getTitleAttribute(ArchivalUnit au, String key,
					 String dfault) {
    String res = getTitleAttribute(au, key);
    return (res != null) ? res : dfault;
  }

  public static boolean getBoolValue(Object value, boolean dfault) {
    if (value instanceof Boolean) {
      return ((Boolean)value).booleanValue();
    }
    return dfault;
  }

  public static int getIntValue(Object value, int dfault) {
    if (value instanceof Integer) {
      return ((Integer)value).intValue();
    }
    return dfault;
  }

  public static Object getAuParamOrTitleDefault(ArchivalUnit au,
						ConfigParamDescr cpd) {
    String key = cpd.getKey();
    String val = null;
    Configuration auConfig = au.getConfiguration();
    if (auConfig == null) {
      return null;
    }
    val = auConfig.get(key);
    if (!StringUtil.isNullString(val)) {
      return getValueOfType(val, cpd);
    }
    TitleConfig tc = au.getTitleConfig();
    if (tc != null) {
      return getTitleDefault(tc, cpd);
    }
    return null;
  }

  public static Object getValueOfType(String valstr, ConfigParamDescr cpd) {
    if (valstr == null) {
      return null;
    }
    try {
      return cpd.getValueOfType(valstr);
    } catch (ConfigParamDescr.InvalidFormatException e) {
      return null;
    }
  }

  public static Object getTitleDefault(TitleConfig tc, ConfigParamDescr cpd) {
    ConfigParamAssignment cpa = tc.findCpa(cpd);
    if (cpa != null) {
      return getValueOfType(cpa.getValue(), cpd);
    }
    return null;
  }

  /** Call release() on the CachedUrl, ignoring any errors */
  public static void safeRelease(CachedUrl cu) {
    try {
      cu.release();
    } catch (Exception e) {}
  }
}
