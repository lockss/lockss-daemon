/*
 * $Id: DefinableArchivalUnit.java,v 1.59.4.2 2007-09-13 21:43:22 dshr Exp $
 */

/*
 Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

import org.apache.commons.collections.*;
import org.apache.oro.text.regex.*;
import org.lockss.config.Configuration;
import org.lockss.crawler.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.util.*;
import org.lockss.plugin.definable.DefinablePlugin.*;
import org.lockss.oai.*;

/**
 * <p>ConfigurableArchivalUnit: An implementatation of Base Archival Unit used
 * with the ConfigurablePlugin to allow a Map of values to be used to configure
 * and define the behaviour of a plugin.</p>
 * @author claire griffin
 * @version 1.0
 */
public class DefinableArchivalUnit extends BaseArchivalUnit {
  static Logger log = Logger.getLogger("DefinableArchivalUnit");

  public static final String PREFIX_NUMERIC = "numeric_";
  public static final int DEFAULT_AU_CRAWL_DEPTH = 1;
  public static final String DEFAULT_AU_EXPLODER_PATTERN = null;
  public static final String KEY_AU_NAME = "au_name";
  public static final String KEY_AU_CRAWL_RULES = "au_crawlrules";
  public static final String KEY_AU_CRAWL_WINDOW = "au_crawlwindow";
  public static final String KEY_AU_CRAWL_WINDOW_SER = "au_crawlwindow_ser";
  public static final String KEY_AU_EXPECTED_BASE_PATH = "au_expected_base_path";
  public static final String KEY_AU_CRAWL_DEPTH = "au_crawl_depth";
  public static final String KEY_AU_MANIFEST = "au_manifest";
  //public static final String KEY_AU_URL_NORMALIZER = "au_url_normalizer";
  public static final String KEY_AU_EXPLODER_HELPER = "au_exploder_helper";
  public static final String KEY_AU_EXPLODER_PATTERN = "au_exploder_pattern";

  public static final String SUFFIX_PARSER = "_parser";
  public static final String SUFFIX_LINK_EXTRACTOR_FACTORY =
    "_link_extractor_factory";
  public static final String SUFFIX_FILTER_RULE = "_filter";
  public static final String SUFFIX_FILTER_FACTORY = "_filter_factory";
  public static final String SUFFIX_FETCH_RATE_LIMITER = "_fetch_rate_limiter";

  public static final String KEY_AU_PERMISSION_CHECKER_FACTORY =
    "au_permission_checker_factory";

  public static final String KEY_AU_LOGIN_PAGE_CHECKER =
    "au_login_page_checker";
  public static final String KEY_AU_REDIRECT_TO_LOGIN_URL_PATTERN =
    "au_redirect_to_login_url_pattern";

  public static final String RANGE_SUBSTITUTION_STRING = "(.*)";
  public static final String NUM_SUBSTITUTION_STRING = "(\\d+)";

  protected ExternalizableMap definitionMap;

  protected DefinableArchivalUnit(Plugin myPlugin) {
    super(myPlugin);
    throw new UnsupportedOperationException(
        "DefinableArchvialUnit requires DefinablePlugin for construction");
  }

  protected DefinableArchivalUnit(DefinablePlugin myPlugin,
                                  ExternalizableMap definitionMap) {
    super(myPlugin);
    this.definitionMap = definitionMap;
  }

  DefinablePlugin getDefinablePlugin() {
    return (DefinablePlugin)plugin;
  }

  protected List getPermissionPages() {
    List templateList;
    Object permission_el = definitionMap.getMapElement(KEY_AU_MANIFEST);

    if (permission_el instanceof String) {
      templateList = ListUtil.list((String)permission_el);
    } else if (permission_el instanceof List) {
       templateList = (List) permission_el;
    } else {
      return super.getPermissionPages();
    }
    List permission_list = new ArrayList(templateList.size());
    for(Iterator it = templateList.iterator(); it.hasNext();) {
      String permissionPage = convertVariableString((String)it.next());
      log.debug3("Adding permission page: "+permissionPage);
      permission_list.add(permissionPage);
    }
    return permission_list;
  }

