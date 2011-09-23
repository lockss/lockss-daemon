/*
 * $Id: TestNumberUtil.java,v 1.4.4.3 2011-09-23 13:23:33 easyonthemayo Exp $
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

import org.lockss.test.*;

/**
 * This is the test class for org.lockss.util.NumberUtil
 */
public class TestNumberUtil extends LockssTestCase {
  @SuppressWarnings("rawtypes")
  public static Class[] testedClasses = {
    org.lockss.util.NumberUtil.class
  };

  public TestNumberUtil(String msg) {
    super(msg);
  }

  /**
   * Test whether strings parse as integers.
   */
  public final void testIsInteger() {
    assertTrue(NumberUtil.isInteger("100"));
    assertFalse(NumberUtil.isInteger("99.9"));
    assertFalse(NumberUtil.isInteger("not an integer"));
    assertFalse(NumberUtil.isInteger("100 non numbers"));
    assertFalse(NumberUtil.isInteger(""));
    assertFalse(NumberUtil.isInteger(null));
  }

  /**
   * Test whether strings parse as numbers.
   */
  public final void testIsNumber() {
    assertTrue(NumberUtil.isNumber("100"));
    assertTrue(NumberUtil.isNumber("C"));
    assertTrue(NumberUtil.isNumber("XVII"));
    assertTrue(NumberUtil.isNumber("N"));

    assertFalse(NumberUtil.isNumber("99.9"));
    assertFalse(NumberUtil.isNumber("not an integer"));
    assertFalse(NumberUtil.isNumber("100 non numbers"));
    assertFalse(NumberUtil.isNumber(""));
    assertFalse(NumberUtil.isNumber(null));

    // Strings not intended to represent Roman numerals will not be considered
    // numbers by default - normalisation must be explicitly enabled to
    // parse the following as Roman numerals:
    assertFalse(NumberUtil.isNumber("Viv"));
    assertFalse(NumberUtil.isNumber("Vic"));
    assertFalse(NumberUtil.isNumber("Civic"));
    assertFalse(NumberUtil.isNumber("Livid"));
    assertFalse(NumberUtil.isNumber("Mivvi"));
    assertFalse(NumberUtil.isNumber("Mill"));
  }

  /**
   * Test whether strings parse as Roman numbers.
   */
  public final void testIsRomanNumber() {
    assertTrue(NumberUtil.isRomanNumber("C"));
    assertTrue(NumberUtil.isRomanNumber("c"));
    assertTrue(NumberUtil.isRomanNumber("XVII"));
    assertTrue(NumberUtil.isRomanNumber("N"));

    // Unnormalized Roman strings
    assertTrue(NumberUtil.isRomanNumber("XVIIIII"));
    assertFalse(NumberUtil.isNumber("XVIIIII"));
    assertTrue(NumberUtil.isRomanNumber("clivic"));
    assertFalse(NumberUtil.isNumber("clivic"));

    // Strings not intended to represent Roman numerals can be taken so
    // when normalisation is not enforced:
    assertTrue(NumberUtil.isRomanNumber("Viv"));
    assertTrue(NumberUtil.isRomanNumber("Vic"));
    assertTrue(NumberUtil.isRomanNumber("Vid"));
    assertTrue(NumberUtil.isRomanNumber("Civic"));
    assertTrue(NumberUtil.isRomanNumber("Livid"));
    assertTrue(NumberUtil.isRomanNumber("Mivvi"));
    assertTrue(NumberUtil.isRomanNumber("Mill"));
    // TODO Is this really what we want by default?

    // Non-Roman strings
    assertFalse(NumberUtil.isRomanNumber("clinic"));
    assertFalse(NumberUtil.isNumber("clinic"));
  }

  public final void testIsNumericalRange() {
    // Integer ranges
    assertTrue(NumberUtil.isNumericalRange("1-1"));
    assertTrue(NumberUtil.isNumericalRange("1-2"));
    assertTrue(NumberUtil.isNumericalRange("1-9"));
    assertTrue(NumberUtil.isNumericalRange("1-10"));
    // Roman numeral ranges
    assertTrue(NumberUtil.isNumericalRange("C-CLXVII"));
    assertTrue(NumberUtil.isNumericalRange("I-VI"));
    assertTrue(NumberUtil.isNumericalRange("V-C")); // compared numerically not lexically
    assertTrue(NumberUtil.isNumericalRange("C-C"));
    // Mixed ranges
    assertTrue(NumberUtil.isNumericalRange("I-4"));
    assertTrue(NumberUtil.isNumericalRange("1-V"));

    // Single volumes are not ranges
    assertFalse(NumberUtil.isNumericalRange("1"));
    assertFalse(NumberUtil.isNumericalRange("V"));
    // Hyphenated strings with unparseable volumes are not ranges
    assertFalse(NumberUtil.isNumericalRange("ROMAN-NUMERALS"));
    assertFalse(NumberUtil.isNumericalRange("LXV-LXVIIP"));
    assertFalse(NumberUtil.isNumericalRange("one - 2"));
    assertFalse(NumberUtil.isNumericalRange("null-null"));
    // Descending values are not ranges
    assertFalse(NumberUtil.isNumericalRange("11-9"));
    assertFalse(NumberUtil.isNumericalRange("10-01"));
    assertFalse(NumberUtil.isNumericalRange("C-V")); // compared numerically not lexically
    // Not a range
    assertFalse(NumberUtil.isNumericalRange("1-2-3"));
    // Valid string ranges are not numerical ranges
    assertFalse(NumberUtil.isNumericalRange("s1-s2"));
  }

