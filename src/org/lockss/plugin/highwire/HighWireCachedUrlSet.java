/*
 * $Id: HighWireCachedUrlSet.java,v 1.1 2002-10-16 04:57:03 tal Exp $
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
 
public class HighWireCachedUrlSet extends BaseCachedUrlSet {

  private String urlRoot; //url root for the web page, eg. http://www.bmj.org
  protected Logger logger = Logger.getLogger("HighWirePlugin");

  /**
   * Standard constructor for HighWireCachedUrlSet.  
   *
   * @param cuss specifies which part of the content is contained in the
   * CachedUrlSet.
   */
  public HighWireCachedUrlSet(ArchivalUnit owner, CachedUrlSetSpec cuss) {
    super(owner, cuss);
    this.urlRoot = (String)cuss.getPrefixList().get(0);;
  }

  /**
   * Standard constructor for HighWireCachedUrlSet.  
   *
   * @param urlPrefix prefix of all URLs in this CachedUrlSet
   */
  public HighWireCachedUrlSet(ArchivalUnit owner, String urlRoot)
      throws REException {
    this(owner, new RECachedUrlSetSpec(urlRoot));
  }

  public String getUrlRoot(){
    return urlRoot;
  }
			    

    // Methods used by the poller

  public CachedUrlSetHasher getContentHasher(MessageDigest hasher){
    return null;
  }

  public CachedUrlSetHasher getNameHasher(MessageDigest hasher){
    return null;
  }

  public Iterator flatSetIterator(){
    return null;
  }

  public Iterator treeSetIterator(){
    return null;
  }

  public Iterator leafIterator(){
    return null;
  }

  public long estimatedHashDuration(){
    return 0;
  }

  public void storeActualHashDuration(long elapsed, Exception err) {
  }

  // Methods used by the crawler

  public CachedUrl makeCachedUrl(String url){
    return new HighWireCachedUrl(this, url);
  }

  public UrlCacher makeUrlCacher(String url){
    return new HighWireUrlCacher(this, url);
  }

  //other

}
