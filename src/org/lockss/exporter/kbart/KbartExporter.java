/*
 * $Id: KbartExporter.java,v 1.2.2.1 2011-02-16 23:46:08 easyonthemayo Exp $
 */

/*

Copyright (c) 2010 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.lockss.config.ConfigManager;
import org.lockss.exporter.kbart.KbartExportFilter.FieldOrdering;
import org.lockss.exporter.kbart.KbartExportFilter.PredefinedFieldOrdering;
import org.lockss.exporter.kbart.KbartTitle.Field;

import static org.lockss.exporter.kbart.KbartTitle.Field.*;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

/**
 * Export the metadata from the LOCKSS holdings according to
 * <emph>KBART Phase I Recommended Practice</emph> document NISO-RP-9-2010.
 * The document suggests that the output metadata file should have its entries
 * ordered alphabetically by title (5.3.1.11) and be in UTF-8.
 * <p>
 * This class handles the plumbing of output streams, and any 
 * required compression, while formatting of the exported records is performed by 
 * the implementation subclass.
 * <p>
 * Instances of this class are intended to be used once, for a single export,
 * and only by a single thread.
 * 
 * @author Neil Mayo
 */
public abstract class KbartExporter {

  private static Logger log = Logger.getLogger("KbartExporter");

  // Footnotes for the interface options
  private static final String TSV_NOTE = "Please note that the TSV format adheres to the KBART recommendations "+
  "and should be used for updating your knowledge bases.";
  private static final String HTML_NOTE =  "The HTML version is for manual inspection of our holdings and is less "+
  "strict than KBART. For example the HTML version is capable of reordering fields and omitting empty columns.";
    
  /** Record of errors for the caller. */
  List<String> errors = new ArrayList<String>();

  /** The total number of TdbTitle objects used as input to the export process. Only included as
   * a courtesy for use in output display. */
  protected int tdbTitleTotal;
  
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
  
  /** A count of how many records have been exported. */
  protected int exportCount = 0;
  
  /** By default, we don't want to exclude empty fields as it will contravene KBART. */
  public static final boolean omitEmptyFieldsByDefault = false;
  
  
  /**
   * Default constructor takes a list of KbartTitle objects to be exported.
   * 
   * @param titles the list of titles which are to be exported
   * @param format the OutputFormat
   */
  public KbartExporter(List<KbartTitle> titles, OutputFormat format) {
    this.titles = titles;
    this.outputFormat = format;
    // Create an identity filter by default
    this.filter = KbartExportFilter.identityFilter(titles);
    // KBART info should be ordered alphabetically by title by default
    Collections.sort(titles, new KbartTitleAlphanumericComparator());
  }
 
  
  /**
   * Set the filter to be used on this exporter.
   * 
   * @param filter an export filter
   */
  public void setFilter(KbartExportFilter filter) {
    this.filter = filter;
  }
  
  /**
   * Return a list of field labels post-filtering. This will not include
   * the labels of omitted fields.
   * @return a list of field labels
   */
  protected List<String> getFieldLabels() {
    return Field.getLabels(filter.getVisibleFieldOrder());
  }

  
  /**
   * Setup output <code>PrintWriter</code> before <code>doExport()</code> is called.
   * 
   * @param os an OutputStream for the exported data
   */
  protected void setup(OutputStream os) throws IOException {
    if (compress && outputFormat.asFile()) {
      zip = new ZipOutputStream(os);
      zip.setLevel(9);
      zip.putNextEntry(new ZipEntry(getFilename()));
      printWriter = new PrintWriter(zip, true);
    } else {
      printWriter = new PrintWriter(os, true);
    } 
  }
  
  /**
   * Finish up after export - in particular, flush and close the output 
   * <code>PrintWriter</code>.
   */
  protected void clearup() throws IOException {
    // Close the ZipOutputStream if it is not null
    if (zip!=null) {
      StringBuffer sb = new StringBuffer();
      sb.append("Export created on " + new Date() + " by " + getHostName());
      sb.append("\nExported " + exportCount + " KBART titles from " + tdbTitleTotal + " TDB titles.");
      zip.closeEntry();
      zip.setComment(sb.toString());
      zip.finish();
    }
    printWriter.flush();
    printWriter.close();
  }


