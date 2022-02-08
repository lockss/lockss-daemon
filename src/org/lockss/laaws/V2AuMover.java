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

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.*;
import okhttp3.Dispatcher;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.lockss.app.LockssDaemon;
import org.lockss.daemon.LockssRunnable;
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
import org.lockss.laaws.model.rs.AuidPageInfo;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.CuIterator;
import org.lockss.plugin.PluginManager;
import org.lockss.poller.PollManager;
import org.lockss.protocol.AuAgreements;
import org.lockss.protocol.DatedPeerIdSet;
import org.lockss.protocol.DatedPeerIdSetImpl;
import org.lockss.protocol.IdentityManager;
import org.lockss.protocol.IdentityManagerImpl;
import org.lockss.repository.AuSuspectUrlVersions;
import org.lockss.repository.RepositoryManager;
import org.lockss.state.AuState;
import org.lockss.uiapi.util.DateFormatter;
import org.lockss.util.Constants;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import org.lockss.util.UrlUtil;

public class V2AuMover {

  static final String PREFIX = Configuration.PREFIX + "v2.migrate.";

  /**
   * The user agent that the migrator will use when connecting to V2 services
   */
  public static final String PARAM_V2_USER_AGENT = PREFIX + "user_agent";
  public static final String DEFAULT_V2_USER_AGENT = "lockss";

  /**
   * Turn on debugging for the configuration service network endpoints - default false..
   */
  public static final String DEBUG_CONFIG_REQUEST = PREFIX + "cfg.debug";
  public static final boolean DEFAULT_DEBUG_CONFIG_REQUEST = false;

  /**
   * Turn on debugging for the repository service network endpoints -default false
   */
  public static final String DEBUG_REPO_REQUEST = PREFIX + "repo.debug";
  public static final boolean DEFAULT_DEBUG_REPO_REQUEST = false;

  /**
   * Max number of simultaneous requests - default 50.
    */
  public static final String PARAM_MAX_REQUESTS = PREFIX + "max_requests";
  public static final int DEFAULT_MAX_REQUESTS = 20;


  /**
   * The V2 collection to migrate into - default lockss
   */
  public static final String PARAM_V2_COLLECTION = PREFIX + "collection";
  public static final String DEFAULT_V2_COLLECTION = "lockss";

  /**
   * The hostname of the v2 Service endpoints - default localhost
   */
  public static final String PARAM_HOSTNAME = PREFIX + "hostname";
  public static final String DEFAULT_HOSTNAME = "localhost";

  /**
   * The repository service port- default 24610
   */
  public static final String PARAM_RS_PORT = PREFIX + "rs.port";
  public static final int DEFAULT_RS_PORT = 24610;

  /**
   * The configuration service port- default 24620
   */
  public static final String PARAM_CFG_PORT = PREFIX + "cfg.port";
  public static final int DEFAULT_CFG_PORT = 24620;

  /**
   * The  maximum number of retries for failures
   */
  public static final String PARAM_MAX_RETRY_COUNT = PREFIX + "max.retries";
  public static final int DEFAULT_MAX_RETRY_COUNT = 4;

  /**
   * The backoff between failed attempts - default 10000 ms
   */
  public static final String PARAM_RETRY_BACKOFF_DELAY = PREFIX + "retry_backoff";
  public static final long DEFAULT_RETRY_BACKOFF_DELAY = 10 * Constants.SECOND;

  /**
   * The Connection timeout
    */
  public static final String PARAM_CONNECTION_TIMEOUT= PREFIX + "connection.timeout";
  public static final long DEFAULT_CONNECTION_TIMEOUT = 30 * Constants.SECOND;

  /**
   * The read/write timeout - default 30 sec
   */
  public static final String PARAM_READ_TIMEOUT = PREFIX + "read.timeout";
  public static final long DEFAULT_READ_TIMEOUT =  30 * Constants.SECOND;

  /** Path to directory holding daemon logs */
  public static final String PARAM_REPORT_DIR =
      ConfigManager.PARAM_PLATFORM_LOG_DIR;
  public static final String DEFAULT_REPORT_DIR = "/tmp";

  /**
   * The Name to use for the migration report
   */
  public static final String PARAM_REPORT_FILE = PREFIX + "report.file";
  public static final String DEFAULT_REPORT_FILE = "v2migration.txt";


  private static final Logger log = Logger.getLogger(V2AuMover.class);

  static NumberFormat bigIntFmt = NumberFormat.getInstance();

  private int maxRetryCount;
  private long retryBackoffDelay;
  private String currentStatus;
  private boolean running = false;

  private final String cliendId =
      org.apache.commons.lang3.RandomStringUtils.randomAlphabetic(8);

  /**
   * The v2 Repository Client
   */
  private final V2RestClient repoClient;

