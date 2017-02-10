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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.lockss.exporter.biblio.BibliographicItemAdapter;
import org.lockss.exporter.biblio.BibliographicUtil;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

/**
 * Representation of a period in terms of years, volumes and issues.
 * 
 * @author Fernando Garcia-Loygorri
 */
public class BibliographicPeriod extends BibliographicItemAdapter {

  public static final String EDGES_SEPARATOR = "-";

  public static final BibliographicPeriod ALL_TIME_PERIOD =
      new BibliographicPeriod(EDGES_SEPARATOR);

  private static final Logger log = Logger.getLogger(BibliographicPeriod.class);
  private static final String RANGES_SEPARATOR = ",";
  private static final String NULL_EDGE_ERROR_MESSAGE =
      "Cannot create a BibliographicPeriod with one null edge";

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
   * Constructor with the two period edges as text.
   * 
   * @param startEdge
   *          A String with the period start edge.
   * @param endEdge
   *          A String with the period end edge.
   */
  public BibliographicPeriod(String startEdge, String endEdge) {
    this(new BibliographicPeriodEdge(startEdge),
	new BibliographicPeriodEdge(endEdge));
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
      throw new IllegalArgumentException(NULL_EDGE_ERROR_MESSAGE);
    }

    setStartEdge(startEdge);
    setEndEdge(endEdge);

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
   * Provides a list of periods from a text representation.
   *
   * @param text A String with the text representation of the periods.
   * @return a List<BibliographicPeriod> with the periods.
   */
  public static List<BibliographicPeriod> createList(String text) {
    if (StringUtil.isNullString(removeSpaces(text))) {
      return new ArrayList<BibliographicPeriod>();
    }

    return createList(BibliographicUtil
	.splitRangeSet(removeSpaces(text)));
  }

  /**
   * Provides a list of periods from a collection of period text
   * representations.
   * 
   * @param textPeriods
   *          A List<String> with the text representations of the periods.
   * @return a List<BibliographicPeriod> with the periods.
   */
  private static List<BibliographicPeriod> createList(List<String> textPeriods)
  {
    final String DEBUG_HEADER = "createList(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "textPeriods = " + textPeriods);

    List<BibliographicPeriod> periods =
	new ArrayList<BibliographicPeriod>(textPeriods.size());

    for (String textPeriod : textPeriods) {
      if (textPeriod != null && textPeriod.length() == 0) {
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Ignoring empty period.");
	continue;
      }

      periods.add(new BibliographicPeriod(textPeriod));
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "periods = " + periods);
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

    for (BibliographicPeriod range : ranges) {
      if (result == null) {
	result = new StringBuilder(range.toDisplayableString());
      } else {
	result.append(RANGES_SEPARATOR).append(range.toDisplayableString());
      }
    }

    return result.toString();
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

  /**
   * Provides an indication of whether this period matches any of the passed
   * periods.
   * 
   * @param others
   *          A Collection<BibliographicPeriod> with the periods against which
   *          to check ehether this period matches any of them.
   * @return a boolean with <code>true</code> if this period matches any of the
   *         the other periods, <code>false</code> otherwise.
   */
  public boolean matches(List<BibliographicPeriod> others) {
    final String DEBUG_HEADER = "matches(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "others = " + others);

    // Check whether this is an empty period.
    if (isEmpty()) {
      // Yes: It cannot match any of the other periods.
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = false");
      return false;
    }

    // Check whether this period covers all time.
    if (isAllTime()) {
      // Yes: It matches any other period.
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = true");
      return true;
    }

    // Check whether there are no other periods to match.
    if (others == null || others.size() < 1) {
      // Yes: This period cannot match the others.
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = false");
      return false;
    }

    // Normalize this period.
    BibliographicPeriod normalizedThis = normalize();
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "normalizedThis = " + normalizedThis);

    // Get the start edge of this normalized period.
    BibliographicPeriodEdge normalizedStartEdge = normalizedThis.getStartEdge();
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "normalizedStartEdge = " + normalizedStartEdge);

