/*
 * $Id: PropUtil.java,v 1.18 2009-07-22 06:41:55 tlipkis Exp $
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

package org.lockss.util;

import java.util.*;
import java.io.*;
import java.net.URLEncoder;


/** Utilities for Properties
 */
public class PropUtil {

  private static final String noProp = new String(); // guaranteed unique obj

  /** Returns a copy of the Properties */
  public static Properties copy(Properties props) {
    Properties copy = new Properties();
    for (Enumeration en = props.propertyNames();
	 en.hasMoreElements(); ) {
      String key = (String)en.nextElement();
      copy.setProperty(key, props.getProperty(key));
    }
    return copy;
  }


  public static Properties fromArgs(String prop, String val) {
    Properties props = new Properties();
    props.put(prop, val);
    return props;
  }

  public static Properties fromArgs(String prop1, String val1,
                                    String prop2, String val2) {
    Properties props = fromArgs(prop1, val1);
    props.put(prop2, val2);
    return props;
  }

  public static Properties fromArgs(String prop1, String val1,
                                    String prop2, String val2,
                                    String prop3, String val3) {
    Properties props = fromArgs(prop1, val1, prop2, val2);
    props.put(prop3, val3);
    return props;
  }

  public static Properties fromArgs(String prop1, String val1,
                                    String prop2, String val2,
                                    String prop3, String val3,
                                    String prop4, String val4) {
    Properties props = fromArgs(prop1, val1, prop2, val2, prop3, val3);
    props.put(prop4, val4);
    return props;
  }

  /** Load Properties from a file */
  public static Properties fromFile(File file) throws IOException {
    Properties res = new Properties();
    InputStream in = null;
    try {
      in = new BufferedInputStream(new FileInputStream(file));
      res.load(in);
    } finally {
      IOUtil.safeClose(in);
    }
    return res;
  }

  /** Store the Properties in the file */
  public static void toFile(File file, Properties props) throws IOException {
    OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
    try {
      props.store(os, "test");
    } finally {
      IOUtil.safeClose(os);
    }
  }

  private static boolean isKeySame(String key, Properties p1, Properties p2) {
    Object o1 = p1.getProperty(key);
    Object o2 = p2.getProperty(key, noProp);
    return (o1 == o2) ||
      // o2 == noProp means p2 didn't have key that p1 has.
      // we know o1 != o2, so if o1 == null, o2 != null.
      !((o2 == noProp || o1 == null || !o1.equals(o2)));
  }

  /**
   * Compare two Properties for equality (same set of properties with
   * same (.equals) values.
   * @param p1 first Properties
   * @param p2 second Properties
   * @return true iff Properties are equal
   */
  public static boolean equalProps(Properties p1, Properties p2) {
    if (p1 == null || p2 == null || p1.size() != p2.size()) {
      return false;
    }
    Enumeration en = p1.keys();
    while (en.hasMoreElements()) {
      String k1 = (String)en.nextElement();
      if (! isKeySame(k1, p1, p2)) {
	return false;
      }
    }
    return true;
  }

  /**
   * Compare two Properties, return the set of keys whose values differ,
   * including keys that don't exist in one or the other Properties).
   * @param p1 first Properties
   * @param p2 second Properties
   * @return Set of keys whose values differ.  Returns empty set if there
   * are no differences.
   */
  public static Set differentKeys(Properties p1, Properties p2) {
    if (p1 == null) {
      if (p2 == null) {
	return null;
      } else {
	return p2.keySet();
      }
    } else if (p2 == null) {
      return p1.keySet();
    }
    Set res = new HashSet();
    Set keys1 = p1.keySet();
    for (Iterator iter = keys1.iterator(); iter.hasNext();) {
      String k1 = (String)iter.next();
      if (! isKeySame(k1, p1, p2)) {
	res.add(k1);
      }
    }
    // add all the keys in p2 that don't appear in p1
    Set p2Only = new HashSet(p2.keySet());
    p2Only.removeAll(keys1);
    res.addAll(p2Only);
    return res;
  }

