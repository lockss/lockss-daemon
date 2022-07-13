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
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import okhttp3.*;
import org.lockss.app.LockssDaemon;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.LockssRunnable;
import org.lockss.laaws.MigrationManager.OpType;
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
import static org.lockss.laaws.Counters.CounterType;

public class V2AuMover {
  private static final Logger log = Logger.getLogger(V2AuMover.class);

  //////////////////////////////////////////////////////////////////////
  // Config params
  //////////////////////////////////////////////////////////////////////

  static final String PREFIX = MigrationManager.PREFIX;

  /**
   * User agent that the migrator will use when connecting to V2 services
   */
  public static final String PARAM_V2_USER_AGENT = PREFIX + "user_agent";
  public static final String DEFAULT_V2_USER_AGENT = "lockss";

  /**
   * Enable debugging for the configuration service network endpoints.
   */
  public static final String DEBUG_CONFIG_REQUEST = PREFIX + "cfg.debug";
  public static final boolean DEFAULT_DEBUG_CONFIG_REQUEST = false;

  /**
   * Enable debugging for the repository service network endpoints.
   */
  public static final String DEBUG_REPO_REQUEST = PREFIX + "repo.debug";
  public static final boolean DEFAULT_DEBUG_REPO_REQUEST = false;

  public static final String EXEC_PREFIX = PREFIX + "executor.";

  /**
   * Executor Spec:
   * <tt><i>queue-max</i>;<i>thread-max</i></tt> or
   * <tt><i>queue-max</i>;<i>core-threads</i>;<i>max-threads</i></tt>

   */
  public static final String PARAM_EXECUTOR_SPEC = EXEC_PREFIX + "<name>.spec";

  /**
   * Copy task Executor.  Queue should be large to reduce waiting for
   * bursty CU iterator.
   */
  public static final String PARAM_COPY_EXECUTOR_SPEC =
    EXEC_PREFIX + "copy.spec";
  public static final String DEFAULT_COPY_EXECUTOR_SPEC = "1000;10";

  /**
   * Verify task Executor.  Queue should be large to reduce waiting
   * for bursty CU iterator.
   */
  public static final String PARAM_VERIFY_EXECUTOR_SPEC =
    EXEC_PREFIX + "verify.spec";
  public static final String DEFAULT_VERIFY_EXECUTOR_SPEC = "1000;10";

  /**
   * Copy CU iterators run in this Executor.  Controls the number of
   * AUs running iterators
   */
  public static final String PARAM_COPY_ITER_EXECUTOR_SPEC =
    EXEC_PREFIX + "copyIter.spec";
  public static final String DEFAULT_COPY_ITER_EXECUTOR_SPEC = "10;2";

  /**
   * Verify CU iterators run in this Executor.  Controls the number of
   * AUs running iterators
   */
  public static final String PARAM_VERIFY_ITER_EXECUTOR_SPEC =
    EXEC_PREFIX + "verifyIter.spec";
  public static final String DEFAULT_VERIFY_ITER_EXECUTOR_SPEC = "10;2";

  /**
   * Index Executor.  Controls max simulataneous finishBulk) operations
   */
  public static final String PARAM_INDEX_EXECUTOR_SPEC =
    EXEC_PREFIX + "index.spec";
  public static final String DEFAULT_INDEX_EXECUTOR_SPEC = "50;5";

  /**
   * Misc Executor, runs AU State copy & verify, finishall.
   */
  public static final String PARAM_MISC_EXECUTOR_SPEC =
    EXEC_PREFIX + "misc.spec";
  public static final String DEFAULT_MISC_EXECUTOR_SPEC = "50;10";

  /**
   * Executor thread timeout
   */
  public static final String PARAM_THREAD_TIMEOUT = PREFIX + "thread.timeout";
  public static final long DEFAULT_THREAD_TIMEOUT = 30 * Constants.SECOND;

  /**
   * V2 collection to migrate into
   */
  public static final String PARAM_V2_COLLECTION = PREFIX + "collection";
  public static final String DEFAULT_V2_COLLECTION = "lockss";

  /**
   * Repository service port
   */
  public static final String PARAM_RS_PORT = PREFIX + "rs.port";
  public static final int DEFAULT_RS_PORT = 24610;

  /**
   * Configuration service port
   */
  public static final String PARAM_CFG_PORT = PREFIX + "cfg.port";
  public static final int DEFAULT_CFG_PORT = 24620;

  /**
   * Maximum number of retries for REST request failures
   */
  public static final String PARAM_MAX_RETRY_COUNT = PREFIX + "max.retries";
  public static final int DEFAULT_MAX_RETRY_COUNT = 4;

  /**
   * Backoff between REST request retries
   */
  public static final String PARAM_RETRY_BACKOFF_DELAY = PREFIX + "retry_backoff";
  public static final long DEFAULT_RETRY_BACKOFF_DELAY = 10 * Constants.SECOND;

  /**
   * Connection timeout
   */
  public static final String PARAM_CONNECTION_TIMEOUT =
    PREFIX + "connection.timeout";
  public static final long DEFAULT_CONNECTION_TIMEOUT = 30 * Constants.SECOND;

  /**
   * Read/write timeout
   */
  public static final String PARAM_READ_TIMEOUT = PREFIX + "read.timeout";
  public static final long DEFAULT_READ_TIMEOUT =  1 * Constants.HOUR;

  /**
   * Read/write timeout for long-running REST call, such as
   * finishBulk()
   */
  public static final String PARAM_LONG_READ_TIMEOUT =
    PREFIX + "read.timeout.long";
  public static final long DEFAULT_LONG_READ_TIMEOUT =  12 * Constants.HOUR;

  /**
   * Path to directory holding daemon logs
   */
  public static final String PARAM_REPORT_DIR =
      ConfigManager.PARAM_PLATFORM_LOG_DIR;
  public static final String DEFAULT_REPORT_DIR = "/tmp";

  /**
   * Migration report file name
   */
  public static final String PARAM_REPORT_FILE = PREFIX + "report.file";
  public static final String DEFAULT_REPORT_FILE = "v2migration.txt";

  /**
   * Error report file name
   */
  public static final String PARAM_ERROR_REPORT_FILE =
    PREFIX + "errorReport.file";
  public static final String DEFAULT_ERROR_REPORT_FILE = "v2migration.err";

  /**
   * If true, partial copies will be done for AUs some of whose
   * content already exists in the V2 repo.  If false, AUs having any
   * content in V2 will be skipped.
   */
  public static final String PARAM_CHECK_MISSING_CONTENT =
    PREFIX + "check.missing.content";
  public static final boolean DEFAULT_CHECK_MISSING_CONTENT = true;

