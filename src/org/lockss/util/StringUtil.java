/*
 * $Id: StringUtil.java,v 1.38.2.1 2004-05-20 08:55:49 tlipkis Exp $
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
import java.text.*;
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
    int oldLen = oldstr.length();
    if (oldLen == 0 || oldstr.equals(newstr)) {
      return line;
    }
    int thisIdx = line.indexOf(oldstr);
    if (thisIdx < 0) {
      return line;
    }
    int lineLen = line.length();
    StringBuffer sb = new StringBuffer(lineLen);
    int oldIdx = 0;
    do {
      for (int ix = oldIdx; ix < thisIdx; ix++) {
	sb.append(line.charAt(ix));
      }
      sb.append(newstr);
      oldIdx = thisIdx + oldLen;
    } while ((thisIdx = line.indexOf(oldstr, oldIdx)) >= 0);
    for (int ix = oldIdx; ix < lineLen; ix++) {
      sb.append(line.charAt(ix));
    }
    return sb.toString();
  }

  public static String replaceFirst(String line, String oldstr, String newstr) {
    int oldLen = oldstr.length();
    if (oldLen == 0 || oldstr.equals(newstr)) {
      return line;
    }
    int index = line.indexOf(oldstr);
    if (index < 0) {
      return line;
    } else {
      int lineLen = line.length();
      StringBuffer sb = new StringBuffer(lineLen);
      sb.append(line.substring(0, index));
      sb.append(newstr);
      if (index + oldLen < lineLen) {
        sb.append(line.substring(index + oldLen));
      }
      return sb.toString();
    }
  }

  /**
   * Concatenate elements of collection into string, with separators
   * @param c - Collection of object (on which toString() will be called)
   * @param separator - String to put between elements
   * @return Concatenated string
   */
  public static String separatedString(Collection c, String separator) {
    return separatedString(c, "", separator, "",
			   new StringBuffer()).toString();
  }

  /**
   * Concatenate elements of object array into string, with separators
   * @param arr - Array of object (on which toString() will be called)
   * @param separator - String to put between elements
   * @return Concatenated string
   */
  public static String separatedString(Object arr[], String separator) {
    return separatedString(ListUtil.fromArray(arr), "", separator, "",
			   new StringBuffer()).toString();
  }

  /**
   * Concatenate elements of collection into string, with separators
   * @param c - Collection of object (on which toString() will be called)
   * @param separator - String to put between elements
   * @param sb - StringBuffer to write result into
   * @return sb
   */
  public static StringBuffer separatedString(Collection c, String separator,
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
  public static String separatedDelimitedString(Collection c, String separator,
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
  public static String separatedDelimitedString(Collection c, String separator,
                                                String delimiter1,
                                                String delimiter2) {
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
  public static String terminatedSeparatedString(Collection c, String separator,
                                                 String terminator) {
    return separatedString(c, "", separator, terminator,
			   new StringBuffer()).toString();
  }

  /**
   * Concatenate elements of collection into string, adding separators,
   * delimitig each element
   * @param c - Collection of object (on which toString() will be called)
   * @param separatorFirst - String to place before first element
   * @param separatorInner - String with which to separate elements
   * @param separatorLast - String to place after last element
   * @return Concatenated string
   */
  public static StringBuffer separatedString(Collection c,
					     String separatorFirst,
                                             String separatorInner,
                                             String separatorLast,
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

  /** Break a string at a separator char, returning a vector of at most
   * maxItems strings.
   * @param discardEmptyStrings if true, empty strings (caused by delimiters
   * at the start or end of the string, or adjacent delimiters) will not be
   * included in the result.
   * @param trimEachString is true, each string in the result will be trim()ed
   */
  public static Vector breakAt(String s, char sep,
			       int maxItems,
			       boolean discardEmptyStrings,
			       boolean trimEachString) {
    Vector res = new Vector();
    int len;
    if (s == null || (len = s.length()) == 0) {
      return res;
    }
    if (maxItems <= 0) {
      maxItems = Integer.MAX_VALUE;
    }
    for (int pos = 0; maxItems > 0; maxItems-- ) {
      int end = s.indexOf(sep, pos);
      if (end == -1) {
	if (pos > len) {
	  break;
	}
	end = len;
      }
      if (!discardEmptyStrings || pos != end) {
	String str = s.substring(pos, end);
	if (trimEachString) {
	  str = str.trim();
	}
	if (!discardEmptyStrings || str.length() != 0) {
	  res.addElement(str);
	}
      }
      pos = end + 1;
    }
    return res;
  }

  /** Break a string at a separator char, returning a vector of at most
   * maxItems strings.
   * @param discardEmptyStrings if true, empty strings (caused by delimiters
   * at the start or end of the string, or adjacent delimiters) will not be
   * included in the result. */
  public static Vector breakAt(String s, char sep,
			       int maxItems,
			       boolean discardEmptyStrings) {
    return breakAt(s, sep, maxItems, discardEmptyStrings, false);
  }

  /** Break a string at a separator char, returning a vector of strings.
   * Include any empty strings in the result. */
  public static Vector breakAt(String s, char sep) {
    return breakAt(s, sep, 0);
  }

  /** Break a string at a separator char, returning a vector of at most
   * maxItems strings.  Include any empty strings in the result. */
  public static Vector breakAt(String s, char sep, int maxItems) {
    return breakAt(s, sep, maxItems, false);
  }

  /** Temporary name for truncateAtAny() */
  public static String trimAfterChars(String str, String chars) {
    return truncateAtAny(str, chars);
  }

  /**
   * Trim the end off of a string starting at any of the characters specified.
   *
   * @param str String to trim
   * @param chars String containing the chars to trim at
   * @return str turncated at the first occurance of any of the chars, or
   * the original string if no occurances
   */
  public static String truncateAtAny(String str, String chars) {
    if (str == null) {
      return null;
    }
    if (chars != null) {
      for (int jx=0; jx < chars.length(); jx++) {
	int pos = str.indexOf(chars.charAt(jx));
	if (pos >= 0) {
	  return str.substring(0, pos);
	}
      }
    }
    return str;
  }

  /**
   * Trim the end off of a string starting at the specified character.
   *
   * @param str String to trim
   * @param chr char to trim at
   * @return str turncated at the first occurance of char, or
   * the original string if no occurance
   */
  public static String truncateAt(String str, char chr) {
    if (str == null) {
      return null;
    }
    int pos = str.indexOf(chr);
    if (pos < 0) {
      return str;
    }
    return str.substring(0, pos);
  }

  /** Like indexOf except is case-independent */
  public static int getIndexIgnoringCase(String str, String subStr) {
    if (str != null && subStr != null) {
      return (str.toUpperCase()).indexOf(subStr.toUpperCase());
    }
    return -1;
  }

  /** Like endsWith except is case-independent */
  public static boolean endsWithIgnoreCase(String str, String end) {
    int lend = end.length();
    return str.regionMatches(true, str.length() - lend, end, 0, lend);
  }

  /** Like startsWith except is case-independent */
  public static boolean startsWithIgnoreCase(String str, String start) {
    return str.regionMatches(true, 0, start, 0, start.length());
  }

  /** Remove the substring beginning with the final occurrence of the
   * separator, if any. */
  public static String upToFinal(String str, String sep) {
    int pos = str.lastIndexOf(sep);
    if (pos < 0) {
      return str;
    }
    return str.substring(0, pos);
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
  public static int countOccurences(String str, String subStr) {
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

  /* Return a string with all the characters from a reader */
  public static String fromReader(Reader r) throws IOException {
    char[] buf = new char[1000];
    StringBuffer sb = new StringBuffer(1000);
    int len;
    while ((len = r.read(buf)) >= 0) {
      sb.append(buf, 0, len);
    }
    return sb.toString();
  }

  /* Return a string with all the characters from an InputStream */
  public static String fromInputStream(InputStream in) throws IOException {
    // use our default encoding rather than system default
    return fromReader(new InputStreamReader(in, Constants.DEFAULT_ENCODING));
  }

  /** Reads in the entire contents of a file into a string */
  public static String fromFile(String path) throws IOException {
    return fromReader(new FileReader(path));
  }

  /** Reads in the entire contents of a file into a string */
  public static String fromFile(File file) throws IOException {
    return fromReader(new FileReader(file));
  }

  /**
   * Test whether a string is null or the empty string
   * @param s the string
   * @return true if s is null or the empty string
   */
  public static boolean isNullString(String s) {
    return s == null || s.length() == 0;
  }

  /**
   * Compare two strings for equality or both null.
   * @param s1 string 1
   * @param s2 string 2
   * @return true if strings are equal or both null
   */
  public static boolean equalStrings(String s1, String s2) {
    if (s1 == null) {
      return s2 == null;
    } else {
      return s1.equals(s2);
    }
  }

  /**
   * Compare two strings for case-independent equality or both null.
   * @param s1 string 1
   * @param s2 string 2
   * @return true if strings are equal or both null
   */
  public static boolean equalStringsIgnoreCase(String s1, String s2) {
    if (s1 == null) {
      return s2 == null;
    } else {
      return s1.equalsIgnoreCase(s2);
    }
  }

  private static long gensymCtr = 0;

  /**
   * Generate a unique string.
   * @param base the initial substring
   * @return a string consisting of the supplied initial substring and a
   * unique counter value.
   */
  public static String gensym(String base) {
    return base + (gensymCtr++);
  }

  /**
   * Trim a hostname, removing "www." from the front, if present, and the
   * TLD from the end.  If this would result in an empty string, the entire
   * name is returned.
   * @param hostname a hostname string
   * @return the trimmed hostname
   */
  public static String trimHostName(String hostname) {
    if (hostname == null) return null;
    int start = 0;
    if (hostname.regionMatches(true, 0, "www.", 0, 4)) {
      start = 4;
    }
    int end = hostname.lastIndexOf('.');
    if (end <= start) {
      // if trimming www left nothing but TLD, return whole name
      return hostname;
    }
    return hostname.substring(start, end);
  }

  /** Parse a string as a time interval.  An interval is specified as an
   * integer with an optional suffix.  No suffix means milliseconds, s, m,
   * h, d, w indicates seconds, minutes, hours, days and weeks
   * respectively.
   * @param str the interval string
   * @return interval in milliseconds
   */
  // tk - extend to accept combinations: xxHyyMzzS, etc.
  public static long parseTimeInterval(String str) {
    try {
      int len = str.length();
      char suffix = str.charAt(len - 1);
      String numstr;
      long mult = 1;
      if (Character.isDigit(suffix)) {
	numstr = str;
      } else {
	numstr = str.substring(0, len - 1);
	switch (Character.toUpperCase(suffix)) {
	case 'S': mult = Constants.SECOND; break;
	case 'M': mult = Constants.MINUTE; break;
	case 'H': mult = Constants.HOUR; break;
	case 'D': mult = Constants.DAY; break;
	case 'W': mult = Constants.WEEK; break;
	default:
	  throw new NumberFormatException("Illegal time interval suffix");
	}
      }
      return Long.parseLong(numstr) * mult;
    } catch (IndexOutOfBoundsException e) {
      throw new NumberFormatException("empty string");
    }
  }

  /** Trim leading and trailing blank lines from a block of text */
  public static String trimBlankLines(String txt) {
    StringBuffer buf = new StringBuffer(txt);
    while (buf.charAt(0) == '\n') {
      buf.deleteCharAt(0);
    }
    while (buf.charAt(buf.length() - 1) == '\n') {
      buf.deleteCharAt(buf.length() - 1);
    }
    return buf.toString();
  }


  // Unit Descriptor
  private static class UD {
    String str;				// suffix string
    long millis;			// milliseconds in unit
    int threshold;			// min units to output
    String stop;			// last unit to output if this matched

    UD(String str, long millis) {
      this(str, millis, 1);
    }

    UD(String str, long millis, int threshold) {
      this(str, millis, threshold, null);
    }

    UD(String str, long millis, int threshold, String stop) {
      this.str = str;
      this.millis = millis;
      this.threshold = threshold;
      this.stop = stop;
    }
  }

  static UD units[] = {
    new UD("w", Constants.WEEK, 3, "h"),
    new UD("d", Constants.DAY, 1, "m"),
    new UD("h", Constants.HOUR),
    new UD("m", Constants.MINUTE),
    new UD("s", Constants.SECOND, 0),
  };

  /** Generate a string representing the time interval.
   * @param millis the time interval in milliseconds
   * @return a string in the form dDhHmMsS
   */
  public static String timeIntervalToString(long millis) {
    StringBuffer sb = new StringBuffer();
    if (millis < 10 * Constants.SECOND) {
      sb.append(millis);
      sb.append("ms");
    } else {
      boolean force = false;
      String stop = null;
      for (int ix = 0; ix < units.length; ix++) {
	UD iu = units[ix];
	long n = millis / iu.millis;
	if (force || n >= iu.threshold) {
	  millis %= iu.millis;
	  sb.append(n);
	  sb.append(iu.str);
	  force = true;
	  if (stop == null) {
	    if (iu.stop != null) {
	      stop = iu.stop;
	    }
	  } else {
	    if (stop.equals(iu.str)) {
	      break;
	    }
	  }
	}
      }
    }
    return sb.toString();
  }

  private static final NumberFormat fmt_1dec = new DecimalFormat("0.0");
  private static final NumberFormat fmt_0dec = new DecimalFormat("0");

  static final String[] byteSuffixes = {"KB", "MB", "GB", "TB"};

  public static String sizeKBToString(long size) {
    double base = 1024.0;
    double x = (double)size;
    
    int len = byteSuffixes.length;
    for (int ix = 0; ix < len; ix++) {
      if (x < base || ix == len-1) {
	StringBuffer sb = new StringBuffer();
	if (x < 10.0) {
	  sb.append(fmt_1dec.format(x));
	} else {
	  sb.append(fmt_0dec.format(x));
	}
	sb.append(byteSuffixes[ix]);
	return sb.toString();
      }
      x = x / base;
    }
    return ""+size;
  }

  /** Remove the first line of the stack trace, iff it duplicates the end
   * of the exception message */
  public static String trimStackTrace(String msg, String trace) {
    int pos = trace.indexOf("\n");
    if (pos > 0) {
      String l1 = trace.substring(0, pos);
      if (msg.endsWith(l1)) {
	return trace.substring(pos + 1);
      }
    }
    return trace;
  }

  /** Translate an exception's stack trace to a string.
   */
  public static String stackTraceString(Throwable th) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    th.printStackTrace(pw);
    return sw.toString();
  }

  /** Convert the first character and every character that follows a space
   *   to uppercase.
   */
  public static String titleCase(String txt) {
    StringBuffer buf = new StringBuffer(txt);
    int len = buf.length();
    buf.setCharAt(0,Character.toUpperCase(buf.charAt(0)));
    for (int i=1; i<len; i++) {
      if (buf.charAt(i-1)==' ') {
        buf.setCharAt(i,Character.toUpperCase(buf.charAt(i)));
      }
    }
    return buf.toString();
  }

}
