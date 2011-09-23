/*
 * $Id: TdbAuAlphanumericComparatorFactory.java,v 1.3.2.1 2011-09-23 13:23:33 easyonthemayo Exp $
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

import java.util.Comparator;

import org.lockss.config.TdbAu;

/**
 * A factory for comparators which attempt sort <code>TdbAu</code>s 
 * alphanumerically on a particular property. The factory methods return 
 * a new {@link TdbAuAlphanumericComparator} which overrides the 
 * <code>getTdbAuComparisonString()</code> method to provide the appropriate 
 * comparison string. 
 *	
 * @author neil
 */
public class TdbAuAlphanumericComparatorFactory {

  /**
   * Return a TdbAu comparator which sorts by the TdbAu name.
   * @return a TdbAu name comparator
   */
  public static Comparator<TdbAu> getNameComparator() {
    return new TdbAuAlphanumericComparator() {
      @Override
      protected String getTdbAuComparisonString(TdbAu tdbAu) {
        return tdbAu.getName();
      }
    };
  }

  /**
   * Create a TdbAu comparator which sorts by the TdbAu volume.
   * @return a TdbAu volume comparator
   */
  public static Comparator<TdbAu> getVolumeComparator() {
    return new TdbAuAlphanumericComparator() {
      @Override
      protected String getTdbAuComparisonString(TdbAu tdbAu) {
        return tdbAu.getVolume();
      }
    };
  }

  /**
   * Create a TdbAu comparator which sorts by the start volume in the TdbAu volume.
   * @return a TdbAu first volume comparator
   */
  public static Comparator<TdbAu> getFirstVolumeComparator() {
    return new TdbAuAlphanumericComparator() {
      @Override
      protected String getTdbAuComparisonString(TdbAu tdbAu) {
        return tdbAu.getStartVolume();
      }
    };
  }

  /**
   * Create a TdbAu comparator which sorts by the end volume in the TdbAu volume.
   * @return a TdbAu last volume comparator
   */
  public static Comparator<TdbAu> getLastVolumeComparator() {
    return new TdbAuAlphanumericComparator() {
      @Override
      protected String getTdbAuComparisonString(TdbAu tdbAu) {
        return tdbAu.getEndVolume();
      }
    };
  }

  /**
   * Create a TdbAu comparator which sorts by the first date in the TdbAu year.
   * @return a TdbAu first date comparator
   */
  public static Comparator<TdbAu> getFirstDateComparator() {
    return new TdbAuAlphanumericComparator() {
      @Override
      protected String getTdbAuComparisonString(TdbAu tdbAu) {
        return tdbAu.getStartYear();
      }
    };
  }

  /**
   * Create a TdbAu comparator which sorts by the last date in the TdbAu year.
   * @return a TdbAu last date comparator
   */
  public static Comparator<TdbAu> getLastDateComparator() {
    return new TdbAuAlphanumericComparator() {
      @Override
      protected String getTdbAuComparisonString(TdbAu tdbAu) {
	return tdbAu.getEndYear();
      }
    };
  }

}

