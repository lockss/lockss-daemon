/*
 * $Id: TestTitleConfig.java,v 1.5 2005-01-19 04:16:34 tlipkis Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.daemon;

import java.util.*;

import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.test.*;
import org.lockss.util.*;

/**
 * This is the test class for org.lockss.daemon.TitleConfig
 */

public class TestTitleConfig extends LockssTestCase {

  public void setUp() {
  }

  public void testConstructors() {
    String id = "pid";
    String name = "foo 2";
    MockPlugin mp = new MockPlugin();
    mp.setPluginId(id);
    TitleConfig tc1 = new TitleConfig(name, mp);
    TitleConfig tc2 = new TitleConfig(name, id);
    assertSame(id, tc1.getPluginName());
    assertSame(id, tc2.getPluginName());
    assertEquals(name, tc1.getDisplayName());
  }

  public void testAccessors1() {
    TitleConfig tc1 = new TitleConfig("a", "b");
    tc1.setJournalTitle("42");
    assertEquals("42", tc1.getJournalTitle());
    tc1.setPluginVersion("4");
    assertEquals("4", tc1.getPluginVersion());
    List foo = new ArrayList();
    tc1.setParams(foo);
    assertSame(foo, tc1.getParams());
  }
  
  public void testGetConfig() {
    ConfigParamDescr d1 = new ConfigParamDescr("key1");
    ConfigParamDescr d2 = new ConfigParamDescr("key2");
    ConfigParamAssignment a1 = new ConfigParamAssignment(d1, "a");
    ConfigParamAssignment a2 = new ConfigParamAssignment(d2, "foo");
    TitleConfig tc1 = new TitleConfig("a", "b");
    tc1.setParams(ListUtil.list(a1, a2));
    Configuration config = tc1.getConfig();
    Configuration exp = ConfigManager.newConfiguration();
    exp.put("key1", "a");
    exp.put("key2", "foo");
    assertEquals(exp, config);
  }

  public void testGetNoEditKeys() {
    ConfigParamDescr d1 = new ConfigParamDescr("key1");
    ConfigParamDescr d2 = new ConfigParamDescr("key2");
    ConfigParamAssignment a1 = new ConfigParamAssignment(d1, "a");
    ConfigParamAssignment a2 = new ConfigParamAssignment(d2, "foo");
    a2.setEditable(true);
    TitleConfig tc1 = new TitleConfig("a", "b");
    tc1.setParams(ListUtil.list(a1, a2));
    Collection u1 = tc1.getUnEditableKeys();
    assertIsomorphic(ListUtil.list("key1"), u1);
  }

  public void testMatchesConfig() {
    ConfigParamDescr d1 = new ConfigParamDescr("key1");
    ConfigParamDescr d2 = new ConfigParamDescr("key2");
    ConfigParamAssignment a1 = new ConfigParamAssignment(d1, "a");
    ConfigParamAssignment a2 = new ConfigParamAssignment(d2, "foo");
    TitleConfig tc1 = new TitleConfig("a", "b");
    tc1.setParams(ListUtil.list(a1, a2));
    Configuration config = tc1.getConfig();
    assertTrue(tc1.matchesConfig(config));
    // should still match with an extra param in config
    config.put("key4", "v");
    assertTrue(tc1.matchesConfig(config));
    // but not with a disagreeing one
    config.put("key1", "v");
    assertFalse(tc1.matchesConfig(config));
    // make it match again
    config.put("key1", "a");
    assertTrue(tc1.matchesConfig(config));
    // then make a param editable, which should make it not match
    a1.setEditable(true);
    assertFalse(tc1.matchesConfig(config));
  }

  public void testEqualsHashWithNulls() {
    TitleConfig tc1 = new TitleConfig(null, (String)null);
    TitleConfig tc1c = new TitleConfig(null, (String)null);
    assertEquals(tc1, tc1c);
    assertEquals(tc1.hashCode(), tc1c.hashCode());
  }

  public void testEqualsHash() {
    ConfigParamDescr d1 = new ConfigParamDescr("key1");
    ConfigParamDescr d2 = new ConfigParamDescr("key2");
    ConfigParamAssignment a1 = new ConfigParamAssignment(d1, "a");
    ConfigParamAssignment a2 = new ConfigParamAssignment(d2, "foo");
    TitleConfig tc1 = new TitleConfig("a", "b");
    tc1.setParams(ListUtil.list(a1, a2));
    tc1.setJournalTitle("jtitle");
    tc1.setPluginVersion("2");
    tc1.setEstimatedSize(42);
    ConfigParamDescr d1c = new ConfigParamDescr("key1");
    ConfigParamDescr d2c = new ConfigParamDescr("key2");
    ConfigParamAssignment a1c = new ConfigParamAssignment(d1c, "a");
    ConfigParamAssignment a2c = new ConfigParamAssignment(d2c, "foo");
    TitleConfig tc1c = new TitleConfig("a", "b");
    tc1c.setParams(ListUtil.list(a1c, a2c));
    tc1c.setJournalTitle("jtitle");
    tc1c.setPluginVersion("2");
    tc1c.setEstimatedSize(42);
    assertEquals(tc1, tc1c);
    assertEquals(tc1.hashCode(), tc1c.hashCode());
    // params are order-independent
    tc1c.setParams(ListUtil.list(a2c, a1c));
    assertEquals(tc1, tc1c);
    assertEquals(tc1.hashCode(), tc1c.hashCode());
    tc1c.setParams(ListUtil.list(a1c, new ConfigParamAssignment(d1c, "b")));
    assertNotEquals(tc1, tc1c);
    tc1c.setParams(ListUtil.list(a1c, a2c));
    assertEquals(tc1, tc1c);
    tc1c.setJournalTitle("sdfsdf");
    assertNotEquals(tc1, tc1c);
    tc1.setJournalTitle("sdfsdf");
    assertEquals(tc1, tc1c);
    tc1c.setPluginVersion("3");
    assertNotEquals(tc1, tc1c);
    tc1.setPluginVersion("3");
    assertEquals(tc1, tc1c);
    tc1c.setEstimatedSize(420);
    assertNotEquals(tc1, tc1c);
    tc1.setEstimatedSize(420);
    assertEquals(tc1, tc1c);

    assertNotEquals(new TitleConfig("a", "b"), new TitleConfig("2", "b"));
    assertNotEquals(new TitleConfig("a", "b"), new TitleConfig("a", "2"));

  }

}
