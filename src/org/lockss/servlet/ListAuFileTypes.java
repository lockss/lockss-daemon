/*
 * $Id: ListAuFileTypes.java,v 1.1.2.1 2013-07-17 10:12:47 easyonthemayo Exp $
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

package org.lockss.servlet;

import edu.harvard.hul.ois.fits.FitsOutput;
import edu.harvard.hul.ois.fits.exceptions.FitsException;
import org.lockss.config.Configuration;
import org.lockss.config.TdbUtil;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import java.io.*;
import java.util.*;

/**
 * A servlet to list the file type of each file in an AU, as determined by FITS.
 * AU is specified by id in the URL; if no id is specified, or it could not be
 * resolved, a list of AUs is shown, à la DaemonStatus?table=ArchivalUnitStatusTable.
 *
 */
public class ListAuFileTypes extends LockssServlet {

  /** The name under which the servlet will appear in the UI. */
  public static final String SERVLET_NAME = "ListFileTypes";

  /** Default title for HTML page. */
  public static final String DEFAULT_TITLE = "ArchivalUnit File Types";
  /** Default encoding for output. */
  public static final String DEFAULT_ENCODING = "UTF-8";
  /** Whether to auto flush the writer streams. */
  protected static final boolean AUTO_FLUSH = true;

  protected PrintWriter printWriter;
  protected String auid;
  protected String auDescription;
  protected ArchivalUnit au;
  protected FitsReportFormat format = FitsReportFormat.DEFAULT;

  static final String PREFIX = Configuration.PREFIX + "listFileTypes.";

  // ----------------------- LOCKSS PARAMS AND DEFAULTS -----------------------
  /** Enable in UI.  Daemon restart required when set to true,
   * not when set false */
  public static final String PARAM_ENABLE_FILE_TYPES = PREFIX + "enabled";
  public static final boolean DEFAULT_ENABLE_FILE_TYPES = false;

  /** Max number of AUs to report in if no auid specified. Only really used for testing. */
  public static final int MAX_AUS = 10;

  // ------------------------------- URL PARAMS -------------------------------
  /** URL param key for identifier for the AU. */
  public static final String KEY_AU_ID = "auid";
  /** URL param key for report format. */
  public static final String KEY_FORMAT = "format";


