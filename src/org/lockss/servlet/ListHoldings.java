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
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
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
import org.mortbay.html.Composite;
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
 * output options - predefined outputs for strict KBART CSV, TSV and an HTML version 
 * of the same; and also the option to view customised HTML output of the same data. 
 * The main default formats are represented as links, while the HTML customisation
 * is achieved via a separate form submission.
 * <p>
 * Possible enhancements for a future version:
 * <ul>
 * <li>Allow the export of a subset of the data based on a collection description.</li>
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

  /** Default output format is CSV. */
  static final OutputFormat OUTPUT_DEFAULT = OutputFormat.KBART_CSV;
  /** Default field selection and ordering is KBART. */
  static final FieldOrdering FIELD_ORDERING_DEFAULT = CustomFieldOrdering.getDefaultOrdering();
  //static final PredefinedFieldOrdering FIELD_ORDERING_DEFAULT = PredefinedFieldOrdering.KBART;
  
  /** Default approach to omitting empty fields - inherited from the exporter base class. */
  static final Boolean OMIT_EMPTY_COLUMNS_BY_DEFAULT = KbartExporter.omitEmptyFieldsByDefault;
  
  private static final String ENCODING = KbartExporter.DEFAULT_ENCODING;
  /** A comma used for separating list elements. */
  private static final String LIST_COMMA = ", "; 
  
  // Form parameters and options
  public static final String ACTION_EXPORT = "Export";
  public static final String ACTION_CUSTOM_EXPORT = "Customise Output";
  /** Apply the current customisation. */
  public static final String ACTION_CUSTOM_OK = "Apply";
  /** Reset customisation to the defaults. */
  public static final String ACTION_CUSTOM_RESET = "Reset";
  /** Cancel the current customisation and show the output again. */
  public static final String ACTION_CUSTOM_CANCEL = "Cancel";
  public static final String KEY_FORMAT = "format";
  public static final String KEY_COMPRESS = "compress";
  public static final String KEY_OMIT_EMPTY_COLS = "omitEmptyCols";
  public static final String KEY_CUSTOM_ORDERING = "ordering";
  public static final String KEY_CUSTOM_ORDERING_LIST = "ordering_list";
  public static final String KEY_CUSTOM_ORDERING_PREVIOUS_MANUAL = "ordering_list_previous_manual";
  
  static final String SESSION_KEY_CUSTOM_OPTS = "org.lockss.servlet.ListHoldings.customHtmlOpts";
  static final String SESSION_KEY_OUTPUT_FORMAT = "org.lockss.servlet.ListHoldings.outputFormat";

  // Bits of state that must be reset in resetLocals()
  /** A reference to the latest system configuration; maintained while the servlet is handling a request. */
  private Configuration sysConfig;
  /** A record of the last manual ordering which was applied to an export; maintained while the servlet is handling a request. */
  private String lastManualOrdering;
  /** Manually specified custom field list. */
  private FieldOrdering customFieldOrdering;
  /** Whether to do an export - set based on the submitted parameters. */
  private boolean doExport = false; 
  
  // A load of removed state:
  /** Whether to omit empty field columns in this instance. */
  //private boolean omitEmptyColumns = OMIT_EMPTY_COLUMNS_BY_DEFAULT;
  /** Selected custom field list. */
  //PredefinedFieldOrdering predefinedFieldOrdering = FIELD_ORDERING_DEFAULT;
  
  /**
   * Get the current configuration and the TDB record.
   */
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    sysConfig = CurrentConfig.getCurrentConfig();
  }

  protected void resetLocals() {
    errMsg = null;
    statusMsg = null;
    sysConfig = null;
    lastManualOrdering = null;
    customFieldOrdering = null;
    doExport = false;
    super.resetLocals();
  }
  
  
  /**
   * Get the Tdb record from the current configuration.
   * @return the current Tdb object
   */
  private Tdb getTdb() {
    if (sysConfig == null) {
      sysConfig = CurrentConfig.getCurrentConfig();
    }
    return sysConfig.getTdb();
  }
  
  /**
   * Get the field ordering which is to be used for this export - either
   * a predefined ordering or a custom ordering depending on the form options. 
   */
  /*private FieldOrdering getSelectedOrdering() {
    switch (exportType) {
    case PREDEFINED: return predefinedFieldOrdering;
    case CUSTOM: return customFieldOrdering; 
    default: return FIELD_ORDERING_DEFAULT;
    }
  }*/

  /** 
   * Handle a request - if there is a format URL param, show the appropriate default format; 
   * otherwise if it is a form submission show custom format; otherwise show the page.
   * The main page is shown if the params indicate so or errors occur. Otherwise the relevant
   * values are set and the code falls through to the end of the method where the export is performed.
   * 
   */
  public void lockssHandleRequest() throws IOException {
    errMsg = null;
    statusMsg = null; 
        
    // Get a set of custom opts, constructed with defaults if necessary
    CustomHtmlOptions customOpts = getSessionCustomHtmlOpts();

    // ---------- Get parameters ----------
    Properties params = getParamsAsProps();
    // Output format parameters (from URL)
    OutputFormat outputFormat = OutputFormat.byName(params.getProperty(KEY_FORMAT));

    // Set compression from the output format
    //if (outputFormat!=null) this.isCompress = outputFormat.isCompressible();

    // Omit empty columns - use the option supplied from the form, or the default if one of the other outputs was chosen
    boolean omitEmptyColumns = Boolean.valueOf( 
	params.getProperty(KEY_OMIT_EMPTY_COLS, OMIT_EMPTY_COLUMNS_BY_DEFAULT.toString()) 
    );
    //this.omitEmptyColumns = this.sysConfig.getBooleanParam(KEY_OMIT_EMPTY_COLS, OMIT_EMPTY_COLUMNS_BY_DEFAULT);
    
    // Custom HTML parameters (from custom form)
    String customAction = params.getProperty(ACTION_TAG, "");
    // Manual custom ordering received from the text area
    String manualOrdering = params.getProperty(KEY_CUSTOM_ORDERING_LIST);
    // Last manual ordering (only received from customisation page)
    lastManualOrdering = params.getProperty(KEY_CUSTOM_ORDERING_PREVIOUS_MANUAL);
    // Set custom ordering to default 
    this.customFieldOrdering = FIELD_ORDERING_DEFAULT;

    // ---------- Interpret parameters ----------
    // Is this a custom output? The custom action is not null.
    boolean isCustom = !StringUtil.isNullString(customAction);
    // Are we exporting? OutputFormat specified, and custom ok/cancel if specified
    this.doExport = outputFormat!=null && (!isCustom || (customAction.equals(ACTION_CUSTOM_OK) || customAction.equals(ACTION_CUSTOM_CANCEL)));

     
    // ---------- Process parameters and show page ----------
    // If custom output, set the field ordering and omit flag
    if (isCustom) {
      // If custom export requested (from the output page) or a customisation was 
      // okayed, set the custom ordering to the supplied manual ordering. If an 
      // export is validated, set the last manual ordering.
      if (customAction.equals(ACTION_CUSTOM_EXPORT) || customAction.equals(ACTION_CUSTOM_OK)) {
	// Try and parse the manual ordering into a list of valid field names
	setCustomFieldOrdering(manualOrdering);
	if (doExport) lastManualOrdering = manualOrdering;
      }
      // Cancel the customisation and set the ordering to the previously applied value (from the session)
      else if (customAction.equals(ACTION_CUSTOM_CANCEL)) {
	setCustomFieldOrdering(manualOrdering);
	if (doExport) manualOrdering = lastManualOrdering;
	omitEmptyColumns = customOpts.omitEmptyColumns;
      }
      // Reset the ordering customisation to the default
      else if (customAction.equals(ACTION_CUSTOM_RESET)) {
        customFieldOrdering = FIELD_ORDERING_DEFAULT;
        //omitEmptyColumns = OMIT_EMPTY_COLUMNS_BY_DEFAULT;
      } 
      
      // Create an object encapsulating the custom HTML options, and store it in the session.
      customOpts = new CustomHtmlOptions(omitEmptyColumns, customFieldOrdering);
      putSessionCustomHtmlOpts(customOpts);
    
    } else {
      // If this is not a custom output, reset the session customisation
      resetSessionOptions();
    }
    
    // Just display the page if there is no export happening
    if (!doExport) {
      log.debug("No export requested; showing "+(isCustom?"custom":"main")+" options");
      // Show the appropriate half of the page depending on whether we are customising
      displayPage(isCustom);
      return;
    }

    // Now we are doing an export - create the exporter
    KbartExporter kexp = createExporter(getTdb(), outputFormat);
    // Make sure the exporter was properly instantiated
    if (kexp==null) {
      log.debug("No exporter; showing main options");
      displayPage();
      return;
    }

    // Do the export
    doExport(kexp);
  }
  
  /**
   * Attempt to set the custom field ordering using the given ordering string.
   * If the set fails, the errMsg is set and the doExport variable is set to false;
   * @param ordering
   * @return whether the set succeeded
   */
  private boolean setCustomFieldOrdering(String ordering) {
    boolean success = false;
    try {
      this.customFieldOrdering = new CustomFieldOrdering(ordering);
      success = true;
    } catch (CustomFieldOrderingException e) {
      errMsg = e.getLocalizedMessage();
      success = false;
      doExport = false;
    }
    return success;
  }
  
  /**
   * Make an exporter to be used in an export; this involves extracting and
   * converting titles from the TDB and passing to the exporter's constructor.
   * The exporter is configured with the basic settings; further configuration 
   * may be necessary for custom exports.
   * 
   * @return a usable exporter, or null if one could not be created
   */
  private KbartExporter createExporter(Tdb tdb, OutputFormat outputFormat) {
    List<KbartTitle> titles = extractKbartTitlesFromTdb();
    // Return if there are no titles
    if (titles.isEmpty()) {
      errMsg = "No KBART titles extracted from TDB.";
      return null;
    }
    
    CustomHtmlOptions opts = getSessionCustomHtmlOpts();
    // Create a filter
    KbartExportFilter filter;
    if (outputFormat.isHtml() && opts !=null) {
      filter = new KbartExportFilter(titles, opts.fieldOrdering, opts.omitEmptyColumns);
    } else {
      filter = new KbartExportFilter(titles);
    }
    
    // Create and configure an exporter
    KbartExporter kexp = outputFormat.makeExporter(titles, filter);
    kexp.setTdbTitleTotal(tdb.getTdbTitleCount());
    
    // Set an HTML form for the HTML output
    assignHtmlCustomForm(kexp);
    return kexp;    
  }
  
  /**
   * Assign an HTML form of custom options to the exporter if necessary.
   * @param kexp the exporter 
   */
  private void assignHtmlCustomForm(KbartExporter kexp) {
    if (kexp.getOutputFormat().isHtml()) {
      kexp.setHtmlCustomForm(makeHtmlCustomForm());
    }
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
    Tdb tdb = getTdb();
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
   * @throws IOException
   */
  private void doExport(KbartExporter kexp) throws IOException {
    OutputFormat outputFormat = kexp.getOutputFormat();
    // Set the content to be a downloadable file if appropriate 
    if (outputFormat.asFile()) {
      String filename = kexp.getFilename();
      String theFile = kexp.isCompress() ? filename+".zip" : filename;
      resp.setHeader( "Content-Disposition", "attachment; filename=" + theFile );
    }

    // Set content headers based on the output format
    resp.setContentType( (kexp.isCompress() ? "application/zip" : outputFormat.getMimeType()) + ";charset="+ENCODING);
    resp.setCharacterEncoding(ENCODING);
    //resp.setContentLength(  );

    // Export to the response OutputStream
    OutputStream out = resp.getOutputStream();
    long s = System.currentTimeMillis();
    kexp.export(out);
    log.debug("Export took approximately " + (System.currentTimeMillis()-s)/1000 + "s");
    out.flush();
    out.close();

    // Check errors (Note: the response has already been written by here, so there is no point setting the err/status msgs)
    List errs = kexp.getErrors();
    log.debug("errs: " + errs);
    if (!errs.isEmpty()) {
      errMsg = StringUtil.separatedString(errs, "<br>");
    } else {
      statusMsg = "File(s) written";
    }
  }

  /**
   * Constructs a string representing the direct update URL of the default output.
   * 
   * @return a string URL indicating the direct address for default output
   */
  public String getDefaultUpdateUrl() {
    return srvAbsURL(myServletDescr(), "format="+OUTPUT_DEFAULT.name() );
  }
  
  /**
   * Generate a table with the page components and options. 
   * 
   * @param isCustom whether to show the customisation options
   * @return a Jetty table with all the page's options
   */
  protected Table layoutTableOfOptions(boolean custom) {
    // Get the path to this servlet so we can postfix output format path
    String thisPath = myServletDescr().path;
    Table tab = new Table(0, "align=\"center\" width=\"80%\"");
    //addBoxSummary(tab);

    tab.newRow();
    tab.newCell("align=\"center\"");
    Tdb tdb = getTdb();
    if (tdb==null || tdb.isEmpty()) {
      tab.add("No titlesets are defined.");
      addBlankRow(tab);
      return tab;
    }
    tab.add("There are "+tdb.getTdbTitleCount()+" titles available for preservation, from "+tdb.getTdbPublisherCount()+" publishers.");
       
    // Add compress option (disabled as the CSV output is not very large)
    //tab.newRow();
    //tab.newCell("align=\"center\"");
    //tab.add(ServletUtil.checkbox(this, KEY_COMPRESS, KEY_COMPRESS, "Compress the output", false));
    
    if (custom) {
      // Add HTML customisation options
      addBlankRow(tab);
      tab.newRow();
      tab.newCell("align=\"center\"");
      tab.add(new Heading(3, "Customise HTML output"));
      tab.add(new Link(srvURL(myServletDescr()), "Return to main export page"));
      tab.add(layoutFormCustomHtmlOpts());
      addBlankRow(tab);
    } else {
      // Add default output formats
      tab.newRow();
      tab.newCell("align=\"center\"");
      tab.add("Please choose from an export format below.");
      addBlankRow(tab);
      // Add format links
      for (OutputFormat fmt : OutputFormat.values()) {
	tab.newRow();
	tab.newCell("align=\"center\"");
	tab.add( new Link(String.format("%s?%s=%s", thisPath, KEY_FORMAT, fmt.name()), "Export as "+fmt.getLabel()) );
	tab.add(addFootnote(fmt.getFootnote()));
      }
      addBlankRow(tab);
    }
    
    // Add some space
    addBlankRow(tab);
    
    return tab;
  }
  
  /**
   * Create a table listing KBART field labels and descriptions.
   */
  private static Table getKbartFieldLegend() {
    Table tab = new Table();
    //tab.style("border: 1px solid black;");
    for (Field f: Field.values()) {
      tab.newRow();
      tab.newCell("align=\"center\"");
      //if (Field.idFields.contains(f)) tab.add(smallFont("ID"));
      String lab = f.getLabel();
      if (Field.idFields.contains(f)) lab = "<b>"+lab+"</b>";
      tab.addCell(smallFont(lab));
      tab.addCell(smallFont(f.getDescription()));
    }
    return tab;
  }
  
  /**
   * Display top level export options, no custom options. 
   * @throws IOException
   */
  private void displayPage() throws IOException {
    displayPage(false);
  }
  
  /**
   * Display top level export options.
   * @param custom whether to show the HTML customisation options
   * @throws IOException
   */
  private void displayPage(boolean custom) throws IOException {
    Page page = newPage();
    layoutErrorBlock(page);
    page.add(layoutTableOfOptions(custom));
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
    
    // Get the opts from the session
    CustomHtmlOptions opts = getSessionCustomHtmlOpts();
    
    Form form = ServletUtil.newForm(srvURL(myServletDescr()));
    form.add(new Input(Input.Hidden, "isForm", "true"));
    // Add a format parameter
    form.add(new Input(Input.Hidden, KEY_FORMAT, OutputFormat.KBART_HTML.name()));
    // Add a hidden field listing the last manual ordering
    form.add(new Input(Input.Hidden, KEY_CUSTOM_ORDERING_PREVIOUS_MANUAL, lastManualOrdering));
    
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
    
    form.add(
	"<br/>Use the box below to provide a custom ordering for the fields - one field per line."+
	"<br/>Omit any fields you don't want, but include an identifying field for sensible results."+
	//"<br/>(" + StringUtils.join(Field.getLabels(Field.idFields), LIST_COMMA) + ")"+
	"<br/>There is a description of the KBART fields below the box; identifying fields are shown in bold."+
	"<br/><br/>"
	);
    
    Table tab = new Table();
    tab.newRow();
    tab.newCell("align=\"center\" valign=\"middle\"");
    
    // Add a text area of an appropriate size
    int taCols = 25; // this should be the longest field width
    int taLines = Field.values().length+1;
    tab.add("Field ordering<br/>");
    tab.add(new TextArea(KEY_CUSTOM_ORDERING_LIST, getOrderingAsCustomFieldList(opts.fieldOrdering)).setSize(taCols, taLines));
    // Omit empty columns option
    tab.add("<br/>");
    tab.add(ServletUtil.checkbox(this, KEY_OMIT_EMPTY_COLS, Boolean.TRUE.toString(), "Omit empty columns<br/>", opts.omitEmptyColumns));
    // Add buttons
    tab.add("<br/><center>");
    layoutSubmitButton(this, tab, ACTION_TAG, ACTION_CUSTOM_OK);
    layoutSubmitButton(this, tab, ACTION_TAG, ACTION_CUSTOM_RESET);
    layoutSubmitButton(this, tab, ACTION_TAG, ACTION_CUSTOM_CANCEL);
    tab.add("</center>");
    
    // Add a legend for the fields
    tab.newCell("align=\"left\" padding=\"10\" valign=\"middle\"");
    tab.add("<br/>"+getKbartFieldLegend());
    
    form.add(tab);

    return form;
  }

  /**
   * Turn the selected ordering into a string containing a separated list of fields. 
   * @return
   */
  private static String getOrderingAsCustomFieldList(FieldOrdering fo) {
    return StringUtils.join(fo.getOrderedLabels(), CustomFieldOrdering.CUSTOM_ORDERING_FIELD_SEPARATOR); 
  }
  
  /**
   * Construct an HTML form providing a link to customisation options for HTML output.
   * This consists of a hidden list of ordered output fields, and a submit button,
   * and will appear on the output page.
   * 
   * @return a Jetty form
   */
  private Form makeHtmlCustomForm() {
    // Get the opts from the session
    CustomHtmlOptions opts = getSessionCustomHtmlOpts();
    String servletUrl = srvURL(myServletDescr());
    Form form = ServletUtil.newForm(servletUrl);
    form.add(new Input(Input.Hidden, KEY_CUSTOM_ORDERING_LIST, getOrderingAsCustomFieldList(opts.fieldOrdering)));
    form.add(new Input(Input.Hidden, KEY_OMIT_EMPTY_COLS, htmlInputTruthValue(opts.omitEmptyColumns)));
    ServletUtil.layoutSubmitButton(this, form, ACTION_TAG, ACTION_CUSTOM_EXPORT);
    form.add(new Link(servletUrl, "Return to main export page"));
    return form;
  }  
  
  /**
   * An alternative to <code>ServletUtil.layoutSubmitButton</code> which doesn't put 
   * every button on a new line.
   * @param servlet the servlet containing the button
   * @param composite the object to add the button to
   * @param key the name of the button
   * @param value the value of the button
   */
  private static void layoutButton(LockssServlet servlet, Composite composite, String key, String value, String type) {
    Input submit = new Input(type, key, value);
    servlet.setTabOrder(submit);
    composite.add(submit);
  }

  private static void layoutSubmitButton(LockssServlet servlet, Composite composite, String key, String value) {
    layoutButton(servlet, composite, key, value, Input.Submit); 
  }
  private static void layoutResetButton(LockssServlet servlet, Composite composite, String key, String value) {
    layoutButton(servlet, composite, key, value, Input.Reset); 
  }

  
  /**
   * Get the string representation of a boolean value, appropriate for use as the value of a form input.
   * @param b a boolean value
   * @return a string which will yield the same value when interpreted as the value of a form input
   */
  private static String htmlInputTruthValue(boolean b) {
    return b ? "true" : "false"; 
  }

  /** Add a blank row to a table. */
  private static void addBlankRow(Table tab) {
    tab.newRow();
    tab.newCell();
    tab.add("&nbsp;");
  }

  /**
   * Surround a string with small font tags.
   * @param s
   * @return
   */
  private static String smallFont(String s) {
   return String.format("<font size=\"-1\">%s</font>", s); 
  }

  /**
   * Add a summary of the LOCKSS box providing the data, to an HTML table.
   * @param tab the table to which to add the summary
   */
  private void addBoxSummary(Table tab) {
    tab.newRow();
    tab.newCell("align=\"center\"");
    tab.add("This is the KBART Metadata Exporter for ");
    tab.add("<b>"+getMachineName()+"</b>.");

    tab.newRow();
    tab.newCell("align=\"center\"");
    tab.add("The permanent KBART output URL for this server is:<br/><b><font color=\"navy\">"+getDefaultUpdateUrl()+"</font></b>");
    addBlankRow(tab);
  }

  /**
   * Get the current custom HTML options from the session. If the cookie is not set, it is set
   * to a new custom HTML options with default settings.
   * @return a CustomHtmlOptions object from the session
   */
  protected CustomHtmlOptions getSessionCustomHtmlOpts() {
    Object o = getSession().getAttribute(SESSION_KEY_CUSTOM_OPTS);
    if (o==null) {
      CustomHtmlOptions opts = CustomHtmlOptions.getDefaultOptions();
      putSessionCustomHtmlOpts(opts);
      return opts;
    }
    return o==null ? null : (CustomHtmlOptions)o; 
  }

  /**
   * Puts the current custom HTML options into the session.
   * @param opts a CustomHtmlOptions object
   */
  protected void putSessionCustomHtmlOpts(CustomHtmlOptions opts) {
    getSession().setAttribute(SESSION_KEY_CUSTOM_OPTS, opts);
  }

  /**
   * Put a default set of options in the session.
   */
  protected void resetSessionOptions() {
    putSessionCustomHtmlOpts(CustomHtmlOptions.getDefaultOptions()); 
  }
  
  /**
   * Get the current output format from the session. If the cookie is not set, it is set to the default format.
   * @return the current output format
   */
  /*protected OutputFormat getOutputFormat() {
    Object o = getSession().getAttribute(SESSION_KEY_OUTPUT_FORMAT);
    if (o==null) {
      OutputFormat format = OUTPUT_DEFAULT;
      putSessionOutputFormat(format);
      return format;
    }
    return (OutputFormat)o;
  }*/
  
  /**
   * Puts the current output format into the session.
   * @param format an OutputFormat
   */
  /*protected void putSessionOutputFormat(OutputFormat format) {
    getSession().setAttribute(SESSION_KEY_OUTPUT_FORMAT, format);
  }*/
  
  /**
   * A simple class for encapsulating HTML customisation options.
   * @author Neil Mayo
   */
  private static class CustomHtmlOptions {
    boolean omitEmptyColumns;
    FieldOrdering fieldOrdering;
    
    public CustomHtmlOptions(boolean omit, FieldOrdering ord) {
      this.omitEmptyColumns = omit;
      this.fieldOrdering = ord;
    }
    
    public static CustomHtmlOptions getDefaultOptions() {
      return new CustomHtmlOptions(OMIT_EMPTY_COLUMNS_BY_DEFAULT, FIELD_ORDERING_DEFAULT);      
    }
  }
  
}
