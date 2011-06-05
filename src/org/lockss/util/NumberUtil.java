/*
 * $Id: NumberUtil.java,v 1.1 2011-06-05 19:16:49 pgust Exp $
 */

/*

Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * This utility class contains a collection of number handling static methods.
 * <p>
 * The roman numeral methods accept and output counting numbers in 
 * both Arabic and Roman number systems, from 1 to 2^31-1, or from
 * I to ((MMCXLVII)CDLXXXIII)DCXLVIIMMMCMXCIX.
 * <p>
 * Roman numerals larger than M are represented by lower-valued Roman numerals 
 * within parentheses, indicating multiplication by 1000. Parentheses can be 
 * nested, so 1,000,000 can be represented as either (M) or ((I)). There is no
 * representation of 0 or negative values in the Roman number system. Side bar
 * notation is not currently supported. For details on large Roman numbers, see
 * http://www.web40571.clarahost.co.uk/roman/howtheywork.htm#larger
 */
public class NumberUtil {
  /** A constant holding the minimum value a counting number can have */
  public static final int MIN_COUNTING_NUMBER = 1;
  
  /** A constant holding the maximum value a counting number can have */
  public static final int MAX_COUNTING_NUMBER = Integer.MAX_VALUE;
  
  /** Roman digits and numbers; descending order important for toRoman() */
  private final static Map<String,Integer> romanToNum; 
  static {
    Map<String,Integer> rtn = new LinkedHashMap<String,Integer>();
    rtn.put("M", 1000); rtn.put("CM", 900);
    rtn.put("D", 500); rtn.put("CD", 400);
    rtn.put("C", 100); rtn.put("XC", 90);
    rtn.put("L", 50); rtn.put("XL", 40);
    rtn.put("X", 10); rtn.put("IX", 9);
    rtn.put("V", 5); rtn.put("IV", 4);
    rtn.put("I", 1);
    romanToNum = Collections.unmodifiableMap(rtn);
  }
  
  /** Roman number token formatting strings used by parseRoman() */
  private static String[] romanFmt = {"%s", "(%s)", "((%s))"}; // OK for ints

  /** Cannot create an instance of this utility class */
  private NumberUtil() {}
  
  /**
   * Determines the Roman value of the system property with the specified name.
   * The argument is treated as the name of a system property. The string
   * value of this property is interpreted as Roman or Arabic value and the
   * corresponding Roman object is returned.  Returns null if the system
   * property does not have the correct format, or if the specified name is
   * empty or <tt>null</tt>
   * 
   * @param nm property name
   * @return the Roman value of the property
   */
  public static Integer getCountingNumber(String nm) {
    return NumberUtil.getCountingNumber(nm, null);
  }
  
  /**
   * Determines the value of the system property with the specified name.
   * The first argument is treated as the name of a system property. The string
   * value of this property is interpreted as Arabic or Roman counting number 
   * and the corresponding value is returned.
   * <p>
   * The second argument is the default value. This value is returned if the
   * system property does not have the correct format, or if the value of the
   * specified property is empty or <tt>null</tt>.
   * 
   * @param nm property name
   * @param val the default value
   * @return the Roman value of the property
   */
  public static Integer getCountingNumber(String nm, Integer val) {
    if ((val != null) && !isCountingNumber(val)) {
      throw new IllegalArgumentException("Not a counting number: " + val);
    }
    
    String prop = System.getProperty(nm);
    if (!StringUtil.isNullString(prop)) {
      try {
        val = NumberUtil.parseCountingNumber(prop);
      } catch (NumberFormatException ex) {
      // fall through
      }
    }
    return val;
  }
  
  /**
   * Determines whether the number is a counting number in the range 1 - 2^31-1.
   * 
   * @param n the number
   * @return <tt>true</tt> if number is a counting number
   */
  public static boolean isCountingNumber(int n) {
    return    (n <= NumberUtil.MAX_COUNTING_NUMBER)  
           && (n >= NumberUtil.MIN_COUNTING_NUMBER);
  }
  
