/*
 * $Id: DefinableArchivalUnit.java,v 1.3 2004-04-28 22:52:05 clairegriffin Exp $
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

import java.util.*;

import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.ArchivalUnit.*;
import org.lockss.plugin.base.*;
import org.lockss.util.*;
import gnu.regexp.*;
import java.net.URL;
import org.lockss.crawler.*;

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
  static final public String CM_AU_PARSER_SUFFIX = "_parser";
  static final public String CM_AU_FILTER_SUFFIX = "_filter";
  static final public int DEFAULT_AU_CRAWL_DEPTH = 1;

  protected ExternalizableMap definitionMap;
  static Logger log = Logger.getLogger("ConfigurableArchivalUnit");

  protected DefinableArchivalUnit(Plugin myPlugin) {
    super(myPlugin);
    throw new UnsupportedOperationException(
        "ConfigurableArchvialUnit requires ConfigurablePlugin for construction");
  }


  protected DefinableArchivalUnit(DefinablePlugin myPlugin,
                                     ExternalizableMap definitionMap) {
    super(myPlugin);
    this.definitionMap = definitionMap;
  }

  public String getManifestPage() {
    String manifestString =
        definitionMap.getString(CM_AU_MANIFEST_KEY, null);
    if(manifestString == null)  {
     return super.getManifestPage();
    }
    manifestString = convertVariableString(manifestString);
    log.debug2("overriding manifest page with " + manifestString);

    return manifestString;
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
  }

  protected String makeName() {
    String namestr = definitionMap.getString(CM_AU_NAME_KEY, "");
    String convstr = convertVariableString(namestr);
    log.debug2("setting name string: " + convstr);
    return convstr;
  }

  protected CrawlRule makeRules() throws gnu.regexp.REException {
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
      throws REException {

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
            Class.forName(window_class).newInstance();
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
    String filter = definitionMap.getString(mimeType + CM_AU_FILTER_SUFFIX,
                                            null);
    if (filter != null) {
      try {
        return (FilterRule) Class.forName(filter).newInstance();
      }
      catch (Exception ex) {
        throw new DefinablePlugin.InvalidDefinitionException(
       auName + " unable to create FilterRule: " + filter, ex);

      }
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
        return (ContentParser) Class.forName(parser_cl).newInstance();
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
  private String[] getStringTokens(String tokenString) {
    StringTokenizer st = new StringTokenizer(tokenString, "\n");
    int num_tokens = st.countTokens();
    String[] strs = new String[num_tokens];
    for (int i = 0; i < num_tokens; i++) {
      strs[i] = st.nextToken();
    }
    return strs;
  }

  String convertVariableString(String variableString) {
    if (StringUtil.isNullString(variableString)) {
      return variableString;
    }
    String[] strs = getStringTokens(variableString);
    String cur_str = strs[0];
    ArrayList args = new ArrayList();
    boolean has_all_args = true;
    for (int i = 1; i < strs.length && has_all_args; i++) {
      String key = strs[i];
      Object val = definitionMap.getMapElement(key);
      if (val != null) {
        args.add(val);
      }
      else {
        log.warning("misssing argument for : " + key);
        has_all_args = false;
      }
    }
    if (has_all_args) {
      PrintfFormat pf = new PrintfFormat(cur_str);
      cur_str = pf.sprintf(args.toArray());
    }
    else {
      log.warning("missing variable arguments");
    }
    return cur_str;
  }

  CrawlRule convertRule(String variableString) throws REException {
    String[] strs = getStringTokens(variableString);
    int value = Integer.valueOf(strs[0]).intValue();
    String rule = strs[1];
    ArrayList args = new ArrayList();
    boolean has_all_args = true;
    for (int i = 2; i < strs.length && has_all_args; i++) {
      String key = strs[i];
      Object val = definitionMap.getMapElement(key);
      if (val != null) {
        args.add(val);
      }
      else {
        log.warning("misssing argument for : " + key);
        has_all_args = false;
      }
    }
    if (has_all_args) {
      PrintfFormat pf = new PrintfFormat(rule);
      rule = pf.sprintf(args.toArray());
      log.debug2("Adding crawl rule: " + rule);
      return new CrawlRules.RE(rule, value);
    }
    return null;
  }

  public interface ConfigurableCrawlWindow {
    public CrawlWindow makeCrawlWindow();
  }
}
