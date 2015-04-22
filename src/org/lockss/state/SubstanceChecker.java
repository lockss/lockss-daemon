/*
 * $Id$
 */

/*

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.state;

import java.util.*;
import java.net.*;
import java.io.*;
import org.apache.oro.text.regex.*;

import org.lockss.util.*;
import org.lockss.util.urlconn.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.state.*;
import org.lockss.hasher.*;
import org.lockss.filter.*;
import org.lockss.extractor.*;
import org.lockss.alert.Alert;

/** Logic to maintain substance state during a scan of URLs in an AU.
 * Typically used during a crawl or vote.
 */
public class SubstanceChecker {
  static Logger log = Logger.getLogger("SubstanceChecker");

  public static final String PREFIX = Configuration.PREFIX +
    "substanceChecker.";

  public static String CONTEXT_CRAWL = "Crawl";
  public static String CONTEXT_VOTE = "Vote";
  public static String CONTEXT_POLL = "Poll";
  public static String CONTEXT_ALL = "All";

  /** Specifies whether and when detection of AUs with no meaningful
   * content ("substance") is enabled.  Performed only for AUs whose plugin
   * specifies patterns for substance or non-substance URLs.  Can be used
   * to generate an alert and to prevent AUs that have collected only
   * non-substance files (manifest, front-matter, boilerplate, logos, etc.)
   * from voting or appearing to contain content.  This can result from
   * site changes that move all the substance outside of the crawl rules,
   * so is useful to detect unanticipated site changes.
   *
   * <dl><lh>Set to one of::</lh>
   *
   * <dt>None</dt><dd>(The default.) Detection of no-substance AUs is
   * disabled.</dd>
   *
   * <dt>Crawl</dt><dd>Detection of no-substance AUs is performed (on the
   * entire content of the AU) during crawls.
   * <code>org.lockss.crawler.reparseAll</code> must be true for this
   * mode.</dd>
   *
   * <dt>Vote</dt><dd>Detection of no-substance AUs is performed when
   * computing a vote.  This allows existing no-substance AUs to be
   * detected even if they were created before this feature was added.</dd>
   *
   * <dt>All</dt><dd>Detection of no-substance AUs is enabled in
   * all the above contexts.</dd>
   *
   * </dl>
   */
  public static final String PARAM_DETECT_NO_SUBSTANCE_MODE =
    PREFIX + "detectNoSubstanceMode";
  public static final String DEFAULT_DETECT_NO_SUBSTANCE_MODE = "All";

  /** State is Unknown iff the AU has no substance patterns, otherwise No
   * if no substance URL has been detected so far, else Yes */
  public enum State {Unknown, Yes, No};

  enum NoSubstanceRedirectUrl {First, Last, All};

  /** If no-substance detection is enabled, determines which URL(s) in a
   * redirect chain are tested for substance-ness.
   *
   * <dl><lh>Set to one of::</lh>
   *
   * <dt>First</dt><dd>The first URL (the only directly linked to) is
   * tested.</dd>
   *
   * <dt>Last</dt><dd>The last URL (the one that actually retrieved a file)
   * is tested.</dd>
   *
   * <dt>All</dt><dd>All URLs in the chain are tested.</dd>
   *
   * </dl>
   */
  public static final String PARAM_DETECT_NO_SUBSTANCE_REDIRECT_URL =
    PREFIX + "detectNoSubstanceRedirectUrl";
  public static final NoSubstanceRedirectUrl
    DEFAULT_DETECT_NO_SUBSTANCE_REDIRECT_URL = NoSubstanceRedirectUrl.Last;

  static final SubstancePredicateFactory DEFAULT_FACT =
    new UrlPredicateFactory();

  protected String enabledContexts = DEFAULT_DETECT_NO_SUBSTANCE_MODE;

  protected ArchivalUnit au;
  protected SubstancePredicate substancePred;
  protected State hasSubstance = State.Unknown;
  protected int substanceCnt = 0;
  protected int substanceMin = -1;
  protected NoSubstanceRedirectUrl detectNoSubstanceRedirectUrl =
    DEFAULT_DETECT_NO_SUBSTANCE_REDIRECT_URL;

  public SubstanceChecker(ArchivalUnit au) {
    this(au, ConfigManager.getCurrentConfig());
  }

