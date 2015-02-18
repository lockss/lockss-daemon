/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

/** TitleSet implementation that returns all known titles */
public class TitleSetAllAus extends BaseTitleSet {
  /** Create a TitleSet that consists of all known titles
   * @param daemon used to get list of all known titles
   */
  public TitleSetAllAus(LockssDaemon daemon) {
    super(daemon, "All AUs");
  }

  /** Filter a collection of titles by the xpath predicate
   * @param allTitles collection of {@link TitleConfig}s to be filtered
   * @return collection of {@link TitleConfig}s that match the predicate
   */
  protected Collection<TitleConfig>
    filterTitles(Collection<TitleConfig> allTitles) {

    return allTitles;
  }

  protected int getActionables() {
    return SET_ADDABLE;
  }

  /** Causes this set to sort first */
  protected int getMajorOrder() {
    return 1;
  }

  public boolean equals(Object o) {
    return (o instanceof TitleSetAllAus);
  }

  public int hashCode() {
    return 0x272035;
  }

  public String toString() {
    return "[TS.AllAus]";
  }
}
