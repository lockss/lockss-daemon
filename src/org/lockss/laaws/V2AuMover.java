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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Vector;
import java.util.stream.Collectors;
import okhttp3.Dispatcher;
import org.lockss.app.LockssDaemon;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.laaws.api.cfg.AusApi;
import org.lockss.laaws.api.rs.StreamingCollectionsApi;
import org.lockss.laaws.client.ApiCallback;
import org.lockss.laaws.client.ApiException;
import org.lockss.laaws.client.V2RestClient;
import org.lockss.laaws.model.cfg.AuConfiguration;
import org.lockss.laaws.model.cfg.V2AuStateBean;
import org.lockss.laaws.model.rs.Artifact;
import org.lockss.laaws.model.rs.ArtifactPageInfo;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.CuIterator;
import org.lockss.plugin.PluginManager;
import org.lockss.repository.RepoSpec;
import org.lockss.state.AuState;
import org.lockss.util.Constants;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import org.lockss.util.UrlUtil;

public class V2AuMover {

  public static final String PREFIX = Configuration.PREFIX + "v2.";
  public static final String PARAM_V2_REPO_SPEC = PREFIX + "repo.spec";
  public static final String PARAM_CFG_ACCESS_URL = PREFIX + "cfg.url";

  public static final String PARAM_V2_USER_AGENT = PREFIX + "user_agent";
  public static final String DEFAULT_V2_USER_AGENT = "lockss";

  public static final String PARAM_V2_USER = PREFIX + "user";
  public static final String PARAM_V2_PASSWD = PREFIX + "passwd";


  public static final String DEBUG_CONFIG_REQUEST = PREFIX + "cfg.debug";
  public static final boolean DEFAULT_DEBUG_CONFIG_REQUEST = false;

  public static final String DEBUG_REPO_REQUEST = PREFIX + "repo.debug";
  public static final boolean DEFAULT_DEBUG_REPO_REQUEST = false;

  public static final String PARAM_MAX_REQUESTS = PREFIX + "max_requests";
  public static final int DEFAULT_MAX_REQUESTS = 50;

  private static final Logger log = Logger.getLogger(V2AuMover.class);
  private final int maxRequests;

  private static final String THREADPOOL_PREFIX = PREFIX + "threadPool.";

  /** Max number of background threads running UI-initiated hashes */
  static final String PARAM_THREADPOOL_SIZE =
    THREADPOOL_PREFIX + "size";
  static final int DEFAULT_THREADPOOL_SIZE = 2;

  /** Duration after which idle threads will be terminated..  -1 = never */
  public static final String PARAM_THREADPOOL_KEEPALIVE =
    THREADPOOL_PREFIX + "keepAlive";
  static final long DEFAULT_THREADPOOL_KEEPALIVE = 5 * Constants.MINUTE;

  private static V2AuMover instance;

  /**
   * The v2 Repository Client
   */
  private V2RestClient repoClient;

  /**
   * The v2 REST collections api implemntation
   */
  private StreamingCollectionsApi rsCollectionsApi;

  /**
   * The v2 REST status api implementation
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
  private final String rsUser;
  private final String rsPass;

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
  public long cuMoved = 0;
  public long cuVersionsMoved = 0;

  // debug support
  private final boolean debugRepoReq;
  private final boolean debugConfigReq;

  private String currentAu;
  private String currentCu;
  private PluginManager pluginManager;
  private Queue<ArchivalUnit> auMoveQueue=new LinkedList<>();
  private Dispatcher dispatcher;
  private long startTime;
  private int auCount = 0;
  boolean allCusQueued=false;

  //---------------
  // Construction
  //---------------
  public static V2AuMover getInstance() {
    if (instance == null) {
      synchronized (V2AuMover.class) {
        if (instance == null) {
          instance = new V2AuMover();
        }
      }
    }
    return instance;
  }

  private V2AuMover() {
    this(null, null, null, null);
  }

  /**
   * The primary constructor for a V2RepoAuCopier
   *
   * @param rspec The v2 RepoSpec string
   * @param ruser The v2 login user
   * @param rpass The v2 login password
   */
  private V2AuMover(String rspec, String ruser, String rpass, String cfgService) {
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
    maxRequests = config.getInt(PARAM_MAX_REQUESTS, DEFAULT_MAX_REQUESTS);
    pluginManager = LockssDaemon.getLockssDaemon().getPluginManager();
    initRepoClient(rspec);
    initConfigClient(cfgService);
  }

