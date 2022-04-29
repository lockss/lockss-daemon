/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University,
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.tdb;

import java.io.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.commons.cli.*;
import org.lockss.tdb.AntlrUtil.SyntaxError;
import org.lockss.tdb.StrictInputStreamReader.MalformedInputRangeException;

public class TdbOut {

  /**
   * <p>
   * A version string for the TdbOut tool ({@value}).
   * </p>
   * 
   * @since 1.68
   */
  public static final String VERSION = "[TdbOut:0.3.1]";
  
  /**
   * <p>
   * Key for the style option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String KEY_STYLE = "style";
  
  /**
   * <p>
   * Single letter for the style option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final char LETTER_STYLE = 's';
  
  /**
   * <p>
   * The argument name for the style option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final String ARG_STYLE = KEY_STYLE.toUpperCase();
  
  /**
   * <p>
   * The CSV style for the style option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String STYLE_CSV = "csv";

  /**
   * <p>
   * The list style for the style option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String STYLE_LIST = "list";

  /**
   * <p>
   * The TSV style for the style option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String STYLE_TSV = "tsv";
  
  /**
   * <p>
   * The available choices for the style option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final List<String> CHOICES_STYLE =
      AppUtil.ul(STYLE_CSV,
                 STYLE_LIST,
                 STYLE_TSV);
  
  /**
   * <p>
   * The style option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final Option OPTION_STYLE =
      Option.builder(Character.toString(LETTER_STYLE))
            .longOpt(KEY_STYLE)
            .hasArg()
            .argName(ARG_STYLE)
            .desc(String.format("use output style %s %s", ARG_STYLE, CHOICES_STYLE))
            .build();

  /**
   * <p>
   * Key for the fields option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String KEY_FIELDS = "fields";

  /**
   * <p>
   * Single letter for the fields option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final char LETTER_FIELDS = 'f';

  /**
   * <p>
   * The argument name for the fields option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final String ARG_FIELDS = KEY_FIELDS.toUpperCase();

  /**
   * <p>
   * The fields option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final Option OPTION_FIELDS =
      Option.builder(Character.toString(LETTER_FIELDS))
            .longOpt(KEY_FIELDS)
            .hasArg()
            .argName(ARG_FIELDS)
            .desc("comma-separated list of fields to output")
            .build();
  
  /**
   * <p>
   * Key for the AUID option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String KEY_AUID = "auid";

  /**
   * <p>
   * Single letter for the AUID option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final char LETTER_AUID = 'a';

  /**
   * <p>
   * The AUID option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final Option OPTION_AUID =
      Option.builder(Character.toString(LETTER_AUID))
            .longOpt(KEY_AUID)
            .desc(String.format("short for --%s=%s --%s=auid", KEY_STYLE, STYLE_LIST, KEY_FIELDS))
            .build();
  
  /**
   * <p>
   * Key for the AUIDplus option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String KEY_AUIDPLUS = "auidplus";

  /**
   * <p>
   * Single letter for the AUIDplus option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final char LETTER_AUIDPLUS = 'p';
  
  /**
   * <p>
   * The AUIDplus option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final Option OPTION_AUIDPLUS =
      Option.builder(Character.toString(LETTER_AUIDPLUS))
            .longOpt(KEY_AUIDPLUS)
            .desc(String.format("short for --%s=%s --%s=auidplus", KEY_STYLE, STYLE_LIST, KEY_FIELDS))
            .build();
  
  /**
   * <p>
   * Key for the count option ({@value}).
   * </p>
   * 
   * @since 1.68
   */
  protected static final String KEY_COUNT = "count";
  
  /**
   * <p>
   * Single letter for the count option ({@value}).
   * </p>
   * 
   * @since 1.68
   */
  protected static final char LETTER_COUNT = 'n';

  /**
   * <p>
   * The count option.
   * </p>
   * 
   * @since 1.68
   */
  protected static final Option OPTION_COUNT =
      Option.builder(Character.toString(LETTER_COUNT))
            .longOpt(KEY_COUNT)
            .desc("print a count of matching AUs")
            .build();
  
