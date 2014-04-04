/*
 * $Id: DaemonStatusServiceImpl.java,v 1.4 2014-04-04 22:00:45 fergaloy-sf Exp $
 */

/*

 Copyright (c) 2013-2014 Board of Trustees of Leland Stanford Jr. University,
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

/**
 * The DaemonStatus web service implementation.
 */
package org.lockss.ws.status;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.jws.WebService;
import org.josql.Query;
import org.josql.QueryExecutionException;
import org.josql.QueryParseException;
import org.josql.QueryResults;
import org.lockss.app.LockssDaemon;
import org.lockss.crawler.CrawlManager;
import org.lockss.daemon.CrawlWindow;
import org.lockss.daemon.TitleConfig;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrlSet;
import org.lockss.plugin.Plugin;
import org.lockss.plugin.PluginManager;
import org.lockss.poller.Poll;
import org.lockss.poller.PollManager;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.state.AuState;
import org.lockss.state.NodeManager;
import org.lockss.state.NodeState;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import org.lockss.ws.entities.AuStatus;
import org.lockss.ws.entities.IdNamePair;
import org.lockss.ws.entities.LockssWebServicesFault;
import org.lockss.ws.entities.LockssWebServicesFaultInfo;
import org.lockss.ws.entities.PluginWsResult;
import org.lockss.ws.status.DaemonStatusService;

@WebService
public class DaemonStatusServiceImpl implements DaemonStatusService {
  private static Logger log = Logger.getLogger(DaemonStatusServiceImpl.class);