  /**
   * If true, the content will be verified after copying
   */
  public static final String PARAM_VERIFY_CONTENT = PREFIX + "verify.content";
  public static final boolean DEFAULT_VERIFY_CONTENT = false;

  /**
   * If true, phase-specific timings will be included with the stats
   */
  public static final String PARAM_DETAILED_STATS = PREFIX + "detailedStats";
  public static final boolean DEFAULT_DETAILED_STATS = true;

  //////////////////////////////////////////////////////////////////////
  // Constants
  //////////////////////////////////////////////////////////////////////

  /** Flag to getCurrentStatus() to build status string on the fly. */
  private static final String STATUS_RUNNING = "**Running**";

  public static final ThreadLocal<NumberFormat> TH_BIGINT_FMT =
    new ThreadLocal<NumberFormat>() {
      @Override protected NumberFormat initialValue() {
        return NumberFormat.getInstance();
      }};
  public static final ThreadLocal<DecimalFormat> TH_PERCENT_FMT =
    new ThreadLocal<DecimalFormat>() {
      @Override protected DecimalFormat initialValue() {
        return new DecimalFormat("0");
      }};

  //////////////////////////////////////////////////////////////////////
  // Fields
  //////////////////////////////////////////////////////////////////////

  // Set from config and/or request arguments

  /** V2 Repository Client */
  private V2RestClient repoClient;
  /** V2 Repository Client with very long timeout */
  private V2RestClient repoLongCallClient;

  /** V2 repo REST status api client */
  private org.lockss.laaws.api.rs.StatusApi repoStatusApiClient;

  /** V2 repo REST collections api client */
  private StreamingCollectionsApi repoCollectionsApiClient;
  /** V2 repo REST collections api client with long timeout */
  private StreamingCollectionsApi repoCollectionsApiLongCallClient;

  /** Repository service port */
  private int repoPort;

  /** V2 repo REST access URL */
  private String repoAccessUrl = null;

  /** V2 configuration client (config and state) */
  private V2RestClient configClient;

  /** V2 cfgsvc REST status api client */
  private org.lockss.laaws.api.cfg.StatusApi cfgStatusApiClient;

  /** V2 State api client */
  private AusApi cfgAusApiClient;

  /** Configuration service port */
  private int cfgPort;

  /** V2 cfgsvc REST access URL */
  private String cfgAccessUrl = null;

  /** V2 host name */
  private String hostName;

  /** User Agent */
  private String userAgent;

  /** User name used to access v2 service */
  private String userName;

  /** Password used to access v2 service */
  private String userPass;

  /** V2 Collection */
  private String collection;

  private OpType opType;

  private boolean isPartialContent = false;
  private boolean checkMissingContent;
  private boolean isCompareBytes;
  private boolean isDetailedStats;

  // Retries and timeouts
  /** the time to wait fora a connection before timing out */
  private long connectTimeout;
  /** the time to wait for read/write before timing out */
  private long readTimeout;
  /** the time to wait for long operations (index) before timing out */
  private long longReadTimeout;

  /** Max number of times to retry requests (after retryable failures) */
  private int maxRetryCount;
  /** Delay for initial retry, multipied by retry count for each
   * successive retry. */
  private long retryBackoffDelay;

  // debug support
  private boolean debugRepoReq;
  private boolean debugConfigReq;

  // Thread pool for each activity, each with its own queue.
  private ThreadPoolExecutor copyIterExecutor;
  private ThreadPoolExecutor verifyIterExecutor;
  private ThreadPoolExecutor copyExecutor;
  private ThreadPoolExecutor verifyExecutor;
  private ThreadPoolExecutor miscExecutor;
  private ThreadPoolExecutor indexExecutor;

  //////////////////////////////////////////////////////////////////////
  // State vars
  //////////////////////////////////////////////////////////////////////

  /** All AUIDs known to V2 repo at start of execution */
  private final ArrayList<String> v2Aus = new ArrayList<>();
  private final LinkedHashSet<ArchivalUnit> auMoveQueue = new LinkedHashSet<>();

  /** Counts down to zero when all AUs in request are finished */
  private CountUpDownLatch ausLatch;
  /** Filled when request completely done and report written */
  private OneShotSemaphore doneSem = new OneShotSemaphore();

  private boolean terminated = false;
  private boolean globalAbort = false;

  PluginManager pluginManager;

  //////////////////////////////////////////////////////////////////////
  // Constructor, config, init
  //////////////////////////////////////////////////////////////////////

  public V2AuMover() {
    pluginManager = LockssDaemon.getLockssDaemon().getPluginManager();
    Configuration config = ConfigManager.getCurrentConfig();
    setInitialConfig(config);
    setConfig(config, null, config.differences(null));
  }

  /**
   * Process one-time config params, which take effect only at the
   * start of a request.
   */
  private void setInitialConfig(Configuration config) {
    userAgent = config.get(PARAM_V2_USER_AGENT, DEFAULT_V2_USER_AGENT);
    collection = config.get(PARAM_V2_COLLECTION, DEFAULT_V2_COLLECTION);
    cfgPort = config.getInt(PARAM_CFG_PORT, DEFAULT_CFG_PORT);
    repoPort = config.getInt(PARAM_RS_PORT, DEFAULT_RS_PORT);
    debugRepoReq = config.getBoolean(DEBUG_REPO_REQUEST,
                                     DEFAULT_DEBUG_REPO_REQUEST);
    debugConfigReq = config.getBoolean(DEBUG_CONFIG_REQUEST,
                                       DEFAULT_DEBUG_CONFIG_REQUEST);

    String logdir = config.get(PARAM_REPORT_DIR, DEFAULT_REPORT_DIR);
    String logfile = config.get(PARAM_REPORT_FILE, DEFAULT_REPORT_FILE);
    String errfile = config.get(PARAM_ERROR_REPORT_FILE,
                                DEFAULT_ERROR_REPORT_FILE);
    reportFile = new File(logdir, logfile);
    errorFile = new File(logdir, errfile);
  }

