/*
 * $Id: MetadataMonitor.java 44257 2015-09-24 22:08:54Z fergaloy-sf $
 */

/*

 Copyright (c) 2015 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import org.lockss.db.DbException;
import org.lockss.db.DbManager;
import org.lockss.metadata.Isbn;
import org.lockss.metadata.Issn;
import org.lockss.metadata.MetadataManager;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.PluginManager;
import org.lockss.servlet.ServletUtil.LinkWithExplanation;
import org.lockss.util.Logger;
import org.mortbay.html.Block;
import org.mortbay.html.Composite;
import org.mortbay.html.Page;
import org.mortbay.html.Table;

/**
 * Metadata monitor servlet.
 * 
 * @author Fernando Garcia-Loygorri
 */
@SuppressWarnings("serial")
public class MetadataMonitor extends LockssServlet {
  private static final Logger log = Logger.getLogger(MetadataMonitor.class);

  private static final String LIST_PUBLISHER_NAMES_LINK = "Publisher Names";
  private static final String LIST_PUBLISHER_NAMES_ACTION =
      "listPublisherNames";
  private static final String LIST_PUBLISHER_NAMES_HELP =
      "Lists the names of publishers";
  private static final String LIST_PUBLISHER_NAMES_HEADER = "Publisher Names";
  private static final String LIST_MULTIPLE_DOI_PREFIX_PUBLISHERS_LINK =
      "Publishers With Multiple DOI Prefixes";
  private static final String LIST_MULTIPLE_DOI_PREFIX_PUBLISHERS_ACTION =
      "listMultipleDoiPrefixPublishers";
  private static final String LIST_MULTIPLE_DOI_PREFIX_PUBLISHERS_HELP =
      "Lists the names of publishers with more than one DOI prefix";
  private static final String LIST_MULTIPLE_DOI_PREFIX_PUBLISHERS_HEADER =
      "Publishers With Multiple DOI Prefixes";
  private static final String LIST_MULTIPLE_DOI_PREFIX_AUS_LINK =
      "Archival Units With Multiple DOI Prefixes";
  private static final String LIST_MULTIPLE_DOI_PREFIX_AUS_ACTION =
      "listMultipleDoiPrefixAus";
  private static final String LIST_MULTIPLE_DOI_PREFIX_AUS_HELP =
      "Lists the names of Archival Units with more than one DOI prefix";
  private static final String LIST_MULTIPLE_DOI_PREFIX_AUS_HEADER =
      "Archival Units With Multiple DOI Prefixes";
  private static final String LIST_MULTIPLE_PUBLISHER_DOI_PREFIXES_LINK =
      "DOI Prefixes Linked To Multiple Publishers";
  private static final String LIST_MULTIPLE_PUBLISHER_DOI_PREFIXES_ACTION =
      "listMultiplePublisherDoiPrefixes";
  private static final String LIST_MULTIPLE_PUBLISHER_DOI_PREFIXES_HELP =
      "Lists the DOI prefixes liked to more than one publisher";
  private static final String LIST_MULTIPLE_PUBLISHER_DOI_PREFIXES_HEADER =
      "DOI Prefixes Linked To Multiple Publishers";
  private static final String LIST_MULTIPLE_ISBSN_PUBLICATIONS_LINK =
      "Publications With More Than 2 ISBNs/ISSNs";
  private static final String LIST_MULTIPLE_ISBSN_PUBLICATIONS_ACTION =
      "listMultipleIsbsnPublications";
  private static final String LIST_MULTIPLE_ISBSN_PUBLICATIONS_HELP =
      "Lists the names of publications with more than two ISBN/ISSN values";
  private static final String LIST_MULTIPLE_ISBSN_PUBLICATIONS_HEADER =
      "Publications With Multiple ISBNs/ISSNs";
  private static final String LIST_MULTIPLE_PUBLICATION_ISBSNS_LINK =
      "ISBNs/ISSNs Linked To Multiple Publications";
  private static final String LIST_MULTIPLE_PUBLICATION_ISBSNS_ACTION =
      "listMultiplePublicationIsbsns";
  private static final String LIST_MULTIPLE_PUBLICATION_ISBSNS_HELP =
      "Lists the ISBN/ISSN values linked to more than one publication";
  private static final String LIST_MULTIPLE_PUBLICATION_ISBSNS_HEADER =
      "ISBNs/ISSNs Linked To Multiple Publications";
  private static final String LIST_MISMATCHED_ISBSNS_PUBLICATION_TYPES_LINK =
      "ISBNs/ISSNs Mismatched To Publication Types";
  private static final String LIST_MISMATCHED_ISBSNS_PUBLICATION_TYPES_ACTION =
      "listMismatchedPublicationTypeIsbsns";
  private static final String LIST_MISMATCHED_ISBSNS_PUBLICATION_TYPES_HELP =
      "Lists the ISBN/ISSN values that do not match the type of publication";
  private static final String LIST_MISMATCHED_ISBSNS_PUBLICATION_TYPES_HEADER =
      "ISBNs/ISSNs Mismatched To Publication Types";
  private static final String LIST_UNKNOWN_PROVIDER_AUS_LINK =
      "Archival Units With Unknown Provider";
  private static final String LIST_UNKNOWN_PROVIDER_AUS_ACTION =
      "listUnknownProviderAus";
  private static final String LIST_UNKNOWN_PROVIDER_AUS_HELP =
      "Lists the names of Archival Units with an unknown provider";
  private static final String LIST_UNKNOWN_PROVIDER_AUS_HEADER =
      "Archival Units With Unknown Provider";
  private static final String LIST_MISMATCHED_PARENT_TO_CHILDREN_LINK =
      "Children Mismatched To Parents";
  private static final String LIST_MISMATCHED_PARENT_TO_CHILDREN_ACTION =
      "listMismatchedParentToChildren";
  private static final String LIST_MISMATCHED_PARENT_TO_CHILDREN_HELP =
      "Lists the children whose parents are of the wrong type";
  private static final String LIST_MISMATCHED_PARENT_TO_CHILDREN_HEADER =
      "Children Mismatched To Parents";
  private static final String LIST_MULTIPLE_PUBLISHER_AUS_LINK =
      "Archival Units With Multiple Publishers";
  private static final String LIST_MULTIPLE_PUBLISHER_AUS_ACTION =
      "listMultiplePublisherAus";
  private static final String LIST_MULTIPLE_PUBLISHER_AUS_HELP =
      "Lists the names of Archival Units with more than one publisher";
  private static final String LIST_MULTIPLE_PUBLISHER_AUS_HEADER =
      "Archival Units With Multiple Publishers";
  private static final String LIST_ITEMS_WITHOUT_NAME_LINK =
      "Metadata Items Without Name";
  private static final String LIST_ITEMS_WITHOUT_NAME_ACTION =
      "listMetadataItemsWithoutName";
  private static final String LIST_ITEMS_WITHOUT_NAME_HELP =
      "Lists the metadata items that have no name";
  private static final String LIST_ITEMS_WITHOUT_NAME_HEADER =
      "Metadata Items Without Name";

