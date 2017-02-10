/*
 * $Id$
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

import java.util.List;
import org.lockss.util.Logger;
import org.lockss.util.NumberUtil;
import org.lockss.util.StringUtil;

/**
 * Representation of the edge of a period in terms of a year, volume and issue.
 * 
 * @author Fernando Garcia-Loygorri
 */
public class BibliographicPeriodEdge {
  public static final BibliographicPeriodEdge INFINITY_EDGE =
      new BibliographicPeriodEdge();

  private static final Logger log =
      Logger.getLogger(BibliographicPeriodEdge.class);

  private String year = null;
  private String volume = null;
  private String issue = null;

  /**
   * Private default constructor.
   */
  private BibliographicPeriodEdge() {
  }

  /**
   * Constructor with the text representation of an edge.
   * 
   * @param edge A String with the text representation of the edge.
   */
  BibliographicPeriodEdge(String edge) {
    // Extract the year.
    setYear(extractEdgeYear(edge));

    // Extract the volume.
    setVolume(extractEdgeVolume(edge));

    // Extract the issue.
    setIssue(extractEdgeIssue(edge));
  }

  /**
   * Constructor with the year, volume and issue.
   * 
   * @param year A String with the year of the period edge.
   * @param volume A String with the volume of the period edge.
   * @param issue A String with the issue of the period edge.
   */
  BibliographicPeriodEdge(String year, String volume, String issue) {
    // Save the year.
    setYear(clean(year));

    // Save the volume.
    setVolume(clean(volume));

    // Save the issue.
    setIssue(clean(issue));
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

    int volumeStart = edge.indexOf("(");

    if (volumeStart == -1) {
      if (edge.indexOf(")") != -1) {
	throw new IllegalArgumentException("Unbalanced parentheses");
      }

      return clean(edge);
    } else if (volumeStart == 0) {
      return null;
    }

    if (edge.substring(0, volumeStart).indexOf(")") != -1) {
      throw new IllegalArgumentException("Unbalanced parentheses");
    }

    return clean(edge.substring(0, volumeStart));
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

    int volumeStart = edge.indexOf("(");
    if (volumeStart == -1) {
      return null;
    }

    return clean(edge.substring(volumeStart + 1, edge.indexOf(")")));
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

    int issueStart = edge.indexOf(")(");
    if (issueStart == -1) {
      return null;
    }

    return clean(edge.substring(issueStart + 2, edge.lastIndexOf(")")));
  }

  /**
   * Removes spaces from a text string anywhere, removes quotes at the beginning
   * and end -if both exist- and reports an empty text string as null.
   * 
   * @param text
   *          A String with the original text.
   * @return a String with the text without spaces anywhere and quotes at the
   *         beginning and end, or null if empty.
   */
  private static String clean(String text) {
    if (StringUtil.isNullString(text)) {
      return null;
    }

    String result = StringUtil.replaceString(text, " ", "");

    if (StringUtil.isNullString(result)) {
      return null;
    }

    if (result.length() > 1
	&& result.startsWith("\"") && result.endsWith("\"")) {
      result = result.substring(1, result.length() - 1);

      if (StringUtil.isNullString(result)) {
        return null;
      }
    }

    return result;
  }

  String getYear() {
    return year;
  }
  void setYear(String year) {
    this.year = year;
  }
  String getVolume() {
    return volume;
  }
  void setVolume(String volume) {
    this.volume = volume;
  }
  String getIssue() {
    return issue;
  }
  void setIssue(String issue) {
    this.issue = issue;
  }

  String getDisplayableYear() {
    return year == null ? "" : year;
  }

  String getDisplayableVolume() {
    return volume == null ? "" : volume;
  }

  String getDisplayableIssue() {
    return issue == null ? "" : issue;
  }

  /**
   * Provides an indication of whether this period edge extends to infinity.
   * 
   * @return a boolean with <code>true</code> if this period edge extends to
   *         infinity, <code>false</code> otherwise.
   */
  public boolean isInfinity() {
    return year == null && volume == null && issue == null;
  }

