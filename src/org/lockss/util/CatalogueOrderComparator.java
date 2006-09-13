/*
 * $Id: CatalogueOrderComparator.java,v 1.6 2006-09-13 17:48:45 adriz Exp $
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
import org.apache.commons.lang.StringUtils;


/**
 * Comparator that implements a suitable ordering for titles in a library
 * catalogue.  Punctuation and initial determiners are removed, then a
 * case-independent comparison is done.  The translated sort keys are
 * cached, so performance will be enhanced by reusing the same instance of
 * the comparator.  The singleton {@link #SINGLETON} is provided for that
 * purpose.
 */

public class CatalogueOrderComparator implements Comparator {
  static final String PUNCTUATION = ".,-:;\"\'/?()[]{}<>!#";
  static final int PADLEN = 9;
  /** An instance of the comparator. */
  public static final CatalogueOrderComparator SINGLETON =
    new CatalogueOrderComparator();

  Map keyMap = Collections.synchronizedMap(new HashMap());

  public int compare(Object o1, Object o2) {
    if (!((o1 instanceof String)
	  && (o2 instanceof String))) {

      throw new IllegalArgumentException("CatalogueOrderComparator(" +
					 o1.getClass().getName() + "," +
					 o2.getClass().getName() + ")");
    }
    return compare((String)o1, (String)o2);
  }

  public int compare(String s1, String s2) {
    return getSortKey(s1).compareToIgnoreCase(getSortKey(s2));
  }

  String getSortKey(String s) {
    String key = (String)keyMap.get(s);
    if (key == null) {
      key = xlate(s);
      keyMap.put(s, key);
    }
    return key;
  }

  String xlate(String s) {
    s = findNumsPadZero(s, "0123456789",PADLEN);  
    s = s.trim();
    s = deleteAll(s, PUNCTUATION);
    s = deleteSpaceBetweenInitials(s);
    s = deleteInitial(s, "a");
    s = deleteInitial(s, "an");
    s = deleteInitial(s, "the");
    s = replaceAccentedChar(s);
    return s;
  }

  String findNumsPadZero(String s, String allAnyCharStr, int padLen) {
    StringBuffer sb = new StringBuffer();
    String sub2Str = s;
    int digIx = StringUtils.indexOfAny(sub2Str, allAnyCharStr);	  
    while (digIx >= 0 ){ 
      String sub1Str = sub2Str.substring(0, digIx);  
      int  numStrLen= getNumStrLen( sub2Str, digIx );// add getNumStrLen to our stringUtil   
      String numStr = sub2Str.substring(digIx, digIx + numStrLen ); // does auto -1 to desired length
      numStr = StringUtils.leftPad(numStr, padLen, "0");   
           //sb = sb + sub1Str + numStr;
            sb.append(sub1Str) ;
            sb.append(numStr) ;
      sub2Str = sub2Str.substring( digIx + numStrLen );
      digIx = StringUtils.indexOfAny(sub2Str, allAnyCharStr);
    }  
    if ( sb.length()== 0 ){  
      sb.append(s);
    }
    else{   
      sb.append(sub2Str) ;    
    }
    return sb.toString();
  }

  int getNumStrLen( String str, int ixStr ){
    int numLen=0;
    while(  Character.isDigit( str.charAt(ixStr) )   ){
  	  ixStr++;
  	  numLen++;
  	  if ( ixStr >= str.length() ){ 
  		  break;
  	  }		  
    } 
    return numLen;	  
  }
  
  String deleteInitial(String s, String sub) {
    int sublen = sub.length();
    if (StringUtil.startsWithIgnoreCase(s, sub) &&
	s.length() > sublen &&
	Character.isWhitespace(s.charAt(sublen))) {
      s = s.substring(sublen + 1, s.length());
      s = s.trim();
    }
    return s;
  }

  // assume all the letter in the abbreviation are in uppercase
  // The method will also turn consecutive whitespace in to one whitespace
  String deleteSpaceBetweenInitials(String s) {
    boolean deleteStart = false;
    StringBuffer sTmp = new StringBuffer();
    StringTokenizer st = new StringTokenizer(s," ");
    while (st.hasMoreTokens()) {
	String token = st.nextToken();
        //check the token is a single uppercase character
	if (token.length() == 1 &&
            Character.isUpperCase(token.charAt(0))) {
	    if (deleteStart) {
	      sTmp = sTmp.append(token);
            }
	    else { //when encounter the first Letter in the abbreviation
	      sTmp = sTmp.append(" ").append(token);
	      deleteStart = true;
	    }
	}
	else {
	  sTmp = sTmp.append(" ").append(token);
	  deleteStart = false;
	}
    }
    return sTmp.toString().trim();
  }

