/*
 * $Id: RunKbartReport.java,v 1.1 2012-01-12 12:47:22 easyonthemayo Exp $
 */

/*

Copyright (c) 2011 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.devtools;

import org.apache.commons.cli.*;
import org.apache.commons.lang.ArrayUtils;
import org.lockss.exporter.biblio.BibliographicItem;
import org.lockss.exporter.biblio.BibliographicItemImpl;
import org.lockss.exporter.biblio.BibliographicUtil;
import org.lockss.exporter.kbart.KbartConverter;
import org.lockss.exporter.kbart.KbartExportFilter;
import org.lockss.exporter.kbart.KbartExporter;
import org.lockss.exporter.kbart.KbartTitle;
import org.lockss.exporter.kbart.KbartTitle.Field;
import static org.lockss.exporter.kbart.KbartExportFilter.PredefinedFieldOrdering;
import com.csvreader.CsvReader;
import org.lockss.util.StringUtil;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Reads holdings data from outside, and processes it into a KBART report.
 * The iteration over the input should probably be extracted to an external
 * class.
 * <p>
 * Uses the Apache Commons cli library, which has some quirks:
 * <ul>
 *   <li>By default, it is not possible to display a usage message where the
 *   options are in the order in which they are specified.</li>
 *   <li>If any required option is missing, parsing fails and the CommandLine
 *   object is null, preventing one from providing any useful feedback, and
 *   rendering the use of a help (-h) option pointless.</li>
 * </ul>
 * This unfortunately leads to some messy options setup as seen below.
 * 
 * @author Neil Mayo
 */
public class RunKbartReport {
  
  /** An input stream providing the CSV input data. */
  private final InputStream inputStream;
  // Settings from the command line
  private final boolean hideEmptyColumns;
  private final boolean showTdbStatus;
  private KbartExportFilter.FieldOrdering fieldOrdering;

  private static final String DATA_FORMATS_CONFIG_FILE = "data-formats.xml";
  /*  data-formats.xml
  org.lockss.exporter.kbart.Data-Format
  format @name
      display-name
      desc
      cols
  */
  /*private static List<KbartExportFilter.FieldOrdering> dataFormats;
  static {
    InputStream file = ClassLoader.getSystemClassLoader().getResourceAsStream(DATA_FORMATS_CONFIG_FILE);
  }*/
  /*private static List<KbartExportFilter.FieldOrdering> dataFormats =
      new ArrayList<KbartExportFilter.FieldOrdering>() {{
        KbartExportFilter.PredefinedFieldOrdering.values();
      }};*/

  // ---------------------------------------------------------------------------

  /**
   * Construct an instance of this class, for running a single report.
   */
  private RunKbartReport(CommandLine clopts) throws FileNotFoundException,
      IllegalArgumentException {
    // Parse options and pass to constructor
    // See new KbartExportFilter(titles) for default options
    this(
        clopts.hasOption(SOPT_HIDE_EMPTY_COLS),
        clopts.hasOption(SOPT_SHOW_STATUS),
        KbartExportFilter.PredefinedFieldOrdering.valueOf(
            clopts.getOptionValue(SOPT_DATA)),
        new FileInputStream(clopts.getOptionValue(SOPT_FILE)),
        System.out
    );
  }

  /**
   * Construct an instance of this class with the supplied properties.
   * @param hideEmptyColumns
   * @param showTdbStatus
   * @param fieldOrdering
   * @param inputStream
   * @param outputStream
   */
  protected RunKbartReport(boolean hideEmptyColumns,
                           boolean showTdbStatus,
                           KbartExportFilter.PredefinedFieldOrdering fieldOrdering,
                           InputStream inputStream, OutputStream outputStream) {
    this.hideEmptyColumns = hideEmptyColumns;
    this.showTdbStatus = showTdbStatus;
    this.inputStream = inputStream;
    this.fieldOrdering = fieldOrdering;

    long s = System.currentTimeMillis();
    // Now we are doing an export - create the exporter
    KbartExporter kexp = createExporter();
    // Make sure the exporter was properly instantiated
    if (kexp==null) die("Could not create exporter");
    // Do the export
    kexp.export(outputStream);
    System.err.format("Export took approximately %ss\n",
        (System.currentTimeMillis() - s) / 1000);
  }


