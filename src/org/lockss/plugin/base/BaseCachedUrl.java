/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.*;
import java.net.*;
import de.schlichtherle.truezip.file.*;
//import de.schlichtherle.truezip.fs.*;

import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.definable.*;
import org.lockss.truezip.*;
import org.lockss.repository.*;
import org.lockss.util.*;
import org.lockss.rewriter.*;
import org.lockss.extractor.*;

/** Base class for CachedUrls.  Expects the LockssRepository for storage.
 * Plugins may extend this to get some common CachedUrl functionality.
 */
public class BaseCachedUrl implements CachedUrl {
  protected ArchivalUnit au;
  protected String url;
  protected static Logger logger = Logger.getLogger(CachedUrl.class);

  private LockssRepository repository;
  private RepositoryNode leaf = null;
  protected RepositoryNode.RepositoryNodeContents rnc = null;
  protected Properties options;

  // Cached here as might be used several times in quick succession
  // (esp. by archive members).  Don't want to store in AU.
  PatternStringMap urlMimeMap = null;

  public static final String PREFIX = Configuration.PREFIX + "baseCachedUrl.";

  private static final String PARAM_SHOULD_FILTER_HASH_STREAM =
    PREFIX + "filterHashStream";
  private static final boolean DEFAULT_SHOULD_FILTER_HASH_STREAM = true;

  public static final String PARAM_FILTER_USE_CHARSET =
    PREFIX + "filterUseCharset";
  public static final boolean DEFAULT_FILTER_USE_CHARSET = true;

  /** Hide files with URLs that don't match the crawl rules (which may have
   * changed since files were collected) */
  public static final String PARAM_INCLUDED_ONLY = PREFIX + "includedOnly";
  static final boolean DEFAULT_INCLUDED_ONLY = true;

  /** Check raw Content-Type property in addition to X-Lockss-content-type.
   * Disable only for backward compatibility. */
  public static final String PARAM_USE_RAW_CONTENT_TYPE =
    PREFIX + "useRawContentType";
  public static final boolean DEFAULT_USE_RAW_CONTENT_TYPE = true;

  public static final String DEFAULT_METADATA_CONTENT_TYPE = "text/html";


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
   * return a string "[BCU: <url>]"
   * @return the string form
   */
  public String toString() {
    return "[BCU: "+ getUrl() + "]";
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
    return openForHashing(null);
  }

  /**
   * Return a stream suitable for hashing with a hash of the unfiltered
   * content.
   * @param hasher HashedInputStream.Hasher containing MessageDigest to be
   * updated
   * @return an InputStream
   */
  public InputStream openForHashing(HashedInputStream.Hasher hasher) {
    if (CurrentConfig.getBooleanParam(PARAM_SHOULD_FILTER_HASH_STREAM,
				      DEFAULT_SHOULD_FILTER_HASH_STREAM)) {
      logger.debug3("Filtering on, returning filtered stream");
      return getFilteredStream(hasher);
    } else {
      logger.debug3("Filtering off, returning unfiltered stream");
      return getUncompressedInputStream(hasher);
    }
  }

  public void setOption(String option, String val) {
    if (options == null) {
      options = new Properties();
    }
    options.setProperty(option, val);
  }

  protected String getOption(String option) {
    if (options == null) {
      return null;
    }
    return options.getProperty(option);
  }

  protected boolean isIncludedOnly() {
    String incOpt = getOption(OPTION_INCLUDED_ONLY);
    if ("true".equalsIgnoreCase(incOpt)) {
      return true;
    }
    if ("false".equalsIgnoreCase(incOpt)) {
      return false;
    }
    return CurrentConfig.getBooleanParam(PARAM_INCLUDED_ONLY,
					 DEFAULT_INCLUDED_ONLY);
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
    if (leaf == null || !leaf.hasContent()) {
      return false;
    }
    if (isIncludedOnly() && !au.shouldBeCached(getUrl())) {
      logger.debug2("hasContent("+getUrl()+"): excluded by crawl rule");
      return false;
    }
    return true;
  }

  public InputStream getUnfilteredInputStream() {
    ensureRnc();
    return rnc.getInputStream();
  }

  public InputStream getUnfilteredInputStream(HashedInputStream.Hasher hasher) {
    InputStream is = getUnfilteredInputStream();
    if (hasher != null) {
      is = newHashedInputStream(is, hasher);
    }
    return is;
  }

  /** Return an InputStream on the content.  If a Content-Encoding header
   * is present indicating that the content is compressed, it is
   * decompressed. */
  public InputStream getUncompressedInputStream() {
    return getUncompressedInputStream(null);
  }

