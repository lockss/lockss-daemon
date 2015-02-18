/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin;

import java.util.*;

import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.daemon.*;


/**
 * Iterator over a set of CachedUrls.
 */
public abstract class CuIterator implements Iterator<CachedUrl> {

  protected CuIterOptions options;

  protected CuIterator setOptions(CuIterOptions options) {
    this.options = options;
    return this;
  }

  /** Set the options from the Configuration */
  public CuIterator setConfig(Configuration config) {
    getOptions().setConfig(config);
    return this;
  }

  /** Return only CachedUrls for which hasContent() is true. */
  public CuIterator setContentOnly(boolean val) {
    getOptions().setContentOnly(val);
    return this;
  }

  /** Return only CachedUrls whose URL is included by the AU's crawl
   * rules. */
  public CuIterator setIncludedOnly(boolean val) {
    getOptions().setIncludedOnly(val);
    return this;
  }

  /** Return the count of CUs excluded by the iterator (due to crawl rules
   * or global exclude patterns). */
  public abstract int getExcludedCount();

  protected CuIterOptions getOptions() {
    if (options == null) {
      options = new CuIterOptions();
    }
    return options;
  }

  /** Create and return a CuIterator over the CachedUrlSet */
  public static CuIterator forCus(CachedUrlSet cus) {
    return new CuContentIterator(cus.contentHashIterator());
  }

}
