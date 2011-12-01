/*
 * $Id: NumberUtil.java,v 1.12 2011-12-01 17:39:32 easyonthemayo Exp $
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
   * Generate a series of numbers incrementing or decrementing by the delta,
   * from the start number to the end number inclusive. The delta should always
   * be positive; if a decreasing sequence is required, reverse the start and
   * end numbers. If the supplied delta is negative, it will be negated.
   * <p>
   * If the start and end numbers are equal, an array containing the single
   * value is returned A <code>NumberFormatException</code> is thrown if the
   * parameters are inconsistent, that is, the difference between start and end
   * is not divisible by the delta,
   *
   * @param start start number
   * @param end end number, which may be less than the start number
   * @param delta the magnitude of the increment or decrement, expressed as a positive integer
   * @return a sequence of numbers, incrementing or decrementing by delta
   * @throws IllegalArgumentException if the parameters are inconsistent
   */
  public static int[] constructSequence(int start, int end, int delta)
      throws NumberFormatException {
    // If the numbers are equal, the sequence will be the single number
    if (start==end) return new int[]{start};
    // Is the sequence descending
    boolean descending = start>end;
    // Ensure the delta is positive
    delta = Math.abs(delta);
    // Check the delta divides into the gap
    if (Math.abs(end-start) % delta > 0) {
      throw new IllegalArgumentException(
          "The difference between start and end must be divisible by delta.");
    }
    // Number of entries
    int n = 1 + Math.abs(end-start)/delta;
    int[] numbers = new int[n];
    for (int i=0; i<n; i++) {
      int d = i*delta;
      numbers[i] = descending ? start-d : start+d;
    }
    return numbers;
  }

  /**
   * Generate a series of numbers incrementing or decrementing by 1,
   * @param start the start number
   * @param end the end number, which may be less than the start number
   * @return a range of numbers, incrementing or decrementing by 1
   */
  public static int[] constructSequence(int start, int end) {
    // No exception should be thrown for a delta of 1!
    return constructSequence(start, end, 1);
  }

  /**
   * Get the range start. Whitespace is trimmed.
   * @param  range a start/stop range separated by a dash, optionally with whitesapce
   * @return the range start <code>null</code> if not specified
   */
  static public String getRangeStart(String range) {
    if (range == null) {
      return null;
    }
    int i = findRangeHyphen(range);
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
    int i = findRangeHyphen(range);
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
   * is, "Georgia", "Kansas", and "Massachusetts" are topics within the
   * volume "Ge-Ma.
   * <p>
   * The range string can also include whitespace, which is trimmed from the 
   * resulting year. Java's <code>String.trim()</code> method trims blanks and 
   * also control characters, but doesn't trim all the forms of blank 
   * specified in the Unicode character set. The value to search for is 
   * <i>not</i> trimmed.
   *
   * @param range a single value or a start/stop range separated by a dash, optionally with whitespace
   * @param value the value
   * @return <code>true</code> if this range includes the value
   */
  static public boolean rangeIncludes(String range, String value) {
    if ((range == null) || (value == null)) {
      return false;
    }
    int i = findRangeHyphen(range);
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
   * Find the position of the middle-most hyphen in a string that represents a
   * range. If there are no hyphens, or an even number of hyphens, it suggests
   * that the string does not describe a range, and -1 is returned.
   * @param range a String representing a range
   * @return the position of the middlemost hyphen, or -1
   */
  public static final int findRangeHyphen(String range) {
    // Count the hyphens
    int n = StringUtil.countOccurences(range, "-");
    // An even number of hyphens (including 0) suggests the string is not a range
    if (n%2==0) return -1;
    // Find the middle hyphen
    int hyphenPos = -1;
    for (int i=0; i<=n/2; i++) hyphenPos = range.indexOf('-', hyphenPos+1);
    return hyphenPos;
  }

  /**
   * Determines the value of the system property with the specified name.
   * The argument is treated as the name of a system property. The string
   * value of this property is interpreted as Arabic or Roman number 
   * and the corresponding value is returned.
   * 
   * @param nm property name
   * @return the Integer value of the property or <tt>null</tt> if the property
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
   * can be parsed directly as an integer. Note that a Roman numeral does not
   * count as an integer. As an alternative that also checks if the string can
   * be parsed as a Roman numeral, use {@link isNumber()}.
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
   * Determines whether the input string represents a number, that is,
   * can be parsed to an integer by treating it as an integer representation,
   * or as a normalised Roman numeral. This requires Roman numerals to be
   * normalised if they are to be recognised and parsed.
   * <p>
   * This is useful for example when the string to be parsed may well
   * represent a legitimate non-numerical string which happens to contain only
   * Roman numeral characters.
   *
   * @param s the input String
   * @return <tt>true</tt> if the input string represents an integer
   */
  public static boolean isNumber(String s) {
    // First try as an integer
    if (isInteger(s)) return true;
    // Secondly try as a Roman numeral, requiring normalisation
    try {
      NumberUtil.parseRomanNumber(s, true);
      return true;
    } catch (NumberFormatException ex) {
      return false;
    }
  }

  /**
   * Determines whether the input string contains any digits and can therefore
   * can be considered to include at least one number. Note that this is
   * different to both {@link isInteger} and {@link isNumber}. A return value
   * of <tt>true</tt> does not indicate that the string can be parsed as a
   * number (though that may be true), but that it contains numbers. The method
   * works by looking at each character in turn, in the anticipation that it
   * will be more efficient than pattern matching, at least for short strings.
   * @param s the input String
   * @return <tt>true</tt> if the input string contains any digits
   */
  public static boolean containsDigit(String s) {
    for (char c : s.toCharArray()) {
      if (Character.isDigit(c)) return true;
    }
    return false;
  }

  /**
   * Determines whether the input string is a mix of digits and non-digits.
   * This is established by checking that it cannot be parsed as an integer but
   * does contain digits. It does not check whether the string has tokens that
   * can be parsed as Roman numbers, so a string like "vol-XI" is not considered
   * to have mixed formats.
   * @param s the input string
   * @return <tt>true</tt> if the input string contains both digits and non-digits
   */
  public static boolean isMixedFormat(String s) {
    return !isInteger(s) && containsDigit(s);
  }

  /**
   * Determines whether a String appears to represent a numerical range. In
   * general, a range is two numeric strings separated by a hyphen '-' with
   * optional whitespace. The second value is expected to be numerically greater
   * than or equal to the first. A numeric string can be parsed as an integer by
   * treating it either as the string representation of an integer, or as a
   * Roman numeral. Otherwise the string is considered to represent a
   * single value. For example "I-4" is considered to be a range, while "S-1"
   * is a single volume identifier.
   * <p>
   * Note that false positives are possible for volume identifiers like "V-C"
   * where the letters are not Roman numerals - if such examples occur.
   *
   * @param s the input String
   * @return <tt>true</tt> if the input string represents a numerical range
   */
  public static boolean isNumericalRange(String s) {
    if (s==null) return false;
    // Even though a single value is considered a range from and to itself in
    // terms of getRangeStart and getRangeEnd, we don't want to consider that a
    // range string in this method. So no hyphen, no range.
    if (s.indexOf('-') < 0) return false;
    // Split and parse the range
    String s1 = getRangeStart(s);
    String s2 = getRangeEnd(s);
    // If either endpoint is not parseable as a number, this is not a range.
    // First check if the strings can be parsed as numbers, but do not accept
    // unnormalised Roman numbers.
    if (!isNumber(s1) || !isNumber(s2)) return false;
    try {
      int i1 = parseInt(s1);
      int i2 = parseInt(s2);
      return i2>=i1;
    } catch (NumberFormatException e) {
      // A string is not parseable as an integer or as a Roman numeral.
      return false;
    }
  }

  /**
   * Pad all the numbers in the string with zeros so that a lexicographical
   * comparison treats numbers by magnitude.
   * <p>
   * This method has been copied verbatim from
   * {@link org.lockss.util.CachingComparator()}.
   *
   * @param s the input String
   * @param padLen the length of padding to impose
   * @return the string with numerical tokens zero-padded
   */
  public static String padNumbers(String s, int padLen) {
    int len = s.length();
    StringBuilder sb = new StringBuilder(len + padLen - 1);
    int ix = 0;
    while (ix < len) {
      char ch = s.charAt(ix);
      if (Character.isDigit(ch)) {
        int jx = ix;
        while (++jx < len) {
          if (!Character.isDigit(s.charAt(jx))) {
            break;
          }
        }
        // jx now points one beyond end of number (or end of string)
        for (int padix = padLen - (jx - ix); padix > 0; padix--) {
          sb.append('0');
        }
        do {
          sb.append(s.charAt(ix++));
        } while (ix < jx);
      } else {
        sb.append(ch);
        ix++;
      }
    }
    return sb.toString();
  }

  /**
   * Determines whether the two integers are are strictly consecutive, that is,
   * whether the second integer comes directly after the first.
   * @param first the first integer
   * @param second the second integer
   * @return <tt>true</tt> if the second integer is one greater than the first
   */
  public static boolean areConsecutive(int first, int second) {
    return second - first == 1;
  }

  /**
   * Determines whether two integers supplied as strings are strictly 
   * consecutive, that is, whether the second integer comes directly after the 
   * first. If the strings cannot be parsed as integers, an exception is thrown.
   * Note that this method will also accept strings representing Roman numerals.
   * 
   * @param first the first string representing a number
   * @param second the second string representing a number
   * @return <tt>true</tt> if the strings could be parsed and the second integer is one greater than the first
   * @throws NumberFormatException if the Strings do not represent valid numbers
   */
  public static boolean areConsecutive(String first, String second)
      throws NumberFormatException {
    return areConsecutive(parseInt(first), parseInt(second));
  }

  /**
   * Determines whether two numbers supplied as strings are equal in value. If
   * the strings cannot both be parsed as integers, <tt>false</tt> is returned.
   * Note that this method will also accept strings representing Roman numerals.
   *
   * @param first the first string representing a number
   * @param second the second string representing a number
   * @return <tt>true</tt> if the strings could be parsed and are equal in value
   */
  public static boolean areEqualValue(String first, String second) {
    try {
      return parseInt(first) == parseInt(second);
    } catch (NumberFormatException e) {
      return false;
    }
  }

  /**
   * Determines whether two ranges supplied as strings are equal, that is, their
   * endpoints are equal in value. If any endpoints cannot be parsed as
   * integers, <tt>false</tt> is returned. This method will accept strings
   * representing ranges involving Roman numerals.
   *
   * @param first the first string representing a number
   * @param second the second string representing a number
   * @return <tt>true</tt> if the strings could be parsed and are equal in value
   */
  public static boolean areRangesEqual(String first, String second) {
    return areEqualValue(getRangeStart(first), getRangeStart(second)) &&
        areEqualValue(getRangeEnd(first), getRangeEnd(second));
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
