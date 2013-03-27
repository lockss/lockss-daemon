/*
 * $Id DisplayContentTable.java 2012/12/03 14:52:00 rwincewicz $
 */

/*

 Copyright (c) 2012 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;
import java.net.URLEncoder;
import java.util.*;
import org.lockss.config.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;
import org.lockss.util.PluginComparator;
import org.mortbay.html.*;

/**
 * Display content utility class
 */
public class DisplayContentTable {
  protected static Logger log = Logger.getLogger("DisplayContentTable");

  private static final int LETTERS_IN_ALPHABET = 26;
  private static final int DEFAULT_NUMBER_IN_GROUP = 4;
  private static final String DEFAULT_GROUPING = "publisher";
  private static final String OK_ICON = "images/button_ok.png";
  private static final String CANCEL_ICON = "images/button_cancel.png";
  private Page page;
  private Block tabsDiv;
  private TreeMap<Character, Character> startLetterList =
          new TreeMap<Character, Character>();
  private Integer numberInGroup;
  private String grouping;
  private String type;
  private ArrayList<String> columnArrayList;
  private ArrayList<ArchivalUnit> publicationHealth;

  /**
   * Constructor method
   *
   * @param page HTML page object
   * @param numberInGroup Number of letters grouped in each tab
   * @param grouping Method used to group the AUs, either by publisher or plugin
   * @throws UnsupportedEncodingException
   */
  public DisplayContentTable(Page page, Integer numberInGroup, String grouping,
          String type) throws UnsupportedEncodingException {
    this.page = page;
    this.numberInGroup = numberInGroup;
    this.grouping = grouping;
    this.type = type;
    columnArrayList = new ArrayList();
    publicationHealth = new ArrayList<ArchivalUnit>();
    createColumn("Title");
    createColumn("Year");
    createColumn("ISSN");
    createColumn("Collected");
    addCss();
    addJQueryJS();
    createSortLink();
    createTypeLinks();
    createTabsDiv();
    populateLetterList();
    createTabList();
    createTabs();
  }

  public DisplayContentTable(Page page, Integer numberInGroup)
          throws UnsupportedEncodingException {
    this(page, numberInGroup, DEFAULT_GROUPING, null);
  }

  public DisplayContentTable(Page page, String grouping)
          throws UnsupportedEncodingException {
    this(page, DEFAULT_NUMBER_IN_GROUP, grouping, null);
  }

  public DisplayContentTable(Page page, String grouping, String type)
          throws UnsupportedEncodingException {
    this(page, DEFAULT_NUMBER_IN_GROUP, grouping, type);
  }

  public DisplayContentTable(Page page) throws UnsupportedEncodingException {
    this(page, DEFAULT_NUMBER_IN_GROUP, DEFAULT_GROUPING, null);
  }

  /**
   * Rearranges AUs according to the plugin
   *
   * @param allAus Collection of AUs
   * @return TreeMap of AUs in the desired format
   */
  public static TreeMap<Plugin, TreeSet<ArchivalUnit>> orderAusByPlugin(Collection allAus) {
    Iterator itr = allAus.iterator();
    TreeMap<Plugin, TreeSet<ArchivalUnit>> ausMap =
            new TreeMap<Plugin, TreeSet<ArchivalUnit>>(new PluginComparator());
    while (itr.hasNext()) {
      ArchivalUnit au = (ArchivalUnit) itr.next();
      Plugin plugin = au.getPlugin();
      if (ausMap.containsKey(plugin)) {
        TreeSet<ArchivalUnit> auSet = (TreeSet) ausMap.get(plugin);
        auSet.add(au);
      } else {
        TreeSet<ArchivalUnit> auSet =
                new TreeSet<ArchivalUnit>(new AuOrderComparator());
        auSet.add(au);
        ausMap.put(plugin, auSet);
      }
    }
    return ausMap;
  }

  /**
   * Rearranges AUs according to the publisher
   *
   * @param allAus Collection of AUs
   * @return TreeMap of AUs in the desired format
   */
  public static TreeMap<String, TreeSet<ArchivalUnit>> orderAusByPublisher(Collection allAus) {
    Iterator itr = allAus.iterator();
    TreeMap<String, TreeSet<ArchivalUnit>> ausMap =
            new TreeMap<String, TreeSet<ArchivalUnit>>();
    while (itr.hasNext()) {
      ArchivalUnit au = (ArchivalUnit) itr.next();
      String publisher = AuUtil.getTitleAttribute(au, "publisher");
      if (publisher == null) {
        publisher = "unknown publisher";
      }
      if (ausMap.containsKey(publisher)) {
        TreeSet<ArchivalUnit> auSet = (TreeSet) ausMap.get(publisher);
        auSet.add(au);
      } else {
        TreeSet<ArchivalUnit> auSet =
                new TreeSet<ArchivalUnit>(new AuOrderComparator());
        auSet.add(au);
        ausMap.put(publisher, auSet);
      }
    }
    return ausMap;
  }

