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

import org.apache.commons.lang3.time.StopWatch;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import okhttp3.*;
import org.apache.commons.codec.binary.Hex;
import org.lockss.app.LockssDaemon;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.LockssRunnable;
import org.lockss.laaws.api.cfg.AusApi;
import org.lockss.laaws.api.rs.StreamingCollectionsApi;
import org.lockss.laaws.client.ApiException;
import org.lockss.laaws.client.V2RestClient;
import org.lockss.laaws.model.rs.AuidPageInfo;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.PluginManager;
import org.lockss.uiapi.util.DateFormatter;
import org.lockss.util.*;

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

  public static final String EXEC_PREFIX = PREFIX + "executor.";

  /**
   * Executor Spec: <queue-max>;<core-threads>;<max-threads>;
   */
  public static final String PARAM_EXECUTOR_SPEC =
    EXEC_PREFIX + "<name>.spec";

  /**
   * Enqueue task Executor.  One thread so AU tasks are executed in
   * order queued. decent size queue to prevent worker threads from
   * hanging trying to add to it.
   */
  public static final String PARAM_ENQUEUE_EXECUTOR_SPEC =
    EXEC_PREFIX + "enqueue.spec";
  public static final String DEFAULT_ENQUEUE_EXECUTOR_SPEC = "100;1;1";

  /**
   * Copy task Executor.  Queue should be large to reduce waiting for
   * bursty CU iterator.
   */
  public static final String PARAM_COPY_EXECUTOR_SPEC =
    EXEC_PREFIX + "copy.spec";
  public static final String DEFAULT_COPY_EXECUTOR_SPEC = "1000;10;10";

  /**
   * Verify task Executor.  Queue should be large to reduce waiting
   * for bursty CU iterator.
   */
  public static final String PARAM_VERIFY_EXECUTOR_SPEC =
    EXEC_PREFIX + "verify.spec";
  public static final String DEFAULT_VERIFY_EXECUTOR_SPEC = "1000;10;10";

  /**
   * Index Executor.  Controls max simulataneous finishBulk) operations
   */
  public static final String PARAM_INDEX_EXECUTOR_SPEC =
    EXEC_PREFIX + "index.spec";
  public static final String DEFAULT_INDEX_EXECUTOR_SPEC = "50;5;5";

  /**
   * Misc Executor
   */
  public static final String PARAM_MISC_EXECUTOR_SPEC =
    EXEC_PREFIX + "misc.spec";
  public static final String DEFAULT_MISC_EXECUTOR_SPEC = "50;10;10";

  /**
   * Executor thread timeout
   */
  public static final String PARAM_THREAD_TIMEOUT = PREFIX + "thread.timeout";
  public static final long DEFAULT_THREAD_TIMEOUT = 30 * Constants.SECOND;

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
   * The read/write timeout
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
  public static final boolean DEFAULT_CHECK_MISSING_CONTENT = true;

  /**
   * If true, the content will be verified after copying
   */
  public static final String PARAM_VERIFY_CONTENT = PREFIX + "verify.content";
  public static final boolean DEFAULT_VERIFY_CONTENT = false;

  /**
   * If true, the verify step will perform a byte-by-byte comparison
   * between V1 and V2 content
   */
  public static final String PARAM_COMPARE_CONTENT = PREFIX + "compare.content";
  public static final boolean DEFAULT_COMPARE_CONTENT = false;

  /**
   * If true, phase-specific timings will be included with the stats
   */
  public static final String PARAM_DETAILED_STATS = PREFIX + "detailedStats";
  public static final boolean DEFAULT_DETAILED_STATS = true;

  private static final Logger log = Logger.getLogger(V2AuMover.class);

  private static final String STATUS_COPYING = "**Copying**";

  static NumberFormat bigIntFmt = NumberFormat.getInstance();
  private static final DecimalFormat percentFmt = new DecimalFormat("0");

  private int maxRetryCount;
  private long retryBackoffDelay;

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
  private long threadTimeout = DEFAULT_THREAD_TIMEOUT;

  // debug support
  private boolean debugRepoReq;
  private boolean debugConfigReq;

  PluginManager pluginManager;
  private final ArrayList<String> v2Aus = new ArrayList<>();
  private final LinkedHashSet<ArchivalUnit> auMoveQueue = new LinkedHashSet<>();

  // Thread pool for each activity, each with its own queue.
  private ThreadPoolExecutor enqueueExecutor;
  private ThreadPoolExecutor copyExecutor;
  private ThreadPoolExecutor verifyExecutor;
  private ThreadPoolExecutor miscExecutor;
  private ThreadPoolExecutor indexExecutor;


