/*
 * $Id: BibliographicPeriod.java,v 1.2 2013-07-18 16:55:06 fergaloy-sf Exp $
 */

/*

 Copyright (c) 2013 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.subscription;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.lockss.exporter.biblio.BibliographicItemAdapter;
import org.lockss.exporter.biblio.BibliographicUtil;
import org.lockss.exporter.biblio.BibliographicOrderScorer.SORT_FIELD;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

/**
 * Representation of a period in terms of years, volumes and issues.
 * 
 * @author Fernando Garcia-Loygorri
 */
public class BibliographicPeriod extends BibliographicItemAdapter
    implements Comparable<BibliographicPeriod> {

  public static final String EDGES_SEPARATOR = "-";

  public static final BibliographicPeriod ALL_TIME_PERIOD =
      new BibliographicPeriod(EDGES_SEPARATOR);

  private static final Logger log = Logger.getLogger(BibliographicPeriod.class);
  private static final String RANGES_SEPARATOR = ",";

  private BibliographicPeriodEdge startEdge;
  private BibliographicPeriodEdge endEdge;

  /**
   * Constructor with the text representation of a period.
   * 
   * @param period A String with the text representation of the period.
   */
  public BibliographicPeriod(String period) {
    // Remove any blank spaces.
    String cleanPeriod = removeSpaces(period);

    if (StringUtil.isNullString(cleanPeriod)) {
      return;
    }

    // Check whether it is the full time period.
    if (cleanPeriod.equals(EDGES_SEPARATOR)) {
      // Yes: Set it up.
      startEdge = BibliographicPeriodEdge.INFINITY_EDGE;
      endEdge = BibliographicPeriodEdge.INFINITY_EDGE;

      // Populate base class members.
      setYear(EDGES_SEPARATOR);
      setVolume(EDGES_SEPARATOR);
      setIssue(EDGES_SEPARATOR);
    } else {
      // No: Get the location of the edges separator.
      int separatorLocation = findEdgesSeparatorLocation(cleanPeriod);

      // Check whether the period is composed of a single edge.
      if (separatorLocation == -1) {
	// Yes: Use the single edge for both edges.
	startEdge = new BibliographicPeriodEdge(cleanPeriod);
	endEdge = new BibliographicPeriodEdge(cleanPeriod);
      } else {
	// No: Populate both edges from the passed period.
	startEdge = new BibliographicPeriodEdge(cleanPeriod.substring(0,
	    separatorLocation));
	endEdge = new BibliographicPeriodEdge(cleanPeriod
	    .substring(separatorLocation + EDGES_SEPARATOR.length()));
      }

      // Populate base class members.
      setYear(startEdge.getDisplayableYear() + EDGES_SEPARATOR
	  + endEdge.getDisplayableYear());
      setVolume(startEdge.getDisplayableVolume() + EDGES_SEPARATOR
	  + endEdge.getDisplayableVolume());
      setIssue(startEdge.getDisplayableIssue() + EDGES_SEPARATOR
	  + endEdge.getDisplayableIssue());
    }
  }

  /**
   * Removes spaces from a text string anywhere and reports an empty text string
   * as null.
   * 
   * @param text
   *          A String with the original text.
   * @return a String with the text without spaces anywhere, or null if empty.
   */
  private static String removeSpaces(String text) {
    if (StringUtil.isNullString(text)) {
      return "";
    }

    return StringUtil.replaceString(text, " ", "");
  }

  /**
   * Locates the separator between the period edges.
   * 
   * @param period
   *          A String with the period.
   * @return an int with the location of the separator between the period edges
   *         or -1 if the separator cannot be found.
   */
  private int findEdgesSeparatorLocation(String period) {
    boolean insideParentheses = false;
    boolean insideQuotes = false;

    // Loop through all the characters in the text.
    for (int location = 0; location < period.length(); location++) {
      // Get the next character.
      String character = period.substring(location,location+1);

      // Check whether this character is not inside a pair of parentheses and
      // not inside a pair of quotes.
      if (!insideParentheses && !insideQuotes) {
	// Yes: Check whether it is the separator character.
	if (EDGES_SEPARATOR.equals(character)) {
	  // Yes: Report it.
	  return location;
	}

	// No: Update the parentheses and quotes indicators.
	insideParentheses = "(".equals(character);
	insideQuotes = "\"".equals(character);
      } else {
	// No: Check whether this character is inside a pair of parentheses.
	if (insideParentheses) {
	  // Yes: Check for the end of the parentheses pair.
	  insideParentheses = !")".equals(character);

	  // Handle the quotes indicator.
	  if (insideQuotes) {
	    insideQuotes = !"\"".equals(character);
	  } else {
	    insideQuotes = "\"".equals(character);
	  }
	} else {
	  // No: It is inside a pair of quotes.
	  insideQuotes = !"\"".equals(character);
	}
      }
    }

    return -1;
  }

  /**
   * Constructor with the two period edges.
   * 
   * @param startEdge A BibliographicPeriodEdge with the period start edge.
   * @param endEdge A BibliographicPeriodEdge with the period end edge.
   */
  public BibliographicPeriod(BibliographicPeriodEdge startEdge,
      BibliographicPeriodEdge endEdge) {
    if (startEdge == null && endEdge == null) {
      return;
    } else if ((startEdge == null && endEdge != null) ||
	(startEdge != null && endEdge == null)) {
      throw new IllegalArgumentException("Cannot create a BibliographicPeriod "
	+ "with one null edge");
    }

    this.startEdge = startEdge;
    this.endEdge = endEdge;

    // Populate base class members.
    setYear(startEdge.getDisplayableYear() + EDGES_SEPARATOR
	  + endEdge.getDisplayableYear());
    setVolume(startEdge.getDisplayableVolume() + EDGES_SEPARATOR
	  + endEdge.getDisplayableVolume());
    setIssue(startEdge.getDisplayableIssue() + EDGES_SEPARATOR
	  + endEdge.getDisplayableIssue());
  }

  /**
   * Constructor with the text representations of the two period edges.
   * 
   * @param startEdgeText
   *          A String with the text representations of the period start edge.
   * @param endEdgeText
   *          A String with the text representations of the period end edge.
   */
  public BibliographicPeriod(String startEdgeText, String endEdgeText) {
    if (StringUtil.isNullString(removeSpaces(startEdgeText))
	&& StringUtil.isNullString(removeSpaces(endEdgeText))) {
      return;
    }

    startEdge = new BibliographicPeriodEdge(startEdgeText);
    endEdge = new BibliographicPeriodEdge(endEdgeText);

    // Populate base class members.
    setYear(startEdge.getDisplayableYear() + EDGES_SEPARATOR
	  + endEdge.getDisplayableYear());
    setVolume(startEdge.getDisplayableVolume() + EDGES_SEPARATOR
	  + endEdge.getDisplayableVolume());
    setIssue(startEdge.getDisplayableIssue() + EDGES_SEPARATOR
	  + endEdge.getDisplayableIssue());
  }

  /**
   * Constructor with the text representations of the year, volume and issue of
   * the two period edges.
   * 
   * @param startYear
   *          A String with the text representations of the period start edge
   *          year.
   * @param startVolume
   *          A String with the text representations of the period start edge
   *          volume.
   * @param startIssue
   *          A String with the text representations of the period start edge
   *          issue.
   * @param endYear
   *          A String with the text representations of the period end edge
   *          year.
   * @param endVolume
   *          A String with the text representations of the period end edge
   *          volume.
   * @param endIssue
   *          A String with the text representations of the period end edge
   *          issue.
   */
  public BibliographicPeriod(String startYear, String startVolume,
      String startIssue, String endYear, String endVolume, String endIssue) {
    if (StringUtil.isNullString(removeSpaces(startYear))
	&& StringUtil.isNullString(removeSpaces(startVolume))
	&& StringUtil.isNullString(removeSpaces(startIssue))
	&& StringUtil.isNullString(removeSpaces(endYear))
	&& StringUtil.isNullString(removeSpaces(endVolume))
	&& StringUtil.isNullString(removeSpaces(endIssue))) {
      return;
    }

    startEdge = new BibliographicPeriodEdge(startYear, startVolume, startIssue);
    endEdge = new BibliographicPeriodEdge(endYear, endVolume, endIssue);

    // Populate base class members.
    setYear(startEdge.getDisplayableYear() + EDGES_SEPARATOR
	  + endEdge.getDisplayableYear());
    setVolume(startEdge.getDisplayableVolume() + EDGES_SEPARATOR
	  + endEdge.getDisplayableVolume());
    setIssue(startEdge.getDisplayableIssue() + EDGES_SEPARATOR
	  + endEdge.getDisplayableIssue());
  }

  /**
   * Provides a collection of periods from a text representation.
   *
   * @param text A String with the text representation of the periods.
   * @return a Collection<BibliographicPeriod> with the periods.
   */
  public static Collection<BibliographicPeriod> createCollection(String text) {
    if (StringUtil.isNullString(text)) {
      return Collections.singleton(new BibliographicPeriod(removeSpaces(text)));
    }

    return createCollection(BibliographicUtil
	.splitRangeSet(removeSpaces(text)));
  }

  /**
   * Provides a collection of periods from a collection of period text
   * representations.
   * 
   * @param textPeriods
   *          A Collection<String> with the text representations of the periods.
   * @return a Collection<BibliographicPeriod> with the periods.
   */
  private static Collection<BibliographicPeriod> createCollection(
      Collection<String> textPeriods) {
    final String DEBUG_HEADER = "createCollection(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "textPeriods.size() = " + textPeriods.size());

    Collection<BibliographicPeriod> periods = new HashSet<BibliographicPeriod>(
	textPeriods.size());

    for (String textPeriod : textPeriods) {
      periods.add(new BibliographicPeriod(textPeriod));
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "periods.size() = " + periods.size());
    return periods;
  }

  /**
   * Provides a text string specifying a collection of ranges.
   *
   * @param ranges A Collection<BibliographicPeriod> with the ranges.
   * @return a String with passed collection of ranges.
   */
  public static String rangesAsString(Collection<BibliographicPeriod> ranges) {
    if (ranges == null || ranges.size() < 1) {
      return null;
    }

    StringBuilder result = null;

    // Sort the ranges.
    List<BibliographicPeriod> rangesAsList = sort(ranges);

    for (BibliographicPeriod range : rangesAsList) {
      if (result == null) {
	result = new StringBuilder(range.toDisplayableString());
      } else {
	result.append(RANGES_SEPARATOR).append(range.toDisplayableString());
      }
    }

    return result.toString();
  }

  /**
   * Sorts a collection of ranges.
   *
   * @param ranges A Collection<BibliographicPeriod> with the ranges.
   * @return a String with passed collection of ranges.
   */
  public static List<BibliographicPeriod> sort(
      Collection<BibliographicPeriod> ranges) {

    if (ranges == null || ranges.size() < 1) {
      return new ArrayList<BibliographicPeriod>();
    }

    // Sort the ranges.
    List<BibliographicPeriod> rangesAsList =
	new ArrayList<BibliographicPeriod>(ranges);

    Collections.sort(rangesAsList);

    return rangesAsList;
  }

  /**
   * Coalesces a collection of periods.
   * 
   * @param periods
   *          A Collection<BibliographicPeriod> with the periods to be
   *          coalesced.
   * @return a List<BibliographicPeriod> with the coalesced periods.
   */
  public static List<BibliographicPeriod> coalesce(
      Collection<BibliographicPeriod> periods) {
    final String DEBUG_HEADER = "coalesce(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "periods = " + periods);

    List<BibliographicPeriod> coalesced;

    if (periods == null || periods.size() < 1) {
      coalesced = new ArrayList<BibliographicPeriod>();
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "coalesced = " + coalesced);
      return coalesced;
    }

    // Sort the periods.
    List<BibliographicPeriod> sortedPeriods = BibliographicPeriod.sort(periods);

    int sortedPeriodCount = sortedPeriods.size();
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "sortedPeriodCount = " + sortedPeriodCount);

    if (sortedPeriodCount < 2) {
      if (log.isDebug2())
	log.debug2(DEBUG_HEADER + "coalesced = " + sortedPeriods);
      return sortedPeriods;
    }

    coalesced = new ArrayList<BibliographicPeriod>(sortedPeriodCount);

    BibliographicPeriod previous = sortedPeriods.get(0);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "previous = " + previous);

    for (int i = 1; i < sortedPeriodCount; i++) {
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "i = " + i);

      BibliographicPeriod current = sortedPeriods.get(i);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "current = " + current);

      if (current.intersects(previous)
	  || current.immediatelyFollows(previous)) {
	previous.coalesce(current);
      } else {
	coalesced.add(previous);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "coalesced = " + coalesced);

	previous = current;
      }

      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "previous = " + previous);
    }

    coalesced.add(previous);

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "coalesced = " + coalesced);
    return coalesced;
  }

  /**
   * Provides an indication of whether this period follows immediately another.
   * 
   * @param other
   *          A BibliographicPeriod with the candidate period to immediately
   *          precede this one.
   * @return a boolean with <code>true</code> if this period follows immediately
   *         the other period, <code>false</code> otherwise.
   */
  private boolean immediatelyFollows(BibliographicPeriod other) {
    final String DEBUG_HEADER = "immediatelyFollows(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "this = " + this);
      log.debug2(DEBUG_HEADER + "other = " + other);
    }

    boolean result = false;

    String previousYear = other.getEndEdge().getYear();
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "previousYear = " + previousYear);
    String nextYear = getStartEdge().getYear();
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "nextYear = " + nextYear);

    String previousVolume = other.getEndEdge().getVolume();
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "previousVolume = " + previousVolume);
    String nextVolume = getStartEdge().getVolume();
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "nextVolume = " + nextVolume);

    try {
      if (nextYear != null && previousYear != null
	  && !nextYear.equals(previousYear)) {
	result = SORT_FIELD.YEAR.areConsecutive(previousYear, nextYear);
	if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);

	if (result && nextVolume != null && previousVolume != null) {
	  result = SORT_FIELD.VOLUME.areConsecutive(previousVolume, nextVolume);
	  if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
	}
      } else if (nextVolume != null && previousVolume != null
	  && !nextVolume.equals(previousVolume)) {
	result = SORT_FIELD.VOLUME.areConsecutive(previousVolume, nextVolume);
	if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
      }
    } catch (NumberFormatException e) {
    }

    return result;
  }

  /**
   * Coalesces another period into this one, if appropriate.
   * 
   * @param other
   *          A BibliographicPeriod with the candidate period to be coalesced
   *          into this one.
   */
  private void coalesce(BibliographicPeriod other) {
    final String DEBUG_HEADER = "coalesce(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "other = " + other);

    // Check whether this is a wider period than the other.
    if (isAllFuture() || other == null || other.isEmpty()) {
      // Yes.
      return;
    }

    // No: Extend the period to the future.
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "endEdge = "
	+ endEdge.toDisplayableString() + "=>"
	+ other.getEndEdge().toDisplayableString());
    setEndEdge(other.getEndEdge());

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides an indication of whether this period intersects a passed range.
   * 
   * @param range
   *          A BibliographicPeriod with the range against which to check the
   *          period.
   * @return a boolean with <code>true</code> if this period intersects the
   *         range, <code>false</code> otherwise.
   */
  boolean intersects(BibliographicPeriod range) {
    final String DEBUG_HEADER = "intersects(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "range = " + range);

    // Check whether there is no range to intersect.
    if (range == null || range.isEmpty()) {
      // Yes: The period cannot intersect the range.
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = false");
      return false;
    }

    return intersects(Collections.singleton(range));
  }

  /**
   * Provides a list of periods from a text representation.
   *
   * @param text A String with the text representation of the periods.
   * @return a List<BibliographicPeriod> with the periods.
   */
  static List<BibliographicPeriod> createList(String text) {
    if (StringUtil.isNullString(text)) {
      return Collections
	  .singletonList(new BibliographicPeriod(removeSpaces(text)));
    }

    return createList(BibliographicUtil.splitRangeSet(removeSpaces(text)));
  }

  /**
   * Provides a list of periods from a collection of period text
   * representations.
   * 
   * @param textPeriods
   *          A Collection<String> with the text representations of the periods.
   * @return a List<BibliographicPeriod> with the periods.
   */
  private static List<BibliographicPeriod> createList(
      Collection<String> textPeriods) {
    final String DEBUG_HEADER = "createList(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "textPeriods.size() = " + textPeriods.size());

    List<BibliographicPeriod> periods = new ArrayList<BibliographicPeriod>(
	textPeriods.size());

    for (String textPeriod : textPeriods) {
      periods.add(new BibliographicPeriod(textPeriod));
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "periods.size() = " + periods.size());
    return periods;
  }

  /**
   * Provides a copy of a passed range reformatted to match a passed edge.
   * 
   * @param range
   *          A BibliographicPeriod with the range to be formatted.
   * @param matchingEdge
   *          A BibliographicPeriodEdge with the edge to be used to match the
   *          edges of the reformatted range.
   * @return a BibliographicPeriod with the reformatted range.
   */
  static BibliographicPeriod matchRangeEdgesToEdge(BibliographicPeriod range,
      BibliographicPeriodEdge matchingEdge) {
    final String DEBUG_HEADER = "matchRangeEdgesToEdge(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "range = " + range);
      log.debug2(DEBUG_HEADER + "matchingEdge = " + matchingEdge);
    }

    BibliographicPeriodEdge startEdge = range.getStartEdge();
    BibliographicPeriodEdge endEdge = range.getEndEdge();
    BibliographicPeriod matchedRange = null;

    if (startEdge == null && endEdge == null) {
      matchedRange = range;
    } else if (startEdge == null) {
      // Create a matched range by matching the end edge.
      matchedRange = new BibliographicPeriod(null,
  	  endEdge.matchEdgeToEdge(false, matchingEdge));
    } else if (endEdge == null) {
      // Create a matched range by matching the start edge.
      matchedRange = new BibliographicPeriod(startEdge.matchEdgeToEdge(true,
	    matchingEdge), null);
    } else {
    // Create a matched range by matching its edges.
      matchedRange = new BibliographicPeriod(startEdge.matchEdgeToEdge(true,
	    matchingEdge), endEdge.matchEdgeToEdge(false, matchingEdge));
    }

    if (log.isDebug2()) log.debug2("matchedRange = " + matchedRange);
    return matchedRange;
  }

  /**
   * Extends into the far future a lis of periods.
   * 
   * @param periods A List<BibliographicPeriod> with the periods to be extended.
   */
  static void extendFuture(List<BibliographicPeriod> periods) {
    final String DEBUG_HEADER = "extendFuture(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "periods.size() = " + periods.size());

    if (periods == null || periods.size() < 1) {
      return;
    }

    BibliographicPeriod lastPeriod = periods.get(periods.size() - 1);
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "lastPeriod = " + lastPeriod);

    if (lastPeriod.isAllTime() || lastPeriod.isAllFuture()
	|| lastPeriod.getStartEdge() == null) {
      return;
    }

    lastPeriod.setEndEdge(BibliographicPeriodEdge.INFINITY_EDGE);
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "lastPeriod = " + lastPeriod);
  }

  public BibliographicPeriodEdge getStartEdge() {
    return startEdge;
  }

  public void setStartEdge(BibliographicPeriodEdge startEdge) {
    this.startEdge = startEdge;
  }

  public BibliographicPeriodEdge getEndEdge() {
    return endEdge;
  }

  public void setEndEdge(BibliographicPeriodEdge endEdge) {
    this.endEdge = endEdge;
  }

  public boolean isAllTime() {
    return !isEmpty() && startEdge.isInfinity() && endEdge.isInfinity();
  }

  public boolean isAllPast() {
    return !isEmpty() && startEdge.isInfinity();
  }

  public boolean isAllFuture() {
    return !isEmpty() && endEdge.isInfinity();
  }

  public boolean isEmpty() {
    return startEdge == null && endEdge == null;
  }

  public String toDisplayableString() {
    // Check whether it is empty.
    if (isEmpty()) {
      // Yes.
      return "";
    // No: Check whether the period covers all time.
    } else if (isAllTime()) {
      // Yes.
      return EDGES_SEPARATOR;
      // No: Check whether the period has two distinct edges.
    } else if (!endEdge.equals(startEdge)) {
      // Yes: Return the formatted period.
      return startEdge.toDisplayableString() + EDGES_SEPARATOR
	  + endEdge.toDisplayableString();
    }

    // No: Return one of the edges as the period.
    return endEdge.toDisplayableString();
  }

  public String toCanonicalString() {
    if (isEmpty()) {
      return "";
    }

    return startEdge.toDisplayableString() + EDGES_SEPARATOR
	+ endEdge.toDisplayableString();
  }

  @Override
  public String toString() {
    return "BibliographicPeriod [startEdge=" + startEdge + ", endEdge="
	+ endEdge + "]";
  }

  @Override
  public int compareTo(BibliographicPeriod other) {
    if (getStartEdge() != null && other != null) {
      return getStartEdge().compareTo(other.getStartEdge());
    } else if (getEndEdge() != null && other != null) {
      return getStartEdge().compareTo(other.getStartEdge());
    }

    return 0;
  }

  /**
   * Provides a normalized version of this object, with both edges explicitly
   * declared and specified in a common format.
   * 
   * @return a BibliographicPeriod with the normalized version of the object.
   */
  public BibliographicPeriod normalize() {
    final String DEBUG_HEADER = "normalize(): ";
    BibliographicPeriod normalized =
	new BibliographicPeriod(toDisplayableString());

    // Check whether it is the full time period.
    if (normalized.isAllTime()) {
      // Yes: Synthesize the two edges.
      normalized.setStartEdge(BibliographicPeriodEdge.FAR_PAST_EDGE);
      normalized.setEndEdge(BibliographicPeriodEdge.FAR_FUTURE_EDGE);
    } else {
      // No: Check whether it is not empty.
      if (!normalized.isEmpty()) {
	// Yes.
	BibliographicPeriodEdge originalStartEdge = normalized.getStartEdge();
	BibliographicPeriodEdge originalEndEdge = normalized.getEndEdge();

	// Check whether there is no start edge.
	if (originalStartEdge.isInfinity()) {
	  // Yes: Synthesize the start edge matching the format of the end edge.
	  normalized.setStartEdge(BibliographicPeriodEdge.FAR_PAST_EDGE
	      .matchEdgeToEdge(true, originalEndEdge));
	  // No: Check whether there is no end edge.
	} else if (originalEndEdge.isInfinity()) {
	  // Yes: Synthesize the end edge matching the format of the start edge.
	  normalized.setEndEdge(BibliographicPeriodEdge.FAR_FUTURE_EDGE
	      .matchEdgeToEdge(false, originalStartEdge));
	} else {
	  // No: Rebuild the period matching the format of each edge against the
	  // other edge.
	  normalized.setStartEdge(originalStartEdge.matchEdgeToEdge(true,
	      originalEndEdge));
	  normalized.setEndEdge(originalEndEdge.matchEdgeToEdge(false,
	      normalized.getStartEdge()));
	}
      }
    }

    // Reformat the period with a common format.
    normalized = new BibliographicPeriod(BibliographicUtil.
	normaliseIdentifier(normalized.toDisplayableString()));
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "normalized = '" + normalized + "'.");
    return normalized;
  }

  /**
   * Provides an indication of whether this period intersects any of the passed
   * ranges.
   * 
   * @param ranges
   *          A Collection<BibliographicPeriod> with the ranges against which to
   *          check the period.
   * @return a boolean with <code>true</code> if this period intersects any of
   *         the ranges, <code>false</code> otherwise.
   */
  boolean intersects(Collection<BibliographicPeriod> ranges) {
    final String DEBUG_HEADER = "intersects(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "ranges = " + ranges);

    // Check whether there are no ranges to intersect.
    if (ranges == null || ranges.size() < 1) {
      // Yes: The period cannot intersect the ranges.
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = false");
      return false;
    }

    Collection<BibliographicPeriod> normalizedRanges =
	new HashSet<BibliographicPeriod>();
    
    // Loop through all the passed ranges.
    for (BibliographicPeriod range : ranges) {
      // Normalize it and add it to the collection of normalized ranges.
      normalizedRanges.add(range.normalize());
    }

    // Check whether the period intersects any of the normalized ranges.
    boolean result = intersectsNormalized(normalizedRanges);
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Provides an indication of whether this period intersects any of the passed
   * normalized ranges.
   * 
   * @param ranges
   *          A Collection<BibliographicPeriod> with the normalized ranges
   *          against which to check the period.
   * @return a boolean with <code>true</code> if this period intersects any of
   *         the ranges, <code>false</code> otherwise.
   */
  private boolean intersectsNormalized(
      Collection<BibliographicPeriod> normalizedRanges) {
    final String DEBUG_HEADER = "intersectsNormalized(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "normalizedRanges.size() = "
	  + normalizedRanges.size());

    BibliographicPeriod normalizedPeriod = normalize();
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "normalizedPeriod = " + normalizedPeriod);

    // Reformat the ranges so that their edges match the format of the period.
    String ranges = rangesAsString(matchRangeEdgesToPeriod(normalizedRanges,
	normalizedPeriod));
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "ranges = " + ranges);

    // Get the period start edge.
    BibliographicPeriodEdge startEdge = normalizedPeriod.getStartEdge();
    String startEdgeText = "";

    if (startEdge != null) {
      startEdgeText = startEdge.toDisplayableString();
    }

    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "startEdgeText = " + startEdgeText);

    // Check whether the edge is covered by one of the ranges.
    if (BibliographicUtil.coverageIncludes(ranges, startEdgeText)) {
      // Yes: No need for any more work.
      if (log.isDebug2())
	log.debug2(DEBUG_HEADER + "coverageIncludes result is true.");
      return true;
    } else {
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "coverageIncludes(" + ranges
	  + "," + startEdgeText + ") returns false.");
    }

    // No: Get the period end edge.
    BibliographicPeriodEdge endEdge = normalizedPeriod.getEndEdge();
    String endEdgeText = "";

    // Check whether it is different than the start edge.
    if ((startEdge == null && endEdge != null)
	|| (startEdge != null && !startEdge.equals(endEdge))) {
      
      // Yes: Check it separately.
      if (endEdge != null) {
	endEdgeText = endEdge.toDisplayableString();
      }

      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "endEdgeText = " + endEdgeText);

      // Check whether the edge is covered by one of the ranges.
      if (BibliographicUtil.coverageIncludes(ranges, endEdgeText)) {
	// Yes: No need for any more work.
	if (log.isDebug2())
	  log.debug2(DEBUG_HEADER + "coverageIncludes result is true.");
	return true;
      } else {
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "coverageIncludes("
	    + ranges + "," + endEdgeText + ") returns false.");
      }
    }

    // Turn around the situation and repeat the checks with the period and the
    // ranges roles reversed.
    Collection<String> splitRanges = BibliographicUtil.splitRangeSet(ranges);
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "splitRanges.size() = " + splitRanges.size());

    // Loop through the ranges.
    for (String range : splitRanges) {
      // Get the start edge of the range.
      startEdgeText = BibliographicUtil.getRangeSetStart(range);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "startEdgeText = " + startEdgeText);

      // Check whether the edge is covered by the period.
      if (BibliographicUtil
	  .coverageIncludes(normalizedPeriod.toDisplayableString(),
	      startEdgeText)) {
	// Yes: No need for any more work.
	if (log.isDebug2())
	  log.debug2(DEBUG_HEADER + "coverageIncludes result is true.");
	return true;
      } else {
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "coverageIncludes("
	    + normalizedPeriod.toDisplayableString() + "," + startEdgeText
	    + ") returns false.");
      }

      // No: Get the end edge of the range.
      endEdgeText = BibliographicUtil.getRangeSetEnd(range);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "endEdge = " + endEdge);

      // Check whether it is different than the start edge.
      if ((startEdgeText == null && endEdgeText != null)
	  || (startEdgeText != null && !startEdgeText.equals(endEdgeText))) {

	// Yes: Check whether the edge is covered by the period.
	if (BibliographicUtil
	    .coverageIncludes(normalizedPeriod.toDisplayableString(),
		endEdgeText)) {
	  // Yes: No need for any more work.
	  if (log.isDebug2())
	    log.debug2(DEBUG_HEADER + "coverageIncludes result is true.");
	  return true;
	} else {
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER + "coverageIncludes("
	      + normalizedPeriod.toDisplayableString() + "," + endEdgeText
	      + ") returns false.");
	}
      }
    }

    // At this point, all the checks have been made and no intersection has been
    // found.
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Ultimate result is false.");
    return false;
  }

  /**
   * Provides a copy of a collection of passed ranges reformatted so that their
   * edges match the format of those of a passed period.
   * 
   * @param ranges
   *          A Collection<String> with the ranges to be reformatted.
   * @param period
   *          A String with the period with the edges to be used to match the
   *          edges of the reformatted ranges.
   * 
   * @return a Collection<String> with the reformatted ranges.
   */
  private Collection<BibliographicPeriod> matchRangeEdgesToPeriod(
      Collection<BibliographicPeriod> ranges, BibliographicPeriod period) {
    final String DEBUG_HEADER = "matchRangeEdgesToPeriod(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "ranges.size() = " + ranges.size());
      log.debug2(DEBUG_HEADER + "period = " + period);
    }

    // Use the first period edge as the matching edge used to reformat the
    // ranges.
    BibliographicPeriodEdge matchingEdge = period.getStartEdge();
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "matchingEdge = " + matchingEdge);

    Collection<BibliographicPeriod> matchedRanges =
	new HashSet<BibliographicPeriod>(ranges.size());

    // Loop through all the ranges.
    for (BibliographicPeriod range : ranges) {
      // Match the format of the range edges to that of the matching edge.
      matchedRanges.add(matchRangeEdgesToEdge(range, matchingEdge));
    }

    if (log.isDebug2())
      log.debug2("matchedRanges.size() = " + matchedRanges.size());
    return matchedRanges;
  }

  /**
   * Provides the subset of ranges from a passed set of ranges that intersect
   * this period.
   * 
   * @param ranges
   *          A Collection<BibliographicPeriod> with the ranges to be checked.
   * @return a Collection<String> with the subset of ranges that intersect this
   *         period.
   */
  public Collection<BibliographicPeriod> intersection(
      Collection<BibliographicPeriod> ranges) {
    final String DEBUG_HEADER = "intersection(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "ranges.size() = " + ranges.size());

    Collection<BibliographicPeriod> result = new HashSet<BibliographicPeriod>();

    // Loop through all the candidate ranges.
    for (BibliographicPeriod range : ranges) {
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "range = " + range);

      // Check whether this range covers everything.
      if (range.isAllTime()) {
	// Yes: Add it to the result set.
	result.add(range);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Matched");
	// No: Check whether this range intersects this period.
      } else if (intersects(range)) {
	// Yes: Add it to the result set.
	result.add(range);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Matched");
      }
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "result.size() = " + result.size());
    return result;
  }
}