    // Get the end edge of this normalized period.
    BibliographicPeriodEdge normalizedEndEdge = normalizedThis.getEndEdge();
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "normalizedEndEdge = " + normalizedEndEdge);

    List<BibliographicPeriod> normalizedOthers =
	new ArrayList<BibliographicPeriod>();
    
    // Loop through all the other periods.
    for (BibliographicPeriod other : others) {
      BibliographicPeriod normalizedOther = other.normalize();
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "normalizedOther = " + normalizedOther);

      // Check whether this period shares an edge with the other one. 
      if (normalizedStartEdge.equals(normalizedOther.getStartEdge())
	  || normalizedStartEdge.equals(normalizedOther.getEndEdge())
	  || normalizedEndEdge.equals(normalizedOther.getStartEdge())
	  || normalizedEndEdge.equals(normalizedOther.getEndEdge())) {
	// Yes: This period matches the other period.
	if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = true");
	return true;
      }

      // Normalize it and add it to the collection of normalized other periods.
      normalizedOthers.add(other.normalize());
    }

    // Check whether the definition of this period includes volumes and/or
    // issues for both edges.
    if (!startEdge.isFullYear() && !endEdge.isFullYear()) {
      // Yes: This period cannot match the others.
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = false");
      return false;
    }

    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "normalizedOthers = " + normalizedOthers);

    // Check whether the definition of this period does not include volumes
    // and/or issues.
    if (includesFullYears()) {
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "includesFullYears = true");

      // Check whether any of the edges of this period match any of the other
      // periods.
      if (normalizedStartEdge.matches(normalizedOthers)
	  || normalizedEndEdge.matches(normalizedOthers)) {
	// Yes: This period matches one of the other periods.
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "matches = true");
	if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = true");
	return true;
      }

      // No: Reverse the roles and try again.
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "matches = false");

      List<BibliographicPeriod> normalizedThisAsList =
	  Collections.singletonList(normalizedThis);

      // Loop through all the other periods.
      for (BibliographicPeriod normalizedOther : normalizedOthers) {
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "normalizedOther = " + normalizedOther);

	// Get the start edge of the current other period.
	BibliographicPeriodEdge normalizedOtherEdge =
	    normalizedOther.getStartEdge();
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "normalizedOtherEdge = "
	    + normalizedOtherEdge);

	// Make the other period edge cover only full years.
	normalizedOtherEdge.setVolume(null);
	normalizedOtherEdge.setIssue(null);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER
	    + "normalizedRangeEdge = " + normalizedOtherEdge);
	  
	// Check whether the start edge of the current other period matches
	// this period.
	if (normalizedOtherEdge.matches(normalizedThisAsList)) {
	  // Yes: This period matches one of the other periods.
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER + "reverse = true");
	  if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = true");
	  return true;
	}

	// No: Get the end edge of the current other period.
	normalizedOtherEdge = normalizedOther.getEndEdge();
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "normalizedOtherEdge = "
	    + normalizedOtherEdge);

	// Make the other period edge cover only full years.
	normalizedOtherEdge.setVolume(null);
	normalizedOtherEdge.setIssue(null);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER
	    + "normalizedRangeEdge = " + normalizedOtherEdge);
	  
	// Check whether the end edge of the current other period matches this
	// period.
	if (normalizedOtherEdge.matches(normalizedThisAsList)) {
	  // Yes: This period matches one of the other periods.
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER + "reverse = true");
	  if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = true");
	  return true;
	}
      }

      // This period does not match any of the others.
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "reverse = false");
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = false");
      return false;
    }

    // No: The definition of one of the edges of this period includes a volume
    // and/or an issue: Check whether the start edge is not the one.
    if (!startEdge.isInfinity() && startEdge.isFullYear()) {
      // Yes.
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "isFarPast = false");

      // Determine whether this edge matches any of the other periods.
      boolean result = normalizedStartEdge.matches(normalizedOthers);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "matches = " + result);
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
      return result;
    }

    // Check whether the end edge of this period is the far future or is the one
    // that includes a volume and/or an issue.
    if (endEdge.isInfinity() || !endEdge.isFullYear()) {
      // Yes: This period does not match any of the others.
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = false");
      return false;
    }

    // No: Determine whether this edge matches any of the other periods.
    boolean result = normalizedStartEdge.matches(normalizedOthers);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "matches = " + result);
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Provides a normalized version of this period specified in a common format.
   * 
   * @return a BibliographicPeriod with the normalized version of the period.
   */
  private BibliographicPeriod normalize() {
    final String DEBUG_HEADER = "normalize(): ";
    // Reformat the period with a common format.
    BibliographicPeriod normalized = new BibliographicPeriod(BibliographicUtil
	.normaliseIdentifier(toDisplayableString()));
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "normalized = '" + normalized + "'.");
    return normalized;
  }

  /**
   * Provides an indication of whether this period includes only publication
   * full years.
   * 
   * @return a boolean with <code>true</code> if this period includes only
   *         publication full years, <code>false</code> otherwise.
   */
  public boolean includesFullYears() {
    return (isAllTime()
	|| ((startEdge == null || startEdge.isFullYear())
	    && (endEdge == null || endEdge.isFullYear())));
  }

  @Override
  public String toString() {
    return "BibliographicPeriod [startEdge=" + startEdge + ", endEdge="
	+ endEdge + "]";
  }
}
