/*
 * $Id: StringUtil.java,v 1.6 2002-10-31 01:23:12 troberts Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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
import java.lang.reflect.*;
import gnu.regexp.*;

/**
 * This is a class to contain generic string utilities
 *
 * @author  Thomas S. Robertson
 * @version 0.0
 */

public class StringUtil {

  /**
   * Replace all occurances of oldstr in line with newstr
   * @param line string to be modified
   * @param oldstr string to be replace
   * @param newstr string to replace oldstr
   * @return new string with oldstr replaced by newstr
   */
  public static String replaceString(String line,
				     String oldstr, String newstr) {
    if (oldstr.compareTo(newstr) == 0){
      return line;
    }
    StringBuffer sb = null;
    if (oldstr.length() == newstr.length()){
      sb = new StringBuffer(line);
      int lastIdx = line.indexOf(oldstr);
      if (lastIdx >= 0){
	do{
	  sb.replace(lastIdx, lastIdx+newstr.length(), newstr);
	}while ((lastIdx = line.indexOf(oldstr, lastIdx+1)) >= 0);
      }
    }
    else{
      sb = new StringBuffer();
      int oldStrIdx = 0;
      int lastIdx = line.indexOf(oldstr);
      if (lastIdx >= 0){
	do{
	  for (int ix=oldStrIdx; ix < lastIdx; ix++){
	    sb.append(line.charAt(ix));
	  }
	  sb.append(newstr);
	  oldStrIdx = lastIdx + oldstr.length();
	}while ((lastIdx = line.indexOf(oldstr, lastIdx+1)) >= 0);
      }
      for (int ix=oldStrIdx; ix<line.length(); ix++){
	sb.append(line.charAt(ix));
      }
    }
    return sb.toString();
  }

  /**
   * Concatenate elements of collection into string, with separators
   * @param c - Collection of object (on which toString() will be called)
   * @param separator - String to put between elements
   * @return Concatenated string
   */
  public static String
    separatedString(Collection c, String separator) {
    return separatedString(c, "", separator, "",
			   new StringBuffer()).toString();
  }

  /**
   * Concatenate elements of object array into string, with separators
   * @param arr - Array of object (on which toString() will be called)
   * @param separator - String to put between elements
   * @return Concatenated string
   */
  public static String
    separatedString(Object arr[], String separator) {
    return separatedString(ListUtil.fromArray(arr), "", separator, "",
			   new StringBuffer()).toString();
  }

  /**
   * Concatenate elements of collection into string, with separators
   * @param c - Collection of object (on which toString() will be called)
   * @param separator - String to put between elements
   * @ sb - StringBuffer to write result into
   * @return sb
   */
  public static StringBuffer
    separatedString(Collection c, String separator,
		    StringBuffer sb) {
    return separatedString(c, "", separator, "", sb);
  }

  /**
   * Concatenate elements of collection into string, delimiting each element,
   * adding separators
   * @param c - Collection of object (on which toString() will be called)
   * @param separator - String to put between elements
   * @param delimiter - String with which to surround each element
   * @return Concatenated string
   */
  public static String
    separatedDelimitedString(Collection c, String separator,
			     String delimiter) {
    return separatedString(c, delimiter,
			   delimiter + separator + delimiter, delimiter,
			   new StringBuffer()).toString();
  }

  /**
   * Concatenate elements of collection into string, delimiting each element,
   * adding separators
   * @param c - Collection of object (on which toString() will be called)
   * @param separator - String to put between elements
   * @param delimiter1 - String with which to prefix each element
   * @param delimiter2 - String with which to suffix each element
   * @return Concatenated string
   */
  public static String
    separatedDelimitedString(Collection c, String separator,
			     String delimiter1, String delimiter2) {
    return separatedString(c, delimiter1,
			   delimiter2 + separator + delimiter1, delimiter2,
			   new StringBuffer()).toString();
  }

  /**
   * Concatenate elements of collection into string, adding separators,
   * terminating with terminator
   * @param c - Collection of object (on which toString() will be called)
   * @param separator - String to put between elements
   * @param terminator - String with which to terminate result
   * @return Concatenated string
   */
  public static String
    terminatedSeparatedString(Collection c, String separator,
			      String terminator) {
    return separatedString(c, "", separator, terminator,
			   new StringBuffer()).toString();
  }

  /**
   * Concatenate elements of collection into string, adding separators,
   * delimitig each element
   * @param c - Collection of object (on which toString() will be called)
   * @param separator - String to put between elements
   * @param separatorFirst - String to place before first element
   * @param separatorInner - String with which to separate elements
   * @param separatorLast - String to place after last element
   * @return Concatenated string
   */
  public static StringBuffer
    separatedString(Collection c, String separatorFirst,
		    String separatorInner, String separatorLast,
		    StringBuffer sb) {
    if (c == null) {
      return sb;
    }
    Iterator iter = c.iterator();
    boolean first = true;
    while (iter.hasNext()) {
      if (first) {
	first = false;
	sb.append(separatorFirst);
      } else {
	sb.append(separatorInner);
      }
      Object obj = iter.next();
      sb.append(obj == null ? "(null)" : obj.toString());
    }
    if (!first) {
      sb.append(separatorLast);
    }
    return sb;
  }

  /**
   * Method to trim the end off of a string as soon as it encounters
   * any one of the characters specified
   *
   * @param str String to trim
   * @param chars String containing the chars to trim after
   * @returns str turncated at the first occurance of any of the chars
   */
  public static String trimAfterChars(String str, String chars) {
    if (str == null) {
      return null;
    }
    if (chars != null) {
      for (int ix=0; ix < str.length(); ix++) {
	for (int jx=0; jx < chars.length(); jx++) {
	  if (str.charAt(ix) == chars.charAt(jx)) {
	    return str.substring(0, ix);
	  }
	}
      }
    }
    return str;
  }

  /** Like indevOf except is case-independent */
  public static int getIndexIgnoringCase(String str, String subStr) {
    if (str != null && subStr != null) {
      return (str.toUpperCase()).indexOf(subStr.toUpperCase());
    }
    return -1;
  }

  /* Return the substring following the final dot */
  public static String shortName(Object object) {
    if (object == null) {
      return null;
    }
    String name = object.toString();
    return name.substring(name.lastIndexOf('.')+1);
  }

  /* Return the non-qualified name of the class */
  public static String shortName(Class clazz) {
    String className = clazz.getName();
    return className.substring(className.lastIndexOf('.')+1);
  }

  /* Return the non-qualified name of the method (Class.method) */
  public static String shortName(Method method) {
    return shortName(method.getDeclaringClass()) +
      "." + method.getName();
  }

  static RE alphanum = new UncheckedRE("([^a-zA-Z0-9])");

  /** Return a copy of the string with all non-alphanumeric chars
   * escaped by backslash.  Useful when embedding an unknown string in
   * a regexp
   */
  public static String escapeNonAlphaNum(String str) {
    return alphanum.substituteAll(str, "\\$1");
  }

  /**
   * Returns the number of instances of a particular substring in a string.
   * This ignores overlap, starting from the left, so 'xxxxxy' would have
   * 2 instances of 'xx', not 4.  Empty string as a substring returns 0.
   */
  public static int substringCount(String str, String subStr) {
    int len = subStr.length();
    if (len == 0) {
      return 0;
    }
    int pos = 0;
    int count = 0;
    while ((pos = str.indexOf(subStr, pos)) >= 0) {
      count++;
      pos += len;
    }
    return count;
  }
}

