/*
 * $Id: CachedUrl.java,v 1.23 2009-05-23 18:06:26 dshr Exp $
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.util.*;
import org.lockss.rewriter.*;
import org.lockss.extractor.*;

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

  /** If content was found by following a redirect from here, the URL at
   * which the content was found (possibly the last in a chain of
   * redirects).
  */
  public static final String HEADER_PREFIX = null;

  /** The URL under which the node was created.  Distinguishes between foo
   * and foo/ */
  public static final String PROPERTY_NODE_URL = "X-Lockss-node-url";

  /** The final URL (that had content) if redirected from here.  If this is
   * present, it is the base URL appropriate for interpreting the
   * content. */
  public static final String PROPERTY_CONTENT_URL = "X-Lockss-content-url";

  /** The URL immediately redirected to from here, if any. */
  public static final String PROPERTY_REDIRECTED_TO = "X-Lockss-redirected-to";

  /** The final URL (that had content) if redirected from here */
  public static final String PROPERTY_REPAIR_FROM = "X-Lockss-repaired-from";

  /** From response.getContentType(); might be inferred */
  public static final String PROPERTY_CONTENT_TYPE = "X-Lockss-content-type";

  /** The original URL requested, potentially at the start of a chains of
   * redirects.  Not predictable; don't use. */
  public static final String PROPERTY_ORIG_URL = "X-Lockss-orig-url";

  /** Date: from response, as a long */
  public static final String PROPERTY_FETCH_TIME = "X_Lockss-server-date";

  public static final String PROPERTY_LAST_MODIFIED = "last-modified";

  /**
   * Return a version-specific CachedUrl for the specified content version
   * @throws UnsupportedOperationException if node has no versions
   * @return a {@link CachedUrl} bound to the specified version
   */
  public CachedUrl getCuVersion(int version);

  /**
   * Return an array of version-specific CachedUrls for all versions of
   * content/props at this URL.  The result is sorted from most to least
   * recent; the CachedUrl for current version is the first element in the
   * array.
   * @throws UnsupportedOperationException if node has no versions
   * @return array of {@link CachedUrl}
   */
  public CachedUrl[] getCuVersions();

  /**
   * Return an array of version-specific CachedUrls for the most recent
   * <code>maxVersions</code> versions of content/props at this URL.  The
   * result is sorted from most to least recent; the CachedUrl for current
   * version is the first element in the array.
   * @throws UnsupportedOperationException if node has no versions
   * @return array of {@link CachedUrl}
   */
  public CachedUrl[] getCuVersions(int maxVersions);

  /**
   * Return the version number.  This is the current version if the
   * CachedUrl isn't bound to a particular version
   * @throws UnsupportedOperationException if node has no versions
   * @return version number
   */
  public int getVersion();

  /**
  * Get an object from which the content of the url can be read
  * from the cache.
  * @return a {@link InputStream} object from which the
  *         unfiltered content of the cached url can be read.
  */
  public InputStream getUnfilteredInputStream();

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
  public Reader openForReading();

  /**
   * Return a LinkRewriterFactory for this
   * CachedUrl
   */
  public LinkRewriterFactory getLinkRewriterFactory();

  /**
   * Get the properties attached to the url in the cache, if any.
   * @return the {@link CIProperties} object attached to the
   *         url.  If no properties have been attached, an
   *         empty {@link CIProperties} object is returned.
   */
  public CIProperties getProperties();

  /**
   * Return the unfiltered content size.
   * @return number of bytes in file
   */
  public long getContentSize();

  /**
   * Return the content type (MIME or MIME;charset)
   * @return the content type
   */
  public String getContentType();

  /**
   * Return the encoding to use for the CachedUrl
   * @return the encoding
   */
  public String getEncoding();

  /**
   * Return the ArchivalUnit to which this CachedUrl belongs.
   * @return the ArchivalUnit
   */
  public ArchivalUnit getArchivalUnit();

  /**
   * Release resources.  Temporary.
   */
  public void release();

  /**
   * Return a MetadataExtractor for the CachedUrl's content type. If there
   * isn't one, a null extractor will be returned.
   * @param contentType content type to get a content parser for
   */
  public MetadataExtractor getMetadataExtractor();

}
