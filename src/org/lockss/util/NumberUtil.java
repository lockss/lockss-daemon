/*
 * $Id: NumberUtil.java,v 1.6.2.2 2011-09-09 17:53:00 easyonthemayo Exp $
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
 * The roman numeral methods accept and output natural numbers in 
 * both Arabic and Roman number systems, from 0 to 2^31-1, or from
 * N to ((MMCXLVII)CDLXXXIII)DCXLVIIMMMCMXCIX. Bede first used 'N' (nulla) 
 * for 0 c. 725AD in a table of lunar epacts.
 * <p>
 * Roman numerals larger than M are represented by lower-valued Roman numerals 
 * within parentheses, indicating multiplication by 1000. Parentheses can be 
 * nested, so 1,000,000 can be represented as either (M) or ((I)). There is no
 * representation of 0 or negative values in the Roman number system. Side bar
 * notation is not currently supported. For details on large Roman numbers, see
 * http://www.web40571.clarahost.co.uk/roman/howtheywork.htm#larger
 */
public class NumberUtil {
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
   * Get the range start. Whitespace is trimmed.
   * @param  range a start/stop range separated by a dash, optionally with whitesapce
   * @return the range start <code>null</code> if not specified
   */
  static public String getRangeStart(String range) {
    if (range == null) {
      return null;
    }
    int i = range.indexOf('-');
    return (i > 0) ? range.substring(0,i).trim() : range.trim();
  }

  /**
   * Get the range end. Whitespace is trimmed.
   * @param range a start/stop range separated by a dash, optionally with whitesapce
   * @return the range end or <code>null</code> if not specified
   */
  static public String getRangeEnd(String range) {
    if (range == null) {
      return null;
    }
    int i = range.indexOf('-');
    return (i > 0) ? range.substring(i+1).trim() : range.trim();
  }

  /**
   * Determine whether a range includes a given value. The range can be a
   * single value  or a start/stop range separated by a dash. If the range
   * is a single value, it will be used as both the start and stop values.
   * <p>
   * If the range and value can be interpreted as numbers (Arabic or Roman),
   * the value is compared numerically with the range. Otherwise, the value 
   * is compared to the range start and stop values as a topic range. That 
   * is, "Georgia", "Kanasas", and "Massachusetts" are topics within the 
   * volume "Ge-Ma.
   * <p>
   * The range string can also include whitespace, which is trimmed from the 
   * resulting year. Java's <code>String.trim()</code> method trims blanks and 
   * also control characters, but doesn't trim all the forms of blank 
   * specified in the Unicode character set. The value to search for is 
   * <i>not</i> trimmed.
   * 
   * @param range a single value or a start/stop range separated by a dash, optionally with whitesapce
   * @param value the value
   * @return <code>true</code> if this range includes the value
   */
  static public boolean rangeIncludes(String range, String value) {
    if ((range == null) || (value == null)) {
      return false;
    }
    int i = range.indexOf('-');
    String startRange = (i > 0) ? range.substring(0,i).trim() : range.trim();
    String endRange = (i > 0) ? range.substring(i+1).trim() : range.trim();
    try {
      // see if value is within range
      int srange = NumberUtil.parseInt(startRange);
      int erange = NumberUtil.parseInt(endRange);
      int ival = NumberUtil.parseInt(value);
      return ( ival >= srange && ival <= erange);
    } catch (NumberFormatException ex) {
      // can't compare numerically, so compare as topic ranges
      return     (value.compareTo(startRange) >= 0) 
              && (   (value.compareTo(endRange) <= 0)
                  || value.startsWith(endRange));
    }
  }

  /**
   * Determines the value of the system property with the specified name.
   * The argument is treated as the name of a system property. The string
   * value of this property is interpreted as Arabic or Roman number 
   * and the corresponding value is returned.
   * 
   * @param nm property name
   * @return the Integervalue of the property or <tt>null</tt> if the property
   *   is not defined or is not an integer
   */
  public static Integer getInteger(String nm) {
    return NumberUtil.getInteger(nm, null);
  }
  