  /**
   * Make an exporter to be used in an export; this involves extracting and
   * converting titles from the TDB and passing to the exporter's constructor.
   * The exporter is configured with the basic settings; further configuration
   * may be necessary for custom exports.
   *
   * @param outputFormat the output format for the exporter
   * @return a usable exporter, or null if one could not be created
   */
  private KbartExporter createExporter() {
    // The list of KbartTitles to export; each title represents a TdbTitle over
    // a particular range of coverage.
    List<KbartTitle> titles = null;
    // Get the list of BibliographicItems and turn them into
    // KbartTitles which represent the coverage ranges available for the titles.
    try {
      titles = KbartConverter.convertTitleAus(
          new KbartCsvTitleIterator(inputStream)
      );
    } catch (Exception e) {
      die("Could not read CSV file. "+e.getMessage(), e);
    }
    System.err.println(titles.size()+" KbartTitles for export");

    // Return if there are no titles
    if (titles.isEmpty()) {
      System.err.println("No titles for export.");
      return null;
    }

    // Create a filter
    KbartExportFilter filter = new KbartExportFilter(titles, fieldOrdering,
        hideEmptyColumns, false);

    // Create and configure a CSV exporter
    KbartExporter kexp = KbartExporter.OutputFormat.CSV.makeExporter(titles, filter);
    return kexp;
  }


  /**
   * An iterator for a CSV input file, outputting a List of BibliographicItems
   * per sequence of consecutive records for the same title. Multiple records
   * for the same title, with different coverage data, should be listed
   * consecutively or they will not be combined into a list.
   * Empty lines will be silently ignored.
   */
  static class KbartCsvTitleIterator implements Iterator<List<BibliographicItem>> {

    /** The iterator on the records in the CSV file. */
    private final KbartCsvIterator recordIterator;
    /** The next item from the record iterator, to start a new title. */
    private BibliographicItem nextItem = null;

    /**
     * Create an iterator on a CSV file, which returns a list of
     * BibliographicItems for each title.
     * @param inputStream
     * @throws IOException
     * @throws IllegalArgumentException
     */
    protected KbartCsvTitleIterator(InputStream inputStream)
        throws IOException, IllegalArgumentException {
      this.recordIterator = new KbartCsvIterator(inputStream);
      this.nextItem = recordIterator.next();
    }

    public boolean hasNext() {
      // If there are more CSV records, there is another title
      //return recordIterator.hasNext();
      return nextItem!=null;
    }

    public List<BibliographicItem> next() {
      if (nextItem==null) throw new NoSuchElementException();
      // Create a title with the next item
      Vector<BibliographicItem> title = new Vector<BibliographicItem>() {{
        add(nextItem);
      }};

      // Read the records until one differs
      while (recordIterator.hasNext()) {
        nextItem = recordIterator.next();
        // Add this item to the title if it is the first or if it
        // appears to have the same id as the previous; otherwise break
        if (title.isEmpty() || BibliographicUtil.haveSameIdentity(title.lastElement(), nextItem)) {
          title.add(nextItem);
          /*if (!title.isEmpty())
              System.err.format("Same identity:\n   %s\n   %s\n",
                title.lastElement(), nextItem);*/
        } else {
          return title;
        }
      }
      // No more records
      nextItem = null;
      return title;
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }


  /**
   * An iterator for a CSV input file, outputting a BibliographicItem per
   * record. If there are multiple records for the same title, with different
   * coverage data, they should be listed consecutively or they will not be
   * combined. Empty lines will be silently ignored.
   */
  static class KbartCsvIterator implements Iterator<BibliographicItem> {

    private final CsvReader csvReader;
    private String[] nextLine;
    protected final KbartFieldMapping mapping;

    /**
     * Create an iterator on a CSV file, which returns a BibliographicItem for
     * each line.
     * @param inputStream
     * @throws IOException
     * @throws RuntimeException
     */
    protected KbartCsvIterator(InputStream inputStream)
        throws IOException, RuntimeException {

      this.csvReader = new CsvReader(inputStream,
          Charset.forName("utf-8"));
      // Read first line as header line
      csvReader.readHeaders();
      // If the first line is not an appropriate list of field names
      // for the mapping, an exception is thrown
      this.mapping = new KbartFieldMapping(csvReader.getHeaders());

      // Read the first line of data
      try {
        this.nextLine = getNonEmptyLine(csvReader);
      } catch (Exception e) {
        throw new NoSuchElementException("There are no data rows.");
      }
    }

    public boolean hasNext() {
      return nextLine != null;
    }

    /**
     * Read from the iterator until the first non-empty line.
     * @param csvIterator
     * @return the next line, or null if there is no such line
     */
    private static String[] getNonEmptyLine(CsvReader csvReader) {
      String[] line = new String[0];
      while (line!=null && ArrayUtils.isEmpty(line)) {
        try {
          if (csvReader.readRecord()) line = csvReader.getValues();
          else return null;
        } catch (IOException e) {
          line = null;
        }
      }
      return line;
    }

