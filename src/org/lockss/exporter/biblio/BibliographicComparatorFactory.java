/*
 * $Id$
 */

/*

Copyright (c) 2011 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.Comparator;

/**
 * A factory for comparators which attempt to sort
 * <code>BibliographicItem</code>s alphanumerically on a particular property.
 * The factory methods return a new {@link BibliographicComparator} which
 * overrides the <code>getBibliographicComparisonString()</code> method to
 * provide the appropriate comparison string.
 * <p>
 * Sorting volume strings can be a challenge; one problem we try to overcome
 * here is that volume strings can consist of a mix of alphanumeric and Roman
 * numeral tokens, rendering standard alphanumeric ordering insufficient.
 * Therefore. when creating a comparator on volume strings, we first try to
 * normalise the string by converting anything that looks like a Roman numeral
 * token into a number. Note that the superclass deals with padding numbers
 * before comparison.
 *
 * @author Neil Mayo
 */
public class BibliographicComparatorFactory {

  /**
   * Return a BibliographicItem comparator which sorts by the name.
   * @return a BibliographicItem name comparator
   */
  public static Comparator<BibliographicItem> getNameComparator() {
    return new BibliographicComparator() {
      @Override
      protected String getBibliographicComparisonString(BibliographicItem item) {
        return item.getName();
      }
    };
  }

  /**
   * Create a BibliographicItem comparator which sorts by the item's volume.
   * @return a BibliographicItem volume comparator
   */
  public static Comparator<BibliographicItem> getIssnComparator() {
    return new BibliographicComparator() {
      @Override
      protected String getBibliographicComparisonString(BibliographicItem item) {
        return item.getIssn();
      }
    };
  }

  /**
   * Create a BibliographicItem comparator which sorts by the item's volume.
   * @return a BibliographicItem volume comparator
   */
  public static Comparator<BibliographicItem> getVolumeComparator() {
    return new BibliographicComparator() {
      @Override
      protected String getBibliographicComparisonString(BibliographicItem item) {
        return item.getVolume();
      }
      @Override
      protected String xlate(String s) {
        return super.xlate(BibliographicUtil.translateRomanTokens(s));
      }
    };
  }

  /**
   * Create a BibliographicItem comparator which sorts by the item's start volume.
   * @return a BibliographicItem start volume comparator
   */
  public static Comparator<BibliographicItem> getStartVolumeComparator() {
    return new BibliographicComparator() {
      @Override
      protected String getBibliographicComparisonString(BibliographicItem item) {
        return item.getStartVolume();
      }
      @Override
      protected String xlate(String s) {
        return super.xlate(BibliographicUtil.translateRomanTokens(s));
      }
    };
  }

  /**
   * Create a BibliographicItem comparator which sorts by the item's end volume.
   * @return a BibliographicItem end volume comparator
   */
  public static Comparator<BibliographicItem> getEndVolumeComparator() {
    return new BibliographicComparator() {
      @Override
      protected String getBibliographicComparisonString(BibliographicItem item) {
        return item.getEndVolume();
      }
      @Override
      protected String xlate(String s) {
        return super.xlate(BibliographicUtil.translateRomanTokens(s));
      }
    };
  }

  /**
   * Create a BibliographicItem comparator which sorts by the first date in the
   * item's year.
   * @return a BibliographicItem start date comparator
   */
  public static Comparator<BibliographicItem> getStartDateComparator() {
    return new BibliographicComparator() {
      @Override
      protected String getBibliographicComparisonString(BibliographicItem item) {
        return item.getStartYear();
      }
    };
  }

  /**
   * Create a BibliographicItem comparator which sorts by the last date in the
   * item's year.
   * @return a BibliographicItem end date comparator
   */
  public static Comparator<BibliographicItem> getEndDateComparator() {
    return new BibliographicComparator() {
      @Override
      protected String getBibliographicComparisonString(BibliographicItem item) {
	return item.getEndYear();
      }
    };
  }

}

