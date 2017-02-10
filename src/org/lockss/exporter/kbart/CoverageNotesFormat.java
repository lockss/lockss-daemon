/*
 * $Id$
 */

/*

Copyright (c) 2012 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.exporter.biblio.BibliographicUtil;
import org.lockss.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import static org.lockss.exporter.kbart.KbartTitle.Field.*;

/**
 * Enumeration of formats for the data in the coverage_notes field in a
 * KBART export. The coverage_notes field will contain a separated list of the
 * actual ranges which are available in the range specified in the title record.
 * The title record itself may show only the endpoints of an amalgamated set of
 * titles. The format is constructed using generic methods which append start
 * and end strings for ranges, range join strings and range separator strings,
 * or a representation of a one-title range. These methods can be overridden
 * to provide enhanced behaviour.
 * <p>
 * Year/volume ranges are presented in formats similar to:
 * <pre>
 *  1991(36), 1995(39)-2005(49), 2010(54)-
 * </pre>
 * The last range here extends to the present.
 * </p>
 *
 * @author Neil Mayo
 */
public enum CoverageNotesFormat {

  /** Show contiguous year ranges included in a record. */
  YEAR("Year Ranges", false, false),

  /** Show contiguous year(volume) ranges included in a record. */
  YEAR_VOLUME("Year(Volume) Ranges", true, false),

  /** Show only endpoints of year ranges included in a record. */
  YEAR_SUMMARY("Year Summary", false, true),

  /** Show only endpoints of year(volume) ranges included in a record. */
  YEAR_VOLUME_SUMMARY("Year(Volume) Summary", true, true),

  /**
   * Show ranges in SFX DataLoader format. This uses available year, volume
   * and issue data and overrides append methods to provide the appropriate
   * format, for example:
   * <pre>
   * $obj->parsedDate(">=",1995,39,undef) && $obj->parsedDate("<=",2005,49,undef)
   * </pre>
   * represents a range 1995(39)-2005(49). Further ranges are appnded with an
   * or "||" separator.
   *
   * The field character limit is set to 500 as SFX has a limit of around 512
   * characters.
   */
  SFX("SFX DataLoader", " && ", " || ", "undef", true, false, 500) {
    @Override
    /** Override the join sep; don't add if there is nothing following. */
    public void appendRangeJoin(StringBuilder sb, boolean hasRangeEnd) {
      if (hasRangeEnd) this.appendRangeJoin(sb);
    }

    @Override
    public void appendStart(StringBuilder sb, KbartTitle kbt) {
      appendPoint(sb, kbt, ">=", false);
    }

    @Override
    public void appendEnd(StringBuilder sb, KbartTitle kbt) {
      appendPoint(sb, kbt, "<=", true);
    }

    @Override
    public void appendEqualRange(StringBuilder sb, KbartTitle kbt) {
      appendPoint(sb, kbt, "==", false);
    }

    /**
     * Generic method to add a point (start, end, etc) representing a range
     * endpoint. The operator represents the type of the point, as in
     * SFX General User’s Guide, Table 16.
     * @param sb a StringBuilder to append to
     * @param kbt a KbartTitle
     * @param operator the operator
     * @param useEnd whether to use the end of the title's range
     */
    public void appendPoint(StringBuilder sb, KbartTitle kbt, String operator,
                            boolean useEnd) {
      // Use the appropriate field
      KbartTitle.Field yfld = useEnd ? DATE_LAST_ISSUE_ONLINE : DATE_FIRST_ISSUE_ONLINE;
      KbartTitle.Field vfld = useEnd ? NUM_LAST_VOL_ONLINE    : NUM_FIRST_VOL_ONLINE;
      KbartTitle.Field ifld = useEnd ? NUM_LAST_ISSUE_ONLINE  : NUM_FIRST_ISSUE_ONLINE;
      // Get a field value or the no value string
      String y = kbt.hasFieldValue(yfld) ? kbt.getField(yfld) : noValStr;
      String v = kbt.hasFieldValue(vfld) ? kbt.getField(vfld) : noValStr;
      String i = kbt.hasFieldValue(ifld) ? kbt.getField(ifld) : noValStr;
      sb.append("$obj->parsedDate(\"").append(operator).append("\",")
          .append(y)
          .append(",")
          .append(v)
          .append(",")
          .append(i)
          .append(")")
      ;
    }
  },
  ;

  // ------------------ Format members and methods below here ------------------

  /** An optional character limit for the coverage notes format. Default 1024. */
  public int charLimit;
  private static final int DEFAULT_CHAR_LIMIT = 1024;
  /** The max number of years allowable between ranges such that they are
   * combined when peforming a reduction on the ranges. */
  public static final int DEFAULT_RANGE_REDUCTION_THRESHOLD = 2;
  /** A description of the coverage notes format, for display to user. */
  public final String label;
  /** String used to separate ranges; comma space by default. */
  protected String rngSep = ", ";
  /** String used to separate endpoints of ranges; hyphen by default. */
  protected String rngJoin = "-";
  /** String to use if there is no value for volume; empty string by default. */
  protected String noValStr = "";
  /** Whether this format provides a summary, that is, only endpoints across
   * the set of ranges. This implies that the rngSep is not used. */
  protected boolean isSummary = false;
  /** Whether to show volume info. */
  protected boolean showVol = false;