  /**
   * <p>
   * Key for the CSV option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String KEY_CSV = "csv";
  
  /**
   * <p>
   * Single letter for the CSV option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final char LETTER_CSV = 'c';

  /**
   * <p>
   * The argument name for the CSV option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final String ARG_CSV = ARG_FIELDS;
  
  /**
   * <p>
   * The CSV option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final Option OPTION_CSV =
      Option.builder(Character.toString(LETTER_CSV))
            .longOpt(KEY_CSV)
            .hasArg()
            .argName(ARG_CSV)
            .desc(String.format("short for --%s=%s --%s=%s", KEY_STYLE, STYLE_CSV, KEY_FIELDS, ARG_CSV))
            .build();
  
  /**
   * <p>
   * Key for the journals option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String KEY_JOURNALS = "journals";

  /**
   * <p>
   * Single letter for the journals option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final char LETTER_JOURNALS = 'j';

  /**
   * <p>
   * The journals option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final Option OPTION_JOURNALS =
      Option.builder(Character.toString(LETTER_JOURNALS))
            .longOpt(KEY_JOURNALS)
            .desc("iterate over titles (not AUs) and output a CSV list of publishers, titles, ISSNs and eISSNs")
            .build();
  
  /**
   * <p>
   * Key for the list option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String KEY_LIST = "list";

  /**
   * <p>
   * Single letter for the list option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final char LETTER_LIST = 'l';
  
  /**
   * <p>
   * The argument name for the list option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final String ARG_LIST = ARG_FIELDS.substring(0, ARG_FIELDS.length() - 1);
  
  /**
   * <p>
   * The list option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final Option OPTION_LIST =
      Option.builder(Character.toString(LETTER_LIST))
            .longOpt(KEY_LIST)
            .hasArg()
            .argName(ARG_LIST)
            .desc(String.format("short for --%s=%s --%s=%s", KEY_STYLE, STYLE_LIST, KEY_FIELDS, ARG_LIST))
            .build();
  
  /**
   * <p>
   * Key for the TSV option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String KEY_TSV = "tsv";
  
  /**
   * <p>
   * Single letter for the TSV option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final char LETTER_TSV = 't';

  /**
   * <p>
   * The argument name for the TSV option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final String ARG_TSV = KEY_FIELDS.toUpperCase();
  
  /**
   * <p>
   * The TSV option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final Option OPTION_TSV =
      Option.builder(Character.toString(LETTER_TSV))
            .longOpt(KEY_TSV)
            .hasArg()
            .argName(ARG_TSV)
            .desc(String.format("short for --%s=%s --%s=%s", KEY_STYLE, STYLE_TSV, KEY_FIELDS, ARG_TSV))
            .build();
  
  /**
   * <p>
   * Key for the journal type option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String KEY_TYPE_JOURNAL = "type-journal";

  /**
   * <p>
   * The journal type option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final Option OPTION_TYPE_JOURNAL =
      Option.builder()
            .longOpt(KEY_TYPE_JOURNAL)
            .desc(String.format("with --%s, output only titles of type '%s'", KEY_JOURNALS, Title.TYPE_JOURNAL))
            .build();
  
  /**
   * <p>
   * A set of options keys; the corresponding options define mutually exclusive
   * actions.
   * </p>
   * 
   * @since 1.67
   */
  protected static final List<String> mutuallyExclusiveActions =
      AppUtil.ul(KEY_AUID,
                 KEY_AUIDPLUS,
                 KEY_COUNT,
                 KEY_CSV,
                 KEY_JOURNALS,
                 KEY_LIST,
                 KEY_STYLE,
                 KEY_TSV);

  /**
   * <p>
   * A set of options keys; the corresponding options specify their own fields,
   * so are mutually exclusive with the fields options.
   * </p>
   * 
   * @since 1.67
   */
  protected static final List<String> mutuallyExclusiveFields =
      AppUtil.ul(KEY_AUID,
                 KEY_AUIDPLUS,
                 KEY_CSV,
                 KEY_FIELDS,
                 KEY_LIST,
                 KEY_TSV);

  /**
   * <p>
   * A TDB builder.
   * </p>
   * 
   * @since 1.67
   */
  protected TdbBuilder tdbBuilder;
  
  /**
   * <p>
   * A TDB query builder.
   * </p>
   * 
   * @since 1.67
   */
  protected TdbQueryBuilder tdbQueryBuilder;
  
  public TdbOut() {
    this.tdbBuilder = new TdbBuilder();
    this.tdbQueryBuilder = new TdbQueryBuilder();
  }
  
  /**
   * <p>
   * Determines from the options map if the count option was requested.
   * </p>
   * 
   * @param options
   *          An options map.
   * @return Whether the count option has been requested.
   * @since 1.68
   */
  public boolean getCount(Map<String, Object> options) {
    return ((Boolean)options.get(KEY_COUNT)).booleanValue();
  }
  
