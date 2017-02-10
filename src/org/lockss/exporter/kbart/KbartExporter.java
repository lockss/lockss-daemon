/*
 * $Id$
 */

/*

Copyright (c) 2010-2012 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.lockss.config.ConfigManager;
import org.lockss.config.TdbUtil.ContentScope;
import org.lockss.exporter.kbart.KbartTitle.Field;

import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import org.mortbay.html.Composite;


/**
 * Export the metadata from the LOCKSS holdings according to
 * <emph>KBART Phase I Recommended Practice</emph> document NISO-RP-9-2010.
 * The document suggests that the output metadata file should have its entries
 * ordered alphabetically by title (5.3.1.11) and be in UTF-8.
 * <p>
 * This class handles the plumbing of output streams, and any 
 * required compression, while formatting of the exported records is performed 
 * by the implementation subclass. A PrintWriter is used for output and is set 
 * to automatically flush the streams after a <code>println()</code> call is 
 * made. Additionally some methods explicitly call flush(), particularly at the 
 * end of the export. 
 * <p>
 * Instances of this class are intended to be used once, for a single export,
 * and only by a single thread.
 *
 * <h3>Note: Iteration</h3>
 * The exporter cannot accept an iterator instead of a list, because
 * it needs to sort KbartTitles alphabetically on title as per KBART Phase I
 * recommendation 5.3.1.11. It also produces summary information from analysis
 * of the entire list of KbartTitles.
 * <p>
 * Using an iterator instead of a list would be less memory intensive, and
 * would require the input list to be ordered.
 *
 * @author Neil Mayo
 */
public abstract class KbartExporter {

  private static Logger log = Logger.getLogger("KbartExporter");

  // Footnotes for the interface options
  private static final String CSV_NOTE = "CSV (comma-separated values) files " +
      "can be imported into spreadsheet programs.";
  private static final String TSV_NOTE = "TSV (tab-separated values) files " +
      "can be imported into spreadsheet programs.";

  //private static final String TSV_NOTE = "The TSV export values are quoted "+
  //"where necessary, and quotes within values are escaped.";
  
  private static final String HTML_NOTE =  "Allows on-screen inspection "+
  "of the title list.";

  public static final OutputFormat OUTPUT_FORMAT_DEFAULT = OutputFormat.CSV;

  /** 
   * Explanation of why some KBART records become duplicates with custom field 
   * orderings, and thus get omitted. 
   */
  protected static final String duplicatesExplanation = "The chosen combination " +
  "of fields does not result in a unique tuple for every record.";

  /** Record of errors for the caller. */
  List<String> errors = new ArrayList<String>();

  /** 
   * The total number of TdbTitle objects used as input to the export process. 
   * Only included as a courtesy for use in output display. 
   */
  protected int tdbTitleTotal;
  /** The scope of the export. */
  protected ContentScope scope;
  
  /** The list of KBART format titles to export. */
  protected final List<KbartTitle> titles;

  /** An export filter for the exporter. */
  protected KbartExportFilter filter;
  
  /** Whether to compress the output. */
  private boolean compress = false;

  /** The OutputFormat of this exporter. */
  protected OutputFormat outputFormat;

  /** A PrintWriter which will be used to emit formatted output. */
  protected PrintWriter printWriter;

  /** A compression stream for this exporter (optional). */
  private ZipOutputStream zip; 
  
  /** A filename for output. This is generated when needed, which may be never. */
  private String filename;
  
  /**
   * An optional HTML form containing customisation options. This option is 
   * meaningless to non-HTML outputs. It is recorded as a Composite so it can
   * in fact contain a variety of forms.
   */
  private Composite customForm;

  /** A count of how many records have been exported. */
  protected int exportCount = 0;
  /** 
   * A record of how many records were excluded because they were duplicates 
   * under the combination of fields specified in the filter's ordering. 
   */
  protected int duplicateCount = 0;
  
  /** 
   * By default, we don't want to exclude empty fields as it will contravene 
   * KBART.
   */
  public static final boolean omitEmptyFieldsByDefault = false;
  /**
   * By default, we don't show health ratings as they are non-KBART.
   */
  public static final boolean showHealthRatingsByDefault = false;
  /**
   * By default, we show the header row.
   */
  public static final boolean omitHeaderRowByDefault = false;

