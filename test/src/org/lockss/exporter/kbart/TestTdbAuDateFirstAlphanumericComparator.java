package org.lockss.exporter.kbart;

import java.util.Collections;
import java.util.List;
import java.util.Vector;

import org.lockss.config.TdbAu;
import org.lockss.config.TdbTestUtil;
import org.lockss.test.LockssTestCase;

import junit.framework.TestCase;

public class TestTdbAuDateFirstAlphanumericComparator extends LockssTestCase {

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
   * Order the list using a date first alphanumeric comparator.
   */
  public final void testCompare() {
    TdbAuAlphanumericComparator comp = new TdbAuDateFirstAlphanumericComparator();
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