  /**
   * Adds required CSS to the page header
   *
   * @param page Page object
   */
  private void addCss() {
    StyleLink cssLink = new StyleLink("/css/lockss.css");
    StyleLink jqueryLink = new StyleLink("/css/jquery-ui-1.8.css");
    page.add(cssLink);
    page.add(jqueryLink);
  }

  /**
   * Adds JQuery Javascript to the header of the page object
   */
  private void addJQueryJS() {
    addJS("js/jquery.min-1.5.js");
    addJS("js/jquery-ui.min-1.8.js");
    addJS("js/auDetails.js");
  }

  /**
   * Adds a link to the top of the table to allow switching between a publisher
   * or plugin-sorted list
   */
  private void createSortLink() {

    String newGrouping;

    if ("plugin".equals(grouping)) {
      newGrouping = "publisher";
    } else {
      newGrouping = "plugin";
    }

    String linkHref = "/DisplayContentStatus?group=" + newGrouping;
    String linkText = "Order by " + newGrouping;
    Link sortLink = new Link(linkHref);
    sortLink.add(linkText);
    page.add(sortLink);
  }

  private void createTypeLinks() {
    Block typeDiv = new Block(Block.Div);
    typeDiv.attribute("class", "typeDiv");
    typeDiv.add("Filter results by ");
    String booksLinkString = "DisplayContentStatus?type=books";
    String journalsLinkString = "DisplayContentStatus?type=journals";
    String allLinkString = "DisplayContentStatus";
    if ("plugin".equals(grouping)) {
      booksLinkString += "&group=plugin";
      journalsLinkString += "&group=plugin";
      allLinkString += "&group=plugin";
    }
    Link booksLink = new Link(booksLinkString);
    booksLink.add("books");
    typeDiv.add(booksLink);
    typeDiv.add(" | ");
    Link journalsLink = new Link(journalsLinkString);
    journalsLink.add("journals");
    typeDiv.add(journalsLink);
    typeDiv.add(" | ");
    Link allLink = new Link(allLinkString);
    allLink.add("Show all");
    typeDiv.add(allLink);
    page.add(typeDiv);
    page.add(new Break(Break.Line));
  }

  /**
   * Adds the div required by jQuery tabs
   */
  private void createTabsDiv() {
    tabsDiv = new Block(Block.Div, "id=\"tabs\"");
    page.add(tabsDiv);
  }

  /**
   * Populates the treemap with start and end letters based on how many letters
   * should be present in each group
   */
  private void populateLetterList() {
    int numberOfTabs = LETTERS_IN_ALPHABET / numberInGroup;

    if (LETTERS_IN_ALPHABET % numberInGroup != 0) {
      numberOfTabs++;
    }
    for (int i = 0; i < numberOfTabs; i++) {
      Character startLetter = (char) ((i * numberInGroup) + 65);
      Character endLetter = (char) (startLetter + numberInGroup - 1);
      if ((int) endLetter > (25 + 65)) {
        endLetter = (char) (25 + 65);
      }
      startLetterList.put(startLetter, endLetter);
    }
  }

  /**
   * Creates the spans required for jQuery tabs to build the desired tabs
   *
   * @throws UnsupportedEncodingException
   */
  private void createTabList() throws UnsupportedEncodingException {

    org.mortbay.html.List tabList =
            new org.mortbay.html.List(org.mortbay.html.List.Unordered);
    tabsDiv.add(tabList);
    for (Map.Entry letterPairs : startLetterList.entrySet()) {
      Character startLetter = (Character) letterPairs.getKey();
      Character endLetter = (Character) letterPairs.getValue();
      if (numberInGroup > 1) {
        Link tabLink = new Link("#" + startLetter);
        Block tabSpan = new Block(Block.Span);
        tabSpan.add(startLetter + " - " + endLetter);
        tabLink.add(tabSpan);
        Composite tabListItem = tabList.newItem();
        tabListItem.add(tabLink);
      } else {
        Link tabLink = new Link("#" + startLetter);
        Block tabSpan = new Block(Block.Span);
        tabSpan.add(startLetter);
        tabLink.add(tabSpan);
        Composite tabListItem = tabList.newItem();
        tabListItem.add(tabLink);
      }
    }
  }

  private void createColumn(String columnTitle) {
    columnArrayList.add(columnTitle);
  }

