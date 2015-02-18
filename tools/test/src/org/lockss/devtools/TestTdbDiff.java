/*
 * $Id$
 */

/*

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.devtools;
import java.io.*;
import java.util.*;

import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.config.Tdb;
import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.test.*;

/**
 * Tests {@link TdbDiff} class.
 */
public class TestTdbDiff extends LockssTestCase {
  MockLockssDaemon theDaemon = null;
  PluginManager pluginMgr = null;
  ConfigManager configMgr = null;
  
  @Override
  public void setUp() throws Exception {
    super.setUp();

    theDaemon = getMockLockssDaemon();
    pluginMgr = theDaemon.getPluginManager();
    configMgr = theDaemon.getConfigManager();
  }

  @Override
  public void tearDown() throws Exception {
    pluginMgr.stopService();
    theDaemon.stopDaemon();
    super.tearDown();
  }
  
  /**
   * Tests correct creation of TdbDiff instance
   */
  @SuppressWarnings("unused")
  public void testTdbDiffNewInstance() {
    Tdb tdb1 = new Tdb();
    Tdb tdb2 = new Tdb();
    try {
      TdbDiff tdbDiff = new TdbDiff(pluginMgr, tdb1, tdb2);
    } catch (Throwable ex) {
      fail("Failed to create TdbDiff instance", ex);
    }
  }
  
  /**
   * Tests argument validation with no PluginManager
   */
  @SuppressWarnings("unused")
  public void testTdbDiffNewInstanceNoPluginMgr() {
    Tdb tdb1 = new Tdb();
    Tdb tdb2 = new Tdb();
    try {
      TdbDiff tdbDiff = new TdbDiff(null, tdb1, tdb2);
      fail("Did not throw exception for no plugin manager");
    } catch (Throwable ex) {
    }
  }
  
  /**
   * Tests argument validation with no TDBs
   */
  @SuppressWarnings("unused")
  public void testTdbDiffNewInstanceNoTdb() {
    Tdb tdb1 = new Tdb();
    Tdb tdb2 = new Tdb();
    try {
      TdbDiff tdbDiff = new TdbDiff(pluginMgr, null, tdb2);
      fail("Did not throw exception for no tdb1");
    } catch (Throwable ex) {
    }
    try {
      TdbDiff tdbDiff = new TdbDiff(pluginMgr, tdb1, null);
      fail("Did not throw exception for no tdb2");
    } catch (Throwable ex) {
    }
  }
  
  /** 
   * Test printing of differences between TDBs.
   * @todo need to beef up this test
   */
  public void testTdbDiffPrint() {
    Tdb tdb1 = new Tdb();
    Tdb tdb2 = new Tdb();
    TdbDiff tdbDiff = new TdbDiff(pluginMgr, tdb1, tdb2);
    tdbDiff.showFields = true;
    tdbDiff.showAll = true;
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(bos);
    tdbDiff.printTdbDiffsByAu(ps);
    String output = bos.toString();
    assertEquals(0, output.length());
  }
}
