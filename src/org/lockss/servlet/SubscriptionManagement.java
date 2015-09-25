/*
 * $Id$
 */

/*

 Copyright (c) 2013-2015 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;
import org.apache.commons.collections.FactoryUtils;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.db.DbException;
import org.lockss.plugin.PluginManager;
import org.lockss.remote.RemoteApi.BatchAuStatus.Entry;
import org.lockss.subscription.BibliographicPeriod;
import org.lockss.subscription.Publisher;
import org.lockss.subscription.PublisherSubscription;
import org.lockss.subscription.SerialPublication;
import org.lockss.subscription.Subscription;
import org.lockss.subscription.SubscriptionManager;
import org.lockss.subscription.SubscriptionOperationStatus;
import org.lockss.subscription.SubscriptionOperationStatus.PublisherStatusEntry;
import org.lockss.subscription.SubscriptionOperationStatus.StatusEntry;
import org.lockss.util.ListUtil;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import org.mortbay.html.Block;
import org.mortbay.html.Composite;
import org.mortbay.html.Form;
import org.mortbay.html.Image;
import org.mortbay.html.Input;
import org.mortbay.html.Link;
import org.mortbay.html.Page;
import org.mortbay.html.Table;

/**
 * Subscription management servlet.
 * 
 * @author Fernando Garcia-Loygorri
 */
@SuppressWarnings("serial")
public class SubscriptionManagement extends LockssServlet {
  private static final Logger log = Logger
      .getLogger(SubscriptionManagement.class);

  // Prefix for the subscription configuration entries.
  private static final String PREFIX = Configuration.PREFIX + "subscription.";

  /**
   * The maximum number of entries that force a single-tab interface.
   */
  public static final String PARAM_MAX_SINGLE_TAB_COUNT = PREFIX
      + "maxSingleTabCount";
  public static final int DEFAULT_MAX_SINGLE_TAB_COUNT = 20;

  public static final String AUTO_ADD_SUBSCRIPTIONS_LINK_TEXT =
      "Synchronize Subscriptions";
  public static final String AUTO_ADD_SUBSCRIPTIONS_ACTION = "autoAdd";
  public static final String AUTO_ADD_SUBSCRIPTIONS_HELP_TEXT =
      "Subscribe to all titles with currently configured archival units";
  public static final String SHOW_ADD_PAGE_LINK_TEXT =
      "Add Titles To Subscription Management";
  public static final String SHOW_ADD_PAGE_ACTION = "showAdd";
  public static final String SHOW_ADD_PAGE_HELP_TEXT =
      "Manually add title subscription options";
  public static final String SHOW_UPDATE_PAGE_LINK_TEXT =
      "Update Existing Subscription Options";
  public static final String SHOW_UPDATE_PAGE_ACTION = "showUpdate";
  public static final String SHOW_UPDATE_PAGE_HELP_TEXT =
      "Change existing title subscription options";
  public static final String TRI_STATE_WIDGET_HIDDEN_ID_SUFFIX = "Hidden";
  public static final String TRI_STATE_WIDGET_HIDDEN_ID_UNSET_VALUE = "unset";
  private static final String ADD_SUBSCRIPTIONS_ACTION = "Add";
  private static final String UPDATE_SUBSCRIPTIONS_ACTION = "Update";

  private static final String SUBSCRIBED_RANGES_PARAM_NAME = "subscribedRanges";
  private static final String UNSUBSCRIBED_RANGES_PARAM_NAME =
      "unsubscribedRanges";

  private static final String SUBSCRIPTIONS_SESSION_KEY = "subscriptions";
  private static final String SUBSCRIBED_PUBLISHERS_SESSION_KEY =
      "subscribedPublishers";
  private static final String TOTAL_SUBSCRIPTION_SESSION_KEY =
      "totalSubscription";
  private static final String UNDECIDED_PUBLISHERS_SESSION_KEY =
      "undecidedPublishers";
  private static final String UNDECIDED_TITLES_SESSION_KEY = "undecidedTitles";
  private static final String BACK_LINK_TEXT_PREFIX = "Back to ";

  private static final String MISSING_COOKIES_MSG = "Please enable cookies";
  private static final String SESSION_EXPIRED_MSG = "Session expired";
  private static final String BAD_SUBSCRIBED_RANGES_MSG =
      "Invalid subscribed ranges";
  private static final String BAD_UNSUBSCRIBED_RANGES_MSG =
      "Invalid unsubscribed ranges";
  private static final String MISSING_TITLE_AUS_MSG =
      "Could not find title AUs";
  private static final String TOTAL_SUBSCRIPTION_WIDGET_ID =
      "totalSubscription";
  private static final String PUBLISHER_SUBSCRIPTION_WIDGET_ID_PREFIX =
      "publisherSubscription";
  private static final String PUBLICATION_SUBSCRIPTION_WIDGET_ID_PREFIX =
      "publicationSubscription";
  private static final int LETTERS_IN_ALPHABET = 26;
  private static final int DEFAULT_LETTERS_PER_TAB = 3;

  private static final String rangesFootnote =
      "Enter ranges separated by commas."
	  + "<br>Each range is of the form "
	  + "Year(Volume)(Issue)-Year(Volume)(Issue) where any element may be "
	  + "omitted; any empty rightmost parenthesis pair may be omitted too."
	  + "<br>If both range ends are identical, the dash and everything "
	  + "following it may be omitted."
	  + "<br>A range starting with a dash extends to infinity in the past."
	  + "<br>A range ending with a dash extends to infinity in the future."
	  + "<br>"
	  + "<br>Examples of valid ranges:"
	  + "<br>1954-2000(10)"
	  + "<br>1988(12)(28)"
	  + "<br>()(5)-"
	  + "<br>-2000(10)";

  // The column headers of the tabbed content.
  private List<String> tabColumnHeaderNames = null;

  // The CSS classes for the column headers of the tabbed content.
  private List<String> tabColumnHeaderCssClasses = null;

  private PluginManager pluginManager;
  private SubscriptionManager subManager;
  private int maxSingleTabCount;

