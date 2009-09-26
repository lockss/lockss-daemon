/*
 * $Id: DefinableArchivalUnit.java,v 1.74 2009-09-26 17:24:29 tlipkis Exp $
 */

/*
 Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.config.*;
import org.lockss.crawler.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.util.*;
import org.lockss.plugin.definable.DefinablePlugin.*;
import org.lockss.oai.*;
import org.lockss.state.AuState;

/**
 * <p>ConfigurableArchivalUnit: An implementatation of Base Archival Unit used
 * with the ConfigurablePlugin to allow a Map of values to be used to configure
 * and define the behaviour of a plugin.</p>
 * @author claire griffin
 * @version 1.0
 */
public class DefinableArchivalUnit extends BaseArchivalUnit {
  static Logger log = Logger.getLogger("DefinableArchivalUnit");

  /** If true, crawl rules in definable plugins are case-independent by
   * default.  Can override per-plugin with
   * <code>au_crawlrules_ignore_case</code> */
  static final String PARAM_CRAWL_RULES_IGNORE_CASE =
    Configuration.PREFIX + "plugin.crawlRulesIgnoreCase";
  static final boolean DEFAULT_CRAWL_RULES_IGNORE_CASE = true;


  public static final String PREFIX_NUMERIC = "numeric_";
  public static final int DEFAULT_AU_CRAWL_DEPTH = 1;
  public static final String DEFAULT_AU_EXPLODER_PATTERN = null;
  public static final String KEY_AU_NAME = "au_name";
  public static final String KEY_AU_CRAWL_RULES = "au_crawlrules";
  public static final String KEY_AU_CRAWL_RULES_IGNORE_CASE =
    "au_crawlrules_ignore_case";
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
  public static final String SUFFIX_LINK_REWRITER_FACTORY =
    "_link_rewriter_factory";
  public static final String SUFFIX_ARTICLE_ITERATOR_FACTORY =
    "_article_iterator_factory";
  public static final String SUFFIX_ARTICLE_MIME_TYPE =
    "_article_mime_type";
  public static final String SUFFIX_METADATA_EXTRACTOR_FACTORY_MAP =
    "_metadata_extractor_factory_map"; 

 public static final String SUFFIX_FETCH_RATE_LIMITER = "_fetch_rate_limiter";

  public static final String KEY_AU_PERMISSION_CHECKER_FACTORY =
    "au_permission_checker_factory";

  public static final String KEY_AU_LOGIN_PAGE_CHECKER =
    "au_login_page_checker";
  public static final String KEY_AU_REDIRECT_TO_LOGIN_URL_PATTERN =
    "au_redirect_to_login_url_pattern";
  public static final String KEY_DONT_POLL =
    "au_dont_poll";

  public static final String RANGE_SUBSTITUTION_STRING = "(.*)";
  public static final String NUM_SUBSTITUTION_STRING = "(\\d+)";

  public static final int MAX_NUM_RANGE_SIZE = 100;

  protected ExternalizableMap definitionMap;

  /** Array of DefinablePlugin keys that hold parameterized printf strings
   * used to generate URL lists */ 
  public static String[] printfUrlListKeys = {
    KEY_AU_START_URL,
    KEY_AU_MANIFEST,
    KEY_AU_REDIRECT_TO_LOGIN_URL_PATTERN,
  };

  /** Array of DefinablePlugin keys that hold parameterized printf strings
   * used to generate human readable strings */ 
  public static String[] printfStringKeys = {
    KEY_AU_NAME,
  };

  /** Array of all DefinablePlugin keys that hold parameterized printf
   * regexp strings */ 
  public static String[] printfRegexpKeys = {
    KEY_AU_CRAWL_RULES,
  };

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

  protected List<String> getElementList(String key) {
    return getDefinablePlugin().getElementList(key);
  }

  protected List convertPatternList(String key) {
    List<String> patternList = getElementList(key);

    if (patternList == null) {
      return null;
    }
    ArrayList<String> res = new ArrayList(patternList.size());
    for (String pattern : patternList) {
      if (StringUtil.isNullString(pattern)) {
	log.warning("Null pattern string in " + key);
	continue;
      }
      List<String> lst = convertUrlList(pattern);
      if (lst == null) {
	log.warning("Null converted string in " + key + ", from " + pattern);
	continue;
      }
      res.addAll(lst);
    }
    res.trimToSize();
    return res;
  }

