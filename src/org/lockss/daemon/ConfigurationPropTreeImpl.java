/*
 * $Id: ConfigurationPropTreeImpl.java,v 1.15 2004-06-29 18:58:22 smorabito Exp $
 */

/*

Copyright (c) 2001-2003 Board of Trustees of Leland Stanford Jr. University,
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
  private boolean isSealed = false;

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

  /**
   * Load plain text Java properties.
   */
  boolean loadTextProperties(InputStream istr) throws IOException {
    props.load(istr);
    return true;
  }

  /**
   * Load LOCKSS XML properties
   */
  boolean loadXmlProperties(InputStream istr) throws IOException {
    return XmlPropertyLoader.load(props, istr);
  }

  boolean store(OutputStream ostr, String header) throws IOException {
    SortedProperties.fromProperties(props).store(ostr, header);
//     props.store(ostr, header);
    return true;
  }

  void reset() {
    props.clear();
  }

  public boolean equals(Object c) {
    if (! (c instanceof ConfigurationPropTreeImpl)) {
      return false;
    }
    ConfigurationPropTreeImpl c0 = (ConfigurationPropTreeImpl)c;
    return PropUtil.equalProps(props, c0.getPropertyTree());
  }

  /** Return the set of keys whose values differ.
   * @param otherConfig the config to compare with.  May be null.
   */
  public Set differentKeys(Configuration otherConfig) {
    if (otherConfig == null) {
      return props.keySet();
    }
    ConfigurationPropTreeImpl oc = (ConfigurationPropTreeImpl)otherConfig;
    return PropUtil. differentKeys(getPropertyTree(), oc.getPropertyTree());
  }

  public boolean containsKey(String key) {
    return props.containsKey(key);
  }

  public String get(String key) {
    return (String)props.get(key);
  }

  /**
   * Return a list of values for the given key.
   */
  public List getList(String key) {
    List propList = null;

    try {
      Object o = props.get(key);
      if (o != null) {
	if (o instanceof List) {
	  propList = (List)o;
	} else {
	  propList = StringUtil.breakAt((String)o, ';');
	}
      }
    } catch (ClassCastException ex) {
      // The client requested a list of something that wasn't actually a list.
      // Throw a Config exception
      throw new IllegalArgumentException("Key does not hold a list value: " + key);
    }
    return propList;
  }

  public void put(String key, String val) {
    if (isSealed) {
      throw new IllegalStateException("Can't modify sealed configuration");
    }
    props.setProperty(key, val);
  }

  /** Remove the value associated with <code>key</code>.
   * @param key the config key to remove
   */
  public void remove(String key) {
    if (isSealed) {
      throw new IllegalStateException("Can't modify sealed configuration");
    }
    props.remove(key);
  }

  public void seal() {
    isSealed = true;
  }

  public boolean isSealed() {
    return isSealed;
  }

  public Configuration getConfigTree(String key) {
    PropertyTree tree = props.getTree(key);
    if (tree == null) {
      return null;
    }
    Configuration res = new ConfigurationPropTreeImpl(tree);
    if (isSealed()) {
      res.seal();
    }
    return res;
  }

  public Set keySet() {
    return Collections.unmodifiableSet(props.keySet());
  }

  public Iterator keyIterator() {
    return keySet().iterator();
  }

  public Iterator nodeIterator() {
    return possiblyEmptyIterator(props.getNodes());
  }

  public Iterator nodeIterator(String key) {
    return possiblyEmptyIterator(props.getNodes(key));
  }

  private Iterator possiblyEmptyIterator(Enumeration enum) {
    if (enum != null) {
      return new EnumerationIterator(enum);
    } else {
      return CollectionUtil.EMPTY_ITERATOR;
    }
  }

  public String toString() {
    return getPropertyTree().toString();
  }

}
