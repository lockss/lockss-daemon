/*
 * $Id: StringUtil.java,v 1.1 2002-08-31 06:54:34 tal Exp $
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
    int index = 0;
    while ((index = line.indexOf(oldstr)) >= 0) {
      line = line.substring(0, index) + newstr +
	line.substring(index + oldstr.length());
    }
    return line;
  }

  /**
   * Concatenate elements of vector into string, with separators
   * @param v - Vector of object (on which toString() will be called)
   * @param separator - String to put between elements
   * @return Concatenated string
   */
  public static String
    separatedString(Vector v, String separator) {
    return separatedString(v, "", separator, "",
			   new StringBuffer()).toString();
  }

  /**
   * Concatenate elements of vector into string, with separators
   * @param v - Vector of object (on which toString() will be called)
   * @param separator - String to put between elements
   * @ sb - StringBuffer to write result into
   * @return sb
   */
  public static StringBuffer
    separatedString(Vector v, String separator,
		    StringBuffer sb) {
    return separatedString(v, "", separator, "", sb);
  }

  /**
   * Concatenate elements of vector into string, delimiting each element,
   * adding separators
   * @param v - Vector of object (on which toString() will be called)
   * @param separator - String to put between elements
   * @param delimiter - String with which to surround each element
   * @return Concatenated string
   */
  public static String
    separatedDelimitedString(Vector v, String separator,
			     String delimiter) {
    return separatedString(v, delimiter,
			   delimiter + separator + delimiter, delimiter,
			   new StringBuffer()).toString();
  }

  /**
   * Concatenate elements of vector into string, delimiting each element,
   * adding separators
   * @param v - Vector of object (on which toString() will be called)
   * @param separator - String to put between elements
   * @param delimiter1 - String with which to prefix each element
   * @param delimiter2 - String with which to suffix each element
   * @return Concatenated string
   */
  public static String
    separatedDelimitedString(Vector v, String separator,
			     String delimiter1, String delimiter2) {
    return separatedString(v, delimiter1,
			   delimiter2 + separator + delimiter1, delimiter2,
			   new StringBuffer()).toString();
  }

  /**
   * Concatenate elements of vector into string, adding separators,
   * terminating with terminator
   * @param v - Vector of object (on which toString() will be called)
   * @param separator - String to put between elements
   * @param terminator - String with which to terminate result
   * @return Concatenated string
   */
  public static String
    terminatedSeparatedString(Vector v, String separator,
			      String terminator) {
    return separatedString(v, "", separator, terminator,
			   new StringBuffer()).toString();
  }

  /**
   * Concatenate elements of vector into string, adding separators,
   * delimitig each element
   * @param v - Vector of object (on which toString() will be called)
   * @param separator - String to put between elements
   * @param separatorFirst - String to place before first element
   * @param separatorInner - String with which to separate elements
   * @param separatorLast - String to place after last element
   * @return Concatenated string
   */
  public static StringBuffer
    separatedString(Vector v, String separatorFirst,
		    String separatorInner, String separatorLast,
		    StringBuffer sb) {
    if (v == null) {
      return sb;
    }
    Enumeration en = v.elements();
    boolean first = true;
    while (en.hasMoreElements()) {
      if (first) {
	first = false;
	sb.append(separatorFirst);
      } else {
	sb.append(separatorInner);
      }
      Object obj = en.nextElement();
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

  public static int getIndexIgnoringCase(String str, String subStr) {
    if (str != null && subStr != null) {
      return (str.toUpperCase()).indexOf(subStr.toUpperCase());
    }
    return -1;
  }
}

