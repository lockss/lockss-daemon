/*
 * $Id: Roman.java,v 1.1 2011-06-03 16:50:00 pgust Exp $
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

import java.util.Arrays;
import java.util.List;


/**
 * The Roman class is specialization of a Number that accepts and outputs
 * positive whole numbers in both the Arabic and the Roman number system.
 * <p>
 * Roman numerals larger than M are represented by lower-valued Roman numerals 
 * within parentheses, indicating multiplication by 1000. Parentheses can be 
 * nested, so 1,000,000 can be represented as either (M) or ((I)). There is no
 * representation of 0 or negative values in the Roman number system. Side bar
 * notation is not currently supported. For details on large Roman numbers, see
 * http://www.web40571.clarahost.co.uk/roman/howtheywork.htm#larger
 * <p>
 * This class supports positive integers from 1 to 2^31-1, or from I to
 * ((MMCXLVII)CDLXXXIII)DCXLVIIMMMCMXCIX. 
 * <p>
 * Any illegal input will throw an exception. It detects bad values like IIX. 
 * All numerals are output in upper case.
 */
public final class Roman extends Number implements Comparable<Roman> {
  /** Serial version ID */
  private static final long serialVersionUID = 7060081368376428595L;

  /** The value of the number */
  private final int value;
  
  /** A constant holding the minimum value a Roman number can have */
  public static final int MIN_VALUE = 1;
  
  /** A constant holding the maximum value a Roman number can have */
  public static final int MAX_VALUE = Integer.MAX_VALUE;
  
  /**
   * Constant for the number of bits needed to represent the integer value
   * of a Roman number. Note that a Roman number cannot be 0 or negative.
   *
   * @since 1.5
   */
  public static final int SIZE = Integer.SIZE;
  
  /** List of Arabic values used for conversion */
  private final static short[] arabicVals = new short[] {
	1,   4,    5,    9,   10,  40,   50,  90,   100, 400,  500, 900,  1000 };
  

  /** List of equivalent Roman values used for conversion */
  private final static List<String> romanVals = Arrays.asList(
	"I", "IV", "V", "IX", "X", "XL", "L", "XC", "C", "CD", "D", "CM", "M" );
  
  /**
   * Constructs a new {@code Roman} from a String representing a number between 
   * 1-2^31-1 (MIN_VALUE to MAX_VALUE) in the Arabic or Roman number system. 
   * 
   * @param s the String
   * @throws NumberFormatException if the String does not represent a valid
   * 	positive whole number in the Arabic or Roman number systems
   */
  public Roman(String s) throws NumberFormatException {
	value = parseRoman(s);
  }
  
  /**
   * Constructs a new {@code Roman} from an integer value. Value must be in 
   * the range 1-2^31-1 (MIN_VALUE to MAX_VALUE), or IllegalArgumentException 
   * is thrown.
   * 
   * @param value the value
   */
  public Roman(int value) {
    this.value = checkValue(value);
  }
  
  @Override
  public byte byteValue() {
	return (byte)value;
  }
  
  @Override
  public int compareTo(Roman object) {
    return value > object.value ? 1 : (value < object.value ? -1 : 0); 
  }
  
  @Override
  public double doubleValue() {
	return value;
  }
  
  @Override
  public boolean equals(Object obj) {
	return (obj instanceof Roman) && ((((Roman)obj).value == value));
  }
  
  @Override
  public float floatValue() {
	return value;
  }
  
  @Override
  public int hashCode() {
	return value;
  }
  
  @Override
  public int intValue() {
	return value;
  }
  
  @Override
  public long longValue() {
	return value;
  }
  
  @Override
  public short shortValue() {
	return (short)value;
  }
  
  @Override
  public String toString() {
	return Roman.toString(value);
  }
  
