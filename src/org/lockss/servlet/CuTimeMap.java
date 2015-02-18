/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.servlet;

import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * A memento wrapper class for a time map of cached urls to of a single
 * resource. <p/>
 * first: the first memento(least recent version) of the cached url.<p/>
 * last: the last memento(most recent version) of the cached url<p/>
 * next: the version directly after the selected version<p/>
 * prev: the version directly before the selected version<p/>
 * selected: the version of current interest. If there is one version,
 * then the first, last and selected are the same.<p/>
 * This class also provides utilities for parsing, generating and determining
 * Memento time stamps.
 */

public class CuTimeMap {
  protected static final Logger log = Logger.getLogger("CuTimeMap");
  static final SimpleDateFormat formatter =
      new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

  static {
    formatter.setTimeZone(TimeZone.getTimeZone("GMT")); // section 2.1.1.1
  }

  /**
   * If the constructor that takes a Collection<CachedUrl> was used, this will
   * store all the arrays containing all past versions of each CachedUrl in the
   * input to the constructor. Otherwise, it will be empty.
   */
  protected Collection<CachedUrl[]> m_versionArrays = Collections.emptySet();
  /** The first or least recent version. */
  private CuMemento m_first;
  /** The last or most recent version. */
  private CuMemento m_last;
  /** The version of focus (the memento nearest to time requested). */
  private CuMemento m_selected;
  /** The version directly after the selected memento, may be null */
  private CuMemento m_next;
  /** The version directly before selected memento, may be null.*/
  private CuMemento m_prev;


// constructors

  /**
   * Given a collection of CachedUrls, constructs a CuTimeMap pointing to the
   * first and last version out of all the CachedUrls in the collection and
   * their other versions. Used to reply to a TimeGateService request.
   * <p/>
   * Releases all CUs accessed.
   * <p/>
   * Precondition: at least one version of a CU in cachedUrls must have a time
   * stamp (either fetch time or last-modified time)
   *
   * @param cachedUrls a collection of CachedUrls
   */
  public CuTimeMap(Collection<CachedUrl> cachedUrls) {
    /*
     * cachedUrls only contains the latest CU for a URL from each AU. Get all
     * versions of each CU.
     */
    m_versionArrays = getVersionArrays(cachedUrls);

    // Two best-so-far candidates and their time stamps.
    CuMemento bestLast = null;
    CuMemento bestFirst = null;

    for (CachedUrl[] cuArray : m_versionArrays) {
      try {

        CuMemento firstCu = newerStampedCu(cuArray, cuArray.length - 1);
        CuMemento lastCu = olderStampedCu(cuArray, 0);

        if (bestFirst == null || firstCu.before(bestFirst)) {
          bestFirst = firstCu;
        }
        if (bestLast == null || lastCu.after(bestLast)) {
          bestLast = lastCu;
        }
      }
      catch (IllegalArgumentException ex) { /* just continue */ }
    }
    // If precondition was met, then bestFirst and bestLast will be non-null.
    if (bestLast == null || bestFirst == null) {
      throw new IllegalArgumentException(
          "Precondition failed: at least one CU provided must have a time " +
          "stamp.");
    }

    m_first = bestFirst;
    m_last = bestLast;
  }

