/*
 * $Id: BaseTitleSet.java,v 1.7 2005-10-11 05:44:15 tlipkis Exp $
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

package org.lockss.daemon;

import java.util.*;

import org.lockss.app.*;
import org.lockss.util.*;

/** Base class for TitleSet implementations */
public abstract class BaseTitleSet implements TitleSet {
  protected static Logger log = Logger.getLogger("TitleSet");

  protected LockssDaemon daemon;
  protected String name;

  public BaseTitleSet(LockssDaemon daemon, String name) {
    if (daemon == null) throw new NullPointerException("null daemon");
    if (name == null) throw new NullPointerException("null name");
    this.daemon = daemon;
    this.name = name;
  }

  /** Return the human-readable name of the title set.
   * @return the name */
  public String getName() {
    return name;
  }

  /** Return the titles in the set.
   * @return a collection of TitleConfig */
  public Collection getTitles() {
    return filterTitles(daemon.getPluginManager().findAllTitleConfigs());
  }

  /** Filter a collection of titles by the xpath predicate
   * @param allTitles collection of {@link TitleConfig}s to be filtered
   * @return a collection of {@link TitleConfig}s
   */
  abstract Collection filterTitles(Collection allTitles);

  /** Match action with actionable set from implementation */
  public final boolean isSetActionable(int action) {
    return (action & getActionables()) != 0;
  }

  abstract protected int getActionables();

  /** Allow different implementations to specify their major sort order
   * relative to other implementations.
   * @return sort index
   */
  abstract int getMajorOrder();

  /** Compare two TitleSets, first by artificial sort order, then by name */
  public int compareTo(Object o) {
    BaseTitleSet oset = (BaseTitleSet)o;
    if (getMajorOrder() != oset.getMajorOrder()) {
      return getMajorOrder() - oset.getMajorOrder();
    }
    return getName().compareTo(oset.getName());
  }

}
