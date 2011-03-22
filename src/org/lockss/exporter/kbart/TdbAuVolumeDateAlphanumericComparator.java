/*
 * $Id: TdbAuVolumeDateAlphanumericComparator.java,v 1.1.2.3 2011-03-22 19:03:09 easyonthemayo Exp $
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
import org.lockss.util.Logger;

/**
 * Sort a set of <code>TdbAu</code> objects from a single title into a pseudo-chronological 
 * order based on volumes, dates and names. There is no guaranteed way to order chronologically due 
 * to the incompleteness of volume and date metadata included in AUs at the time of writing; however 
 * the naming convention of AUs provides a best-guess alternative method.
 * <p>
 * First an attempt is made to order by any available volume information. If the volumes are 
 * non-numerical, the comparator falls back to the superclass, which attempts to order by years 
 * (in the case of a range, last years then first years), and then finally alphanumerically.
 *	
 * @author neil
 */
public class TdbAuVolumeDateAlphanumericComparator extends TdbAuDateFirstAlphanumericComparator {

  private static Logger log = Logger.getLogger("TdbAuVolumeDateAlphanumericComparator");

  @Override
  public int compare(TdbAu au1, TdbAu au2) {
    // First try volume comparison
    String vol1 = KbartTdbAuUtil.findVolume(au1);
    String vol2 = KbartTdbAuUtil.findVolume(au2);
    try {
      if (!"".equals(vol1) && !"".equals(vol2)) {
	int res = KbartTdbAuUtil.compareIntStrings(vol1, vol2);
	// Return if the volumes show a difference, otherwise fall through to date comparison
	if (res!=0) return res;
     }
    } catch (NumberFormatException e) {
      //log.warning("Could not compare volumes numerically ("+vol1+", "+vol2+")");
      // fall through to date/alphanumeric comparison
    }

    // Next, try date first comparison from superclass
    return super.compare(au1, au2);
  }
    
}