    /**
     *
     * @return a BibliographicItem representing the next non-empty record
     */
    public BibliographicItem next() throws NoSuchElementException {
      if (nextLine==null) throw new NoSuchElementException();
      // Create the next BibliographicItem
      BibliographicItem bibItem = makeBibItem(nextLine);
      // Get next non-empty line
      nextLine = getNonEmptyLine(csvReader);
      return bibItem;
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }

    private BibliographicItem makeBibItem(String[] props) {
      return mapping.getBibliographicItem(props);
    }

    /**
     * A mapping of KBART fields to their column positions in the input file.
     */
    class KbartFieldMapping {
      private final Map<Field, Integer> fieldPositions;
      protected final boolean containsIdField;
      protected final boolean containsRangeField;

      /**
       *
       * @param header an array of header strings
       */
      KbartFieldMapping(String[] header) {
        this.fieldPositions = new HashMap<Field, Integer>();
        // Map field names that match KBART names, to their position
        for (int i=0; i< header.length; i++) {
          String s = header[i];
          try {
            Field field = Field.valueOf(s.toUpperCase());
            fieldPositions.put(field, i);
          } catch (Exception e) {
            // No such KBART field; ignore
          }
        }
        this.containsIdField = containsIdField();
        this.containsRangeField = containsRangeField();
        if (!containsIdField)
          throw new IllegalArgumentException("No id fields.");
        //if (!containsRangeField)
        //  throw new IllegalArgumentException("No range fields.");
      }

      /**
       * Get the value of the field from the array using the mapping.
       * @param f the field to find
       * @param values a list of values matching the field mapping
       * @return the mapped value of the field, or <tt>null</tt> if no such field or it is empty
       */
      public String getValue(Field f, String[] values) {
        try {
          //return fieldPositions.containsKey(f) ? values[fieldPositions.get(f)] : null;
          String s = values[fieldPositions.get(f)];
          // Return null if the string is empty
          return StringUtil.isNullString(s) ? null : s;
        } catch (Exception e) {
          return null;
        }
      }

      private boolean containsIdField() {
        for (Field f : Field.idFields) {
          if (fieldPositions.keySet().contains(f)) return true;
        }
        return false;
      }

      private boolean containsRangeField() {
        for (Field f : Field.rangeFields) {
          if (fieldPositions.keySet().contains(f)) return true;
        }
        return false;
      }

      /**
       * Create a BibliographicItem that returns values for KBART fields from
       * the value set supplied. Uses BibliographicItemImpl to take advantage of
       * its basic functionality, in particular the getIssn() implementation.
       * @param values an array of mapped String values
       * @return
       */
      public BibliographicItem getBibliographicItem(final String[] values) {
        return new BibliographicItemImpl() {
          public String toString() {
            return String.format("BibliographicItem %s %s", getPrintIssn(), getJournalTitle());
          }
        }
            .setPrintIssn(getValue(Field.PRINT_IDENTIFIER, values))
            .setEissn(getValue(Field.ONLINE_IDENTIFIER, values))
                //.setIssnL("")
            .setJournalTitle(getValue(Field.PUBLICATION_TITLE, values))
            .setPublisherName(getValue(Field.PUBLISHER_NAME, values))
            .setName(getValue(Field.PUBLICATION_TITLE, values))
                //.setVolume("")
                //.setYear("")
                //.setIssue("")
            .setStartVolume(getValue(Field.NUM_FIRST_VOL_ONLINE, values))
            .setEndVolume(getValue(Field.NUM_LAST_VOL_ONLINE, values))
            .setStartYear(getValue(Field.DATE_FIRST_ISSUE_ONLINE, values))
            .setEndYear(getValue(Field.DATE_LAST_ISSUE_ONLINE, values))
            .setStartIssue(getValue(Field.NUM_FIRST_ISSUE_ONLINE, values))
            .setEndIssue(getValue(Field.NUM_LAST_ISSUE_ONLINE, values))
            ;
      }
    }
  }

  //////////////////////////////////////////////////////////////////////////////
  // OPTIONS PROCESSING
  //////////////////////////////////////////////////////////////////////////////
  // Short names of options
  private static final String SOPT_HELP = "h";
  private static final String SOPT_HIDE_EMPTY_COLS = "e";
  private static final String SOPT_DATA = "d";
  private static final String SOPT_FILE = "i";
  private static final String SOPT_SHOW_STATUS = "s";

  private static final KbartExporter.OutputFormat defaultOutput =
      KbartExporter.OUTPUT_FORMAT_DEFAULT;

