/*
 * $Id: HashService.java,v 1.2 2002-07-09 13:40:13 dshr Exp $
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
import java.security.MessageDigest;

/**
 * This interface is implemented by the generic LOCKSS daemon.
 * The generic daemon uses this interface to hash the files
 * representing the preserved content.
 *
 * XXX Note that the HashService must do time-slice scheduling
 * so that large requests,  which can take hours,  do not pre-empt
 * smaller requests. And it must refuse to overcommit the available
 * resources.
 *
 * @author  David S. H. Rosenthal
 * @version 0.0
 */
public interface HashService {
    /**
     * Ask for the content of the <code>CachedUrlSet</code> object to be
     * hashed by the <code>hasher</code> before the expiration of
     * <code>deadline</code>, and the result provided to the
     * <code>callback</code>.
     * @param urlset   a <code>CachedUrlSet</code> object representing
     *                 the content to be hashed.
     * @param hasher   a <code>MessageDigest</code> object to which
     *                 the content will be provided.
     * @param deadline the time by which the callbeack must have been
     *                 called, or an exception thrown.
     * @param callback the object whose <code>hashComplete()</code>
     *                 method will be called when hashing succeds
     *                 or fails.
     * @param cookie   used to disambiguate callbacks
     * @return <code>true</code> if the request has been queued,
     *         <code>false</code> if the resources to do it are not
     *         available.
     */
    public boolean hashContent(CachedUrlSet urlset,
			       MessageDigest hasher,
			       DeadLine deadline,
			       HashServiceCallBack callback,
			       Object cookie);
    /**
     * Ask for the names in the <code>CachedUrlSet</code> object to be
     * hashed by the <code>hasher</code> before the expiration of
     * <code>deadline</code>, and the result provided to the
     * <code>callback</code>.
     * @param urlset   a <code>CachedUrlSet</code> object representing
     *                 the content to be hashed.
     * @param hasher   a <code>MessageDigest</code> object to which
     *                 the content will be provided.
     * @param deadline the time by which the callbeack must have been
     *                 called, or an exception thrown.
     * @param callback the object whose <code>hashComplete()</code>
     *                 method will be called when hashing succeds
     *                 or fails.
     * @param cookie   used to disambiguate callbacks
     * @return <code>true</code> if the request has been queued,
     *         <code>false</code> if the resources to do it are not
     *         available.
     */
    public boolean hashNames(CachedUrlSet urlset,
			     MessageDigest hasher,
			     DeadLine deadline,
			     HashServiceCallBack callback,
			     Object cookie);
}