  /** Return an InputStream on the content.  If a Content-Encoding header
   * is present indicating that the content is compressed, it is
   * decompressed.  The Content-Encoding and Content-Length headers, and
   * the results of getContentSize(), will continue to reflect the
   * compressed content, not what is returned in this stream. */
  public InputStream getUncompressedInputStream(HashedInputStream.Hasher hasher) {
    InputStream in = getUnfilteredInputStream(hasher);;
    String contentEncoding = getProperty(PROPERTY_CONTENT_ENCODING);
    // Daemon versions 1.67 and 1.68 decompressed on receipt but didn't
    // remove the Content-Encoding header.  If decompression fails return
    // the raw stream.
    return StreamUtil.getUncompressedInputStreamOrFallback(in,
							   contentEncoding,
							   getUrl());
  }

  // Clients of CachedUrl expect InputStreams to support mark/reset
  private InputStream newHashedInputStream(InputStream is,
					   HashedInputStream.Hasher hasher) {
    return new BufferedInputStream(new HashedInputStream(is, hasher));
  }

  private String getProperty(String prop) {
    CIProperties props = getProperties();
    if (props != null) {
      return props.getProperty(prop);
    }
    return null;
  }

  public String getContentType() {
    String res = null;
    CIProperties props = getProperties();
    if (props != null) {
      res = props.getProperty(PROPERTY_CONTENT_TYPE);
    }
    if (res == null &&
	CurrentConfig.getBooleanParam(PARAM_USE_RAW_CONTENT_TYPE,
				      DEFAULT_USE_RAW_CONTENT_TYPE)) {
      res = props.getProperty("Content-Type");
    }
    if (res != null) {
      return res;
    }
    return matchUrlMimeMap(getUrl());
  }

  PatternStringMap getUrlMimeTypeMap() {
    if (urlMimeMap == null) {
      urlMimeMap = au.makeUrlMimeTypeMap();
    }
    return urlMimeMap;
  }

  String matchUrlMimeMap(String url) {
    PatternStringMap map = getUrlMimeTypeMap();;
    String mime = map.getMatch(url);
    if (mime != null) {
      logger.debug("Inferred mime type: " + mime + " for " + getUrl());
      return mime;
    }
    return null;
  }

  public String getEncoding() {
    String res = null;
    if (CurrentConfig.getBooleanParam(PARAM_FILTER_USE_CHARSET,
				      DEFAULT_FILTER_USE_CHARSET)) {
      res = HeaderUtil.getCharsetFromContentType(getContentType());
    }
    if (res == null) {
      res = Constants.DEFAULT_ENCODING;
    }
    return res;
  }

  public Reader openForReading() {
    try {
      return CharsetUtil.getReader(this);
    } catch (IOException e) {
      // XXX Wrong Exception.  Should this method be declared to throw
      // UnsupportedEncodingException?
      logger.error("Creating InputStreamReader for '" + getUrl() + "'", e);
      throw new LockssRepository.RepositoryStateException
	("Couldn't create InputStreamReader:" + e.toString());
    }
  }

  public LinkRewriterFactory getLinkRewriterFactory() {
    LinkRewriterFactory ret = null;
    String ctype = getContentType();
    if (ctype != null) {
      ret = au.getLinkRewriterFactory(ctype);
    }
    return ret;
  }

  public CIProperties getProperties() {
    ensureRnc();
    return CIProperties.fromProperties(rnc.getProperties());
  }

  /**
   * Add to the properties attached to the url in the cache, if any.
   * Requires {@link #release()}
   * @param key
   * @param value
   * Throws IllegalOperationException if either the key is not on
   * the list of keys it is permitted to add, or if the properties
   * already contains the key.
   */
  public void addProperty(String key, String value) {
    ensureRnc();
    rnc.addProperty(key, value);
  }

  public long getContentSize() {
    return getNodeVersion().getContentSize();
  }

  /**
   * Return a FileMetadataExtractor for the CachedUrl's content type, or
   * null if the plugin has no FileMetadataExtractor for that MIME type
   * @param target the purpose for which metadata is being extracted
   */
  public FileMetadataExtractor getFileMetadataExtractor(MetadataTarget target) {
    String ct = getContentType();
    FileMetadataExtractor ret = au.getFileMetadataExtractor(target, ct);
    return ret;
  }

  public synchronized void release() {
    if (rnc != null) {
      rnc.release();
      rnc = null;
    }
  }

  protected synchronized void ensureRnc() {
    if (rnc == null) {
      rnc = getNodeVersion().getNodeContents();
    }
  }

  private LockssDaemon getDaemon() {
    return au.getPlugin().getDaemon();
  }

