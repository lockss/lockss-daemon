/*
 * $Id: TestBibliographicComparator.java,v 1.1.10.2 2012-06-26 00:56:54 tlipkis Exp $
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
package org.lockss.exporter.biblio;

import org.lockss.test.LockssTestCase;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

public class TestBibliographicComparator extends LockssTestCase {

  private final List<BibliographicItem> itemsWithYearsReverseOrder = new Vector<BibliographicItem>();
  private final List<BibliographicItem> itemsWithNames = new Vector<BibliographicItem>();

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

    // These should be ordered by name, taking account of numerical tokens
    itemsWithNames.add( new BibliographicItemImpl().setName(TITLE_ORDERED_7) );
    itemsWithNames.add( new BibliographicItemImpl().setName(TITLE_ORDERED_6) );
    itemsWithNames.add( new BibliographicItemImpl().setName(TITLE_ORDERED_5) );
    itemsWithNames.add( new BibliographicItemImpl().setName(TITLE_ORDERED_4) );
    itemsWithNames.add( new BibliographicItemImpl().setName(TITLE_ORDERED_3) );
    itemsWithNames.add( new BibliographicItemImpl().setName(TITLE_ORDERED_2) );
    itemsWithNames.add( new BibliographicItemImpl().setName(TITLE_ORDERED_1) );
    
    // These should be ordered by name, regardless of year
    itemsWithYearsReverseOrder.add( new BibliographicItemImpl().setName(TITLE_ORDERED_7).setYear("2001") );
    itemsWithYearsReverseOrder.add( new BibliographicItemImpl().setName(TITLE_ORDERED_6).setYear("2002") );
    itemsWithYearsReverseOrder.add( new BibliographicItemImpl().setName(TITLE_ORDERED_5).setYear("2003") );
    itemsWithYearsReverseOrder.add( new BibliographicItemImpl().setName(TITLE_ORDERED_4).setYear("2004") );
    itemsWithYearsReverseOrder.add( new BibliographicItemImpl().setName(TITLE_ORDERED_3).setYear("2005") );
    itemsWithYearsReverseOrder.add( new BibliographicItemImpl().setName(TITLE_ORDERED_2).setYear("2006") );
    itemsWithYearsReverseOrder.add( new BibliographicItemImpl().setName(TITLE_ORDERED_1).setYear("2007") );

  }

  protected void tearDown() throws Exception {
    super.tearDown();
  }

  /**
   * The alphanumeric comparator orders purely on names.
   */
  public final void testCompare() {
    Comparator<BibliographicItem> comp = BibliographicComparatorFactory.getNameComparator();

    // Shuffle and sort names
    Collections.shuffle(itemsWithNames);
    Collections.sort(itemsWithNames, comp);
    // Check the titles one by one 
    for (int i = 0; i < itemsWithNames.size(); i++) {
      assertEquals(orderedTitles[i], itemsWithNames.get(i).getName());
    }

    // Sort names with years
    Collections.sort(itemsWithYearsReverseOrder, comp);
    for (int i = 0; i < itemsWithYearsReverseOrder.size(); i++) {
      assertEquals(orderedTitles[i], itemsWithYearsReverseOrder.get(i).getName());
    }
  }

}
