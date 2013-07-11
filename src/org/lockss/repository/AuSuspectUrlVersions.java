/*
 * $Id: AuSuspectUrlVersions.java,v 1.1 2013-07-11 20:38:39 dshr Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.repository;

import java.util.*;
import org.lockss.util.TimeBase;

/**
 * Instances represent the set of versions of urls in an AU that
 * have been marked suspect because they failed a local hash
 * verification, meaning that either the url's content or its
 * stored hash is corrupt.
 */
public class AuSuspectUrlVersions {
  public class SuspectUrlVersion {
    private final String url;
    private final int version;
    private final long created;

    protected SuspectUrlVersion(String url, int version) {
      this.url = url;
      this.version = version;
      this.created = TimeBase.nowMs();
    }
    protected String getUrl() {
      return url;
    }
    protected int getVersion() {
      return version;
    }
    protected long getCreated() {
      return created;
    }
    public int hashCode() {
      return url.hashCode() + version;
    }
    public boolean equals(Object obj) {
      if (obj instanceof SuspectUrlVersion) {
	SuspectUrlVersion suv = (SuspectUrlVersion) obj;
	return (url.equals(suv.getUrl()) && version == suv.getVersion());
      }
      return false;
    }
  }
  private Set<SuspectUrlVersion> suspectVersions =
    new HashSet<SuspectUrlVersion>();

  protected AuSuspectUrlVersions() {
    /* XXX load suspectVersions from underlying file XXX */
  }

  /**
   * Return true if the version of the url has been marked suspect.
   * @return true if version of url has been marked suspect
   */
  public boolean isSuspect(String url, int version) {
    return suspectVersions.contains(new SuspectUrlVersion(url, version));
  }

  /**
   * Mark the version of the url as suspect
   */
  public void markAsSuspect(String url, int version) {
    suspectVersions.add(new SuspectUrlVersion(url, version));
    /* XXX write through to underlying file XXX */
  }
}