  /**
   * The v2 REST collections api implemntation
   */
  private final StreamingCollectionsApi repoCollectionsApiClient;

  /**
   * The v2 REST status api implementation
   */
  private final org.lockss.laaws.api.rs.StatusApi repoStatusApiClient;

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
  private String userAgent;

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
  private final int repoPort;

  private final File reportFile;

  // Access information for the V2 Configuration Service
  private String cfgAccessUrl = null;
  // Access information for the V2 Rest Repository
  private String repoAccessUrl = null;
  /** the maximum allowed requests across all hosts */
  private int maxRequests;

  // timeouts
  /** the time to wait fora a connection before timing out */
  private final long connectTimeout;
  /** the time to wait for read/write before timing out */
  private final long readTimeout;

  // timer
  private long startTime;

  // debug support
  private boolean debugRepoReq;
  private boolean debugConfigReq;

  PluginManager pluginManager;
  IdentityManagerImpl idManager;
  RepositoryManager repoManager;
  PollManager pollManager;
  private final ArrayList<String> v2Aus = new ArrayList<>();
  private final LinkedHashSet<ArchivalUnit> auMoveQueue = new LinkedHashSet<>();
  private Dispatcher dispatcher;
  boolean allCusQueued = false;
  private long reqId = 0;

  // report
  private String currentAu;

  private PrintWriter reportWriter;

  // counters for total run
  private long totalAusMoved = 0;
  private long totalUrlsMoved = 0;
  private long totalArtifactsMoved = 0;
  private long totalBytesMoved = 0;
  private long totalContentBytesMoved = 0;
  private long totalRunTime = 0;
  private long totalErrorCount = 0;

  // Report fields
  // Counters for a single au
  private long auUrlsMoved = 0;
  private long auArtifactsMoved = 0;
  private long auBytesMoved = 0;
  private long auContentBytesMoved = 0;
  private long auRunTime = 0;
  private long auErrorCount = 0;
  private List<String> auErrors = new ArrayList<>();
  private final List<String> errorList = new ArrayList<>();

  private boolean terminated = false;
  private boolean isPartialContent = false;


  public V2AuMover() {
    // Get our lockss daemon managers
    pluginManager = LockssDaemon.getLockssDaemon().getPluginManager();
    IdentityManager idmgr = LockssDaemon.getLockssDaemon().getIdentityManager();
    if( idmgr instanceof IdentityManagerImpl )
      idManager =((IdentityManagerImpl) LockssDaemon.getLockssDaemon().getIdentityManager());
    repoManager = LockssDaemon.getLockssDaemon().getRepositoryManager();
    pollManager = LockssDaemon.getLockssDaemon().getPollManager();
    // Get configuration parameters to support overriding our defaults
    Configuration config = ConfigManager.getCurrentConfig();
    userAgent = config.get(PARAM_V2_USER_AGENT, DEFAULT_V2_USER_AGENT);
    collection = config.get(PARAM_V2_COLLECTION, DEFAULT_V2_COLLECTION);
    cfgPort = config.getInt(PARAM_CFG_PORT, DEFAULT_CFG_PORT);
    repoPort = config.getInt(PARAM_RS_PORT, DEFAULT_RS_PORT);
    connectTimeout = config.getTimeInterval(PARAM_CONNECTION_TIMEOUT,DEFAULT_CONNECTION_TIMEOUT);
    readTimeout = config.getTimeInterval(PARAM_READ_TIMEOUT,DEFAULT_READ_TIMEOUT);
    maxRetryCount=config.getInt(PARAM_MAX_RETRY_COUNT, DEFAULT_MAX_RETRY_COUNT);
    retryBackoffDelay=config.getLong(PARAM_RETRY_BACKOFF_DELAY,DEFAULT_RETRY_BACKOFF_DELAY);

    String logdir = config.get(PARAM_REPORT_DIR, DEFAULT_REPORT_DIR);
    String logfile = config.get(PARAM_REPORT_FILE, DEFAULT_REPORT_FILE);

    reportFile = new File(logdir, logfile);
    repoClient = new V2RestClient();
    repoClient.setConnectTimeout((int) connectTimeout);
    repoClient.setReadTimeout((int) readTimeout);
    repoClient.setWriteTimeout((int) readTimeout);
    repoStatusApiClient = new org.lockss.laaws.api.rs.StatusApi(repoClient);
    repoCollectionsApiClient = new StreamingCollectionsApi(repoClient);

    configClient = new V2RestClient();
    configClient.setConnectTimeout((int) connectTimeout);
    configClient.setReadTimeout((int) readTimeout);
    configClient.setWriteTimeout((int) readTimeout);
    // Assign the client to the status api and aus api
    cfgStatusApiClient = new org.lockss.laaws.api.cfg.StatusApi(configClient);
    cfgAusApiClient = new AusApi(configClient);
  }

