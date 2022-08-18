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

package org.lockss.test;

import java.io.*;
import java.security.*;
import java.util.*;

import org.apache.oro.text.regex.*;
import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.crawler.*;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.UrlFetcher.RedirectScheme;
import org.lockss.plugin.base.*;
import org.lockss.rewriter.*;
import org.lockss.state.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;
import org.lockss.extractor.*;

/**
 * Base class for test plugins that don't want to implement all the
 * required methods.
 * Extend only the nested classes to which you need to add bahavior.
 */
public class NullPlugin {

  /**
   * Base class for test <code>Plugin</code>s.  Default methods do nothing
   * or return constants.
   */
  public static class Plugin implements org.lockss.plugin.Plugin {
    protected Plugin() {
    }

    public void initPlugin(LockssDaemon daemon) {
    }

    public void stopPlugin() {
    }

    public void stopAu(org.lockss.plugin.ArchivalUnit au) {
    }

    public String getPluginId() {
      return "NullPlugin";
    }

    public String getVersion() {
      return "NullVersion";
    }

    public String getRequiredDaemonVersion() {
      return "0.0.0";
    }

    public String getFeatureVersion(Plugin.Feature feat) {
      return null;
    }

    public String getPluginName() {
      return "Null Plugin";
    }

    public String getPublishingPlatform() {
      return null;
    }

    public LockssDaemon getDaemon() {
      return null;
    }

    public List getSupportedTitles() {
      return null;
    }

    public TitleConfig getTitleConfig(String title) {
      return null;
    }

    public List getAuConfigDescrs() {
      return null;
    }

    public ConfigParamDescr findAuConfigDescr(String key) {
      return null;
    }

    public AuParamFunctor getAuParamFunctor() {
      return null;
    }

    public org.lockss.plugin.ArchivalUnit configureAu(Configuration config,
						      org.lockss.plugin.ArchivalUnit au)
	throws org.lockss.plugin.ArchivalUnit.ConfigurationException {
      return null;
    }

    public org.lockss.plugin.ArchivalUnit createAu(Configuration auConfig)
	throws org.lockss.plugin.ArchivalUnit.ConfigurationException {
      return null;
    }

    public Collection getAllAus() {
      return null;
    }

    public Object newAuxClass(String className, Class expectedType) {
      return null;
    }

    public CacheResultMap getCacheResultMap() {
      return new HttpResultMap();
    }

    public String getDefaultArticleMimeType() {
      return null;
    }

    public MimeTypeMap getMimeTypeMap() {
      return null;
    }

    public org.lockss.extractor.ArticleMetadataExtractor
	getArticleMetadataExtractor(MetadataTarget target,
				    org.lockss.plugin.ArchivalUnit au) {
      return null;
    }

    public ArticleIteratorFactory getArticleIteratorFactory() {
      return null;
    }

    public FileMetadataExtractor
      getFileMetadataExtractor(MetadataTarget target,
			       String contentType,
			       org.lockss.plugin.ArchivalUnit au) {
      return null;
    }

    public boolean isBulkContent() {
      return false;
    }
  }

  /**
   * Base class for test <code>CachedUrl</code>s.  Default methods do nothing
   * or return constants.
   */
  public static class CachedUrl implements org.lockss.plugin.CachedUrl {

    protected CachedUrl() {
    }

    public String toString() {
      return "[NullPlugin.CachedUrl]";
    }

    public org.lockss.plugin.ArchivalUnit getArchivalUnit() {
      return null;
    }

    public String getUrl() {
      return null;
    }

    public void setOption(String option, String val) {
    }

    public boolean hasContent() {
      return false;
    }

    public boolean isLeaf() {
      return true;
    }

    public int getType() {
      return CachedUrlSetNode.TYPE_CACHED_URL;
    }

    public org.lockss.plugin.CachedUrl getCuVersion(int version) {
      throw new UnsupportedOperationException("Not implemented");
    }

    public org.lockss.plugin.CachedUrl[] getCuVersions() {
      return new org.lockss.plugin.CachedUrl[0];
    }

    public org.lockss.plugin.CachedUrl[] getCuVersions(int maxVersions) {
      return new org.lockss.plugin.CachedUrl[0];
    }

    public int getVersion() {
      return 1;
    }

