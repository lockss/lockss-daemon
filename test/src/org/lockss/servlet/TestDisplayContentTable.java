/*
 * $Id TestDisplayContentTable.java 2012/12/03 14:52:00 rwincewicz $
 */

/*

 Copyright (c) 2012 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.servlet;

import java.util.*;
import org.lockss.plugin.*;
import org.lockss.protocol.MockIdentityManager;
import org.lockss.test.*;

/**
 *
 * @author rwincewicz
 */
public class TestDisplayContentTable extends LockssTestCase {
  
  MockArchivalUnit au1;
  MockArchivalUnit au2;
  MockArchivalUnit au3;
  MockArchivalUnit au4;
  MockArchivalUnit au5;
  
  private Collection createAuList() {
    MockLockssDaemon daemon = getMockLockssDaemon(); 
    MockIdentityManager idm = new MockIdentityManager();
    daemon.setIdentityManager(idm);
    ArrayList<ArchivalUnit> auList = new ArrayList<ArchivalUnit>();
    au1 = MockArchivalUnit.newInited(daemon);
    MockPlugin plugin1 = new MockPlugin(daemon);
    plugin1.setPluginName("B plugin");
    au1.setPlugin(plugin1);
    auList.add(au1);
    au2 = MockArchivalUnit.newInited(daemon);
    MockPlugin plugin2 = new MockPlugin(daemon);
    plugin2.setPluginName("D plugin");
    au2.setPlugin(plugin2);
    auList.add(au2);
    au3 = MockArchivalUnit.newInited(daemon);
    MockPlugin plugin3 = new MockPlugin(daemon);
    plugin3.setPluginName("A plugin");
    au3.setPlugin(plugin3);
    auList.add(au3);
    au4 = MockArchivalUnit.newInited(daemon);
    MockPlugin plugin4 = new MockPlugin(daemon);
    plugin4.setPluginName("C plugin");
    au4.setPlugin(plugin4);
    auList.add(au4);
    au5 = MockArchivalUnit.newInited(daemon);
    MockPlugin plugin5 = new MockPlugin(daemon);
    au5.setPlugin(plugin5);
    auList.add(au5);
    return auList;
  }
  
  public void testOrderAusByPlugin() {
    Collection<ArchivalUnit> aus = createAuList();
    assertNotNull(aus);
    TreeMap<Plugin, TreeSet<ArchivalUnit>> auList = DisplayContentTable.orderAusByPlugin(aus);
    assertNotNull(auList);
    Set auSet = auList.keySet();
    Object[] auArray = auSet.toArray();
    Plugin plugin1 = (Plugin) auArray[0];
    assertEquals("A plugin", plugin1.getPluginName());
    Plugin plugin2 = (Plugin) auArray[1];
    assertEquals("B plugin", plugin2.getPluginName());
    Plugin plugin3 = (Plugin) auArray[2];
    assertEquals("C plugin", plugin3.getPluginName());
    Plugin plugin4 = (Plugin) auArray[3];
    assertEquals("D plugin", plugin4.getPluginName());
    Plugin plugin5 = (Plugin) auArray[4];
    assertEquals("Mock Plugin", plugin5.getPluginName());
  }
  
  public void testOrderAusByPublisher() {
    Collection<ArchivalUnit> aus = createAuList();
    assertNotNull(aus);
  }
  
  public void testCleanName() {
    String name = "(T e& s &t)";
    String cleanedName = DisplayContentTable.cleanName(name);
    assertEquals("T_e_s_t", cleanedName);
  }
}