  /**
   * Compile a list of regular expression into a list of Patterns
   *
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
   *
   * @param str  the string we are looking for
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
   *
   * @throws IOException if error occurred while moving au.
   */
  public void moveAllAus(String host, String uname, String upass, List<Pattern> selPatterns) throws IOException {
    initRequest(host, uname, upass);
    if (v2ServicesUnavailable()) {
      throw new IOException("Move request failed, V2 Services are not ready.");
    }
    // get the aus known to the v2 repo
    getV2Aus();
    // get the aus known by the v1 repository
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
    if (!terminated && currentAu == null)
      moveNextAu();
  }

  public Runner moveAllAusAsynch(String host, String uname, String upass, List<Pattern> selPatterns) {
    V2AuMover.Runner runner = new V2AuMover.Runner(host, uname, upass, selPatterns);
    new Thread(runner).start();
    return runner;
  }

  public class Runner extends LockssRunnable {
    String host;
    String uname;
    String upass;
    List<Pattern> selPatterns;

    public Runner(String host, String uname, String upass,
                  List<Pattern> selPatterns) {
      super("V2AuMover");
      this.host = host;
      this.uname = uname;
      this.upass = upass;
      this.selPatterns = selPatterns;
    }

    public String getCurrentStatus() {
      if (currentStatus.startsWith("Copying: ")) {
        return currentStatus + ", " + auUrlsMoved + " URLs, " +
          auArtifactsMoved + " versions copied.";
      }
      return currentStatus;
    }

    public boolean isRunning() {
      return running;
    }

    public void lockssRun() {
      try {
        initRequest(host, uname, upass);
        currentStatus = "Checking V2 services";
        if (v2ServicesUnavailable()) {
          currentStatus = "V2 services are not ready";
          running = false;
          return;
        }
        currentStatus = "Checking AUs already in V2 repo";
        // get the aus known to the v2 repo
        getV2Aus();
        // get the aus known by the v1 repository
        for (ArchivalUnit au : pluginManager.getAllAus()) {
          if (pluginManager.isInternalAu(au)) {
            continue;
          }
          if (selPatterns == null || selPatterns.isEmpty() ||
              isMatch(au.getAuId(), selPatterns)) {
            auMoveQueue.add(au);
          }
        }
        currentStatus = "Copying AUs";
        log.debug("Moving " + auMoveQueue.size() + " aus.");
        // Check to see if we are currently working on an au.
        if (!terminated && currentAu == null) {
          moveNextAu();
        } else {
          running = false;
        }
      } catch (IOException e) {
        log.error("Unexpected exception", e);
        currentStatus = e.getMessage();
        running = false;
      }
    }
  }