  /**
   * Provides the properties of this period edge in a form suitable for display.
   * 
   * @return a String with the edge properties in a form suitable for display.
   */
  String toDisplayableString() {
    if (isInfinity()) {
      return "";
    }

    StringBuilder builder = new StringBuilder();

    // Add the year if it exists.
    if (!StringUtil.isNullString(year)) {
      // Quote it if necessary.
      if (year.contains(BibliographicPeriod.EDGES_SEPARATOR)) {
	builder.append("\"").append(year).append("\"");
      } else {
	builder.append(year);
      }
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

  /**
   * Provides an indication of whether this period edge matches any of the
   * passed ranges.
   * 
   * A numeric year-only edge matches if the its value falls in or between the
   * start and end edges of a range.
   * 
   * A non-numeric year-only edge matches if its value is the same as the year
   * of the start or end edges of a range.
   * 
   * An edge with a volume and/or issue matches if a range has that exact edge
   * as its start and/or end edge.
   * 
   * @param ranges
   *          A Collection<BibliographicPeriod> with the ranges against which to
   *          check the period edge.
   * @return a boolean with <code>true</code> if this period edge matches any of
   *         the ranges, <code>false</code> otherwise.
   */
  boolean matches(List<BibliographicPeriod> ranges) {
    final String DEBUG_HEADER = "matches(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "ranges = " + ranges);

    if (log.isDebug3()) {
      log.debug3(DEBUG_HEADER + "year = " + year);
      log.debug3(DEBUG_HEADER + "volume = " + volume);
      log.debug3(DEBUG_HEADER + "issue = " + issue);
    }

    // Check whether the definition of this period includes a volume and or an
    // issue.
    if (!isFullYear()) {
      // Yes: Loop through all the passed ranges.
      for (BibliographicPeriod range : ranges) {
	// Check whether this edge match any of the ranges edges.
	if (equals(range.getStartEdge()) || equals(range.getEndEdge())) {
	  // Yes: The edge matches one of the ranges.
	  if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result is true.");
	  return true;
	}
      }

      // The edge does not match any of the ranges.
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result is false.");
      return false;
    }

    // No: Check whether the year is not a number.
    if (!NumberUtil.isNumber(year)) {
      // Yes: Loop through all the passed ranges.
      for (BibliographicPeriod range : ranges) {
	// Check whether the years match.
	if (year != null && (year.equals(range.getStartEdge().getYear())
	    || year.equals(range.getEndEdge().getYear()))) {
	  // Yes: The edge matches one of the ranges.
	  if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result is true.");
	  return true;
	}
      }

      // The edge does not match any of the ranges.
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result is false.");
      return false;
    }

    // No: Get the numeric year value.
    int yearAsInt = NumberUtil.parseInt(year);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "yearAsInt = " + yearAsInt);

    // Loop through all the passed ranges.
    for (BibliographicPeriod range : ranges) {
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "range = " + range);

      // Check whether the range does not extends to the far past.
      if (!range.getStartEdge().isInfinity()) {
	// Yes: Get the range start year.
	String rangeYear = range.getStartEdge().getYear();

	// Check whether the range start year is a number.
	if (NumberUtil.isNumber(rangeYear)) {
	  // Yes: Get the the range start year as a number.
	  int rangeYearAsInt = NumberUtil.parseInt(rangeYear);
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "rangeYearAsInt = " + rangeYearAsInt);

	  // Check whether the years match.
	  if (yearAsInt == rangeYearAsInt) {
	    // Yes: The edge matches this range.
	    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result is true.");
	    return true;
	  }

	  // No: Check whether the year of this edge is smaller than the range
	  // start year.
	  if (yearAsInt < rangeYearAsInt) {
	    // Yes: This edge cannot match this range.
	    continue;
	  }
	} else {
	  // No: This edge cannot match this range.
	  continue;
	}
      }

      // Check whether the range extends to the far future.
      if (range.getEndEdge().isInfinity()) {
	// Yes: This edge is smaller than the end of the range and so it matches
	// this range.
	if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result is true.");
	return true;
      }

      // No: Get the range end year.
      String rangeYear = range.getEndEdge().getYear();

      // Check whether the range end year is a number.
      if (NumberUtil.isNumber(rangeYear)) {
	// Yes: Get the the range end year as a number.
	int rangeYearAsInt = NumberUtil.parseInt(rangeYear);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "rangeYearAsInt = " + rangeYearAsInt);

	// Check whether the years match.
	if (yearAsInt == rangeYearAsInt) {
	  // Yes: The edge matches this range.
	  if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result is true.");
	  return true;
	}

	// No: Check whether the year of this edge is smaller than the
	// range end year.
	if (yearAsInt < rangeYearAsInt) {
	  // Yes: The edge matches this range.
	  if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result is true.");
	  return true;
	}
      }
    }

    // The edge does not match any of the ranges.
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result is false.");
    return false;
  }

  /**
   * Provides an indication of whether this period edge specifies a publication
   * full year.
   * 
   * @return a boolean with <code>true</code> if this period edge specifies a
   *         publication full year, <code>false</code> otherwise.
   */
  boolean isFullYear() {
    return (isInfinity()
	|| (!StringUtil.isNullString(year) && StringUtil.isNullString(volume)
	    && StringUtil.isNullString(issue)));
  }

  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return false;
    }

    if (this == other) {
      return true;
    }

    if (!(other instanceof BibliographicPeriodEdge)) {
      return false;
    }

    BibliographicPeriodEdge otherBpe = (BibliographicPeriodEdge) other;

    if (year == null) {
      if (otherBpe.getYear() != null) {
	return false;
      }
    } else {
      if (!year.equals(otherBpe.getYear())) {
	return false;
      }
    }

    if (volume == null) {
      if (otherBpe.getVolume() != null) {
	return false;
      }
    } else {
      if (!volume.equals(otherBpe.getVolume())) {
	return false;
      }
    }

    if (issue == null) {
      if (otherBpe.getIssue() != null) {
	return false;
      }
    } else {
      if (!issue.equals(otherBpe.getIssue())) {
	return false;
      }
    }

    return true;
  }

  @Override
  public String toString() {
    return "BibliographicPeriodEdge [year=" + year + ", volume=" + volume
	+ ", issue=" + issue + "]";
  }
}
