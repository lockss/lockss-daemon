/*
 * $Id: CachedUrlSet.java,v 1.13 2003-02-20 01:37:23 aalto Exp $
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
import java.util.Iterator;
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
 * @version 0.0
 */
public interface CachedUrlSet extends NamedElement {
  /**
   * @return the {@link ArchivalUnit} to which this CachedUrlSet belongs
   */
  public ArchivalUnit getArchivalUnit();

    /**
     * Return the <code>CachedUrlSetSpec</code>
     * describing the set of URLs that are members of this
     * <code>CachedUrlSet</code>.
     * @return the CachedUrlSet
     */
    public CachedUrlSetSpec getSpec();

    /**
     * Return true if the url matches an entry in the
     * <code>CachedUrlSet</code> object's list.
     * @param url    url to be matched
     * @return <code>true</code> if the url matches an entry in the list,
     *         i.e. is in the file set, <code>false</code> otherwise
     */
    public boolean containsUrl(String url);

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
     * Return an <code>Iterator</code> of <code>NamedElement</code>
     * objects representing the direct descendants of this
     * <code>CachedUrlSet</code>.  These are CachedUrlSets for internal nodes
     * and CachedUrls for leaf nodes.
     * @return an <code>Iterator</code> of the <code>NamedElement</code>
     *         matching the members of the
     *         <code>CachedUrlSetSpec</code> list.
     */
    public Iterator flatSetIterator();

    /**
     * Return an <code>Iterator</code> of <code>NamedElement</code>
     * objects representing all the nodes of the tree rooted at this
     * <code>CachedUrlSet</code>.  These are CachedUrlSets for internal nodes
     * and CachedUrls for leaf nodes.
     * @return an <code>Iterator</code> of <code>NamedElement</code>s
     *         for all the nodes matching the members of the
     *         <code>CachedUrlSetSpec</code> list.
     */
    public Iterator treeIterator();

    /**
     * Return an estimate of the time required to hash the content.
     * @return an estimate of the time required to hash the content.
     */
    public long estimatedHashDuration();

    /**
     * Provide the measured duration of a hash attempt and an
     * indication of success or failure.
     * @param elapsed the measured duration of a hash attempt.
     * @param err the exception that terminated the hash, or null if it
     * succeeded
     */
    public void storeActualHashDuration(long elapsed, Exception err);

    // Methods used by readers

    /**
     * Return true if content for the url is currently a part of the
     * <code>CachedUrlSet</code>.
     * <i>Ie</i>, if <code>makeCachedUrl(</code><i>url</i><code>)</code>
     * would return a <code>CachedUrl</code> for which <code>exists()</code>
     * is true.
     * @param url the url of interest
     * @return true of the url is part of the cache
     */
    public boolean isCached(String url);

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

    /**
     * Returns the primary url referenced by the CachedUrlSet.
     * @return the url
     */
    public String getPrimaryUrl();

    /**
     * Needs to be overridden to hash CachedUrlSets properly.
     * @return the hashcode
     */
    public int hashCode();

    /**
     * Needs to be overridden to hash CachedUrlSets properly.
     * @param obj the object to compare to
     * @return true if equal
     */
    public boolean equals(Object obj);
}
