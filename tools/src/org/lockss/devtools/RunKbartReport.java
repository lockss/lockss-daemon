/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.lockss.exporter.biblio.BibliographicItem;
import org.lockss.exporter.biblio.BibliographicItemImpl;
import org.lockss.exporter.biblio.BibliographicUtil;
import org.lockss.exporter.kbart.KbartConverter;
import org.lockss.exporter.kbart.KbartExportFilter;
import static org.lockss.exporter.kbart.KbartExportFilter.*;
import org.lockss.exporter.kbart.KbartExporter;
import org.lockss.exporter.kbart.KbartTitle;
import org.lockss.exporter.kbart.KbartTitle.Field;
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

  // Alternative names for non-KBART fields in the input file.
  /** Case-insensitive name for a volume string. */
  protected static final String VOLUME_STR = "volume";
  /** Case-insensitive name for a year/date string. */
  protected static final String YEAR_STR = "year";
  /** Case-insensitive name for a issue string. */
  protected static final String ISSUE_STR = "issue";
  /** Case-insensitive name for an ISSN-L string. */
  protected static final String ISSNL_STR = "issnl";
  /** Array of the non-KBART field names. */
  protected static final String[] nonKbartFields =
      {VOLUME_STR, YEAR_STR, ISSUE_STR, ISSNL_STR};
  /** Array of the possible fields for volume string. */
  protected static final String[] volFields =
      {VOLUME_STR, "param[volume]", "param[volume_name]", "param[volume_str]"};
  /** Array of the possible fields for year string. */
  protected static final String[] yrFields =
      {YEAR_STR, "param[year]"};
  /** Array of the possible fields for issue string. */
  protected static final String[] issFields =
      {ISSUE_STR, "param[num_issue_range]", "param[issue_set]", 
          "param[issue_no]", "param[issues]", "param[issue_no.]", 
          "param[issue_dir]"};

  static public enum PubType {
    book,
    journal
  };
  
  /** An input stream providing the CSV input data. */
  private final InputStream inputStream;
  // Settings from the command line
  private final boolean hideEmptyColumns;
  private final boolean showTdbStatus;
  private KbartExportFilter.ColumnOrdering columnOrdering;
  private final PubType publicationType;

  //private static final String DATA_FORMATS_CONFIG_FILE = "data-formats.xml";
  /*  data-formats.xml
  org.lockss.exporter.kbart.Data-Format
  format @name
      display-name
      desc
      cols
  */
  /*private static List<KbartExportFilter.ColumnOrdering> dataFormats;
  static {
    InputStream file = ClassLoader.getSystemClassLoader().getResourceAsStream(DATA_FORMATS_CONFIG_FILE);
  }*/
  /*private static List<KbartExportFilter.ColumnOrdering> dataFormats =
      new ArrayList<KbartExportFilter.ColumnOrdering>() {{
        KbartExportFilter.PredefinedColumnOrdering.values();
      }};*/

  // ---------------------------------------------------------------------------

  /**
   * Construct an instance of this class with the supplied properties.
   * @param publicationType
   * @param hideEmptyColumns
   * @param showTdbStatus
   * @param columnOrdering
   * @param inputStream
   * @param outputStream
   */
  protected RunKbartReport(PubType publicationType,
                           boolean hideEmptyColumns,
                           boolean showTdbStatus,
                           PredefinedColumnOrdering columnOrdering,
                           InputStream inputStream, OutputStream outputStream) {
    this.publicationType = publicationType;
    this.hideEmptyColumns = hideEmptyColumns;
    this.showTdbStatus = showTdbStatus;
    this.inputStream = inputStream;
    this.columnOrdering = columnOrdering;

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
          new KbartCsvTitleIterator(inputStream, publicationType)
      );
    } catch (Exception e) {
      die("Could not read CSV file. "+e.getMessage(), e);
    }
    System.err.println(titles.size()+" KbartTitles for export");

    // Create a filter
    KbartExportFilter filter = new KbartExportFilter(titles, columnOrdering,
        hideEmptyColumns, false, false);

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
    protected KbartCsvTitleIterator(
        InputStream inputStream, PubType publicationType)
        throws IOException, IllegalArgumentException {
      this.recordIterator = new KbartCsvIterator(inputStream, publicationType);
      if (recordIterator.hasNext()) {
        this.nextItem = recordIterator.next();
      }
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
    protected final BibliographicItemFieldMapping mapping;
    protected final PubType publicationType;

    /**
     * Create an iterator on a CSV file, which returns a BibliographicItem for
     * each line.
     * @param inputStream
     * @param publicationType
     * @throws IOException
     * @throws RuntimeException
     */
    protected KbartCsvIterator(InputStream inputStream, PubType publicationType)
        throws IOException, RuntimeException {

      this.publicationType = publicationType;
      this.csvReader = new CsvReader(inputStream,
          Charset.forName("utf-8"));
      // Read first line as header line
      csvReader.readHeaders();
      // If the first line is not an appropriate list of field names
      // for the mapping, an exception is thrown
      this.mapping = new BibliographicItemFieldMapping(csvReader.getHeaders());

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
     * A mapping of BibliographicItem fields to their column positions in the
     * input file. Looks for standard KBART field names, and also combined 
     * range strings for volume, year and issue.
     */
    protected class BibliographicItemFieldMapping {
      /** Map of KBART Fields to field positions. */
      private final Map<Field, Integer> kbartFieldPositions;
      /** Map of alternative field names to field positions. */
      private final Map<String, Integer> otherFieldPositions;
      protected final boolean containsIdField;
      protected final boolean containsRangeField;

      /**
       * Takes an array of header labels, and
       * @param header an array of header strings
       */
      BibliographicItemFieldMapping(String[] header) {
        this.kbartFieldPositions = new HashMap<Field, Integer>();
        this.otherFieldPositions = new HashMap<String, Integer>();
        // Map field names that match KBART names, to their position
        for (int i=0; i< header.length; i++) {
          String s = header[i];
          try {
            Field field = Field.valueOf(s.toUpperCase());
            kbartFieldPositions.put(field, i);
          } catch (Exception e) {
            // No such KBART field; map the field name to a position
            otherFieldPositions.put(s.toLowerCase(), i);
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
       * Get the value of the field from the array, using the mapping.
       * @param f the field to find
       * @param values a list of values matching the field mapping
       * @return the mapped value of the field, or empty string if no such field or it is empty
       */
      public String getValue(Field f, String[] values) {
        try {
          //return fieldPositions.containsKey(f) ? values[fieldPositions.get(f)] : null;
          String s = values[kbartFieldPositions.get(f)];
          // Return empty string if the string is empty or null
          return StringUtil.isNullString(s) ? "" : s;
        } catch (Exception e) {
          return "";
        }
      }

      /**
       * Get the value of the field from the array, using the mapping.
       * @param name the name of the field to find
       * @param values a list of values matching the field mapping
       * @return the mapped value of the field, or <tt>null</tt> if no such field or it is empty
       */
      public String getValue(String name, String[] values) {
        try {
          String s = values[otherFieldPositions.get(name)];
          // Return null if the string is empty
          return StringUtil.isNullString(s) ? null : s;
        } catch (Exception e) {
          return null;
        }
      }

      /**
       * Try and find a field value from one of the several possible field names
       * enumerated in the candidates. The first non-null non-empty value is
       * returned.
       * @param values
       * @return
       */
      public String findValue(String[] values, String[] candidates) {
        for (String f : candidates) {
          String val = getValue(f, values);
          if (val!=null) return val;
        }
        return null;
      }

      private boolean containsIdField() {
        for (Field f : Field.idFields) {
          if (kbartFieldPositions.keySet().contains(f)) return true;
        }
        return false;
      }

      private boolean containsRangeField() {
        for (Field f : Field.rangeFields) {
          if (kbartFieldPositions.keySet().contains(f)) return true;
        }
        return false;
      }

      /**
       * Create a BibliographicItem with field values taken from the value set
       * supplied. Uses BibliographicItemImpl to take advantage of
       * its basic functionality, in particular the getIssn() implementation.
       * @param values an array of mapped String values
       * @return
       */
      public BibliographicItem getBibliographicItem(final String[] values) {
        return new BibliographicItemImpl() {
          @Override
          public String toString() {
            String pubIdentifier = getIsbn();
            if (StringUtil.isNullString(pubIdentifier)) {
              pubIdentifier = getIssn();
            }
            return String.format("BibliographicItem %s %s", 
                                 pubIdentifier, getPublicationTitle());
          }
        }
        .setPrintIsbn(getValue(Field.PRINT_IDENTIFIER, values))
        .setEisbn(getValue(Field.ONLINE_IDENTIFIER, values))
        .setPrintIssn(getValue(Field.PRINT_IDENTIFIER, values))
        .setEissn(getValue(Field.ONLINE_IDENTIFIER, values))
        .setIssnL(getValue(ISSNL_STR, values))
        .setPublicationTitle(getValue(Field.PUBLICATION_TITLE, values))
        .setPublisherName(getValue(Field.PUBLISHER_NAME, values))
        .setName(getValue("name", values))  // not standard KBART field
        .setStartVolume(getValue(Field.NUM_FIRST_VOL_ONLINE, values))
        .setEndVolume(getValue(Field.NUM_LAST_VOL_ONLINE, values))
        .setStartYear(getValue(Field.DATE_FIRST_ISSUE_ONLINE, values))
        .setEndYear(getValue(Field.DATE_LAST_ISSUE_ONLINE, values))
        .setStartIssue(getValue(Field.NUM_FIRST_ISSUE_ONLINE, values))
        .setEndIssue(getValue(Field.NUM_LAST_ISSUE_ONLINE, values))
        .setCoverageDepth(getValue(Field.COVERAGE_DEPTH, values))
        // Set volume/year/issue strings last - if they are non-null, they
        // will be used to set the start and end values too, overriding
        // what might have been set earlier in set[Start|End]*
        .setVolume(findValue(values, volFields))
        .setYear(findValue(values, yrFields))
        .setIssue(findValue(values, issFields))
        .setPublicationType(publicationType.toString());
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
  private static final String FOR_JOURNALS = "J";
  private static final String FOR_BOOKS = "B";

  private static final KbartExporter.OutputFormat defaultOutput =
      KbartExporter.OUTPUT_FORMAT_DEFAULT;

  private static final PredefinedColumnOrdering DEFAULT_COLUMN_ORDERING =
      KbartExportFilter.COLUMN_ORDERING_DEFAULT;


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
   * Add the given OptionGroup to the options order list as well as the 
   * options spec.
   * @param g an OptionGroup
   * @param setReq whether to set the option as a required one
   */
  private static void addOptionGroup(OptionGroup g, boolean setReq) {
    g.setRequired(setReq);
    for (Object opt : g.getOptions()) {
      optionList.add((Option)opt);
    }
    options.addOptionGroup(g);
  }

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
    // options for generating MD file
    OptionGroup reportTypeGroup = new OptionGroup()
      .addOption(OptionBuilder
        .withDescription("for books")
        .withLongOpt("books")
         .create(FOR_BOOKS))
      .addOption(OptionBuilder
        .withDescription("for journals")
        .withLongOpt("journals")
        .create(FOR_JOURNALS));
    addOptionGroup(reportTypeGroup, true);
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
    // Print blank line
    System.err.println();
    help.printHelp("RunKbartReport", options, true);
    if (error) {
      for (Object o : options.getRequiredOptions()) {
        System.err.format("Note that the -%s option is required\n", o);
      }
    }
    // Show defaults
    System.err.println("");
    //System.err.println("Default output format is "+defaultOutput);
    //System.err.println("Default data format is "+DEFAULT_COLUMN_ORDERING);

    // Print data format options
    System.err.format("Data format argument must be one of the following " +
        "identifiers (default %s):\n", DEFAULT_COLUMN_ORDERING.name());
    for (PredefinedColumnOrdering ord : PredefinedColumnOrdering.values()) {
      System.err.format("  %s (%s)\n", ord.name(), ord.description);
    }
    System.err.format("\nInput file should be UTF-8 encoded and include a " +
        "header row with field names matching KBART field names or any of the " +
        "following: %s.\n\n", StringUtils.join(nonKbartFields, ", "));
    System.exit(0);
  }

  /**
   * Parse options and create an instance.
   * See new KbartExportFilter(titles) for default options.
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

    // Determine the ordering for the output data
    PredefinedColumnOrdering ordering;
    try {
      ordering = PredefinedColumnOrdering.valueOf(
          cl.getOptionValue(SOPT_DATA)
      );
    } catch (Exception e) {
      ordering = DEFAULT_COLUMN_ORDERING;
    }

    
    PubType pubType = null;
    if (cl.hasOption(FOR_JOURNALS)) pubType = PubType.journal;
    if (cl.hasOption(FOR_BOOKS)) pubType = PubType.book;
    
    // Create an instance
    try {
      // input from named file or stdin if "-" specified
      String f = cl.getOptionValue(SOPT_FILE);
      InputStream in = "-".equals(f) ? System.in : new FileInputStream(f);
      new RunKbartReport(
          pubType,
          cl.hasOption(SOPT_HIDE_EMPTY_COLS),
          cl.hasOption(SOPT_SHOW_STATUS),
          ordering,
          in,
          System.out
      );
    } catch (Exception e) {
      System.err.println("Could not create RunKbartReport: "+e.getMessage());
    }
  }


}
