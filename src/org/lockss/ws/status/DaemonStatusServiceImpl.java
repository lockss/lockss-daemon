/*
 * $Id$
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

import static org.lockss.config.ConfigManager.PARAM_PLATFORM_ADMIN_EMAIL;
import static org.lockss.config.ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST;
import static org.lockss.config.ConfigManager.PARAM_PLATFORM_FQDN;
import static org.lockss.config.ConfigManager.PARAM_PLATFORM_IP_ADDRESS;
import static org.lockss.config.ConfigManager.PARAM_PLATFORM_LOCAL_V3_IDENTITY;
import static org.lockss.config.ConfigManager.PARAM_PLATFORM_PROJECT;
import static org.lockss.config.ConfigManager.PARAM_PLATFORM_SECOND_IP_ADDRESS;
import static org.lockss.config.ConfigManager.PARAM_PLATFORM_SMTP_HOST;
import static org.lockss.config.ConfigManager.PARAM_PLATFORM_SMTP_PORT;
import static org.lockss.util.BuildInfo.BUILD_HOST;
import static org.lockss.util.BuildInfo.BUILD_TIMESTAMP;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.jws.WebService;
import org.josql.Query;
import org.josql.QueryExecutionException;
import org.josql.QueryParseException;
import org.josql.QueryResults;
import org.lockss.app.LockssDaemon;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.PluginManager;
import org.lockss.util.BuildInfo;
import org.lockss.util.DaemonVersion;
import org.lockss.util.Logger;
import org.lockss.util.PlatformUtil;
import org.lockss.util.PlatformVersion;
import org.lockss.util.StringUtil;
import org.lockss.util.TimeBase;
import org.lockss.ws.entities.AuStatus;
import org.lockss.ws.entities.AuWsResult;
import org.lockss.ws.entities.CrawlWsResult;
import org.lockss.ws.entities.DaemonVersionWsResult;
import org.lockss.ws.entities.JavaVersionWsResult;
import org.lockss.ws.entities.IdNamePair;
import org.lockss.ws.entities.LockssWebServicesFault;
import org.lockss.ws.entities.LockssWebServicesFaultInfo;
import org.lockss.ws.entities.PeerWsResult;
import org.lockss.ws.entities.PlatformConfigurationWsResult;
import org.lockss.ws.entities.PlatformWsResult;
import org.lockss.ws.entities.PluginWsResult;
import org.lockss.ws.entities.PollWsResult;
import org.lockss.ws.entities.RepositorySpaceWsResult;
import org.lockss.ws.entities.RepositoryWsResult;
import org.lockss.ws.entities.TdbAuWsResult;
import org.lockss.ws.entities.TdbPublisherWsResult;
import org.lockss.ws.entities.TdbTitleWsResult;
import org.lockss.ws.entities.VoteWsResult;
import org.lockss.ws.status.DaemonStatusService;

@WebService
public class DaemonStatusServiceImpl implements DaemonStatusService {
  public static String BUILD_TIMESTAMP_FORMAT = "dd-MMM-yy HH:mm:ss zzz";
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
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auId = " + auId);

    // Input validation.
    if (StringUtil.isNullString(auId)) {
      throw new LockssWebServicesFault(
	  new IllegalArgumentException("Invalid Archival Unit identifier"),
	  new LockssWebServicesFaultInfo("Archival Unit identifier = " + auId));
    }

    // Get the status.
    AuStatus result = new AuHelper().getAuStatus(auId);
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);

    return result;
  }

  /**
   * Provides the selected properties of selected plugins in the system.
   * 
   * @param pluginQuery
   *          A String with the
   *          <a href="package-summary.html#SQL-Like_Query">SQL-like query</a>
   *          used to specify what properties to retrieve from which plugins.
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
   * Provides the selected properties of selected archival units in the system.
   * 
   * @param auQuery
   *          A String with the
   *          <a href="package-summary.html#SQL-Like_Query">SQL-like query</a>
   *          used to specify what properties to retrieve from which archival
   *          units.
   * @return a List<AuWsResult> with the results.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<AuWsResult> queryAus(String auQuery) throws LockssWebServicesFault
  {
    final String DEBUG_HEADER = "queryAus(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auQuery = " + auQuery);

    AuHelper auHelper = new AuHelper();
    List<AuWsResult> results = null;

    // Create the full query.
    String fullQuery = createFullQuery(auQuery, AuHelper.SOURCE_FQCN,
	AuHelper.PROPERTY_NAMES, AuHelper.RESULT_FQCN);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "fullQuery = " + fullQuery);

    // Create a new JoSQL query.
    Query q = new Query();

    try {
      // Parse the SQL-like query.
      q.parse(fullQuery);

      try {
	// Execute the query.
	QueryResults qr = q.execute(auHelper.createUniverse());

	// Get the query results.
	results = (List<AuWsResult>)qr.getResults();
	if (log.isDebug3()) {
	  log.debug3(DEBUG_HEADER + "results.size() = " + results.size());
	  log.debug3(DEBUG_HEADER + "results = "
	      + auHelper.nonDefaultToString(results));
	}
      } catch (QueryExecutionException qee) {
	log.error("Caught QueryExecuteException", qee);
	log.error("fullQuery = '" + fullQuery + "'");
	throw new LockssWebServicesFault(qee,
	    new LockssWebServicesFaultInfo("auQuery = " + auQuery));
      }
    } catch (QueryParseException qpe) {
      log.error("Caught QueryParseException", qpe);
      log.error("fullQuery = '" + fullQuery + "'");
	throw new LockssWebServicesFault(qpe,
	    new LockssWebServicesFaultInfo("auQuery = " + auQuery));
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "results = "
	+ auHelper.nonDefaultToString(results));
    return results;
  }

  /**
   * Provides the selected properties of selected peers in the system.
   * 
   * @param peerQuery
   *          A String with the
   *          <a href="package-summary.html#SQL-Like_Query">SQL-like query</a>
   *          used to specify what properties to retrieve from which peers.
   * @return a List<PeerWsResult> with the results.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<PeerWsResult> queryPeers(String peerQuery)
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "queryPeers(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "peerQuery = " + peerQuery);

    PeerHelper peerHelper = new PeerHelper();
    List<PeerWsResult> results = null;

    // Create the full query.
    String fullQuery = createFullQuery(peerQuery, PeerHelper.SOURCE_FQCN,
	PeerHelper.PROPERTY_NAMES, PeerHelper.RESULT_FQCN);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "fullQuery = " + fullQuery);

    // Create a new JoSQL query.
    Query q = new Query();

    try {
      // Parse the SQL-like query.
      q.parse(fullQuery);

      try {
	// Execute the query.
	QueryResults qr = q.execute(peerHelper.createUniverse());

	// Get the query results.
	results = (List<PeerWsResult>)qr.getResults();
	if (log.isDebug3()) {
	  log.debug3(DEBUG_HEADER + "results.size() = " + results.size());
	  log.debug3(DEBUG_HEADER + "results = "
	      + peerHelper.nonDefaultToString(results));
	}
      } catch (QueryExecutionException qee) {
	log.error("Caught QueryExecuteException", qee);
	log.error("fullQuery = '" + fullQuery + "'");
	throw new LockssWebServicesFault(qee,
	    new LockssWebServicesFaultInfo("peerQuery = " + peerQuery));
      }
    } catch (QueryParseException qpe) {
      log.error("Caught QueryParseException", qpe);
      log.error("fullQuery = '" + fullQuery + "'");
	throw new LockssWebServicesFault(qpe,
	    new LockssWebServicesFaultInfo("peerQuery = " + peerQuery));
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "results = "
	+ peerHelper.nonDefaultToString(results));
    return results;
  }

  /**
   * Provides the selected properties of selected votes in the system.
   * 
   * @param voteQuery
   *          A String with the
   *          <a href="package-summary.html#SQL-Like_Query">SQL-like query</a>
   *          used to specify what properties to retrieve from which votes.
   * @return a List<VoteWsResult> with the results.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<VoteWsResult> queryVotes(String voteQuery)
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "queryVotes(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "voteQuery = " + voteQuery);

    VoteHelper voteHelper = new VoteHelper();
    List<VoteWsResult> results = null;

    // Create the full query.
    String fullQuery = createFullQuery(voteQuery, VoteHelper.SOURCE_FQCN,
	VoteHelper.PROPERTY_NAMES, VoteHelper.RESULT_FQCN);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "fullQuery = " + fullQuery);

    // Create a new JoSQL query.
    Query q = new Query();

    try {
      // Parse the SQL-like query.
      q.parse(fullQuery);

      try {
	// Execute the query.
	QueryResults qr = q.execute(voteHelper.createUniverse());

	// Get the query results.
	results = (List<VoteWsResult>)qr.getResults();
	if (log.isDebug3()) {
	  log.debug3(DEBUG_HEADER + "results.size() = " + results.size());
	  log.debug3(DEBUG_HEADER + "results = "
	      + voteHelper.nonDefaultToString(results));
	}
      } catch (QueryExecutionException qee) {
	log.error("Caught QueryExecuteException", qee);
	log.error("fullQuery = '" + fullQuery + "'");
	throw new LockssWebServicesFault(qee,
	    new LockssWebServicesFaultInfo("voteQuery = " + voteQuery));
      }
    } catch (QueryParseException qpe) {
      log.error("Caught QueryParseException", qpe);
      log.error("fullQuery = '" + fullQuery + "'");
	throw new LockssWebServicesFault(qpe,
	    new LockssWebServicesFaultInfo("voteQuery = " + voteQuery));
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "results = "
	+ voteHelper.nonDefaultToString(results));
    return results;
  }

  /**
   * Provides the selected properties of selected repository spaces in the
   * system.
   * 
   * @param repositorySpaceQuery
   *          A String with the
   *          <a href="package-summary.html#SQL-Like_Query">SQL-like query</a>
   *          used to specify what properties to retrieve from which repository
   *          spaces.
   * @return a List<RepositorySpaceWsResult> with the results.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<RepositorySpaceWsResult> queryRepositorySpaces(
      String repositorySpaceQuery) throws LockssWebServicesFault {
    final String DEBUG_HEADER = "queryRepositorySpaces(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "repositorySpaceQuery = "
	+ repositorySpaceQuery);

    RepositorySpaceHelper repositorySpaceHelper = new RepositorySpaceHelper();
    List<RepositorySpaceWsResult> results = null;

    // Create the full query.
    String fullQuery = createFullQuery(repositorySpaceQuery,
	RepositorySpaceHelper.SOURCE_FQCN, RepositorySpaceHelper.PROPERTY_NAMES,
	RepositorySpaceHelper.RESULT_FQCN);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "fullQuery = " + fullQuery);

    // Create a new JoSQL query.
    Query q = new Query();

    try {
      // Parse the SQL-like query.
      q.parse(fullQuery);

      try {
	// Execute the query.
	QueryResults qr = q.execute(repositorySpaceHelper.createUniverse());

	// Get the query results.
	results = (List<RepositorySpaceWsResult>)qr.getResults();
	if (log.isDebug3()) {
	  log.debug3(DEBUG_HEADER + "results.size() = " + results.size());
	  log.debug3(DEBUG_HEADER + "results = "
	      + repositorySpaceHelper.nonDefaultToString(results));
	}
      } catch (QueryExecutionException qee) {
	log.error("Caught QueryExecuteException", qee);
	log.error("fullQuery = '" + fullQuery + "'");
	throw new LockssWebServicesFault(qee,
	    new LockssWebServicesFaultInfo("repositorySpaceQuery = "
		+ repositorySpaceQuery));
      }
    } catch (QueryParseException qpe) {
      log.error("Caught QueryParseException", qpe);
      log.error("fullQuery = '" + fullQuery + "'");
	throw new LockssWebServicesFault(qpe,
	    new LockssWebServicesFaultInfo("repositorySpaceQuery = "
		+ repositorySpaceQuery));
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "results = "
	+ repositorySpaceHelper.nonDefaultToString(results));
    return results;
  }

  /**
   * Provides the selected properties of selected repositories in the system.
   * 
   * @param repositoryQuery
   *          A String with the
   *          <a href="package-summary.html#SQL-Like_Query">SQL-like query</a>
   *          used to specify what properties to retrieve from which
   *          repositories.
   * @return a List<RepositoryWsResult> with the results.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<RepositoryWsResult> queryRepositories(String repositoryQuery)
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "queryRepositories(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "repositoryQuery = "
	+ repositoryQuery);

    RepositoryHelper repositoryHelper = new RepositoryHelper();
    List<RepositoryWsResult> results = null;

    // Create the full query.
    String fullQuery = createFullQuery(repositoryQuery,
	RepositoryHelper.SOURCE_FQCN, RepositoryHelper.PROPERTY_NAMES,
	RepositoryHelper.RESULT_FQCN);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "fullQuery = " + fullQuery);

    // Create a new JoSQL query.
    Query q = new Query();

    try {
      // Parse the SQL-like query.
      q.parse(fullQuery);

      try {
	// Execute the query.
	QueryResults qr = q.execute(repositoryHelper.createUniverse());

	// Get the query results.
	results = (List<RepositoryWsResult>)qr.getResults();
	if (log.isDebug3()) {
	  log.debug3(DEBUG_HEADER + "results.size() = " + results.size());
	  log.debug3(DEBUG_HEADER + "results = "
	      + repositoryHelper.nonDefaultToString(results));
	}
      } catch (QueryExecutionException qee) {
	log.error("Caught QueryExecuteException", qee);
	log.error("fullQuery = '" + fullQuery + "'");
	throw new LockssWebServicesFault(qee,
	    new LockssWebServicesFaultInfo("repositoryQuery = "
		+ repositoryQuery));
      }
    } catch (QueryParseException qpe) {
      log.error("Caught QueryParseException", qpe);
      log.error("fullQuery = '" + fullQuery + "'");
	throw new LockssWebServicesFault(qpe,
	    new LockssWebServicesFaultInfo("repositoryQuery = "
		+ repositoryQuery));
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "results = "
	+ repositoryHelper.nonDefaultToString(results));
    return results;
  }

  /**
   * Provides the selected properties of selected crawls in the system.
   * 
   * @param crawlQuery
   *          A String with the
   *          <a href="package-summary.html#SQL-Like_Query">SQL-like query</a>
   *          used to specify what properties to retrieve from which crawls.
   * @return a List<CrawlWsResult> with the results.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<CrawlWsResult> queryCrawls(String crawlQuery)
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "queryCrawls(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "crawlQuery = " + crawlQuery);

    CrawlHelper crawlHelper = new CrawlHelper();
    List<CrawlWsResult> results = null;

    // Create the full query.
    String fullQuery = createFullQuery(crawlQuery, CrawlHelper.SOURCE_FQCN,
	CrawlHelper.PROPERTY_NAMES, CrawlHelper.RESULT_FQCN);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "fullQuery = " + fullQuery);

    // Create a new JoSQL query.
    Query q = new Query();

    try {
      // Parse the SQL-like query.
      q.parse(fullQuery);

      try {
	// Execute the query.
	QueryResults qr = q.execute(crawlHelper.createUniverse());

	// Get the query results.
	results = (List<CrawlWsResult>)qr.getResults();
	if (log.isDebug3()) {
	  log.debug3(DEBUG_HEADER + "results.size() = " + results.size());
	  log.debug3(DEBUG_HEADER + "results = "
	      + crawlHelper.nonDefaultToString(results));
	}
      } catch (QueryExecutionException qee) {
	log.error("Caught QueryExecuteException", qee);
	log.error("fullQuery = '" + fullQuery + "'");
	throw new LockssWebServicesFault(qee,
	    new LockssWebServicesFaultInfo("crawlQuery = " + crawlQuery));
      }
    } catch (QueryParseException qpe) {
      log.error("Caught QueryParseException", qpe);
      log.error("fullQuery = '" + fullQuery + "'");
	throw new LockssWebServicesFault(qpe,
	    new LockssWebServicesFaultInfo("crawlQuery = " + crawlQuery));
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "results = "
	+ crawlHelper.nonDefaultToString(results));
    return results;
  }

  /**
   * Provides the selected properties of selected polls in the system.
   * 
   * @param pollQuery
   *          A String with the
   *          <a href="package-summary.html#SQL-Like_Query">SQL-like query</a>
   *          used to specify what properties to retrieve from which polls.
   * @return a List<PollWsResult> with the results.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<PollWsResult> queryPolls(String pollQuery)
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "queryPolls(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "pollQuery = " + pollQuery);

    PollHelper pollHelper = new PollHelper();
    List<PollWsResult> results = null;

    // Create the full query.
    String fullQuery = createFullQuery(pollQuery, PollHelper.SOURCE_FQCN,
	PollHelper.PROPERTY_NAMES, PollHelper.RESULT_FQCN);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "fullQuery = " + fullQuery);

    // Create a new JoSQL query.
    Query q = new Query();

    try {
      // Parse the SQL-like query.
      q.parse(fullQuery);

      try {
	// Execute the query.
	QueryResults qr = q.execute(pollHelper.createUniverse());

	// Get the query results.
	results = (List<PollWsResult>)qr.getResults();
	if (log.isDebug3()) {
	  log.debug3(DEBUG_HEADER + "results.size() = " + results.size());
	  log.debug3(DEBUG_HEADER + "results = "
	      + pollHelper.nonDefaultToString(results));
	}
      } catch (QueryExecutionException qee) {
	log.error("Caught QueryExecuteException", qee);
	log.error("fullQuery = '" + fullQuery + "'");
	throw new LockssWebServicesFault(qee,
	    new LockssWebServicesFaultInfo("pollQuery = " + pollQuery));
      }
    } catch (QueryParseException qpe) {
      log.error("Caught QueryParseException", qpe);
      log.error("fullQuery = '" + fullQuery + "'");
	throw new LockssWebServicesFault(qpe,
	    new LockssWebServicesFaultInfo("pollQuery = " + pollQuery));
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "results = "
	+ pollHelper.nonDefaultToString(results));
    return results;
  }

  /**
   * Provides the platform configuration.
   * 
   * @return a PlatformConfigurationWsResult with the platform configuration.
   * @throws LockssWebServicesFault
   */
  @Override
  public PlatformConfigurationWsResult getPlatformConfiguration()
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getPlatformConfiguration(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    PlatformConfigurationWsResult result = new PlatformConfigurationWsResult();
    LockssDaemon theDaemon = LockssDaemon.getLockssDaemon();
    Configuration config = ConfigManager.getCurrentConfig();

    result.setHostName(config.get(PARAM_PLATFORM_FQDN));

    result.setIpAddress(config.get(PARAM_PLATFORM_IP_ADDRESS));

    if (theDaemon.isClockss()) {
      result.setIpAddress(config.get(PARAM_PLATFORM_SECOND_IP_ADDRESS));
    }

    result.setGroups((List<String>)(config.getPlatformGroupList()));
    result.setProject(config.get(PARAM_PLATFORM_PROJECT));
    result.setV3Identity(config.get(PARAM_PLATFORM_LOCAL_V3_IDENTITY));

    String smtpHost = config.get(PARAM_PLATFORM_SMTP_HOST);

    if (smtpHost != null) {
      int smtpPort =
	  config.getInt(PARAM_PLATFORM_SMTP_PORT,
			org.lockss.mail.SmtpMailService.DEFAULT_SMTPPORT);
      result.setMailRelay(smtpHost + ":" + smtpPort);
    }

    result.setAdminEmail(config.get(PARAM_PLATFORM_ADMIN_EMAIL));
    result.setDisks((List<String>)
	(config.getList(PARAM_PLATFORM_DISK_SPACE_LIST)));

    result.setCurrentTime(TimeBase.nowMs());
    result.setUptime(TimeBase.msSince(theDaemon.getStartDate().getTime()));

    DaemonVersion daemonVersion = ConfigManager.getDaemonVersion();
    DaemonVersionWsResult daemonVersionResult = new DaemonVersionWsResult();

    daemonVersionResult.setFullVersion(daemonVersion.displayString());
    daemonVersionResult.setMajorVersion(daemonVersion.getMajorVersion());
    daemonVersionResult.setMinorVersion(daemonVersion.getMinorVersion());
    daemonVersionResult.setBuildVersion(daemonVersion.getBuildVersion());

    result.setDaemonVersion(daemonVersionResult);

    JavaVersionWsResult javaVersionResult = new JavaVersionWsResult();

    Properties sprops = System.getProperties();
    javaVersionResult.setVersion(sprops.getProperty("java.version"));
    javaVersionResult.setSpecificationVersion(sprops.getProperty("java.specification.version"));
    javaVersionResult.setRuntimeVersion(sprops.getProperty("java.runtime.version"));
    javaVersionResult.setRuntimeName(sprops.getProperty("java.runtime.name"));

    result.setJavaVersion(javaVersionResult);

    PlatformVersion platformVersion = Configuration.getPlatformVersion();
    PlatformWsResult platform = new PlatformWsResult();

    if (platformVersion != null) {
      platform.setName(platformVersion.getName());
      platform.setVersion(platformVersion.getVersion());
      platform.setSuffix(platformVersion.getSuffix());
      result.setPlatform(platform);
    }

    result.setCurrentWorkingDirectory(PlatformUtil.getCwd());

    result.setProperties((List<String>)
	(ConfigManager.getConfigManager().getConfigUrlList()));

    result.setBuildHost(BuildInfo.getBuildProperty(BUILD_HOST));
    result.setBuildTimestamp(getBuildTimestamp());

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Provides the selected properties of selected title database publishers.
   * 
   * @param tdbPublisherQuery
   *          A String with the
   *          <a href="package-summary.html#SQL-Like_Query">SQL-like query</a>
   *          used to specify what properties to retrieve from which title
   *          database publishers.
   * @return a List<TdbPublisherWsResult> with the results.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<TdbPublisherWsResult> queryTdbPublishers(String tdbPublisherQuery)
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "queryTdbPublishers(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "tdbPublisherQuery = " + tdbPublisherQuery);

    TdbPublisherHelper tdbPublisherHelper = new TdbPublisherHelper();
    List<TdbPublisherWsResult> results = null;

    // Create the full query.
    String fullQuery = createFullQuery(tdbPublisherQuery,
	TdbPublisherHelper.SOURCE_FQCN, TdbPublisherHelper.PROPERTY_NAMES,
	TdbPublisherHelper.RESULT_FQCN);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "fullQuery = " + fullQuery);

    // Create a new JoSQL query.
    Query q = new Query();

    try {
      // Parse the SQL-like query.
      q.parse(fullQuery);

      try {
	// Execute the query.
	QueryResults qr = q.execute(tdbPublisherHelper.createUniverse());

	// Get the query results.
	results = (List<TdbPublisherWsResult>)qr.getResults();
	if (log.isDebug3()) {
	  log.debug3(DEBUG_HEADER + "results.size() = " + results.size());
	  log.debug3(DEBUG_HEADER + "results = "
	      + tdbPublisherHelper.nonDefaultToString(results));
	}
      } catch (QueryExecutionException qee) {
	log.error("Caught QueryExecuteException", qee);
	log.error("fullQuery = '" + fullQuery + "'");
	throw new LockssWebServicesFault(qee,
	    new LockssWebServicesFaultInfo("tdbPublisherQuery = "
		+ tdbPublisherQuery));
      }
    } catch (QueryParseException qpe) {
      log.error("Caught QueryParseException", qpe);
      log.error("fullQuery = '" + fullQuery + "'");
	throw new LockssWebServicesFault(qpe,
	    new LockssWebServicesFaultInfo("tdbPublisherQuery = "
		+ tdbPublisherQuery));
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "results = "
	+ tdbPublisherHelper.nonDefaultToString(results));
    return results;
  }

  /**
   * Provides the selected properties of selected title database titles.
   * 
   * @param tdbTitleQuery
   *          A String with the
   *          <a href="package-summary.html#SQL-Like_Query">SQL-like query</a>
   *          used to specify what properties to retrieve from which title
   *          database titles.
   * @return a List<TdbTitleWsResult> with the results.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<TdbTitleWsResult> queryTdbTitles(String tdbTitleQuery)
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "queryTdbTitles(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "tdbTitleQuery = " + tdbTitleQuery);

    TdbTitleHelper tdbTitleHelper = new TdbTitleHelper();
    List<TdbTitleWsResult> results = null;

    // Create the full query.
    String fullQuery = createFullQuery(tdbTitleQuery,
	TdbTitleHelper.SOURCE_FQCN, TdbTitleHelper.PROPERTY_NAMES,
	TdbTitleHelper.RESULT_FQCN);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "fullQuery = " + fullQuery);

    // Create a new JoSQL query.
    Query q = new Query();

    try {
      // Parse the SQL-like query.
      q.parse(fullQuery);

      try {
	// Execute the query.
	QueryResults qr = q.execute(tdbTitleHelper.createUniverse());

	// Get the query results.
	results = (List<TdbTitleWsResult>)qr.getResults();
	if (log.isDebug3()) {
	  log.debug3(DEBUG_HEADER + "results.size() = " + results.size());
	  log.debug3(DEBUG_HEADER + "results = "
	      + tdbTitleHelper.nonDefaultToString(results));
	}
      } catch (QueryExecutionException qee) {
	log.error("Caught QueryExecuteException", qee);
	log.error("fullQuery = '" + fullQuery + "'");
	throw new LockssWebServicesFault(qee,
	    new LockssWebServicesFaultInfo("tdbTitleQuery = "
		+ tdbTitleQuery));
      }
    } catch (QueryParseException qpe) {
      log.error("Caught QueryParseException", qpe);
      log.error("fullQuery = '" + fullQuery + "'");
	throw new LockssWebServicesFault(qpe,
	    new LockssWebServicesFaultInfo("tdbTitleQuery = "
		+ tdbTitleQuery));
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "results = "
	+ tdbTitleHelper.nonDefaultToString(results));
    return results;
  }

  /**
   * Provides the selected properties of selected title database archival units.
   * 
   * @param tdbAuQuery
   *          A String with the
   *          <a href="package-summary.html#SQL-Like_Query">SQL-like query</a>
   *          used to specify what properties to retrieve from which title
   *          database archival units.
   * @return a List<TdbAuWsResult> with the results.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<TdbAuWsResult> queryTdbAus(String tdbAuQuery)
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "queryTdbAus(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "tdbAuQuery = " + tdbAuQuery);

    TdbAuHelper tdbAuHelper = new TdbAuHelper();
    List<TdbAuWsResult> results = null;

    // Create the full query.
    String fullQuery = createFullQuery(tdbAuQuery, TdbAuHelper.SOURCE_FQCN,
	TdbAuHelper.PROPERTY_NAMES, TdbAuHelper.RESULT_FQCN);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "fullQuery = " + fullQuery);

    // Create a new JoSQL query.
    Query q = new Query();

    try {
      // Parse the SQL-like query.
      q.parse(fullQuery);

      try {
	// Execute the query.
	QueryResults qr = q.execute(tdbAuHelper.createUniverse());

	// Get the query results.
	results = (List<TdbAuWsResult>)qr.getResults();
	if (log.isDebug3()) {
	  log.debug3(DEBUG_HEADER + "results.size() = " + results.size());
	  log.debug3(DEBUG_HEADER + "results = "
	      + tdbAuHelper.nonDefaultToString(results));
	}
      } catch (QueryExecutionException qee) {
	log.error("Caught QueryExecuteException", qee);
	log.error("fullQuery = '" + fullQuery + "'");
	throw new LockssWebServicesFault(qee,
	    new LockssWebServicesFaultInfo("tdbAuQuery = " + tdbAuQuery));
      }
    } catch (QueryParseException qpe) {
      log.error("Caught QueryParseException", qpe);
      log.error("fullQuery = '" + fullQuery + "'");
	throw new LockssWebServicesFault(qpe,
	    new LockssWebServicesFaultInfo("tdbAuQuery = " + tdbAuQuery));
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "results = "
	+ tdbAuHelper.nonDefaultToString(results));
    return results;
  }

  /**
   * Provides the full query that is equivalent to the simplified query for a
   * given class.
   * 
   * A full query looks like
   * SELECT new org.lockss.ws.entities.SomeWsResult()
   * {propertyName -> propertyName, ...}
   * FROM org.lockss.ws.entities.SomeWsSource
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

  /**
   * Provides the build timestamp.
   * 
   * @return A long with the build timestamp.
   * @throws LockssWebServicesFault
   */
  protected long getBuildTimestamp() throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getBuildTimestamp(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    try {
      long timestamp = (new SimpleDateFormat(BUILD_TIMESTAMP_FORMAT))
	  .parse(BuildInfo.getBuildProperty(BUILD_TIMESTAMP)).getTime();

      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "timestamp = " + timestamp);
      return timestamp;
    } catch (ParseException pe) {
      log.error("Caught ParseException", pe);
      log.error("BuildInfo.getBuildProperty(BUILD_TIMESTAMP)) = '"
	  + BuildInfo.getBuildProperty(BUILD_TIMESTAMP) + "'");
      throw new LockssWebServicesFault(pe,
	  new LockssWebServicesFaultInfo("BUILD_TIMESTAMP = "
	      + BuildInfo.getBuildProperty(BUILD_TIMESTAMP)));
    }
  }
}
