/*
 * $Id: UrlCacher.java,v 1.3 2002-11-27 20:03:34 troberts Exp $
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
import java.io.*;
import java.util.Properties;

/**
 * UrlCacher is used to store the contents and
 * meta-information of a single url being cached.  It is implemented by the
 * plug-in, which provides a static method taking a String url and
 * returning an object implementing the UrlCacher interface.
 */
public interface UrlCacher {
    /**
     * Return the url being represented
     * @return the <code>String</code> url being represented.
     */
    public String getUrl();
    /**
     * Return the {@link CachedUrlSet} to which this UrlCacher belongs.
     */
    public CachedUrlSet getCachedUrlSet();
    /**
     * Return <code>true</code> if the underlying url is one that
     * the plug-in believes should be preserved.
     * @return <code>true</code> if the underlying url is one that
     *         the plug-in believes should be preserved.
     */
    public boolean shouldBeCached();
    /**
     * Return a <code>CachedUrl</code> for the content stored.  May be
     * called only after the content is completely written.
     * @return <code>CachedUrl</code> for the content stored.
     */
    public CachedUrl getCachedUrl();

    // Write interface - used by the crawler to write into the cache.

    /**
     * Convenience method to
     * copy the content and properties from the source into the cache,
     * using {@link #getUncachedInputStream()},
     * {@link #getUncachedProperties()} and storeContent()
     * @throws java.io.IOException on many possible I/O problems.
     */
    public void cache() throws IOException;
}
