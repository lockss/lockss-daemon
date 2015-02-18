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
 * Base class for an Iterable over a set of {@link CachedUrl}s.  Optionally
 * excludes CUs that don't match the crawl rules.
 */
public abstract class CuIterable implements Iterable<CachedUrl> {
  protected CuIterOptions options;

  protected abstract CuIterator makeIterator();

  /** Create and set the options for a new CuIterator.  Subclasses must
   * implement {@link @makeIterator()} to create in instance of the desired
   * class. */
  public final CuIterator iterator() {
    CuIterator iter = makeIterator();
    if (options != null) {
      iter.setOptions(options);
    }
    return iter;
  }

  public CuIterOptions getOptions() {
    if (options == null) {
      options = new CuIterOptions();
    }
    return options;
  }

  protected CuIterable setOptions(CuIterOptions options) {
    this.options = options;
    return this;
  }

  /** Set the options from the Configuration */
  public CuIterable setConfig(Configuration config) {
    getOptions().setConfig(config);
    return this;
  }

  /** Return only CachedUrls for which hasContent() is true. */
  public CuIterable setContentOnly(boolean val) {
    getOptions().setContentOnly(val);
    return this;
  }

  /** Return only CachedUrls whose URL is included by the AU's crawl
   * rules. */
  public CuIterable setIncludedOnly(boolean val) {
    getOptions().setIncludedOnly(val);
    return this;
  }

}