  /** By default, we don't exclude titles with no identifier. */
  public static boolean excludeNoIdTitlesByDefault = false;

  /** Default encoding for output. */
  public static final String DEFAULT_ENCODING = "UTF-8";
  /** Whether to auto flush the writer streams. */
  protected static final boolean AUTO_FLUSH = true;

  /**
   * Default constructor takes a list of KbartTitle objects to be exported.
   * Creates an export filter and sorts the titles. Due to this sorting,
   * it is not possible to accept an iterator instead of a list, which would
   * be less memory intensive.
   *
   * @param titles the list of titles which are to be exported
   * @param format the OutputFormat
   */
  public KbartExporter(List<KbartTitle> titles, OutputFormat format) {
    this.titles = titles;
    this.outputFormat = format;
    this.initExporter();
    // Create an identity filter by default
    this.filter = KbartExportFilter.identityFilter(titles);
    // KBART info should be ordered alphabetically by title by default
    Collections.sort(titles,
        KbartTitleComparatorFactory.getComparator(Field.PUBLICATION_TITLE)
    );
  }

  /**
   * Initialise the exporter with properties it inherits from the output format.
   */
  private void initExporter() {
    this.setCompress(outputFormat.isCompressible());
  }

  
  /**
   * Set the filter to be used on this exporter. Also uses the filter to sort
   * the titles for custom output.
   * 
   * @param filter an export filter
   */
  public void setFilter(KbartExportFilter filter) {
    this.filter = filter;
    // Use the filter to sort the titles for custom output
    filter.sortTitlesByFirstTwoFields();
    // Set cols based on filter
    KbartExportFilter.ColumnOrdering ordering = filter.getColumnOrdering();
  }

  /**
   * Return a list of column labels post-filtering. This will not include
   * the labels of omitted fields, but will include any labels of custom
   * constant columns.
   * @return a list of column labels
   */
  protected List<String> getColumnLabels() {
    List<String> labs = filter.getVisibleColumnOrdering().getOrderedLabels();
    return labs;
  }

  
  /**
   * Setup output <code>PrintWriter</code> before <code>doExport()</code> is 
   * called.
   * 
   * @param os an OutputStream for the exported data
   */
  protected void setup(OutputStream os) throws IOException {
    OutputStreamWriter osw = new OutputStreamWriter(os, DEFAULT_ENCODING);
    // If compression is enabled, wrap the stream in a zip stream before 
    // the os writer
    if (compress && outputFormat.asFile()) {
      zip = new ZipOutputStream(os);
      zip.setLevel(9);
      zip.putNextEntry(new ZipEntry(getFilename()));
      osw = new OutputStreamWriter(zip, DEFAULT_ENCODING);
    }
    printWriter = new PrintWriter(new BufferedWriter(osw), AUTO_FLUSH); 
  }
  
  /**
   * Finish up after export - in particular, flush and close the output 
   * <code>PrintWriter</code>.
   */
  protected void clearup() throws IOException {
    // Close the ZipOutputStream if it is not null
    if (zip!=null) {
      StringBuffer sb = new StringBuffer();
      sb.append("Export created on ").append(new Date()).append(" by ")
        .append(getHostName());
      sb.append("\nExported ").append(exportCount).append(" KBART titles from ")
        .append(tdbTitleTotal).append(" TDB titles.");
      zip.closeEntry();
      zip.setComment(sb.toString());
      zip.finish();
    }
    printWriter.flush();
    printWriter.close();
  }


  /**
   * Do the actual exporting. This is the core of the export process, which 
   * can be reused. The <code>setup()</code> and <code>clearup()</code> methods 
   * provide default setup and tear down functions but may also be overridden 
   * by subclasses to customise other aspects.
   * <p>
   * The exporter first checks with the filter whether this title should be in 
   * the output.
   */
  private void doExport() throws IOException {
    if (!filter.isOmitHeader()) emitHeader();
    for (KbartTitle title : titles) {
      // Don't output some titles
      if (!filter.isTitleForOutput(title)) {
        duplicateCount++;
        continue;
      }
      exportCount++;
      emitRecord(filter.getVisibleFieldValues(title));
    }
    // flush writer and all its underlying streams
    printWriter.flush();
  }
  