  /**
   * <p>
   * Determines from the options map the output style.
   * </p>
   * 
   * @param options
   *          An options map.
   * @return The output style -- either {@Link #STYLE_CSV} or
   *         {@link #STYLE_TSV}.
   * @since 1.67
   */
  public String getStyle(Map<String, Object> options) {
    return (String)options.get(KEY_STYLE);
  }
  
  /**
   * <p>
   * Determines from the options map the output fields.
   * </p>
   * 
   * @param options
   *          An options map.
   * @return The output fields.
   * @since 1.67
   */
  public List<String> getFields(Map<String, Object> options) {
    return (List<String>)options.get(KEY_FIELDS);
  }
  
  public Tdb processInputData(Map<String, Object> options)
      throws FileNotFoundException, IOException {
    Tdb tdb = null;
    InputStream is = null;
    String f = InputData.get(options);
    try {
      is = "-".equals(f) ? System.in : new FileInputStream(f);
      tdb = TdbParse.readTdb(is);
    }
    catch (FileNotFoundException fnfe) {
      AppUtil.error(options, fnfe, "%s: file not found", f);
    }
    finally {
      is.close();
    }
    return tdb;
  }
  
  /**
   * <p>
   * Parses the TDB files listed in the options map.
   * </p>
   * 
   * @param options
   *          The options map.
   * @return A parsed {@link Tdb} structure.
   * @throws IOException
   *           if any I/O exceptions occur.
   * @since 1.67
   */
  public Tdb processFiles(Map<String, Object> options) throws IOException {
    List<String> inputFiles = InputOption.getInput(options);
    for (String f : inputFiles) {
      try {
        if ("-".equals(f)) {
          f = "<stdin>";
          tdbBuilder.parse(f, System.in, StandardCharsets.UTF_8);
        }
        else {
          tdbBuilder.parse(f, StandardCharsets.UTF_8);
        }
      }
      catch (FileNotFoundException fnfe) {
        AppUtil.warning(options, fnfe, "%s: file not found", f);
        KeepGoing.addError(options, fnfe);
      }
      catch (MalformedInputException mie) {
        AppUtil.warning(options, mie, "%s: %s", f, mie.getMessage());
        KeepGoing.addError(options, mie);
      }
      catch (IOException ioe) {
        AppUtil.warning(options, ioe, "%s: I/O error", f);
        KeepGoing.addError(options, ioe);
      }
      catch (SyntaxError se) {
        AppUtil.warning(options, se, se.getMessage());
        KeepGoing.addError(options, se);
      }
    }
    
    List<Exception> errors = KeepGoing.getErrors(options);
    int errs = errors.size();
    if (KeepGoing.isKeepGoing(options) && errs > 0) {
      AppUtil.error(options, errors, "Encountered %d %s; exiting", errs, errs == 1 ? "error" : "errors");
    }
    return tdbBuilder.getTdb();
  }
  
  /**
   * <p>
   * Produces output from a parsed TDB.
   * </p>
   * 
   * @param options
   *          The options map.
   * @param tdb
   *          A TDB structure.
   * @since 1.67
   */
  public void produceOutput(Map<String, Object> options, Tdb tdb) {
    PrintStream out = OutputOption.getSingleOutput(options);
    Predicate<Au> auPredicate = tdbQueryBuilder.getAuPredicate(options);
    boolean csv = STYLE_CSV.equals(getStyle(options));
    boolean count = getCount(options);
    
    List<Functor<Au, String>> traitFunctors = new ArrayList<Functor<Au, String>>();
    if (!count) {
      for (String field : getFields(options)) {
        Functor<Au, String> traitFunctor = Au.traitFunctor(field);
        if (traitFunctor == null) {
          AppUtil.error("Unknown field '%s'", field);
        }
        traitFunctors.add(traitFunctor);
      }
    }
    
    int counter = 0;
    for (Au au : tdb.getAus()) {
      if (auPredicate.test(au)) {
        ++counter;
        if (count) {
          continue;
        }
      }
      else {
        continue;
      }
      boolean first = true;
      StringBuilder sb = new StringBuilder(1024);
      for (Functor<Au, String> traitFunctor : traitFunctors) {
        if (!first) {
          sb.append(csv ? ',' : '\t');
        }
        String output = traitFunctor.apply(au);
        if (output != null) {
          sb.append(csv ? csvValue(output) : output);
        }
        first = false;
      }
      out.println(sb.toString());
    }
    
    if (count) {
      out.println(counter);  
    }
    
    out.close();
  }
  
