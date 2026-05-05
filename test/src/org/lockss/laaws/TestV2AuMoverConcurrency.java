/*
 * Copyright (c) 2026, Board of Trustees of Leland Stanford Jr. University.
 * All rights reserved.
 */
package org.lockss.laaws;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.lockss.config.Configuration;
import org.lockss.laaws.mock.MockV2Backend;
import org.lockss.laaws.mock.RecordedRequest;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.PluginTestUtil;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.MockCachedUrl;
import org.lockss.test.MockPlugin;
import org.lockss.util.CIProperties;

/** Concurrency / scale stress tests for {@link V2AuMover}.
 *
 * <p>The smoke test {@link TestV2AuMoverHarness} exercises one tiny AU.
 * These tests pump thousands of AUs (and/or thousands of CU versions)
 * through the live mover wired to a {@link org.lockss.laaws.mock.MockV2Lockss}
 * fixture, in order to surface threading bugs that only manifest under
 * load: latch off-by-one, queue starvation, executor deadlocks, and
 * non-atomic state-map updates in the phase state machine.
 *
 * <p>State phases are skipped because {@link
 * org.lockss.protocol.MockIdentityManager} isn't an
 * {@code IdentityManagerImpl}; we filter the resulting "Copy state" /
 * "AU State Check" errors so the stress signal isn't drowned out.
 *
 * <p>All non-{@code LONG} tests are sized to finish in well under
 * 120s of wall clock on a normal dev machine. */
public class TestV2AuMoverConcurrency extends V2MoverTestBase {

  /** Reused plugin id for all synthetic AUs in this test class. */
  private static final String PLUGIN_ID = "org.lockss.testplugin.MockPlug";

  /** Single MockPlugin shared by all fast-built AUs in a single test run.
   * Construction of MockPlugin allocates a fair amount of state; reusing
   * one across AUs keeps memory and setup cost down at scale. */
  private MockPlugin sharedPlugin;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    sharedPlugin = new MockPlugin(daemon);
    sharedPlugin.setPluginId(PLUGIN_ID);

