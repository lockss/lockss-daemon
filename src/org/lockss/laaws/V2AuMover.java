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
import java.util.*;
import java.util.concurrent.*;
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
import org.lockss.laaws.model.rs.AuidPageInfo;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
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

  private long startTime;
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
    compareBytes=config.getBoolean(PARAM_COMPARE_CONTENT, DEFAULT_COMPARE_CONTENT);
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

  public boolean isCompareBytes() {
    return compareBytes;
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
    try {
      initRequest(args);
      currentStatus = "Checking V2 services";
      checkV2ServicesAvailable();
      // get the aus known to the v2 repository
      getV2Aus();
      auMoveQueue.add(args.au);
      moveQueuedAus();
    } finally {
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
    } finally {
      closeReport();
    }
  }

  private void moveQueuedAus() {
    totalAusToMove = auMoveQueue.size();
    log.debug("Moving " + totalAusToMove + " aus.");

    for (ArchivalUnit au : auMoveQueue) {
      if (terminated) {
        break;
      }
      moveAu(au);
      log.debug2("moveAu() returned: terminated: " +
                 terminated + ", queue: " + auMoveQueue);
    }
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
      Exception taskException = null;
      // Set AU start time the first time a task for the AU starts running
      AuStatus auStat = task.getAuStatus();
      if (auStat.getStartTime(task.getType().getPhase()) < 0) {
        auStat.setStartTime(task.getType().getPhase(), System.currentTimeMillis());
      }
      try {
        switch (task.getType()) {
        case COPY_CU_VERSIONS:
          try {
            CuMover mover = new CuMover(v2Mover, task);
            mover.run();
          } finally {
            task.getLatch().countDown();
          }
          break;
        case COPY_AU_STATE:
          // wait until all of this AU's CU copies are done
          log.debug("Waiting until AU copy finished: " + task.getAuStatus().getAuName());
          task.getLatch().await();
          log.debug("AU copy finished: " + task.getAuStatus().getAuName());
          AuStateMover asmover = new AuStateMover(v2Mover, task);
          asmover.run();
          break;
        case CHECK_CU_VERSIONS:
          CuChecker cuChecker = new CuChecker(v2Mover, task);
          cuChecker.run();
          break;
        case CHECK_AU_STATE:
          AuStateChecker asChecker = new AuStateChecker(v2Mover, task);
          asChecker.run();
          break;
        default:
          log.error("Unknown migration task type: " + task.getType());
        }
      } catch (Exception e) {
        taskException = e;
      } finally {
        task.complete(taskException);
      }
    }
  }

  /**
   * Perform all actions to move an AU: queue CUs, queue state, queue verify
   * @throws IOException on network failures.
   */
  protected void moveAu(ArchivalUnit au) {
    // Marker for getCurrentStatus() to return current AU's progress.
    log.debug("Moving " + au.getName() + " - " + auMoveQueue.size() + " AUs remaining.");
    AuStatus auStat = new AuStatus(au);
    String auName = au.getName();
    log.info("Handling request to move AU: " + auName);
    log.info("AuId: " + au.getAuId());
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
      log.info(auName + ": Moving AU Artifacts...");
      moveAuArtifacts(au, auStat);
      moveAuState(au, auStat);
      finishAu(au);
      if (!terminated) {
        log.info(auName + ": Successfully moved AU Artifacts.");
      }
      else {
        log.info(auName + ": Au move terminated because of errors.");
        totalAusWithErrors++;
      }
      updateReport(au, auStat);
      totalAusMoved++;
      if (v2Aus.contains(au.getAuId())) {
        totalAusPartiallyMoved++;
      }
//       updateTotals();
    }
    catch (Exception ex) {
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
        dispatcher.cancelAll();
//         throw new IOException("Au Move Request terminated due to errors:" + err);
      }
    }
  }

  /**
   * Move one V1 Au including all cachedUrls and all versions.
   *
   * @param au au The ArchivalUnit to move
   */
  void moveAuArtifacts(ArchivalUnit au, AuStatus auStat) throws ApiException {
    CountUpDownLatch latch = new CountUpDownLatch();
    synchronized (runningAUs) {
      runningAUs.put(au.getAuId(), auStat);
    }
    // Queue copies for all CUs in the v1 repo.
    for (CachedUrl cu : au.getAuCachedUrlSet().getCuIterable()) {
      MigrationTask task = MigrationTask.copyCuVersions(this, au, cu)
        .setCounters(auStat.getCounters())
        .setLatch(latch);
      latch.countUp();
      taskExecutor.execute(new TaskRunner(this, task));
    }
  }

  /**
   * Move one V1 Au including all cachedUrls and all versions.
   *
   * @param au au The ArchivalUnit to move
   */
  void finishAu(ArchivalUnit au) {
    CountUpDownLatch latch = new CountUpDownLatch();
    // Get the au CachedUrls from the v1 repo.
    for (CachedUrl cu : au.getAuCachedUrlSet().getCuIterable()) {
      MigrationTask task = MigrationTask.copyCuVersions(this, au, cu)
        .setLatch(latch);
      latch.countUp();
      taskExecutor.execute(new TaskRunner(this, task));
    }
  }

  /**
   * Check the Artifacts moved to the V2 repository for errors
   * @param au the ArchivalUnit which needs to be checked.
   */
  void checkAuArtifacts(ArchivalUnit au) {
    String auId = au.getAuId();
    log.info("Comparing v2 artifacts with V1 data");
    /* get Au cachedUrls from Lockss*/
    for (CachedUrl cu : au.getAuCachedUrlSet().getCuIterable()) {
      taskExecutor.execute(new TaskRunner(this,
          MigrationTask.copyCuVersions(this, au, cu)));
    }
  }


  /**
   * Complete the au move by moving the state and config information.
   *
   * @param au the au we are moving.
   */
  void moveAuState(ArchivalUnit au, AuStatus auStat) {
    if (!terminated) {
      taskExecutor.execute(new TaskRunner(this,
          MigrationTask.copyAuState(this,
              au)));
    }
  }

  /**
   * Check the the State information moved to the V2 repository.
   *
   * @param au the au we are checking.
   */
  void checkAuState(ArchivalUnit au) {
    if (!terminated) {
      taskExecutor.execute(new TaskRunner(this,
          MigrationTask.checkAuState(this,
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

  // Statistics and status keeping and reporting

  private final File reportFile;

  private String currentStatus;
  private boolean running = true; // init true avoids race while starting

  private PrintWriter reportWriter;

  private long totalAusToMove = 0;
  private long totalAusMoved = 0;
  private long totalAusPartiallyMoved = 0; // also included in totalAusMoved
  private long totalAusSkipped = 0;
  private long totalAusWithErrors = 0;
  private long totalRunTime = 0;
  private Counters totalCounters = new Counters();
  private AuStatus totalStatus = new AuStatus(null);

  private Map<String,AuStatus> runningAUs = new LinkedHashMap<>();

  private final List<String> errorList = new ArrayList<>();

  enum Phase {COPY, VERIFY, TOTAL}

  enum CounterType {
    URLS_MOVED,
    URLS_SKIPPED,
    ARTIFACTS_MOVED,
    ARTIFACTS_SKIPPED,
    BYTES_MOVED,
    CONTENT_BYTES_MOVED,
    RUNTIME
  }

  public static class Counters {
    Map<CounterType,Counter> counters = new HashMap<>();
    private long errorCount = 0;
    private List<String> errors = new ArrayList<>();

    public Counters() {
      for (CounterType type : CounterType.values()) {
        counters.put(type, new Counter());
      }
    }

    public synchronized Counter get(CounterType type) {
      return counters.get(type);
    }

    public long getVal(CounterType type) {
      return counters.get(type).getVal();
    }

    public void addError(String msg) {
      errors.add(msg);
    }

    public synchronized void add(Counters ctrs) {
      for (CounterType type : CounterType.values()) {
        get(type).add(ctrs.get(type));
      }
      errors.addAll(ctrs.errors);
    }
  }

  /** Status and counters for a single AU in progress */
  public static class AuStatus {

    String auid;
    String auname;
    String status;
    Counters ctrs;
    Map<Phase,Long> startTime = new HashMap<>();
    Map<Phase,Long> endTime = new HashMap<>();
    long copyTime = -1;
    long verifyTime = -1;
    List<String> errors = new ArrayList<>();

    public AuStatus(ArchivalUnit au) {
      auid = au.getAuId();
      auname = au.getName();
      ctrs = new Counters();
      status = "Initializing";
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

    public String getStatus() {
      return status;
    }

    public List<String> getErrors() {
      return errors;
    }

    public int getErrorCount() {
      return errors.size();
    }

    public long getStartTime(Phase phase) {
      return startTime.get(phase);
    }

    public long getRunTime(Phase phase) {
      return endTime.get(phase) - startTime.get(phase);
    }

    public void setStatus(String val) {
      status = val;
    }

    public void setStartTime(Phase phase, long time) {
      startTime.put(phase, time);
    }

    public void setEndTime(Phase phase, long time) {
      endTime.put(phase, time);
    }

    public void addError(String msg) {
      errors.add(msg);
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


  /**
   * Return a string describing the current progress, or completion state.
   */
  public String getCurrentStatus() {
    if (STATUS_COPYING.equals(currentStatus)) {
      StringBuilder sb = new StringBuilder();
      sb.append("Copied");
      sb.append(totalAusMoved);
      sb.append(" of ");
      sb.append(totalAusToMove);
      sb.append(" AUs, ");
      addCounterStatus(sb, totalCounters, totalStatus, Phase.TOTAL);
      return sb.toString();
    }
    return currentStatus;
  }

  public List<String> getCurrentStatusList() {

    Map<String,AuStatus> auStats;
    synchronized (runningAUs) {
      auStats = new LinkedHashMap<>(runningAUs);
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

  private String getOneAuStatus(AuStatus auStat) {
    Counters ctrs = auStat.getCounters();
    StringBuilder sb = new StringBuilder();
    sb.append(auStat.getAuName());
    sb.append(": ");
    addCounterStatus(sb, ctrs, auStat, Phase.COPY);
    return sb.toString();
  }

  private void addCounterStatus(StringBuilder sb, Counters ctrs, AuStatus auStat, Phase phase) {
    sb.append(StringUtil.bigNumberOfUnits(ctrs.getVal(CounterType.URLS_MOVED),
                                          "URL"));
    sb.append(" copied, ");
    sb.append(bigIntFmt.format(ctrs.getVal(CounterType.URLS_SKIPPED)));
    sb.append(" skipped, ");
    sb.append(StringUtil.bigNumberOfUnits(ctrs.getVal(CounterType.ARTIFACTS_MOVED),
                                          "version"));
    sb.append(" copied, ");
    sb.append(bigIntFmt.format(ctrs.getVal(CounterType.ARTIFACTS_SKIPPED)));
    sb.append(" skipped, ");
    sb.append(StringUtil.bigNumberOfUnits(ctrs.getVal(CounterType.BYTES_MOVED),
                                          "byte"));
    sb.append(" copied, in ");
    sb.append(StringUtil.timeIntervalToString(auStat.getRunTime(phase)));
    if (ctrs.getVal(CounterType.BYTES_MOVED) > 0) {
      sb.append(", at ");
      sb.append(StringUtil.byteRateToString(ctrs.getVal(CounterType.BYTES_MOVED),
                                            auStat.getRunTime(phase)));
    }
  }

  public boolean isRunning() {
    return running;
  }

  /**
   * Returns the list of errors which occurred while attempting to move the AU(s).
   *
   * @return the list of error strings
   */
  public List<String> getErrors() {
    return errorList;
  }

  private void addRunningAU(ArchivalUnit au) {
    AuStatus austat = new AuStatus(au);
    synchronized (runningAUs) {
      runningAUs.put(au.getAuId(), austat);
    }
  }

  private void removeRunningAU(ArchivalUnit au) {
    synchronized (runningAUs) {
      runningAUs.remove(au.getAuId());
    }
  }

  public AuStatus getAuStatus(ArchivalUnit au) {
    synchronized (runningAUs) {
      return runningAUs.get(au.getAuId());
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
    sb.append(", in ");
    sb.append(StringUtil.timeIntervalToString(auStat. getRunTime(Phase.COPY)));
    String auData = sb.toString();
    reportWriter.println("AU Name: " + auStat.getAuName());
    reportWriter.println("AU ID: " + auStat.getAuId());
    if (terminated) {
      reportWriter.println("Move terminated with error.");
      reportWriter.println(auData);
    }
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
    addCounterStatus(sb, totalCounters, totalStatus, Phase.TOTAL);
//     sb.append(StringUtil.bigNumberOfUnits(totalUrlsMoved, "URL") + ", ");
//     sb.append(StringUtil.bigNumberOfUnits(totalArtifactsMoved, "version") + ", ");
//     sb.append(StringUtil.bigNumberOfUnits(totalContentBytesMoved, "content byte") + ", ");
//     sb.append(StringUtil.bigNumberOfUnits(totalBytesMoved, "total byte") + ", ");
//     if (totalBytesMoved > 0) {
//       sb.append("at ");
//       sb.append(StringUtil.byteRateToString(totalBytesMoved, totalRunTime));
//       sb.append(", ");
//     }
//     sb.append(StringUtil.bigNumberOfUnits(totalErrorCount, "error") + ", ");
//     sb.append("in ");
//     sb.append(StringUtil.timeIntervalToString(totalRunTime));
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
   * Add an Error to the list and update error count;
   * @param err the error string to add.
   */
//   void addError(String err) {
//     errorList.add(err);
//     auErrors.add(err);
//     auErrorCount++;
//   }

//   /**
//    * update the reported totals after completing an au move
//    */
//   void updateTotals() {
//     totalBytesMoved += auBytesMoved;
//     totalContentBytesMoved += auContentBytesMoved;
//     totalUrlsMoved += auUrlsMoved;
//     totalArtifactsMoved += auArtifactsMoved;
//     totalErrorCount += auErrorCount;
//   }

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
      if(response.body() != null)
        log.warning("Error response body: " + response.body().string());
    }
    catch (IOException e) {
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


}


//       errorList.add(msg);
//       auErrors.add(msg);

// in one au
//       totalCounters.runTime = System.currentTimeMillis() - startTime;
// in finish all
//       totalCounters.runTime = System.currentTimeMillis() - startTime;

