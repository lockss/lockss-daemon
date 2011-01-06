/*
 * $Id: KbartExporter.java,v 1.1 2011-01-06 18:32:53 neilmayo Exp $
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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.lockss.config.ConfigManager;
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

  /** Record of errors for the caller. */
  List<String> errors = new ArrayList<String>();

  /** The total number of TdbTitle objects used as input to the export process. Only included as
   * a courtesy for use in output display. */
  protected int tdbTitleTotal;
  
  /** The list of KBART format titles to export. */
  protected final List<KbartTitle> titles;

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
  private int expCount;
  
  /**
   * Default constructor takes a list of KbartTitle objects to be exported.
   * 
   * @param titles the list of titles which are to be exported
   * @param format the OutputFormat
   */
  public KbartExporter(List<KbartTitle> titles, OutputFormat format) {
    this.titles = titles;
    this.outputFormat = format;
    // KBART info should be ordered alphabetically by title by default
    Collections.sort(titles);
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
      sb.append("\nExported " + expCount + " KBART titles from " + tdbTitleTotal + " TDB titles.");
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
   * 
   * @param out an OutputStream for the exported data
   */
  private void doExport(OutputStream os) throws IOException {
    for (KbartTitle title : titles) {
      emitRecord(title);
      expCount++;
    } 
    // flush writer and all its underlying streams
    printWriter.flush();
  }
  
  /**
   * Format a single title and write it to the output.
   *  
   * @param title the KbartTitle record to format and write to output
   */
  protected abstract void emitRecord(KbartTitle title) throws IOException;

  
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
   * A factory interface for making <code>KbartExporter</code> instances.
   * 
   * @author neil
   */
  public interface Factory {
    public KbartExporter makeExporter(List<KbartTitle> titles);
  }

  /** 
   * Enumeration of <code>KbartExporter</code> types and factories for their creation. 
   */
  public static enum OutputFormat implements Factory {
    
    KBART_TSV("KBART TSV (tab-separated values)", "text/tab-separated-values", "tsv", true, true) {
      @Override     
      public KbartExporter makeExporter(List<KbartTitle> titles) {
	return new SeparatedValuesKbartExporter(titles, this);
      }
    },
    
    /*KBART_CSV("KBART CSV (comma-separated values)", "text/plain", "csv", true, true) {
      @Override     
      public KbartExporter makeExporter(List<KbartTitle> titles) {
      return new SeparatedValuesKbartExporter(titles, this, SeparatedValuesKbartExporter.COMMA);
      }
    },*/
    
    KBART_HTML("HTML (on-screen)", "text/html", "html", false, false) {
      @Override     
      public KbartExporter makeExporter(List<KbartTitle> titles) {
	return new HtmlKbartExporter(titles, this);
      }
    };

    private final String label;
    private final String mimeType;
    private final String fileExtension;
    private final boolean asFile;
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
     */
    OutputFormat(String label, String mimeType, String fileExtension, boolean asFile, boolean isCompressible) {
      this.label = label;
      this.mimeType = mimeType;
      this.fileExtension = fileExtension;
      this.asFile = asFile;
      this.isCompressible = isCompressible;
    }

    /**
     * Make a KbartExporter of the appropriate type, using the supplied KbartTitles.
     * 
     * @param titles a list of <code>KbartTitle</code> objects
     */
    public abstract KbartExporter makeExporter(List<KbartTitle> titles);
   
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
      for (OutputFormat of : values()) {
        if (of.name().equals(name)) return of;
      }
      return null;
    }

  };

  
}