  /**
   * Given a collection of CachedUrls, constructs a CuTimeMap or TimeMapService
   * containing the CachedUrls that should be pointed to for navigation
   * purposes as specified by section 2.2.1.4. Each element of the collection
   * must be from its own AU and contain content for the same URL.
   * <p/>
   * Releases any CUs accessed as a result of constructing this object.
   * <p/>
   * <ul> <li>curMemento will point to the CachedUrl in one of the
   * versionArrays that was the most recent as of acceptDatetime, if
   * acceptDatetime is a POSIX time. Otherwise, if acceptDatetime is -1,
   * the call is treated as a request for the latest version.</li>
   * <p/>
   * <li>firstMemento and lastMemento will point to the earliest and latest
   * versions across all members of cachedUrls.</li>
   * <p/>
   * <li>prevMemento will point to the version directly before thisMemento if it
   * exists, otherwise null.</li>
   * <p/>
   * <li>nextMemento will point to the version directly after thisMemento if it
   * exists, otherwise null.</li> </ul>
   * <p/>
   * prevMemento and nextMemento will be null iff thisMemento is the oldest or
   * newest version known to the server, respectively.
   *
   * @param cachedUrls a collection CachedUrls, each from its own AU, containing
   * content for the same URL
   * @param preferredTime the time to center the pointers around; must be
   * either -1 or a POSIX time
   */
  public CuTimeMap(Collection<CachedUrl> cachedUrls,
                   long preferredTime) throws ParseException {
    this(cachedUrls);

    if (preferredTime < -1) {
      throw new IllegalArgumentException(
          "Precondition failure: acceptDatetime must be a positive " +
          "millisecond-since-epoch time or -1 but is " + preferredTime);
    }
    // Three best-so-far candidates and their time stamps.
    m_versionArrays = getVersionArrays(cachedUrls);

    // Two best-so-far candidates and their time stamps.
    CuMemento bestSelect = null;
    CuMemento bestPrev = null;
    CuMemento bestNext = null;
    CuMemento prevSelect = null;

    for (CachedUrl[] cuArray : m_versionArrays) {
      CuTimeMap curMap = new CuTimeMap(cuArray, preferredTime);
      if (bestSelect == null) {
  /*
   * Since this is the first candidate we've considered, the contents of
	 * current map is the best.
	 */
        bestPrev = curMap.m_prev; // might still be null
        bestSelect = curMap.m_selected;  // should not be null
        bestNext = curMap.m_next; // might still be null
      }
      else {
        prevSelect = bestSelect;
        // find the best select, these should never be null
        bestSelect = nearest(preferredTime, curMap.m_selected, bestSelect);
        // bestNext  could be either curMap.m_next or curMap.m_selected
        long best_time = bestSelect.time.getTime();
        if (best_time > preferredTime) {
          // all values may be over the time, so we need to check current,
          // prev selections relative to the current time
          bestNext = nearestButMore(best_time, bestNext, curMap.m_selected);
          bestNext = nearestButMore(best_time, bestNext, prevSelect);
          bestPrev = nearestButLess(best_time, bestPrev, curMap.m_prev);
          bestPrev = nearestButLess(best_time, bestPrev, prevSelect);
        }
        else {
          bestNext = nearestButMore(best_time, bestNext, curMap.m_next);
          bestNext = nearestButMore(best_time, bestNext, prevSelect);
          bestPrev = nearestButLess(best_time, bestPrev, curMap.m_selected);
          bestPrev = nearestButLess(best_time, bestPrev, prevSelect);
        }
      }
    }
    m_prev = bestPrev;  // might be null
    m_selected = bestSelect;  // shouldn't be null
    m_next = bestNext;  // might be null
  }

  /**
   * Used to construct a CuTimeMap based on only the CachedUrls in the given
   * array so from a single au. If acceptDatetime is non-negative,
   * then thisMemento is the version current as of acceptDatetime,
   * and the other pointers are also only drawn from the array. If
   * acceptDatetime is -1, then it is assumed to be the time of the
   * most recent version.
   * <p/>
   * Releases any CUs that need releasing as a result of this call.
   * <p/>
   * Precondition: at least one element of cuArray has a time stamp
   * <p/>
   * Precondition: acceptDatetime is a positive milliseconds-since-epoch or -1
   *
   * @param cuArray a non-empty array of CachedUrls, at least one having a time
   * stamp
   * @param acceptDatetime the number of milliseconds since epoch that
   * thisMemento should be current as of
   */
  protected CuTimeMap(CachedUrl[] cuArray, long acceptDatetime) {
    if (acceptDatetime < -1) {
      throw new IllegalArgumentException("acceptDatetime < -1");
    }
     /*
     * Since cuArray is sorted from latest to earliest, firstTime should be the
     * rightmost timestamped element, and lastTime should be the leftmost.
     */
    m_first = newerStampedCu(cuArray, cuArray.length - 1);  /* oldest */
    m_last = olderStampedCu(cuArray, 0);                    /* newest */

    /*
     * If the user doesn't provide an "Accept-Datetime" header, acceptDatetime
     * will be -1. In this case, we are supposed to return the most recent
     * Memento. (section 3.2.2.6) Or if accept datetime is after the most
     * recent item we return the most recent Memento.
     */
    if (acceptDatetime == -1 ||
        m_last.time.getTime() < acceptDatetime) {
      m_selected = m_last;
      try {
        m_prev = olderStampedCu(cuArray, m_last.index + 1);
      }
      catch (IllegalArgumentException ex) {
 	    /* selected is already the latest, next  should be null.*/
        m_prev = null;
      }
      m_next = null;
    }
    else if (m_first.time.getTime() > acceptDatetime) {
    /*
     * If the user requests a date earlier than the first Memento.
     * According to 3.2.2.1, we must provide the browser with the earliest
     * version. This means this CuTimeMap should end up with firstMemento ==
     * thisMemento and prevMemento == null.
     */
      m_selected = m_first;
      m_prev = null;
      try {
        m_next = newerStampedCu(cuArray, m_selected.index - 1);
      }
      catch (IllegalArgumentException ex) {
 	    /* selected is already the latest, next  should be null.*/
        m_next = null;
      }
    }
    else {
      /*
       * "Accept-Datetime" was provided and is acceptable.
       *
       * Linear-search for the element whose date is nearest to acceptDatetime
       * but earlier than it.
       */
      CuMemento bestSelect = null; // best estimate of thisMemento so far
      CuMemento bestPrev = null; // best estimate of prevMemento so far
      int cursor = cuArray.length - 1;
      while (cursor >= 0) {
        try {
          CuMemento candidate = newerStampedCu(cuArray, cursor);
          cursor = candidate.index - 1;

          if (candidate.time.getTime() <= acceptDatetime) {
            bestPrev = bestSelect;
            bestSelect = candidate;
          }
          else {
	    /*
	     * We've reached a candidate newer than acceptDatetime, so the current
	     * bestThis and bestPrev are correct.
	     */
            break;
          }
        }
        catch (IllegalArgumentException ex) {
	  /*
	   * We've reached the earliest element of cuArray that has a time
	   * stamp, so the current bestThis and bestPrev are the best we can do,
	   * if they exist. And if they don't exist, that means cuArray lacks a
	   * time-stamped element, so the method's precondition fails, and
	   * bestThis will be null. Handled below.
	   */
          break;
        }
      }
      // Check for precondition.
      if (bestSelect == null) {
        throw new IllegalArgumentException(
            "At least one element of cuArray must have a time stamp.");
      }
      m_selected = bestSelect;
      m_prev = bestPrev;
      try {
        m_next = newerStampedCu(cuArray, m_selected.index - 1);
      }
      catch (IllegalArgumentException ex) {
 	      /* selected is already the latest, next should be null.*/
        m_next = null;
      }
    }
  }

