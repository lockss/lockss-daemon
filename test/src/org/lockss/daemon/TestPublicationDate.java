/*
 * $Id$
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

package org.lockss.daemon;

import java.text.ParseException;
import java.util.*;
import org.lockss.test.*;

/**
 * Test class for org.lockss.util.ArrayIterator
 */

public class TestPublicationDate extends LockssTestCase {

  /**
   * Test big-endian date formats that put year first, month second, and day last.
   */
  public void testBigEndian() throws ParseException {
    assertEquals("2010-03-13", PublicationDate.parse("2010.3.13",Locale.US).toString());
    assertEquals("2010-03", PublicationDate.parse("2010.3",Locale.US).toString()); 
    assertEquals("2010-03-13", PublicationDate.parse("2010.3.13",Locale.US).toString()); 
    assertEquals("2010-03-13", PublicationDate.parse("2010-3-13",Locale.US).toString()); 
    assertEquals("2010-03", PublicationDate.parse("2010-3",Locale.US).toString()); 
    assertEquals("2010-03-13", PublicationDate.parse("2010 March 13",Locale.US).toString()); 
    assertEquals("2010-03-13", PublicationDate.parse("2010. March, 13",Locale.US).toString()); 
    assertEquals("2010-03-13", PublicationDate.parse("2010 March 13th",Locale.US).toString()); 
    assertEquals("2010-03-13", PublicationDate.parse("2010 13 marzo",Locale.ITALY).toString()); 
    assertEquals("2010-03-13", PublicationDate.parse("2010/3/13",Locale.US).toString()); 
    assertEquals("2010-03-13", PublicationDate.parse("2010/3/13",Locale.UK).toString()); 
    assertEquals("2010-03-13", PublicationDate.parse("2010/March/13",Locale.US).toString()); 
    assertEquals("2010-03-13", PublicationDate.parse("2010/M\u00e4rz/13",Locale.GERMANY).toString());  // a-umlaut 
    assertEquals("2010-03-13", PublicationDate.parse("2010/Mar./13",Locale.US).toString()); 
    assertEquals("2010-03-13", PublicationDate.parse("2010/Mar./13",Locale.US).toString()); 
    assertEquals("2010-03", PublicationDate.parse("2010/3",Locale.US).toString()); 
    assertEquals("2010-03", PublicationDate.parse("2010/Mar",Locale.US).toString()); 
    assertEquals("2010-03-13", PublicationDate.parse("2010-Mar-13, Sunday",Locale.US).toString()); 
    assertEquals("2010-Q1", PublicationDate.parse("2010-Q1",Locale.US).toString());
  }
  
  /**
   * Test little-endian date formats that put day first, month second, and year last.
   */
  public void testLittleEndian() throws ParseException {
    assertEquals("2010-03", PublicationDate.parse("Mar 2010",Locale.US).toString());
    assertEquals("2010-03", PublicationDate.parse("Mar., 2010",Locale.US).toString());
//  "May-June 2010", //     -> 2010-05 ??
//  "May/Jun 2010", //    -> 2010-05 ??
    assertEquals("2010-03-13", PublicationDate.parse("13 Mar 2010",Locale.US).toString());
    assertEquals("2010-03-13", PublicationDate.parse("13th March 2010",Locale.US).toString());
    assertEquals("2010-S2", PublicationDate.parse("Summer Quarter, 2010",Locale.US).toString());
    assertEquals("2010-S2", PublicationDate.parse("Summer 2010",Locale.US).toString());
//    assertEquals("2010-S1", PublicationDate.parse("Fr\u00fchling, 2010",Locale.GERMANY).toString());  // u-mlaut
//    assertEquals("2010-Q2", PublicationDate.parse("2 \u00ba trimestre, 2010",Locale.ITALY).toString()); // ordinal indicator
//    "Summer-Fall 2010", //    2010 2X  (non-standard)  ??
//    "Summer/Fall 2010", //    2010 2X  (non-standard)  ??
    assertEquals("2010-Q1", PublicationDate.parse("First Quarter 2010",Locale.US).toString());
    assertEquals("2010-Q1", PublicationDate.parse("First Quarter, 2010",Locale.US).toString());
    assertEquals("2010-Q1", PublicationDate.parse("ersten Quartal 2010",Locale.GERMANY).toString());
    assertEquals("2010-Q1", PublicationDate.parse("1st Quarter 2010",Locale.US).toString());
    assertEquals("2010-Q1", PublicationDate.parse("Quarter 1 2010",Locale.US).toString());
    assertEquals("2010-Q1", PublicationDate.parse("Quarter 1, 2010",Locale.US).toString());
    assertEquals("2010-Q1", PublicationDate.parse("Quarter One 2010",Locale.US).toString());
    assertEquals("2010-Q1", PublicationDate.parse("1Q 2010",Locale.US).toString());
    assertEquals("2010-Q1", PublicationDate.parse("Q1 2010",Locale.US).toString());  
    assertEquals("2010-Q1", PublicationDate.parse("Q1/2010",Locale.US).toString());  
  }
  
  /**
   * Test middle-endian date formats that put month first, day second, and year third.
   * This form is peculiar to the US English locale.
   */
  public void testMiddleEndian() throws ParseException {
    assertEquals("2010-03-13", PublicationDate.parse("March 13, 2010",Locale.US).toString());
    assertEquals("2010-03-13", PublicationDate.parse("Mar. 13, 2010",Locale.US).toString()); 
    assertEquals("2010-03-13", PublicationDate.parse("13 Mar. 2010",Locale.US).toString()); 
    assertEquals("2010-03-13", PublicationDate.parse("13 Mar., 2010",Locale.US).toString()); 
    assertEquals("2010-03-13", PublicationDate.parse("Mar 13th, 2010",Locale.US).toString());
    assertEquals("2010-03-13", PublicationDate.parse("Mar 13th, 2010",Locale.UK).toString());
    assertEquals("2010-03-13", PublicationDate.parse("3/13/2010",Locale.US).toString());
    assertEquals("2010-03-13", PublicationDate.parse("Mar/13/2010",Locale.US).toString());
    assertEquals("2010-03-13", PublicationDate.parse("Mar./13/2010",Locale.US).toString());
    assertEquals("2010-03-13", PublicationDate.parse("Mar.13.2010",Locale.US).toString());
    assertEquals("2010-03-13", PublicationDate.parse("Mar.13.2010",Locale.UK).toString());
    assertEquals("2010-03-13", PublicationDate.parse("Mar.13.2010",Locale.UK).toString());
    assertEquals("2010-03-13", PublicationDate.parse("3.13.2010",Locale.US).toString());
    assertEquals("2010-03-13", PublicationDate.parse("March.13.2010",Locale.US).toString());
  }
  
  /**
   * Test bad date formats
   */
  public void testBad() {
    try {
      PublicationDate.parse("20111226");
      fail();
    } catch (ParseException ex) { }
    try {
      PublicationDate.parse("funky.chicken");
      fail();
    } catch (ParseException ex) {}
    try {
      PublicationDate.parse("MCM");
      fail();
    } catch (ParseException ex) {}
  }

  public void testYearOnly() throws ParseException {
    assertEquals("2010", PublicationDate.parse("2010").toString());
  }

}
