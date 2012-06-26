/*
 * $Id: ListHoldings.java,v 1.33.4.4 2012-06-26 00:56:55 tlipkis Exp $
 */

/*

Copyright (c) 2010-2011 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.lockss.config.*;
import org.lockss.config.TdbUtil.ContentScope;
import org.lockss.daemon.AuHealthMetric;
import org.lockss.daemon.MetadataDatabaseUtil;
import org.lockss.exporter.biblio.BibliographicItem;
import org.lockss.exporter.kbart.*;
import org.lockss.exporter.kbart.KbartExporter.OutputFormat;
import org.lockss.exporter.kbart.ReportFormat.ReportDataFormat;
import org.lockss.exporter.kbart.CoverageNotesFormat;
import org.lockss.exporter.kbart.KbartExportFilter.CustomFieldOrdering;
import org.lockss.exporter.kbart.KbartExportFilter.FieldOrdering;
import org.lockss.exporter.kbart.KbartExportFilter.CustomFieldOrdering.CustomFieldOrderingException;
import org.lockss.exporter.kbart.KbartTitle.Field;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import org.lockss.util.TimeBase;
import org.mortbay.html.Form;
import org.mortbay.html.Input;
import org.mortbay.html.Table;
import org.mortbay.html.Composite;
import org.mortbay.html.TextArea;
import org.mortbay.html.Heading;
import org.mortbay.html.Link;
import org.mortbay.html.Page;

/** 
 * This servlet provides access to holdings metadata, transforming the TDB data 
 * into KBART format data which can be imported into a spreadsheet. There are
 * predefined output options for CSV, TSV and HTML. The content of
 * the output can be strict KBART, or can be customised in terms of fields and
 * field ordering. A health metric rating can also be appended to the custom
 * output, though this is currently disabled.
 * <p>
 * Possible enhancements for a future version:
 * <ul>
 * <li>Allow the export of a subset of the data based on a collection
 *   description.</li>
 * </ul> 
 * 
 * @author Neil Mayo
 */
@SuppressWarnings("serial")
public class ListHoldings extends LockssServlet {
  
  protected static Logger log = Logger.getLogger("ListHoldings");    

  static final String PREFIX = Configuration.PREFIX + "listHoldings.";

  private static final String BREAK = "<br/><br/>";
  private static final String ENCODING = KbartExporter.DEFAULT_ENCODING;
  /** Create a footnote for platforms that don't support the health metric. */
  private String notAvailFootnote;

  // ----------------------- LOCKSS PARAMS AND DEFAULTS -----------------------
  /** Enable ListHoldings in UI.  Daemon restart required when set to true,
   * not when set false */
  public static final String PARAM_ENABLE_HOLDINGS = PREFIX + "enabled";
  public static final boolean DEFAULT_ENABLE_HOLDINGS = false;

  /** Enable "preserved" option when ListHoldings UI is enabled. */
  public static final String
    PARAM_ENABLE_PRESERVED_HOLDINGS = PREFIX + "enablePreserved";
  public static final boolean DEFAULT_ENABLE_PRESERVED_HOLDINGS = true;
  
  /** Enable "use metadata" option when "preserved" option is enabled. */
  public static final String
    PARAM_USE_METATATA_FOR_PRESERVED_HOLDINGS = 
      PREFIX + "useMetadataForPreserved";
  public static final boolean 
    DEFAULT_USE_METADATA_FOR_PRESERVED_HOLDINGS = false;

  // ------------------------------- URL PARAMS -------------------------------
  // These keys are used in the URL for direct access to particular reports.
  // DO NOT CHANGE
  public static final String KEY_TITLE_SCOPE = "scope";
  public static final String KEY_OUTPUT_FORMAT = "format";
  public static final String KEY_REPORT_FORMAT = "report";
  public static final String KEY_COVERAGE_NOTES_FORMAT = "coverageNotesFormat";

  // ------------------------ LABELS FOR SUBMIT BUTTONS ------------------------
  public static final String ACTION_EXPORT = "List Titles";
  public static final String ACTION_CUSTOM = "Customise Fields";
  public static final String ACTION_HIDE_CUSTOM_EXPORT = "Hide Customise Fields";
  /** Apply the current customisation. */
  public static final String ACTION_CUSTOM_OK = "List Titles";
  /** Reset customisation to the defaults. */
  public static final String ACTION_CUSTOM_RESET = "Reset";
  /** Cancel the current customisation and show the output again. */
  public static final String ACTION_CUSTOM_CANCEL = "Cancel";

  // ------------------------- FORM PARAMS AND OPTIONS -------------------------
  public static final String KEY_COMPRESS = "compress";
  public static final String KEY_OMIT_EMPTY_COLS = "omitEmptyCols";
  public static final String KEY_SHOW_HEALTH = "showHealthRatings";
  public static final String KEY_CUSTOM = "isCustom";
  public static final String KEY_CUSTOM_ORDERING = "ordering";
  public static final String KEY_CUSTOM_ORDERING_LIST = "ordering_list";
  public static final String KEY_CUSTOM_ORDERING_PREVIOUS_MANUAL = "ordering_list_previous_manual";