  // don't hold onto objects after request finished
  protected void resetLocals() {
    super.resetLocals();
    if (printWriter!=null) {
      printWriter.flush();
      printWriter.close();
      printWriter = null;
    }
    auid = null;
    auDescription = null;
    au = null;
    format = FitsReportFormat.DEFAULT;
  }


  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    long s = TimeBase.nowMs();
    log.debug(String.format("FITS took %s to initialise",
        StringUtil.timeIntervalToString(TimeBase.msSince(s))));
  }

  /**
   * Handle the request and run a report.
   * @throws IOException
   */
  protected void lockssHandleRequest() throws IOException {
    // Make sure Aus have been started before continuing
    if (!getLockssDaemon().areAusStarted()) {
      displayNotStarted();
      return;
    }

    // Set the format from URL
    this.format = FitsReportFormat.getFormat(getParamsAsProps().getProperty(KEY_FORMAT));
    // Create the output writer
    printWriter = new PrintWriter(
        new BufferedWriter(new OutputStreamWriter(resp.getOutputStream(), DEFAULT_ENCODING)),
        AUTO_FLUSH);

    // Set the auid from URL
    this.auid = getParamsAsProps().getProperty(KEY_AU_ID);
    this.au = getLockssDaemon().getPluginManager().getAuFromId(auid);

    if (StringUtil.isNullString(auid) || au==null) {
      actionShowArchivalUnitList();
    } else {
      actionRunReport();
    }
  }


  // -------------------------------------------------------------------------
  // ACTION: Show a list of ArchivalUnits
  // -------------------------------------------------------------------------
  private void actionShowArchivalUnitList() throws IOException {
    //List<ArchivalUnit> aus = getLockssDaemon().getPluginManager().getAllAus();
    Collection<ArchivalUnit> aus = TdbUtil.getPreservedAus();
    printWriter.printf("<html><head>");
    printWriter.printf("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=%s\"/>", DEFAULT_ENCODING);
    printWriter.printf("<title>%s</title>%s</head><body>", DEFAULT_TITLE, css);
    printWriter.printf("<h1>Please choose an ArchivalUnit to report on</h1><p>");
    printWriter.flush();
    for (ArchivalUnit au : aus) {
      String url = String.format("/%s?auid=%s", SERVLET_NAME, UrlUtil.encodeUrl(au.getAuId()));
      printWriter.printf("<a href=\"%s\">%s</a><br/>", url, au.getName());
      printWriter.flush();
    }
    printWriter.printf("</p></body></html>");
    printWriter.flush();
  }

  // -------------------------------------------------------------------------
  // ACTION: Run the AU report
  // -------------------------------------------------------------------------
  private void actionRunReport() throws IOException {
    // Set the content to be a downloadable file if appropriate
    if (format.asFile) {
      // Write a byte-order mark (BOM) for excel to the output stream
      StreamUtil.writeUtf8ByteOrderMark(resp.getOutputStream());
      String filename = String.format("%s.%s", au.getName(), format.fileExtension);
      resp.setHeader( "Content-Disposition", "attachment; filename=" + filename );
    }
    // Set content headers based on the output format
    resp.setContentType(String.format("%s;charset=%s", format.mimeType, DEFAULT_ENCODING));
    resp.setCharacterEncoding(DEFAULT_ENCODING);

    // Run report
    printToReport(format.header(au));
    doAuAnalysis(au);
    printToReport(format.footer());
  }

  /**
   * Print a string to the report's writer, flushing the stream. If an exception
   * occurs, it is logged and nothing is written to output.
   * @param s
   */
  private void printToReport(String s) {
    try {
      printWriter.println(s);
      printWriter.flush();
    } catch (Exception e) {
      log.debug("Problem printing to report: "+e);
      e.printStackTrace();
    }
  }

  /**
   * Perform FITS analysis on a single AU, and print the results.
   * Additionally the FITS result is saved as an XML temp file.
   * @param au
   */
  private void doAuAnalysis(ArchivalUnit au) {
    // Do the analysis
    long s = TimeBase.nowMs();
    // Get a CachedUrl Iterator to iterate all files in the AU
    Iterator<CachedUrl> it = au.getAuCachedUrlSet().archiveMemberIterator();
    if (!it.hasNext()) {
      printToReport(au.getName() + " : <i>No Articles</i>");
      return;
    }

    // Print header for AU
    //printToReport(String.format("<tr><th colspan=\"2\">%s</th></tr>", au.getName()));
    // Print a table of results for the AU
    boolean odd = false;
    printToReport(format.reportHeader());
    int c = 1;
    while (it.hasNext()) {
      c++;
      odd = !odd;
      final CachedUrl cachedUrl = it.next();
      try {
        final FitsOutput fitsOut = FitsUtil.doFitsAnalysis(cachedUrl.getUnfilteredInputStream());
        //final FitsOutput fitsOut = FitsUtil.doFitsAnalysis(cachedUrl);
        // Print the output
        printToReport(format.formatRecord(new FitsRecord(cachedUrl, fitsOut)));
      } catch (FitsException e) {
        log.warning("FITS analysis error: "+e);
        e.printStackTrace();
      } catch (IOException e) {
        log.warning("Stream could not be written to file: "+e);
        e.printStackTrace();
      }
    }
    printToReport(format.reportFooter());

    // AU-level logging
    String analysisSummary = String.format("FITS took %s to analyse %s files for %s",
        StringUtil.timeIntervalToString(TimeBase.msSince(s)), c, au.getName());
    log.debug(analysisSummary);
  }


  /** Some CSS for the output table. */
  private static final String css;
  static {
    StringBuilder sb = new StringBuilder();
    sb.append("<style type=\"text/css\">");
    sb.append("div.header { position: fixed; top: 0; left: 0; width: 100%; z-index: 99; background-color: navy; color: white; }");
    sb.append("table, th, tr, td { border: 1px solid black; }");
    //sb.append("th { font-size: x-small; }");
    sb.append("tr {  }");
    // Override the centering from lockss.css
    sb.append("tr td { white-space: nowrap; text-align: left; }");
    sb.append("</style>");
    css = sb.toString();
  };


  /**
   * A class to format records in the appropriate way for a variety of report
   * formats. Note this is a more generic version of the OutputFormats enum in
   * ListHoldings and the two should be merged.
   */
  private static enum FitsReportFormat {
    HTML ("text/html", "html", false) {
      private boolean odd = false;
      private int index = 0;
      private FitsRecord.Field[] fitsFields = new FitsRecord.Field[] {
          FitsRecord.Field.NAME, FitsRecord.Field.TYPE, FitsRecord.Field.MIME
      };
      @Override
      public String header(ArchivalUnit au) {
        StringBuilder sb = new StringBuilder();
        // Write html and head tags (including a metatag declaring the
        // content type and a title tag), body tag and head title
        sb.append("<html><head>");
        sb.append(String.format("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=%s\"/>", DEFAULT_ENCODING));
        sb.append(String.format("<title>%s</title>", au.getName()));
        sb.append("<link rel=\"stylesheet\" href=\"/css/lockss.css\">");
        sb.append(css);
        sb.append("</head><body>");
        // Add links back and to other formats
        sb.append("<p>");
        sb.append(String.format("<a href=\"/%s\">Back</a>", SERVLET_NAME)).append("<br/>");
        for (FitsReportFormat f : values()) {
          if (f == this) continue; // Don't add link to HTML
          sb.append(String.format(
              "<a href=\"/%s?format=%s&auid=%s\">%s</a>",
              SERVLET_NAME, f.name(), UrlUtil.encodeUrl(au.getAuId()), f.name()
          )).append("<br/>");
        }
        sb.append("</p>");
        sb.append(String.format("<h1>%s</h1>", au.getName()));
        return sb.toString();
      };
      @Override
      public String footer() {
        StringBuilder sb = new StringBuilder();
        // Insert a FITS acknowledgement and close body and html tags
        sb.append("<hr/><p>").append(FitsUtil.FITS_ACK_HTML)
            .append("</p>").append("</body></html>");
        return sb.toString();
      };

      public String reportHeader() {
        StringBuilder sb = new StringBuilder();
        sb.append("<table>");
        sb.append(formatRecord(FitsRecord.getHeaderValues(fitsFields), true));
        return sb.toString();
      }
      public String reportFooter() {
        return "</table>";
      }

      public String formatRecord(List<String> fields) {
        return formatRecord(fields, false);
      }
      /**
       * Format a list of fields as an HTML row, using header cells if necessary.
       * @param fields list of field values
       * @param header whether to use header &lt;th&gt; cells
       * @return a string representing the record
       */
      private String formatRecord(List<String> fields, boolean header) {
        if (fields==null) return "";
        if (!header) odd = !odd;
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("<tr class=\"%s\">", (odd ? "odd-row" : "even-row")));
        if (header) sb.append("<th>Index</th>");
        for (String f : fields) {
          sb.append(header ? "<th>" : "<td>");
          sb.append(f);
          sb.append("&nbsp;");
          sb.append(header ? "</th>" : "</td>");
        }
        sb.append("</tr>");
        return sb.toString();
      };

      // Formats the values of a record in a better way than the default formatRecord()
      @Override
      public String formatRecord(FitsRecord record) {
        odd = !odd;
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("<tr class=\"%s\">", (odd ? "odd-row" : "even-row")));
        // index
        sb.append("<td>").append(++index).append("</td>");
        // name
        sb.append("<td>").append(record.name).append("&nbsp;").append("</td>");
        // type as pronom link
        sb.append("<td>");
        boolean showPronomLink = !StringUtil.isNullString(record.pronomUrl);
        if (showPronomLink) sb.append("<a href=\"").append(record.pronomUrl).append("\">");
        sb.append(record.type);
        if (showPronomLink) sb.append("</a>");
        sb.append("</td>");
        // mime type
        sb.append("<td>").append(record.mimeType).append("&nbsp;").append("</td>");
        sb.append("</tr>");
        return sb.toString();
      }
    },

    CSV ("text/plain", "csv", true) {
      public String header(ArchivalUnit au) { return ""; }
      public String formatRecord(List<String> values) {
        return StringUtil.csvEncodeValues(values);
      }
    },

    TSV ("text/tab-separated-values", "tsv", true) {
      public String header(ArchivalUnit au) { return ""; }
      public String formatRecord(List<String> values) {
        return StringUtil.separatedString(values, "\t");
      }
    }
    ;

    private FitsReportFormat(String mimeType, String fileExtension, boolean asFile) {
      this.mimeType = mimeType;
      this.fileExtension = fileExtension;
      this.asFile = asFile;
    }

    public final String mimeType;
    public final String fileExtension;
    public final boolean asFile;

    public static final FitsReportFormat DEFAULT = HTML;

    // Methods producing various elements of the report as Strings
    /** Construct a page header, using the AU. By default returns the AU's name. */
    //public String header(ArchivalUnit au) { return au.getName(); }
    public abstract String header(ArchivalUnit au);
    public String header() { return ""; }
    // Header of the report, including any column header row
    public String reportHeader() { return formatRecord(FitsRecord.getHeaderValues()); };
    // Footer of the report
    public String reportFooter() { return ""; };
    /** Construct a page footer. By default returns empty string. */
    public String footer() { return ""; }

    /** Format the supplied list of strings as a record. */
    public abstract String formatRecord(List<String> fields);
    /** Format the supplied FITS output object as a record. By default,
     * returns the content type. */
    /*public String formatRecord(FitsOutput fitsOutput) {
      return FitsUtil.getContentType(fitsOutput);
    }*/
    /** Format the supplied FITS record as a record in the report.
     * By default gets a list of field values from the record and formats them.
     * Can be overriden to add more complicated markup, as for HTML format.*/
    public String formatRecord(FitsRecord record) {
      return formatRecord(record.getValues());
    }

    /**
     * Get a report format that matches a given name, supplied via URL.
     * If the name does not match a format, return the default format.
     * @param name the case-insensitive name of a FitsReportFormat
     * @return format matching the name, or the default format
     */
    public static FitsReportFormat getFormat(String name) {
      try {
        return valueOf(name.toUpperCase());
      } catch (Exception e) {
        return DEFAULT;
      }
    }

  }

  /**
   * An interpreted representation of a FitsOutput, representing the details of
   * a record which will be emitted in the report. It can provide a basic
   * list of the values, or can be queried for individual properties to make
   * a more complex record.
   */
  private static final class FitsRecord {
    private final FitsOutput fitsOutput;
    public final String name, url, type, mimeType, pronomUrl;
    private final Map<Field, String> fieldValueMap;

    /** Names of fields stored in the record. */
    public static enum Field {
      NAME("AU file"),
      URL("Original URL"),
      TYPE("FITS-identified type"),
      MIME("MIME type"),
      PRONOM("URL to PRONOM")
      ;
      public final String label;
      private Field(String label) { this.label = label; }
    }

    public FitsRecord(ArticleFiles auFile, FitsOutput fitsOutput) {
      this.fitsOutput = fitsOutput;
      // FitsRecord interpreted values pulled out of the Fits object
      this.url = auFile.getFullTextUrl();
      this.name = url; // the id is just the URL for now
      this.type = FitsUtil.getContentType(fitsOutput);
      this.mimeType = FitsUtil.getMimeType(fitsOutput);
      this.pronomUrl = FitsUtil.getPronomUrl(fitsOutput);
      // Add all values to the mapping from Fields
      fieldValueMap = new HashMap<Field, String>() {{
        put(Field.URL, url);
        put(Field.NAME, name);
        put(Field.TYPE, type);
        put(Field.MIME, mimeType);
        put(Field.PRONOM, pronomUrl);
      }};
    }

    public FitsRecord(CachedUrl cachedUrl, FitsOutput fitsOutput) {
      //this.auFile = auFile;
      this.fitsOutput = fitsOutput;
      // FitsRecord interpreted values pulled out of the Fits object
      this.url = cachedUrl.getUrl();
      this.name = url; // the id is just the URL for now
      this.type = FitsUtil.getContentType(fitsOutput);
      this.mimeType = FitsUtil.getMimeType(fitsOutput);
      this.pronomUrl = FitsUtil.getPronomUrl(fitsOutput);
      // Add all values to the mapping from Fields
      fieldValueMap = new HashMap<Field, String>() {{
        put(Field.URL, url);
        put(Field.NAME, name);
        put(Field.TYPE, type);
        put(Field.MIME, mimeType);
        put(Field.PRONOM, pronomUrl);
      }};
    }

    /**
     * Default list of header values, for the default list of fields returned
     * by getValues().
     * @return
     */
    public static List<String> getHeaderValues() {
      return getHeaderValues(Field.values());
    }
    /**
     * Provide a base list of string field values which can be used to
     * represent this record as simply as possible.
     * @return
     */
    public List<String> getValues() {
      return getValues(Field.values());
    }

    /**
     * Get the header values for the specified fields.
     * @param fields
     * @return
     */
    public static List<String> getHeaderValues(final Field[] fields) {
      return new ArrayList<String>() {{
        for (Field f : fields) add(f.label);
      }};
    }
    /**
     * Get the values for the specified fields.
     * @param fields
     * @return
     */
    public List<String> getValues(final Field[] fields) {
      return new ArrayList<String>() {{
        for (Field f : fields) {
          add(fieldValueMap.get(f));
        }
      }};
    }

  }

}