  String deleteAll(String s, String chars) {
    for (int ix = 0; ix < chars.length(); ix++) {
      String c = chars.substring(ix, ix + 1);
      s = StringUtil.replaceString(s, c, "");
    }
    return s;
  }

    /** the accented character table except {Ð(\u00d0),Þ(\u00de),ß(\u00df), ð(\u00f0),þ(\u00fe)} */
  static final String[][] ACCENTTABLE = {
      {"\u00c0","A"}, // À, A with grave
      {"\u00c1","A"}, // Á, A with acute
      {"\u00c2","A"}, // Â, A with circumflex
      {"\u00c3","A"}, // Â, A with tilde
      {"\u00c4","A"}, // Ä, A with diaeresis
      {"\u00c5","A"}, // Å, A with ring above
      {"\u00c6","AE"}, // Æ, AE
      {"\u00c7","C"}, // Ç, C with cedilla
      {"\u00c8","E"}, // È, E with grave
      {"\u00c9","E"}, // É, E with acute
      {"\u00ca","E"}, // Ê, E with circumflex
      {"\u00cb","E"}, // Ë, E with diaeresis
      {"\u00cc","I"}, // Ì, I with grave
      {"\u00cd","I"}, // Í, I with acute
      {"\u00ce","I"}, // Î, I with circumflex
      {"\u00cf","I"}, // Ï, I with diaeresis
      {"\u00d1","N"}, // Ñ, N with tilde
      {"\u00d2","O"}, // Ò, O with grave
      {"\u00d3","O"}, // Ó, O with acute
      {"\u00d4","O"}, // Ô, O with circumflex
      {"\u00d5","O"}, // Õ, O with tilde
      {"\u00d6","O"}, // Ö, O with diaeresis
      {"\u00d8","O"}, // Ø, O with a stroke
      {"\u00d9","U"}, // Ù, U with grave
      {"\u00da","U"}, // Ú, U with acute
      {"\u00db","U"}, // Û, U with circumflex
      {"\u00dc","U"}, // Ü, U with diaeresis
      {"\u00dd","Y"}, // Ý, Y with acute
      {"\u00e0","a"}, // à, a with grave
      {"\u00e1","a"}, // á, a with acute
      {"\u00e2","a"}, // â, a with circumflex
      {"\u00e3","a"}, // ã, a with tilde
      {"\u00e4","a"}, // ä, a with diaeresis
      {"\u00e5","a"}, // å, a with ring above
      {"\u00e6","ae"}, // æ, ae
      {"\u00e7","c"}, // ç, c with cedilla
      {"\u00e8","e"}, // è, e with grave
      {"\u00e9","e"}, // é, e with acute
      {"\u00ea","e"}, // ê, e with circumflex
      {"\u00eb","e"}, // ë, e with diaeresis
      {"\u00ec","i"}, // ì, i with grave
      {"\u00ed","i"}, // í, i with acute
      {"\u00ee","i"}, // î, i with circumflex
      {"\u00ef","i"}, // ï, i with diaeresis
      {"\u00f1","n"}, // ñ, n with tilde
      {"\u00f2","o"}, // ò, o with grave
      {"\u00f3","o"}, // ó, o with acute
      {"\u00f4","o"}, // ô, o with circumflex
      {"\u00f5","o"}, // õ, o with tilde
      {"\u00f6","o"}, // ö, o with diaeresis
      {"\u00f8","o"}, // ø, o with stroke
      {"\u00f9","u"}, // ù, u with grave
      {"\u00fa","u"}, // ú, u with acute
      {"\u00fb","u"}, // û, u with circumflex
      {"\u00fc","u"}, // ü, u with diaeresis
      {"\u00fd","y"}, // ý, y with acute
      {"\u00ff","y"}, // ÿ, y with diaeresis
                                        };

  String replaceAccentedChar(String s) {
    for (int iy = 0; iy < ACCENTTABLE.length; iy++) {
      s = StringUtil.replaceString(s, ACCENTTABLE[iy][0], ACCENTTABLE[iy][1]);
    }
    return s;
  }

}
