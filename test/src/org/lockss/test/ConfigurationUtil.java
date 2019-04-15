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

import java.io.IOException;
import java.util.*;
import java.net.*;

import org.lockss.config.*;
import org.lockss.util.*;

/** Utilities for Configuration and ConfigManager
 */
public class ConfigurationUtil {
  public static Logger log = Logger.getLogger("ConfigUtil");

  private static ConfigManager mgr() {
    return ConfigManager.getConfigManager();
  }

  /** Read a Configuration from a file.
   */
  public static Configuration fromFile(String f) throws IOException {
    return mgr().readConfig(ListUtil.list(f));
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
    res.copyFrom(c2);
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

  /** Reset the current configuration so all params have their default
   * value */
  public static void resetConfig() {
    setCurrentConfigFromProps(new Properties());
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

  /** Add the contents of the file to the current config
   */
  public static boolean addFromFile(String f) throws IOException {
    return installConfig(merge(CurrentConfig.getCurrentConfig(),
                               fromFile(f)));
  }

  /** Add the contents of the URL to the current config.  To load from a
   * file in the test dir do, <i>eg</i>:
   * <code>ConfigurationUtil.addFromUrl(getResource("sample.xml"));</code>
   * @param url URL of a config file.
   */
  public static boolean addFromUrl(URL url) throws IOException {
    return addFromUrl(url.toString());
  }

  /** Add the contents of the URL to the current config.
   * @param url URL of a config file.
   */
  public static boolean addFromUrl(String url) throws IOException {
    return installConfig(merge(CurrentConfig.getCurrentConfig(),
                               fromUrlList(ListUtil.list(url))));
  }

  /** Remove a param from the current Configuration
   * @param key the name of the param to remove
   */
  public static boolean removeKey(String key) {
    Configuration cur = CurrentConfig.getCurrentConfig().copy();
    cur.remove(key);
    return installConfig(cur);
  }

  /** Remove a collection of params from the current Configuration
   * @param keys Collection of names of params to remove
   */
  public static boolean removeKeys(Collection<String> keys) {
    Configuration cur = CurrentConfig.getCurrentConfig().copy();
    for (String key : keys) {
      cur.remove(key);
    }
    return installConfig(cur);
  }

  /** Install the Tdb in the current config
   * @param url URL of a config file.
   */
  public static boolean setTdb(Tdb tdb) {
    Configuration config = CurrentConfig.getCurrentConfig().copy();
    config.setTdb(tdb);
    return installConfig(config);
  }

  /** Install the supplied Configuration as the current configuration.
   */
  public static boolean installConfig(Configuration config) {
    MemoryConfigFile cf = new MemoryConfigFile("foo", config, 1);
    try {
      PrivilegedAccessor.invokeMethod(mgr(), "updateConfig",
				      ListUtil.list(cf));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return true;
  }


}