  /**
   * Format a header line and write it to the output.
   */
  protected abstract void emitHeader() throws IOException;

  /**
   * Format a single title and write it to the output.
   *  
   * @param values the KbartTitle record to format and write to output
   */
  protected abstract void emitRecord(List<String> values) throws IOException;

  
  /**
   * Run the export process. That is, run <code>setup()</code> in preparation, 
   * call <code>doExport()</code>, then run <code>clearup()</code> to clear up.
   * 
   * @param os an OutputStream for the exported data
   */
  public void export(OutputStream os) {
    // Setup and perform the export
    try {
      setup(os);
      doExport();
    } catch (IOException e) {
      recordError("Problem exporting to " + outputFormat.getLabel(), e);
    }
    
    // Clear up after the export
    try {
      clearup();
    } catch (IOException e) {
      recordError("Error clearing up after export", e);
    } finally {
      if (printWriter!=null) {
        printWriter.flush();
        printWriter.close();
        printWriter = null;
      }
    }
  }


  /**
   * Construct a filename for the KBART output based on the KBART 
   * recommendation 5.3.1.2. That is, something like 
   * <tt>ProviderName_AllTitles_YYYY-MM-DD.txt</tt>.
   * The filename is only generated once and then stored in the
   * instance. This prevents a disagreement between filenames if 
   * it is called multiple times (e.g. to construct a zip filename also).
   * <p>
   * Note that the DateFormat can be unreliable in different locales; 
   * in some rare cases the date format will not match the KBART 
   * recommendations, and in these cases an error will be logged.
   * 
   * @return a filename in the recommended format
   */
  private String generateFilename() {
    // The following call can cause weird behaviour and prevent this method completing:
    //DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

    // Instead, get a DateFormat for the current locale
    DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
    // Then try to cast it and change the pattern:
    try {
      ((SimpleDateFormat)df).applyPattern("yyyy-MM-dd");
    } catch (Exception e) {
      log.error("Cannot create an appropriate date format in this locale", e);
    }
        
    // Generate the filename
    String providerName = getProviderName();
    String date = df.format(new Date());
    String collnName = getCollectionName();
    String ext = outputFormat.getFileExtension();
    return StringUtil.separatedString(
	new String[]{providerName, collnName, date}, "_"
    ) + "." + ext;
  }
  
  /**
   * Provide a filename for KBART output based on the KBART 
   * recommendation 5.3.1.2. That is, something like 
   * <tt>ProviderName_AllTitles_YYYY-MM-DD.txt</tt>.
   * <p>
   * This method is not intended for use by multiple threads.
   * 
   * @return a filename in the recommended format
   */
  public String getFilename() {
    if (this.filename==null) this.filename = generateFilename(); 
    return this.filename;
  }
  
  /**
   * Return the name of the current provider; this should be the web domain 
   * where the data is hosted, "without punctuation" (KBART 5.3.1.3), which 
   * apparently means "without the top-level domain". Unfortunately the examples
   * in the given recommendation do not make this clear, and although it does 
   * say that the idea is to provide a clear distinction from data provided by 
   * others, it does not suggest that the identifier should be unique. <s>For
   * the moment I have used the host name without further processing.</s>
   * <p>
   * We now just return the name of the provider, which seems better to
   * represent the intended meaning of this recommendation.
   * 
   * @return an appropriate provider name 
   */
  private String getProviderName() {
    // Remove punctuation
    /*String providerName = getHostName();
    providerName = providerName.replaceAll("[-_.,]", "");
    return providerName;*/
    return "lockss";
  }
  
  protected String getHostName() {
    String res = ConfigManager.getPlatformHostname();
    if (res == null) {
      try {
	InetAddress inet = InetAddress.getLocalHost();
	return inet.getHostName();
      } catch (UnknownHostException e) {
	log.warning("Can't get hostname", e);
	return "unknown";
      }
    }
    return res;
  }
  
  protected String getDate() {
    return DateFormat.getDateInstance(DateFormat.LONG).format(new Date()); 
  }