  /**
   * Initialization for Rest Bepository client
   *
   * @param rspec The v2 RepoSpec string
   */
  private void initRepoClient(String rspec) {
    log.debug3("RepoSpec=" + rspec);
    this.repoSpec = RepoSpec.fromSpec(rspec);
    this.collection = repoSpec.getCollection();
    rsRestLocation = repoSpec.getUrl();
    if (UrlUtil.isMalformedUrl(rsRestLocation)) {
      log.error("Malformed repository service url: " + cfgRestLocation);
      throw new IllegalArgumentException("RepoSpec contained malformed url: " + rsRestLocation);
    }
    log.debug3("Setting user: " + rsUser + "setting password: " + rsPass);
    // Create a new RepoClient
    repoClient = new V2RestClient();
    repoClient.setUsername(rsUser);
    repoClient.setPassword(rsPass);
    repoClient.setUserAgent(userAgent);
    repoClient.setBasePath(rsRestLocation);
    repoClient.setDebugging(debugRepoReq);
    dispatcher = repoClient.getHttpClient().dispatcher();
//   dispatcher().setMaxRequests(maxRequests);
    log.debug("Dispatcher - max requests: " + dispatcher.getMaxRequests());
    // Assign client to CollectionsApi and StatusApi
    rsStatusApi = new org.lockss.laaws.api.rs.StatusApi(repoClient);
    rsCollectionsApi = new StreamingCollectionsApi(repoClient);
  }

  /**
   * Initialization of the Configuration Service Client
   *
   * @param accessUrl the url in with or without user:pass
   */
  private void initConfigClient(String accessUrl) {
    configClient = new V2RestClient();
    log.debug("Configuration Service: " + accessUrl);
    parseConfigServiceAccessUrl(accessUrl);
    if (cfgRestLocation == null || UrlUtil.isMalformedUrl(cfgRestLocation)) {
      log.error("Missing or Invalid configuration service url: " + cfgRestLocation);
      throw new IllegalArgumentException(
        "RestConfigurationService Url is malformed: " + rsRestLocation);
    }
    if (cfgUser == null) {
      cfgUser = rsUser;
    }
    if (cfgPass == null) {
      cfgPass = rsPass;
    }
    configClient.setUsername(cfgUser);
    configClient.setPassword(cfgPass);
    configClient.setUserAgent(userAgent);
    configClient.setBasePath(cfgRestLocation);
    configClient.setDebugging(debugConfigReq);
    // Assign the client to the status api and aus api
    cfgStatusApi = new org.lockss.laaws.api.cfg.StatusApi(configClient);
    cfgAusApi = new AusApi(configClient);
  }

  //-------------------------
  // Primary Public Functions
  // -------------------------

  /**
   * Move all Aus that are not currently in the move queue.
   * @throws IOException if error occurred while moving au.
   */
  public void moveAllAus() throws IOException {
    for (ArchivalUnit au: pluginManager.getAllAus()) {
      if (!auMoveQueue.contains(au)) {
        auMoveQueue.add(au);
      }
    }
    log.debug("Moving " + auMoveQueue.size() + " aus.");
    // Check to see if we are currently working on an au.
    if(currentAu == null)
      moveNextAu();
  }



  /**
   * Move one au as identified by the name of the au
   * iff it's not already in the queue
   * @param auId The ArchivalUnit Id string
   */
  public void moveOneAu(String auId) throws IOException {
    ArchivalUnit au = pluginManager.getAuFromId(auId);
    if(au != null) {
      moveOneAu(au);
    }
  }

  /**
   * Move the requested Au
   * iff it's not already in the queue.
   * @param au the ArchivalUnit to move
   * @throws IOException
   */
  public void moveOneAu(ArchivalUnit au) throws IOException {
    if (!auMoveQueue.contains(au)) {
      auMoveQueue.add(au);
    }
    // if we aren't working on an au move the next au.
    if(currentAu == null)
      moveNextAu();
  }


  protected void moveNextAu() throws IOException {
    ArchivalUnit au = auMoveQueue.poll();
    if(au != null) {
      allCusQueued=false;
      cuVersionsMoved=0;
      cuMoved=0;
      log.debug("Moving " + au.getName() + " - " + auMoveQueue.size() +" remaining.");
      currentAu = au.getAuId();
      //todo: turn off au crawling?
      //todo: turn off au polling?
      moveAu(au);
    }
  }