    public InputStream getUnfilteredInputStream() {
      return new StringInputStream("");
    }

    public InputStream getUnfilteredInputStream(HashedInputStream.Hasher hasher) {
      return new HashedInputStream(new StringInputStream(""), hasher);
    }

    public InputStream getUncompressedInputStream() {
      return getUnfilteredInputStream();
    }

    public InputStream getUncompressedInputStream(HashedInputStream.Hasher
						  hasher) {
      return getUnfilteredInputStream(hasher);
    }

    public InputStream openForHashing() {
      return getUnfilteredInputStream();
    }

    public InputStream openForHashing(HashedInputStream.Hasher hasher) {
      return new HashedInputStream(getUnfilteredInputStream(), hasher);
    }

    public Reader openForReading() {
      throw new UnsupportedOperationException("Not implemented");
    }

    public long getContentSize() {
      return 0;
    }

    public String getContentType(){
      return null;
    }

    public String getEncoding(){
      return null;
    }

    public LinkRewriterFactory getLinkRewriterFactory() {
      return null;
    }

    public FileMetadataExtractor getFileMetadataExtractor(MetadataTarget target) {
      return null;
    }

    public CIProperties getProperties() {
      return new CIProperties();
    }

    public void addProperty(String key, String value) {
    }


    public void release() {
    }

    public void delete() throws UnsupportedOperationException, IOException {
    }

    public CachedUrl getArchiveMemberCu(ArchiveMemberSpec ams) {
      return null;
    }

    @Override
    public boolean isArchiveMember() {
      return false;
    }
  }

  /**
   * Base class for test <code>UrlCacher</code>s.  Default methods do nothing
   * or return constants.
   */
  public static class UrlCacher implements org.lockss.plugin.UrlCacher {
    private String url;
    private String contents = null;
    private CIProperties props = new CIProperties();

    protected UrlCacher() {
    }

    public String getUrl() {
      return null;
    }

    public org.lockss.plugin.ArchivalUnit getArchivalUnit() {
      return null;
    }
    /** @deprecated */
    public org.lockss.plugin.CachedUrlSet getCachedUrlSet() {
      return null;
    }

    public String toString() {
      return "[NullPlugin.UrlCacher]";
    }

    public org.lockss.plugin.CachedUrl getCachedUrl() {
      return new CachedUrl();
    }

    public boolean shouldBeCached() {
      return false;
    }

    public void setFetchFlags(BitSet fetchFlags) {
    }

    public BitSet getFetchFlags() {
      return new BitSet();
    }

    public void setRequestProperty(String key, String value) {
    }

    public void setRedirectScheme(RedirectScheme scheme) {
    }

    public void setWatchdog(LockssWatchdog wdog) {
    }
    
    public LockssWatchdog getWatchdog() {
      return null;
    }

    public void setPreviousContentType(String previousContentType) {
    }

    public void setCrawlRateLimiter(CrawlRateLimiter crl) {
    }

    public void storeContent() throws IOException {
    }

    public void storeContent(InputStream input,
			     CIProperties headers) throws IOException {
    }

    public InputStream getUncachedInputStream() {
      return new StringInputStream("");
    }

    public CIProperties getUncachedProperties() {
      return new CIProperties();
    }

    public void reset() {
    }

    public void setConnectionPool(LockssUrlConnectionPool connectionPool) {
      throw new UnsupportedOperationException();
    }

    public void setLocalAddress(IPAddr addr) {
      throw new UnsupportedOperationException();
    }

    public void setProxy(String proxyHost, int proxyPort) {
      throw new UnsupportedOperationException();
    }

    public CacheException getInfoException() {
      throw new UnsupportedOperationException();
    }

    public void setPermissionMapSource(PermissionMapSource source) {
      throw new UnsupportedOperationException();
    }

    public void setRedirectUrls(List<String> redirectUrls) {
      throw new UnsupportedOperationException();
    }
    
    public void setFetchUrl(String fetchUrl) {
      throw new UnsupportedOperationException();
    }

  }

  /**
   * Base class for test <code>CachedUrlSet</code>s.  Default methods do
   * nothing or return constants or empty enumerations.
   */
  public static class CachedUrlSet implements org.lockss.plugin.CachedUrlSet {

