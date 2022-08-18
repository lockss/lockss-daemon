/*
 * $Id$
 */

/*
 Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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
import java.io.IOException;
import org.apache.commons.lang3.tuple.*;

import org.apache.commons.collections.*;
import org.apache.oro.text.regex.*;
import org.lockss.config.*;
import org.lockss.crawler.*;
import org.lockss.daemon.*;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.util.*;
import org.lockss.util.Constants.RegexpContext;
import org.lockss.util.urlconn.*;
import org.lockss.plugin.exploded.ExplodingUrlConsumerFactory;
import org.lockss.state.AuState;

import static org.lockss.plugin.PrintfConverter.PrintfContext;

/**
 * <p>ConfigurableArchivalUnit: An implementatation of Base Archival Unit used
 * with the ConfigurablePlugin to allow a Map of values to be used to configure
 * and define the behaviour of a plugin.</p>
 * @author claire griffin
 * @version 1.0
 */
public class DefinableArchivalUnit extends BaseArchivalUnit 
    implements ExplodableArchivalUnit {
  static Logger log = Logger.getLogger("DefinableArchivalUnit");

  /** If true, crawl rules in definable plugins are case-independent by
   * default.  Can override per-plugin with
   * <code>au_crawlrules_ignore_case</code> */
  static final String PARAM_CRAWL_RULES_IGNORE_CASE =
    Configuration.PREFIX + "plugin.crawlRulesIgnoreCase";
  static final boolean DEFAULT_CRAWL_RULES_IGNORE_CASE = true;

  /** If true, crawl rules implicitly include the start URLs and permission
   * URLs */
  static final String PARAM_CRAWL_RULES_INCLUDE_START =
    Configuration.PREFIX + "plugin.crawlRulesIncludeStartUrl";
  static final boolean DEFAULT_CRAWL_RULES_INCLUDE_START = true;

  static final int CRAWL_RULE_CONTAINS_SET_THRESHOLD = 12;

  public static final String PREFIX_NUMERIC = "numeric_";
  public static final String KEY_AU_NAME = "au_name";
  public static final String KEY_AU_CRAWL_RULES = "au_crawlrules";
  public static final String KEY_AU_CRAWL_RULES_IGNORE_CASE =
    "au_crawlrules_ignore_case";
  public static final String KEY_AU_CRAWL_WINDOW = "au_crawlwindow";
  public static final String KEY_AU_CRAWL_WINDOW_SER = "au_crawlwindow_ser";
  public static final String KEY_AU_EXPECTED_BASE_PATH = "au_expected_base_path";
  public static final String KEY_AU_REFETCH_DEPTH = "au_refetch_depth";
  public static final int DEFAULT_AU_REFETCH_DEPTH = 1;
  // The old name of au_refetch_depth
  public static final String KEY_AU_CRAWL_DEPTH_OBSOLESCENT = "au_crawl_depth";

  public static final String KEY_AU_PERMISSION_URL = "au_permission_url";
  // The old name of au_permission_url
  public static final String KEY_AU_MANIFEST_OBSOLESCENT = "au_manifest";

  public static final String KEY_AU_HTTP_COOKIES = "au_http_cookie";
  /** List of HTTP request headers <hdr>:<val> to be sent with each fetch */
  public static final String KEY_AU_HTTP_REQUEST_HEADERS =
    "au_http_request_header";

  //public static final String KEY_AU_URL_NORMALIZER = "au_url_normalizer";
  public static final String KEY_AU_EXPLODER_HELPER = "au_exploder_helper";
  public static final String KEY_AU_EXPLODER_PATTERN = "au_exploder_pattern";

  public static final String KEY_AU_EXCLUDE_URLS_FROM_POLLS_PATTERN =
    "au_exclude_urls_from_polls_pattern";
  public static final String KEY_AU_URL_POLL_RESULT_WEIGHT =
    "au_url_poll_result_weight";
  public static final String KEY_AU_SUBSTANCE_URL_PATTERN =
    "au_substance_url_pattern";
  public static final String KEY_AU_NON_SUBSTANCE_URL_PATTERN =
    "au_non_substance_url_pattern";
  public static final String KEY_AU_PERMITTED_HOST_PATTERN =
    "au_permitted_host_pattern";
  public static final String KEY_AU_REPAIR_FROM_PEER_IF_MISSING_URL_PATTERN =
    "au_repair_from_peer_if_missing_url_pattern";

  public static final String KEY_AU_URL_MIME_TYPE_OLDNAME = "au_url_mime_type";
  public static final String KEY_AU_URL_MIME_TYPE = "au_url_mime_type_map";
  public static final String KEY_AU_URL_MIME_VALIDATION =
    "au_url_mime_validation_map";

  public static final String KEY_AU_CRAWL_COOKIE_POLICY =
    "au_crawl_cookie_policy";
  public static final String KEY_AU_URL_RATE_LIMITER_MAP =
    "au_url_rate_limiter_map";
  public static final String KEY_AU_MIME_RATE_LIMITER_MAP =
    "au_mime_rate_limiter_map";
  public static final String KEY_AU_RATE_LIMITER_INFO =
    "au_rate_limiter_info";
  public static final String KEY_AU_ADDITIONAL_URL_STEMS =
    "au_additional_url_stems";


  /** Suffix for testing override submaps.  Values in a XXX_override map
   * will be copied to the main map when in testing mode XXX.  In the
   * presence of plugin inheritence, the first entry in (child-overrides,
   * child, parent-overrides, parent) wins.  */
  public static final String SUFFIX_OVERRIDE = "_override";

  public static final String SUFFIX_LINK_EXTRACTOR_FACTORY =
    "_link_extractor_factory";
  public static final String SUFFIX_FILTER_RULE = "_filter";
  // XXX _filter_factory should be changed to _hash_filter_factory but
  // plugins will have to be changed.  Note that this symbol is also used
  // in PdfUtil to refer to the PDF filter factory hint in the title DB;
  // either that will need to change or the title DB will.
  public static final String SUFFIX_HASH_FILTER_FACTORY = "_filter_factory";
  public static final String SUFFIX_CRAWL_FILTER_FACTORY =
    "_crawl_filter_factory";
  public static final String SUFFIX_LINK_REWRITER_FACTORY =
    "_link_rewriter_factory";
  public static final String SUFFIX_CONTENT_VALIDATOR_FACTORY =
    "_content_validator_factory";
  public static final String SUFFIX_ARTICLE_MIME_TYPE =
    "_article_mime_type";
  public static final String SUFFIX_METADATA_EXTRACTOR_FACTORY_MAP =
    "_metadata_extractor_factory_map"; 

  public static final String KEY_AU_PERMISSION_CHECKER_FACTORY =
    "au_permission_checker_factory";

  public static final String KEY_AU_PARAM_FUNCTOR = "au_param_functor";

  public static final String KEY_AU_LOGIN_PAGE_CHECKER =
    "au_login_page_checker";
  public static final String KEY_AU_REDIRECT_TO_LOGIN_URL_PATTERN =
    "au_redirect_to_login_url_pattern";
  public static final String KEY_DONT_POLL =
    "au_dont_poll";
  public static final String KEY_AU_FEATURE_URL_MAP = "au_feature_urls";

  public static final String AU_FEATURE_SELECTION_ATTR = "au_feature_key";
  public static final String AU_COVERAGE_DEPTH_ATTR = "au_coverage_depth";

  protected ExternalizableMap definitionMap;
  protected LoginPageChecker loginPageChecker;
  protected String cookiePolicy;
  protected int refetchDepth = -1;

  /** Context in which various printf templates are interpreted, for
   * argument type checking */
  static Map<String,PrintfConverter.PrintfContext> printfKeysContext =
    new HashMap<String,PrintfConverter.PrintfContext>();
  static {
    printfKeysContext.put(KEY_AU_START_URL, PrintfContext.URL);
    printfKeysContext.put(KEY_AU_PERMISSION_URL, PrintfContext.URL);
    printfKeysContext.put(KEY_AU_START_URL, PrintfContext.URL);
    printfKeysContext.put(KEY_AU_ADDITIONAL_URL_STEMS, PrintfContext.URL);

    printfKeysContext.put(KEY_AU_REDIRECT_TO_LOGIN_URL_PATTERN,
			  PrintfContext.Regexp);
    printfKeysContext.put(KEY_AU_CRAWL_RULES, PrintfContext.Regexp);
    printfKeysContext.put(KEY_AU_EXCLUDE_URLS_FROM_POLLS_PATTERN,
			  PrintfContext.Regexp);
    printfKeysContext.put(KEY_AU_SUBSTANCE_URL_PATTERN, PrintfContext.Regexp);
    printfKeysContext.put(KEY_AU_NON_SUBSTANCE_URL_PATTERN,
			  PrintfContext.Regexp);
    printfKeysContext.put(KEY_AU_PERMITTED_HOST_PATTERN,
			  PrintfContext.Regexp);
    printfKeysContext.put(KEY_AU_REPAIR_FROM_PEER_IF_MISSING_URL_PATTERN,
			  PrintfContext.Regexp);
    printfKeysContext.put(KEY_AU_NAME, PrintfContext.Display);
    // XXX These may use params supplied by OpenUrlResolver, need to inject
    // that list here in order to check
//     printfKeysContext.put(KEY_AU_FEATURE_URL_MAP,
// 			  PrintfContext.URL);
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

  public Collection<String> getPermissionUrls() {
    Collection<String> res = convertUrlListList(KEY_AU_PERMISSION_URL);
    if (res == null) {
      res = super.getPermissionUrls();
    }
    return res;
  }

  public String getPerHostPermissionPath() {
    return (String)definitionMap.getMapElement(DefinablePlugin.KEY_PER_HOST_PERMISSION_PATH);
  }

  public List<String> getHttpCookies() {
    return listOrEmpty(getElementList(KEY_AU_HTTP_COOKIES));
  }

  public List<String> getHttpRequestHeaders() {
    return listOrEmpty(getElementList(KEY_AU_HTTP_REQUEST_HEADERS));
  }

  /** Use rate limiter source specified in AU, if any, then in plugin, then
   * default */
  @Override
  protected String getFetchRateLimiterSource() {
    String defaultSource =
      CurrentConfig.getParam(PARAM_DEFAULT_FETCH_RATE_LIMITER_SOURCE,
			     DEFAULT_DEFAULT_FETCH_RATE_LIMITER_SOURCE);
    String pluginSrc = 
      definitionMap.getString(DefinablePlugin.KEY_PLUGIN_FETCH_RATE_LIMITER_SOURCE,
			      defaultSource);
    String auSrc =
      paramMap.getString(KEY_AU_FETCH_RATE_LIMITER_SOURCE, pluginSrc);
    return CurrentConfig.getParam(PARAM_OVERRIDE_FETCH_RATE_LIMITER_SOURCE,
				  auSrc);
  }

  @Override
  public RateLimiterInfo getRateLimiterInfo() {
    if (definitionMap.containsKey(KEY_AU_RATE_LIMITER_INFO)) {
      // If the plugin contains an explicit RateLimiterInfo, use it.  Add
      // the CrawlPoolKey if necessary..
      RateLimiterInfo rli =
	(RateLimiterInfo)definitionMap.getMapElement(KEY_AU_RATE_LIMITER_INFO);
      if (rli.getCrawlPoolKey() == null) {
	rli.setCrawlPoolKey(getFetchRateLimiterKey());
      }
      return rli;
    }
    RateLimiterInfo rli = super.getRateLimiterInfo();
    Map<String,String> patterns =
      definitionMap.getMap(KEY_AU_URL_RATE_LIMITER_MAP, null);
    if (patterns != null) {
      rli.setUrlRates(patterns);
    }
    Map<String,String> mimes =
      definitionMap.getMap(KEY_AU_MIME_RATE_LIMITER_MAP, null);
    if (mimes != null) {
      rli.setMimeRates(mimes);
    }
    return rli;
  }

  @Override
  public Collection<String> getStartUrls() {
    List<String> res = convertUrlListList(KEY_AU_START_URL);
    log.debug2("Setting start urls " + res);
    if(res == null) {
      res = Collections.emptyList();
    }
    return res;
  }

  @Override
  protected Collection<String> getAdditionalUrlStems()
      throws MalformedURLException {
    List<String> res = convertUrlListList(KEY_AU_ADDITIONAL_URL_STEMS);
    log.debug2("Setting additional urlStems " + res);
    if(res == null) {
      res = Collections.emptyList();
    }
    return UrlUtil.getUrlPrefixes(res);
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
    // If/when these are used, don't waste map entries on default values
//     long l_val;
//     l_val = definitionMap.getLong(KEY_AU_MAX_SIZE,
//                                   DEFAULT_AU_MAX_SIZE);
//     paramMap.putLong(KEY_AU_MAX_SIZE, l_val);

//     l_val = definitionMap.getLong(KEY_AU_MAX_FILE_SIZE,
//                                   DEFAULT_AU_MAX_FILE_SIZE);
//     paramMap.putLong(KEY_AU_MAX_FILE_SIZE, l_val);

  }

  protected void addImpliedConfigParams()
      throws ArchivalUnit.ConfigurationException {
    super.addImpliedConfigParams();
    String umsg =
      definitionMap.getString(DefinablePlugin.KEY_PLUGIN_AU_CONFIG_USER_MSG,
			      null);
    if (umsg != null) {
      paramMap.putString(KEY_AU_CONFIG_USER_MSG, makeConfigUserMsg(umsg));
    }
  }

  protected String makeConfigUserMsg(String fmt) {
    String res = convertNameString(fmt);
    if (fmt.equals(res)) return fmt;
    return res;
  }

  public List<Pattern> makeExcludeUrlsFromPollsPatterns()
      throws ArchivalUnit.ConfigurationException {
    return compileRegexpList(KEY_AU_EXCLUDE_URLS_FROM_POLLS_PATTERN,
			     RegexpContext.Url);
  }

  public PatternFloatMap makeUrlPollResultWeightMap()
      throws ArchivalUnit.ConfigurationException {
    List<String> resultWeightSpec =
      getElementList(KEY_AU_URL_POLL_RESULT_WEIGHT, null);
    if (resultWeightSpec != null) {
      List<String> lst = new ArrayList<String>();
      for (String pair : resultWeightSpec) {
	// Separate printf from priority, process printf, reassemble for
	// PatternFloatMap

	// Find the last occurrence of comma to avoid regexp quoting
	int pos = pair.lastIndexOf(',');
	if (pos < 0) {
	  throw new IllegalArgumentException("Malformed pattern,float pair; no comma: "
					     + pair);
	}
	String printf = pair.substring(0, pos);
	String weight = pair.substring(pos + 1);
	try {
	  float w = Float.parseFloat(weight);
	  if (w < 0.0 || w > 1.0) {
	    throw new IllegalArgumentException("Illegal URL result weight, must be between 0.0 and 1.0: " + weight);
	  }
	} catch (NumberFormatException e) {
	  throw new IllegalArgumentException("Illegal URL result weight: " +
					     weight);
	}
	PrintfConverter.MatchPattern mp =
	  convertVariableRegexpString(printf, RegexpContext.Url);
	if (mp.getRegexp() != null) {
	  lst.add(mp.getRegexp() + "," + weight);
	}
      }
      return PatternFloatMap.fromSpec(lst);
    } else {
      return null;
    }
  }

  public AuCacheResultMap makeAuCacheResultMap()
      throws ArchivalUnit.ConfigurationException {
    // Union the entries in the plugin's plugin_cache_result_list with
    // the au_redirect_to_login_url_pattern, if any
    List<Pair<String,String>> redirList =
      new ArrayList<>(getDefinablePlugin().getResultMapEntries());
    // the list
    String redirToLoginPat =
      (String)definitionMap.getMapElement(KEY_AU_REDIRECT_TO_LOGIN_URL_PATTERN);
    if (!StringUtil.isNullString(redirToLoginPat)) {
      redirList.add(ImmutablePair.of("redir:" + redirToLoginPat,
                                     "org.lockss.util.urlconn.CacheException$RedirectToLoginPageException"));
    }

    // Now process all the "redir:" entries into a PatternObjectMap
    List<Pair<String,Object>> objPats = new ArrayList<>();
    for (Pair<String,String> pair : redirList) {
      // Process only redir: patterns
      java.util.regex.Matcher m = DefinablePlugin.RESULT_MAP_REDIR_PAT.matcher(pair.getLeft());
      if (m.matches()) {
        Object val = getDefinablePlugin().parseResultMapRhs(pair.getRight());
        String patStr =
          convertVariableRegexpString(m.group(1),
                                      RegexpContext.Url).getRegexp();
        objPats.add(ImmutablePair.of(patStr, val));
      }
    }
    // And make an AuHttpResultMap from that and the plugin's HttpResultMap.
    return new AuHttpResultMap(plugin.getCacheResultMap(),
                               PatternObjectMap.fromPairs(objPats));
  }

  private Object parseRedirPatRhs(String rhs) {
    try {
      return getDefinablePlugin().parseResultMapRhs(rhs);
    } catch (PluginException.InvalidDefinition e) {
      throw new IllegalArgumentException("Illegal redir action: " + rhs);
    }
  }

  @Override
  public PatternStringMap makeUrlMimeTypeMap() {
    if (definitionMap.containsKey(KEY_AU_URL_MIME_TYPE)) {
      return makeUrlStringMap(getElementList(KEY_AU_URL_MIME_TYPE, null));
    } else {
      return makeUrlStringMap(getElementList(KEY_AU_URL_MIME_TYPE_OLDNAME,
					     null));
    }
  }

  @Override
  public PatternStringMap makeUrlMimeValidationMap() {
    return makeUrlStringMap(getElementList(KEY_AU_URL_MIME_VALIDATION, null));
  }

  public PatternStringMap makeUrlStringMap(List<String> spec) {
    if (spec != null) {
      List<String> lst = new ArrayList<String>();
      for (String pair : spec) {
	// Separate printf from value string, process printf, reassemble for
	// PatternStringMap

	// Find the last occurrence of comma to avoid regexp quoting
	int pos = pair.lastIndexOf(',');
	if (pos < 0) {
	  throw new IllegalArgumentException("Malformed pattern,string pair; no comma: "
					     + pair);
	}
	String printf = pair.substring(0, pos);
	String val = pair.substring(pos + 1).trim();
	PrintfConverter.MatchPattern mp =
	  convertVariableRegexpString(printf, RegexpContext.Url);
	if (mp.getRegexp() != null) {
	  lst.add(mp.getRegexp() + "," + val);
	}
      }
      return new PatternStringMap(lst);
    } else {
      return PatternStringMap.EMPTY;
    }
  }

  public List<Pattern> makeNonSubstanceUrlPatterns()
      throws ArchivalUnit.ConfigurationException {
    return compileRegexpList(KEY_AU_NON_SUBSTANCE_URL_PATTERN,
			     AuUtil.getTitleAttribute(this,
						      AU_COVERAGE_DEPTH_ATTR),
			     RegexpContext.Url);
  }

  public List<Pattern> makeSubstanceUrlPatterns()
      throws ArchivalUnit.ConfigurationException {
    return compileRegexpList(KEY_AU_SUBSTANCE_URL_PATTERN,
			     AuUtil.getTitleAttribute(this,
						      AU_COVERAGE_DEPTH_ATTR),
			     RegexpContext.Url);
  }
  
  public List<Pattern> makePermittedHostPatterns()
      throws ArchivalUnit.ConfigurationException {
    return compileRegexpList(KEY_AU_PERMITTED_HOST_PATTERN,
			     RegexpContext.Url);
  }

  public List<Pattern> makeRepairFromPeerIfMissingUrlPatterns()
      throws ArchivalUnit.ConfigurationException {
    return compileRegexpList(KEY_AU_REPAIR_FROM_PEER_IF_MISSING_URL_PATTERN,
			     RegexpContext.Url);
  }

  public String getExploderPattern() {
    return definitionMap.getString(KEY_AU_EXPLODER_PATTERN,
        null);
  }
  
  public ExploderHelper getExploderHelper() {
    return getDefinablePlugin().getExploderHelper();
  }

  public SubstancePredicate makeSubstancePredicate()
      throws ArchivalUnit.ConfigurationException, PluginException.LinkageError {
    SubstancePredicateFactory fact =
      getDefinablePlugin().getSubstancePredicateFactory();
    if (fact == null) {
      return null;
    }
    return fact.makeSubstancePredicate(this);
  }

  Map<String,List<String>> featureUrlMap;

  /** Return URLs expanded from au_feature_urls.
   *
   * XXX This is unused.  OpenUrlResolver processes au_feature_urls
   * directly, as those printfs refer to params other than AU config
   * params and can't be processed in the normal way. */
  public List<String> getAuFeatureUrls(String auFeature) {
    if (featureUrlMap == null) {
      featureUrlMap = makeAuFeatureUrlMap();
    }
    return listOrEmpty(featureUrlMap.get(auFeature));
  }

  protected Map<String,List<String>> makeAuFeatureUrlMap() {
    Map<String,?> plugFeatureUrlMap =
      definitionMap.getMap(KEY_AU_FEATURE_URL_MAP, null);
    if (plugFeatureUrlMap == null) {
      return Collections.emptyMap();
    } else {
      Map<String,List<String>> res = new HashMap<String,List<String>>();
      for (Map.Entry<String,?> ent : plugFeatureUrlMap.entrySet()) {
	String featKey = ent.getKey();
	Object val = ent.getValue();
	res.put(StringPool.TDBAU_ATTRS.intern(featKey),
		convertFeatureUrlVal(val));
      }
      return res;
    }
  }

  private List<String> convertFeatureUrlVal(Object val) {
    if (val instanceof String) {
      return convertUrlList((String)val);
    } else if (val instanceof List) {
      return convertUrlList((List)val, KEY_AU_FEATURE_URL_MAP);
    } else if (val instanceof Map) {
      TitleConfig tc = getTitleConfig();
      String selkey = (tc != null)
	? AuUtil.getTitleAttribute(this, AU_FEATURE_SELECTION_ATTR)
	: null;
      if (selkey == null) {
	selkey = "*";
      }
      Object selval = ((Map)val).get(selkey);
      if (selval == null && !selkey.equals("*")) {
	selval = ((Map)val).get("*");
      }
      if (selval == null) {
	return null;
      }
      return convertFeatureUrlVal(selval);
    }
    log.warning("Unknown feature URL datatype ("
		+ StringUtil.shortName(val.getClass())
		+ "): " + val);
    return null;
  }


  List<Pattern> compileRegexpList(String key, RegexpContext context)
      throws ArchivalUnit.ConfigurationException {
    List<String> lst = convertRegexpList(key, context);
    if (lst == null) {
      return null;
    }
    return compileRegexpList(lst, key);
  }

  List<Pattern> compileRegexpList(String key, String mapkey,
				  RegexpContext context)
      throws ArchivalUnit.ConfigurationException {
    List<String> pats = getElementList(key, mapkey);
    return compileRegexpList(convertRegexpList(pats, key, RegexpContext.Url),
			     key);
  }

  List<Pattern> compileRegexpList(List<String> regexps, String key)
      throws ArchivalUnit.ConfigurationException {
    if (regexps == null) {
      return null;
    }
    List<Pattern> res = new ArrayList<Pattern>(regexps.size());
    Perl5Compiler comp = RegexpUtil.getCompiler();
    int flags = Perl5Compiler.READ_ONLY_MASK;
    if (isCaseIndependentCrawlRules()) {
      flags += Perl5Compiler.CASE_INSENSITIVE_MASK;
    }
    for (String re : regexps) {
      try {
	res.add(comp.compile(re, flags));
      } catch (MalformedPatternException e) {
	String msg = "Can't compile URL pattern: " + re;
	if (key != null) {
	  msg = "In " + key + ": " + msg;
	}
	log.error(msg + ": " + e.toString());
	throw new ArchivalUnit.ConfigurationException(msg, e);
      }
    }
    return res;
  }

  @Deprecated
  public boolean isLoginPageUrl(String url) {
    try {
      Object rhs = makeAuCacheResultMap().mapUrl(this, null, null, url);
      return rhs instanceof CacheException.RedirectToLoginPageException;
    } catch (ConfigurationException e) {
      log.warning("Error checking login URL: " + url, e);
      return false;
    }
  }    

  protected String makeName() {
    String namestr = definitionMap.getString(KEY_AU_NAME, "");
    String name = convertNameString(namestr);
    log.debug2("setting name string: " + name);
    return name;
  }

  protected CrawlRule makeRule() throws ConfigurationException {
    CrawlRule rule;
    try {
      rule = makeRule0();
    } catch(LockssRegexpException e) {
      throw new ConfigurationException("Illegal regexp in crawl rules: " +
          e.getMessage(), e);
    }
    if (rule == null
	|| !CurrentConfig.getBooleanParam(PARAM_CRAWL_RULES_INCLUDE_START,
					  DEFAULT_CRAWL_RULES_INCLUDE_START)) {
      return rule;
    }
    // If any of the the start URLs or permission URLs aren't otherwise
    // included in the crawl rule, add them explicitly, by wrapping the
    // rule in one that first checks the start and permission URLs.

    Collection<String> expUrls = new HashSet<String>();

    Collection<String> perms = getPermissionUrls();
    if (perms != null) {
      for (String url : perms) {
	if (rule.match(url) != CrawlRule.INCLUDE) {
	  expUrls.add(url);
	}
      }
    }
    Collection<String> starts = getStartUrls();
    if (starts != null) {
      for (String url : starts) {
	if (rule.match(url) != CrawlRule.INCLUDE) {
	  expUrls.add(url);
	}
      }
    }
    if (expUrls.isEmpty()) {
      return rule;
    } else {
      if (expUrls.size() < CRAWL_RULE_CONTAINS_SET_THRESHOLD) {
	expUrls = new ArrayList(expUrls);
      }
      // Must check the explicit list first, even though it will hardly
      // ever match, as main rule could return EXCLUDE
      return new CrawlRules.FirstMatch(ListUtil.list(new CrawlRules.Contains(expUrls),
						     rule));

    }
  }

  boolean isCaseIndependentCrawlRules() {
    boolean defaultIgnoreCase =
      CurrentConfig.getBooleanParam(PARAM_CRAWL_RULES_IGNORE_CASE,
				    DEFAULT_CRAWL_RULES_IGNORE_CASE);
    return definitionMap.getBoolean(KEY_AU_CRAWL_RULES_IGNORE_CASE,
				    defaultIgnoreCase);
  }

  CrawlRule makeRule0() throws LockssRegexpException {
    Object rule = definitionMap.getMapElement(KEY_AU_CRAWL_RULES);

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
	CrawlRule cr = convertRule(rule_template,
				   isCaseIndependentCrawlRules());
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

  public LoginPageChecker getLoginPageChecker() {
    if(loginPageChecker == null) {
      loginPageChecker = getDefinablePlugin().makeLoginPageChecker();
    }
    return loginPageChecker;
  }
  
  public String getCookiePolicy() {
    if(cookiePolicy == null) {
      cookiePolicy = definitionMap.getString(KEY_AU_CRAWL_COOKIE_POLICY, null);
    }
    return cookiePolicy;
  }
  
  public int getRefetchDepth() {
    if(refetchDepth == -1) {
      refetchDepth = definitionMap.getInt(KEY_AU_REFETCH_DEPTH,
          DEFAULT_AU_REFETCH_DEPTH);
    }
    return refetchDepth;
  }
  
  @Override
  public UrlConsumerFactory getUrlConsumerFactory() {
    if(getExploderPattern() != null) {
      return new ExplodingUrlConsumerFactory();
    }
    return getDefinablePlugin().getUrlConsumerFactory();
  }
  
  @Override
  public UrlFetcher makeUrlFetcher(CrawlerFacade facade, String url) {
    return getDefinablePlugin().makeUrlFetcher(facade, url);
  }
  
  @Override
  public CrawlSeed makeCrawlSeed(Crawler.CrawlerFacade crawlFacade) {
    CrawlSeed ret;
    ret = getDefinablePlugin().getCrawlSeed(crawlFacade);
    if(ret == null) {
      ret = new BaseCrawlSeed(this);
    }
    return ret;
  }

  @Override
  public Collection<String> getAccessUrls() {
    FeatureUrlHelper helper = getDefinablePlugin().getFeatureUrlHelper(this);
    if (helper != null) {
      try {
	return helper.getAccessUrls(this);
      } catch (PluginException | IOException e) {
	log.warning("Error generating access URLs, using start URLs instead",
		    e);
	return getStartUrls();
      }
    } else {
      return super.getAccessUrls();
    }
  }

  public List<PermissionChecker> makePermissionCheckers() {
    PermissionCheckerFactory fact =
      getDefinablePlugin().getPermissionCheckerFactory();
    if (fact != null) {
      try {
    	List<PermissionChecker> permissionCheckers = fact.createPermissionCheckers(this);
    	if(!permissionCheckers.isEmpty()) {
    		return permissionCheckers;
    	}
      } catch (PluginException e) {
	throw new RuntimeException(e);
      }
    }
    return null;
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

  protected List<String> convertUrlListList(String key) {
    return convertUrlList(getElementList(key), key);
  }

  protected List<String> convertRegexpList(String key, RegexpContext context) {
    return convertRegexpList(getElementList(key), key, context);
  }

  protected <T> List<T> listOrEmpty(List<T> lst) {
    return lst != null ? lst : Collections.EMPTY_LIST;
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

  PrintfConverter.MatchPattern
    convertVariableRegexpString(String printfString, RegexpContext context) {
    return PrintfConverter.newRegexpConverter(plugin, paramMap, context).getMatchPattern(printfString);
  }

  List<String> convertUrlList(String printfString) {
    return PrintfConverter.newUrlListConverter(plugin, paramMap).getUrlList(printfString);
  }

  String convertNameString(String printfString) {
    return PrintfConverter.newNameConverter(plugin, paramMap).getName(printfString);
  }

  protected List<String> convertUrlList(List<String> printfStrings, String key) {
    if (printfStrings == null) {
      return null;
    }
    // Just a guess; each printf may generate more than one URL
    ArrayList<String> res = new ArrayList<String>(printfStrings.size());
    for (String pattern : printfStrings) {
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

  protected List<String> convertRegexpList(List<String> printfStrings,
					   String key,
					   RegexpContext context) {
    if (printfStrings == null) {
      return null;
    }
    ArrayList<String> res = new ArrayList<String>(printfStrings.size());
    for (String pattern : printfStrings) {
      if (StringUtil.isNullString(pattern)) {
	log.warning("Null pattern string in " + key);
	continue;
      }
      String pat =
	convertVariableRegexpString(pattern, context).getRegexp();
      if (pat == null) {
	log.warning("Null converted regexp in " + key + ", from " + pattern);
	continue;
      }
      res.add(pat);
    }
    return res;
  }

  protected List<String> getElementList(String key) {
    return getDefinablePlugin().getElementList(key);
  }

  protected List<String> getElementList(String key, String mapkey) {
    return getDefinablePlugin().getElementList(key, mapkey);
  }

  CrawlRule convertRule(String ruleString, boolean ignoreCase)
      throws LockssRegexpException {

    int pos = ruleString.indexOf(",");
    int action = Integer.parseInt(ruleString.substring(0, pos));
    String printfString = ruleString.substring(pos + 1);

    PrintfConverter.MatchPattern mp =
      convertVariableRegexpString(printfString, RegexpContext.Url);
    if (mp.getRegexp() == null) {
      return null;
    }
    List<List> matchArgs = mp.getMatchArgs();
    switch (matchArgs.size()) {
    case 0:
      return new CrawlRules.RE(mp.getRegexp(), ignoreCase, action);
    case 1:
      List argPair = matchArgs.get(0);
      AuParamType ptype = mp.getMatchArgTypes().get(0);
      switch (ptype) {
      case Range:
	return new CrawlRules.REMatchRange(mp.getRegexp(),
					   ignoreCase,
					   action,
					   (String)argPair.get(0),
					   (String)argPair.get(1));
      case NumRange:
	return new CrawlRules.REMatchRange(mp.getRegexp(),
					   ignoreCase,
					   action,
					   ((Long)argPair.get(0)).longValue(),
					   ((Long)argPair.get(1)).longValue());
      default:
	throw new RuntimeException("Shouldn't happen.  Unknown REMatchRange arg type: " + ptype);
      }

    default:
      throw new LockssRegexpException("Multiple range args not yet supported");
    }
  }


  public interface ConfigurableCrawlWindow {
    public CrawlWindow makeCrawlWindow()
	throws PluginException;
  }

}