  /**
   * Move one au as identified by the name of the au
   * iff it's not already in the queue
   *
   * @param auId The ArchivalUnit Id string
   */
  public void moveOneAu(String host, String uname, String upass, String auId) throws IOException {
    ArchivalUnit au = pluginManager.getAuFromId(auId);
    if (au != null) {
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
   *
   * @param au the ArchivalUnit to move
   * @throws IOException if unable to connect to services
   */

  public void moveOneAu(String host, String uname, String upass, ArchivalUnit au) throws IOException {
    initRequest(host, uname, upass);
    if (v2ServicesUnavailable()) {
      throw new IOException(au.getAuId() + "Move request failed, V2 Services are not ready.");
    }
    // get the aus known to the v2 repository
    getV2Aus();
    auMoveQueue.add(au);
    // if we aren't working on an au move the next au (sync this).
    if (!terminated && currentAu == null)
      moveNextAu();
  }

  public void initRequest(String host, String uname, String upass) throws
      IllegalArgumentException {
    errorList.clear();
    Configuration config = ConfigManager.getCurrentConfig();
    // allow for changing these between runs when testing
    debugRepoReq = config.getBoolean(DEBUG_REPO_REQUEST, DEFAULT_DEBUG_REPO_REQUEST);
    debugConfigReq = config.getBoolean(DEBUG_CONFIG_REQUEST, DEFAULT_DEBUG_CONFIG_REQUEST);
    maxRequests = config.getInt(PARAM_MAX_REQUESTS, DEFAULT_MAX_REQUESTS);
    hostName = config.get(PARAM_HOSTNAME, DEFAULT_HOSTNAME);

    if (host != null) {
      hostName = host;
    }
    if (uname != null) {
      userName = uname;
    }
    if (upass != null) {
      userPass = upass;
    }
    if (userName == null || userPass == null) {
      errorList.add("Missing user name or password.");
      throw new IllegalArgumentException("Missing user name or password");
    }
    try {
      cfgAccessUrl = new URL("http", hostName, cfgPort, "").toString();
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
      configClient.addInterceptor(new RetryErrorInterceptor());
    } catch (MalformedURLException mue) {
      errorList.add("Error parsing REST Configuration Service URL: "
          + mue.getMessage());
      throw new IllegalArgumentException(
          "Missing or Invalid configuration service hostName: " + hostName + " port: "+cfgPort);
    }

    try {
      repoAccessUrl = new URL("http", hostName, repoPort, "").toString();
      if (repoAccessUrl == null || UrlUtil.isMalformedUrl(repoAccessUrl)) {
        errorList.add("Missing or Invalid repository service url: " + repoAccessUrl);
        throw new IllegalArgumentException(
            "Missing or Invalid configuration service url: " + repoAccessUrl);
      }
      // Create a new RepoClient
      repoClient.setUsername(userName);
      repoClient.setPassword(userPass);
      repoClient.setUserAgent(userAgent);
      repoClient.setBasePath(repoAccessUrl);
      repoClient.setDebugging(debugRepoReq);
      repoClient.addInterceptor(new RetryErrorInterceptor());
      dispatcher = repoClient.getHttpClient().dispatcher();
      dispatcher.setMaxRequests(maxRequests);
      dispatcher.setMaxRequestsPerHost(maxRequests);
    } catch (MalformedURLException mue) {
      errorList.add("Error parsing REST Configuration Service URL: " + mue.getMessage());
      throw new IllegalArgumentException(
          "Missing or Invalid configuration service hostName: " + hostName + " port: "+repoPort);
    }
    startReportFile();
    totalArtifactsMoved = 0;
    totalUrlsMoved = 0;
    totalBytesMoved = 0;
    totalContentBytesMoved = 0;
    totalRunTime = 0;
    startTime = System.currentTimeMillis();
  }


  /**
   * Start appending the Report file for current Au Move
   */
  void startReportFile() {

    try {
      log.info("Writing report to file " + reportFile.getAbsolutePath());
      reportWriter = new PrintWriter(Files.newOutputStream(reportFile.toPath(), CREATE, APPEND), true);
      reportWriter.println("--------------------------------------------------");
      reportWriter.println("  V2 Au Migration Report - " + DateFormatter.now());
      reportWriter.println("--------------------------------------------------");
      reportWriter.println();
      if (reportWriter.checkError()) {
        log.warning("Error writing report file.");
      }
    } catch (IOException e) {
      log.error("Report file will not be written: Unable to open report file:" + e.getMessage());
    }
  }

  /**
   * Update the report for the current Au
   * @param auName the name of the current Au
   */
  void updateReport(String auName) {
    String auData = "urlsMoved: " + auUrlsMoved +
        "  artifactsMoved: " + auArtifactsMoved +
        "  contentBytesMoved: " + auContentBytesMoved +
        "  totalBytesMoved: "  + auBytesMoved +
        "  byteRate: " + StringUtil.byteRateToString(auBytesMoved, auRunTime) +
        "  errors: " + auErrorCount +
        "  totalRuntime: " + StringUtil.timeIntervalToString(auRunTime);
    if (reportWriter != null) {
      reportWriter.println("AU Name: "+ auName);
      reportWriter.println("AU ID: "+ currentAu);
      if(terminated) {
        reportWriter.println("Move terminated with error.");
        reportWriter.println(auData);
      }
      else {
        if(isPartialContent) {
          if(auArtifactsMoved > 0) {// if we moved something
            reportWriter.println("Move remaining unmigrated au content.");
            reportWriter.println(auData);
          }
        }
        else {
          reportWriter.println(auData);
        }
      }
      if (!auErrors.isEmpty()) {
        for (String err : auErrors) {
          reportWriter.println(" " + err);
        }
      }
      reportWriter.println();
      if (reportWriter.checkError()) {
        log.warning("Error writing report file.");
      }
    }
    log.info(auData);
    for (String err : auErrors) {
      log.error(err);
    }
  }

  /**
   * Close the report before exiting
   */
  void closeReport() {
    String summary = "AusMoved: " + totalAusMoved +
      ",  urlsMoved: " + bigIntFmt.format(totalUrlsMoved) +
      ",  artifactsMoved: " + bigIntFmt.format(totalArtifactsMoved) +
      ",  contentBytesMoved: " + bigIntFmt.format(totalContentBytesMoved) +
      ",  totalBytesMoved: " + bigIntFmt.format(totalBytesMoved) +
      ",  byteRate: " + StringUtil.byteRateToString(totalBytesMoved, totalRunTime) +
      ",  errors: " + totalErrorCount +
      ",  totalRuntime: " + StringUtil.timeIntervalToString(totalRunTime);
    running = false;
    currentStatus = summary;
    if (reportWriter != null) {
      reportWriter.println(summary);
      if (reportWriter.checkError()) {
        log.warning("Error writing report file.");
      }
      reportWriter.close();
    }
    log.info(summary);
  }

  protected void moveNextAu() throws IOException {
    if (!terminated && auMoveQueue.iterator().hasNext()) {
      ArchivalUnit au = auMoveQueue.iterator().next();
      currentStatus = "Copying: " + au.getName();
      allCusQueued = false;
      log.debug("Moving " + au.getName() + " - " + auMoveQueue.size() + " AUs remaining.");
      currentAu = au.getAuId();
      auBytesMoved = 0;
      auContentBytesMoved = 0;
      auUrlsMoved = 0;
      auArtifactsMoved = 0;
      auErrorCount = 0;
      auErrors.clear();
      auRunTime = 0;
      moveAu(au);
    } else {
      totalRunTime = System.currentTimeMillis() - startTime;
      closeReport();
    }
  }

  protected void moveAu(ArchivalUnit au) throws IOException {
    long au_move_start = System.currentTimeMillis(); // Get the start Time
    String auName = au.getName();
    log.info("Handling request to move AU: " + auName);
    log.info("AuId: " + currentAu);
    try {
      if (v2ServicesUnavailable()) {
        terminated = true;
        return;
      }
      log.info(auName + ": Moving AU Artifacts...");
      moveAuArtifacts(au);
      finishAuMove(au);
      if(!terminated)
        log.info(auName + ": Successfully moved AU Artifacts.");
      else
        log.info(auName+ ": Au move terminated because of errors.");
      auRunTime = System.currentTimeMillis() - au_move_start;
      updateReport(auName);
      updateTotals();
      auMoveQueue.remove(au);
      currentAu = null;
      moveNextAu();
    } catch (Exception ex) {
      String err;
      if (ex instanceof ApiException) {
        err = auName + ": Attempt to move Au failed:" + ((ApiException) ex).getCode() + ": "
            + ex.getMessage();
      } else {
        err = auName + ": Attempt to move Au failed:" + ex.getMessage();
        terminated = true;
      }
      log.error(err);
      auErrors.add(err);
      errorList.add(err);
      auErrorCount++;
      totalErrorCount += auErrorCount;
      finishAuMove(au);
      if (terminated) {
        dispatcher.cancelAll();
        closeReport();
        throw new IOException("Au Move Request terminated due to errors:" + err);
      }
    }
  }



  /**
   * Move one V1 Au including all cachedUrls and all versions.
   *
   * @param au au The ArchivalUnit to move
   * @throws ApiException if REST request results in errors
   */
  protected void moveAuArtifacts(ArchivalUnit au) throws ApiException {

    //Get the au artifacts from the v1 repo.
    /* get Au cachedUrls from Lockss*/
    for (CuIterator iter = au.getAuCachedUrlSet().getCuIterator(); iter.hasNext(); ) {
      List<Artifact> cuArtifacts = new ArrayList<>();
      CachedUrl cachedUrl = iter.next();
      String v1Url = cachedUrl.getUrl();
      String v2Url = null;
      try {
        v2Url = UrlUtil.normalizeUrl(cachedUrl.getProperties().getProperty(CachedUrl.PROPERTY_NODE_URL), au);
      } catch (Exception ex) {
        log.warning("Unable to normalize uri for " + v1Url, ex);
      } finally {
        AuUtil.safeRelease(cachedUrl);
      }
      if (v2Url == null) {
        v2Url = v1Url;
      }
      // we have the possibility that some or all of the artifacts were moved.
      // We are looking for previously moved versions of the 'current' cu
      ArtifactPageInfo pageInfo;
      String token = null;
      // if the v2 repo knows about this au we need to call getArtifacts.
      if(v2Aus.contains(au.getAuId())) {
        isPartialContent = true;
        do {
          pageInfo = repoCollectionsApiClient.getArtifacts(collection, au.getAuId(),
              v2Url, null, null, false, null, token);
          cuArtifacts.addAll(pageInfo.getArtifacts());
          token = pageInfo.getPageInfo().getContinuationToken();
        } while (!terminated && !StringUtil.isNullString(token));
        if(log.isDebug3())
          log.debug3("Found " + cuArtifacts.size() + " matches for " + v2Url);
      }
      moveCuVersions(v1Url, cachedUrl, cuArtifacts);
    }
    allCusQueued = true;
  }


  void updateTotals() {
    totalAusMoved++;
    totalBytesMoved += auBytesMoved;
    totalContentBytesMoved += auContentBytesMoved;
    totalUrlsMoved += auUrlsMoved;
    totalArtifactsMoved += auArtifactsMoved;
    totalErrorCount += auErrorCount;
  }

  /* ------------------
  testing getters & setters
 */
  void setAuCounters(long urls, long artifacts, long bytes, long contentBytes, long runTime, long errors,
      List<String> errs) {
    auUrlsMoved = urls;
    auArtifactsMoved = artifacts;
    auBytesMoved = bytes;
    auContentBytesMoved = contentBytes;
    auRunTime = runTime;
    auErrorCount = errors;
    auErrors = errs;
  }

  void setTotalCounters(long aus, long urls, long artifacts, long bytes, long contentBytes, long runTime, long errors) {
    totalAusMoved = aus;
    totalUrlsMoved = urls;
    totalArtifactsMoved = artifacts;
    totalBytesMoved = bytes;
    totalContentBytesMoved = contentBytes;
    totalRunTime = runTime;
    totalErrorCount = errors;
  }

  LinkedHashSet<ArchivalUnit> getAuMoveQueue() {
    return auMoveQueue;
  }

  void setCurrentAu(String auName) {
    currentAu = auName;
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

  String getRepoAccessUrl() {
    return repoAccessUrl;
  }

  /**
   * Check V2 service status for ready
   * @return true if status for repository  is ready, false if either are not ready
   * @throws IOException
   */
  boolean v2ServicesUnavailable() throws IOException {
    boolean result = true;

    log.info("Checking V2 Repository Status");
    try {
      String msg;
      if (!repoStatusApiClient.getStatus().getReady()) {
        msg = "Unable to move au. V2 Repository Service is not ready.";
        errorList.add(msg);
        auErrors.add(msg);
      }
      else {
        log.info("Checking V2 Configuration Status");
        if (!cfgStatusApiClient.getStatus().getReady()) {
          msg = "Unable to move au. V2 Configuration Service is not ready.";
          errorList.add(msg);
          auErrors.add(msg);
        }
        else {
          result = false;
        }
      }
    }
    catch (Exception ex) {
      String msg = ex.getMessage();
      errorList.add(msg);
      auErrors.add(msg);
      throw new IOException( "Unable to get status for v2 services: " + msg);
    }
    return result;
  }

  void getV2Aus() throws IOException {
    // get the aus known by the v2 repository
    try {
      AuidPageInfo pageInfo;
      String token = null;
      do {
        pageInfo = repoCollectionsApiClient.getAus(collection, null, token);
        v2Aus.addAll(pageInfo.getAuids());
        token = pageInfo.getPageInfo().getContinuationToken();
      } while (!terminated && !StringUtil.isNullString(token));
    } catch (ApiException apie) {
      String err = "Error occurred while retrieving v2 Au list: " + apie.getMessage();
      errorList.add(err);
      log.error(err, apie);
      closeReport();
      String msg = apie.getCode() == 0 ? apie.getMessage()
          : apie.getCode() + " - " + apie.getMessage();
      throw new IOException( "Unable to get Au List from v2 Repository: " + msg);
    }
  }

  /**
   * Move all versions of a cachedUrl.
   *
   * @param v1Url       The uri for the current cached url.
   * @param cachedUrl   The cachedUrl we which to move
   * @param v2Artifacts The list of artifacts which already match this cachedUrl uri.
   */
  private void moveCuVersions(String v1Url, CachedUrl cachedUrl, List<Artifact> v2Artifacts) {
    if (!terminated) {
      String auid = cachedUrl.getArchivalUnit().getAuId();
      CachedUrl[] localVersions = cachedUrl.getCuVersions();
      Queue<CachedUrl> cuQueue = Collections.asLifoQueue(new ArrayDeque<>());
      //If we have more v1 versions than the v2 repo - copy the missing items
      if (v2Artifacts.size() > 0) {
        log.debug2("v2 versions available=" + v2Artifacts.size() + " v1 versions available=" + localVersions.length);
      }
      // if the v2 repository has fewer versions than the v1 repository
      // then move the missing versions or release the cu version.
      int vers_to_move = localVersions.length - v2Artifacts.size();
      if (vers_to_move > 0) {
        for (int vx =0; vx < localVersions.length; vx++) {
          CachedUrl ver = localVersions[vx];
          if(vx < vers_to_move) {
            cuQueue.add(ver);
          }
          else {
            AuUtil.safeRelease(ver);
          }
        }
        if(log.isDebug2())
          log.debug2("Moving " + vers_to_move + "/" + localVersions.length+ " versions...");
        moveNextCuVersion(auid, v1Url, cuQueue);
      }
    }
  }

  private void moveNextCuVersion(String auid, String v1Url, Queue<CachedUrl> cuQueue) {
    Long collectionDate = null;
    CachedUrl cu = cuQueue.poll();
    if (cu == null) {
      if(log.isDebug3())
        log.debug3("All versions of " + v1Url + " have been queued.");
      return;
    }
    try {
      String fetchTime = cu.getProperties().getProperty(CachedUrl.PROPERTY_FETCH_TIME);
      if (!StringUtil.isNullString(fetchTime)) {
        collectionDate = Long.parseLong(fetchTime);
      } else {
        log.debug2(v1Url + ":version: " + cu.getVersion() + " is missing fetch time.");
      }
      if(log.isDebug3())
        log.debug3("Moving cu version " + cu.getVersion() + " - fetched at " + fetchTime);
      createArtifact(auid, v1Url, collectionDate, cu, collection, cuQueue);
    } catch (ApiException apie) {
      String err = v1Url + ": failed to create version: " + cu.getVersion() + ": " +
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
   *
   * @param auid           au identifier for the CachedUrl we are moving.
   * @param v1Url          the uri  for the CachedUrl we are moving.
   * @param collectionDate the date at which this item was collected or null
   * @param cu             the CachedUrl we are moving.
   * @param collectionId   the v2 collection we are moving to
   * @throws ApiException the rest exception thrown should anything fail in the request.
   */
  private void createArtifact(String auid, String v1Url, Long collectionDate,
                              CachedUrl cu, String collectionId, Queue<CachedUrl> cuQueue) throws ApiException {
    repoCollectionsApiClient.createArtifactAsync(collectionId, auid, v1Url, cu, collectionDate,
        new CreateArtifactCallback(auid, v1Url,cu.getContentSize(), cuQueue));
  }

  /**
   * Make a synchronous rest call to commit the artifact that just completed successful creation.
   *
   * @param uncommitted the v2 artifact to be committed
   * @throws ApiException the rest exception thrown should anything fail in the request.
   */
  private void commitArtifact(Artifact uncommitted) throws ApiException {
    Artifact committed;
    committed = repoCollectionsApiClient.updateArtifact(uncommitted.getCollection(),
        uncommitted.getId(), true);
    log.debug3("Successfully committed artifact " + committed.getId());
    auArtifactsMoved++;
  }

  /**
   * Complete the au move by moving the state and config information.
   *
   * @param au the au we are moving.
   */
  private void finishAuMove(ArchivalUnit au) {
    do { //Wait until we are done the processing
      try {
        Thread.sleep(200);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    } while (!terminated && dispatcher.runningCallsCount() > 0);
    if (!terminated) {
      String auName = au.getName();
      log.info(auName + ": Moving AU Agreements...");
      moveAuAgreements(au);
      log.info(auName + ": Moving AU Suspect Urls...");
      moveAuSuspectUrlVersions(au);
      log.info(auName + ": Moving No Au Peer Set...");
      moveNoAuPeerSet(au);
      log.info(auName + ": Moving AU State...");
      moveAuState(au);
      //This needs to be last
      log.info(auName + ": Moving AU Configuration...");
      moveAuConfig(au);
    }
  }

  /**
   * Make a synchronous rest call to configuration service to add configuration info for an au.
   *
   * @param au The ArchivalUnit whose configuration is to be move
   */
  void moveAuConfig(ArchivalUnit au) {
    Configuration v1config = au.getConfiguration();
    AuConfiguration v2config = new AuConfiguration().auId(au.getAuId());
    String auName = au.getName();
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

  private void moveAuAgreements(ArchivalUnit au) {
    AuAgreements v1AuAgreements = idManager.findAuAgreements(au);
    String auName = au.getName();
    if(v1AuAgreements != null) {
      try {
        cfgAusApiClient.patchAuAgreements(au.getAuId(), v1AuAgreements.getPrunedBean(au.getAuId()), makeCookie());
        log.info(auName + ": Successfully moved AU Agreements.");
      } catch (Exception ex) {
        String err = auName + ": Attempt to move au agreements: " + ex.getMessage();
        log.error("Unable to move Au Agreements : " + err, ex);
        errorList.add(err);
        auErrors.add(err);
        auErrorCount++;
      }
    }
    else {
      log.warning("No Au agreements found for au.");
    }
  }

  private void moveAuSuspectUrlVersions(ArchivalUnit au) {
    AuSuspectUrlVersions asuv = AuUtil.getSuspectUrlVersions(au);
    String auName = au.getName();
    if(asuv != null) {
      try {
        cfgAusApiClient.putAuSuspectUrlVersions(au.getAuId(), asuv.getBean(au.getAuId()), makeCookie());
        log.info(auName + ": Successfully moved AU Suspect Url Versions.");
      } catch (Exception ex) {
        String err = auName + ": Attempt to move au suspect url versions failed: " + ex.getMessage();
        log.error("Unable to move Au Suspect Url Versions: " + err, ex);
        errorList.add(err);
        auErrors.add(err);
        auErrorCount++;
      }
    }
    else {
      log.warning(auName + ": No Au suspect url versions found.");
    }
  }

  private void moveNoAuPeerSet(ArchivalUnit au) {
    DatedPeerIdSet noAuPeerSet = pollManager.getNoAuPeerSet(au);
    String auName = au.getName();
    if(noAuPeerSet instanceof DatedPeerIdSetImpl) {
      try {
        cfgAusApiClient.putNoAuPeers(au.getAuId(), ((DatedPeerIdSetImpl)noAuPeerSet).getBean(au.getAuId()), makeCookie());
        log.info(auName + ": Successfully moved no Au peers.");
      } catch (Exception ex) {
        String err = auName + ": Attempt to move no AU peers failed: " + ex.getMessage();
        log.error("Unable to move no AU peers set: " + err, ex);
        errorList.add(err);
        auErrors.add(err);
        auErrorCount++;
      }
    } else {
      log.warning(auName + ": No Au peer set found for au");
    }
}

  /**
   * Make a synchronous rest call to configuration service to add state info for an au.
   *
   * @param au The ArchivalUnit  to move
   */
  private void moveAuState(ArchivalUnit au) {
    AuState v1State = AuUtil.getAuState(au);
    String auName = au.getName();
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
    } else {
      log.warning(auName + ": No State information found for au");
    }
  }

  private String makeCookie() {
    return cliendId + "-" + ++reqId;
  }


  /**
   * A simple class to encompass an ApiCallback
   */
  protected class CreateArtifactCallback implements ApiCallback<Artifact> {
    String auId;
    String v1Url;
    Queue<CachedUrl> cuQueue;
    long contentSize;

    public CreateArtifactCallback(String auid, String uri, long contentSize, Queue<CachedUrl> cuQueue) {
      this.auId = auid;
      this.v1Url = uri;
      this.cuQueue = cuQueue;
      this.contentSize = contentSize;
    }

    @Override
    public void onFailure(ApiException e, int statusCode,
                          Map<String, List<String>> responseHeaders) {
      String err = "Create Artifact for " + v1Url + " failed: " + statusCode + " - " + e.getMessage();
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
        auContentBytesMoved += contentSize;
        if (cuQueue.peek() != null)
          moveNextCuVersion(auId, v1Url, cuQueue);
        else
          auUrlsMoved++;
      } catch (ApiException e) {
        String err = "Attempt to commit artifact failed: " + e.getCode() + " - " + e.getMessage();
        errorList.add(err);
        auErrors.add(err);
        auErrorCount++;
      }
    }

    @Override
    public void onUploadProgress(long bytesWritten, long contentLength, boolean done) {
      log.debug3("Create Artifact uploaded " + bytesWritten + " of " + contentLength + " bytes..");
      auBytesMoved += bytesWritten;
      if (done) {
        log.debug2("Create Artifact upload of " + bytesWritten + " complete.");
      }
    }

    @Override
    public void onDownloadProgress(long bytesRead, long contentLength, boolean done) {
      log.debug3("Create Artifact downloaded " + bytesRead + " of " + contentLength + " bytes..");
      if (done) {
        log.debug2("Create Artifact download " + bytesRead + "  complete");
      }

    }
  }

  public class RetryErrorInterceptor implements Interceptor {

    @Override
    public Response intercept(final Chain chain) throws IOException {
      final Request request = chain.request();
      int tryCount = 0;
      Response response = null;

      // first call is actual call, following are first retries
      while ((response == null || !response.isSuccessful()) && tryCount < maxRetryCount) {
        tryCount++;
        try {
          response = chain.proceed(request);
          if (response.isSuccessful()) {
            return response;
          }
          else {
            int errCode = response.code();
            logErrorBody(response);
            if (errCode == 401 || errCode == 403) {
              response.close();
              // no retries
              break;
            }
            // close response before retry
            response.close();
          }
        }
        catch (final IOException ioe) {
          if (response != null) {
            log.debug3("Retrying: " + ioe.getMessage());
            response.close();
          }
          // We've run out of retries so throw the exception
          if (tryCount >= maxRetryCount) {
            log.debug2("Exceeded retries - exiting");
            terminated = true;
            throw ioe;
          }
        }
        // sleep before retrying
        try {
          Thread.sleep(retryBackoffDelay * tryCount);
        }
        catch (InterruptedException e1) {
          throw new RuntimeException(e1);
        }
      }
      //We run out of retries
      if (response != null) {
        int errCode = response.code();
        logErrorBody(response);
        // we've run out of retries...
        if (errCode == 401 || errCode == 403 || errCode >= 500) {
          terminated = true;
          String msg =  errCode + " - " + response.message()  + ": " + response.body();
          throw new IOException(msg);
        }
      }
      // last try should proceed as is
      return chain.proceed(request);
    }
  }

  void logErrorBody(Response response) {
    try {
      log.warning("Error response body: " + response.body().string());
    } catch (IOException e) {
      log.error("Exception trying to retrieve error response body", e);
    }
  }

}


