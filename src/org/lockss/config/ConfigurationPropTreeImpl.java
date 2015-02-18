/*
 * $Id$
 */

/*

Copyright (c) 2001-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.config;

import java.io.*;
import java.util.*;

import org.lockss.util.*;

/** <code>ConfigurationPropTreeImpl</code> represents the config parameters
 * as a {@link org.lockss.util.PropertyTree}
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

  public boolean store(OutputStream ostr, String header) throws IOException {
    return store(ostr, header, null);
  }

  // Based on Properties#store(), modified to output additional properties
  // first, and the main set of properties alphabetically
  public boolean store(OutputStream out, String comments,
		       Properties additionalProps)
      throws IOException {
    if (additionalProps != null) {
      // If we were given additional props, write them first with the
      // comment, then write ourself with no comment or date
      additionalProps.store(out, comments);
      store0(new BufferedWriter(new OutputStreamWriter(out, "8859_1")),
	     null, false, true);
    } else {
      // else write ourself with the comment and date
      store0(new BufferedWriter(new OutputStreamWriter(out, "8859_1")),
	     comments, true, true);
    }
    return true;
  }

  private void store0(BufferedWriter bw, String comments,
		      boolean addDateComment,
		      boolean escUnicode)
      throws IOException {
    if (comments != null) {
      writeComments(bw, comments);
    }
    if (addDateComment) {
      bw.write("#" + new Date().toString());
      bw.newLine();
    }
    synchronized (this) {
      for (String key : sortedKeys()) {
	String val = (String)get(key);
	key = saveConvert(key, true, escUnicode);
	// No need to escape embedded and trailing spaces for value
	val = saveConvert(val, false, escUnicode);
	bw.write(key);
	bw.write("=");
	bw.write(val);
	bw.newLine();
      }
    }
    bw.flush();
  }

  private Collection<String> sortedKeys() {
     return new TreeSet(props.keySet());
  }

  private static void writeComments(BufferedWriter bw, String comments) 
      throws IOException {
    bw.write("#");
    int len = comments.length();  
    int current = 0;
    int last = 0;
    char[] uu = new char[6];
    uu[0] = '\\';
    uu[1] = 'u';
    while (current < len) {
      char c = comments.charAt(current);
      if (c > '\u00ff' || c == '\n' || c == '\r') {
	if (last != current) 
	  bw.write(comments.substring(last, current));
	if (c > '\u00ff') {
	  uu[2] = toHex((c >> 12) & 0xf);
	  uu[3] = toHex((c >>  8) & 0xf);
	  uu[4] = toHex((c >>  4) & 0xf);
	  uu[5] = toHex( c        & 0xf);
	  bw.write(new String(uu));
	} else {
	  bw.newLine();
	  if (c == '\r' && 
	      current != len - 1 && 
	      comments.charAt(current + 1) == '\n') {
	    current++;
	  }
	  if (current == len - 1 ||
	      (comments.charAt(current + 1) != '#' &&
	       comments.charAt(current + 1) != '!'))
	    bw.write("#");
	}
	last = current + 1;
      } 
      current++;
    }
    if (last != current) 
      bw.write(comments.substring(last, current));
    bw.newLine();
  }

  /*
   * Converts unicodes to encoded &#92;uxxxx and escapes
   * special characters with a preceding slash
   */
  // Based on Properties#saveConvert() with efficiency tweaks
  private String saveConvert(String theString,
			     boolean escapeSpace,
			     boolean escapeUnicode) {
    int len = theString.length();
    boolean needEscape = false;
    for (int ix = 0; ix < len; ix++) {
      char aChar = theString.charAt(ix);
      switch(aChar) {
      case ' ':
	needEscape = (ix == 0 || escapeSpace);
	break;
      case '\\':
      case '\t':
      case '\n':
      case '\r':
      case '\f':
      case '=':
      case ':':
      case '#':
      case '!':
	needEscape = true;
	break;
      default:
	needEscape = (escapeUnicode && ((aChar < 0x0020) || (aChar > 0x007e)));
	break;
      }
      if (needEscape) {
	int bufLen = len * 2;
	if (bufLen < 0) {
	  bufLen = Integer.MAX_VALUE;
	}
	StringBuilder sb = new StringBuilder(bufLen);
	sb.append(theString, 0, ix);
	for (int iy = ix; iy < len; iy++) {
	  aChar = theString.charAt(iy);
	  // Handle common case first, selecting largest block that
	  // avoids the specials below
	  if ((aChar > 61) && (aChar < 127)) {
	    if (aChar == '\\') {
	      sb.append('\\');
	    }
	    sb.append(aChar);
	    continue;
	  }
	  switch(aChar) {
	  case ' ':
	    if (iy == 0 || escapeSpace) {
	      sb.append('\\');
	    }
	    sb.append(' ');
	    break;
	  case '\t':sb.append('\\'); sb.append('t');
	    break;
	  case '\n':sb.append('\\'); sb.append('n');
	    break;
	  case '\r':sb.append('\\'); sb.append('r');
	    break;
	  case '\f':sb.append('\\'); sb.append('f');
	    break;
	  case '=': // Fall through
	  case ':': // Fall through
	  case '#': // Fall through
	  case '!':
	    sb.append('\\'); sb.append(aChar);
	    break;
	  default:
	    if (escapeUnicode && ((aChar < 0x0020) || (aChar > 0x007e))) {
	      sb.append('\\');
	      sb.append('u');
	      sb.append(toHex((aChar >> 12) & 0xF));
	      sb.append(toHex((aChar >>  8) & 0xF));
	      sb.append(toHex((aChar >>  4) & 0xF));
	      sb.append(toHex( aChar        & 0xF));
	    } else {
	      sb.append(aChar);
	    }
	  }
	}
	return sb.toString();
      }
    }
    return theString;
  }

  /**
   * Convert a nibble to a hex character
   * @param	nibble	the nibble to convert.
   */
  private static char toHex(int nibble) {
    return hexDigit[(nibble & 0xF)];
  }

  /** A table of hex digits */
  private static final char[] hexDigit = {
    '0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'
  };

  /**
   * Reset this instance.
   */
  void reset() {
    super.reset();
    props.clear();
  }

  /** Return the set of keys whose values differ.
   * This operation is transitive: c1.differentKeys(c2) yields the same 
   * result as c2.differentKeys(c1).
   * 
   * @param otherConfig the config to compare with.  May be null.
   */
  public Set<String> differentKeys(Configuration otherConfig) {
    if (this == otherConfig) {
      return Collections.EMPTY_SET;
    }
    PropertyTree otherTree = (otherConfig == null) ? 
        new PropertyTree() : ((ConfigurationPropTreeImpl)otherConfig).getPropertyTree();
    return PropUtil.differentKeysAndPrefixes(getPropertyTree(), otherTree);
  }

  public boolean containsKey(String key) {
    return props.containsKey(key);
  }

  public String get(String key) {
    return (String)props.get(key);
  }

  /** Return the config value associated with <code>key</code>, returning
   * null in place of the empty string.
   */
  public String getNonEmpty(String key) {
    String val = get(key);
    if (StringUtil.isNullString(val)) {
      val = null;
    }
    return val;
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
	  propList = StringUtil.breakAt((String)o, ';', 0, true);
	}
      } else {
	propList = Collections.EMPTY_LIST;
      }
    } catch (ClassCastException ex) {
      // The client requested a list of something that wasn't actually a list.
      // Throw an exception
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
    
    // also seal the title database
    Tdb tdb = getTdb();
    if (tdb != null) {
      tdb.seal();
    }
  }

  public boolean isSealed() {
    return isSealed;
  }

  public Configuration getConfigTree(String key) {
    PropertyTree tree = props.getTree(key);
    if (tree == null) {
      // getTree never returns null, but be safe
      return ConfigManager.EMPTY_CONFIGURATION;
    }
    Configuration res = new ConfigurationPropTreeImpl(tree);
    if (isSealed()) {
      res.seal();
    }
    return res;
  }

  // Cache the unmodifiable keySet so tests can compare identity
  private Set keySet = null;

  public Set keySet() {
    if (keySet == null) {
      keySet = Collections.unmodifiableSet(props.keySet());
    }
    return keySet;
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

  private Iterator possiblyEmptyIterator(Enumeration en) {
    if (en != null) {
      return new EnumerationIterator(en);
    } else {
      return CollectionUtil.EMPTY_ITERATOR;
    }
  }

  public String toString() {
    return getPropertyTree().toString();
  }
}
