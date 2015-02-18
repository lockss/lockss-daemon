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
 * Options to control filtering by {@link CuIterable} and {@link
 * CuIterator}.
 */
public class CuIterOptions {
  public static final String PREFIX = Configuration.PREFIX + "cuIterator.";

  /** Include in the iterator only CUs that have content. */
  static final String PARAM_CONTENT_ONLY = PREFIX + "contentOnly";
  static final boolean DEFAULT_CONTENT_ONLY = true;

  /** Include in the iterator only CUs whose URL matches the crawl rules
   * (which may have changed since files were collected) */
  static final String PARAM_INCLUDED_ONLY = PREFIX + "includedOnly";
  static final boolean DEFAULT_INCLUDED_ONLY = true;


  private boolean contentOnly;
  private boolean includedOnly;

  CuIterOptions() {
    setDefaultConfig();
  }

  CuIterOptions setDefaultConfig() {
    return setConfig(ConfigManager.getCurrentConfig());
  }

  CuIterOptions setConfig(Configuration config) {
    contentOnly = config.getBoolean(PARAM_CONTENT_ONLY, DEFAULT_CONTENT_ONLY);
    includedOnly = config.getBoolean(PARAM_INCLUDED_ONLY, DEFAULT_INCLUDED_ONLY);
    return this;
  }

  CuIterOptions setContentOnly(boolean val) {
    contentOnly = val;
    return this;
  }

  CuIterOptions setIncludedOnly(boolean val) {
    includedOnly = val;
    return this;
  }

  boolean isContentOnly() {
    return contentOnly;
  }

  boolean isIncludedOnly() {
    return includedOnly;
  }

}
