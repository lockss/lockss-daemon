/*
 * $Id$
 */

/*

Copyright (c) 2021 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.laaws;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;
import org.lockss.app.LockssDaemon;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.laaws.api.cfg.AusApi;
import org.lockss.laaws.api.rs.StreamingCollectionsApi;
import org.lockss.laaws.client.ApiException;
import org.lockss.laaws.client.V2RestClient;
import org.lockss.laaws.model.cfg.AuConfiguration;
import org.lockss.laaws.model.cfg.V2AuStateBean;
import org.lockss.laaws.model.rs.Artifact;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.CuIterator;
import org.lockss.repository.RepoSpec;
import org.lockss.state.AuState;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import org.lockss.util.UrlUtil;

public class V2AuMover {

  public static final String PREFIX = Configuration.PREFIX + "v2.";
  public static final String PARAM_V2_REPO_SPEC = PREFIX + "repo.spec";
  public static final String PARAM_CFG_ACCESS_URL = PREFIX + "cfg.url";

  public static final String PARAM_V2_USER_AGENT = PREFIX + "user_agent";
  public static final String  DEFAULT_V2_USER_AGENT = "lockss";

  public static final String PARAM_V2_USER = PREFIX + "user";
  public static final String PARAM_V2_PASSWD = PREFIX + "passwd";


  public static final String DEBUG_CONFIG_REQUEST = PREFIX + "cfg.debug";
  public static final boolean  DEFAULT_DEBUG_CONFIG_REQUEST = false;

  public static final String DEBUG_REPO_REQUEST = PREFIX + "repo.debug";
  public static final boolean  DEFAULT_DEBUG_REPO_REQUEST = false;

  private static final Logger log = Logger.getLogger(V2AuMover.class);


  /**
   * The v2 Repository Client
   */
  private V2RestClient repoClient;

  /**
   * The v2 REST collections api implemntation
   */
  private StreamingCollectionsApi rsCollectionsApi;

  /**
   *  The v2 REST status api implementation
   */
  private org.lockss.laaws.api.rs.StatusApi rsStatusApi;

  /**
   * The v2 Repository Spec
   */
  private RepoSpec repoSpec;

  /**
   * The v2 Collection
   */
  private String collection;

  // Access information for the V2 Rest Repository
  private String rsRestLocation = null;
  private String rsUser = null;
  private String rsPass = null;

  private final String cliendId =
    org.apache.commons.lang3.RandomStringUtils.randomAlphabetic(8);

  private long reqId = 0;

  /**
   * The v2 configuration client for configuration service access
   */
  private V2RestClient configClient;

  /**
   * The v2 configuration api we use
   */
  private AusApi cfgAusApi;
  private org.lockss.laaws.api.cfg.StatusApi cfgStatusApi;


  // Access information for the V2 Configuration Service
  private String cfgRestLocation = null;
  private String cfgUser = null;
  private String cfgPass = null;

  /**
   * User Agent
   */
  private final String userAgent;

  // counters
  public long cuMoved=0;
  public long cuVersionsMoved=0;

  // debug support
  private boolean debugRepoReq;
  private boolean debugConfigReq;



  public V2AuMover() {
    this(null, null, null, null);
  }


  /**
   * The primary constructor for a V2RepoAuCopier
   * @param rspec The v2 RepoSpec string
   * @param ruser The v2 login user
   * @param rpass The v2 login password
   */
  public V2AuMover(String rspec, String ruser, String rpass, String cfgService) {
    Configuration config = ConfigManager.getCurrentConfig();
    if (rspec == null) {
      rspec = config.get(PARAM_V2_REPO_SPEC);
    }
    rsUser = (ruser == null) ? config.get(PARAM_V2_USER) : ruser;
    rsPass = (rpass == null) ? config.get(PARAM_V2_PASSWD) : rpass;

    if (cfgService == null) {
      cfgService = config.get(PARAM_CFG_ACCESS_URL);
    }
    userAgent = config.get(PARAM_V2_USER_AGENT, DEFAULT_V2_USER_AGENT);
    debugRepoReq = config.getBoolean(DEBUG_REPO_REQUEST, DEFAULT_DEBUG_REPO_REQUEST);
    debugConfigReq = config.getBoolean(DEBUG_CONFIG_REQUEST, DEFAULT_DEBUG_CONFIG_REQUEST);
    initRepoClient(rspec);
    initConfigClient(cfgService);
  }

  /**
   * Move one au as identified by the name of the au
   * @param auId The ArchivalUnit Id string
   */
  public void moveAu(String auId) throws IOException {
    ArchivalUnit au = LockssDaemon.getLockssDaemon().getPluginManager().getAuFromId(auId);
    moveAu(au);
  }

  public void moveAu(ArchivalUnit au) throws IOException {
    String auId = au.getAuId();
    String auName = au.getName();
    log.debug("Handling request to move AU: "+ auName);
    try {
      log.debug("Checking V2 Repository Status");
      if (!rsStatusApi.getStatus().isReady()) {
        log.error("V2 Repository Service Status: NOT READY");
        throw new IOException(auName+ ": Unable to move au. V2 Repository Service is not ready.");
      }
      log.debug("Checking V2 Configuration Status");
      if(!cfgStatusApi.getStatus().isReady()) {
        log.error("V2 Configuration Service: NOT READY");
        throw new IOException(auName+ ": Unable to move au. V2 Configuration Service is not ready.");
      }
      log.debug(auName+ ": Moving AU Artifacts...");
      moveAuArtifacts(au);
      log.debug(auName+ ": Moving AU State...");
      moveAuState(au);
      log.debug(auName+ ": Moving AU Configuration...");
      moveAuConfig(au);
    }
    catch (ApiException apie) {
      log.error("Attempt to move Au " + auName + " failed:" + apie.getCode() + ": " + apie.getResponseBody());
      throw new IOException("Attempt to move Au " + auName + " failed:" + apie.getCode()+ ": " + apie.getResponseBody());
    }
  }

  /**
   * Initialization for Rest Bepository client
   * @param rspec The v2 RepoSpec string
   */
  protected void initRepoClient(String rspec) {
    log.debug3("RepoSpec="+rspec);
    this.repoSpec=RepoSpec.fromSpec(rspec);
    this.collection=repoSpec.getCollection();
    rsRestLocation = repoSpec.getUrl();
    if(UrlUtil.isMalformedUrl(rsRestLocation)) {
      log.error("Malformed repository service url: " + cfgRestLocation);
      throw new IllegalArgumentException("RepoSpec contained malformed url: "+ rsRestLocation);
    }
    log.debug3("Setting user: "+ rsUser + "setting password: " + rsPass);
    // Create a new RepoClient
    repoClient = new V2RestClient();
    repoClient.setUsername(rsUser);
    repoClient.setPassword(rsPass);
    repoClient.setUserAgent(userAgent);
    repoClient.setBasePath(rsRestLocation);
    repoClient.setDebugging(debugRepoReq);
    // Assign client to CollectionsApi and StatusApi
    rsStatusApi= new org.lockss.laaws.api.rs.StatusApi(repoClient);
    rsCollectionsApi = new StreamingCollectionsApi(repoClient);
  }

  /**
   * Initialization of the Configuration Service Client
   * @param accessUrl the url in with or without user:pass
   */
  protected void initConfigClient(String accessUrl) {
    configClient = new V2RestClient();
    log.debug("Configuration Service: "+accessUrl);
    parseConfigServiceAccessUrl(accessUrl);
    if(cfgRestLocation == null || UrlUtil.isMalformedUrl(cfgRestLocation)) {
      log.error("Missing or Invalid configuration service url: " + cfgRestLocation);
      throw new IllegalArgumentException("RestConfigurationService Url is malformed: "+ rsRestLocation);
    }
    if(cfgUser == null)
      cfgUser = rsUser;
    if(cfgPass == null)
      cfgPass = rsPass;
    configClient.setUsername(cfgUser);
    configClient.setPassword(cfgPass);
    configClient.setUserAgent(userAgent);
    configClient.setBasePath(cfgRestLocation);
    configClient.setDebugging(debugConfigReq);
    // Assign the client to the status api and aus api
    cfgStatusApi = new org.lockss.laaws.api.cfg.StatusApi(configClient);
    cfgAusApi = new AusApi(configClient);
  }

  /**
   * Move one AU
   * @param au The ArchivalUnit to move
   */
  protected void moveAuArtifacts(ArchivalUnit au) throws ApiException {
    long startTime = System.currentTimeMillis(); // Get the start Time
    long endTime;
    /* get Au items from Lockss*/
    for (CuIterator iter = au.getAuCachedUrlSet().getCuIterator(); iter.hasNext(); ) {
      CachedUrl cachedUrl = iter.next();
      moveCuVersions(cachedUrl);
      cuMoved++;
    }
    endTime=System.currentTimeMillis();
    log.info(au.getName() + ": Successfully moved AU Artifacts.");
    log.info("CachedUrls Moved: " + cuMoved + "     Artifacts Moved: " +cuVersionsMoved +
      "   runTime (secs): " + (endTime - startTime) /1000);
  }

  protected void moveAuConfig(ArchivalUnit au) throws ApiException {
    Configuration v1config = au.getConfiguration();
    AuConfiguration v2config = new AuConfiguration().auId(au.getAuId());

    if (v1config != null) {
      // copy the keys
      v1config.keySet().stream().filter(key -> !key.equalsIgnoreCase("reserved.repository"))
        .forEach(key -> v2config.putAuConfigItem(key, v1config.get(key)));
      // send the configuration
      cfgAusApi.putAuConfig(v2config);
      log.info(au.getName() + ": Successfully moved AU Configuration");
    }
    else {
      // TODO: should this be an error or a warning?
      log.warning(au.getName() + ": No Configuration found for au");
    }
  }

  protected void moveAuState(ArchivalUnit au) throws ApiException{
    AuState v1State = AuUtil.getAuState(au);
    if (v1State != null) {
      V2AuStateBean v2State = new V2AuStateBean(v1State);
      cfgAusApi.patchAuState(v2State.toMap(), au.getAuId(), makeCookie());
    }
  }


  private void moveCuVersions(CachedUrl cachedUrl) throws ApiException {
    String auid = cachedUrl.getArchivalUnit().getAuId();
    CachedUrl[] cu_vers = cachedUrl.getCuVersions();
    for (CachedUrl cu : cu_vers) {
      String uri = cu.getUrl();
      Long collectionDate = Long
          .parseLong(cu.getProperties().getProperty(CachedUrl.PROPERTY_FETCH_TIME));
        Artifact response = moveArtifact(auid, uri, collectionDate, cu, collection);
        log.debug3("Moved version " + cu.getVersion() + " has repo id: " + response.getId());
        cuVersionsMoved++;
    }
    log.debug2("Completed move of all versions of " + cachedUrl.getUrl());
  }

  private Artifact moveArtifact(String auid, String uri, Long collectionDate,
      CachedUrl cu, String collectionId) throws ApiException {
    Artifact uncommitted;
    Artifact commited = null;
    uncommitted = rsCollectionsApi.createArtifact(auid, uri, collectionDate, cu, collectionId);
    if(uncommitted != null)
      commited = rsCollectionsApi.updateArtifact(true,uncommitted.getCollection(), uncommitted.getId());
    return commited;
  }

  // cfg utilities
  /**
   * Saves the individual components of the Configuration REST web service URL.
   */
  private void parseConfigServiceAccessUrl(String accessUrl) {
    final String DEBUG_HEADER = "parseConfigServiceAccessUrl(): ";

    // Ignore missing information about the Configuration REST web service.
    if (StringUtil.isNullString(accessUrl)) {
      return;
    }

    try {
      URL url = new URL(accessUrl);

      // Get the passed credentials.
      String credentialsAsString = url.getUserInfo();
      if (log.isDebug3()) log.debug3(DEBUG_HEADER
        + "credentialsAsString = " + credentialsAsString);

      // Check whether credentials were passed.
      if (StringUtil.isNullString(credentialsAsString)) {
        // No.
        cfgRestLocation = accessUrl;
      } else {
        // Yes: Parse them.
        parseCredentials(credentialsAsString);
        // Get the service location.
        cfgRestLocation = new URL(url.getProtocol(), url.getHost(),
          url.getPort(), url.getFile()).toString();
      }
    } catch (MalformedURLException mue) {
      log.error("Error parsing REST Configuration Service URL: "
        + mue);

      cfgRestLocation = null;
      cfgUser = null;
      cfgPass = null;
    }
    log.info("REST Configuration service location = " + cfgRestLocation);
  }

  private void parseCredentials(String credentialsAsString) {
    final String DEBUG_HEADER = "parseCredentials(): ";
    Vector<String> credentials = StringUtil.breakAt(credentialsAsString, ":");

    if (credentials != null && credentials.size() == 2) {
      cfgUser = credentials.get(0);
      cfgPass = credentials.get(1);
      if (log.isDebug3()) {
        log.debug3(DEBUG_HEADER + "serviceUser : servicePassword = "
          + cfgUser + " : " + cfgUser);
      }
    }
  }

  private String makeCookie() {
    return cliendId + "-" + ++reqId;
  }
}
