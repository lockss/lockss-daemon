/*
 * $Id: ReportFormat.java,v 1.2.2.2 2012-05-31 17:07:52 easyonthemayo Exp $
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
  //protected String description;

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
   * Process without amalgamating into title per line;
   * just add coverage notes fields
   */
  private static List<KbartTitle> processKbart(List<KbartTitle> titles,
                                         CoverageNotesFormat coverageNotesFormat) {
    TitleCoverageRanges titleCoverageRanges = new TitleCoverageRanges(coverageNotesFormat);
    // Add a coverage notes field to each title
    for (KbartTitle kbt : titles) {
      titleCoverageRanges.reset(kbt);
      kbt.setField(COVERAGE_NOTES, titleCoverageRanges.constructCoverageNotes());
    }
    return titles;
  }

  /**
   * Process a list of KbartTitles, combining those which are from the same
   * title according to the specified coverage notes format, and return a new list.
   * @param titles
   * @return
   */
  public static List<KbartTitle> process(List<KbartTitle> titles,
                                         CoverageNotesFormat coverageNotesFormat,
                                         ReportDataFormat reportDataFormat) {
    // KBART info should be ordered alphabetically by title by default
    Collections.sort(titles,
        KbartTitleComparatorFactory.getComparator(PUBLICATION_TITLE)
    );
    // XXX Set coverage note format to SFX if SFX output chosen
    if (reportDataFormat == ReportDataFormat.SFX) coverageNotesFormat = CoverageNotesFormat.SFX;
    // Process std KBART is not one line per title
    if (!reportDataFormat.isOneLinePerTitle()) return processKbart(titles, coverageNotesFormat);

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
   * Update the target title with range end values from the source title.
   * @param source title containing the end values
   * @param target title which will receive the end values
   */
  public static void updateEndValues(KbartTitle source, KbartTitle target) {
    String de = source.getField(DATE_LAST_ISSUE_ONLINE);
    String ve = source.getField(NUM_LAST_VOL_ONLINE);
    String ie = source.getField(NUM_LAST_ISSUE_ONLINE);
    // Extend range end values
    target.setField(DATE_LAST_ISSUE_ONLINE, de);
    target.setField(NUM_LAST_ISSUE_ONLINE, ie);
    target.setField(NUM_LAST_VOL_ONLINE, ve);
  }


  /**
   * A class that records KbartTitles contributing to a set of coverage ranges,
   * and uses a CoverageNotesFormat to produce an amalgamated coverage note
   * string. It is assumed that KbartTitles are added to this set in
   * chronological order.
   */
  public static class TitleCoverageRanges {

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
      updateEndValues(kbt, combinedTitle);
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
        format.addSummaryCoverage(kbTitles, coverageNotes);
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
   * Format of the report produced; that is, the combination of title
   * amalgamation, visible fields etc.
   */
  public static enum ReportDataFormat {
    // KBART is the default format, with an entry per complete range
    KBART("Title Ranges", "KBART standard format; each line is a complete title " +
        "range. Coverage gaps will results in multiple ranges for a single title.", false),
    // The KBART format but with an entry per title
    TITLES("Titles", "Like KBART, but each title has only one line, with " +
        "amalgamated ranges; precise coverage is indicated in the " +
        "coverage_notes field.", true),
    // SFX is a special variation on the one_per_line, with a specific coverage_notes format
    SFX("SFX DataLoader", "Suitable for import into ExLibris SFX.", true)
    ;
    
    private ReportDataFormat(String label, String footnote, boolean oneLinePerTitle) {
      this.label = label;
      this.footnote = footnote;
      this.oneLinePerTitle = oneLinePerTitle;
    }

    /** The displayed label for the output. */
    private final String label;
    /** An optional footnote elaborating the export format. */
    private final String footnote;
    /** Whether this format should amalgamate titles to produce one record per title. */
    private final boolean oneLinePerTitle;

    public String getLabel() { return label; }
    public String getFootnote() { return footnote; }
    protected boolean isOneLinePerTitle() { return oneLinePerTitle;} 

    /**
     * Get a ReportDataFormat by name. Upper cases the name so lower case values
     * can be passed in URLs.
     *
     * @param name a string representing the name of the format
     * @return a ReportDataFormat with the specified name, or null if none was found
     */
    public static ReportDataFormat byName(String name) {
      return byName(name,  null);
    }
    /**
     * Get an ReportDataFormat by name, or the default if the name cannot be parsed.
     *
     * @param name a string representing the name of the format
     * @param def the default to return if the name is invalid
     * @return an ReportDataFormat with the specified name, or the default
     */
    public static ReportDataFormat byName(String name, ReportDataFormat def) {
      try {
        return valueOf(name.toUpperCase());
      } catch (Exception e) {
        return def;
      }
    }

  }

}
