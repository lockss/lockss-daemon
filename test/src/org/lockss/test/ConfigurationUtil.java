/*
 * $Id: ConfigurationUtil.java,v 1.4 2003-06-20 22:34:55 claire Exp $
 *

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

import java.io.*;
import java.util.*;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.mortbay.tools.*;

/** Utilities for Configuration
 */
public class ConfigurationUtil {
  /** Create a Configuration from the supplied property list and install
   * it as the current configuration.
   */
  public static boolean setCurrentConfigFromProps(Properties props) {
    Configuration config = fromProps(props);
    return installConfig(config);
  }

  /** Create a Configuration from the contents of the URLs in the list and
   * install it as the current configuration.
   */
  public static boolean setCurrentConfigFromUrlList(List l) {
    Configuration config = Configuration.readConfig(l);
    return installConfig(config);
  }

  /** Create a Configuration from the supplied string and install it as the
   * current configuration.
   */
  public static boolean setCurrentConfigFromString(String s)
      throws IOException {
    return setCurrentConfigFromUrlList(ListUtil.list(FileUtil.urlOfString(s)));
  }

  /** Install the supplied Configuration as the current configuration.
   */
  public static boolean installConfig(Configuration config) {
    try {
      PrivilegedAccessor.invokeMethod(config, "installConfig", config);
    } catch (Exception e) {
      throw new RuntimeException(e.toString());
    }
    return true;
  }

  /** Create a Configuration from the supplied string.
   */
  public static Configuration fromString(String s)
      throws IOException {
    List l = ListUtil.list(FileUtil.urlOfString(s));
    return Configuration.readConfig(l);
  }

  /** Create a Configuration from the supplied Properties.
   */
  public static Configuration fromProps(Properties props) {
    PropertyTree tree = new PropertyTree(props);
    try {
      return (Configuration)PrivilegedAccessor.
	invokeConstructor("org.lockss.daemon.ConfigurationPropTreeImpl", tree);
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
}
