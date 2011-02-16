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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.config.CurrentConfig;
import org.lockss.config.Tdb;
import org.lockss.exporter.kbart.KbartConverter;
import org.lockss.exporter.kbart.KbartExportFilter;
import org.lockss.exporter.kbart.KbartExporter;
import org.lockss.exporter.kbart.KbartTitle;
import org.lockss.exporter.kbart.KbartExporter.OutputFormat;
import org.lockss.exporter.kbart.KbartExportFilter.CustomFieldOrdering;
import org.lockss.exporter.kbart.KbartExportFilter.FieldOrdering;
import org.lockss.exporter.kbart.KbartExportFilter.PredefinedFieldOrdering;
import org.lockss.exporter.kbart.KbartExportFilter.CustomFieldOrdering.CustomFieldOrderingException;
import org.lockss.exporter.kbart.KbartTitle.Field;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import org.mortbay.html.Form;
import org.mortbay.html.Heading;
import org.mortbay.html.Input;
import org.mortbay.html.Link;
import org.mortbay.html.Page;
import org.mortbay.html.Table;
import org.mortbay.html.TextArea;

/** 
 * This servlet provides access to holdings metadata, transforming the TDB data 
 * into KBART format data which can be imported into a spreadsheet. There are several 
 * output options - predefined outputs for strict KBART TSV and an HTML version 
 * of the same; and also the option to view customised HTML output of the same data. 
 * The main default formats are represented as links, while the HTML customisation
 * is achieved via a form submission.
 * <p>
 * Possible enhancements for a future version:
 * <ul>
 * <li>Allow the export of a subset of the data based on a collection description.</li>
 * <li>Provide the ability for the user to define a custom list of fields and an ordering.</li>
 * </ul> 
 * 
 * @author Neil Mayo
 */
public class ListHoldings extends LockssServlet {
  
  protected static Logger log = Logger.getLogger("ListHoldings");    
  
  static final String PREFIX = Configuration.PREFIX + "listHoldings.";

  /** Enable ListHoldings in UI.  Daemon restart required when set to true,
   * not when set false */
  public static final String PARAM_ENABLE_HOLDINGS = PREFIX + "enabled";
  public static final boolean DEFAULT_ENABLE_HOLDINGS = false;

  /** Default output format is TSV. */
  static final OutputFormat OUTPUT_DEFAULT = OutputFormat.KBART_TSV;
  /** Default field selection and ordering is KBART. */
  static final PredefinedFieldOrdering FIELD_ORDERING_DEFAULT = PredefinedFieldOrdering.KBART;
  /** Default approach to omitting empty fields - inherited from the exporter base class. */
  static final Boolean OMIT_EMPTY_COLUMNS_BY_DEFAULT = KbartExporter.omitEmptyFieldsByDefault;
  
  // Form parameters and options
  public static final String ACTION_EXPORT = "Export";
  public static final String ACTION_CUSTOM_EXPORT = "Custom Export";
  public static final String KEY_FORMAT = "format";
  public static final String KEY_COMPRESS = "compress";
  public static final String KEY_OMIT_EMPTY_COLS = "omitEmptyCols";
  public static final String KEY_CUSTOM_ORDERING = "ordering";
  public static final String KEY_CUSTOM_ORDERING_LIST = "ordering_list";
  
  private Configuration sysConfig;
  
  /** Tdb to export. */
  private Tdb tdb;

  // Output options generated by user input to the form
  /** Whether to compress the output. Each output format has a flag indicating whether it is compressible;
   * this may be overridden by a form option. */
  private boolean isCompress;
  /** Whether to omit empty field columns in this instance. */
  private boolean omitEmptyColumns = OMIT_EMPTY_COLUMNS_BY_DEFAULT;
  
  /** Selected output format. */
  private OutputFormat outputFormat = OUTPUT_DEFAULT;
  /** Selected custom field list. */
  PredefinedFieldOrdering predefinedFieldOrdering = FIELD_ORDERING_DEFAULT;
  /** Manually specified custom field list. */
  CustomFieldOrdering customFieldOrdering = new CustomFieldOrdering(new ArrayList<Field>(Field.getFieldSet()));
  /** What type of export is represented by the form options. */
  ExportType exportType = ExportType.DEFAULT;

  protected enum ExportType { DEFAULT, PREDEFINED, CUSTOM } 
    