  /**
   * Test consecutive numbers represented as integers.
   */
  public final void testAreConsecutiveIntInt() {
    assertTrue(  NumberUtil.areConsecutive(0,  1) );
    assertTrue(  NumberUtil.areConsecutive(1,  2) );
    assertTrue(  NumberUtil.areConsecutive(-1, 0) );
    
    assertFalse( NumberUtil.areConsecutive(0,  0) );
    assertFalse( NumberUtil.areConsecutive(1,  1) );
    assertFalse( NumberUtil.areConsecutive(0,  2) );
    assertFalse( NumberUtil.areConsecutive(1,  3) );
    assertFalse( NumberUtil.areConsecutive(0, -1) );
    assertFalse( NumberUtil.areConsecutive(2,  1) );
    assertFalse( NumberUtil.areConsecutive(1, -2) );
  }
  
  /**
   * Test consecutive numbers represented as strings.
   */
  public final void testAreConsecutiveStringString() {
    // Consecutive numbers, Roman and Arabic
    assertTrue(  NumberUtil.areConsecutive("0",  "1")   );
    assertTrue(  NumberUtil.areConsecutive("1",  "2")   );
    assertTrue(  NumberUtil.areConsecutive("-1", "0")   );
    assertTrue(  NumberUtil.areConsecutive("VI", "VII") );
    assertTrue(  NumberUtil.areConsecutive("L", "LI")   );
    // Strings are also trimmed before parsing
    assertTrue(  NumberUtil.areConsecutive(" 1 ", " 2 ")   );
    assertTrue(  NumberUtil.areConsecutive(" L ", " LI ")   );
    
    // Strings which cannot be parsed as ints
    try {
      assertFalse( NumberUtil.areConsecutive("number 1",  "number 2")  );
      fail("Should throw a NumberFormatException.");
    } catch (NumberFormatException e) { /* do nothing */ }
    try {
      assertFalse( NumberUtil.areConsecutive("one",  "two")  );
      fail("Should throw a NumberFormatException.");
    } catch (NumberFormatException e) { /* do nothing */ }

    // Parseable numbers, not consecutive
    assertFalse( NumberUtil.areConsecutive("L", "LII")  );
    assertFalse( NumberUtil.areConsecutive("0", "0")  );
    assertFalse( NumberUtil.areConsecutive("1", "1")  );
    assertFalse( NumberUtil.areConsecutive("0", "2")  );
    assertFalse( NumberUtil.areConsecutive("1", "3")  );
    assertFalse( NumberUtil.areConsecutive("0", "-1") );
    assertFalse( NumberUtil.areConsecutive("2", "1")  );
    assertFalse( NumberUtil.areConsecutive("1", "-2") );
  }

  public final void testAreEqualValue() {
    assertTrue( NumberUtil.areEqualValue("1", "1") );
    assertTrue( NumberUtil.areEqualValue(" 1 ", " 01 ") );
    assertTrue( NumberUtil.areEqualValue(" 1 ", "I") );
    assertTrue( NumberUtil.areEqualValue("11", " XI ") );
    assertTrue( NumberUtil.areEqualValue("N", "0000") );
    // Strings not both parseable as integers
    assertFalse( NumberUtil.areEqualValue("NO", "YES") );
    assertFalse( NumberUtil.areEqualValue("NO", "NO") );
    assertFalse( NumberUtil.areEqualValue("", "1") );
    assertFalse( NumberUtil.areEqualValue(null, "1") );
    assertFalse( NumberUtil.areEqualValue("one", "1") );
  }