  /**
   * Creates the tabs for the content display and populates them with the
   * required data
   *
   * @throws UnsupportedEncodingException
   */
  private void createTabs() throws UnsupportedEncodingException {

    TreeMap ausMap;
    String sortName;
    Collection<ArchivalUnit> allAus = TdbUtil.getConfiguredAus();

    if ("books".equals(type)) {
      allAus = TdbUtil.filterAusByType(allAus, TdbUtil.ContentType.BOOKS);
    } else if ("journals".equals(type)) {
      allAus = TdbUtil.filterAusByType(allAus, TdbUtil.ContentType.JOURNALS);
    }

    if ("plugin".equals(grouping)) {
      ausMap = orderAusByPlugin(allAus);
    } else {
      ausMap = orderAusByPublisher(allAus);
    }
    for (Map.Entry letterPairs : startLetterList.entrySet()) {
      Character startLetter = (Character) letterPairs.getKey();

      Table divTable = createTabDiv(startLetter.toString());

      Iterator mapIterator = ausMap.entrySet().iterator();

      while (mapIterator.hasNext()) {
        Map.Entry pairs = (Map.Entry) mapIterator.next();
        if ("plugin".equals(grouping)) {
          Plugin plugin = (Plugin) pairs.getKey();
          sortName = plugin.getPluginName();
        } else {
          sortName = pairs.getKey().toString();
        }

        String firstLetterPub = sortName.substring(0, 1);
        TreeSet<ArchivalUnit> auSet = (TreeSet) pairs.getValue();

        Boolean belongsInTab = false;

        for (int j = 0; j < numberInGroup; j++) {
          Character letterString = (char) (startLetter + j);
          if (letterString.toString().equalsIgnoreCase(firstLetterPub)) {
            belongsInTab = true;
          }
        }

        if (belongsInTab) {
          createTabContent(divTable, sortName, auSet);
        }
      }
    }
  }

  /**
   * Creates the div for each tab
   *
   * @param letter Start letter of the tab group
   * @return Returns string to add to the page
   */
  private Table createTabDiv(String letter) {
    Block tabDiv = new Block("div", "id=\"" + letter + "\"");
    Table divTable = new Table(0, "class=\"status-table\"");
    divTable.newRow();
    divTable.addCell("");
    Iterator<String> it = columnArrayList.listIterator();
    while (it.hasNext()) {
      Block columnHeader = new Block(Block.Bold);
      columnHeader.add(it.next());
      divTable.addCell(columnHeader, "class=\"column-header\"");
    }
    tabDiv.add(divTable);
    tabsDiv.add(tabDiv);
    return divTable;
  }

  /**
   * Creates the relevant content for each of the tabs
   *
   * @param divTable The table object to add to
   * @param sortName Name of the publisher or plugin
   * @param auSet TreeSet of archival units
   * @throws UnsupportedEncodingException
   */
  private static void createTabContent(Table divTable, String sortName,
          TreeSet<ArchivalUnit> auSet) throws UnsupportedEncodingException {
    String cleanNameString = cleanName(sortName);
    createTitleRow(divTable, sortName);
    if (auSet != null) {
      int rowCount = 0;
      for (ArchivalUnit au : auSet) {
        createAuRow(divTable, au, cleanNameString, rowCount);
        rowCount++;
      }
    }
  }

  /**
   * Builds the title row for each of the publishers or plugins
   *
   * @param divTable The table object to add to
   * @param sortName Name of the publisher or plugin
   */
  private static void createTitleRow(Table divTable, String sortName) {
    String cleanNameString = cleanName(sortName);
    divTable.newRow();
    Link headingLink = new Link("javascript:showRows('" + cleanNameString
            + "_class', '" + cleanNameString + "_id', '" + cleanNameString
            + "_AUimage')");
    headingLink.attribute("id=\"" + cleanNameString + "_id\"");
    Image headingLinkImage = new Image("images/expand.png");
    headingLinkImage.attribute("id =\"" + cleanNameString + "_AUimage\"");
    headingLinkImage.attribute("class=\"title-icon\"");
    headingLinkImage.attribute("alt", "Expand AU");
    headingLinkImage.attribute("title", "Expand AU");
    headingLink.add(headingLinkImage);
    headingLink.add(sortName);
    Block boldHeadingLink = new Block(Block.Bold);
    boldHeadingLink.add(headingLink);
    divTable.addHeading(boldHeadingLink, "class=\"pub-title\"");
  }

