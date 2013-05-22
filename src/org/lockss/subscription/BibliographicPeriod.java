/*
 * $Id: BibliographicPeriod.java,v 1.1 2013-05-22 23:40:20 fergaloy-sf Exp $
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
public class BibliographicPeriod extends BibliographicItemAdapter {
  public static final BibliographicPeriod ALL_TIME_PERIOD =
      new BibliographicPeriod(true);

  private static final Logger log = Logger.getLogger(BibliographicPeriod.class);

  private static final String EDGES_SEPARATOR = "-";
  private static final String FAR_PAST_EDGE = "0";
  private static final String FAR_FUTURE_EDGE = "9999";
  private static final String RANGES_SEPARATOR = ",";

  private String startEdge;
  private String endEdge;
  private boolean allTime = false;
  private boolean allPast = false;
  private boolean allFuture = false;

  public BibliographicPeriod(String period) {
    // Remove any blank spaces.
    String cleanPeriod = removeSpaces(period);

    // Check whether it is the full time period.
    if (cleanPeriod.equals(EDGES_SEPARATOR)) {
      // Yes: Mark it accordingly.
      setAllTime(true);
    } else {
      // No: Get the location of the edges separator.
      int separatorLocation = cleanPeriod.indexOf(EDGES_SEPARATOR);

      // Check whether the period is composed of a single edge.
      if (separatorLocation == -1) {
	// Yes: Use the single edge for both edges.
	startEdge = cleanPeriod;
	endEdge = cleanPeriod;
      } else {
	// No: Populate both edges from the passed period.
	startEdge = cleanPeriod.substring(0, separatorLocation);
	endEdge =
	    cleanPeriod.substring(separatorLocation + EDGES_SEPARATOR.length());
	allPast = StringUtil.isNullString(startEdge);
	allFuture = StringUtil.isNullString(endEdge);
      }

      // Populate base class members.
      setYear(extractEdgeYear(startEdge) + EDGES_SEPARATOR
	  + extractEdgeYear(endEdge));
      setVolume(extractEdgeVolume(startEdge) + EDGES_SEPARATOR
	  + extractEdgeVolume(endEdge));
      setIssue(extractEdgeIssue(startEdge) + EDGES_SEPARATOR
	  + extractEdgeIssue(endEdge));
    }
  }

  private static String removeSpaces(String text) {
    if (StringUtil.isNullString(text)) {
      return "";
    }

    return StringUtil.replaceString(text, " ", "");
  }

  public BibliographicPeriod(String startEdge, String endEdge) {
    if ((startEdge != null && startEdge.indexOf(EDGES_SEPARATOR) != -1)
	|| (endEdge != null && endEdge.indexOf(EDGES_SEPARATOR) != -1)) {
      throw new IllegalArgumentException("Edges cannot contain '"
	+ EDGES_SEPARATOR + "'");
    }

    this.startEdge = removeSpaces(startEdge);
    this.endEdge = removeSpaces(endEdge);
    allPast = StringUtil.isNullString(this.startEdge);
    allFuture = StringUtil.isNullString(this.endEdge);

    // Populate base class members.
    setYear(extractEdgeYear(this.startEdge) + EDGES_SEPARATOR
	+ extractEdgeYear(this.endEdge));
    setVolume(extractEdgeVolume(this.startEdge) + EDGES_SEPARATOR
	+ extractEdgeVolume(this.endEdge));
    setIssue(extractEdgeIssue(this.startEdge) + EDGES_SEPARATOR
	+ extractEdgeIssue(this.endEdge));
  }

  public BibliographicPeriod(String startYear, String startVolume,
      String startIssue, String endYear, String endVolume, String endIssue) {
    if ((startYear != null && startYear.indexOf(EDGES_SEPARATOR) != -1)
	|| (startVolume != null && startVolume.indexOf(EDGES_SEPARATOR) != -1)
	|| (startIssue != null && startIssue.indexOf(EDGES_SEPARATOR) != -1)
	|| (endYear != null && endYear.indexOf(EDGES_SEPARATOR) != -1)
	|| (endVolume != null && endVolume.indexOf(EDGES_SEPARATOR) != -1)
	|| (endIssue != null && endIssue.indexOf(EDGES_SEPARATOR) != -1)) {
      throw new IllegalArgumentException(
	  "Years, volumes and issues cannot contain '" + EDGES_SEPARATOR + "'");
    }

    // Get the start of the period.
    startEdge =	displayablePeriodEdge(removeSpaces(startYear),
	removeSpaces(startVolume), removeSpaces(startIssue));

    // Get the end of the period.
    endEdge = displayablePeriodEdge(removeSpaces(endYear),
	removeSpaces(endVolume), removeSpaces(endIssue));

    allPast = StringUtil.isNullString(startEdge);
    allFuture = StringUtil.isNullString(endEdge);

    // Populate base class members.
    setYear(extractEdgeYear(startEdge) + EDGES_SEPARATOR
	+ extractEdgeYear(endEdge));
    setVolume(extractEdgeVolume(startEdge) + EDGES_SEPARATOR
	+ extractEdgeVolume(endEdge));
    setIssue(extractEdgeIssue(startEdge) + EDGES_SEPARATOR
	+ extractEdgeIssue(endEdge));
  }

  private BibliographicPeriod(boolean allTime) {
    setAllTime(allTime);
  }

  public static Collection<BibliographicPeriod> createCollection(String text) {
    if (StringUtil.isNullString(text)) {
      return Collections.singleton(new BibliographicPeriod(removeSpaces(text)));
    }

    return createCollection(BibliographicUtil
	.splitRangeSet(removeSpaces(text)));
  }

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

    StringBuilder buffer = null;

    for (BibliographicPeriod range : ranges) {
      if (buffer == null) {
	buffer = new StringBuilder(range.toDisplayableString());
      } else {
	buffer.append(RANGES_SEPARATOR).append(range.toDisplayableString());
      }
    }

    return buffer.toString();
  }

  /**
   * Provides the properties of an edge in a form suitable for display.
   * 
   * @param year
   *          A String with the period edge year, if any.
   * @param volume
   *          A String with the period edge volume, if any.
   * @param issue
   *          A String with the period edge issue, if any.
   * @return a String with the edge properties in a form suitable for display.
   */
  public static String displayablePeriodEdge(String year, String volume,
      String issue) {
    if ((year != null && year.indexOf(EDGES_SEPARATOR) != -1)
	|| (volume != null && volume.indexOf(EDGES_SEPARATOR) != -1)
	|| (issue != null && issue.indexOf(EDGES_SEPARATOR) != -1)) {
      throw new IllegalArgumentException(
	  "Years, volumes and issues cannot contain '" + EDGES_SEPARATOR + "'");
    }

    StringBuilder builder = new StringBuilder();

    // Add the year if it exists.
    if (!StringUtil.isNullString(year)) {
      builder.append(year);
    }

    // Check whether the volume exists.
    if (!StringUtil.isNullString(volume)) {
      // Yes: Add it.
      builder.append("(").append(volume).append(")");

      // Check whether the issue exists.
      if (!StringUtil.isNullString(issue)) {
	// Yes: Add it.
	builder.append("(").append(issue).append(")");
      }
    } else {
      // No: Check whether the issue exists.
      if (!StringUtil.isNullString(issue)) {
	// Yes: Add it preceded by an empty volume.
	builder.append("()(").append(issue).append(")");
      }
    }

    return builder.toString();
  }

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

    List<BibliographicPeriod> sortedPeriods =
	new ArrayList<BibliographicPeriod>(periods);

    // Sort the periods.
    BibliographicUtil.sortByVolumeYear(sortedPeriods);

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

  private boolean immediatelyFollows(BibliographicPeriod other) {
    try {
      return SORT_FIELD.YEAR.areAppropriatelyConsecutive(other, this)
	  && SORT_FIELD.VOLUME.areAppropriatelyConsecutive(other, this);
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private void coalesce(BibliographicPeriod other) {
    final String DEBUG_HEADER = "coalesce(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "other = " + other);

    // Check whether this is a wider period than the other.
    if (isAllTime() || other == null || other.isEmpty()) {
      // Yes.
      return;
    }

    // No: Extend the period to the future.
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "endEdge = " + endEdge
	+ "=>" + other.getEndEdge());
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

  static List<BibliographicPeriod> createList(String text) {
    if (StringUtil.isNullString(text)) {
      return Collections
	  .singletonList(new BibliographicPeriod(removeSpaces(text)));
    }

    return createList(BibliographicUtil.splitRangeSet(removeSpaces(text)));
  }

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
   * Provides the year of a period edge, if any.
   * 
   * @param edge
   *          A String with the edge.
   * @return a String with the period edge year, if any.
   */
  static String extractEdgeYear(String edge) {
    if (StringUtil.isNullString(edge)) {
      return null;
    }

    if (edge.indexOf(EDGES_SEPARATOR) != -1) {
      throw new IllegalArgumentException("Edges cannot contain '"
	  + EDGES_SEPARATOR + "'");
    }

    int volumeStart = edge.indexOf("(");

    if (volumeStart == -1) {
      return edge;
    } else if (volumeStart == 0) {
      return null;
    }

    return edge.substring(0, volumeStart);
  }

  /**
   * Provides the volume of a period edge, if any.
   * 
   * @param edge
   *          A String with the edge.
   * @return a String with the period edge volume, if any.
   */
  static String extractEdgeVolume(String edge) {
    if (StringUtil.isNullString(edge)) {
      return null;
    }

    if (edge.indexOf(EDGES_SEPARATOR) != -1) {
      throw new IllegalArgumentException("Edges cannot contain '"
	  + EDGES_SEPARATOR + "'");
    }

    int volumeStart = edge.indexOf("(");
    if (volumeStart == -1) {
      return null;
    }

    return edge.substring(volumeStart + 1, edge.indexOf(")"));
  }

  /**
   * Provides the issue of a period edge, if any.
   * 
   * @param edge
   *          A String with the edge.
   * @return a String with the period edge issue, if any.
   */
  static String extractEdgeIssue(String edge) {
    if (StringUtil.isNullString(edge)) {
      return null;
    }

    if (edge.indexOf(EDGES_SEPARATOR) != -1) {
      throw new IllegalArgumentException("Edges cannot contain '"
	  + EDGES_SEPARATOR + "'");
    }

    int issueStart = edge.indexOf(")(");
    if (issueStart == -1) {
      return null;
    }

    return edge.substring(issueStart + 2, edge.lastIndexOf(")"));
  }

  /**
   * Provides a period edge in a format matching another edge.
   * 
   * @param edge
   *          A String with the edge to be formatted.
   * @param isStart
   *          A boolean with an indication of whether the edge to be formatted
   *          is the start edge of the period.
   * @param matchingEdge
   *          A String with the edge to be used to match the reformatted edge.
   * @return a String with the reformatted period edge.
   */
  static String matchEdgeToEdge(String edge, boolean isStart,
      String matchingEdge) {
    final String DEBUG_HEADER = "matchEdgeToEdge(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "edge = " + edge);
      log.debug2(DEBUG_HEADER + "isStart = " + isStart);
      log.debug2(DEBUG_HEADER + "matchingEdge = " + matchingEdge);
    }

    if ((edge != null && edge.indexOf(EDGES_SEPARATOR) != -1)
	|| (matchingEdge != null
	    && matchingEdge.indexOf(EDGES_SEPARATOR) != -1)) {
      throw new IllegalArgumentException("Edges cannot contain '"
	  + EDGES_SEPARATOR + "'");
    }

    String cleanEdge = removeSpaces(edge);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "cleanEdge = " + cleanEdge);

    String cleanMatchingEdge = removeSpaces(matchingEdge);
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "cleanMatchingEdge = " + cleanMatchingEdge);

    // Extract the properties of the edge to be reformatted.
    String edgeYear = extractEdgeYear(cleanEdge);
    if (log.isDebug3()) log.debug3("edgeYear = '" + edgeYear + "'.");

    String edgeVolume = extractEdgeVolume(cleanEdge);
    if (log.isDebug3()) log.debug3("edgeVolume = '" + edgeVolume + "'.");

    String edgeIssue = extractEdgeIssue(cleanEdge);
    if (log.isDebug3()) log.debug3("edgeIssue = '" + edgeIssue + "'.");

    // Check whether the matching edge has a volume but the edge to be
    // reformatted does not.
    if (!StringUtil.isNullString(extractEdgeVolume(cleanMatchingEdge))
	&& StringUtil.isNullString(edgeVolume)) {
      // Yes: Assign a volume to the edge to be reformatted.
      edgeVolume = isStart ? FAR_PAST_EDGE : FAR_FUTURE_EDGE;
    }

    // Check whether the matching edge has an issue but the edge does not.
    if (!StringUtil.isNullString(extractEdgeIssue(cleanMatchingEdge))
	&& StringUtil.isNullString(edgeIssue)) {
      // Yes: Assign an issue to the edge to be reformatted.
      edgeIssue = isStart ? FAR_PAST_EDGE : FAR_FUTURE_EDGE;
    }

    // Rebuild the edge to be reformatted.
    String matchedEdge = displayablePeriodEdge(edgeYear, edgeVolume, edgeIssue);
    if (log.isDebug2()) log.debug2("matchedEdge = '" + matchedEdge + "'.");
    return matchedEdge;
  }

  /**
   * Provides a copy of a passed range reformatted to match a passed edge.
   * 
   * @param range
   *          A String with the range to be formatted.
   * @param matchingEdge
   *          A String with the edge to be used to match the edges of the
   *          reformatted range.
   * @return a String with the reformatted range.
   */
  static BibliographicPeriod matchRangeEdgesToEdge(BibliographicPeriod range,
      String matchingEdge) {
    final String DEBUG_HEADER = "matchRangeEdgesToEdge(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "range = " + range);
      log.debug2(DEBUG_HEADER + "matchingEdge = " + matchingEdge);
    }

    if (matchingEdge != null && matchingEdge.indexOf(EDGES_SEPARATOR) != -1) {
      throw new IllegalArgumentException("Edges cannot contain '"
	  + EDGES_SEPARATOR + "'");
    }

    String cleanMatchingEdge = removeSpaces(matchingEdge);
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "cleanMatchingEdge = " + cleanMatchingEdge);

    // Create a matched range by matching its edges.
    BibliographicPeriod matchedRange =
	new BibliographicPeriod(matchEdgeToEdge(range.getStartEdge(), true,
	    cleanMatchingEdge),
	    matchEdgeToEdge(range.getEndEdge(), false, cleanMatchingEdge));
    if (log.isDebug2()) log.debug2("matchedRange = " + matchedRange);
    return matchedRange;
  }

  static void extendFuture(List<BibliographicPeriod> periods) {
    final String DEBUG_HEADER = "extendFuture(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "periods.size() = " + periods.size());

    if (periods == null || periods.size() < 1) {
      return;
    }

    BibliographicPeriod lastPeriod = periods.get(periods.size() - 1);
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "lastPeriod = " + lastPeriod);

    if (lastPeriod.isAllTime() || lastPeriod.isAllFuture()) {
      return;
    }

    lastPeriod.setEndEdge("");
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "lastPeriod = " + lastPeriod);
  }

  public String getStartEdge() {
    return startEdge;
  }

  public void setStartEdge(String startEdge) {
    if (startEdge != null && startEdge.indexOf(EDGES_SEPARATOR) != -1) {
      throw new IllegalArgumentException("Edges cannot contain '"
	  + EDGES_SEPARATOR + "'");
    }

    this.startEdge = removeSpaces(startEdge);
    allPast = StringUtil.isNullString(this.startEdge);

    // Populate base class members.
    setYear(extractEdgeYear(this.startEdge) + EDGES_SEPARATOR
	+ extractEdgeYear(this.endEdge));
    setVolume(extractEdgeVolume(this.startEdge) + EDGES_SEPARATOR
	+ extractEdgeVolume(this.endEdge));
    setIssue(extractEdgeIssue(this.startEdge) + EDGES_SEPARATOR
	+ extractEdgeIssue(this.endEdge));
  }

  public String getEndEdge() {
    return endEdge;
  }

  public void setEndEdge(String endEdge) {
    if (endEdge != null && endEdge.indexOf(EDGES_SEPARATOR) != -1) {
      throw new IllegalArgumentException("Edges cannot contain '"
	  + EDGES_SEPARATOR + "'");
    }

    this.endEdge = removeSpaces(endEdge);
    allFuture = StringUtil.isNullString(this.endEdge);

    // Populate base class members.
    setYear(extractEdgeYear(this.startEdge) + EDGES_SEPARATOR
	+ extractEdgeYear(this.endEdge));
    setVolume(extractEdgeVolume(this.startEdge) + EDGES_SEPARATOR
	+ extractEdgeVolume(this.endEdge));
    setIssue(extractEdgeIssue(this.startEdge) + EDGES_SEPARATOR
	+ extractEdgeIssue(this.endEdge));
  }

  public boolean isAllTime() {
    return allTime;
  }

  public void setAllTime(boolean allTime) {
    this.allTime = allTime;

    if (this.allTime) {
      setStartEdge(null);
      setEndEdge(null);

      // Populate base class members.
      setYear(FAR_PAST_EDGE + EDGES_SEPARATOR + FAR_FUTURE_EDGE);
      setVolume(FAR_PAST_EDGE + EDGES_SEPARATOR + FAR_FUTURE_EDGE);
      setIssue(FAR_PAST_EDGE + EDGES_SEPARATOR + FAR_FUTURE_EDGE);
    }
  }

  public boolean isAllPast() {
    return allPast;
  }

  public void setAllPast(boolean allPast) {
    this.allPast = allPast;
  }

  public boolean isAllFuture() {
    return allFuture;
  }

  public void setAllFuture(boolean allFuture) {
    this.allFuture = allFuture;
  }

  public boolean isEmpty() {
    return StringUtil.isNullString(toDisplayableString());
  }

  public String toDisplayableString() {
    // Check whether the period covers all time.
    if (allTime) {
      // Yes.
      return EDGES_SEPARATOR;
      // No: Check whether the period has two distinct edges.
    } else if (!endEdge.equals(startEdge)) {
      // Yes: Return the formatted period.
      return (startEdge == null ? "" : startEdge) + EDGES_SEPARATOR
	  + (endEdge == null ? "" : endEdge);
    }

    // No: Return one of the edges as the period.
    return endEdge == null ? "" : endEdge;
  }

  public String toCanonicalString() {
    if (isEmpty()) {
      return "";
    }

    return (startEdge == null ? "" : startEdge) + EDGES_SEPARATOR
	+ (endEdge == null ? "" : endEdge);
  }

  @Override
  public String toString() {
    return "BibliographicPeriod [startEdge=" + startEdge + ", endEdge="
	+ endEdge + ", allTime=" + allTime + "]";
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
      normalized.setStartEdge(FAR_PAST_EDGE);
      normalized.setEndEdge(FAR_FUTURE_EDGE);
      normalized.setAllTime(false);
    } else {
      // No: Check whether it is not empty.
      if (!normalized.isEmpty()) {
	// Yes.
	String originalStartEdge = normalized.getStartEdge();
	String originalEndEdge = normalized.getEndEdge();

	// Check whether there is no start edge.
	if (originalStartEdge.length() == 0) {
	  // Yes: Synthesize the start edge matching the format of the end edge.
	  normalized.setStartEdge(matchEdgeToEdge(FAR_PAST_EDGE, true,
	      originalEndEdge));
	  // No: Check whether there is no end edge.
	} else if (originalEndEdge.length() == 0) {
	  // Yes: Synthesize the end edge matching the format of the start edge.
	  normalized.setEndEdge(matchEdgeToEdge(FAR_FUTURE_EDGE, false,
	      originalStartEdge));
	} else {
	  // No: Rebuild the period matching the format of each edge against the
	  // other edge.
	  normalized.setStartEdge(matchEdgeToEdge(originalStartEdge, true,
	      originalEndEdge));
	  normalized.setEndEdge(matchEdgeToEdge(originalEndEdge, false,
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
    String startEdge = normalizedPeriod.getStartEdge();
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "startEdge = " + startEdge);

    // Check whether the edge is covered by one of the ranges.
    if (BibliographicUtil.coverageIncludes(ranges, startEdge)) {
      // Yes: No need for any more work.
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result is true.");
      return true;
    }

    // No: Get the period end edge.
    String endEdge = normalizedPeriod.getEndEdge();
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "endEdge = " + endEdge);

    // Check whether the edge is covered by one of the ranges.
    if (BibliographicUtil.coverageIncludes(ranges, endEdge)) {
      // Yes: No need for any more work.
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result is true.");
      return true;
    }

    // Turn around the situation and repeat the checks with the period and the
    // ranges roles reversed.
    Collection<String> splitRanges = BibliographicUtil.splitRangeSet(ranges);
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "splitRanges.size() = " + splitRanges.size());

    // Loop through the ranges.
    for (String range : splitRanges) {
      // Get the start edge of the range.
      startEdge = BibliographicUtil.getRangeSetStart(range);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "startEdge = " + startEdge);

      // Check whether the edge is covered by the period.
      if (BibliographicUtil
	  .coverageIncludes(normalizedPeriod.toDisplayableString(),
	      startEdge)) {
	// Yes: No need for any more work.
	if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result is true.");
	return true;
      }

      // No: Get the end edge of the range.
      endEdge = BibliographicUtil.getRangeSetEnd(range);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "endEdge = " + endEdge);

      // Check whether the edge is covered by the period.
      if (BibliographicUtil
	  .coverageIncludes(normalizedPeriod.toDisplayableString(), endEdge)) {
	// Yes: No need for any more work.
	if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result is true.");
	return true;
      }
    }

    // At this point, all the checks have been made and no intersection has been
    // found.
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result is false.");
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
    String matchingEdge = period.getStartEdge();
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