  /**
   * Determines whether the number represented by the String is a 
   * counting number in the range 1-2^31-1.
   * 
   * @param n the number
   * @return <tt>true</tt> if number is a counting number
   */
  public static boolean isCountingNumber(String s) {
    try {
      NumberUtil.parseCountingNumber(s);
      return true;
    } catch (NumberFormatException ex) {
      return false;
    }
  }
  
  /**
   * Returns <tt>true</tt> if the Arabic or Roman counting number represented 
   * by the input string is in normal form for its number system. For example,
   * "01" and "IIII" are not normalized, while "1" and "IV" are.
   * 
   * @param s the String
   * @return <tt>true</tt> if String representing Arabic or Roman 
   *  counting number is normalized
   */
  public static boolean isNormalizedCountingNumber(String s) {
    try {
      NumberUtil.parseRomanNumber(s, true);
      return true;
    } catch (NumberFormatException ex) {
      try {
        return NumberUtil.toArabicNumber(s).equals(s);
      } catch (IllegalArgumentException ex2) {
      }
    }
    return false;
  }
  
  /**
   * Determines whether the input string represents a Roman number.
   * 
   * @param s the input String
   * @return <tt>true</tt> if the input string represents a Roman number
   */
  public static boolean isRomanNumber(String roman) {
    try {
      NumberUtil.parseRomanNumber(roman, false);
      return true;
    } catch (NumberFormatException ex) {
      return false;
    }
  }
  /**
   * Returns an integer from a String representing a counting number in the 
   * Arabic or Roman number system, within the range 1 - 2^31-1.
   * 
   * @param s the String
   * @return the number
   * @throws NumberFormatException if the String does not represent a valid
   *     counting number in the Arabic or Roman number systems
   */
  public static int parseCountingNumber(String s) 
    throws NumberFormatException {
    try {
      return NumberUtil.parseRomanNumber(s, false);
    } catch (NumberFormatException ex) {
      int n = Integer.parseInt(s);
      if (!isCountingNumber(n)) {
        throw new NumberFormatException("Not a counting number: " + s);
      }
      return n;
    }
  }
  
  /**
   * Returns an integer from a String array representing a number in
   * the Roman number system. The number must be in the range 1 - 2^31-1.
   * Elements of the array represent individual roman digits, of the 
   * kind returned by {@link #toRomanDigits()}.
   * 
   * @param tokens the array of roman tokens
   * @return the integer value
   */
  public static int parseRomanDigits(String[] tokens) {
    if ((tokens == null) || (tokens.length == 0)) {
      throw new IllegalArgumentException();
    }
    int romanValue = 0;
    for (String s : tokens) {
      int parenCount = s.lastIndexOf('(')+1;
      String token = s.substring(parenCount, s.length()-parenCount);
      Integer val = romanToNum.get(token);
      if (val == null) {
        throw new NumberFormatException("Not a roman digit: " + s);
      }
      // scale value by paren count
      int n = val;
      for (int i = 0; i < parenCount; i++) n *= 1000;
      romanValue += n;
      if (romanValue <= 0) {
        throw new NumberFormatException("Number out of range.");
      }
    }
    
    return romanValue;
  }
  
  /**
   * Returns an integer from a String representing a number in the
   * Roman number system. The number must be within the range 1 - 2^31-1.
   * The string may not contain any other characters than allowed by the 
   * Roman numeral alphabet (the characters 'I', 'V', 'X', 'L', 'C', 'D' 
   * and 'M' are allowed). Parentheses can be used for large Roman numbers.
   * If the string does not represent a Roman number, the exception is thrown. 
   * <p>
   * The input String is validated by comparing it to the String representation
   * of the parsed value, which is in normalized form. There are many examples
   * of non-normal Roman number representations on monuments and books. See the
   * Wikipedia article on "Roman_numerals" for further discussion and examples.
   * 
   * @param roman the String
   * @param validate <code>true</code> if String representation is required
   *     to be in normalized form
   * @return the integer
   * @throws NumberFormatException if the String does not represent a valid
   *    number in the Roman number system
   */
  public static int parseRomanNumber(String roman, boolean validate) 
    throws NumberFormatException {
    int romanValue = NumberUtil.parseRomanNumber(roman, (List<String>)null);
        
    // the algorithm above not only creates a number that is correct for any 
    // well formed Roman string but also some non well formed Roman string 
    // (e.g. IIX), so the trick is to compare the normal string for 8 (VIII) 
    // with the one given (IIX)
    if (validate && !toRomanNumber(romanValue).equals(roman)) {
      throw new NumberFormatException("Not normalized: " + roman);
    }
        
    return romanValue;
  }
  