  /**
   * <p>
   * Produces output when the journals option is requested on the command line.
   * </p>
   * 
   * @param options
   *          The options map.
   * @param tdb
   *          A TDB structure.
   * @since 1.67
   */
  public void produceJournals(Map<String, Object> options, Tdb tdb) {
    PrintStream out = OutputOption.getSingleOutput(options);
    boolean typeJournal = KEY_TYPE_JOURNAL.equals(options.get(KEY_JOURNALS));
    for (Title title : tdb.getTitles()) {
      if (typeJournal && !Title.TYPE_JOURNAL.equals(title.getType())) {
        continue;
      }
      String titleStr = title.getIssn();
      String eissnStr = title.getEissn();
      out.println(String.format("%s,%s,%s,%s",
                                csvValue(title.getPublisher().getName()),
                                csvValue(title.getName()),
                                csvValue((titleStr == null) ? "" : titleStr),
                                csvValue((eissnStr == null) ? "" : eissnStr)));
    }
    out.close();
  }
  
  /**
   * <p>
   * Add this module's options to a Commons CLI {@link Options} instance.
   * </p>
   * 
   * @param options
   *          A Commons CLI {@link Options} instance.
   * @since 1.67
   */
  public void addOptions(Options options) {
    // Options from other modules
    options.addOption(Help.option()); // --help
    InputOption.addOptions(options); // --input
    options.addOption(InputData.option()); // --input-data
    options.addOption(KeepGoing.option()); // --keep-going
    OutputOption.addOptions(options); // --output
    options.addOption(Verbose.option()); // --verbose
    options.addOption(Version.option()); // --version
    tdbQueryBuilder.addOptions(options);
    // Own options
    options.addOption(OPTION_AUID);
    options.addOption(OPTION_AUIDPLUS);
    options.addOption(OPTION_COUNT);
    options.addOption(OPTION_CSV);
    options.addOption(OPTION_FIELDS);
    options.addOption(OPTION_JOURNALS);
    options.addOption(OPTION_LIST);
    options.addOption(OPTION_STYLE);
    options.addOption(OPTION_TSV);
    options.addOption(OPTION_TYPE_JOURNAL);
  }
  
  /**
   * <p>
   * Processes a {@link CommandLineAccessor} instance and stores appropriate
   * information in the given options map.
   * </p>
   * 
   * @param options
   *          An options map.
   * @param cmd
   *          A {@link CommandLineAccessor} instance.
   * @since 1.67
   */
  public Map<String, Object> processCommandLine(CommandLineAccessor cmd) {
    Map<String, Object> options = new HashMap<String, Object>();
    // Help already processed
    Version.parse(cmd, VERSION, TdbBuilder.VERSION, TdbQueryBuilder.VERSION); // may exit

    InputOption.processCommandLine(options, cmd);
    InputData.parse(options, cmd);
    if (InputOption.getInput(options) == null && InputData.get(options) == null) {
      AppUtil.error("No input files specified");
    }
    if (InputOption.getInput(options) != null && InputData.get(options) != null) {
      AppUtil.error("--%s cannot be used with a list of input files", InputOption.KEY_INPUT);
    }
    
    KeepGoing.parse(options, cmd);
    OutputOption.processCommandLine(options, cmd);
    Verbose.parse(options, cmd);
    tdbQueryBuilder.processCommandLine(options, cmd);

    int actions = count(cmd, mutuallyExclusiveActions);
    if (actions == 0) {
      AppUtil.error("Specify an action among %s", mutuallyExclusiveActions);
    }
    if (actions > 1) {
      AppUtil.error("Specify only one action among %s", mutuallyExclusiveActions);
    }
    if (count(cmd, mutuallyExclusiveFields) > 1) {
      AppUtil.error("Specify only one option among %s", mutuallyExclusiveFields);
    }
    
    if (cmd.hasOption(KEY_JOURNALS)) {
      options.put(KEY_JOURNALS, KEY_JOURNALS);
    }
    if (cmd.hasOption(KEY_TYPE_JOURNAL)) {
      if (!options.containsKey(KEY_JOURNALS)) {
        AppUtil.error("--%s can only be used with --%s", KEY_TYPE_JOURNAL, KEY_JOURNALS);
      }
      options.put(KEY_JOURNALS, KEY_TYPE_JOURNAL);
    }
    
    if (cmd.hasOption(KEY_AUID)) {
      options.put(KEY_STYLE, STYLE_TSV);
      options.put(KEY_FIELDS, Arrays.asList("auid"));
    }
    if (cmd.hasOption(KEY_AUIDPLUS)) {
      options.put(KEY_STYLE, STYLE_TSV);
      options.put(KEY_FIELDS, Arrays.asList("auidplus"));
    }
    options.put(KEY_COUNT, Boolean.valueOf(cmd.hasOption(KEY_COUNT)));
    if (cmd.hasOption(KEY_CSV)) {
      options.put(KEY_STYLE, STYLE_CSV);
      options.put(KEY_FIELDS, Arrays.asList(cmd.getOptionValue(KEY_CSV).split(",")));
    }
    if (cmd.hasOption(KEY_LIST)) {
      options.put(KEY_STYLE, STYLE_TSV);
      options.put(KEY_FIELDS, Arrays.asList(cmd.getOptionValue(KEY_LIST)));
    }
    if (cmd.hasOption(KEY_TSV)) {
      options.put(KEY_STYLE, STYLE_TSV);
      options.put(KEY_FIELDS, Arrays.asList(cmd.getOptionValue(KEY_TSV).split(",")));
    }
    if (cmd.hasOption(KEY_STYLE)) {
      String style = cmd.getOptionValue(KEY_STYLE);
      if (!CHOICES_STYLE.contains(style)) {
        AppUtil.error("Invalid style '%s'; must be among %s", style, CHOICES_STYLE);
      }
      options.put(KEY_STYLE, STYLE_CSV.equals(style) ? STYLE_CSV : STYLE_TSV);
    }
    if (cmd.hasOption(KEY_FIELDS)) {
      options.put(KEY_FIELDS, Arrays.asList(cmd.getOptionValue(KEY_FIELDS).split(",")));
    }

    if (getStyle(options) != null) {
      List<String> fields = getFields(options);
      if (fields == null || fields.size() == 0) {
        AppUtil.error("No output fields specified");
      }
    }
    
    return options;
  }