  // the getters for our history elements
  public CuMemento first() {
    return m_first;
  }

  public CuMemento last() {
    return m_last;
  }

  public CuMemento selected() {
    return m_selected;
  }

  public CuMemento next() {
    return m_next;
  }

  public CuMemento prev() {
    return m_prev;
  }

  /**
   * Returns a collection containing, for each CachedUrl in the input
   * collection, an array of all versions of that CachedUrl, sorted so that the
   * latest version is first.
   *
   * @param cachedUrls the CachedURLs to find all versions of
   * @return a Collection of arrays containing all versions of each CachedUrl
   */
  static protected Collection<CachedUrl[]> getVersionArrays(
      Collection<CachedUrl> cachedUrls) {
    Collection<CachedUrl[]> result = new ArrayList<CachedUrl[]>();
    for (CachedUrl cu : cachedUrls) {
      result.add(cu.getCuVersions());
    }
    return result;
  }

  /**
   * Returns the rightmost element with a time stamp whose index is equal to or
   * less than cursor. If cuArray is sorted from newest to oldest, this is the
   * next-newest, inclusive of the one at cursor.
   * <p/>
   * Releases all CUs accessed.
   *
   * @param cuArray a non-empty array of CachedUrls
   * @param cursor the starting point in the array
   * @return the previous (inclusive) time-stamp-having element
   * @throws IllegalArgumentException if there is no time-stamped element at or
   * left of the cursor
   */
  protected CuMemento newerStampedCu(CachedUrl[] cuArray, int cursor)
      throws IllegalArgumentException {
    return nearbyStampedCu(cuArray, cursor, false);
  }

  /**
   * Returns the leftmost time-stamp-having element whose index is equal to or
   * greater than cursor. If cuArray is sorted from newest to oldest, this is
   * the next-oldest, inclusive of the one at the cursor.
   * <p/>
   * Releases all CUs accessed.
   *
   * @param cuArray a non-empty array of CachedUrls
   * @param cursor the starting point in the array
   * @return the next (inclusive) time-stamp-having element
   * @throws IllegalArgumentException if there is no time-stamped element at or
   * right of the cursor
   */
  protected CuMemento olderStampedCu(CachedUrl[] cuArray, int cursor)
      throws IllegalArgumentException {
    return nearbyStampedCu(cuArray, cursor, true);
  }

  // utility functions
  protected CuMemento nearest(long target, CuMemento mem1,
                                    CuMemento mem2)
  {
    if(mem1 == null) return mem2;
    if(mem2 == null) return mem1;
    // neither is null
    long mem1_comp = mem1.time.getTime()-target;
    long mem2_comp = mem2.time.getTime()-target;

    // if both are less - return the greater
    if(mem1_comp <= 0 && mem2_comp <= 0)
      return (mem1_comp > mem2_comp) ? mem1 : mem2;
    // if both are over return whichever is less
    return (mem1_comp > mem2_comp) ? mem2 : mem1;
  }

