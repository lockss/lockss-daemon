/*
 * Copyright (c) 2026, Board of Trustees of Leland Stanford Jr. University.
 * All rights reserved.
 */
package org.lockss.laaws;

import java.util.List;

import org.lockss.laaws.mock.MockV2Backend;
import org.lockss.laaws.mock.RecordedRequest;
import org.lockss.test.MockArchivalUnit;

/** Smoke test that proves the {@link org.lockss.laaws.mock.MockV2Lockss}
 * fixture can drive a live {@link V2AuMover} through a CopyAus operation
 * end-to-end without errors.
 *
 * Definition of done for the harness foundation: this test passes. */
public class TestV2AuMoverHarness extends V2MoverTestBase {

  private static final String TEST_AUID =
      "org|lockss|testplugin|MockPlug&base_url~http%3A%2F%2Fexample%2Ecom%2F";

  public void testSmokeCopyOneAu() throws Exception {
    // 1. Build a tiny v1 AU with 3 URLs, 1 version each.
    MockArchivalUnit au = createV1Au(TEST_AUID, 3, 1);

    // 2. Drive a CopyAndVerify through the live V2AuMover.  baseArgs()
    //    targets the running MockV2Lockss; dryRun=true suppresses the
    //    isTargetInMigrationMode probe, so no real V2 is needed.
    V2AuMover mover = newMover();
    V2AuMover.Args args = baseArgs(MigrationManager.OpType.CopyAndVerify)
        .setAu(au);
    mover.executeRequests(java.util.Collections.singletonList(args));

    MockV2Backend backend = mockV2.backend();

    // 3. The harness must have observed bulk-start + bulk-finish exactly
    //    once each, and 3 artifact uploads (one per CU).
    int bulkStarts = countBulkOp(backend, "start", au.getAuId());
    int bulkFinishes = countBulkOp(backend, "finish", au.getAuId());
    assertEquals("bulk start", 1, bulkStarts);
    assertEquals("bulk finish", 1, bulkFinishes);

    // POST /artifacts is the create-artifact endpoint; the migration
    // reports also POST artifacts on a different auid, so filter by ours.
    int au_artifacts = backend.artifactsFor(au.getAuId()).size();
    assertEquals("artifacts recorded for auid", 3, au_artifacts);

    // 4. No URL-level copy errors. State-copy errors are tolerated here
    //    because MockIdentityManager is not an IdentityManagerImpl, so
    //    AuStateMover/Checker can't find agreements; integration tests in
    //    follow-up tasks will install a real IdentityManagerImpl.
    List<String> errors = mover.getErrors();
    if (errors != null) {
      for (String e : errors) {
        if (e.contains("Copy state") || e.contains("AU State Check")) continue;
        fail("V2AuMover reported unexpected error: " + e);
      }
    }

    // 5. Sanity: mover ran to completion.
    assertFalse("mover still running", mover.isRunning());
    assertFalse("mover marked failed", mover.isFailed());
  }

  private static int countBulkOp(MockV2Backend backend, String op,
                                 String auid) {
    int n = 0;
    for (RecordedRequest r : backend.recordedRequests()) {
      if (!"POST".equalsIgnoreCase(r.getMethod())) continue;
      if (!r.getPath().endsWith("/bulk")) continue;
      if (!r.getPath().contains(encode(auid))) continue;
      if (!op.equals(r.queryParam("op"))) continue;
      n++;
    }
    return n;
  }

  private static String encode(String s) {
    try {
      return java.net.URLEncoder.encode(s, "UTF-8")
          .replace("+", "%20");
    } catch (Exception e) {
      return s;
    }
  }
}