  /**
   * Returns an integer from a String representing a number in the
   * Roman number system. The number must be in the range 1 - 2^31-1.
   * <p>
   * If a list is provided, it will contain Roman number tokens 
   * corresponding to the input string. The roman string "MCMDLLLIX" 
   * returns ["M" "CM" "D" "L" "L" "L" "IX"], while the string 
   * "(MCM)DLLLIX" returns ["(M)" "(CM)", "D" "L" "L" "IX"]
   * 
   * @param s the input Roman string
   * @return roman array of numbers corresponding to the input number
   * @throws NumberFormatException if the input is not a valid Roman number
   */
  private static int parseRomanNumber(String roman, List<String> list) 
    throws NumberFormatException {
    String notRomanNumber = "Not a roman number: " + roman;
    int romanNumber = 0;

    if (StringUtil.isNullString(roman)) {
      throw new NumberFormatException(notRomanNumber);
    }
    roman = roman.trim().toUpperCase();
    int romanLength = roman.length();

    // determine paren count and corresponding scale of number
    int parenCount = 0;
    int scale = 1;
    int i = 0;
    for (; i < romanLength && (roman.charAt(i) == '('); i++) {
      parenCount++;
      scale *= 1000;  // scale by thousands
    }

    while (i < romanLength) {
      if (roman.charAt(i) == ')') {
        if (--parenCount < 0) {
          throw new NumberFormatException(notRomanNumber);
        }
        scale /= 1000;
        i++;
      } else {
        Integer val = null;
        String token = null;
        try {
          token = roman.substring(i,i+2);
          val = romanToNum.get(token);
        } catch (IndexOutOfBoundsException ex) {}  // substring checks args
        if (val == null) {
          token = roman.substring(i,i+1);
          val = romanToNum.get(token);
          if (val == null) {
            throw new NumberFormatException(notRomanNumber);
          }
        }
        // add scaled value and validate number
        romanNumber += val * scale;
        if (romanNumber <= 0) {
          throw new NumberFormatException(notRomanNumber);
        }
        // add roman token to return list 
        if (list != null) {
          list.add(String.format(romanFmt[parenCount], token));
        }
        
        i += token.length();
      }
    }
    
    if (parenCount != 0) {
      throw new NumberFormatException(notRomanNumber);
    }

    return romanNumber;
  }
  
  /**
   * Splits String representing an Arabic or Roman number into 
   * Roman number digits. The string "MCMDLLLIX" returns the array 
   * ["M" "CM" "D" "L" "L" "L" "IX"], while the string "(MCM)DLLLIX"
   * returns ["(M)" "(CM)", "D" "L" "L" "IX"]
   * <p>
   * The return value will not be normalized if the input String
   * represents a non-normalized roman number 
   * 
   * @param s the input Roman string
   * @return roman array of tokens corresponding to the input number
   * @throws NumberFormatException if the input is not a valid Roman number
   */
  public static String[] toRomanDigits(String s) 
    throws NumberFormatException {
    List<String> romanChars = new ArrayList<String>();
    try {
      NumberUtil.parseRomanNumber(s, romanChars);
    } catch (NumberFormatException ex) {
      int n = Integer.parseInt(s);
      if (!NumberUtil.isCountingNumber(n)) {
        throw new NumberFormatException("Not a counting number: " + s);
      }
      parseRomanNumber(NumberUtil.toRomanNumber(n), romanChars);
    }
    return romanChars.toArray(new String[romanChars.size()]);
  }
  