    public String toString() {
      return "[NullPlugin.CachedUrlSet]";
    }

    public CachedUrlSetSpec getSpec() {
      return null;
    }

    public org.lockss.plugin.ArchivalUnit getArchivalUnit() {
      return null;
    }

    public void storeActualHashDuration(long elapsed, Exception err) {
    }

    public void setExcludeFilesUnchangedAfter(long date) {
    }

    public Iterator flatSetIterator() {
      return null;
    }

    public Iterator treeSetIterator() {
      return null;
    }

    public Iterator contentHashIterator() {
      return null;
    }

    public CuIterator getCuIterator() {
      return null;
    }

    public CuIterable getCuIterable() {
      return new CuIterable() {
	protected CuIterator makeIterator() {
	  return getCuIterator();
	}};
    }

    public CuIterator archiveMemberIterator() {
      return null;
    }

    public boolean isLeaf() {
      return false;
    }

    public int getType() {
      return CachedUrlSetNode.TYPE_CACHED_URL_SET;
    }

    public org.lockss.daemon.CachedUrlSetHasher
      getContentHasher(MessageDigest digest) {
      return new CachedUrlSetHasher();
    }

    public org.lockss.daemon.CachedUrlSetHasher
      getNameHasher(MessageDigest digest) {
      return new CachedUrlSetHasher();
    }

    public long estimatedHashDuration() {
      return 1000;
    }

    public boolean hasContent() {
      return false;
    }

    public boolean containsUrl(String url) {
      return false;
    }

    public int hashCode() {
      return 0;
    }

    public String getUrl() {
      return "null";
    }

    public int cusCompare(org.lockss.plugin.CachedUrlSet cus2) {
      return -1;
    }
  }

