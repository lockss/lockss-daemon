/*
 * Copyright (c) 2026, Board of Trustees of Leland Stanford Jr. University.
 * All rights reserved.
 */
package org.lockss.laaws;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.lockss.config.Configuration;
import org.lockss.laaws.mock.MockV2Lockss;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.PluginTestUtil;
import org.lockss.test.ConfigurationUtil;
import org.lockss.protocol.MockIdentityManager;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.MockCachedUrl;
import org.lockss.test.MockCrawlManager;
import org.lockss.test.MockLockssDaemon;
import org.lockss.test.MockPlugin;
import org.lockss.test.MockPollManager;
import org.lockss.util.CIProperties;
import org.lockss.util.Deadline;

/** Abstract base class for tests that drive {@link V2AuMover} against a
 * {@link MockV2Lockss} fixture.  Provides setUp/tearDown that:
 *  - creates a {@code MockLockssDaemon} with a {@link MockIdentityManager},
 *  - starts a {@code MockV2Lockss} on ephemeral ports,
 *  - applies the migration config params so V2AuMover routes to the mock.
 *
 * Subclasses must extend {@link LockssTestCase} via this class, and may
 * use the helpers below to construct AUs and run mover ops. */
public abstract class V2MoverTestBase extends LockssTestCase {

  protected MockLockssDaemon daemon;
  protected MockIdentityManager idMgr;
  protected MockV2Lockss mockV2;
  protected File tempDir;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    daemon = getMockLockssDaemon();
    idMgr = new MockIdentityManager();
    idMgr.initService(daemon);
    daemon.setIdentityManager(idMgr);

    // Pre-install lightweight mocks for managers that V2AuMover's
    // constructor pulls from the daemon.  Without these, MockLockssDaemon
    // would lazily instantiate the real implementations, which can
    // require heavyweight startup we don't want in a unit test.
    MockCrawlManager mockCrawl = new MockCrawlManager();
    daemon.setCrawlManager(mockCrawl);
    MockPollManager mockPoll = new MockPollManager();
    daemon.setPollManager(mockPoll);

    tempDir = getTempDir();

    // Force migration reports to land in the test temp dir.
    ConfigurationUtil.addFromArgs(
        V2AuMover.PARAM_REPORT_DIR, tempDir.getAbsolutePath());

    mockV2 = new MockV2Lockss();
    mockV2.start();