  // ------------------------------ SESSION KEYS ------------------------------
  /** Session key for storing custom options between customisation screens. */
  static final String SESSION_KEY_CUSTOM_OPTS = "org.lockss.servlet.ListHoldings.customOpts";

  // ----------------------------- OPTION DEFAULTS -----------------------------
  // These are used in the state variables representing parameters of the export
  /** Default output format is HTML. */
  static final OutputFormat OUTPUT_DEFAULT = OutputFormat.HTML;
  /** Default report format is KBART. */
  static final ReportDataFormat REPORT_DEFAULT = ReportDataFormat.KBART;
  /** Default scope is the default in the scope enum. */
  static final ContentScope SCOPE_DEFAULT = ContentScope.DEFAULT_SCOPE;
  /** Default coverage notes format is year(volume) ranges. */
  static final CoverageNotesFormat COVERAGE_NOTES_DEFAULT = CoverageNotesFormat.YEAR_VOLUME;

  /** Default field selection and ordering is KBART. */
  static final FieldOrdering FIELD_ORDERING_DEFAULT = CustomFieldOrdering.getDefaultOrdering();
  /** Default approach to omitting empty fields - inherited from the exporter base class. */
  static final Boolean OMIT_EMPTY_COLUMNS_BY_DEFAULT = KbartExporter.omitEmptyFieldsByDefault;
  /** Default approach to showing health ratings - inherited from the exporter base class. */
  static final Boolean SHOW_HEALTH_RATINGS_BY_DEFAULT = KbartExporter.showHealthRatingsByDefault;

  // -------------------------- STATE FOR URL PARAMS --------------------------
  // Bits of state that must be reset to defaults in resetLocals()
  /** Holdings scope option. */
  private ContentScope selectedScope = ContentScope.DEFAULT_SCOPE;
  private OutputFormat outputFormat = OUTPUT_DEFAULT;
  private ReportDataFormat reportDataFormat = REPORT_DEFAULT;
  private CoverageNotesFormat coverageNotesFormat = COVERAGE_NOTES_DEFAULT;

  // ---------------------------- TRANSITORY STATE ----------------------------
  // Bits of state that must be reset in resetLocals()
  /** A record of the last manual ordering which was applied to an export;
   * maintained while the servlet is handling a request. */
  private String lastManualOrdering;
  /** Manually specified custom field list. */
  private FieldOrdering customFieldOrdering;
  /** Whether to do an export - set based on the submitted parameters. */
  private boolean doExport = false;

  // --------------------------------------------------------------------------
  // --------------------------------------------------------------------------


