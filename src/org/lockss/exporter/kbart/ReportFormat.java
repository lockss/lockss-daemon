/*
 * $Id: ReportFormat.java,v 1.1 2012-05-30 00:31:56 easyonthemayo Exp $
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
import static org.lockss.exporter.kbart.KbartTitle.Field.*;

import java.util.*;

/**
 * Represents the format of the report; currently that is either KBART or
 * one-title-per-line. Different report formats can process the basic report
 * data to produce a different set of output rows.
 *
 * Eventually it would make sense to redefine the export process to yield a
 * cleaner sequence encompassing all the different options which have now been
 * added on top of the original KBART package, This would clarify the work flow
 * and remove the "Kbart" prefix from classes where it is no longer relevant.
 *
 * @author Neil Mayo
 */
public class ReportFormat {

  /** Default encoding for output. */
  //public static final String DEFAULT_ENCODING = "UTF-8";

  /** A description of the report format, for display to user. */
  protected String description;

  /** A default field ordering for the report. */
  protected KbartExportFilter.FieldOrdering defaultFieldOrdering;

  /** A listing of the fields which can appear in the report. Anything
   * not in this list will be ignored. */
  protected List<KbartTitle.Field> relevantFields;

  /*
  private static final String TITLE_PER_LINE_NOTE = "One entry per title, with " +
      "coverage gaps recorded in the coverage_notes field.";
  private static final String SFX_NOTE =
      "SFX DataLoader format can be imported into SFX sytems.";
*/

  //protected Map<KbartTitle.Field, FieldFormatter> fieldFormats;

  /**
   *
   * @param titles the list of titles which are to be exported
   */
  /*public ReportFormat(List<KbartTitle> titles, OutputFormat format) {
    //super(titles, format);
    this.titles = titles;
  }*/


  /**
   * Temporary method using a default year volume format for processing.
   */
  public static List<KbartTitle> process(List<KbartTitle> titles) {
    return process(titles, CoverageNotesFormat.YEAR_VOLUME);
  }

  /**
   * Process a list of KbartTitles, combining those which are from the same
   * title according to the specified coverage notes format, and return a new list.
   * @param titles
   * @return
   */
  public static List<KbartTitle> process(List<KbartTitle> titles,
                                         CoverageNotesFormat coverageNotesFormat) {
    // KBART info should be ordered alphabetically by title by default
    Collections.sort(titles,
        KbartTitleComparatorFactory.getComparator(PUBLICATION_TITLE)
    );
    TitleCoverageRanges titleCoverageRanges = new TitleCoverageRanges(coverageNotesFormat);
    KbartTitle lastTitle = null;
    List<KbartTitle> newList = new ArrayList<KbartTitle>();
    // Compare properties and combine titles with same identifying fields
    for (KbartTitle kbt : titles) {
      // First title - init the coverage ranges with the title
      if (lastTitle==null) {
        titleCoverageRanges.reset(kbt);
      }
      // Not first KbartTitle, and from the same title as the last -
      // add to coverage ranges
      else if (BibliographicUtil.areFromSameTitle(
          new BibliographicKbartTitle(lastTitle),
          new BibliographicKbartTitle(kbt))) {
        titleCoverageRanges.add(kbt);
      }
      // First KbartTitle in a new title - complete old title and start a new one
      else {
        newList.add(titleCoverageRanges.getCombinedTitle());
        // Reset coverage ranges with the title
        titleCoverageRanges.reset(kbt);
      }
      lastTitle = kbt;
    }
    // Complete final title and return new list
    newList.add(titleCoverageRanges.getCombinedTitle());
    return newList;
  }

  /**
   * A class that records KbartTitles contributing to a set of coverage ranges,
   * and uses a CoverageNotesFormat to produce an amalgamated coverage note
   * string. It is assumed that KbartTitles are added to this set in
   * chronological order.
   */
  private static class TitleCoverageRanges {

    /** The format to adhere to in coverge range declarations. */
    private CoverageNotesFormat format;
    /** A string builder used to build up the full coverage string. */
    private StringBuilder sb = new StringBuilder();
    /** Record of the individual coverage ranges to include. */
    private List<KbartTitle> kbTitles = new ArrayList<KbartTitle>();
    /**
     * The KbartTitle representing the combined information from all titles
     * which are added to the range set.
     */
    private KbartTitle combinedTitle = null;

    /**
     * Create a coverage note of the specified format.
     * @param format a CoverageNotesFormat
     */
    public TitleCoverageRanges(final CoverageNotesFormat format) {
      this.format = format;
    }