  /**
   * Set or update config params that can be changed on the fly.
   */
  public void setConfig(Configuration config, Configuration oldConfig,
			Configuration.Differences changedKeys) {
    if (changedKeys.contains(PREFIX)) {

      // XXX timeouts take effect only at request start.  Check
      // whether ok to call V2RestClient.setConnectTimeout(), etc. on
      // the fly - creates a new OkHttpClient
      connectTimeout = config.getTimeInterval(PARAM_CONNECTION_TIMEOUT,
                                              DEFAULT_CONNECTION_TIMEOUT);
      readTimeout = config.getTimeInterval(PARAM_READ_TIMEOUT,
                                           DEFAULT_READ_TIMEOUT);
      longReadTimeout = config.getTimeInterval(PARAM_LONG_READ_TIMEOUT,
                                               DEFAULT_LONG_READ_TIMEOUT);

      maxRetryCount = config.getInt(PARAM_MAX_RETRY_COUNT,
                                    DEFAULT_MAX_RETRY_COUNT);
      retryBackoffDelay = config.getLong(PARAM_RETRY_BACKOFF_DELAY,
                                         DEFAULT_RETRY_BACKOFF_DELAY);
      checkMissingContent = config.getBoolean(PARAM_CHECK_MISSING_CONTENT,
                                              DEFAULT_CHECK_MISSING_CONTENT);
      isDetailedStats=config.getBoolean(PARAM_DETAILED_STATS,
                                        DEFAULT_DETAILED_STATS);

      copyIterExecutor =
        createOrReConfigureExecutor(copyIterExecutor, config,
                                    PARAM_COPY_ITER_EXECUTOR_SPEC,
                                    DEFAULT_COPY_ITER_EXECUTOR_SPEC);
      verifyIterExecutor =
        createOrReConfigureExecutor(verifyIterExecutor, config,
                                    PARAM_VERIFY_ITER_EXECUTOR_SPEC,
                                    DEFAULT_VERIFY_ITER_EXECUTOR_SPEC);
      copyExecutor = createOrReConfigureExecutor(copyExecutor, config,
                                                 PARAM_COPY_EXECUTOR_SPEC,
                                                 DEFAULT_COPY_EXECUTOR_SPEC);
      verifyExecutor = createOrReConfigureExecutor(verifyExecutor, config,
                                                   PARAM_VERIFY_EXECUTOR_SPEC,
                                                   DEFAULT_VERIFY_EXECUTOR_SPEC);
      indexExecutor = createOrReConfigureExecutor(indexExecutor, config,
                                                  PARAM_INDEX_EXECUTOR_SPEC,
                                                  DEFAULT_INDEX_EXECUTOR_SPEC);
      miscExecutor = createOrReConfigureExecutor(miscExecutor, config,
                                                 PARAM_MISC_EXECUTOR_SPEC,
                                                 DEFAULT_MISC_EXECUTOR_SPEC);
    }
  }

  /**
   * Create the REST clients to access the remote repo & cfgsvc
   * specified in the args, and initialize the status.
   *
   * @param args the arguments for this request.
   * @throws IllegalArgumentException
   */
  void initRequest(Args args) throws IllegalArgumentException {

    currentStatus = "Initializing";
    running = true;
    hostName = args.host;
    userName = args.uname;
    userPass = args.upass;
    opType = args.opType;
    isCompareBytes = args.isCompareContent;

    if (StringUtil.isNullString(hostName)) {
      String msg = "Destination hostname must be supplied.";
      totalCounters.addError(msg);
      throw new IllegalArgumentException(msg);
    }
    if (userName == null || userPass == null) {
      String msg = "Missing user name or password.";
      totalCounters.addError(msg);
      throw new IllegalArgumentException(msg);
    }
    try {
      cfgAccessUrl = new URL("http", hostName, cfgPort, "").toString();
      if (cfgAccessUrl == null || UrlUtil.isMalformedUrl(cfgAccessUrl)) {
        totalCounters.addError("Missing or invalid configuration service url: " + cfgAccessUrl);
        throw new IllegalArgumentException(
            "Missing or invalid configuration service url: " + cfgAccessUrl);
      }
      configClient = makeV2RestClient();
      setTimeouts(configClient, "config", connectTimeout, readTimeout);
      setClientParams(configClient, userName, userPass, userAgent,
                      cfgAccessUrl, debugRepoReq);
      // Assign the client to the status api and aus api
      cfgStatusApiClient = new org.lockss.laaws.api.cfg.StatusApi(configClient);
      cfgAusApiClient = new AusApi(configClient);
    }
    catch (MalformedURLException mue) {
      totalCounters.addError("Error parsing REST Configuration Service URL: "
          + mue.getMessage());
      throw new IllegalArgumentException(
          "Missing or invalid configuration service hostName: " + hostName + " port: " + cfgPort);
    }

    try {
      repoAccessUrl = new URL("http", hostName, repoPort, "").toString();
      if (repoAccessUrl == null || UrlUtil.isMalformedUrl(repoAccessUrl)) {
        totalCounters.addError("Missing or invalid repository service url: " + repoAccessUrl);
        throw new IllegalArgumentException(
            "Missing or invalid configuration service url: " + repoAccessUrl);
      }
      // Create a new RepoClient
      repoClient = makeV2RestClient();
      setTimeouts(repoClient, "repo", connectTimeout, readTimeout);
      repoLongCallClient = makeV2RestClient(repoClient);
      setTimeouts(repoLongCallClient, "index", connectTimeout, longReadTimeout);
      setClientParams(repoClient, userName, userPass, userAgent,
                      repoAccessUrl, debugRepoReq);
      setClientParams(repoLongCallClient, userName, userPass, userAgent,
                      repoAccessUrl, debugRepoReq);

      repoStatusApiClient = new org.lockss.laaws.api.rs.StatusApi(repoClient);
      repoCollectionsApiClient = new StreamingCollectionsApi(repoClient);
      repoCollectionsApiLongCallClient =
        new StreamingCollectionsApi(repoLongCallClient);
    }
    catch (MalformedURLException mue) {
      totalCounters.addError("Error parsing REST Configuration Service URL: " + mue.getMessage());
      throw new IllegalArgumentException(
          "Missing or invalid configuration service hostName: " + hostName + " port: " + repoPort);
    }

    // Must be called after config & args are processed
    initPhaseMap();
    openReportFiles();

    startTime = now();
  }

  V2RestClient makeV2RestClient() {
    return makeV2RestClient(null);
  }

  V2RestClient makeV2RestClient(V2RestClient derivedFromClient) {
    V2RestClient res;
    if (derivedFromClient == null) {
      res = new V2RestClient();
    } else {
      res = new V2RestClient(derivedFromClient.getHttpClient());
    }
    res.setTempFolderPath(PlatformUtil.getSystemTempDir());
    return res;
  }

  V2RestClient setTimeouts(V2RestClient client, String name,
                           long connectTimeout, long dataTimeout) {
    log.debug2("setTimeouts("+ name +"): connect = " +
               StringUtil.timeIntervalToString(connectTimeout) + ", data = " +
               StringUtil.timeIntervalToString(dataTimeout));
    client.setConnectTimeout((int)connectTimeout);
    client.setReadTimeout((int)dataTimeout);
    client.setWriteTimeout((int)dataTimeout);
    return client;
  }

  private void setClientParams(V2RestClient client, String userName,
                               String userPass, String userAgent,
                               String accessUrl, boolean debugReq) {
    client.setUsername(userName);
    client.setPassword(userPass);
    client.setUserAgent(userAgent);
    client.setBasePath(accessUrl);
    client.setDebugging(debugReq);
    client.addInterceptor(new RetryErrorInterceptor());
  }

