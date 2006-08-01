/*
 * $Id: TestBlackwellUrlNormalizer.java,v 1.1 2006-08-01 05:21:51 tlipkis Exp $
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

package org.lockss.plugin.blackwell;

import java.io.*;
import java.util.*;

import org.lockss.plugin.*;
import org.lockss.test.*;
import org.lockss.util.*;

/**
 * Test class for org.lockss.plugin.blackwell.BlackwellUrlNormalizer
 */
public class TestBlackwellUrlNormalizer extends LockssTestCase {

  UrlNormalizer norm;
  ArchivalUnit mau;

  public void setUp() {
    norm = new BlackwellUrlNormalizer();
    mau = new MockArchivalUnit();
  }

  void assertNorm(String exp, String url) {
    assertEquals(exp, norm.normalizeUrl(url, mau));
  }

  public void testUrl() {
    assertNorm("http://foo.bar/a.h", "http://foo.bar/a.h");
    assertNorm("http://foo.bar/a.h", "http://foo.bar/a.h?cookieSet=1");
    assertNorm("http://foo.bar/a.h?b=d", "http://foo.bar/a.h?b=d&cookieSet=1");
    assertNorm("http://foo.bar/a.h?b=d", "http://foo.bar/a.h?cookieSet=1&b=d");
  }
}
