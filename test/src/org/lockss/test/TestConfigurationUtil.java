/*
 * $Id: TestConfigurationUtil.java,v 1.1 2003-07-16 00:05:06 tlipkis Exp $
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

package org.lockss.test;

import java.util.*;
import java.io.*;
import java.net.*;
import org.lockss.util.*;
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
    assertEquals(2, config.keySet().size());
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

}
