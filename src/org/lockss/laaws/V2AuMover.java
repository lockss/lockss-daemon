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
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import okhttp3.Dispatcher;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.codec.binary.Hex;
import org.lockss.app.LockssDaemon;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.LockssRunnable;
import org.lockss.laaws.api.cfg.AusApi;
import org.lockss.laaws.api.rs.StreamingCollectionsApi;
import org.lockss.laaws.client.ApiException;
import org.lockss.laaws.client.V2RestClient;
import org.lockss.laaws.model.rs.Artifact;
import org.lockss.laaws.model.rs.ArtifactPageInfo;
import org.lockss.laaws.model.rs.AuidPageInfo;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.CachedUrlSet;
import org.lockss.plugin.CuIterator;
import org.lockss.plugin.PluginManager;
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
   * Max number of worker threads
    */
  public static final String PARAM_MAX_THREADS = PREFIX + "max_threads";
  public static final int DEFAULT_MAX_THREADS = 20;

  /**
   * Maximum number of queued tasks 
   */
  public static final String PARAM_QUEUE_MAX = PREFIX + "queueMax";
  public static final int DEFAULT_QUEUE_MAX = 50;

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

  /**
   * The flag to indicate if Auditor should compare artifact bytes with cachedUrl
   */
  public static final String PARAM_COMPARE_CONTENT = PREFIX + "compare.content";
  public static final boolean DEFAULT_COMPARE_CONTENT = false;

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
  private final ArrayList<String> v2Aus = new ArrayList<>();
  private final LinkedHashSet<ArchivalUnit> auMoveQueue = new LinkedHashSet<>();
  private BlockingQueue<Runnable> taskQueue;
  private ThreadPoolExecutor taskExecutor;
  private Dispatcher dispatcher;
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
  private boolean compareBytes;


  public V2AuMover() {
    // Get our lockss daemon managers
    pluginManager = LockssDaemon.getLockssDaemon().getPluginManager();
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
    boolean compareBytes=config.getBoolean(PARAM_COMPARE_CONTENT, DEFAULT_COMPARE_CONTENT);
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

  StreamingCollectionsApi getRepoCollectionsApiClient() {
    return repoCollectionsApiClient;
  }

  public String getCollection() {
    return collection;
  }

  public AusApi getCfgAusApiClient() {
    return cfgAusApiClient;
  }

  public String makeCookie() {
    return cliendId + "-" + ++reqId;
  }

  public ArrayList<String> getKnownV2Aus() {
    return v2Aus;
  }

  /**
   * Initialize the request with the information needed to establish a connection,
   * zero out any counters and open file for reporting.
   *
   * @param args the arguments for this request.
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
    int maxThreads = config.getInt(PARAM_MAX_THREADS, DEFAULT_MAX_THREADS);
    hostName = config.get(PARAM_HOSTNAME, DEFAULT_HOSTNAME);

    int queueMax = config.getInt(PARAM_QUEUE_MAX, DEFAULT_QUEUE_MAX);
    taskQueue = new LinkedBlockingQueue<>(queueMax);
    taskExecutor = makeBlockingThreadPoolExecutor(maxThreads/*1, maxThreads,
                                                  60, TimeUnit.SECONDS,
                                                  taskQueue*/);
                                          

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
      dispatcher.setMaxRequests(maxThreads);
      dispatcher.setMaxRequestsPerHost(maxThreads);
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

  public ThreadPoolExecutor makeBlockingThreadPoolExecutor(int maxThreads) {
    ThreadPoolExecutor exec =
      new ThreadPoolExecutor(1, maxThreads,
                                 60, TimeUnit.SECONDS,
                                 taskQueue);
    exec.setRejectedExecutionHandler(new RejectedExecutionHandler() {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
          try {
            // block until there's room
            executor.getQueue().put(r);
            // check afterwards and throw if pool shutdown
            if (executor.isShutdown()) {
              throw new RejectedExecutionException("Task " + r +
                                                   " rejected from because shutdown");
            }
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RejectedExecutionException("Producer interrupted", e);
          }
        }
      });
    return exec;
  }

  public class TaskRunner extends LockssRunnable {
    MigrationTask task;
    V2AuMover v2Mover;

    public TaskRunner(V2AuMover v2Mover, MigrationTask task) {
      super("V2AuMover");
      this.v2Mover = v2Mover;
      this.task = task;
    }

    public void lockssRun() {
      switch (task.getType()) {
      case COPY_CU_VERSIONS:
        CachedUrl cu = task.getCu();
        CuMover cumover = new CuMover(v2Mover, cu.getArchivalUnit(), cu);
        cumover.run();
        break;
      case COPY_AU_STATE:
        AuStateMover asmover = new AuStateMover(v2Mover, task.getAu());
        asmover.run();
        break;
      default:
        log.error("Unknown migration task type: " + task.getType());
      }
    }
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
    StringBuilder sb = new StringBuilder();
    sb.append(StringUtil.bigNumberOfUnits(auUrlsMoved, "URL") + " moved, ");
    sb.append(StringUtil.bigNumberOfUnits(auArtifactsMoved, "version") + ", ");
    sb.append(StringUtil.bigNumberOfUnits(auContentBytesMoved, "content byte") + ", ");
    sb.append(StringUtil.bigNumberOfUnits(auBytesMoved, "total byte") + ", ");
    sb.append("at ");
    sb.append(StringUtil.byteRateToString(auBytesMoved, auRunTime));
    sb.append(", ");
    sb.append(StringUtil.bigNumberOfUnits(auErrorCount, "error"));
    sb.append(", in ");
    sb.append(StringUtil.timeIntervalToString(auRunTime));
    String auData = sb.toString();
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
    sb.append(StringUtil.bigNumberOfUnits(totalAusMoved + totalAusSkipped, "AU") + " moved");
    if (totalAusPartiallyMoved > 0 || totalAusSkipped > 0) {
      sb.append(" (");
      if (totalAusSkipped > 0) {
        sb.append(bigIntFmt.format(totalAusSkipped));
        sb.append(" previously");
        if (totalAusPartiallyMoved > 0) {
          sb.append(", ");
        }
      }
      if (totalAusPartiallyMoved > 0) {
        sb.append(bigIntFmt.format(totalAusPartiallyMoved));
        sb.append(" partially)");
      }
      sb.append(")");
    }
    sb.append(", ");
    sb.append(StringUtil.bigNumberOfUnits(totalUrlsMoved, "URL") + ", ");
    sb.append(StringUtil.bigNumberOfUnits(totalArtifactsMoved, "version") + ", ");
    sb.append(StringUtil.bigNumberOfUnits(totalContentBytesMoved, "content byte") + ", ");
    sb.append(StringUtil.bigNumberOfUnits(totalBytesMoved, "total byte") + ", ");
    if (totalBytesMoved > 0) {
      sb.append("at ");
      sb.append(StringUtil.byteRateToString(totalBytesMoved, totalRunTime));
      sb.append(", ");
    }
    sb.append(StringUtil.bigNumberOfUnits(totalErrorCount, "error") + ", ");
    sb.append("in ");
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
      // XXX wrong - need to wait until all workers are done
      while (!taskQueue.isEmpty()) {
        try {
          Thread.sleep(500);
        }
        catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
      moveAuState(au);
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
      addError(err);
      totalErrorCount += auErrorCount;
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
   */
  void moveAuArtifacts(ArchivalUnit au) {
    // Get the au CachedUrls from the v1 repo.
    for (CachedUrl cu : au.getAuCachedUrlSet().getCuIterable()) {
      
      taskExecutor.execute(new TaskRunner(this,
                                          MigrationTask.copyCuVersions(this,
                                                                       au,
                                                                       cu)));
    }
  }

  /**
   * Complete the au move by moving the state and config information.
   *
   * @param au the au we are moving.
   */
  void moveAuState(ArchivalUnit au) {
    if (!terminated) {
      taskExecutor.execute(new TaskRunner(this,
          MigrationTask.copyAuState(this,
              au)));
    }
  }

  public String getV2Url(ArchivalUnit au, CachedUrl cu) {
    String v1Url = cu.getUrl();
    String v2Url = null;
    try {
      v2Url = UrlUtil.normalizeUrl(cu.getProperties().getProperty(CachedUrl.PROPERTY_NODE_URL),
          au);
    }
    catch (Exception ex) {
      log.warning("Unable to normalize uri for " + v1Url, ex);
    }
    finally {
      AuUtil.safeRelease(cu);
    }
    if (v2Url == null) {
      v2Url = v1Url;
    }
    if(log.isDebug3())
      log.debug3("v1 url=" + v1Url +"  v2 url= " + v2Url);
    return v2Url;
  }

  public List<String> getAlreadyKnownV2AuIds() {
    return v2Aus;
  }

  private List<Artifact> getV2ArtifactsForUrl(String auId,  String v2Url)
      throws ApiException {
    ArtifactPageInfo pageInfo;
    String token = null;
    List<Artifact> cuArtifacts = new ArrayList<>();
    // if the v2 repo knows about this au we need to call getArtifacts.
    if (v2Aus.contains(auId)) {
      isPartialContent = true;
      log.debug2("Checking for unmoved content: " + v2Url);
      do {
        pageInfo = repoCollectionsApiClient.getArtifacts(collection, auId,
            v2Url, null, "all", false, null, token);
        cuArtifacts.addAll(pageInfo.getArtifacts());
        token = pageInfo.getPageInfo().getContinuationToken();
      } while (!terminated && !StringUtil.isNullString(token));
      log.debug2("Found " + cuArtifacts.size() + " matches for " + v2Url);
    }
    return cuArtifacts;
  }



  /**
   * Check for a successful move of au content
   * @param au The ArchivalUnit we moved.
   * @param compareBytes true if each artifact should be fetched and byteCompare.
   */
  void checkAu(ArchivalUnit au, boolean compareBytes) {
    long au_check_start = System.currentTimeMillis(); // Get the start Time
    String auName = au.getName();
    String auId = au.getAuId();
    final String TRUE = "true";
    //Get the cachedUrls from the v1 repo.
    final CachedUrlSet auCachedUrlSet = au.getAuCachedUrlSet();
    // check all metadata
    log.info("Checking content on v2 service");
    /* get Au cachedUrls from Lockss*/
    for (CuIterator iter = au.getAuCachedUrlSet().getCuIterator(); iter.hasNext(); ) {
      CachedUrl cu = iter.next();
      CachedUrl[] cuVersions = cu.getCuVersions();
      String v2Url = getV2Url(au, cu);
      List<Artifact> cuArtifacts;
      try {
        cuArtifacts = getAllCuArtifacts(au, v2Url);
        if(!terminated) {
          //Todo: rename v1Artifact && v2Artifacts
          int versionCompare = cuArtifacts.size() - cuVersions.length;

          if (versionCompare > 0) {
            String err = "Mismatched version count for " + v2Url
                + ": V2 Repo has more versions than V1 Repo";
            addError(err);
            terminated = true;
          }
          else if (versionCompare < 0) {
            String err = "Mismatched version count for " + v2Url
                + ": V2 Repo has fewer versions than V1 Repo";
            addError(err);
            terminated = true;
          }
          else {
            log.info("Checking Artifact metadata...");
            for (int ver = 0; ver < cuVersions.length; ver++) {
              Artifact art = cuArtifacts.get(ver);
              CachedUrl cuVersion = cuVersions[ver];
              String fetchTime = cu.getProperties().getProperty(CachedUrl.PROPERTY_FETCH_TIME);
              Long collectionDate = null;
              if (!StringUtil.isNullString(fetchTime)) {
                collectionDate = Long.parseLong(fetchTime);
              }
              else {
                log.debug2(v2Url + ":version: " + cu.getVersion() + " is missing fetch time.");
              }
              boolean isMatch;
              isMatch = art.getAuid().equals(auId) &&
                art.getCollection().equals(collection)  &&
                art.getCollectionDate().equals(collectionDate) &&
                art.getCommitted().equals(Boolean.TRUE);
              if ( isMatch && compareBytes) {
                log.debug3("Fetching  content for byte compare");
/*
              todo: This needs to be fixed to return a ArtifactData at the api level.
              final ArtifactData artifact = repoCollectionsApiClient.getArtifact(collection, art.getId(),
                    "ALWAYS");
*/
              }
            }
          }
        }
      }
      catch (Exception ex) {
        String err = v2Url + " comparison failed: " + ex.getMessage();
        log.error(err, ex);
        addError(err);
        terminated = true;
      }
      finally {
        AuUtil.safeRelease(cu);
      }
    }
    long au_check_stop = System.currentTimeMillis();
    log.info("Au Check Runtime: " + StringUtil.timeIntervalToString(au_check_stop-au_check_start));
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
   * Retrieve all the artifacts for an AU
   * @param au The ArchivalUnit to retrieve
   * @return a list of Artifacts.
   */
  List<Artifact>  getAllCuArtifacts(ArchivalUnit au, String url) {
    List<Artifact> auArtifacts = new ArrayList<>();
    ArtifactPageInfo pageInfo;
    String token = null;
    do {
      try {
        pageInfo = repoCollectionsApiClient.getArtifacts(collection, au.getAuId(),
            url, null, "all", false, null, token);
        auArtifacts.addAll(pageInfo.getArtifacts());
        token = pageInfo.getPageInfo().getContinuationToken();
      }
      catch (ApiException apie) {
        String msg = apie.getCode() == 0 ? apie.getMessage()
            : apie.getCode() + " - " + apie.getMessage();
        String err = "Error occurred while retrieving artifacts for au: " + msg;
        addError(err);
        log.error(err, apie);
        terminated = true;
      }
    } while (!terminated && !StringUtil.isNullString(token));
    return auArtifacts;
  }


  /**
   * Add an Error to the list and update error count;
   * @param err the error string to add.
   */
  void addError(String err) {
    errorList.add(err);
    auErrors.add(err);
    auErrorCount++;
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




  void logErrorBody(Response response) {
    try {
      if(response.body() != null)
        log.warning("Error response body: " + response.body().string());
    }
    catch (IOException e) {
      log.error("Exception trying to retrieve error response body", e);
    }
  }



  public static class DigestCachedUrl {
    MessageDigest md;
    CachedUrl cu;
    static final String HASH_ALGORITHM="SHA-256";
    String contentDigest=null;
    long bytesMoved;

    public DigestCachedUrl(CachedUrl cu) {
      this.cu = cu;
    }

    public MessageDigest createMessageDigest() {
      try {
        md = MessageDigest.getInstance(HASH_ALGORITHM);
        contentDigest=null;
      }
      catch (NoSuchAlgorithmException e) {
        // this should never occur
        log.critical("Digest algorithm: " + HASH_ALGORITHM + ": "
            + e.getMessage());
      }
      return md;
    }

    public MessageDigest getMessageDigest() {
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

    public long getBytesMoved() {
      return bytesMoved;
    }

    public void setBytesMoved(long bytesMoved) {
      this.bytesMoved = bytesMoved;
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
            log.debug("Retrying: " + ioe.getMessage());
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