  /**
   * Set the total number of Tdb titles informing this export.
   *  
   * @param n total number
   */
  public void setTdbTitleTotal(int n) {
    this.tdbTitleTotal = n; 
  }

  /**
   * Set the scope of the export.
   * @param scope a ContentScope
   */
  public void setContentScope(ContentScope scope) {
    this.scope = scope;
  }

  /**
   * Return the name of the collection; this uses the scope outputName if a
   * scope is available.
   * See KBART 5.3.1.3.
   * 
   * @return an appropriate collection name for the (section of) TDB being exported 
   */ 
  private String getCollectionName() {
    return scope==null ? ContentScope.DEFAULT_SCOPE.outputName : scope.outputName;
  }
  
  /**
   * Set the compression option.
   * @param val whether to compress the data
   */
  public void setCompress(boolean val) {
    compress = val;
  }

  /**
   * Provide a reference to the OutputFormat of the exporter.
   * @return the output format of this exporter
   */
  public OutputFormat getOutputFormat() {
    return outputFormat; 
  }
  
  /**
   * Get the value of the compression option.
   * @return whether to compress the data
   */
  public boolean isCompress() {
    return compress;
  }
  
  protected void recordError(String msg, Throwable t) {
    log.error(msg, t);
    errors.add(msg + ": " + t.toString());
  }

  public List<String> getErrors() {
    return errors;
  }

  /**
   * Provide a user-friendly summary of which fields were omitted from the output
   * of this filter due to not appearing in the ordering.
   * 
   * @return a printable string
   */
  public String getOmittedFieldsSummary() {
    return filter.omittedFieldsManually() ?
        String.format("Manually omitted columns: (%s)",
            StringUtil.separatedString(
                EnumSet.complementOf(filter.getColumnOrdering().getFields()), ", "
            )
        ) : "";
  }
  
  /**
   * Provide a user-friendly summary of how many KBART records were omitted 
   * from the output of this filter due to duplicate output tuples. This 
   * depends upon which fields are included in the visible output, and whether 
   * they constitute a unique tuple for each record. 
   * 
   * @return a printable string
   */
  public String getOmittedTitlesSummary() {
    return duplicateCount > 0 ?
        String.format("Duplicate records omitted: %s (%s)",
            duplicateCount,
            duplicatesExplanation
        ) : "";
  }
  
  /**
   * Provide a user-friendly summary of which fields were omitted from the output
   * of this filter because they are empty.
   * 
   * @return a printable string
   */
  public String getEmptySummary() {
    return filter.omittedEmptyFields() ? 
        String.format("Empty columns omitted: (%s)",
            StringUtil.separatedString(filter.getOmittedEmptyFields(), ", ")
        )
        : "";
  } 

  /**
   * Return the custom form for HTML output. It may not be set.
   * @return a form containing customisation options; may be null
   */
  public Composite getHtmlCustomForm() {
    return customForm; 
  }
  
  /**
   * Set the HTML form which represents customisable options.
   * @param form a fully-defined Jetty form
   */
  public void setHtmlCustomForm(Composite form) {
    //if (!isHtml) throw new UnsupportedOperationException();
    if (this.outputFormat.isHtml) this.customForm = form;
  }

  /** 
   * Enumeration of <code>KbartExporter</code> types and factories for their 
   * creation. 
   */
  public static enum OutputFormat {

    /** The extension of the TSV file must be ".txt" as per KBART 5.3.1.2 */
    TSV("TSV",
     "text/tab-separated-values", "txt",
     true, false, false,
        TSV_NOTE) {
      @Override     
      public KbartExporter makeExporter(List<KbartTitle> titles, 
          KbartExportFilter filter) {
        KbartExporter kbe = new SeparatedValuesKbartExporter(titles, this,
            SeparatedValuesKbartExporter.SEPARATOR_TAB);
        kbe.setFilter(filter);
	return kbe;
      }
    },
    
    CSV(
        "CSV",
        "text/plain", "csv", 
        true, false, false, 
        CSV_NOTE) {
      @Override     
      public KbartExporter makeExporter(List<KbartTitle> titles, 
          KbartExportFilter filter) {
        KbartExporter kbe = new SeparatedValuesKbartExporter(titles, this,
            SeparatedValuesKbartExporter.SEPARATOR_COMMA);
        kbe.setFilter(filter);
        return kbe;
      }
    },
    
