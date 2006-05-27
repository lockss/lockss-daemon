/*
 * $Id: BaseCachedUrl.java,v 1.25 2006-05-27 06:36:04 tlipkis Exp $
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

package org.lockss.plugin.base;

import java.io.*;
import java.net.MalformedURLException;

import org.lockss.config.*;
import org.lockss.plugin.*;
import org.lockss.repository.*;
import org.lockss.util.*;

/** Base class for CachedUrls.  Expects the LockssRepository for storage.
 * Plugins may extend this to get some common CachedUrl functionality.
 */
public class BaseCachedUrl implements CachedUrl {
  protected ArchivalUnit au;
  protected String url;
  protected static Logger logger = Logger.getLogger("CachedUrl");

  private LockssRepository repository;
  private RepositoryNode leaf = null;
  protected RepositoryNode.RepositoryNodeContents rnc = null;

  private static final String PARAM_SHOULD_FILTER_HASH_STREAM =
    Configuration.PREFIX+"baseCachedUrl.filterHashStream";
  private static final boolean DEFAULT_SHOULD_FILTER_HASH_STREAM = true;

  public BaseCachedUrl(ArchivalUnit owner, String url) {
    this.au = owner;
    this.url = url;
  }

  public String getUrl() {
    return url;
  }

  public int getType() {
    return CachedUrlSetNode.TYPE_CACHED_URL;
  }

  public boolean isLeaf() {
    return true;
  }

  /**
   * Overrides normal <code>toString()</code> to return a string like "BCU: <url>"
   * @return the string form
   */
  public String toString() {
    return "[BCU: "+url+"]";
  }


  /**
   * Return the ArchivalUnit to which this CachedUrl belongs.
   * @return the ArchivalUnit
   */
  public ArchivalUnit getArchivalUnit() {
    return au;
  }

  protected RepositoryNodeVersion getNodeVersion() {
    ensureLeafLoaded();
    return leaf;
  }

  public CachedUrl getCuVersion(int version) {
    ensureLeafLoaded();
    return new Version(au, url, leaf.getNodeVersion(version));
  }

  public CachedUrl[] getCuVersions() {
    return getCuVersions(Integer.MAX_VALUE);
  }

  public CachedUrl[] getCuVersions(int maxVersions) {
    ensureLeafLoaded();
    RepositoryNodeVersion[] nodeVers = leaf.getNodeVersions(maxVersions);
    CachedUrl[] res = new CachedUrl[nodeVers.length];
    for (int ix = res.length - 1; ix >= 0; ix--) {
      res[ix] = new Version(au, url, nodeVers[ix]);
    }
    return res;
  }

  public int getVersion() {
    return getNodeVersion().getVersion();
  }

  /**
   * Return a stream suitable for hashing.  This may be a filtered stream.
   * @return an InputStream
   */
  public InputStream openForHashing() {
    if (CurrentConfig.getBooleanParam(PARAM_SHOULD_FILTER_HASH_STREAM,
				      DEFAULT_SHOULD_FILTER_HASH_STREAM)) {
      logger.debug3("Filtering on, returning filtered stream");
      return getFilteredStream();
    } else {
      logger.debug3("Filtering off, returning unfiltered stream");
      return getUnfilteredInputStream();
    }
  }

  public boolean hasContent() {
    if (repository==null) {
      getRepository();
    }
    if (leaf==null) {
      try {
        leaf = repository.getNode(url);
      } catch (MalformedURLException mue) {
	return false;
      }
    }
    return (leaf == null) ? false : leaf.hasContent();
  }

  public InputStream getUnfilteredInputStream() {
    ensureRnc();
    return rnc.getInputStream();
  }

  public Reader openForReading() {
    ensureRnc();
    try {
      return
	new BufferedReader(new InputStreamReader(rnc.getInputStream(),
						 Constants.DEFAULT_ENCODING));
    } catch (IOException e) {
      // XXX Wrong Exception.  Should this method be declared to throw
      // UnsupportedEncodingException?
      logger.error("Creating InputStreamReader for '" + url + "'", e);
      throw new LockssRepository.RepositoryStateException
	("Couldn't create InputStreamReader:" + e.toString());
    }
  }

 public CIProperties getProperties() {
    ensureRnc();
    return CIProperties.fromProperties(rnc.getProperties());
  }

  public long getContentSize() {
    return getNodeVersion().getContentSize();
  }

  public void release() {
    if (rnc != null) {
      rnc.release();
    }
  }

  private void ensureRnc() {
    if (rnc == null) {
      rnc = getNodeVersion().getNodeContents();
    }
  }

  private void getRepository() {
    repository = au.getPlugin().getDaemon().getLockssRepository(au);
  }

  private void ensureLeafLoaded() {
    if (repository==null) {
      getRepository();
    }
    if (leaf==null) {
      try {
        leaf = repository.createNewNode(url);
      } catch (MalformedURLException mue) {
        logger.error("Couldn't load node due to bad url: "+url);
        throw new IllegalArgumentException("Couldn't parse url properly.");
      }
    }
  }

  private InputStream getFilteredStream() {
    CIProperties props = getProperties();
    String contentType = props.getProperty(PROPERTY_CONTENT_TYPE);
    logger.debug3("Getting filtered stream for "+contentType);
    FilterRule fr = au.getFilterRule(contentType);
    if (fr != null) {
      Reader rd = fr.createFilteredReader(openForReading());
      return new ReaderInputStream(rd);
    } else {
      logger.debug2("No FilterRule, not filtering");
    }
    return getUnfilteredInputStream();
  }

  /** A CachedUrl that's bound to a specific version. */
  static class Version extends BaseCachedUrl {
    private RepositoryNodeVersion nodeVer;

    public Version(ArchivalUnit owner, String url,
		   RepositoryNodeVersion nodeVer) {
      super(owner, url);
      this.nodeVer = nodeVer;
    }

    protected RepositoryNodeVersion getNodeVersion() {
      return nodeVer;
    }

    public boolean hasContent() {
      return getNodeVersion().hasContent();
    }

  }

}
