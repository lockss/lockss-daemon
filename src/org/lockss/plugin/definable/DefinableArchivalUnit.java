/*
 * $Id: DefinableArchivalUnit.java,v 1.12 2004-08-12 23:15:15 clairegriffin Exp $
 */

/*
 Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.plugin.definable;

import java.net.*;
import java.util.*;

import org.lockss.crawler.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.ArchivalUnit.*;
import org.lockss.plugin.base.*;
import org.lockss.util.*;

/**
 * <p>ConfigurableArchivalUnit: An implementatation of Base Archival Unit used
 * with the ConfigurablePlugin to allow a Map of values to be used to configure
 * and define the behaviour of a plugin.</p>
 * @author claire griffin
 * @version 1.0
 */
public class DefinableArchivalUnit extends BaseArchivalUnit {
  static final public String CM_AU_START_URL_KEY = "au_start_url";
  static final public String CM_AU_NAME_KEY = "au_name";
  static final public String CM_AU_RULES_KEY = "au_crawlrules";
  static final public String CM_AU_SHORT_YEAR_PREFIX = "au_short_";
  static final public String CM_AU_HOST_SUFFIX = "_host";
  static final public String CM_AU_PATH_SUFFIX = "_path";
  static final public String CM_AU_CRAWL_WINDOW_KEY = "au_crawlwindow";
  static final public String CM_AU_EXPECTED_PATH = "au_expected_base_path";
  static final public String CM_AU_CRAWL_DEPTH = "au_crawl_depth";
  static final public String CM_AU_DEFAULT_NC_CRAWL_KEY =
      "au_def_new_content_crawl";
  static final public String CM_AU_DEFAULT_PAUSE_TIME = "au_def_pause_time";
  static final public String CM_AU_MANIFEST_KEY = "au_manifest";
  static final public String CM_AU_MAX_SIZE_KEY = "au_maxsize";
  static final public String CM_AU_MAX_FILE_SIZE_KEY = "au_max_file_size";

  static final public String CM_AU_PARSER_SUFFIX = "_parser";
  static final public String CM_AU_FILTER_SUFFIX = "_filter";
  static final public int DEFAULT_AU_CRAWL_DEPTH = 1;

  protected ExternalizableMap definitionMap;
  protected ClassLoader classLoader;

  static Logger log = Logger.getLogger("ConfigurableArchivalUnit");


  protected DefinableArchivalUnit(Plugin myPlugin) {
    super(myPlugin);
    throw new UnsupportedOperationException(
        "ConfigurableArchvialUnit requires ConfigurablePlugin for construction");
  }


  protected DefinableArchivalUnit(DefinablePlugin myPlugin,
                                     ExternalizableMap definitionMap) {
    this(myPlugin, definitionMap, myPlugin.getClass().getClassLoader());
  }

  protected DefinableArchivalUnit(DefinablePlugin myPlugin,
				  ExternalizableMap definitionMap,
				  ClassLoader classLoader) {
    super(myPlugin);
    this.definitionMap = definitionMap;
    this.classLoader = classLoader;
  }

  protected List getPermissionPages() {
    Object permission_el = definitionMap.getMapElement(CM_AU_MANIFEST_KEY);

    if (permission_el instanceof String) {
      String permission_str = convertVariableString((String)permission_el);
      log.debug2("overriding permission page with " + permission_str);
      return ListUtil.list(permission_str);
    }
    else if (permission_el instanceof List) {
      return (List) permission_el;
    }
    else {
      return super.getPermissionPages();
    }
  }

  protected String makeStartUrl() {
    String startstr = definitionMap.getString(CM_AU_START_URL_KEY, "");
    String convstr = convertVariableString(startstr);
    log.debug2("setting start url " + convstr);
    return convstr;
  }

  public String getStartUrl() {
    return startUrlString;
  }

  protected void loadAuConfigDescrs(Configuration config) throws
      ConfigurationException {
    List descrList = plugin.getAuConfigDescrs();
    for (Iterator it = descrList.iterator(); it.hasNext(); ) {
      ConfigParamDescr descr = (ConfigParamDescr) it.next();
      String key = descr.getKey();

      try {
        Object val = descr.getValueOfType(config.get(key));
        definitionMap.setMapElement(key, val);
        // we store years in two formats - short and long
        if (descr.getType() == ConfigParamDescr.TYPE_YEAR) {
          int year = ((Integer)val).intValue() % 100;
          definitionMap.putInt(CM_AU_SHORT_YEAR_PREFIX + key, year);
        }
        if (descr.getType() == ConfigParamDescr.TYPE_URL) {
          URL url = definitionMap.getUrl(key, null);
          if(url != null) {
            definitionMap.putString(key+CM_AU_HOST_SUFFIX, url.getHost());
            definitionMap.putString(key+CM_AU_PATH_SUFFIX, url.getPath());
          }
        }
      }
      catch (Exception ex) {
        throw new ConfigurationException("Error configuring: " + key, ex);
      }
    }
    // now load any specialized parameters
    defaultFetchDelay =
        definitionMap.getLong(CM_AU_DEFAULT_PAUSE_TIME,
                                 DEFAULT_MILLISECONDS_BETWEEN_CRAWL_HTTP_REQUESTS);
    defaultContentCrawlIntv =
        definitionMap.getLong(CM_AU_DEFAULT_NC_CRAWL_KEY,
                                 DEFAULT_NEW_CONTENT_CRAWL_INTERVAL);
    maxAuSize = definitionMap.getLong(CM_AU_MAX_SIZE_KEY, 0);
    maxAuFileSize = definitionMap.getLong(CM_AU_MAX_FILE_SIZE_KEY, 0);

  }

