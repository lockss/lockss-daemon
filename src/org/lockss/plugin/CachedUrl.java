/*

Copyright (c) 2000-2021 Board of Trustees of Leland Stanford Jr. University,
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

import org.mortbay.http.HttpFields;
import org.lockss.config.*;
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

  public static final String PREFIX = Configuration.PREFIX + "cachedUrl.";

  /** Allow CachedUrls to be deleted from the repository */
  public static final String PARAM_ALLOW_DELETE = PREFIX + "allowDelete";
  public static final boolean DEFAULT_ALLOW_DELETE = false;

  /** The string prepended to response header names as they're copied into
   * the CachedUrl's properties.  null means no prefix.
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

  /** The identity of the peer from which this file was obtained as a
   * repair */
  public static final String PROPERTY_REPAIR_FROM = "X-Lockss-repaired-from";

  /** The time the repair was obtained, as a long */
  public static final String PROPERTY_REPAIR_DATE = "X-Lockss-repaired-date";

  /** From response.getContentType(); might be inferred */
  public static final String PROPERTY_CONTENT_TYPE = "X-Lockss-content-type";

  /** The original URL requested, potentially at the start of a chains of
   * redirects.  Not predictable; don't use. */
  public static final String PROPERTY_ORIG_URL = "X-Lockss-orig-url";

  /** Local time when file collected.  *Not* derived from the Date: header
   * from the server, which is stored separately if present.  Poorly named
   * but cannot be changed. */
  public static final String PROPERTY_FETCH_TIME = "X_Lockss-server-date";

  /** Referer header that was sent with the request for this URL, if any.
      Used by the repair crawler */
  public static final String PROPERTY_REQ_REFERRER = "X-Lockss-referrer";

  public static final String PROPERTY_LAST_MODIFIED = "last-modified";

  /** Checksum: The checksum (hash) of the content in <alg>:<hash> format */
  // XXX This must be lowercase, as it's sometimes get/put in a
  // case-sensitive context in RepositoryNode.  The use of CIProperties
  // should be pushed down into the repository.
  public static final String PROPERTY_CHECKSUM = "x-lockss-checksum";

  public static final String PROPERTY_CONTENT_ENCODING = "content-encoding";
  public static final String PROPERTY_CONTENT_LENGTH = "content-length";

  /** This property is present only in the headers of the uncommitted
   * (unsealed) CachedUrl passed to a ContentValidator.  If present its
   * value is a list of all the URLs in the redirect chain.  It must be
   * accessed with get(), not getProperty() */
  public static final String PROPERTY_VALIDATOR_REDIRECT_URLS = 
    "x-lockss-validator-redirect-urls";

  /** CachedUrl properties that the daemon uses internally, should not be
   * served with content */

  public String[] LOCKSS_INTERNAL_PROPERTIES = {
    PROPERTY_CONTENT_TYPE,
    PROPERTY_CONTENT_URL,
    PROPERTY_NODE_URL,
    PROPERTY_ORIG_URL,
    PROPERTY_REDIRECTED_TO,
    org.lockss.repository.RepositoryNodeImpl.LOCKSS_VERSION_NUMBER,
  };
				       
  /** CachedUrl properties that reflect the audit process, conditionally
   * served with content (see {@link
   * org.lockss.jatty.ProxyManager#PARAM_INCLUDE_LOCKSS_AUDIT_PROPS}) */

  public String[] LOCKSS_AUDIT_PROPERTIES = {
    PROPERTY_REPAIR_FROM,
    PROPERTY_REPAIR_DATE,
    PROPERTY_CHECKSUM,
  };

  /** Response headers whose original values are useful to carry along with
   * the content, but which are overwritten if the content is served by the
   * proxy to another LOCKSS daemon.  The original value of these headers
   * will be preserved (and served) as a header with the original name
   * prefixed by {@value
   * org.lockss.jetty.CuResourceHandler#ORIG_HEADER_PREFIX} . */

  public String[] LOCKSS_PREFIX_ORIG_PROPERTIES = {
    HttpFields.__Date,
    HttpFields.__Server,
    PROPERTY_FETCH_TIME,
  };

  /** If true, CachedURLs outside the crawl spec will appear to have no
   * content  */
  public static final String OPTION_INCLUDED_ONLY = "IncludedOnly";

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
   * Deletes the Artifact from the repository.  May require explicit
   * configuraton to enable.
   * @throws UnsupportedOperationException if configuration or
   * repository doesn't allow deletion.
   * @throws IOException if deletion failed.
   */
  public void delete() throws UnsupportedOperationException, IOException;

  /**
   * Set options that control behavior<br>
   *
   * {@link #OPTION_INCLUDED_ONLY} ({@value #OPTION_INCLUDED_ONLY}):
   * Overrides config param only if explicitly set true or false
   */
  public void setOption(String option, String val);

  /**
  * Get an object from which the content of the url can be read
  * from the cache.
  * @return a {@link InputStream} object from which the
  *         unfiltered content of the cached url can be read.
  */
  public InputStream getUnfilteredInputStream();

  /**
  * Get an object from which the raw content of the url can be read from
  * the cache.  Also computes a hash of the raw file

  * @param md MessageDigest that will see the unfiltered content
  * @return a {@link InputStream} object from which the
  *         unfiltered content of the cached url can be read.
  */
  public InputStream getUnfilteredInputStream(HashedInputStream.Hasher hasher);

  /**
  * Return an InputStream on the content, uncompressing it if it was
  * received compressed (i.e., with a Content-Encoding of <code>gzip</code>
  * or <code>deflate</code>.  The contents of the returned stream may not
  * match the result of {@link #getContentSize()}, or the Content-Encoding
  * or Content-Length properties.
  * @return a {@link InputStream} object from which the uncompressed and
  * unfiltered content of the cached url can be read.
  */
  public InputStream getUncompressedInputStream();

  /**
  * Return an InputStream on the content, uncompressing it if it was
  * received compressed.
  * @param md MessageDigest that will see the content, before any
  * uncompression or filtering is applied
  * @return a {@link InputStream} object from which the uncompressed and
  * unfiltered content of the cached url can be read.
  */
  public InputStream getUncompressedInputStream(HashedInputStream.Hasher
						hasher);

  /**
   * Get an inputstream of the content suitable for hashing.  Uncompressed
   * if necessary, and filtered if so specified by the plugin.
   * @return an {@link InputStream}
   */
  public InputStream openForHashing();

  /**
   * Get an inputstream of the content suitable for hashing.  Uncompressed
   * if necessary, and filtered if so specified by the plugin.  Also
   * computes a hash of the raw file (no filter, no decompression).
   * @param md MessageDigest to hash the  unfiltered content
   * @return an {@link InputStream}
   */
  public InputStream openForHashing(HashedInputStream.Hasher hasher);

  /**
   * Return a Reader on the content, uncompressed if it was recieved
   * compressed.
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
   * Requires {@link #release()}
   * @return the {@link CIProperties} object attached to the
   *         url.  If no properties have been attached, an
   *         empty {@link CIProperties} object is returned.
   */
  public CIProperties getProperties();

  /**
   * Add to the properties attached to the url in the cache, if any.
   * Requires {@link #release()}
   * @param key
   * @param value
   * Throws UnsupportedOperationException if either the key is not on
   * the list of keys it is permitted to add, or if the properties
   * already contains the key.
   */
  public void addProperty(String key, String value);

  /**
   * Return the unfiltered content size.
   * @return number of bytes in file
   */
  public long getContentSize();

  /**
   * Return the content type (MIME or MIME;charset)
   * Accesses the Properties, so requires {@link #release()}
   * @return the content type
   */
  public String getContentType();

  /**
   * Return the encoding to use for the CachedUrl
   * Accesses the Properties, so requires {@link #release()}
   * @return the encoding
   */
  public String getEncoding();

  /**
   * Return the ArchivalUnit to which this CachedUrl belongs.
   * @return the ArchivalUnit
   */
  public ArchivalUnit getArchivalUnit();

  /**
   * Release resources.  Required if the InputStream or Properties have
   * been accessed.  Should be called in a finally block (when possible).
   */
  public void release();

  /**
   * Return a FileMetadataExtractor for the CachedUrl's content type. If
   * there isn't one, a null extractor will be returned.
   * @param target the purpose for which metadata is being extracted
   */
  public FileMetadataExtractor getFileMetadataExtractor(MetadataTarget target);

  /**
   * If the CachedUrl is an archive file, return a CachedUrl that
   * references the specified member of the archive instead of the whole
   * archive.  Behavior is undefined if applied to a CU that isn't an
   * archive file.
   * @param ams describes the archive member.
   */
  public CachedUrl getArchiveMemberCu(ArchiveMemberSpec ams);

  /**
   * Provides an indication of whether this CachedUrl is a member of an archive.
   * 
   * @return <code>true</code> if this CachedUrl is a member of an archive,
   *         <code>false</code> otherwise.
   */
  boolean isArchiveMember();

}