  protected void moveAu(ArchivalUnit au) throws IOException {
    startTime = System.currentTimeMillis(); // Get the start Time
    String auName = au.getName();
    log.info("Handling request to move AU: " + auName);
    log.info("AuId: " + currentAu);
    try {
      log.info("Checking V2 Repository Status");
      if (!rsStatusApi.getStatus().getReady()) {
        log.error("V2 Repository Service Status: NOT READY");
        throw new IOException(auName + ": Unable to move au. V2 Repository Service is not ready.");
      }
      log.info("Checking V2 Configuration Status");
      if (!cfgStatusApi.getStatus().getReady()) {
        log.error("V2 Configuration Service: NOT READY");
        throw new IOException(
          auName + ": Unable to move au. V2 Configuration Service is not ready.");
      }
      log.info(auName + ": Moving AU Artifacts...");
      moveAuArtifacts(au);
      finishAuMove(au);
    } catch (ApiException apie) {
      log.error(auName + ": Attempt to move Au failed:" + apie.getCode() + ": "
        + apie.getMessage());
    }
  }


  /**
   *  Move one V1 Au including all cachedUrls and all versions.
   * @param au au The ArchivalUnit to move
   * @throws ApiException
   */
  protected void moveAuArtifacts(ArchivalUnit au) throws ApiException {
    //Get the au artifacts from the repo.
    List<Artifact> auArtifacts= new ArrayList<>();
    ArtifactPageInfo pageInfo;
    try {
      String token = null;
      do {
        pageInfo = rsCollectionsApi.getArtifacts(collection, au.getAuId(),
          null, null, null, false, null, token);
        auArtifacts.addAll(pageInfo.getArtifacts());
        token= pageInfo.getPageInfo().getContinuationToken();
      } while (!StringUtil.isNullString(token));
    }
    catch(ApiException apie) {
      log.warning("Unable to determine if repo has prexisting artifacts for " + au.getName());
      log.warning("All CachedUrls will be added");
    }
    /* get Au cachedUrls from Lockss*/
    for (CuIterator iter = au.getAuCachedUrlSet().getCuIterator(); iter.hasNext(); ) {
      CachedUrl cachedUrl = iter.next();
      currentCu = cachedUrl.getUrl();
      List<Artifact> cuArtifacts = auArtifacts.stream()
        .filter(artifact -> artifact.getUri().equals(currentCu))
        .collect(Collectors.toList());
      if(log.isDebug2())
        log.debug2(currentCu + ": Found " + cuArtifacts.size() + " previously moved artifacts");
      moveCuVersions(cachedUrl, cuArtifacts);
      cuMoved++;
    }
    allCusQueued=true;
   }


  /**
   * Move all versions of a cachedUrl.
   * @param cachedUrl The cachedUrl we which to move
   * @param cuArtifacts The list of artifacts which already match this cachedUrl uri.
   */
  private void moveCuVersions(CachedUrl cachedUrl, List<Artifact> cuArtifacts) {
    String auid = cachedUrl.getArchivalUnit().getAuId();
    String uri = cachedUrl.getUrl();
    CachedUrl[] localVersions = cachedUrl.getCuVersions();
    int start;
    if (localVersions.length > cuArtifacts.size()) {
      // we have at least one unmoved version
      start = cuArtifacts.size();
      // there are version of the cu which need to be moved.
      for (int i = start; i < localVersions.length; i++) {
        Long collectionDate = null;
        CachedUrl cu = localVersions[i];
        try {
          String fetchTime = cu.getProperties().getProperty(CachedUrl.PROPERTY_FETCH_TIME);
          if (!StringUtil.isNullString(fetchTime)) {
            collectionDate = Long.parseLong(fetchTime);
          }
          else {
            log.debug(uri + ":version: " + cu.getVersion() + " is missing fetch time.");
          }
          log.debug2("Moving cu version " + cu.getVersion() + " - fetched at " + fetchTime);
          createArtifact(auid, uri, collectionDate, cu, collection);
        } catch (ApiException apie) {
          log.error(uri + ": failed to move version: " + cu.getVersion() + ": " +
            apie.getCode() + " - " + apie.getMessage());
        } finally {
          AuUtil.safeRelease(cu);
        }
      }
    }
    log.debug2("Completed move of all versions of " + currentCu);
  }


  /**
   * Make an asynchronous rest call to the V2 repository to create a new artifact.
   * @param auid au identifer for the CachedUrl we are moving.
   * @param uri the uri  for the CachedUrl we are moving.
   * @param collectionDate the date at which this item was collected or null
   * @param cu the CachedUrl we are moving.
   * @param collectionId the v2 collection we are moving to
   * @throws ApiException the rest exception thrown should anything fail in the request.
   */
  private void createArtifact(String auid, String uri, Long collectionDate,
    CachedUrl cu, String collectionId) throws ApiException {
    rsCollectionsApi.createArtifactAsync(collectionId, auid, uri, cu, collectionDate,
      new CreateArtifactCallback());
  }

