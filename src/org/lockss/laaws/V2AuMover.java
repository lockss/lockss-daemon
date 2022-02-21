/*

Copyright (c) 2021-2022 Board of Trustees of Leland Stanford Jr. University,
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.*;
import okhttp3.Dispatcher;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.codec.binary.Hex;
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

  /**
   * The flag to indicate whether known v2 Aus should be checked for missing content.
   */
  public static final String PARAM_CHECK_MISSING_CONTENT = PREFIX + "check.missing.content";
  public static final boolean DEFAUL_CHECK_MISSING_CONTENT = true;

  private static final Logger log = Logger.getLogger(V2AuMover.class);

  private static final String STATUS_COPYING = "**Copying**";

  static NumberFormat bigIntFmt = NumberFormat.getInstance();

  private int maxRetryCount;
  private long retryBackoffDelay;
  private String currentStatus;
  private boolean running = true; // init true avoids race while starting

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
  private ArchivalUnit currentAu;

  private PrintWriter reportWriter;

  // counters for total run
  private long totalAusToMove = 0;
  private long totalAusMoved = 0;
  private long totalAusPartiallyMoved = 0; // also included in totalAusMoved
  private long totalAusSkipped = 0;
  private long totalAusWithErrors = 0;
  private long totalUrlsMoved = 0;
  private long totalArtifactsMoved = 0;
  private long totalBytesMoved = 0;
  private long totalContentBytesMoved = 0;
  private long totalRunTime = 0;
  private long totalErrorCount = 0;
  private long workingOn = 0; // the ordinal of the AU currently being copied

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
  private boolean checkMissingContent;


  public V2AuMover() {
    // Get our lockss daemon managers
    pluginManager = LockssDaemon.getLockssDaemon().getPluginManager();
    IdentityManager idmgr = LockssDaemon.getLockssDaemon().getIdentityManager();
    if (idmgr instanceof IdentityManagerImpl) {
      idManager = ((IdentityManagerImpl) LockssDaemon.getLockssDaemon().getIdentityManager());
    }
    repoManager = LockssDaemon.getLockssDaemon().getRepositoryManager();
    pollManager = LockssDaemon.getLockssDaemon().getPollManager();
    // Get configuration parameters to support overriding our defaults
    Configuration config = ConfigManager.getCurrentConfig();
    userAgent = config.get(PARAM_V2_USER_AGENT, DEFAULT_V2_USER_AGENT);
    collection = config.get(PARAM_V2_COLLECTION, DEFAULT_V2_COLLECTION);
    cfgPort = config.getInt(PARAM_CFG_PORT, DEFAULT_CFG_PORT);
    repoPort = config.getInt(PARAM_RS_PORT, DEFAULT_RS_PORT);
    connectTimeout = config.getTimeInterval(PARAM_CONNECTION_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT);
    readTimeout = config.getTimeInterval(PARAM_READ_TIMEOUT, DEFAULT_READ_TIMEOUT);
    maxRetryCount = config.getInt(PARAM_MAX_RETRY_COUNT, DEFAULT_MAX_RETRY_COUNT);
    retryBackoffDelay = config.getLong(PARAM_RETRY_BACKOFF_DELAY, DEFAULT_RETRY_BACKOFF_DELAY);
    checkMissingContent = config.getBoolean(PARAM_CHECK_MISSING_CONTENT,
        DEFAUL_CHECK_MISSING_CONTENT);
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

  /** Entry point from MigrateContent servlet */
  public void executeRequest(Args args) {
    try {
      if (args.au != null) {
        // If an AU was supplied, copy it
        moveOneAu(args);
      } else {
        // else copy all AUs (that match sel pattern)
        moveAllAus(args);
      }
    } catch (IOException e) {
      log.error("Unexpected exception", e);
      currentStatus = e.getMessage();
      running = false;
    } finally {
      if (running) {
        log.warning("Unexpectedly still running at exit");
        currentStatus = "Unexpected exit";
      }
      running = false;
    }
  }

  /**
   * Return a string describing the current progress, or completion state.
   */
  public String getCurrentStatus() {
    if (STATUS_COPYING.equals(currentStatus)) {
      return "Copying (" + workingOn + " of " + totalAusToMove + " AUs): " +
        currentAu.getName() + ", " + auUrlsMoved + " URLs, " +
        auArtifactsMoved + " versions copied.";
    }
    return currentStatus;
  }

  public boolean isRunning() {
    return running;
  }

  /**
   * Move one au iff it's not already in the queue
   *
   * @param args arg block holding all request args
   * @throws IOException if unable to connect to services or other error
   */
  public void moveOneAu(Args args) throws IOException {
    if (args.au == null) {
      throw new IllegalArgumentException("No AU supplied");
    }
    try {
      initRequest(args);
      currentStatus = "Checking V2 services";
      if (v2ServicesUnavailable()) {
        throw new IOException("V2 Services are not ready.");
      }
      // get the aus known to the v2 repository
      getV2Aus();
      auMoveQueue.add(args.au);
      while (!terminated && auMoveQueue.iterator().hasNext()) {
        moveNextAu();
      }
    } finally {
      totalRunTime = System.currentTimeMillis() - startTime;
      closeReport();
    }
  }

  /**
   * Move all AUs that match the select patterns and aren't already in
   * the queue
   *
   * @param args arg block holding all request args
   * @throws IOException if unable to connect to services or other error
   */
  public void moveAllAus(Args args) throws IOException {
    try {
      initRequest(args);
      currentStatus = "Checking V2 services";
      if (v2ServicesUnavailable()) {
        throw new IOException("V2 Services are not ready.");
      }
      // get the aus known to the v2 repo
      getV2Aus();
      // get the local AUs to move
      for (ArchivalUnit au : pluginManager.getAllAus()) {
        // Don't copy plugin registry AUs
        if (pluginManager.isInternalAu(au)) {
          continue;
        }
        // Filter by selection pattern if set
        if (args.selPatterns == null || args.selPatterns.isEmpty() ||
            isMatch(au.getAuId(), args.selPatterns)) {
          auMoveQueue.add(au);
        }
      }
      totalAusToMove = auMoveQueue.size();
      log.debug("Moving " + totalAusToMove + " aus.");

      while (!terminated && auMoveQueue.iterator().hasNext()) {
        moveNextAu();
        log.debug2("moveNextAu() returned: terminated: " +
                   terminated + ", queue: " + auMoveQueue);
      }
    } finally {
      totalRunTime = System.currentTimeMillis() - startTime;
      closeReport();
    }
  }

  /**
   * Returns the list of errors which occurred while attempting to move the AU(s).
   *
   * @return the list of error strings
   */
  public List<String> getErrors() {
    return errorList;
  }

  /**
   * Initialize the request with the information needed to establish a connection,
   * zero out any counters and open file for reporting.
   *
   * @param host the v2 hostname
   * @param uname the v2 user name
   * @param upass the v2 user password
   * @throws IllegalArgumentException if host is cannot be made into valid url.
   */
  void initRequest(Args args) throws
      IllegalArgumentException {
    currentStatus = "Initializing";
    running = true;
    Configuration config = ConfigManager.getCurrentConfig();
    // allow for changing these between runs when testing
    debugRepoReq = config.getBoolean(DEBUG_REPO_REQUEST, DEFAULT_DEBUG_REPO_REQUEST);
    debugConfigReq = config.getBoolean(DEBUG_CONFIG_REQUEST, DEFAULT_DEBUG_CONFIG_REQUEST);
    maxRequests = config.getInt(PARAM_MAX_REQUESTS, DEFAULT_MAX_REQUESTS);
    hostName = config.get(PARAM_HOSTNAME, DEFAULT_HOSTNAME);

    if (args.host != null) {
      hostName = args.host;
    }
    if (args.uname != null) {
      userName = args.uname;
    }
    if (args.upass != null) {
      userPass = args.upass;
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
    }
    catch (MalformedURLException mue) {
      errorList.add("Error parsing REST Configuration Service URL: "
          + mue.getMessage());
      throw new IllegalArgumentException(
          "Missing or Invalid configuration service hostName: " + hostName + " port: " + cfgPort);
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
    }
    catch (MalformedURLException mue) {
      errorList.add("Error parsing REST Configuration Service URL: " + mue.getMessage());
      throw new IllegalArgumentException(
          "Missing or Invalid configuration service hostName: " + hostName + " port: " + repoPort);
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
   * Start appending the Report file for current Au move request
   */
  void startReportFile() {

    try {
      log.info("Writing report to file " + reportFile.getAbsolutePath());
      reportWriter = new PrintWriter(Files.newOutputStream(reportFile.toPath(), CREATE, APPEND),
          true);
      reportWriter.println("--------------------------------------------------");
      reportWriter.println("  V2 Au Migration Report - " + DateFormatter.now());
      reportWriter.println("--------------------------------------------------");
      reportWriter.println();
      if (reportWriter.checkError()) {
        log.warning("Error writing report file.");
      }
    }
    catch (IOException e) {
      log.error("Report file will not be written: Unable to open report file:" + e.getMessage());
    }
  }

  /**
   * Update the report for the current Au
   *
   * @param auName the name of the current Au
   */
  void updateReport(String auName) {
    String auData = "UrlsMoved: " + bigIntFmt.format(auUrlsMoved) +
      ", VersionsMoved: " + bigIntFmt.format(auArtifactsMoved) +
      ", ContentBytesMoved: " + bigIntFmt.format(auContentBytesMoved) +
      ", TotalBytesMoved: "  + bigIntFmt.format(auBytesMoved) +
      ", ByteRate: " + StringUtil.byteRateToString(auBytesMoved, auRunTime) +
      ", Errors: " + auErrorCount +
      ", TotalRuntime: " + StringUtil.timeIntervalToString(auRunTime);
    if (reportWriter != null) {
      reportWriter.println("AU Name: " + auName);
      reportWriter.println("AU ID: " + currentAu.getAuId());
      if (terminated) {
        reportWriter.println("Move terminated with error.");
        reportWriter.println(auData);
      }
      else {
        if (isPartialContent) {
          if (auArtifactsMoved > 0) {// if we moved something
            reportWriter.println("Moved remaining unmigrated au content.");
            reportWriter.println(auData);
          }
          else {
            reportWriter.println("All au content already migrated.");
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
    StringBuilder sb = new StringBuilder();
    sb.append("AusMoved: ");
    sb.append(totalAusMoved);
    if (totalAusPartiallyMoved > 0) {
      sb.append(" (");
      sb.append(totalAusMoved);
      sb.append(" partially)");
    }
    if (totalAusSkipped > 0) {
      sb.append(", AusSkipped: ");
      sb.append(totalAusSkipped);
    }
    sb.append(", UrlsMoved: ");
    sb.append(bigIntFmt.format(totalUrlsMoved));
    sb.append(", VersionsMoved: ");
    sb.append(bigIntFmt.format(totalArtifactsMoved));
    sb.append(", ContentBytesMoved: ");
    sb.append(bigIntFmt.format(totalContentBytesMoved));
    sb.append(", TotalBytesMoved: ");
    sb.append(bigIntFmt.format(totalBytesMoved));
    sb.append(", ByteRate: ");
    sb.append(StringUtil.byteRateToString(totalBytesMoved, totalRunTime));
    sb.append(", Errors: ");
    sb.append(bigIntFmt.format(totalErrorCount));
    sb.append(", TotalRuntime: ");
    sb.append(StringUtil.timeIntervalToString(totalRunTime));
    String summary = sb.toString();
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

  /**
   * Select the next au to move.
   * @throws IOException on network failures.
   */
  protected void moveNextAu() throws IOException {
    ArchivalUnit au = auMoveQueue.iterator().next();
    workingOn++;
    // Marker for getCurrentStatus() to return current AU's progress. 
    currentStatus = STATUS_COPYING;
    currentAu = au;
    allCusQueued = false;
    log.debug("Moving " + au.getName() + " - " + auMoveQueue.size() + " AUs remaining.");
    auBytesMoved = 0;
    auContentBytesMoved = 0;
    auUrlsMoved = 0;
    auArtifactsMoved = 0;
    auErrorCount = 0;
    auErrors.clear();
    auRunTime = 0;
    try {
      moveAu(au);
      log.debug2("moveAu returned");
    } finally {
      auMoveQueue.remove(au);
    }
  }

  /**
   * Queue an Au's artifacts for moving.
   * @param au the au to move
   * @throws IOException on unexpected network failures
   */
  void moveAu(ArchivalUnit au) throws IOException {
    long au_move_start = System.currentTimeMillis(); // Get the start Time
    String auName = au.getName();
    log.info("Handling request to move AU: " + auName);
    log.info("AuId: " + currentAu.getAuId());
    if (v2Aus.contains(au.getAuId())) {
      if (!checkMissingContent) {
        log.info("V2 Repo already has au " + au.getName() + ", skipping.");
        totalAusSkipped++;
        return;
      }
      else {
        log.info("V2 Repo already has au " + au.getName() + ", added to check for unmoved content.");
      }
    }

    try {
      if (v2ServicesUnavailable()) {
        terminated = true;
        return;
      }
      log.info(auName + ": Moving AU Artifacts...");
      moveAuArtifacts(au);
      finishAuMove(au);
      if (!terminated) {
        log.info(auName + ": Successfully moved AU Artifacts.");
      }
      else {
        log.info(auName + ": Au move terminated because of errors.");
        totalAusWithErrors++;
      }
      auRunTime = System.currentTimeMillis() - au_move_start;
      updateReport(auName);
      totalAusMoved++;
      if (v2Aus.contains(au.getAuId())) {
        totalAusPartiallyMoved++;
      }
      updateTotals();
    }
    catch (Exception ex) {
      updateReport(auName);
      totalAusWithErrors++;
      String err;
      if (ex instanceof ApiException) {
        err = auName + ": Attempt to move Au failed:" + ((ApiException) ex).getCode() + ": "
            + ex.getMessage();
      }
      else {
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
  void moveAuArtifacts(ArchivalUnit au) throws ApiException {
    //Get the au artifacts from the v1 repo.
    /* get Au cachedUrls from Lockss*/
    for (CuIterator iter = au.getAuCachedUrlSet().getCuIterator(); iter.hasNext(); ) {
      List<Artifact> cuArtifacts = new ArrayList<>();
      CachedUrl cachedUrl = iter.next();
      String v1Url = cachedUrl.getUrl();
      String v2Url = null;
      try {
        v2Url = UrlUtil.normalizeUrl(cachedUrl.getProperties().getProperty(CachedUrl.PROPERTY_NODE_URL),
            au);
      }
      catch (Exception ex) {
        log.warning("Unable to normalize uri for " + v1Url, ex);
      }
      finally {
        AuUtil.safeRelease(cachedUrl);
      }
      if (v2Url == null) {
        v2Url = v1Url;
      }
      log.debug3("v1 url=" + v1Url +"  v2 url= " + v2Url);
      // we have the possibility that some or all of the artifacts were moved.
      // We are looking for previously moved versions of the 'current' cu
      ArtifactPageInfo pageInfo;
      String token = null;
      // if the v2 repo knows about this au we need to call getArtifacts.
      if (v2Aus.contains(au.getAuId())) {
        isPartialContent = true;
        log.debug2("Checking for unmoved content: " + v2Url);
        do {
          pageInfo = repoCollectionsApiClient.getArtifacts(collection, au.getAuId(),
              v2Url, null, "all", false, null, token);
          cuArtifacts.addAll(pageInfo.getArtifacts());
          token = pageInfo.getPageInfo().getContinuationToken();
        } while (!terminated && !StringUtil.isNullString(token));
        log.debug2("Found " + cuArtifacts.size() + " matches for " + v2Url);
      }
      moveCuVersions(v2Url, cachedUrl, cuArtifacts);
    }
    allCusQueued = true;
  }

  /**
   * Move all versions of a cachedUrl.
   *
   * @param v2Url       The uri for the current cached url.
   * @param cachedUrl   The cachedUrl we which to move
   * @param v2Artifacts The list of artifacts which already match this cachedUrl uri.
   */
  void moveCuVersions(String v2Url, CachedUrl cachedUrl, List<Artifact> v2Artifacts) {
    if (!terminated) {
      String auid = cachedUrl.getArchivalUnit().getAuId();
      CachedUrl[] localVersions = cachedUrl.getCuVersions();
      Queue<CachedUrl> cuQueue = Collections.asLifoQueue(new ArrayDeque<>());
      int v2Count = v2Artifacts.size();
      //If we have more v1 versions than the v2 repo - copy the missing items
      if (v2Count > 0) {
        log.debug3("v2 versions available=" + v2Count + " v1 versions available="
            + localVersions.length);
      }
      // if the v2 repository has fewer versions than the v1 repository
      // then move the missing versions or release the cu version.
      int vers_to_move = localVersions.length - v2Count;
      log.debug3("Queueing " + vers_to_move + "/" + localVersions.length + " versions...");
      if (vers_to_move > 0) {
        for (int vx = 0; vx < localVersions.length; vx++) {
          CachedUrl ver = localVersions[vx];
          if (vx < vers_to_move) {
            cuQueue.add(ver);
          }
          else {
            AuUtil.safeRelease(ver);
          }
        }
        moveNextCuVersion(auid, v2Url, cuQueue);
      }
    }
  }

  /**
   * Queue a request to create an artifact for the next version of a CachedUrl
   * @param auid The au we are moving
   * @param v2Url The v2 url
   * @param cuQueue The queue of cached url versions.
   */
  void moveNextCuVersion(String auid, String v2Url, Queue<CachedUrl> cuQueue) {
    Long collectionDate = null;
    CachedUrl cu = cuQueue.poll();
    if (cu == null) {
      if (log.isDebug3()) {
        log.debug3("All versions of " + v2Url + " have been queued.");
      }
      return;
    }
    try {
      String fetchTime = cu.getProperties().getProperty(CachedUrl.PROPERTY_FETCH_TIME);
      if (!StringUtil.isNullString(fetchTime)) {
        collectionDate = Long.parseLong(fetchTime);
      }
      else {
        log.debug2(v2Url + ":version: " + cu.getVersion() + " is missing fetch time.");
      }
      if (log.isDebug3()) {
        log.debug3("Moving cu version " + cu.getVersion() + " - fetched at " + fetchTime);
      }
      createArtifact(auid, v2Url, collectionDate, cu, collection, cuQueue);
    }
    catch (ApiException apie) {
      String err = v2Url + ": failed to create version: " + cu.getVersion() + ": " +
          apie.getCode() + " - " + apie.getMessage();
      log.warning(err);
      auErrors.add(err);
      errorList.add(err);
      auErrorCount++;
    }
    finally {
      AuUtil.safeRelease(cu);
    }
  }

  /**
   * Check V2 service status for ready
   *
   * @return true if status for repository  is ready, false if either are not ready
   * @throws IOException if server is unable to return status.
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
      throw new IOException("Unable to get status for V2 services: " + msg);
    }
    return result;
  }

  /**
   * Get all of the Aus known to the V2 repository.
   * @throws IOException if repostitory is unreachable.
   */
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
    }
    catch (ApiException apie) {
      String err = "Error occurred while retrieving V2 Au list: " + apie.getMessage();
      errorList.add(err);
      log.error(err, apie);
      String msg = apie.getCode() == 0 ? apie.getMessage()
          : apie.getCode() + " - " + apie.getMessage();
      throw new IOException("Unable to get Au List from V2 Repository: " + msg);
    }
  }


  /**
   * update the reported totals after completing an au move
   */
  void updateTotals() {
    totalBytesMoved += auBytesMoved;
    totalContentBytesMoved += auContentBytesMoved;
    totalUrlsMoved += auUrlsMoved;
    totalArtifactsMoved += auArtifactsMoved;
    totalErrorCount += auErrorCount;
  }

  /* ------------------
  testing getters & setters
 */
  void setAuCounters(long urls, long artifacts, long bytes, long contentBytes, long runTime,
      long errors,
      List<String> errs) {
    auUrlsMoved = urls;
    auArtifactsMoved = artifacts;
    auBytesMoved = bytes;
    auContentBytesMoved = contentBytes;
    auRunTime = runTime;
    auErrorCount = errors;
    auErrors = errs;
  }

  void setTotalCounters(long aus, long urls, long artifacts, long bytes, long contentBytes,
      long runTime, long errors) {
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

  void setCurrentAu(ArchivalUnit au) {
    currentAu = au;
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
   * Make an asynchronous rest call to the V2 repository to create a new artifact.
   *
   * @param auid           au identifier for the CachedUrl we are moving.
   * @param v2Url          the uri  for the CachedUrl we are moving.
   * @param collectionDate the date at which this item was collected or null
   * @param cu             the CachedUrl we are moving.
   * @param collectionId   the v2 collection we are moving to
   * @throws ApiException the rest exception thrown should anything fail in the request.
   */
  private void createArtifact(String auid, String v2Url, Long collectionDate,
      CachedUrl cu, String collectionId, Queue<CachedUrl> cuQueue) throws ApiException {
    log.debug3("enqueing create artifact request...");
    DigestCachedUrl dcu = new DigestCachedUrl(cu);
    repoCollectionsApiClient.createArtifactAsync(collectionId, auid, v2Url, dcu, collectionDate,
        new CreateArtifactCallback(auid, v2Url, dcu, cuQueue));
  }

  /**
   * Make a synchronous rest call to commit the artifact that just completed successful creation.
   *
   * @param uncommitted the v2 artifact to be committed
   * @param dcu
   * @throws ApiException the rest exception thrown should anything fail in the request.
   */
  private void commitArtifact(Artifact uncommitted, DigestCachedUrl dcu) throws ApiException {
    Artifact committed;
    log.debug3("committing artifact " + uncommitted.getId());
    committed = repoCollectionsApiClient.updateArtifact(uncommitted.getCollection(),
        uncommitted.getId(), true);
    if (!committed.getContentDigest().equals(dcu.getContentDigest())) {
      String err="Error in commit of " + dcu.getCu().getUrl() + " content digest do not match";
      log.error(err);
      log.debug("v1 digest: " +dcu.getContentDigest()+  " v2 digest: " + committed.getContentDigest());
      errorList.add(err);
      auErrors.add(err);
      auErrorCount++;
    } else {
      if (log.isDebug2()) {
        log.debug2("Hash match: " + dcu.getCu().getUrl() + ": v1 digest: " +dcu.getContentDigest()+  " v2 digest: " + committed.getContentDigest());
      }
      log.debug3("Successfully committed artifact " + committed.getId());
      auArtifactsMoved++;
    }

  }

  /**
   * Complete the au move by moving the state and config information.
   *
   * @param au the au we are moving.
   */
  private void finishAuMove(ArchivalUnit au) {
    log.debug("Waiting for move to complete all processing for " + au.getName() + ".");
    do { //Wait until we are done the processing
      try {
        Thread.sleep(200);
      }
      catch (InterruptedException e) {
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
  private void moveAuConfig(ArchivalUnit au) {
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
      }
      catch (Exception ex) {
        String err = auName + ": Attempt to move au configuration failed: " + ex.getMessage();
        log.error(err, ex);
        errorList.add(err);
        auErrors.add(err);
        auErrorCount++;
      }
    }
    else {
      log.warning(auName + ": No Configuration found for au");
    }
  }

  /**
   *  Make a synchronous rest call to V2 configuration service to add the V1 Au Agreement Table.
   *
   * @param au the archival unit to be updated.
   */
  private void moveAuAgreements(ArchivalUnit au) {
    AuAgreements v1AuAgreements = idManager.findAuAgreements(au);
    String auName = au.getName();
    if (v1AuAgreements != null) {
      try {
        cfgAusApiClient.patchAuAgreements(au.getAuId(), v1AuAgreements.getPrunedBean(au.getAuId()),
            makeCookie());
        log.info(auName + ": Successfully moved AU Agreements.");
      }
      catch (Exception ex) {
        String err = auName + ": Attempt to move au agreements failed: " + ex.getMessage();
        log.error(err, ex);
        errorList.add(err);
        auErrors.add(err);
        auErrorCount++;
      }
    }
    else {
      log.warning("No Au agreements found for au.");
    }
  }

  /**
   * Make a synchronous rest call to V2 configuration service to add the V1 Au Suspect Urls list.
   *
   * @param au the archival unit to be updated.
   */
  private void moveAuSuspectUrlVersions(ArchivalUnit au) {
    AuSuspectUrlVersions asuv = AuUtil.getSuspectUrlVersions(au);
    String auName = au.getName();
    if (asuv != null) {
      try {
        cfgAusApiClient.putAuSuspectUrlVersions(au.getAuId(), asuv.getBean(au.getAuId()),
            makeCookie());
        log.info(auName + ": Successfully moved AU Suspect Url Versions.");
      }
      catch (Exception ex) {
        String err =
            auName + ": Attempt to move au suspect url versions failed: " + ex.getMessage();
        log.error(err, ex);
        errorList.add(err);
        auErrors.add(err);
        auErrorCount++;
      }
    }
    else {
      log.warning(auName + ": No Au suspect url versions found.");
    }
  }

  /**
   * Make a synchronous rest call to V2 configuration service to add the V1 Au NoAuPeerSet list.
   *
   * @param au the archival unit to be updated.
   */
  private void moveNoAuPeerSet(ArchivalUnit au) {
    DatedPeerIdSet noAuPeerSet = pollManager.getNoAuPeerSet(au);
    String auName = au.getName();
    if (noAuPeerSet instanceof DatedPeerIdSetImpl) {
      try {
        cfgAusApiClient.putNoAuPeers(au.getAuId(),
            ((DatedPeerIdSetImpl) noAuPeerSet).getBean(au.getAuId()), makeCookie());
        log.info(auName + ": Successfully moved no Au peers.");
      }
      catch (Exception ex) {
        String err = auName + ": Attempt to move no AU peers failed: " + ex.getMessage();
        log.error(err, ex);
        errorList.add(err);
        auErrors.add(err);
        auErrorCount++;
      }
    }
    else {
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
      }
      catch (ApiException apie) {
        String err = auName + ": Attempt to move au state failed: " + apie.getCode() +
            "- " + apie.getMessage();
        log.error(err, apie);
        errorList.add(err);
        auErrors.add(err);
        auErrorCount++;
      }
    }
    else {
      log.warning(auName + ": No State information found for au");
    }
  }

  private String makeCookie() {
    return cliendId + "-" + ++reqId;
  }

  void logErrorBody(Response response) {
    try {
      if(response.body() != null)
        log.warning("Error response body: " + response.body().string());
    }
    catch (IOException e) {
      log.error("Exception trying to retrieve error response body", e);
    }
  }

  /**
   * A simple class to encompass an ApiCallback
   */
  protected class CreateArtifactCallback implements ApiCallback<Artifact> {

    String auId;
    String v2Url;
    DigestCachedUrl dcu;
    Queue<CachedUrl> cuQueue;
    long contentSize;

    public CreateArtifactCallback(String auid, String uri, DigestCachedUrl dcu,
        Queue<CachedUrl> cuQueue) {
      this.auId = auid;
      this.v2Url = uri;
      this.dcu = dcu;
      this.cuQueue = cuQueue;
      this.contentSize = dcu.getCu().getContentSize();
    }

    @Override
    public void onFailure(ApiException e, int statusCode,
        Map<String, List<String>> responseHeaders) {
      String err =
          "Create Artifact for " + v2Url + " failed: " + statusCode + " - " + e.getMessage();
      log.debug(err);
      errorList.add(err);
      auErrors.add(err);
      auErrorCount++;
    }

    @Override
    public void onSuccess(Artifact result, int statusCode,
        Map<String, List<String>> responseHeaders) {
      try {
        log.debug3("Successfully created artifact (" + statusCode + "): " + result.getId());
        commitArtifact(result, dcu);
        auContentBytesMoved += contentSize;
        if (cuQueue.peek() != null) {
          moveNextCuVersion(auId, v2Url, cuQueue);
        }
        else {
          auUrlsMoved++;
        }
      }
      catch (ApiException e) {
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

  public class DigestCachedUrl {
    MessageDigest md;
    CachedUrl cu;
    String HASH_ALGORITHM="SHA-256";
    String contentDigest=null;

    public DigestCachedUrl(CachedUrl cu) {
      this.cu = cu;
    }

    public MessageDigest getMessageDigest() {
      if (md == null)
        try {
          md = MessageDigest.getInstance(HASH_ALGORITHM);
        }
        catch (NoSuchAlgorithmException e) {
          log.critical("Digest algorithm: " + HASH_ALGORITHM + ": "
                       + e.getMessage());
        }
     return md;
    }


    public CachedUrl getCu() {
      return cu;
    }

    public String getContentDigest() {
      if( contentDigest == null) {
        contentDigest = String.format("%s:%s",
            HASH_ALGORITHM,
            new String(Hex.encodeHex(md.digest())));
        log.debug2("contentDigest: " + contentDigest);
      }
      return contentDigest;
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
              // no retries
              break;
            }
            // close response before retry
            response.close();
          }
        }
        catch (final IOException ioe) {
          if (tryCount < maxRetryCount) {
            log.debug3("Retrying: " + ioe.getMessage());
            if (response != null) {
              // close response before retry
              response.close();
            }
          }
          else {
            log.debug("Exceeded retries - exiting");
            terminated = true;
            if (response != null) {
              response.close();
            }
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
          String msg = errCode + " - " + response.message() + ": " + response.body();
          response.close();
          throw new IOException(msg);
        }
      }
      // last try should proceed as is
      return chain.proceed(request);
    }
  }

  /** Argument block from MigrateContent servlet */
  public static class Args {
    String host;
    String uname;
    String upass;
    List<Pattern> selPatterns;
    ArchivalUnit au;

    public Args setHost(String host) {
      this.host = host;
      return this;
    }
    public Args setUname(String uname) {
      this.uname = uname;
      return this;
    }
    public Args setUpass(String upass) {
      this.upass = upass;
      return this;
    }
    public Args setSelPatterns(List<Pattern> selPatterns) {
      this.selPatterns = selPatterns;
      return this;
    }
    public Args setAu(ArchivalUnit au) {
      this.au = au;
      return this;
    }
  }
}