  /**
   * Determines the value of the system property with the specified name.
   * The first argument is treated as the name of a system property. The string
   * value of this property is interpreted as Arabic or Roman natural number 
   * and the corresponding value is returned.
   * <p>
   * The second argument is the default value. This value is returned if the
   * system property does not have the correct format, or if the value of the
   * specified property is empty or <tt>null</tt>.
   * 
   * @param nm property name
   * @param val the default value
   * @return the Integer value of the property
   */
  public static Integer getInteger(String nm, Integer val) {
    String prop = System.getProperty(nm);
    if (!StringUtil.isNullString(prop)) {
      try {
        val = NumberUtil.parseInt(prop);
      } catch (NumberFormatException ex) {
      // fall through
      }
    }
    return val;
  }
  
  /**
   * Determines whether the input string represents a Roman number.
   * 
   * @param s the input String
   * @return <tt>true</tt> if the input string represents a Roman number
   */
  public static boolean isRomanNumber(String s) {
    try {
      NumberUtil.parseRomanNumber(s, false);
      return true;
    } catch (NumberFormatException ex) {
      return false;
    }
  }

  /**
   * Determines whether the input string represents an integer, that is,
   * can be parsed as an integer.
   * 
   * @param s the input String
   * @return <tt>true</tt> if the input string represents an integer
   */
  public static boolean isInteger(String s) {
    try {
      Integer.parseInt(s);
      return true;
    } catch (NumberFormatException ex) {
      return false;
    }
  }
  
  /**
   * Determines whether the two integers are are strictly consecutive, that is,
   * whether the second integer comes directly after the first.
   * @param first the first integer
   * @param second the second integer
   * @return <tt>true</tt> if the second integer is one greater than the first
   */
  public static final boolean areConsecutive(int first, int second) {
    return second - first == 1;
  }

  /**
   * Determines whether two integers supplied as strings are strictly 
   * consecutive, that is, whether the second integer comes directly after the 
   * first. If the strings cannot be parsed as integers, an exception is thrown.
   * Note that this method will also accept strings representing Roman numerals.
   * 
   * @param first the first string representing an integer
   * @param second the second string representing an integer
   * @return <tt>true</tt> if the strings could be parsed and the second integer is one greater than the first
   * @throws NumberFormatException if the Strings do not represent valid numbers
   */
  public static final boolean areConsecutive(String first, String second) throws NumberFormatException {
    return areConsecutive(parseInt(first), parseInt(second));
  }

  /**
   * Returns a short from a String representing a number in the 
   * Arabic or Roman number system.
   * 
   * @param s the String
   * @return the number
   * @throws NumberFormatException if the String does not represent a valid
   *     short in the Arabic or Roman number systems
   */
  public static short parseShort(String s) 
    throws NumberFormatException {
    try {
      return Short.parseShort(s);
    } catch (NumberFormatException ex) {
      int value = NumberUtil.parseRomanNumber(s);
      if (value > Short.MAX_VALUE) {
        throw new NumberFormatException(
            "Value out of range. Value:\"" + s + "\"");
      }
      return (short)value;
    }
  }
  
  /**
   * Returns an integer from a String representing a number in the 
   * Arabic or Roman number system. Trims the string first to allow for 
   * whitespace. Java's <code>String.trim()</code> method trims blanks and 
   * also control characters, but doesn't trim all the forms of blank 
   * specified in the Unicode character set.
   * 
   * @param s the String
   * @return the number
   * @throws NumberFormatException if the String does not represent a valid
   *     integer in the Arabic or Roman number systems
   */
  public static int parseInt(String s) 
    throws NumberFormatException {
    try {
      if (!StringUtil.isNullString(s)) s = s.trim(); 
      return Integer.parseInt(s);
    } catch (NumberFormatException ex) {
      return NumberUtil.parseRomanNumber(s);
    }
  }
  