  protected List getPermissionPages() {
    List res = convertPatternList(KEY_AU_MANIFEST);
    if (res == null) {
      return super.getPermissionPages();
    }
    return res;
  }

  public String getPerHostPermissionPath() {
    return (String)definitionMap.getMapElement(DefinablePlugin.KEY_PER_HOST_PERMISSION_PATH);
  }

  /** Use rate limiter source specified in AU, if any, then in plugin, then
   * default */
  @Override
  protected String getFetchRateLimiterSource() {
    String pluginSrc = 
      definitionMap.getString(DefinablePlugin.KEY_PLUGIN_FETCH_RATE_LIMITER_SOURCE,
			      DEFAULT_DEFAULT_FETCH_RATE_LIMITER_SOURCE);
    return paramMap.getString(KEY_AU_FETCH_RATE_LIMITER_SOURCE, pluginSrc);
  }

  @Override
  protected List<String> makeStartUrls() throws ConfigurationException {
    List res = convertPatternList(KEY_AU_START_URL);
    if (res == null) {
      String msg = "Bad start url pattern: "
	+ getElementList(KEY_AU_START_URL);
      log.error(msg);
      throw new ConfigurationException(msg);
    }
    log.debug2("Setting start urls " + res);
    return res;
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

    String patStr = convertVariableRegexpString(val).regexp;
    if (patStr == null) {
      String msg = "Missing regexp args: " + val;
      log.error(msg);
      throw new ConfigurationException(msg);
    }
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
    String name = convertNameString(namestr);
    log.debug2("setting name string: " + name);
    return name;
  }