  private static final String BACK_LINK_PREFIX = "Back to ";

  private DbManager dbManager;
  private MetadataManager mdManager;
  private PluginManager pluginManager;

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
    dbManager = getLockssDaemon().getDbManager();
    mdManager = getLockssDaemon().getMetadataManager();
    pluginManager = getLockssDaemon().getPluginManager();
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

    // If the database is not available, display a warning message.
    if (!dbManager.isReady()) {
      displayNotStarted();
      return;
    }

    String action = req.getParameter(ACTION_TAG);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "action = " + action);

    try {
      if (LIST_PUBLISHER_NAMES_ACTION.equals(action)) {
	listPublisherNames();
      } else if (LIST_MULTIPLE_DOI_PREFIX_PUBLISHERS_ACTION.equals(action)) {
	listMultipleDoiPrefixPublishers();
      } else if (LIST_MULTIPLE_PUBLISHER_DOI_PREFIXES_ACTION.equals(action)) {
	listMultiplePublisherDoiPrefixes();
      } else if (LIST_MULTIPLE_DOI_PREFIX_AUS_ACTION.equals(action)) {
	listMultipleDoiPrefixAus();
      } else if (LIST_MULTIPLE_ISBSN_PUBLICATIONS_ACTION.equals(action)) {
	listMultipleIsbsnPublications();
      } else if (LIST_MULTIPLE_PUBLICATION_ISBSNS_ACTION.equals(action)) {
	listMultiplePublicationIsbsns();
      } else if (LIST_MISMATCHED_ISBSNS_PUBLICATION_TYPES_ACTION.equals(action))
      {
	listMismatchedPublicationTypeIsbsns();
      } else if (LIST_UNKNOWN_PROVIDER_AUS_ACTION.equals(action)) {
	listUnknownProviderAus();
      } else if (LIST_MISMATCHED_PARENT_TO_CHILDREN_ACTION.equals(action)) {
	listMismatchedParentToChildren();
      } else if (LIST_MULTIPLE_PUBLISHER_AUS_ACTION.equals(action)) {
	listMultiplePublisherAus();
      } else if (LIST_ITEMS_WITHOUT_NAME_ACTION.equals(action)) {
	listMetadataItemsWithoutName();
      } else {
	displayMainPage();
      }
    } catch (DbException dbe) {
      throw new RuntimeException(dbe);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Displays the metadata monitor main page.
   * 
   * @throws IOException
   */
  void displayMainPage() throws IOException {
    final String DEBUG_HEADER = "displayMainPage(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");
    Page page = newPage();
    layoutErrorBlock(page);
    ServletUtil.layoutMenu(page, getMenuDescriptors());
    endPage(page);
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides the links displayed in the metadata monitor main page.
   * 
   * @return an Iterator<LinkWithExplanation> with the link data.
   */
  private Iterator<LinkWithExplanation> getMenuDescriptors() {
    String ACTION = ACTION_TAG + "=";
    ServletDescr myDescr = myServletDescr();
    ArrayList<LinkWithExplanation> list = new ArrayList<LinkWithExplanation>();

    // List the publisher names.
    list.add(getMenuDescriptor(myDescr,
	LIST_PUBLISHER_NAMES_LINK,
	ACTION + LIST_PUBLISHER_NAMES_ACTION,
	LIST_PUBLISHER_NAMES_HELP));

    // List the publishers with multiple DOI prefixes.
    list.add(getMenuDescriptor(myDescr,
	LIST_MULTIPLE_DOI_PREFIX_PUBLISHERS_LINK,
	ACTION + LIST_MULTIPLE_DOI_PREFIX_PUBLISHERS_ACTION,
	LIST_MULTIPLE_DOI_PREFIX_PUBLISHERS_HELP));

    // List the DOI prefixes linked to multiple publishers.
    list.add(getMenuDescriptor(myDescr,
	LIST_MULTIPLE_PUBLISHER_DOI_PREFIXES_LINK,
	ACTION + LIST_MULTIPLE_PUBLISHER_DOI_PREFIXES_ACTION,
	LIST_MULTIPLE_PUBLISHER_DOI_PREFIXES_HELP));

    // List the Archival Units with multiple DOI prefixes.
    list.add(getMenuDescriptor(myDescr,
	LIST_MULTIPLE_DOI_PREFIX_AUS_LINK,
	ACTION + LIST_MULTIPLE_DOI_PREFIX_AUS_ACTION,
	LIST_MULTIPLE_DOI_PREFIX_AUS_HELP));

    // List the publications with more than 2 ISBN/ISSN values.
    list.add(getMenuDescriptor(myDescr,
	LIST_MULTIPLE_ISBSN_PUBLICATIONS_LINK,
	ACTION + LIST_MULTIPLE_ISBSN_PUBLICATIONS_ACTION,
	LIST_MULTIPLE_ISBSN_PUBLICATIONS_HELP));

    // List the ISBN/ISSN values linked to multiple publications.
    list.add(getMenuDescriptor(myDescr,
	LIST_MULTIPLE_PUBLICATION_ISBSNS_LINK,
	ACTION + LIST_MULTIPLE_PUBLICATION_ISBSNS_ACTION,
	LIST_MULTIPLE_PUBLICATION_ISBSNS_HELP));

    // List the ISBN/ISSN values that do not match the type of publication.
    list.add(getMenuDescriptor(myDescr,
	LIST_MISMATCHED_ISBSNS_PUBLICATION_TYPES_LINK,
	ACTION + LIST_MISMATCHED_ISBSNS_PUBLICATION_TYPES_ACTION,
	LIST_MISMATCHED_ISBSNS_PUBLICATION_TYPES_HELP));

    // List the Archival Units with an unknown provider.
    list.add(getMenuDescriptor(myDescr,
	LIST_UNKNOWN_PROVIDER_AUS_LINK,
	ACTION + LIST_UNKNOWN_PROVIDER_AUS_ACTION,
	LIST_UNKNOWN_PROVIDER_AUS_HELP));

    // List the children whose parents are of the wrong type.
    list.add(getMenuDescriptor(myDescr,
	LIST_MISMATCHED_PARENT_TO_CHILDREN_LINK,
	ACTION + LIST_MISMATCHED_PARENT_TO_CHILDREN_ACTION,
	LIST_MISMATCHED_PARENT_TO_CHILDREN_HELP));

    // List the Archival Units with multiple publishers.
    list.add(getMenuDescriptor(myDescr,
	LIST_MULTIPLE_PUBLISHER_AUS_LINK,
	ACTION + LIST_MULTIPLE_PUBLISHER_AUS_ACTION,
	LIST_MULTIPLE_PUBLISHER_AUS_HELP));

    // List the metadata items that have no name.
    list.add(getMenuDescriptor(myDescr,
	LIST_ITEMS_WITHOUT_NAME_LINK,
	ACTION + LIST_ITEMS_WITHOUT_NAME_ACTION,
	LIST_ITEMS_WITHOUT_NAME_HELP));

    return list.iterator();
  }

  /**
   * Displays the names of the publishers in the database.
   * 
   * @throws DbException
   *           if any problem occurred accessing the database.
   * @throws IOException
   */
  private void listPublisherNames() throws DbException, IOException {
    final String DEBUG_HEADER = "listPublisherNames(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    String attributes = "align=\"center\" cellspacing=\"4\" cellpadding=\"5\"";

    // Create the results table.
    Table results = new Table(0, attributes);
    results.newRow();
    results.newCell("align=\"center\" class=\"colhead\"");
    results.add("Publisher Name");

    // Get the publisher names.
    Collection<String> publisherNames = mdManager.getPublisherNames();
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "publisherNames.size() = "
	+ publisherNames.size());

    // Loop  through the publisher names.
    for (String publisherName : publisherNames) {
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "publisherName = " + publisherName);
      results.newRow();
      results.newCell("align=\"center\"");
      results.add(publisherName);
    }

    makeTablePage(LIST_PUBLISHER_NAMES_HEADER, results);
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Creates a page with a table of results.
   * 
   * @param heading
   *          A String with the page heading.
   * @param results
   *          A Table with the results to be displayed.
   * @throws IOException
   */
  private void makeTablePage(String heading, Table results) throws IOException {
    makeTablePage(heading, results, null);
  }

  /**
   * Creates a page with two tables of results.
   * 
   * @param heading
   *          A String with the page heading.
   * @param results1
   *          A Table with the first results to be displayed.
   * @param results2
   *          A Table with the last results to be displayed.
   * @throws IOException
   */
  private void makeTablePage(String heading, Table results1, Table results2)
      throws IOException {
    makeTablePage(heading, results1, results2, null);
  }

  /**
   * Creates a page with three tables of results.
   * 
   * @param heading
   *          A String with the page heading.
   * @param results1
   *          A Table with the first results to be displayed.
   * @param results2
   *          A Table with the second results to be displayed.
   * @param results3
   *          A Table with the last results to be displayed.
   * @throws IOException
   */
  private void makeTablePage(String heading, Table results1, Table results2,
      Table results3) throws IOException {
    final String DEBUG_HEADER = "makeTablePage(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "heading = " + heading);

    Page page = newPage();
    layoutErrorBlock(page);

    String attributes = "align=\"center\" cellspacing=\"4\" cellpadding=\"5\"";

    Table mainTable = new Table(0, attributes);
    mainTable.addHeading(heading);
    mainTable.newRow();
    mainTable.newCell();
    mainTable.newRow();
    mainTable.newCell();

    mainTable.add(results1);

    if (results2 != null) {
      mainTable.newRow();
      mainTable.newCell();
      mainTable.newRow();
      mainTable.newCell();

      mainTable.add(results2);

      if (results3 != null) {
	mainTable.newRow();
	mainTable.newCell();
	mainTable.newRow();
	mainTable.newCell();

	mainTable.add(results3);
      }
    }

    Composite comp = new Block(Block.Center);
    comp.add(mainTable);
    comp.add("<br>");
    page.add(comp);

    ServletUtil.layoutBackLink(page,
	  srvLink(AdminServletManager.SERVLET_MD_MONITOR, BACK_LINK_PREFIX
	          + getHeading(AdminServletManager.SERVLET_MD_MONITOR)));

    endPage(page);
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Displays the DOI prefixes for the publishers in the database with multiple
   * DOI prefixes.
   * 
   * @throws DbException
   *           if any problem occurred accessing the database.
   * @throws IOException
   */
  private void listMultipleDoiPrefixPublishers()
      throws DbException, IOException {
    final String DEBUG_HEADER = "listMultipleDoiPrefixPublishers(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    String attributes = "align=\"left\" cellspacing=\"4\" cellpadding=\"5\"";

    // Create the results table.
    Table results = new Table(0, attributes);
    results.newRow();
    results.newCell("align=\"right\" class=\"colhead\"");
    results.add("Publisher Name");
    results.newCell("align=\"left\" class=\"colhead\"");
    results.add("DOI Prefixes");

    // Get the DOI prefixes linked to the publishers.
    Map<String, Collection<String>> publishersDoiPrefixes =
	mdManager.getPublishersWithMultipleDoiPrefixes();
    if (log.isDebug3()) log.debug3(DEBUG_HEADER
	+ "publishersDoiPrefixes.size() = " + publishersDoiPrefixes.size());

    // Check whether there are results to display.
    if (publishersDoiPrefixes.size() > 0) {
      // Yes: Loop through the publishers.
      for (String publisherName : publishersDoiPrefixes.keySet()) {
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "publisherName = " + publisherName);

	results.newRow();
	results.newCell("align=\"right\"");
	results.add(publisherName);
	results.newCell("align=\"left\"");

	Table prefixes = new Table(1, attributes);

	for (String prefix : publishersDoiPrefixes.get(publisherName)) {
	  prefixes.newRow();
	  prefixes.newCell("align=\"left\"");
	  prefixes.add(prefix);
	}

	results.add(prefixes);
      }
    } else {
      // No.
      results.newRow();
      results.newCell();
      results.add("");
      results.newRow();
      results.newCell("colspan=\"2\" align=\"center\"");
      results.add("No publishers are linked to more than one DOI prefix");
    }

    makeTablePage(LIST_MULTIPLE_DOI_PREFIX_PUBLISHERS_HEADER, results);
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Displays the publisher names linked to DOI prefixes in the database that
   * are linked to multiple publishers.
   * 
   * @throws DbException
   *           if any problem occurred accessing the database.
   * @throws IOException
   */
  private void listMultiplePublisherDoiPrefixes()
      throws DbException, IOException {
    final String DEBUG_HEADER = "listMultiplePublisherDoiPrefixes(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    String attributes = "align=\"left\" cellspacing=\"4\" cellpadding=\"5\"";

    // Create the results table.
    Table results = new Table(0, attributes);
    results.newRow();
    results.newCell("align=\"right\" class=\"colhead\"");
    results.add("DOI Prefix");
    results.newCell("align=\"left\" class=\"colhead\"");
    results.add("Publisher Names");

    // Get the publishers linked to the DOI prefixes.
    Map<String, Collection<String>> doiPrefixesPublishers =
	mdManager.getDoiPrefixesWithMultiplePublishers();
    if (log.isDebug3()) log.debug3(DEBUG_HEADER
	+ "doiPrefixesPublishers.size() = " + doiPrefixesPublishers.size());

    // Check whether there are results to display.
    if (doiPrefixesPublishers.size() > 0) {
      // Yes: Loop through the DOI prefixes.
      for (String prefix : doiPrefixesPublishers.keySet()) {
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "prefix = " + prefix);

	results.newRow();
	results.newCell("align=\"right\"");
	results.add(prefix);
	results.newCell("align=\"left\"");

	Table publisherNames = new Table(1, attributes);

	for (String publisherName : doiPrefixesPublishers.get(prefix)) {
	  publisherNames.newRow();
	  publisherNames.newCell("align=\"left\"");
	  publisherNames.add(publisherName);
	}

	results.add(publisherNames);
      }
    } else {
      // No.
      results.newRow();
      results.newCell();
      results.add("");
      results.newRow();
      results.newCell("colspan=\"2\" align=\"center\"");
      results.add("No DOI prefixes are linked to more than one publisher");
    }

    makeTablePage(LIST_MULTIPLE_PUBLISHER_DOI_PREFIXES_HEADER, results);
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Displays the DOI prefixes for the Archival Units in the database with
   * multiple DOI prefixes.
   * 
   * @throws DbException
   *           if any problem occurred accessing the database.
   * @throws IOException
   */
  private void listMultipleDoiPrefixAus() throws DbException, IOException {
    final String DEBUG_HEADER = "listMultipleDoiPrefixAus(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    String attributes = "align=\"left\" cellspacing=\"4\" cellpadding=\"5\"";

    // Create the results table.
    Table results = new Table(0, attributes);
    results.newRow();
    results.newCell("align=\"right\" class=\"colhead\"");
    results.add("Archival Unit Name");
    results.newCell("align=\"left\" class=\"colhead\"");
    results.add("DOI Prefixes");

    // The Archival Units that have multiple DOI prefixes, sorted by name.
    Map<String, Collection<String>> ausToList =
	mdManager.getAuNamesWithMultipleDoiPrefixes();
    if (log.isDebug3()) log.debug3(DEBUG_HEADER
	+ "ausToList.size() = " + ausToList.size());

    // Check whether there are results to display.
    if (ausToList.size() > 0) {
      // Yes: Loop through the Archival Units to be listed.
      for (String auName : ausToList.keySet()) {
	results.newRow();
	results.newCell("align=\"right\"");
	results.add(auName);
	results.newCell("align=\"left\"");
      
	Table prefixes = new Table(1, attributes);

	for (String prefix : ausToList.get(auName)) {
	  prefixes.newRow();
	  prefixes.newCell("align=\"left\"");
	  prefixes.add(prefix);
	}

	results.add(prefixes);
      }
    } else {
      // No.
      results.newRow();
      results.newCell();
      results.add("");
      results.newRow();
      results.newCell("colspan=\"2\" align=\"center\"");
      results.add("No Archival Units are linked to more than one DOI prefix");
    }

    makeTablePage(LIST_MULTIPLE_DOI_PREFIX_AUS_HEADER, results);
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Displays the ISSNs/ISBNs for the publications in the database with more
   * than two ISSNs/ISBNS.
   * 
   * @throws DbException
   *           if any problem occurred accessing the database.
   * @throws IOException
   */
  private void listMultipleIsbsnPublications() throws DbException, IOException {
    final String DEBUG_HEADER = "listMultipleIsbsnPublications(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    String attributes = "align=\"left\" cellspacing=\"4\" cellpadding=\"5\"";

    // Create the ISSN results table.
    Table results1 = new Table(0, attributes);
    results1.newRow();
    results1.newCell("align=\"right\" class=\"colhead\"");
    results1.add("Publication Name");
    results1.newCell("align=\"left\" class=\"colhead\"");
    results1.add("ISSNs");

    // Get the ISSNs linked to the publications.
    Map<String, Collection<Issn>> publicationsIssns =
	mdManager.getPublicationsWithMoreThan2Issns();
    if (log.isDebug3()) log.debug3(DEBUG_HEADER
	+ "publicationsIssns.size() = " + publicationsIssns.size());

    // Check whether there are results to display.
    if (publicationsIssns.size() > 0) {
      // Yes: Loop through the publications.
      for (String publicationName : publicationsIssns.keySet()) {
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "publicationName = " + publicationName);

	results1.newRow();
	results1.newCell("align=\"right\"");
	results1.add(publicationName);
	results1.newCell("align=\"left\"");

	Table issns = new Table(1, attributes);

	for (Issn issn : publicationsIssns.get(publicationName)) {
	  issns.newRow();
	  issns.newCell("align=\"left\"");
	  issns.add(issn.getValue() + " [" + issn.getType().substring(0, 1)
	      + "]");
	}

	results1.add(issns);
      }
    } else {
      // No.
      results1.newRow();
      results1.newCell();
      results1.add("");
      results1.newRow();
      results1.newCell("colspan=\"2\" align=\"center\"");
      results1.add("No publications are linked to more than two ISSNs");
    }

    // Create the ISBN results table.
    Table results2 = new Table(0, attributes);
    results2.newRow();
    results2.newCell("align=\"right\" class=\"colhead\"");
    results2.add("Publication Name");
    results2.newCell("align=\"left\" class=\"colhead\"");
    results2.add("ISBNs");

    // Get the ISBNs linked to the publications.
    Map<String, Collection<Isbn>> publicationsIsbns =
	mdManager.getPublicationsWithMoreThan2Isbns();
    if (log.isDebug3()) log.debug3(DEBUG_HEADER
	+ "publicationsIsbns.size() = " + publicationsIsbns.size());

    // Check whether there are results to display.
    if (publicationsIsbns.size() > 0) {
      // Yes: Loop through the publications.
      for (String publicationName : publicationsIsbns.keySet()) {
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "publicationName = " + publicationName);

	results2.newRow();
	results2.newCell("align=\"right\"");
	results2.add(publicationName);
	results2.newCell("align=\"left\"");

	Table isbns = new Table(1, attributes);

	for (Isbn isbn : publicationsIsbns.get(publicationName)) {
	  isbns.newRow();
	  isbns.newCell("align=\"left\"");
	  isbns.add(isbn.getValue() + " [" + isbn.getType().substring(0, 1)
	      + "]");
	}

	results2.add(isbns);
      }
    } else {
      // No.
      results2.newRow();
      results2.newCell();
      results2.add("");
      results2.newRow();
      results2.newCell("colspan=\"2\" align=\"center\"");
      results2.add("No publications are linked to more than two ISBNs");
    }

    makeTablePage(LIST_MULTIPLE_ISBSN_PUBLICATIONS_HEADER, results1, results2);
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Displays the publication names linked to ISSNs/ISBNs in the database that
   * are linked to multiple publications.
   * 
   * @throws DbException
   *           if any problem occurred accessing the database.
   * @throws IOException
   */
  private void listMultiplePublicationIsbsns() throws DbException, IOException {
    final String DEBUG_HEADER = "listMultiplePublicationIsbsns(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    String attributes = "align=\"left\" cellspacing=\"4\" cellpadding=\"5\"";

    // Create the ISSN results table.
    Table results1 = new Table(0, attributes);
    results1.newRow();
    results1.newCell("align=\"right\" class=\"colhead\"");
    results1.add("ISSNs");
    results1.newCell("align=\"left\" class=\"colhead\"");
    results1.add("Publication Name");

    // Get the publications linked to the ISSNs.
    Map<String, Collection<String>> issnsPublications =
	mdManager.getIssnsWithMultiplePublications();
    if (log.isDebug3()) log.debug3(DEBUG_HEADER
	+ "issnsPublications.size() = " + issnsPublications.size());

    // Check whether there are results to display.
    if (issnsPublications.size() > 0) {
      // Yes: Loop through the ISSNs. 
      for (String issn : issnsPublications.keySet()) {
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "issn = " + issn);

	results1.newRow();
	results1.newCell("align=\"right\"");
	results1.add(issn);
	results1.newCell("align=\"left\"");

	Table publicationNames = new Table(1, attributes);

	for (String publicationName : issnsPublications.get(issn)) {
	  publicationNames.newRow();
	  publicationNames.newCell("align=\"left\"");
	  publicationNames.add(publicationName);
	}

	results1.add(publicationNames);
      }
    } else {
      // No.
      results1.newRow();
      results1.newCell();
      results1.add("");
      results1.newRow();
      results1.newCell("colspan=\"2\" align=\"center\"");
      results1.add("No ISSNs are linked to more than one publication");
    }

    // Create the ISBN results table.
    Table results2 = new Table(0, attributes);
    results2.newRow();
    results2.newCell("align=\"right\" class=\"colhead\"");
    results2.add("ISBNs");
    results2.newCell("align=\"left\" class=\"colhead\"");
    results2.add("Publication Name");

    // Get the publications linked to the ISBNs.
    Map<String, Collection<String>> isbnsPublications =
	mdManager.getIsbnsWithMultiplePublications();
    if (log.isDebug3()) log.debug3(DEBUG_HEADER
	+ "isbnsPublications.size() = " + isbnsPublications.size());

    // Check whether there are results to display.
    if (isbnsPublications.size() > 0) {
      // Yes: Loop through the ISBNs. 
      for (String isbn : isbnsPublications.keySet()) {
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "isbn = " + isbn);

	results2.newRow();
	results2.newCell("align=\"right\"");
	results2.add(isbn);
	results2.newCell("align=\"left\"");

	Table publicationNames = new Table(1, attributes);

	for (String publicationName : isbnsPublications.get(isbn)) {
	  publicationNames.newRow();
	  publicationNames.newCell("align=\"left\"");
	  publicationNames.add(publicationName);
	}

	results2.add(publicationNames);
      }
    } else {
      // No.
      results2.newRow();
      results2.newCell();
      results2.add("");
      results2.newRow();
      results2.newCell("colspan=\"2\" align=\"center\"");
      results2.add("No ISBNs are linked to more than one publication");
    }

    makeTablePage(LIST_MULTIPLE_PUBLICATION_ISBSNS_HEADER, results1, results2);
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Displays the ISBN/ISSN values in the database that do not match the type of
   * publication.
   * 
   * @throws DbException
   *           if any problem occurred accessing the database.
   * @throws IOException
   */
  private void listMismatchedPublicationTypeIsbsns()
      throws DbException, IOException {
    final String DEBUG_HEADER = "listMismatchedPublicationTypeIsbsns(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    String attributes = "align=\"left\" cellspacing=\"4\" cellpadding=\"5\"";

    // Create the book results table.
    Table results1 = new Table(0, attributes);
    results1.newRow();
    results1.newCell("align=\"right\" class=\"colhead\"");
    results1.add("Book Name");
    results1.newCell("align=\"left\" class=\"colhead\"");
    results1.add("ISSNs");

    // Get the ISSNs linked to the books.
    Map<String, Collection<String>> publicationsIssns =
	mdManager.getBooksWithIssns();
    if (log.isDebug3()) log.debug3(DEBUG_HEADER
	+ "publicationsIssns.size() = " + publicationsIssns.size());

    // Check whether there are results to display.
    if (publicationsIssns.size() > 0) {
      // Yes: Loop through the publications.
      for (String publicationName : publicationsIssns.keySet()) {
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "publicationName = " + publicationName);

	results1.newRow();
	results1.newCell("align=\"right\"");
	results1.add(publicationName);
	results1.newCell("align=\"left\"");

	Table issns = new Table(1, attributes);

	for (String issn : publicationsIssns.get(publicationName)) {
	  issns.newRow();
	  issns.newCell("align=\"left\"");
	  issns.add(issn);
	}

	results1.add(issns);
      }
    } else {
      // No.
      results1.newRow();
      results1.newCell();
      results1.add("");
      results1.newRow();
      results1.newCell("colspan=\"2\" align=\"center\"");
      results1.add("No books are linked to ISSNs");
    }

    // Create the periodical results table.
    Table results2 = new Table(0, attributes);
    results2.newRow();
    results2.newCell("align=\"right\" class=\"colhead\"");
    results2.add("Periodical Name");
    results2.newCell("align=\"left\" class=\"colhead\"");
    results2.add("ISBNs");

    // Get the ISBNs linked to the periodicals.
    Map<String, Collection<String>> publicationsIsbns =
	mdManager.getPeriodicalsWithIsbns();
    if (log.isDebug3()) log.debug3(DEBUG_HEADER
	+ "publicationsIsbns.size() = " + publicationsIsbns.size());

    // Check whether there are results to display.
    if (publicationsIsbns.size() > 0) {
      // Yes: Loop through the publications.
      for (String publicationName : publicationsIsbns.keySet()) {
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "publicationName = " + publicationName);

	results2.newRow();
	results2.newCell("align=\"right\"");
	results2.add(publicationName);
	results2.newCell("align=\"left\"");

	Table isbns = new Table(1, attributes);

	for (String isbn : publicationsIsbns.get(publicationName)) {
	  isbns.newRow();
	  isbns.newCell("align=\"left\"");
	  isbns.add(isbn);
	}

	results2.add(isbns);
      }
    } else {
      // No.
      results2.newRow();
      results2.newCell();
      results2.add("");
      results2.newRow();
      results2.newCell("colspan=\"2\" align=\"center\"");
      results2.add("No periodicals are linked to ISBNs");
    }

    makeTablePage(LIST_MISMATCHED_ISBSNS_PUBLICATION_TYPES_HEADER, results1,
	results2);
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Displays the Archival Units in the database with an unknown provider.
   * 
   * @throws DbException
   *           if any problem occurred accessing the database.
   * @throws IOException
   */
  private void listUnknownProviderAus() throws DbException, IOException {
    final String DEBUG_HEADER = "listUnknownProviderAus(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    // The Archival Units that have an unknown provider.
    List<String> ausToList = new ArrayList<String>();

    // Get the identifiers of the Archival Units with an unknown provider.
    Collection<String> unknownProviderAuIds =
	mdManager.getUnknownProviderAuIds();
    if (log.isDebug3()) log.debug3(DEBUG_HEADER
	+ "unknownProviderAuIds.size() = " + unknownProviderAuIds.size());

    // Loop through the Archival Units.
    for (String auId : unknownProviderAuIds) {
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auId = " + auId);

      ArchivalUnit au = pluginManager.getAuFromId(auId);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "au = " + au);

      if (au != null) {
	ausToList.add(au.getName());
      } else {
	ausToList.add(auId);
      }
    }

    Collections.sort(ausToList);

    String attributes = "align=\"center\" cellspacing=\"4\" cellpadding=\"5\"";

    // Create the results table.
    Table results = new Table(0, attributes);
    results.newRow();
    results.newCell("align=\"center\" class=\"colhead\"");
    results.add("Archival Unit Name");

    // Check whether there are results to display.
    if (ausToList.size() > 0) {
      // Yes: Loop through the Archival Units to be listed.
      for (String auName : ausToList) {
	results.newRow();
	results.newCell("align=\"center\"");
	results.add(auName);
      }
    } else {
      // No.
      results.newRow();
      results.newCell();
      results.add("");
      results.newRow();
      results.newCell("align=\"center\"");
      results.add("No Archival Units have an unknown provider");
    }

    makeTablePage(LIST_UNKNOWN_PROVIDER_AUS_HEADER, results);
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Displays the children whose parents are of the wrong type.
   * 
   * @throws DbException
   *           if any problem occurred accessing the database.
   * @throws IOException
   */
  private void listMismatchedParentToChildren()
      throws DbException, IOException {
    final String DEBUG_HEADER = "listMismatchedParentToChildren(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    String attributes = "align=\"center\" cellspacing=\"4\" cellpadding=\"5\"";

    // Create the journal articles table.
    Table results1 = new Table(0, attributes);
    results1.newRow();
    results1.newCell("align=\"center\" class=\"colhead\"");
    results1.add("Journal Article Title");
    results1.newCell("align=\"center\" class=\"colhead\"");
    results1.add("Parent Title");
    results1.newCell("align=\"center\" class=\"colhead\"");
    results1.add("Parent Type");
    results1.newCell("align=\"center\" class=\"colhead\"");
    results1.add("Archival Unit Name");

    // Get the journal articles not linked to journals.
    Collection<Map<String, String>> mismatchedArticles =
	mdManager.getMismatchedParentJournalArticles();
    if (log.isDebug3()) log.debug3(DEBUG_HEADER
	+ "mismatchedArticles.size() = " + mismatchedArticles.size());

    // Check whether there are results to display.
    if (mismatchedArticles.size() > 0) {
      // Yes: Loop through the articles.
      for (Map<String, String> articleData : mismatchedArticles) {
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "articleData = " + articleData);

	results1.newRow();
	results1.newCell("align=\"center\"");
	results1.add(articleData.get("col1"));
	results1.newCell("align=\"center\"");
	results1.add(articleData.get("col2"));
	results1.newCell("align=\"center\"");
	results1.add(articleData.get("col3"));
	results1.newCell("align=\"center\"");

	String auId = PluginManager.generateAuId(articleData.get("col5"),
	    articleData.get("col4"));
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auId = " + auId);

	ArchivalUnit au = pluginManager.getAuFromId(auId);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "au = " + au);

	if (au != null) {
	  results1.add(au.getName());
	} else {
	  results1.add(auId);
	}
      }
    } else {
      // No.
      results1.newRow();
      results1.newCell();
      results1.add("");
      results1.newRow();
      results1.newCell("colspan=\"4\" align=\"center\"");
      results1.add("No journal articles have a mismatched parent");
    }

    // Create the book chapters table.
    Table results2 = new Table(0, attributes);
    results2.newRow();
    results2.newCell("align=\"center\" class=\"colhead\"");
    results2.add("Book Chapter Title");
    results2.newCell("align=\"center\" class=\"colhead\"");
    results2.add("Parent Title");
    results2.newCell("align=\"center\" class=\"colhead\"");
    results2.add("Parent Type");
    results2.newCell("align=\"center\" class=\"colhead\"");
    results2.add("Archival Unit Name");

    // Get the book chapters not linked to books or book series.
    Collection<Map<String, String>> mismatchedChapters =
	mdManager.getMismatchedParentBookChapters();
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "mismatchedChapters.size() = "
	+ mismatchedChapters.size());

    // Check whether there are results to display.
    if (mismatchedChapters.size() > 0) {
      // Yes: Loop through the chapters.
      for (Map<String, String> chapterData : mismatchedChapters) {
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "chapterData = " + chapterData);

	results2.newRow();
	results2.newCell("align=\"center\"");
	results2.add(chapterData.get("col1"));
	results2.newCell("align=\"center\"");
	results2.add(chapterData.get("col2"));
	results2.newCell("align=\"center\"");
	results2.add(chapterData.get("col3"));
	results2.newCell("align=\"center\"");

	String auId = PluginManager.generateAuId(chapterData.get("col5"),
	    chapterData.get("col4"));
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auId = " + auId);

	ArchivalUnit au = pluginManager.getAuFromId(auId);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "au = " + au);

	if (au != null) {
	  results2.add(au.getName());
	} else {
	  results2.add(auId);
	}
      }
    } else {
      // No.
      results2.newRow();
      results2.newCell();
      results2.add("");
      results2.newRow();
      results2.newCell("colspan=\"4\" align=\"center\"");
      results2.add("No book chapters have a mismatched parent");
    }

    // Create the book volumes table.
    Table results3 = new Table(0, attributes);
    results3.newRow();
    results3.newCell("align=\"center\" class=\"colhead\"");
    results3.add("Book Volume Title");
    results3.newCell("align=\"center\" class=\"colhead\"");
    results3.add("Parent Title");
    results3.newCell("align=\"center\" class=\"colhead\"");
    results3.add("Parent Type");
    results3.newCell("align=\"center\" class=\"colhead\"");
    results3.add("Archival Unit Name");

    // Get the book volumes not linked to books or book series.
    Collection<Map<String, String>> mismatchedVolumes =
	mdManager.getMismatchedParentBookVolumes();
    if (log.isDebug3()) log.debug3(DEBUG_HEADER	+ "mismatchedVolumes.size() = "
	+ mismatchedVolumes.size());

    // Check whether there are results to display.
    if (mismatchedVolumes.size() > 0) {
      // Yes: Loop through the volumes.
      for (Map<String, String> volumeData : mismatchedVolumes) {
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "volumeData = " + volumeData);

	results3.newRow();
	results3.newCell("align=\"center\"");
	results3.add(volumeData.get("col1"));
	results3.newCell("align=\"center\"");
	results3.add(volumeData.get("col2"));
	results3.newCell("align=\"center\"");
	results3.add(volumeData.get("col3"));
	results3.newCell("align=\"center\"");

	String auId = PluginManager.generateAuId(volumeData.get("col5"),
	    volumeData.get("col4"));
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auId = " + auId);

	ArchivalUnit au = pluginManager.getAuFromId(auId);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "au = " + au);

	if (au != null) {
	  results3.add(au.getName());
	} else {
	  results3.add(auId);
	}
      }
    } else {
      // No.
      results3.newRow();
      results3.newCell();
      results3.add("");
      results3.newRow();
      results3.newCell("colspan=\"4\" align=\"center\"");
      results3.add("No book volumes have a mismatched parent");
    }

    makeTablePage(LIST_MISMATCHED_PARENT_TO_CHILDREN_HEADER, results1, results2,
	results3);
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Displays the publisher for the Archival Units in the database with multiple
   * publishers.
   * 
   * @throws DbException
   *           if any problem occurred accessing the database.
   * @throws IOException
   */
  private void listMultiplePublisherAus() throws DbException, IOException {
    final String DEBUG_HEADER = "listMultiplePublisherAus(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    String attributes = "align=\"left\" cellspacing=\"4\" cellpadding=\"5\"";

    // Create the results table.
    Table results = new Table(0, attributes);
    results.newRow();
    results.newCell("align=\"right\" class=\"colhead\"");
    results.add("Archival Unit Name");
    results.newCell("align=\"left\" class=\"colhead\"");
    results.add("Publishers");

    // The Archival Units that have multiple publishers, sorted by name.
    Map<String, Collection<String>> ausToList =
	mdManager.getAuNamesWithMultiplePublishers();
    if (log.isDebug3()) log.debug3(DEBUG_HEADER
	+ "ausToList.size() = " + ausToList.size());

    // Check whether there are results to display.
    if (ausToList.size() > 0) {
      // Yes: Loop through the Archival Units to be listed.
      for (String auName : ausToList.keySet()) {
	results.newRow();
	results.newCell("align=\"right\"");
	results.add(auName);
	results.newCell("align=\"left\"");
      
	Table publishers = new Table(1, attributes);

	for (String publisher : ausToList.get(auName)) {
	  publishers.newRow();
	  publishers.newCell("align=\"left\"");
	  publishers.add(publisher);
	}

	results.add(publishers);
      }
    } else {
      // No.
      results.newRow();
      results.newCell();
      results.add("");
      results.newRow();
      results.newCell("colspan=\"2\" align=\"center\"");
      results.add("No Archival Units are linked to more than one publisher");
    }

    makeTablePage(LIST_MULTIPLE_PUBLISHER_AUS_HEADER, results);
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Displays the metadata items that have no name.
   * 
   * @throws DbException
   *           if any problem occurred accessing the database.
   * @throws IOException
   */
  private void listMetadataItemsWithoutName() throws DbException, IOException {
    final String DEBUG_HEADER = "listMetadataItemsWithoutName(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    String attributes = "align=\"left\" cellspacing=\"4\" cellpadding=\"5\"";

    // Create the results table.
    Table results = new Table(0, attributes);
    results.newRow();
    results.newCell("align=\"center\" class=\"colhead\"");
    results.add("Item Count");
    results.newCell("align=\"center\" class=\"colhead\"");
    results.add("Item Type");
    results.newCell("align=\"center\" class=\"colhead\"");
    results.add("Publisher");
    results.newCell("align=\"center\" class=\"colhead\"");
    results.add("Parent Title");
    results.newCell("align=\"center\" class=\"colhead\"");
    results.add("Parent Type");
    results.newCell("align=\"center\" class=\"colhead\"");
    results.add("Archival Unit Name");

    // The metadata items that have no name, sorted by publisher, parent type,
    // parent title and item type.
    Collection<Map<String, String>> unnamedItems = mdManager.getUnnamedItems();
    if (log.isDebug3()) log.debug3(DEBUG_HEADER
	+ "unnamedItems.size() = " + unnamedItems.size());

    // Check whether there are results to display.
    if (unnamedItems.size() > 0) {
      // Yes: Loop through the items.
      for (Map<String, String> itemData : unnamedItems) {
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "itemData = " + itemData);

	results.newRow();
	results.newCell("align=\"center\"");
	results.add(itemData.get("col1"));
	results.newCell("align=\"center\"");
	results.add(itemData.get("col2"));
	results.newCell("align=\"center\"");
	results.add(itemData.get("col7"));
	results.newCell("align=\"center\"");
	results.add(itemData.get("col3"));
	results.newCell("align=\"center\"");
	results.add(itemData.get("col4"));
	results.newCell("align=\"center\"");

	String auId = PluginManager.generateAuId(itemData.get("col6"),
	    itemData.get("col5"));
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auId = " + auId);

	ArchivalUnit au = pluginManager.getAuFromId(auId);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "au = " + au);

	if (au != null) {
	  results.add(au.getName());
	} else {
	  results.add(auId);
	}

      }
    } else {
      // No.
      results.newRow();
      results.newCell();
      results.add("");
      results.newRow();
      results.newCell("colspan=\"6\" align=\"center\"");
      results.add("No metadata items without name");
    }

    makeTablePage(LIST_ITEMS_WITHOUT_NAME_HEADER, results);
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }
}