  /**
   * Check that the value of an integer is in the Roman numeral range
   * of 1-2^31-1 (MIN_VALUE  to MAX_VALUE).
   * 
   * @param n the integer
   * @throws IllegalArgumentException if the number is out of range
   */
  private static int checkValue(int n) throws IllegalArgumentException {
    if (n > Roman.MAX_VALUE  || n < Roman.MIN_VALUE) {
      throw new IllegalArgumentException(
    	    "Value must be in range " 
    	  + Roman.MIN_VALUE + "-" + Roman.MAX_VALUE + ": " + n);
    }
    return n;
  }

  /**
   * Returns a Roman object from a String Arabic or Roman number representation.
   * The number must be within the Roman numeral range 1-2^31-1 (MAX_VALUE to
   * MIN_VALUE).
   * 
   * @param s the String
   * @return the Roman object
   * @throws NumberFormatException if the String does not represent a valid
   * 	positive whole number in the Arabic or Roman number systems
   */
  public static Roman decode(String s) throws NumberFormatException {
	return Roman.valueOf(s);
  }
  
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
  public static Roman getRoman(String nm) {
	return Roman.getRoman(nm, null);
  }
  
  /**
   * Determines the Roman value of the system property with the specified name.
   * The first argument is treated as the name of a system property. The string
   * value of this property is interpreted as Roman or Arabic value and the
   * corresponding Roman object is returned.
   * <p>
   * The second argument is the default value. This value is returned if the
   * system property does not have the correct format, or if the specified name
   * is empty or <tt>null</tt>
   * 
   * @param nm property name
   * @param val the default value
   * @return the Roman value of the property
   */
  public static Roman getRoman(String nm, Roman val) {
	String prop = System.getProperty(nm);
	if (prop != null) {
	  try {
		val = Roman.decode(prop);
	  } catch (NumberFormatException ex) {
		// fall through
	  }
	}
	return val;
  }
  
  /**
   * Returns an integer from a String representing a number in the Arabic or
   * Roman number system. The number must be within the range 1-2^31-1.
   * 
   * @param s the String
   * @return the number
   * @throws NumberFormatException if the String does not represent a valid
   * 	positive whole number in the Arabic or Roman number systems
   */
  public static int parseRoman(String s) throws NumberFormatException {
	try {
	  return Roman.parseRoman(s, false);
	} catch (NumberFormatException ex) {
	  try {
	  	return checkValue(Integer.parseInt(s));
	  } catch (IllegalArgumentException ex2) {
	      throw new NumberFormatException(ex2.getMessage());
	  }
	}
  }
  
  /**
   * Returns an integer from a String representing a number in the
   * Roman number system. The number must be within the range 1-2^31-1.
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
   * 	to be in normalized form
   * @return the integer
   * @throws NumberFormatException if the String does not represent a valid
   *	number in the Roman number system
   */
  public static int parseRoman(String roman, boolean validate) 
  	throws NumberFormatException {
	if ((roman == null) || (roman.length() == 0)) {
	  throw new NumberFormatException("Not a roman number");
	}
	
	int romanValue = 0;
	roman = roman.trim().toUpperCase();
	short lastRomanDigitValue = Short.MAX_VALUE;
	int romanLength = roman.length();
	
	for (int i = 0; i < romanLength; i++) {
	  if (roman.charAt(i) == '(') {
		int j = roman.lastIndexOf(')');
		if (j > 0) {
		  try {
			romanValue = parseRoman(roman.substring(i+1,j)) * 1000;
			lastRomanDigitValue = Short.MAX_VALUE;
			i = j; 
		  } catch (NumberFormatException ex) {
			throw new NumberFormatException("Not a roman number: " + roman);
		  }
		} else {
		  throw new NumberFormatException("Not a roman number: " + roman);
		}
	  } else {
    	int idx = romanVals.indexOf(roman.substring(i,i+1));
    	if (idx < 0) {
    	  throw new NumberFormatException("Not a roman number: " + roman);
    	}
    	short romanDigitValue = arabicVals[idx];
    	romanValue += romanDigitValue;
    	if (romanDigitValue > lastRomanDigitValue) {
    	  romanValue -= 2 * lastRomanDigitValue;
    	}
    	lastRomanDigitValue = romanDigitValue;
	  }
	  
	  if (romanValue <= 0) {
		throw new NumberFormatException("Roman number too large: " + roman);
	  }
	}
		
	// the algorithm above not only creates a number that is correct for any 
	// well formed Roman string but also some non well formed Roman string 
	// (e.g. IIX), so the trick is to compare the normal string for 8 (VIII) 
	// with the one given (IIX)
	if (validate && !toString(romanValue).equals(roman)) {
	  throw new NumberFormatException("Incorrect order: " + roman);
	}
		
	return romanValue;
  }
	