  //////////////////////////////////////////////////////////////////////
  // Public entry point and top level control
  //////////////////////////////////////////////////////////////////////

  /** Entry point from MigrateContent servlet.  Synchronous - doesn't
   * return until all AUs in request have been copied. */
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

  public void abortCopy() {
    log.info("Abort requested");
    globalAbort = true;
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
   * Move one AU.
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
    startTotalTimers();
    initRequest(args);
    currentStatus = "Checking V2 services";
    checkV2ServicesAvailable();
    // get the aus known to the v2 repository
    getV2Aus();
    auMoveQueue.add(args.au);
    moveQueuedAus();
  }

  /**
   * Move all AUs that match the select patterns.
   *
   * @param args arg block holding all request args
   * @throws IOException if unable to connect to services or other error
   */
  public void moveAllAus(Args args) throws IOException {
    startTotalTimers();
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

  void startTotalTimers() {
    totalTimers.start(Phase.TOTAL);
    totalTimers.start(Phase.COPY);
    totalTimers.start(Phase.VERIFY);
  }

  /** Start/enqueue all AUs in auMoveQueue */
  private void moveQueuedAus() {
    ausLatch = new CountUpDownLatch(1, "AU");
    currentStatus = STATUS_RUNNING;
    totalAusToMove = auMoveQueue.size();
    log.debug("Moving " + totalAusToMove + " aus.");

    for (ArchivalUnit au : auMoveQueue) {
      if (isAbort()) {
        break;
      }
      ausLatch.countUp();
      moveAu(au);
    }
    ausLatch.countDown();
    enqueueFinishAll();
  }

  /**
   * Start the state machine for an AU
   */
  protected void moveAu(ArchivalUnit au) {
    log.debug2("Starting " + au.getName());
    AuStatus auStat = new AuStatus(this, au);
    auStat.getCounters().setParent(totalCounters);
    String auName = au.getName();
    log.debug("Starting state machine for AU: " + auName);

    auStat.setPhase(Phase.QUEUE);       // For display purposes only,
                                        // unlikely to be seen.
    switch (opType) {
    case VerifyOnly:
      try {
        addActiveAu(au, auStat);
        enterPhase(auStat, Phase.VERIFY);
      } catch (Exception ex) {
        log.error("Unexpect exception starting AU verify", ex);
        updateReport(auStat);
        totalAusWithErrors++;
        String err = auName + ": Attempt to verify Au failed: " + ex.getMessage();
        auStat.addError(err);
        terminated = true;
      }
      break;
      case CopyOnly:
    case CopyAndVerify:
      if (existsInV2(au)) {
        if (!checkMissingContent) {
          log.debug2("V2 Repo already has au " + au.getName() + ", skipping.");
          totalAusSkipped++;
          enterPhase(auStat, Phase.FINISH);
          return;
        } else {
          log.debug2("V2 Repo already has au " + au.getName() + ", added to check for unmoved content.");
        }
      }

      try {
        addActiveAu(au, auStat);
        log.debug("Enqueueing: " + auName);
        // Bulk mode works correctly only if the AU is completely absent
        // from the V2 repo
        if (!existsInV2(au)) {
          // Might be better to delay startBulk() until first copy task runs
          try {
            startBulk(collection, auStat.getAuId());
            auStat.setIsBulk(true); // remember to finishBulk for this AU
          } catch (UnsupportedOperationException e) {
            // Expected if not running against a V2 repo configured to
            // use the hybrid Volatile/Solr index
            log.debug2("startBulk() not supported, continuing.");
          }
        }
        // Start the process.  Enqueues the CuMover tasks.
        enterPhase(auStat, Phase.COPY);

      } catch (Exception ex) {
        log.error("Unexpect exception starting AU copy", ex);
        updateReport(auStat);
        totalAusWithErrors++;
        String err = auName + ": Attempt to move Au failed: " + ex.getMessage();
        auStat.addError(err);
        terminated = true;
      }
      break;
    }
  }

  //////////////////////////////////////////////////////////////////////
  // State machine
  //////////////////////////////////////////////////////////////////////

  /*
   * Ad hoc state machine to drive AU through migration phases.
   * (States are called Phases for historical reasons and because
   * "State" has lots of other uses here.)  Phases optionally specify
   * an entry action, and pool/queue in which to run that action, and
   * a next phase.
   *
   * Most entry actions enqueue one or more Phase-specific
   * MigrationTasks, which run in a thread pool.  These phases
   * maintain a CountUpDownLatch, incremented as work is added to the
   * pool queue, decremented as work is completed.  When the latch
   * counts down to zero, all the work for the phase is done, and
   * exitPhase is called (from the latch's runAtZero hook).
   *
   * The phase transition actions are implemented in a switch below.
   *
   */

  // Phases are defined in reverse order to avoid illegal forward references
  enum Phase {
    QUEUE("Queued"),                    // First phase
    START("Starting"),
    COPY("Copying", "Copied"),
    INDEX("Indexing"),
    VERIFY("Checking", "Checked"),
    COPY_STATE("Copying State"),
    CHECK_STATE("Checking State"),
    FINISH("Finishing"),
    DONE(""),                           // Last phase
    ABORT("Aborted"),

    TOTAL("Total");    // not really a phase, used for aggregate stats

    private String gerund;
    private String pastPart;

    Phase(String gerund) {
      this(gerund, null);
    }

    Phase(String gerund, String pastPart) {
      this.gerund = gerund;
      this.pastPart = pastPart;
    }

    public String toString() {
      return gerund;
    }

    public String gerund() {
      return gerund;
    }

    public String pastPart() {
      return pastPart;
    }

    public String verb() {
      return gerund.replaceAll("ing", "");
    }
  }

  Map<Phase,PD> pdMap;

  Phase firstStatePhase() {
    return opType.isCopy() ? Phase.COPY_STATE : Phase.CHECK_STATE;
  }

  void initPhaseMap() {
    pdMap =
      MapUtil.
      map(
          Phase.COPY, new PD(Action.EnqCopy, copyIterExecutor, Phase.INDEX),
          Phase.INDEX, new PD(Action.EnqIndex, null/*indexExecutor*/,
                              opType.isVerify() ? Phase.VERIFY : firstStatePhase()),
          Phase.VERIFY, new PD(Action.EnqVerify, verifyIterExecutor,
                               firstStatePhase()),
          Phase.COPY_STATE, new PD(Action.EnqCopyState, null, Phase.CHECK_STATE),
          Phase.CHECK_STATE, new PD(Action.EnqCheckState, null, Phase.FINISH),
          Phase.FINISH, new PD(Action.FinishAu, null, Phase.DONE)
          );
  }

  class PD {
    private Action enterAction;
    private ThreadPoolExecutor enterExecutor;
    private Phase next;

    PD(Phase next) {
      this(null, null, next);
    }

    PD(Action enterAction, ThreadPoolExecutor enterExecutor, Phase next) {
      this.enterAction = enterAction;
      this.enterExecutor = enterExecutor;
      this.next = next;
    }

    public Action getEnterAction() {
      return enterAction;
    }

    public ThreadPoolExecutor getEnterExecutor() {
      return enterExecutor;
    }

    public Phase getNextPhase() {
      return next;
    }

    public String toString() {
      return "[PD]";
    }
  }

  private final PD NULL_PD = new PD(null, null, null);

  PD getPD(Phase phase) {
    PD res = pdMap.get(phase);
    return res != null ? res : NULL_PD;
  }

  Action getEnterAction(Phase phase) {
    return getPD(phase).getEnterAction();
  }

  ThreadPoolExecutor getEnterExecutor(Phase phase) {
    return getPD(phase).getEnterExecutor();
  }

  Phase getNextPhase(Phase phase) {
    return getPD(phase).getNextPhase();
  }

  /** Enter a phase and run or enqueue the enterAction if any */
  void enterPhase(AuStatus auStat, Phase phase) {
    if (getEnterExecutor(phase) != null) {
      log.debug2("enqueueEnterPhase("+phase+")");
      getEnterExecutor(phase).execute(() -> doEnterPhase(auStat, phase));
    } else {
      doEnterPhase(auStat, phase);
    }
  }

  /** Enter the phase and run its enterAction. */
  void doEnterPhase(AuStatus auStat, Phase phase) {
    log.debug2("enterPhase("+phase+")");
    auStat.setPhase(phase);
    Action action = getEnterAction(phase);
    if (action != null) {
      doAction(auStat, action);
    }
  }

  /** Exit the current phase, enter the next if one is specified */
  void exitPhase(AuStatus auStat) {
    Phase phase = auStat.getPhase();
    log.debug2("exitPhase("+phase+")");
    if (phase == null) {
      return;
    }
    if (auStat.hasStarted(phase)) {
      auStat.stop(phase);
    }
    Phase next = getNextPhase(phase);
    if (next != null) {
      enterPhase(auStat, next);
    }
  }

  //////////////////////////////////////////////////////////////////////
  // State transition actions
  //////////////////////////////////////////////////////////////////////

  enum Action { EnqCopy, EnqVerify, EnqCopyState, EnqCheckState, EnqIndex, FinishAu }

  void doAction(AuStatus auStat, Action action) {
    ArchivalUnit au = auStat.getAu();
    String auName = auStat.getAuName();
    switch (action) {
    case EnqCopy:
      log.debug2("Enqueueing copy AU: " + auName);
      enqueueCopyAuContent(auStat);
      break;
    case EnqVerify:
      if (opType.isVerify()) {
        log.debug2("Enqueueing AU verify: " + auName);
        enqueueVerifyAuContent(auStat);
      } else {
        log.debug2("Skipping verify: " + auName);
        exitPhase(auStat);
      }
      break;
    case EnqCopyState:
      log.debug2("Enqueueing copy AU state: " + auName);
      enqueueTask(MigrationTask.copyAuState(this, au), auStat, miscExecutor);
      break;
    case EnqCheckState:
      log.debug2("Enqueueing check AU state: " + auName);
      enqueueTask(MigrationTask.checkAuState(this, au), auStat, miscExecutor);
      break;
    case EnqIndex:
      if (auStat.isBulk()) {
        log.debug2("Enqueueing index AU: " + auName);
        enqueueTask(MigrationTask.finishAuBulk(this, auStat.getAu()), auStat, indexExecutor);
      } else {
        exitPhase(auStat);
      }
      break;
    case FinishAu:
      log.debug2("Finishing AU: " + auName);
      removeActiveAu(au);
      addFinishedAu(au, auStat);
      updateReport(auStat);
      if (auStat.isAbort()) {
        enterPhase(auStat, Phase.ABORT);
      } else {
        totalAusMoved++;
        if (checkMissingContent && existsInV2(auStat.getAuId())) {
          Counters ctrs = auStat.getCounters();
          if (ctrs != null) {
            if (ctrs.isNonZero(CounterType.ARTIFACTS_MOVED)) {
              totalAusPartiallyMoved++;
            } else {
              totalAusSkipped++;
            }
          }
        }
        auStat.endPhase();
      }
      ausLatch.countDown();
      break;
    }
  }

  //////////////////////////////////////////////////////////////////////
  // MigrationTask logic run in thread pools
  //////////////////////////////////////////////////////////////////////

  /**
   * Runnable handed to pool executor
   */
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
      Throwable taskException = null;
      Phase phase = task.getTaskPhase();
      // Set phase start time the first time a task for that phase starts
      if (phase != null && auStat != null) {
        auStat.startIfNotStarted(phase);
      }
      try {
        switch (task.getType()) {
        case COPY_CU_VERSIONS:
          if (auStat.isAbort()) {
            break;
          }
          log.debug2("Moving CU: " + task.getCu());
          long startC = now();
          CuMover mover = new CuMover(v2Mover, task);
          mover.run();
          task.getCounters().add(CounterType.COPY_TIME, now() - startC);

          log.debug2("Moved CU: " + task.getCu());
          break;
        case CHECK_CU_VERSIONS:
          if (auStat.isAbort()) {
            break;
          }
          log.debug2("Checking CU: " + task.getCu());
          long startCh = now();
          CuChecker checker = new CuChecker(v2Mover, task);
          checker.run();
          task.getCounters().add(CounterType.VERIFY_TIME, now() - startCh);
          log.debug2("Checked CU: " + task.getCu());
          break;
        case FINISH_AU_BULK:
          // Currently don't check for abort here, as want to return
          // the AU to non-bulk state no matter what.

          // finish the bulk store, copy all Artifact entries to
          // permanent ArtifactIndex
          try {
            long startIndex = now();
            finishBulk(collection, auStat.getAuId());

            task.getCounters().add(CounterType.INDEX_TIME, now() - startIndex);
            log.debug2("finishBulk took " +
                       StringUtil.timeIntervalToString(now() - startIndex) +
                       ", " + auStat.getAuName());

          } catch (UnsupportedOperationException e) {
            log.warning("finishBulk() not supported");
          }
          break;
        case COPY_AU_STATE:
          if (auStat.isAbort()) {
            break;
          }
          log.debug2("Moving AU state: " + auStat.getAuName());
          long startS = now();
          AuStateMover stateMover = new AuStateMover(v2Mover, task);
          stateMover.run();
          task.getCounters().add(CounterType.STATE_TIME, now() - startS);
          log.debug2("Moved AU state: " + auStat.getAuName());
          break;
        case CHECK_AU_STATE:
          if (auStat.isAbort()) {
            break;
          }
          log.debug2("Checking AU state: " + auStat.getAuName());
          long startCH = now();
          AuStateChecker asChecker = new AuStateChecker(v2Mover, task);
          asChecker.run();
          task.getCounters().add(CounterType.STATE_TIME, now() - startCH);
          log.debug2("Checked AU state: " + auStat.getAuName());
          break;
        case FINISH_ALL:
          log.debug2("FINISH_ALL: wait");
          ausLatch.await();
          totalTimers.stop(Phase.TOTAL);
          closeReports();
          doneSem.fill();
          break;
        default:
          log.error("Unknown migration task type: " + task.getType());
        }
      } catch (Exception | OutOfMemoryError e) {
        String msg = "Task failed: " + task;
        log.error(msg, e);
        task.addError(msg + ": " + e.toString());
        taskException = e;
      } finally {
        task.countDown();
        task.complete(taskException);
        setThreadName("V2AuMover idle");
      }
    }
  }

  //////////////////////////////////////////////////////////////////////
  // Enqueue tasks
  //////////////////////////////////////////////////////////////////////

  /** Enqueue an arbitrary task into an arbitrary pool.  Arrange for
   * exitPhase() to be called when the task completes */
  void enqueueTask(MigrationTask task, AuStatus auStat,
                   ThreadPoolExecutor executor) {
//     if (auStat.isAbort()) {
//       // XXX correct to call endPhase() here?
//       auStat.endPhase();
//       return;
//     }
    Phase phase = task.getTaskPhase();
    log.debug2("enqueueTask "+task.getType() + ", phase: " + phase);
    CountUpDownLatch latch = null;
    if (phase != null) {
      if (auStat.getLatch(phase) == null) {
        latch = makePhaseEndingLatch(auStat, phase.toString());
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
   * Enqueue a copy task for each CU in the AU.  Will block if the
   * pool's queue fills.  Arrange for exitPhase() to be called when
   * all the CuMover tasks have completed */
  void enqueueCopyAuContent(AuStatus auStat) {
    CountUpDownLatch latch = makePhaseEndingLatch(auStat, "Copy");
    auStat.setLatch(Phase.COPY, latch);
    ArchivalUnit au = auStat.getAu();
    log.debug2("Enqueueing CU copies: " + au.getName());
    // Queue copies for all CUs in the v1 repo.
    for (CachedUrl cu : au.getAuCachedUrlSet().getCuIterable()) {
      if (auStat.isAbort()) {
        break;
      }
      auStat.setHasV1Content(true);
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
   * Enqueue a verify task for each CU in the AU.  Will block if the
   * pool's queue fills.  Arrange for exitPhase() to be called when
   * all the CuChecker tasks have completed */
  void enqueueVerifyAuContent(AuStatus auStat) {
    CountUpDownLatch latch = makePhaseEndingLatch(auStat, "Verify");
    auStat.setLatch(Phase.VERIFY, latch);
    ArchivalUnit au = auStat.getAu();
    log.debug2("Enqueueing CU verifies: " + au.getName());
    // Queue compares for all CUs in the v1 repo.
    for (CachedUrl cu : au.getAuCachedUrlSet().getCuIterable()) {
      if (auStat.isAbort()) {
        break;
      }
      auStat.setHasV1Content(true);
      if (opType == OpType.VerifyOnly && !existsInV2(au)) {
        // If VerifyOnly, short circuit if no content in V2
        log.debug2("Verify content: AU missing from V2: " + au.getName());
        auStat.addError("Not present in V2 repository");
        enterPhase(auStat, Phase.FINISH);
        break;
      }
      MigrationTask task = MigrationTask.checkCuVersions(this, auStat.getAu(), cu)
        .setAuStatus(auStat)
        .setCounters(auStat.getCounters())
        .setCountDownLatch(latch);
      latch.countUp();
      verifyExecutor.execute(new TaskRunner(this, task));
    }
    latch.countDown();
  }

  /** Enqueue a FINISH_ALL task */
  void enqueueFinishAll() {
    MigrationTask task = MigrationTask.finishAll(this);
    miscExecutor.execute(new TaskRunner(this, task));
  }

  /**
   * Return a CountUpDownLatch initialzied to 1, which, when it counts
   * down to zero, causes the AU's current phase to end.
   */
  CountUpDownLatch makePhaseEndingLatch(AuStatus auStat, String name) {
    CountUpDownLatch latch = new CountUpDownLatch(1, name);
    latch.setRunAtZero(() -> auStat.endPhase());
    return latch;
  }

  //////////////////////////////////////////////////////////////////////
  // V2 repo and cfg clients
  //////////////////////////////////////////////////////////////////////

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
      } while (!isAbort() && !StringUtil.isNullString(token));
    } catch (ApiException apie) {
      if (apie.getMessage().indexOf("404: Not Found") > 0) {
        return;
      } else {
        //LockssRestServiceException: The collection does not exist
        String err = "Error occurred while retrieving V2 Au list: " + apie.getMessage();
        totalCounters.addError(err);
        log.error(err, apie);
        String msg = apie.getCode() == 0 ? apie.getMessage()
          : apie.getCode() + " - " + apie.getMessage();
        throw new IOException("Unable to get Au List from V2 Repository: " + msg);
      }
    }
  }

  // Here mostly to make stack traces easier to read
  void startBulk(String collection, String auid) throws ApiException {
    repoCollectionsApiClient.handleBulkAuOp(collection, auid, "start");
  }

  void finishBulk(String collection, String auid) throws ApiException {
    repoCollectionsApiLongCallClient.handleBulkAuOp(collection, auid, "finish");
  }

  /**
   * Implements retry strategy for Okhttp REST client
   */
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
      switch (errCode) {
      case 401:
      case 403:
      case 404:
      case 501:
        return true;
      default:
        return false;
      }
    }
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
        // XXX Should parse as json, extract various fields
        sb.append(", body: ").append(body.string());
      }
      log.warning(sb.toString());
    }
    catch (Exception e) {
      log.error("Exception trying to retrieve error response body", e);
    }
  }

  //////////////////////////////////////////////////////////////////////
  // Accessors
  //////////////////////////////////////////////////////////////////////

  public boolean isCompareBytes() {
    return isCompareBytes;
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

  // Cookie not needed in this context
  public synchronized String makeCookie() {
    return null;
  }

  public ArrayList<String> getKnownV2Aus() {
    return v2Aus;
  }

  //////////////////////////////////////////////////////////////////////
  // Utility classes
  //////////////////////////////////////////////////////////////////////

  /** Argument block from MigrateContent servlet */
  public static class Args {
    String host;
    String uname;
    String upass;
    OpType opType;
    boolean isCompareContent;
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
    public Args setOpType(OpType type) {
      this.opType = type;
      return this;
    }
    public Args setCompareContent(boolean val) {
      this.isCompareContent = val;
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

  //////////////////////////////////////////////////////////////////////
  // Thread pools, config specification and creation.
  //////////////////////////////////////////////////////////////////////

  static class ExecSpec {
    int queueSize;
    int coreThreads;
    int maxThreads;
  }

  ExecSpec parsePoolSpec(String spec) {
    return parsePoolSpecInto(spec, new ExecSpec());
  }

  ExecSpec parsePoolSpecInto(String spec, ExecSpec eSpec) {
    List<String> specList = StringUtil.breakAt(spec, ";", 3, false, true);
    switch (specList.size()) {
    case 3: eSpec.maxThreads = Integer.parseInt(specList.get(2));
    case 2: eSpec.coreThreads = Integer.parseInt(specList.get(1));
    case 1: eSpec.queueSize = Integer.parseInt(specList.get(0));
    }
    // if no explicit maxThreads, make it same as coreThreads
    if (specList.size() == 2) {
      eSpec.maxThreads = eSpec.coreThreads;
    }
    return eSpec;
  }

  ThreadPoolExecutor createOrReConfigureExecutor(ThreadPoolExecutor executer,
                                                 Configuration config,
                                                 String specParam,
                                                 String defaultSpec) {
    String spec = config.get(specParam);
    // Set default for each field
    ExecSpec eSpec = parsePoolSpec(defaultSpec);
    // Override from param
    eSpec = parsePoolSpecInto(spec, eSpec);
    // If illegal, use default
    if (eSpec.coreThreads > eSpec.maxThreads) {
      log.warning("coreThreads (" + eSpec.coreThreads +
                  ") must be less than maxThreads (" + eSpec.maxThreads + ")");
      eSpec = parsePoolSpec(defaultSpec);
    }
    long threadTimeout =
      config.getTimeInterval(PARAM_THREAD_TIMEOUT, DEFAULT_THREAD_TIMEOUT);
    if (executer == null) {
      return makeExecutor(eSpec.queueSize, threadTimeout,
                          eSpec.coreThreads, eSpec.maxThreads);
    } else {
      executer.setCorePoolSize(eSpec.coreThreads);
      executer.setMaximumPoolSize(eSpec.maxThreads);
      executer.setKeepAliveTime(threadTimeout, TimeUnit.MILLISECONDS);
      // Can't change queue capacity, would need to copy to new queue
      // which requires awkward locking/synchronzation?
      return executer;
    }
  }

  public ThreadPoolExecutor makeExecutor(int queueMax, long threadTimeout,
                                         int coreThreads, int maxThreads) {
    ThreadPoolExecutor exec =
      new ThreadPoolExecutor(coreThreads, maxThreads,
                             threadTimeout, TimeUnit.MILLISECONDS,
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

  //////////////////////////////////////////////////////////////////////
  // Thread pool & queue Instrumentation.
  //////////////////////////////////////////////////////////////////////

  public List<String> getInstruments() {
    List<String> res = new ArrayList<>();
    res.add(getExecutorStats("CopyIter", copyIterExecutor));
    res.add(getExecutorStats("VerifyIter", verifyIterExecutor));
    res.add(getExecutorStats("Copy", copyExecutor));
    res.add(getExecutorStats("Verify", verifyExecutor));
    res.add(getExecutorStats("Index", indexExecutor));
    res.add(getExecutorStats("Misc", miscExecutor));
    return res;
  }

  public String getExecutorStats(String name, ThreadPoolExecutor executor) {
    StringBuilder sb = new StringBuilder();
    sb.append(name);
    sb.append(" ");
    sb.append("Threads: ");
    sb.append(executor.getPoolSize());
    sb.append("/");
    sb.append(executor.getLargestPoolSize());
    sb.append("/");
    sb.append(executor.getMaximumPoolSize());
    sb.append(", ");
    sb.append(executor.getActiveCount());
    sb.append(" active, ");
    sb.append(StringUtil.bigNumberOfUnits(executor.getTaskCount(), "task"));
    sb.append(" started");
    BlockingQueue queue = executor.getQueue();
    int size = queue.size();
    sb.append(", queue size: ");
    sb.append(size);
    sb.append("/");
    sb.append(size + queue.remainingCapacity());
    return sb.toString();
  }

  //////////////////////////////////////////////////////////////////////
  // Statistics and status keeping and reporting
  //////////////////////////////////////////////////////////////////////

  private long startTime;
  private long totalRunTime = 0;
  private File reportFile;
  private File errorFile;
  private PrintWriter reportWriter;
  private PrintWriter errorWriter;

  private String currentStatus;
  private boolean running = true; // init true avoids race while starting

  private long totalAusToMove = 0;
  private long totalAusMoved = 0;
  private long totalAusPartiallyMoved = 0; // also included in totalAusMoved
  private long totalAusSkipped = 0;
  // XXX s.b. incremented by tasks that get errors, isn't
  private long totalAusWithErrors = 0;
  private OpTimers totalTimers = new OpTimers(this);
  private Counters totalCounters = totalTimers.getCounters();

  private Map<String,AuStatus> activeAus = new LinkedHashMap<>();
  private Map<String,AuStatus> finishedAus = new LinkedHashMap<>();

  /**
   * Return a string describing the current progress, or completion state.
   */
  public List<String> getCurrentStatus() {
    List<String> res = new ArrayList<>();
    if (STATUS_RUNNING.equals(currentStatus)) {
      StringBuilder sb = new StringBuilder();
      sb.append("Running, processed ");
      sb.append(totalAusMoved);
      sb.append(" of ");
      sb.append(totalAusToMove);
      sb.append(" AUs");
      res.add(sb.toString());
      sb = new StringBuilder();
      totalTimers.addCounterStatus(sb, opType);
//       log.critical("Status: getCurrentStatus(): " + sb.toString());
      res.add(sb.toString());
    } else {
      res.add(currentStatus);
    }
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

  public List<String> getFinishedStatusPage(int index, int size) {
    Map<String,AuStatus> auStats;
    synchronized (finishedAus) {
      List<String> res = new ArrayList<>();
      int ix = 0;
      int ctr = 0;
      for (AuStatus auStat : finishedAus.values()) {
        if (ix++ >= index) {
          String one = getOneAuStatus(auStat);
          if (one != null) {
            res.add(one);
            ctr++;
          }
        }
        if (ctr >= size) {
          break;
        }
      }
      return res;
    }
  }

  // XXX need to enhance to account for verify phase
  private String getOneAuStatus(AuStatus auStat) {
    Counters ctrs = auStat.getCounters();
    StringBuilder sb = new StringBuilder();
    Phase phase = auStat.getPhase();
    String phaseName = phase.toString();
    if (false && auStat.isAbort()) {
      sb.append("Aborted: ");
    } else {
      switch (auStat.getPhase()) {
      case DONE:
        break;
      case COPY:
      case VERIFY:
      case INDEX:
        if (!auStat.hasStarted(phase)) {
          phaseName = phase.verb() + " queued";
        }
        break;
      }
      if (!StringUtil.isNullString(phaseName)) {
        sb.append(phaseName);
        sb.append(": ");
      }
    }
    sb.append(auStat.getAuName());
    switch (auStat.getPhase()) {
    case START:
      break;
    case COPY:
    case VERIFY:
      auStat.addCounterStatus(sb, opType, ": ");
      break;
    case DONE:
      if (!auStat.hasV1Content()) {
        sb.append(": No V1 content");
        break;
      }
    default:
      auStat.addCounterStatus(sb, opType, ": ");
    }
    String foo = sb.toString();
    if (foo.contains("Public")) {
//       log.critical("Status: getOneAuStatus(): " + foo);
    }
    return sb.toString();
  }

  public boolean isRunning() {
    return running;
  }

  public boolean isTerminated() {
    return terminated;
  }

  public boolean isAbort() {
    return globalAbort;
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

  //////////////////////////////////////////////////////////////////////
  // Report files
  //////////////////////////////////////////////////////////////////////

  /**
   * Open (append) the Report file and error file
   */
  void openReportFiles() {
    String now = DateFormatter.now();
    reportWriter = openReportFile(reportFile, "Report", now);
    errorWriter = openReportFile(errorFile, "Error Report", now);
  }

  PrintWriter openReportFile(File file, String title, String now) {
    PrintWriter res = null;
    try {
      log.info("Writing " + title + " to " + file.getAbsolutePath());
      res = new PrintWriter(Files.newOutputStream(file.toPath(),
                                                  CREATE, APPEND),
                            true);
      res.println("--------------------------------------------------");
      res.println("  V2 Au Migration " + title + " - " + now);
      res.println("--------------------------------------------------");
      res.println();
      if (res.checkError()) {
        log.warning("Error writing " + title + " file.");
      }
    }
    catch (IOException e) {
      log.error(title + " file will not be written: Unable to open file:" +
                e.getMessage());
    }
    return res;
  }

  /**
   * Update the report for the current Au.
   * @param au The ArchivalUnit which is being counted
   * @param auStat The AuStatus for this ArchivalUnit.
   */
  void updateReport(AuStatus auStat) {
    if (reportWriter == null) {
      log.error("updateReport called when no reportWriter",
                new Throwable());
      return;
    }
    if (auStat == null) {
      log.error("updateReport called with AU that's not running: " +
                auStat.getAuName(),
                new Throwable());
      reportWriter.println("updateReport called with AU that's not running: " +
                           auStat.getAuName());
      return;
    }
    StringBuilder sb = new StringBuilder();
    auStat.addCounterStatus(sb, opType);
    String auData = sb.toString();
    reportWriter.println("AU Name: " + auStat.getAuName());
    reportWriter.println("AU ID: " + auStat.getAuId());
    if (auStat.isAbort()) {
      reportWriter.print("Aborted after ");
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
      writeErrors(reportWriter, auStat);
      errorWriter.println("Errors in AU: " + auStat.getAuName());
      errorWriter.println("AU ID: " + auStat.getAuId());
      writeErrors(errorWriter, auStat);
    }
    if (auStat.getErrorCount() > 0) {
    }
    reportWriter.println();
    if (reportWriter.checkError()) {
      log.warning("Error writing report file.");
    }
  }

  void writeErrors(PrintWriter writer, AuStatus auStat) {
    for (String err : auStat.getErrors()) {
      writer.println(" " + err);
    }
    writer.println();
  }

  /**
   * Close the report before exiting
   */
  void closeReports() {
    String now = DateFormatter.now();
    closeReport(reportWriter, now);
    closeReport(errorWriter, now);

  }

  void appendTotalSummary(StringBuilder sb) {
    if (opType.isVerifyOnly()) {
      sb.append(StringUtil.bigNumberOfUnits(totalAusMoved, "AU") + " checked");
    }
    sb.append(StringUtil.bigNumberOfUnits(totalAusMoved, "AU") + " copied");
    if (totalAusPartiallyMoved > 0 || totalAusSkipped > 0) {
      sb.append(" (");
      if (totalAusSkipped > 0) {
        sb.append(bigIntFormat(totalAusSkipped));
        sb.append(" previously");
        if (totalAusPartiallyMoved > 0) {
          sb.append(", ");
        }
      }
      if (totalAusPartiallyMoved > 0) {
        sb.append(bigIntFormat(totalAusPartiallyMoved));
        sb.append(" partially");
      }
      sb.append(")");
    }
  }

  void closeReport(PrintWriter writer, String now) {
    StringBuilder sb = new StringBuilder();
    if (opType.isVerifyOnly()) {
    }
    sb.append(StringUtil.bigNumberOfUnits(totalAusMoved, "AU") + " copied");
    if (totalAusPartiallyMoved > 0 || totalAusSkipped > 0) {
      sb.append(" (");
      if (totalAusSkipped > 0) {
        sb.append(bigIntFormat(totalAusSkipped));
        sb.append(" previously");
        if (totalAusPartiallyMoved > 0) {
          sb.append(", ");
        }
      }
      if (totalAusPartiallyMoved > 0) {
        sb.append(bigIntFormat(totalAusPartiallyMoved));
        sb.append(" partially");
      }
      sb.append(")");
    }
    totalTimers.addCounterStatus(sb, opType, ": ");
    String summary = sb.toString();
    running = false;
    currentStatus = summary;
    if (writer != null) {
      writer.println("--------------------------------------------------");
      writer.println((isAbort() ? " Aborted" : "  Finished") + " with " +
                     StringUtil.bigNumberOfUnits(totalTimers.getErrorCount(),
                                                 "error") +
                     " at " + now);
      writer.println(summary);
      writer.println("--------------------------------------------------");
      writer.println("");
      if (writer.checkError()) {
        log.warning("Error writing report file.");
      }

      writer.close();
    }
    log.info(summary);
  }

  //////////////////////////////////////////////////////////////////////
  // Utilities
  //////////////////////////////////////////////////////////////////////

  public static String bigIntFormat(long x) {
    return TH_BIGINT_FMT.get().format(x);
  }

  public static String percentFormat(double x) {
    return TH_PERCENT_FMT.get().format(x);
  }

  long now() {
    return System.currentTimeMillis();
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

  //////////////////////////////////////////////////////////////////////
  // testing getters & setters
  //////////////////////////////////////////////////////////////////////

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

}
