package org.lockss.plugin;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import java.util.Vector;
import java.util.Iterator;
import java.util.Enumeration;
import java.security.MessageDigest;
import gnu.regexp.RE;
import gnu.regexp.REException;
import org.lockss.daemon.CachedUrl;
import org.lockss.daemon.CachedUrlSet;
import org.lockss.daemon.CachedUrlSetHasher;
import org.lockss.daemon.CachedUrlSetSpec;
import org.lockss.crawler.Crawler;

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

/**
 * This is a first cut at making a HighWire plugin
 *
 * @author  Thomas S. Robertson
 * @version 0.0
 */
 
public class HighWirePlugin implements CachedUrlSet{



  private String url;
  private Vector urls;
  private String urlRoot; //url root for the web page, eg. http://www.bmj.org
  private int volume;  //volume that this plugin is for
  private Vector rules;

  private HighWirePlugin(){
    urls = new Vector();
  }

  /**
   * Standard constructor for HighWirePlugin.  
   *
   * @param urlRoot root that all URLs which this plugin will crawl must have
   * (ie, http://shadow3.stanford.edu)
   * @param volume journal volume number that we are crawling (used with url 
   * root to generate the rules)
   */
  public HighWirePlugin(String urlRoot, int volume){
    this();
    this.urlRoot = urlRoot;
    this.volume = volume;
    try{
      rules = makeRules(urlRoot, volume);
    }
    catch (REException ree){
      ree.printStackTrace();
    }
  }

  private static Vector makeRules(String urlRoot, int volume)
      throws REException{
    Vector rules = new Vector();

    rules.add(new CrawlRule(urlRoot+"/lockss-volume"+volume+".shtml", true));
    rules.add(new CrawlRule(".*ck=nck.*", false));
    rules.add(new CrawlRule(".*ck=nck.*", false));
    rules.add(new CrawlRule(".*adclick.*", false));
    rules.add(new CrawlRule(".*/cgi/mailafriend.*", false));
    rules.add(new CrawlRule(".*/content/current/.*", true));
    rules.add(new CrawlRule(".*/content/vol"+volume+"/.*", true));
    rules.add(new CrawlRule(".*/cgi/content/.*/"+volume+"/.*", true));
    rules.add(new CrawlRule(".*/cgi/reprint/"+volume+"/.*", true));
    rules.add(new CrawlRule(".*/icons.*", true));
    rules.add(new CrawlRule(".*/math.*", true));
    rules.add(new CrawlRule("http://.*/.*/.*", false));
    return rules;
  }

  public String getUrlRoot(){
    return urlRoot;
  }
			    
  public Enumeration getRules(){
    return rules.elements();
  }
			    




  //CacheUrlSet methods
  public void addToList(CachedUrlSetSpec spec){
    urls.add(spec);
  }

  public boolean removeFromList(CachedUrlSetSpec spec){
    return urls.remove(spec);
  }

  public boolean memberOfList(CachedUrlSetSpec spec){
    return urls.contains(spec);
  }

  public Enumeration listEnumeration(){
    if (urls == null){
      return null;
    }
    return urls.elements();
  }

  public boolean memberOfSet(String url){
    return false;
  }


    // Methods used by the poller

  public CachedUrlSetHasher getContentHasher(MessageDigest hasher){
    return null;
  }

  public CachedUrlSetHasher getNameHasher(MessageDigest hasher){
    return null;
  }

  public Enumeration flatEnumeration(){
    return null;
  }

  public Enumeration treeEnumeration(){
    return null;
  }

  public long hashDuration(){
    return 0;
  }

  public long duration(long elapsed, boolean success){
    return 0;
  }

  // Methods used by the crawler

  public CachedUrl makeCachedUrl(String url){
    return new HighWireCachedUrl(url, this);
  }

  //other

}
