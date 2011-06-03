/*
 * $Id: TestRoman.java,v 1.1 2011-06-03 16:51:17 pgust Exp $
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
 * This is the test class for org.lockss.util.Roman
 */
public class TestRoman extends LockssTestCase {
  @SuppressWarnings("rawtypes")
  public static Class[] testedClasses = {
    org.lockss.util.Roman.class
  };

  public TestRoman(String msg) {
    super(msg);
  }


  /**
   * Test normalized Roman and Arabic numbers.
   */
  public void testNormalized() {
	String[][] validNormalizedRomanStrings = {
	  {"I","1"}, {"VI","6"}, {"XIII","13"}, {"LXXX","80"}, 
	  {"XC","90"}, {"DLXI","561"}, {"MCMLXXIV","1974"},
	  {"(MMMDCCCL)MMCDXXIX","3852429"},
	  {"MMM", "3000"}, {"(IV)", "4000"}, {"(V)M","6000"},
	  {"(MMMV)CMXCIX", "3005999"},
	  {"(MMMDCCCL)MMCDXXIX", "3852429"},
	  {"((MMCXLV)MMCDLXXX)MMMDCXLVII",Integer.toString(Integer.MAX_VALUE)}

	};
	  
	for (String s[] : validNormalizedRomanStrings) {
	  try {
		String romanValue = s[0];
		String arabicValue = s[1];
		int shortValue = Integer.parseInt(arabicValue);
		
		// roman value matches roman string representation
		assertEquals(romanValue, new Roman(romanValue).toString());
		// roman value is normalized
		assertEquals(romanValue, Roman.toRomanString(romanValue));
		// roman is normalized
		Roman.parseRoman(romanValue, true);
		// roman matches arabic
		assertTrue(new Roman(romanValue).equals(new Roman(arabicValue)));
		// roman matches integer
		assertTrue(new Roman(romanValue).compareTo(new Roman(shortValue)) == 0);
	  } catch (NumberFormatException ex) {
		fail(ex.getMessage());
	  } catch (IllegalArgumentException ex) {
		fail(ex.getMessage());
	  }
	}
  }	
  
  /**
   * Test unnormalized Roman numbers.
   */
  public void testUnnormalized() {
	String[][] validUnnormalizedRomanStrings = {
	  {"VIIII","9"}, {"LXXXXI","91"}, {"DDDDIV","2004"}, 
	  {"((I))","1000000"}, {"(MMMDCCCLII)CDXXIX","3852429"},
	  {"((MMCXLVII)CDLXXXIII)DCXLVII",Integer.toString(Integer.MAX_VALUE)}
	};
		  
	for (String s[] : validUnnormalizedRomanStrings) {
	  String romanValue = s[0];
	  String arabicValue = s[1];
	  try {
		int shortValue = Integer.parseInt(arabicValue);
		
		// unnormalized roman value matches roman string representation
		assertNotEquals(romanValue, new Roman(romanValue).toString());
		// roman value is nnot normalized
		assertNotEquals(romanValue, Roman.toRomanString(romanValue));
		// roman matches arabic
		assertTrue(new Roman(romanValue).equals(new Roman(arabicValue)));
		// roman matches integer
		assertTrue(new Roman(romanValue).compareTo(new Roman(shortValue)) == 0);
	  } catch (NumberFormatException ex) {
		fail(ex.getMessage());
	  } catch (IllegalArgumentException ex) {
		fail(ex.getMessage());
	  }

	  try {
		// roman is not normalized
		Roman.parseRoman(romanValue, true);
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
	  "xyzzy", "1234abc", "", "-iv", 
	  "((MMCXLVII)CDLXXXIII)DCXLVIII"
	};
		  
	for (String s : invalidRomanStrings) {
	  try {
		new Roman(s); 
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
		new Roman(s); 
		fail(s + " is an invalid Arabic String");
	  } catch (NumberFormatException ex) {
	  } catch (IllegalArgumentException ex) {
	  }
	}
	
	for (String s : invalidArabicStrings) {
	  try {
		short shortValue = Short.parseShort(s);
		new Roman(shortValue); 
		fail(shortValue + " is an invalid integer value");
	  } catch (NumberFormatException ex) {
	  } catch (IllegalArgumentException ex) {
	  }
	}
  }
}
