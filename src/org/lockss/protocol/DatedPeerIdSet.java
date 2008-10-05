/**
 * 
 */
package org.lockss.protocol;

import java.io.IOException;

/**
 * @author edwardsb
 *
 * This method adds a date (implemented as a long) to the set.
 */
public interface DatedPeerIdSet extends PersistentPeerIdSet {
  /**
   * Notice that the "date" being set is actually a long.  The code internally uses long for dates.
   * You can convert between dates and longs with:
   * 
   *   Date d -> long
   *   d.getTime()
   *   
   *   long l -> Date
   *   Date newDate = new Date();
   *   newDate.setTime(l);
   *   
   * @param l
   */
  public void setDate(long l) throws IOException;
  
  /**
   * 
   * @return long
   */
  public long getDate() throws IOException;
}