  /** Get the current configuration and the TDB record. */
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
  }

  @Override
  /** Reset the transitory state. */
  protected void resetLocals() {
    // Reset transitory state
    errMsg = null;
    statusMsg = null;
    lastManualOrdering = null;
    customFieldOrdering = null;
    doExport = false;
    // Reset export parameters to defaults
    selectedScope = ContentScope.DEFAULT_SCOPE;
    outputFormat = OUTPUT_DEFAULT;
    reportDataFormat = REPORT_DEFAULT;
    coverageNotesFormat = COVERAGE_NOTES_DEFAULT;
    // Finally reset super locals
    super.resetLocals();
  }


  /**
   * Get an enum by name. Upper cases the name so lower case values
   * can be passed in URLs.
   *
   * @param enumClass the enum we want a value from
   * @param name a string representing the name of the format
   * @return an enum, of the type T, with the specified name, or null if none was found
   */
  /*protected static <T extends Enum<T>> T byName(String name) {
    return byName(name, null);
  }*/

  /**
   * Get an OutputFormat by name, or the default if the name cannot be parsed.
   *
   * @param name a string representing the name of the format
   * @param def the default to return if the name is invalid
   * @return an OutputFormat with the specified name, or the default
   */
  /*protected static <T extends Enum<T>> T byName(String name, T def) {
    log.debug("XXX name "+name+" def "+def);
    try {
      if (def==null) return (T)T.valueOf(T.class, name.toUpperCase());
        return (T)def.valueOf(def.getClass(), name.toUpperCase());
    } catch (Exception e) {
      e.printStackTrace();
      return def;
    }
  }*/


  /** 
   * Handle a request - if there is a format URL param, show the appropriate
   * default format; otherwise if it is a form submission show custom format;
   * otherwise show the page. The main page is shown if the params indicate so
   * or errors occur. Otherwise the relevant values are set and the code falls
   * through to the end of the method where the export is performed.
   * <p>
   *   The bare minimum for an export to occur is the output format; all other
   *   values can be set to defaults. If the output format is not set, then the
   *   options are shown. If the export is customised, one of the custom submit
   *   actions must have a value.
   * </p>
   */
  public void lockssHandleRequest() throws IOException {
    errMsg = null;
    statusMsg = null; 
     
    // Create a footnote if options are not available on this platform.
    if (!AuHealthMetric.isSupported()) {
      /*String fn = String.format("Not available on this platform (%s).",
	PlatformUtil.getInstance());*/
      String fn = "Not available on this platform.";
      notAvailFootnote = addFootnote(fn);
    }
    
    // Get a set of custom opts, constructed with defaults if necessary
    KbartCustomOptions customOpts = getSessionCustomOpts();

    // ---------- Get parameters ----------
    Properties params = getParamsAsProps();
    // Output format parameters (from URL)
    // Set the output format if specified (do not set a default as
    // this param indictates whether an export has been requested).
    outputFormat = OutputFormat.byName(params.getProperty(KEY_OUTPUT_FORMAT));
    // Set other arguments to defaults unless specified
    reportDataFormat = ReportDataFormat.byName(params.getProperty(KEY_REPORT_FORMAT), REPORT_DEFAULT);
    coverageNotesFormat = CoverageNotesFormat.byName(params.getProperty(KEY_COVERAGE_NOTES_FORMAT), COVERAGE_NOTES_DEFAULT);
    selectedScope = ContentScope.byName(params.getProperty(KEY_TITLE_SCOPE), SCOPE_DEFAULT);

    // Set compression from the output format
    //if (outputFormat!=null) this.isCompress = outputFormat.isCompressible();

    // Omit empty columns - use the option supplied from the form, or the
    // default if one of the other outputs was chosen
    boolean omitEmptyColumns = Boolean.valueOf( 
	params.getProperty(KEY_OMIT_EMPTY_COLS,
            OMIT_EMPTY_COLUMNS_BY_DEFAULT.toString())
    );
    // Show health ratings - use the option supplied from the form, or the
    // default if one of the other outputs was chosen
    boolean showHealthRatings = Boolean.valueOf( 
	params.getProperty(KEY_SHOW_HEALTH,
            SHOW_HEALTH_RATINGS_BY_DEFAULT.toString())
    );
    
    // Custom HTML parameters (from custom form)
    String action = params.getProperty(ACTION_TAG, "");
    // Manual custom ordering received from the text area
    String manualOrdering = params.getProperty(KEY_CUSTOM_ORDERING_LIST);
    // Last manual ordering (only received from customisation page)
    lastManualOrdering = params.getProperty(KEY_CUSTOM_ORDERING_PREVIOUS_MANUAL);
    // Set custom ordering to default 
    this.customFieldOrdering = FIELD_ORDERING_DEFAULT;
    
    // ---------- Interpret parameters ----------
    // Is this a custom output? Show custom options or apply to an export.
    // Set isCustom based on "customise" submit action
    // or "isCustom" flag in custom form; default false.
    boolean isCustom =
        (!StringUtil.isNullString(action) && action.equals(ACTION_CUSTOM))
            || Boolean.valueOf(params.getProperty(KEY_CUSTOM, "false"));

    // Are we exporting? Export button pressed, or custom output and OK/cancel.
    // OR Minimal args (outputFormat) configured and no action.
    this.doExport = (outputFormat!=null && StringUtil.isNullString(action)) ||
        action.equals(ACTION_EXPORT) ||
        (isCustom &&
            (action.equals(ACTION_CUSTOM_OK) || action.equals(ACTION_CUSTOM_CANCEL))
        );

    // ---------- Process parameters and show page ----------
    // If custom output, set the field ordering and omit flag
    if (isCustom) {
      // If custom export requested (from the output page) or a customisation was 
      // okayed, set the custom ordering to the supplied manual ordering. If an 
      // export is validated, set the last manual ordering.
      if (action.equals(ACTION_HIDE_CUSTOM_EXPORT)) {
        // hide custom form
        isCustom = false;
      } else if (action.equals(ACTION_CUSTOM) ||
                 action.equals(ACTION_CUSTOM_OK)) {
        // Try and parse the manual ordering into a list of valid field names
        setCustomFieldOrdering(manualOrdering);
        if (doExport) lastManualOrdering = manualOrdering;
      }
      // Cancel the customisation and set the ordering to the previously
      // applied value (from the session)
      else if (action.equals(ACTION_CUSTOM_CANCEL)) {
        setCustomFieldOrdering(manualOrdering);
        if (doExport) manualOrdering = lastManualOrdering;
        omitEmptyColumns = customOpts.isOmitEmptyColumns();
      }
      // Reset the ordering customisation to the default
      else if (action.equals(ACTION_CUSTOM_RESET)) {
        customFieldOrdering = FIELD_ORDERING_DEFAULT;
        //omitEmptyColumns = OMIT_EMPTY_COLUMNS_BY_DEFAULT;
      }
      
      // Create an object encapsulating the custom HTML options, and store it
      // in the session.
      customOpts = new KbartCustomOptions(omitEmptyColumns, showHealthRatings,
          customFieldOrdering);
      putSessionCustomOpts(customOpts);
    }
    // If this is not a valid custom or one-per-line output, reset the
    // session customisation
    else resetSessionOptions();


    // Just display the page if there is no export happening
    if (!doExport) {
      log.debug("No export requested; showing "+(isCustom?"custom":"main")+" options");
      // Show the appropriate half of the page depending on whether we are customising
      displayPage(isCustom);
      return;
    }

    // Now we are doing an export - create the exporter
    KbartExporter kexp = createExporter(outputFormat, selectedScope,
        reportDataFormat, coverageNotesFormat);

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
   * Attempt to set the custom field ordering using the given ordering string. If
   * the set fails, the errMsg is set and the doExport variable is set to false;
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
   * @param outputFormat the output format for the exporter
   * @param scope the scope of titles to export
   * @param reportDataFormat the format of the report
   * @return a usable exporter, or null if one could not be created
   */
  private KbartExporter createExporter(OutputFormat outputFormat, 
      ContentScope scope, ReportDataFormat reportDataFormat, 
      CoverageNotesFormat coverageNotesFormat) {

    // The following counts the number of TdbTitles informing the export, by 
    // processing the list of AUs in the given scope. It is provided as 
    // information in the export, but is actually a little meaningless and 
    // should probably be omitted.
    int numTdbTitles = TdbUtil.getNumberTdbTitles(scope);

    KbartCustomOptions opts = getSessionCustomOpts(false);

    // The list of KbartTitles to export; each title represents a TdbTitle over
    // a particular range of coverage.
    List<KbartTitle> titles = getKbartTitlesForExport(scope);
    //KbartTitleIterator titles = getKbartTitlesForExport(scope);
 
    /*log.info(String.format("Creating exporter for %d titles in scope %s\n",
	titles.size(), scope));*/
    log.info(String.format("Creating exporter for titles in scope %s\n", scope));
    
    // Return if there are no titles
    if (titles.isEmpty()) {
      errMsg = "No "+scope.label+" titles for export.";
      return null;
    }

    // Process the titles using a report format, to add supplementary data or
    // amalgamate records as required
    titles = ReportFormat.process(titles, coverageNotesFormat, reportDataFormat);

    // Create a filter
    KbartExportFilter filter;
    if (opts !=null) {
      filter = new KbartExportFilter(titles, opts.getFieldOrdering(),
          opts.isOmitEmptyColumns(), opts.isShowHealthRatings());
    } else {
      filter = new KbartExportFilter(titles);
    }

    // Create and configure an exporter
    KbartExporter kexp = outputFormat.makeExporter(titles, filter);
    kexp.setTdbTitleTotal(numTdbTitles);
    kexp.setContentScope(scope);
    
    // Set an HTML form for the HTML output if necessary
    if (kexp.getOutputFormat().isHtml()) {
      kexp.setHtmlCustomForm(makeHtmlCustomForm());
    }

    return kexp;    
  }
  
  /**
   * Get the list of TdbTitles or AUs in the given scope, and turn them into
   * KbartTitles which represent the coverage ranges available for titles in 
   * the scope.
   * 
   * @param scope the scope of titles to create
   * @return an iterator over KbartTitles
   */
  /*private KbartTitleIterator getKbartTitlesForExport(ContentScope scope) {
    // If we are exporting in a scope where ArchivalUnits are not available,
    // act on a list of TdbTitles, with their full AU ranges. 
    if (!scope.areAusAvailable) {
      Collection<TdbTitle> tdbTitles = TdbUtil.getTdbTitles(scope);
      return KbartTitleIterator.getKbartTitleIterator(tdbTitles);
      //titles = KbartConverter.convertTitles(tdbTitles);
    }
    // Otherwise we need to look at the lists of individual AUs in order to
    // calculate ranges
    else {
      // Whether the output will include any range fields; if there is no custom 
      // ordering, then the default will be used, which will include range fields
      boolean rangeFieldsIncluded = KbartExportFilter.includesRangeFields(
	  getSessionCustomOpts().getFieldOrdering().getOrdering());
      Collection<ArchivalUnit> aus = TdbUtil.getAus(scope);
      Map<TdbTitle, List<ArchivalUnit>> map = TdbUtil.mapTitlesToAus(aus);
      return KbartTitleIterator.getKbartTitleIterator(map.values().iterator(),
          getShowHealthRatings(), rangeFieldsIncluded);
    }
  }*/

  /**
   * Get the list of TdbTitles or AUs in the given scope, and turn them into
   * KbartTitles which represent the coverage ranges available for titles in
   * the scope.
   *
   * @param scope the scope of titles to create
   * @return a list of KbartTitles
   */
  private List<KbartTitle> getKbartTitlesForExport(ContentScope scope) {

    List<KbartTitle> titles;
    Iterator<KbartTitle> titleIterator;
    // If we are exporting in a scope where ArchivalUnits are not available,
    // act on a list of TdbTitles, with their full AU ranges.
    if (!scope.areAusAvailable) {
      Collection<TdbTitle> tdbTitles = TdbUtil.getTdbTitles(scope);
      // TODO Sort the TdbTitles first if we are expecting an iterator back (??)
      titles = KbartConverter.convertTitles(tdbTitles);
      // TODO titleIterator = new KbartConverter.TdbTitleKbartTitleIterator(tdbTitles.iterator());
    }
    // Otherwise we need to look at the lists of individual AUs in order to
    // calculate ranges
    else {
      // Whether the output will include any range fields; if there is no custom
      // ordering, then the default will be used, which will include range fields
      boolean rangeFieldsIncluded = KbartExportFilter.includesRangeFields(
          getSessionCustomOpts().getFieldOrdering().getOrdering());
      if (scope == ContentScope.COLLECTED && useMetadataForPreserved()) {
        // try listing collected content from the metadata database first;
        // list of bibliographic items is assumed to be sorted by ISSN
        // TODO (PJG) note: should return AUs from DB and 
        // show aggregate health of AU for each title
        List<? extends BibliographicItem> items = 
            MetadataDatabaseUtil.getBibliographicItems();
        if (items.size() > 0) {
          log.debug2("Found bibliographic items: " + items.size());
          titles = new ArrayList<KbartTitle>();
          int i = 0;
          if (i < items.size()) {
            for (int j = i+1; j <= items.size(); j++) {
              // convert portion of bibliographic items list with same ISSN
              if (   (j == items.size()) 
                  || !items.get(i).getIssn().equals(items.get(j).getIssn())) {
                if (log.isDebug3()) {
                  for (int k = i; k < j; k++) {
                    log.debug3("printIssn: " + items.get(k).getPrintIssn()
                               + " eissn: " + items.get(k).getEissn());
                  }
                }
                List<KbartTitle> currentTitles =
                  KbartConverter.convertTitleToKbartTitles(items.subList(i, j));
                titles.addAll(currentTitles);
                i = j;
              }
            }
          }
          if (log.isDebug3()) {
            for (KbartTitle title : titles) {
              log.debug3("printIssn: " 
                         + title.getField(KbartTitle.Field.PRINT_IDENTIFIER)
                         + "eIssn: " 
                         + title.getField(KbartTitle.Field.ONLINE_IDENTIFIER));
            }
          }
          return titles;
        }
      }
      
      // list content from the title database
      Collection<ArchivalUnit> aus = TdbUtil.getAus(scope);
      Map<TdbTitle, List<ArchivalUnit>> map = TdbUtil.mapTitlesToAus(aus);
      titles = KbartConverter.convertTitleAus(map.values(), getShowHealthRatings(),
        rangeFieldsIncluded);
      //TODO titleIterator = new KbartConverter.AuKbartTitleIterator(
      // map.values().iterator(), getShowHealthRatings(), rangeFieldsIncluded);

    }
    // TODO Sort here if not performed in KbartConverter
    return titles;
  }

  /**
   * Assign an HTML form of custom options to the exporter if necessary.
   * @param kexp the exporter 
   */
  /*private void assignHtmlCustomForm(KbartExporter kexp) {
    if (kexp.getOutputFormat().isHtml()) {
      kexp.setHtmlCustomForm(makeHtmlCustomForm());
    }
  }*/

  /**
   * Use the supplied exporter to export the data to the given output stream.
   *
   * @param kexp an exporter to use for export
   * @param out an output stream for the export
   * @throws IOException
   */
  public void doExport(KbartExporter kexp, OutputStream out) throws IOException {
    long s = TimeBase.nowMs();
    kexp.export(out);
    log.debug("Export took approximately "
	      + StringUtil.timeIntervalToString(TimeBase.msSince(s)));
    out.flush();
    out.close();
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
    resp.setContentType(
        (kexp.isCompress() ? "application/zip" : outputFormat.getMimeType()) +
            ";charset="+ENCODING);
    resp.setCharacterEncoding(ENCODING);
    //resp.setContentLength(  );

    // Export to the response OutputStream
    doExport(kexp, resp.getOutputStream());
    
    // Check errors (Note: the response has already been written by here, so
    // there is no point setting the err/status msgs)
    List<String> errs = kexp.getErrors();
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
    return srvAbsURL(myServletDescr(), "format=" + OUTPUT_DEFAULT.name());
  }
  
  /**
   * Determine whether "preserved" option is enabled.
   * 
   * @return <code>true</code> if "preserved" option is enabled.
   */
  private boolean isEnablePreserved() {
    return CurrentConfig.getBooleanParam(PARAM_ENABLE_PRESERVED_HOLDINGS, 
					 DEFAULT_ENABLE_PRESERVED_HOLDINGS);
  }

  /**
   * Determine whether "metadata" option is enabled for preserved content.
   * 
   * @return <code>true</code> if use metadata option is enabled
   */
  private boolean useMetadataForPreserved() {
    return CurrentConfig.getBooleanParam(
        PARAM_USE_METATATA_FOR_PRESERVED_HOLDINGS, 
        DEFAULT_USE_METADATA_FOR_PRESERVED_HOLDINGS);
  }

  /**
   * Generate a table with the page components and options. 
   *
   * @param custom whether to show the customisation options
   * @return a Jetty table with all the page's options
   */
  protected Table layoutTableOfOptions(boolean custom) {
    // Get the path to this servlet so we can postfix output format path
//    String thisPath = myServletDescr().path;
    Table tab = new Table(0, "align=\"center\" width=\"80%\"");
    //addBoxSummary(tab);

    tab.newRow();
    tab.newCell("align=\"center\"");
    Tdb tdb = TdbUtil.getTdb();
    if (tdb==null || tdb.isEmpty()) {
      tab.add("No titlesets are defined.");
      addBlankRow(tab);
      return tab;
    }

    // Create a form for whatever options we are showing
    Form form = ServletUtil.newForm(srvURL(myServletDescr()));
    form.add(new Input(Input.Hidden, "isForm", "true"));
    if (isEnablePreserved()) {
      form.add("View or export a list of titles " +
      	"that are available for collection, configured for collection, " +
    	"or actually collected in your LOCKSS box."
      );
      // Footnote for HealthMetric-based preserved option
      /*form.add(addFootnote("Titles in the 'ingested' output are " +
    	"included based on a metric which assigns each configured volume " +
    	"a health value. This health value is currently experimental and the " +
    	"output of this option should not yet be considered authoritative."
      ));*/
      /*form.add(addFootnote("Titles in the 'collected' output are " +
          "included based on whether each configured volume appears " +
          "to have 'substance', that is whether enough material has been " +
          "ingested."
      ));*/
    } else {
      form.add("View or export a list of titles " +
      "available for collection, or configured for collection in " +
      "your LOCKSS box."
      );
    }
    form.add(BREAK);
    form.add(String.format(
	"There are %s titles available for collection, from %s publishers.", 
	tdb.getTdbTitleCount(), tdb.getTdbPublisherCount()
    ));
      form.add(BREAK);
    // Add an option to select the scope of exported holdings
    form.add(layoutScopeOptions());

    // Add compress option (disabled as the CSV output is not very large)
    //tab.newRow();
    //tab.newCell("align=\"center\"");
    //tab.add(ServletUtil.checkbox(this, KEY_COMPRESS, KEY_COMPRESS, "Compress the output", false));
    
    // Create a sub table within the form
    Table subTab = new Table(0, "align=\"center\" width=\"80%\"");
    subTab.newRow();
    subTab.newCell("align=\"center\"");
    addReportFormatOptions(subTab);
    subTab.newRow();
    subTab.newCell("align=\"center\"");
    addOutputFormatOptions(subTab);
    subTab.newRow();
    subTab.newCell("align=\"center\"");
    //addBlankRow(subTab);
    subTab.add(BREAK);
    subTab.newRow();
    subTab.newCell("align=\"center\"");

    // Add the appropriate options to the sub table in the form
    if (custom) {
      // Add "hide custom" button at top
      layoutSubmitButton(this, subTab, ACTION_TAG, ACTION_HIDE_CUSTOM_EXPORT);
      // Add HTML customisation options
      subTab.add(new Heading(3, ACTION_CUSTOM));
      layoutFormCustomOpts(subTab);
      subTab.add(BREAK);
      layoutSubmitButton(this, subTab, ACTION_TAG, ACTION_CUSTOM_OK);
    } else {
      // Show customise button and advice
      layoutSubmitButton(this, subTab, ACTION_TAG, ACTION_CUSTOM);
      if (isEnablePreserved()) {
        subTab.add(BREAK+"By default, list is in the industry-standard KBART " +
      		"format. Alternatively you can customise the list to define " +
      		"which columns are visible and in what order they appear. " +
      		"Use this option also to add health ratings to the output."+BREAK);
      } else {
        subTab.add(BREAK+"By default, list is in the industry-standard KBART " +
            "format. Alternatively you can customise the list to define " +
            "which columns are visible and in what order they appear. "+BREAK);
      }
      // Show the option to customise the export details, which are KBART by default:
      //form.add(new Input(Input.Hidden, KEY_TITLE_SCOPE, selectedScope.name()));
      form.add(new Input(Input.Hidden, KEY_COVERAGE_NOTES_FORMAT, 
                         COVERAGE_NOTES_DEFAULT.toString()));
      form.add(new Input(Input.Hidden, KEY_CUSTOM_ORDERING_LIST, 
                         getOrderingAsCustomFieldList(FIELD_ORDERING_DEFAULT)));
      form.add(new Input(Input.Hidden, KEY_OMIT_EMPTY_COLS, 
                         htmlInputTruthValue(false)));
      // Add the submit at the bottom
      layoutSubmitButton(this, subTab, ACTION_TAG, ACTION_EXPORT);
    }
    
    // Add some space
    addBlankRow(tab);
    addBlankRow(tab);
    // Add the sub table to the form, and the form to the main table
    form.add(subTab);

    tab.newRow();
    tab.newCell("align=\"center\"");
    tab.add(form);
    return tab;
  }

  /**
   * Layout output data format options (CSV, TSV, screen).
   * @param tab
   */
  private void addOutputFormatOptions(Table tab) {
    // Add default output formats
    //tab.add("Please choose one of the following actions.<br/>");
    tab.newRow();
    tab.newCell("align=\"center\" valign=\"middle\"");
    tab.add("Output: ");

    for (OutputFormat fmt : OutputFormat.values()) {
      boolean selected = outputFormat!=null ?
          fmt==outputFormat : fmt==OUTPUT_DEFAULT;

      tab.add(ServletUtil.radioButton(this, KEY_OUTPUT_FORMAT, fmt.name(),
          fmt.getLabel(), selected));
      tab.add(addFootnote(fmt.getFootnote()));
      tab.add(" &nbsp; ");
    }
  }

  /**
   * Layout output report format options (KBART, title-per-line, SFX DataLoader).
   * These are base options, which may be modified by customisation.
   * @param tab
   */
  private void addReportFormatOptions(Table tab) {
    tab.newRow();
    tab.newCell("align=\"center\" valign=\"middle\"");
    tab.add("Format: ");

    for (ReportDataFormat fmt : ReportDataFormat.values()) {
      boolean selected = reportDataFormat!=null ?
          fmt==reportDataFormat : fmt==REPORT_DEFAULT;

      tab.add(ServletUtil.radioButton(this, KEY_REPORT_FORMAT, fmt.name(),
          fmt.getLabel(), selected));
      tab.add(addFootnote(fmt.getFootnote()));
      tab.add(" &nbsp; ");
    }
  }

  /**
   * Layout coverage note format options.
   * @param tab
   */
  private void addCoverageNoteFormatOptions(Table tab) {
    tab.newRow();
    tab.newCell("align=\"center\" valign=\"middle\"");
    //tab.add("Format: ");
    tab.add("<br/>Choose a format for ranges in the coverage field:<br/>");
    int count = 0;
    for (CoverageNotesFormat fmt : CoverageNotesFormat.values()) {
      // Start a new row every 3 options
      if (count++%3==0) {
        tab.newRow();
        tab.newCell("align=\"center\" valign=\"middle\"");
      }
      boolean selected = coverageNotesFormat!=null ?
          fmt==coverageNotesFormat : fmt==COVERAGE_NOTES_DEFAULT;
      tab.add(ServletUtil.radioButton(this, KEY_COVERAGE_NOTES_FORMAT, fmt.name(),
          fmt.label, selected));
      tab.add(" &nbsp; ");
    }
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
   * Layout content scope options.
   */
  private Table layoutScopeOptions() {
    Table tab = new Table();
    tab.newRow();
    tab.newCell("align=\"center\" valign=\"middle\"");
    tab.add("Show: ");
    for (ContentScope scope : ContentScope.values()) {
      //long s = System.currentTimeMillis();
      boolean scopeEnabled = true;
      if (scope==ContentScope.COLLECTED) {
        if (!isEnablePreserved()) continue;
        scopeEnabled = AuHealthMetric.isSupported();
      }
      // NOTE: getNumberTdbTitles() first has to produce a full list of titles;
      // this is expensive, in particular for the Preserved option, and so
      // should only be done when required.
      int total = TdbUtil.getNumberTdbTitles(scope);
      /*log.debug(String.format("Title count %s took approximately %ss",
          scope, (System.currentTimeMillis()-s)/1000
      ));*/
      String label = String.format("%s (%d)", scope.label, total);
      tab.add(ServletUtil.radioButton(this, KEY_TITLE_SCOPE, scope.name(),
          label, scope==selectedScope, scopeEnabled));
      if (!scopeEnabled) tab.add(notAvailFootnote);
      tab.add(" &nbsp; ");
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
    endPage(page);
  }

  /**
   * Create a form of options for custom HTML output. This includes options to
   * use a custom field list or ordering, and to omit empty columns.
   * 
   * @return an HTML form 
   */
  private void layoutFormCustomOpts(Composite comp) {
    
    // Get the opts from the session
    KbartCustomOptions opts = getSessionCustomOpts();
    
    // Add a format parameter
    //comp.add(new Input(Input.Hidden, KEY_FORMAT, OutputFormat.HTML.name()));
    // Add a hidden field listing the last manual ordering
    comp.add(new Input(Input.Hidden, KEY_CUSTOM_ORDERING_PREVIOUS_MANUAL,
        lastManualOrdering));
    comp.add(new Input(Input.Hidden, KEY_CUSTOM, "true"));

    // Add one-per-line customisation opts
    Table covTab = new Table();
    addCoverageNoteFormatOptions(covTab);
    covTab.add(BREAK);
    comp.add(covTab);

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
    
    comp.add(
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
    tab.add(new TextArea(KEY_CUSTOM_ORDERING_LIST, 
        getOrderingAsCustomFieldList(opts.getFieldOrdering())).setSize(taCols, taLines));
    // Omit empty columns option
    tab.add("<br/>");
    tab.add(ServletUtil.checkbox(this, KEY_OMIT_EMPTY_COLS, 
        Boolean.TRUE.toString(), "Omit empty columns<br/>",
        opts.isOmitEmptyColumns()));
    // Show health option if available
    if (isEnablePreserved() && getShowHealthRatings()) {
      tab.add(ServletUtil.checkbox(this, KEY_SHOW_HEALTH,
          Boolean.TRUE.toString(), "Show health ratings",
          opts.isShowHealthRatings(), AuHealthMetric.isSupported()));
      if (!AuHealthMetric.isSupported()) {
        tab.add(notAvailFootnote);
      } else {
        tab.add(addFootnote("Health ratings will only be shown for titles which " +
                            "you have configured on your box. The '"+ContentScope.ALL+"' export " +
                            "will not show health ratings."));
      }
    }
    // Add buttons
    tab.add("<br/><br/><center>");
    //layoutSubmitButton(this, tab, ACTION_TAG, ACTION_CUSTOM_OK);
    layoutSubmitButton(this, tab, ACTION_TAG, ACTION_CUSTOM_RESET);
    layoutSubmitButton(this, tab, ACTION_TAG, ACTION_CUSTOM_CANCEL);
    tab.add("</center>");
    
    // Add a legend for the fields
    tab.newCell("align=\"left\" padding=\"10\" valign=\"middle\"");
    tab.add("<br/>"+getKbartFieldLegend());
    
    comp.add(tab);
  }



  /**
   * Turn the selected ordering into a string containing a separated list of fields. 
   * @return
   */
  private static String getOrderingAsCustomFieldList(FieldOrdering fo) {
    return StringUtils.join(fo.getOrderedLabels(), 
	CustomFieldOrdering.CUSTOM_ORDERING_FIELD_SEPARATOR);
    /*return StringUtils.join(fo.getOrdering(), 
	CustomFieldOrdering.CUSTOM_ORDERING_FIELD_SEPARATOR);*/
  }
  
  /**
   * Construct an HTML form providing a link to customisation options for output.
   * This consists of a hidden list of ordered output fields, and a submit button,
   * and will appear on the output page.
   * 
   * @return a Jetty form
   */
  private Form makeHtmlCustomForm() {
    // Get the opts from the session
    KbartCustomOptions opts = getSessionCustomOpts();
    String servletUrl = srvURL(myServletDescr());
    Form form = ServletUtil.newForm(servletUrl);
    // Indicate that we are expecting custom out
    form.add(new Input(Input.Hidden, KEY_CUSTOM, "true"));
    form.add(new Input(Input.Hidden, KEY_TITLE_SCOPE, selectedScope.name()));
    form.add(new Input(Input.Hidden, KEY_CUSTOM_ORDERING_LIST,
        getOrderingAsCustomFieldList(opts.getFieldOrdering())));
    form.add(new Input(Input.Hidden, KEY_OMIT_EMPTY_COLS,
        htmlInputTruthValue(opts.isOmitEmptyColumns())));
    form.add(new Input(Input.Hidden, KEY_SHOW_HEALTH,
        htmlInputTruthValue(opts.isShowHealthRatings())));
    form.add(new Input(Input.Hidden, KEY_OUTPUT_FORMAT, outputFormat.name()));
    form.add(new Input(Input.Hidden, KEY_REPORT_FORMAT, reportDataFormat.name()));
    form.add(new Input(Input.Hidden, KEY_COVERAGE_NOTES_FORMAT, coverageNotesFormat.name()));
    ServletUtil.layoutSubmitButton(this, form, ACTION_TAG, ACTION_CUSTOM);
    form.add(new Link(servletUrl, "Return to main title list page"));
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
  private static void layoutButton(LockssServlet servlet, 
      Composite composite, String key, String value, String type) {
    Input submit = new Input(type, key, value);
    servlet.setTabOrder(submit);
    composite.add(submit);
  }

  private static void layoutSubmitButton(LockssServlet servlet, 
      Composite composite, String key, String value) {
    layoutButton(servlet, composite, key, value, Input.Submit); 
  }
  private static void layoutResetButton(LockssServlet servlet, 
      Composite composite, String key, String value) {
    layoutButton(servlet, composite, key, value, Input.Reset); 
  }

  
  /**
   * Get the string representation of a boolean value, appropriate for use as
   * the value of a form input.
   * @param b a boolean value
   * @return a string which will yield the same value when interpreted as the value of a form input
   */
  private static String htmlInputTruthValue(boolean b) {
    return b ? "true" : "false"; 
  }

  /**
   * Add a blank row to a table or a line break to any other composite element.
   */
  private static void addBlankRow(Composite comp) {
    if (comp instanceof Table) {
      Table tab = (Table)comp;
      tab.newRow();
      tab.newCell();
      tab.add("&nbsp;");
    }
    else comp.add("<br/>");
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
    tab.add("The permanent KBART output URL for this server is:<br/>" +
        "<b><font color=\"navy\">"+getDefaultUpdateUrl()+"</font></b>");
    addBlankRow(tab);
  }

  
  protected boolean getShowHealthRatings() {
    KbartCustomOptions opts = getSessionCustomOpts();
    return opts == null ? SHOW_HEALTH_RATINGS_BY_DEFAULT : opts.isShowHealthRatings();
  }
  
  /**
   * Get the current custom HTML options from the session. If the cookie is not
   * set, a new set of options is created and added to the session. This is a
   * convenience method which does not return null.
   * @return a CustomOptions object from the session
   */
  protected KbartCustomOptions getSessionCustomOpts() {
    return getSessionCustomOpts(true); 
  }

  /**
   * Get the current custom HTML options from the session. If the cookie is not
   * set, then either null is returned, or a new options object is added to the 
   * session and returned, depending on the value of <code>createIfAbsent</code>.
   * This can be useful for testing whether custom options are available
   *  
   * @param createIfAbsent whether to create a new CustomOptions in the session
   * @return a CustomOptions object from the session, or null
   */
  protected KbartCustomOptions getSessionCustomOpts(boolean createIfAbsent) {
    Object o = getSession().getAttribute(SESSION_KEY_CUSTOM_OPTS);
    if (o==null && createIfAbsent) {
      KbartCustomOptions opts = KbartCustomOptions.getDefaultOptions();
     putSessionCustomOpts(opts);
     return opts;
    }
    return o==null ? null : (KbartCustomOptions)o;
  }
  
  /**
   * Puts the supplied custom HTML options into the session.
   * @param opts a CustomOptions object
   */
  protected void putSessionCustomOpts(KbartCustomOptions opts) {
    getSession().setAttribute(SESSION_KEY_CUSTOM_OPTS, opts);
  }

  /**
   * Put a default set of options in the session.
   */
  protected void resetSessionOptions() {
    putSessionCustomOpts(KbartCustomOptions.getDefaultOptions());
  }

}
