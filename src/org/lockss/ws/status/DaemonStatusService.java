/*
 * $Id$
 */

/*

 Copyright (c) 2013-2015 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.ws.status;

import java.util.Collection;
import java.util.List;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import org.lockss.ws.entities.AuStatus;
import org.lockss.ws.entities.AuWsResult;
import org.lockss.ws.entities.CrawlWsResult;
import org.lockss.ws.entities.IdNamePair;
import org.lockss.ws.entities.LockssWebServicesFault;
import org.lockss.ws.entities.PeerWsResult;
import org.lockss.ws.entities.PlatformConfigurationWsResult;
import org.lockss.ws.entities.PluginWsResult;
import org.lockss.ws.entities.PollWsResult;
import org.lockss.ws.entities.RepositorySpaceWsResult;
import org.lockss.ws.entities.RepositoryWsResult;
import org.lockss.ws.entities.TdbAuWsResult;
import org.lockss.ws.entities.TdbPublisherWsResult;
import org.lockss.ws.entities.TdbTitleWsResult;
import org.lockss.ws.entities.VoteWsResult;

/**
 * The DaemonStatus web service interface.
 */
@WebService
public interface DaemonStatusService {
  /**
   * Provides an indication of whether the daemon is ready.
   * 
   * @return a boolean with the indication.
   * @throws LockssWebServicesFault
   */
  @WebMethod
  boolean isDaemonReady() throws LockssWebServicesFault;

  /**
   * Provides a list of the identifier/name pairs of the archival units in the
   * system.
   * 
   * @return a List<IdNamePair> with the identifier/name pairs of the archival
   *         units in the system.
   * @throws LockssWebServicesFault
   */
  @WebMethod
  Collection<IdNamePair> getAuIds() throws LockssWebServicesFault;

  /**
   * Provides the status information of an archival unit in the system.
   * 
   * @param auId
   *          A String with the identifier of the archival unit.
   * @return an AuStatus with the status information of the archival unit.
   * @throws LockssWebServicesFault
   */
  @WebMethod
  AuStatus getAuStatus(@WebParam(name = "auId") String auId)
      throws LockssWebServicesFault;

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
  @WebMethod
  List<PluginWsResult> queryPlugins(@WebParam(name = "pluginQuery") String
      pluginQuery) throws LockssWebServicesFault;

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
  @WebMethod
  List<AuWsResult> queryAus(@WebParam(name = "auQuery") String auQuery)
      throws LockssWebServicesFault;

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
  @WebMethod
  List<PeerWsResult> queryPeers(@WebParam(name = "peerQuery") String peerQuery)
      throws LockssWebServicesFault;

  /**
   * Provides the selected properties of selected votes in the system.
   * 
   * @param voteQuery
   *          A String with the
   *          <a href="package-summary.html#SQL-Like_Query">SQL-like query</a>
   *          used to specify what properties to retrieve from which votes.
   * @return a List<VotelWsResult> with the results.
   * @throws LockssWebServicesFault
   */
  @WebMethod
  List<VoteWsResult> queryVotes(@WebParam(name = "voteQuery") String voteQuery)
      throws LockssWebServicesFault;

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
  @WebMethod
  List<RepositorySpaceWsResult> queryRepositorySpaces(
      @WebParam(name = "repositorySpaceQuery") String repositorySpaceQuery)
	  throws LockssWebServicesFault;

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
  @WebMethod
  List<RepositoryWsResult> queryRepositories(
      @WebParam(name = "repositoryQuery") String repositoryQuery)
	  throws LockssWebServicesFault;

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
  @WebMethod
  List<CrawlWsResult> queryCrawls(
      @WebParam(name = "crawlQuery") String crawlQuery)
	  throws LockssWebServicesFault;

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
  @WebMethod
  List<PollWsResult> queryPolls(@WebParam(name = "pollQuery") String pollQuery)
      throws LockssWebServicesFault;

  /**
   * Provides the platform configuration.
   * 
   * @return a PlatformConfigurationWsResult with the platform configuration.
   * @throws LockssWebServicesFault
   */
  @WebMethod
  PlatformConfigurationWsResult getPlatformConfiguration()
      throws LockssWebServicesFault;

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
  @WebMethod
  List<TdbPublisherWsResult> queryTdbPublishers(
      @WebParam(name = "tdbPublisherQuery") String tdbPublisherQuery)
	  throws LockssWebServicesFault;

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
  @WebMethod
  List<TdbTitleWsResult> queryTdbTitles(
      @WebParam(name = "tdbTitleQuery") String tdbTitleQuery)
	  throws LockssWebServicesFault;

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
  @WebMethod
  List<TdbAuWsResult> queryTdbAus(
      @WebParam(name = "tdbAuQuery") String tdbAuQuery)
	  throws LockssWebServicesFault;

  /**
   * Provides the URLs in an archival unit.
   * 
   * @param auId
   *          A String with the identifier of the archival unit.
   * @param url
   *          A String with the URL above which no results will be provided, or
   *          <code>NULL</code> if all the URLS are to be provided.
   * @return a List<String> with the results.
   * @throws LockssWebServicesFault
   */
  @WebMethod
  List<String> getAuUrls(@WebParam(name = "auId") String auId,
      @WebParam(name = "url") String url) throws LockssWebServicesFault;
}
