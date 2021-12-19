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
import org.lockss.plugin.*;
import org.lockss.state.AuState;
import org.lockss.uiapi.util.DateFormatter;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import org.lockss.util.TimeBase;
import org.lockss.util.UrlUtil;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.APPEND;

public class V2AuMover {

  static final String PREFIX = Configuration.PREFIX + "v2.migrate.";

  public static final String PARAM_V2_USER_AGENT = PREFIX + "user_agent";
  public static final String DEFAULT_V2_USER_AGENT = "lockss";

  public static final String DEBUG_CONFIG_REQUEST = PREFIX + "cfg.debug";
  public static final boolean DEFAULT_DEBUG_CONFIG_REQUEST = false;

  public static final String DEBUG_REPO_REQUEST = PREFIX + "repo.debug";
  public static final boolean DEFAULT_DEBUG_REPO_REQUEST = false;

  public static final String PARAM_MAX_REQUESTS = PREFIX + "max_requests";
  public static final int DEFAULT_MAX_REQUESTS = 50;

  public static final String PARAM_V2_COLLECTION = PREFIX + "collection";
  public static final String DEFAULT_V2_COLLECTION = "lockss";

  public static final String PARAM_HOSTNAME = PREFIX + "hostname";
  public static final String DEFAULT_HOSTNAME = "localhost";

  public static final String PARAM_RS_PORT = PREFIX + "rs.port";
  public static final int DEFAULT_RS_PORT = 24610;

  public static final String PARAM_CFG_PORT = PREFIX + "cfg.port";
  public static final int DEFAULT_CFG_PORT = 24620;
  /** Path to directory holding daemon logs */
  public static final String PARAM_REPORT_DIR =
    ConfigManager.PARAM_PLATFORM_LOG_DIR;
  public static final String DEFAULT_REPORT_DIR = "/tmp";

  public static final String PARAM_REPORT_FILE= PREFIX + "report.file";
  public static final String DEFAULT_REPORT_FILE = "v2migration.txt";
  private static final Logger log = Logger.getLogger(V2AuMover.class);
  // The format of a date as required by the export output file name.
  private static final SimpleDateFormat dateFormat = new SimpleDateFormat(
    "yyyy-MM-dd-HH-mm-ss");

  private final String cliendId =
    org.apache.commons.lang3.RandomStringUtils.randomAlphabetic(8);

  /**
   * The v2 Repository Client
   */
  private final V2RestClient repoClient;

  /**
   * The v2 REST collections api implemntation
   */
  private final StreamingCollectionsApi rsCollectionsApiClient;

  /**
   * The v2 REST status api implementation
   */
  private final org.lockss.laaws.api.rs.StatusApi rsStatusApiClient;

  /**
   * The v2 Collection
   */
  private final String collection;

  /**
   * The v2 configuration client for configuration service access
   */
  private final V2RestClient configClient;

  /**
   * The v2 configuration api we use
   */
  private final AusApi cfgAusApiClient;
  private final org.lockss.laaws.api.cfg.StatusApi cfgStatusApiClient;


  /**
   * User Agent
   */
  private final String userAgent;

  /**
   * The base host used to access v2 service
   */
  private String hostName;
  /**
   * The user name used to access v2 service
   */
  private String userName;
  /**
   * The user password used to access v2 service
   */
  private String userPass;
  /**
   * The configuration service port
   */
  private final int cfgPort;
  /**
   * The repository service port
   */
  private final int rsPort;

  private File reportFile;

  // Access information for the V2 Configuration Service
  private String cfgAccessUrl = null;
  // Access information for the V2 Rest Repository
  private String rsAccessUrl = null;
  private int maxRequests;

  // timer
  private long startTime;

  // debug support
  private boolean debugRepoReq;
  private boolean debugConfigReq;

  PluginManager pluginManager;
  private LinkedHashSet<ArchivalUnit> auMoveQueue=new LinkedHashSet<>();
  private Dispatcher dispatcher;
  boolean allCusQueued=false;
  private long reqId = 0;

  // report
  private String currentAu;
  private String currentUrl;

  private PrintWriter reportWriter;