  protected String makeName() {
    String namestr = definitionMap.getString(CM_AU_NAME_KEY, "");
    String convstr = convertVariableString(namestr);
    log.debug2("setting name string: " + convstr);
    return convstr;
  }

  protected CrawlRule makeRules() throws LockssRegexpException {
    List rules = new LinkedList();
    List templates = (List) definitionMap.getCollection(CM_AU_RULES_KEY,
        Collections.EMPTY_LIST);
    Iterator it = templates.iterator();
    while (it.hasNext()) {
      String rule_template = (String) it.next();
      rules.add(convertRule(rule_template));
    }
    if(rules.size() > 0)
      return new CrawlRules.FirstMatch(rules);
    else {
      log.error("No crawl rules found for plugin: " + makeName());
      return null;
    }
  }

  protected CrawlSpec makeCrawlSpec()
      throws LockssRegexpException {

    CrawlRule rule = makeRules();
    int depth = definitionMap.getInt(CM_AU_CRAWL_DEPTH, DEFAULT_AU_CRAWL_DEPTH);
    return new CrawlSpec(startUrlString, rule, depth);
  }

  protected CrawlWindow makeCrawlWindow() {
    CrawlWindow window = null;
    String window_class;
    window_class = definitionMap.getString(CM_AU_CRAWL_WINDOW_KEY,
                                              null);
    if (window_class != null) {
      try {
        ConfigurableCrawlWindow ccw = (ConfigurableCrawlWindow)
            Class.forName(window_class, true, classLoader).newInstance();
        window = ccw.makeCrawlWindow();
      }
      catch (Exception ex) {
        throw new DefinablePlugin.InvalidDefinitionException(
       auName + " failed to create crawl window from " + window_class, ex);
      }
    }
    return window;
  }


  protected FilterRule constructFilterRule(String mimeType) {
    Object filter_el = definitionMap.getMapElement(mimeType
        + CM_AU_FILTER_SUFFIX);
    try {
      if (filter_el instanceof String) {
	return (FilterRule) Class.forName( (String) filter_el, true, classLoader).newInstance();
      }
      else if (filter_el instanceof List) {
        if ( ( (List) filter_el).size() > 0) {
          return new DefinableFilterRule((List) filter_el);
        }
      }
    }
    catch (Exception ex) {
      throw new DefinablePlugin.InvalidDefinitionException(
          auName + " unable to create FilterRule: " + filter_el, ex);
    }
    return super.constructFilterRule(mimeType);
  }


  /**
   * Currently the only ContentParser we have is GoslingHtmlParser, so this
   * gets returned for any string that starts with "test/html".  Null otherwise
   * @param mimeType mime type to get a content parser for
   * @return GoslingHtmlParser if mimeType starts with "test/html",
   * null otherwise
   */
  public ContentParser getContentParser(String mimeType) {
    String parser_cl = definitionMap.getString(mimeType + CM_AU_PARSER_SUFFIX,
                                               null);
    if (parser_cl != null) {
      try {
        return (ContentParser) Class.forName(parser_cl, true, classLoader).newInstance();
      }
      catch (Exception ex) {
        throw new DefinablePlugin.InvalidDefinitionException(
       auName + " unable to create ContentParser: " + parser_cl, ex);
      }
    }
    return super.getContentParser(mimeType);
  }


// ---------------------------------------------------------------------
//   VARIABLE ARGUMENT REPLACEMENT SUPPORT ROUTINES
// ---------------------------------------------------------------------
  String convertVariableString(String printfString) {
    String converted_string = printfString;
    PrintfUtil.PrintfData p_data = PrintfUtil.stringToPrintf(printfString);
    String format = p_data.getFormat();
    Collection p_args = p_data.getArguments();
    ArrayList substitute_args = new ArrayList(p_args.size());

    boolean has_all_args = true;
    for (Iterator it = p_args.iterator(); it.hasNext(); ) {
      String key = (String) it.next();
      Object val = definitionMap.getMapElement(key);
      if (val != null) {
        substitute_args.add(val);
      }
      else {
        log.warning("misssing argument for : " + key);
        has_all_args = false;
      }
    }
    if (has_all_args) {
      PrintfFormat pf = new PrintfFormat(format);
      converted_string = pf.sprintf(substitute_args.toArray());
    }
    else {
      log.warning("missing variable arguments");
    }
    return converted_string;
  }

  CrawlRule convertRule(String printfString) throws LockssRegexpException {
    String rule = convertVariableString(printfString);
    String val_str = printfString.substring(0, printfString.indexOf(","));
    int value = Integer.valueOf(val_str).intValue();
    return new CrawlRules.RE(rule, value);
  }


  public interface ConfigurableCrawlWindow {
    public CrawlWindow makeCrawlWindow();
  }
}