    HTML(
        "On-screen",
        "text/html", "html",
        false, false, true, 
        HTML_NOTE) {
      @Override     
      public KbartExporter makeExporter(List<KbartTitle> titles, 
          KbartExportFilter filter) {
        KbartExporter kbe = new HtmlKbartExporter(titles, this);
        kbe.setFilter(filter);
        return kbe;
      }
    };
  
    /** An optional footnote elaborating the export format. */ 
    private final String footnote;
    /** The displayed label for the output. */ 
    private final String label;
    /** The MIME type of the output. */ 
    private final String mimeType;
    /** The extension for file output. */ 
    private final String fileExtension;
    /** Is the export produced as a file. */ 
    private final boolean asFile;
    /** Whether the file is compressible. */ 
    private boolean isCompressible;
    /** Whether the output is HTML. */
    private boolean isHtml;
    
    /**
     * Construct an OutputFormat with the given label, MIME type and
     * file option.  
     * 
     * @param label human-readable label for the format
     * @param mimeType what the intended MIME type of the output is
     * @param fileExtension a file extension for the file (no period) 
     * @param asFile whether this output format should be supplied as a file
     * @param isCompressible whether this output format may be compressed
     * @param isHtml whether this output format is HTML-based
     * @param footnote an optional footnote describing the option in more detail 
     */
    OutputFormat(String label, String mimeType, String fileExtension, 
	boolean asFile, boolean isCompressible, boolean isHtml, 
	String footnote) {
      this.label = label;
      this.mimeType = mimeType;
      this.fileExtension = fileExtension;
      this.asFile = asFile;
      this.isCompressible = isCompressible;
      this.isHtml = isHtml;
      this.footnote = footnote;
    }
    
    OutputFormat(String label, String mimeType, String fileExtension, 
                 boolean asFile, boolean isCompressible, boolean isHtml) {
      this(label, mimeType, fileExtension, asFile, isCompressible, isHtml, "");
    }

    /**
     * Make a KbartExporter of the appropriate type, using the supplied 
     * KbartTitles.
     * 
     * @param titles a list of <code>KbartTitle</code> objects
     * @param filter the filter to be used in the export
     */
    public abstract KbartExporter makeExporter(List<KbartTitle> titles, 
                                               KbartExportFilter filter);
    
    /**
     * Indicates whether the format should be supplied as a file.
     * Some formats target a file (for writing or download) while others 
     * are intended for streaming to the display.
     * 
     * @return whether this output is intended to be received as a file
     */
    public boolean asFile() { return asFile; }

    /**
     * Get the MIME type for the format.
     * 
     * @return a string representing the MIME type
     */
    public String getMimeType() { return mimeType; }

    /**
     * Get the file extension for the format.
     * 
     * @return a string representing the preferred file extension
     */
    public String getFileExtension() { return fileExtension; }

    /**
     * Get the label of the format.
     * 
     * @return the label
     */
    public String getLabel() { return label; }

    /**
     * Get the footnote of the format.
     * 
     * @return the footnote
     */
    public String getFootnote() { return footnote; }

    /**
     * Whether the format is compressible. Caller should not compress formats 
     * that return false here.
     *  
     * @return whether the format is compressible
     */
    public boolean isCompressible() { return isCompressible; }

    /**
     * Whether the format is HTML based.
     *  
     * @return whether the format is HTML based
     */
    public boolean isHtml() { return isHtml; }

    /**
     * Get an OutputFormat by name. Upper cases the name so lower case values
     * can be passed in URLs.
     *
     * @param name a string representing the name of the format
     * @return an OutputFormat with the specified name, or null if none was found
     */
    public static OutputFormat byName(String name) {
      return byName(name, null);
    }
    /**
     * Get an OutputFormat by name, or the default if the name cannot be parsed.
     *
     * @param name a string representing the name of the format
     * @param def the default to return if the name is invalid
     * @return an OutputFormat with the specified name, or the default
     */
    public static OutputFormat byName(String name, OutputFormat def) {
      try {
        return valueOf(name.toUpperCase());
      } catch (Exception e) {
        return def;
      }
    }

  };
  
}
