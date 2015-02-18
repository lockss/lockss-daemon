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

package org.lockss.test;

import java.util.*;
import java.io.*;
import java.net.*;
import org.lockss.util.*;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.test.*;

/**
 * Test class for <code>org.lockss.util.ConfigurationUtil</code>
 */

public class TestConfigurationUtil extends LockssTestCase {
  public static Class testedClasses[] = {
    org.lockss.test.ConfigurationUtil.class
  };

  private void check(Configuration config) throws Configuration.InvalidParam {
    assertEquals("12", config.get("prop1"));
    assertEquals(true, config.getBoolean("prop2"));
    // ConfigManager.copyPlatformParams() can add params
    assertTrue("Fewer than 2 resulting params", config.keySet().size() >= 2);
  }

  private void check() throws IOException, Configuration.InvalidParam {
    check(ConfigManager.getCurrentConfig());
  }

  public void testFromString()
      throws IOException, Configuration.InvalidParam {
    String s = "prop1=12\nprop2=true\n";
    check(ConfigurationUtil.fromString(s));
  }

  public void testSetFromString()
      throws IOException, Configuration.InvalidParam {
    String s = "prop1=12\nprop2=true\n";
    ConfigurationUtil.setCurrentConfigFromString(s);
    check();
  }

  public void testFromProps()
      throws IOException, Configuration.InvalidParam {
    Properties props = new Properties();
    props.put("prop1", "12");
    props.put("prop2", "true");
    check(ConfigurationUtil.fromProps(props));
  }

  public void testSetFromProps()
      throws IOException, Configuration.InvalidParam {
    Properties props = new Properties();
    props.put("prop1", "12");
    props.put("prop2", "true");
    ConfigurationUtil.setCurrentConfigFromProps(props);
    check();
  }

  public void testFromArgs()
      throws IOException, Configuration.InvalidParam {
    check(ConfigurationUtil.fromArgs("prop1", "12",
				     "prop2", "true"));
  }

  public void testSetFromArgs()
      throws IOException, Configuration.InvalidParam {
    ConfigurationUtil.setFromArgs("prop1", "12",
				  "prop2", "true");
    check();
  }

  public void testAddFromArgs()
      throws IOException, Configuration.InvalidParam {
    ConfigurationUtil.setFromArgs("prop1", "12");
    ConfigurationUtil.addFromArgs("prop2", "true");
    check();
  }

  public void testAddFromFile()
      throws IOException, Configuration.InvalidParam {
    ConfigurationUtil.setFromArgs("prop1", "xxx");
    File file = FileTestUtil.writeTempFile("config", "prop1=12\nprop2=true\n");
    ConfigurationUtil.addFromFile(file.toString());
    check();
  }

  public void testAddFromResource()
      throws IOException, Configuration.InvalidParam {
    ConfigurationUtil.setFromArgs("prop1", "xxx");
    File file = FileTestUtil.writeTempFile("config", "prop1=12\nprop2=true\n");
    ConfigurationUtil.addFromFile(file.toString());
    check();
  }

  public void testResetConfig()
      throws IOException, Configuration.InvalidParam {
    ConfigurationUtil.setFromArgs("prop1", "12");
    ConfigurationUtil.addFromArgs("prop2", "true");
    Configuration config = ConfigManager.getCurrentConfig();
    assertEquals("12", config.get("prop1"));
    assertEquals(true, config.getBoolean("prop2"));
    ConfigurationUtil.resetConfig();
    config = ConfigManager.getCurrentConfig();
    assertNull(config.get("prop1"));
    assertNull(config.get("prop2"));
  }

  private String xmlSample =
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
    "<lockss-config>\n" +
    "<property name=\"prop1\" value=\"12\" />\n" +
    "<property name=\"prop2\" value=\"true\" />\n" +
    "</lockss-config>\n";

  public void testAddFromXmlFile()
      throws IOException, Configuration.InvalidParam {
    ConfigurationUtil.setFromArgs("prop1", "xxx");
    File file = FileTestUtil.writeTempFile("config", ".xml", xmlSample);
    ConfigurationUtil.addFromFile(file.toString());
    check();
  }

  public void testAddFromXmlUrl()
      throws IOException, Configuration.InvalidParam {
    ConfigurationUtil.setFromArgs("prop1", "yyy");
    File file = FileTestUtil.writeTempFile("config", ".xml", xmlSample);
    ConfigurationUtil.addFromUrl(FileTestUtil.urlOfFile(file.toString()));
    check();
  }

  public void testRemove()
      throws IOException, Configuration.InvalidParam {
    final List<Set> keysets = new ArrayList<Set>();
    ConfigManager.getConfigManager().registerConfigurationCallback(new Configuration.Callback() {
	public void configurationChanged(Configuration config,
					 Configuration oldConfig,
					 Configuration.Differences diffs) {
	  HashSet<String> set = new HashSet<String>();
	  for (String s : (List<String>)ListUtil.list("p1", "p2", "p3", "p4")) {
	    if (config.containsKey(s)) {
	      set.add(s);
	    }
	  }
	  keysets.add(set);
	};
      });

    ConfigurationUtil.addFromArgs("p1", "x", "p2", "y", "p3", "z", "p4", "0");
    Configuration config = ConfigManager.getCurrentConfig();
    assertEquals("x", config.get("p1", "def1"));
    assertEquals("y", config.get("p2", "def2"));
    assertEquals("z", config.get("p3", "def3"));
    assertEquals("0", config.get("p4", "def4"));

    assertEquals(1, keysets.size());
    assertEquals(SetUtil.set("p1", "p2", "p3", "p4"), keysets.get(0));

    ConfigurationUtil.removeKey("p1");
    config = ConfigManager.getCurrentConfig();
    assertEquals("def1", config.get("p1", "def1"));
    assertEquals("y", config.get("p2", "def2"));
    assertEquals("z", config.get("p3", "def3"));
    assertEquals("0", config.get("p4", "def4"));

    assertEquals(2, keysets.size());
    assertEquals(SetUtil.set("p2", "p3", "p4"), keysets.get(1));

    ConfigurationUtil.removeKeys(ListUtil.list("p2", "p3"));
    config = ConfigManager.getCurrentConfig();
    assertEquals("def1", config.get("p1", "def1"));
    assertEquals("def2", config.get("p2", "def2"));
    assertEquals("def3", config.get("p3", "def3"));
    assertEquals("0", config.get("p4", "def4"));

    assertEquals(3, keysets.size());
    assertEquals(SetUtil.set("p4"), keysets.get(2));
  }

}