  /**
   * Creates table rows for each AU
   *
   * @param divTable The table object to add to
   * @param au Archival unit object for this row
   * @param cleanNameString Sanitised name used to create classes and divs
   * @param rowCount Row number, used to allocate CSS class
   * @throws UnsupportedEncodingException
   */
  private static void createAuRow(Table divTable, ArchivalUnit au,
          String cleanNameString, int rowCount)
          throws UnsupportedEncodingException {
    TdbAu tdbAu = TdbUtil.getTdbAu(au);
    String auName = au.getName();
    String cleanedAuName = auName.replaceAll(" ", "_");
    String encodedHref = URLEncoder.encode(au.getAuId(), "UTF-8");
    divTable.newRow();
    Block newRow = divTable.row();
    newRow.attribute("class", cleanNameString + "_class hide-row "
            + rowCss(rowCount));
    Block auDiv = new Block(Block.Div);
    auDiv.attribute("id", cleanedAuName);
    auDiv.attribute("class", "au-title");
    Link auLink = new Link("DaemonStatus");
    if (tdbAu != null) {
      auLink.attribute("onClick", "updateDiv('" + cleanedAuName + "', '"
              + encodedHref + "', '"
              + cleanNameString + tdbAu.getYear() + "_image');return false");
      Image auLinkImage = new Image("images/expand.png");
      auLinkImage.attribute("id", cleanNameString + tdbAu.getYear() + "_image");
      auLinkImage.attribute("class", "title-icon");
      auLinkImage.attribute("alt", "Expand Volume");
      auLinkImage.attribute("title", "Expand Volume");
      auLink.add(auLinkImage);
      auLink.add(auName);
      auDiv.add(auLink);
      divTable.addCell(auDiv);
      divTable.addCell(tdbAu.getJournalTitle());
      divTable.addCell(tdbAu.getYear());
      divTable.addCell(tdbAu.getIssn());
      divTable.addCell(checkCollected(au));
    } else {
      log.debug("TdbAu not found for Au " + au.getName());
      auLink.attribute("onClick", "updateDiv('" + cleanedAuName + "', '"
              + encodedHref + "', '"
              + cleanNameString + rowCount + "_image');return false");
      Image auLinkImage = new Image("images/expand.png");
      auLinkImage.attribute("id", cleanNameString + rowCount + "_image");
      auLinkImage.attribute("class", "title-icon");
      auLinkImage.attribute("alt", "Expand Volume");
      auLinkImage.attribute("title", "Expand Volume");
      auLink.add(auLinkImage);
      auLink.add(auName);
      auDiv.add(auLink);
      divTable.addCell(auDiv);
      divTable.addCell(au.getName());
      divTable.addCell("unknown");
      divTable.addCell("unknown");
      divTable.addCell(checkCollected(au));
    }
    Block serveContentDiv = new Block(Block.Div);
    Link serveContentLink = new Link("ServeContent?auid=" + encodedHref);
    serveContentLink.target("_blank");
    serveContentLink.add("Serve content");
    serveContentDiv.add(serveContentLink);
    divTable.addCell(serveContentDiv);
  }

  /**
   * Checks if the AU is collected and returns the relevant image
   *
   * @param au Archival unit
   * @return URL of relevant image
   */
  private static Image checkCollected(ArchivalUnit au) {
    Image collectedImage;

    if (TdbUtil.isAuPreserved(au)) {
      collectedImage = new Image(OK_ICON);
      collectedImage.attribute("title", "AU has substance");
      collectedImage.attribute("alt", "AU has substance");
    } else {
      collectedImage = new Image(CANCEL_ICON);
      collectedImage.attribute("title", "AU does not have substance");
      collectedImage.attribute("alt", "AU does not have substance");
    }
    return collectedImage;
  }

  /**
   * Sanitises a string so that it can be used as a div id
   *
   * @param name
   * @return Returns sanitised string
   */
  public static String cleanName(String name) {
    return name.replace(" ", "_").replace("&", "").replace("(", "")
            .replace(")", "");
  }

  /**
   * Provides a CSS class based on the row number
   *
   * @param rowCount Row number
   * @return Returns a CSS class for that row
   */
  public static String rowCss(Integer rowCount) {
    String css = (rowCount % 2 == 0) ? "even-row" : "odd-row";
    return css;
  }

  /**
   * Adds a JS button to remove an AU
   *
   * @param cleanedAuName
   * @return Returns HTML used to render the button
   */
  private static Input removeAuButton(String cleanedAuName) {
    Input button = new Input("button", "", "Remove AU");
    button.attribute("id=\"" + cleanedAuName + "\"");
    button.attribute("onClick=\"lockssButton(this, 'DoRemoveAus')\"");
    return button;
  }

  /**
   * Adds javascript to the page based on the URL provided
   *
   * @param jsLocation URL of javascript file
   */
  private void addJS(String jsLocation) {
    Script ajaxScript = new Script("");
    ajaxScript.attribute("src", jsLocation);
    ajaxScript.attribute("type", "text/javascript");
    page.add(ajaxScript);
  }
}