  protected String makeStartUrl() {
    String startstr = definitionMap.getString(KEY_AU_START_URL, "");
    String convstr = convertVariableString(startstr);
    log.debug2("setting start url " + convstr);
    return convstr;
  }

  protected void loadAuConfigDescrs(Configuration config) throws
      ConfigurationException {
    super.loadAuConfigDescrs(config);
    // override any defaults
    defaultFetchDelay = definitionMap.getLong(KEY_AU_DEFAULT_PAUSE_TIME,
        DEFAULT_FETCH_DELAY);

    defaultContentCrawlIntv = definitionMap.getLong(KEY_AU_DEFAULT_NEW_CONTENT_CRAWL_INTERVAL,
        DEFAULT_NEW_CONTENT_CRAWL_INTERVAL);

    // install any other values - should these be config params?
    long l_val;
    l_val = definitionMap.getLong(KEY_AU_MAX_SIZE,
                                  DEFAULT_AU_MAX_SIZE);
    paramMap.putLong(KEY_AU_MAX_SIZE, l_val);

    l_val = definitionMap.getLong(KEY_AU_MAX_FILE_SIZE,
                                  DEFAULT_AU_MAX_FILE_SIZE);
    paramMap.putLong(KEY_AU_MAX_FILE_SIZE, l_val);

  }

  protected void addImpliedConfigParams()
      throws ArchivalUnit.ConfigurationException {
    super.addImpliedConfigParams();
    String umsg =
      definitionMap.getString(DefinablePlugin.KEY_PLUGIN_AU_CONFIG_USER_MSG,
			      null);
    if (umsg != null) {
      paramMap.putString(KEY_AU_CONFIG_USER_MSG, umsg);
    }
    String urlPat =
      (String)definitionMap.getMapElement(KEY_AU_REDIRECT_TO_LOGIN_URL_PATTERN);
    if (urlPat != null) {
      paramMap.setMapElement(KEY_AU_REDIRECT_TO_LOGIN_URL_PATTERN,
			     makeLoginUrlPattern(urlPat));
    }
  }

  protected Pattern makeLoginUrlPattern(String val)
      throws ArchivalUnit.ConfigurationException {

    String patStr = convertVariableRegexpString(val);
    try {
      return
	RegexpUtil.getCompiler().compile(patStr, Perl5Compiler.READ_ONLY_MASK);
    } catch (MalformedPatternException e) {
      String msg = "Can't compile URL pattern: " + patStr;
      log.error(msg + ": " + e.toString());
      throw new ArchivalUnit.ConfigurationException(msg, e);
    }
  }

  public boolean isLoginPageUrl(String url) {
    Pattern urlPat =
      (Pattern)paramMap.getMapElement(KEY_AU_REDIRECT_TO_LOGIN_URL_PATTERN);
    if (urlPat == null) {
      return false;
    }
    Perl5Matcher matcher = RegexpUtil.getMatcher();
    return  matcher.contains(url, urlPat);
  }    

  protected String makeName() {
    String namestr = definitionMap.getString(KEY_AU_NAME, "");
    String convstr = convertVariableString(namestr);
    log.debug2("setting name string: " + convstr);
    return convstr;
  }

