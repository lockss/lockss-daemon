/*
 * $Id: StaticContentPlugin.java,v 1.4 2003-04-17 00:55:50 troberts Exp $
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

package org.lockss.test;

import java.io.*;
import java.util.*;
import java.net.*;
import java.security.MessageDigest;
import org.lockss.app.*;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.state.*;
import org.lockss.test.*;

import java.math.BigInteger;

/**
 * Minimal fully functional plugin capable of serving a little static content.
 */
public class StaticContentPlugin extends BasePlugin implements PluginTestable {
  static Logger log = Logger.getLogger("StaticContentPlugin");

  Map cuMap = new HashMap();

  public StaticContentPlugin() {
  }

  public String getVersion() {
    return "42";
  }

  public List getSupportedAUNames() {
    return null;
  }

  public List getAUConfigProperties() {
    return null;
  }

  public ArchivalUnit createAU(Configuration auConfig)
      throws ArchivalUnit.ConfigurationException {
    return new SAU(this);
  }

  public void registerArchivalUnit(ArchivalUnit au) {
    aus.add(au);
  }

  public void unregisterArchivalUnit(ArchivalUnit au) {
    aus.remove(au);
  }
  
  public Collection getDefiningConfigKeys() {
    return Collections.EMPTY_LIST;
  }

  public class SAU extends BaseArchivalUnit {

    protected SAU(Plugin myPlugin) {
      super(myPlugin);
    }

    public void setConfiguration(Configuration config) 
	throws ConfigurationException {
      super.setConfiguration(config);
    }

    public String getName() {
      return "Static Content AU";
    }

    public boolean shouldBeCached(String url) {
      return true;
    }

    public List getNewContentCrawlUrls() {
      return null;
    }

    public CachedUrlSet makeCachedUrlSet(CachedUrlSetSpec cuss) {
      return new SCUS(this, cuss);
    }

    public CachedUrlSet cachedUrlSetFactory(ArchivalUnit owner,
					    CachedUrlSetSpec cuss) {
      return null;
    }

    public CachedUrl cachedUrlFactory(CachedUrlSet owner, String url) {
      return null;
    }

    public UrlCacher urlCacherFactory(CachedUrlSet owner, String url) {
      return null;
    }

    /**
     * Create a CU with content and store it in AU
     * @param owner the CUS owner
     * @param url the url
     * @param type the type
     * @param contents the contents
     */
    public void storeCachedUrl(CachedUrlSet owner, String url,
				String type, String contents) {
      SCU scu = new SCU(owner, url, type, contents);
      cuMap.put(scu.getUrl(), scu);
    }

    public void storeCachedUrl(String url, String type, String contents) {
      storeCachedUrl(null, url, type, contents);
    }

    public String toString() {
      return "[sau: " + cuMap + "]";
    }
  }

  public class SCU extends BaseCachedUrl {
    private String contents = null;
    private Properties props = new Properties();

    public SCU(CachedUrlSet owner, String url) {
      super(owner, url);
    }

    /**
     * Create a CachedUrl with content
     * @param owner the CUS owner
     * @param url the url
     * @param type the type
     * @param contents the contents
     */
    public SCU(CachedUrlSet owner, String url,
	       String type, String contents) {
      this(owner, url);
      setContents(contents);
      props.setProperty("Content-Type", type);
    }

    private void setContents(String s) {
      contents = s;
      props.setProperty("Content-Length", ""+s.length());
    }

    public String getUrl() {
      return url;
    }

    public boolean hasContent() {
      return contents != null;
    }

    public boolean isLeaf() {
      return true;
    }

    public InputStream openForReading() {
      return new StringInputStream(contents);
    }

    public InputStream openForHashing() {
      return openForReading();
    }

    public byte[] getContentSize() {
      return (new BigInteger(
			     Integer.toString(contents.length()))).toByteArray();
    }

    public Properties getProperties() {
      return props;
    }
  }

  class SCUS extends BaseCachedUrlSet {
    public SCUS(ArchivalUnit owner, CachedUrlSetSpec spec) {
      super(owner, spec);
    }

    public void storeActualHashDuration(long elapsed, Exception err) {
    }

    public Iterator flatSetIterator() {
      return null;
    }

    public Iterator contentHashIterator() {
      return null;
    }

    public boolean isLeaf() {
      return false;
    }

    public CachedUrlSetHasher getContentHasher(MessageDigest hasher) {
      return null;
    }

    public CachedUrlSetHasher getNameHasher(MessageDigest hasher) {
      return null;
    }

    public long estimatedHashDuration() {
      return 1000;
    }


    public CachedUrl makeCachedUrl(String url) {
      CachedUrl res = (CachedUrl)cuMap.get(url);;
      log.debug("makeCachedUrl(" + url + ") = " + res);
      return (CachedUrl)cuMap.get(url);
    }

    public org.lockss.plugin.UrlCacher makeUrlCacher(String url) {
      return null;
    }
  }

}