  // counters for total run
  private long totalAusMoved =0;
  private long totalUrlsMoved = 0;
  private long totalArtifactsMoved = 0;
  private long totalBytesMoved = 0;
  private long totalRunTime = 0;
  private long totalErrorCount = 0;

  // Counters for a single au
  private long auUrlsMoved =0;
  private long auArtifactsMoved =0;
  private long auBytesMoved=0;
  private long auRunTime=0;
  private long auErrorCount=0;
  private List<String> auErrors= new ArrayList<>();
  private List<String> errorList = new ArrayList<>();


  public V2AuMover() {
    Configuration config = ConfigManager.getCurrentConfig();
    pluginManager = LockssDaemon.getLockssDaemon().getPluginManager();
    // support for overriding our defaults
    userAgent = config.get(PARAM_V2_USER_AGENT, DEFAULT_V2_USER_AGENT);
    collection = config.get(PARAM_V2_COLLECTION, DEFAULT_V2_COLLECTION);
    cfgPort = config.getInt(PARAM_CFG_PORT, DEFAULT_CFG_PORT);
    rsPort = config.getInt(PARAM_RS_PORT, DEFAULT_RS_PORT);
    String logdir = config.get(PARAM_REPORT_DIR, DEFAULT_REPORT_DIR);
    String logfile = config.get(PARAM_REPORT_FILE, DEFAULT_REPORT_FILE);
    reportFile = new File(logdir, logfile);
    repoClient = new V2RestClient();
    rsStatusApiClient = new org.lockss.laaws.api.rs.StatusApi(repoClient);
    rsCollectionsApiClient = new StreamingCollectionsApi(repoClient);

    configClient = new V2RestClient();
    // Assign the client to the status api and aus api
    cfgStatusApiClient = new org.lockss.laaws.api.cfg.StatusApi(configClient);
    cfgAusApiClient = new AusApi(configClient);
  }

  /**
   * Compile a list of regular expression into a list of Patterns
   * @param regexps the list of java regular expressions
   * @return a list of java regular patterns
   */
  static public List<Pattern> compileRegexps(List<String> regexps) {
    List<Pattern> res = new ArrayList<>();
    for (String re : regexps) {
      res.add(Pattern.compile(re));
    }
    return res;
  }

  /**
   * Return true if string matches any of a list of Patterns
   * @param str the string we are looking for
   * @param pats the list of compiled java patterns
   * @return true if string matches any pattern, false otherwise.
   */
  static public boolean isMatch(String str, List<Pattern> pats) {
    for (Pattern pat : pats) {
      Matcher matcher = pat.matcher(str);
      if (matcher.find()) {
        return true;
      }
    }
    return false;
  }

