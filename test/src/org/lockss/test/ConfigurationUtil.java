/*
 * $Id: ConfigurationUtil.java,v 1.18 2006-04-05 22:32:21 tlipkis Exp $
 *

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.IOException;
import java.util.*;

import org.lockss.config.*;
import org.lockss.util.*;

/** Utilities for Configuration and ConfigManager
 */
public class ConfigurationUtil {
  public static Logger log = Logger.getLogger("ConfigUtil");

  private static ConfigManager mgr() {
    return ConfigManager.getConfigManager();
  }

  /** Create a Configuration from the supplied string.
   */
  public static Configuration fromString(String s)
      throws IOException {
    List l = ListUtil.list(FileTestUtil.urlOfString(s));
    return mgr().readConfig(l);
  }

  /** Create a Configuration from the supplied Properties.
   */
  public static Configuration fromProps(Properties props) {
    PropertyTree tree = new PropertyTree(props);
    try {
      return (Configuration)PrivilegedAccessor.
	invokeConstructor("org.lockss.config.ConfigurationPropTreeImpl", tree);
    } catch (ClassNotFoundException e) {
      // because I don't want to change all the callers of this
      throw new RuntimeException(e.toString());
    } catch (NoSuchMethodException e) {
      // because I don't want to change all the callers of this
      throw new RuntimeException(e.toString());
    } catch (IllegalAccessException e) {
      // because I don't want to change all the callers of this
      throw new RuntimeException(e.toString());
    } catch (java.lang.reflect.InvocationTargetException e) {
      // because I don't want to change all the callers of this
      throw new RuntimeException(e.toString());
    } catch (InstantiationException e) {
      // because I don't want to change all the callers of this
      throw new RuntimeException(e.toString());
    }
  }

  /** Return a Configuration that's the union of the two Configurations
   */
  public static Configuration merge(Configuration c1, Configuration c2) {
    Configuration res = c1.copy();
    for (Iterator iter = c2.keyIterator(); iter.hasNext(); ) {
      String key = (String)iter.next();
      res.put(key, c2.get(key));
    }
    return res;
  }

  /** Create a Configuration from the contents of the URLs in the list
   */
  public static Configuration fromUrlList(List l) throws IOException {
    return mgr().readConfig(l);
  }

  /** Create a Configuration with a single param set to the specified
   * value.
   */
  public static Configuration fromArgs(String prop, String val) {
    Properties props = new Properties();
    props.put(prop, val);
    return fromProps(props);
  }

  /** Create a Configuration with two params set to the specified
   * values.
   */
  public static Configuration fromArgs(String prop1, String val1,
				       String prop2, String val2) {
    Properties props = new Properties();
    props.put(prop1, val1);
    props.put(prop2, val2);
    return fromProps(props);
  }

  /** Create a Configuration with three params set to the specified
   * values.
   */
  public static Configuration fromArgs(String prop1, String val1,
                                       String prop2, String val2,
                                       String prop3, String val3) {
    // JAVA5: merge fromArgs variants into fromArgs(String...) ?
    Properties props = new Properties();
    props.put(prop1, val1);
    props.put(prop2, val2);
    props.put(prop3, val3);
    return fromProps(props);
  }

  /** Create a Configuration with four params set to the specified
   * values.
   */
  public static Configuration fromArgs(String prop1, String val1,
                                       String prop2, String val2,
                                       String prop3, String val3,
                                       String prop4, String val4) {
    // JAVA5: merge fromArgs variants into fromArgs(String...) ?
    Properties props = new Properties();
    props.put(prop1, val1);
    props.put(prop2, val2);
    props.put(prop3, val3);
    props.put(prop4, val4);
    return fromProps(props);
  }

  /** Create a Configuration from the supplied property list and install
   * it as the current configuration.
   */
  public static boolean setCurrentConfigFromProps(Properties props) {
    return installConfig(fromProps(props));
  }

  /** Create a Configuration from the contents of the URLs in the list and
   * install it as the current configuration.
   */
  public static boolean setCurrentConfigFromUrlList(List l)
      throws IOException {
    return installConfig(fromUrlList(l));
  }

  /** Create a Configuration from the supplied string and install it as the
   * current configuration.
   */
  public static boolean setCurrentConfigFromString(String s)
      throws IOException {
    return installConfig(fromString(s));
  }

  /** Create a Configuration with a single param set to the specified
   * value, and install it as the current configuration.
   */
  public static boolean setFromArgs(String prop, String val) {
    return installConfig(fromArgs(prop, val));
  }

  /** Create a Configuration with two params set to the specified
   * values, and install it as the current configuration.
   */
  public static boolean setFromArgs(String prop1, String val1,
				    String prop2, String val2) {
    return installConfig(fromArgs(prop1, val1, prop2, val2));
  }

  /** Add the values to the current config
   */
  public static boolean addFromProps(Properties props) {
    return installConfig(merge(CurrentConfig.getCurrentConfig(),
                               fromProps(props)));
  }

  /** Add the value to the current config
   */
  public static boolean addFromArgs(String prop, String val) {
    return installConfig(merge(CurrentConfig.getCurrentConfig(),
                               fromArgs(prop, val)));
  }

  /** Add two values to the current config
   */
  public static boolean addFromArgs(String prop1, String val1,
                                    String prop2, String val2) {
    return installConfig(merge(CurrentConfig.getCurrentConfig(),
			       fromArgs(prop1, val1, prop2, val2)));
  }

  /** Add three values to the current config
   */
  public static boolean addFromArgs(String prop1, String val1,
                                    String prop2, String val2,
                                    String prop3, String val3) {
    // JAVA5: merge addFromArgs variants into addFromArgs(String...) ?
    return installConfig(merge(CurrentConfig.getCurrentConfig(),
                               fromArgs(prop1, val1, prop2, val2, prop3, val3)));
  }

  /** Add four values to the current config
   */
  public static boolean addFromArgs(String prop1, String val1,
                                    String prop2, String val2,
                                    String prop3, String val3,
                                    String prop4, String val4) {
    // JAVA5: merge addFromArgs variants into addFromArgs(String...) ?
    return installConfig(merge(CurrentConfig.getCurrentConfig(),
                               fromArgs(prop1, val1, prop2, val2,
                                        prop3, val3, prop4, val4)));
  }

  /** Install the supplied Configuration as the current configuration.
   */
  public static boolean installConfig(Configuration config) {
    MemoryConfigFile cf = new MemoryConfigFile("foo", config, 1);
    try {
      PrivilegedAccessor.invokeMethod(mgr(), "updateConfig",
				      ListUtil.list(cf));
    } catch (Exception e) {
      //      throw new RuntimeException(e.toString());
      throw new RuntimeException(StringUtil.stackTraceString(e));
    }
    return true;
  }


}
