/*
 * $Id: Crawler.java,v 1.2 2002-07-09 13:40:13 dshr Exp $
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

package org.lockss.daemon;
import java.io.IOException;

/**
 * This interface is implemented by the generic LOCKSS daemon.
 * The plug-ins use it to call the crawler to actually fetch
 * content.
 *
 * @author  David S. H. Rosenthal
 * @version 0.0
 */
public interface Crawler {
    /**
     * Fetch all files matching the <code>target</code> and (recursively)
     * all files they refer to. provided that all files fetched must match
     * <code>include</code> and no files fetched may match
     * <code>exclude</code>.  Return a <code>CachedUrlSet</code>
     * representing the exact set of files fetched (i.e. with no regular
     * expressions).
     * @param target   a <code>CachedUrlSet</code> object representing the
     *                 starting point of the crawl.
     * @param deadline the crawl must finish before this expires.
     * @return         a <code>CachedUrlSet</code> object listing the files
     *                 fetched,  or null if no files were fetched.
     * @exception java.io.IOException on any number of IO problems.
     */
    public CachedUrlSet fetch(CachedUrlSet target,
			      DeadLine deadline) throws IOException;
}
