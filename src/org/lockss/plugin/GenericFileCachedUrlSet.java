/*
 * $Id: GenericFileCachedUrlSet.java,v 1.1 2002-10-23 23:45:49 aalto Exp $
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

package org.lockss.plugin;

import java.io.*;
import java.util.*;
import java.security.MessageDigest;
import gnu.regexp.RE;
import gnu.regexp.REException;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.hasher.GenericCachedUrlSetHasher;

/**
 * This is the CachedUrlSet implementation for the SimulatedPlugin.
 *
 * @author  Emil Aalto
 * @version 0.0
 */

public abstract class GenericFileCachedUrlSet extends BaseCachedUrlSet {
  private long lastDuration = 0;
  private Exception lastException = null;

  public GenericFileCachedUrlSet(ArchivalUnit owner, CachedUrlSetSpec spec) {
    super(owner, spec);
  }

  public Iterator flatSetIterator() {
    // XXX implement correctly
    return null;
  }

  public Iterator treeSetIterator() {
    // XXX implement correctly
    return null;
  }

  public Iterator leafIterator() {
    // XXX implement correctly
    return null;
  }

  public CachedUrlSetHasher getContentHasher(MessageDigest hasher) {
    return new GenericCachedUrlSetHasher(this, hasher, false);
  }

  public CachedUrlSetHasher getNameHasher(MessageDigest hasher) {
    return new GenericCachedUrlSetHasher(this, hasher, true);
  }

  public void storeActualHashDuration(long elapsed, Exception err) {
    lastDuration = elapsed;
    lastException = err;
  }

  public long estimatedHashDuration() {
    if (lastDuration>0) return lastDuration;
    return 0;
  }

  public CachedUrl makeCachedUrl(String url) {
    return new GenericFileCachedUrl(this, url);
  }
}
