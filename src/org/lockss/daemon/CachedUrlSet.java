/*
 * $Id: CachedUrlSet.java,v 1.5 2002-10-08 01:01:31 tal Exp $
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
import java.util.Enumeration;
import java.security.MessageDigest;

/**
 * This interface is implemented by plug-ins for the LOCKSS daemons.  The
 * generic daemon uses this interface to perform I/O on the files
 * representing the preserved content.  The generic daemon treats
 * <code>CachedUrlSet</code> objects as containing a set of files whose
 * URLs match a list of <code>CachedUrlSetSpec</code>s (<code>[url-prefix,
 * regular-expression]</code> pairs).
 *
 * @author  David S. H. Rosenthal
 * @version 0.0 */
public interface CachedUrlSet {
    /**
     * Add the <code>CachedUrlSetSpec</code> to the
     * <code>CachedUrlSet</code> object's list.
     * @param spec specification of a set of urls.
     */
    public void addToList(CachedUrlSetSpec spec);
    /**
     * Remove the <code>CachedUrlSetSpec</code>
     * from the <code>CachedUrlSet</code> object's list.
     * @param spec specification of a set of urls.
     * @return <code>true</code> if the removal was successful,
     *         <code>false</code> otherwise
     */
    public boolean removeFromList(CachedUrlSetSpec spec);
    /**
     * Return true if the <code>CachedUrlSetSpec</code>
     * is in the <code>CachedUrlSet</code> object's list.
     * @param spec specification of a set of urls.
     * @return <code>true</code> if the argument pair is in the list,
     *         <code>false</code> otherwise
     */
    public boolean memberOfList(CachedUrlSetSpec spec);
    /**
     * Return an <code>Enumeration</code> of <code>CachedUrlSetSpec</code>
     * objects describing the set of URLs that are members of this
     * <code>CachedUrlSet</code>. */
    public Enumeration listEnumeration();
    /**
     * Return true if the url matches an entry in the
     * <code>CachedUrlSet</code> object's list.
     * @param url    url to be matched
     * @return <code>true</code> if the url matches an entry in the list,
     *         i.e. is in the file set, <code>false</code> otherwise
     */
    public boolean memberOfSet(String url);

    // Methods used by the poller

    public CachedUrlSetHasher getContentHasher(MessageDigest hasher);
    /**
     * Return an object that can be used to hash the names of cached urls
     * that match the list of <code>CachedUrlSetSpec</code>
     * entries.
     * @param hasher a <code>MessageDigest</code> object to which the
     *               names will be supplied.
     * @return a <code>CachedUrlSetHasher</code> object that will
     *         hash the names of cached urls matching this
     *         <code>CachedUrlSet</code>.
     */
    public CachedUrlSetHasher getNameHasher(MessageDigest hasher);
    /**
     * Return an <code>Enumeration</code> of <code>CachedUrlSet</code>
     * objects representing the direct descendants of this
     * <code>CachedUrlSet</code> object.
     * @return an <code>Enumeration</code> of the <code>CachedUrlSet</code>
     *         matching the members of the
     *         <code>CachedUrlSetSpec</code> list.
     */
    public Enumeration flatEnumeration();
    /**
     * Return an <code>Enumeration</code> of <code>CachedUrlSet</code>
     * objects representing the entire tree rooted at this
     * <code>CachedUrlSet</code> object.
     * @return an <code>Enumeration</code> of the <code>CachedUrlSet</code>
     *         matching the members of the
     *         <code>CachedUrlSetSpec</code> list.
     */
    public Enumeration treeEnumeration();
    /**
     * Return an estimate of the time required to hash the content.
     * @return an estimate of the time required to hash the content.
     */
    public long hashDuration();
    /**
     * Provide the measured duration of a hash attempt and an
     * indication of success or failure.
     * @param elapsed the measured duration of a hash attempt.
     * @param err the exception that terminated the hash, or null if it
     * succeeded
     */
    public long duration(long elapsed, Exception err);

    // Methods used by readers

    /**
     * Create a <code>CachedUrl</code> object within the set.
     * @param url the url of interest
     * @return a <code>CachedUrl</code> object representing the url.
     */
    public CachedUrl makeCachedUrl(String url);

    // Methods used by writers

    /**
     * Create a <code>UrlCacher</code> object within the set.
     * @param url the url of interest
     * @return a <code>UrlCacher</code> object representing the url.
     */
    public UrlCacher makeUrlCacher(String url);
}
