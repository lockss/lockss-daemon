package org.lockss.util;

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

/**
 * This is a class to contain generic string utilities
 *
 * @author  Thomas S. Robertson
 * @version 0.0
 */

public class StringUtils{

  /**
   * Method to trim the end off of a string as soon as it encounters
   * any one of the characters specified
   *
   * @param str String to trim
   * @param chars String containing the chars to trim after
   * @returns str turncated at the first occurance of any of the chars
   */
  public static String trimAfterChars(String str, String chars){
    if (str == null){
      return null;
    }
    if (chars != null){
      for (int ix=0; ix < str.length(); ix++){
	for (int jx=0; jx < chars.length(); jx++){
	  if (str.charAt(ix) == chars.charAt(jx)){
	    return str.substring(0, ix);
	  }
	}
      }
    }
    return str;
  }

  public static int getIndexIgnoringCase(String str, String subStr){
    if (str != null && subStr != null){
      return (str.toUpperCase()).indexOf(subStr.toUpperCase());
    }
    return -1;
  }
}