    /**
     * Add a KbartTitle to the coverage notes record. This involves updating the
     * range endpoints and adding the KbartTitle object to the list for later
     * processing to produce the coverage notes string.
     * @param kbt
     */
    public void add(KbartTitle kbt) {
      if (combinedTitle == null) this.combinedTitle = new KbartTitle(kbt);
      //String ds = kbt.getField(DATE_FIRST_ISSUE_ONLINE);
      String de = kbt.getField(DATE_LAST_ISSUE_ONLINE);
      //String vs = kbt.getField(NUM_FIRST_VOL_ONLINE);
      String ve = kbt.getField(NUM_LAST_VOL_ONLINE);
      //String is = kbt.getField(NUM_FIRST_ISSUE_ONLINE);
      String ie = kbt.getField(NUM_LAST_ISSUE_ONLINE);
      // Extend range end values
      combinedTitle.setField(DATE_LAST_ISSUE_ONLINE, de);
      combinedTitle.setField(NUM_LAST_ISSUE_ONLINE, ie);
      combinedTitle.setField(NUM_LAST_VOL_ONLINE, ve);
      // Add the title to the list (or start the coverage notes string
      // according to the format)
      kbTitles.add(kbt);
    }
    /**
     * Reset the coverage set.
     */
    public void reset() {
      this.kbTitles.clear();
      this.combinedTitle = null;
    }
    /**
     * Reset the coverage set and init the combined title to match the supplied
     * KbartTitle.
     * @param kbt
     */
    public void reset(KbartTitle kbt) {
      this.reset();
      this.add(kbt);
    }
    /**
     * Construct the coverage notes string from the list of KbartTitles,
     * according to the format.
     * @return
     */
    public String constructCoverageNotes() {
      StringBuilder coverageNotes = new StringBuilder();
      if (format.isSummary) {
        // Create coverage notes from start of first and end of last
        // TODO coverageNotes.append(kbTitles.get(0));
      }
      else
        for (KbartTitle kbt : kbTitles) {
          format.addTitleCoverage(kbt, coverageNotes);
        }
      return coverageNotes.toString();
    }

    /**
     * Get a KbartTitle representing the combination of the titles listed in
     * this
     * @return
     */
    public KbartTitle getCombinedTitle() {
      combinedTitle.setField(COVERAGE_NOTES, constructCoverageNotes());
      return combinedTitle;
    }
  }


  /**
   * Enumeration of formats for the data in coverage notes field.
   */
  public static enum CoverageNotesFormat {

    SFX_DATALOADER("SFX DataLoader", " && ", " || ", "undef", true, false) {
      @Override
      public void appendStart(StringBuilder sb, KbartTitle kbt) {
        appendPoint(sb, kbt, ">=");
      }

      @Override
      public void appendEnd(StringBuilder sb, KbartTitle kbt) {
        appendPoint(sb, kbt, "<=");
      }
      @Override
      public void appendEqualRange(StringBuilder sb, KbartTitle kbt) {
        appendPoint(sb, kbt, "=");
      }
      /**
       * Generic method to add a point (start, end, etc) representing a range
       * endpoint. The operator represents the type of the point, as in
       * SFX General User’s Guide, Table 16.
       * @param sb
       * @param kbt
       * @param operator
       */
      public void appendPoint(StringBuilder sb, KbartTitle kbt, String operator) {
        String ys = kbt.getField(DATE_FIRST_ISSUE_ONLINE);
        String vs = kbt.hasFieldValue(NUM_FIRST_VOL_ONLINE) ?
            kbt.getField(NUM_FIRST_VOL_ONLINE) : this.noValStr;
        String is = kbt.hasFieldValue(NUM_FIRST_ISSUE_ONLINE) ?
            kbt.getField(NUM_FIRST_ISSUE_ONLINE) : this.noValStr;
        sb.append("$obj->parsedDate(\"").append(operator).append("\",")
            .append(ys)
            .append(",")
            .append(vs)
            .append(",")
            .append(is)
            .append(")")
        ;
      }
    },
    YEAR_RANGES("Year Ranges", false, false),
    YEAR_VOLUME("Year(Volume) Ranges", true, false),
    YEAR_SUMMARY("Year Summary", false, true),
    YEAR_VOLUME_SUMMARY("Year(Volume) Summary", true, true)
    ;