  /**
   * <p>
   * Secondary entry point of this class, after the command line has been
   * parsed.
   * </p>
   * 
   * @param cmd
   *          A parsed command line.
   * @throws IOException
   *           if an I/O error occurs.
   * @since 1.67
   */
  public void run(Map<String, Object> options) throws IOException {
    Tdb tdb = InputData.get(options) == null ? processFiles(options) : processInputData(options);
    if (options.containsKey(KEY_JOURNALS)) {
      produceJournals(options, tdb);
    }
    else {
      produceOutput(options, tdb);
    }
  }

  /**
   * <p>
   * Primary entry point of this class, before the command line has been parsed.
   * </p>
   * 
   * @param mainArgs
   *          Command line arguments.
   * @throws Exception if any error occurs.
   * @since 1.67
   */
  public void run(String[] mainArgs) throws Exception {
    Options opts = new Options();
    addOptions(opts);
    CommandLineAccessor cmd = new CommandLineAdapter(new DefaultParser().parse(opts, mainArgs));
    Help.parse(cmd, opts, getClass());
    Map<String, Object> options = processCommandLine(cmd);
    run(options);
  }
  
  /**
   * <p>
   * Counts how many of the strings in a given set appear in a
   * {@link CommandLineAccessor} instance.
   * </p>
   * 
   * @param cmd
   *          A {@link CommandLineAccessor} instance.
   * @param optionStrings
   *          The set of strings to be counted in the options.
   * @return The number of strings from the set that appeared on the command
   *         line.
   * @since 1.67
   */
  protected static int count(CommandLineAccessor cmd, List<String> optionStrings) {
    int c = 0;
    for (String optionString : optionStrings) {
      if (cmd.hasOption(optionString)) {
        ++c;
      }
    }
    return c;
  }
  
  /**
   * <p>
   * Returns a string suitable to be used as a CSV value (quoted if it contains
   * a comma or quotation mark, and with quotation marks doubled).
   * </p>
   * 
   * @param str
   *          A plain string.
   * @return A CSV-encoded string.
   * @since 1.67
   */
  protected static String csvValue(String str) {
    int i1 = str.indexOf('"');
    int i2 = str.indexOf(',');
    if (i1 < 0 && i2 < 0) {
      return str;
    }
    else {
      return "\"" + str.replace("\"", "\"\"") + "\"";
    }
  }
  
  /**
   * <p>
   * Creates a {@link TdbOut} instance and calls {@link #run(String[])}.
   * </p>
   * 
   * @param args
   *          Command line arguments.
   * @throws Exception
   *           if any error occurs.
   * @since 1.67
   */
  public static void main(String[] args) throws Exception {
    new TdbOut().run(args);
  }

}
