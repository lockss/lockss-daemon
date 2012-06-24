/*
 * $Id: SubstanceChecker.java,v 1.4 2012-06-24 19:39:51 pgust Exp $
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
import org.apache.commons.collections.map.LRUMap;
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

/** 
 */
public class SubstanceChecker {
  static Logger log = Logger.getLogger("SubstanceChecker");

  public static final String PREFIX = Configuration.PREFIX +
    "substanceChecker.";

  public static String CONTEXT_CRAWL = "Crawl";
  public static String CONTEXT_VOTE = "Vote";
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

  protected String enabledContexts = DEFAULT_DETECT_NO_SUBSTANCE_MODE;

  protected ArchivalUnit au;
  protected List<Pattern> substancePats = null;
  protected List<Pattern> nonSubstancePats = null;
  protected Set<String> additionalNonSubstanceUrls = null;
  protected State hasSubstance = State.Unknown;
  protected Perl5Matcher matcher = new Perl5Matcher();
  protected NoSubstanceRedirectUrl detectNoSubstanceRedirectUrl =
    DEFAULT_DETECT_NO_SUBSTANCE_REDIRECT_URL;

  public SubstanceChecker(ArchivalUnit au) {
    this(au, ConfigManager.getCurrentConfig());
  }

  public SubstanceChecker(ArchivalUnit au, Configuration config) {
    this.au = au;
    setConfig(config);
    try {
      nonSubstancePats = au.makeNonSubstanceUrlPatterns();
      substancePats = au.makeSubstanceUrlPatterns();
      if (nonSubstancePats != null || substancePats != null) {
	if (nonSubstancePats != null) {
	  additionalNonSubstanceUrls = getAdditionalNonSubstanceUrls();
	}
	hasSubstance = State.No;
	log.debug("NonSubstancePatterns: "
		  + RegexpUtil.regexpCollection(nonSubstancePats));
	log.debug("SubstancePatterns: " +
		  RegexpUtil.regexpCollection(substancePats));
      } else {
	hasSubstance = State.Unknown;
      }
    } catch (ArchivalUnit.ConfigurationException e) {
      log.error("Error in substance or non-substance pattern, disabling substance checking", e);
      nonSubstancePats = null;
      substancePats = null;
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

  public boolean isEnabledFor(String context) {
    if (nonSubstancePats == null && substancePats == null) {
      log.debug3("isEnabledFor(" + context + "): false, no patterns");
      return false;
    }
    boolean res = enabledContexts.equalsIgnoreCase(CONTEXT_ALL)
      || StringUtil.indexOfIgnoreCase(enabledContexts, context) >= 0;
    log.debug("isEnabledFor(" + context + "): " + res);
    return res;
  }

  protected Set<String> getAdditionalNonSubstanceUrls() {
    Set<String> res = new HashSet<String>();
    CrawlSpec spec = au.getCrawlSpec();
    addImplicitUrls(spec.getPermissionPages(), res);
    addImplicitUrls(spec.getStartingUrls(), res);
    return res;
  }

  private void addImplicitUrls(Collection<String> urls, Set<String> to) {
    for (String url : urls) {
      if (!isMatch(url, nonSubstancePats)) {
	to.add(url);
      }
    }
  }

  /** If substance not detected yet, check whether this CachedUrl contains
   * substance.  Releasing the CachedUrl is the responsibility of the
   * caller. */
  public void checkSubstance(CachedUrl cu) {
    if (hasSubstance == State.Yes) {
      // no need to check if already established has substance.
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
    if (hasSubstance == State.Yes) {
      // no need to check if already established has substance.
      return;
    }
    checkSubstanceUrl(url);
  }

  private void nowHasSubstance(String url) {
    switch (hasSubstance) {
    case No:
    case Unknown:
      log.debug("First substance found: " + url);
      break;
    default:
    }
    hasSubstance = State.Yes;
  }

  private void checkSubstanceUrl(String url) {
    if (substancePats != null) {
      if (isMatch(url, substancePats)) {
	nowHasSubstance(url);
	if (log.isDebug3()) {
	  log.debug3("checkSubstanceUrl(" + url + ") matched substance");
	}
	return;
      } else {
        if (log.isDebug3()) {
          log.debug3("checkSubstanceUrl(" + url + ") does not match substance");
        }
      }
    }
    if (nonSubstancePats != null) {
      if (!isMatch(url, nonSubstancePats)
	  && (additionalNonSubstanceUrls == null
	      || !additionalNonSubstanceUrls.contains(url))) {
	if (log.isDebug3()) {
	  log.debug3("checkSubstanceUrl(" + url + ") matched non-substance");
	}
	nowHasSubstance(url);
      }
    }
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

  public State hasSubstance() {
    return hasSubstance;
  }

  public void setHasSubstance(State state) {
    hasSubstance = state;
  }
}