    /** A description of the coverage notes format, for display to user. */
    public String description;
    /** String used to separate ranges; comma space by default. */
    protected String rngSep = ", ";
    /** String used to separate endpoints of ranges; hyphen by default. */
    private String rngJoin = "-";
    /** String to use if there is no value for volume; empty string by default. */
    protected String noValStr = "";
    /** Whether this format provides a summary, that is, only endpoints across
     * the set of ranges. This implies that the rngSep is not used. */
    protected boolean isSummary = false;
    /** Whether to show volume info; false by default. */
    protected boolean showVol = false;

    /** Construct a non-summary enum with default values for strings. */
    CoverageNotesFormat(String description) {
      this.description = description;
    }
    /**
     * Construct an enum with the given description and values for note strings.
     * The description is required; if any other string is null, the default
     * will be used. Also allows switches to be specified indicating whether to
     * show volumes, and whether the format is a summary one (only showing
     * endpoints).
     */
    CoverageNotesFormat(String description, String rngJoin, String rngSep,
                        String noValStr, boolean showVol, boolean summary) {
      this.description = description;
      if (rngJoin!=null) this.rngJoin = rngJoin;
      if (rngSep!=null) this.rngSep = rngSep;
      if (noValStr !=null) this.noValStr = noValStr;
      this.showVol = showVol;
      this.isSummary = summary;
    }
    /**
     * Construct an enum with the given summary switch, and default values for
     * note strings.
     */
    CoverageNotesFormat(String description, boolean showVol, boolean summary) {
      this(description, null, null, null, showVol, summary);
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
        if (sb.length()!=0) sb.append(rngSep);
        sb.append(cn);
      }
    }

    /**
     * Construct the coverage note info for the given KbartTitle; that is, the
     * range represented by the title. This is presented in the format:
     * <br/>
     *  <i></i>1991(36), 1995(39)-2005(49), 2010(54)-</i>
     * <br/>
     * The last range here extends to the present.
     * If volumes are not available, the '(volume)' part is omitted.
     * It is assumed that if a start value is missing, there is no range data for
     * that field; if there is a start value with no end value, this is a range
     * extending to the present. KbartTitles ought to provide a minimum of start
     * date or volume; usually if one date is available, they should both be.
     * We then have to do some interpretation.
     * @param kbt a KbartTitle
     * @return a string representing the coverage range of the title
     */
    public String constructCoverageNote(KbartTitle kbt) {
      // Get the start and end dates and volumes
      // Note: use kbt.getFieldValue() to retrieve the actual end values
      // regardless of whethere they represent to the present.
      String ds = kbt.getField(KbartTitle.Field.DATE_FIRST_ISSUE_ONLINE);
      String de = kbt.getField(KbartTitle.Field.DATE_LAST_ISSUE_ONLINE);
      String vs = kbt.getField(KbartTitle.Field.NUM_FIRST_VOL_ONLINE);
      String ve = kbt.getField(KbartTitle.Field.NUM_LAST_VOL_ONLINE);

      StringBuilder range = new StringBuilder();

      // Are there dates and/or vols
      //boolean vols = !StringUtil.isNullString(vs);
      //boolean dates = !StringUtil.isNullString(ds);
      // Return empty string if no vals
      //if (!vols && !dates) return "";


      // Check first for empty strings
      if (StringUtil.isNullString(ds) && StringUtil.isNullString(vs)) return "";

      // Is there a non-empty end value
      boolean end = !StringUtil.isNullString(de) || !StringUtil.isNullString(ve);
      // Only show a range if the start and end of an available field are different
      //boolean rng = (vols && !vs.equals(ve)) || (dates && !ds.equals(de));
      boolean rng = !vs.equals(ve) || !ds.equals(de);

      appendStart(range, kbt);
      if (rng) {
        appendRangeJoin(range);
        if (end) appendEnd(range, kbt);
      }

      // Construct ranges
      /*if (dates && !vols) {
        appendStart(range, kbt);
        if (rng) {
          appendRangeJoin(range);
          if (end) appendEnd(range, kbt);
        }
      }
      else if (!dates && vols) {
        appendStart(range, kbt);
        if (rng) {
          appendRangeJoin(range);
          if (end) appendEnd(range, kbt);
        }
      }
      else if (dates && vols) {
        appendStart(range, kbt);
        if (rng) {
          appendRangeJoin(range);
          if (end) appendEnd(range, kbt);
        }
      }*/
       // else neither dates nor vols = empty string


      //System.err.println("coverage notes "+range);
      return range.toString();
    }


  }

}
