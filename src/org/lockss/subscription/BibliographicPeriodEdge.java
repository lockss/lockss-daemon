/*
 * $Id: BibliographicPeriodEdge.java,v 1.1 2013-07-18 16:51:04 fergaloy-sf Exp $
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

import org.lockss.exporter.biblio.BibliographicOrderScorer.SORT_FIELD;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

/**
 * Representation of the edge of a period in terms of a year, volume and issue.
 * 
 * @author Fernando Garcia-Loygorri
 */
public class BibliographicPeriodEdge
    implements Comparable<BibliographicPeriodEdge> {
  public static final BibliographicPeriodEdge INFINITY_EDGE =
      new BibliographicPeriodEdge();

  private static final String FAR_PAST_EDGE_VALUE = "0";
  private static final String FAR_FUTURE_EDGE_VALUE = "9999";

  public static final BibliographicPeriodEdge FAR_PAST_EDGE =
      new BibliographicPeriodEdge(FAR_PAST_EDGE_VALUE);

  public static final BibliographicPeriodEdge FAR_FUTURE_EDGE =
      new BibliographicPeriodEdge(FAR_FUTURE_EDGE_VALUE);

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
  public BibliographicPeriodEdge(String edge) {
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
  public BibliographicPeriodEdge(String year, String volume, String issue) {
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
      return clean(edge);
    } else if (volumeStart == 0) {
      return null;
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

  public String getYear() {
    return year;
  }
  public void setYear(String year) {
    this.year = year;
  }
  public String getVolume() {
    return volume;
  }
  public void setVolume(String volume) {
    this.volume = volume;
  }
  public String getIssue() {
    return issue;
  }
  public void setIssue(String issue) {
    this.issue = issue;
  }

  public String getDisplayableYear() {
    return year == null ? "" : year;
  }

  public String getDisplayableVolume() {
    return volume == null ? "" : volume;
  }

  public String getDisplayableIssue() {
    return issue == null ? "" : issue;
  }

  public boolean isInfinity() {
    return year == null && volume == null && issue == null;
  }

  /**
   * Provides the properties of the edge in a form suitable for display.
   * 
   * @return a String with the edge properties in a form suitable for display.
   */
  public String toDisplayableString() {
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
   * Provides this period edge in a format matching another edge.
   * 
   * @param edge
   *          A BibliographicPeriod with the edge to be formatted.
   * @param isStart
   *          A boolean with an indication of whether the edge to be formatted
   *          is the start edge of the period.
   * @param matchingEdge
   *          A BibliographicPeriodEdge with the edge to be used to match the
   *          reformatted edge.
   * @return a BibliographicPeriodEdge with the reformatted period edge.
   */
  public BibliographicPeriodEdge matchEdgeToEdge(boolean isStart,
      BibliographicPeriodEdge matchingEdge) {
    final String DEBUG_HEADER = "matchEdgeToEdge(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "isStart = " + isStart);
      log.debug2(DEBUG_HEADER + "matchingEdge = " + matchingEdge);
    }

    // Extract the properties of the edge to be reformatted.
    String edgeYear = getYear();
    if (log.isDebug3()) log.debug3("edgeYear = '" + edgeYear + "'.");

    String edgeVolume = getVolume();
    if (log.isDebug3()) log.debug3("edgeVolume = '" + edgeVolume + "'.");

    String edgeIssue = getIssue();
    if (log.isDebug3()) log.debug3("edgeIssue = '" + edgeIssue + "'.");

    // Check whether the matching edge has a volume but the edge to be
    // reformatted does not.
    if (matchingEdge != null
	&& !StringUtil.isNullString(matchingEdge.getVolume())
	&& StringUtil.isNullString(edgeVolume)) {
      // Yes: Assign a volume to the edge to be reformatted.
      edgeVolume = isStart ? FAR_PAST_EDGE_VALUE : FAR_FUTURE_EDGE_VALUE;
    }

    // Check whether the matching edge has an issue but the edge does not.
    if (matchingEdge != null
	&& !StringUtil.isNullString(matchingEdge.getIssue())
	&& StringUtil.isNullString(edgeIssue)) {
      // Yes: Assign an issue to the edge to be reformatted.
      edgeIssue = isStart ? FAR_PAST_EDGE_VALUE : FAR_FUTURE_EDGE_VALUE;
    }

    // Rebuild the edge to be reformatted.
    BibliographicPeriodEdge matchedEdge =
	new BibliographicPeriodEdge(edgeYear, edgeVolume, edgeIssue);
    if (log.isDebug2()) log.debug2("matchedEdge = "
	+ matchedEdge.toDisplayableString() + ".");
    return matchedEdge;
  }

  /**
   * Provides an indication of whether this period edge follows another.
   * 
   * @param other
   *          A BibliographicPeriodEdge with the candidate period edge to
   *          precede this one.
   * @return a boolean with <code>true</code> if this period edge follows the
   *         other period edge, <code>false</code> otherwise.
   */
  boolean follows(BibliographicPeriodEdge other) {
    final String DEBUG_HEADER = "follows(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "this = " + this);
      log.debug2(DEBUG_HEADER + "other = " + other);
    }

    boolean result = false;

    if (other == null) {
      return result;
    }

    String previousYear = other.getYear();
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "previousYear = " + previousYear);

    String nextYear = getYear();
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "nextYear = " + nextYear);

    String previousVolume = other.getVolume();
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "previousVolume = " + previousVolume);

    String nextVolume = getVolume();
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "nextVolume = " + nextVolume);

    try {
      if (nextYear != null && previousYear != null) {
	if (!nextYear.equals(previousYear)) {
	  result = SORT_FIELD.YEAR.areIncreasing(previousYear, nextYear);
	} else {
	  if (nextVolume != null && previousVolume != null
	      && !nextVolume.equals(previousVolume)) {
	    result =
		SORT_FIELD.VOLUME.areIncreasing(previousVolume, nextVolume);
	  }
	}
      } else if (nextVolume != null && previousVolume != null
	  && !nextVolume.equals(previousVolume)) {
	result = SORT_FIELD.VOLUME.areIncreasing(previousVolume, nextVolume);
      }
    } catch (NumberFormatException e) {
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
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
  public int compareTo(BibliographicPeriodEdge other) {
    final String DEBUG_HEADER = "follows(): ";

    if (follows(other)) {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + this + " follows " + other);
      return 1;
    }

    if (other != null && other.follows(this)) {
      if (log.isDebug2())
	log.debug2(DEBUG_HEADER + this + " precedes " + other);
      return -1;
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + this + " is same as " + other);
    return 0;
  }

  @Override
  public String toString() {
    return "BibliographicPeriodEdge [year=" + year + ", volume=" + volume
	+ ", issue=" + issue + "]";
  }
}
