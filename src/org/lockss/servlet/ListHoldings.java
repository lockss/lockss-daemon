/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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
import org.apache.commons.lang3.StringUtils;
import org.lockss.app.LockssDaemon;
import org.lockss.config.*;
import org.lockss.config.TdbUtil.ContentScope;
import org.lockss.config.TdbUtil.ContentType;
import org.lockss.daemon.AuHealthMetric;
import org.lockss.metadata.MetadataDatabaseUtil;
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
 * The servlet is accessible via direct URL, using the parameters
 * scope, format, report and coverageNotesFormat. For this to work, the bare
 * minimum parameter of 'format' must be supplied, and its presence must be
 * recognised by the servlet to indicate that an export is taking place.
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
  
  private static final Logger log = Logger.getLogger(ListHoldings.class);

  static final String PREFIX = Configuration.PREFIX + "listHoldings.";

  private static final String BREAK = "<br/><br/>";
  private static final String ENCODING = null;
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
    PARAM_USE_METADATA_FOR_PRESERVED_HOLDINGS =
      PREFIX + "useMetadataForPreserved";
  public static final boolean 
    DEFAULT_USE_METADATA_FOR_PRESERVED_HOLDINGS = false;

  // ------------------------------- URL PARAMS -------------------------------
  // These keys are used in the URL for direct access to particular reports.
  // DO NOT CHANGE
  public static final String KEY_TITLE_SCOPE = "scope";
  public static final String KEY_TITLE_TYPE = "type";
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
  /** Export the current report in some format. */
  public static final String ACTION_CUSTOM_EXPORT = "Export";

  // --------------------- I18N LABELS FOR SUBMIT BUTTONS ----------------------
  public static final String I18N_ACTION_EXPORT = i18n.tr("List Titles");
  public static final String I18N_ACTION_CUSTOM = i18n.tr("Customise Fields");
  public static final String I18N_ACTION_HIDE_CUSTOM_EXPORT = i18n.tr("Hide Customise Fields");
  public static final String I18N_ACTION_CUSTOM_OK = i18n.tr("List Titles");
  public static final String I18N_ACTION_CUSTOM_RESET = i18n.tr("Reset");
  public static final String I18N_ACTION_CUSTOM_CANCEL = i18n.tr("Cancel");
  public static final String I18N_ACTION_CUSTOM_EXPORT = i18n.tr("Export");

  // ------------------------- FORM PARAMS AND OPTIONS -------------------------
  public static final String KEY_COMPRESS = "compress";
  public static final String KEY_OMIT_EMPTY_COLS = "omitEmptyCols";
  public static final String KEY_OMIT_HEADER = "omitHeader";
  public static final String KEY_EXCLUDE_NOID_TITLES = "excludeNoIdTitles";
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
  /** Default scope is the default in the scope enum. */
  static final ContentScope SCOPE_DEFAULT = ContentScope.DEFAULT_SCOPE;
  /** Default type is the default in the type enum. */
  static final ContentType TYPE_DEFAULT = ContentType.DEFAULT_TYPE;

  /** Default approach to omitting empty fields - inherited from the exporter base class. */
  static final Boolean OMIT_EMPTY_COLUMNS_BY_DEFAULT = null;
  /** Default approach to omitting header row is false (do not omit). */
  static final Boolean OMIT_HEADER_ROW_BY_DEFAULT = null;
  /** Default approach to excluding id-less titles. */
  static final Boolean EXCLUDE_NOID_TITLES_BY_DEFAULT = null;
  /** Default approach to showing health ratings - inherited from the exporter base class. */
  static final Boolean SHOW_HEALTH_RATINGS_BY_DEFAULT = null;

  // -------------------------- STATE FOR URL PARAMS --------------------------
  // Bits of state that must be reset to defaults in resetLocals()
  /** Holdings scope option. */
  private ContentScope selectedScope = ContentScope.DEFAULT_SCOPE;
  private ContentType selectedType = ContentType.DEFAULT_TYPE;

  // ---------------------------- TRANSITORY STATE ----------------------------
  // Bits of state that must be reset in resetLocals()
  /** A record of the last manual ordering which was applied to an export;
   * maintained while the servlet is handling a request. */
  private String lastManualOrdering;
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
    doExport = false;
    // Reset export parameters to defaults
    selectedScope = ContentScope.DEFAULT_SCOPE;
    selectedType = ContentType.DEFAULT_TYPE;
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
      String fn = i18n.tr("Not available on this platform.");
      notAvailFootnote = addFootnote(fn);
    }
  }

  /**
   * Attempt to set the custom field ordering using the given ordering string. If
   * the set fails, the errMsg is set and the doExport variable is set to false.
   * @param ordering
   * @return whether the set succeeded
   */
  private boolean setCustomColumnOrdering(String ordering) {
    boolean success = false;
    return success;
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
	  getSessionCustomOpts().getColumnOrdering().getOrderedFields());
      Collection<ArchivalUnit> aus = TdbUtil.getAus(scope);
      Map<TdbTitle, List<ArchivalUnit>> map = TdbUtil.mapTitlesToAus(aus);
      return KbartTitleIterator.getKbartTitleIterator(map.values().iterator(),
          getShowHealthRatings(), rangeFieldsIncluded);
    }
  }*/

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
   * Constructs a string representing the direct update URL of the default output.
   * 
   * @return a string URL indicating the direct address for default output
   */
  public String getDefaultUpdateUrl() {
    return null;
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
    boolean useMetadata = 
      CurrentConfig.getBooleanParam(
        PARAM_USE_METADATA_FOR_PRESERVED_HOLDINGS,
        DEFAULT_USE_METADATA_FOR_PRESERVED_HOLDINGS);
    if (log.isDebug3())
      log.debug3(PARAM_USE_METADATA_FOR_PRESERVED_HOLDINGS+": "+useMetadata);
    return useMetadata;
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
      tab.add(i18n.tr("No titlesets are defined."));
      addBlankRow(tab);
      return tab;
    }

    // Create a form for whatever options we are showing
    Form form = ServletUtil.newForm(srvURL(myServletDescr()));
    form.add(new Input(Input.Hidden, "isForm", "true"));
    if (isEnablePreserved()) {
      form.add(i18n.tr("View or export a list of titles " +
      	"that are available for collection, configured for collection, " +
    	"or actually collected in your LOCKSS box."
      ));
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
      form.add(i18n.tr("View or export a list of titles " +
      "available for collection, or configured for collection in " +
      "your LOCKSS box."
      ));
    }
    form.add(BREAK);
    form.add(i18n.tr(
	"There are {0} titles available for collection, from {1} publishers.",
	tdb.getTdbTitleCount(), tdb.getTdbPublisherCount()
    ));
      form.add(BREAK);
    // Add an option to select the scope of exported holdings
    form.add(layoutScopeOptions());
    // Add an option to select the type of exported holdings
    form.add(layoutTypeOptions());

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
      ServletUtil.layoutSubmitButton(this, subTab, ACTION_TAG, ACTION_HIDE_CUSTOM_EXPORT,
          I18N_ACTION_HIDE_CUSTOM_EXPORT, false, true);
      // Add HTML customisation options
      subTab.add(new Heading(3, I18N_ACTION_CUSTOM));
      layoutFormCustomOpts(subTab);
      subTab.add(BREAK);
      ServletUtil.layoutSubmitButton(this, subTab, ACTION_TAG, ACTION_CUSTOM_OK,
          I18N_ACTION_CUSTOM_OK, false, true);
    } else {
      // Show customise button and advice
      ServletUtil.layoutSubmitButton(this, subTab, ACTION_TAG, ACTION_CUSTOM,
          I18N_ACTION_CUSTOM, false, true);
      if (isEnablePreserved()) {
        subTab.add(BREAK +
            i18n.tr("By default, list is in the industry-standard KBART " +
                "format. Alternatively you can customise the list to define " +
                "which columns are visible and in what order they appear. ") +
            (AuHealthMetric.isSupported() ?
                i18n.tr("Use this option also to add health ratings to the output.")
                : "") +
            BREAK);
      } else {
        subTab.add(BREAK+
            i18n.tr("By default, list is in the industry-standard KBART " +
                "format. Alternatively you can customise the list to define " +
                "which columns are visible and in what order they appear. ") +
            BREAK);
      }
      // Show the option to customise the export details, which are KBART by default:
      //form.add(new Input(Input.Hidden, KEY_TITLE_SCOPE, selectedScope.name()));
      form.add(new Input(Input.Hidden, KEY_OMIT_EMPTY_COLS,
          htmlInputTruthValue(false)));
      form.add(new Input(Input.Hidden, KEY_OMIT_HEADER,
          htmlInputTruthValue(false)));
      form.add(new Input(Input.Hidden, KEY_EXCLUDE_NOID_TITLES,
          htmlInputTruthValue(false)));
      // Add the submit at the bottom
      ServletUtil.layoutSubmitButton(this, subTab, ACTION_TAG, ACTION_EXPORT,
          I18N_ACTION_EXPORT);
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
    tab.add(i18n.tr("Output: "));
  }

  /**
   * Layout output report format options (KBART, title-per-line, SFX DataLoader).
   * These are base options, which may be modified by customisation.
   * @param tab
   */
  private void addReportFormatOptions(Table tab) {
    tab.newRow();
    tab.newCell("align=\"center\" valign=\"middle\"");
    tab.add(i18n.tr("Format: "));
  }

  /**
   * Layout coverage note format options.
   * @param tab
   */
  private void addCoverageNoteFormatOptions(Table tab) {
    tab.newRow();
    tab.newCell("align=\"center\" valign=\"middle\"");
    //tab.add("Format: ");
    tab.add("<br/>"+i18n.tr("Choose a format for ranges in the coverage field:")+"<br/>");
    int count = 0;
  }


  /**
   * Create a table listing KBART field labels and descriptions.
   */
  private static Table getKbartFieldLegend() {
    Table tab = new Table();
    //tab.style("border: 1px solid black;");
    return tab;
  }

  /**
   * Layout content scope options.
   */
  private Table layoutScopeOptions() {
    Table tab = new Table();
    tab.newRow();
    tab.newCell("align=\"center\" valign=\"middle\"");
    tab.add(i18n.tr("Show: "));
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
      long total = TdbUtil.getNumberTdbTitles(scope, ContentType.ALL);
      if (scope == ContentScope.COLLECTED && useMetadataForPreserved()) {
        // use number of publications in the metadata database if greater
        // to account for file transfer content titles; this also handles
        // cases where indexing is disabled or is not yet complete
        total = Math.max(total, 
                         LockssDaemon.getLockssDaemon()
                                     .getMetadataManager()
                                     .getPublicationCount());
      }
      /*log.debug(String.format("Title count %s took approximately %ss",
          scope, (System.currentTimeMillis()-s)/1000
      ));*/
      String label = String.format("%s (%d)", i18n.tr(scope.label), total);
      tab.add(ServletUtil.radioButton(this, KEY_TITLE_SCOPE, scope.name(),
          label, scope==selectedScope, scopeEnabled));
      if (!scopeEnabled) tab.add(notAvailFootnote);
      tab.add(" &nbsp; ");
    }
    return tab;
  }

  /**
   * Layout content type options.
   */
  private Table layoutTypeOptions() {
    Table tab = new Table();
    tab.newRow();
    tab.newCell("align=\"center\" valign=\"middle\"");
    tab.add(i18n.tr("Title type: "));
    for (ContentType type : ContentType.values()) {
      boolean typeEnabled = true;
      tab.add(ServletUtil.radioButton(this, KEY_TITLE_TYPE, type.name(),
          type.label, type==selectedType, typeEnabled));
      if (!typeEnabled) tab.add(notAvailFootnote);
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
   * use a custom field list or ordering, and to omit empty columns or the header.
   * 
   * @return an HTML form 
   */
  private void layoutFormCustomOpts(Composite comp) {

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
    for (PredefinedColumnOrdering order: PredefinedColumnOrdering.values()) {
      form.add( 
	  ServletUtil.radioButton(this, KEY_CUSTOM_ORDERING, order.name(), 
	      order.displayName+" <span style=\"font-style:italic;font-size:small\">("+order.description+")</span><br/>", order==COLUMN_ORDERING_DEFAULT)
      );
    }
     */
    
    comp.add(
        "<br/>"+i18n.tr("Use the box below to provide a custom ordering for the fields - one field per line.")+
            "<br/>"+i18n.tr("Omit any fields you don't want, but include an identifying field for sensible results.")+
            //"<br/>"+i18n.tr("(" + StringUtils.join(Field.getLabels(Field.idFields), LIST_COMMA) + ")")+
            "<br/>"+i18n.tr("There is a description of the KBART fields next to the box; identifying fields are shown in bold.")+
            "<br/><br/>"
    );

    Table tab = new Table();
    tab.newRow();
    tab.newCell("align=\"center\" valign=\"middle\"");
    
    // Add a text area of an appropriate size
    int taCols = 25; // this should be the longest field width
    tab.add(i18n.tr("Field ordering")+"<br/>");
    // Omit empty columns option
    tab.add("<br/>");
    // Omit header option
    tab.add("<br/>");
    // Exclude no-id titles option
    /*tab.add("<br/>");
    tab.add(ServletUtil.checkbox(this, KEY_EXCLUDE_NOID_TITLES,
        Boolean.TRUE.toString(), i18n.tr("Exclude titles with no title_id")+"<br/>",
        opts.isExcludeNoIdTitles()));*/
    // Show health option if available
    if (isEnablePreserved() && getShowHealthRatings()) {
      if (!AuHealthMetric.isSupported()) {
        tab.add(notAvailFootnote);
      } else {
        tab.add(addFootnote(i18n.tr("Health ratings will only be shown for titles which " +
                            "you have configured on your box. The '{0}' export " +
                            "will not show health ratings.", ContentScope.ALL)));
      }
    }
    // Add buttons
    tab.add("<br/><br/><center>");
    ServletUtil.layoutSubmitButton(this, tab, ACTION_TAG, ACTION_CUSTOM_RESET,
        I18N_ACTION_CUSTOM_RESET, false, true);
    ServletUtil.layoutSubmitButton(this, tab, ACTION_TAG, ACTION_CUSTOM_CANCEL,
        I18N_ACTION_CUSTOM_CANCEL, false, true);
    tab.add("</center>");
    
    // Add a legend for the fields
    tab.newCell("align=\"left\" padding=\"10\" valign=\"middle\"");
    tab.add("<br/>"+getKbartFieldLegend());
    
    comp.add(tab);
  }
  
  /**
   * Construct an HTML form providing a link to customisation options for output.
   * This consists of a hidden list of ordered output fields, and a variety of
   * submit buttons, one for accessing the customisation options, and others
   * for directly exporting the current report in one of the non-HTML formats.
   * It is intended to be used on the output page.
   * 
   * @return a Jetty composite element
   */
  private Composite makeHtmlCustomForm() {
    String servletUrl = srvURL(myServletDescr());
    Composite panel = new Composite();
    panel.add(" &nbsp; ");
    // Provide selector for output options, and submit
    panel.add("<p>");
    panel.add(new Link(servletUrl, i18n.tr("Return to main title list page")));
    panel.add("</p>");
    return panel;
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
  /*private void addBoxSummary(Table tab) {
    tab.newRow();
    tab.newCell("align=\"center\"");
    tab.add(i18n.tr("This is the KBART Metadata Exporter for {0}",
        "<b>"+getMachineName()+"</b>."));

    tab.newRow();
    tab.newCell("align=\"center\"");
    tab.add(i18n.tr("The permanent KBART output URL for this server is:")+
        "<br/><b><font color=\"navy\">"+getDefaultUpdateUrl()+"</font></b>");
    addBlankRow(tab);
  }*/

  
  protected boolean getShowHealthRatings() {
    return SHOW_HEALTH_RATINGS_BY_DEFAULT;
  }

  /**
   * Put a default set of options in the session.
   */
  protected void resetSessionOptions() {
  }

}