  /**
   * Provides an indication of whether the daemon is ready.
   * 
   * @return a boolean with the indication.
   * @throws LockssWebServicesFault
   */
  @Override
  public boolean isDaemonReady() throws LockssWebServicesFault {
    final String DEBUG_HEADER = "isDaemonReady(): ";

    try {
      log.debug2(DEBUG_HEADER + "Invoked.");
      PluginManager pluginMgr =
	  (PluginManager) LockssDaemon.getManager(LockssDaemon.PLUGIN_MANAGER);
      boolean areAusStarted = pluginMgr.areAusStarted();
      log.debug2(DEBUG_HEADER + "areAusStarted = " + areAusStarted);

      return areAusStarted;
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }

  /**
   * Provides a list of the identifier/name pairs of the archival units in the
   * system.
   * 
   * @return a List<IdNamePair> with the identifier/name pairs of the archival
   *         units in the system.
   * @throws LockssWebServicesFault
   */
  @Override
  public Collection<IdNamePair> getAuIds() throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getAuIds(): ";

    try {
      log.debug2(DEBUG_HEADER + "Invoked.");
      Collection<IdNamePair> result = new ArrayList<IdNamePair>();
      PluginManager pluginMgr =
	  (PluginManager) LockssDaemon.getManager(LockssDaemon.PLUGIN_MANAGER);

      for (ArchivalUnit au : pluginMgr.getAllAus()) {
	log.debug2(DEBUG_HEADER + "au = " + au);
	result.add(new IdNamePair(au.getAuId(), au.getName()));
      }

      log.debug2(DEBUG_HEADER + "result.size() = " + result.size());
      return result;
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }

  /**
   * Provides the status information of an archival unit in the system.
   * 
   * @param auId
   *          A String with the identifier of the archival unit.
   * @return an AuStatus with the status information of the archival unit.
   * @throws LockssWebServicesFault
   */
  @Override
  public AuStatus getAuStatus(String auId) throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getAuStatus(): ";

    try {
      log.debug2(DEBUG_HEADER + "auId = " + auId);
      if (StringUtil.isNullString(auId)) {
	throw new LockssWebServicesFault(
	    new IllegalArgumentException("Invalid Archival Unit identifier"),
	    new LockssWebServicesFaultInfo("Archival Unit identifier = "
		+ auId));
      }

      LockssDaemon theDaemon = LockssDaemon.getLockssDaemon();
      PluginManager pluginMgr = theDaemon.getPluginManager();
      ArchivalUnit au = pluginMgr.getAuFromId(auId);
      if (au == null) {
	throw new LockssWebServicesFault(
	    "No Archival Unit with provided identifier",
	    new LockssWebServicesFaultInfo("Archival Unit identifier = "
		+ auId));
      }

      AuStatus result = new AuStatus();

      result.setVolume(au.getName());

      TitleConfig tc = au.getTitleConfig();
      if (tc != null) {
	result.setJournalTitle(tc.getJournalTitle());
      }

      Plugin plugin = au.getPlugin();
      result.setPluginName(plugin.getPluginName());

      String yearAsString = null;
      try {
	yearAsString = AuUtil.getTitleAttribute(au, "year");
	if (yearAsString != null) {
	  result.setYear(Integer.valueOf(yearAsString));
	}
      } catch (NumberFormatException nfe) {
	log.warning("Invalid year title attribute '" + yearAsString
	    + "' for auId '" + auId + "'", nfe);
      }

      NodeManager nodeMgr = theDaemon.getNodeManager(au);
      AuState state = nodeMgr.getAuState();

      AuState.AccessType atype = state.getAccessType();
      if (atype != null) {
	result.setAccessType(atype.toString());
      }
      long contentSize = AuUtil.getAuContentSize(au, false);
      if (contentSize != -1) {
	result.setContentSize(contentSize);
      }

      long du = AuUtil.getAuDiskUsage(au, false);
      if (du != -1) {
	result.setDiskUsage(du);
      }

      String spec = LockssRepositoryImpl.getRepositorySpec(au);
      String repo =
	  LockssRepositoryImpl.mapAuToFileLocation(LockssRepositoryImpl
	      .getLocalRepositoryPath(spec), au);
      result.setRepository(repo);

      CachedUrlSet auCus = au.getAuCachedUrlSet();
      NodeState topNode = nodeMgr.getNodeState(auCus);

      if (AuUtil.getProtocolVersion(au) == Poll.V3_PROTOCOL) {
	if (state.getV3Agreement() < 0) {
	  if (state.getLastCrawlTime() < 0) {
	    result.setStatus("Waiting for Crawl");
	  } else {
	    result.setStatus("Waiting for Poll");
	  }
	} else {
	  result.setStatus(doubleToPercent(state.getHighestV3Agreement())
	      + "% Agreement");
	  if (state.getHighestV3Agreement() != state.getV3Agreement()) {
	    result.setRecentPollAgreement(state.getV3Agreement());
	  }
	}
      } else {
	result.setStatus(topNode.hasDamage() ? "Repairing" : "Ok");
      }

      String publishingPlatform = plugin.getPublishingPlatform();
      if (!StringUtil.isNullString(publishingPlatform)) {
	result.setPublishingPlatform(publishingPlatform);
      }

      String publisher = AuUtil.getTitleAttribute(au, "publisher");
      if (!StringUtil.isNullString(publisher)) {
	result.setPublisher(publisher);
      }

      result.setAvailableFromPublisher(!AuUtil.isPubDown(au));
      result.setSubstanceState(state.getSubstanceState().toString());
      result.setCreationTime(state.getAuCreationTime());

      AuUtil.AuProxyInfo aupinfo = AuUtil.getAuProxyInfo(au);
      if (aupinfo.isAuOverride()) {
	String disp =
	    (aupinfo.getHost() == null ? "Direct connection" : aupinfo
		.getHost() + ":" + aupinfo.getPort());
	result.setCrawlProxy(disp);
      }

      CrawlWindow window = au.getCrawlSpec().getCrawlWindow();
      if (window != null) {
	String wmsg = window.toString();
	if (wmsg.length() > 140) {
	  wmsg = "(not displayable)";
	}
	if (!window.canCrawl()) {
	  wmsg = "Currently closed: " + wmsg;
	}
	result.setCrawlWindow(wmsg);
      }

      String crawlPool = au.getFetchRateLimiterKey();
      if (crawlPool == null) {
	crawlPool = "(none)";
      }
      result.setCrawlPool(crawlPool);

      result.setLastCompletedCrawl(state.getLastCrawlTime());

      long lastCrawlAttempt = state.getLastCrawlAttempt();
      if (lastCrawlAttempt > 0) {
	result.setLastCrawl(lastCrawlAttempt);
	result.setLastCrawlResult(state.getLastCrawlResultMsg());
      }

      long lastTopLevelPollTime = state.getLastTopLevelPollTime();
      if (lastTopLevelPollTime > 0) {
	result.setLastCompletedPoll(lastTopLevelPollTime);
      }

      long lastPollStart = state.getLastPollStart();
      if (lastPollStart > 0) {
	result.setLastPoll(lastPollStart);
	String pollResult = state.getLastPollResultMsg();
	if (!StringUtil.isNullString(pollResult)) {
	  result.setLastPollResult(state.getLastPollResultMsg());
	}
      }

      CrawlManager crawlMgr = theDaemon.getCrawlManager();
      result.setCurrentlyCrawling(crawlMgr.getStatusSource().getStatus()
	  .isRunningNCCrawl(au));

      PollManager pollMgr = theDaemon.getPollManager();
      result.setCurrentlyPolling(pollMgr.isPollRunning(au));

      if (theDaemon.isDetectClockssSubscription()) {
	result.setSubscriptionStatus(AuUtil.getAuState(au)
	    .getClockssSubscriptionStatusString());
      }

      if (log.isDebug2()) {
	log.debug2(DEBUG_HEADER + "result = " + result);
      }

      return result;
    } catch (LockssWebServicesFault wsf) {
      throw wsf;
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }

  /**
   * Provides the selected properties of selected plugins in the system.
   * 
   * @param query
   *          A String with the query used to specify what properties to
   *          retrieve from which plugins.
   * @return a List<PluginWsResult> with the results.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<PluginWsResult> queryPlugins(String pluginQuery)
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "queryPlugins(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "pluginQuery = " + pluginQuery);

    PluginHelper pluginHelper = new PluginHelper();
    List<PluginWsResult> results = null;

    // Create the full query.
    String fullQuery = createFullQuery(pluginQuery, PluginHelper.SOURCE_FQCN,
	PluginHelper.PROPERTY_NAMES, PluginHelper.RESULT_FQCN);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "fullQuery = " + fullQuery);

    // Create a new JoSQL query.
    Query q = new Query();

    try {
      // Parse the SQL-like query.
      q.parse(fullQuery);

      try {
	// Execute the query.
	QueryResults qr = q.execute(pluginHelper.createUniverse());

	// Get the query results.
	results = (List<PluginWsResult>)qr.getResults();
	if (log.isDebug3()) {
	  log.debug3(DEBUG_HEADER + "results.size() = " + results.size());
	  log.debug3(DEBUG_HEADER + "results = "
	      + pluginHelper.nonDefaultToString(results));
	}
      } catch (QueryExecutionException qee) {
	log.error("Caught QueryExecuteException", qee);
	log.error("fullQuery = '" + fullQuery + "'");
	throw new LockssWebServicesFault(qee,
	    new LockssWebServicesFaultInfo("pluginQuery = " + pluginQuery));
      }
    } catch (QueryParseException qpe) {
      log.error("Caught QueryParseException", qpe);
      log.error("fullQuery = '" + fullQuery + "'");
	throw new LockssWebServicesFault(qpe,
	    new LockssWebServicesFaultInfo("pluginQuery = " + pluginQuery));
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "results = "
	+ pluginHelper.nonDefaultToString(results));
    return results;
  }

  /**
   * Formats 100 times a double to print as a percentage. An input value of 1.0
   * will produce "100.00".
   * 
   * @param d
   *          A double with the value to convert.
   * @return a String representing the double.
   */
  private String doubleToPercent(double d) {
    int i = (int) (d * 10000);
    double pc = i / 100.0;
    return new DecimalFormat("0.00").format(pc);
  }

  /**
   * Provides the full query that is equivalent to the simplified query for a
   * given class.
   * 
   * A full query looks like
   * SELECT new org.lockss.ws.entities.SomeWsResult()
   * {propertyName -> propertyName, ...}
   * FROM org.lockss.ws.entities.SomeWsResult
   * WHERE ...
   * 
   * @param originalQuery
   *          A String withe the original query.
   * @param resultClassName
   *          A String with the fully-qualified class name of the objects
   *          returned by the query.
   * @param sourceClassName
   *          A String with the fully-qualified class name of the objects
   *          used as source in the query.
   * @param selectPropertyNames
   *          A Collection<String> with the names of the properties in the query
   *          'select' clause.
   * @return a String with the full query.
   * @throws LockssWebServicesFault
   */
  private String createFullQuery(String originalQuery, String sourceClassName,
      Set<String> propertyNames, String resultClassName)
	  throws LockssWebServicesFault {
    final String DEBUG_HEADER = "createFullQuery(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "originalQuery = " + originalQuery);
      log.debug2(DEBUG_HEADER + "sourceClassName = " + sourceClassName);
      log.debug2(DEBUG_HEADER + "propertyNames = " + propertyNames);
      log.debug2(DEBUG_HEADER + "resultClassName = " + resultClassName);
    }

    // Get the property names specified in the query 'select' clause.
    Collection<String> selectPropertyNames =
	getSelectPropertyNames(originalQuery, propertyNames);
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "selectPropertyNames = " + selectPropertyNames);

