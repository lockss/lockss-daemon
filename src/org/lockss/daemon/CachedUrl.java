/*
 * $Id: CachedUrl.java,v 1.4 2002-07-09 13:40:13 dshr Exp $
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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * This interface is used by the crawler and by the proxy.  It
 * represents the contents and meta-information of a single cached
 * url.  It is implemented by the plug-in,  which provides a static
 * method taking a String url and returning an object implementing
 * the CachedUrl interface.
 *
 * @author  David S. H. Rosenthal
 * @version 0.0
 */
public interface CachedUrl {
    /**
     * Return the url being represented
     * @return the <code>String</code> url being represented.
     */
    public String toString();
    /**
     * Return <code>true</code> if the object describes a url that
     * exists in the cache.
     * @return <code>true</code> if the object describes a url that
     *         exists in the cache, <code>false</code> otherwise.
     */
    public boolean exists();
    /**
     * Return <code>true</code> if the underlying url is one that
     * the plug-in believes should be preserved.
     * @return <code>true</code> if the underlying url is one that
     *         the plug-in believes should be preserved.
     */
    public boolean shouldBeCached();

    // Read interface - used by the proxy to access the cache.

    /**
     * Get an object from which the content of the url can be read
     * from the cache.
     * @return a <code>InputStream</code> object from which the
     *         unfiltered content of the cached url can be read.
     */
    public InputStream openForReading();
    /**
     * Get the properties attached to the url in the cache, if any.
     * @return the <code>Properties</code> object attached to the
     *         url.  If no properties have been attached, an
     *         empty <code>Properties</code> object is returned.
     */
    public Properties getProperties();

    // Write interface - used by the crawler to write into the cache.

    /**
     * Store the content from an input stream with associated
     * properties in the <code>CachedUrl</code> object.
     * @param input   an <code>InputStream</code> object from which the
     *                content can be read.
     * @param headers a <code>Properties</code> object containing the
     *                relevant HTTP headers.
     * @exception java.io.IOException on many possible I/O problems.
     */
    public void storeContent(InputStream input,
			     Properties headers) throws IOException;

    // Un-cached read interface, accessing the underlying object,  used
    // by the crawler to fetch content.  It is implemented by the plug-in
    // to allow it to provide appropriate credentials.

    /**
     * Return an <code>InputStream</code> object which accesses the
     * object being cached rather than the object in the cache.
     * @return an <code>InputStream</code> object from which the contents of
     *         the original object being cached can be read.
     */
    public InputStream getUncachedInputStream();
    /**
     * Return a <code>Properties</code> object containing the headers of
     * the object being cached rather than the object in the cache.
     * @return a <code>Properties</code> object containing the headers of
     *         the original object being cached.
     */
    public Properties getUncachedProperties();
}