  /**
   * Compare two PropertyTrees, return the set of keys whose values differ,
   * including all distinct prefixes of keys whose values differ.
   * <i>E.g.</i>, if the key <code>foo.bar.frob</code> is in the difference
   * set, the result will contain <code>foo.bar.frob</code>,
   * <code>foo.bar.</code>, <code>foo.bar</code> <code>foo.</code> and
   * <code>foo</code>.  This is meant to be used to quickly determine
   * whether any part of a given subtree has changed, by checking for the
   * common prefix.  The somewhat odd behavior w.r.t. dots is for the
   * convenience of code that defines its prefix string with a final dot.
   * @param p1 first PropertyTree
   * @param p2 second PropertyTree
   * @return Set of keys and prefixes whose values differ.  Returns empty
   * set if there are no differences.
   * @throws NullPointerException if either PropertyTree is null
   */
  public static Set differentKeysAndPrefixes(PropertyTree p1,
					     PropertyTree p2) {
    Set res = new HashSet();
    Set keys1 = p1.keySet();
    for (Iterator iter = keys1.iterator(); iter.hasNext();) {
      String k1 = (String)iter.next();
      if (! isKeySame(k1, p1, p2)) {
	addKeyAndPrefixes(res, k1);
      }
    }
    // add all the keys in p2 that don't appear in p1
    Set p2Only = new HashSet(p2.keySet());
    p2Only.removeAll(keys1);
    for (Iterator iter = p2.keySet().iterator(); iter.hasNext();) {
      String k2 = (String)iter.next();
      if (!p1.containsKey(k2)) {
	addKeyAndPrefixes(res, k2);
      }
    }
    return res;
  }

  /** Add the key and all its prefixes to the set.  Proceeds from longest
   * to shortest, stopping if it generates a prefix already in the set
   * (because all shorter prefixes must already be in the set */
  private static void addKeyAndPrefixes(Set set, String key) {
    if (set.add(key)) {
      int len = key.length();
      int pos = len;
      while ((pos = key.lastIndexOf(".", pos - 1)) > 0) {
	if (!(((pos+1 == len) || set.add(key.substring(0, pos + 1))) &&
	      set.add(key.substring(0, pos)))) {
	  break;
	}
      }
    }
  }

  /**
   * Turns the properties into a canonical string
   * @return a canonical string generated from the props
   * @param props properties
   */
  public static String propsToCanonicalEncodedString(Properties props) {
    if (props == null || props.isEmpty()) {
      return "";
    }
    StringBuffer sb = new StringBuffer();
    SortedSet sortedKeys = new TreeSet(props.keySet());

    for (Iterator it=sortedKeys.iterator(); it.hasNext();) {
      String key = (String)it.next();
      sb.append(PropKeyEncoder.encode(key));
      sb.append("~");
      sb.append(PropKeyEncoder.encode(props.getProperty(key)));
      if (it.hasNext()) {
	sb.append("&");
      }
    }
    return sb.toString();
  }

  /**
   * Turns the canonical string into properties
   * @param s a string returned by propsToCanonicalEncodedString()
   * @return Properties from which the canonical string was generated
   */
  public static Properties canonicalEncodedStringToProps(String s)
      throws IllegalArgumentException {
    Properties res = new Properties();
    if (StringUtil.isNullString(s)) {
      return res;
    }
    StringTokenizer tk = new StringTokenizer(s, "~&", true);
    while (tk.hasMoreElements()) {
      String key = tk.nextToken();
      String tok;
      try {
	tok = tk.nextToken();
      } catch (NoSuchElementException e) {
	throw new IllegalArgumentException("No delimiter after prop: " + key);
      }
      if (!tok.equals("~")) {
	throw new IllegalArgumentException("Delimiter not \"~\": " + tok);
      }
      String val;
      try {
	val = tk.nextToken();
      } catch (NoSuchElementException e) {
	throw new IllegalArgumentException("No value for prop: " + key);
      }
      res.setProperty(PropKeyEncoder.decode(key),
		      PropKeyEncoder.decode(val));

      if (tk.hasMoreElements()) {
	tok = tk.nextToken();
	if (!tok.equals("&")) {
	  throw new IllegalArgumentException("Delimiter not \"&\": " + tok);
	}
      }
    }
    return res;
  }

  /**
   * Prints the Properties to the PrintStream
   * @param props properties to print
   * @param out stream to print to
   */
  public static void printPropsTo(Properties props, PrintStream out) {
    SortedSet keys = new TreeSet();
    for (Iterator iter = props.keySet().iterator(); iter.hasNext(); ) {
      keys.add((String)iter.next());
    }
    for (Iterator iter = keys.iterator(); iter.hasNext(); ) {
      String key = (String)iter.next();
      out.println(key + " = " + (String)props.getProperty(key));
    }
  }


}