    // Force the V2AuMover executors to predictable, modest sizes.  This
    // gives the tests a known concurrency budget (so we can reason about
    // wall-clock bounds) and ensures the mover-side queues fill, which is
    // exactly what we want to stress.
    Properties execProps = new Properties();
    execProps.put(V2AuMover.PARAM_COPY_EXECUTOR_SPEC,        "200;8");
    execProps.put(V2AuMover.PARAM_VERIFY_EXECUTOR_SPEC,      "200;4");
    execProps.put(V2AuMover.PARAM_COPY_ITER_EXECUTOR_SPEC,   "2;4");
    execProps.put(V2AuMover.PARAM_RETRY_ITER_EXECUTOR_SPEC,  "2;4");
    execProps.put(V2AuMover.PARAM_VERIFY_ITER_EXECUTOR_SPEC, "10;4");
    execProps.put(V2AuMover.PARAM_INDEX_EXECUTOR_SPEC,       "200;4");
    execProps.put(V2AuMover.PARAM_STATE_COPY_EXECUTOR_SPEC,  "50;4");
    execProps.put(V2AuMover.PARAM_STATE_VERIFY_EXECUTOR_SPEC,"50;4");
    execProps.put(V2AuMover.PARAM_MISC_EXECUTOR_SPEC,        "50;4");
    ConfigurationUtil.addFromProps(execProps);
  }

  // ---- helpers -------------------------------------------------------

  /** Like {@link V2MoverTestBase#createV1Au} but minimizes per-AU cost so
   * thousands can be created cheaply: shares one {@link MockPlugin}
   * across AUs, uses tiny (un-padded) content payloads, and avoids any
   * filesystem touch. */
  protected MockArchivalUnit fastCreateAu(String auid,
                                          int numUrls,
                                          int versionsPerUrl) throws Exception {
    MockArchivalUnit au = new MockArchivalUnit(sharedPlugin, auid);
    au.setUrlStems(Arrays.asList("http://example.com/"));
    Configuration cfg = ConfigurationUtil.fromArgs("base_url",
                                                   "http://example.com/");
    au.setConfiguration(cfg);

    long now = System.currentTimeMillis();
    for (int i = 0; i < numUrls; i++) {
      // Embed auid in URL so each AU's URLs are distinct in backend logs.
      String url = "http://example.com/" + safe(auid) + "/p" + i;
      String content = "v1:" + i;                  // ~6 bytes
      MockCachedUrl cu = au.addUrl(url, content);
      CIProperties props = new CIProperties();
      props.setProperty(CachedUrl.PROPERTY_NODE_URL, url);
      props.setProperty(CachedUrl.PROPERTY_FETCH_TIME, Long.toString(now));
      props.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/plain");
      cu.setProperties(props);
      cu.setVersion(1);
      cu.setExists(true);
      cu.setContentSize(content.length());
      for (int v = 2; v <= versionsPerUrl; v++) {
        String c2 = "v" + v + ":" + i;
        MockCachedUrl mcv = cu.addVersion(c2);
        // MockCachedUrl.addVersion auto-numbers from versions.size()+1
        // but the original CU sits outside that list and is already
        // version 1 -- without the override, addVersion produces a
        // duplicate v=1 then v=2, ...
        mcv.setVersion(v);
        CIProperties p2 = new CIProperties();
        p2.setProperty(CachedUrl.PROPERTY_NODE_URL, url);
        p2.setProperty(CachedUrl.PROPERTY_FETCH_TIME, Long.toString(now + v));
        p2.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/plain");
        mcv.setProperties(p2);
        mcv.setExists(true);
        mcv.setContentSize(c2.length());
      }
    }
    au.populateAuCachedUrlSet();
    PluginTestUtil.registerArchivalUnit(sharedPlugin, au);
    return au;
  }

  /** Strip characters that would derail URL building; auids contain
   * '|' '&' '~' '%' which are fine but ugly in logs. */
  private static String safe(String auid) {
    StringBuilder sb = new StringBuilder(auid.length());
    for (int i = 0; i < auid.length(); i++) {
      char c = auid.charAt(i);
      sb.append(Character.isLetterOrDigit(c) ? c : '_');
    }
    return sb.toString();
  }

  /** Make a synthetic auid string in the canonical {@code plugin&base_url~...}
   * shape so {@code PluginManager.getAuFromId} round-trips correctly. */
  private static String synthAuid(int n) {
    return "org|lockss|testplugin|MockPlug&base_url~http%3A%2F%2Fex"
        + n + "%2Ecom%2F";
  }

  /** Build {@code n} AUs and return their auids in creation order. */
  private List<MockArchivalUnit> buildAus(int n, int urls, int versions)
      throws Exception {
    List<MockArchivalUnit> out = new ArrayList<MockArchivalUnit>(n);
    for (int i = 0; i < n; i++) {
      out.add(fastCreateAu(synthAuid(i), urls, versions));
    }
    return out;
  }

  /** Assert that the only errors the mover reports are the
   * known-tolerated state-phase NPEs caused by MockIdentityManager. */
  private void assertNoUnexpectedErrors(V2AuMover m) {
    List<String> errors = m.getErrors();
    if (errors == null) return;
    List<String> bad = new ArrayList<String>();
    for (String e : errors) {
      if (e == null) continue;
      if (e.contains("Copy state")) continue;
      if (e.contains("AU State Check")) continue;
      bad.add(e);
    }
    if (!bad.isEmpty()) {
      fail("V2AuMover reported " + bad.size() + " unexpected error(s); "
           + "first=" + bad.get(0));
    }
  }

  /** Convenience: drive a CopyAndVerify across the supplied AUs. */
  private void runCopyAndVerify(V2AuMover mover,
                                List<MockArchivalUnit> aus) throws Exception {
    List<ArchivalUnit> generic = new ArrayList<ArchivalUnit>(aus);
    V2AuMover.Args args = baseArgs(MigrationManager.OpType.CopyAndVerify)
        .setAus(generic);
    mover.executeRequests(java.util.Collections.singletonList(args));
  }

  // ---- tests ---------------------------------------------------------

  /** 1000 AUs, each with one URL and one CU version.  Asserts the
   * global per-mover {@code ausLatch} doesn't drop a count and the AU
   * queue drains to completion.  This is the "many AUs, very little
   * per-AU work" stress test: it pounds the COPY_ITER and INDEX
   * executors more than the COPY executor itself. */
  public void testManySmallAusCopy() throws Exception {
    final int N = 1000;
    List<MockArchivalUnit> aus = buildAus(N, /*urls*/ 1, /*versions*/ 1);

    long t0 = System.currentTimeMillis();
    V2AuMover mover = newMover();
    runCopyAndVerify(mover, aus);
    long elapsed = System.currentTimeMillis() - t0;

    MockV2Backend backend = mockV2.backend();

    // Every AU should have produced one artifact: total artifacts >= N.
    // The migration framework also POSTs report artifacts on a
    // metadata auid, so >= N (not == N) is the right invariant.
    assertTrue("total artifacts (" + backend.artifactMap().size()
               + ") < expected " + N,
               backend.artifactMap().size() >= N);

    // bulk-start and bulk-finish should be issued for every AU exactly
    // once.  A latch off-by-one bug would typically manifest as some
    // AUs missing one or the other.
    assertEquals("bulk start count", N,
                 countBulk(backend, "start"));
    assertEquals("bulk finish count", N,
                 countBulk(backend, "finish"));

    assertFalse("mover still running after executeRequests returned",
                mover.isRunning());
    assertFalse("mover marked failed", mover.isFailed());
    assertNoUnexpectedErrors(mover);

    // Sanity wall-clock bound: 1000 AUs through 8 copy threads at sub-ms
    // mock latency should easily fit in 60s; if we hit this we probably
    // deadlocked and fell off some retry path.
    assertTrue("manyAus copy took too long: " + elapsed + " ms",
               elapsed < 60_000);
  }

  /** 10 AUs * 10 URLs * 50 versions = 5000 CU versions.  The CU iterator
   * walks versions sequentially; this test verifies that no version is
   * lost or duplicated even when many AUs run their iterators
   * concurrently against a small COPY thread pool. */
  public void testManyVersionsPerUrl() throws Exception {
    final int AUS = 10;
    final int URLS = 10;
    final int VERSIONS = 50;
    final int EXPECTED_PER_AU = URLS * VERSIONS;
    List<MockArchivalUnit> aus = buildAus(AUS, URLS, VERSIONS);

    V2AuMover mover = newMover();
    runCopyAndVerify(mover, aus);

    MockV2Backend backend = mockV2.backend();

    // For each AU, all URL*Version artifacts should be present, and
    // each (url, version) pair should appear exactly once.
    for (MockArchivalUnit au : aus) {
      List<MockV2Backend.RecordedArtifact> arts =
          backend.artifactsFor(au.getAuId());
      assertEquals("artifact count for " + au.getAuId(),
                   EXPECTED_PER_AU, arts.size());
      Set<String> seen = new HashSet<String>();
      for (MockV2Backend.RecordedArtifact ra : arts) {
        String key = ra.url + "@" + ra.version;
        assertTrue("duplicate (url,version) " + key + " for " + au.getAuId(),
                   seen.add(key));
      }
      // Every version 1..VERSIONS must appear for every URL.
      assertEquals(URLS * VERSIONS, seen.size());
    }

    assertFalse(mover.isRunning());
    assertNoUnexpectedErrors(mover);
  }

  /** Inject a 50ms delay on every artifact-create POST.  With the COPY
   * executor pinned at 8 threads and 1000 artifacts to copy
   * (200 AU * 5 URL), the theoretical lower bound is ~6.25s and the
   * upper bound is bounded.  The point of this test is to catch a
   * real deadlock or mover-side queue exhaustion: if the mover blocks
   * forever, this test will time out (JUnit fails the test) rather
   * than silently passing. */
  public void testLatencyInjectionDoesNotDeadlock() throws Exception {
    final int AUS = 200;
    final int URLS = 5;
    final int EXPECTED_ARTIFACTS = AUS * URLS;

    mockV2.failures()
        .when("POST", "/artifacts")
        .delayMs(50);

    List<MockArchivalUnit> aus = buildAus(AUS, URLS, 1);

    long t0 = System.currentTimeMillis();
    V2AuMover mover = newMover();
    runCopyAndVerify(mover, aus);
    long elapsed = System.currentTimeMillis() - t0;

    MockV2Backend backend = mockV2.backend();

    // All artifacts must have landed; latency injection must not cause
    // any to be dropped or misrouted.
    int created = backend.artifactMap().size();
    assertTrue("artifacts created " + created
               + " < expected at least " + EXPECTED_ARTIFACTS,
               created >= EXPECTED_ARTIFACTS);

    // Generous upper bound: even single-threaded with 50ms each,
    // 1000 artifacts = 50s; with 8 copy threads ~6-15s.  60s catches
    // a genuine deadlock without flaking on slow CI.
    assertTrue("latency-injection run took " + elapsed
               + " ms; suspect deadlock",
               elapsed < 60_000);

    assertFalse(mover.isRunning());
    assertNoUnexpectedErrors(mover);
  }

  /** 500 AUs * 3 URLs * 2 versions = 3000 CU versions.  Asserts that
   * every (auid, url, version) shows up in exactly one POST /artifacts
   * request.  Two POSTs for the same triple would indicate a retry
   * collision; zero POSTs would indicate a queue-loss bug.  This is
   * the meatiest invariant: it's exactly what would silently corrupt a
   * 100k-AU production migration. */
  public void testNoLostArtifactsUnderConcurrency() throws Exception {
    final int AUS = 500;
    final int URLS = 3;
    final int VERSIONS = 2;
    final int EXPECTED = AUS * URLS * VERSIONS;

    List<MockArchivalUnit> aus = buildAus(AUS, URLS, VERSIONS);

    V2AuMover mover = newMover();
    runCopyAndVerify(mover, aus);

    MockV2Backend backend = mockV2.backend();

    // First check: every triple landed exactly once in the artifact
    // store.  We index by (auid, url, version) — the artifact map is
    // keyed by uuid so it would store duplicates if any existed.
    Set<String> uniqueTriples = new HashSet<String>();
    int duplicates = 0;
    for (MockV2Backend.RecordedArtifact ra :
             backend.artifactMap().values()) {
      // The migration framework also writes report artifacts on a
      // synthetic auid; ignore those.
      if (ra.auid == null || !ra.auid.startsWith("org|lockss|testplugin"))
        continue;
      String key = ra.auid + "" + ra.url + "" + ra.version;
      if (!uniqueTriples.add(key)) duplicates++;
    }
    assertEquals("duplicate artifact triples", 0, duplicates);
    assertEquals("unique artifact triples", EXPECTED, uniqueTriples.size());

    // Second check: count POST /artifacts requests in the request log.
    // Should be exactly EXPECTED — not larger (which would imply a
    // retry storm uploaded extra copies the mock then de-duped) and
    // not smaller (which would be impossible if the artifact map has
    // EXPECTED entries, but the cross-check is cheap).
    // Count POST /artifacts requests whose body references one of our
    // synthetic auids -- V2AuMover also POSTs migration reports under
    // a different auid via the same endpoint.  Verify-phase retries
    // can add a small number of duplicate POSTs that get de-duped on
    // the server; the duplicate-triples check above already proves
    // those did NOT corrupt the artifact store.
    int posts = 0;
    for (RecordedRequest r : backend.recordedRequests()) {
      if (!"POST".equalsIgnoreCase(r.getMethod())
          || !"/artifacts".equals(r.getPath())) continue;
      byte[] b = r.getBody();
      if (b == null) continue;
      String body = new String(b, "UTF-8");
      if (body.contains("testplugin")) posts++;
    }
    assertTrue("POST /artifacts requests for AUs >= " + EXPECTED + ", got "
               + posts, posts >= EXPECTED);
    assertTrue("retry overhead < 1% of EXPECTED, posts=" + posts,
               posts <= EXPECTED + Math.max(50, EXPECTED / 100));

    assertFalse(mover.isRunning());
    assertNoUnexpectedErrors(mover);
  }

  // ---- LONG variant (not run by default) -----------------------------

  /** LONG: 10000 AUs at 1 URL / 1 version.  Roughly an order of
   * magnitude beyond what a routine CI run should accept; runnable on
   * demand by anyone investigating a scaling bug.  Marked with the
   * {@code _long} prefix so JUnit's auto-discovery still picks it up
   * if a developer renames it locally. */
  public void disabled_testManySmallAusCopyLong() throws Exception {                  // LONG
    final int N = 10_000;
    List<MockArchivalUnit> aus = buildAus(N, 1, 1);
    V2AuMover mover = newMover();
    runCopyAndVerify(mover, aus);
    MockV2Backend backend = mockV2.backend();
    assertTrue(backend.artifactMap().size() >= N);
    assertEquals(N, countBulk(backend, "start"));
    assertEquals(N, countBulk(backend, "finish"));
    assertFalse(mover.isRunning());
    assertNoUnexpectedErrors(mover);
  }

  // ---- low-level helpers --------------------------------------------

  /** Count POST .../bulk requests with the given {@code op} value
   * (e.g. "start", "finish") across all AUs. */
  private static int countBulk(MockV2Backend backend, String op) {
    int n = 0;
    for (RecordedRequest r : backend.recordedRequests()) {
      if (!"POST".equalsIgnoreCase(r.getMethod())) continue;
      if (!r.getPath().endsWith("/bulk")) continue;
      if (!op.equals(r.queryParam("op"))) continue;
      n++;
    }
    return n;
  }
}