    // Eagerly create the MigrationManager BEFORE applying config: that
    // way the manager registers itself as a config callback and picks up
    // dryRunEnabled=true when applyMigrationConfig() fires the change.
    daemon.getMigrationManager();
    mockV2.applyMigrationConfig();
  }

  @Override
  protected void tearDown() throws Exception {
    if (mockV2 != null) {
      mockV2.stop();
      mockV2 = null;
    }
    super.tearDown();
  }

  // ---- AU construction helpers --------------------------------------

  /** Construct a {@link MockArchivalUnit} with synthetic content for
   * {@code numUrls} URLs, each with {@code versionsPerUrl} versions
   * and ~1 KB of content per version. */
  protected MockArchivalUnit createV1Au(String auid,
                                        int numUrls,
                                        int versionsPerUrl)
      throws org.lockss.plugin.ArchivalUnit.ConfigurationException {
    MockPlugin plugin = new MockPlugin(daemon);
    plugin.setPluginId("org.lockss.testplugin.MockPlug");
    MockArchivalUnit au = new MockArchivalUnit(plugin, auid);
    au.setUrlStems(Arrays.asList("http://example.com/"));
    Configuration cfg = ConfigurationUtil.fromArgs("base_url",
                                                   "http://example.com/");
    au.setConfiguration(cfg);

    long now = System.currentTimeMillis();
    for (int i = 0; i < numUrls; i++) {
      String url = "http://example.com/page" + i + ".html";
      String content = makePadded("content for " + url + " v0", 1024);
      MockCachedUrl cu = au.addUrl(url, content);
      // Set version-1 properties; further versions cloned via addVersion.
      CIProperties props = new CIProperties();
      props.setProperty(CachedUrl.PROPERTY_NODE_URL, url);
      props.setProperty(CachedUrl.PROPERTY_FETCH_TIME, Long.toString(now));
      props.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
      cu.setProperties(props);
      cu.setVersion(1);
      cu.setExists(true);
      cu.setContentSize(content.length());
      for (int v = 2; v <= versionsPerUrl; v++) {
        String c2 = makePadded("content for " + url + " v" + (v-1), 1024);
        MockCachedUrl mcv = cu.addVersion(c2);
        // MockCachedUrl.addVersion auto-numbers from versions.size()+1, but
        // the original CU sits outside that list and is already version 1.
        mcv.setVersion(v);
        CIProperties p2 = new CIProperties();
        p2.setProperty(CachedUrl.PROPERTY_NODE_URL, url);
        p2.setProperty(CachedUrl.PROPERTY_FETCH_TIME, Long.toString(now + v));
        p2.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
        mcv.setProperties(p2);
        mcv.setExists(true);
        mcv.setContentSize(c2.length());
      }
    }
    au.populateAuCachedUrlSet();
    // Register so daemon.getPluginManager().getAuFromId(auid) finds it.
    PluginTestUtil.registerArchivalUnit(plugin, au);
    return au;
  }

  private static String makePadded(String prefix, int size) {
    StringBuilder sb = new StringBuilder(size);
    sb.append(prefix);
    while (sb.length() < size) sb.append('.');
    return sb.toString();
  }

  // ---- Mover lifecycle helpers --------------------------------------

  /** Construct a fresh V2AuMover bound to the current MockLockssDaemon.
   * Caller is responsible for calling executeRequests / wait helpers. */
  protected V2AuMover newMover() {
    return new V2AuMover();
  }

  protected void runCopyAus(V2AuMover mover, List<String> auids) throws Exception {
    List<ArchivalUnit> aus = new ArrayList<ArchivalUnit>();
    for (String auid : auids) {
      ArchivalUnit au = daemon.getPluginManager().getAuFromId(auid);
      if (au != null) aus.add(au);
    }
    V2AuMover.Args args = baseArgs(MigrationManager.OpType.CopyAndVerify)
        .setAus(aus);
    mover.executeRequests(java.util.Collections.singletonList(args));
  }

  protected void runCopyConfig(V2AuMover mover) throws Exception {
    V2AuMover.Args args = baseArgs(MigrationManager.OpType.CopyConfig);
    mover.executeRequests(java.util.Collections.singletonList(args));
  }

  protected void runCopySystemSettings(V2AuMover mover) throws Exception {
    V2AuMover.Args args = baseArgs(MigrationManager.OpType.CopySystemSettings);
    mover.executeRequests(java.util.Collections.singletonList(args));
  }

  protected void runCopyDatabase(V2AuMover mover) throws Exception {
    V2AuMover.Args args = baseArgs(MigrationManager.OpType.CopyDatabase);
    mover.executeRequests(java.util.Collections.singletonList(args));
  }

  /** Block up to {@code timeoutMs} for {@code mover} to enter a
   * non-running state.  Polling-based: {@link V2AuMover#executeRequests}
   * is normally synchronous; this is a safety net for any future
   * test that drives the mover from a separate thread. */
  protected void awaitMigrationComplete(V2AuMover mover, long timeoutMs) {
    Deadline d = Deadline.in(timeoutMs);
    while (!d.expired()) {
      if (!mover.isRunning()) return;
      try { Thread.sleep(50); } catch (InterruptedException ignored) { return; }
    }
  }

  /** Build a base Args block populated with hostname/credentials
   * pointing at the local MockV2Lockss. */
  protected V2AuMover.Args baseArgs(MigrationManager.OpType opType) {
    return new V2AuMover.Args()
        .setHost(mockV2.getHostname())
        .setUname(mockV2.getUsername())
        .setUpass(mockV2.getPassword())
        .setOpType(opType);
  }
}
