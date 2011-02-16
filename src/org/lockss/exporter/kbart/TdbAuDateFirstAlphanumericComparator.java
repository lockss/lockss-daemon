package org.lockss.exporter.kbart;

import org.lockss.config.TdbAu;
import org.lockss.util.Logger;

/**
 * Sort a set of <code>TdbAu</code> objects from a single title into a pseudo-chronological 
 * order based on dates and names. There is no guaranteed way to order chronologically due 
 * to the incompleteness of date metadata included in AUs at the time of writing; however 
 * the naming convention of AUs provides a best-guess alternative method.
 * <p>
 * First an attempt is made to order by any available date information. By convention of TDB AU 
 * naming, ascending alphanumerical order should also be chronological order within a title, so 
 * this is tried second. Note that in general the AU names are identical to the title name plus 
 * a volume or year identifier. However if a title's title/name changes during its run, multiple 
 * titles can appear in the AU records. This is probably wrong as the new title should get a 
 * different ISSN and have a separate title record.
 * <p>
 * Perhaps this comparator should throw an exception if the conventions appear to be 
 * contravened, rather than trying to order with arbitrary names. 
 *	
 * @author neil
 */
public class TdbAuDateFirstAlphanumericComparator extends TdbAuAlphanumericComparator {

  private static Logger log = Logger.getLogger("TdbAuDateFirstAlphanumericComparator");

  @Override
  public int compare(TdbAu au1, TdbAu au2) {
    // First try year comparison
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
    return super.compare(au1, au2);
  }
}