  public final void testAreRangesEqual() {
    assertTrue( NumberUtil.areRangesEqual("1-5", "1 - 5") );
    assertTrue( NumberUtil.areRangesEqual("1-5", "I-V") );
    assertTrue( NumberUtil.areRangesEqual("1-V", "I-5") );
    assertTrue( NumberUtil.areRangesEqual("11", " XI ") );
    assertTrue( NumberUtil.areRangesEqual("N-CXVI", "0000 - 116 ") );
  }

  /**
   * Test normalized Roman and Arabic numbers.
   */
  public void testNormalized() {
	String[] validNormalizedRomanStrings = {
	  "N", "I", "VI", "XIII", "LXXX", "XC", 
	  "DLXI", "MCMLXXIV",
	  "(MMMDCCCL)MMCDXXIX", 
	  "MMM", "(IV)", "(V)M",
	  "(MMMV)CMXCIX", "(MMMDCCCL)MMCDXXIX",
	  "((MMCXLV)MMCDLXXX)MMMDCXLVII"
	};
    String[] equivalentArabicStrings = {
        "0", "1", "6", "13", "80", "90", 
        "561", "1974",
        "3852429", 
        "3000", "4000", "6000",
        "3005999", "3852429",
        Integer.toString(Integer.MAX_VALUE) 
    };

    // string arrays of roman digits split from normalized roman strings
    String[][] equivalentRomanDigits = {
      {"N"}, {"I"}, {"V","I"}, {"X","I","I","I"}, {"L","X","X","X"}, {"XC"}, 
      {"D","L","X","I"}, {"M","CM","L","X","X","IV"},
      {"(M)","(M)","(M)","(D)","(C)","(C)","(C)","(L)",
       "M","M","CD","X","X","IX"}, 
      {"M","M", "M"}, {"(IV)"}, {"(V)","M"},
      {"(M)","(M)","(M)","(V)","CM","XC","IX"},
      {"(M)","(M)","(M)","(D)","(C)","(C)","(C)","(L)",
       "M","M","CD","X","X","IX"},
      {"((M))","((M))","((C))","((XL))","((V))",
       "(M)","(M)","(CD)","(L)","(X)","(X)","(X)",
       "M","M","M","D","C","XL","V","I","I"}
    };
	  
	for (int i = 0; i < validNormalizedRomanStrings.length; i++) {
      String romanValue = validNormalizedRomanStrings[i];
      String arabicValue = equivalentArabicStrings[i];
      
	  try {
      int intValue = Integer.parseInt(arabicValue);
      // roman value represents a valid Roman number
      assertTrue(NumberUtil.isRomanNumber(romanValue));
      // roman value is normalized
      assertEquals(romanValue, NumberUtil.toRomanNumber(romanValue));
      // roman is normalized
      NumberUtil.parseRomanNumber(romanValue, true);
      // matches roman value
      assertEquals(intValue, NumberUtil.parseInt(romanValue));
      // matches arabic value
      assertEquals(intValue, NumberUtil.parseInt(arabicValue));
      // match split roman string
      assertEquals(equivalentRomanDigits[i],
          NumberUtil.toRomanDigits(romanValue));
      // match split roman string
      assertEquals(equivalentRomanDigits[i],
          NumberUtil.toRomanDigits(arabicValue));
      assertEquals(intValue,
          NumberUtil.parseRomanDigits(equivalentRomanDigits[i]));
    } catch (NumberFormatException ex) {
      fail(ex.getMessage(),ex);
    } catch (IllegalArgumentException ex) {
      fail(ex.getMessage());
    }
	}
  }	
  
  /**
   * Test unnormalized Roman numbers.
   */
  public void testUnnormalized() {
    String[] validUnnormalizedRomanStrings = {
        "VIIII", "LXXXXI", "IM", "DDDDIV", "((I))", "(MMMDCCCLII)CDXXIX",
        "((MMCXLVII)CDLXXXIII)DCXLVII"
    };
    String[] equivalentArabicStrings = {
        "9", "91", "999", "2004", "1000000", "3852429",
        Integer.toString(Integer.MAX_VALUE)
    };
		  
    for (int i = 0; i < validUnnormalizedRomanStrings.length; i++) {
      String romanValue = validUnnormalizedRomanStrings[i];
      String arabicValue = equivalentArabicStrings[i];
	  
      try {
        // roman value represents a valid Roman number
        assertTrue(NumberUtil.isRomanNumber(romanValue));
        // unnormalized roman value matches roman string representation
        assertNotEquals(romanValue, NumberUtil.toRomanNumber(romanValue));
        // roman matches arabic
        assertEquals(NumberUtil.toRomanNumber(romanValue),
            NumberUtil.toRomanNumber(arabicValue));
        // roman matches integer
        assertEquals(NumberUtil.parseInt(romanValue),
            NumberUtil.parseInt(arabicValue));
      } catch (NumberFormatException ex) {
        fail(ex.getMessage());
      } catch (IllegalArgumentException ex) {
        fail(ex.getMessage());
      }

      try {
        // roman is not normalized
        NumberUtil.parseRomanNumber(romanValue, true);
        fail(romanValue + " is unnormalized");
      } catch (NumberFormatException ex) {
      } catch (IllegalArgumentException ex) {
        fail(ex.getMessage());
      }
    }
  }

