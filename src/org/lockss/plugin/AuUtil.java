/*
 * $Id: AuUtil.java,v 1.3 2005-02-02 09:42:30 tlipkis Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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
import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;
import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.repository.*;

/**
 * Static AU- and plugin-related utility methods.  These might logically
 * belong in either Plugin or ArchivalUnit, but they are defined entirely
 * in terms of already-public methods, so need not be implemented by plugin
 * writers, thus there's no need to muddy those interfaces.
 */
public class AuUtil {

  public static LockssDaemon getDaemon(ArchivalUnit au) {
    return au.getPlugin().getDaemon();
  }

  /**
   * Return the size of the AU, calculating it if necessary.
   * @param au the AU
   * @return the AU's total content size.
   */
  public static long getAuContentSize(ArchivalUnit au) {
    LockssDaemon daemon = getDaemon(au);
    LockssRepository repo = daemon.getLockssRepository(au);
    try {
      RepositoryNode repoNode = repo.getNode(au.getAuCachedUrlSet().getUrl());
      return repoNode.getTreeContentSize(null);
    } catch (MalformedURLException ignore) {
      return -1;
    }
  }

  /**
   * Return the disk space used by the AU, including all overhead,
   * calculating it if necessary.
   * @param au the AU
   * @return the AU's disk usage in bytes.
   */
  public static long getAuDiskUsage(ArchivalUnit au) {
    LockssDaemon daemon = getDaemon(au);
    LockssRepository repo = daemon.getLockssRepository(au);
    try {
      RepositoryNode repoNode = repo.getNode(au.getAuCachedUrlSet().getUrl());
      if (repoNode instanceof AuNodeImpl) {
	return ((AuNodeImpl)repoNode).getDiskUsage();
      }
    } catch (MalformedURLException ignore) {
    }
    return -1;
  }

  /** Return true if the supplied AU config appears to be compatible with
   * the plugin.  Checks only that all required (definitional) parameters
   * have values. */
  public static boolean isConfigCompatibleWithPlugin(Configuration config,
						     Plugin plugin) {
    Set have = config.keySet();
    for (Iterator iter = plugin.getAuConfigDescrs().iterator();
	 iter.hasNext();) {
      ConfigParamDescr descr = (ConfigParamDescr)iter.next();
      String key = descr.getKey();
      String val = config.get(key);
      if (val == null) {
	if (descr.isDefinitional()) {
	  return false;
	}
      } else {
	if (!descr.isValidValueOfType(val)) {
	  return false;
	}
      }
    }
    return true;
  }

}