  /**
   * Returns a long from a String representing a number in the 
   * Arabic or Roman number system.
   * 
   * @param s the String
   * @return the number
   * @throws NumberFormatException if the String does not represent a valid
   *     long in the Arabic or Roman number systems
   */
  public static long parseLong(String s) 
    throws NumberFormatException {
    try {
      return Long.parseLong(s);
    } catch (NumberFormatException ex) {
      return NumberUtil.parseRomanNumber(s);
    }
  }
  
  /**
   * Returns a float from a String representing a number in the 
   * Arabic or Roman number system.
   * 
   * @param s the String
   * @return the number
   * @throws NumberFormatException if the String does not represent a valid
   *     float in the Arabic or Roman number systems
   */
  public static float parseFloat(String s) 
    throws NumberFormatException {
    try {
      return Float.parseFloat(s);
    } catch (NumberFormatException ex) {
      return NumberUtil.parseRomanNumber(s);
    }
  }
  
  /**
   * Returns a double from a String representing a number in the 
   * Arabic or Roman number system.
   * 
   * @param s the String
   * @return the number
   * @throws NumberFormatException if the String does not represent a valid
   *     double in the Arabic or Roman number systems
   */
  public static double parseDouble(String s) 
    throws NumberFormatException {
    try {
      return Double.parseDouble(s);
    } catch (NumberFormatException ex) {
      return NumberUtil.parseRomanNumber(s);
    }
  }
  
