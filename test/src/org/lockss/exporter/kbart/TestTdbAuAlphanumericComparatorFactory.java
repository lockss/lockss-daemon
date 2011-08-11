/*
 * $Id: TestTdbAuAlphanumericComparatorFactory.java,v 1.1 2011-08-11 16:52:38 easyonthemayo Exp $
 */

/*

Copyright (c) 2010-2011 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.exporter.kbart;

import java.util.Collections;
import java.util.List;
import java.util.Vector;

import org.apache.commons.collections.comparators.ComparatorChain;
import org.lockss.config.TdbAu;
import org.lockss.config.TdbTestUtil;
import org.lockss.test.LockssTestCase;

import junit.framework.TestCase;

public class TestTdbAuAlphanumericComparatorFactory extends LockssTestCase {

  private final List<TdbAu> ausWithYearRanges = new Vector<TdbAu>();
  private final List<TdbAu> ausWithYears = new Vector<TdbAu>();
  private final List<TdbAu> ausWithYearsReverseOrder = new Vector<TdbAu>();
  private final List<TdbAu> ausWithNames = new Vector<TdbAu>();

  private final String TITLE_ORDERED_1 = "A Journal Volume 5 with spurious extra text";
  private final String TITLE_ORDERED_2 = "A Journal Volume 6";
  private final String TITLE_ORDERED_3 = "A Journal Volume 7";
  private final String TITLE_ORDERED_4 = "A Journal Volume 8a";
  private final String TITLE_ORDERED_5 = "A Journal Volume 9";
  private final String TITLE_ORDERED_6 = "A Journal Volume 10";
  private final String TITLE_ORDERED_7 = "A Journal Volume 100";
  private final String[] orderedTitles = new String[] {
    TITLE_ORDERED_1,
    TITLE_ORDERED_2,
    TITLE_ORDERED_3,
    TITLE_ORDERED_4,
    TITLE_ORDERED_5,
    TITLE_ORDERED_6,
    TITLE_ORDERED_7
  };
  
  protected void setUp() throws Exception {
    super.setUp();
  
    // These should be ordered by year, regardless of name
    ausWithYears.add( TdbTestUtil.createBasicAu(TITLE_ORDERED_7, "2006") );
    ausWithYears.add( TdbTestUtil.createBasicAu(TITLE_ORDERED_6, "2005") );
    ausWithYears.add( TdbTestUtil.createBasicAu(TITLE_ORDERED_5, "2004") );
    ausWithYears.add( TdbTestUtil.createBasicAu(TITLE_ORDERED_4, "2003") );
    ausWithYears.add( TdbTestUtil.createBasicAu(TITLE_ORDERED_3, "2002") );
    ausWithYears.add( TdbTestUtil.createBasicAu(TITLE_ORDERED_2, "2001") );
    ausWithYears.add( TdbTestUtil.createBasicAu(TITLE_ORDERED_1, "2000") );
    
    // These should be ordered by name, taking account of numerical tokens
    ausWithNames.add( TdbTestUtil.createBasicAu(TITLE_ORDERED_7, null) );
    ausWithNames.add( TdbTestUtil.createBasicAu(TITLE_ORDERED_6, null) );
    ausWithNames.add( TdbTestUtil.createBasicAu(TITLE_ORDERED_5, null) );
    ausWithNames.add( TdbTestUtil.createBasicAu(TITLE_ORDERED_4, null) );
    ausWithNames.add( TdbTestUtil.createBasicAu(TITLE_ORDERED_3, null) );
    ausWithNames.add( TdbTestUtil.createBasicAu(TITLE_ORDERED_2, null) );
    ausWithNames.add( TdbTestUtil.createBasicAu(TITLE_ORDERED_1, null) );
    
    // These should be ordered by year; if ordered by name the ordering failed 
    ausWithYearsReverseOrder.add( TdbTestUtil.createBasicAu(TITLE_ORDERED_1, "2006") );
    ausWithYearsReverseOrder.add( TdbTestUtil.createBasicAu(TITLE_ORDERED_2, "2005") );
    ausWithYearsReverseOrder.add( TdbTestUtil.createBasicAu(TITLE_ORDERED_3, "2004") );
    ausWithYearsReverseOrder.add( TdbTestUtil.createBasicAu(TITLE_ORDERED_4, "2003") );
    ausWithYearsReverseOrder.add( TdbTestUtil.createBasicAu(TITLE_ORDERED_5, "2002") );
    ausWithYearsReverseOrder.add( TdbTestUtil.createBasicAu(TITLE_ORDERED_6, "2001") );
    ausWithYearsReverseOrder.add( TdbTestUtil.createBasicAu(TITLE_ORDERED_7, "2000") );

    // These should be ordered by year, regardless of name
    ausWithYearRanges.add( TdbTestUtil.createBasicAu(TITLE_ORDERED_7, "2006-2007") );
    ausWithYearRanges.add( TdbTestUtil.createBasicAu(TITLE_ORDERED_6, "2006") );
    ausWithYearRanges.add( TdbTestUtil.createBasicAu(TITLE_ORDERED_5, "2003-2006") );
    ausWithYearRanges.add( TdbTestUtil.createBasicAu(TITLE_ORDERED_4, "2002-2003") );
    ausWithYearRanges.add( TdbTestUtil.createBasicAu(TITLE_ORDERED_3, "2001") );
    ausWithYearRanges.add( TdbTestUtil.createBasicAu(TITLE_ORDERED_2, "2000-2001") );
    ausWithYearRanges.add( TdbTestUtil.createBasicAu(TITLE_ORDERED_1, "2000") );
    
  }

  protected void tearDown() throws Exception {
    super.tearDown();
  }

  /**
   * Order the list using a last date, first date, name alphanumeric comparator.
   */
  public final void testCompare() {
    //TdbAuAlphanumericComparator comp = new TdbAuDateFirstAlphanumericComparator();
    //TdbAuAlphanumericComparator comp = TdbAuAlphanumericComparatorFactory.getFirstDateComparator();
    /*CompositeComparator<TdbAu> comp = 
      new CompositeComparator<TdbAu>(TdbAuAlphanumericComparatorFactory.getLastDateComparator())
      .compose(TdbAuAlphanumericComparatorFactory.getFirstDateComparator())
      .compose(TdbAuAlphanumericComparatorFactory.getNameComparator())
      ;*/
    ComparatorChain comp = 
      new ComparatorChain(TdbAuAlphanumericComparatorFactory.getLastDateComparator());
    comp.addComparator(TdbAuAlphanumericComparatorFactory.getFirstDateComparator());
    comp.addComparator(TdbAuAlphanumericComparatorFactory.getNameComparator());
      
    // Sort the two arrays, which should sort on year if available, or name
    // The result should be the same
    Collections.sort(ausWithYears, comp);
    Collections.sort(ausWithNames, comp);
    Collections.sort(ausWithYearRanges, comp);

    // Check the titles one by one 
    for (int i = 0; i < ausWithYears.size(); i++) {
      String name = orderedTitles[i];
      assertEquals(ausWithYears.get(i).getName(), name);
      assertEquals(ausWithNames.get(i).getName(), name);
      assertEquals(ausWithYearRanges.get(i).getName(), name);
    }

    // Sort the other array - the result should yield the names in reverse order
    Collections.sort(ausWithYearsReverseOrder, comp);
    int n = orderedTitles.length-1; // last index
    for (int i = 0; i < ausWithYearsReverseOrder.size(); i++) {
      assertEquals(orderedTitles[n-i], ausWithYearsReverseOrder.get(i).getName());
    }
  }

}