  /**
   * Returns a String representation of an Arabic counting number
   * that is equivalent to an Arabic or Roman counting number string. 
   * 
   * @param s string representing Arabic or possibly unnormalized Roman number
   * @return the normalized form of the input Roman number
   * @throws NumberFormatException if the String does not represent a valid
   *  positive number in the Arabic Roman number system
   */
  public static String toArabicNumber(String s) 
    throws NumberFormatException {
    return Integer.toString(NumberUtil.parseCountingNumber(s));
  }
  
  /**
   * Returns a String representation of a normalized Roman number that is 
   * equivalent to an Arabic or Roman counting number string. 
   * <p>
   * There are many examples of non-normal Roman number representations on 
   * monuments and books. See the Wikipedia article on "Roman_numerals" 
   * for further discussion and examples.
   * 
   * @param s string representing Arabic or possibly unnormalized Roman number
   * @return the normalized form of the input Roman number
   * @throws NumberFormatException if the String does not represent a valid
   *    positive number in the Arabic Roman number system
   */
  public static String toRomanNumber(String s) 
    throws NumberFormatException {
    return NumberUtil.toRomanNumber(NumberUtil.parseCountingNumber(s));
  }
  
  /**
   * Returns the String representation of the value in the Roman number system.
   * The value must be in the range 1-2^31-1, or IllegalArgumentException is
   * thrown.
   *  
   * @param value the value
   * @return the String representation in the Roman number system
   */
  public static String toRomanNumber(int value) {
    if (!isCountingNumber(value)) {
      throw new IllegalArgumentException("Not a counting number: " + value);
    }
    StringBuilder result = new StringBuilder();
    
    if (value > 3999) {  // largest "regular" roman number
      // calculate upper roman number to be multiplied by 1000 
      String upper = Roman.toString(value/1000);
      
      // extract lower value to be converted directly
      value = value%1000;

      // normalize: shift trailing 'I's in upper part to 'M's in lower part;
      // e.g. 2852429 is "(MMMDCCCL)MMCDXXIX" instead of "(MMMDCCCLII)CDXXIX". 
      // This normalized form of the Roman number is shown at: 
      // http://www.web40571.clarahost.co.uk/roman/howtheywork.htm#larger
      int i = 0;
      if (upper.endsWith("III")) i = 3;
      else if (upper.endsWith("II")) i = 2;
      else if (upper.endsWith("I")) i = 1;
      
      result.append('(');
      result.append(upper, 0, upper.length()-i);
      result.append(')');
      result.append("MMM", 0, i);
    }
    
    // start with largest value, and work toward smallest.
    for (Map.Entry<String,Integer> entry : romanToNum.entrySet()) {
      // remove as many of this value as possible (maybe none).
      while (value >= entry.getValue()) {
        value -= entry.getValue();      // Subtract value
        result.append(entry.getKey());  // Add roman equivalent
      }
    }
    return result.toString();
  }
  
  
  /**
   * Show usage message and exit.
   */
  private static void showUsageAndExit() {
    System.out.print(
        "Usage:"
        + "\n\tjava NumberUtil -i <counting_number>"
        + "\n\tjava NumberUtil -i <start counting_number> <end counting_number>"
        + "\n\tjava NumberUtil -r <counting_number>"
        + "\n\tjava NumberUtil -r <start counting_number> <end counting_number>"
        + "\n");
    System.exit(-1);
  }
        
  /**
   * This method is used for testing purposes.
   * @param args the test arguments
   */
  public static void main(String[] args) {
    // obviously only ready for test purposes, very ugly code
    try {
      if (args.length >= 2) {
        int start = NumberUtil.parseCountingNumber(args[1]);
        int end = (args.length > 2) 
                    ? NumberUtil.parseCountingNumber(args[2]) : start;
        for (int i = start; i <= end; i++) {
          if (args[0].equals("-i")) {
              System.out.println(i);
          } else if (args[0].equals("-r")) {
              System.out.println(NumberUtil.toRomanNumber(i));
          } else {
            showUsageAndExit();
          }
        }
      } else {
        showUsageAndExit();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
