/*
 * $Id$
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.app.*;
import org.lockss.util.*;

/** Comparator for sorting Aus alphabetically by title.  This is used in a
 * TreeSet, so must return 0 only for identical objects. */
public class AuOrderComparator implements Comparator {
  CatalogueOrderComparator coc = CatalogueOrderComparator.SINGLETON;

  public int compare(Object o1, Object o2) {
    if (o1 == o2) {
      return 0;
    }
    if (!((o1 instanceof ArchivalUnit)
	  && (o2 instanceof ArchivalUnit))) {
      throw new IllegalArgumentException("AuOrderComparator(" +
					 o1.getClass().getName() + "," +
					 o2.getClass().getName() + ")");
    }
    ArchivalUnit a1 = (ArchivalUnit)o1;
    ArchivalUnit a2 = (ArchivalUnit)o2;
    int res = coc.compare(a1.getName(), a2.getName());
    if (res == 0) {
      res = coc.compare(a1.getAuId(), a2.getAuId());
    }
    if (res == 0) {
      // this can happen during testing.  Mustn't be equal; return a
      // consistent order
      res = o1.hashCode() - o2.hashCode();
    }
    if (res == 0) {
      res = 1;
    }
    return res;
  }
}
