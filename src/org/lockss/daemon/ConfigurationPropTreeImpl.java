/*
 * $Id: ConfigurationPropTreeImpl.java,v 1.1 2002-08-31 06:26:35 tal Exp $
 */

/*

Copyright (c) 2001-2002 Board of Trustees of Leland Stanford Jr. University,
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
import java.io.*;
import java.net.*;
import java.text.*;
import org.lockss.util.*;

import org.mortbay.tools.*;

/** <code>ConfigurationPropTreeImpl</code> represents the config parameters
 * as a <code>PropertyTree</code>
 */
public class ConfigurationPropTreeImpl extends Configuration {
  private PropertyTree props;

  ConfigurationPropTreeImpl() {
    super();
    props = new PropertyTree();
  }

  private ConfigurationPropTreeImpl(PropertyTree tree) {
    super();
    props = tree;
  }

  PropertyTree getPropertyTree() {
    return props;
  }

  boolean load(InputStream istr) throws IOException {
    props.load(istr);
    return true;
  }

  public void reset() {
    props.clear();
  }

  public boolean equals(Object c) {
    if (! (c instanceof ConfigurationPropTreeImpl)) {
      return false;
    }
    ConfigurationPropTreeImpl c0 = (ConfigurationPropTreeImpl)c;
    return PropUtil.equalProps(props, c0.getPropertyTree());
  }

  Set differentKeys(Configuration otherConfig) {
    ConfigurationPropTreeImpl oc = (ConfigurationPropTreeImpl)otherConfig;
    return PropUtil. differentKeys(getPropertyTree(), oc.getPropertyTree());
  }

  /** Get config param as string */
  public String get(String key) {
    return (String)props.get(key);
  }

  /** Get config param as boolean */
  public boolean getBoolean(String key) {
    return props.getBoolean(key);
  }

  /** Get config param as boolean with default */
  public boolean getBoolean(String key, boolean dfault) {
    return props.getBoolean(key, dfault);
  }

  /** Returns a Configuration instance containing all the keys at or
   * below <code>key</code>
   */
  public Configuration getConfigTree(String key) {
    PropertyTree tree = props.getTree(key);
    return (tree == null) ? null : new ConfigurationPropTreeImpl(tree);
  }

  /* Returns an <code>Iterator</code> over all the keys in the configuration.
   */
  public Iterator keyIterator() {
    return props.keySet().iterator();
  }

  /* Returns an <code>Iterator</code> over all the top level
     keys in the configuration.
   */
  public Iterator nodeIterator() {
    return new EnumerationIterator(props.getNodes());
  }

  /* Returns an <code>Iterator</code> over the keys in the configuration
   * below <code>key</code>
   */
  public Iterator nodeIterator(String key) {
    return new EnumerationIterator(props.getNodes(key));
  }

  public String toString() {
    return getPropertyTree().toString();
  }

}
