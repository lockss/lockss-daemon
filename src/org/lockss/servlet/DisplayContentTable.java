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
import java.util.List;

import org.lockss.config.TdbUtil;
import org.lockss.daemon.Crawler;
import org.lockss.plugin.*;
import org.lockss.state.AuState;
import org.lockss.util.Logger;
import org.lockss.util.PluginComparator;
import org.lockss.util.StringUtil;
import org.mortbay.html.*;

/**
 * Display content utility class
 */
public class DisplayContentTable {
  protected static Logger log = Logger.getLogger("DisplayContentTable");

  private static final int LETTERS_IN_ALPHABET = 26;
  private static final int DEFAULT_NUMBER_IN_GROUP = 4;
  private static final String DEFAULT_GROUPING = "publisher";
  private static final String LOADING_SPINNER = "images/ajax-loader.gif";
  private Page page;
  private Block tabsDiv;
  private TreeMap<Character, Character> startLetterList =
          new TreeMap<Character, Character>();
  private Integer numberInGroup;
  private String grouping;
  private String type;
  private String filterKey;
  private String timeKey;

  /**
   * Constructor method
   *
   * @param page HTML page object
   * @param numberInGroup Number of letters grouped in each tab
   * @param grouping Method used to group the AUs, either by publisher or plugin
   * @throws UnsupportedEncodingException
   */
  public DisplayContentTable(Page page, Integer numberInGroup, String grouping,
          String type, String filterKey, String timeKey) throws UnsupportedEncodingException {
    this.page = page;
    this.numberInGroup = numberInGroup;
    this.grouping = grouping;
    this.type = type;
    this.filterKey = filterKey;
    this.timeKey = timeKey;
    addCss();
    createSortLink();
    createTypeLinks();
    createTabsDiv();
    populateLetterList();
    createTabList();
    addJQueryJS();
  }

  public DisplayContentTable(Page page, Integer numberInGroup)
          throws UnsupportedEncodingException {
    this(page, numberInGroup, DEFAULT_GROUPING, null, null, null);
  }

  public DisplayContentTable(Page page, String grouping)
          throws UnsupportedEncodingException {
    this(page, DEFAULT_NUMBER_IN_GROUP, grouping, null, null, null);
  }

  public DisplayContentTable(Page page, String grouping, String type)
          throws UnsupportedEncodingException {
    this(page, DEFAULT_NUMBER_IN_GROUP, grouping, type, null, null);
  }

  public DisplayContentTable(Page page, String grouping, String type, String filter)
          throws UnsupportedEncodingException {
    this(page, DEFAULT_NUMBER_IN_GROUP, grouping, type, filter, null);
  }

    public DisplayContentTable(Page page, String grouping, String type, String filter, String timeKey)
            throws UnsupportedEncodingException {
        this(page, DEFAULT_NUMBER_IN_GROUP, grouping, type, filter, timeKey);
    }

  public DisplayContentTable(Page page) throws UnsupportedEncodingException {
    this(page, DEFAULT_NUMBER_IN_GROUP, DEFAULT_GROUPING, null, null, null);
  }

