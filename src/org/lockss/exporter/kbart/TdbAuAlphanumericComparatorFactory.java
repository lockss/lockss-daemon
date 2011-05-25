/*
 * $Id: TdbAuAlphanumericComparatorFactory.java,v 1.1.2.2 2011-05-25 13:50:33 easyonthemayo Exp $
 */

/*

Copyright (c) 2010 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.config.TdbAu;

/**
 * A factory for comparators which attempt sort <code>TdbAu</code>s alphanumerically on a 
 * particular property. The factory methods return a new {@link TdbAuAlphanumericComparator}
 * which overrides the <code>getTdbAuComparisonString()</code> method to provide the 
 * appropriate comparison string. The case-sensitivity of string comparison can be defined 
 * after construction using the {@link TdbAuAlphanumericComparator.setCaseSensitive()}
 * method. 
 *	
 * @author neil
 */
public class TdbAuAlphanumericComparatorFactory {

  /**
   * Create a TdbAu comparator which sorts by the TdbAu name.
   * @return a TdbAu name comparator
   */
  public static TdbAuAlphanumericComparator getNameComparator() {
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
  public static TdbAuAlphanumericComparator getVolumeComparator() {
    return new TdbAuAlphanumericComparator() {
      @Override
      protected String getTdbAuComparisonString(TdbAu tdbAu) {
        return tdbAu.getVolume();
      }
    };
  }

  /**
   * Create a TdbAu comparator which sorts by the first date in the TdbAu year.
   * @return a TdbAu first date comparator
   */
  public static TdbAuAlphanumericComparator getFirstDateComparator() {
    return new TdbAuAlphanumericComparator() {
      @Override
      protected String getTdbAuComparisonString(TdbAu tdbAu) {
	return KbartTdbAuUtil.getFirstYear( KbartTdbAuUtil.findYear(tdbAu) );
      }
    };
  }

  /**
   * Create a TdbAu comparator which sorts by the last date in the TdbAu year.
   * @return a TdbAu last date comparator
   */
  public static TdbAuAlphanumericComparator getLastDateComparator() {
    return new TdbAuAlphanumericComparator() {
      @Override
      protected String getTdbAuComparisonString(TdbAu tdbAu) {
	return KbartTdbAuUtil.getLastYear( KbartTdbAuUtil.findYear(tdbAu) );
      }
    };
  }

}

