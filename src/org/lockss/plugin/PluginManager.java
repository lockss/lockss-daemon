/*
 * $Id: PluginManager.java,v 1.4 2003-02-06 05:16:06 claire Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.*;
import org.lockss.daemon.*;
import org.lockss.app.*;

/**
 * Plugin global functionality
 *
 * @author  TAL
 * @version 0.0
 */
public class PluginManager implements LockssManager {
  private static PluginManager theManager = null;
  private static LockssDaemon theDaemon = null;

  private static Vector archivalUnits = new Vector();


  /* ------- LockssManager implementation ------------------
  /**
   * init the plugin manager.
   * @param daemon the LockssDaemon instance
   * @throws LockssDaemonException if we already instantiated this manager
   * @see org.lockss.app.LockssManager.initService()
   */
  public void initService(LockssDaemon daemon) throws LockssDaemonException {
    if(theManager == null) {
       theDaemon = daemon;
       theManager = new PluginManager();
     }
     else {
       throw new LockssDaemonException("Multiple Instantiation.");
     }

  }

  /**
   * start the plugin manager.
   * @see org.lockss.app.LockssManager#startService()
   */
  public void startService() {

  }

  /**
   * stop the plugin manager
   * @see org.lockss.app.LockssManager#stopService()
   */
  public void stopService() {
    // TODO: checkpoint here
    theManager = null;
  }

  /**
   * Register the <code>ArchivalUnit</code>, so that
   * it can be found by <code>Plugin.findArchivalUnit()</code>.
   * @param au <code>ArchivalUnit</code> to add.
   */
  public void registerArchivalUnit(ArchivalUnit au) {
    if (!archivalUnits.contains(au)) {
      archivalUnits.addElement(au);
    }
  }

  /**
   * Unregister the <code>ArchivalUnit</code>, so that
   * it will not be found by <code>Plugin.findArchivalUnit()</code>.
   * @param au <code>ArchivalUnit</code> to remove.
   */
  public void unregisterArchivalUnit(ArchivalUnit au) {
    archivalUnits.remove(au);
  }

  /**
   * Find the <code>ArchivalUnit</code>
   * that comtains a URL.
   * @param url The URL to search for.
   * @return The <code>ArchivalUnit</code> that contains the URL, or
   * null if none found.  It is an error for more than one
   * <code>ArchivalUnit</code> to contain the url.
   */
  public ArchivalUnit findArchivalUnit(String url) {
    for (Iterator iter = archivalUnits.iterator();
	 iter.hasNext();) {
      Object o = iter.next();
      if (o instanceof ArchivalUnit) {
	ArchivalUnit au = (ArchivalUnit)o;
	if (au.shouldBeCached(url)) {
	  return au;
	}
      }
    }
    return null;
  }

  /**
   * Find the <code>CachedUrlSet</code>
   * that comtains a URL.
   * @param url The URL to search for.
   * @return The <code>CachedUrlSet</code> that contains the URL, or
   * null if none found.  It is an error for more than one
   * <code>ArchivalUnit</code> to contain the url.
   */
  public CachedUrlSet findAUCachedUrlSet(String url) {
    ArchivalUnit au = findArchivalUnit(url);
    if (au == null) {
      return null;
    }
    return au.getAUCachedUrlSet();
  }

  /**
   * Get the list of ArchivalUnits.
   * @return an Iterator of ArchivalUnits
   */
  public static Iterator getArchivalUnits() {
    return Collections.unmodifiableList(archivalUnits).iterator();
  }

  /**
   * Returns the number of archival units currently registered.
   * @return an integer
   */
  public static int getNumArchivalUnits() {
    return archivalUnits.size();
  }

//    /**
//     * Find or create a <code>CachedUrlSet</code> representing the content
//     * specified by the URL and pattern.
//     * @param url
//     * @param regex
//     */
//    public static CachedUrlSet findCachedUrlSet(String url, String regex) {
//      ArchivalUnit au = findArchivalUnit(url);
//      if (au == null) {
//        return null;
//      }
//      return au.makeCachedUrlSet(url, regex);
//    }
}
