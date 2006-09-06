/*
 * $Id: VoteBlockComparator.java,v 1.3 2005-11-16 07:44:09 smorabito Exp $
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

package org.lockss.protocol;

import java.util.*;

/**
 * A Comparator class for V3 VoteBlocks.
 */
public class VoteBlockComparator implements Comparator {
  public int compare(Object o1, Object o2) {
    long filteredLength1 = ((VoteBlock)o1).getFilteredLength();
    long filteredLength2 = ((VoteBlock)o2).getFilteredLength();

    if (filteredLength1 > filteredLength2) {
      return 1;
    } else if (filteredLength2 > filteredLength1) {
      return -1;
    }

    // Filtered lengths are equal, compare unfiltered length

    long unfilteredLength1 = ((VoteBlock)o1).getUnfilteredLength();
    long unfilteredLength2 = ((VoteBlock)o2).getUnfilteredLength();

    if (unfilteredLength1 > unfilteredLength2) {
      return 1;
    } else if (unfilteredLength2 > unfilteredLength1) {
      return -1;
    }

    // Unfiltered lengths are equal, compare file names

    String fn1 = ((VoteBlock)o1).getUrl();
    String fn2 = ((VoteBlock)o2).getUrl();

    return fn1.compareTo(fn2);
  }
}
