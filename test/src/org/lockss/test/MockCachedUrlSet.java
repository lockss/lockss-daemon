package org.lockss.test;

import java.util.Enumeration;
import java.util.Vector;
import java.util.List;
import java.util.Iterator;
import java.security.MessageDigest;
import org.lockss.daemon.*;
import org.lockss.test.MockCachedUrlSetSpec;


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
 * This is a mock version of <code>CachedUrlSet</code> used for testing
 *
 * @author  Thomas S. Robertson
 * @version 0.0
 */

public class MockCachedUrlSet implements CachedUrlSet{

  private Vector urls = null;

  private MockCachedUrlSet(){
    urls = new Vector();
  }

  /**
   * Make a new MockCachedUrlSet object with a list populated with
   * the urls specified in rootUrls (and no reg expressions)
   *
   * @param rootUrls list of string representation of the urls to 
   * add to the new MockCachedUrlSet's list
   * @return MockCachedUrlSet with urls in rootUrls in its list
   */
  public static MockCachedUrlSet createFromListOfRootUrls(String[] rootUrls){
    MockCachedUrlSet cus = new MockCachedUrlSet();
    if (rootUrls != null){
      for (int ix = 0; ix < rootUrls.length; ix++){
	String curUrl = (String)rootUrls[ix];
	CachedUrlSetSpec spec = new MockCachedUrlSetSpec(curUrl, null);
	cus.addToList(spec);
      }
    }
    return cus;
  }



  public void addToList(CachedUrlSetSpec spec){
    urls.add(spec);
  }

  public boolean removeFromList(CachedUrlSetSpec spec){
    return false;
  }

  public boolean memberOfList(CachedUrlSetSpec spec){
    return false;
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
    return null;
  }

  public UrlCacher makeUrlCacher(String url){
    return null;
  }

  //methods used to generate proper mock objects

}