  protected CrawlRule makeRules() throws LockssRegexpException {
    Object rule = definitionMap.getMapElement(KEY_AU_CRAWL_RULES);

    if (rule instanceof String) {
	CrawlRuleFromAuFactory fact = (CrawlRuleFromAuFactory)
            newAuxClass((String) rule, CrawlRuleFromAuFactory.class);
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

    if (rules.size() > 0) {
      return new CrawlRules.FirstMatch(rules);
    } else {
      log.error("No crawl rules found for plugin: " + makeName());
      return null;
    }
  }

  protected OaiRequestData makeOaiData() {
    URL oai_request_url =
      paramMap.getUrl(ConfigParamDescr.OAI_REQUEST_URL.getKey());
    String oaiRequestUrlStr = oai_request_url.toString();
    String oai_au_spec = null;
    try {
      oai_au_spec = paramMap.getString(ConfigParamDescr.OAI_SPEC.getKey());
    } catch (NoSuchElementException ex) {
      // This is acceptable.  Null value will fetch all entries.
      log.debug("No oai_spec for this plugin.");
    }
    log.debug3("Creating OaiRequestData with oaiRequestUrlStr" +
	       oaiRequestUrlStr + " and oai_au_spec " + oai_au_spec);
    return new OaiRequestData(oaiRequestUrlStr,
                      "http://purl.org/dc/elements/1.1/",
                      "identifier",
                      oai_au_spec,
                      "oai_dc"
                      );

  }

  protected CrawlSpec makeCrawlSpec() throws LockssRegexpException {

    CrawlRule rule = makeRules();
    String crawl_type = definitionMap.getString(DefinablePlugin.KEY_CRAWL_TYPE,
                                                DefinablePlugin.DEFAULT_CRAWL_TYPE);
    //XXX put makePermissionCheckersHere

    if(crawl_type.equals(DefinablePlugin.CRAWL_TYPE_OAI)) {
      boolean follow_links =
          definitionMap.getBoolean(DefinablePlugin.KEY_FOLLOW_LINKS, true);
      return new OaiCrawlSpec(makeOaiData(), getPermissionPages(),
                              null, rule, follow_links,
                              makeLoginPageChecker());
    } else { // for now use the default spider crawl spec
      int depth = definitionMap.getInt(KEY_AU_CRAWL_DEPTH, DEFAULT_AU_CRAWL_DEPTH);
      String exploderPattern = definitionMap.getString(KEY_AU_EXPLODER_PATTERN,
						  DEFAULT_AU_EXPLODER_PATTERN);
      ExploderHelper eh = getDefinablePlugin().getExploderHelper();
      //XXX change to a list
//       String startUrl = paramMap.getString(AU_START_URL);

//       return new SpiderCrawlSpec(ListUtil.list(startUrl),
      return new SpiderCrawlSpec(ListUtil.list(startUrlString),
				 getPermissionPages(), rule, depth,
				 makePermissionChecker(),
				 makeLoginPageChecker(), exploderPattern, eh);
    }
  }

  protected LoginPageChecker makeLoginPageChecker() {
    String loginPageCheckerClass =
      definitionMap.getString(KEY_AU_LOGIN_PAGE_CHECKER, null);
    if (loginPageCheckerClass == null) {
      return null;
    }
    LoginPageChecker checker =
      (LoginPageChecker)newAuxClass(loginPageCheckerClass,
				    LoginPageChecker.class);
    return checker;
  }

  protected PermissionChecker makePermissionChecker() {
    String permissionCheckerFactoryClass =
      definitionMap.getString(KEY_AU_PERMISSION_CHECKER_FACTORY, null);
    if (permissionCheckerFactoryClass == null) {
      return null;
    }
    log.debug3("Found PermissionCheckerFactory class: " +
	       permissionCheckerFactoryClass);

    PermissionCheckerFactory fact =
      (PermissionCheckerFactory)newAuxClass(permissionCheckerFactoryClass,
					    PermissionCheckerFactory.class);
    log.debug("Loaded PermissionCheckerFactory: " + fact);
    try {
      List permissionCheckers = fact.createPermissionCheckers(this);
      if (permissionCheckers.size() > 1) {
        log.error("Plugin specifies multiple permission checkers, but we " +
		  "only support one: " + this);

      }
      return (PermissionChecker)permissionCheckers.get(0);
    } catch (PluginException e) {
      throw new RuntimeException(e);
    }
  }

  protected CrawlWindow makeCrawlWindow() {
    return getDefinablePlugin().makeCrawlWindow();
  }

// ---------------------------------------------------------------------
//   CLASS LOADING SUPPORT ROUTINES
// ---------------------------------------------------------------------

  Object newAuxClass(String className, Class expectedType) {
    return getDefinablePlugin().newAuxClass(className, expectedType);
  }

// ---------------------------------------------------------------------
//   VARIABLE ARGUMENT REPLACEMENT SUPPORT ROUTINES
// ---------------------------------------------------------------------
  String convertVariableRegexpString(String printfString) {
    return convertVariableString(printfString, true);
  }

  String convertVariableString(String printfString) {
    return convertVariableString(printfString, false);
  }    

  String convertVariableString(String printfString, boolean quoteRegexp) {
    String converted_string = printfString;
    PrintfUtil.PrintfData p_data = PrintfUtil.stringToPrintf(printfString);
    String format = p_data.getFormat();
    Collection p_args = p_data.getArguments();
    ArrayList substitute_args = new ArrayList(p_args.size());

    boolean has_all_args = true;
    for (Iterator it = p_args.iterator(); it.hasNext(); ) {
      String key = (String) it.next();
      Object val = paramMap.getMapElement(key);
      if (val != null){
        if (val instanceof Vector) {
          Vector vec = (Vector) val;
          if(vec.elementAt(0) instanceof Long) {
            substitute_args.add(NUM_SUBSTITUTION_STRING);
          } else {
            substitute_args.add(RANGE_SUBSTITUTION_STRING);
          }
        } else {
	  if (quoteRegexp && val instanceof String) {
	    val = Perl5Compiler.quotemeta((String)val);
	  }
          substitute_args.add(val);
        }
      } else {
        log.warning("misssing argument for : " + key);
        has_all_args = false;
      }
    }

    if (has_all_args) {
      PrintfFormat pf = new PrintfFormat(format);
      converted_string = pf.sprintf(substitute_args.toArray());
    } else {
      log.warning("missing variable arguments");
    }
    return converted_string;
  }

  CrawlRule convertRule(String printfString) throws LockssRegexpException {
    String rule = convertVariableRegexpString(printfString);
    String action_str = printfString.substring(0, printfString.indexOf(","));
    int action = Integer.valueOf(action_str).intValue();
    Vector vec;
    if (rule.indexOf(RANGE_SUBSTITUTION_STRING) != -1
        || rule.indexOf(NUM_SUBSTITUTION_STRING) != -1) {
      // Check for range or set

      for (Iterator iter = plugin.getAuConfigDescrs().iterator() ; iter.hasNext() ; ) {
        ConfigParamDescr descr = (ConfigParamDescr)iter.next();
        switch (descr.getType()) {
          case ConfigParamDescr.TYPE_RANGE:
            vec = (Vector)paramMap.getMapElement(descr.getKey());
            if (vec != null) {
              return new CrawlRules.REMatchRange(rule,
                                                 action,
                                                 (String)vec.elementAt(0),
                                                 (String)vec.elementAt(1));
            }
            break;
          case ConfigParamDescr.TYPE_NUM_RANGE:
            vec = (Vector)paramMap.getMapElement(descr.getKey());
            if (vec != null) {
              return new CrawlRules.REMatchRange(rule,
                                                 action,
                                                 ((Long)vec.elementAt(0)).longValue(),
                                                 ((Long)vec.elementAt(1)).longValue());
            }
            break;
          case ConfigParamDescr.TYPE_SET:
            vec = (Vector)paramMap.getMapElement(descr.getKey());
            if (vec != null) {
              return new CrawlRules.REMatchSet(rule,
                                               action,
                                               new HashSet(vec));
            }
            break;
        }
      }

    }

    return new CrawlRules.RE(rule, action);
  }

  public interface ConfigurableCrawlWindow {
    public CrawlWindow makeCrawlWindow()
	throws PluginException;
  }

}