  /** Construct a non-summary enum with default values for strings. */
  CoverageNotesFormat(String label) {
    this.label = label;
  }
  /**
   * Construct an enum with the given label and values for note strings.
   * The label is required; if any other string is null, the default
   * will be used. Also allows switches to be specified indicating whether to
   * show volumes, and whether the format is a summary one (only showing
   * endpoints).
   */
  CoverageNotesFormat(String label, String rngJoin, String rngSep,
                      String noValStr, boolean showVol, boolean summary) {
    this(label, rngJoin, rngSep, noValStr, showVol, summary, DEFAULT_CHAR_LIMIT);
  }
  CoverageNotesFormat(String label, String rngJoin, String rngSep,
                      String noValStr, boolean showVol, boolean summary,
                      int charLimit) {
    this.label = label;
    if (rngJoin!=null) this.rngJoin = rngJoin;
    if (rngSep!=null) this.rngSep = rngSep;
    if (noValStr !=null) this.noValStr = noValStr;
    this.showVol = showVol;
    this.isSummary = summary;
    this.charLimit = charLimit;
  }
  /**
   * Construct an enum with the given summary switch, and default values for
   * note strings.
   */
  CoverageNotesFormat(String label, boolean showVol, boolean summary) {
    this(label, null, null, null, showVol, summary);
  }

  /**
   * Append start data to the given StringBuilder. This method provides a
   * default implementation which includes a bracketed volume identifier if
   * available and wanted. It may be overridden by subclasses.
   * @param sb a StringBuilder
   * @param kbt a KbartTitle with the data
   */
  public void appendStart(StringBuilder sb, KbartTitle kbt) {
    String ys = kbt.getField(DATE_FIRST_ISSUE_ONLINE);
    sb.append(ys);
    if (showVol && kbt.hasFieldValue(NUM_FIRST_VOL_ONLINE))
      sb.append("(")
          .append(kbt.getField(NUM_FIRST_VOL_ONLINE))
          .append(")");
  }

  /**
   * Append end data to the given StringBuilder. This method provides a
   * default implementation which includes a bracketed volume identifier if
   * available and wanted. It may be overridden by subclasses.
   * @param sb a StringBuilder
   * @param kbt a KbartTitle with the data
   */
  public void appendEnd(StringBuilder sb, KbartTitle kbt) {
    String ye = kbt.getField(DATE_LAST_ISSUE_ONLINE);
    sb.append(ye);
    if (showVol && kbt.hasFieldValue(NUM_LAST_VOL_ONLINE))
      sb.append("(")
          .append(kbt.getField(NUM_LAST_VOL_ONLINE))
          .append(")");
  }

  /**
   * Append start data to the given StringBuilder. This method provides a
   * default implementation which includes a bracketed volume identifier if
   * available and wanted. It may be overridden by subclasses.
   * @param sb a StringBuilder
   * @param kbt a KbartTitle with the data
   */
  public void appendEqualRange(StringBuilder sb, KbartTitle kbt) {
    appendStart(sb, kbt);
  }

  /**
   * Append a range join string to the given StringBuilder. The hasRangeEnd
   * parameter allows the enum to take different actions depending on whether
   * a range end point is to follow the join. This method provides a default
   * implementation which just calls the one-arg method directly, but may be
   * overridden by subclasses to take different action on the boolean.
   * @param sb a StringBuilder
   * @param hasRangeEnd whether there is a range end point to follow
   */
  public void appendRangeJoin(StringBuilder sb, boolean hasRangeEnd) {
    appendRangeJoin(sb);
  }

  /**
   * Append a range join string to the given StringBuilder. This method
   * provides a default implementation using the rngJoin parameter, but may be
   * overridden by subclasses.
   * @param sb a StringBuilder
   */
  public void appendRangeJoin(StringBuilder sb) { sb.append(rngJoin); }

  /**
   * Append a range separator string to the given StringBuilder. This method
   * provides a default implementation using the rngSep parameter, but may be
   * overridden by subclasses.
   * @param sb a StringBuilder
   */
  public void appendRangeSep(StringBuilder sb) { sb.append(rngSep); }

  /**
   * Append a coverage note for the range represented in a KbartTitle to a
   * StringBuilder representing amalgamated coverage notes.
   * @param kbt a KbartTitle representing a range
   * @param sb the StringBuilder to append to
   */
  protected void addTitleCoverage(KbartTitle kbt, StringBuilder sb) {
    String cn = constructCoverageNote(kbt);
    if (!StringUtil.isNullString(cn)) {
      if (sb.length()!=0) appendRangeSep(sb);
      sb.append(cn);
    }
  }