  /**
   * Do the actual exporting. This is the core of the export process, which can be reused.
   * The <code>setup()</code> and <code>clearup()</code> methods provide default setup and
   * tear down functions but may also be overridden by subclasses to
   * customise other aspects.
   * <p>
   * The exporter first checks with the filter whether this title should be in the output.
   * 
   * @param out an OutputStream for the exported data
   */
  private void doExport(OutputStream os) throws IOException {
    for (KbartTitle title : titles) {
      if (!filter.isTitleForOutput(title)) return;
      exportCount++;
      emitRecord(filter.getVisibleFieldValues(title));
    } 
    // flush writer and all its underlying streams
    printWriter.flush();
  }
  
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
      doExport(os);
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
   * in some rare cases the date format will not match the KBART recommendations,
   * and in these cases an error will be logged.
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
    return StringUtil.separatedString(new String[]{providerName, collnName, date}, "_") + "." + ext;
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
   * in the given recommendation do not make this clear, and although it does say that
   * the idea is to provide a clear distinction from data provided by others, it 
   * does not suggest that the identifier should be unique. For the moment I have 
   * used the host name without further processing. 
   * 
   * @return an appropriate provider name 
   */
  private String getProviderName() {
    return getHostName(); 
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

  /**
   * Set the total number of Tdb titles informing this export.
   *  
   * @param n total number
   */
  public void setTdbTitleTotal(int n) {
    this.tdbTitleTotal = n; 
  }
  
 
  /**
   * Return the name of the collection; this is probably only useful if we 
   * provide the option to export a subset of the data. For the moment this 
   * method just returns "AllTitles". See KBART 5.3.1.3.
   * 
   * @return an appropriate collection name for the (section of) TDB that is being exported 
   */ 
  private String getCollectionName() {
    return "AllTitles"; 
  }
  
  /**
   * Set the compression option.
   * @param val whether to compress the data
   */
  public void setCompress(boolean val) {
    compress = val;
  }

  /**
   * Get the value of the compression option.
   * @return whether to compress the data
   */
  public boolean getCompress() {
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
  public String getOmittedSummary() {
    return filter.omittedFieldsManually() ?
	String.format("Manually omitted columns: (%s)", 
	    StringUtil.separatedString(EnumSet.complementOf(filter.getFieldOrdering().getFields()), ", ")
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
	String.format("Empty columns omitted: (%s)", StringUtil.separatedString(filter.getEmptyFields(), ", "))
	: "";
  } 

  

  /** 
   * Enumeration of <code>KbartExporter</code> types and factories for their creation. 
   */
  public static enum OutputFormat {
        
    // Don't compress the TSV output
    KBART_TSV("KBART TSV (tab-separated values)", "text/tab-separated-values", "tsv", true, false, TSV_NOTE) {
      @Override     
      public KbartExporter makeExporter(List<KbartTitle> titles, KbartExportFilter filter) {
	KbartExporter kbe = new SeparatedValuesKbartExporter(titles, this);
	kbe.setFilter(filter);
	return kbe;
      }
    },
    
    /*KBART_CSV("KBART CSV (comma-separated values)", "text/plain", "csv", true, true) {
      @Override     
      public KbartExporter makeExporter(List<KbartTitle> titles, KbartExportFilter filter) {
	KbartExporter kbe = new SeparatedValuesKbartExporter(titles, this, SeparatedValuesKbartExporter.COMMA);
	kbe.setFilter(filter);
	return kbe;
      }
    },*/
    
    KBART_HTML("HTML (on-screen)", "text/html", "html", false, false, HTML_NOTE) {
      @Override     
      public KbartExporter makeExporter(List<KbartTitle> titles, KbartExportFilter filter) {
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
    
    /**
     * Construct an OutputFormat with the given label, MIME type and
     * file option.  
     * 
     * @param label human-readable label for the format
     * @param mimeType what the intended MIME type of the output is
     * @param fileExtension a file extension for the file (no period) 
     * @param asFile whether this output format should be supplied as a file
     * @param isCompressible whether this output format may be compressed
     * @param footnote an optional footnote describing the option in more detail 
     */
    OutputFormat(String label, String mimeType, String fileExtension, boolean asFile, boolean isCompressible, String footnote) {
      this.footnote = footnote;
      this.label = label;
      this.mimeType = mimeType;
      this.fileExtension = fileExtension;
      this.asFile = asFile;
      this.isCompressible = isCompressible;
    }
    
    OutputFormat(String label, String mimeType, String fileExtension, boolean asFile, boolean isCompressible) {
      this(label, mimeType, fileExtension, asFile, isCompressible, "");
    }

    /**
     * Make a KbartExporter of the appropriate type, using the supplied KbartTitles.
     * 
     * @param titles a list of <code>KbartTitle</code> objects
     */
    public abstract KbartExporter makeExporter(List<KbartTitle> titles, KbartExportFilter filter);
   
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
     * Whether the format is compressible. Caller should not compress formats that return false here.
     *  
     * @return whether the format is compressible
     */
    public boolean isCompressible() { return isCompressible; }

    /**
     * Get an OutputFormat by name.
     * 
     * @param name a string representing the name of the format
     * @return an OutputFormat with the specified name, or null if none was found
     */
    public static OutputFormat byName(String name) {
      if (name==null) return null;
      for (OutputFormat of : values()) {
        if (of.name().equals(name)) return of;
      }
      return null;
    }

  };

  
}
