/*
 * $Id: BaseCachedUrlSet.java,v 1.2 2002-11-05 01:46:50 aalto Exp $
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

package org.lockss.daemon;

//  import java.io.*;
import java.util.*;
//  import java.net.*;
//  import java.security.MessageDigest;
//  import org.lockss.daemon.*;
//  import org.lockss.plugin.*;
//  import org.lockss.test.*;

/** Abstract base class for CachedUrlSets.
 * Plugins may extend this to get some common CachedUrlSet functionality.
 */
public abstract class BaseCachedUrlSet implements CachedUrlSet {
  protected ArchivalUnit au;
  protected CachedUrlSetSpec spec;

  /** Must invoke this constructor in plugin subclass. */
  public BaseCachedUrlSet(ArchivalUnit owner, CachedUrlSetSpec spec) {
    this.spec = spec;
    this.au = owner;
  }

  /** Return the CachedUrlSetSpec */
  public CachedUrlSetSpec getSpec() {
    return spec;
  }

  /** Return the enclosing ArchivalUnit */
  public ArchivalUnit getArchivalUnit() {
    return au;
  }

  /** Return true if content for the url is present in the CachedUrlSet */
  public boolean isCached(String url) {
    CachedUrl cu = makeCachedUrl(url);
    return cu == null ? false : cu.exists();
  }

  /** Return true if the url falls within the scope of this CachedUrlSet,
   * whether it is present in the cache or not
   */
  public boolean containsUrl(String url) {
    return (null != makeCachedUrl(url));
  }
}