  /**
   * Rearranges AUs according to the plugin
   *
   * @param allAus Collection of AUs
   * @return TreeMap of AUs in the desired format
   */
  public static TreeMap<Plugin, TreeSet<ArchivalUnit>> orderAusByPlugin(Collection<ArchivalUnit> allAus) {
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

    public static TreeMap<String, TreeMap<String, TreeSet<ArchivalUnit>>>
    orderAusByPluginAndTitle(Collection<ArchivalUnit> allAus, String filter) {
        TreeMap<String, TreeMap<String, TreeSet<ArchivalUnit>>> ausMap = new TreeMap<String, TreeMap<String, TreeSet<ArchivalUnit>>>();
        for (ArchivalUnit au : allAus) {
            String plugin = au.getPlugin().getPluginName();
            TreeMap<String, TreeSet<ArchivalUnit>> pluginMap = ausMap.get(plugin);
            String title = au.getTdbAu().getJournalTitle();
            if (ausMap.containsKey(plugin)) {
                TreeSet<ArchivalUnit> titleSet;
                if (pluginMap != null) {
                    if (pluginMap.containsKey(title)) {
                        titleSet = pluginMap.get(title);
                        if (filterAu(au, filter)) {
                            titleSet.add(au);
                        }
                    } else {
                        titleSet = new TreeSet<ArchivalUnit>(new AuOrderComparator());
                        if (filterAu(au, filter)) {
                            titleSet.add(au);
                        }
                    }
                    pluginMap.put(title, titleSet);
                }
            } else {
                pluginMap = new TreeMap<String, TreeSet<ArchivalUnit>>();
                TreeSet<ArchivalUnit> titleSet = new TreeSet<ArchivalUnit>(new AuOrderComparator());
                if (filterAu(au, filter)) {
                    titleSet.add(au);
                }
                pluginMap.put(title, titleSet);
            }
            ausMap.put(plugin, pluginMap);
        }
        return ausMap;
    }

  /**
   * Rearranges AUs according to the publisher
   *
   * @param allAus Collection of AUs
   * @return TreeMap of AUs in the desired format
   */
  public static TreeMap<String, TreeSet<ArchivalUnit>> orderAusByPublisher(Collection<ArchivalUnit> allAus) {
    Iterator<ArchivalUnit> itr = allAus.iterator();
    TreeMap<String, TreeSet<ArchivalUnit>> ausMap =
            new TreeMap<String, TreeSet<ArchivalUnit>>();
    while (itr.hasNext()) {
      ArchivalUnit au = itr.next();
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

    public static TreeMap<String, TreeMap<String, TreeSet<ArchivalUnit>>>
    orderAusByPublisherAndTitle(Collection<ArchivalUnit> allAus, String filter) {
        TreeMap<String, TreeMap<String, TreeSet<ArchivalUnit>>> ausMap = new TreeMap<String, TreeMap<String, TreeSet<ArchivalUnit>>>();
        for (ArchivalUnit au : allAus) {
            String publisher = AuUtil.getTitleAttribute(au, "publisher");
            if (publisher == null) {
                publisher = "unknown publisher";
            }
            String title = au.getTdbAu().getJournalTitle();
            TreeMap<String, TreeSet<ArchivalUnit>> publisherMap;
            if (ausMap.containsKey(publisher)) {
                publisherMap = (TreeMap) ausMap.get(publisher);
                TreeSet<ArchivalUnit> titleSet;
                if (publisherMap != null) {
                    if (publisherMap.containsKey(title)) {
                    titleSet = publisherMap.get(title);
                    if (filterAu(au, filter)) {
                        titleSet.add(au);
                    }
                } else {
                    titleSet = new TreeSet<ArchivalUnit>(new AuOrderComparator());
                    if (filterAu(au, filter)) {
                        titleSet.add(au);
                    }
                }
                publisherMap.put(title, titleSet);
                }
            } else {
                publisherMap = new TreeMap<String, TreeSet<ArchivalUnit>>();
                TreeSet<ArchivalUnit> titleSet = new TreeSet<ArchivalUnit>(new AuOrderComparator());
                if (filterAu(au, filter)) {
                    titleSet.add(au);
                }
                publisherMap.put(title, titleSet);
            }
            ausMap.put(publisher, publisherMap);
        }
        return ausMap;
    }

    private static Boolean filterAu(ArchivalUnit au, String filter) {
        if (StringUtil.isNullString(filter)) {
            return true;
        } else {
            AuState auState = AuUtil.getAuState(au);
            if ("neverCrawled".equals(filter)) {
                return auState.getLastCrawlResult() == Crawler.STATUS_QUEUED || auState.getLastCrawlResult() == -1;
            } else if ("noSubstance".equals(filter)) {
                return auState.hasNoSubstance();
            } else if ("noPermission".equals(filter)) {
                return auState.getLastCrawlResult() == Crawler.STATUS_NO_PUB_PERMISSION;
            } else if ("serverDown".equals(filter)) {
                return auState.getLastCrawlResult() == Crawler.STATUS_FETCH_ERROR;
            } else {
                return true;
            }
        }
    }

  /**
   * Adds required CSS to the page header
   */
  private void addCss() {
    StyleLink jqueryLink = new StyleLink("http://code.jquery.com/ui/1.10.3/themes/smoothness/jquery-ui.css");
    page.addHeader(jqueryLink);
    StyleLink jqueryTooltipLink = new StyleLink("/css/jquery.ui.tooltip.css");
    page.addHeader(jqueryTooltipLink);
    StyleLink cssLink = new StyleLink("/css/lockss-new.css");
    page.addHeader(cssLink);
  }

  /**
   * Adds JQuery Javascript to the header of the page object
   */
  private void addJQueryJS() {
    addJS("http://code.jquery.com/jquery-1.9.1.js");
    addJS("http://code.jquery.com/ui/1.10.3/jquery-ui.js");
    addJS("js/auDetails-new.js");
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
      String timeString = "";
    if ("accurate".equals(timeKey)) {
        timeString = "&amp;timeKey=" + timeKey;
    }

    String linkHref = "/DisplayContentStatus?group=" + newGrouping + timeString;
    String linkText = "Order by " + newGrouping;
    Link sortLink = new Link(linkHref);
    sortLink.attribute("class", "filters");
    sortLink.add(linkText);
    page.add(sortLink);
  }

  private void createTypeLinks() {
    Block typeDiv = new Block(Block.Div);
    typeDiv.attribute("class", "typeDiv");
    typeDiv.add("Filter results by ");
      List<String> viewList = Arrays.asList("books", "journals", "Show all");
      for (String view : viewList) {
          Link filterLink;
          if ("accurate".equals(timeKey)) {
            filterLink = new Link("DisplayContentStatus?type=" + view.replaceAll(" ", "").toLowerCase()
                    + filterByGroup() + "&amp;timeKey=accurate");
          } else {
            filterLink = new Link("DisplayContentStatus?type=" + view.replaceAll(" ", "").toLowerCase()
                    + filterByGroup());
          }
          filterLink.attribute("class", "filters");
          filterLink.add(view);
          typeDiv.add(filterLink);
          typeDiv.add(" | ");
      }
      Block filterDiv = new Block(Block.Div);
      Form filterForm = new Form();
      filterForm.method("GET");
      filterForm.add("Filter by crawl state ");
      filterForm.attribute("class", "typeDiv");
      Select filterSelect = new Select("filterKey", false);
      filterSelect.add("No filter", StringUtil.isNullString(filterKey), "");
      filterSelect.add("Never crawled", "neverCrawled".equals(filterKey), "neverCrawled");
      filterSelect.add("No permission", "noPermission".equals(filterKey), "noPermission");
      filterSelect.add("No substance", "noSubstance".equals(filterKey), "noSubstance");
      filterSelect.add("Server down", "serverDown".equals(filterKey), "serverDown");
      filterForm.add(filterSelect);
      Input tab = new Input("hidden", "tab", "0");
      Input time = new Input("hidden", "timeKey", timeKey);
      tab.attribute("id", "filter-tab");
      filterForm.add(tab);
      filterForm.add(time);
      Input filterSubmit = new Input("submit", "submit");
      filterForm.add(filterSubmit);
      filterDiv.add(filterForm);
      // Not sure how to best display this at the moment
//      Block timeDiv = new Block(Block.Div);
//      String accuracy = "accurate";
//      String timeLabel = "Accurate";
//      if ("accurate".equals(timeKey)) {
//          accuracy = "";
//          timeLabel = "Friendly";
//      }
//      Link timeLink = new Link("DisplayContentStatus?timeKey=" + accuracy);
//      timeLink.attribute("class", "filters");
//      timeLink.add(timeLabel);
//      timeDiv.add("Time: ");
//      timeDiv.add(timeLink);
      page.add(typeDiv);
      page.add(filterDiv);
//      page.add(timeDiv);
      page.add(new Break(Break.Line));
  }

    private String filterByGroup() {
        if ("plugin".equals(grouping)) {
            return "&amp;group=plugin";
        } else {
            return "";
        }
    }

  /**
   * Adds the div required by jQuery tabs
   */
  private void createTabsDiv() {
    tabsDiv = new Block(Block.Div, "id='tabs'");
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
    Integer tabCount = 1;
    for (Map.Entry letterPairs : startLetterList.entrySet()) {
      Character startLetter = (Character) letterPairs.getKey();
      Character endLetter = (Character) letterPairs.getValue();
        Link tabLink = new Link("DisplayContentTab?start=" + startLetter + "&amp;end=" + endLetter + "&amp;group="
                + grouping + "&amp;type=" + type + "&amp;filter=" + filterKey +
                "&amp;timeKey=" + timeKey, startLetter + " - " + endLetter);
        Composite tabListItem = tabList.newItem();
        tabListItem.add(tabLink);
        Block loadingDiv = new Block(Block.Div, "id='ui-tabs-" + tabCount++ + "'");
        Image loadingImage = new Image(LOADING_SPINNER);
        loadingImage.alt("Loading...");
        loadingDiv.add(loadingImage);
        loadingDiv.add(" Loading...");
        tabsDiv.add(loadingDiv);
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
            .replace(")", "").replaceAll(",", "");
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
