/*

Copyright (c) 2000-2026 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.lockss.test.LockssTestCase;
import org.lockss.util.ListUtil;

/**
 * Tests for the incremental (paged) error/warning list retrieval added
 * for GitHub issue #743 ("Make the migration errors list
 * incremental"), which is implemented in {@link Counters}.
 */
public class TestCounters extends LockssTestCase {

  private Counters ctrs;

  public void setUp() throws Exception {
    super.setUp();
    ctrs = new Counters();
  }

  public void testEmptyList() {
    assertEquals(0, ctrs.getErrorListSize());
    assertEmpty(ctrs.getErrorsPage(0, 10));
  }

  /** A client polling from the start, one page at a time, should be
   * able to reconstruct the exact full list without ever re-fetching
   * an already-seen entry, and without the whole list being resent on
   * each call. */
  public void testIncrementalPollingGetsOnlyNewErrors() {
    for (int i = 0; i < 5; i++) {
      ctrs.addError("error " + i);
    }
    assertEquals(5, ctrs.getErrorListSize());

    // First poll: client has seen 0 so far, asks for what's new.
    List<String> page1 = ctrs.getErrorsPage(0, 3);
    assertEquals(ListUtil.list("error 0", "error 1", "error 2"), page1);

    // Add more errors between polls, as would happen during a live
    // migration.
    ctrs.addError("error 5");
    ctrs.addError("error 6");
    assertEquals(7, ctrs.getErrorListSize());

    // Second poll: client has seen 3, should get only the 4 new ones
    // (2, 3, 4 not yet seen by page1's caller are indices 3,4; plus
    // the 2 added since).  Exactly 4 new entries starting at index 3.
    List<String> page2 = ctrs.getErrorsPage(3, 4);
    assertEquals(ListUtil.list("error 3", "error 4", "error 5", "error 6"), page2);

    // A page request should never return more than requested, and
    // should never re-return entries already delivered by an earlier
    // page in this scenario.
    assertEquals(3, page1.size());
    assertEquals(4, page2.size());
  }

  /** A client that polls from the beginning, repeatedly asking for
   * everything not yet seen, must be able to reconstruct the entire
   * list. */
  public void testFullListReconstructableFromScratch() {
    List<String> expected = new ArrayList<>();
    for (int i = 0; i < 37; i++) {
      String msg = "msg " + i;
      expected.add(msg);
      ctrs.addError(msg);
    }

    List<String> reconstructed = new ArrayList<>();
    int pageSize = 5;
    while (reconstructed.size() < ctrs.getErrorListSize()) {
      int index = reconstructed.size();
      int remaining = ctrs.getErrorListSize() - index;
      List<String> page = ctrs.getErrorsPage(index, Math.min(pageSize, remaining));
      assertFalse("getErrorsPage returned an empty page though more entries "
                  + "remained", page.isEmpty());
      reconstructed.addAll(page);
    }
    assertEquals(expected, reconstructed);
  }

  /** Warnings share the same underlying list as errors (as before this
   * change), so they must also show up via the incremental API. */
  public void testWarningsIncludedInPagedList() {
    ctrs.addError("err 1");
    ctrs.addWarning("something odd");
    assertEquals(2, ctrs.getErrorListSize());
    List<String> page = ctrs.getErrorsPage(0, 2);
    assertEquals("err 1", page.get(0));
    assertEquals("Warning: something odd", page.get(1));
  }

  public void testPageBeyondEndIsEmpty() {
    ctrs.addError("only one");
    assertEmpty(ctrs.getErrorsPage(1, 10));
    assertEmpty(ctrs.getErrorsPage(5, 10));
  }

  public void testNegativeIndexIsEmpty() {
    ctrs.addError("only one");
    assertEmpty(ctrs.getErrorsPage(-1, 10));
  }

  /** Errors can be added by AU-mover worker threads at the same time
   * the servlet thread is paging through the list for a status
   * request.  Concurrent addition during a read must not throw
   * (e.g. ConcurrentModificationException), and every page read must
   * be an internally-consistent prefix slice: no duplicate, garbled,
   * or out-of-order entries. */
  public void testConcurrentAdditionsWhilePolling() throws Exception {
    final int numToAdd = 2000;
    final AtomicBoolean failed = new AtomicBoolean(false);
    final List<Throwable> errors =
      Collections.synchronizedList(new ArrayList<Throwable>());

    Thread writer = new Thread(() -> {
      try {
        for (int i = 0; i < numToAdd; i++) {
          ctrs.addError("err " + i);
        }
      } catch (Throwable t) {
        failed.set(true);
        errors.add(t);
      }
    });

    final List<String> seen = new ArrayList<>();
    Thread reader = new Thread(() -> {
      try {
        long deadline = System.currentTimeMillis() + 25000;
        while (seen.size() < numToAdd && System.currentTimeMillis() < deadline) {
          int index = seen.size();
          int size = ctrs.getErrorListSize() - index;
          if (size > 0) {
            List<String> page = ctrs.getErrorsPage(index, size);
            // Every entry returned must be exactly the expected
            // value for its position -- i.e., the list is a stable,
            // append-only sequence and pages are consistent prefix
            // slices, never reordered or duplicated.
            for (int i = 0; i < page.size(); i++) {
              String expected = "err " + (index + i);
              if (!expected.equals(page.get(i))) {
                throw new IllegalStateException(
                    "Expected " + expected + " but got " + page.get(i) +
                    " at position " + (index + i));
              }
            }
            seen.addAll(page);
          }
        }
      } catch (Throwable t) {
        failed.set(true);
        errors.add(t);
      }
    });

    writer.start();
    reader.start();
    writer.join(30000);
    reader.join(30000);

    if (failed.get()) {
      StringBuilder sb = new StringBuilder();
      for (Throwable t : errors) {
        sb.append(t).append("\n");
      }
      fail("Concurrent read/write of error list failed:\n" + sb);
    }
    // The reader has a generous wall-clock deadline (not a CPU-bound
    // spin cap, which could be exhausted by scheduler contention
    // rather than a real bug), so if it didn't collect everything,
    // something is genuinely wrong (e.g. livelock or a correctness
    // bug), not just a slow machine.
    assertEquals(numToAdd, seen.size());
    for (int i = 0; i < numToAdd; i++) {
      assertEquals("err " + i, seen.get(i));
    }
  }
}
