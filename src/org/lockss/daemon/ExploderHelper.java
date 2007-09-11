/*
 * $Id: ExploderHelper.java,v 1.1.2.1 2007-09-11 19:14:57 dshr Exp $
 */

/*

Copyright (c) 2007 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.app.LockssDaemon;

/**
 * The interface to publisher-specific ExploderHelper classes.
 * They encapsulate knowledge about what to do with individual
 * entries in an archive that's being exploded.
 *
 * @author  David S. H. Rosenthal
 * @version 0.0
 */

public interface ExploderHelper {

  /**
   * Do what needs to be done to this ArchiveEntry, which is a
   * generic data structure describing the entry. This method
   * typically sets fields descbing the AU the entry is to end
   * up in, the URL it is to be assigned, and the header fields.
   * @param ae the entry to be processed
   */
  public void process(ArchiveEntry ae);

}