  //-------------------------
  // Primary Public Functions
  // -------------------------
  /**
   * Move all Aus that are not currently in the move queue.
   * @throws IOException if error occurred while moving au.
   */
  public void moveAllAus(String host, String uname, String upass,  List<Pattern> selPatterns) throws IOException {
    initRequest(host, uname, upass);
    for (ArchivalUnit au : pluginManager.getAllAus()) {
      if (pluginManager.isInternalAu(au)) {
        continue;
      }
      if (selPatterns == null || selPatterns.isEmpty() ||
          isMatch(au.getAuId(), selPatterns)) {
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
  public void moveOneAu(String host, String uname, String upass,String auId) throws IOException {
    ArchivalUnit au = pluginManager.getAuFromId(auId);
    if(au != null) {
      moveOneAu(host, uname, upass, au);
    }
  }

  public boolean isAvailable() {
    return auMoveQueue.isEmpty();
  }

  public List<String> getErrors() {
    return errorList;
  }

  /**
   * Move the requested Au
   * iff it's not already in the queue.
   * @param au the ArchivalUnit to move
   * @throws IOException if unable to connect to services
   */
  public void moveOneAu(String host, String uname, String upass, ArchivalUnit au) throws IOException {
    initRequest(host, uname, upass);
    auMoveQueue.add(au);
    // if we aren't working on an au move the next au (sync this).
    if(currentAu == null)
      moveNextAu();
  }

  public void initRequest(String host, String uname, String upass) throws
    IllegalArgumentException
  {
    errorList.clear();
    Configuration config = ConfigManager.getCurrentConfig();
    // allow for changing these between runs when testing
    debugRepoReq = config.getBoolean(DEBUG_REPO_REQUEST, DEFAULT_DEBUG_REPO_REQUEST);
    debugConfigReq = config.getBoolean(DEBUG_CONFIG_REQUEST, DEFAULT_DEBUG_CONFIG_REQUEST);
    maxRequests = config.getInt(PARAM_MAX_REQUESTS, DEFAULT_MAX_REQUESTS);
    hostName = config.get(PARAM_HOSTNAME, DEFAULT_HOSTNAME);

    if(host != null) {
      hostName =host;
    }
    if(uname != null) {
      userName=uname;
    }
    if(upass != null) {
      userPass =upass;
    }
    if(userName== null || userPass == null) {
      errorList.add("Missing user name or password.");
      throw new IllegalArgumentException("Missing user name or password");
    }
    try {
      cfgAccessUrl=new URL("http", hostName, cfgPort,"").toString();
      if (cfgAccessUrl == null || UrlUtil.isMalformedUrl(cfgAccessUrl)) {
        errorList.add("Missing or Invalid configuration service url: " + cfgAccessUrl);
        throw new IllegalArgumentException(
          "Missing or Invalid configuration service url: " + cfgAccessUrl);
      }
      configClient.setUsername(userName);
      configClient.setPassword(userPass);
      configClient.setUserAgent(userAgent);
      configClient.setBasePath(cfgAccessUrl);
      configClient.setDebugging(debugConfigReq);
    } catch (MalformedURLException mue) {
      errorList.add("Error parsing REST Configuration Service URL: "
        + mue.getMessage());
      throw new IllegalArgumentException(
        "Missing or Invalid configuration service url: " + cfgAccessUrl);
    }

    try {
      rsAccessUrl=new URL("http", hostName, rsPort, "").toString();
      if (rsAccessUrl == null || UrlUtil.isMalformedUrl(rsAccessUrl)) {
        errorList.add("Missing or Invalid repository service url: " + rsAccessUrl);
        throw new IllegalArgumentException(
          "Missing or Invalid configuration service url: " + rsAccessUrl);
      }
      // Create a new RepoClient
      repoClient.setUsername(userName);
      repoClient.setPassword(userPass);
      repoClient.setUserAgent(userAgent);
      repoClient.setBasePath(rsAccessUrl);
      repoClient.setDebugging(debugRepoReq);
      dispatcher = repoClient.getHttpClient().dispatcher();
      dispatcher.setMaxRequests(maxRequests);
    } catch (MalformedURLException mue) {
      errorList.add("Error parsing REST Configuration Service URL: " + mue.getMessage());
      throw new IllegalArgumentException(
        "Missing or Invalid configuration service url: " + rsAccessUrl);
    }

    startReportFile();
    totalArtifactsMoved =0;
    totalUrlsMoved =0;
    totalBytesMoved=0;
    totalRunTime=0;
    startTime=System.currentTimeMillis();
  }


  void startReportFile() {

    try {
      log.info("Writing report to file "+ reportFile.getAbsolutePath());
      reportWriter = new PrintWriter(Files.newOutputStream(reportFile.toPath(), CREATE, APPEND),true);
      reportWriter.println("--------------------------------------------------");
      reportWriter.println("  V2 Au Migration Report - "+ DateFormatter.now());
      reportWriter.println("--------------------------------------------------");
      reportWriter.println();
      if(reportWriter.checkError()) {
        log.warning("Error writing report file.");
      }
    }
    catch (IOException e) {
      log.error("Report file will not be written: Unable to open report file:"+ e.getMessage());
    }
  }

  void updateReport() {
    String auData = "Au:" + currentAu +
      "  urlsMoved: " + auUrlsMoved +
      "  artifactsMoved: " + auArtifactsMoved +
      "  bytesMoved: " + auBytesMoved +
      "  errors: " + auErrorCount +
      "  totalRuntime: " + StringUtil.timeIntervalToString(auRunTime);
    if(reportWriter != null) {
      reportWriter.println(auData);
      for(String err : auErrors) {
        reportWriter.println(err);
      }
      reportWriter.println();
      if(reportWriter.checkError()) {
        log.warning("Error writing report file.");
      }
    }
    log.info(auData);
    for(String err : auErrors) {
      log.error(err);
    }
  }

  void closeReport() {
    String summary = "AusMoved: " + totalAusMoved +
      "  urlsMoved: "+ totalUrlsMoved +
      "  artifactsMoved: "+ totalArtifactsMoved +
      "  bytesMoved: " + totalBytesMoved +
      "  errors: " + totalErrorCount +
      "  totalRuntime: " + StringUtil.timeIntervalToString(totalRunTime);
    if(reportWriter != null) {
      reportWriter.println(summary);
      if(reportWriter.checkError()) {
        log.warning("Error writing report file.");
      }
      reportWriter.close();
    }
    log.info(summary);
  }

  protected void moveNextAu() throws IOException {
    if(auMoveQueue.iterator().hasNext()) {
      ArchivalUnit au = auMoveQueue.iterator().next();
      allCusQueued=false;
      log.debug("Moving " + au.getName() + " - " + auMoveQueue.size() +" remaining.");
      currentAu = au.getAuId();
      auBytesMoved=0;
      auUrlsMoved =0;
      auArtifactsMoved =0;
      auErrorCount=0;
      auErrors.clear();
      auRunTime=0;
      moveAu(au);
    }
    else {
      totalRunTime=System.currentTimeMillis() - startTime;
      closeReport();
    }
  }

  protected void moveAu(ArchivalUnit au) throws IOException {
    long au_move_start= System.currentTimeMillis(); // Get the start Time
    String auName = au.getName();
    log.info("Handling request to move AU: " + auName);
    log.info("AuId: " + currentAu);
    try {
      log.info("Checking V2 Repository Status");
      if (!rsStatusApiClient.getStatus().getReady()) {
        errorList.add(auName + ": Unable to move au. V2 Repository Service is not ready.");
        return;
      }
      log.info("Checking V2 Configuration Status");
      if (!cfgStatusApiClient.getStatus().getReady()) {
        errorList.add( auName + ": Unable to move au. V2 Configuration Service is not ready.");
        return;
      }
      log.info(auName + ": Moving AU Artifacts...");
      moveAuArtifacts(au);
      finishAuMove(au);
      log.info(auName + ": Successfully moved AU Artifacts.");
      auRunTime = System.currentTimeMillis() -  au_move_start;
      updateReport();
      //update our totals.
      totalAusMoved++;
      totalBytesMoved+=auBytesMoved;
      totalUrlsMoved += auUrlsMoved;
      totalArtifactsMoved += auArtifactsMoved;
      auMoveQueue.remove(au);
      currentAu=null;
      // add our totals then move to the next au.
      moveNextAu();
    } catch (ApiException apie) {
      String err=auName + ": Attempt to move Au failed:" + apie.getCode() + ": "
        + apie.getMessage();
      log.error(err);
      auErrors.add(err);
      errorList.add(err);
      auErrorCount++;
      finishAuMove(au);
    }
    totalErrorCount+=auErrorCount;
  }


  /**
   *  Move one V1 Au including all cachedUrls and all versions.
   * @param au au The ArchivalUnit to move
   * @throws ApiException if REST request results in errors
   */
  protected void moveAuArtifacts(ArchivalUnit au) throws ApiException {
    List<Artifact>cuArtifacts=new ArrayList<>();

    //Get the au artifacts from the v1 repo.
    /* get Au cachedUrls from Lockss*/
    for (CuIterator iter = au.getAuCachedUrlSet().getCuIterator(); iter.hasNext(); ) {
      CachedUrl cachedUrl = iter.next();
      currentUrl = cachedUrl.getUrl();
      String v2Url = null;
      try {
        v2Url = UrlUtil.normalizeUrl(cachedUrl.getProperties().getProperty(CachedUrl.PROPERTY_NODE_URL), au);
      } catch (Exception ex) {
        log.warning("Unable to normalize uri for " + currentUrl, ex);
      } finally {
        AuUtil.safeRelease(cachedUrl);
      }
      if (v2Url == null) {
        v2Url = currentUrl;
      }
      // we have the possibility that some or all of the artifacts were moved.
      // We looking for previously moved versions of the 'current' cu
      ArtifactPageInfo pageInfo;
      try {
        String token = null;
        do {
          pageInfo = rsCollectionsApiClient.getArtifacts(collection, au.getAuId(),
            v2Url, null, null, false, null, token);
          cuArtifacts.addAll(pageInfo.getArtifacts());
          token= pageInfo.getPageInfo().getContinuationToken();
        } while (!StringUtil.isNullString(token));
      }
      catch(ApiException apie) {
        log.warning("Unable to determine if repo has preexisting artifacts for " + currentUrl);
        log.warning("All CachedUrls will be added");
      }
      moveCuVersions(v2Url, cachedUrl, cuArtifacts);
      auUrlsMoved++;
    }
    allCusQueued=true;
   }


  /* ------------------
  testing getters & setters
 */
  void setAuCounters(long urls, long artifacts, long bytes, long runTime, long errors, List<String>errs)
  {
    auUrlsMoved = urls;
    auArtifactsMoved = artifacts;
    auBytesMoved = bytes;
    auRunTime= runTime;
    auErrorCount = errors;
    auErrors=errs;
  }

  void setTotalCounters(long aus, long urls, long artifacts, long bytes, long runTime, long errors)
  {
    totalAusMoved = aus;
    totalUrlsMoved = urls;
    totalArtifactsMoved = artifacts;
    totalBytesMoved = bytes;
    totalRunTime = runTime;
    totalErrorCount = errors;
  }

  LinkedHashSet<ArchivalUnit> getAuMoveQueue() {
    return auMoveQueue;
  }

  void setCurrentAu(String auName) {
    currentAu=auName;
  }
  String getUserName() {
    return userName;
  }

  String getUserPass() {
    return userPass;
  }

  File getReportFile() {
    return reportFile;
  }

  String getCfgAccessUrl() {
    return cfgAccessUrl;
  }

  String getRsAccessUrl() {
    return rsAccessUrl;
  }

  /**
   * Move all versions of a cachedUrl.
   * @param cachedUrl The cachedUrl we which to move
   * @param v2Artifacts The list of artifacts which already match this cachedUrl uri.
   */
  private void moveCuVersions(String uri, CachedUrl cachedUrl, List<Artifact> v2Artifacts) {
    String auid = cachedUrl.getArchivalUnit().getAuId();
    CachedUrl[] localVersions = cachedUrl.getCuVersions();
    Queue<CachedUrl> cuQueue = Collections.asLifoQueue(new ArrayDeque<>());
    cuQueue.addAll(Arrays.asList(localVersions));
    if(!v2Artifacts.isEmpty()){
      for(int i=0; i< v2Artifacts.size(); i++) {
        // remove the previous number of artifacts from the queue
        cuQueue.remove();
      }
    }
    // Now loop through the queue (but use a callback to handle the next element)
    moveNextCuVersion(auid, uri,cuQueue);

  }

  private void moveNextCuVersion(String auid, String uri, Queue<CachedUrl> cuQueue) {
    Long collectionDate = null;
    CachedUrl cu = cuQueue.poll();
    if(cu == null) {
      log.debug3("Queued move requests of all versions of " + currentUrl);
      return;
    }
    try {
      String fetchTime = cu.getProperties().getProperty(CachedUrl.PROPERTY_FETCH_TIME);
      if (!StringUtil.isNullString(fetchTime)) {
        collectionDate = Long.parseLong(fetchTime);
      }
      else {
        log.debug2(uri + ":version: " + cu.getVersion() + " is missing fetch time.");
      }
      log.debug3("Moving cu version " + cu.getVersion() + " - fetched at " + fetchTime);
      createArtifact(auid, uri, collectionDate, cu, collection,cuQueue);
    } catch (ApiException apie) {
      String err=uri + ": failed to create version: " + cu.getVersion() + ": " +
        apie.getCode() + " - " + apie.getMessage();
      log.warning(err);
      auErrors.add(err);
      auErrorCount++;
    } finally {
      AuUtil.safeRelease(cu);
    }
  }


  /**
   * Make an asynchronous rest call to the V2 repository to create a new artifact.
   * @param auid au identifier for the CachedUrl we are moving.
   * @param uri the uri  for the CachedUrl we are moving.
   * @param collectionDate the date at which this item was collected or null
   * @param cu the CachedUrl we are moving.
   * @param collectionId the v2 collection we are moving to
   * @throws ApiException the rest exception thrown should anything fail in the request.
   */
  private void createArtifact(String auid, String uri, Long collectionDate,
    CachedUrl cu, String collectionId, Queue<CachedUrl> cuQueue) throws ApiException {
    rsCollectionsApiClient.createArtifactAsync(collectionId, auid, uri, cu, collectionDate,
      new CreateArtifactCallback(auid, uri, cuQueue));
  }

  /**
   * Make a synchronous rest call to commit the artifact that just completed successful creation.
   * @param uncommitted the v2 artifact to be committed
   * @throws ApiException the rest exception thrown should anything fail in the request.
   */
  private void commitArtifact(Artifact uncommitted) throws ApiException {
    Artifact committed;
    committed = rsCollectionsApiClient.updateArtifact(uncommitted.getCollection(),
      uncommitted.getId(), true);
    log.debug3("Successfully committed artifact " + committed.getId());
    auArtifactsMoved++;
  }

  /**
   * Complete the au move by moving the state and config information.
   * @param au the au we are moving.
   */
  private void finishAuMove(ArchivalUnit au) {
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
        cfgAusApiClient.putAuConfig(au.getAuId(), v2config);
        log.info(auName + ": Successfully moved AU Configuration.");
      } catch (ApiException apie) {
        String err = auName + ": Attempt to move au configuration failed: " + apie.getCode() +
          "- " + apie.getMessage();
        errorList.add(err);
        auErrors.add(err);
        auErrorCount++;
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
        cfgAusApiClient.patchAuState(au.getAuId(), v2State.toMap(), makeCookie());
        log.info(auName + ": Successfully moved AU State.");
      } catch (ApiException apie) {
        String err = auName + ": Attempt to move au state failed: " + apie.getCode() +
          "- " + apie.getMessage();
        errorList.add(err);
        auErrors.add(err);
        auErrorCount++;
      }
    }
    else {
      log.warning(auName + ": No State information found for au");
    }
  }
  // config service utilities


  private String makeCookie() {
    return cliendId + "-" + ++reqId;
  }


  /**
   * A simple class to encompass an ApiCallback
   */
  protected class CreateArtifactCallback implements ApiCallback<Artifact> {
    String auId;
    String uri;
    Queue<CachedUrl> cuQueue;

    public CreateArtifactCallback(String auid, String uri, Queue<CachedUrl> cuQueue) {
      this.auId= auid;
      this.uri= uri;
      this.cuQueue=cuQueue;
    }

    @Override
    public void onFailure(ApiException e, int statusCode,
      Map<String, List<String>> responseHeaders) {
      String err = "Create Artifact request failed: " + statusCode + " - " + e.getMessage();
      errorList.add(err);
      auErrors.add(err);
      auErrorCount++;
    }

    @Override
    public void onSuccess(Artifact result, int statusCode,
      Map<String, List<String>> responseHeaders) {
      try {
        log.debug3("Successfully created artifact (" + statusCode + "): " + result.getId());
        commitArtifact(result);
        if(cuQueue.peek() != null)
          moveNextCuVersion(auId, uri, cuQueue);
      } catch (ApiException e) {
        String err = "Attempt to commit artifact failed: " + e.getCode() + " - " + e.getMessage();
        errorList.add(err);
        auErrors.add(err);
        auErrorCount++;
      }
    }

    @Override
    public void onUploadProgress(long bytesWritten, long contentLength, boolean done) {
      log.debug3("Create Artifact uploaded " + bytesWritten + " of " + contentLength + "bytes..");
      auBytesMoved+=bytesWritten;
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