  /**
   * Make a synchronous rest call to commit the artifact that just completed successful creation.
   * @param uncommitted the v2 artifact to be committed
   * @throws ApiException the rest exception thrown should anything fail in the request.
   */
  private void commitArtifact(Artifact uncommitted) throws ApiException {
    Artifact committed;
    committed = rsCollectionsApi.updateArtifact(uncommitted.getCollection(),
      uncommitted.getId(), true);
    log.debug("Successfully committed artifact " + committed.getId());
    cuVersionsMoved++;
  }

  /**
   * Complete the au move by moving the state and config information.
   * @param au the au we are moving.
   * @throws IOException
   */
  private void finishAuMove(ArchivalUnit au) throws IOException{
    do { //Wait until we are done the processing
      try {
        Thread.sleep(200);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    } while (dispatcher.runningCallsCount() != 0 );
    String auName = au.getName();
    log.info(auName + ": Moving AU State...");
    moveAuState(au);
    log.info(auName + ": Moving AU Configuration...");
    moveAuConfig(au);
    log.info(auName + ": Successfully moved AU Artifacts.");
    if (log.isDebug()) {
      long endTime = System.currentTimeMillis();
      log.debug("CachedUrls Moved: " + cuMoved + "     Artifacts Moved: " + cuVersionsMoved +
        "   runTime (secs): " + (endTime - startTime) / 1000);
    }
    // we are no longer actively working on any au so null out the currentAu.
    currentAu=null;
    moveNextAu();
  }

  /**
   * Make a synchronous rest call to configuration service to add configuration info for an au.
   * @param au The ArchivalUnit whose configuration is to be move
   */
  private void moveAuConfig(ArchivalUnit au) {
    Configuration v1config = au.getConfiguration();
    AuConfiguration v2config = new AuConfiguration().auId(au.getAuId());
    String auName=au.getName();
    if (v1config != null) {
      try {
        // copy the keys
        v1config.keySet().stream().filter(key -> !key.equalsIgnoreCase("reserved.repository"))
          .forEach(key -> v2config.putAuConfigItem(key, v1config.get(key)));
        // send the configuration
        cfgAusApi.putAuConfig(au.getAuId(), v2config);
        log.info(auName + ": Successfully moved AU Configuration.");
      } catch (ApiException apie) {
        log.error(auName + ": Attempt to move au configuration failed: " + apie.getCode() +
          "- " + apie.getMessage());
      }
    } else {
      log.warning(auName + ": No Configuration found for au");
    }
  }

  /**
   * Make a synchronous rest call to configuration service to add state info for an au.
   * @param au The ArchivalUnit  to move
   */
  private void moveAuState(ArchivalUnit au) {
    AuState v1State = AuUtil.getAuState(au);
    String auName=au.getName();
    if (v1State != null) {
      try {
        V2AuStateBean v2State = new V2AuStateBean(v1State);
        cfgAusApi.patchAuState(au.getAuId(), v2State.toMap(), makeCookie());
        log.info(auName + ": Successfully moved AU State.");
      } catch (ApiException apie) {
        log.error(auName + ": Attempt to move au state failed: " + apie.getCode() +
          "- " + apie.getMessage());
      }
    }
    else {
      log.warning(auName + ": No State information found for au");
    }
  }
  // config service utilities

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
      if (log.isDebug3()) {
        log.debug3(DEBUG_HEADER
          + "credentialsAsString = " + credentialsAsString);
      }

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

  /**
   * A simple class to encompass an ApiCallback
   */
  protected class CreateArtifactCallback implements ApiCallback<Artifact> {

    @Override
    public void onFailure(ApiException e, int statusCode,
      Map<String, List<String>> responseHeaders) {
      log.error("Create Artifact request failed: " + statusCode + " - " + e.getMessage());
    }

    @Override
    public void onSuccess(Artifact result, int statusCode,
      Map<String, List<String>> responseHeaders) {
      try {
        log.debug("Successfully created artifact (" + statusCode + "): " + result.getId());
        commitArtifact(result);
      } catch (ApiException e) {
        log.error("Attempt to commit artifact failed: " + e.getCode() + " - " + e.getMessage());
      }
    }

    @Override
    public void onUploadProgress(long bytesWritten, long contentLength, boolean done) {
      log.debug3("Create Artifact uploaded " + bytesWritten + " of " + contentLength + "bytes..");
      if (done) {
        log.debug2("Create Artifact upload of " + bytesWritten + " complete.");
      }
    }

    @Override
    public void onDownloadProgress(long bytesRead, long contentLength, boolean done) {
      log.debug3("Create Artifact downloaded " + bytesRead + " of " + contentLength + "bytes..");
      if (done) {
        log.debug2("Create Artifact download " + bytesRead + "  complete");
      }

    }
  }
}
