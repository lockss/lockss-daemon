/*
 * $Id$
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.app.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.state.*;
import org.lockss.test.*;
import org.lockss.plugin.base.*;
import org.lockss.util.*;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;

/**
 * This is the test class for org.lockss.plugin.AuOrderComparator
 */
public class TestAuOrderComparator extends LockssTestCase {

  MockArchivalUnit makeMockAU(String name) {
    MockArchivalUnit au = new MockArchivalUnit();
    au.setName(name);
    return au;
  }

  public void testOrder() {
    MockArchivalUnit au1 = makeMockAU("The Definite Article volume 3");
    MockArchivalUnit au2 = makeMockAU("The Definite Article volume 1");
    MockArchivalUnit au3 = makeMockAU("Definite Article volume 2");
    MockArchivalUnit au4 = makeMockAU("The Definite Article volume 4");
    Set set = new TreeSet(new AuOrderComparator());
    set.add(au1);
    set.add(au2);
    set.add(au3);
    set.add(au4);
    assertIsomorphic(ListUtil.list(au2, au3, au1, au4), set);
  }
}