  private void getRepository() {
    repository = getDaemon().getLockssRepository(au);
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
        throw new IllegalArgumentException("Couldn't parse url properly.", mue);
      }
    }
  }

  protected InputStream getFilteredStream() {
    return getFilteredStream(null);
  }

  protected InputStream getFilteredStream(HashedInputStream.Hasher hasher) {
    String contentType = getContentType();
    // first look for a FilterFactory
    FilterFactory fact = au.getHashFilterFactory(contentType);
    if (fact != null) {
      if (logger.isDebug3()) {
	logger.debug3("Filtering " + contentType +
		      " with " + fact.getClass().getName());
      }
      InputStream unfis = getUncompressedInputStream(hasher);
      try {
	return fact.createFilteredInputStream(au, unfis, getEncoding());
      } catch (PluginException e) {
	IOUtil.safeClose(unfis);
	throw new RuntimeException(e);
      } catch (RuntimeException e) {
	IOUtil.safeClose(unfis);
	throw e;
      }
    }
    // then look for deprecated FilterRule
    FilterRule fr = au.getFilterRule(contentType);
    if (fr != null) {
      if (logger.isDebug3()) {
	logger.debug3("Filtering " + contentType +
		      " with " + fr.getClass().getName());
      }
      Reader unfrdr = openForReading();
      try {
	Reader rd = fr.createFilteredReader(unfrdr);
	return new ReaderInputStream(rd);
      } catch (PluginException e) {
	IOUtil.safeClose(unfrdr);
        throw new RuntimeException(e);
      }
    }
    if (logger.isDebug3()) logger.debug3("Not filtering " + contentType);
    InputStream ret = getUncompressedInputStream();
    if (hasher != null) {
      ret = newHashedInputStream(ret, hasher);
    }
    return ret;
  }

  public CachedUrl getArchiveMemberCu(ArchiveMemberSpec ams) {
    Member memb = new Member(au, url, this, ams);
    return memb;
  }

  CachedUrl getArchiveMemberCu(ArchiveMemberSpec ams, TFile memberTf) {
    Member memb = new Member(au, url, this, ams, memberTf);
    return memb;
  }

  @Override
  public boolean isArchiveMember() {
    return false;
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
      if (!getNodeVersion().hasContent()) {
	return false;
      }
      if (isIncludedOnly() && !au.shouldBeCached(getUrl())) {
	logger.debug2("hasContent("+getUrl()+"): excluded by crawl rule");
	return false;
      }
      return true;
    }

    /**
     * return a string "[BCU: v=n <url>]"
     * @return the string form
     */
    public String toString() {
      int ver;
      try {
	ver = getVersion();
      } catch (RuntimeException e) {
	ver = -1;
      }
      return "[BCU: v=" + ver + " " + url+"]";
    }
  }

  /** Special behavior for CUs that are archive members.  This isn't
   * logically a subtype of CachedUrl because not all places that accept a
   * CachedUrl can operate an archive member, but it's the convenient way
   * to implement it.  Perhaps it should be a supertype (interface)? */
  static class Member extends BaseCachedUrl {
    protected BaseCachedUrl bcu;
    protected ArchiveMemberSpec ams;
    protected TFileCache.Entry tfcEntry = null;
    protected TFile memberTf = null;
    protected CIProperties memberProps = null;

    Member(ArchivalUnit au, String url, BaseCachedUrl bcu,
	   ArchiveMemberSpec ams) {
      super(au, url);
      this.ams = ams;
      this.bcu = bcu;
    }

    Member(ArchivalUnit au, String url, BaseCachedUrl bcu,
	   ArchiveMemberSpec ams, TFile memberTf) {
      super(au, url);
      this.ams = ams;
      this.bcu = bcu;
      this.memberTf = memberTf;
    }

    @Override
    public String getUrl() {
      return ams.toUrl();
    }

    @Override
    /** True if the archive exists and the member exists */
    public boolean hasContent() {
      if (!super.hasContent()) {
	return false;
      }
      try {
	TFile tf = getTFile();
	if (tf == null) {
	  return false;
	}
	if (!tf.isDirectory()) {
	  return false;
	}
	return getMemberTFile().exists();
      } catch (Exception e) {
	String msg =
	  "Couldn't open member for which exists() was true: " + this;
	logger.error(msg);
	throw new LockssRepository.RepositoryStateException(msg, e);
      }
    }

    @Override
    public InputStream getUnfilteredInputStream() {
      if (!super.hasContent()) {
	return null;
      }
      try {
	TFile tf = getTFile();
	if (tf == null) {
	  return null;
	}
	if (!tf.isDirectory()) {
	  logger.error("tf.isDirectory() = false");
	  return null;
	}
	TFile membtf = getMemberTFile();
	if (!membtf.exists()) {
	  return null;
	}
	InputStream is = new TFileInputStream(membtf);
	if (CurrentConfig.getBooleanParam(RepositoryNodeImpl.PARAM_MONITOR_INPUT_STREAMS,
					  RepositoryNodeImpl.DEFAULT_MONITOR_INPUT_STREAMS)) {
	  is = new MonitoringInputStream(is, this.toString());
	}
	return is;
      } catch (Exception e) {
	String msg =
	  "Couldn't open member for which exists() was true: " + this;
	logger.error(msg);
	throw new LockssRepository.RepositoryStateException(msg, e);
      }
    }

    /** Properties of an archive member are synthesized from its size and
     * extension, and the enclosing archive's collection properties
     * (collection date, Last-Modified) */
    @Override
    public CIProperties getProperties() {
      if (memberProps == null) {
	memberProps = synthesizeProperties();
      }
      return memberProps;
    }

    private CIProperties synthesizeProperties() {
      CIProperties res = new CIProperties();
      try {
	TFileCache.Entry ent = getTFileCacheEntry();
	if (ent.getArcCuProps() != null) {
	  res.putAll(ent.getArcCuProps());
	}
      } catch (IOException e) {
	logger.warning("Couldn't copy archive props to member CU", e);
      }

      res.put(CachedUrl.PROPERTY_NODE_URL, getUrl());
      res.put("Length", getContentSize());

      try {
	// If member has last modified, overwrite any inherited from archive
	// props.
	TFile membtf = getMemberTFile();
	long lastMod = membtf.lastModified();
	if (lastMod > 0) {
	  res.put(CachedUrl.PROPERTY_LAST_MODIFIED,
		  DateTimeUtil.GMT_DATE_FORMATTER.format(new Date(lastMod)));
	}
      } catch (IOException e) {
	logger.warning("Couldn't get member Last-Modified", e);
      }

      String ctype = inferContentType();
      if (!StringUtil.isNullString(ctype)) {
	res.put("Content-Type", ctype);
	res.put(PROPERTY_CONTENT_TYPE, ctype);

      }
      return res;
    }

    private String inferContentType() {
      String ext = FileUtil.getExtension(ams.getName());
      if (ext == null) {
	return null;
      }
      return MimeUtil.getMimeTypeFromExtension(ext);
    }

    @Override
    public long getContentSize() {
      try {
	return getMemberTFile().length();
      } catch (IOException e) {
	throw new LockssRepository.RepositoryStateException
	  ("Couldn't get archive member length", e);
      }
    }


    // XXX Should release do something other than release the archive CU?
//     public void release() {
//       if (rnc != null) {
// 	rnc.release();
//       }
//     }

    private TFile getMemberTFile() throws IOException {
      checkValidTfcEntry();
      if (memberTf == null) {
	memberTf = new TFile(getTFile(), ams.getName());
      }
      return memberTf;
    }

    private TFile getTFile() throws IOException {
      TFileCache.Entry ent = getTFileCacheEntry();
      if (ent == null) {
	return null;
      }
      return ent.getTFile();
    }

    void checkValidTfcEntry() {
      if (tfcEntry != null && !tfcEntry.isValid()) {
	tfcEntry = null;
      }
    }

    private TFileCache.Entry getTFileCacheEntry() throws IOException {
      checkValidTfcEntry();
      if (tfcEntry == null) {
	TrueZipManager tzm = bcu.getDaemon().getTrueZipManager();
	tfcEntry = tzm.getCachedTFileEntry(au.makeCachedUrl(url));
      }
      return tfcEntry;
    }

    ArchiveMemberSpec getArchiveMemberSpec() {
      return ams;
    }

    @Override
    public boolean isArchiveMember() {
      return true;
    }

    String getArchiveUrl() {
      return super.getUrl();
    }

    @Override
    public CachedUrl getArchiveMemberCu(ArchiveMemberSpec ams) {
      throw new UnsupportedOperationException("Can't create a CU member from a CU member: "
					      + this);
    }

    @Override
    public CachedUrl getCuVersion(int version) {
      throw new UnsupportedOperationException("Can't access versions of a CU member: "
					      + this);
    }

    @Override
    public CachedUrl[] getCuVersions(int maxVersions) {
      throw new UnsupportedOperationException("Can't access versions of a CU member: "
					      + this);
    }

    public String toString() {
      return "[BCUM: "+ getUrl() + "]";
    }

  }
}
