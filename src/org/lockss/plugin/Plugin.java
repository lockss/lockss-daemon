/*
 * $Id: Plugin.java,v 1.1 2002-07-17 09:02:22 tal Exp $
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

/**
 * Plugin global functionality 
 *
 * @author  TAL
 * @version 0.0
 */
public class Plugin {

  private static Vector cachedUrlSets = new Vector();

  /**
   * Register the <code>CachedUrlSet</code>, so that
   * it can be found by <code>Plugin.findCachedUrlSet()</code>.
   * @param cus <code>CachedUrlSet</code> to add.
   */
  public static void registerCachedUrlSet(CachedUrlSet cus) {
    if (!cachedUrlSets.contains(cus)) {
      cachedUrlSets.addElement(cus);
    }
  }
      
  /**
   * Unregister the <code>CachedUrlSet</code>, so that
   * it will not be found by <code>Plugin.findCachedUrlSet()</code>.
   * @param cus <code>CachedUrlSet</code> to remove.
   */
  public static void unregisterCachedUrlSet(CachedUrlSet cus) {
    cachedUrlSets.remove(cus);
  }

  /**
   * Find the <code>CachedUrlSet</code> that comtains a URL.
   * @param url The URL to search for.
   * @return The <code>CachedUrlSet</code> that contains the URL, or
   * null if none found.  If more than one <code>CachedUrlSet</code>
   * contains the URL, the earliest one registered is returned.
   */
  public static CachedUrlSet findCachedUrlSet(String url) {
    for (Iterator iter = cachedUrlSets.iterator();
	 iter.hasNext();) {
      Object o = iter.next();
      if (o instanceof CachedUrlSet) {
	CachedUrlSet cus = (CachedUrlSet)o;
	if (cus.memberOfSet(url)) {
	  return cus;
	}
      }
    }
    return null;
  }
}