    StringBuilder builder = new StringBuilder("SELECT ")
    .append(createSelectClause(resultClassName, selectPropertyNames))
    .append(" FROM ").append(sourceClassName);

    String whereClause = getWhereClause(originalQuery);
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "whereClause = " + whereClause);

    if (!StringUtil.isNullString(whereClause)) {
      builder.append(" WHERE ").append(whereClause);
    }

    String fullQuery = builder.toString();
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "fullQuery = " + fullQuery);
    return fullQuery;
  }

  /**
   * Provides the individual properties used in the 'select' clause of a query.
   * 
   * @param query
   *          A String with the query.
   * @param allPropertyNames
   *          A Set<String> with all the possible property names.
   * @return a Collection<String> with the names of the properties in the
   *         'select' clause of a query.
   * @throws LockssWebServicesFault
   */
  private Collection<String> getSelectPropertyNames(String query,
      Set<String> allPropertyNames) throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getSelectPropertyNames(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "query = " + query);
      log.debug2(DEBUG_HEADER + "allPropertyNames = " + allPropertyNames);
    }

    // Locate the beginning of the 'select' clause.
    String lcQuery = query.toLowerCase();
    String beginString = "select ";
    int beginIndex = lcQuery.indexOf(beginString) + beginString.length();
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "beginIndex = " + beginIndex);

    // Handle a missing 'select' clause.
    if (beginIndex < beginString.length()) {
      String message = "No SELECT clause in the query";
      log.debug(message);
      log.debug("query = " + query);
      throw new LockssWebServicesFault(message,
	  new LockssWebServicesFaultInfo("query = " + query));
    }

    // Locate the end of the 'select' clause.
    String endString = "where ";
    int endIndex = lcQuery.indexOf(endString, beginIndex);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "endIndex = " + endIndex);

    // Handle a missing 'where' clause.
    if (endIndex < 0) {
      endIndex = query.length();
    }

    // Handle an empty 'select' clause.
    if (endIndex == beginIndex) {
      String message = "Empty SELECT clause in the query";
      log.debug(message);
      log.debug("query = " + query);
      throw new LockssWebServicesFault(message,
	  new LockssWebServicesFaultInfo("query = " + query));
    }

    String selectClause = query.substring(beginIndex, endIndex).trim();
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "selectClause = " + selectClause);

    // Handle an empty 'select' clause.
    if (StringUtil.isNullString(selectClause)) {
      String message = "Empty SELECT clause in the query";
      log.debug(message);
      log.debug("query = " + query);
      throw new LockssWebServicesFault(message,
	  new LockssWebServicesFaultInfo("query = " + query));
    }

    Collection<String> selectNames = null;

    // Check whether the query 'select' clause uses a wildcard.
    if ("*".equals(selectClause)) {
      // Yes: Use all of the property names.
      selectNames = new HashSet<String>(allPropertyNames);
    } else {
      // No: Extract the specified property names.
      selectNames = StringUtil.breakAt(selectClause, ",", true);

      // Validate the property names extracted.
      validatePropertyNames(selectNames, allPropertyNames, query);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "selectNames = " + selectNames);
    return selectNames;
  }

  /**
   * Validates property names.
   * 
   * @param propertyNames
   *          A Collection<String> with the names of the properties to be
   *          validated.
   * @param validPropertyNames
   *          A Set<String> with the valid property names.
   * @param query A String with the query.
   * @throws LockssWebServicesFault
   *           if the validation fails.
   */
  private void validatePropertyNames(Collection<String> propertyNames,
      Set<String> validPropertyNames, String query)
	  throws LockssWebServicesFault {
    final String DEBUG_HEADER = "validatePropertyNames(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "propertyNames = " + propertyNames);
      log.debug2(DEBUG_HEADER + "validPropertyNames = " + validPropertyNames);
      log.debug2(DEBUG_HEADER + "query = " + query);
    }

    Set<String> invalidNames = new HashSet<String>();

    // Loop through all the property names to be validated.
    for (String name : propertyNames) {
      // Check whether the name of this property is not among those that are
      // valid.
      if (!validPropertyNames.contains(name)) {
	// Yes: Place it in the list of names that are not valid.
	invalidNames.add(name);
	log.debug("Property '" + name + "' not in set " + validPropertyNames);
      }
    }

    // Check whether any property names were found invalid.
    if (invalidNames.size() > 0) {
      // Yes: Report the problem.
      StringBuilder builder = new StringBuilder("Invalid name(s) ");
      boolean isFirst = true;

      for (String name : invalidNames) {
	if (!isFirst) {
	  builder.append(", ");
	} else {
	  isFirst = false;
	}

	builder.append("'").append(name).append("'");
      }

      String message = builder.append(" in the query").toString();
      log.debug(message);
      log.debug("query = " + query);
      throw new LockssWebServicesFault(message,
	  new LockssWebServicesFaultInfo("query = " + query));
    }
  }

  /**
   * Provides a query 'select' clause for a class and its property names.
   * 
   * @param className
   *          A String with the fully-qualified class name.
   * @param propertyNames
   *          A Collection<String> with the names of the properties in the query
   *          'select' clause.
   * @return a String with the query 'select' clause.
   */
  private String createSelectClause(String className,
      Collection<String> propertyNames) {
    final String DEBUG_HEADER = "createSelectClause(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "className = " + className);
      log.debug2(DEBUG_HEADER + "propertyNames = " + propertyNames);
    }

    // Initialize the 'select' clause with the class name.
    StringBuilder builder =
	new StringBuilder("new ").append(className).append("() {");

    boolean isFirst = true;

    // Loop through all the property names that must appear in the 'select'
    // clause.
    for (String name : propertyNames) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      // Append this property name to the 'select' clause.
      builder.append(name).append(" -> ").append(name);
    }

    // Finish the 'select' clause.
    String selectClause = builder.append("}").toString();
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "selectClause = " + selectClause);
    return selectClause;
  }

  /**
   * Provides the 'where' clause of a query.
   * 
   * @param query
   *          A String with the query containing the 'where' clause.
   * @return A String with the query 'where' clause.
   * @throws LockssWebServicesFault
   */
  private String getWhereClause(String query) throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getWhereClause(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "query = " + query);
    String whereClause = "";

    // Locate the beginning of the 'where' clause.
    String beginString = " where ";
    int beginIndex =
	query.toLowerCase().indexOf(beginString) + beginString.length();
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "beginIndex = " + beginIndex);

    // Check whether a 'where' clause exists.
    if (beginIndex >= beginString.length()) {
      // Yes: Extract the contents of the 'where' clause.
      whereClause = query.substring(beginIndex).trim();

      // Handle an empty 'where' clause.
      if (StringUtil.isNullString(whereClause)) {
	String message = "Empty WHERE clause in the query";
	log.debug(message);
	log.debug("query = " + query);
	throw new LockssWebServicesFault(message,
	    new LockssWebServicesFaultInfo("query = " + query));
      }
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "whereClause = " + whereClause);
    return whereClause;
  }
}