//   private Dispatcher dispatcher;
  private long reqId = 0;

  private long startTime;
  private CountUpDownLatch ausLatch;
  private OneShotSemaphore doneSem = new OneShotSemaphore();
  private boolean terminated = false;
  private boolean isPartialContent = false;
  private boolean checkMissingContent;
  private boolean isCompareBytes;
  private boolean isVerifyContent;
  private boolean isDetailedStats;

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
        DEFAULT_CHECK_MISSING_CONTENT);
    String logdir = config.get(PARAM_REPORT_DIR, DEFAULT_REPORT_DIR);
    String logfile = config.get(PARAM_REPORT_FILE, DEFAULT_REPORT_FILE);
    isVerifyContent=config.getBoolean(PARAM_VERIFY_CONTENT, DEFAULT_VERIFY_CONTENT);
    isCompareBytes=config.getBoolean(PARAM_COMPARE_CONTENT, DEFAULT_COMPARE_CONTENT);
    isDetailedStats=config.getBoolean(PARAM_DETAILED_STATS, DEFAULT_DETAILED_STATS);

    threadTimeout = config.getTimeInterval(PARAM_THREAD_TIMEOUT, DEFAULT_THREAD_TIMEOUT);
    enqueueExecutor = makeExecutorFromnSpec(config.get(PARAM_ENQUEUE_EXECUTOR_SPEC),
                                         DEFAULT_ENQUEUE_EXECUTOR_SPEC);
    copyExecutor = makeExecutorFromnSpec(config.get(PARAM_COPY_EXECUTOR_SPEC),
                                         DEFAULT_COPY_EXECUTOR_SPEC);
    verifyExecutor = makeExecutorFromnSpec(config.get(PARAM_VERIFY_EXECUTOR_SPEC),
                                           DEFAULT_VERIFY_EXECUTOR_SPEC);
    indexExecutor = makeExecutorFromnSpec(config.get(PARAM_INDEX_EXECUTOR_SPEC),
                                          DEFAULT_INDEX_EXECUTOR_SPEC);
    miscExecutor = makeExecutorFromnSpec(config.get(PARAM_MISC_EXECUTOR_SPEC),
                                          DEFAULT_MISC_EXECUTOR_SPEC);

    reportFile = new File(logdir, logfile);
    repoClient = new V2RestClient();
    repoClient.setConnectTimeout((int) connectTimeout);
    repoClient.setReadTimeout((int) readTimeout);
    repoClient.setWriteTimeout((int) readTimeout);
    repoStatusApiClient = new org.lockss.laaws.api.rs.StatusApi(repoClient);
    repoCollectionsApiClient = new StreamingCollectionsApi(repoClient);
    repoClient.setTempFolderPath(PlatformUtil.getSystemTempDir());

    configClient = new V2RestClient();
    configClient.setConnectTimeout((int) connectTimeout);
    configClient.setReadTimeout((int) readTimeout);
    configClient.setWriteTimeout((int) readTimeout);
    configClient.setTempFolderPath(PlatformUtil.getSystemTempDir());
    // Assign the client to the status api and aus api
    cfgStatusApiClient = new org.lockss.laaws.api.cfg.StatusApi(configClient);
    cfgAusApiClient = new AusApi(configClient);
  }

  //
  // Config accessors
  //

  public boolean isCompareBytes() {
    return isCompareBytes;
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
      waitUntilDone();
    } catch (IOException e) {
      log.error("Unexpected exception", e);
      currentStatus = e.getMessage();
      running = false;
    } finally {
    }
  }

  private void waitUntilDone() {
    try {
      // Wait until last AU finishes
      ausLatch.await();
      // Then a while more for complete completion
      doneSem.waitFull(Deadline.in(10000));
    } catch (InterruptedException e) {
      log.warning("waitUntilDone interrupted" + e);
    }
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
    if (pluginManager.isInternalAu(args.au)) {
      throw new IllegalArgumentException("Can't move internal AUs");
    }
    totalTimers.start(Phase.TOTAL);
    initRequest(args);
    currentStatus = "Checking V2 services";
    checkV2ServicesAvailable();
    // get the aus known to the v2 repository
    getV2Aus();
    auMoveQueue.add(args.au);
    moveQueuedAus();
  }

  /**
   * Move all AUs that match the select patterns and aren't already in
   * the queue
   *
   * @param args arg block holding all request args
   * @throws IOException if unable to connect to services or other error
   */
  public void moveAllAus(Args args) throws IOException {
    totalTimers.start(Phase.TOTAL);
    initRequest(args);
    currentStatus = "Checking V2 services";
    checkV2ServicesAvailable();
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
    moveQueuedAus();
  }

  private void moveQueuedAus() {
    ausLatch = new CountUpDownLatch(1, "AU");
    currentStatus = STATUS_COPYING;
    totalAusToMove = auMoveQueue.size();
    log.debug("Moving " + totalAusToMove + " aus.");

    for (ArchivalUnit au : auMoveQueue) {
      if (terminated) {
        break;
      }
      ausLatch.countUp();
      moveAu(au);
    }
    ausLatch.countDown();
    enqueueFinishAll();
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

  public synchronized String makeCookie() {
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
  void initRequest(Args args) throws IllegalArgumentException {
    currentStatus = "Initializing";
    running = true;
    Configuration config = ConfigManager.getCurrentConfig();
    // allow for changing these between runs when testing
    debugRepoReq = config.getBoolean(DEBUG_REPO_REQUEST, DEFAULT_DEBUG_REPO_REQUEST);
    debugConfigReq = config.getBoolean(DEBUG_CONFIG_REQUEST, DEFAULT_DEBUG_CONFIG_REQUEST);
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
        errorList.add("Missing or invalid configuration service url: " + cfgAccessUrl);
        throw new IllegalArgumentException(
            "Missing or invalid configuration service url: " + cfgAccessUrl);
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
          "Missing or invalid configuration service hostName: " + hostName + " port: " + cfgPort);
    }

    try {
      repoAccessUrl = new URL("http", hostName, repoPort, "").toString();
      if (repoAccessUrl == null || UrlUtil.isMalformedUrl(repoAccessUrl)) {
        errorList.add("Missing or invalid repository service url: " + repoAccessUrl);
        throw new IllegalArgumentException(
            "Missing or invalid configuration service url: " + repoAccessUrl);
      }
      // Create a new RepoClient
      repoClient.setUsername(userName);
      repoClient.setPassword(userPass);
      repoClient.setUserAgent(userAgent);
      repoClient.setBasePath(repoAccessUrl);
      repoClient.setDebugging(debugRepoReq);
      repoClient.addInterceptor(new RetryErrorInterceptor());
    }
    catch (MalformedURLException mue) {
      errorList.add("Error parsing REST Configuration Service URL: " + mue.getMessage());
      throw new IllegalArgumentException(
          "Missing or invalid configuration service hostName: " + hostName + " port: " + repoPort);
    }
    startReportFile();
    startTime = now();
  }

  public ThreadPoolExecutor makeExecutor(int queueMax, long queueTimeout,
                                         int coreThreads, int maxThreads) {
    ThreadPoolExecutor exec =
      new ThreadPoolExecutor(coreThreads, maxThreads,
                             queueTimeout, TimeUnit.MILLISECONDS,
                             new LinkedBlockingQueue<>(queueMax));
    exec.allowCoreThreadTimeOut(true);
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

  /**
   * Perform all actions to move an AU: queue CUs, queue state, queue verify
   * @throws IOException on network failures.
   */
  protected void moveAu(ArchivalUnit au) {
    // Marker for getCurrentStatus() to return current AU's progress.
    log.debug("Moving " + au.getName() + " - " + auMoveQueue.size() + " AUs remaining.");
    AuStatus auStat = new AuStatus(au);
    auStat.getCounters().setParent(totalCounters);
    String auName = au.getName();
    log.info("Handling request to move AU: " + auName);
    log.info("AuId: " + au.getAuId());
    if (existsInV2(au)) {
      if (!checkMissingContent) {
        log.info("V2 Repo already has au " + au.getName() + ", skipping.");
        totalAusSkipped++;
        enqueueTask(MigrationTask.finishAu(this, au), auStat, miscExecutor);
//         enqueueFinishAu(auStat);
        return;
      }
      else {
        log.info("V2 Repo already has au " + au.getName() + ", added to check for unmoved content.");
      }
    }

    try {
      auStat.setPhase(Phase.QUEUE);
      addActiveAu(au, auStat);
      log.info("Enqueueing: " + auName);
      // Bulk mode works correctly only if the AU is completely absent
      // from the V2 repo
      if (!existsInV2(au)) {
        // Would be better to delay startBulk() until first copy task runs
        try {
          startBulk(collection, auStat.getAuId());
          auStat.setIsBulk(true);
        } catch (UnsupportedOperationException e) {
          log.debug2("startBulk() not supported");
        }
      }
      enterPhase(auStat, Phase.COPY);
      if (!terminated) {
        log.info("Enqueued AU: " + auName);
      }
      else {
        log.info(auName + ": Au move terminated because of errors.");
        totalAusWithErrors++;
      }
    }
    catch (Exception ex) {
      log.error("Exception in queueing loop", ex);
      updateReport(au, auStat);
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
      auStat.addError(err);
      if (terminated) {
//         throw new IOException("Au Move Request terminated due to errors:" + err);
      }
    }
  }

  void enqueueingError(Throwable t) {
    log.error("Exception in queueing loop, terminating", t);
    terminated = true;
//     auStat.addError(err);
  }


  // phase		wait for		queue
  // queue
  // start
  // copy					copy
  // finishBulk		copy			bulk
  // verify		finishBulk		verify
  // state		verify or finishBulk	misc
  // finish		state			misc

  enum Action { EnqCopy, EnqVerify, EnqState, EnqIndex, FinishAu }

  enum Phase {
    QUEUE("Queued"),
    START("Starting"),
    DONE(""),
    FINISH("Finishing", Action.FinishAu, Phase.DONE),
    STATE("Copying State", Action.EnqState, Phase.FINISH),
    VERIFY("Checking", Action.EnqVerify, Phase.STATE),
    INDEX("Indexing", Action.EnqIndex, Phase.VERIFY),
    COPY("Copying", Action.EnqCopy, Phase.INDEX),
    TOTAL("Total");                     // not really a phase

    private String name;
    private Action enterAction;
    private Phase next;

    Phase(String name) {
      this.name = name;
    }

    Phase(String name, Action enterAction, Phase next) {
      this(name);
      this.enterAction = enterAction;
      this.next = next;
    }

    public Action getEnterAction() {
      return enterAction;
    }

    public Phase getNextPhase() {
      return next;
    }

    public String toString() {
      return name;
    }
  }

  void enterPhase(AuStatus auStat, Phase phase) {
    if (isEnqueueEnterAction(auStat, phase.getEnterAction())) {
      enqueueEnterPhase(auStat, phase);
    } else {
      doEnterPhase(auStat, phase);
    }
  }

  void enqueueEnterPhase(AuStatus auStat, Phase phase) {
    log.debug2("enqueueEnterPhase("+phase+")");
    enqueueExecutor.execute(() -> doEnterPhase(auStat, phase));
  }

  void doEnterPhase(AuStatus auStat, Phase phase) {
    log.debug2("enterPhase("+phase+")");
    auStat.setPhase(phase);
    Action action = phase.getEnterAction();
    if (action != null) {
      doAction(auStat, action);
    }
  }

  void exitPhase(AuStatus auStat) {
    Phase phase = auStat.getPhase();
    log.debug2("exitPhase("+phase+")");
    if (phase == null) {
      return;
    }
    Phase next = phase.getNextPhase();
    if (auStat.hasStarted(phase)) {
      auStat.stop(phase);
    }
    if (next != null) {
      enterPhase(auStat, next);
    }
  }

  void doAction(AuStatus auStat, Action action) {
    ArchivalUnit au = auStat.getAu();
    switch (action) {
    case EnqCopy:
      enqueueCopyAuContent(auStat);
      break;
    case EnqVerify:
      if (isVerifyContent) {
        enqueueVerifyAuContent(auStat);
      } else {
        exitPhase(auStat);
      }
      break;
    case EnqState:
      enqueueTask(MigrationTask.copyAuState(this, au), auStat, miscExecutor);
      break;
    case EnqIndex:
      if (auStat.isBulk()) {
        enqueueTask(MigrationTask.finishAuBulk(this, auStat.getAu()), auStat, indexExecutor);
      } else {
        exitPhase(auStat);
      }
      break;
    case FinishAu:
      MigrationTask finishTask = MigrationTask.finishAu(this, auStat.getAu());
      log.debug2("Finishing AU: " + auStat.getAuName());
      removeActiveAu(au);
      addFinishedAu(au, auStat);
      updateReport(au, auStat);
      totalAusMoved++;
      if (checkMissingContent && existsInV2(auStat.getAuId())) {
        totalAusPartiallyMoved++;
      }
      auStat.endPhase();
      ausLatch.countDown();
      break;
    }
  }

  boolean isEnqueueEnterAction(AuStatus auStat, Action action) {
    if (action == null) {
      return false;
    }
    switch (action) {
    case EnqVerify:
    case EnqIndex:
      return true;
    default:
      return false;
    }
  }


  void enqueueTask(MigrationTask task, AuStatus auStat,
                   ThreadPoolExecutor executor) {
    if (terminated) {
      return;
    }
    Phase phase = task.getTaskPhase();
    log.debug2("enqueueTask "+task.getType() + ", phase: " + phase);
    CountUpDownLatch latch = null;
    if (phase != null) {
      if (auStat.getLatch(phase) == null) {
        latch = new CountUpDownLatch(1, phase.toString());
        auStat.setLatch(phase, latch);
      } else {
        latch = auStat.getLatch(phase);
      }
    }
    task.setAuStatus(auStat);
    task.setCounters(auStat.getCounters());
    task.setCountDownLatch(latch);
    executor.execute(new TaskRunner(this, task));
  }

  /**
   * Move one V1 Au including all cachedUrls and all versions.
   *
   * @param au au The ArchivalUnit to move
   */
  void enqueueCopyAuContent(AuStatus auStat) {
    CountUpDownLatch latch = new CountUpDownLatch(1, "Copy");
    auStat.setLatch(Phase.COPY, latch);
    ArchivalUnit au = auStat.getAu();
    // Queue copies for all CUs in the v1 repo.
    for (CachedUrl cu : au.getAuCachedUrlSet().getCuIterable()) {
      MigrationTask task = MigrationTask.copyCuVersions(this, au, cu)
        .setCounters(auStat.getCounters())
        .setAuStatus(auStat)
        .setCountDownLatch(latch);
      latch.countUp();
      copyExecutor.execute(new TaskRunner(this, task));
    }
    latch.countDown();
  }

  /**
   * Check the Artifacts moved to the V2 repository for errors
   * @param au the ArchivalUnit which needs to be checked.
   */
  void enqueueVerifyAuContent(AuStatus auStat) {
    CountUpDownLatch latch = new CountUpDownLatch(1, "Verify");
    auStat.setLatch(Phase.VERIFY, latch);
    ArchivalUnit au = auStat.getAu();
    // Queue compares for all CUs in the v1 repo.
    for (CachedUrl cu : au.getAuCachedUrlSet().getCuIterable()) {
      MigrationTask task = MigrationTask.checkCuVersions(this, auStat.getAu(), cu)
        .setAuStatus(auStat)
        .setCounters(auStat.getCounters())
        .setCountDownLatch(latch);
      latch.countUp();
      verifyExecutor.execute(new TaskRunner(this, task));
    }
    latch.countDown();
  }

  void enqueueFinishAll() {
    MigrationTask task = MigrationTask.finishAll(this);
    miscExecutor.execute(new TaskRunner(this, task));
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
      setThreadName("V2AuMover: " + task.getType().toString());
      AuStatus auStat = task.getAuStatus();
      if (auStat != null) {
        log.debug3("Running " + task.getType() + " for " + auStat.getAu());
      }
      Exception taskException = null;
      Phase phase = task.getTaskPhase();
      // Set phase start time the first time a task for that phase starts
      if (phase != null && auStat != null) {
        if (auStat != null && !auStat.hasStarted(phase)) {
          auStat.start(phase);
        }
      }
      try {
        switch (task.getType()) {
        case COPY_CU_VERSIONS:
          log.debug2("Moving CU: " + task.getCu());
          long startC = now();
          CuMover mover = new CuMover(v2Mover, task);
          mover.run();
          task.getCounters().add(CounterType.COPY_TIME, now() - startC);

          log.debug2("Moved CU: " + task.getCu());
          break;
        case CHECK_CU_VERSIONS:
          log.debug2("Checking CU: " + task.getCu());
          long startCh = now();
          CuChecker checker = new CuChecker(v2Mover, task);
          checker.run();
          task.getCounters().add(CounterType.VERIFY_TIME, now() - startCh);
          log.debug2("Checked CU: " + task.getCu());
          break;
        case FINISH_AU_BULK:
          // finish the bulk store, copy all Artifact entries to
          // permanent ArtifactIndex
          try {
            long finishStart = now();
            finishBulk(collection, auStat.getAuId());

            log.debug2("finishBulk took " +
                       StringUtil.timeIntervalToString(now() - finishStart) +
                       ", " + auStat.getAuName());

          } catch (UnsupportedOperationException e) {
            log.warning("finishBulk() not supported");
          }
          break;
        case COPY_AU_STATE:
          log.debug2("Moving AU state: " + auStat.getAuName());
          long startS = now();
          AuStateMover stateMover = new AuStateMover(v2Mover, task);
          stateMover.run();
          log.debug2("Moved AU state: " + auStat.getAuName());
          log.debug2("Checking AU state: " + auStat.getAuName());
          AuStateChecker asChecker = new AuStateChecker(v2Mover, task);
          asChecker.run();
          task.getCounters().add(CounterType.STATE_TIME, now() - startS);
          log.debug2("Checked AU state: " + auStat.getAuName());
          break;
        case CHECK_AU_STATE:
          throw new UnsupportedOperationException("Not set up to invoke CHECK_AU_STATE separately");
        case FINISH_AU:
          try {
            ArchivalUnit au = auStat.getAu();
            log.debug2("Finishing AU: " + auStat.getAuName());
            removeActiveAu(au);
            auStat.setPhase(Phase.DONE);
            addFinishedAu(au, auStat);
            updateReport(au, auStat);
            totalAusMoved++;
            if (checkMissingContent && existsInV2(auStat.getAuId())) {
              totalAusPartiallyMoved++;
            }
          } finally {
            ausLatch.countDown();
          }
          break;
        case FINISH_ALL:
          log.debug2("FINISH_ALL: wait");
          ausLatch.await();
          totalTimers.stop(Phase.TOTAL);
          closeReport();
          doneSem.fill();
          break;
        default:
          log.error("Unknown migration task type: " + task.getType());
        }
      } catch (Exception e) {
        log.error("Task failed: " + task, e);
        taskException = e;
      } finally {
        task.countDown();
        task.complete(taskException);
        setThreadName("V2AuMover idle");
      }
    }
  }

  // Here mostly to make stack traces easier to read
  void startBulk(String collection, String auid) throws ApiException {
    repoCollectionsApiClient.handleBulkAuOp(collection, auid, "start");
  }

  void finishBulk(String collection, String auid) throws ApiException {
    repoCollectionsApiClient.handleBulkAuOp(collection, auid, "finish");
  }

  boolean existsInV2(String auid) {
    return v2Aus.contains(auid);
  }

  boolean existsInV2(ArchivalUnit au ) {
    return v2Aus.contains(au.getAuId());
  }

  public String getV2Url(ArchivalUnit au, CachedUrl cu) {
    String v1Url = cu.getUrl();
    String v2Url = null;
    try {
//       v2Url = UrlUtil.normalizeUrl(cu.getProperties().getProperty(CachedUrl.PROPERTY_NODE_URL),
//           au);
      v2Url = cu.getProperties().getProperty(CachedUrl.PROPERTY_NODE_URL);
      if (v2Url == null) {
        v2Url = v1Url;
      }
    }
    catch (Exception ex) {
      log.warning("Unable to normalize V1 uri: " + v1Url, ex);
    }
    if (v2Url == null) {
      v2Url = v1Url;
    }
    if(log.isDebug3())
      log.debug3("v1 url=" + v1Url +"  v2 url= " + v2Url);
    return v2Url;
  }

  /**
   * Check V2 service status for ready
   *
   * @return true if status for repository  is ready, false if either are not ready
   * @throws IOException if server is unable to return status.
   */
  void checkV2ServicesAvailable() throws IOException {
    try {
      log.info("Checking V2 Repository Status");
      if (!repoStatusApiClient.getStatus().getReady()) {
        throw new IOException("V2 Repository Service is not ready");
      }
      log.info("Checking V2 Configuration Status");
      if (!cfgStatusApiClient.getStatus().getReady()) {
        throw new IOException("V2 Configuration Service is not ready");
      }
    } catch (Exception e) {
      throw new ServiceUnavailableException("Couldn't fetch service status", e);
    }
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
      int errCode = 0;
      String errMsg = "";
      String msgPrefix = "Exceeded retries: ";
      while ((response == null || (!response.isSuccessful()) && tryCount < maxRetryCount)) {
        tryCount++;
        try {
          response = chain.proceed(request);
          if (response.isSuccessful()) {
            return response;
          }
          else {
            errCode = response.code();
            errMsg=response.message();
            logErrorBody(response);
            if (isNonRetryableResponse(errCode)) {
              // no retries
              msgPrefix = "Unretryable error: ";
              break;
            }
            // close response before retry
            response.close();
          }
        }
        catch (final IOException ioe) {
          if (tryCount < maxRetryCount) {
            if (log.isDebug2()) {
              log.debug2("Retrying", ioe);
            } else {
              log.debug("Retrying: " + ioe);
            }
            if (response != null) {
              // close response before retry
              response.close();
            }
          }
          else {
            if (log.isDebug2()) {
              // already logged the stack trace
              log.debug2("Exceeded retries - exiting: " + ioe);
            } else {
              // log stack trace when give up.
              log.debug("Exceeded retries - exiting", ioe);
            }
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
      //We've run out of retries and it is not IOException.
      String msg = msgPrefix + errCode + ": " + errMsg;
      log.debug(msg);
      response.close();
      if (errCode == 501) {
        throw new UnsupportedOperationException(msg);
      }
      throw new IOException(msg);
    }

    private boolean isNonRetryableResponse(int errCode) {
      return (errCode == 401 || errCode == 403 || errCode == 501);
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

  // Statistics and status keeping and reporting

  private final File reportFile;

  private String currentStatus;
  private boolean running = true; // init true avoids race while starting

  private PrintWriter reportWriter;

  private long totalAusToMove = 0;
  private long totalAusMoved = 0;
  private long totalAusPartiallyMoved = 0; // also included in totalAusMoved
  private long totalAusSkipped = 0;
  // XXX s.b. incremented by tasks that get errors, isn't
  private long totalAusWithErrors = 0;
  private long totalRunTime = 0;
  private Counters totalCounters = new Counters();
  private OpTimers totalTimers = new OpTimers();

  private Map<String,AuStatus> activeAus = new LinkedHashMap<>();
  private Map<String,AuStatus> finishedAus = new LinkedHashMap<>();

  private final List<String> errorList = new ArrayList<>();

  enum CounterType {
    URLS_MOVED,
    URLS_SKIPPED,
    ARTIFACTS_MOVED,
    ARTIFACTS_SKIPPED,
    BYTES_MOVED,
    CONTENT_BYTES_MOVED,
    URLS_VERIFIED,
    ARTIFACTS_VERIFIED,
    BYTES_VERIFIED,
    CONTENT_BYTES_VERIFIED,
    COPY_TIME,
    VERIFY_TIME,
    STATE_TIME
  }

  public static class Counters {
    Map<CounterType,Counter> counters = new HashMap<>();
    private long errorCount = 0;
    private List<String> errors = new ArrayList<>();
    private Counters parent;

    public Counters() {
      for (CounterType type : CounterType.values()) {
        counters.put(type, new Counter());
      }
    }

    public Counters setParent(Counters parent) {
      this.parent = parent;
      return this;
    }

    public synchronized Counter get(CounterType type) {
      return counters.get(type);
    }

    public long getVal(CounterType type) {
      return counters.get(type).getVal();
    }

    public boolean isNonZero(CounterType type) {
      return counters.get(type).getVal() > 0;
    }

    public void incr(CounterType type) {
      counters.get(type).incr();
      if (parent != null) {
        parent.incr(type);
      }
    }

    public void add(CounterType type, long val) {
      counters.get(type).add(val);
      if (parent != null) {
        parent.add(type, val);
      }
    }

    public void addError(String msg) {
      errors.add(msg);
      if (parent != null) {
        parent.addError(msg);
      }
    }

    public List<String> getErrors() {
      return errors;
    }

    public synchronized void add(Counters ctrs) {
      for (CounterType type : CounterType.values()) {
        get(type).add(ctrs.get(type));
      }
      errors.addAll(ctrs.errors);
    }
  }

  public static class OpTimers {
    protected Map<Phase,StopWatch> timerMap = new HashMap<>();
    protected Map<Phase,CountUpDownLatch> latchMap = new HashMap<>();
    protected Counters ctrs;
    String status;

    public OpTimers() {
      ctrs = new Counters();
      status = "Initializing";
    }

    public void start(Phase phase) {
      timerMap.put(phase, StopWatch.createStarted());
    }

    public void stop(Phase phase) {
      if (!timerMap.containsKey(phase)) {
        throw new IllegalStateException("No " + phase + " timer, can't stop");
      }
      timerMap.get(phase).stop();
    }

    public void suspend(Phase phase) {
      if (!timerMap.containsKey(phase)) {
        throw new IllegalStateException("No " + phase + " timer, can't suspend");
      }
      timerMap.get(phase).suspend();
    }

    public void resume(Phase phase) {
      if (!timerMap.containsKey(phase)) {
        throw new IllegalStateException("No " + phase + " timer, can't resume");
      }
      timerMap.get(phase).resume();
    }

    public boolean hasStarted(Phase phase) {
      return timerMap.containsKey(phase) && getStartTime(phase) > 0;
    }

    public long getStartTime(Phase phase) {
      if (!timerMap.containsKey(phase)) {
        throw new IllegalStateException("No " + phase + " timer, can't get start time");
      }
      return timerMap.get(phase).getStartTime();
    }

    public long getStopTime(Phase phase) {
      if (!timerMap.containsKey(phase)) {
        throw new IllegalStateException("No " + phase + " timer, can't get stop time");
      }
      return timerMap.get(phase).getStopTime();
    }

    public long getElapsedTime(Phase phase) {
      if (!timerMap.containsKey(phase)) {
        throw new IllegalStateException("No " + phase + " timer, can't get elapsed time");
      }
      return timerMap.get(phase).getTime();
    }

    public void setLatch(Phase phase, CountUpDownLatch latch) {
      latchMap.put(phase, latch);
    }

    public CountUpDownLatch getLatch(Phase phase) {
      return latchMap.get(phase);
    }

    public String getStatus() {
      return status;
    }

    public void setStatus(String val) {
      status = val;
    }

  }

  /** Status and counters for a single AU in progress */
  public class AuStatus extends OpTimers {

    ArchivalUnit au;
    String auid;
    String auname;
    boolean isBulk;
    Phase auPhase = Phase.START;
    Runnable atPhaseEnd;
    Map<Phase,Runnable> endActions = new HashMap<>();

    public AuStatus(ArchivalUnit au) {
      super();
      this.au = au;
      auid = au.getAuId();
      auname = au.getName();
    }

    public ArchivalUnit getAu() {
      return au;
    }

    public String getAuName() {
      return auname;
    }

    public String getAuId() {
      return auid;
    }

    public Counters getCounters() {
      return ctrs;
    }

    public boolean isBulk() {
      return isBulk;
    }

    public AuStatus setIsBulk(boolean isBulk) {
      this.isBulk = isBulk;
      return this;
    }

    public Phase getPhase() {
      return auPhase;
    }

    public AuStatus setPhase(Phase phase) {
      auPhase = phase;
      return this;
    }

    public void endPhase() {
      exitPhase(this);
    }

    public List<String> getErrors() {
      return ctrs.getErrors();
    }

    public int getErrorCount() {
      return ctrs.getErrors().size();
    }

    public void addError(String msg) {
      ctrs.addError(msg);
    }

    public int hashCode() {
      return 33 * auid.hashCode();
    }

    public boolean equals(Object obj) {
      if (obj == null) { return false; }
      if (obj == this) { return true; }
      if (obj.getClass() != getClass()) {
        return false;
      }
      return auid.equals(((AuStatus)obj).auid);
    }
  }

  //
  // Executor specification and creation.
  //

  static class ExecSpec {
    int queueSize;
    int coreThreads;
    int maxThreads;
  }

  ExecSpec parseSpec(String spec) {
    return parseSpecInto(spec, new ExecSpec());
  }

  ExecSpec parseSpecInto(String spec, ExecSpec eSpec) {
    List<String> specList = StringUtil.breakAt(spec, ";", 3, false, true);
    switch (specList.size()) {
    case 3: eSpec.maxThreads = Integer.parseInt(specList.get(2));
    case 2: eSpec.coreThreads = Integer.parseInt(specList.get(1));
    case 1: eSpec.queueSize = Integer.parseInt(specList.get(0));
    }
    return eSpec;
  }


  ThreadPoolExecutor makeExecutorFromnSpec(String spec, String defaultSpec) {
    ExecSpec eSpec = parseSpec(defaultSpec);
    eSpec = parseSpecInto(spec, eSpec);
    return makeExecutor(eSpec.queueSize, threadTimeout,
                        eSpec.coreThreads, eSpec.maxThreads);
  }

  public String getExecutorStats(String name, ThreadPoolExecutor executor) {
    StringBuilder sb = new StringBuilder();
    sb.append(name);
    sb.append(" ");
    sb.append("Threads: ");
    sb.append(executor.getPoolSize());
    sb.append("/");
    sb.append(executor.getLargestPoolSize());
    sb.append(", ");
    sb.append(executor.getActiveCount());
    sb.append(" active, ");
    sb.append(executor.getTaskCount());
    sb.append(" tasks started");
    BlockingQueue queue = executor.getQueue();
    int size = queue.size();
    sb.append(", queue size: ");
    sb.append(size);
    sb.append("/");
    sb.append(size + queue.remainingCapacity());
    return sb.toString();
  }


  /**
   * Return a string describing the current progress, or completion state.
   */
  public String getCurrentStatus() {
    if (STATUS_COPYING.equals(currentStatus)) {
      StringBuilder sb = new StringBuilder();
      sb.append("Copied ");
      sb.append(totalAusMoved);
      sb.append(" of ");
      sb.append(totalAusToMove);
      sb.append(" AUs, ");
      addCounterStatus(sb, totalCounters, totalTimers, Phase.TOTAL);
      return sb.toString();
    }
    return currentStatus;
  }

  public List<String> getInstruments() {
    List<String> res = new ArrayList<>();
    res.add(getExecutorStats("Copy", copyExecutor));
    res.add(getExecutorStats("Index", indexExecutor));
    res.add(getExecutorStats("Verify", verifyExecutor));
    res.add(getExecutorStats("Misc", miscExecutor));
    res.add(getExecutorStats("Enqueue", enqueueExecutor));
    return res;
  }

  public List<String> getActiveStatusList() {
    Map<String,AuStatus> auStats;
    synchronized (activeAus) {
      auStats = new LinkedHashMap<>(activeAus);
    }
    List<String> res = new ArrayList<>();
    for (AuStatus auStat : auStats.values()) {
      String one = getOneAuStatus(auStat);
      if (one != null) {
        res.add(one);
      }
    }
    return res;
  }

  public List<String> getFinishedStatusList() {
    Map<String,AuStatus> auStats;
    synchronized (finishedAus) {
      auStats = new LinkedHashMap<>(finishedAus);
    }
    List<String> res = new ArrayList<>();
    for (AuStatus auStat : auStats.values()) {
      String one = getOneAuStatus(auStat);
      if (one != null) {
        res.add(one);
      }
    }
    return res;
  }

  // XXX need to enhance to account for verify phase
  private String getOneAuStatus(AuStatus auStat) {
    Counters ctrs = auStat.getCounters();
    StringBuilder sb = new StringBuilder();
    Phase phase = auStat.getPhase();
    String phaseName = phase.toString();
    switch (auStat.getPhase()) {
    case DONE:
      break;
    case COPY:
    case VERIFY:
      if (!auStat.hasStarted(phase)) {
        phaseName = "Queued " + unGerund(phaseName);
      }
      break;
    }
    if (!StringUtil.isNullString(phaseName)) {
      sb.append(phaseName);
      sb.append(": ");
    }
    sb.append(auStat.getAuName());
    switch (auStat.getPhase()) {
    case START:
      break;
    default:
      addCounterStatus(sb, ctrs, auStat, Phase.COPY, ": ");
    }
    return sb.toString();
  }

  String unGerund(String gerund) {
    return gerund.replaceAll("ing", "");
  }

  private void addCounterStatus(StringBuilder sb, Counters ctrs,
                                OpTimers auStat, Phase phase) {
    addCounterStatus(sb, ctrs, auStat, phase, null);
  }

  private void addCounterStatus(StringBuilder sb, Counters ctrs,
                                OpTimers auStat, Phase phase,
                                String separator) {
    // No stats if not started yet
    if (!auStat.hasStarted(phase)) {
      return;
    }
    if (separator != null) {
      sb.append(separator);
    }
    sb.append(StringUtil.bigNumberOfUnits(ctrs.getVal(CounterType.URLS_MOVED),
                                          "URL"));
    if (ctrs.isNonZero(CounterType.URLS_SKIPPED)) {
      sb.append(" (");
      sb.append(bigIntFmt.format(ctrs.getVal(CounterType.URLS_SKIPPED)));
      sb.append(" skipped)");
    }
    sb.append(", ");
    sb.append(StringUtil.bigNumberOfUnits(ctrs.getVal(CounterType.ARTIFACTS_MOVED),
                                          "version"));
    if (ctrs.isNonZero(CounterType.ARTIFACTS_SKIPPED)) {
      sb.append(" (");
      sb.append(bigIntFmt.format(ctrs.getVal(CounterType.ARTIFACTS_SKIPPED)));
      sb.append(" skipped)");
    }
    sb.append(", ");
    sb.append(StringUtil.bigNumberOfUnits(ctrs.getVal(CounterType.BYTES_MOVED),
                                          "byte"));
    sb.append(", in ");
    sb.append(StringUtil.timeIntervalToString(auStat.getElapsedTime(phase)));
    if (ctrs.getVal(CounterType.BYTES_MOVED) > 0) {
      sb.append(", at ");
      sb.append(StringUtil.byteRateToString(ctrs.getVal(CounterType.BYTES_MOVED),
                                            auStat.getElapsedTime(phase)));

    }
    if (isDetailedStats
        && ctrs.getVal(CounterType.COPY_TIME) > 0
//         && ctrs.getVal(CounterType.VERIFY_TIME) > 0
        ) {
      double c = ctrs.getVal(CounterType.COPY_TIME);
      double v = ctrs.getVal(CounterType.VERIFY_TIME);
      double s = ctrs.getVal(CounterType.STATE_TIME);
      double t = c + v + s;
      double cp = Math.round(100*c/t);
      double vp = Math.round(100*v/t);
      double sp = Math.round(100*s/t);
      sb.append(" (");
      if (cp != 0.0) {
        sb.append(percentFmt.format(cp));
        sb.append("%");
        if (ctrs.getVal(CounterType.BYTES_MOVED) > 0) {
          sb.append(", ");
          sb.append(StringUtil
                    .byteRateToString(ctrs.getVal(CounterType.BYTES_MOVED),
                                      (long)(auStat.getElapsedTime(phase)
                                             * (c / t))));
        }
        sb.append(" copy");
      }
      if (vp != 0.0) {
        if (cp != 0.0) {
          sb.append("; ");
        }
        sb.append(percentFmt.format(vp));
        sb.append("%");
        if (ctrs.getVal(CounterType.BYTES_VERIFIED) > 0) {
          sb.append(", ");
          sb.append(StringUtil
                    .byteRateToString(ctrs.getVal(CounterType.BYTES_VERIFIED),
                                      (long)(auStat.getElapsedTime(phase)
                                             * (v / t))));
        }
        sb.append(" verify");
      }
      if (sp != 0.0) {
        if (cp + vp != 0.0) {
          sb.append(", ");
        }
        sb.append(percentFmt.format(sp));
        sb.append("% state");
      }
      sb.append(")");
    }
  }

  public boolean isRunning() {
    return running;
  }

  public boolean isTerminated() {
    return terminated;
  }

  /**
   * Returns the list of errors which occurred while attempting to move the AU(s).
   *
   * @return the list of error strings
   */
  public List<String> getErrors() {
    return totalCounters.getErrors();
  }

  private void addActiveAu(ArchivalUnit au, AuStatus austat) {
    synchronized (activeAus) {
      activeAus.put(au.getAuId(), austat);
    }
  }

  private void removeActiveAu(ArchivalUnit au) {
    synchronized (activeAus) {
      activeAus.remove(au.getAuId());
    }
  }

  private void addFinishedAu(ArchivalUnit au, AuStatus austat) {
    synchronized (finishedAus) {
      finishedAus.put(au.getAuId(), austat);
    }
  }

  public AuStatus getAuStatus(ArchivalUnit au) {
    synchronized (activeAus) {
      if (activeAus.containsKey(au.getAuId())) {
        return activeAus.get(au.getAuId());
      }
    }
    synchronized (finishedAus) {
      if (finishedAus.containsKey(au.getAuId())) {
        return finishedAus.get(au.getAuId());
      }
    }
    return null;
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
   * Update the report for the current Au.
   * @param au The ArchivalUnit which is being counted
   * @param auStat The AuStatus for this ArchivalUnit.
   */
  void updateReport(ArchivalUnit au, AuStatus auStat) {
    if (reportWriter == null) {
      log.error("updateReport called when no reportWriter",
                new Throwable());
      return;
    }
    AuStatus austat = getAuStatus(au);
    if (austat == null) {
      log.error("updateReport called with AU that's not running: " +
                au.getName(),
                new Throwable());
      reportWriter.println("updateReport called with AU that's not running: " +
                           au.getName());
      return;
    }
    StringBuilder sb = new StringBuilder();
    addCounterStatus(sb, austat.getCounters(), austat, Phase.COPY);
    sb.append(", ");
    sb.append(StringUtil.bigNumberOfUnits(auStat.getErrorCount(), "error"));
    String auData = sb.toString();
    reportWriter.println("AU Name: " + auStat.getAuName());
    reportWriter.println("AU ID: " + auStat.getAuId());
    if (terminated) {
      reportWriter.println("Move terminated with error.");
    }
    reportWriter.println(auData);
//       else {
//         if (isPartialContent) {
//           if (auArtifactsMoved > 0) {// if we moved something
//             reportWriter.println("Moved remaining unmigrated au content.");
//             reportWriter.println(auData);
//           }
//           else {
//             reportWriter.println("All au content already migrated.");
//             reportWriter.println(auData);
//           }
//         }
//         else {
//           reportWriter.println(auData);
//         }
//       }
    if (!auStat.getErrors().isEmpty()) {
      for (String err : auStat.getErrors()) {
        reportWriter.println(" " + err);
      }
    }
    reportWriter.println();
    if (reportWriter.checkError()) {
      log.warning("Error writing report file.");
    }
  }

  /**
   * Close the report before exiting
   */
  void closeReport() {
    StringBuilder sb = new StringBuilder();
    sb.append(StringUtil.bigNumberOfUnits(totalAusMoved - totalAusSkipped, "AU") + " copied");
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
        sb.append(" partially");
      }
      sb.append(")");
    }
    addCounterStatus(sb, totalCounters, totalTimers, Phase.TOTAL, ": ");
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

  /* ------------------
  testing getters & setters
 */
  void setAuCounters(long urls, long artifacts, long bytes, long contentBytes, long runTime,
      long errors,
      List<String> errs) {
//     auUrlsMoved = urls;
//     auArtifactsMoved = artifacts;
//     auBytesMoved = bytes;
//     auContentBytesMoved = contentBytes;
//     auRunTime = runTime;
//     auErrorCount = errors;
//     auErrors = errs;
  }

  void setTotalCounters(long aus, long urls, long artifacts, long bytes, long contentBytes,
      long runTime, long errors) {
//     totalAusMoved = aus;
//     totalUrlsMoved = urls;
//     totalArtifactsMoved = artifacts;
//     totalBytesMoved = bytes;
//     totalContentBytesMoved = contentBytes;
//     totalRunTime = runTime;
//     totalErrorCount = errors;
  }

  LinkedHashSet<ArchivalUnit> getAuMoveQueue() {
    return auMoveQueue;
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
      StringBuilder sb = new StringBuilder();
      sb.append("Error response returned: ").append(response.code())
        .append(": ").append(response.message());
      sb.append(", Headers: ");
      response.headers().forEach(header -> sb.append(header.getFirst()).append(":").append(header.getSecond()));
      ResponseBody body = response.body();
      if (body != null) {
        // XXX Need to parse a json, extract various fields
        sb.append(", body: ").append(body.string());
      }
      log.warning(sb.toString(), new Throwable());
    }
    catch (Exception e) {
      log.error("Exception trying to retrieve error response body", e);
    }
  }

  static class ServiceUnavailableException extends IOException {
    public ServiceUnavailableException(String msg) {
      super(msg);
    }
    public ServiceUnavailableException(String msg, Throwable t) {
      super(msg);
      initCause(t);
    }
    public ServiceUnavailableException(Throwable t) {
      super();
      initCause(t);
    }
  }


  /*
   * Utilities
   */

  static long now() {
    return System.currentTimeMillis();
  }

  /*
   * Regexp utilities
   */

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

}
