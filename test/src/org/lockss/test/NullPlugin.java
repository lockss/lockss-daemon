/*
 * $Id: NullPlugin.java,v 1.39 2003-07-30 05:36:52 tlipkis Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.*;
import java.net.*;
import java.security.MessageDigest;
import org.lockss.app.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.state.*;
import org.lockss.test.*;

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

    public String getPluginId() {
      return "NullPlugin";
    }

    public String getVersion() {
      return "NullVersion";
    }

    public String getPluginName() {
      return "Null Plugin";
    }

    public LockssDaemon getDaemon() {
      return null;
    }

    public List getSupportedTitles() {
      return null;
    }

    public Configuration getConfigForTitle(String title) {
      return null;
    }

    public List getAUConfigProperties() {
      return null;
    }

    public Collection getDefiningConfigKeys() {
      return Collections.EMPTY_LIST;
    }

    public org.lockss.plugin.ArchivalUnit configureAU(Configuration config,
						      org.lockss.plugin.ArchivalUnit au)
	throws org.lockss.plugin.ArchivalUnit.ConfigurationException {
      return null;
    }

    public org.lockss.plugin.ArchivalUnit createAU(Configuration auConfig)
	throws org.lockss.plugin.ArchivalUnit.ConfigurationException {
      return null;
    }

    public Collection getAllAUs() {
      return null;
    };
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

    public boolean hasContent() {
      return false;
    }

    public boolean isLeaf() {
      return true;
    }

    public int getType() {
      return CachedUrlSetNode.TYPE_CACHED_URL;
    }

    public InputStream openForReading() {
      return new StringInputStream("");
    }

    public InputStream openForHashing() {
      return openForReading();
    }

    public Reader getReader() {
      throw new UnsupportedOperationException("Not implemented");
    }

    public byte[] getUnfilteredContentSize() {
      return new byte[0];
    }

    public Properties getProperties() {
      return new Properties();
    }
  }

  /**
   * Base class for test <code>UrlCacher</code>s.  Default methods do nothing
   * or return constants.
   */
  public static class UrlCacher implements org.lockss.plugin.UrlCacher {
    private String url;
    private String contents = null;
    private Properties props = new Properties();

    protected UrlCacher() {
    }

    public String getUrl() {
      return null;
    }

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

    public void cache() throws IOException {
    }

    public void forceCache() throws IOException {
    }

    public void storeContent(InputStream input,
			     Properties headers) throws IOException {
    }

    public InputStream getUncachedInputStream() {
      return new StringInputStream("");
    }

    public Properties getUncachedProperties() {
      return new Properties();
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

    public Iterator flatSetIterator() {
      return null;
    }

    public Iterator treeSetIterator() {
      return null;
    }

    public Iterator contentHashIterator() {
      return null;
    }

    public boolean isLeaf() {
      return false;
    }

    public int getType() {
      return CachedUrlSetNode.TYPE_CACHED_URL_SET;
    }

    public org.lockss.daemon.CachedUrlSetHasher
      getContentHasher(MessageDigest hasher) {
      return new CachedUrlSetHasher();
    }

    public org.lockss.daemon.CachedUrlSetHasher
      getNameHasher(MessageDigest hasher) {
      return new CachedUrlSetHasher();
    }

    public long estimatedHashDuration() {
      return 1000;
    }

    public boolean hasContent() {
      return false;
    }

    public org.lockss.plugin.CachedUrl makeCachedUrl(String url) {
      return new CachedUrl();
    }

    public org.lockss.plugin.UrlCacher makeUrlCacher(String url) {
      return new UrlCacher();
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

    public org.lockss.plugin.CachedUrlSet getAUCachedUrlSet() {
      return null;
    }

    public CrawlSpec getCrawlSpec() {
      return null;
    }

    public boolean shouldBeCached(String url) {
      return false;
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

    public String getAUId() {
      return "null_au_id";
    }

    public String getName() {
      return "null_name";
    }

    public void pauseBeforeFetch() {
    }

    public long getFetchDelay() {
      return 0;
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

  }

  /**
   * Base class for test <code>CachedUrlSetHasher</code>s.  Default methods
   * do nothing or return constants.
   */
  public static class CachedUrlSetHasher
    implements org.lockss.daemon.CachedUrlSetHasher {

    public boolean finished() {
      return false;
    }

    public int hashStep(int numBytes) {
      return 0;
    }
  }
}
