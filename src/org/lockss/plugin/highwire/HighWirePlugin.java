/*
 * $Id: HighWirePlugin.java,v 1.11 2002-11-06 00:10:06 aalto Exp $
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

package org.lockss.plugin.highwire;

import java.io.*;
import java.net.*;
import java.util.*;
import java.security.MessageDigest;
import gnu.regexp.*;
import org.lockss.daemon.*;
import org.lockss.crawler.*;
import org.lockss.util.*;
import org.lockss.plugin.*;

/**
 * This is a first cut at making a HighWire plugin
 *
 * @author  Thomas S. Robertson
 * @version 0.0
 */

public class HighWirePlugin extends BaseArchivalUnit {
  static public final String LOG_NAME = "HighWirePlugin";
  protected Logger logger = Logger.getLogger(LOG_NAME);

  /**
   * Standard constructor for HighWirePlugin.
   *
   * @param start URL to start crawl
   */
  public HighWirePlugin(String start) throws REException, MalformedURLException{
    super(makeCrawlSpec(start));
  }

  public CachedUrlSet cachedUrlSetFactory(ArchivalUnit owner,
					     CachedUrlSetSpec cuss) {
    return new HighWireCachedUrlSet(owner, cuss);
  }

  public CachedUrl cachedUrlFactory(CachedUrlSet owner, String url) {
    return new HighWireCachedUrl(owner, url);
  }

  public UrlCacher urlCacherFactory(CachedUrlSet owner, String url) {
    return new HighWireUrlCacher(owner, url);
  }

  private static CrawlSpec makeCrawlSpec(String start)
      throws REException, MalformedURLException {
    String prefix = UrlUtil.getUrlPrefix(start);
    int volume = getUrlVolumeNumber(start);

    CrawlRule rule = makeRules(prefix, volume);
    return new CrawlSpec(start, rule);
  }

  protected static int getUrlVolumeNumber(String urlStr) throws MalformedURLException{
    URL url = new URL(urlStr);
    String path = url.getPath();

    String volStr = "lockss-volume";
    int volStrIdx = path.indexOf(volStr);
    int startIdx =  volStrIdx + volStr.length();
    int endIdx = path.lastIndexOf(".");
    if (volStrIdx < 0 || endIdx < 0 || endIdx <= startIdx){
      return -1;
    }
    return Integer.parseInt(path.substring(startIdx, endIdx));
  }

  private static CrawlRule makeRules(String urlRoot, int volume)
      throws REException{
    List rules = new LinkedList();
    final int incl = CrawlRules.RE.MATCH_INCLUDE;
    final int excl = CrawlRules.RE.MATCH_EXCLUDE;
    String escapedRoot = StringUtil.escapeNonAlphaNum(urlRoot);

    rules.add(new CrawlRules.RE("^" + escapedRoot,
				CrawlRules.RE.NO_MATCH_EXCLUDE));
    rules.add(new CrawlRules.RE(escapedRoot+"/lockss-volume"+volume+".shtml",
				incl));
    rules.add(new CrawlRules.RE(".*ck=nck.*", excl));
    rules.add(new CrawlRules.RE(".*ck=nck.*", excl));
    rules.add(new CrawlRules.RE(".*adclick.*", excl));
    rules.add(new CrawlRules.RE(".*/cgi/mailafriend.*", excl));
    rules.add(new CrawlRules.RE(".*/content/current/.*", incl));
    rules.add(new CrawlRules.RE(".*/content/vol"+volume+"/.*", incl));
    rules.add(new CrawlRules.RE(".*/cgi/content/.*/"+volume+"/.*", incl));
    rules.add(new CrawlRules.RE(".*/cgi/reprint/"+volume+"/.*", incl));
    rules.add(new CrawlRules.RE(".*/icons.*", incl));
    rules.add(new CrawlRules.RE(".*/math.*", incl));
    rules.add(new CrawlRules.RE("http://.*/.*/.*", excl));
    return new CrawlRules.FirstMatch(rules);
  }

  public String getPluginId() {
    return "highwire";
  }
  public String getAUId() {
    try {
      String url = (String)getCrawlSpec().getStartingUrls().get(0);
      return Integer.toString(getUrlVolumeNumber(url));
    } catch (MalformedURLException ex) {
      return "null";
    }
  }
}