  /**
   * Append a summary coverage note for the range represented in
   * the list of KbartTitles to a StringBuilder. The summary note
   * runs from the start of the first title to the end of the last.
   * @param kbTitle a list of KbartTitles representing a range
   * @param sb the StringBuilder to append to
   */
  protected void addSummaryCoverage(List<KbartTitle> kbTitles, StringBuilder sb) {
    KbartTitle start = kbTitles.get(0);
    KbartTitle end = kbTitles.get(kbTitles.size()-1);
    KbartTitle dummyKbt = new KbartTitle(start);
    ReportFormat.updateEndValues(end, dummyKbt);
    addTitleCoverage(dummyKbt, sb);
  }

  /**
   * Construct the coverage note for the given KbartTitle; that is, the
   * range(s) represented by the title.
   * <p>
   * It is assumed that if start values are missing, there is no range data for
   * that field; if there is a start value with no end value, this is a range
   * extending to the present. If the start and end values are equal, it is a
   * single title. KbartTitles ought to provide a minimum of start
   * date or volume; usually if one date/vol is available, they should both be.
   *
   * @param kbt a KbartTitle
   * @return a string representing the coverage range of the title
   */
  public String constructCoverageNote(KbartTitle kbt) {
    // Get the start and end dates and volumes
    // Note: use kbt.getFieldValue() to retrieve the actual end values
    // regardless of whether they represent to the present.
    String ds = kbt.getField(KbartTitle.Field.DATE_FIRST_ISSUE_ONLINE);
    String de = kbt.getField(KbartTitle.Field.DATE_LAST_ISSUE_ONLINE);
    String vs = kbt.getField(KbartTitle.Field.NUM_FIRST_VOL_ONLINE);
    String ve = kbt.getField(KbartTitle.Field.NUM_LAST_VOL_ONLINE);

    StringBuilder range = new StringBuilder();
    // Check first for empty start strings
    if (StringUtil.isNullString(ds) && StringUtil.isNullString(vs)) return "";
    // Is there a non-empty end value
    boolean end = !StringUtil.isNullString(de) || !StringUtil.isNullString(ve);
    // Only show a range if the start and end of an available field are different
    boolean rng = !vs.equals(ve) || !ds.equals(de);

    if (rng) {
      appendStart(range, kbt);
      appendRangeJoin(range, end);
      if (end) appendEnd(range, kbt);
    } else appendEqualRange(range, kbt);
    return range.toString();
  }


  /**
   * Go through the given list of KbartTitles and try to reduce the number of
   * entries by increasing the scope of the ranges. By default we do this by
   * attempting to combine ranges which are separated by no more than 2 years.
   * @param titles
   * @return
   */
  public List<KbartTitle> restrictRanges(List<KbartTitle> titles) {
    List<KbartTitle> newList = new ArrayList<KbartTitle>();
    int currentEndDate = 0;
    KbartTitle rangeTitle = null;
    //
    for (KbartTitle kbt : titles) {
      // if this is the first title, make it the current range title and record end
      if (rangeTitle==null) {
        rangeTitle = kbt;
        currentEndDate = BibliographicUtil.stringYearAsInt(rangeTitle.getFieldValue(DATE_LAST_ISSUE_ONLINE));
        continue;
      }
      // Compare the last date in the joinedTitle with the first in the next title
      // Get the dates of the current title
      int sDate = BibliographicUtil.stringYearAsInt(kbt.getFieldValue(DATE_FIRST_ISSUE_ONLINE));
      int eDate = BibliographicUtil.stringYearAsInt(kbt.getFieldValue(DATE_LAST_ISSUE_ONLINE));
      // If either date is invalid or the difference is greater than the
      // threshold, add the title and start a new one.
      if (currentEndDate==0 || sDate==0 || sDate - currentEndDate > DEFAULT_RANGE_REDUCTION_THRESHOLD) {
        newList.add(rangeTitle);
        rangeTitle = kbt;
      } // Otherwise set the end date
      else rangeTitle.setField(DATE_LAST_ISSUE_ONLINE, ""+eDate);
      // Set the end date
      currentEndDate = eDate;
    }
    // Add last title to the list
    newList.add(rangeTitle);
    return newList;
  }


  /**
   * Get a CoverageNotesFormat by name. Upper cases the name so lower case values
   * can be passed in URLs.
   *
   * @param name a string representing the name of the format
   * @return a CoverageNotesFormat with the specified name, or null if none was found
   */
  public static CoverageNotesFormat byName(String name) {
    return byName(name,  null);
  }
  /**
   * Get an CoverageNotesFormat by name, or the default if the name cannot be parsed.
   *
   * @param name a string representing the name of the format
   * @param def the default to return if the name is invalid
   * @return an CoverageNotesFormat with the specified name, or the default
   */
  public static CoverageNotesFormat byName(String name, CoverageNotesFormat def) {
    try {
      return valueOf(name.toUpperCase());
    } catch (Exception e) {
      return def;
    }
  }


}
