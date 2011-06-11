/*
 * $Id: TestNumberUtil.java,v 1.3 2011-06-11 21:44:15 pgust Exp $
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
   * Test normalized Roman and Arabic numbers.
   */
  public void testNormalized() {
	String[] validNormalizedRomanStrings = {
	  "I", "VI", "XIII", "LXXX", "XC", 
	  "DLXI", "MCMLXXIV",
	  "(MMMDCCCL)MMCDXXIX", 
	  "MMM", "(IV)", "(V)M",
	  "(MMMV)CMXCIX", "(MMMDCCCL)MMCDXXIX",
	  "((MMCXLV)MMCDLXXX)MMMDCXLVII"
	};
    String[] equivalentArabicStrings = {
        "1", "6", "13", "80", "90", 
        "561", "1974",
        "3852429", 
        "3000", "4000", "6000",
        "3005999", "3852429",
        Integer.toString(Integer.MAX_VALUE) 
    };

    // string arrays of roman digits split from normalized roman strings
    String[][] equivalentRomanDigits = {
      {"I"}, {"V","I"}, {"X","I","I","I"}, {"L","X","X","X"}, {"XC"}, 
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
		Roman.parseRoman(romanValue, true);
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
	  "-1", "0", Long.toString(Roman.MAX_VALUE+1)
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
}