  protected CrawlRule makeRules() throws LockssRegexpException {
    Object rule = definitionMap.getMapElement(KEY_AU_CRAWL_RULES);
    boolean defaultIgnoreCase =
      CurrentConfig.getBooleanParam(PARAM_CRAWL_RULES_IGNORE_CASE,
				    DEFAULT_CRAWL_RULES_IGNORE_CASE);
    boolean ignoreCase =
      definitionMap.getBoolean(KEY_AU_CRAWL_RULES_IGNORE_CASE,
			       defaultIgnoreCase);

    if (rule instanceof String) {
	CrawlRuleFromAuFactory fact = (CrawlRuleFromAuFactory)
            newAuxClass((String) rule, CrawlRuleFromAuFactory.class);
	return fact.createCrawlRule(this);
    }
    ArrayList rules = null;
    if (rule instanceof List) {
      List<String> templates = (List<String>)rule;
      rules = new ArrayList(templates.size());
      for (String rule_template : templates) {
	CrawlRule cr = convertRule(rule_template, ignoreCase);
	if (cr != null) {
	  rules.add(cr);
	}
      }
      rules.trimToSize();

      if (rules.size() > 0) {
	return new CrawlRules.FirstMatch(rules);
      } else {
	log.error("No crawl rules found for plugin: " + makeName());
	return null;
      }
    }
    return null;
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

      return new SpiderCrawlSpec(getNewContentCrawlUrls(),
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

  public boolean shouldCallTopLevelPoll(AuState aus) {
    if (definitionMap.getBoolean(KEY_DONT_POLL, false)) {
      return false;
    }
    return super.shouldCallTopLevelPoll(aus);
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

  MatchPattern convertVariableRegexpString(String printfString) {
    return new RegexpConverter().getMatchPattern(printfString);
  }

  List<String> convertUrlList(String printfString) {
    return new UrlListConverter().getUrlList(printfString);
  }

  String convertNameString(String printfString) {
    return new NameConverter().getName(printfString);
  }

  CrawlRule convertRule(String ruleString, boolean ignoreCase)
      throws LockssRegexpException {

    int pos = ruleString.indexOf(",");
    int action = Integer.parseInt(ruleString.substring(0, pos));
    String printfString = ruleString.substring(pos + 1);

    MatchPattern mp = convertVariableRegexpString(printfString);
    if (mp.regexp == null) {
      return null;
    }
    List<List> matchArgs = mp.matchArgs;
    switch (matchArgs.size()) {
    case 0:
      return new CrawlRules.RE(mp.regexp, ignoreCase, action);
    case 1:
      List argPair = matchArgs.get(0);
      ConfigParamDescr descr = mp.matchArgDescrs.get(0);
      switch (descr.getType()) {
      case ConfigParamDescr.TYPE_RANGE:
	return new CrawlRules.REMatchRange(mp.regexp,
					   ignoreCase,
					   action,
					   (String)argPair.get(0),
					   (String)argPair.get(1));
      case ConfigParamDescr.TYPE_NUM_RANGE:
	return new CrawlRules.REMatchRange(mp.regexp,
					   ignoreCase,
					   action,
					   ((Long)argPair.get(0)).longValue(),
					   ((Long)argPair.get(1)).longValue());
      default:
	throw new RuntimeException("Shouldn't happen.  Unknown REMatchRange arg type: " + descr);
      }

    default:
      throw new LockssRegexpException("Multiple range args not yet supported");
    }
  }

  abstract class Converter {
    PrintfUtil.PrintfData p_data;
    String format;
    PrintfFormat pf;
    Collection<String> p_args;
    ArrayList substitute_args;
    boolean missingArgs = false;

    void convert(String printfString) {
      p_data = PrintfUtil.stringToPrintf(printfString);
      format = p_data.getFormat();
      p_args = p_data.getArguments();
      pf = new PrintfFormat(format);
      substitute_args = new ArrayList(p_args.size());

      for (String key : p_args) {
	Object val = paramMap.getMapElement(key);
	ConfigParamDescr descr = plugin.findAuConfigDescr(key);
	if (val != null) {
	  interpArg(key, val, descr);
	} else {
	  missingArgs = true;
	  log.warning("missing argument for : " + key);
	  interpNullArg(key, val, descr);
	}
      }
    }

    void interpArg(String key, Object val, ConfigParamDescr descr) {
      switch (descr != null ? descr.getType()
	      : ConfigParamDescr.TYPE_STRING) {
      case ConfigParamDescr.TYPE_SET:
	interpSetArg(key, val, descr);
	break;
      case ConfigParamDescr.TYPE_RANGE:
	interpRangeArg(key, val, descr);
	break;
      case ConfigParamDescr.TYPE_NUM_RANGE:
	interpNumRangeArg(key, val, descr);
	break;
      default:
	interpPlainArg(key, val, descr);
	break;
      }
    }

    abstract void interpSetArg(String key, Object val,
			       ConfigParamDescr descr);
    abstract void interpRangeArg(String key, Object val,
				 ConfigParamDescr descr);
    abstract void interpNumRangeArg(String key, Object val,
				    ConfigParamDescr descr);
    abstract void interpPlainArg(String key, Object val,
				 ConfigParamDescr descr);
    void interpNullArg(String key, Object val, ConfigParamDescr descr) {
    }
  }

  class RegexpConverter extends Converter {
    ArrayList matchArgs = new ArrayList();
    ArrayList matchArgDescrs = new ArrayList();

    void interpSetArg(String key, Object val, ConfigParamDescr descr) {
      // val must be a list; ok to throw if not
      List<String> vec = (List<String>)val;
      List tmplst = new ArrayList(vec.size());
      for (String ele : vec) {
	tmplst.add(Perl5Compiler.quotemeta(ele));
      }
      substitute_args.add(StringUtil.separatedString(tmplst, "(?:", "|", ")"));
    }

    void interpRangeArg(String key, Object val, ConfigParamDescr descr) {
      substitute_args.add(RANGE_SUBSTITUTION_STRING);
      matchArgs.add(val);
      matchArgDescrs.add(descr);
    }

    void interpNumRangeArg(String key, Object val, ConfigParamDescr descr) {
      substitute_args.add(NUM_SUBSTITUTION_STRING);
      matchArgs.add(val);
      matchArgDescrs.add(descr);
    }

    void interpPlainArg(String key, Object val, ConfigParamDescr descr) {
      if (val instanceof String) {
	val = Perl5Compiler.quotemeta((String)val);
      }
      substitute_args.add(val);
    }

    MatchPattern getMatchPattern(String printfString) {
      convert(printfString);
      if (missingArgs) {
	log.warning("Missing variable arguments: " + p_data);
	return new MatchPattern();
      }
      if (log.isDebug3()) {
	log.debug3("sprintf(\""+format+"\", "+substitute_args+")");
      }
      return new MatchPattern(pf.sprintf(substitute_args.toArray()),
			      matchArgs, matchArgDescrs);
    }

  }

  class UrlListConverter extends Converter {
    boolean haveSets = false;

    void haveSets() {
      if (!haveSets) {
	// if this is first set seen, replace all values so far with
	// singleton list of value
	for (int ix = 0; ix < substitute_args.size(); ix++) {
	  substitute_args.set(ix,
			      Collections.singletonList(substitute_args.get(ix)));
	}
	haveSets = true;
      }
    }

    void interpSetArg(String key, Object val, ConfigParamDescr descr) {
      haveSets();
      // val must be a list; ok to throw if not
      List<String> vec = (List<String>)val;
      substitute_args.add(vec);
    }

    void interpRangeArg(String key, Object val, ConfigParamDescr descr) {
      throw new PluginException.InvalidDefinition("String range params are not allowed in URL patterns: " + key);
    }

    void interpNumRangeArg(String key, Object val, ConfigParamDescr descr) {
      haveSets();
      // val must be a list; ok to throw if not
      List<Long> vec = (List<Long>)val;
      long min = vec.get(0).longValue();
      long max = vec.get(1).longValue();
      long size = max - min + 1;
      if (size > MAX_NUM_RANGE_SIZE) {
	log.error("Excessively large numeric range: " + min + "-" + max
		  + ", truncating");
	max = min + MAX_NUM_RANGE_SIZE;
	size = max - min + 1;
      }
      List lst = new ArrayList((int)size);
      for (long x = min; x <= max; x++) {
	lst.add(x);
      }
      substitute_args.add(lst);
    }

    void interpPlainArg(String key, Object val, ConfigParamDescr descr) {
      if (haveSets) {
	substitute_args.add(Collections.singletonList(val));
      } else {
	substitute_args.add(val);
      }
    }

    List getUrlList(String printfString) {
      convert(printfString);
      if (missingArgs) {
	log.warning("Missing variable arguments: " + p_data);
	return null;
      }
      if (!substitute_args.isEmpty() && haveSets) {
	ArrayList res = new ArrayList();
	for (CartesianProductIterator iter =
	       new CartesianProductIterator(substitute_args);
	     iter.hasNext(); ) {
	  Object[] oneCombo = (Object[])iter.next();
	  if (log.isDebug3()) {
	    log.debug3("sprintf(\""+format+"\", "+oneCombo+")");
	  }
	  res.add(pf.sprintf(oneCombo));
	}
	res.trimToSize();
	return res;
      } else {
	if (log.isDebug3()) {
	  log.debug3("sprintf(\""+format+"\", "+substitute_args+")");
	}
	return
	  Collections.singletonList(pf.sprintf(substitute_args.toArray()));
      }
    }
  }

  class NameConverter extends Converter {

    void interpSetArg(String key, Object val, ConfigParamDescr descr) {
      // val must be a list; ok to throw if not
      List<String> vec = (List<String>)val;
      substitute_args.add(StringUtil.separatedString(vec, ", "));
    }

    void interpRangeArg(String key, Object val, ConfigParamDescr descr) {
      // val must be a list; ok to throw if not
      List<String> vec = (List<String>)val;
      substitute_args.add(StringUtil.separatedString(vec, "-"));
    }

    void interpNumRangeArg(String key, Object val, ConfigParamDescr descr) {
      interpRangeArg(key, val, descr);
    }

    void interpPlainArg(String key, Object val, ConfigParamDescr descr) {
      substitute_args.add(val);
    }

    void interpNullArg(String key, Object val, ConfigParamDescr descr) {
      substitute_args.add("(null)");
    }

    String getName(String printfString) {
      convert(printfString);
      if (missingArgs) {
	log.warning("Missing variable arguments: " + p_data);
      }
      if (log.isDebug3()) {
	log.debug3("sprintf(\""+format+"\", "+substitute_args+")");
      }
      return pf.sprintf(substitute_args.toArray());
    }
  }


  static class MatchPattern {
    String regexp;
    List<List> matchArgs;
    List<ConfigParamDescr> matchArgDescrs;

    MatchPattern() {
    }

    MatchPattern(String regexp,
		 List<List> matchArgs,
		 List<ConfigParamDescr> matchArgDescrs) {
      this.regexp = regexp;
      this.matchArgs = matchArgs;
      this.matchArgDescrs = matchArgDescrs;
    }
  }

  public interface ConfigurableCrawlWindow {
    public CrawlWindow makeCrawlWindow()
	throws PluginException;
  }

}