  /**
   * Returns a String representation of an Arabic number that is 
   * equivalent to an Arabic or possibly unnormalized Roman number string. 
   * 
   * @param s string representing Arabic or possibly unnormalized Roman number
   * @return the normalized form of the input Roman number
   * @throws NumberFormatException if the String does not represent a valid
   *	positive number in the Arabic Roman number system
   */
  public static String toArabicString(String s) 
	throws NumberFormatException {
	return Integer.toString(Roman.parseRoman(s));
  }
  
  /**
   * Returns a String representation of a normalized Roman number that is 
   * equivalent to an Arabic or possibly unnormalized Roman number string. 
   * <p>
   * There are many examples of non-normal Roman number representations on 
   * monuments and books. See the Wikipedia article on "Roman_numerals" 
   * for further discussion and examples.
   * 
   * @param s string representing Arabic or possibly unnormalized Roman number
   * @return the normalized form of the input Roman number
   * @throws NumberFormatException if the String does not represent a valid
   *	positive number in the Arabic Roman number system
   */
  public static String toRomanString(String s) 
	throws NumberFormatException {
	return Roman.toString(Roman.parseRoman(s));
  }
  
  /**
   * Returns the String representation of the value in the Roman number system.
   * The value must be in the range 1-2^31-1, or IllegalArgumentException is
   * thrown.
   *  
   * @param value the value
   * @return the String representation in the Roman number system
   */
  public static String toString(int value) {
	checkValue(value);
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
    for (int i = arabicVals.length-1; (i >= 0) && (value > 0); --i) {
      // remove as many of this value as possible (maybe none).
      while (value >= arabicVals[i]) {
    	value -= arabicVals[i];           // Subtract value
        result.append(romanVals.get(i));  // Add roman equivalent
      }
    }
    return result.toString();
  }
  
  /**
   * Returns the {@code Roman} object from a String representing a positive
   * whole number in the Arabic or Roman number system.
   * 
   * @param s the String
   * @return the Roman object
   * @throws NumberFormatException if the String does not represent a valid
   * 	positive whole number in the Arabic or Roman number systems
   */
  public static Roman valueOf(String s) throws NumberFormatException {
	return new Roman(s);
  }
  
  /**
   * Returns the {@code Roman} object holding the value specified by the 
   * integer value. The value must be in the range 1-2^31-1 (MIN_VALUE to 
   * MAX_VALUE), or IllegalArgumentException is thrown.
   *
   * @param value the integer
   * @return the {@code Roman} object
   */
  public static Roman valueOf(int value) {
	return new Roman(value);
  }

  /**
   * Show usage message and exit.
   */
  private static void showUsageAndExit() {
	System.out.print(
		"Usage:"
		+ "\n\tjava Roman -i <WholeNumber>"
		+ "\n\tjava Roman -i <start WholeNumber> <end WholeNumber>"
		+ "\n\tjava Roman -r <WholeNumber>"
		+ "\n\tjava Roman -r <start WholeNumber> <end WholeNumber>\n");
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
		int start = Roman.parseRoman(args[1]);
		int end =  (args.length > 2) ? Roman.parseRoman(args[2]) : start;
		for (int i = start; i <= end; i++) {
		  if (args[0].equals("-i")) {
			  System.out.println(i);
		  } else if (args[0].equals("-r")) {
			  System.out.println(Roman.toString(i));
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
