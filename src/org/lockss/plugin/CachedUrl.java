/*
 * $Id: CachedUrl.java,v 1.7 2003-07-23 00:14:51 troberts Exp $
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
import java.util.Properties;
//import org.lockss.daemon.CachedUrlSetNode;

/**
 * <code>CachedUrl</code> is used to access the contents and
 * meta-information of a single cached url.  The contents and
 * meta-information represented by any particular <code>CachedUrl</code>
 * instance are immutable, thus no locking or synchronization is required
 * by readers.  Any new content obtained for the url (<i>eg</i>, by a new
 * crawl or a repair) will be visible only via a newly obtained
 * <code>CachedUrl</code>.
 *
 * <code>CachedUrl</code> is implemented by the plug-in, which provides a
 * static method taking a String url and returning an object implementing
 * the <code>CachedUrl</code> interface.
 *
 * @author  David S. H. Rosenthal
 * @see UrlCacher
 * @version 0.0 */
public interface CachedUrl extends CachedUrlSetNode {
    /**
     * Get an object from which the content of the url can be read
     * from the cache.
     * @return a {@link InputStream} object from which the
     *         unfiltered content of the cached url can be read.
     */
    public InputStream openForReading();

    /**
     * Get an inputstream of the content suitable for hashing.
     * Probably filtered.
     * @return an {@link InputStream}
     */
    public InputStream openForHashing();

    /**
     * Return a reader on this CachedUrl
     * @return {@link Reader}
     */
    public Reader getReader();

    /**
     * Get the properties attached to the url in the cache, if any.
     * @return the {@link Properties} object attached to the
     *         url.  If no properties have been attached, an
     *         empty {@link Properties} object is returned.
     */
    public Properties getProperties();

    /**
     * Return the unfiltered content size.
     * @return a byte[]
     */
    public byte[] getUnfilteredContentSize();

    /**
     * Return the ArchivalUnit to which this CachedUrl belongs.
     * @return the ArchivalUnit
     */
    public ArchivalUnit getArchivalUnit();
}
