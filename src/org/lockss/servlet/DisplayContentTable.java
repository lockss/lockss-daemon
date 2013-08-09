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
import java.util.*;
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
  private Page page;
  private Block tabsDiv;
  private TreeMap<Character, Character> startLetterList =
          new TreeMap<Character, Character>();
  private Integer numberInGroup;
  private String grouping;
  private String type;

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
    addCss();
    createSortLink();
    createTypeLinks();
    createTabsDiv();
    populateLetterList();
    createTabList();
//    createTabs();
    addJQueryJS();
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
   */
  private void addCss() {
    StyleLink jqueryLink = new StyleLink("/css/jquery-ui-1.8.css");
    page.addHeader(jqueryLink);
    StyleLink jqueryTooltipLink = new StyleLink("/css/jquery.ui.tooltip.css");
    page.addHeader(jqueryTooltipLink);
    StyleLink cssLink = new StyleLink("/css/lockss.css");
    page.addHeader(cssLink);
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
    StringBuilder booksLinkString = new StringBuilder("DisplayContentStatus?type=books");
    StringBuilder journalsLinkString = new StringBuilder("DisplayContentStatus?type=journals");
    StringBuilder allLinkString = new StringBuilder("DisplayContentStatus");
    if ("plugin".equals(grouping)) {
      booksLinkString.append("&group=plugin");
      journalsLinkString.append("&group=plugin");
      allLinkString.append("?group=plugin");
    }
    Link booksLink = new Link(booksLinkString.toString());
    booksLink.add("books");
    typeDiv.add(booksLink);
    typeDiv.add(" | ");
    Link journalsLink = new Link(journalsLinkString.toString());
    journalsLink.add("journals");
    typeDiv.add(journalsLink);
    typeDiv.add(" | ");
    Link allLink = new Link(allLinkString.toString());
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
        Link tabLink = new Link("DisplayContentTab?start=" + startLetter + "&end=" + endLetter + "&group="
                + grouping + "&type=" + type, startLetter + " - " + endLetter);
        Composite tabListItem = tabList.newItem();
        tabListItem.add(tabLink);
    }
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
   * Adds javascript to the page based on the URL provided
   *
   * @param jsLocation URL of javascript file
   */
  private void addJS(String jsLocation) {
    Script ajaxScript = new Script("");
    ajaxScript.attribute("src", jsLocation);
    ajaxScript.attribute("type", "text/javascript");
    page.addHeader(ajaxScript);
  }
}