  protected CuMemento nearestButLess(long target, CuMemento mem1,
                                     CuMemento mem2)
  {
    if(mem1 == null && mem2 == null) return null;
    if(mem1 == null) return (mem2.time.getTime() < target) ? mem2 : null;
    if(mem2 == null) return (mem1.time.getTime() < target) ? mem1 : null;

    long mem1_comp = mem1.time.getTime()-target;
    long mem2_comp = mem2.time.getTime()-target;

    if(mem1_comp < 0 && mem2_comp >= 0) return mem1;
    if(mem2_comp < 0 && mem1_comp >= 0) return mem2;
    // if both are under - return the later
    if(mem1_comp < 0 && mem2_comp < 0)
      return (mem1_comp > mem2_comp) ? mem1 : mem2;
    // if both are over or equal
    return null;

  }

  protected CuMemento nearestButMore(long target, CuMemento mem1,
                                     CuMemento mem2)
  {
    if(mem1 == null && mem2 == null) return null;
    if(mem1 == null) return (mem2.time.getTime() > target) ? mem2 : null;
    if(mem2 == null) return (mem1.time.getTime() > target) ? mem1 : null;

    long mem1_comp = mem1.time.getTime()-target;
    long mem2_comp = mem2.time.getTime()-target;

    if(mem1_comp <= 0 && mem2_comp > 0) return mem2;
    if(mem2_comp <= 0 && mem1_comp > 0) return mem1;
    // if both are over - return the earlier
    if(mem1_comp > 0 && mem2_comp > 0)
      return mem1_comp > mem2_comp ? mem2 : mem1;
    return null;
  }

  /**
   * If !isRight, returns the previous time-stamped element in the array,
   * ordered by indexes (not time stamps). Or if isRight, returns the next.
   * Here, "previous" and "next" refer to the array's ordering, not the time
   * stamps. The returned element is wrapped in a CuTimeIx. The search for a
   * time-stamped element is inclusive, so the returned element might be the one
   * at cursor.
   * <p/>
   * Releases all CUs accessed.
   *
   * @param cuArray an array of CachedUrls containing at least one time-stamped
   * CachedUrl to the right (if isRight) or left (if !isRight) of cursor,
   * inclusive
   * @param cursor the array index to begin searching from; must be in bounds
   * @return the next (if isRight) or previous (if !isRight) element that has a
   *         time stamp, inclusive of cursor.
   * @throws IllegalArgumentException if there is no time-stamped element to
   * offer
   */
  private CuMemento nearbyStampedCu(CachedUrl[] cuArray, int cursor,
                               boolean isRight)
      throws IllegalArgumentException {

    CuMemento candidate = null;
    int dirInt = isRight ? 1 : -1;

     while(cursor >=0 && cursor < cuArray.length) {
      CachedUrl cu = cuArray[cursor];
      try {
        Date time = TimeServlet.cuTime(cu);
        candidate = new CuMemento(cu, time, cursor);
        AuUtil.safeRelease(cu);
        break;
      }
      catch(IllegalArgumentException iaEx) {
        AuUtil.safeRelease(cu);
        cursor += dirInt; // cuTime found no time stamp, so continue.
      }
    }

    if (candidate != null) {
      return candidate;
    }
    else {
      throw new IllegalArgumentException("no time-stamped element there");
    }
  }

  /**
   * A single memento. A CachedUrl with its time stamp and index in a array of
   * cached urls. Useful for preventing multiple calls to cuTime and
   * AuUtil.safeRelease.
   */
  protected static class CuMemento {
    /** A CachedUrl. */
    protected final CachedUrl cu;
    /** cu's time stamp according to cuTime. */
    protected final Date time;
    /** cu's index in some array. */
    protected final int index;

    /**
     * Construct a CuTimeIx with ix = ix, cu = cuArray[ix], time = cuTime(cu).
     * cu must have a time stamp.
     *
     * @param cu the CachedUrl this memento
     * @param time the timestamp for this version of the cu
     * @param index the pos in the array of versions of cu
     */
    public CuMemento(CachedUrl cu, Date time, int index){
      this.cu = cu;
      this.time = time;
      this.index = index;
    }

    public boolean before(final CuMemento memento) {
      return memento != null && time.getTime() < memento.time.getTime();
    }

    public boolean after(final CuMemento memento) {
      return memento != null && time.getTime() > memento.time.getTime();
    }

    public String url() {
      return cu.getUrl();
    }

    public boolean sameAuAndVersion(CuMemento mem)
    {
      CachedUrl cu2 = mem.cu;

      if(cu == null || cu2 == null)
        return false;
      if(!cu.getArchivalUnit().getAuId().equals(cu2.getArchivalUnit().getAuId()))
        return false;
      return cu.getVersion() == cu2.getVersion();
    }
  }
}
