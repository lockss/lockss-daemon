/*
 * $Id: DefinableArchivalUnit.java,v 1.18 2004-09-22 23:50:19 clairegriffin Exp $
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
import org.lockss.plugin.base.*;
import org.lockss.util.*;
import org.lockss.plugin.definable.DefinablePlugin.*;

/**
 * <p>ConfigurableArchivalUnit: An implementatation of Base Archival Unit used
 * with the ConfigurablePlugin to allow a Map of values to be used to configure
 * and define the behaviour of a plugin.</p>
 * @author claire griffin
 * @version 1.0
 */
public class DefinableArchivalUnit extends BaseArchivalUnit {
  static final public String AU_SHORT_YEAR_PREFIX = "au_short_";
  static final public String AU_HOST_SUFFIX = "_host";
  static final public String AU_PATH_SUFFIX = "_path";
  static final public int DEFAULT_AU_CRAWL_DEPTH = 1;
  static final public String AU_START_URL_KEY = "au_start_url";
  static final public String AU_NAME_KEY = "au_name";
  static final public String AU_RULES_KEY = "au_crawlrules";
  static final public String AU_CRAWL_WINDOW_KEY = "au_crawlwindow";
  static final public String AU_EXPECTED_PATH = "au_expected_base_path";
  static final public String AU_CRAWL_DEPTH = "au_crawl_depth";
  static final public String AU_MANIFEST_KEY = "au_manifest";
  static final public String AU_URL_NORMALIZER_KEY = "au_url_normalizer";


  protected ClassLoader classLoader;
  protected ExternalizableMap definitionMap;
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
    Object permission_el = definitionMap.getMapElement(AU_MANIFEST_KEY);

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
    String startstr = definitionMap.getString(AU_START_URL_KEY, "");
    String convstr = convertVariableString(startstr);
    log.debug2("setting start url " + convstr);
    return convstr;
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
          definitionMap.putInt(AU_SHORT_YEAR_PREFIX + key, year);
        }
        if (descr.getType() == ConfigParamDescr.TYPE_URL) {
          URL url = definitionMap.getUrl(key, null);
          if(url != null) {
            definitionMap.putString(key+AU_HOST_SUFFIX, url.getHost());
            definitionMap.putString(key+AU_PATH_SUFFIX, url.getPath());
          }
        }
      }
      catch (Exception ex) {
        throw new ConfigurationException("Error configuring: " + key, ex);
      }
    }
    // override any defaults
    defaultFetchDelay = definitionMap.getLong(AU_DEFAULT_PAUSE_TIME,
        DEFAULT_MILLISECONDS_BETWEEN_CRAWL_HTTP_REQUESTS);

    defaultContentCrawlIntv = definitionMap.getLong(AU_DEFAULT_NC_CRAWL_KEY,
        DEFAULT_NEW_CONTENT_CRAWL_INTERVAL);

    // install any other values - should these be config params?
    long l_val;
    l_val = definitionMap.getLong(AU_MAX_SIZE_KEY,
                                  DEFAULT_AU_MAX_SIZE);
    paramMap.putLong(AU_MAX_SIZE_KEY, l_val);

    l_val = definitionMap.getLong(AU_MAX_FILE_SIZE_KEY,
                                  DEFAULT_AU_MAX_FILE_SIZE);
    paramMap.putLong(AU_MAX_FILE_SIZE_KEY, l_val);

  }

  protected String makeName() {
    String namestr = definitionMap.getString(AU_NAME_KEY, "");
    String convstr = convertVariableString(namestr);
    log.debug2("setting name string: " + convstr);
    return convstr;
  }

  protected CrawlRule makeRules() throws LockssRegexpException {
    Object rule = definitionMap.getMapElement(AU_RULES_KEY);

    if (rule instanceof String) {
	CrawlRuleFromAuFactory fact = (CrawlRuleFromAuFactory)
            loadClass((String) rule, "CrawlRule");
	return fact.createCrawlRule(this);
    }
    List rules = new LinkedList();
    if(rule instanceof List) {
      List templates = (List) rule;
      Iterator it = templates.iterator();

      while (it.hasNext()) {
	String rule_template = (String) it.next();

	rules.add(convertRule(rule_template));
      }
    }

    if (rules.size() > 0)
      return new CrawlRules.FirstMatch(rules);
    else {
      log.error("No crawl rules found for plugin: " + makeName());
      return null;
    }
  }


  protected CrawlSpec makeCrawlSpec()
      throws LockssRegexpException {

    CrawlRule rule = makeRules();
    int depth = definitionMap.getInt(AU_CRAWL_DEPTH, DEFAULT_AU_CRAWL_DEPTH);
    return new CrawlSpec(startUrlString, rule, depth);
  }

  protected CrawlWindow makeCrawlWindow() {
    CrawlWindow window = null;
    String window_class;
    window_class = definitionMap.getString(AU_CRAWL_WINDOW_KEY,
                                              null);
    if (window_class != null) {
      ConfigurableCrawlWindow ccw =
          (ConfigurableCrawlWindow) loadClass(window_class, "CrawlWindow");
       window = ccw.makeCrawlWindow();
     }
    return window;
  }

  protected UrlNormalizer makeUrlNormalizer() {
    UrlNormalizer urlNormalizer = null;
    String normalizerClass = definitionMap.getString(AU_URL_NORMALIZER_KEY, null);
    if (normalizerClass != null) {
      urlNormalizer = (UrlNormalizer)loadClass(normalizerClass, "UrlNormalizer");
    }
    return urlNormalizer;
  }

  protected FilterRule constructFilterRule(String mimeType) {
    Object filter_el = definitionMap.getMapElement(mimeType
        + AU_FILTER_SUFFIX);

    if (filter_el instanceof String) {
      return (FilterRule) loadClass( (String) filter_el, "FilterRule");
    }
    else if (filter_el instanceof List) {
      if ( ( (List) filter_el).size() > 0) {
        return new DefinableFilterRule( (List) filter_el);
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
    String parser_cl = definitionMap.getString(mimeType + AU_PARSER_SUFFIX,
                                               null);
    if (parser_cl != null) {
      return (ContentParser) loadClass(parser_cl, "ContentParser");
    }
    return super.getContentParser(mimeType);
  }

// ---------------------------------------------------------------------
//   CLASS LOADING SUPPORT ROUTINES
// ---------------------------------------------------------------------

  Object loadClass(String className, String description) {
    try {
      return Class.forName(className, true, classLoader).newInstance();
    }
    catch (Exception ex) {
      throw new InvalidDefinitionException(
          auName + " unable to create " + description + ": " + className, ex);
    }
    catch (LinkageError le) {
      throw new InvalidDefinitionException(
          auName + " unable to create " + description + ": " + className, le);

    }
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