  public static class ArchivalUnit
    implements org.lockss.plugin.ArchivalUnit {


    public void setConfiguration(Configuration config) {
    }

    public Configuration getConfiguration() {
      return null;
    }

    public org.lockss.plugin.CachedUrlSet makeCachedUrlSet(CachedUrlSetSpec spec) {
      return null;
    }

    public org.lockss.plugin.CachedUrl makeCachedUrl(String url) {
      return null;
    }

    public org.lockss.plugin.UrlCacher makeUrlCacher(String url) {
      return null;
    }

    public org.lockss.plugin.CachedUrlSet getAuCachedUrlSet() {
      return null;
    }

    public List<Pattern> makeExcludeUrlsFromPollsPatterns() {
      return null;
    }

    public PatternStringMap makeUrlMimeTypeMap() {
      return PatternStringMap.EMPTY;
    }

    public PatternStringMap makeUrlMimeValidationMap() {
      return PatternStringMap.EMPTY;
    }

    public AuCacheResultMap makeAuCacheResultMap()
        throws ArchivalUnit.ConfigurationException {
      return AuHttpResultMap.DEFAULT;
    }

    public PatternFloatMap makeUrlPollResultWeightMap() {
      return null;
    }

    public List<Pattern> makeNonSubstanceUrlPatterns() {
      return null;
    }

    public List<Pattern> makeSubstanceUrlPatterns() {
      return null;
    }

    public SubstancePredicate makeSubstancePredicate() {
      return null;
    }

    public List<Pattern> makePermittedHostPatterns() {
      return null;
    }

    public List<Pattern> makeRepairFromPeerIfMissingUrlPatterns() {
      return null;
    }

    public String getPerHostPermissionPath() {
      return null;
    }

    public List<String> getHttpCookies() {
      return Collections.EMPTY_LIST;
    }

    public List<String> getHttpRequestHeaders() {
      return Collections.EMPTY_LIST;
    }

    public boolean shouldBeCached(String url) {
      return false;
    }

    public boolean isLoginPageUrl(String url) {
      return false;
    }

    public String siteNormalizeUrl(String url) {
      return url;
    }

    public Collection getUrlStems() {
      return Collections.EMPTY_LIST;
    }

    public org.lockss.plugin.Plugin getPlugin() {
      return null;
    }

    public String getPluginId() {
      return "null_plugin_id";
    }

    public String getAuId() {
      return "null_au_id";
    }

    public String getName() {
      return "null_name";
    }

    public TitleConfig getTitleConfig() {
      return null;
    }

    public TdbAu getTdbAu() {
      return null;
    }

    public void pauseBeforeFetch(String previousContentType) {
    }

    public RateLimiter findFetchRateLimiter() {
      return RateLimiter.UNLIMITED;
    }

    public String getFetchRateLimiterKey() {
      return null;
    }

    public RateLimiterInfo getRateLimiterInfo() {
      return null;
    }

    public int hashCode() {
      return 0;
    }

    public List getNewContentCrawlUrls() {
      return null;
    }

    public boolean shouldCrawlForNewContent(AuState aus) {
      return false;
    }

    public boolean shouldCallTopLevelPoll(AuState aus) {
      return false;
    }

    public LinkExtractor getLinkExtractor(String mimeType) {
      throw new UnsupportedOperationException("not implemented");
    }

    public FilterRule getFilterRule(String mimeType) {
      throw new UnsupportedOperationException("Not implemented");
    }

    public FilterFactory getHashFilterFactory(String mimeType) {
      throw new UnsupportedOperationException("Not implemented");
    }

    public FilterFactory getCrawlFilterFactory(String mimeType) {
      throw new UnsupportedOperationException("Not implemented");
    }

    public LinkRewriterFactory getLinkRewriterFactory(String mimeType) {
      throw new UnsupportedOperationException("Not implemented");
    }

    public ContentValidatorFactory getContentValidatorFactory(String
							      contentType){
      throw new UnsupportedOperationException("Not implemented");
    }

    public Iterator<ArticleFiles> getArticleIterator() {
      throw new UnsupportedOperationException("Not implemented");
    }

    public Iterator<ArticleFiles> getArticleIterator(MetadataTarget target) {
      throw new UnsupportedOperationException("Not implemented");
    }

    public FileMetadataExtractor getFileMetadataExtractor(MetadataTarget target,
							  String contentType) {
      return null;
    }

    public TypedEntryMap getProperties() {
      return null;
    }

    public Comparator<CrawlUrl> getCrawlUrlComparator() {
      return null;
    }

    public List<String> getAuFeatureUrls(String auFeature) {
      return null;
    }

    public boolean isBulkContent() {
      return false;
    }

    public ArchiveFileTypes getArchiveFileTypes() {
      return null;
    }

    public org.lockss.plugin.UrlCacher makeUrlCacher(UrlData ud) {
      return null;
    }

    public CrawlSeed makeCrawlSeed(Crawler.CrawlerFacade crawlFacade) {
      return null;
    }

    public UrlFetcher makeUrlFetcher(CrawlerFacade facade, String url) {
      return null;
    }

    public boolean inCrawlWindow() {
      return false;
    }

    public List<PermissionChecker> makePermissionCheckers() {
      return null;
    }

    public Collection<String> getStartUrls() {
      return null;
    }

    public Collection<String> getAccessUrls() {
      return null;
    }

    public Collection<String> getPermissionUrls() {
      return null;
    }


    public int getRefetchDepth() {
      return 0;
    }

    public LoginPageChecker getLoginPageChecker() {
      return null;
    }

    public String getCookiePolicy() {
      return null;
    }

    public boolean shouldRefetchOnCookies() {
      return false;
    }

    public CrawlWindow getCrawlWindow() {
      return null;
    }

    public UrlConsumerFactory getUrlConsumerFactory() {
      return null;
    }

    public boolean storeProbePermission() {
      return false;
    }

    public boolean sendReferrer() {
      return true;
    }
  }

  /**
   * Base class for test <code>CachedUrlSetHasher</code>s.  Default methods
   * do nothing or return constants.
   */
  public static class CachedUrlSetHasher
    implements org.lockss.daemon.CachedUrlSetHasher {

    public void setFiltered(boolean val) {
    }

    public org.lockss.plugin.CachedUrlSet getCachedUrlSet() {
      return null;
    }

    public long getEstimatedHashDuration() {
      return 0;
    }

    public void storeActualHashDuration(long elapsed, Exception err) {
    }

    public String typeString() {
      return null;
    }

    public MessageDigest[] getDigests() {
      return null;
    }

    public boolean finished() {
      return false;
    }

    public void abortHash() {
    }

    public int hashStep(int numBytes) {
      return 0;
    }
  }
}