  /**
   * Test invalid Roman and Arabic numbers.
   */
  public void testInvalid() {
    String[] invalidRomanStrings = {
        "xyzzy", "1234IV", "", "-iv", "100",
        "((MMCXLVII)CDLXXXIII)DCXLVIII"  // too large
    };
		  
    for (String s : invalidRomanStrings) {
      try {
        // String is not a Roman number
        assertFalse(NumberUtil.isRomanNumber(s));
        // String is not a Roman number
        NumberUtil.parseRomanNumber(s);
        fail(s + " is an invaid Roman string");
      } catch (NumberFormatException ex) {
      } catch (IllegalArgumentException ex) {
      }
    }
	
    String[] invalidArabicStrings = {
        "-1", Long.toString(Integer.MAX_VALUE+1)
    };
			  
    for (String s : invalidArabicStrings) {
      try {
        // String is not a Roman number
        assertFalse(NumberUtil.isRomanNumber(s));
        int intValue = Integer.parseInt(s);
        NumberUtil.toRomanNumber(intValue);
        fail(intValue + " is an invalid integer value");
      } catch (NumberFormatException ex) {
      } catch (IllegalArgumentException ex) {
      }
    }
  }
  
  /**
   * Test numeric and text ranges
   */
  public void testRange() {
    assertEquals("123", NumberUtil.getRangeStart("123-456"));
    assertEquals("123", NumberUtil.getRangeStart("123"));
    assertEquals("456", NumberUtil.getRangeEnd("123-456"));
    assertEquals("456", NumberUtil.getRangeEnd("456"));
    assertEquals("a-12", NumberUtil.getRangeStart(" a-12 - a-15 "));
    assertEquals("a-15", NumberUtil.getRangeEnd(" a-12 - a-15 "));
    assertEquals("merry-go-round", NumberUtil.getRangeStart("merry-go-round"));
    assertEquals("merry-go-round", NumberUtil.getRangeEnd("merry-go-round"));

    /// test numeric ranges
    assertTrue(NumberUtil.rangeIncludes("123-456", "123"));
    assertTrue(NumberUtil.rangeIncludes("123-456", "CXXIV"));
    assertFalse(NumberUtil.rangeIncludes("123-456", "CXX"));
    
    // test text ranges
    assertTrue(NumberUtil.rangeIncludes("abc-def", "abc"));
    assertTrue(NumberUtil.rangeIncludes("abc-def", "def"));
    assertTrue(NumberUtil.rangeIncludes("abc-def", "bcdefg"));
    assertTrue(NumberUtil.rangeIncludes("abc", "abc"));
    
    // Test with whitespace
    assertEquals("123", NumberUtil.getRangeStart(" 123 -456"));
    assertEquals("123", NumberUtil.getRangeStart(" 123"));
    assertEquals("456", NumberUtil.getRangeEnd("  123-  456"));
    assertEquals("456", NumberUtil.getRangeEnd("456   "));
    assertTrue(NumberUtil.rangeIncludes("123 - 456", "CXXIV"));
    // Note that the search value is not trimmed, so cannot have initial whitespace
    assertTrue(NumberUtil.rangeIncludes("abc-def", "def  "));
    assertTrue(NumberUtil.rangeIncludes(" abc  -  def ", "bcdefg  "));

    // Test ranges with multiple hyphens
    assertEquals(3, NumberUtil.findRangeHyphen("one-two"));
    assertEquals(5, NumberUtil.findRangeHyphen(" 123 - 789 "));
    assertEquals(5, NumberUtil.findRangeHyphen("0-2-4-6-8-10"));
    assertEquals(5, NumberUtil.findRangeHyphen("a-12 - a-15"));
    assertEquals(0, NumberUtil.findRangeHyphen("-"));
    assertEquals(2, NumberUtil.findRangeHyphen("-----"));
    // Test non-ranges with multiple hyphens
    assertEquals(-1, NumberUtil.findRangeHyphen("1-2-3"));
    assertEquals(-1, NumberUtil.findRangeHyphen("123"));
    assertEquals(-1, NumberUtil.findRangeHyphen("merry-go-round"));
    assertEquals(-1, NumberUtil.findRangeHyphen("----------"));
  }
}
