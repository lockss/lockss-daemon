/*
 * $Id: GenericCachedUrlSetHasher.java,v 1.1 2002-10-23 23:55:41 aalto Exp $
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

package org.lockss.hasher;
import java.io.*;
import java.util.Iterator;
import java.security.MessageDigest;
import org.lockss.daemon.*;

/**
 * An generic implementation of the CachedUrlSetHasher
 *
 * @author  Emil Aalto
 * @version 0.0
 */
public class GenericCachedUrlSetHasher implements CachedUrlSetHasher {
  private CachedUrlSet owner;
  private MessageDigest hasher;
  private boolean isFinished;
  private boolean namesOnly;
  private Iterator urlIter;
  private CachedUrl currentUrl;
  private int byteIndex;

  public GenericCachedUrlSetHasher(CachedUrlSet owner, MessageDigest hasher, boolean namesOnly) {
    this.owner = owner;
    this.hasher = hasher;
    isFinished = false;
    this.namesOnly = namesOnly;
    urlIter = owner.leafIterator();
    byteIndex = 0;
    currentUrl = null;
  }

  public int hashStep(int numBytes) throws IOException {
    if (currentUrl==null) getNextUrl();
    if (isFinished) return 0;
    // hash numBytes from the file, or just its name
    // when done with all files, set isFinished=true

    return 0;
  }

  public boolean finished() {
    return isFinished;
  }

  private void getNextUrl() {
    if (!urlIter.hasNext()) {
      isFinished = true;
    } else {
      currentUrl = (CachedUrl)urlIter.next();
      byteIndex = 0;
    }
  }
}
