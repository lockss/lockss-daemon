/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.*;

import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;

/**
 * UrlCacher is used to store the contents and
 * meta-information of a single url being cached.  It is implemented by the
 * plug-in, which provides a static method taking a String url and
 * returning an object implementing the UrlCacher interface.
 */
public interface UrlCacher {

  public static final int REFETCH_FLAG = 1;
  public static final int DONT_CLOSE_INPUT_STREAM_FLAG = 4;


  /**
   * Return the ArchivalUnit to which this UrlCacher belongs.
   * @return the ArchivalUnit
   */
  public ArchivalUnit getArchivalUnit();

  /**
   * Return the url being represented
   * @return the {@link String} url being represented.
   */
  public String getUrl();

  /**
   * Return a {@link CachedUrl} for the content stored.  May be
   * called only after the content is completely written.
   * @return {@link CachedUrl} for the content stored.
   */
  public CachedUrl getCachedUrl();

  /**
   * Sets various attributes of the fetch operation
   * Currently these are:
   * refetch - refetch the content even if it's already present and up to date
   * don't close input stream - needed for archives
   * @param fetchFlags BitSet encapsulating the fetch flags
   */
  public void setFetchFlags(BitSet fetchFlags);

  /**
   * Gest the fetch flags
   */
  public BitSet getFetchFlags();

  
  /** Set a LockssWatchdog that should be poked periodically while copying
   * the content from the network input stream to the repository.
   * @see StreamUtil#copy(InputStream, OutputStream, long, LockssWatchdog)
   */
  public void setWatchdog(LockssWatchdog wdog);
  
  public LockssWatchdog getWatchdog();

  /**
   * Copies the content and properties from the source into the cache.
   * Fetches content with if-modified-since unless REFETCH_FLAG is set.
   * @return CACHE_RESULT_FETCHED if the content was fetched and stored,
   * CACHE_RESULT_NOT_MODIFIED if the server reported the contents as
   * unmodified.
   * @throws java.io.IOException on many possible I/O problems.
   */
  public void storeContent()
      throws IOException;

  /**
   * Return an exception with info to be reported in the crawl status along
   * with the URL (presumably produced by a validator), or null.
   */
  public CacheException getInfoException();
  
  /**
   * If we want to store under every redirect url set the list
   * @param redirectUrls
   */
  public void setRedirectUrls(List<String> redirectUrls);
  
  /**
   * If the fetchUrl is different from the origUrl
   * @param fetchUrl
   */
  public void setFetchUrl(String fetchUrl);

}
