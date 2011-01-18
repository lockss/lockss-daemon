/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.lockss.config.Configuration;
import org.lockss.config.CurrentConfig;
import org.lockss.config.Tdb;
import org.lockss.exporter.kbart.KbartConverter;
import org.lockss.exporter.kbart.KbartExporter;
import org.lockss.exporter.kbart.KbartTitle;
import org.lockss.exporter.kbart.KbartExporter.OutputFormat;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import org.mortbay.html.Form;
import org.mortbay.html.Input;
import org.mortbay.html.Link;
import org.mortbay.html.Page;
import org.mortbay.html.Table;

/** 
 * This servlet provides access to holdings metadata, transforming the TDB data 
 * into KBART format data which can be imported into a spreadsheet.
 * <p>
 * A possible enhancement for a future version is to allow the export of a subset
 * of the data based on a collection description. 
 * 
 */
public class ListHoldings extends LockssServlet {
  
  protected static Logger log = Logger.getLogger("ListHoldings");    
  
  /** Default output format is TSV. */
  static final OutputFormat OUTPUT_DEFAULT = OutputFormat.KBART_TSV;
  /** Selected output format. */
  private OutputFormat outputFormat = OUTPUT_DEFAULT;

  public static final String ACTION_EXPORT = "Export";
  public static final String PARAM_FORMAT = "format";
  public static final String PARAM_COMPRESS = "compress";
  
  private Configuration sysConfig;
  
  private Tdb tdb;

  // Output options
  boolean isCompress;
  
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    this.sysConfig = CurrentConfig.getCurrentConfig();
    this.tdb = sysConfig.getTdb();
  }

  /** Handle a request */
  public void lockssHandleRequest() throws IOException {
    errMsg = null;
    statusMsg = null; 

    // Set outputFormat from the URL param 
    String formatParam = req.getParameter(PARAM_FORMAT);
    outputFormat = OutputFormat.byName(formatParam);
    if (outputFormat==null) outputFormat = OUTPUT_DEFAULT;
    
    // Set compression
    isCompress = outputFormat.isCompressible();
    
    // Show page or perform action
    if (StringUtil.isNullString(formatParam)) {
      displayPage();
    } else {
      log.info("Exporting metadata as "+outputFormat.getLabel());
      doExport();
    }
  }

  /**
   * Perform an export of the data to the selected output stream. This 
   * involves getting a TDB record from the system config, converting 
   * the appropriate sections of it into KBART format, then emitting
   * that title by title.
   */
  private void doExport() {
    
    // Update the tdb
    this.tdb = this.sysConfig.getTdb();
    
    if (tdb==null || tdb.isEmpty()) {
      errMsg = "No titlesets are defined.";
      return;
    }

    // Create TDB/KB extractor/translator
    KbartConverter kbconv = new KbartConverter(tdb);
    List<KbartTitle> titles = kbconv.extractAllTitles();
      
    try {
      // Create an exporter
      KbartExporter kexp = outputFormat.makeExporter(titles);
      kexp.setTdbTitleTotal(tdb.getTdbTitleCount());
      kexp.setCompress(isCompress);
      
      // Set the content to be a downloadable file if appropriate 
      if (outputFormat.asFile()) {
	String filename = kexp.getFilename();
	String theFile = isCompress ? filename+".zip" : filename;
	resp.setHeader( "Content-Disposition", "attachment; filename=" + theFile );
      }
      
      // Set content headers based on the output format
      resp.setContentType( (isCompress ? "application/zip" : outputFormat.getMimeType()) + ";charset=UTF-8");
      //resp.setContentLength(  );

      // Export to the response OutputStream
      OutputStream out = resp.getOutputStream();
      long s = System.currentTimeMillis();
      kexp.export(out);
      log.info("Export took approximately " + (System.currentTimeMillis()-s)/1000 + "s");
      out.flush();
      out.close();
      
      // Check errors
      List errs = kexp.getErrors();
      log.debug("errs: " + errs);
      if (!errs.isEmpty()) {
        errMsg = StringUtil.separatedString(errs, "<br>");
      } else {
        statusMsg = "File(s) written";
      }
    } catch (Exception e) {
      errMsg = e.getMessage();
      return;
    }
  }

  
  /**
   * Generate a table with the page components and options. 
   * 
   * @return a Jetty table with all the page's options
   */
  protected Table getTableOfOptions() {
    // Get the path to this servlet so we can postfix output format path
    String thisPath = myServletDescr().path;
    
    Table tab = new Table(0, "align=\"center\" width=\"80%\"");

    tab.newRow();
    tab.newCell("align=\"center\"");
    tab.add("This is the KBART Metadata Exporter for ");
    tab.add(getMachineName());

    tab.newRow();
    tab.newCell("align=\"center\"");
    if (tdb==null || tdb.isEmpty()) {
      tab.add("No titlesets are defined.");
      addBlankLine(tab);
      return tab;
    }
    
    tab.add("There are "+tdb.getTdbTitleCount()+" titles to export, from "+tdb.getTdbPublisherCount()+" publishers.");
    addBlankLine(tab);
    
    tab.newRow();
    tab.newCell("align=\"center\"");
    tab.add("Please choose from an export format below.");
    
    addBlankLine(tab);
    
    // Add compress option
    //tab.newRow();
    //tab.newCell("align=\"center\"");
    //tab.add(ServletUtil.checkbox(this, PARAM_COMPRESS, PARAM_COMPRESS, "Compress the output", false));
    
    // Add format radio buttons
    for (OutputFormat fmt : OutputFormat.values()) {
      tab.newRow();
      tab.newCell("align=\"center\"");
      tab.add( new Link(String.format("%s?%s=%s", thisPath, PARAM_FORMAT, fmt.name()), "Export as "+fmt.getLabel()) );
      tab.add(addFootnote(fmt.getFootnote()));
    }
    // Add some space
    addBlankLine(tab);
    return tab;
  }
  
  private void addBlankLine(Table tab) {
    tab.newRow();
    tab.newCell();
    tab.add("&nbsp;");
  }


  /** Display top level batch config choices */
  private void displayPage() throws IOException {
    Page page = newPage();
    layoutErrorBlock(page);
    if (tdb!=null && !tdb.isEmpty()) {
      page.add(getTableOfOptions());
    }
    layoutFooter(page);
    ServletUtil.writePage(resp, page);
  }
    
}
