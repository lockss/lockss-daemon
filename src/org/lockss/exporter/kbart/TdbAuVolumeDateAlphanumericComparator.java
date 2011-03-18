/*
 * $Id: TdbAuVolumeDateAlphanumericComparator.java,v 1.1.2.2 2011-03-18 16:38:03 easyonthemayo Exp $
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
 * non-numerical, the comparator attempts to order by years. By convention of TDB AU naming, 
 * ascending alphanumerical name order should also be chronological order within a title, so 
 * this is tried last. Note that in general the AU names are identical to the title name plus 
 * a volume or year identifier. However if a title's title/name changes during its run, multiple 
 * titles can appear in the AU records. This is probably wrong as the new title should get a 
 * different ISSN and have a separate title record.
 * <p>
 * Perhaps this comparator should throw an exception if the conventions appear to be 
 * contravened, rather than trying to order with arbitrary names. 
 *	
 * @author neil
 */
public class TdbAuVolumeDateAlphanumericComparator extends TdbAuAlphanumericComparator {

  private static Logger log = Logger.getLogger("TdbAuVolumeDateAlphanumericComparator");

  @Override
  public int compare(TdbAu au1, TdbAu au2) {
    // First try volume comparison
    String vol1 = KbartTdbAuUtil.findVolume(au1);
    String vol2 = KbartTdbAuUtil.findVolume(au2);
    try {
      if (!"".equals(vol1) && !"".equals(vol2)) {
	return KbartTdbAuUtil.compareIntStrings(vol1, vol2);
      }
    } catch (NumberFormatException e) {
      //log.warning("Could not compare volumes numerically ("+vol1+", "+vol2+")");
      // fall through to an alphanumeric comparison
    }

    // Second try year comparison
    String yr1 = KbartTdbAuUtil.getFirstYear(KbartTdbAuUtil.findYear(au1));
    String yr2 = KbartTdbAuUtil.getFirstYear(KbartTdbAuUtil.findYear(au2));
    try {
      if (!"".equals(yr1) && !"".equals(yr2)) {
	return KbartTdbAuUtil.compareStringYears(yr1, yr2);
      }
    } catch (NumberFormatException e) {
      log.warning("Could not compare years ("+yr1+", "+yr2+")");
      // fall through to an alphanumeric comparison
    }
   
    // TODO Try alphanumeric volume comparison before resorting to names?
    
    // Default alphanumeric comparison on name
    return super.compare(au1, au2);
  }
    
}