  public SubstanceChecker(ArchivalUnit au, Configuration config) {
    this.au = au;
    setConfig(config);
    try {
      substancePred = au.makeSubstancePredicate();
      if (substancePred == null) {
	substancePred = DEFAULT_FACT.makeSubstancePredicate(au);
      }
      if (substancePred != null) {
	hasSubstance = State.No;
      } else {
	hasSubstance = State.Unknown;
      }
    } catch (ArchivalUnit.ConfigurationException e) {
      log.error("Error in substance or non-substance pattern, disabling substance checking", e);
      hasSubstance = State.Unknown;
    } catch (PluginException.LinkageError e) {
      log.error("Error creating SubstancePredicate, disabling substance checking", e);
      hasSubstance = State.Unknown;
    }
  }

  void setConfig(Configuration config) {
    enabledContexts = config.get(PARAM_DETECT_NO_SUBSTANCE_MODE,
				 DEFAULT_DETECT_NO_SUBSTANCE_MODE);
    detectNoSubstanceRedirectUrl =
      (NoSubstanceRedirectUrl)
      config.getEnum(NoSubstanceRedirectUrl.class,
		     PARAM_DETECT_NO_SUBSTANCE_REDIRECT_URL,
		     DEFAULT_DETECT_NO_SUBSTANCE_REDIRECT_URL);
  }

  /** If called, puts SubstanceChecker into counting mode, where it counts
   * up to the specified minimum then stops checking */
  public void setSubstanceMin(int min) {
    substanceMin = min;
  }

  public boolean isEnabledFor(String context) {
    if (substancePred == null) {
      log.debug3("isEnabledFor(" + context + "): false, no predicate");
      return false;
    }
    boolean res = enabledContexts.equalsIgnoreCase(CONTEXT_ALL)
      || StringUtil.indexOfIgnoreCase(enabledContexts, context) >= 0;
    log.debug("isEnabledFor(" + context + "): " + res);
    return res;
  }

  // return true if there's no reason to check any more URLs
  private boolean isStateFullyDetermined() {
    switch (hasSubstance) {
    case No:
    case Unknown:
      return false;
    case Yes:
      // if already established has substance, continue checking only until
      // reach threshold
      return (substanceMin <= 0) ? true : substanceCnt >= substanceMin;
    }
    throw new ShouldNotHappenException();
  }

  /** If substance not detected yet, check whether this CachedUrl contains
   * substance.  Releasing the CachedUrl is the responsibility of the
   * caller. */
  public void checkSubstance(CachedUrl cu) {
    if (isStateFullyDetermined()) {
      return;
    }
    Properties props;
    switch (detectNoSubstanceRedirectUrl) {
    case First:
      checkSubstanceUrl(cu.getUrl());
      break;
    case Last:
      props = cu.getProperties();
      String url = props.getProperty(CachedUrl.PROPERTY_CONTENT_URL);
      if (url == null) {
	url = cu.getUrl();
      }
      checkSubstanceUrl(url);
      break;
    case All:
      props = cu.getProperties();
      String redirUrl = props.getProperty(CachedUrl.PROPERTY_REDIRECTED_TO);
      if (redirUrl == null) {
	checkSubstanceUrl(cu.getUrl());
      } else {
	List<String> urls = new ArrayList<String>();
	do {
	  urls.add(redirUrl);
	  CachedUrl redirCu = au.makeCachedUrl(redirUrl);
	  if (redirCu == null) {
	    break;
	  }
	  Properties redirProps = redirCu.getProperties();
	  redirUrl = redirProps.getProperty(CachedUrl.PROPERTY_REDIRECTED_TO);
	} while (redirUrl != null);

	for (String u : urls) {
	  checkSubstanceUrl(u);
	}
      }
      break;
    }
  }

  /** If substance not detected yet, check whether this URL is a substance
   * URL */
  public void checkSubstance(String url) {
    if (isStateFullyDetermined()) {
      return;
    }
    checkSubstanceUrl(url);
  }

  /** Iterate through AU until substance found. */ 
  public void findSubstance() {
    if (isStateFullyDetermined()) {
      log.debug("findSubstance() already known");
      return;
    }
    log.debug("findSubstance() searching");
    for (CachedUrl cu : au.getAuCachedUrlSet().getCuIterable()) {
      checkSubstance(cu);
      if (isStateFullyDetermined()) {
	return;
      }
    }
  }

  private void foundSubstanceUrl(String url) {
    switch (hasSubstance) {
    case No:
    case Unknown:
      log.debug("First substance found: " + url);
      break;
    default:
    }
    hasSubstance = State.Yes;
    substanceCnt++;
  }