  /**
   * Initializes the configuration when loaded.
   * 
   * @param config
   *          A ServletConfig with the servlet configuration.
   * @throws ServletException
   */
  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    pluginManager = getLockssDaemon().getPluginManager();
    subManager = getLockssDaemon().getSubscriptionManager();
    configure();
  }

  /**
   * Handles configuration options.
   */
  private void configure() {
    final String DEBUG_HEADER = "config(): ";
    Configuration config = ConfigManager.getCurrentConfig();

    // Get the number of publications beyond which a multi-tabbed interface is
    // to be used.
    maxSingleTabCount =
	config.getInt(PARAM_MAX_SINGLE_TAB_COUNT, DEFAULT_MAX_SINGLE_TAB_COUNT);
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "maxSingleTabCount = " + maxSingleTabCount);
  }

  /**
   * Processes the user request.
   * 
   * @throws IOException
   *           if any problem occurred writing the page.
   */
  public void lockssHandleRequest() throws IOException {
    final String DEBUG_HEADER = "lockssHandleRequest(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    // If the AUs are not started, display a warning message.
    if (!pluginManager.areAusStarted()) {
      displayNotStarted();
      return;
    }

    SubscriptionOperationStatus status;

    // The operation to be performed.
    String action = req.getParameter(ACTION_TAG);

    try {
      if (SHOW_ADD_PAGE_ACTION.equals(action)) {
	displayAddPage();
      } else if (SHOW_UPDATE_PAGE_ACTION.equals(action)) {
	displayUpdatePage();
      } else if (ADD_SUBSCRIPTIONS_ACTION.equals(action)) {
	status = addSubscriptions();
	displayResults(status, SHOW_ADD_PAGE_LINK_TEXT, SHOW_ADD_PAGE_ACTION);
      } else if (UPDATE_SUBSCRIPTIONS_ACTION.equals(action)) {
	status = updateSubscriptions();
	displayResults(status, SHOW_UPDATE_PAGE_LINK_TEXT,
	    	       SHOW_UPDATE_PAGE_ACTION);
      } else if (AUTO_ADD_SUBSCRIPTIONS_ACTION.equals(action)) {
	// Add the necessary subscription options so that all the configured AUs
	// fall in subscribed ranges and do not fall in any unsubscribed range.
	status = subManager.subscribeAllConfiguredAus();
	displayResults(status, AUTO_ADD_SUBSCRIPTIONS_LINK_TEXT, null);
      } else {
	displayAddPage();
      }
    } catch (DbException dbe) {
      throw new RuntimeException(dbe);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Displays the page used to add new subscription decisions.
   * 
   * @throws IOException
   *           if any problem occurred writing the page.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private void displayAddPage() throws IOException, DbException {
    final String DEBUG_HEADER = "displayAddPage(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    // Start the page.
    Page page = newPage();
    addJavaScript(page);
    addCssLocations(page);
    addJQueryLocations(page);

    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "pluginMgr.areAusStarted() = "
	+ pluginManager.areAusStarted());
    if (!pluginManager.areAusStarted()) {
      page.add(ServletUtil.notStartedWarning());
    }

    ServletUtil.layoutExplanationBlock(page,
	"Add new serial title subscription options");

    // Get the current value of the total subscription setting.
    Boolean totalSubscriptionSetting = null;

    if (subManager.isTotalSubscriptionEnabled()) {
      totalSubscriptionSetting = subManager.isTotalSubscription();
      if (log.isDebug3()) log.debug3(DEBUG_HEADER
	  + "totalSubscriptionSetting = " + totalSubscriptionSetting);
    }

    if (totalSubscriptionSetting != null) {
      layoutErrorBlock(page);

      // Create the form.
      Form form = ServletUtil.newForm(srvURL(myServletDescr()));

      form.add(getTotalSubscriptionTable(totalSubscriptionSetting));

      // Save the undecided publications in the session to compare after the
      // form is submitted.
      HttpSession session = getSession();

      session.setAttribute(TOTAL_SUBSCRIPTION_SESSION_KEY,
	  totalSubscriptionSetting);

      // Add the submit button.
      ServletUtil.layoutSubmitButton(this, form, ACTION_TAG,
	  ADD_SUBSCRIPTIONS_ACTION, "Update");

      // Add the tribox javascript.
      addFormJavaScriptLocation(form, "js/tribox.js");

      // Add the form to the page.
      page.add(form);
    } else {
      // Initialize the publishers for the publications for which no
      // subscription decision has been made.
      Map<String, Publisher> publishers = new HashMap<String, Publisher>();

      // Get the publications for which no subscription decision has been made
      // and populate their publishers.
      List<SerialPublication> publications =
	  subManager.getUndecidedPublications(publishers);

      if (publications.size() > 0) {
	layoutErrorBlock(page);

	// Create the form.
	Form form = ServletUtil.newForm(srvURL(myServletDescr()));

	if (subManager.isTotalSubscriptionEnabled()) {
	  form.add(getTotalSubscriptionTable(totalSubscriptionSetting));
	}

	// Determine whether to use a single-tab or multiple-tab interface.
	int lettersPerTab = DEFAULT_LETTERS_PER_TAB;

	if (publications.size() <= maxSingleTabCount) {
	  lettersPerTab = LETTERS_IN_ALPHABET;
	}

	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "lettersPerTab = " + lettersPerTab);

	// Create the tabs container, an HTML division element, as required by
	// jQuery tabs.
	Block tabsDiv = new Block(Block.Div, "id=\"tabs\"");

	// Add it to the form.
	form.add(tabsDiv);

	// Create the tabs on the page, add them to the tabs container and
	// obtain a map of the tab tables keyed by the letters they cover.
	Map<String, Table> divTableMap =
	    ServletUtil.createTabsWithTable(LETTERS_IN_ALPHABET, lettersPerTab,
		getTabColumnHeaderNames(), "sub-row-title",
		getColumnHeaderCssClasses(), tabsDiv);

	// Populate the tabs content with the publications for which no
	// subscription decision has been made.
	populateTabsPublications(publications, publishers, divTableMap);

	// Save the undecided publications in the session to compare after the
	// form is submitted.
	HttpSession session = getSession();
	session.setAttribute(UNDECIDED_TITLES_SESSION_KEY, publications);

	// Save the publishers in the session to compare after the form is
	// submitted.
	session.setAttribute(UNDECIDED_PUBLISHERS_SESSION_KEY, publishers);

	if (subManager.isTotalSubscriptionEnabled()) {
	  session.setAttribute(TOTAL_SUBSCRIPTION_SESSION_KEY,
	      totalSubscriptionSetting);
	}

	// Add the submit button.
	ServletUtil.layoutSubmitButton(this, form, ACTION_TAG,
	    ADD_SUBSCRIPTIONS_ACTION, ADD_SUBSCRIPTIONS_ACTION);

	// Add the tribox javascript.
	addFormJavaScriptLocation(form, "js/tribox.js");

	// Add the form to the page.
	page.add(form);
      } else {
	errMsg = "There are no subscriptions to add";
	layoutErrorBlock(page);
      }
    }

    // Add the link to go back to the previous page.
    ServletUtil.layoutBackLink(page,
	srvLink(AdminServletManager.SERVLET_BATCH_AU_CONFIG,
	    	BACK_LINK_TEXT_PREFIX
	    	+ getHeading(AdminServletManager.SERVLET_BATCH_AU_CONFIG)));

    // Finish up.
    endPage(page);

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides the HTML table with the Total Subscription widget.
   * 
   * @param totalSubscriptionSetting
   *          A Boolean with the state of the Total Subscription widget.
   * @return a Table representing the HTML table.
   */
  private Table getTotalSubscriptionTable(Boolean totalSubscriptionSetting) {
    String tsClassName = "total-subscription";
    Table tsTable = new Table(0, "class=\"" + tsClassName + "\"");
    tsTable.newRow();

    Block triBoxLabel = new Block(Block.Bold);
    triBoxLabel.add("Total Subscription");

    tsTable.addCell(triBoxLabel,
	"class=\"" + tsClassName + "\" align=\"right\" width=\"49%\"");

    Input checkBox = new Input(Input.Checkbox, TOTAL_SUBSCRIPTION_WIDGET_ID);
    checkBox.attribute("class", "tribox");
    checkBox.attribute("id", TOTAL_SUBSCRIPTION_WIDGET_ID);

    tsTable.addCell(checkBox,
	"class=\"" + tsClassName + "\" align=\"center\" width=\"1%\"");

    Block textBlock = new Block(Block.Bold);
    textBlock.add(getTriStateDisplayTextSpan(TOTAL_SUBSCRIPTION_WIDGET_ID,
	totalSubscriptionSetting));

    tsTable.addCell(textBlock,
	"class=\"" + tsClassName + "\" align=\"left\" width=\"49%\"");

    tsTable.addCell(getTriStateHiddenInput(TOTAL_SUBSCRIPTION_WIDGET_ID,
	totalSubscriptionSetting), "class=\"" + tsClassName + "\" width=\"0\"");

    return tsTable;
  }

  /**
   * Provides a span widget used to display the text that represents the state
   * of a tri-state widget.
   * 
   * @param baseName
   *          A String with the name of the tri-state widget.
   * @param value
   *          A Boolean with the state of the widget.
   * @return a Block with the span showing the text that represents the state of
   *         the tri-state widget.
   */
  private Block getTriStateDisplayTextSpan(String baseName, Boolean value) {
    Block triStateDisplayTextSpan = new Block(Block.Span);
    triStateDisplayTextSpan.attribute("id", baseName + "Text");

    return triStateDisplayTextSpan;
  }

  /**
   * Provides a hidden input widget used to initialize and submit the state of a
   * tri-state widget.
   * 
   * @param baseName
   *          A String with the name of the tri-state widget.
   * @param value
   *          A Boolean with the state of the widget.
   * @return an Input representing the hidden input widget.
   */
  private Input getTriStateHiddenInput(String baseName, Boolean value) {
    Input triStateHiddenInput;
    String name = baseName + TRI_STATE_WIDGET_HIDDEN_ID_SUFFIX;

    if (value == null) {
      triStateHiddenInput =
	  new Input(Input.Hidden, name, TRI_STATE_WIDGET_HIDDEN_ID_UNSET_VALUE);
    } else {
      triStateHiddenInput = new Input(Input.Hidden, name, value.toString());
    }

    triStateHiddenInput.attribute("id", name);

    return triStateHiddenInput;
  }

  /**
   * Provides the column headers of the tabbed content.
   * 
   * @return a List<String> with the column headers of the tabbed content.
   */
  private List<String> getTabColumnHeaderNames() {
    // Lazy loading.
    if (tabColumnHeaderNames == null) {
      tabColumnHeaderNames = (List<String>)ListUtil
      .list("Publisher/Publication<br />Overall Subscription",
	    "Subscribed<br />Publication Ranges&nbsp;"
		+ addFootnote(rangesFootnote),
	    "Unsubscribed<br />Publication Ranges&nbsp;"
		+ addFootnote(rangesFootnote));
    }

    return tabColumnHeaderNames;
  }

  /**
   * Provides the CSS classes for the column headers of the tabbed content.
   * 
   * @return a List<String> with the CSS classes for the column headers of the
   *         tabbed content.
   */
  private List<String> getColumnHeaderCssClasses() {
    // Lazy loading.
    if (tabColumnHeaderCssClasses == null) {
      tabColumnHeaderCssClasses = (List<String>)ListUtil
      .list("sub-column-header-subscribe-all",
	    "sub-column-header-subscribed-ranges",
	    "sub-column-header-unsubscribed-ranges");
    }

    return tabColumnHeaderCssClasses;
  }

  /**
   * Populates the tabs with the publication data to be displayed.
   * 
   * @param publications
   *          A List<SerialPublication> with the publications to be used to
   *          populate the tabs.
   * @param publishers
   *          A Map<String, Publisher> with the publishers involved mapped by
    *          their name.
   * @param divTableMap
   *          A Map<String, Table> with the tabs tables mapped by the first
   *          letter of the tab letter group.
   */
  private void populateTabsPublications(List<SerialPublication> publications,
      Map<String, Publisher> publishers, Map<String, Table> divTableMap) {
    final String DEBUG_HEADER = "populateTabsPublications(): ";
    if (log.isDebug2()) {
      if (publications != null) {
	log.debug2(DEBUG_HEADER + "publications.size() = "
	    + publications.size());
      } else {
	log.debug2(DEBUG_HEADER + "publications is null");
      }
      if (publishers != null) {
	log.debug2(DEBUG_HEADER + "publishers.size() = "
	    + publishers.size());
      } else {
	log.debug2(DEBUG_HEADER + "publishers is null");
      }
    }

    Map.Entry<String, TreeSet<SerialPublication>> pubEntry;
    String publisherName;
    TreeSet<SerialPublication> pubSet;

    // Get a map of the publications keyed by their publisher.
    MultiValueMap publicationMap = orderPublicationsByPublisher(publications);
    if (log.isDebug3()) {
      if (publicationMap != null) {
	log.debug3(DEBUG_HEADER + "publicationMap.size() = "
	    + publicationMap.size());
      } else {
	log.debug3(DEBUG_HEADER + "publicationMap is null");
      }
    }

    // Loop through all the publications mapped by their publisher.
    Iterator<Map.Entry<String, TreeSet<SerialPublication>>> pubIterator =
	(Iterator<Map.Entry<String, TreeSet<SerialPublication>>>)publicationMap
	.entrySet().iterator();

    while (pubIterator.hasNext()) {
      pubEntry = pubIterator.next();

      // Get the publisher name.
      publisherName = pubEntry.getKey();
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "publisherName = " + publisherName);

      // Get the publisher number.
      Long publisherNumber =
	  publishers.get(publisherName).getPublisherNumber();
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "publisherNumber = " + publisherNumber);

      // Get the set of publications for this publisher.
      pubSet = pubEntry.getValue();
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "pubSet.size() = " + pubSet.size());

      // Populate a tab with the publications for this publisher.
      populateTabPublisherPublications(publisherName, publisherNumber,
	  null, pubSet, divTableMap);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Maps publications by publisher.
   * 
   * @param publications
   *          A List<SerialPublication> with the publications.
   * @return a MultiValueMap with the map in the desired format.
   */
  private MultiValueMap orderPublicationsByPublisher(
      List<SerialPublication> publications) {
    final String DEBUG_HEADER = "orderPublicationsByPublisher(): ";
    if (log.isDebug2()) {
      if (publications != null) {
	log.debug2(DEBUG_HEADER + "publications.size() = "
	    + publications.size());
      } else {
	log.debug2(DEBUG_HEADER + "publications is null");
      }
    }

    SerialPublication publication;
    String publisherName;
    String publicationName;

    // Initialize the resulting map.
    MultiValueMap publicationMap = MultiValueMap
	.decorate(new TreeMap<String, TreeSet<SerialPublication>>(),
	    FactoryUtils.prototypeFactory(new TreeSet<SerialPublication>(
		subManager.getPublicationComparator())));

    int publicationCount = 0;

    if (publications != null) {
      publicationCount = publications.size();
    }

    // Loop through all the publications.
    for (int i = 0; i < publicationCount; i++) {
      publication = publications.get(i);

      // The publisher name.
      publisherName = publication.getPublisherName();
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "publisherName = " + publisherName);

      // Do nothing with this publication if it has no publisher.
      if (StringUtil.isNullString(publisherName)) {
	log.error("Publication '" + publication.getPublicationName()
	    + "' is unsubscribable because it has no publisher.");
	continue;
      }

      // The publication name.
      publicationName = publication.getPublicationName();
      if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "publicationName = " + publicationName);

      // Initialize the unique name of the publication for display purposes.
      String uniqueName = publicationName;

      // Check whether the publication name displayed must be qualified with the
      // provider name to make it unique.
      if ((i > 0
	  && publicationName.equals(publications.get(i - 1)
	      .getPublicationName()) && publisherName.equals(publications.get(
	  i - 1).getPublisherName()))
	  || (i < publicationCount - 1
	      && publicationName.equals(publications.get(i + 1)
	          .getPublicationName()) && publisherName.equals(publications
	      .get(i + 1).getPublisherName()))) {
	// Yes: Create the unique name.
	if (!StringUtil.isNullString(publication.getProviderName())) {
	  uniqueName += " [" + publication.getProviderName() + "]";
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "uniqueName = " + uniqueName);
	}
      }

      // Remember the publication unique name.
      publication.setUniqueName(uniqueName);

      // Add the publication to this publisher set of publications.
      publicationMap.put(publisherName, publication);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
    return publicationMap;
  }

  /**
   * Populates a tab with the publications for a publisher.
   * 
   * @param publisherName
   *          A String with the name of the publisher.
   * @param publisherNumber
   *          A Long with the number assigned to the publisher.
   * @param publisherSubscriptionSetting
   *          A Boolean with the setting of the publisher subscription.
   * @param pubSet
   *          A TreeSet<SerialPublication> with the publisher publications.
   * @param divTableMap
   *          A Map<String, Table> with the tabs tables mapped by the first
   *          letter of the tab letter group.
   */
  private void populateTabPublisherPublications(String publisherName,
      Long publisherNumber, Boolean publisherSubscriptionSetting,
      TreeSet<SerialPublication> pubSet, Map<String, Table> divTableMap) {
    final String DEBUG_HEADER = "populateTabPublisherPublications(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "publisherName = " + publisherName);

    // The publisher name first letter.
    String firstLetterPub = publisherName.substring(0, 1).toUpperCase();
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "firstLetterPub = " + firstLetterPub);

    // Get the tab table that corresponds to this publisher.
    Table divTable = divTableMap.get(firstLetterPub);

    // Check whether no table corresponds naturally to this publisher.
    if (divTable == null) {
      // Yes: Use the first table.
      divTable = divTableMap.get("A");

      // Check whether no table is found.
      if (divTable == null) {
	// Yes: Report the problem and skip this publisher.
	log.error("Publisher '" + publisherName
	    + "' belongs to an unknown tab: Skipped.");

	return;
      }
    }

    // Sanitize the publisher name so that it can be used as an HTML division
    // identifier.
    String cleanNameString = StringUtil.sanitizeToIdentifier(publisherName);
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "cleanNameString = " + cleanNameString);

    String publisherRowTitle = publisherName;

    // Check whether there are any publications to show.
    if (pubSet != null && pubSet.size() > 0) {
      // Yes: Get the publisher row title.
      publisherRowTitle += " (" + pubSet.size() + ")";
    }

    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "publisherRowTitle = " + publisherRowTitle);

    // Create in the table the title row for the publisher.
    createPublisherRow(publisherRowTitle, cleanNameString, publisherNumber,
	publisherSubscriptionSetting, divTable);

    // Check whether there are any publications to show.
    if (pubSet != null) {
      // Yes: Add them.
      int rowIndex = 0;

      // Loop through all the publications.
      for (SerialPublication publication : pubSet) {
        // Create in the table a row for the publication.
        createPublicationRow(publication, cleanNameString, rowIndex, divTable);
        rowIndex++;
      }
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Creates a row for a publisher.
   * 
   * @param publisherName
   *          A String with the name of the publisher.
   * @param publisherId
   *          A String with the identifier of the publisher.
   * @param publisherNumber
   *          A Long with the number assigned to the publisher.
   * @param publisherSubscriptionSetting
   *          A Boolean with the setting of the publisher subscription.
   * @param divTable
   *          A Table with the table where to create the row.
   */
  private void createPublisherRow(String publisherName, String publisherId,
      Long publisherNumber, Boolean publisherSubscriptionSetting,
      Table divTable) {
    final String DEBUG_HEADER = "createPublisherRow(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "publisherName = " + publisherName);
      log.debug2(DEBUG_HEADER + "publisherId = " + publisherId);
      log.debug2(DEBUG_HEADER + "publisherNumber = " + publisherNumber);
      log.debug2(DEBUG_HEADER + "publisherSubscriptionSetting = "
	  + publisherSubscriptionSetting);
    }

    divTable.newRow();

    Block rowHeading = new Block(Block.Span);

    // Check whether the publisher subscription has not been set.
    if (publisherSubscriptionSetting == null) {
      // Yes: Add the publisher name as a link. 
      Link headingLink = new Link("javascript:showRows('" + publisherId
  	+ "_class', '" + publisherId + "_id', '" + publisherId
  	+ "_Publisherimage')");
      headingLink.attribute("id=\"" + publisherId + "_id\"");

      Image headingLinkImage = new Image("images/expand.png");
      headingLinkImage.attribute("id =\"" + publisherId + "_Publisherimage\"");
      headingLinkImage.attribute("class=\"title-icon\"");
      headingLinkImage.attribute("alt", "Expand Publisher");
      headingLinkImage.attribute("title", "Expand Publisher");

      headingLink.add(headingLinkImage);
      headingLink.add(publisherName);

      Block boldHeadingLink = new Block(Block.Bold);
      boldHeadingLink.add(headingLink);

      rowHeading.add(boldHeadingLink);
    } else {
      // No: Add the publisher name as plain text.
      Block boldHeading = new Block(Block.Bold);
      boldHeading.add("&nbsp;&nbsp;&nbsp;&nbsp;" + publisherName);

      rowHeading.add(boldHeading);
    }

    divTable.addCell(rowHeading, "class=\"pub-title\"");

    divTable.addCell(getPublisherSubscriptionBlock(publisherNumber,
	publisherSubscriptionSetting), "align=\"center\"");

    // No range boxes for the publisher row.
    divTable.addCell("&nbsp;");
    divTable.addCell("&nbsp;");

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides the HTML block with the publisher subscription widget.
   * 
   * @param publisherNumber
   *          A Long with the number assigned to the publisher.
   * @param publisherSubscriptionSetting
   *          A Boolean with the state of the publisher subscription widget.
   * @return a Block representing the publisher subscription widget.
   */
  private Block getPublisherSubscriptionBlock(Long publisherNumber,
      Boolean publisherSubscriptionSetting) {
    String publisherSubscriptionWidgetId =
	PUBLISHER_SUBSCRIPTION_WIDGET_ID_PREFIX + publisherNumber;

    Block triBoxBlock = new Block(Block.Bold);

    Input checkBox = new Input(Input.Checkbox, publisherSubscriptionWidgetId);
    checkBox.attribute("class", "tribox");
    checkBox.attribute("id", publisherSubscriptionWidgetId);

    triBoxBlock.add(checkBox);
    triBoxBlock.add("&nbsp;");

    triBoxBlock.add(getTriStateDisplayTextSpan(publisherSubscriptionWidgetId,
	publisherSubscriptionSetting));

    triBoxBlock.add(getTriStateHiddenInput(publisherSubscriptionWidgetId,
	publisherSubscriptionSetting));

    return triBoxBlock;
  }

  /**
   * Creates a row for a publication.
   * 
   * @param publication
   *          A SerialPublication with the publication.
   * @param publisherId
   *          A String with the identifier of the publisher.
   * @param rowIndex
   *          An int with row number.
   * @param divTable
   *          A Table with the table where to create the row.
   */
  private void createPublicationRow(SerialPublication publication,
      String publisherId, int rowIndex, Table divTable) {
    final String DEBUG_HEADER = "createPublicationRow(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "publication = " + publication);
      log.debug2(DEBUG_HEADER + "publisherId = " + publisherId);
      log.debug2(DEBUG_HEADER + "rowIndex = " + rowIndex);
    }

    divTable.newRow();

    Block newRow = divTable.row();
    newRow.attribute("class", publisherId + "_class hide-row "
	+ ServletUtil.rowCss(rowIndex, 3));

    divTable.addCell(publication.getUniqueName(),
	"class=\"sub-publication-name\"");

    Long publicationNumber = publication.getPublicationNumber();
    String subscribedRangesId =
	SUBSCRIBED_RANGES_PARAM_NAME + publicationNumber;
    String unsubscribedRangesId =
	UNSUBSCRIBED_RANGES_PARAM_NAME + publicationNumber;

    // The publication subscription widget.
    divTable.addCell(getPublicationSubscriptionBlock(publicationNumber, null,
	subscribedRangesId, unsubscribedRangesId));

    // The subscribed ranges.
    Input subscribedInputBox = new Input(Input.Text, subscribedRangesId, "");
    subscribedInputBox.setSize(25);
    subscribedInputBox.attribute("id", subscribedRangesId);

    divTable.addCell(subscribedInputBox);

    // The unsubscribed ranges.
    Input unsubscribedInputBox =
	new Input(Input.Text, unsubscribedRangesId, "");
    unsubscribedInputBox.setSize(25);
    unsubscribedInputBox.attribute("id", unsubscribedRangesId);

    divTable.addCell(unsubscribedInputBox);

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides the HTML block with the publication subscription widget.
   * 
   * @param publicationNumber
   *          A Long with the number assigned to the publication.
   * @param publicationSubscriptionSetting
   *          A Boolean with the state of the publication subscription widget.
   * @param subscribedRangesId
   *          A String with the identifier of the subscribed ranges input box.
   * @param unsubscribedRangesId
   *          A String with the identifier of the unsubscribed ranges input box.
   * @return a Block representing the publication subscription widget.
   */
  private Block getPublicationSubscriptionBlock(Long publicationNumber,
      Boolean publicationSubscriptionSetting, String subscribedRangesId,
      String unsubscribedRangesId) {
    String publicationSubscriptionWidgetId =
	PUBLICATION_SUBSCRIPTION_WIDGET_ID_PREFIX + publicationNumber;

    Block triBoxBlock = new Block(Block.Span);

    Input checkBox = new Input(Input.Checkbox, publicationSubscriptionWidgetId);
    checkBox.attribute("class", "tribox");
    checkBox.attribute("id", publicationSubscriptionWidgetId);
    checkBox.attribute("onchange", "publicationSubscriptionChanged('"
	+ publicationSubscriptionWidgetId + TRI_STATE_WIDGET_HIDDEN_ID_SUFFIX
	+ "', '" + subscribedRangesId + "', '" + unsubscribedRangesId + "')");
    triBoxBlock.add(checkBox);
    triBoxBlock.add("&nbsp;");

    triBoxBlock.add(getTriStateDisplayTextSpan(publicationSubscriptionWidgetId,
	publicationSubscriptionSetting));

    triBoxBlock.add(getTriStateHiddenInput(publicationSubscriptionWidgetId,
	publicationSubscriptionSetting));

    return triBoxBlock;
  }

  /**
   * Creates any subscriptions specified by the user in the form.
   * 
   * @return a SubscriptionOperationStatus with a summary of the status of the
   *         operation.
   */
  private SubscriptionOperationStatus addSubscriptions() {
    final String DEBUG_HEADER = "addSubscriptions(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    SubscriptionOperationStatus status = new SubscriptionOperationStatus();

    if (!hasSession()) {
      status.addStatusEntry(null, false, MISSING_COOKIES_MSG, null);
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "status = " + status);
      return status;
    } else {
      session = getSession();
    }

    // Get the Total Subscription setting presented in the form just submitted.
    Boolean totalSubscriptionSetting = null;

    if (subManager.isTotalSubscriptionEnabled()) {
      totalSubscriptionSetting =
	  (Boolean)session.getAttribute(TOTAL_SUBSCRIPTION_SESSION_KEY);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER
	  + "totalSubscriptionSetting = " + totalSubscriptionSetting);
    }

    // Get the publishers presented in the form just submitted.
    Map<String, Publisher> publishers = (Map<String, Publisher>)session
	.getAttribute(UNDECIDED_PUBLISHERS_SESSION_KEY);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "publishers = " + publishers);

    // Get the undecided publications presented in the form just submitted.
    List<SerialPublication> publications = (List<SerialPublication>)session
	.getAttribute(UNDECIDED_TITLES_SESSION_KEY);
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "publications = " + publications);

    // Handle session expiration.
    if (totalSubscriptionSetting == null
	&& (publications == null || publications.size() == 0)) {
      status.addStatusEntry(null, false, SESSION_EXPIRED_MSG, null);
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "status = " + status);
      return status;
    }

    // Get the map of parameters received from the submitted form.
    Map<String,String> parameterMap = getParamsAsMap();
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "parameterMap = " + parameterMap);

    boolean totalSubscriptionChanged = false;
    Boolean updatedTotalSubscriptionSetting = null;

    if (subManager.isTotalSubscriptionEnabled()) {
      updatedTotalSubscriptionSetting =
	  getTriBoxValue(parameterMap, TOTAL_SUBSCRIPTION_WIDGET_ID);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER
	  + "updatedTotalSubscriptionSetting = "
	  + updatedTotalSubscriptionSetting);

      totalSubscriptionChanged = (totalSubscriptionSetting == null
	  && updatedTotalSubscriptionSetting != null)
	  || (totalSubscriptionSetting != null
	  && updatedTotalSubscriptionSetting == null)
	  || (totalSubscriptionSetting != null
	  && totalSubscriptionSetting.booleanValue()
	  != updatedTotalSubscriptionSetting.booleanValue());
      if (log.isDebug3()) log.debug3(DEBUG_HEADER
	  + "totalSubscriptionChanged = " + totalSubscriptionChanged);
    }

    if (totalSubscriptionChanged && updatedTotalSubscriptionSetting != null
	&& updatedTotalSubscriptionSetting.booleanValue()) {
      subManager.handleStartingTotalSubscription(status);
    } else {
      Map<String, PublisherSubscription> publisherSubscriptions =
	  buildPublisherSubscriptionMap(publishers, parameterMap);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "publisherSubscriptions = "
	  + publisherSubscriptions);

      Subscription subscription;
      Collection<Subscription> subscriptions = new ArrayList<Subscription>();

      if (totalSubscriptionSetting == null
	  && updatedTotalSubscriptionSetting == null) {
	// Loop through all the publications presented in the form.
	for (SerialPublication publication : publications) {
	  // Skip publications for publisher subscriptions.
	  if (!publisherSubscriptions
	      .containsKey(publication.getPublisherName())) {
	    // Add it to the list of subscriptions added, if necessary.
	    subscription = buildPublicationSubscriptionIfNeeded(publication,
		parameterMap, status);

	    if (subscription != null) {
	      subscriptions.add(subscription);
	    }
	  }
	}
      }

      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "subscriptions = " + subscriptions);

      // Add any added subscriptions to the system.
      if (totalSubscriptionChanged || publisherSubscriptions.size() > 0
	  || subscriptions.size() > 0) {
	subManager.addSubscriptions(totalSubscriptionChanged,
	    updatedTotalSubscriptionSetting, publisherSubscriptions.values(),
	    subscriptions, status);
      }
    }

    session.removeAttribute(UNDECIDED_TITLES_SESSION_KEY);
    session.removeAttribute(UNDECIDED_PUBLISHERS_SESSION_KEY);

    if (subManager.isTotalSubscriptionEnabled()) {
      session.removeAttribute(TOTAL_SUBSCRIPTION_SESSION_KEY);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "status = " + status);
    return status;
  }

  /**
   * Provides the setting of a submitted tri-state widget.
   * 
   * @param parameterMap
   *          A Map<String,String> with the submitted parameter names and
   *          values.
   * @param id
   *          A String with the widget identifier.
   * @return a Boolean with the setting of the widget.
   */
  Boolean getTriBoxValue(Map<String,String> parameterMap, String id) {
    if (parameterMap == null || parameterMap.isEmpty() || id == null
	|| id.trim().length() == 0) {
      return null;
    }

    String result =
	parameterMap.get(id.trim() + TRI_STATE_WIDGET_HIDDEN_ID_SUFFIX);

    if (result == null
	|| TRI_STATE_WIDGET_HIDDEN_ID_UNSET_VALUE.equals(result)) {
      return null;
    } else if ("true".equals(result)) {
      return Boolean.TRUE;
    }

    return Boolean.FALSE;
  }

  /**
   * Provides a map of submitted publisher subscriptions keyed by the publisher
   * name.
   * 
   * @param publishers
   *          A Map<String, Publisher> with a map of the originally undecided
   *          publishers keyed by the publisher name.
   * @param parameterMap
   *          A Map<String,String> with the map of parameters received from the
   *          submitted form.
   * @return a Map<String, PublisherSubscription> with the submitted publisher
   *         subscriptions keyed by the publisher name.
   */
  private Map<String, PublisherSubscription> buildPublisherSubscriptionMap(
      Map<String, Publisher> publishers, Map<String,String> parameterMap) {
    final String DEBUG_HEADER = "buildPublisherSubscriptionMap(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "publishers.size() = " + publishers.size());

    Map<String, PublisherSubscription> publisherSubscriptions =
	new HashMap<String, PublisherSubscription>();

    // Loop through all the originally undecided publishers.
    for (String publisherName : publishers.keySet()) {
      Publisher publisher = publishers.get(publisherName);

      Boolean publisherSubscriptionSetting = getTriBoxValue(parameterMap,
	  PUBLISHER_SUBSCRIPTION_WIDGET_ID_PREFIX
	  + publisher.getPublisherNumber());

      // Check whether a publisher subscription has been specified.
      if (publisherSubscriptionSetting != null) {
	// Yes: Remember it.
	PublisherSubscription publisherSubscription =
	    new PublisherSubscription();

	publisherSubscription.setPublisher(publisher);
	publisherSubscription.setSubscribed(publisherSubscriptionSetting);

	publisherSubscriptions.put(publisherName, publisherSubscription);
      }
    }

    if (log.isDebug2()) log.debug3(DEBUG_HEADER + "publisherSubscriptions = "
	+ publisherSubscriptions);
    return publisherSubscriptions;
  }

  /**
   * Builds a subscription for a publication, if needed.
   * 
   * @param publication
   *          A SerialPublication with the publication.
   * @param parameterMap
   *          A Map<String, String> with the submitted form parameter names and
   *          their values.
   * @param status
   *          A SubscriptionOperationStatus where to record any failures.
   * @return a Subscription with the subscription to be created, if needed, or
   *         <code>null</code> otherwise.
   */
  private Subscription buildPublicationSubscriptionIfNeeded(
      SerialPublication publication, Map<String, String> parameterMap,
      SubscriptionOperationStatus status) {
    final String DEBUG_HEADER = "buildPublicationSubscriptionIfNeeded(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "publication = " + publication);

    // Check whether the publication has no TdbTitle.
    if (publication.getTdbTitle() == null) {
      // Yes: Get the publication name.
      String publicationName = publication.getPublicationName();
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "publicationName = "
	  + publicationName);

      // Report the problem.
      String message =
	  "Cannot find tdbTitle with name '" + publicationName + "'.";
      log.error(message);
      status.addStatusEntry(publication.getPublicationName(), false,
	  MISSING_TITLE_AUS_MSG, null);
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "subscription = null");
      return null;
    }

    String subscribedRangesText = "";
    String unsubscribedRangesText = "";
    List<BibliographicPeriod> subscribedRanges;
    List<BibliographicPeriod> unsubscribedRanges;
    Subscription subscription = null;

    Long publicationNumber = publication.getPublicationNumber();

    Boolean overallSubscriptionSetting = getTriBoxValue(parameterMap,
	      PUBLICATION_SUBSCRIPTION_WIDGET_ID_PREFIX + publicationNumber);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER
	+ "overallSubscriptionSetting = " + overallSubscriptionSetting);

    // Check whether the "Subscribe All" option has been selected.
    if (overallSubscriptionSetting != null
	&& overallSubscriptionSetting.booleanValue()) {
      // Yes: Handle a full subscription request. Determine whether the
      // subscribed ranges have changed.
      subscription = new Subscription();
      subscription.setPublication(publication);
      subscription.setSubscribedRanges(Collections
	  .singletonList(BibliographicPeriod.ALL_TIME_PERIOD));

      String unsubscribedRangesParamName =
	  UNSUBSCRIBED_RANGES_PARAM_NAME + publicationNumber;

      // Check whether there are exceptions to the full subscription request.
      if (parameterMap.containsKey(unsubscribedRangesParamName)) {
	// Yes: Handle exceptions to a full subscription request.
	unsubscribedRangesText = StringUtil
	    .replaceString(parameterMap.get(unsubscribedRangesParamName), " ",
		"");
	if (log.isDebug3()) log.debug3(DEBUG_HEADER
	    + "unsubscribedRangesText = " + unsubscribedRangesText);

	if (!StringUtil.isNullString(unsubscribedRangesText)) {
	  // Get the unsubscribed ranges as a collection.
	  try {
	    unsubscribedRanges =
		BibliographicPeriod.createList(unsubscribedRangesText);
	  } catch (IllegalArgumentException iae) {
	    status.addStatusEntry(publication.getPublicationName(), false,
		BAD_UNSUBSCRIBED_RANGES_MSG, null);
	    if (log.isDebug2())
	      log.debug2(DEBUG_HEADER + "subscription = null");
	    return null;
	  }

	  // Validate the specified unsubscribed ranges.
	  Collection<BibliographicPeriod> invalidRanges =
	      subManager.validateRanges(unsubscribedRanges, publication);

	  // Check whether the unsubscribed ranges are valid.
	  if (invalidRanges == null || invalidRanges.size() == 0) {
	    // Yes: Remember them.
	    subscription.setUnsubscribedRanges(unsubscribedRanges);
	  } else {
	    // No: Report the problem.
	    reportInvalidRanges(status, invalidRanges,
		publication.getPublicationName());

	    if (log.isDebug2())
	      log.debug2(DEBUG_HEADER + "subscription = null");
	    return null;
	  }
	}
      }

      // No: Check whether the "Unsubscribe All" option has been selected.
    } else if (overallSubscriptionSetting != null
	&& overallSubscriptionSetting.booleanValue()) {
      subscription = new Subscription();
      subscription.setPublication(publication);
      subscription.setUnsubscribedRanges(Collections
	  .singletonList(BibliographicPeriod.ALL_TIME_PERIOD));
    } else {
      // No: Handle a partial subscription request.
      String unsubscribedRangesParamName =
	  UNSUBSCRIBED_RANGES_PARAM_NAME + publicationNumber;

      // Check whether there are unsubscribed ranges specified in the form.
      if (parameterMap.containsKey(unsubscribedRangesParamName)) {
	// Yes: Get them.
	unsubscribedRangesText = StringUtil
	    .replaceString(parameterMap.get(unsubscribedRangesParamName), " ",
		"");
	if (log.isDebug3()) log.debug3(DEBUG_HEADER
	    + "unsubscribedRangesText = " + unsubscribedRangesText);

	if (!StringUtil.isNullString(unsubscribedRangesText)) {
	  // Get the unsubscribed ranges as a collection.
	  try {
	    unsubscribedRanges =
		BibliographicPeriod.createList(unsubscribedRangesText);
	  } catch (IllegalArgumentException iae) {
	    status.addStatusEntry(publication.getPublicationName(), false,
		BAD_UNSUBSCRIBED_RANGES_MSG, null);
	    if (log.isDebug2())
	      log.debug2(DEBUG_HEADER + "subscription = null");
	    return null;
	  }

	  // Validate the specified unsubscribed ranges.
	  Collection<BibliographicPeriod> invalidRanges =
	      subManager.validateRanges(unsubscribedRanges, publication);

	  // Check whether the unsubscribed ranges are valid.
	  if (invalidRanges == null || invalidRanges.size() == 0) {
	    // Yes: Remember them.
	    subscription = new Subscription();
	    subscription.setPublication(publication);
	    subscription.setUnsubscribedRanges(unsubscribedRanges);
	  } else {
	    // No: Report the problem.
	    reportInvalidRanges(status, invalidRanges,
		publication.getPublicationName());

	    if (log.isDebug2())
	      log.debug2(DEBUG_HEADER + "subscription = null");
	    return null;
	  }
	}
      }

      String subscribedRangesParamName =
	  SUBSCRIBED_RANGES_PARAM_NAME + publicationNumber;

      // Check whether there are subscribed ranges specified in the form.
      if (parameterMap.containsKey(subscribedRangesParamName)) {
	// Yes: Get them.
	subscribedRangesText = StringUtil
	    .replaceString(parameterMap.get(subscribedRangesParamName), " ",
		"");
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "subscribedRangesText = "
	    + subscribedRangesText);

	if (!StringUtil.isNullString(subscribedRangesText)) {
	  try {
	    // Get the subscribed ranges as a collection.
	    subscribedRanges =
		BibliographicPeriod.createList(subscribedRangesText);
	  } catch (IllegalArgumentException iae) {
	    status.addStatusEntry(publication.getPublicationName(), false,
		BAD_SUBSCRIBED_RANGES_MSG, null);
	    if (log.isDebug2())
	      log.debug2(DEBUG_HEADER + "subscription = null");
	    return null;
	  }

	  // Validate the specified subscribed ranges.
	  Collection<BibliographicPeriod> invalidRanges =
	      subManager.validateRanges(subscribedRanges, publication);

	  // Check whether the subscribed ranges are valid.
	  if (invalidRanges == null || invalidRanges.size() == 0) {
	    // Yes: Remember them.
	    if (subscription == null) {
	      subscription = new Subscription();
	      subscription.setPublication(publication);
	    }

	    subscription.setSubscribedRanges(subscribedRanges);
	  } else {
	    // No: Report the problem.
	    reportInvalidRanges(status, invalidRanges,
		publication.getPublicationName());

	    if (log.isDebug2())
	      log.debug2(DEBUG_HEADER + "subscription = null");
	    return null;
	  }
	}
      }
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "subscription = " + subscription);
    return subscription;
  }

  /**
   * Adds to the status report any ranges that are not valid.
   * 
   * @param status
   *          A SubscriptionOperationStatus where to record the invalid ranges.
   * @param invalidRanges
   *          A Collection<BibliographicPeriod> with the invalid ranges.
   * @param publicationName
   *          A String with the name of the publication with the invalid ranges.
   */
  private void reportInvalidRanges(SubscriptionOperationStatus status,
      Collection<BibliographicPeriod> invalidRanges, String publicationName) {
    StringBuilder message = new StringBuilder(BAD_UNSUBSCRIBED_RANGES_MSG);
    boolean isFirst = true;

    for (BibliographicPeriod range : invalidRanges) {
      if (isFirst) {
	message.append(": '");
      } else {
	message.append(", '");
      }

      message.append(range.toDisplayableString()).append("'");
    }

    status.addStatusEntry(publicationName, false, message.toString(), null);
  }

  /**
   * Displays a page with the results of the previous operation.
   * 
   * @param status
   *          A SubscriptionOperationStatus with the results of the previous
   *          operation.
   * @param backLinkDisplayText
   *          A String with the text of the link used to go back to the previous
   *          page.
   * @param backLinkAction
   *          A String with the action needed to go back to the previous page.
   * @throws IOException
   *           if any problem occurred writing the page.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private void displayResults(SubscriptionOperationStatus status,
      String backLinkDisplayText, String backLinkAction) throws IOException,
      DbException {
    final String DEBUG_HEADER = "displayResults(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "status = " + status);
      log.debug2(DEBUG_HEADER + "backLinkDisplayText = " + backLinkDisplayText);
      log.debug2(DEBUG_HEADER + "backLinkAction = " + backLinkAction);
    }

    // Start the page.
    Page page = newPage();
    addJavaScript(page);

    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "pluginMgr.areAusStarted() = "
	+ pluginManager.areAusStarted());
    if (!pluginManager.areAusStarted()) {
      page.add(ServletUtil.notStartedWarning());
    }

    ServletUtil.layoutExplanationBlock(page,
				       backLinkDisplayText + " Results");

    PublisherStatusEntry totalSubscriptionEntry = null;
    List<PublisherStatusEntry> publisherSuscriptionEntries =
	new ArrayList<PublisherStatusEntry>();

    if (subManager.isTotalSubscriptionEnabled()) {
      totalSubscriptionEntry = status.getTotalSubscriptionStatusEntry();
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "totalSubscriptionEntry = "
	  + totalSubscriptionEntry);

      publisherSuscriptionEntries =
	  status.getPublisherSubscriptionStatusEntries();
    }

    List<StatusEntry> statusEntries = status.getStatusEntries();
    page.add("<br>");

    if (totalSubscriptionEntry == null
	&& publisherSuscriptionEntries.size() == 0
	&& statusEntries.size() == 0) {
      // Handle a no-op.
      errMsg = "No operation was specified";
      layoutErrorBlock(page);
    } else {
      if (totalSubscriptionEntry == null
	  && publisherSuscriptionEntries.size() == 0
	  && statusEntries.size() == 1
	  && StringUtil.isNullString(statusEntries.get(0).getPublicationName())
	  && !statusEntries.get(0).isSubscriptionSuccess()) {
	// Handle an overall error.
	errMsg = statusEntries.get(0).getSubscriptionErrorMessage();
	layoutErrorBlock(page);
      } else {
	// Handle itemized subscription results.
	layoutErrorBlock(page);

	int border = 1;
	String attributes =
	    "align=\"center\" cellspacing=\"4\" cellpadding=\"5\"";

	if (totalSubscriptionEntry != null
	    || publisherSuscriptionEntries.size() > 0) {
	  displayPublisherSubscriptionResults(border, attributes,
	      totalSubscriptionEntry, publisherSuscriptionEntries, page);
	}

	if (statusEntries.size() > 0) {
	  displayPublicationSubscriptionResults(border, attributes,
	      statusEntries, backLinkDisplayText, page);
	}
      }
    }

    // The link to go back to the previous page.
    if (!StringUtil.isNullString(backLinkAction)) {
      ServletUtil.layoutBackLink(page,
	  srvLink(AdminServletManager.SERVLET_SUB_MANAGEMENT,
	          BACK_LINK_TEXT_PREFIX + backLinkDisplayText,
	          "lockssAction=" + backLinkAction));
    }

    // The link to go back to the journal configuration page.
    ServletUtil.layoutBackLink(page,
	srvLink(AdminServletManager.SERVLET_BATCH_AU_CONFIG,
	    	BACK_LINK_TEXT_PREFIX
	    	+ getHeading(AdminServletManager.SERVLET_BATCH_AU_CONFIG)));

    // Finish up.
    endPage(page);

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Displays a table with the results of requested publisher subscriptions.
   * 
   * @param border
   *          An int with the width of the table border.
   * @param attributes
   *          A String with the table attributes.
   * @param totalSubscriptionEntry
   *          A PublisherStatusEntry with the result of the Total Subscription
   *          option.
   * @param publisherSuscriptionEntries
   *          A List<PublisherStatusEntry> with the results of the publisher
   *          subscriptions.
   * @param page
   *          A Page representing the page where the table is displayed.
   */
  private void displayPublisherSubscriptionResults(int border,
      String attributes, PublisherStatusEntry totalSubscriptionEntry,
      List<PublisherStatusEntry> publisherSuscriptionEntries, Page page) {
    Table results = new Table(border, attributes);
    results.addHeading("Publisher");
    results.addHeading("Subscription");
    results.addHeading("Status");

    if (totalSubscriptionEntry != null) {
      results.newRow();
      results.newCell();
      results.add("ALL");
      results.newCell();

      if (totalSubscriptionEntry.getSubscriptionStatus() == null) {
	results.add("Not Set");
      } else if (totalSubscriptionEntry.getSubscriptionStatus()
	  .booleanValue()) {
	results.add("Always");
      } else {
	results.add("Never");
      }

      results.newCell();

      if (totalSubscriptionEntry.isSubscriptionSuccess()) {
	results.add("Updated");
      } else {
	results.add(totalSubscriptionEntry.getSubscriptionErrorMessage());
      }
    }

    // Loop through all the itemized subscription results.
    for (PublisherStatusEntry entry : publisherSuscriptionEntries) {
      results.newRow();
      results.newCell();
      results.add(encodeText(entry.getPublisherName()));
      results.newCell();

      if (entry.getSubscriptionStatus() == null) {
	results.add("Not Set");
      } else if (entry.getSubscriptionStatus().booleanValue()) {
	results.add("Always");
      } else {
	results.add("Never");
      }

      results.newCell();

      if (entry.isSubscriptionSuccess()) {
	results.add("Updated");
      } else {
	results.add(entry.getSubscriptionErrorMessage());
      }
    }

    Composite comp = new Block(Block.Center);
    comp.add(results);
    comp.add("<br>");

    page.add(comp);
  }

  /**
   * Displays a table with the results of requested publication subscriptions.
   * 
   * @param border
   *          An int with the width of the table border.
   * @param attributes
   *          A String with the table attributes.
   * @param statusEntries
   *          A List<StatusEntry> with the results of the publication
   *          subscriptions.
   * @param page
   *          A Page representing the page where the table is displayed.
   */
  private void displayPublicationSubscriptionResults(int border,
      String attributes, List<StatusEntry> statusEntries,
      String backLinkDisplayText, Page page) {
    // Create the results table.
    Table results = new Table(border, attributes);
    results.addHeading("Title");
    results.addHeading("Subscription Status");

    if (!AUTO_ADD_SUBSCRIPTIONS_LINK_TEXT.equals(backLinkDisplayText)) {
      results.addHeading("AU Configuration");
    }

    // Loop through all the itemized subscription results.
    for (StatusEntry entry : statusEntries) {
      // Display a row per itemized subscription result.
      results.newRow();
      results.newCell();
      results.add(encodeText(entry.getPublicationName()));
      results.newCell();

      if (entry.isSubscriptionSuccess()) {
	if (SHOW_UPDATE_PAGE_LINK_TEXT.equals(backLinkDisplayText)) {
	  results.add("Updated");
	} else if (SHOW_ADD_PAGE_LINK_TEXT.equals(backLinkDisplayText)) {
	  results.add("Added");
	} else {
	  results.add("Added/Updated");
	}
      } else {
	results.add(entry.getSubscriptionErrorMessage());
      }

      if (!AUTO_ADD_SUBSCRIPTIONS_LINK_TEXT.equals(backLinkDisplayText)) {
	results.newCell();

	if (entry.getAuStatus() != null
	    && entry.getAuStatus().getStatusList() != null
	    && entry.getAuStatus().getStatusList().size() > 0) {
	  Table auResults = new Table(border, attributes);
	  auResults.addHeading("AU");
	  auResults.addHeading("Status");

	  for (Entry auEntry : entry.getAuStatus().getStatusList()) {
	    auResults.newRow();
	    auResults.newCell();
	    auResults.add(encodeText(auEntry.getName()));
	    auResults.newCell();
	    if (StringUtil.isNullString(auEntry.getExplanation())) {
	      auResults.add(auEntry.getStatus());
	    } else {
	      auResults.add(auEntry.getExplanation());
	    }
	  }

	  results.add(auResults);
	}
      }
    }

    Composite comp = new Block(Block.Center);
    comp.add(results);
    comp.add("<br>");

    page.add(comp);
  }

  /**
   * Displays the page that allows the user to change subscription decisions.
   * 
   * @throws IOException
   *           if any problem occurred writing the page.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private void displayUpdatePage() throws IOException, DbException {
    final String DEBUG_HEADER = "displayUpdatePage(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    // Start the page.
    Page page = newPage();
    addJavaScript(page);
    addCssLocations(page);
    addJQueryLocations(page);

    ServletUtil.layoutExplanationBlock(page,
	"Update existing subscription options for serial titles");

    // Get the current value of the total subscription setting.
    Boolean totalSubscriptionSetting = null;

    if (subManager.isTotalSubscriptionEnabled()) {
      totalSubscriptionSetting = subManager.isTotalSubscription();
      if (log.isDebug3()) log.debug3(DEBUG_HEADER
	  + "totalSubscriptionSetting = " + totalSubscriptionSetting);
    }

    if (totalSubscriptionSetting != null) {
      layoutErrorBlock(page);

      // Create the form.
      Form form = ServletUtil.newForm(srvURL(myServletDescr()));

      form.add(getTotalSubscriptionTable(totalSubscriptionSetting));

      // Save the existing subscriptions in the session to compare after the
      // form is submitted.
      HttpSession session = getSession();

      session.setAttribute(TOTAL_SUBSCRIPTION_SESSION_KEY,
	  totalSubscriptionSetting);

      // The submit button.
      ServletUtil.layoutSubmitButton(this, form, ACTION_TAG,
	  UPDATE_SUBSCRIPTIONS_ACTION, UPDATE_SUBSCRIPTIONS_ACTION);

      // Add the tribox javascript.
      addFormJavaScriptLocation(form, "js/tribox.js");

      // Add the form to the page.
      page.add(form);
    } else {
      // Initialize the publisher subscriptions to be displayed.
      Map<String, PublisherSubscription> subscribedPublishers =
  	subManager.findAllSubscribedPublishers();

      // Get the existing subscriptions with ranges.
      List<Subscription> subscriptions =
  	  subManager.findAllSubscriptionsAndRanges(subscribedPublishers);
      if (log.isDebug3())
  	log.debug3(DEBUG_HEADER + "subscriptions = " + subscriptions);

      if (subscriptions.size() > 0 || subscribedPublishers.size() > 0) {
	layoutErrorBlock(page);

	// Create the form.
	Form form = ServletUtil.newForm(srvURL(myServletDescr()));

	if (subManager.isTotalSubscriptionEnabled()) {
	  form.add(getTotalSubscriptionTable(totalSubscriptionSetting));
	}

	// Determine whether to use a single-tab or multiple-tab interface.
	int lettersPerTab = DEFAULT_LETTERS_PER_TAB;

	if (subscriptions.size() <= maxSingleTabCount) {
	  lettersPerTab = LETTERS_IN_ALPHABET;
	}

	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "lettersPerTab = " + lettersPerTab);

	// Create the tabs container, an HTML division element, as required by
	// jQuery tabs.
	Block tabsDiv = new Block(Block.Div, "id=\"tabs\"");

	// Add it to the form.
	form.add(tabsDiv);

	// Create the tabs on the page, add them to the tabs container and
	// obtain a map of the tab tables keyed by the letters they cover.
	Map<String, Table> divTableMap =
	    ServletUtil.createTabsWithTable(LETTERS_IN_ALPHABET, lettersPerTab,
		getTabColumnHeaderNames(), "sub-row-title",
		getColumnHeaderCssClasses(), tabsDiv);

	// Populate the tabs content with the publications for which
	// subscription decisions have already been made.
	populateTabsSubscriptions(subscriptions, subscribedPublishers,
	    divTableMap);

	// Save the existing subscriptions in the session to compare after the
	// form is submitted.
	HttpSession session = getSession();
	session.setAttribute(SUBSCRIPTIONS_SESSION_KEY, subscriptions);

	// Save the subscribed publishers in the session to compare after the
	// form is submitted.
	session.setAttribute(SUBSCRIBED_PUBLISHERS_SESSION_KEY,
	    subscribedPublishers);

	if (subManager.isTotalSubscriptionEnabled()) {
	  session.setAttribute(TOTAL_SUBSCRIPTION_SESSION_KEY,
	      totalSubscriptionSetting);
	}

	// The submit button.
	ServletUtil.layoutSubmitButton(this, form, ACTION_TAG,
	    UPDATE_SUBSCRIPTIONS_ACTION, UPDATE_SUBSCRIPTIONS_ACTION);

	// Add the tribox javascript.
	addFormJavaScriptLocation(form, "js/tribox.js");

	// Add the form to the page.
	page.add(form);
      } else {
	errMsg = "There are no subscriptions to update";
	layoutErrorBlock(page);
      }
    }

    // The link to go back to the previous page.
    ServletUtil.layoutBackLink(page,
	srvLink(AdminServletManager.SERVLET_BATCH_AU_CONFIG,
	    	BACK_LINK_TEXT_PREFIX
	    	+ getHeading(AdminServletManager.SERVLET_BATCH_AU_CONFIG)));

    // Finish up.
    endPage(page);

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Populates the tabs with the subscription data to be displayed.
   * 
   * @param subscriptions
   *          A List<Subscription> with the subscriptions to be used to populate
   *          the tabs.
   * @param subscribedPublishers
   *          A Map<String, PublisherSubscription> with the publisher
   *          subscriptions mapped by the publisher name.
   * @param divTableMap
   *          A Map<String, Table> with the tabs tables mapped by the first
   *          letter of the tab letter group.
   */
  private void populateTabsSubscriptions(List<Subscription> subscriptions,
      Map<String, PublisherSubscription> subscribedPublishers,
      Map<String, Table> divTableMap) {
    final String DEBUG_HEADER = "populateTabsSubscriptions(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "subscriptions = " + subscriptions);
      log.debug2(DEBUG_HEADER + "subscribedPublishers = "
	  + subscribedPublishers);
    }

    // Number each subscribed publisher.
    List<PublisherSubscription> publisherSubscriptions =
	new ArrayList<PublisherSubscription>();

    long publisherNumber = 1;

    for (String publisherName : subscribedPublishers.keySet()) {
      PublisherSubscription publisherSubscription =
	  subscribedPublishers.get(publisherName);
      publisherSubscription.getPublisher()
      .setPublisherNumber(publisherNumber++);

      publisherSubscriptions.add(publisherSubscription);
    }

    // Sort the publisher subscriptions by publisher name.
    Collections.sort(publisherSubscriptions);

    // Get a map of the subscriptions keyed by their publisher.
    MultiValueMap subscriptionMap =
	orderSubscriptionsByPublisher(subscriptions);
    if (log.isDebug3()) {
      if (subscriptionMap != null) {
	log.debug3(DEBUG_HEADER + "subscriptionMap.size() = "
	    + subscriptionMap.size());
      } else {
	log.debug3(DEBUG_HEADER + "subscriptionMap is null");
      }
    }

    // Loop through all the publishers, subscribed or not.
    for (PublisherSubscription publisherSubscription : publisherSubscriptions) {
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "publisherSubscription = "
	  + publisherSubscription);

      // Check whether it is a subscribed publisher.
      if (publisherSubscription.getSubscribed() != null) {
	// Yes: Populate a tab with no title subscriptions for this publisher.
	populateTabPublisherSubscriptions(publisherSubscription, null,
	    divTableMap);
      } else {
	// No: Get the publisher name.
	String publisherName =
	    publisherSubscription.getPublisher().getPublisherName();
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "publisherName = " + publisherName);

	// Populate a tab with the subscriptions for this publisher.
	populateTabPublisherSubscriptions(publisherSubscription,
	    (TreeSet<Subscription>)subscriptionMap.get(publisherName),
	    divTableMap);
      }
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Maps subscriptions by publisher.
   * 
   * @param subscriptions
   *          A Collection<Subscription> with the subscriptions.
   * @return a MultiValueMap with the map in the desired format.
   */
  private MultiValueMap orderSubscriptionsByPublisher(
      List<Subscription> subscriptions) {
    final String DEBUG_HEADER = "orderSubscriptionsByPublisher(): ";
    if (log.isDebug2()) {
      if (subscriptions != null) {
	log.debug2(DEBUG_HEADER + "subscriptions.size() = "
	    + subscriptions.size());
      } else {
	log.debug2(DEBUG_HEADER + "subscriptions is null");
      }
    }

    Subscription subscription;
    SerialPublication publication;
    String publisherName;
    String publicationName;

    // Initialize the resulting map.
    MultiValueMap subscriptionMap = MultiValueMap
	.decorate(new TreeMap<String, TreeSet<Subscription>>(),
	    FactoryUtils.prototypeFactory(new TreeSet<Subscription>(
		subManager.getSubscriptionByPublicationComparator())));

    int subscriptionCount = 0;

    if (subscriptions != null) {
      subscriptionCount = subscriptions.size();
    }

    // Loop through all the subscriptions.
    for (int i = 0; i < subscriptionCount; i++) {
      subscription = subscriptions.get(i);

      // The publication.
      publication = subscription.getPublication();

      // The publisher name.
      publisherName = publication.getPublisherName();
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "publisherName = " + publisherName);

      // Do nothing with this subscription if it has no publisher.
      if (StringUtil.isNullString(publisherName)) {
	log.error("Publication '" + publication.getPublicationName()
	    + "' is unsubscribable because it has no publisher.");
	continue;
      }

      // The publication name.
      publicationName = publication.getPublicationName();
      if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "publicationName = " + publicationName);

      // Initialize the unique name of the publication for display purposes.
      String uniqueName = publicationName;

      // Check whether the publication name displayed must be qualified with the
      // provider name to make it unique.
      if ((i > 0
	   && publicationName.equals(subscriptions.get(i - 1).getPublication()
	       .getPublicationName())
	   && publisherName.equals(subscriptions.get(i - 1).getPublication()
	       .getPublisherName()))
	  ||
	  (i < subscriptionCount - 1
	   && publicationName.equals(subscriptions.get(i + 1).getPublication()
	       .getPublicationName())
	   && publisherName.equals(subscriptions.get(i + 1).getPublication()
	       .getPublisherName()))) {
	// Yes: Create the unique name.
	if (!StringUtil.isNullString(publication.getProviderName())) {
	  uniqueName += " [" + publication.getProviderName() + "]";
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "uniqueName = " + uniqueName);
	}
      }

      // Remember the publication unique name.
      publication.setUniqueName(uniqueName);

      // Add the subscription to this publisher set of subscriptions.
      subscriptionMap.put(publisherName, subscription);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
    return subscriptionMap;
  }

  /**
   * Populates a tab with the subscriptions for a publisher.
   * 
   * @param publisherSubscription
   *          A PublisherSubscription with the publisher subscription.
   * @param subSet
   *          A TreeSet<Subscription> with the publisher subscriptions.
   * @param divTableMap
   *          A Map<String, Table> with the tabs tables mapped by the first
   *          letter of the tab letter group.
   */
  private void populateTabPublisherSubscriptions(
      PublisherSubscription publisherSubscription, 
      TreeSet<Subscription> subSet, Map<String, Table> divTableMap) {
    final String DEBUG_HEADER = "populateTabPublisherSubscriptions(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "publisherSubscription = "
	  + publisherSubscription);

      if (subSet == null) {
	log.debug2(DEBUG_HEADER + "subSet = null");
      } else {
	log.debug2(DEBUG_HEADER + "subSet.size() = " + subSet.size());
      }
    }

    // The publisher name.
    String publisherName =
	publisherSubscription.getPublisher().getPublisherName();
    if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "publisherName = " + publisherName);

    // The publisher name first letter.
    String firstLetterPub = publisherName.substring(0, 1).toUpperCase();
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "firstLetterPub = " + firstLetterPub);

    // Get the tab table that corresponds to this publisher.
    Table divTable = divTableMap.get(firstLetterPub);

    // Check whether no table corresponds naturally to this publisher.
    if (divTable == null) {
      // Yes: Use the first table.
      divTable = divTableMap.get("A");

      // Check whether no table is found.
      if (divTable == null) {
	// Yes: Report the problem and skip this publisher.
	log.error("Publisher '" + publisherName
	    + "' belongs to an unknown tab: Skipped.");

	return;
      }
    }

    // Sanitize the publisher name so that it can be used as an HTML division
    // identifier.
    String cleanNameString = StringUtil.sanitizeToIdentifier(publisherName);
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "cleanNameString = " + cleanNameString);

    String publisherRowTitle = publisherName;

    // Check whether there are any publications to show.
    if (subSet != null && subSet.size() > 0) {
      // Yes: Get the publisher row title.
      publisherRowTitle += " (" + subSet.size() + ")";
    }

    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "publisherRowTitle = " + publisherRowTitle);

    // Create in the table the title row for the publisher.
    createPublisherRow(publisherRowTitle, cleanNameString,
	publisherSubscription.getPublisher().getPublisherNumber(),
	publisherSubscription.getSubscribed(), divTable);

    // Check whether there are any subscriptions to show.
    if (subSet != null) {
      // Yes: Add them.
      int rowIndex = 0;

      // Loop through all the subscriptions.
      for (Subscription subscription : subSet) {
        // Create in the table a row for the subscription.
        createSubscriptionRow(subscription, cleanNameString, rowIndex,
            divTable);
        rowIndex++;
      }
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Creates a row for a subscription.
   * 
   * @param subscription
   *          A Subscription with the subscription.
   * @param publisherId
   *          A String with the identifier of the publisher.
   * @param rowIndex
   *          An int with row number.
   * @param divTable
   *          A Table with the table where to create the row.
   */
  private void createSubscriptionRow(Subscription subscription,
      String publisherId, int rowIndex, Table divTable) {
    final String DEBUG_HEADER = "createSubscriptionRow(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "subscription = " + subscription);
      log.debug2(DEBUG_HEADER + "publisherId = " + publisherId);
      log.debug2(DEBUG_HEADER + "rowIndex = " + rowIndex);
    }

    divTable.newRow();

    Block newRow = divTable.row();
    newRow.attribute("class", publisherId + "_class hide-row "
	+ ServletUtil.rowCss(rowIndex, 3));

    // The subscription publication.
    SerialPublication publication = subscription.getPublication();

    divTable.addCell(publication.getUniqueName(),
	"class=\"sub-publication-name\"");

    Long subscriptionSeq = subscription.getSubscriptionSeq();
    String subscribedRangesId =
	SUBSCRIBED_RANGES_PARAM_NAME + subscriptionSeq;
    String unsubscribedRangesId =
	UNSUBSCRIBED_RANGES_PARAM_NAME + subscriptionSeq;

    // The subscribed ranges.
    Collection<BibliographicPeriod> subscribedRanges =
	subscription.getSubscribedRanges();
    boolean subscribedRangesDisabled = false;
    String subscribedRangesText =
	BibliographicPeriod.rangesAsString(subscribedRanges);

    // The unsubscribed ranges.
    Collection<BibliographicPeriod> unsubscribedRanges =
	subscription.getUnsubscribedRanges();
    boolean unsubscribedRangesDisabled = false;
    String unsubscribedRangesText =
	BibliographicPeriod.rangesAsString(unsubscribedRanges);

    Boolean publicationSubscriptionSetting = null;

    if (subscribedRanges != null && subscribedRanges.size() == 1
	&& subscribedRanges.iterator().next().isAllTime()) {
      publicationSubscriptionSetting = Boolean.TRUE;
      subscribedRangesDisabled = true;
      subscribedRangesText = "";
    } else if (unsubscribedRanges != null && unsubscribedRanges.size() == 1
	&& unsubscribedRanges.iterator().next().isAllTime()) {
      publicationSubscriptionSetting = Boolean.FALSE;
      subscribedRangesDisabled = true;
      subscribedRangesText = "";
      unsubscribedRangesDisabled = true;
      unsubscribedRangesText = "";
    }

    divTable.addCell(getPublicationSubscriptionBlock(subscriptionSeq,
	publicationSubscriptionSetting, subscribedRangesId,
	unsubscribedRangesId));

    // The subscribed ranges.
    Input subscribedInputBox = new Input(Input.Text, subscribedRangesId,
	subscribedRangesText);
    subscribedInputBox.setSize(25);
    subscribedInputBox.attribute("id", subscribedRangesId);

    if (subscribedRangesDisabled) {
      subscribedInputBox.attribute("disabled", true);
    }

    divTable.addCell(subscribedInputBox);
    
    Input unsubscribedInputBox = new Input(Input.Text, unsubscribedRangesId,
	unsubscribedRangesText);
    unsubscribedInputBox.setSize(25);
    unsubscribedInputBox.attribute("id", unsubscribedRangesId);

    if (unsubscribedRangesDisabled) {
      unsubscribedInputBox.attribute("disabled", true);
    }

    divTable.addCell(unsubscribedInputBox);

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Updates any subscriptions specified by the user in the form.
   * 
   * @return a SubscriptionOperationStatus with a summary of the status of the
   *         operation.
   */
  private SubscriptionOperationStatus updateSubscriptions() {
    final String DEBUG_HEADER = "updateSubscriptions(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    SubscriptionOperationStatus status = new SubscriptionOperationStatus();

    if (!hasSession()) {
      status.addStatusEntry(null, false, MISSING_COOKIES_MSG, null);
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "status = " + status);
      return status;
    } else {
      session = getSession();
    }

    // Get the Total Subscription setting presented in the form just submitted.
    Boolean totalSubscriptionSetting = null;

    if (subManager.isTotalSubscriptionEnabled()) {
      totalSubscriptionSetting =
	  (Boolean)session.getAttribute(TOTAL_SUBSCRIPTION_SESSION_KEY);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER
	  + "totalSubscriptionSetting = " + totalSubscriptionSetting);
    }

    // Get the publisher subscriptions presented in the form just submitted.
    Map<String, PublisherSubscription> subscribedPublishers =
	(Map<String, PublisherSubscription>)session
	.getAttribute(SUBSCRIBED_PUBLISHERS_SESSION_KEY);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "subscribedPublishers = "
	+ subscribedPublishers);

    // Get the subscriptions presented in the form just submitted.
    List<Subscription> subscriptions =
	(List<Subscription>) session.getAttribute(SUBSCRIPTIONS_SESSION_KEY);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "subscriptions = "
	+ subscriptions);

    // Handle session expiration.
    if (totalSubscriptionSetting == null
	&& (subscriptions == null || subscriptions.size() == 0)) {
      status.addStatusEntry(null, false, SESSION_EXPIRED_MSG, null);
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "status = " + status);
      return status;
    }

    // Get the map of parameters received from the submitted form.
    Map<String,String> parameterMap = getParamsAsMap();
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "parameterMap = " + parameterMap);

    boolean totalSubscriptionChanged = false;
    Boolean updatedTotalSubscriptionSetting = null;

    if (subManager.isTotalSubscriptionEnabled()) {
      updatedTotalSubscriptionSetting =
	  getTriBoxValue(parameterMap, TOTAL_SUBSCRIPTION_WIDGET_ID);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER
	  + "updatedTotalSubscriptionSetting = "
	  + updatedTotalSubscriptionSetting);

      totalSubscriptionChanged = (totalSubscriptionSetting == null
	  && updatedTotalSubscriptionSetting != null)
	  || (totalSubscriptionSetting != null
	  && updatedTotalSubscriptionSetting == null)
	  || (totalSubscriptionSetting != null
	  && totalSubscriptionSetting.booleanValue()
	  != updatedTotalSubscriptionSetting.booleanValue());
      if (log.isDebug3()) log.debug3(DEBUG_HEADER
	  + "totalSubscriptionChanged = " + totalSubscriptionChanged);
    }

    if (totalSubscriptionChanged && updatedTotalSubscriptionSetting != null
	&& updatedTotalSubscriptionSetting.booleanValue()) {
      subManager.handleStartingTotalSubscription(status);
    } else {
      boolean subChanged = false;
      Map<String, PublisherSubscription> updatePublisherSubscriptions =
	  new HashMap<String, PublisherSubscription>();
      Collection<Subscription> updateSubscriptions =
	  new ArrayList<Subscription>();

      if (totalSubscriptionSetting == null
	  && updatedTotalSubscriptionSetting == null) {
	// Loop through all the publisher subscriptions presented in the form.
	for (String publisherName : subscribedPublishers.keySet()) {
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "publisherName = " + publisherName);

	  PublisherSubscription publisherSubscription =
	      subscribedPublishers.get(publisherName);
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER
	      + "publisherSubscription = " + publisherSubscription);

	  Boolean oldPublisherSubscriptionSetting =
	      publisherSubscription.getSubscribed();
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER
	      + "oldPublisherSubscriptionSetting = "
	      + oldPublisherSubscriptionSetting);

	  Boolean newPublisherSubscriptionSetting = getTriBoxValue(parameterMap,
	      PUBLISHER_SUBSCRIPTION_WIDGET_ID_PREFIX
	      + publisherSubscription.getPublisher().getPublisherNumber());
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER
	      + "newPublisherSubscriptionSetting = "
	      + newPublisherSubscriptionSetting);

	  publisherSubscription.setSubscribed(newPublisherSubscriptionSetting);

	  // Get an indication of whether the publisher subscription has been
	  // changed.
	  if (oldPublisherSubscriptionSetting == null) {
	    subChanged = !(newPublisherSubscriptionSetting == null);
	  } else {
	    subChanged = !oldPublisherSubscriptionSetting
		.equals(newPublisherSubscriptionSetting);
	  }

	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "subChanged = " + subChanged);

	  if (subChanged) {
	    updatePublisherSubscriptions.put(publisherName,
		publisherSubscription);
	  }
	}

	if (log.isDebug3()) log.debug3(DEBUG_HEADER
	    + "updatePublisherSubscriptions.size() = "
	    + updatePublisherSubscriptions.size());

	// Loop through all the subscriptions presented in the form.
	for (Subscription subscription : subscriptions) {
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "subscription = " + subscription);

	  String publisherName =
	      subscription.getPublication().getPublisherName();
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "publisherName = " + publisherName);

	  if (subscribedPublishers.get(publisherName).getSubscribed() == null) {
	    // Get an indication of whether the subscription has been changed.
	    subChanged =
		isSubscriptionUpdateNeeded(subscription, parameterMap, status);
	    if (log.isDebug3())
	      log.debug3(DEBUG_HEADER + "subChanged = " + subChanged);

	    if (subChanged) {
	      updateSubscriptions.add(subscription);
	    }
	  } else {
	    if (log.isDebug3())
	      log.debug3(DEBUG_HEADER + "Ignored title subscription because "
		  + "publisher subscription is "
		  + subscribedPublishers.get(publisherName).getSubscribed());
	  }
	}

	if (log.isDebug3()) log.debug3(DEBUG_HEADER
	    + "updateSubscriptions.size() = " + updateSubscriptions.size());
      }

      // Record any updated subscriptions in the system.
      if (totalSubscriptionChanged || updatePublisherSubscriptions.size() > 0
	  || updateSubscriptions.size() > 0) {
	subManager.updateSubscriptions(totalSubscriptionChanged,
	    updatedTotalSubscriptionSetting,
	    updatePublisherSubscriptions.values(), updateSubscriptions, status);
      }
    }

    session.removeAttribute(SUBSCRIPTIONS_SESSION_KEY);
    session.removeAttribute(SUBSCRIBED_PUBLISHERS_SESSION_KEY);

    if (subManager.isTotalSubscriptionEnabled()) {
      session.removeAttribute(TOTAL_SUBSCRIPTION_SESSION_KEY);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "status = " + status);
    return status;
  }

  /**
   * Provides an indication of whether a subscription needs to be updated.
   * 
   * @param subscription
   *          A Subscription with the subscription.
   * @param parameterMap
   *          A Map<String, String> with the submitted form parameter names and
   *          their values.
   * @param status
   *          A SubscriptionOperationStatus where to record any failures.
   * @return a boolean with <code>true</code> if the subscription needs to be
   *         updated, or <code>false</code> otherwise.
   */
  private boolean isSubscriptionUpdateNeeded(Subscription subscription,
      Map<String, String> parameterMap, SubscriptionOperationStatus status) {
    final String DEBUG_HEADER = "isSubscriptionUpdateNeeded(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "subscription = " + subscription);

    SerialPublication publication = subscription.getPublication();

    // Check whether the publication has no TdbTitle.
    if (publication.getTdbTitle() == null) {
      // Yes: Get the publication name.
      String publicationName = publication.getPublicationName();
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "publicationName = "
	  + publicationName);

      // Report the problem.
      String message =
	  "Cannot find tdbTitle with name '" + publicationName + "'.";
      log.error(message);
      status.addStatusEntry(publication.getPublicationName(), false,
	  MISSING_TITLE_AUS_MSG, null);
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = false");
      return false;
    }

    boolean subsChanged = false;
    boolean unsubsChanged = false;
    String subscribedRangesText = "";
    String unsubscribedRangesText = "";
    List<BibliographicPeriod> subscribedRanges =
	subscription.getSubscribedRanges();
    List<BibliographicPeriod> unsubscribedRanges =
	subscription.getUnsubscribedRanges();
    String subscribedRangesAsString = "";
    String unsubscribedRangesAsString = "";

    Long subscriptionSeq = subscription.getSubscriptionSeq();
    if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "subscriptionSeq = " + subscriptionSeq);

    Boolean overallSubscriptionSetting = getTriBoxValue(parameterMap,
	      PUBLICATION_SUBSCRIPTION_WIDGET_ID_PREFIX + subscriptionSeq);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER
	+ "overallSubscriptionSetting = " + overallSubscriptionSetting);

    // Check whether the "Subscribe All" option has been selected.
    if (overallSubscriptionSetting != null
	&& overallSubscriptionSetting.booleanValue()) {
      // Yes: Handle a full subscription request. Determine whether the
      // subscribed ranges have changed.
      subsChanged = subscribedRanges == null || subscribedRanges.size() != 1
		|| !subscribedRanges.iterator().next().isAllTime();

      subscription.setSubscribedRanges(Collections
	  .singletonList(BibliographicPeriod.ALL_TIME_PERIOD));

      String unsubscribedRangesParamName =
	  UNSUBSCRIBED_RANGES_PARAM_NAME + subscriptionSeq;

      // Check whether there are exceptions to the full subscription request.
      if (parameterMap.containsKey(unsubscribedRangesParamName)) {
	// Yes: Get them.
	unsubscribedRangesText = StringUtil
	    .replaceString(parameterMap.get(unsubscribedRangesParamName), " ",
		"");
	if (log.isDebug3()) log.debug3(DEBUG_HEADER
	    + "unsubscribedRangesText = " + unsubscribedRangesText);
      }

      // Get any unsubscribed ranges in the original subscription.
      unsubscribedRangesAsString =
	  BibliographicPeriod.rangesAsString(unsubscribedRanges);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER
	  + "unsubscribedRangesAsString = " + unsubscribedRangesAsString);

      if (unsubscribedRangesAsString == null) {
	unsubscribedRangesAsString = "";
	if (log.isDebug3()) log.debug3(DEBUG_HEADER
	    + "unsubscribedRangesAsString = " + unsubscribedRangesAsString);
      }

      // Determine whether the unsubscribed ranges have changed.
      unsubsChanged =
	  !unsubscribedRangesText.equals(unsubscribedRangesAsString);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "unsubsChanged = " + unsubsChanged);

      if (unsubsChanged) {
	// Handle modified unsubscribed ranges.
	try {
	  unsubscribedRanges =
	      BibliographicPeriod.createList(unsubscribedRangesText);
	} catch (IllegalArgumentException iae) {
	  status.addStatusEntry(
	      subscription.getPublication().getPublicationName(), false,
	      BAD_UNSUBSCRIBED_RANGES_MSG, null);
	  if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = false");
	  return false;
	}

	if (unsubscribedRanges != null && unsubscribedRanges.size() > 0) {
	  // Validate the specified unsubscribed ranges.
	  Collection<BibliographicPeriod> invalidRanges =
	      subManager.validateRanges(unsubscribedRanges, publication);

	  // Check whether any unsubscribed ranges are not valid.
	  if (invalidRanges != null && invalidRanges.size() > 0) {
	    // Yes: Report the problem.
	    reportInvalidRanges(status, invalidRanges,
		publication.getPublicationName());

	    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = false");
	    return false;
	  }
	}

	subscription.setUnsubscribedRanges(unsubscribedRanges);
      }

      // No: Check whether the "Unsubscribe All" option has been selected.
    } else if (overallSubscriptionSetting != null
	&& !overallSubscriptionSetting.booleanValue()) {
      // Yes: Determine whether the subscribed ranges have changed.
      subsChanged = subscribedRanges != null && subscribedRanges.size() > 0;
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "subsChanged = " + subsChanged);

      // Determine whether the unsubscribed ranges have changed.
      unsubsChanged = unsubscribedRanges == null
	  || unsubscribedRanges.size() != 1
	  || !unsubscribedRanges.iterator().next().isAllTime();
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "unsubsChanged = " + unsubsChanged);

      if (subsChanged || unsubsChanged) {
	// Handle modified subscription ranges.
	subscription.setSubscribedRanges(Collections
	    .singletonList(new BibliographicPeriod("")));
	subscription.setUnsubscribedRanges(Collections
	    .singletonList(BibliographicPeriod.ALL_TIME_PERIOD));
      }
    } else {
      // No: Handle a partial subscription request.
      String unsubscribedRangesParamName =
	  UNSUBSCRIBED_RANGES_PARAM_NAME + subscriptionSeq;

      if (parameterMap.containsKey(unsubscribedRangesParamName)) {
	unsubscribedRangesText = StringUtil
	    .replaceString(parameterMap.get(unsubscribedRangesParamName), " ",
		"");
      }

      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "unsubscribedRangesText = "
	  + unsubscribedRangesText);

      unsubscribedRangesAsString =
	  BibliographicPeriod.rangesAsString(unsubscribedRanges);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER
	  + "unsubscribedRangesAsString = " + unsubscribedRangesAsString);

      if (unsubscribedRangesAsString == null) {
	unsubscribedRangesAsString = "";
	if (log.isDebug3()) log.debug3(DEBUG_HEADER
	    + "unsubscribedRangesAsString = " + unsubscribedRangesAsString);
      }

      unsubsChanged =
	  !unsubscribedRangesText.equals(unsubscribedRangesAsString);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "unsubsChanged = " + unsubsChanged);

      if (unsubsChanged) {
	try {
	  unsubscribedRanges =
	      BibliographicPeriod.createList(unsubscribedRangesText);
	} catch (IllegalArgumentException iae) {
	  status.addStatusEntry(
	      subscription.getPublication().getPublicationName(), false,
	      BAD_UNSUBSCRIBED_RANGES_MSG, null);
	  if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = false");
	  return false;
	}

	if (unsubscribedRanges != null && unsubscribedRanges.size() > 0) {
	  // Validate the specified unsubscribed ranges.
	  Collection<BibliographicPeriod> invalidRanges =
	      subManager.validateRanges(unsubscribedRanges, publication);

	  // Check whether any unsubscribed ranges are not valid.
	  if (invalidRanges != null && invalidRanges.size() > 0) {
	    // Yes: Report the problem.
	    reportInvalidRanges(status, invalidRanges,
		publication.getPublicationName());

	    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = false");
	    return false;
	  }
	}

	subscription.setUnsubscribedRanges(unsubscribedRanges);
      }

      String subscribedRangesParamName =
	  SUBSCRIBED_RANGES_PARAM_NAME + subscriptionSeq;

      if (parameterMap.containsKey(subscribedRangesParamName)) {
	subscribedRangesText = StringUtil
	    .replaceString(parameterMap.get(subscribedRangesParamName), " ",
		"");
      }

      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "subscribedRangesText = "
	  + subscribedRangesText);

      subscribedRangesAsString =
	  BibliographicPeriod.rangesAsString(subscribedRanges);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER
	  + "subscribedRangesAsString = " + subscribedRangesAsString);

      if (subscribedRangesAsString == null) {
	subscribedRangesAsString = "";
	if (log.isDebug3()) log.debug3(DEBUG_HEADER
	    + "subscribedRangesAsString = " + subscribedRangesAsString);
      }

      subsChanged = !subscribedRangesText.equals(subscribedRangesAsString);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "subsChanged = " + subsChanged);

      if (subsChanged) {
	try {
	  subscribedRanges =
	      BibliographicPeriod.createList(subscribedRangesText);
	} catch (IllegalArgumentException iae) {
	  status.addStatusEntry(
	      subscription.getPublication().getPublicationName(), false,
	      BAD_SUBSCRIBED_RANGES_MSG, null);
	  if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = false");
	  return false;
	}

	if (subscribedRanges != null && subscribedRanges.size() > 0) {
	  // Validate the specified subscribed ranges.
	  Collection<BibliographicPeriod> invalidRanges =
	      subManager.validateRanges(subscribedRanges, publication);

	  // Check whether any subscribed ranges are not valid.
	  if (invalidRanges != null && invalidRanges.size() > 0) {
	    // Yes: Report the problem.
	    reportInvalidRanges(status, invalidRanges,
		publication.getPublicationName());

	    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = false");
	    return false;
	  }
	}

	subscription.setSubscribedRanges(subscribedRanges);
      }
    }

    boolean result = subsChanged || unsubsChanged;
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }
}