  /**
   * Get the current configuration and the TDB record.
   */
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    this.sysConfig = CurrentConfig.getCurrentConfig();
    this.tdb = sysConfig.getTdb();
  }
  
  /**
   * Get the field ordering which is to be used for this export - either
   * a predefined ordering or a custom ordering depending on the form options. 
   */
  private FieldOrdering getSelectedOrdering() {
    switch (exportType) {
    case PREDEFINED: return predefinedFieldOrdering;
    case CUSTOM: return customFieldOrdering; 
    default: return FIELD_ORDERING_DEFAULT;
    }
  }

  /** 
   * Handle a request - if there is a format URL param, show the appropriate default format; 
   * otherwise if it is a form submission show custom format; otherwise show the page.
   */
  public void lockssHandleRequest() throws IOException {
    errMsg = null;
    statusMsg = null; 

    // Get parameters
    Properties params = getParamsAsProps();
    // Output format parameters (from URL)
    this.outputFormat = OutputFormat.byName(params.getProperty(KEY_FORMAT));
    // Omit empty columns - use the option supplied from the form, or the default if one of the other outputs was chosen
    this.omitEmptyColumns = Boolean.valueOf( 
	params.getProperty(KEY_OMIT_EMPTY_COLS, OMIT_EMPTY_COLUMNS_BY_DEFAULT.toString()) 
    );
    //TODO: ? this.omitEmptyColumns = this.sysConfig.getBooleanParam(KEY_OMIT_EMPTY_COLS, OMIT_EMPTY_COLUMNS_BY_DEFAULT);
    // Custom HTML parameters (from form)
    String action = params.getProperty(ACTION_TAG, "");
    // Predefined ordering
    String orderParam = params.getProperty(KEY_CUSTOM_ORDERING, FIELD_ORDERING_DEFAULT.name());
    // Manual custom ordering
    String manualOrdering = params.getProperty(KEY_CUSTOM_ORDERING_LIST);

    // Is this a custom HTML output?
    boolean isCustom = !StringUtil.isNullString(action) && action.equals(ACTION_CUSTOM_EXPORT);

    // If there is a manual ordering specified, try and parse it into a list of valid field names
    if (isCustom && manualOrdering!=null) {
      try {
      	this.customFieldOrdering = new CustomFieldOrdering(manualOrdering);
      	this.exportType = ExportType.CUSTOM;
      } catch (CustomFieldOrderingException e) {
	errMsg = e.getLocalizedMessage();
	displayPage();
	return;
      }
    }
    
    // Just display the page if there is no action and no output format
    if (!isCustom && outputFormat==null) {
      displayPage();
      return;
    }

    // Setup ordering parameter for predefined custom output
    // DISABLED - if predefined options re-enabled, update the conditional here
    // to distinguish between manual and predefined custom orderings.  
    /*if (isCustom) {
      try {
	this.fieldOrdering = PredefinedFieldOrdering.valueOf(orderParam);
        this.exportType = ExportType.PREDEFINED;
      } catch (Exception e) {
	this.fieldOrdering = FIELD_ORDERING_DEFAULT;
	errMsg = "Unknown field ordering specified: "+orderParam+".";
	log.warning(errMsg, e);
	displayPage();
	return;
      }
    }*/
    
    // Set compression from the output format
    this.isCompress = outputFormat.isCompressible();
    
    // Create the exporter
    KbartExporter kexp = createExporter(this.tdb);
    if (kexp==null) {
      displayPage();
      return;
    }

    // Write to log before starting export
    /*log.info("Exporting metadata " + 
	(isCustom ? "using custom HTML view '"+this.fieldOrdering.displayName+"'" : "as "+outputFormat.getLabel())
    );*/
    
    // Do the export
    doExport(kexp);
  }
  
  /**
   * Make an exporter to be used in an export; this involves extracting and
   * converting titles from the TDB and passing to the exporter's constructor.
   * The exporter is configured with the basic settings; further configuration 
   * may be necessary for custom exports.
   * 
   * @return a usable exporter, or null if one could not be created
   */
  private KbartExporter createExporter(Tdb tdb) {
    List<KbartTitle> titles = extractKbartTitlesFromTdb();
    // Return if there are no titles
    if (titles.isEmpty()) {
      errMsg = "No KBART titles extracted from TDB.";
      return null;
    }
    // Create a filter
    KbartExportFilter filter = new KbartExportFilter(titles, getSelectedOrdering(), omitEmptyColumns);
    // Create and configure an exporter
    KbartExporter kexp = outputFormat.makeExporter(titles, filter);
    kexp.setTdbTitleTotal(tdb.getTdbTitleCount());
    kexp.setCompress(isCompress);
    return kexp;    
  }
  
  // TODO move extraction to KbartExporter?
  /**
   * Get the titles out of the tdb and convert them to KbartTitles. This uses the KbartConverter
   * and is an expensive operation.
   * 
   * @return a list of converted titles from the tdb
   */
  private List<KbartTitle> extractKbartTitlesFromTdb() {
    // Update the tdb
    this.tdb = this.sysConfig.getTdb();
    if (tdb==null || tdb.isEmpty()) {
      return Collections.emptyList();
    }
    // Create TDB/KB extractor/translator
    KbartConverter kbconv = new KbartConverter(tdb);
    List<KbartTitle> titles = kbconv.extractAllTitles();
    return titles;
  }
  
  /**
   * Perform an export of the data to the selected output stream. This 
   * involves getting a TDB record from the system config, converting 
   * the appropriate sections of it into KBART format, then emitting
   * that title by title.
   * 
   * @param kexp an exporter to use for export
   */
  private void doExport(KbartExporter kexp) {
    try {
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
      log.debug("Export took approximately " + (System.currentTimeMillis()-s)/1000 + "s");
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
   * Constructs a string representing the direct update URL of the TSV output.
   * 
   * @return a string URL indicating the direct address for TSV output
   */
  public String getTsvUpdateUrl() {
    return srvAbsURL(myServletDescr(), "format="+OUTPUT_DEFAULT.name() );
  }
  
  /**
   * Generate a table with the page components and options. 
   * 
   * @return a Jetty table with all the page's options
   */
  protected Table layoutTableOfOptions() {
    // Get the path to this servlet so we can postfix output format path
    String thisPath = myServletDescr().path;
    
    Table tab = new Table(0, "align=\"center\" width=\"80%\"");

    tab.newRow();
    tab.newCell("align=\"center\"");
    tab.add("This is the KBART Metadata Exporter for ");
    tab.add("<b>"+getMachineName()+"</b>.");

    tab.newRow();
    tab.newCell("align=\"center\"");
    tab.add("The permanent TSV output URL for this server is:<br/><b><font color=\"navy\">"+getTsvUpdateUrl()+"</font></b>");
    addBlankRow(tab);
    
    tab.newRow();
    tab.newCell("align=\"center\"");
    if (tdb==null || tdb.isEmpty()) {
      tab.add("No titlesets are defined.");
      addBlankRow(tab);
      return tab;
    }
    tab.add("There are "+tdb.getTdbTitleCount()+" titles to export, from "+tdb.getTdbPublisherCount()+" publishers.");
    //addBlankRow(tab);
    
    tab.newRow();
    tab.newCell("align=\"center\"");
    tab.add("Please choose from an export format below.");
    
    addBlankRow(tab);
    
    // Add compress option
    //tab.newRow();
    //tab.newCell("align=\"center\"");
    //tab.add(ServletUtil.checkbox(this, KEY_COMPRESS, KEY_COMPRESS, "Compress the output", false));
    
    // Add format links
    for (OutputFormat fmt : OutputFormat.values()) {
      tab.newRow();
      tab.newCell("align=\"center\"");
      tab.add( new Link(String.format("%s?%s=%s", thisPath, KEY_FORMAT, fmt.name()), "Export as "+fmt.getLabel()) );
      tab.add(addFootnote(fmt.getFootnote()));
    }
    addBlankRow(tab);
    
    // Add a form for HTML options
    tab.newRow();
    tab.newCell("align=\"center\"");
    tab.add("<hr width=\"80%\"/>");
    tab.add(new Heading(3, "Alternatively, customise HTML output:"));
    tab.add(layoutFormCustomHtmlOpts());
    addBlankRow(tab);

    // Add some space
    addBlankRow(tab);
    
    return tab;
  }
  
  /** Add a blank row to a table. */
  private void addBlankRow(Table tab) {
    tab.newRow();
    tab.newCell();
    tab.add("&nbsp;");
  }


  /** Display top level batch config choices */
  private void displayPage() throws IOException {
    Page page = newPage();
    layoutErrorBlock(page);
    if (tdb!=null && !tdb.isEmpty()) {
      page.add(layoutTableOfOptions());
    }
    // Finish page
    layoutFooter(page);
    ServletUtil.writePage(resp, page);
  }

  /**
   * Create a form of options for custom HTML output. This includes options to use a 
   * custom field list or ordering, and to omit empty columns.
   * 
   * @return an HTML form 
   */
  private Form layoutFormCustomHtmlOpts() {
    Form form = ServletUtil.newForm(srvURL(myServletDescr()));
    form.add(new Input(Input.Hidden, "isForm", "true"));
    // Add a format parameter
    form.add(new Input(Input.Hidden, KEY_FORMAT, OutputFormat.KBART_HTML.name()));
    
    /*
    form.add("<br/>Choose a field set:<br/>");
    // Field ordering options (radio buttons)
    for (PredefinedFieldOrdering order: PredefinedFieldOrdering.values()) {
      form.add( 
	  ServletUtil.radioButton(this, KEY_CUSTOM_ORDERING, order.name(), 
	      order.displayName+" <span style=\"font-style:italic;font-size:small\">("+order.description+")</span><br/>", order==FIELD_ORDERING_DEFAULT)
      );
    }
     */
    
    form.add("<br/>Please provide a custom ordering for the fields - one field per line. Omit any fields you don't want, " +
    		"but include an identifying field (" +
    		StringUtils.join(Field.getLabels(Field.idFields), ", ") +
    		") for sensible results.<br/>");
    
    // Add a text area of an appropriate size
    int taCols = 25; // this should be the longest field width
    int taLines = Field.values().length+1;
    String fieldList = StringUtils.join(Field.getLabels(), "\n");
    form.add(new TextArea(KEY_CUSTOM_ORDERING_LIST, fieldList).setSize(taCols, taLines));

    // TODO : add a list of available fields

    // Omit empty columns option
    form.add(ServletUtil.checkbox(this, KEY_OMIT_EMPTY_COLS, Boolean.TRUE.toString(), "Omit empty columns<br/>", true));

    if (tdb!=null && !tdb.isEmpty()) {
      ServletUtil.layoutSubmitButton(this, form, ACTION_TAG, ACTION_CUSTOM_EXPORT);
    }
    return form;
  }

}
