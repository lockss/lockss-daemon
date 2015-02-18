/*
 * $Id$
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
  private final List<BibliographicItem> itemsWithRomanMixedVolumes = new Vector<BibliographicItem>();
  private final List<BibliographicItem> itemsWithRomanVolumes = new Vector<BibliographicItem>();

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

  // Volumes with mixed format incorporating Roman tokens
  private final String ROMAN_MIXED_VOL_ORDERED_1 = "os-1";
  private final String ROMAN_MIXED_VOL_ORDERED_2 = "os-2";
  private final String ROMAN_MIXED_VOL_ORDERED_3 = "os-III";
  private final String ROMAN_MIXED_VOL_ORDERED_4 = "os-IV";
  private final String ROMAN_MIXED_VOL_ORDERED_5 = "os-V";
  private final String ROMAN_MIXED_VOL_ORDERED_6 = "os-Xylophone";
  private final String[] orderedRomanMixedVols = new String[] {
      ROMAN_MIXED_VOL_ORDERED_1, ROMAN_MIXED_VOL_ORDERED_2, ROMAN_MIXED_VOL_ORDERED_3,
      ROMAN_MIXED_VOL_ORDERED_4, ROMAN_MIXED_VOL_ORDERED_5, ROMAN_MIXED_VOL_ORDERED_6
  };

  // Volumes with plain format Roman tokens
  private final String ROMAN_VOL_ORDERED_1 = "1";
  private final String ROMAN_VOL_ORDERED_2 = "2";
  private final String ROMAN_VOL_ORDERED_3 = "III";
  private final String ROMAN_VOL_ORDERED_4 = "IV";
  private final String ROMAN_VOL_ORDERED_5 = "V";
  private final String ROMAN_VOL_ORDERED_6 = "Xylophone";
  private final String[] orderedRomanVols = new String[] {
      ROMAN_VOL_ORDERED_1, ROMAN_VOL_ORDERED_2, ROMAN_VOL_ORDERED_3,
      ROMAN_VOL_ORDERED_4, ROMAN_VOL_ORDERED_5, ROMAN_VOL_ORDERED_6
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

    // These should be ordered by volume
    itemsWithRomanMixedVolumes.add( new BibliographicItemImpl().setVolume(ROMAN_MIXED_VOL_ORDERED_1) );
    itemsWithRomanMixedVolumes.add( new BibliographicItemImpl().setVolume(ROMAN_MIXED_VOL_ORDERED_2) );
    itemsWithRomanMixedVolumes.add( new BibliographicItemImpl().setVolume(ROMAN_MIXED_VOL_ORDERED_3) );
    itemsWithRomanMixedVolumes.add( new BibliographicItemImpl().setVolume(ROMAN_MIXED_VOL_ORDERED_4) );
    itemsWithRomanMixedVolumes.add( new BibliographicItemImpl().setVolume(ROMAN_MIXED_VOL_ORDERED_5) );
    // This one cannot be processed as a number and should come last
    itemsWithRomanMixedVolumes.add( new BibliographicItemImpl().setVolume(ROMAN_MIXED_VOL_ORDERED_6) );

    // These should be ordered by volume
    itemsWithRomanVolumes.add( new BibliographicItemImpl().setVolume(ROMAN_VOL_ORDERED_1) );
    itemsWithRomanVolumes.add( new BibliographicItemImpl().setVolume(ROMAN_VOL_ORDERED_2) );
    itemsWithRomanVolumes.add( new BibliographicItemImpl().setVolume(ROMAN_VOL_ORDERED_3) );
    itemsWithRomanVolumes.add( new BibliographicItemImpl().setVolume(ROMAN_VOL_ORDERED_4) );
    itemsWithRomanVolumes.add( new BibliographicItemImpl().setVolume(ROMAN_VOL_ORDERED_5) );
    // This one cannot be processed as a number and should come last
    itemsWithRomanVolumes.add( new BibliographicItemImpl().setVolume(ROMAN_VOL_ORDERED_6) );

  }

  protected void tearDown() throws Exception {
    super.tearDown();
  }

  /**
   * Try out some sorting using bibliographic comparisons.
   */
  public final void testNameComparator() {
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

  /**
   * Try out some sorting using bibliographic comparisons.
   */
  public final void testVolumeComparator() {
    Comparator<BibliographicItem> comp = BibliographicComparatorFactory.getVolumeComparator();

    // Shuffle and sort names
    Collections.shuffle(itemsWithRomanVolumes);
    Collections.sort(itemsWithRomanVolumes, comp);
    // Check the volumes one by one
    for (int i = 0; i < itemsWithRomanVolumes.size(); i++) {
      assertEquals(orderedRomanVols[i], itemsWithRomanVolumes.get(i).getVolume());
    }

    // Shuffle and sort names
    Collections.shuffle(itemsWithRomanMixedVolumes);
    Collections.sort(itemsWithRomanMixedVolumes, comp);
    // Check the volumes one by one
    for (int i = 0; i < itemsWithRomanMixedVolumes.size(); i++) {
      assertEquals(orderedRomanMixedVols[i], itemsWithRomanMixedVolumes.get(i).getVolume());
    }
  }

}