  private static final PredefinedFieldOrdering defaultFieldOrdering =
      KbartExportFilter.FIELD_ORDERING_DEFAULT;


  /**
   * Options spec.
   */
  private static Options options = new Options();

  /**
   * A list to track the desired order of options in usage output, as Apache
   * Commons doesn't use the order of addition by default.
   */
  private static List<Option> optionList = new ArrayList<Option>();

  /**
   * Add the given Option to the options order list as well as the options spec.
   * @param o an Option
   * @param setReq whether to set the option as a required one
   */
  private static void addOption(Option o, boolean setReq) {
    o.setRequired(setReq);
    optionList.add(o);
    options.addOption(o);
  }

  /**
   * Add the given Option to the options order list as well as the options spec,
   * as an optional Option.
   * @param o an Option
   */
  private static void addOption(Option o) {
    addOption(o, false);
  }

  /**
   * Add the given OptionGroup to the options spec, and its component Options
   * to the options order list. An OptionGroup contains mutually exclusive
   * Options. The selected/default option is set to the first in the list by
   * default.
   * @param og an OptionGroup
   */
  private static void addOptionGroup(OptionGroup og) {
    for (Object o : og.getOptions()) optionList.add((Option)o);
    options.addOptionGroup(og);
  }

  // Create all the options, add them to the spec and mark those that are required.
  static {
    // The input file option is required
    addOption(new Option(SOPT_FILE, "input-file", true, "Path to the input file"), true);
    // Help option
    addOption(new Option(SOPT_HELP, "help", false, "Show help"));
    // Option to hide empty cols
    addOption(new Option(SOPT_HIDE_EMPTY_COLS, "hide-empty-cols", false, "Hide output columns that are empty."));
    // Output data format - defines the fields and their ordering
    addOption(new Option(SOPT_DATA, "data-format", true, "Format of the output data records."));
    // Option to show TDB status // TODO Not yet available
    //addOption(new Option(SOPT_SHOW_STATUS, "show-tdb-status", false, "Show status field from TDB."));
  }

  private static void selectDefaultGroupOption(OptionGroup og, Option opt) {
    try {
      og.setSelected(opt);
    }
    catch (AlreadySelectedException e) {/*Don't care*/}
    catch (NoSuchElementException e) {
      System.err.format("The default option %s is not available.", opt);
      og.setRequired(true); // The user must specify
    }
  }


  /**
   * Print a message, and exit.
   * @param msg a message
   */
  private static void die(String msg) {
    System.err.println(msg);
    System.exit(1);
  }

  /**
   * Print a message with exception, show exception stack trace, and exit.
   * @param msg a message
   * @param e an exception
   */
  private static void die(String msg, Exception e) {
    System.err.format("%s %s\n", msg, e);
    e.printStackTrace();
    System.exit(1);
  }

  /**
   * Print a usage message.
   * @param error whether a parsing error provoked this usage display
   */
  private static void usage(boolean error) {
    HelpFormatter help = new HelpFormatter();
    // Set a comparator that will output the options in the order
    // they were specified above
    help.setOptionComparator(new Comparator<Option>() {
      public int compare(Option option, Option option1) {
        Integer i = optionList.indexOf(option);
        Integer i1 = optionList.indexOf(option1);
        return i.compareTo(i1);
      }
    });
    help.printHelp("RunKbartReport", options, true);
    if (error) {
      for (Object o : options.getRequiredOptions()) {
        System.err.format("Note that the -%s option is required\n", o);
      }
    }
    // Show defaults
    System.err.println("");
    //System.err.println("Default output format is "+defaultOutput);
    //System.err.println("Default data format is "+defaultFieldOrdering);

    // Print data format options
    System.err.format("Data format argument must be one of the following " +
        "identifiers (default %s):\n", defaultFieldOrdering.name());
    for (KbartExportFilter.PredefinedFieldOrdering ord :
        KbartExportFilter.PredefinedFieldOrdering.values()) {
      System.err.format("  %s (%s)\n", ord.name(), ord.description);
    }
    System.err.println("\nInput file should be UTF-8 encoded.\n");
    System.exit(0);
  }

  /**
   * Parse options and create an instance.
   * @param args
   */
  public static void main(String[] args) {
    // Try parsing the args
    CommandLine cl = null;
    try {
      cl = new GnuParser().parse(options, args);
    } catch (ParseException e) {
      //e.printStackTrace();
      System.err.println("Could not parse options");
      usage(true);
    }
    if (cl==null || cl.hasOption(SOPT_HELP)) usage(false);

    // Create an instance
    try {
      RunKbartReport report = new RunKbartReport(cl);
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }

}