  /**
   * Returns an integer from a String array representing a number in
   * the Roman number system. The number must be in the range 0 - 2^31-1.
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
    // special-case "N" (nulla) as 0
    if ((tokens.length == 1) && "N".equals(tokens[0])) {
      return 0;
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
   * Roman number system. The number must be within the range 0 - 2^31-1.
   * The string may not contain any other characters than allowed by the 
   * Roman numeral alphabet: 'I', 'V', 'X', 'L', 'C', 'D' and 'M'. The
   * letter 'N' is also allowed. The Bede first used 'N' (nulla) for 0 
   * around 725AD in a table of lunar epacts. Parentheses can be used for 
   * large Roman numbers. If the string does not represent a Roman number, 
   * the exception is thrown. 
   * 
   * @param roman the String
   * @return the integer
   * @throws NumberFormatException if the String does not represent a valid
   *    number in the Roman number system
   */
  public static int parseRomanNumber(String roman) 
    throws NumberFormatException {
    return parseRomanNumber(roman, false);
  }
  /**
   * Returns an integer from a String representing a number in the
   * Roman number system. The number must be within the range 0 - 2^31-1.
   * The string may not contain any other characters than allowed by the 
   * Roman numeral alphabet: 'I', 'V', 'X', 'L', 'C', 'D' and 'M'. The
   * letter 'N' is also allowed. The Bede first used 'N' (nulla) for 0 
   * around 725AD in a table of lunar epacts. Parentheses can be used for 
   * large Roman numbers. If the string does not represent a Roman number, 
   * the exception is thrown. 
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
   * Roman number system. The number must be in the range 0 - 2^31-1.
   * The Bede first used 'N' (nulla) for 0 around 725AD in a table
   * of epacts. 
   * <p>
   * If a list is provided, it will contain Roman number tokens 
   * corresponding to the input string. The roman string "MCMDLLLIX" 
   * returns ["M" "CM" "D" "L" "L" "L" "IX"], while the string 
   * "(MCM)DLLLIX" returns ["(M)" "(CM)", "D" "L" "L" "IX"]. If the
   * input roman number is not normalized, the tokens will not be either.
   * 
   * @param s the input Roman string
   * @return roman array of numbers corresponding to the input number
   * @throws NumberFormatException if the input is not a valid Roman number
   */
  private static int parseRomanNumber(String roman, List<String> tokens) 
    throws NumberFormatException {
    String notRomanNumber = "Not a roman number: " + roman;
    if (StringUtil.isNullString(roman)) {
      throw new NumberFormatException(notRomanNumber);
    }
    roman = roman.trim().toUpperCase();
    // special-case "N" (nulla) for 0
    if ("N".equals(roman)) {
      if (tokens != null) {
        tokens.add("N");
      }
      return 0;
    }
    int romanLength = roman.length();
    
    // determine paren count and corresponding scale of number
    int parenCount = 0;
    int scale = 1;
    int i = 0;
    for (; i < romanLength && (roman.charAt(i) == '('); i++) {
      if (++parenCount > 2) {
        throw new NumberFormatException(notRomanNumber);
      }
      scale *= 1000;  // scale by thousands
    }

    int romanNumber = 0;
    int lastTokenVal = Integer.MAX_VALUE;
    List<String> list = (tokens != null) ? new ArrayList<String>() : null;

    for ( ; i < romanLength; i++) {
      if (roman.charAt(i) == ')') {
        if (--parenCount < 0) {
          throw new NumberFormatException(notRomanNumber);
        }
        scale /= 1000;
        lastTokenVal = Integer.MAX_VALUE;
      } else {
        String token = roman.substring(i,i+1);
        Integer tokenVal = romanToNum.get(token);
        if (tokenVal == null) {
          throw new NumberFormatException(notRomanNumber);
        }
        // add scaled value and validate number
        tokenVal *= scale;
        romanNumber += tokenVal;
        if (tokenVal > lastTokenVal) {
          // back down number if previous token modified current one (e.g. IV)
          romanNumber -= 2 * lastTokenVal;
          if (list != null) {
            // use previous and current character as token 
            token = roman.substring(i-1,i+1);
            list.set(list.size()-1, String.format(romanFmt[parenCount], token));
          }
        } else if (list != null) {
          // add roman token to return list 
          list.add(String.format(romanFmt[parenCount], token));
        }
        lastTokenVal = tokenVal;
        if (romanNumber <= 0) {
          throw new NumberFormatException(notRomanNumber);
        }
      }
    }
    
    if (parenCount != 0) {
      throw new NumberFormatException(notRomanNumber);
    }
    
    // return tokens and roman number
    if (list != null) {
      tokens.addAll(list);
    }

    return (int)romanNumber;
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
      parseRomanNumber(NumberUtil.toRomanNumber(n), romanChars);
    }
    return romanChars.toArray(new String[romanChars.size()]);
  }
  
  /**
   * Returns a String representation of an Arabic natural number
   * that is equivalent to an Arabic or Roman natural number string. 
   * 
   * @param s string representing Arabic or possibly unnormalized Roman number
   * @return the normalized form of the input Roman number
   * @throws NumberFormatException if the String does not represent a valid
   *  positive number in the Arabic Roman number system
   */
  public static String toArabicNumber(String s) 
    throws NumberFormatException {
    return Integer.toString(NumberUtil.parseInt(s));
  }
  
  /**
   * Returns a String representation of a normalized Roman number that is 
   * equivalent to an Arabic or Roman natural number string. 
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
    return NumberUtil.toRomanNumber(NumberUtil.parseInt(s));
  }
  
  /**
   * Returns the String representation of the value in the Roman number system.
   * The value must be in the range 0-2^31-1, or IllegalArgumentException is
   * thrown.
   *  
   * @param value the value
   * @return the String representation in the Roman number system
   */
  public static String toRomanNumber(int value) {
    if (value < 0) {
      throw new IllegalArgumentException("Value out of range: " + value);
    }
    if (value == 0) {
      return "N";
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
        + "\n\tjava NumberUtil -i <natural_number>"
        + "\n\tjava NumberUtil -i <start natural_number> <end natural_number>"
        + "\n\tjava NumberUtil -r <natural_number>"
        + "\n\tjava NumberUtil -r <start natural_number> <end natural_number>"
        + "\n");
    System.exit(-1);
  }

  /**
   * This method is used for testing purposes.
   * @param args the test arguments
   */
  public static void main(String[] args) {
    try {
      if (args.length >= 2) {
        int start = NumberUtil.parseInt(args[1]);
        int end = (args.length > 2) 
                    ? NumberUtil.parseInt(args[2]) : start;
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