  private void checkSubstanceUrl(String url) {
    if (isSubstanceUrl(url)) {
      foundSubstanceUrl(url);
    }
  }

  public boolean isSubstanceUrl(String url) {
    return substancePred.isSubstanceUrl(url);
  }

  public State hasSubstance() {
    return hasSubstance;
  }

  public int getSubstanceCnt() {
    return substanceCnt;
  }

  public int getSubstanceMin() {
    return substanceMin;
  }

  public void setHasSubstance(State state) {
    hasSubstance = state;
  }

  /** A SubstancePredicate that matches URLs against plugin-supplied regexp
   * lists: <code>au_substance_url_pattern</code> or
   * <code>au_non_substance_url_pattern</code> */
  public static class UrlPredicate implements SubstancePredicate {
    protected ArchivalUnit au;
    protected List<Pattern> substancePats = null;
    protected List<Pattern> nonSubstancePats = null;
    protected Set<String> additionalNonSubstanceUrls = null;
    protected Perl5Matcher matcher = new Perl5Matcher();

    public UrlPredicate(ArchivalUnit au, List<Pattern> substancePats,
			List<Pattern> nonSubstancePats) {
      this.au = au;
      this.substancePats = substancePats;
      this.nonSubstancePats = nonSubstancePats;
      if (nonSubstancePats != null || substancePats != null) {
	if (nonSubstancePats != null) {
	  additionalNonSubstanceUrls = getAdditionalNonSubstanceUrls();
	}
      }
    }

    public boolean isSubstanceUrl(String url) {
      if (substancePats != null) {
	if (isMatchSubstancePat(url)) {
	  if (log.isDebug3()) {
	    log.debug3("checkSubstanceUrl(" + url + ") matched substance");
	  }
	  return true;
	} else {
	  if (log.isDebug3()) {
	    log.debug3("checkSubstanceUrl(" + url + ") does not match substance");
	  }
	}
      }
      if (nonSubstancePats != null) {
	if (!isMatchNonSubstancePat(url)) {
	  if (log.isDebug3()) {
	    log.debug3("checkSubstanceUrl(" + url + ") matched non-substance");
	  }
	  return true;
	} else {
	  if (log.isDebug3()) {
	    log.debug3("checkSubstanceUrl(" + url + ") does not match non-substance");
	  }
	}
      }
      return false;
    }

    public boolean isMatchSubstancePat(String url) {
      return substancePats != null && isMatch(url, substancePats);
    }

    public boolean isMatchNonSubstancePat(String url) {
      return (nonSubstancePats != null && isMatch(url, nonSubstancePats)) ||
	(additionalNonSubstanceUrls != null &&
	 additionalNonSubstanceUrls.contains(url));
    }

    boolean isMatch(String url, List<Pattern> pats) {
      for (Pattern pat : pats) {
	if (matcher.contains(url, pat)) {
	  if (log.isDebug3()) {
	    log.debug3("checkSubstanceUrl(" + url + ") matches "
		       + RegexpUtil.regexpCollection(pats));
	  }
	  return true;
	}
      }
      if (log.isDebug3()) {
	log.debug3("checkSubstanceUrl(" + url + ") no match "
		   + RegexpUtil.regexpCollection(pats));
      }
      return false;
    }

    private Set<String> getAdditionalNonSubstanceUrls() {
      Set<String> res = new HashSet<String>();
      addImplicitUrls(au.getPermissionUrls(), res);
      addImplicitUrls(au.getStartUrls(), res);
      return res;
    }

    private void addImplicitUrls(Collection<String> urls, Set<String> to) {
      for (String url : urls) {
	if (!isMatch(url, nonSubstancePats)) {
	  to.add(url);
	}
      }
    }
  }

  /** Factory that creates a {@link UrlPredicate} for the AU */
  public static class UrlPredicateFactory implements SubstancePredicateFactory {
    public SubstancePredicate makeSubstancePredicate(ArchivalUnit au) {
      try {
	List<Pattern> substancePats = au.makeSubstanceUrlPatterns();
	List<Pattern> nonSubstancePats = au.makeNonSubstanceUrlPatterns();
	if (nonSubstancePats != null || substancePats != null) {
	  return new UrlPredicate(au, substancePats, nonSubstancePats);
	}
	return null;
      } catch (ArchivalUnit.ConfigurationException e) {
	log.error("Error in substance or non-substance pattern, disabling substance URL checking", e);
	return null;
      }
    }
  }
}
