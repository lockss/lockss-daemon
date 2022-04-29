/*

Copyright (c) 2000-2022 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.Arrays;

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
   * Test constructSequence() with delta 1.
   */
  public final void testConstructSequenceIntInt() {
    // Increasing sequences
    assertTrue(Arrays.equals(new int[]{0}, NumberUtil.constructSequence(0,0)));
    assertTrue(Arrays.equals(new int[]{0,1,2}, NumberUtil.constructSequence(0,2)));
    assertTrue(Arrays.equals(new int[]{-2,-1,0}, NumberUtil.constructSequence(-2,0)));

    // Decreasing sequences
    assertTrue(Arrays.equals(new int[]{2,1,0}, NumberUtil.constructSequence(2,0)));
    assertTrue(Arrays.equals(new int[]{0,-1,-2}, NumberUtil.constructSequence(0,-2)));

    // No exception should occur with a delta of 1
    try {
      NumberUtil.constructSequence(0,0);
      NumberUtil.constructSequence(0,1);
      NumberUtil.constructSequence(0,2);
      NumberUtil.constructSequence(0,-1);
      NumberUtil.constructSequence(-1,0);
      NumberUtil.constructSequence(-10,10);
    } catch (IllegalArgumentException e) {
      fail("Should not throw IllegalArgumentException with unit delta!");
    }
  }

  /**
   * Test constructSequence() with specified delta.
   */
  public final void testConstructSequenceIntIntInt() {
    // Equal start and end with any delta should produce one number.
    for (int i=-5; i<5; i++) {
      int[] exp = new int[]{i};
      assertTrue(Arrays.equals(exp, NumberUtil.constructSequence(i,i,0)));
      assertTrue(Arrays.equals(exp, NumberUtil.constructSequence(i,i,5)));
      assertTrue(Arrays.equals(exp, NumberUtil.constructSequence(i,i,-5)));
      assertTrue(Arrays.equals(exp, NumberUtil.constructSequence(i,i,999)));
    }
    // Increasing sequences
    assertTrue(Arrays.equals(new int[]{0,2}, NumberUtil.constructSequence(0,2,2)));
    assertTrue(Arrays.equals(new int[]{0,2,4}, NumberUtil.constructSequence(0,4,2)));
    assertTrue(Arrays.equals(new int[]{-3,0,3}, NumberUtil.constructSequence(-3,3,3)));
    // Inverted delta should get corrected
    assertTrue(Arrays.equals(new int[]{-3,-1,1,3}, NumberUtil.constructSequence(-3,3,-2)));

    // Decreasing sequences
    assertTrue(Arrays.equals(new int[]{2,0}, NumberUtil.constructSequence(2,0,2)));
    assertTrue(Arrays.equals(new int[]{4,2,0}, NumberUtil.constructSequence(4,0,2)));
    assertTrue(Arrays.equals(new int[]{3,0,-3}, NumberUtil.constructSequence(3,-3,3)));
    // Inverted delta should get corrected
    assertTrue(Arrays.equals(new int[]{3,1,-1,-3}, NumberUtil.constructSequence(3,-3,-2)));

    // Invalid delta should produce exception
    try {
      NumberUtil.constructSequence(0,2,3);
      fail("Should have thrown IllegalArgumentException due to invalid delta.");
    } catch (IllegalArgumentException e) {
      // Expected
    }
  }

  public final void testConstructPaddedIntSequence() {
    assertIsomorphic(
        Arrays.asList(new String[]{"001", "002", "003"}),
        NumberUtil.constructPaddedIntSequence("001", "003", 1)
    );
    assertIsomorphic(
        Arrays.asList(new String[]{"001", "003"}),
        NumberUtil.constructPaddedIntSequence("001", "003", 2)
    );
    assertIsomorphic(
        Arrays.asList(new String[]{"1", "2", "3"}),
        NumberUtil.constructPaddedIntSequence("1", "3", 1)
    );
    assertIsomorphic(
        Arrays.asList(new String[]{"9", "10", "11"}),
        NumberUtil.constructPaddedIntSequence("9", "11", 1)
    );
    assertIsomorphic(
        Arrays.asList(new String[]{"09", "10", "11"}),
        NumberUtil.constructPaddedIntSequence("09", "11", 1)
    );

    // Should throw exception on inconsistent args
    try {
      NumberUtil.constructPaddedIntSequence("001", "003", 3);
      fail("Should throw exception");
    } catch (IllegalArgumentException e) {
      /*Expected*/
    }
    try {
      NumberUtil.constructPaddedIntSequence("001", "006", 2);
      fail("Should throw exception");
    } catch (IllegalArgumentException e) {
      /*Expected*/
    }
    // Zero-padded and not
    try {
      NumberUtil.constructPaddedIntSequence("1", "003", 1);
      fail("Should throw exception");
    } catch (IllegalArgumentException e) {
      /*Expected*/
    }
  }

  public final void testConstructRomanSequence() {
    assertEquals(Arrays.asList(new String[]{"I", "II", "III", "IV", "V"}),
        NumberUtil.constructRomanSequence("I","V",1));
    // Lower case
    assertEquals(Arrays.asList(new String[]{"i", "ii", "iii", "iv", "v"}),
        NumberUtil.constructRomanSequence("i","v",1));
    // Unnormalised 5 - 11 // TODO Should iiix be seen as 7?
    assertEquals(Arrays.asList(new String[]{"iv", "v", "vi", "vii", "viii", "ix", "x", "xi"}),
        NumberUtil.constructRomanSequence("iiii","iiix",1));
    // Unnormalised 5 - 7
    assertEquals(Arrays.asList(new String[]{"iv", "v", "vi", "vii"}),
        NumberUtil.constructRomanSequence("iiii","iiiiiii",1));
    // Decrementing sequence
    assertEquals(Arrays.asList(new String[]{"V", "IV", "III", "II", "I"}),
        NumberUtil.constructRomanSequence("V","I",1));
    // Incrementing by 3
    assertEquals(Arrays.asList(new String[]{"II", "V", "VIII", "XI"}),
        NumberUtil.constructRomanSequence("II","XI",3));
    // Mixed Roman and Arabic args; roman output
    /*assertEquals(Arrays.asList(new String[]{"I", "II", "III"}),
        NumberUtil.constructRomanSequence("1","III",1));*/
    // Try and construct a sequence with args that don't add up
    try {
      NumberUtil.constructRomanSequence("II","X",3);
      fail("Exception expected");
    } catch (Exception e) {
      // Expected exception
    }
    // Mixed Roman and Arabic are not allowed
    try {
      NumberUtil.constructRomanSequence("1","III",1);
      fail("Exception expected");
    } catch (Exception e) {
      // Expected exception
    }
  }

  public final void testConstructAlphabeticSequence() {

    assertIsomorphic(
        Arrays.asList(new String[]{"a", "b", "c"}),
        NumberUtil.constructAlphabeticSequence("a", "c", 1)
    );
    assertIsomorphic(
        Arrays.asList(new String[]{"aaa", "ana", "baa"}),
        NumberUtil.constructAlphabeticSequence("aaa", "baa", 26*13)
    );


    // Try strings of different lengths
    try {
      NumberUtil.constructAlphabeticSequence("a", "aa", 1);
      fail("Should throw exception for different length strings");
    } catch (IllegalArgumentException e) {
      /*Expected exception*/
    }

    // Should throw exception on inconsistent args - diff not divisible by delta
    try {
      NumberUtil.constructAlphabeticSequence("001", "003", 3);
      fail("Should throw exception for inconsistent args");
    } catch (IllegalArgumentException e) {
      /*Expected*/
    }
    try {
      NumberUtil.constructAlphabeticSequence("001", "006", 2);
      fail("Should throw exception for inconsistent args");
    } catch (IllegalArgumentException e) {
      /*Expected*/
    }
  }

  /**
   * Test whether strings are recognised as digits only up to a length parsable
   * as integer.
   */
  public final void testIsIntegerDigits() {
    assertTrue(NumberUtil.isIntegerDigits("100"));
    assertTrue(NumberUtil.isIntegerDigits("0"));
    assertTrue(NumberUtil.isIntegerDigits("1"));
    assertTrue(NumberUtil.isIntegerDigits("123456789"));
    // Part of the contract of this method is that strings yielding true
    // should parse as integers
    assertTrue(NumberUtil.isInteger("100"));
    assertTrue(NumberUtil.isInteger("0"));
    assertTrue(NumberUtil.isInteger("1"));
    assertTrue(NumberUtil.isInteger("123456789"));

    // Too long
    assertFalse(NumberUtil.isIntegerDigits("1234567890"));
    // Not digits
    assertFalse(NumberUtil.isIntegerDigits("99.9"));
    assertFalse(NumberUtil.isIntegerDigits("not an integer"));
    assertFalse(NumberUtil.isIntegerDigits("100 non numbers"));
    assertFalse(NumberUtil.isIntegerDigits(""));
    assertFalse(NumberUtil.isIntegerDigits(null));
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

    // signed numbers
    assertTrue(NumberUtil.isNumber("-100"));
    assertTrue(NumberUtil.isNumber("-00"));
    assertTrue(NumberUtil.isNumber("-123456789"));
    assertTrue(NumberUtil.isNumber("+100"));
    assertTrue(NumberUtil.isNumber("+00"));
    assertTrue(NumberUtil.isNumber("+123456789"));

    // signed Roman numbers not allowed
    assertFalse(NumberUtil.isNumber("+C"));
    assertFalse(NumberUtil.isNumber("-XVII"));

    // Not numbers
    assertFalse(NumberUtil.isNumber("99.9"));
    assertFalse(NumberUtil.isNumber("not an integer"));
    assertFalse(NumberUtil.isNumber("100 non numbers"));
    assertFalse(NumberUtil.isNumber(""));
    assertFalse(NumberUtil.isNumber(null));

    // Strings represent non-normal roman numbers:
    assertTrue(NumberUtil.isNumber("Viv"));
    assertTrue(NumberUtil.isNumber("Vic"));
    assertTrue(NumberUtil.isNumber("Civic"));
    assertTrue(NumberUtil.isNumber("Livid"));
    assertTrue(NumberUtil.isNumber("Mivvi"));
    assertTrue(NumberUtil.isNumber("Mill"));
  }

  /**
   * Test whether strings contain digits.
   */
  public final void testContainsDigit() {
    assertTrue(NumberUtil.containsDigit("1"));
    assertTrue(NumberUtil.containsDigit("a1"));
    assertTrue(NumberUtil.containsDigit("1a"));
    assertTrue(NumberUtil.containsDigit("a1a"));
    assertTrue(NumberUtil.containsDigit("1a1"));
    assertTrue(NumberUtil.isMixedFormat("A string with 1 digit!"));

    assertFalse(NumberUtil.containsDigit(""));
    assertFalse(NumberUtil.containsDigit(null));
    assertFalse(NumberUtil.containsDigit("XI"));
    assertFalse(NumberUtil.containsDigit("a string"));
  }

  /**
   * Test whether strings are mixed format.
   */
  public final void testIsMixedFormat() {
    assertTrue(NumberUtil.isMixedFormat("a1"));
    assertTrue(NumberUtil.isMixedFormat("1a"));
    assertTrue(NumberUtil.isMixedFormat("2a1"));
    assertTrue(NumberUtil.isMixedFormat("a1-1"));
    assertTrue(NumberUtil.isMixedFormat("a1a"));
    assertTrue(NumberUtil.isMixedFormat("A mixed format string with 1 digit!"));

    // Single format strings are not mixed format
    assertFalse(NumberUtil.isMixedFormat(""));
    assertFalse(NumberUtil.isMixedFormat("XI"));
    assertFalse(NumberUtil.isMixedFormat("a string"));
    assertFalse(NumberUtil.isMixedFormat("1"));
    assertFalse(NumberUtil.isMixedFormat("200"));
    // Roman numeral tokens are not parsed
    assertFalse(NumberUtil.isMixedFormat("vol-XI"));
  }

  /**
   * Test whether strings parse as Roman numbers.
   */
  public final void testIsRomanNumber() {
    assertTrue(NumberUtil.isRomanNumber("C"));
    assertTrue(NumberUtil.isRomanNumber("XVII"));
    assertTrue(NumberUtil.isRomanNumber("N"));

    // Try lower and mixed case
    assertTrue(NumberUtil.isRomanNumber("c"));
    assertTrue(NumberUtil.isRomanNumber("xvII"));
    assertTrue(NumberUtil.isRomanNumber("n"));
    
    // Unnormalized Roman strings
    assertTrue(NumberUtil.isRomanNumber("XVIIIII"));
    assertTrue(NumberUtil.isNumber("XVIIIII"));
    assertTrue(NumberUtil.isRomanNumber("clivic"));
    assertTrue(NumberUtil.isNumber("clivic"));

    // Strings not intended to represent Roman numerals can be taken so
    // when normalisation is not enforced:
    checkUnnormalisedRomanNumber("Viv");
    checkUnnormalisedRomanNumber("Vic");
    checkUnnormalisedRomanNumber("Vid");
    checkUnnormalisedRomanNumber("Civic");
    checkUnnormalisedRomanNumber("Livid");
    checkUnnormalisedRomanNumber("Mivvi");
    checkUnnormalisedRomanNumber("Mill");
    // TODO Is this really what we want by default?

    // Non-Roman strings
    assertFalse(NumberUtil.isRomanNumber("clinic"));
    assertFalse(NumberUtil.isNumber("clinic"));
  }


  /**
   * Test whether unnormalised strings parse as Roman numbers in general, and
   * fail when parsing with normalisation..
   */
  protected final void checkUnnormalisedRomanNumber(String s) {
    assertTrue(NumberUtil.isRomanNumber(s));
    assertFalse(NumberUtil.isNormalisedRomanNumber(s));
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


  public final void testPadNumbers() {
    assertEquals("001", NumberUtil.padNumbers("1", 3));
    assertEquals("001", NumberUtil.padNumbers("01", 3));
    assertEquals("001", NumberUtil.padNumbers("001", 3));
    assertEquals("1000", NumberUtil.padNumbers("1000", 3));

    // Padding to <1 does nothing to the input string
    assertEquals("1", NumberUtil.padNumbers("1", 0));
    assertEquals("001", NumberUtil.padNumbers("001", 0));
    assertEquals("001", NumberUtil.padNumbers("001", 1));
    assertEquals("001", NumberUtil.padNumbers("001", -1));
    assertEquals("1", NumberUtil.padNumbers("1", 1));
    assertEquals("1", NumberUtil.padNumbers("1", -1));

    // Pad numbers
    assertEquals("001", NumberUtil.padNumbers(1, 3));
    assertEquals("1000", NumberUtil.padNumbers(1000, 3));
    assertEquals("1", NumberUtil.padNumbers(1, 0));

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

  public final void testAreAlphabeticallyConsecutive() {
    assertTrue(NumberUtil.areAlphabeticallyConsecutive("z", "ba"));
    assertTrue(NumberUtil.areAlphabeticallyConsecutive("aa", "ab"));
    assertTrue(NumberUtil.areAlphabeticallyConsecutive("ab", "ac"));
    assertTrue(NumberUtil.areAlphabeticallyConsecutive("ay", "az"));
    assertTrue(NumberUtil.areAlphabeticallyConsecutive("bz", "ca"));

    assertTrue(NumberUtil.areAlphabeticallyConsecutive("aaa", "aab"));
    assertTrue(NumberUtil.areAlphabeticallyConsecutive("aaz", "aba"));
    assertTrue(NumberUtil.areAlphabeticallyConsecutive("azz", "baa"));
    assertTrue(NumberUtil.areAlphabeticallyConsecutive("zzy", "zzz"));
    assertTrue(NumberUtil.areAlphabeticallyConsecutive("zyz", "zza"));

    // Try mixed case - should be preserved
    assertTrue(NumberUtil.areAlphabeticallyConsecutive("AAA", "AAB"));
    assertTrue(NumberUtil.areAlphabeticallyConsecutive("aaZ", "abA"));
    assertTrue(NumberUtil.areAlphabeticallyConsecutive("aZz", "bAa"));
    assertTrue(NumberUtil.areAlphabeticallyConsecutive("Zzy", "Zzz"));
    assertTrue(NumberUtil.areAlphabeticallyConsecutive("ZyZ", "ZzA"));

    // Not consecutive
    assertFalse(NumberUtil.areAlphabeticallyConsecutive("a", "c"));
    assertFalse(NumberUtil.areAlphabeticallyConsecutive("a", "z"));
    assertFalse(NumberUtil.areAlphabeticallyConsecutive("b", "a"));

    // Changing case means the strings are not alphabetically consecutive
    assertFalse(NumberUtil.areAlphabeticallyConsecutive("AAA", "AAb"));
    assertFalse(NumberUtil.areAlphabeticallyConsecutive("a", "B"));
    assertFalse(NumberUtil.areAlphabeticallyConsecutive("A", "b"));
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

    // Test if rangeIncludes() can be used to establish whether a<=b
    // 0-b is range, a is search term
    assertTrue( NumberUtil.rangeIncludes("0-abc", "abc"));
    assertTrue( NumberUtil.rangeIncludes("0-abc", "ab"));
    assertFalse(NumberUtil.rangeIncludes("0-abc", "abd"));
    assertTrue( NumberUtil.rangeIncludes("0-abc", "0"));
    assertTrue( NumberUtil.rangeIncludes("0-abc", "01"));
    assertTrue( NumberUtil.rangeIncludes("0-abc", "1"));

    // Use compareTo
    assertEquals("abc".compareTo("abc")<=0,
        NumberUtil.rangeIncludes("0-abc", "abc"));
    assertEquals("ab".compareTo("abc")<0,
        NumberUtil.rangeIncludes("0-abc", "ab"));
    assertEquals("abd".compareTo("abc")<0,
        NumberUtil.rangeIncludes("0-abc", "abd"));
    assertEquals("0".compareTo("abc")<0,
        NumberUtil.rangeIncludes("0-abc", "0"));
    assertEquals("01".compareTo("abc")<0,
        NumberUtil.rangeIncludes("0-abc", "01"));
    assertEquals("1".compareTo("abc")<0,
        NumberUtil.rangeIncludes("0-abc", "1"));


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

 
  public final void testIncrementAlphabeticalString() {
    assertEquals("aab", NumberUtil.incrementBase26String("aaa"));
    assertEquals("aba", NumberUtil.incrementBase26String("aaz"));
    assertEquals("baa", NumberUtil.incrementBase26String("azz"));
    assertEquals("zzz", NumberUtil.incrementBase26String("zzy"));
    assertEquals("zza", NumberUtil.incrementBase26String("zyz"));

    // Try mixed case - should be preserved
    assertEquals("AAB", NumberUtil.incrementBase26String("AAA"));
    assertEquals("abA", NumberUtil.incrementBase26String("aaZ"));
    assertEquals("bAa", NumberUtil.incrementBase26String("aZz"));
    assertEquals("Zzz", NumberUtil.incrementBase26String("Zzy"));
    assertEquals("ZzA", NumberUtil.incrementBase26String("ZyZ"));

    // Try incrementing by different amounts
    assertEquals("aac", NumberUtil.incrementBase26String("aaa", 2));
    assertEquals("abc", NumberUtil.incrementBase26String("aaz", 3));
    assertEquals("baz", NumberUtil.incrementBase26String("azz", 26));
    assertEquals("zzz", NumberUtil.incrementBase26String("zzy", 1));
    assertEquals("zzz", NumberUtil.incrementBase26String("zyz", 26));
    assertEquals("baa", NumberUtil.incrementBase26String("a", 26 * 26));

    // Try decrementing
    assertEquals("aaa", NumberUtil.incrementBase26String("aab", -1));
    assertEquals("aax", NumberUtil.incrementBase26String("aaz", -2));
    assertEquals("axz", NumberUtil.incrementBase26String("azz", -52));
    assertEquals("aza", NumberUtil.incrementBase26String("azz", -25));
    assertEquals("abc", NumberUtil.incrementBase26String("abc", 0));
    assertEquals("yzz", NumberUtil.incrementBase26String("zzz", -(26 * 26)));

    // Try strings at the limit for their length
    assertEquals("ba", NumberUtil.incrementBase26String("z"));
    assertEquals("baa", NumberUtil.incrementBase26String("zz"));
    assertEquals("baaa", NumberUtil.incrementBase26String("zzz"));

    // Try decrementing to negative number
    try {
      NumberUtil.incrementBase26String("a", -1);
      fail("Should throw exception when trying to decrement below 0");
    } catch (NumberFormatException e) {
      /*Expected exception.*/
    }

    // Non-alphabetic chars should cause exception
    for (String s : new String[]{"abc1", "ABC1","£AbC"}) {
      try {
        NumberUtil.incrementBase26String(s);
        fail("Should throw exception with non-alphabetic chars");
      } catch (NumberFormatException e) {
        /*Expected exception.*/
      } 
    }
  }

  public final void testBase26() {
    assertEquals("a", NumberUtil.toBase26(0));
    assertEquals("b", NumberUtil.toBase26(1));
    assertEquals("ba", NumberUtil.toBase26(26));
    assertEquals("cb", NumberUtil.toBase26(53));
    assertEquals("baa", NumberUtil.toBase26(26*26));

    assertEquals(0, NumberUtil.fromBase26("a"));
    assertEquals(1, NumberUtil.fromBase26("b"));
    assertEquals(26, NumberUtil.fromBase26("ba"));
    assertEquals(53, NumberUtil.fromBase26("cb"));
    assertEquals(26*26, NumberUtil.fromBase26("baa"));

    // Try converting negative number
    try {
      NumberUtil.toBase26(-1);
      fail("Should throw exception when trying to convert negative number to base-26");
    } catch (NumberFormatException e) {
      /*Expected exception.*/
    }

    // Try converting non-alphabetical number
    try {
      NumberUtil.fromBase26("-abc");
      fail("Should throw exception for non-base-26 string input.");
    } catch (Exception e) {
      /*Expected*/
    }

  }


  public final void testIsContiguousRange() {
    // Empty string does not represent a contiguous range
    assertFalse(NumberUtil.isContiguousRange(""));

    // Single ranges
    assertTrue(NumberUtil.isContiguousRange("1"));
    assertTrue(NumberUtil.isContiguousRange("1-3"));
    assertTrue(NumberUtil.isContiguousRange("I - III"));

    // Consecutive ranges
    assertTrue(NumberUtil.isContiguousRange("1, 2-4, 5-6, 7"));
    assertTrue(NumberUtil.isContiguousRange("10-14, 15-20"));
    assertTrue(NumberUtil.isContiguousRange("s1-s4; s5"));
    // Ranges with Roman tokens and hyphens in range identifiers
    assertFalse(NumberUtil.isContiguousRange("s1-I-s1-IIII; s1-V")); // IIII is not normalised form
    assertTrue(NumberUtil.isContiguousRange("s1-I-s1-IV; s1-V"));
    assertTrue(NumberUtil.isContiguousRange("s1-II - s1-4; s1-V"));
    // Alphabetic ranges, mixed delimiters
    assertTrue(NumberUtil.isContiguousRange("aa,ab,ac-ay;az-bz;ca-no"));
    assertTrue(NumberUtil.isContiguousRange("a1-2,a1-3,a1-4-a1-10"));
    assertTrue(NumberUtil.isContiguousRange("a1-02 , a1-03 ; a1-04  -  a1-10"));


    // Not consecutive ranges
    assertFalse(NumberUtil.isContiguousRange("10-14, 16-20"));
    assertFalse(NumberUtil.isContiguousRange("10-14, 14-20"));
    // Test on Romans
    assertFalse(NumberUtil.isContiguousRange("I, III"));

  }

  public void testRoundToNDecimals() {
    assertEquals(5.0, NumberUtil.roundToNDecimals(5.0, 1));
    assertEquals(5.0, NumberUtil.roundToNDecimals(5.0, 3));
    assertEquals(5.2, NumberUtil.roundToNDecimals(5.25, 1));
    assertEquals(5.3, NumberUtil.roundToNDecimals(5.252, 1));
    assertEquals(5.3, NumberUtil.roundToNDecimals(5.2529999, 1));
    assertEquals(5.25, NumberUtil.roundToNDecimals(5.252, 2));
    assertEquals(5.252, NumberUtil.roundToNDecimals(5.252, 3));
    assertEquals(5.252, NumberUtil.roundToNDecimals(5.252, 4));
  }

  public void testIntPow() {
    assertEquals(0, NumberUtil.intPow(0, 6));
    assertEquals(1, NumberUtil.intPow(10, 0));
    assertEquals(10, NumberUtil.intPow(10, 1));
    assertEquals(100, NumberUtil.intPow(10, 2));
    assertEquals(1000, NumberUtil.intPow(10, 3));
    assertEquals(10000, NumberUtil.intPow(10, 4));
    assertEquals(10000000, (long)Math.pow(10, 7));
    assertEquals(100000000, (long)Math.pow(10, 8));
    assertEquals(425927596977747L, NumberUtil.intPow(123, 7));
    try {
      assertEquals(10000, NumberUtil.intPow(10, -4));
      fail("Negative exponent should throw");
    } catch (IllegalArgumentException e) {
    }
  }

}
